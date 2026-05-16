/*
 __  __                  _               _               _ 
|  \/  | ___  __ _  __ _| |__   __ _ ___| |_ ___ _ __ __| |
| |\/| |/ _ \/ _` |/ _` | '_ \ / _` / __| __/ _ \ '__/ _` |
| |  | |  __/ (_| | (_| | |_) | (_| \__ \ ||  __/ | | (_| |
|_|  |_|\___|\__, |\__,_|_.__/ \__,_|___/\__\___|_|  \__,_|
             |___/                                         
© Perpetrated by tonikelope since 2016
 */
package com.tonikelope.megabasterd;

import static com.tonikelope.megabasterd.MainPanel.THREAD_POOL;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public final class SmartMegaProxyManager {

    public static final int PROXY_BLOCK_TIME = 300;
    public static final int PROXY_AUTO_REFRESH_TIME = 60;
    public static final int PROXY_AUTO_REFRESH_SLEEP_TIME = 30;
    public static final boolean RESET_SLOT_PROXY = true;
    public static final boolean RANDOM_SELECT = true;
    /**
     * Default for the post-509 window during which SmartProxy stays active for
     * the affected download even after a successful chunk. Was the hard-coded
     * {@code ChunkDownloader.SMART_PROXY_RECHECK_509_TIME = 3600}; now
     * overridable via DB setting "smart_proxy_509_recheck_window" so a user
     * whose VPN clears quota in seconds isn't forced into a 1-hour proxy-mode
     * window. (#751 / C4)
     */
    public static final int RECHECK_509_WINDOW_DEFAULT = 3600;

    private static final Logger LOG = Logger.getLogger(SmartMegaProxyManager.class.getName());
    private final ConcurrentHashMap<String, Long[]> _proxy_list;
    // ConcurrentHashMap (not HashMap): the Authenticator.getPasswordAuthentication
    // callback at SmartProxyAuthenticator.getPasswordAuthentication() runs on
    // arbitrary JDK HTTP/SOCKS connection threads, with no shared monitor with
    // refreshProxyList() which clears+repopulates this map. Plain HashMap under
    // concurrent structural mutation (clear+put) can NPE on resize and stall
    // the connection thread.
    private static final java.util.concurrent.ConcurrentHashMap<String, String> PROXY_LIST_AUTH = new java.util.concurrent.ConcurrentHashMap<>();
    private final MainPanel _main_panel;
    private volatile int _ban_time;
    private volatile int _proxy_timeout;
    private volatile boolean _force_smart_proxy;
    private volatile int _autorefresh_time;
    private volatile long _last_refresh_timestamp;
    private volatile boolean _random_select;
    private volatile boolean _reset_slot_proxy;
    private volatile int _recheck_509_window;

    public boolean isRandom_select() {
        return _random_select;
    }

    public boolean isReset_slot_proxy() {
        return _reset_slot_proxy;
    }

    /**
     * Window (seconds) after a 509 during which SmartProxy stays "armed" for
     * affected downloads, even after individual chunks succeed. Configurable
     * via DB setting "smart_proxy_509_recheck_window". Defaults to
     * {@link #RECHECK_509_WINDOW_DEFAULT} (3600 s). (#751 / C4)
     */
    public int getRecheck_509_window() {
        return _recheck_509_window;
    }

    public int getProxy_timeout() {
        return _proxy_timeout;
    }

    public boolean isForce_smart_proxy() {
        return _force_smart_proxy;
    }

    public SmartMegaProxyManager(MainPanel main_panel) {
        // Proxy list URLs are now discovered by scanning the "custom_proxy_list"
        // textarea for '#URL' lines on every refresh, so the manager no longer
        // needs an explicit URL parameter and can aggregate from multiple
        // sources simultaneously. (#753)
        _proxy_list = new ConcurrentHashMap<>();
        _main_panel = main_panel;

        // ChunkDownloader.java:200 silently disables SmartProxy when a static
        // HTTP proxy is also configured (`&& !MainPanel.isUse_proxy()`). The
        // user often doesn't realise this and assumes SmartProxy is still
        // protecting them from 509. Surface it loudly at startup. (#751)
        if (MainPanel.isUse_proxy()) {
            LOG.log(Level.WARNING, "[SmartProxy] Static HTTP proxy is ALSO enabled in settings; "
                    + "SmartProxy will be IGNORED by ChunkDownloader (static proxy takes priority). "
                    + "Disable the static HTTP proxy if you want SmartProxy to handle 509.");
        }

        refreshSmartProxySettings();

        THREAD_POOL.execute(() -> {
            refreshProxyList();

            // Honour MainPanel.isExit() so the auto-refresh thread terminates
            // cleanly on shutdown instead of spinning until JVM kills the
            // daemon. Also restore the interrupt flag on InterruptedException.
            while (!_main_panel.isExit()) {

                while (!_main_panel.isExit() && System.currentTimeMillis() < _last_refresh_timestamp + _autorefresh_time * 60L * 1000L) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                if (!_main_panel.isExit() && MainPanel.isUse_smart_proxy()) {

                    refreshProxyList();
                }
            }
        });
    }

    private static int clampWithWarn(String key, int value, int min, int max) {
        if (value < min) {
            LOG.log(Level.WARNING, "[SmartProxy] setting {0}={1} is below the supported minimum {2}; clamping. Values that low make the recovery path effectively unusable.",
                    new Object[]{key, value, min});
            return min;
        }
        if (value > max) {
            LOG.log(Level.WARNING, "[SmartProxy] setting {0}={1} is above the supported maximum {2}; clamping.",
                    new Object[]{key, value, max});
            return max;
        }
        return value;
    }

    private synchronized int countBlockedProxies() {

        int i = 0;

        Long current_time = System.currentTimeMillis();

        for (String k : _proxy_list.keySet()) {

            if (_proxy_list.get(k)[0] != -1 && _proxy_list.get(k)[0] > current_time - _ban_time * 1000L) {

                i++;
            }
        }

        return i;

    }

    public synchronized void refreshSmartProxySettings() {
        String smartproxy_ban_time = DBTools.selectSettingValue("smartproxy_ban_time");

        // Clamp ban_time to [10, 3600] s. Below 10 s a banned proxy is
        // unblocked before the worker that banned it has finished retrying
        // somewhere else, so the pool churns the same bad entries; above
        // 1 h the proxy is effectively dead and should just be removed
        // from the list manually. (#752)
        int requested_ban = MiscTools.parseIntOr(smartproxy_ban_time, PROXY_BLOCK_TIME);
        _ban_time = clampWithWarn("smartproxy_ban_time", requested_ban, 10, 3600);

        String smartproxy_timeout = DBTools.selectSettingValue("smartproxy_timeout");

        // Clamp proxy_timeout to [3, 120] s. Below 3 s most real-world
        // public proxies cannot complete a TCP+TLS handshake, so every
        // attempt times out and the worker burns through the list without
        // ever connecting. Stored and reported in seconds; converted to
        // ms below for the JDK URLConnection setters. (#752)
        int requested_timeout = MiscTools.parseIntOr(smartproxy_timeout, Transference.HTTP_PROXY_TIMEOUT / 1000);
        _proxy_timeout = clampWithWarn("smartproxy_timeout", requested_timeout, 3, 120) * 1000;

        String force_smart_proxy_string = DBTools.selectSettingValue("force_smart_proxy");

        if (force_smart_proxy_string != null) {

            _force_smart_proxy = force_smart_proxy_string.equals("yes");
        } else {
            _force_smart_proxy = MainPanel.FORCE_SMART_PROXY;
        }

        String autorefresh_smart_proxy_string = DBTools.selectSettingValue("smartproxy_autorefresh_time");

        _autorefresh_time = MiscTools.parseIntOr(autorefresh_smart_proxy_string, PROXY_AUTO_REFRESH_TIME);

        String reset_slot_proxy = DBTools.selectSettingValue("reset_slot_proxy");

        if (reset_slot_proxy != null) {

            _reset_slot_proxy = reset_slot_proxy.equals("yes");
        } else {
            _reset_slot_proxy = RESET_SLOT_PROXY;
        }

        String random_select = DBTools.selectSettingValue("random_proxy");

        if (random_select != null) {

            _random_select = random_select.equals("yes");
        } else {
            _random_select = RANDOM_SELECT;
        }

        String recheck_setting = DBTools.selectSettingValue("smart_proxy_509_recheck_window");

        int recheck = MiscTools.parseIntOr(recheck_setting, RECHECK_509_WINDOW_DEFAULT);
        // Clamp to a sane range: anything below 60 s is pointless (a refresh
        // would arrive sooner than that), anything above 24 h is just
        // "permanent" which is what FORCE proxy mode already expresses.
        if (recheck < 60) {
            recheck = 60;
        } else if (recheck > 86_400) {
            recheck = 86_400;
        }
        _recheck_509_window = recheck;

        LOG.log(Level.INFO, "SmartProxy BAN_TIME: " + String.valueOf(_ban_time) + "   TIMEOUT: " + String.valueOf(_proxy_timeout / 1000) + "   REFRESH: " + String.valueOf(_autorefresh_time) + "   FORCE: " + String.valueOf(_force_smart_proxy) + "   RANDOM: " + String.valueOf(_random_select) + "   RESET-SLOT-PROXY: " + String.valueOf(_reset_slot_proxy) + "   RECHECK-509: " + String.valueOf(_recheck_509_window));
    }

    public synchronized int getProxyCount() {

        return _proxy_list.size();
    }

    /**
     * Returns a snapshot of every proxy in the pool, regardless of ban
     * state, as {@code {address, "http"|"socks"}} pairs in pool order.
     * Used by the test dialog to enumerate the pool exhaustively --
     * {@link #getProxy(ArrayList)} cannot be used for that because it
     * filters banned entries and ban-recovers via timeout, so a test
     * could never observe a currently-banned proxy. (#753 audit)
     */
    public synchronized java.util.List<String[]> getProxySnapshot() {
        java.util.List<String[]> snapshot = new ArrayList<>(_proxy_list.size());
        for (Map.Entry<String, Long[]> e : _proxy_list.entrySet()) {
            Long[] meta = e.getValue();
            boolean socks = meta != null && meta[1] != null && meta[1].longValue() != -1L;
            snapshot.add(new String[]{e.getKey(), socks ? "socks" : "http"});
        }
        return snapshot;
    }

    public synchronized String[] getProxy(ArrayList<String> excluded) {

        // Iterative refresh loop with a cap. Was recursive: every call that
        // found all proxies excluded slept 30s then recursed -- with an
        // ever-growing excluded list (workers keep adding failed proxies),
        // a permanently-bad list could deepen the stack indefinitely and
        // eventually StackOverflowError. 5 attempts == ~2.5 min, plenty for
        // a refresh to pull a usable proxy.
        final int MAX_REFRESH_ATTEMPTS = 5;

        for (int attempt = 0; attempt < MAX_REFRESH_ATTEMPTS; attempt++) {

            if (_proxy_list.size() > 0) {

                Set<String> keys = _proxy_list.keySet();

                List<String> keysList = new ArrayList<>(keys);

                if (isRandom_select()) {
                    Collections.shuffle(keysList);
                }

                Long current_time = System.currentTimeMillis();

                for (String k : keysList) {

                    Long[] entry = _proxy_list.get(k);

                    if (entry == null) {
                        continue;
                    }

                    if ((entry[0] == -1 || entry[0] < current_time - _ban_time * 1000L) && (excluded == null || !excluded.contains(k))) {

                        return new String[]{k, entry[1] == -1L ? "http" : "socks"};
                    }
                }
            }

            LOG.log(Level.WARNING, "{0} Smart Proxy Manager: NO PROXYS AVAILABLE!! (Refreshing in {1} secs, attempt {2}/{3})",
                    new Object[]{Thread.currentThread().getName(), PROXY_AUTO_REFRESH_SLEEP_TIME, attempt + 1, MAX_REFRESH_ATTEMPTS});

            try {
                Thread.sleep(PROXY_AUTO_REFRESH_SLEEP_TIME * 1000L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return null;
            }

            refreshProxyList();
        }

        return null;
    }

    /**
     * Rewrites the {@code custom_proxy_list} DB setting so that the only
     * inline proxy entries are those whose addresses appear in
     * {@code working_addrs}. Lines starting with {@code #} (remote URL
     * sources) and blank separator lines are preserved verbatim, so a
     * user who relies on auto-refreshed lists can prune dead entries
     * without losing the URL sources that feed them. The SOCKS marker
     * and any auth trailer are looked up from live state so each saved
     * line round-trips through {@link #parseProxyEntry} unchanged. After
     * writing, kicks an async refresh so the live pool matches the new
     * textarea immediately. (#753)
     *
     * @param working_addrs addresses (IP:PORT) to keep, in the desired
     *                      output order
     * @return number of inline entries written
     * @throws SQLException if the DB write fails
     */
    public synchronized int saveWorkingProxiesToCustomList(java.util.Collection<String> working_addrs) throws SQLException {

        String current = DBTools.selectSettingValue("custom_proxy_list");
        StringBuilder sb = new StringBuilder();
        if (current != null) {
            for (String line : current.split("\\r?\\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    sb.append(line).append('\n');
                }
            }
        }

        int written = 0;
        for (String addr : working_addrs) {
            Long[] entry = _proxy_list.get(addr);
            // Entry shape: {ban_ts, type} where type == -1L for HTTP and
            // 1L for SOCKS. An entry missing from the live map is still
            // saved (as HTTP, no auth) -- it was working a moment ago,
            // and over-writing is safer than dropping it silently.
            boolean socks = entry != null && entry[1] != null && entry[1] != -1L;
            String auth = PROXY_LIST_AUTH.get(addr);
            if (socks) {
                sb.append('*');
            }
            sb.append(addr);
            if (auth != null) {
                sb.append('@').append(auth);
            }
            sb.append('\n');
            written++;
        }

        DBTools.insertSettingValue("custom_proxy_list", sb.toString());

        // Refresh asynchronously so the caller (Swing EDT in practice)
        // returns immediately.
        MainPanel.THREAD_POOL.execute(this::refreshProxyList);

        return written;
    }

    public synchronized void blockProxy(String proxy, String cause) {

        // All mutators (blockProxy, refreshProxyList, getProxy) are synchronized
        // on `this`, so the get-mutate sequence below is atomic w.r.t. other
        // map operations -- the ConcurrentHashMap is belt-and-braces. The
        // previous code also did a redundant put() back of the same array
        // reference; that's a no-op since proxy_data was already in the map.
        // Use computeIfPresent to express the atomic mutate-in-place clearly,
        // and to safely no-op if the entry was concurrently removed (e.g.,
        // by a refreshProxyList() that's running on the same monitor in some
        // future refactor where this method is no longer synchronized). (#751)
        if (_proxy_list.containsKey(proxy)) {

            if (this._ban_time == 0) {

                _proxy_list.remove(proxy);

                LOG.log(Level.WARNING, "[Smart Proxy] REMOVING PROXY {0} ({1})", new Object[]{proxy, cause});

            } else {

                _proxy_list.computeIfPresent(proxy, (k, proxy_data) -> {
                    proxy_data[0] = System.currentTimeMillis();
                    return proxy_data;
                });

                LOG.log(Level.WARNING, "[Smart Proxy] BLOCKING PROXY {0} ({1} secs) ({2})", new Object[]{proxy, _ban_time, cause});

            }

            _main_panel.getView().updateSmartProxyStatus("SmartProxy: ON (" + String.valueOf(getProxyCount() - countBlockedProxies()) + ")" + (this.isForce_smart_proxy() ? " F!" : ""));

        }
    }

    /**
     * Parses a single proxy line into {@code into_list}/{@code into_auth}.
     * Accepts the historical syntax ({@code [*]IP:PORT[@user_b64:password_b64]})
     * AND scheme-prefixed forms ({@code http://}, {@code https://},
     * {@code socks://}, {@code socks4://}, {@code socks4a://}, {@code socks5://}),
     * which were previously rejected as malformed. (#753)
     * <p>
     * Note: authentication is only supported in the legacy
     * {@code IP:PORT@b64user:b64pass} trailer form. A standard
     * {@code http://user:pass@host:port} URL is NOT decoded — write
     * {@code http://host:port@b64user:b64pass} (or the bare equivalent) to
     * supply credentials.
     *
     * @param raw       raw line (will be trimmed; blank lines silently ignored)
     * @param source    "custom" or "URL" — only used in the warning log
     * @param into_list destination for {@code IP:PORT -> {ban_ts, type}} entries
     * @param into_auth destination for {@code IP:PORT -> b64user:b64pass} pairs
     */
    private static void parseProxyEntry(String raw, String source,
            java.util.Map<String, Long[]> into_list,
            java.util.Map<String, String> into_auth) {

        if (raw == null) {
            return;
        }
        String line = raw.trim();
        if (line.isEmpty()) {
            return;
        }

        // Lines that start with '#' identify a remote proxy-list URL embedded
        // in custom_proxy_list. They are extracted elsewhere; silently skip
        // them here instead of logging them as malformed entries.
        if (line.startsWith("#")) {
            return;
        }

        boolean socks = false;
        boolean had_scheme = false;

        // Strip the historical "*" SOCKS marker first; it can also precede a
        // scheme prefix in case a user mixed conventions.
        if (line.startsWith("*")) {
            socks = true;
            line = line.substring(1).trim();
        }

        // Recognise scheme prefixes case-insensitively. All SOCKS variants
        // collapse to JDK Proxy.Type.SOCKS. HTTPS proxies also use the
        // HTTP CONNECT method on the JDK side, so they map to
        // Proxy.Type.HTTP.
        String lower = line.toLowerCase();
        if (lower.startsWith("http://")) {
            line = line.substring(7);
            had_scheme = true;
        } else if (lower.startsWith("https://")) {
            line = line.substring(8);
            had_scheme = true;
        } else if (lower.startsWith("socks5://")) {
            socks = true;
            line = line.substring(9);
            had_scheme = true;
        } else if (lower.startsWith("socks4a://")) {
            socks = true;
            line = line.substring(10);
            had_scheme = true;
        } else if (lower.startsWith("socks4://")) {
            socks = true;
            line = line.substring(9);
            had_scheme = true;
        } else if (lower.startsWith("socks://")) {
            socks = true;
            line = line.substring(8);
            had_scheme = true;
        }

        if (line.contains("@")) {
            String[] proxy_parts = line.split("@");
            if (proxy_parts.length != 2) {
                // Stray '@' (e.g. user@pass@host:port): can't disambiguate
                // which half is the host:port, so reject. (#751)
                LOG.log(Level.WARNING, "[Smart Proxy] skipping malformed {0} entry: {1}", new Object[]{source, raw});
                return;
            }
            // The host:port half may carry a trailing path the scheme brought
            // along (e.g. http://host:port/foo@auth). Strip from that half
            // only -- the auth half is base64(user):base64(pass), and the
            // base64 alphabet includes '/', so a global path-strip would
            // silently corrupt legitimate auth values (#753 audit).
            String hostport = proxy_parts[0];
            int slash = hostport.indexOf('/');
            if (slash >= 0) {
                hostport = hostport.substring(0, slash);
            }
            hostport = hostport.trim();

            if (!hostport.matches(".+?:[0-9]{1,5}")) {
                LOG.log(Level.WARNING, "[Smart Proxy] skipping malformed {0} entry: {1}", new Object[]{source, raw});
                return;
            }

            // Disambiguation guard: a URL-style "user:pass@host:port"
            // credential where pass is numeric (e.g. "user:1234") would
            // ALSO match the legacy "host:port@b64u:b64p" shape on parts[0]
            // and lead us to store "user:1234" as the host. When the line
            // had a scheme prefix AND parts[1] itself looks like host:port
            // (whereas a real base64 trailer has no internal port-shaped
            // segment), it's almost certainly URL-style auth -- reject
            // with a pointed log instead of silently misparsing. (#753 audit)
            if (had_scheme && proxy_parts[1].matches(".+?:[0-9]{1,5}")) {
                LOG.log(Level.WARNING, "[Smart Proxy] skipping {0} entry with URL-style user:pass@host:port credential (not supported -- use IP:PORT@b64user:b64pass form instead): {1}",
                        new Object[]{source, raw});
                return;
            }

            into_auth.put(hostport, proxy_parts[1]);
            into_list.put(hostport, new Long[]{-1L, socks ? 1L : -1L});
            return;
        }

        // No auth trailer: drop any trailing path the scheme may have
        // brought along (e.g. http://1.2.3.4:8080/list.txt). Bare lines
        // without a scheme are not allowed to have a path -- the legacy
        // parser rejected them via the strict regex.
        if (had_scheme) {
            int slash = line.indexOf('/');
            if (slash >= 0) {
                line = line.substring(0, slash);
            }
        }
        line = line.trim();

        if (line.matches(".+?:[0-9]{1,5}")) {
            into_list.put(line, new Long[]{-1L, socks ? 1L : -1L});
        } else {
            LOG.log(Level.WARNING, "[Smart Proxy] skipping malformed {0} entry: {1}", new Object[]{source, raw});
        }
    }

    /**
     * Fetches a single proxy-list URL and merges its entries into the
     * supplied maps. Throws {@link IOException} on connect/read failure or
     * non-200 status — callers swallow the exception per-URL so a single
     * bad source can't kill the whole refresh. (#753)
     */
    private static void fetchAndMerge(String url_str,
            java.util.Map<String, Long[]> into_list,
            java.util.Map<String, String> into_auth) throws IOException {

        HttpURLConnection con = null;
        try {
            URL url = new URL(url_str);

            if (!"https".equalsIgnoreCase(url.getProtocol())) {
                LOG.log(Level.WARNING, "Smart proxy list URL is not HTTPS ({0}); response is unauthenticated and could be MITM'd", url_str);
            }

            con = (HttpURLConnection) url.openConnection();
            con.setUseCaches(false);
            con.setRequestProperty("User-Agent", MainPanel.DEFAULT_USER_AGENT);

            // Bound the fetch. Without timeouts a hung proxy-list URL holds
            // the SmartMegaProxyManager monitor (synchronized method) and
            // blocks every getProxy() / blockProxy() call across all
            // workers -- exactly the wrong time to stall, since we're
            // already in 509 recovery. (#751)
            con.setConnectTimeout(15_000);
            con.setReadTimeout(30_000);

            int http_status = con.getResponseCode();
            if (http_status != 200) {
                MiscTools.drainAndCloseErrorStream(con);
                throw new IOException("HTTP " + http_status);
            }

            String data;
            try (InputStream is = con.getInputStream(); ByteArrayOutputStream byte_res = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];
                int reads;
                while ((reads = is.read(buffer)) != -1) {
                    byte_res.write(buffer, 0, reads);
                }
                data = new String(byte_res.toByteArray(), "UTF-8");
            }

            for (String line : data.split("\n")) {
                parseProxyEntry(line, "URL", into_list, into_auth);
            }
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    /**
     * Refreshes the live proxy pool from {@code custom_proxy_list}. The
     * textarea is scanned line by line:
     * <ul>
     *   <li>Lines starting with {@code #} followed by {@code http://} or
     *       {@code https://} are treated as remote proxy-list URLs. ALL
     *       such lines are fetched and the entries aggregated. (#753)</li>
     *   <li>Other non-empty lines are parsed as inline proxy entries by
     *       {@link #parseProxyEntry}.</li>
     * </ul>
     * Inline entries and URL-sourced entries coexist; a single failed URL
     * does not invalidate the rest. The live pool is only swapped if the
     * resulting set has at least one entry — otherwise the previous list
     * is preserved and the status label is marked {@code "stale"}. (#751)
     */
    public synchronized void refreshProxyList() {

        try {
            String custom_proxy_list = DBTools.selectSettingValue("custom_proxy_list");

            LinkedHashMap<String, Long[]> new_list = new LinkedHashMap<>();
            HashMap<String, String> new_auth = new HashMap<>();
            ArrayList<String> urls = new ArrayList<>();
            boolean had_input = false;

            if (custom_proxy_list != null) {
                for (String line : custom_proxy_list.split("\\r?\\n")) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    had_input = true;
                    if (trimmed.startsWith("#")) {
                        // Strip the '#' and accept either '#http://...' or
                        // '# http://...' so users aren't tripped up by a
                        // stray space.
                        String url_part = trimmed.substring(1).trim();
                        String lower = url_part.toLowerCase();
                        if (lower.startsWith("http://") || lower.startsWith("https://")) {
                            urls.add(url_part);
                        } else if (!url_part.isEmpty()) {
                            LOG.log(Level.WARNING, "[Smart Proxy] skipping malformed #URL entry: {0}", line);
                        }
                    } else {
                        parseProxyEntry(line, "custom", new_list, new_auth);
                    }
                }
            }

            int urls_ok = 0;
            int urls_fail = 0;
            for (String u : urls) {
                try {
                    fetchAndMerge(u, new_list, new_auth);
                    urls_ok++;
                } catch (MalformedURLException ex) {
                    urls_fail++;
                    LOG.log(Level.SEVERE, "[Smart Proxy] proxy-list URL is malformed ({0}): {1}", new Object[]{u, ex.getMessage()});
                } catch (IOException ex) {
                    urls_fail++;
                    LOG.log(Level.WARNING, "[Smart Proxy] proxy-list URL fetch failed ({0}) -- continuing with remaining sources: {1}",
                            new Object[]{u, ex.getMessage()});
                }
            }

            if (new_list.isEmpty()) {
                // Preserve previous list. Was: clear() + populate during
                // parse, so an empty / garbage body wiped the previous list
                // before we knew whether the new one was viable. (#751)
                if (had_input) {
                    LOG.log(Level.WARNING, "[Smart Proxy] refresh produced 0 entries (URLs ok={0}, failed={1}) -- preserving previous list ({2} entries)",
                            new Object[]{urls_ok, urls_fail, _proxy_list.size()});
                    _main_panel.getView().updateSmartProxyStatus("SmartProxy: ON (" + String.valueOf(getProxyCount()) + " stale)" + (this.isForce_smart_proxy() ? " F!" : ""));
                } else {
                    _main_panel.getView().updateSmartProxyStatus("SmartProxy: ON (0 proxies!)" + (this.isForce_smart_proxy() ? " F!" : ""));
                    LOG.log(Level.INFO, "[Smart Proxy] no inline entries and no URLs configured");
                }
            } else {
                _proxy_list.clear();
                _proxy_list.putAll(new_list);
                PROXY_LIST_AUTH.clear();
                PROXY_LIST_AUTH.putAll(new_auth);

                // When SOME URLs failed but we still got a usable pool, show
                // the partial-success state so the user can tell one of
                // their providers is sick without having to read the log.
                String suffix = "";
                if (urls_fail > 0) {
                    suffix = " [" + urls_ok + "/" + (urls_ok + urls_fail) + " sources]";
                }
                _main_panel.getView().updateSmartProxyStatus("SmartProxy: ON (" + String.valueOf(getProxyCount()) + ")" + suffix + (this.isForce_smart_proxy() ? " F!" : ""));
                LOG.log(Level.INFO, "[Smart Proxy] proxy list refreshed ({0} entries; URLs ok={1}, failed={2})",
                        new Object[]{_proxy_list.size(), urls_ok, urls_fail});
            }
        } finally {
            _last_refresh_timestamp = System.currentTimeMillis();
        }
    }

    public static class SmartProxyAuthenticator extends Authenticator {

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {

            InetAddress ipaddr = getRequestingSite();
            int port = getRequestingPort();

            String auth_data;

            if ((auth_data = PROXY_LIST_AUTH.get(ipaddr.getHostAddress() + ":" + String.valueOf(port))) != null) {

                try {
                    String[] auth_data_parts = auth_data.split(":");

                    String user = new String(MiscTools.BASE642Bin(auth_data_parts[0]), "UTF-8");

                    String password = new String(MiscTools.BASE642Bin(auth_data_parts[1]), "UTF-8");

                    return new PasswordAuthentication(user, password.toCharArray());

                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(SmartMegaProxyManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            return null;
        }
    }

}
