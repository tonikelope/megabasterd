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

    public static final int SMART_PROXY_RECHECK_509_TIME = 3600;
    private static final Logger LOG = Logger.getLogger(ChunkDownloader.class.getName());
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
    private volatile long _509_timestamp = -1;

    private String _current_smart_proxy;

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

            SmartMegaProxyManager proxy_manager = MainPanel.getProxy_manager();

            while (!_download.getMain_panel().isExit() && !_exit && !_download.isStopped()) {

                if (_download.isPaused() && !_download.isStopped() && !_download.getChunkmanager().isExit()) {

                    _download.pause_worker();

                    secureWait();
                }

                if (http_error == 509 && (_509_timestamp == -1 || _509_timestamp + SMART_PROXY_RECHECK_509_TIME * 1000 < System.currentTimeMillis())) {
                    _509_timestamp = System.currentTimeMillis();
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

                if (MainPanel.isUse_smart_proxy() && ((proxy_manager != null && proxy_manager.isForce_smart_proxy()) || _current_smart_proxy != null || http_error == 509 || (_509_timestamp != -1 && _509_timestamp + SMART_PROXY_RECHECK_509_TIME * 1000 > System.currentTimeMillis())) && !MainPanel.isUse_proxy()) {

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

                            if (_current_smart_proxy != null && _509_timestamp != -1) {

                                _509_timestamp = -1;

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
                        // Now 509 also backs off (with smart_proxy off it is the
                        // only place the worker can yield). Sleeping in 1s slices
                        // lets the worker notice main_panel.isExit() within ~1s
                        // during shutdown instead of waiting out the full backoff. (#751)
                        if (!_exit && !_download.isStopped() && !timeout && _current_smart_proxy == null && http_error != 403) {

                            _error_wait = true;

                            _download.getView().updateSlotsStatus();

                            long wait_secs = MiscTools.getWaitTimeExpBackOff(++conta_error);
                            for (long i = 0; i < wait_secs && !_exit && !_download.isStopped() && !_download.getMain_panel().isExit(); i++) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException exc) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                            }

                            _error_wait = false;

                            _download.getView().updateSlotsStatus();

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
