package com.tonikelope.megabasterd;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static com.tonikelope.megabasterd.MiscTools.*;
import static com.tonikelope.megabasterd.MainPanel.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

/**
 *
 * @author tonikelope
 */
public class ChunkDownloader implements Runnable, SecureSingleThreadNotifiable {

    private final int _id;
    private final Download _download;
    private volatile boolean _exit;
    private final Object _secure_notify_lock;
    private volatile boolean _error_wait;
    private boolean _notified;
    private SmartMegaProxyManager _proxy_manager;

    public ChunkDownloader(int id, Download download) {
        _notified = false;
        _exit = false;
        _secure_notify_lock = new Object();
        _id = id;
        _download = download;
        _proxy_manager = null;
        _error_wait = false;

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
                    _secure_notify_lock.wait();
                } catch (InterruptedException ex) {
                    _exit = true;
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                }
            }

            _notified = false;
        }
    }

    @Override
    public void run() {

        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Worker [{1}]: let''s do some work!", new Object[]{Thread.currentThread().getName(), _id});

        HttpURLConnection con = null;

        try {

            int conta_error = 0;

            String worker_url = null, current_proxy = null;

            boolean error = false, error509 = false, error403 = false, error503 = false;

            while (!_exit && !_download.isStopped() && !error503) {

                if (worker_url == null || error403) {

                    worker_url = _download.getDownloadUrlForWorker();
                }

                Chunk chunk = new Chunk(_download.nextChunkId(), _download.getFile_size(), worker_url, Download.CHUNK_SIZE_MULTI);

                if (con == null || error) {

                    if (error509 && MainPanel.isUse_smart_proxy() && !MainPanel.isUse_proxy()) {

                        if (MainPanel.isUse_smart_proxy() && _proxy_manager == null) {

                            _proxy_manager = new SmartMegaProxyManager(null);

                        }

                        if (error && !error403 && current_proxy != null) {

                            _proxy_manager.blockProxy(current_proxy);
                            Logger.getLogger(MiscTools.class.getName()).log(Level.WARNING, "{0}: excluding proxy -> {1}", new Object[]{Thread.currentThread().getName(), current_proxy});

                        }

                        current_proxy = _proxy_manager.getFastestProxy();

                        if (current_proxy != null) {

                            String[] proxy_info = current_proxy.split(":");

                            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy_info[0], Integer.parseInt(proxy_info[1])));

                            URL url = new URL(chunk.getUrl());

                            con = (HttpURLConnection) url.openConnection(proxy);

                            getDownload().getMain_panel().getView().setSmartProxy(true);
                            getDownload().enableProxyTurboMode();

                        } else {

                            URL url = new URL(chunk.getUrl());

                            con = (HttpURLConnection) url.openConnection();

                            getDownload().getMain_panel().getView().setSmartProxy(false);
                        }

                    } else {

                        URL url = new URL(chunk.getUrl());

                        if (MainPanel.isUse_proxy()) {

                            con = (HttpURLConnection) url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(MainPanel.getProxy_host(), MainPanel.getProxy_port())));

                            if (MainPanel.getProxy_user() != null && !"".equals(MainPanel.getProxy_user())) {

                                con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((MainPanel.getProxy_user() + ":" + MainPanel.getProxy_pass()).getBytes()));
                            }
                        } else {

                            con = (HttpURLConnection) url.openConnection();
                        }

                        getDownload().getMain_panel().getView().setSmartProxy(false);
                    }

                }

                con.setConnectTimeout(Download.HTTP_TIMEOUT);

                con.setReadTimeout(Download.HTTP_TIMEOUT);

                con.setRequestProperty("User-Agent", MainPanel.DEFAULT_USER_AGENT);

                error = false;

                error403 = false;

                error509 = false;

                error503 = false;

                if (getDownload().isError509()) {
                    getDownload().getView().set509Error(false);
                }

                try {

                    if (!_exit && !_download.isStopped()) {

                        int http_status = con.getResponseCode();

                        if (http_status != 200) {

                            Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Failed : HTTP error code : {1}", new Object[]{Thread.currentThread().getName(), http_status});

                            error = true;

                            if (http_status == 509) {

                                error509 = true;

                                if (MainPanel.isUse_smart_proxy()) {
                                    getDownload().getView().set509Error(true);
                                }
                            } else if (http_status == 403) {

                                error403 = true;
                            } else if (http_status == 503) {

                                error503 = true;
                            }

                        } else {

                            try (InputStream is = new ThrottledInputStream(con.getInputStream(), _download.getMain_panel().getStream_supervisor())) {

                                byte[] buffer = new byte[DEFAULT_BYTE_BUFFER_SIZE];

                                int reads;

                                while (!_exit && !_download.isStopped() && !_download.getChunkwriter().isExit() && chunk.getOutputStream().size() < chunk.getSize() && (reads = is.read(buffer)) != -1) {

                                    chunk.getOutputStream().write(buffer, 0, reads);

                                    _download.getPartialProgressQueue().add(reads);

                                    _download.getProgress_meter().secureNotify();

                                    if (_download.isPaused() && !_download.isStopped()) {

                                        _download.pause_worker();

                                        secureWait();

                                    } else if (!_download.isPaused() && _download.getMain_panel().getDownload_manager().isPaused_all()) {

                                        _download.pause();

                                        _download.pause_worker();

                                        secureWait();
                                    }
                                }
                            }
                        }

                        if (chunk.getOutputStream().size() < chunk.getSize()) {

                            if (chunk.getOutputStream().size() > 0) {
                                _download.getPartialProgressQueue().add(-1 * chunk.getOutputStream().size());

                                _download.getProgress_meter().secureNotify();

                            }

                            error = true;
                        }

                        if (error && !_download.isStopped()) {

                            _download.rejectChunkId(chunk.getId());

                            conta_error++;

                            if (!_exit) {

                                _error_wait = true;

                                _download.getView().updateSlotsStatus();

                                if (!MainPanel.isUse_smart_proxy()) {
                                    Thread.sleep(getWaitTimeExpBackOff(conta_error) * 1000);
                                }

                                _error_wait = false;

                                _download.getView().updateSlotsStatus();
                            }

                        } else if (!error) {

                            Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Worker [{1}] has downloaded chunk [{2}]!", new Object[]{Thread.currentThread().getName(), _id, chunk.getId()});

                            _download.getChunkwriter().getChunk_queue().put(chunk.getId(), chunk);

                            _download.getChunkwriter().secureNotify();

                            conta_error = 0;
                        }

                    } else if (_exit) {

                        _download.rejectChunkId(chunk.getId());
                    }
                } catch (IOException ex) {

                    error = true;

                    _download.rejectChunkId(chunk.getId());

                    if (chunk.getOutputStream().size() > 0) {
                        _download.getPartialProgressQueue().add(-1 * chunk.getOutputStream().size());

                        _download.getProgress_meter().secureNotify();
                    }

                    if (!(ex instanceof SocketTimeoutException)) {
                        conta_error++;
                    }

                    if (!_exit) {

                        _error_wait = true;

                        _download.getView().updateSlotsStatus();

                        if (!MainPanel.isUse_smart_proxy()) {
                            Thread.sleep(getWaitTimeExpBackOff(conta_error) * 1000);
                        }

                        _error_wait = false;

                        _download.getView().updateSlotsStatus();
                    }

                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);

                } catch (InterruptedException ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);

                } finally {

                    if (con != null) {
                        con.disconnect();
                        con = null;
                    }
                }
            }

        } catch (ChunkInvalidException e) {

        } catch (Exception ex) {
            _download.stopDownloader(ex.getMessage());
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }

        _download.stopThisSlot(this);

        _download.getChunkwriter().secureNotify();

        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Worker [{1}]: bye bye", new Object[]{Thread.currentThread().getName(), _id});
    }
}
