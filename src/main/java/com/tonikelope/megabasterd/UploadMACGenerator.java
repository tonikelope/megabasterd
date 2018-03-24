package com.tonikelope.megabasterd;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import static com.tonikelope.megabasterd.CryptTools.*;
import static com.tonikelope.megabasterd.MiscTools.*;
import java.io.File;
import java.io.FileInputStream;

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
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
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

        try {

            boolean mac = false;

            HashMap upload_progress = DBTools.selectUploadProgress(_upload.getFile_name(), _upload.getMa().getEmail());

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

                try (FileInputStream is = new FileInputStream(new File(_upload.getFile_name()))) {

                    long chunk_id = 1L;
                    long tot = 0L;

                    try {
                        while (!_exit && !_upload.isStopped() && !_upload.getMain_panel().isExit()) {

                            Chunk current_chunk = new Chunk(chunk_id++, _upload.getFile_size(), null);
                            int reads;

                            try {

                                int[] chunk_mac = {file_iv[0], file_iv[1], file_iv[0], file_iv[1]};
                                byte[] byte_block = new byte[16];
                                long conta_chunk = 0L;
                                long chunk_size = current_chunk.getSize();

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
                                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                            }

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

            Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} MAC GENERATOR {1} finished MAC CALCULATION. Waiting workers to finish uploading...", new Object[]{Thread.currentThread().getName(), this.getUpload().getFile_name()});

            while (!_exit && !_upload.isStopped() && !_upload.getChunkworkers().isEmpty()) {
                while (_upload.getMain_panel().isExit()) {
                    _upload.secureNotifyWorkers();
                    secureWait();
                }

                secureWait();
            }

            _upload.getMain_panel().getUpload_manager().getFinishing_uploads_queue().add(_upload);

            _upload.secureNotify();

            Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} MAC GENERATOR {1} BYE BYE...", new Object[]{Thread.currentThread().getName(), this.getUpload().getFile_name()});

        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }

    }

}
