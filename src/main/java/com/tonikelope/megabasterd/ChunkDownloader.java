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

        if (_chunk_inputstream != null) {

            this._reset_current_chunk = true;

            try {
                _chunk_inputstream.close();
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
                    LOG.log(Level.SEVERE, ex.getMessage());
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

                        proxy_manager.blockProxy(_current_smart_proxy, timeout ? "TIMEOUT!" : "HTTP " + String.valueOf(http_error));

                        String[] smart_proxy = proxy_manager.getProxy(_excluded_proxy_list);

                        _current_smart_proxy = smart_proxy[0];

                        smart_proxy_socks = smart_proxy[1].equals("socks");

                    } else if (_current_smart_proxy == null) {

                        if (!getDownload().isTurbo()) {
                            getDownload().enableTurboMode();
                        }

                        String[] smart_proxy = proxy_manager.getProxy(_excluded_proxy_list);

                        _current_smart_proxy = smart_proxy[0];

                        smart_proxy_socks = smart_proxy[1].equals("socks");

                    }

                    if (_current_smart_proxy != null) {

                        String[] proxy_info = _current_smart_proxy.split(":");

                        Proxy proxy = new Proxy(smart_proxy_socks ? Proxy.Type.SOCKS : Proxy.Type.HTTP, new InetSocketAddress(proxy_info[0], Integer.parseInt(proxy_info[1])));

                        URL url = new URL(chunk_url);

                        con = (HttpURLConnection) url.openConnection(proxy);

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

                long chunk_reads = 0;

                chunk_error = true;

                timeout = false;

                File tmp_chunk_file = null, chunk_file = null;

                LOG.log(Level.INFO, "{0} Worker [{1}] is downloading chunk [{2}]! {3}", new Object[]{Thread.currentThread().getName(), _id, chunk_id, _download.getFile_name()});

                try {

                    if (!_exit && !_download.isStopped()) {

                        http_status = con.getResponseCode();

                        if (http_status != 200) {

                            LOG.log(Level.INFO, "{0} Worker [{1}] Failed chunk download : HTTP error code : {2} {3}", new Object[]{Thread.currentThread().getName(), _id, http_status, _download.getFile_name()});

                            http_error = http_status;

                        } else {

                            chunk_file = new File(_download.getChunkmanager().getChunks_dir() + "/" + MiscTools.HashString("sha1", _download.getUrl()) + ".chunk" + chunk_id);

                            if (!chunk_file.exists() || chunk_file.length() != chunk_size) {

                                tmp_chunk_file = new File(_download.getChunkmanager().getChunks_dir() + "/" + MiscTools.HashString("sha1", _download.getUrl()) + ".chunk" + chunk_id + ".tmp");

                                _chunk_inputstream = new ThrottledInputStream(con.getInputStream(), _download.getMain_panel().getStream_supervisor());

                                try (OutputStream tmp_chunk_file_os = new BufferedOutputStream(new FileOutputStream(tmp_chunk_file))) {

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

                            if (tmp_chunk_file != null && chunk_file != null && (!chunk_file.exists() || chunk_file.length() != chunk_size)) {

                                if (chunk_file.exists()) {
                                    chunk_file.delete();
                                }

                                tmp_chunk_file.renameTo(chunk_file);
                            }

                            chunk_error = false;

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

                        if (!_exit && !_download.isStopped() && !timeout && _current_smart_proxy == null && http_error != 509 && http_error != 403 && http_error != 503) {

                            _error_wait = true;

                            _download.getView().updateSlotsStatus();

                            try {
                                Thread.sleep(MiscTools.getWaitTimeExpBackOff(++conta_error) * 1000);
                            } catch (InterruptedException exc) {
                            }

                            _error_wait = false;

                            _download.getView().updateSlotsStatus();

                        } else if (http_error == 503 && _current_smart_proxy == null && !_download.isTurbo()) {
                            setExit(true);
                        }

                    } else {
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
