package com.tonikelope.megabasterd;

import static com.tonikelope.megabasterd.CryptTools.*;
import static com.tonikelope.megabasterd.MiscTools.*;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

/**
 *
 * @author tonikelope
 */
public final class UploadMACGenerator implements Runnable, SecureSingleThreadNotifiable {

    private final Upload _upload;
    private final Object _secure_notify_lock;
    private boolean _notified;
    private volatile boolean _exit;

    public UploadMACGenerator(Upload upload) {
        _secure_notify_lock = new Object();
        _notified = false;
        _upload = upload;
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
                    LOG.log(Level.SEVERE, null, ex);
                }
            }

            _notified = false;
        }
    }

    public Upload getUpload() {
        return _upload;
    }

    public boolean isExit() {
        return _exit;
    }

    public void setExit(boolean exit) {
        _exit = exit;
    }

    @Override
    public void run() {

        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

        try {

            boolean mac = false;

            HashMap upload_progress = DBTools.selectUploadProgress(_upload.getFile_name(), _upload.getMa().getFull_email());

            int[] file_mac = new int[]{0, 0, 0, 0};

            if (upload_progress != null) {

                if ((String) upload_progress.get("meta_mac") != null) {

                    int[] meta_mac = bin2i32a(BASE642Bin((String) upload_progress.get("meta_mac")));

                    _upload.setFile_meta_mac(meta_mac);

                    mac = true;
                }
            }

            if (!mac) {

                int[] file_iv = bin2i32a(_upload.getByte_file_iv()), int_block, mac_iv = CryptTools.AES_ZERO_IV_I32A;

                Cipher cryptor = genCrypter("AES", "AES/CBC/NoPadding", _upload.getByte_file_key(), i32a2bin(mac_iv));

                try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(_upload.getFile_name()))) {

                    long chunk_id = 1L;
                    long tot = 0L;
                    int[] chunk_mac = new int[4];
                    byte[] byte_block = new byte[16];

                    try {
                        while (!_exit && !_upload.isStopped() && !_upload.getMain_panel().isExit()) {

                            int reads;

                            long chunk_offset = ChunkManager.calculateChunkOffset(chunk_id, 1);

                            long chunk_size = ChunkManager.calculateChunkSize(chunk_id, _upload.getFile_size(), chunk_offset, 1);

                            ChunkManager.checkChunkID(chunk_id, _upload.getFile_size(), chunk_offset);

                            try {

                                chunk_mac[0] = file_iv[0];
                                chunk_mac[1] = file_iv[1];
                                chunk_mac[2] = file_iv[0];
                                chunk_mac[3] = file_iv[1];

                                long conta_chunk = 0L;

                                while (conta_chunk < chunk_size && (reads = is.read(byte_block)) != -1) {

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

                                    conta_chunk += reads;

                                    tot = reads;

                                }

                                for (int i = 0; i < file_mac.length; i++) {
                                    file_mac[i] ^= chunk_mac[i];
                                }

                                file_mac = bin2i32a(cryptor.doFinal(i32a2bin(file_mac)));

                            } catch (IOException | IllegalBlockSizeException | BadPaddingException ex) {
                                LOG.log(Level.SEVERE, null, ex);
                            }

                            chunk_id++;

                        }

                        mac = (tot == _upload.getFile_size());

                    } catch (ChunkInvalidException e) {

                        mac = true;
                    }

                    if (!_exit && mac) {

                        int[] meta_mac = {file_mac[0] ^ file_mac[1], file_mac[2] ^ file_mac[3]};

                        _upload.setFile_meta_mac(meta_mac);
                    }
                }
            }

            LOG.log(Level.INFO, "{0} MAC GENERATOR {1} finished MAC CALCULATION. Waiting workers to finish uploading...", new Object[]{Thread.currentThread().getName(), this.getUpload().getFile_name()});

            while (!_exit && !_upload.isStopped() && !_upload.getChunkworkers().isEmpty()) {
                while (_upload.getMain_panel().isExit()) {
                    _upload.secureNotifyWorkers();
                    secureWait();
                }

                secureWait();
            }

            _upload.getMain_panel().getUpload_manager().getFinishing_uploads_queue().add(_upload);

            _upload.secureNotify();

            LOG.log(Level.INFO, "{0} MAC GENERATOR {1} BYE BYE...", new Object[]{Thread.currentThread().getName(), this.getUpload().getFile_name()});

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

    }
    private static final Logger LOG = Logger.getLogger(UploadMACGenerator.class.getName());

}
