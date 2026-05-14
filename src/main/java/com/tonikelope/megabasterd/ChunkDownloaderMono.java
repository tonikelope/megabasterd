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

import static com.tonikelope.megabasterd.CryptTools.forwardMEGALinkKeyIV;
import static com.tonikelope.megabasterd.CryptTools.genDecrypter;
import static com.tonikelope.megabasterd.CryptTools.initMEGALinkKey;
import static com.tonikelope.megabasterd.CryptTools.initMEGALinkKeyIV;
import static com.tonikelope.megabasterd.MainPanel.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.CipherInputStream;

/**
 *
 * @author tonikelope
 */
public class ChunkDownloaderMono extends ChunkDownloader {

    private static final Logger LOG = Logger.getLogger(ChunkDownloaderMono.class.getName());

    public static final int READ_TIMEOUT_RETRY = 3;

    /**
     * Safety cap for the in-memory chunk buffer. mono-slot chunks are
     * <= 1 MiB by design (calculateChunkSize uses size_multi=1) so any
     * value above a few MiB is essentially unreachable. We keep the cap
     * generous to leave headroom for future tweaks while still refusing
     * pathological sizes that would OOM the JVM.
     */
    private static final int MAX_CHUNK_BUFFER_BYTES = 8 * 1024 * 1024;

    public ChunkDownloaderMono(Download download) {
        super(1, download);
    }

    @Override
    public void run() {

        LOG.log(Level.INFO, "{0} Worker [{1}]: let''s do some work! {2}", new Object[]{Thread.currentThread().getName(), getId(), getDownload().getFile_name()});

        HttpURLConnection con = null;

        try {

            String worker_url = null;
            int http_error = 0, http_status = 0, conta_error = 0;
            boolean chunk_error = false, timeout = false;
            long chunk_id, bytes_downloaded = getDownload().getProgress();
            byte[] byte_file_key = initMEGALinkKey(getDownload().getFile_key());
            byte[] byte_iv = initMEGALinkKeyIV(getDownload().getFile_key());

            CipherInputStream cis = null;

            while (!getDownload().getMain_panel().isExit() && !isExit() && !getDownload().isStopped()) {

                if (worker_url == null || http_error == 403) {

                    worker_url = getDownload().getDownloadUrlForWorker();
                }

                chunk_id = getDownload().nextChunkId();

                long chunk_offset = ChunkWriterManager.calculateChunkOffset(chunk_id, 1);

                long chunk_size = ChunkWriterManager.calculateChunkSize(chunk_id, getDownload().getFile_size(), chunk_offset, 1);

                ChunkWriterManager.checkChunkID(chunk_id, getDownload().getFile_size(), chunk_offset);

                long chunk_reads = 0;

                try {

                    if (con == null || chunk_error) {

                        if (http_error == 509 && MainPanel.isRun_command()) {

                            MainPanel.run_external_command();

                        }

                        URL url = new URL(worker_url + "/" + chunk_offset);

                        if (MainPanel.isUse_proxy()) {

                            con = (HttpURLConnection) url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(MainPanel.getProxy_host(), MainPanel.getProxy_port())));

                            if (MainPanel.getProxy_user() != null && !"".equals(MainPanel.getProxy_user())) {

                                con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((MainPanel.getProxy_user() + ":" + MainPanel.getProxy_pass()).getBytes("UTF-8")));
                            }
                        } else {

                            con = (HttpURLConnection) url.openConnection();
                        }

                        con.setUseCaches(false);

                        con.setRequestProperty("User-Agent", MainPanel.DEFAULT_USER_AGENT);

                        http_status = con.getResponseCode();

                        cis = new CipherInputStream(new ThrottledInputStream(con.getInputStream(), getDownload().getMain_panel().getStream_supervisor()), genDecrypter("AES", "AES/CTR/NoPadding", byte_file_key, forwardMEGALinkKeyIV(byte_iv, bytes_downloaded)));

                    }

                    chunk_error = true;

                    timeout = false;

                    http_error = 0;

                    if (http_status != 200) {

                        LOG.log(Level.INFO, "{0} Failed : HTTP error code : {1} {2}", new Object[]{Thread.currentThread().getName(), http_status, getDownload().getFile_name()});

                        http_error = http_status;

                        MiscTools.drainAndCloseErrorStream(con);

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

                            // Buffer the whole chunk in memory before touching the
                            // output_stream. Writing directly while reading meant
                            // that a mid-chunk network failure left partial bytes
                            // in the final file -- the retry then wrote correct
                            // bytes one offset later and every subsequent chunk
                            // landed shifted, corrupting the file (the
                            // _file.length() != _file_size check in Download.java
                            // catches it as a visible I/O ERROR, but the file is
                            // still trashed). Buffering means a failed chunk is
                            // simply discarded; nothing reaches the output file
                            // until chunk_reads == chunk_size.
                            if (chunk_size > MAX_CHUNK_BUFFER_BYTES) {
                                LOG.log(Level.SEVERE, "Mono chunk {0} size {1} exceeds buffer cap {2} -- aborting",
                                        new Object[]{chunk_id, chunk_size, MAX_CHUNK_BUFFER_BYTES});
                                throw new IOException("Mono chunk size exceeds in-memory buffer cap");
                            }

                            byte[] chunk_buffer = new byte[(int) chunk_size];
                            int reads = 0;

                            while (!getDownload().isStopped() && chunk_reads < chunk_size
                                    && (reads = cis.read(chunk_buffer, (int) chunk_reads, (int) (chunk_size - chunk_reads))) != -1) {

                                chunk_reads += reads;

                                getDownload().getPartialProgress().add((long) reads);

                                getDownload().getProgress_meter().secureNotify();

                                if (getDownload().isPaused() && !getDownload().isStopped() && chunk_reads < chunk_size) {

                                    getDownload().pause_worker_mono();

                                    secureWait();

                                }
                            }

                            if (chunk_reads == chunk_size) {

                                // Atomic commit to the output stream: either the
                                // whole chunk lands or none of it does.
                                getDownload().getOutput_stream().write(chunk_buffer, 0, (int) chunk_size);

                                bytes_downloaded += chunk_reads;

                                chunk_error = false;

                                http_error = 0;

                                conta_error = 0;

                            }
                        }

                    }

                } catch (IOException ex) {

                    if (ex instanceof SocketTimeoutException) {
                        timeout = true;
                        LOG.log(Level.SEVERE, "{0} TIMEOUT downloading chunk {1}", new Object[]{Thread.currentThread().getName(), chunk_id});
                    } else {
                        LOG.log(Level.SEVERE, ex.getMessage());
                    }

                } finally {

                    if (chunk_error) {

                        getDownload().rejectChunkId(chunk_id);

                        if (chunk_reads > 0) {
                            getDownload().getPartialProgress().add(-1 * chunk_reads);

                            getDownload().getProgress_meter().secureNotify();
                        }

                        if (!isExit() && !getDownload().isStopped() && !timeout && http_error != 403) {

                            setError_wait(true);

                            try {
                                Thread.sleep(MiscTools.getWaitTimeExpBackOff(++conta_error) * 1000);
                            } catch (InterruptedException exc) {
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
            LOG.log(Level.SEVERE, error.getMessage());
        }

        getDownload().stopThisSlot(this);

        getDownload().secureNotify();

        LOG.log(Level.INFO, "{0} ChunkDownloaderMONO {1}: bye bye", new Object[]{Thread.currentThread().getName(), getDownload().getFile_name()});

    }
}
