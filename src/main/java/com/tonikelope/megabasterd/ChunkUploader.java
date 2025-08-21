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

import org.apache.commons.io.input.QueueInputStream;
import org.apache.commons.io.output.QueueOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.CipherInputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;

import static com.tonikelope.megabasterd.CryptTools.forwardMEGALinkKeyIV;
import static com.tonikelope.megabasterd.CryptTools.genCrypter;

/**
 *
 * @author tonikelope
 */
public class ChunkUploader implements Runnable, SecureSingleThreadNotifiable {

    private static final Logger LOG = LogManager.getLogger(ChunkUploader.class);

    public static final int MAX_CHUNK_ERROR = 50;
    private final int _id;
    private final Upload _upload;
    private volatile boolean _exit;
    private final Object _secure_notify_lock;
    private volatile boolean _error_wait;
    private volatile boolean _notified;
    private volatile boolean _chunk_exception;

    public ChunkUploader(int id, Upload upload) {
        _notified = false;
        _secure_notify_lock = new Object();
        _id = id;
        _upload = upload;
        _chunk_exception = false;
        _exit = false;
        _error_wait = false;
    }

    public boolean isChunk_exception() {
        return _chunk_exception;
    }

    public void setExit(boolean exit) {
        _exit = exit;
    }

    public boolean isError_wait() {
        return _error_wait;
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
                    _secure_notify_lock.wait(1000);
                } catch (InterruptedException ex) {
                    _exit = true;
                    LOG.fatal("Sleep interrupted! {}", ex.getMessage());
                }
            }

            _notified = false;
        }
    }

    public int getId() {
        return _id;
    }

    public boolean isExit() {
        return _exit;
    }

    public Upload getUpload() {
        return _upload;
    }

    public void setError_wait(boolean error_wait) {
        _error_wait = error_wait;
    }

    @Override
    public void run() {

        LOG.info("ChunkUploader {} hello! {}", getId(), _upload.getFile_name());

        long chunk_id;
        byte[] buffer = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];
        byte[] buffer_enc = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];
        boolean fatal_error = false;
        int errorCount = 0;

        try {

            while (!_upload.getMain_panel().isExit() && !_exit && !_upload.isStopped() && errorCount < MAX_CHUNK_ERROR && !fatal_error) {

                if (_upload.isPaused() && !_upload.isStopped()) {
                    _upload.pause_worker();
                    secureWait();
                }

                String worker_url = _upload.getUl_url();

                int reads = 0, http_status;

                long tot_bytes_up;

                chunk_id = _upload.nextChunkId();

                long chunk_offset = ChunkWriterManager.calculateChunkOffset(chunk_id, 1);

                long chunk_size = ChunkWriterManager.calculateChunkSize(chunk_id, _upload.getFile_size(), chunk_offset, 1);

                ChunkWriterManager.checkChunkID(chunk_id, _upload.getFile_size(), chunk_offset);

                String chunk_url = ChunkWriterManager.genChunkUrl(worker_url, _upload.getFile_size(), chunk_offset, chunk_size);

                URL url = new URL(chunk_url);

                HttpURLConnection con;

                if (MainPanel.isUse_proxy()) {

                    con = (HttpURLConnection) url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(MainPanel.getProxy_host(), MainPanel.getProxy_port())));

                    if (MainPanel.getProxy_user() != null && !MainPanel.getProxy_user().isEmpty()) {

                        con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((MainPanel.getProxy_user() + ":" + MainPanel.getProxy_pass()).getBytes(StandardCharsets.UTF_8)));
                    }
                } else {

                    con = (HttpURLConnection) url.openConnection();
                }

                con.setConnectTimeout(Transference.HTTP_CONNECT_TIMEOUT);

                con.setReadTimeout(Transference.HTTP_READ_TIMEOUT);

                con.setRequestMethod("POST");

                con.setDoOutput(true);

                con.setUseCaches(false);

                con.setFixedLengthStreamingMode(chunk_size);

                con.setRequestProperty("User-Agent", MainPanel.DEFAULT_USER_AGENT);

                tot_bytes_up = 0;

                boolean chunk_error = true;

                boolean timeout = false;

                try {

                    if (!_exit) {

                        RandomAccessFile f = new RandomAccessFile(_upload.getFile_name(), "r");

                        f.seek(chunk_offset);

                        ByteArrayOutputStream chunk_mac = new ByteArrayOutputStream();

                        try (QueueInputStream qis = new QueueInputStream(); QueueOutputStream qos = qis.newQueueOutputStream(); BufferedInputStream bis = new BufferedInputStream(Channels.newInputStream(f.getChannel())); CipherInputStream cis = new CipherInputStream(qis, genCrypter("AES", "AES/CTR/NoPadding", _upload.getByte_file_key(), forwardMEGALinkKeyIV(_upload.getByte_file_iv(), chunk_offset))); OutputStream out = new ThrottledOutputStream(con.getOutputStream(), _upload.getMain_panel().getStream_supervisor())) {

                            LOG.info("Uploading chunk {} from worker {} {}...", chunk_id, _id, _upload.getFile_name());

                            while (!_exit && tot_bytes_up < chunk_size && (reads = bis.read(buffer)) != -1) {

                                chunk_mac.write(buffer, 0, reads);

                                for (int i = 0; i < reads; i++) {
                                    qos.write(buffer[i]);
                                }

                                for (int i = 0; i < reads; i++) {
                                    buffer_enc[i] = (byte) cis.read();
                                }

                                out.write(buffer_enc, 0, reads);

                                _upload.getPartialProgress().add((long) reads);

                                _upload.getProgress_meter().secureNotify();

                                tot_bytes_up += reads;

                                if (_upload.isPaused() && !_exit && !_upload.isStopped() && tot_bytes_up < chunk_size) {

                                    _upload.pause_worker();

                                    secureWait();

                                }
                            }

                            _upload.getMac_generator().CHUNK_QUEUE.put(chunk_offset, chunk_mac);
                        }

                        if (!_exit) {

                            if ((http_status = con.getResponseCode()) != 200) {

                                LOG.info("Worker {} Failed : HTTP error code : {} {}", _id, http_status, _upload.getFile_name());

                            } else if (tot_bytes_up == chunk_size || reads == -1) {

                                if (_upload.getProgress() == _upload.getFile_size()) {
                                    _upload.getView().printStatusWarning("Waiting for completion handler ... ***DO NOT EXIT MEGABASTERD NOW***");
                                    _upload.getView().getPause_button().setEnabled(false);
                                }

                                String httpResponse;

                                try (InputStream is = con.getInputStream(); ByteArrayOutputStream byte_res = new ByteArrayOutputStream()) {

                                    while ((reads = is.read(buffer)) != -1) {
                                        byte_res.write(buffer, 0, reads);
                                    }

                                    httpResponse = byte_res.toString(StandardCharsets.UTF_8);
                                }

                                if (!httpResponse.isEmpty()) {
                                    if (MegaAPI.checkMEGAError(httpResponse) != 0) {
                                        LOG.warn("Worker {} UPLOAD FAILED! (MEGA ERROR: {}) {}", _id, MegaAPI.checkMEGAError(httpResponse), _upload.getFile_name());
                                        fatal_error = true;
                                    } else {
                                        LOG.info("Worker {} Completion handler -> {} {}", _id, httpResponse, _upload.getFile_name());
                                        _upload.setCompletion_handler(httpResponse);
                                        chunk_error = false;
                                    }
                                } else if (_upload.getProgress() != _upload.getFile_size() || _upload.getCompletion_handler() != null) {
                                    chunk_error = false;
                                }
                            }
                        }

                        if (_upload.getMain_panel().isExit()) {
                            secureWait();
                        }
                    }

                } catch (IOException ex) {

                    if (ex instanceof SocketTimeoutException) {
                        timeout = true;
                        LOG.fatal("Worker {} {} TIMEOUT reading chunk {}", _id, _upload.getFile_name(), chunk_id);
                    } else {
                        LOG.fatal("Worker {} {} ERROR reading chunk {}: {}", _id, _upload.getFile_name(), chunk_id, ex.getMessage());
                    }

                } finally {

                    if (chunk_error) {

                        LOG.warn("Uploading chunk {} from worker {} FAILED! {}...", chunk_id, _id, _upload.getFile_name());

                        _upload.rejectChunkId(chunk_id);

                        if (tot_bytes_up > 0) {

                            _upload.getPartialProgress().add(-1 * tot_bytes_up);

                            _upload.getProgress_meter().secureNotify();
                        }

                        if (fatal_error) {

                            _upload.stopUploader("UPLOAD FAILED: FATAL ERROR");

                            LOG.fatal("UPLOAD FAILED: FATAL ERROR {}", _upload.getFile_name());

                        } else if (++errorCount == MAX_CHUNK_ERROR) {

                            _upload.stopUploader("UPLOAD FAILED: too many errors");

                            LOG.fatal("UPLOAD FAILED: too many errors {}", _upload.getFile_name());

                        } else if (!_exit && !_upload.isStopped() && !timeout) {

                            _error_wait = true;

                            _upload.getView().updateSlotsStatus();

                            try {
                                Thread.sleep(MiscTools.getWaitTimeExpBackOff(errorCount) * 1000);
                            } catch (InterruptedException ignored) { }

                            _error_wait = false;

                            _upload.getView().updateSlotsStatus();

                        }

                    } else {
                        LOG.info("Worker {} has uploaded chunk {} {}", _id, chunk_id, _upload.getFile_name());
                        errorCount = 0;
                    }

                    con.disconnect();
                }

            }

        } catch (ChunkInvalidException ex) {

            _chunk_exception = true;

        } catch (OutOfMemoryError | Exception error) {
            _upload.stopUploader(error.getMessage());
            LOG.fatal(error.getMessage());
        }

        _upload.stopThisSlot(this);

        _upload.getMac_generator().secureNotify();

        LOG.info("ChunkUploader [{}] {} bye bye...", _id, _upload.getFile_name());

    }

}
