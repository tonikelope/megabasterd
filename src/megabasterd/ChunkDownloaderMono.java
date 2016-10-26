package megabasterd;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import static megabasterd.MainPanel.THROTTLE_SLICE_SIZE;
import static megabasterd.MiscTools.getWaitTimeExpBackOff;
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
        int reads, max_reads, conta_error, http_status = 200;
        byte[] buffer = new byte[THROTTLE_SLICE_SIZE];
        boolean error;
        HttpGet httpget = null;
        CloseableHttpResponse httpresponse = null;

        System.out.println("Worker [" + getId() + "]: let's do some work!");

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

                if (httpget == null || error) {

                    httpget = new HttpGet(new URI(worker_url + "/" + chunk.getOffset()));

                    httpget.addHeader("Connection", "close");

                    httpresponse = httpclient.execute(httpget);

                    is = new ThrottledInputStream(httpresponse.getEntity().getContent(), getDownload().getMain_panel().getStream_supervisor());

                    http_status = httpresponse.getStatusLine().getStatusCode();
                }

                error = false;

                if (http_status != HttpStatus.SC_OK) {

                    System.out.println("Failed : HTTP error code : " + http_status);

                    error = true;

                } else {

                    try {

                        if (!isExit() && !getDownload().isStopped() && is != null) {

                            while (!getDownload().isStopped() && !getDownload().getChunkwriter().isExit() && chunk.getOutputStream().size() < chunk.getSize() && (reads = is.read(buffer, 0, (max_reads = (int) (chunk.getSize() - chunk.getOutputStream().size())) <= buffer.length ? max_reads : buffer.length)) != -1) {
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
                    } catch (IOException ex) {
                        error = true;

                        getDownload().rejectChunkId(chunk.getId());

                        if (chunk.getOutputStream().size() > 0) {
                            getDownload().getPartialProgressQueue().add(-1 * chunk.getOutputStream().size());

                            getDownload().getProgress_meter().secureNotify();
                        }

                        getLogger(ChunkDownloaderMono.class.getName()).log(Level.SEVERE, null, ex);

                    } catch (InterruptedException ex) {
                        getLogger(ChunkDownloaderMono.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

        } catch (ChunkInvalidIdException e) {

        } catch (IOException ex) {

            getLogger(ChunkDownloaderMono.class.getName()).log(Level.SEVERE, null, ex);

            getDownload().emergencyStopDownloader(ex.getMessage());

        } catch (URISyntaxException ex) {
            Logger.getLogger(ChunkDownloaderMono.class.getName()).log(Level.SEVERE, null, ex);
        }

        getDownload().stopThisSlot(this);

        getDownload().getChunkwriter().secureNotify();

        System.out.println("Worker [" + getId() + "]: bye bye");

    }
}
