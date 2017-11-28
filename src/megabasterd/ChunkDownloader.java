package megabasterd;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static megabasterd.MainPanel.*;
import static megabasterd.MiscTools.*;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

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

    public ChunkDownloader(int id, Download download) {
        _notified = false;
        _exit = false;
        _secure_notify_lock = new Object();
        _id = id;
        _download = download;
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

        String worker_url = null;
        Chunk chunk;
        int reads, conta_error, http_status;
        InputStream is;
        boolean error;
        String current_proxy = null;
        CloseableHttpClient httpclient = null;

        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Worker [{1}]: let''s do some work!", new Object[]{Thread.currentThread().getName(), _id});

        try {

            conta_error = 0;

            error = false;

            while (!_exit && !_download.isStopped()) {

                if (_download.isUse_smart_proxy() && !MainPanel.isUse_smart_proxy()) {

                    _download.setUse_smart_proxy(false);
                }

                if (httpclient == null || error || (MainPanel.isUse_smart_proxy() && _download.isUse_smart_proxy())) {

                    if (error && !_download.isUse_smart_proxy()) {
                        _download.setUse_smart_proxy(true);
                    }

                    if (_download.isUse_smart_proxy() && !MainPanel.isUse_proxy()) {

                        if (error && current_proxy != null) {

                            Logger.getLogger(getClass().getName()).log(Level.WARNING, "{0} Worker [{1}]: excluding proxy -> {2}", new Object[]{Thread.currentThread().getName(), _id, current_proxy});

                            _download.getExcluded_proxies().add(current_proxy);
                        }

                        current_proxy = MainPanel.getProxy_manager().getRandomProxy(_download.getExcluded_proxies());

                        if (httpclient != null) {
                            try {
                                httpclient.close();
                            } catch (IOException ex) {
                                Logger.getLogger(ChunkDownloader.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }

                        httpclient = current_proxy != null ? MiscTools.getApacheKissHttpClientSmartProxy(current_proxy) : MiscTools.getApacheKissHttpClient();

                    } else if (httpclient == null) {

                        httpclient = MiscTools.getApacheKissHttpClient();
                    }
                }

                if (worker_url == null || error) {

                    worker_url = _download.getDownloadUrlForWorker();
                }

                chunk = new Chunk(_download.nextChunkId(), _download.getFile_size(), worker_url, Download.CHUNK_SIZE_MULTI);

                HttpGet httpget = new HttpGet(new URI(chunk.getUrl()));

                error = false;

                try (CloseableHttpResponse httpresponse = httpclient.execute(httpget)) {

                    if (!_exit && !_download.isStopped()) {

                        is = new ThrottledInputStream(httpresponse.getEntity().getContent(), _download.getMain_panel().getStream_supervisor());

                        http_status = httpresponse.getStatusLine().getStatusCode();

                        if (http_status != HttpStatus.SC_OK) {
                            Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Failed : HTTP error code : {1}", new Object[]{Thread.currentThread().getName(), http_status});

                            error = true;

                        } else {

                            byte[] buffer = new byte[THROTTLE_SLICE_SIZE];

                            while (!_exit && !_download.isStopped() && !_download.getChunkwriter().isExit() && chunk.getOutputStream().size() < chunk.getSize() && (reads = is.read(buffer)) != -1) {

                                chunk.getOutputStream().write(buffer, 0, reads);

                                _download.getPartialProgressQueue().add(reads);

                                _download.getProgress_meter().secureNotify();

                                if (_download.isPaused() && !_download.isStopped()) {

                                    _download.pause_worker();

                                    secureWait();
                                }
                            }

                            is.close();

                            if (chunk.getOutputStream().size() < chunk.getSize()) {

                                if (chunk.getOutputStream().size() > 0) {
                                    _download.getPartialProgressQueue().add(-1 * chunk.getOutputStream().size());

                                    _download.getProgress_meter().secureNotify();

                                }

                                error = true;
                            }
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

                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);

                } catch (InterruptedException ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);

                }
            }

        } catch (ChunkInvalidException e) {

        } catch (IOException ex) {
            _download.emergencyStopDownloader(ex.getMessage());
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(ChunkDownloader.class.getName()).log(Level.SEVERE, null, ex);
        } finally {

            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (IOException ex) {
                    Logger.getLogger(ChunkDownloader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        _download.stopThisSlot(this);

        _download.getChunkwriter().secureNotify();

        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Worker [{1}]: bye bye", new Object[]{Thread.currentThread().getName(), _id});
    }
}
