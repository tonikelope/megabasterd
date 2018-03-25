package com.tonikelope.megabasterd;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import static com.tonikelope.megabasterd.MiscTools.*;
import static com.tonikelope.megabasterd.MainPanel.*;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

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

        try (CloseableHttpClient httpclient = MiscTools.getApacheKissHttpClient()) {

            String worker_url = null;
            int conta_error = 0, http_status = 200;
            boolean error = false, error509 = false;
            HttpGet httpget = null;
            CloseableHttpResponse httpresponse = null;

            getDownload().getView().set509Error(false);

            InputStream is = null;

            while (!isExit() && !getDownload().isStopped() && (error509 || conta_error < MAX_SLOT_ERROR)) {

                if (worker_url == null || error) {

                    worker_url = getDownload().getDownloadUrlForWorker();

                    if (httpresponse != null) {

                        httpresponse.close();
                    }
                }

                Chunk chunk = new Chunk(getDownload().nextChunkId(), getDownload().getFile_size(), null);

                try {

                    if (httpget == null || error) {

                        httpget = new HttpGet(new URI(worker_url + "/" + chunk.getOffset()));

                        httpresponse = httpclient.execute(httpget);

                        is = new ThrottledInputStream(httpresponse.getEntity().getContent(), getDownload().getMain_panel().getStream_supervisor());

                        http_status = httpresponse.getStatusLine().getStatusCode();
                    }

                    error = false;

                    if (error509) {

                        getDownload().getView().set509Error(false);
                    }

                    error509 = false;

                    if (http_status != HttpStatus.SC_OK) {

                        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Failed : HTTP error code : {1}", new Object[]{Thread.currentThread().getName(), http_status});

                        error = true;

                        if (http_status == 509) {

                            error509 = true;

                            getDownload().getView().set509Error(true);
                        }

                        getDownload().rejectChunkId(chunk.getId());

                        conta_error++;

                        if (!isExit()) {

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

                                if (!isExit()) {

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
                }
            }

        } catch (ChunkInvalidException e) {

        } catch (Exception ex) {

            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            getDownload().stopDownloader(ex.getMessage());
        }

        getDownload().stopThisSlot(this);

        getDownload().getChunkwriter().secureNotify();

        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Worker [{1}]: bye bye", new Object[]{Thread.currentThread().getName(), getId()});

    }
}
