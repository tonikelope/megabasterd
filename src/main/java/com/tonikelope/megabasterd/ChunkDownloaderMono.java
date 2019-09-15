package com.tonikelope.megabasterd;

import static com.tonikelope.megabasterd.CryptTools.forwardMEGALinkKeyIV;
import static com.tonikelope.megabasterd.CryptTools.genDecrypter;
import static com.tonikelope.megabasterd.CryptTools.initMEGALinkKey;
import static com.tonikelope.megabasterd.CryptTools.initMEGALinkKeyIV;
import static com.tonikelope.megabasterd.MainPanel.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.CipherInputStream;

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

        Thread.currentThread().setPriority(Math.max(Thread.currentThread().getPriority() - 1, Thread.MIN_PRIORITY));

        LOG.log(Level.INFO, "{0} Worker [{1}]: let''s do some work!", new Object[]{Thread.currentThread().getName(), getId()});

        HttpURLConnection con = null;

        try {

            String worker_url = null;
            int http_error = 0, http_status = 0;
            boolean chunk_error = false;
            long chunk_id, bytes_downloaded = getDownload().getProgress();
            byte[] byte_file_key = initMEGALinkKey(getDownload().getFile_key());
            byte[] byte_iv = initMEGALinkKeyIV(getDownload().getFile_key());
            byte[] buffer = new byte[DEFAULT_BYTE_BUFFER_SIZE];

            CipherInputStream cis = null;

            while (!getDownload().getMain_panel().isExit() && !isExit() && !getDownload().isStopped()) {

                if (worker_url == null || http_error == 403) {

                    worker_url = getDownload().getDownloadUrlForWorker();
                }

                chunk_id = getDownload().nextChunkId();

                long chunk_offset = ChunkManager.calculateChunkOffset(chunk_id, 1);

                long chunk_size = ChunkManager.calculateChunkSize(chunk_id, getDownload().getFile_size(), chunk_offset, 1);

                ChunkManager.checkChunkID(chunk_id, getDownload().getFile_size(), chunk_offset);

                long chunk_reads = 0;

                try {

                    if (con == null || chunk_error) {

                        URL url = new URL(worker_url + "/" + chunk_offset);

                        if (MainPanel.isUse_proxy()) {

                            con = (HttpURLConnection) url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(MainPanel.getProxy_host(), MainPanel.getProxy_port())));

                            if (MainPanel.getProxy_user() != null && !"".equals(MainPanel.getProxy_user())) {

                                con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((MainPanel.getProxy_user() + ":" + MainPanel.getProxy_pass()).getBytes("UTF-8")));
                            }
                        } else {

                            con = (HttpURLConnection) url.openConnection();
                        }

                        con.setConnectTimeout(Download.HTTP_TIMEOUT);

                        con.setReadTimeout(Download.HTTP_TIMEOUT);

                        con.setUseCaches(false);

                        con.setRequestProperty("User-Agent", MainPanel.DEFAULT_USER_AGENT);

                        http_status = con.getResponseCode();

                        cis = new CipherInputStream(new ThrottledInputStream(con.getInputStream(), getDownload().getMain_panel().getStream_supervisor()), genDecrypter("AES", "AES/CTR/NoPadding", byte_file_key, forwardMEGALinkKeyIV(byte_iv, bytes_downloaded)));

                    }

                    chunk_error = true;

                    http_error = 0;

                    if (http_status != 200) {

                        LOG.log(Level.INFO, "{0} Failed : HTTP error code : {1}", new Object[]{Thread.currentThread().getName(), http_status});

                        http_error = http_status;

                        getDownload().rejectChunkId(chunk_id);

                        if (!isExit() && http_error != 403) {

                            setError_wait(true);

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException excep) {

                            }

                            setError_wait(false);
                        }

                    } else {

                        if (!isExit() && !getDownload().isStopped() && cis != null) {

                            int reads = 0;

                            while (!getDownload().isStopped() && chunk_reads < chunk_size && (reads = cis.read(buffer, 0, Math.min((int) (chunk_size - chunk_reads), buffer.length))) != -1) {

                                getDownload().getOutput_stream().write(buffer, 0, reads);

                                chunk_reads += reads;

                                getDownload().getPartialProgress().add((long) reads);

                                getDownload().getProgress_meter().secureNotify();

                                if (getDownload().isPaused() && !getDownload().isStopped()) {

                                    getDownload().pause_worker_mono();

                                    secureWait();

                                }

                            }

                            if (chunk_reads == chunk_size) {

                                bytes_downloaded += chunk_reads;

                                chunk_error = false;

                                http_error = 0;

                            }
                        }

                    }

                } catch (IOException ex) {

                    LOG.log(Level.SEVERE, null, ex);

                } finally {

                    if (chunk_error) {

                        getDownload().rejectChunkId(chunk_id);

                        if (chunk_reads > 0) {
                            getDownload().getPartialProgress().add(-1 * chunk_reads);

                            getDownload().getProgress_meter().secureNotify();
                        }

                        if (!isExit() && !getDownload().isStopped() && http_error != 403) {

                            setError_wait(true);

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException excep) {
                            }

                            setError_wait(false);
                        }

                        if (con != null) {
                            con.disconnect();
                            con = null;
                        }

                    }

                }

            }

        } catch (ChunkInvalidException e) {

        } catch (OutOfMemoryError | Exception error) {
            getDownload().stopDownloader(error.getMessage());
            LOG.log(Level.SEVERE, null, error.getMessage());
        }

        getDownload().stopThisSlot(this);

        getDownload().secureNotify();

        LOG.log(Level.INFO, "{0} Worker [{1}]: bye bye", new Object[]{Thread.currentThread().getName(), getId()});

    }
    private static final Logger LOG = Logger.getLogger(ChunkDownloaderMono.class.getName());
}
