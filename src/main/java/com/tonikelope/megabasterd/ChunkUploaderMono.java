package com.tonikelope.megabasterd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import static com.tonikelope.megabasterd.MainPanel.*;
import static com.tonikelope.megabasterd.MiscTools.*;
import static com.tonikelope.megabasterd.CryptTools.*;
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

        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} ChunkUploaderMONO {1} hello! {2}", new Object[]{Thread.currentThread().getName(), getId(), getUpload().getFile_name()});

        try (CloseableHttpClient httpclient = getApacheKissHttpClient(); RandomAccessFile f = new RandomAccessFile(getUpload().getFile_name(), "r");) {

            String worker_url = getUpload().getUl_url();

            OutputStream out = null;

            FutureTask<CloseableHttpResponse> futureTask = null;

            int conta_error = 0, reads, http_status, tot_bytes_up = -1;

            boolean error = false;

            CloseableHttpResponse httpresponse = null;

            while (!isExit() && !getUpload().isStopped()) {

                Chunk chunk = new Chunk(getUpload().nextChunkId(), getUpload().getFile_size(), null);

                f.seek(chunk.getOffset());

                byte[] buffer = new byte[MainPanel.THROTTLE_SLICE_SIZE];

                do {

                    chunk.getOutputStream().write(buffer, 0, f.read(buffer, 0, Math.min((int) (chunk.getSize() - chunk.getOutputStream().size()), buffer.length)));

                } while (!isExit() && !getUpload().isStopped() && chunk.getOutputStream().size() < chunk.getSize());

                if (tot_bytes_up == -1 || error) {

                    final HttpPost httppost = new HttpPost(new URI(worker_url + "/" + chunk.getOffset()));

                    final long postdata_length = getUpload().getFile_size() - chunk.getOffset();

                    final PipedOutputStream pipeout = new PipedOutputStream();

                    final PipedInputStream pipein = new PipedInputStream(pipeout);

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

                        try (CipherInputStream cis = new CipherInputStream(chunk.getInputStream(), genCrypter("AES", "AES/CTR/NoPadding", getUpload().getByte_file_key(), forwardMEGALinkKeyIV(getUpload().getByte_file_iv(), chunk.getOffset())))) {

                            Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Uploading chunk {1} from worker {2}...", new Object[]{Thread.currentThread().getName(), chunk.getId(), getId()});

                            while (!isExit() && !getUpload().isStopped() && (reads = cis.read(buffer)) != -1 && out != null) {
                                out.write(buffer, 0, reads);

                                getUpload().getPartialProgress().add(reads);

                                getUpload().getProgress_meter().secureNotify();

                                tot_bytes_up += reads;

                                if (getUpload().isPaused() && !getUpload().isStopped()) {

                                    getUpload().pause_worker_mono();

                                    secureWait();

                                } else if (!getUpload().isPaused() && getUpload().getMain_panel().getUpload_manager().isPaused_all()) {

                                    getUpload().pause();

                                    getUpload().pause_worker_mono();

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

                                Logger.getLogger(getClass().getName()).log(Level.WARNING, "{0} Uploading chunk {1} FAILED!...", new Object[]{Thread.currentThread().getName(), chunk.getId()});

                                getUpload().rejectChunkId(chunk.getId());

                                conta_error++;

                                if (!isExit()) {

                                    setError_wait(true);

                                    Thread.sleep(getWaitTimeExpBackOff(conta_error) * 1000);

                                    setError_wait(false);
                                }

                            } else if (!error && chunk.getOffset() + tot_bytes_up < getUpload().getFile_size()) {

                                Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Worker {1} has uploaded chunk {2}", new Object[]{Thread.currentThread().getName(), getId(), chunk.getId()});

                                Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} {1} {2}", new Object[]{chunk.getOffset(), tot_bytes_up, getUpload().getFile_size()});

                                conta_error = 0;
                            }
                        }

                    } else if (isExit()) {

                        getUpload().rejectChunkId(chunk.getId());
                    }

                } catch (IOException ex) {

                    Logger.getLogger(getClass().getName()).log(Level.WARNING, "{0} Uploading chunk {1} FAILED!...", new Object[]{Thread.currentThread().getName(), chunk.getId()});

                    error = true;

                    getUpload().rejectChunkId(chunk.getId());

                    if (tot_bytes_up > 0) {

                        getUpload().getPartialProgress().add(-1 * tot_bytes_up);

                        getUpload().getProgress_meter().secureNotify();
                    }

                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);

                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | InterruptedException ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);

                }

                if (!error && chunk.getOffset() + tot_bytes_up == getUpload().getFile_size() && futureTask != null) {

                    Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Worker {1} has uploaded chunk {2}", new Object[]{Thread.currentThread().getName(), getId(), chunk.getId()});

                    Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} {1} {2}", new Object[]{chunk.getOffset(), tot_bytes_up, getUpload().getFile_size()});

                    Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} has finished uploading all chunks. Waiting for completion handle...", new Object[]{Thread.currentThread().getName()});

                    try {

                        httpresponse = futureTask.get();

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

                                        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Completion handle -> {1}", new Object[]{Thread.currentThread().getName(), response});

                                        getUpload().setCompletion_handle(response);
                                    }

                                } else {

                                    throw new IOException("UPLOAD FAILED! (Completion handle is empty)");
                                }
                            }
                        }

                    } catch (ExecutionException | InterruptedException | CancellationException exception) {

                        throw new IOException("UPLOAD FAILED! (Completion handle is empty)");

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

        } catch (ChunkInvalidException e) {

        } catch (Exception ex) {

            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }

        getUpload().stopThisSlot(this);

        getUpload().getMac_generator().secureNotify();

        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} ChunkUploaderMONO {1} bye bye...", new Object[]{Thread.currentThread().getName(), getId()});
    }

}
