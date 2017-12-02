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
public class ChunkDownloaderMono extends ChunkDownloader {

    public ChunkDownloaderMono(Download download) {
        super(1, download);
    }

    @Override
    public void run() {

        String worker_url = null;
        Chunk chunk;
        int reads, conta_error, http_status = 200;
        boolean error;
        HttpGet httpget = null;
        CloseableHttpResponse httpresponse = null;

        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Worker [{1}]: let''s do some work!", new Object[]{Thread.currentThread().getName(), getId()});

        try (CloseableHttpClient httpclient = MiscTools.getApacheKissHttpClient()) {
            conta_error = 0;

            error = false;

            InputStream is = null;

            while (!isExit() && !getDownload().isStopped()) {

                if (worker_url == null || error) {

                    worker_url = getDownload().getDownloadUrlForWorker();

                    if (httpresponse != null) {

                        httpresponse.close();
                    }
                }

                chunk = new Chunk(getDownload().nextChunkId(), getDownload().getFile_size(), null);

                try {

                    if (httpget == null || error) {

                        httpget = new HttpGet(new URI(worker_url + "/" + chunk.getOffset()));

                        httpresponse = httpclient.execute(httpget);

                        is = new ThrottledInputStream(httpresponse.getEntity().getContent(), getDownload().getMain_panel().getStream_supervisor());

                        http_status = httpresponse.getStatusLine().getStatusCode();
                    }

                    error = false;

                    if (http_status != HttpStatus.SC_OK) {

                        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Failed : HTTP error code : {1}", new Object[]{Thread.currentThread().getName(), http_status});

                        error = true;

                        getDownload().rejectChunkId(chunk.getId());

                        conta_error++;

                        if (!isExit()) {

                            setError_wait(true);

                            Thread.sleep(getWaitTimeExpBackOff(conta_error) * 1000);

                            setError_wait(false);
                        }

                    } else {

                        if (!isExit() && !getDownload().isStopped() && is != null) {

                            byte[] buffer = new byte[THROTTLE_SLICE_SIZE];

                            while (!getDownload().isStopped() && !getDownload().getChunkwriter().isExit() && chunk.getOutputStream().size() < chunk.getSize() && (reads = is.read(buffer, 0, Math.min((int) (chunk.getSize() - chunk.getOutputStream().size()), buffer.length))) != -1) {
                                chunk.getOutputStream().write(buffer, 0, reads);

                                getDownload().getPartialProgressQueue().add(reads);

                                getDownload().getProgress_meter().secureNotify();

                                if (getDownload().isPaused() && !getDownload().isStopped()) {

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

        } catch (IOException ex) {

            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);

            getDownload().emergencyStopDownloader(ex.getMessage());

        } catch (URISyntaxException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(ChunkDownloaderMono.class.getName()).log(Level.SEVERE, null, ex);
        }

        getDownload().stopThisSlot(this);

        getDownload().getChunkwriter().secureNotify();

        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Worker [{1}]: bye bye", new Object[]{Thread.currentThread().getName(), getId()});

    }
}
