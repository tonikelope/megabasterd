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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public final class SmartMegaProxyManager {

    public static String DEFAULT_SMART_PROXY_URL = null;
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
    private volatile String _proxy_list_url;
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

    public SmartMegaProxyManager(String proxy_list_url, MainPanel main_panel) {
        _proxy_list_url = (proxy_list_url != null && !"".equals(proxy_list_url)) ? proxy_list_url : DEFAULT_SMART_PROXY_URL;
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

    public synchronized void refreshProxyList(String url_list) {
        if (url_list != null) {
            _proxy_list_url = url_list;
        } else {
            _proxy_list_url = null;
        }

        THREAD_POOL.execute(() -> {
            refreshProxyList();
        });
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
        } else if (lower.startsWith("https://")) {
            line = line.substring(8);
        } else if (lower.startsWith("socks5://")) {
            socks = true;
            line = line.substring(9);
        } else if (lower.startsWith("socks4a://")) {
            socks = true;
            line = line.substring(10);
        } else if (lower.startsWith("socks4://")) {
            socks = true;
            line = line.substring(9);
        } else if (lower.startsWith("socks://")) {
            socks = true;
            line = line.substring(8);
        }

        // Drop any trailing path/query the scheme may have brought along
        // (e.g. "http://1.2.3.4:8080/" or "http://host:port/list.txt").
        int slash = line.indexOf('/');
        if (slash >= 0) {
            line = line.substring(0, slash);
        }
        line = line.trim();

        if (line.contains("@")) {
            String[] proxy_parts = line.split("@");
            // Defensive: a stray '@' (e.g. user@pass@host:port) would expose
            // proxy_parts[1] as the wrong value; require exactly 2 parts AND
            // a valid host:port shape on the first half. (#751)
            if (proxy_parts.length == 2 && proxy_parts[0].matches(".+?:[0-9]{1,5}")) {
                into_auth.put(proxy_parts[0], proxy_parts[1]);
                into_list.put(proxy_parts[0], new Long[]{-1L, socks ? 1L : -1L});
            } else {
                LOG.log(Level.WARNING, "[Smart Proxy] skipping malformed {0} entry: {1}", new Object[]{source, raw});
            }
            return;
        }

        if (line.matches(".+?:[0-9]{1,5}")) {
            into_list.put(line, new Long[]{-1L, socks ? 1L : -1L});
        } else {
            LOG.log(Level.WARNING, "[Smart Proxy] skipping malformed {0} entry: {1}", new Object[]{source, raw});
        }
    }

    public synchronized void refreshProxyList() {

        HttpURLConnection con = null;

        try {

            String custom_proxy_list = (_proxy_list_url == null ? DBTools.selectSettingValue("custom_proxy_list") : null);

            LinkedHashMap<String, Long[]> custom_clean_list = new LinkedHashMap<>();

            HashMap<String, String> custom_clean_list_auth = new HashMap<>();

            if (custom_proxy_list != null) {

                ArrayList<String> custom_list = new ArrayList<>(Arrays.asList(custom_proxy_list.split("\\r?\\n")));

                for (String proxy : custom_list) {
                    parseProxyEntry(proxy, "custom", custom_clean_list, custom_clean_list_auth);
                }

                if (!custom_clean_list.isEmpty()) {

                    _proxy_list.clear();

                    _proxy_list.putAll(custom_clean_list);
                }

                if (!custom_clean_list_auth.isEmpty()) {

                    PROXY_LIST_AUTH.clear();

                    PROXY_LIST_AUTH.putAll(custom_clean_list_auth);
                }

            }

            if (custom_clean_list.isEmpty() && _proxy_list_url != null && !"".equals(_proxy_list_url)) {

                if (custom_proxy_list != null && !custom_proxy_list.trim().isEmpty()) {
                    // Custom list is non-empty but parsed to zero usable entries
                    // -- before falling back to the URL, surface that so the user
                    // knows their custom list is being ignored. (#751)
                    LOG.log(Level.WARNING, "[Smart Proxy] custom proxy list contained no parseable entries; falling back to URL fetch");
                }

                URL url = new URL(this._proxy_list_url);

                if (!"https".equalsIgnoreCase(url.getProtocol())) {
                    LOG.log(Level.WARNING, "Smart proxy list URL is not HTTPS ({0}); response is unauthenticated and could be MITM'd", url.toString());
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

                    // PRESERVE the previous proxy list. Was: data = "" + split
                    // + clear() -- a transient 4xx/5xx wiped the list silently,
                    // leaving workers in 509 recovery with zero proxies. The
                    // status label below makes the staleness visible. (#751)
                    LOG.log(Level.WARNING, "Smart proxy list fetch failed: HTTP {0} -- preserving previous list ({1} entries)",
                            new Object[]{http_status, _proxy_list.size()});

                    MiscTools.drainAndCloseErrorStream(con);

                    _main_panel.getView().updateSmartProxyStatus("SmartProxy: ON (" + String.valueOf(getProxyCount()) + " stale)" + (this.isForce_smart_proxy() ? " F!" : ""));

                    return;
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

                // Parse into a temporary map FIRST. Only swap into _proxy_list
                // if the new list has at least one usable entry. Was: clear()
                // + populate during parse, so an empty / garbage body wiped
                // the previous list before we knew whether the new one was
                // viable. (#751)
                LinkedHashMap<String, Long[]> new_list = new LinkedHashMap<>();
                HashMap<String, String> new_auth = new HashMap<>();

                for (String proxy : data.split("\n")) {
                    parseProxyEntry(proxy, "URL", new_list, new_auth);
                }

                if (new_list.isEmpty()) {

                    LOG.log(Level.WARNING, "Smart proxy list fetch returned 0 usable entries -- preserving previous list ({0} entries)",
                            _proxy_list.size());

                    _main_panel.getView().updateSmartProxyStatus("SmartProxy: ON (" + String.valueOf(getProxyCount()) + " stale)" + (this.isForce_smart_proxy() ? " F!" : ""));

                } else {

                    _proxy_list.clear();
                    _proxy_list.putAll(new_list);

                    PROXY_LIST_AUTH.clear();
                    PROXY_LIST_AUTH.putAll(new_auth);

                    _main_panel.getView().updateSmartProxyStatus("SmartProxy: ON (" + String.valueOf(getProxyCount()) + ")" + (this.isForce_smart_proxy() ? " F!" : ""));

                    LOG.log(Level.INFO, "{0} Smart Proxy Manager: proxy list refreshed ({1})", new Object[]{Thread.currentThread().getName(), _proxy_list.size()});
                }

            } else if (!custom_clean_list.isEmpty()) {

                _main_panel.getView().updateSmartProxyStatus("SmartProxy: ON (" + String.valueOf(getProxyCount()) + ")" + (this.isForce_smart_proxy() ? " F!" : ""));

                LOG.log(Level.INFO, "{0} Smart Proxy Manager: proxy list refreshed ({1})", new Object[]{Thread.currentThread().getName(), _proxy_list.size()});
            } else {
                _main_panel.getView().updateSmartProxyStatus("SmartProxy: ON (0 proxies!)" + (this.isForce_smart_proxy() ? " F!" : ""));
                LOG.log(Level.INFO, "{0} Smart Proxy Manager: NO PROXYS");
            }

        } catch (MalformedURLException ex) {
            LOG.log(Level.SEVERE, "Smart proxy list URL is malformed: {0}", ex.getMessage());
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Smart proxy list fetch threw -- preserving previous list ({0} entries): {1}",
                    new Object[]{_proxy_list.size(), ex.getMessage()});
            _main_panel.getView().updateSmartProxyStatus("SmartProxy: ON (" + String.valueOf(getProxyCount()) + " stale)" + (this.isForce_smart_proxy() ? " F!" : ""));
        } finally {
            if (con != null) {
                con.disconnect();
            }

        }

        _last_refresh_timestamp = System.currentTimeMillis();

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
