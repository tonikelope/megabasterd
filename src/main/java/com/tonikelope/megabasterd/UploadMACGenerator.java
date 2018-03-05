package com.tonikelope.megabasterd;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import static com.tonikelope.megabasterd.CryptTools.*;
import static com.tonikelope.megabasterd.MiscTools.*;

/**
 *
 * @author tonikelope
 */
public final class UploadMACGenerator implements Runnable, SecureSingleThreadNotifiable {

    private long _last_chunk_id_read;
    private final ConcurrentHashMap<Long, Chunk> _chunk_queue;
    private final Upload _upload;
    private final Object _secure_notify_lock;
    private boolean _notified;
    private volatile boolean _exit;
    private long _bytes_read;

    public UploadMACGenerator(Upload upload) {
        _secure_notify_lock = new Object();
        _notified = false;
        _upload = upload;
        _chunk_queue = new ConcurrentHashMap();
        _bytes_read = _upload.getProgress();
        _last_chunk_id_read = _upload.getLast_chunk_id_dispatched();
        _exit = false;
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
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                }
            }

            _notified = false;
        }
    }

    public long getLast_chunk_id_read() {
        return _last_chunk_id_read;
    }

    public ConcurrentHashMap<Long, Chunk> getChunk_queue() {
        return _chunk_queue;
    }

    public Upload getUpload() {
        return _upload;
    }

    public long getBytes_read() {
        return _bytes_read;
    }

    public boolean isExit() {
        return _exit;
    }

    public void setExit(boolean exit) {
        _exit = exit;
    }

    @Override
    public void run() {

        try {

            Chunk chunk;

            HashMap upload_progress = DBTools.selectUploadProgress(_upload.getFile_name(), _upload.getMa().getEmail());

            int[] file_mac = new int[]{0, 0, 0, 0};

            if (upload_progress != null) {

                if ((String) upload_progress.get("temp_mac") != null) {

                    file_mac = bin2i32a(BASE642Bin((String) upload_progress.get("temp_mac")));
                }
            }

            int[] file_iv = bin2i32a(_upload.getByte_file_iv()), int_block, mac_iv = CryptTools.AES_ZERO_IV_I32A;

            int reads;

            String temp_file_data;
            boolean new_chunk = false;
            boolean upload_workers_finish = false;
            Cipher cryptor = genCrypter("AES", "AES/CBC/NoPadding", _upload.getByte_file_key(), i32a2bin(mac_iv));

            while (!_exit && (!_upload.isStopped() || !_upload.getChunkworkers().isEmpty()) && (_bytes_read < _upload.getFile_size() || (_upload.getFile_size() == 0 && _last_chunk_id_read < 1))) {

                while (_chunk_queue.containsKey(_last_chunk_id_read + 1)) {

                    if (!upload_workers_finish && _upload.getProgress() == _upload.getFile_size()) {

                        _upload.getView().printStatusNormal("Finishing FILE MAC calculation... ***DO NOT EXIT MEGABASTERD NOW***");

                        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Macgenerator {1} Finishing FILE MAC calculation...", new Object[]{Thread.currentThread().getName(), this.getUpload().getFile_name()});

                        _upload.getView().getPause_button().setEnabled(false);

                        if (!_upload.getMain_panel().getUpload_manager().getFinishing_uploads_queue().contains(_upload)) {
                            _upload.getMain_panel().getUpload_manager().getFinishing_uploads_queue().add(_upload);
                        }

                        upload_workers_finish = true;
                    }

                    chunk = _chunk_queue.get(_last_chunk_id_read + 1);

                    InputStream chunk_is = chunk.getInputStream();

                    if (Upload.CHUNK_SIZE_MULTI == 1 || chunk.getId() <= 7) {

                        try {

                            int[] chunk_mac = {file_iv[0], file_iv[1], file_iv[0], file_iv[1]};
                            byte[] byte_block = new byte[16];

                            while ((reads = chunk_is.read(byte_block)) != -1) {

                                if (reads < byte_block.length) {
                                    for (int i = reads; i < byte_block.length; i++) {
                                        byte_block[i] = 0;
                                    }
                                }

                                int_block = bin2i32a(byte_block);

                                for (int i = 0; i < chunk_mac.length; i++) {
                                    chunk_mac[i] ^= int_block[i];
                                }

                                chunk_mac = bin2i32a(cryptor.doFinal(i32a2bin(chunk_mac)));
                            }

                            for (int i = 0; i < file_mac.length; i++) {
                                file_mac[i] ^= chunk_mac[i];
                            }

                            file_mac = bin2i32a(cryptor.doFinal(i32a2bin(file_mac)));

                        } catch (IOException | IllegalBlockSizeException | BadPaddingException ex) {
                            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {

                        do {
                            int[] chunk_mac = {file_iv[0], file_iv[1], file_iv[0], file_iv[1]};

                            byte[] byte_block = new byte[16];

                            long chunk_size = 0;

                            do {

                                if ((reads = chunk_is.read(byte_block)) != -1) {

                                    if (reads < byte_block.length) {

                                        for (int i = reads; i < byte_block.length; i++) {
                                            byte_block[i] = 0;
                                        }
                                    }

                                    int_block = bin2i32a(byte_block);

                                    for (int i = 0; i < chunk_mac.length; i++) {
                                        chunk_mac[i] ^= int_block[i];
                                    }

                                    chunk_mac = bin2i32a(cryptor.doFinal(i32a2bin(chunk_mac)));

                                    chunk_size += byte_block.length;
                                }

                            } while (reads != -1 && chunk_size < 1024 * 1024);

                            if (chunk_size > 0) {

                                for (int i = 0; i < file_mac.length; i++) {
                                    file_mac[i] ^= chunk_mac[i];
                                }

                                file_mac = bin2i32a(cryptor.doFinal(i32a2bin(file_mac)));
                            }

                        } while (reads != -1);

                    }

                    _bytes_read += chunk.getSize();

                    _last_chunk_id_read = chunk.getId();

                    _chunk_queue.remove(chunk.getId());

                    new_chunk = true;

                    temp_file_data = (String.valueOf(_bytes_read) + "|" + Bin2BASE64(i32a2bin(file_mac)));

                    Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Macgenerator -> {1} {2} {3} {4}", new Object[]{Thread.currentThread().getName(), temp_file_data, _upload.calculateLastUploadedChunk(_bytes_read), _last_chunk_id_read, this.getUpload().getFile_name()});

                }

                if (!upload_workers_finish && new_chunk) {

                    DBTools.updateUploadProgres(_upload.getFile_name(), _upload.getMa().getEmail(), _bytes_read, Bin2BASE64(i32a2bin(file_mac)));

                    new_chunk = false;
                }

                if (!_exit && (!_upload.isStopped() || !_upload.getChunkworkers().isEmpty()) && (_bytes_read < _upload.getFile_size() || (_upload.getFile_size() == 0 && _last_chunk_id_read < 1))) {

                    Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} {1}/{2} METAMAC wait {3}...", new Object[]{Thread.currentThread().getName(), _bytes_read, _upload.getFile_size(), this.getUpload().getFile_name()});

                    if (_upload.getMain_panel().isExit()) {

                        _upload.secureNotifyWorkers();
                    }

                    secureWait();
                }
            }

            if (_bytes_read == _upload.getFile_size()) {

                int[] meta_mac = {file_mac[0] ^ file_mac[1], file_mac[2] ^ file_mac[3]};

                _upload.setFile_meta_mac(meta_mac);
            }

            _upload.secureNotify();

            Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} MAC GENERATOR {1} BYE BYE...", new Object[]{Thread.currentThread().getName(), this.getUpload().getFile_name()});

        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }

    }

}
