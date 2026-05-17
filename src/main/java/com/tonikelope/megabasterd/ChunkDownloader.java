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

import static com.tonikelope.megabasterd.MainPanel.*;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public class ChunkDownloader implements Runnable, SecureSingleThreadNotifiable {

    /**
     * Legacy hard-coded post-509 SmartProxy window. Now lives in
     * SmartMegaProxyManager as a configurable; this constant is kept as the
     * ultimate fallback when no proxy manager is wired (e.g. very early
     * startup) so the original semantics hold. (#751 / C4)
     */
    public static final int SMART_PROXY_RECHECK_509_TIME = SmartMegaProxyManager.RECHECK_509_WINDOW_DEFAULT;
    private static final Logger LOG = Logger.getLogger(ChunkDownloader.class.getName());

    /**
     * Debug toggle to simulate MEGA HTTP 509 (bandwidth quota) responses
     * without burning real quota. Enabled via the JVM property
     * {@code -Dmb.debug.force509=N}:
     * <ul>
     *   <li>{@code N > 0} forces the next N successful direct-IP chunk
     *       responses to be reported as 509 (then disarms automatically).</li>
     *   <li>{@code N = -1} forces every direct response forever until the
     *       JVM restarts.</li>
     *   <li>{@code N = 0} (the default, property absent) is a no-op.</li>
     * </ul>
     * Only direct connections are intercepted; SmartProxy-routed traffic
     * always sees the real MEGA response. That mirrors the "my IP is
     * already in 509 but the proxy IP isn't" scenario from issue #752 and
     * lets the SmartProxy recovery path be exercised end-to-end.
     */
    private static final java.util.concurrent.atomic.AtomicLong DEBUG_FORCE_509_REMAINING;

    static {
        long n = 0;
        String prop = System.getProperty("mb.debug.force509");
        if (prop != null) {
            try {
                n = Long.parseLong(prop.trim());
            } catch (NumberFormatException ignore) {
            }
        }
        DEBUG_FORCE_509_REMAINING = new java.util.concurrent.atomic.AtomicLong(n);
        if (n != 0) {
            LOG.log(Level.WARNING, "DEBUG: ChunkDownloader will force HTTP 509 on direct requests ({0}). DO NOT USE IN PRODUCTION.",
                    n < 0 ? "always-on" : ("first " + n + " responses"));
        }
    }

    private static boolean consumeForced509() {
        while (true) {
            long cur = DEBUG_FORCE_509_REMAINING.get();
            if (cur == 0) {
                return false;
            }
            if (cur < 0) {
                return true;
            }
            if (DEBUG_FORCE_509_REMAINING.compareAndSet(cur, cur - 1)) {
                if (cur == 1) {
                    LOG.log(Level.WARNING, "DEBUG: force-509 budget exhausted; subsequent direct responses will be real");
                }
                return true;
            }
        }
    }

    private final int _id;
    private final Download _download;
    private volatile boolean _exit;
    private final Object _secure_notify_lock;
    private volatile boolean _error_wait;
    private volatile boolean _chunk_exception;
    private volatile boolean _notified;
    private final ArrayList<String> _excluded_proxy_list;
    private volatile boolean _reset_current_chunk;
    private volatile InputStream _chunk_inputstream = null;

    /**
     * Last HTTP status code this worker saw on its most recent chunk attempt.
     * Exposed so Download (watchdog) and DownloadView (status label) can tell
     * "stalled because of 509 quota" from "stalled because of network".
     * Protected so the ChunkDownloaderMono subclass can clear it on chunk
     * success. (#751)
     */
    protected volatile int _last_http_error = 0;

    /**
     * True while this worker is in the exp-backoff sleep after a 509 (bandwidth
     * quota) specifically. Used by Download.run()'s watchdog to choose the
     * shorter quota-stall timeout instead of the generic 600 s, and by
     * DownloadView to show "Quota reached -- retrying in Xs". (#751)
     */
    protected volatile boolean _in_509_backoff = false;

    /**
     * Public IP captured the first time this worker hit a 509 during the
     * current burst. While in 509 backoff we periodically compare
     * MainPanel.getCachedPublicIp() against this value; a change means the user
     * activated a VPN / switched IP and we should cut the backoff short and
     * retry immediately rather than waiting out the full exp-backoff or the 10
     * min watchdog. Protected so mono can clear it on chunk success. (#751)
     */
    protected volatile String _ip_at_first_509 = null;

    /**
     * Seconds remaining in the current 509 backoff (decreases from wait_secs to
     * 0 inside the sleep loop). Used by DownloadView to render "retrying in Xs"
     * without having to peek into the loop. (#751)
     */
    protected volatile int _backoff_seconds_remaining = 0;

    /**
     * Wakeup flag for {@link #sleepWithIpAwareBackoff}. Set asynchronously by
     * {@link #wakeFromBackoff()} when SmartProxy is enabled at runtime: the
     * sleep loop polls this every 1 s slice and, if true, clears it and
     * breaks early so the outer worker loop can re-evaluate routing instead
     * of waiting out the exp-backoff. Volatile (single producer + single
     * consumer; no CAS needed). (#758)
     */
    private volatile boolean _wake_from_backoff = false;

    private String _current_smart_proxy;

    /**
     * Break this worker out of its current 509 / network exp-backoff sleep on
     * the next 1 s tick so it can re-evaluate routing. Called from
     * MainPanelView after the user enables SmartProxy at runtime -- otherwise
     * the worker would sleep for the full exp-backoff (which on retry 5+ runs
     * into minutes) before noticing the proxy pool is now available. (#758)
     */
    public void wakeFromBackoff() {
        _wake_from_backoff = true;
    }

    public void RESET_CURRENT_CHUNK() {

        // Cache to a local var to avoid TOCTOU: the worker thread can null
        // _chunk_inputstream between our null-check and our close(), which
        // would NPE since the catch only handles IOException.
        InputStream s = _chunk_inputstream;

        if (s != null) {

            this._reset_current_chunk = true;

            try {
                s.close();
            } catch (IOException ex) {
                Logger.getLogger(ChunkDownloader.class.getName()).log(Level.SEVERE, null, ex);
            }

            _chunk_inputstream = null;
        }
    }

    public ChunkDownloader(int id, Download download) {
        _notified = false;
        _exit = false;
        _chunk_exception = false;
        _secure_notify_lock = new Object();
        _id = id;
        _download = download;
        _current_smart_proxy = null;
        _excluded_proxy_list = new ArrayList<>();
        _error_wait = false;
        _reset_current_chunk = false;

    }

    public boolean isChunk_exception() {
        return _chunk_exception;
    }

    public String getCurrent_smart_proxy() {
        return _current_smart_proxy;
    }

    public void setExit(boolean exit) {
        _exit = exit;
    }

    public boolean isExit() {
        return _exit;
    }

    public Download getDownload() {
        return _download;
    }

    public int getId() {
        return _id;
    }

    public boolean isError_wait() {
        return _error_wait;
    }

    public void setError_wait(boolean error_wait) {
        _error_wait = error_wait;
    }

    public int getLast_http_error() {
        return _last_http_error;
    }

    public boolean isIn_509_backoff() {
        return _in_509_backoff;
    }

    public int getBackoff_seconds_remaining() {
        return _backoff_seconds_remaining;
    }

    public String getIp_at_first_509() {
        return _ip_at_first_509;
    }

    /**
     * Sleeps an exp-backoff window in 1 s slices, while: - capturing the public
     * IP the first time http_status == 509 in a burst, then re-checking every
     * 30 s and breaking out early if the IP changed (user activated a VPN); -
     * updating the slot status label each second so the "509 Xs" countdown is
     * live in the UI; - respecting MainPanel.isExit() so shutdown isn't blocked
     * for the full backoff window.
     *
     * Returns true if the sleep terminated early because the public IP changed
     * -- the caller should reset its conta_error counter so the next backoff
     * starts from scratch instead of resuming an exp curve that no longer
     * applies. Returns false on normal completion, shutdown, or interruption.
     *
     * Protected so ChunkDownloaderMono (which also wants the same VPN-aware
     * behaviour) can share the implementation. (#751)
     */
    protected boolean sleepWithIpAwareBackoff(int conta_error_count, int http_status) {

        _last_http_error = http_status;
        _in_509_backoff = (http_status == 509);

        // User-disable hook for the IP-change-aware retry. Default ON.
        // Niche use case is a NAT-behind-NAT setup where public-IP
        // detection mis-triggers. (#751 / C1)
        String auto_resume_setting = com.tonikelope.megabasterd.DBTools.selectSettingValue("auto_resume_ip_change");
        boolean ip_change_enabled = (auto_resume_setting == null || auto_resume_setting.equals("yes"));

        if (http_status == 509 && _ip_at_first_509 == null && ip_change_enabled) {
            String ip0 = MainPanel.getCachedPublicIp();
            if (ip0 != null) {
                _ip_at_first_509 = ip0;
                LOG.log(Level.INFO, "{0} Worker [{1}] 509 -- captured IP {2} for change detection",
                        new Object[]{Thread.currentThread().getName(), _id, ip0});
            }
        }

        _error_wait = true;
        _download.getView().updateSlotsStatus();

        long wait_secs = MiscTools.getWaitTimeExpBackOff(conta_error_count);
        _backoff_seconds_remaining = (int) wait_secs;

        boolean broke_for_ip_change = false;

        // Reset the wake flag at entry so a stale set() from a previous
        // backoff doesn't short-circuit this one before it has a chance to
        // serve a single iteration. (#758)
        _wake_from_backoff = false;

        for (long i = 0; i < wait_secs && !_exit && !_download.isStopped() && !_download.getMain_panel().isExit(); i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException exc) {
                Thread.currentThread().interrupt();
                break;
            }
            _backoff_seconds_remaining = (int) (wait_secs - i - 1);

            // External wakeup: SmartProxy was just enabled at runtime (or some
            // other state change worth re-evaluating before the exp-backoff
            // naturally expires). Break the sleep so the outer worker loop
            // can pick the new routing on its next iteration. Returns true so
            // the caller resets conta_error -- the wake is conceptually
            // equivalent to an IP-change break. (#758)
            if (_wake_from_backoff) {
                _wake_from_backoff = false;
                LOG.log(Level.INFO, "{0} Worker [{1}] external wakeup -- breaking exp-backoff and retrying immediately",
                        new Object[]{Thread.currentThread().getName(), _id});
                broke_for_ip_change = true;
                break;
            }

            if (_in_509_backoff || i % 5 == 4) {
                _download.getView().updateSlotsStatus();
            }

            if (_in_509_backoff && _ip_at_first_509 != null && i > 0 && (i + 1) % 30 == 0) {
                String now_ip = MainPanel.getCachedPublicIp();
                if (now_ip != null && !now_ip.equals(_ip_at_first_509)) {
                    LOG.log(Level.INFO, "{0} Worker [{1}] public IP changed {2} -> {3}; breaking 509 backoff and retrying immediately",
                            new Object[]{Thread.currentThread().getName(), _id, _ip_at_first_509, now_ip});
                    _ip_at_first_509 = null;
                    broke_for_ip_change = true;
                    break;
                }
            }
        }

        _backoff_seconds_remaining = 0;
        _in_509_backoff = false;
        _error_wait = false;
        _download.getView().updateSlotsStatus();

        return broke_for_ip_change;
    }

    @Override
    public void secureNotify() {
        synchronized (_secure_notify_lock) {

            _notified = true;

            _secure_notify_lock.notify();
        }
    }

    @Override
    public void secureWait() {

        synchronized (_secure_notify_lock) {
            while (!_notified) {

                try {
                    _secure_notify_lock.wait(1000);
                } catch (InterruptedException ex) {
                    _exit = true;
                    Thread.currentThread().interrupt();
                    LOG.log(Level.FINE, "secureWait interrupted");
                    return;
                }
            }

            _notified = false;
        }
    }

    @Override
    public void run() {

        LOG.log(Level.INFO, "{0} Worker [{1}]: let''s do some work! {2}", new Object[]{Thread.currentThread().getName(), _id, _download.getFile_name()});

        HttpURLConnection con;

        try {

            int http_error = 0, http_status = -1, conta_error = 0;

            boolean chunk_error = false, timeout = false, smart_proxy_socks = false;

            String worker_url = null;

            byte[] buffer = new byte[DEFAULT_BYTE_BUFFER_SIZE];

            // Refreshed each loop iteration below: capturing once was a stale
            // reference bug -- if SmartProxy was OFF at worker startup,
            // proxy_manager stayed null even after the user enabled SmartProxy
            // at runtime, and the routing path at line ~394 onwards would
            // either NPE on getProxy() / blockProxy() or never switch to the
            // proxy pool. Re-read every iteration so runtime toggles are
            // visible on the next chunk attempt. (#758)
            SmartMegaProxyManager proxy_manager = MainPanel.getProxy_manager();

            while (!_download.getMain_panel().isExit() && !_exit && !_download.isStopped()) {

                proxy_manager = MainPanel.getProxy_manager();

                if (_download.isPaused() && !_download.isStopped() && !_download.getChunkmanager().isExit()) {

                    _download.pause_worker();

                    secureWait();
                }

                long recheck_window_ms = (proxy_manager != null ? proxy_manager.getRecheck_509_window() : SMART_PROXY_RECHECK_509_TIME) * 1000L;

                // Stamp the SHARED 509 timestamp at Download level so every
                // worker of the same download switches to SmartProxy mode in
                // lockstep. Was per-worker (`_509_timestamp`) which meant 7
                // of 8 slots kept hammering MEGA direct after slot 1 had
                // already switched. (#751 / C4)
                long download_509_ts = _download.get509BurstTimestamp();
                if (http_error == 509 && (download_509_ts == -1 || download_509_ts + recheck_window_ms < System.currentTimeMillis())) {
                    _download.set509BurstTimestamp(System.currentTimeMillis());
                }

                if (worker_url == null || http_error == 403) {

                    worker_url = _download.getDownloadUrlForWorker();
                }

                long chunk_id = _download.nextChunkId();

                long chunk_offset = ChunkWriterManager.calculateChunkOffset(chunk_id, Download.CHUNK_SIZE_MULTI);

                long chunk_size = ChunkWriterManager.calculateChunkSize(chunk_id, _download.getFile_size(), chunk_offset, Download.CHUNK_SIZE_MULTI);

                ChunkWriterManager.checkChunkID(chunk_id, _download.getFile_size(), chunk_offset);

                String chunk_url = ChunkWriterManager.genChunkUrl(worker_url, _download.getFile_size(), chunk_offset, chunk_size);

                if (http_error == 509 && MainPanel.isRun_command()) {
                    MainPanel.run_external_command();
                } else if (http_error != 509 && MainPanel.LAST_EXTERNAL_COMMAND_TIMESTAMP != -1) {
                    MainPanel.LAST_EXTERNAL_COMMAND_TIMESTAMP = -1;
                }

                long dl_509_ts_check = _download.get509BurstTimestamp();
                // proxy_manager null-guard hoisted to the outer AND: previously
                // it lived only inside the FORCE branch of the inner OR, so a
                // 509 or post-509 recheck window could enter this block with
                // proxy_manager == null and NPE at proxy_manager.getProxy() /
                // blockProxy() further down. With the re-read at the top of
                // the while loop this should be rare, but the static-field
                // reads (isUse_smart_proxy + getProxy_manager) are NOT atomic
                // so there is still a microscopic window during runtime toggle.
                // Belt-and-braces. (#758)
                if (MainPanel.isUse_smart_proxy() && proxy_manager != null && (proxy_manager.isForce_smart_proxy() || _current_smart_proxy != null || http_error == 509 || (dl_509_ts_check != -1 && dl_509_ts_check + recheck_window_ms > System.currentTimeMillis())) && !MainPanel.isUse_proxy()) {

                    if (_current_smart_proxy != null && chunk_error) {

                        if (!timeout && http_error != 429) {
                            proxy_manager.blockProxy(_current_smart_proxy, timeout ? "TIMEOUT!" : "HTTP " + String.valueOf(http_error));
                        } else if (timeout) {
                            _excluded_proxy_list.add(_current_smart_proxy);
                            LOG.log(Level.WARNING, "{0} Worker [{1}] PROXY {2} TIMEOUT", new Object[]{Thread.currentThread().getName(), _id, _current_smart_proxy});
                        } else {
                            _excluded_proxy_list.add(_current_smart_proxy);
                            LOG.log(Level.WARNING, "{0} Worker [{1}] PROXY {2} TOO MANY CONNECTIONS", new Object[]{Thread.currentThread().getName(), _id, _current_smart_proxy});
                        }

                        // getProxy() returns null when every proxy is excluded
                        // or banned and the refresh-retry loop exhausted itself.
                        // Previously we indexed [0]/[1] unconditionally and NPE'd
                        // the worker -- exactly during a 509 storm, when smart
                        // proxy is supposed to bail us out. Null now means
                        // "fall back to direct for this chunk" (the path below
                        // already handles _current_smart_proxy == null). (#751)
                        String[] smart_proxy = proxy_manager.getProxy(_excluded_proxy_list);

                        if (smart_proxy != null) {
                            _current_smart_proxy = smart_proxy[0];
                            smart_proxy_socks = smart_proxy[1].equals("socks");
                        } else {
                            LOG.log(Level.WARNING, "{0} Worker [{1}] SmartProxy exhausted (every proxy excluded/banned) -- falling back to direct", new Object[]{Thread.currentThread().getName(), _id});
                            _current_smart_proxy = null;
                        }

                    } else if (_current_smart_proxy == null) {

                        String[] smart_proxy = proxy_manager.getProxy(_excluded_proxy_list);

                        if (smart_proxy != null) {
                            _current_smart_proxy = smart_proxy[0];
                            smart_proxy_socks = smart_proxy[1].equals("socks");
                        } else {
                            LOG.log(Level.WARNING, "{0} Worker [{1}] SmartProxy exhausted (no usable proxy) -- falling back to direct", new Object[]{Thread.currentThread().getName(), _id});
                            _current_smart_proxy = null;
                        }

                    }

                    if (_current_smart_proxy != null) {

                        if (!getDownload().isTurbo()) {
                            getDownload().enableTurboMode();
                        }

                        // Parse the proxy entry defensively. Was a raw
                        // Integer.parseInt(proxy_info[1]) which threw
                        // NumberFormatException -- killing the worker -- on any
                        // garbage entry that slipped through the regex (or any
                        // future format change in the remote-fetched list).
                        // Treat malformed entries as banned + skip to direct
                        // fallback for this chunk; getProxy() will avoid the
                        // bad entry next round. (#751)
                        String[] proxy_info = _current_smart_proxy.split(":");
                        int proxy_port = -1;
                        if (proxy_info.length == 2) {
                            try {
                                int p = Integer.parseInt(proxy_info[1]);
                                if (p >= 1 && p <= 65535) {
                                    proxy_port = p;
                                }
                            } catch (NumberFormatException ignore) {
                            }
                        }

                        if (proxy_port < 0) {
                            LOG.log(Level.WARNING, "{0} Worker [{1}] malformed smart proxy entry {2} -- banning + direct fallback",
                                    new Object[]{Thread.currentThread().getName(), _id, _current_smart_proxy});
                            proxy_manager.blockProxy(_current_smart_proxy, "Malformed entry");
                            _excluded_proxy_list.add(_current_smart_proxy);
                            _current_smart_proxy = null;
                            URL url = new URL(chunk_url);
                            con = (HttpURLConnection) url.openConnection();
                        } else {
                            Proxy proxy = new Proxy(smart_proxy_socks ? Proxy.Type.SOCKS : Proxy.Type.HTTP, new InetSocketAddress(proxy_info[0], proxy_port));
                            URL url = new URL(chunk_url);
                            con = (HttpURLConnection) url.openConnection(proxy);
                        }

                    } else {

                        LOG.log(Level.INFO, "{0} Worker [{1}] SmartProxy getProxy returned NULL! {2}", new Object[]{Thread.currentThread().getName(), _id, _download.getFile_name()});

                        URL url = new URL(chunk_url);

                        con = (HttpURLConnection) url.openConnection();
                    }

                } else {

                    URL url = new URL(chunk_url);

                    if (MainPanel.isUse_proxy()) {

                        _current_smart_proxy = null;

                        con = (HttpURLConnection) url.openConnection(new Proxy(smart_proxy_socks ? Proxy.Type.SOCKS : Proxy.Type.HTTP, new InetSocketAddress(MainPanel.getProxy_host(), MainPanel.getProxy_port())));

                        if (MainPanel.getProxy_user() != null && !"".equals(MainPanel.getProxy_user())) {

                            con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((MainPanel.getProxy_user() + ":" + MainPanel.getProxy_pass()).getBytes("UTF-8")));
                        }

                    } else {

                        con = (HttpURLConnection) url.openConnection();
                    }
                }

                if (_current_smart_proxy != null && proxy_manager != null) {
                    con.setConnectTimeout(proxy_manager.getProxy_timeout());
                    con.setReadTimeout(proxy_manager.getProxy_timeout() * 2);
                } else {
                    con.setConnectTimeout(Transference.HTTP_CONNECT_TIMEOUT);
                    con.setReadTimeout(Transference.HTTP_READ_TIMEOUT);
                }

                con.setUseCaches(false);

                con.setRequestProperty("User-Agent", MainPanel.DEFAULT_USER_AGENT);

                // Force a fresh TCP/TLS socket per chunk and explicitly opt
                // out of Java's KeepAliveCache reuse for this connection. With
                // N parallel ChunkDownloader threads to the same gfs host,
                // the cache can hand the same idle socket to two workers in
                // sequence; if either worker's previous response wasn't drained
                // exactly to the byte (e.g. the "recovered previous chunk"
                // path completes getResponseCode() but never touches
                // getInputStream()), the next request reads its response
                // through a socket that still has leftover bytes in either
                // the JDK buffer or the TLS layer. With Connection: close
                // the server tears down after each chunk and Java does not
                // pool the socket -- isolating each worker's HTTP exchange.
                // Closes the multi-slot intermittent corruption pattern in
                // #749 / #740 / #672 / #746.
                con.setRequestProperty("Connection", "close");

                long chunk_reads = 0;

                chunk_error = true;

                timeout = false;

                http_error = 0;

                File tmp_chunk_file = null, chunk_file = null;

                LOG.log(Level.INFO, "{0} Worker [{1}] is downloading chunk [{2}]! {3}", new Object[]{Thread.currentThread().getName(), _id, chunk_id, _download.getFile_name()});

                try {

                    if (!_exit && !_download.isStopped()) {

                        http_status = con.getResponseCode();

                        if (http_status == 200 && _current_smart_proxy == null && consumeForced509()) {
                            LOG.log(Level.WARNING, "{0} Worker [{1}] DEBUG: overriding HTTP 200 with synthetic 509 for chunk [{2}]",
                                    new Object[]{Thread.currentThread().getName(), _id, chunk_id});
                            http_status = 509;
                            try {
                                con.disconnect();
                            } catch (Exception ignore) {
                            }
                        }

                        if (http_status != 200) {

                            LOG.log(Level.INFO, "{0} Worker [{1}] Failed chunk download : HTTP error code : {2} {3}", new Object[]{Thread.currentThread().getName(), _id, http_status, _download.getFile_name()});

                            http_error = http_status;

                            MiscTools.drainAndCloseErrorStream(con);

                        } else {

                            chunk_file = new File(_download.getChunkmanager().getChunks_dir() + "/" + MiscTools.HashString("sha1", _download.getUrl()) + ".chunk" + chunk_id);

                            if (!chunk_file.exists() || chunk_file.length() != chunk_size) {

                                tmp_chunk_file = new File(_download.getChunkmanager().getChunks_dir() + "/" + MiscTools.HashString("sha1", _download.getUrl()) + ".chunk" + chunk_id + ".tmp");

                                _chunk_inputstream = new ThrottledInputStream(con.getInputStream(), _download.getMain_panel().getStream_supervisor());

                                // Stream directly to .chunk{N}.tmp with a small
                                // reusable buffer. Memory footprint is bounded
                                // by buffer.length (16 KiB) per slot regardless
                                // of chunk_size or slot count. The previous
                                // in-memory buffering of the full chunk turned
                                // out to be unnecessary once Connection: close
                                // (above) removed the socket-pool cross-talk
                                // between workers -- which was the real root
                                // cause of #749 / #740 / #672 / #746. The
                                // post-write fsync + size guards further down
                                // close the residual write-rename race.
                                FileOutputStream tmp_chunk_file_fos = new FileOutputStream(tmp_chunk_file);

                                try (OutputStream tmp_chunk_file_os = new BufferedOutputStream(tmp_chunk_file_fos)) {

                                    int reads = 0;

                                    if (!_exit && !_download.isStopped() && !_download.getChunkmanager().isExit()) {

                                        while (!_reset_current_chunk && !_exit && !_download.isStopped() && !_download.getChunkmanager().isExit() && chunk_reads < chunk_size && (reads = _chunk_inputstream.read(buffer, 0, Math.min((int) (chunk_size - chunk_reads), buffer.length))) != -1) {
                                            tmp_chunk_file_os.write(buffer, 0, reads);

                                            chunk_reads += reads;

                                            _download.getPartialProgress().add((long) reads);

                                            _download.getProgress_meter().secureNotify();

                                            if (!_reset_current_chunk && _download.isPaused() && !_exit && !_download.isStopped() && !_download.getChunkmanager().isExit() && chunk_reads < chunk_size) {

                                                _download.pause_worker();

                                                secureWait();

                                            }
                                        }

                                    }

                                    // Only fsync if we believe the chunk is
                                    // complete. Partial chunks are about to be
                                    // deleted by the failure path anyway, so
                                    // the fsync would just slow down retries.
                                    if (chunk_reads == chunk_size) {
                                        tmp_chunk_file_os.flush();
                                        tmp_chunk_file_fos.getFD().sync();
                                    }
                                }

                            } else {

                                LOG.log(Level.INFO, "{0} Worker [{1}] has RECOVERED PREVIOUS chunk [{2}]! {3}", new Object[]{Thread.currentThread().getName(), _id, chunk_id, _download.getFile_name()});

                                chunk_reads = chunk_size;

                                _download.getPartialProgress().add(chunk_size);

                                _download.getProgress_meter().secureNotify();
                            }

                        }

                        if (chunk_reads == chunk_size) {

                            LOG.log(Level.INFO, "{0} Worker [{1}] has OK DOWNLOADED (" + (_current_smart_proxy != null ? "smartproxy" : "direct") + ") chunk [{2}]! {3}", new Object[]{Thread.currentThread().getName(), _id, chunk_id, _download.getFile_name()});

                            boolean rename_failed = false;

                            // Verify the on-disk size of the tmp file actually
                            // matches what we counted in chunk_reads. close() of
                            // the BufferedOutputStream should have flushed all
                            // bytes, but if anything went sideways (disk full,
                            // truncated flush, AV interception, partial write
                            // returned by the OS), promoting this tmp file to
                            // .chunk{N} would feed ChunkWriterManager a short
                            // chunk, shifting every subsequent chunk's CTR-IV
                            // offset and breaking the file's CBC-MAC at the end.
                            // This is the producer-side guard that matches the
                            // consumer-side size check in ChunkWriterManager (#749).
                            if (tmp_chunk_file != null) {
                                long tmp_size = tmp_chunk_file.length();
                                if (tmp_size != chunk_size) {
                                    LOG.log(Level.WARNING, "{0} Worker [{1}] chunk [{2}] tmp file size mismatch: on-disk={3} expected={4} -- requeueing chunk",
                                            new Object[]{Thread.currentThread().getName(), _id, chunk_id, tmp_size, chunk_size});
                                    rename_failed = true;
                                }
                            }

                            if (!rename_failed && tmp_chunk_file != null && chunk_file != null && (!chunk_file.exists() || chunk_file.length() != chunk_size)) {

                                if (chunk_file.exists() && !chunk_file.delete()) {
                                    LOG.log(Level.WARNING, "{0} Worker [{1}] failed to delete pre-existing chunk file {2}",
                                            new Object[]{Thread.currentThread().getName(), _id, chunk_file});
                                }

                                if (!tmp_chunk_file.renameTo(chunk_file)) {
                                    // Rename can fail in Windows when an antivirus has the .tmp
                                    // open or the destination is locked. Without surfacing this,
                                    // ChunkWriterManager would wait forever for .chunk{N} and the
                                    // slot would freeze (see #706 / #684). Mark the chunk as
                                    // failed so it gets requeued.
                                    LOG.log(Level.WARNING, "{0} Worker [{1}] failed to rename {2} -> {3}; requeueing chunk",
                                            new Object[]{Thread.currentThread().getName(), _id, tmp_chunk_file, chunk_file});
                                    rename_failed = true;
                                } else if (chunk_file.length() != chunk_size) {
                                    // Post-rename sanity check. On NTFS rename
                                    // is supposed to be atomic and preserve the
                                    // source's content, but on network drives /
                                    // ReFS / sketchy mount points we've seen
                                    // size mismatches survive. If we trusted
                                    // this and notified the writer, CTR-IV
                                    // would drift exactly as if the producer
                                    // had silently truncated.
                                    LOG.log(Level.WARNING, "{0} Worker [{1}] chunk [{2}] post-rename size mismatch: on-disk={3} expected={4} -- requeueing chunk",
                                            new Object[]{Thread.currentThread().getName(), _id, chunk_id, chunk_file.length(), chunk_size});
                                    rename_failed = true;
                                    // Evict the bad file so the next attempt at
                                    // this chunk_id doesn't see it and either
                                    // accept it as "recovered previous chunk"
                                    // (size check at line ~316 would still trip,
                                    // but defence in depth) or trip our writer-
                                    // side size check.
                                    if (!chunk_file.delete()) {
                                        LOG.log(Level.WARNING, "{0} Worker [{1}] could not delete bad post-rename chunk file {2}",
                                                new Object[]{Thread.currentThread().getName(), _id, chunk_file});
                                    }
                                }
                            }

                            chunk_error = rename_failed;

                            conta_error = 0;

                            http_error = 0;

                            // Clear the 509-burst state captured by the
                            // IP-change-aware backoff so a future 509 starts
                            // a fresh detection window. (#751)
                            _ip_at_first_509 = null;
                            _last_http_error = 0;

                            if (_current_smart_proxy != null && _download.get509BurstTimestamp() != -1) {

                                // Reset the SHARED timestamp so the rest of
                                // the workers also exit "post-509 armed" mode
                                // on this download. (#751 / C4)
                                _download.reset509BurstTimestamp();

                                if (_download.isTurbo()) {
                                    _download.disableTurboMode();
                                }
                            }

                            _excluded_proxy_list.clear();

                            _download.getChunkmanager().secureNotify();
                        }
                    }

                } catch (IOException | IllegalStateException ex) {

                    if (ex instanceof SocketTimeoutException) {
                        timeout = true;
                        LOG.log(Level.WARNING, "{0} Worker [{1}] TIMEOUT downloading chunk [{2}]! {3}", new Object[]{Thread.currentThread().getName(), _id, chunk_id, _download.getFile_name()});

                    } else {
                        LOG.log(Level.SEVERE, ex.getMessage());
                    }

                } finally {

                    if (_chunk_inputstream != null) {

                        try {
                            _chunk_inputstream.close();
                        } catch (IOException ex) {
                            Logger.getLogger(ChunkDownloader.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        _chunk_inputstream = null;
                    }

                    if (chunk_error) {

                        LOG.log(Level.INFO, "{0} Worker [{1}] has FAILED downloading (" + (_current_smart_proxy != null ? "smartproxy" : "direct") + ") chunk [{2}]! {3}", new Object[]{Thread.currentThread().getName(), _id, chunk_id, _download.getFile_name()});

                        if (tmp_chunk_file != null && tmp_chunk_file.exists()) {
                            tmp_chunk_file.delete();
                        }

                        _download.rejectChunkId(chunk_id);

                        if (chunk_reads > 0) {
                            _download.getPartialProgress().add(-1 * chunk_reads);
                            _download.getProgress_meter().secureNotify();
                        }

                        // Apply exp backoff when there is no proxy-switch fallback.
                        // Previously 509 was excluded from this path, which left
                        // workers tight-looping on MEGA bandwidth-quota responses
                        // (no Thread.sleep at all) -- hammering MEGA, burning CPU,
                        // and making the app freeze on exit because the loop only
                        // checks main_panel.isExit() between full HTTP roundtrips.
                        // Now 509 also backs off (via the IP-change-aware helper
                        // shared with the mono path). (#751)
                        if (!_exit && !_download.isStopped() && !timeout && _current_smart_proxy == null && http_error != 403) {

                            boolean ip_changed = sleepWithIpAwareBackoff(++conta_error, http_error);
                            if (ip_changed) {
                                conta_error = 0;
                            }

                        } else if (http_error == 503 && _current_smart_proxy == null && !_download.isTurbo()) {
                            setExit(true);
                        }

                    } else if (proxy_manager != null && proxy_manager.isReset_slot_proxy()) {
                        _current_smart_proxy = null;
                    }

                    if (_reset_current_chunk) {
                        LOG.log(Level.WARNING, "Worker [{0}] FORCE RESET CHUNK [{1}]! {2}", new Object[]{_id, chunk_id, _download.getFile_name()});
                        _current_smart_proxy = null;
                        _reset_current_chunk = false;
                    }

                    con.disconnect();
                }
            }

        } catch (ChunkInvalidException e) {

            _chunk_exception = true;

        } catch (OutOfMemoryError | Exception error) {

            _download.stopDownloader(error.getMessage());

            LOG.log(Level.SEVERE, error.getMessage());

        }

        _download.stopThisSlot(this);

        _download.getChunkmanager().secureNotify();

        LOG.log(Level.INFO, "{0} ChunkDownloader [{1}] {2}: bye bye", new Object[]{Thread.currentThread().getName(), _id, _download.getFile_name()});
    }
}
