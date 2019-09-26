package com.tonikelope.megabasterd;

import static com.tonikelope.megabasterd.CryptTools.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.CipherInputStream;

/**
 *
 * @author tonikelope
 */
public class ChunkUploader implements Runnable, SecureSingleThreadNotifiable {

    public static final int MAX_CHUNK_ERROR = 50;
    private final int _id;
    private final Upload _upload;
    private volatile boolean _exit;
    private final Object _secure_notify_lock;
    private volatile boolean _error_wait;
    private boolean _notified;

    public ChunkUploader(int id, Upload upload) {
        _notified = false;
        _secure_notify_lock = new Object();
        _id = id;
        _upload = upload;
        _exit = false;
        _error_wait = false;
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
                    _secure_notify_lock.wait();
                } catch (InterruptedException ex) {
                    _exit = true;
                    LOG.log(Level.SEVERE, null, ex);
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

        LOG.log(Level.INFO, "{0} ChunkUploader {1} hello! {2}", new Object[]{Thread.currentThread().getName(), getId(), getUpload().getFile_name()});

        long chunk_id = 0;

        byte[] buffer = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];

        try {

            int conta_error = 0;

            while (!_upload.getMain_panel().isExit() && !_exit && !_upload.isStopped() && conta_error < MAX_CHUNK_ERROR) {

                if (_upload.isPaused() && !_upload.isStopped()) {

                    _upload.pause_worker();

                    secureWait();

                }

                String worker_url = _upload.getUl_url();

                int reads = 0, http_status;

                long tot_bytes_up;

                chunk_id = _upload.nextChunkId();

                long chunk_offset = ChunkManager.calculateChunkOffset(chunk_id, Upload.CHUNK_SIZE_MULTI);

                long chunk_size = ChunkManager.calculateChunkSize(chunk_id, _upload.getFile_size(), chunk_offset, Upload.CHUNK_SIZE_MULTI);

                ChunkManager.checkChunkID(chunk_id, _upload.getFile_size(), chunk_offset);

                String chunk_url = ChunkManager.genChunkUrl(worker_url, _upload.getFile_size(), chunk_offset, chunk_size);

                URL url = new URL(chunk_url);

                HttpURLConnection con;

                if (MainPanel.isUse_proxy()) {

                    con = (HttpURLConnection) url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(MainPanel.getProxy_host(), MainPanel.getProxy_port())));

                    if (MainPanel.getProxy_user() != null && !"".equals(MainPanel.getProxy_user())) {

                        con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((MainPanel.getProxy_user() + ":" + MainPanel.getProxy_pass()).getBytes("UTF-8")));
                    }
                } else {

                    con = (HttpURLConnection) url.openConnection();
                }

                con.setRequestMethod("POST");

                con.setDoOutput(true);

                con.setUseCaches(false);

                con.setFixedLengthStreamingMode(chunk_size);

                con.setConnectTimeout(Upload.HTTP_TIMEOUT);

                con.setReadTimeout(Upload.HTTP_TIMEOUT);

                con.setRequestProperty("User-Agent", MainPanel.DEFAULT_USER_AGENT);

                tot_bytes_up = 0;

                boolean chunk_error = true;

                try {

                    if (!_exit && !_upload.isStopped()) {

                        RandomAccessFile f = new RandomAccessFile(_upload.getFile_name(), "r");

                        f.seek(chunk_offset);

                        try (CipherInputStream cis = new CipherInputStream(new BufferedInputStream(Channels.newInputStream(f.getChannel())), genCrypter("AES", "AES/CTR/NoPadding", _upload.getByte_file_key(), forwardMEGALinkKeyIV(_upload.getByte_file_iv(), chunk_offset))); OutputStream out = new ThrottledOutputStream(con.getOutputStream(), _upload.getMain_panel().getStream_supervisor())) {

                            LOG.log(Level.INFO, "{0} Uploading chunk {1} from worker {2}...", new Object[]{Thread.currentThread().getName(), chunk_id, _id});

                            while (!_exit && !_upload.isStopped() && tot_bytes_up < chunk_size && (reads = cis.read(buffer)) != -1) {
                                out.write(buffer, 0, reads);

                                _upload.getPartialProgress().add((long) reads);

                                _upload.getProgress_meter().secureNotify();

                                tot_bytes_up += reads;

                                if (_upload.isPaused() && !_upload.isStopped()) {

                                    _upload.pause_worker();

                                    secureWait();

                                }
                            }
                        }

                        if (!_upload.isStopped() && !_exit) {

                            if ((http_status = con.getResponseCode()) != 200) {

                                LOG.log(Level.INFO, "{0} Failed : HTTP error code : {1}", new Object[]{Thread.currentThread().getName(), http_status});

                            } else if (tot_bytes_up == chunk_size || reads == -1) {

                                String httpresponse;

                                try (InputStream is = con.getInputStream(); ByteArrayOutputStream byte_res = new ByteArrayOutputStream()) {

                                    while ((reads = is.read(buffer)) != -1) {

                                        byte_res.write(buffer, 0, reads);
                                    }

                                    httpresponse = new String(byte_res.toByteArray(), "UTF-8");

                                }

                                if (httpresponse.length() > 0) {

                                    if (MegaAPI.checkMEGAError(httpresponse) != 0) {

                                        LOG.log(Level.WARNING, "{0} UPLOAD FAILED! (MEGA ERROR: {1})", new Object[]{Thread.currentThread().getName(), MegaAPI.checkMEGAError(httpresponse)});

                                    } else {

                                        LOG.log(Level.INFO, "{0} Completion handle -> {1}", new Object[]{Thread.currentThread().getName(), httpresponse});

                                        _upload.setCompletion_handle(httpresponse);

                                        chunk_error = false;
                                    }

                                } else {

                                    chunk_error = false;
                                }
                            }

                        }

                        if (_upload.getMain_panel().isExit()) {
                            secureWait();
                        }
                    }

                } catch (IOException ex) {

                    LOG.log(Level.SEVERE, null, ex.getMessage());

                } finally {

                    if (chunk_error) {

                        LOG.log(Level.WARNING, "{0} Uploading chunk {1} from worker {2} FAILED!...", new Object[]{Thread.currentThread().getName(), chunk_id, _id});

                        _upload.rejectChunkId(chunk_id);

                        if (tot_bytes_up > 0) {

                            _upload.getPartialProgress().add(-1 * tot_bytes_up);

                            _upload.getProgress_meter().secureNotify();
                        }

                        if (!_exit) {

                            if (++conta_error == MAX_CHUNK_ERROR) {

                                _upload.stopUploader("UPLOAD FAILED: too many errors");

                                LOG.log(Level.SEVERE, null, "UPLOAD FAILED: too many errors");

                            } else {

                                _error_wait = true;

                                _upload.getView().updateSlotsStatus();

                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException excep) {

                                }

                                _error_wait = false;

                                _upload.getView().updateSlotsStatus();

                            }
                        }

                    } else {

                        LOG.log(Level.INFO, "{0} Worker {1} has uploaded chunk {2}", new Object[]{Thread.currentThread().getName(), _id, chunk_id});

                        conta_error = 0;
                    }

                    con.disconnect();
                }

            }

        } catch (ChunkInvalidException ex) {

        } catch (OutOfMemoryError | Exception error) {
            _upload.stopUploader(error.getMessage());
            LOG.log(Level.SEVERE, null, error.getMessage());
        }

        _upload.stopThisSlot(this);

        _upload.getMac_generator().secureNotify();

        LOG.log(Level.INFO, "{0} ChunkUploader {1} bye bye...", new Object[]{Thread.currentThread().getName(), _id});

    }
    private static final Logger LOG = Logger.getLogger(ChunkUploader.class.getName());

}
