/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package megabasterd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import static megabasterd.ChunkUploader.FUTURE_TIMEOUT;
import static megabasterd.MainPanel.THREAD_POOL;
import static megabasterd.MiscTools.getWaitTimeExpBackOff;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 *
 * @author tonikelope
 */
public class ChunkUploaderMono extends ChunkUploader {

    public ChunkUploaderMono(Upload upload) {
        super(1, upload);
    }

    @Override
    public void run() {
        System.out.println("ChunkUploaderMONO " + getId() + " hello! " + getUpload().getFile_name());

        String worker_url = getUpload().getUl_url();
        Chunk chunk;
        int reads, to_read, conta_error, re, http_status, tot_bytes_up = -1;
        byte[] buffer = new byte[MainPanel.THROTTLE_SLICE_SIZE];
        boolean error = false;

        try (CloseableHttpClient httpclient = MiscTools.getApacheKissHttpClient(); RandomAccessFile random_file = new RandomAccessFile(getUpload().getFile_name(), "r");) {

            conta_error = 0;

            OutputStream out = null;

            FutureTask<CloseableHttpResponse> futureTask = null;

            while (!isExit() && !getUpload().isStopped()) {
                CloseableHttpResponse httpresponse = null;

                chunk = new Chunk(getUpload().nextChunkId(), getUpload().getFile_size(), null);

                random_file.seek(chunk.getOffset());

                do {
                    to_read = chunk.getSize() - chunk.getOutputStream().size() >= buffer.length ? buffer.length : (int) (chunk.getSize() - chunk.getOutputStream().size());

                    re = random_file.read(buffer, 0, to_read);

                    chunk.getOutputStream().write(buffer, 0, re);

                } while (!isExit() && !getUpload().isStopped() && chunk.getOutputStream().size() < chunk.getSize());

                if (tot_bytes_up == -1 || error) {

                    final HttpPost httppost = new HttpPost(new URI(worker_url + "/" + chunk.getOffset()));

                    final long postdata_length = getUpload().getFile_size() - chunk.getOffset();

                    final PipedInputStream pipein = new PipedInputStream();

                    final PipedOutputStream pipeout = new PipedOutputStream(pipein);

                    futureTask = new FutureTask<>(new Callable() {
                        @Override
                        public CloseableHttpResponse call() throws IOException {

                            httppost.setEntity(new InputStreamEntity(pipein, postdata_length));

                            return httpclient.execute(httppost);
                        }
                    });

                    THREAD_POOL.execute(futureTask);

                    out = new ThrottledOutputStream(pipeout, getUpload().getMain_panel().getStream_supervisor());

                }

                tot_bytes_up = 0;

                error = false;

                try {

                    if (!isExit() && !getUpload().isStopped()) {

                        try (CipherInputStream cis = new CipherInputStream(chunk.getInputStream(), CryptTools.genCrypter("AES", "AES/CTR/NoPadding", getUpload().getByte_file_key(), CryptTools.forwardMEGALinkKeyIV(getUpload().getByte_file_iv(), chunk.getOffset())))) {

                            System.out.println(" Subiendo chunk " + chunk.getId() + " desde worker " + getId() + "...");

                            while (!isExit() && !getUpload().isStopped() && tot_bytes_up < chunk.getSize() && (reads = cis.read(buffer)) != -1 && out != null) {
                                out.write(buffer, 0, reads);

                                getUpload().getPartialProgress().add(reads);

                                getUpload().getProgress_meter().secureNotify();

                                tot_bytes_up += reads;

                                if (getUpload().isPaused() && !getUpload().isStopped()) {

                                    getUpload().pause_worker();

                                    secureWait();
                                }
                            }
                        }

                        if (!getUpload().isStopped()) {

                            if (tot_bytes_up < chunk.getSize()) {
                                if (tot_bytes_up > 0) {

                                    getUpload().getPartialProgress().add(-1 * tot_bytes_up);

                                    getUpload().getProgress_meter().secureNotify();
                                }

                                error = true;
                            }

                            if (error && !getUpload().isStopped()) {

                                getUpload().rejectChunkId(chunk.getId());

                                conta_error++;

                                if (!isExit()) {

                                    setError_wait(true);

                                    Thread.sleep(getWaitTimeExpBackOff(conta_error) * 1000);

                                    setError_wait(false);
                                }

                            } else if (!error) {

                                System.out.println(" Worker " + getId() + " ha subido chunk " + chunk.getId());

                                System.out.println(chunk.getOffset() + " " + tot_bytes_up + " " + getUpload().getFile_size());

                                if (chunk.getOffset() + tot_bytes_up < getUpload().getFile_size()) {

                                    getUpload().getMac_generator().getChunk_queue().put(chunk.getId(), chunk);

                                    getUpload().getMac_generator().secureNotify();

                                    conta_error = 0;
                                }
                            }

                        }

                    } else if (isExit()) {

                        getUpload().rejectChunkId(chunk.getId());
                    }

                } catch (IOException ex) {
                    error = true;

                    getUpload().rejectChunkId(chunk.getId());

                    if (tot_bytes_up > 0) {

                        getUpload().getPartialProgress().add(-1 * tot_bytes_up);

                        getUpload().getProgress_meter().secureNotify();
                    }

                    getLogger(ChunkUploader.class.getName()).log(Level.SEVERE, null, ex);

                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | InterruptedException ex) {
                    getLogger(ChunkUploader.class.getName()).log(Level.SEVERE, null, ex);

                }

                if (!error && chunk.getOffset() + tot_bytes_up == getUpload().getFile_size() && futureTask != null) {

                    try {

                        httpresponse = futureTask.get(FUTURE_TIMEOUT, TimeUnit.SECONDS);

                        http_status = httpresponse.getStatusLine().getStatusCode();

                        if (http_status != HttpStatus.SC_OK) {
                            throw new IOException("UPLOAD FAILED! (HTTP STATUS: " + http_status + ")");

                        } else {

                            InputStream is = httpresponse.getEntity().getContent();

                            try (ByteArrayOutputStream byte_res = new ByteArrayOutputStream()) {

                                while ((reads = is.read(buffer)) != -1) {

                                    byte_res.write(buffer, 0, reads);
                                }

                                String response = new String(byte_res.toByteArray());

                                if (response.length() > 0) {

                                    if (MegaAPI.checkMEGAError(response) != 0) {
                                        throw new IOException("UPLOAD FAILED! (MEGA ERROR: " + MegaAPI.checkMEGAError(response) + ")");

                                    } else {

                                        System.out.println("Completion handle -> " + response);

                                        getUpload().setCompletion_handle(response);

                                        conta_error = 0;

                                        getUpload().getMac_generator().getChunk_queue().put(chunk.getId(), chunk);

                                        getUpload().getMac_generator().secureNotify();
                                    }
                                } else {

                                    throw new IOException("UPLOAD FAILED! (UPLOAD RESPONSE IS EMPTY)");
                                }
                            }
                        }

                    } catch (ExecutionException | InterruptedException | CancellationException | TimeoutException exception) {

                        if (exception instanceof TimeoutException) {
                            futureTask.cancel(true);
                        }

                        error = true;

                        getUpload().rejectChunkId(chunk.getId());

                        getUpload().getPartialProgress().add(-1 * tot_bytes_up);

                        getUpload().getProgress_meter().secureNotify();

                    } finally {

                        if (out != null) {

                            out.close();
                        }

                        if (httpresponse != null) {

                            httpresponse.close();
                        }
                    }

                } else if (error) {

                    if (out != null) {

                        out.close();
                    }

                    if (futureTask != null) {
                        futureTask.cancel(true);
                    }
                }
            }

        } catch (ChunkInvalidIdException e) {

        } catch (IOException ex) {

            getUpload().emergencyStopUploader(ex.getMessage());

            getLogger(ChunkUploader.class.getName()).log(Level.SEVERE, null, ex);

        } catch (URISyntaxException ex) {
            Logger.getLogger(ChunkUploaderMono.class.getName()).log(Level.SEVERE, null, ex);
        }

        getUpload().stopThisSlot(this);

        getUpload().getMac_generator().secureNotify();

        System.out.println("ChunkUploaderMONO " + getId() + " bye bye...");
    }

}
