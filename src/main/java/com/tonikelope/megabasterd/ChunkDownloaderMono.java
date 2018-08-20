package com.tonikelope.megabasterd;

import java.io.IOException;
import java.io.InputStream;
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
public class ChunkDownloaderMono extends ChunkDownloader {

    public ChunkDownloaderMono(Download download) {
        super(1, download);
    }

    @Override
    public void run() {

        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Worker [{1}]: let''s do some work!", new Object[]{Thread.currentThread().getName(), getId()});

        HttpURLConnection con = null;

        try {

            String worker_url = null;
            int conta_error = 0, http_status = 200;
            boolean error = false, error403 = false;

            getDownload().getView().set509Error(false);

            InputStream is = null;

            while (!isExit() && !getDownload().isStopped()) {

                if (worker_url == null || error403) {

                    worker_url = getDownload().getDownloadUrlForWorker();
                }

                Chunk chunk = new Chunk(getDownload().nextChunkId(), getDownload().getFile_size(), null);

                try {

                    if (con == null || error) {

                        URL url = new URL(worker_url + "/" + chunk.getOffset());

                        if (MainPanel.isUse_proxy()) {

                            con = (HttpURLConnection) url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(MainPanel.getProxy_host(), MainPanel.getProxy_port())));

                            if (MainPanel.getProxy_user() != null && !"".equals(MainPanel.getProxy_user())) {

                                con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((MainPanel.getProxy_user() + ":" + MainPanel.getProxy_pass()).getBytes()));
                            }
                        } else {

                            con = (HttpURLConnection) url.openConnection();
                        }

                        con.setConnectTimeout(Download.HTTP_TIMEOUT);

                        con.setReadTimeout(Download.HTTP_TIMEOUT);

                        con.setRequestProperty("User-Agent", MainPanel.DEFAULT_USER_AGENT);

                        http_status = con.getResponseCode();

                        is = new ThrottledInputStream(con.getInputStream(), getDownload().getMain_panel().getStream_supervisor());
                    }

                    error = false;

                    error403 = false;

                    if (getDownload().isError509()) {
                        getDownload().getView().set509Error(false);
                    }

                    if (http_status != 200) {

                        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Failed : HTTP error code : {1}", new Object[]{Thread.currentThread().getName(), http_status});

                        error = true;

                        if (http_status == 509) {

                            getDownload().getView().set509Error(true);

                        } else if (http_status == 403) {

                            error403 = true;
                        }

                        getDownload().rejectChunkId(chunk.getId());

                        conta_error++;

                        if (!isExit() && !error403) {

                            setError_wait(true);

                            Thread.sleep(getWaitTimeExpBackOff(conta_error) * 1000);

                            setError_wait(false);
                        }

                    } else {

                        if (!isExit() && !getDownload().isStopped() && is != null) {

                            byte[] buffer = new byte[DEFAULT_BYTE_BUFFER_SIZE];

                            int reads;

                            while (!getDownload().isStopped() && !getDownload().getChunkwriter().isExit() && chunk.getOutputStream().size() < chunk.getSize() && (reads = is.read(buffer, 0, Math.min((int) (chunk.getSize() - chunk.getOutputStream().size()), buffer.length))) != -1) {
                                chunk.getOutputStream().write(buffer, 0, reads);

                                getDownload().getPartialProgressQueue().add(reads);

                                getDownload().getProgress_meter().secureNotify();

                                if (getDownload().isPaused() && !getDownload().isStopped()) {

                                    getDownload().pause_worker_mono();

                                    secureWait();

                                } else if (!getDownload().isPaused() && getDownload().getMain_panel().getDownload_manager().isPaused_all()) {

                                    getDownload().pause();

                                    getDownload().pause_worker_mono();

                                    secureWait();
                                }
                            }

                            if (chunk.getOutputStream().size() < chunk.getSize()) {

                                if (chunk.getOutputStream().size() > 0) {
                                    getDownload().getPartialProgressQueue().add(-1 * chunk.getOutputStream().size());

                                    getDownload().getProgress_meter().secureNotify();
                                }

                                error = true;
                            }

                            if (error && !getDownload().isStopped()) {

                                getDownload().rejectChunkId(chunk.getId());

                                conta_error++;

                                if (!isExit() && !error403) {

                                    setError_wait(true);

                                    Thread.sleep(getWaitTimeExpBackOff(conta_error) * 1000);

                                    setError_wait(false);
                                }

                            } else if (!error) {

                                getDownload().getChunkwriter().getChunk_queue().put(chunk.getId(), chunk);

                                getDownload().getChunkwriter().secureNotify();

                                conta_error = 0;
                            }

                        } else if (isExit()) {

                            getDownload().rejectChunkId(chunk.getId());
                        }

                    }

                } catch (IOException ex) {
                    error = true;

                    getDownload().rejectChunkId(chunk.getId());

                    if (chunk.getOutputStream().size() > 0) {
                        getDownload().getPartialProgressQueue().add(-1 * chunk.getOutputStream().size());

                        getDownload().getProgress_meter().secureNotify();
                    }

                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);

                } catch (InterruptedException ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                } finally {
                    if (error && con != null) {
                        con.disconnect();
                        con = null;
                    }
                }

            }

        } catch (ChunkInvalidException e) {

        } catch (OutOfMemoryError | Exception error) {
            getDownload().stopDownloader(error.getMessage());
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, error.getMessage());
        }

        getDownload().stopThisSlot(this);

        getDownload().getChunkwriter().secureNotify();

        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Worker [{1}]: bye bye", new Object[]{Thread.currentThread().getName(), getId()});

    }
}
