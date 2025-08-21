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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static com.tonikelope.megabasterd.CryptTools.genCrypter;
import static com.tonikelope.megabasterd.MiscTools.BASE642Bin;
import static com.tonikelope.megabasterd.MiscTools.Bin2BASE64;
import static com.tonikelope.megabasterd.MiscTools.bin2i32a;
import static com.tonikelope.megabasterd.MiscTools.i32a2bin;

/**
 *
 * @author tonikelope
 */
public class UploadMACGenerator implements Runnable, SecureSingleThreadNotifiable {

    private static final Logger LOG = LogManager.getLogger(UploadMACGenerator.class);

    private final Upload _upload;
    private final Object _secure_notify_lock;
    private volatile boolean _notified;
    private volatile boolean _exit;
    public final ConcurrentHashMap<Long, ByteArrayOutputStream> CHUNK_QUEUE = new ConcurrentHashMap<>();

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
                    _secure_notify_lock.wait(1000);
                } catch (InterruptedException ex) {
                    _exit = true;
                    LOG.log(Level.FATAL, "Sleep interrupted! {}", ex.getMessage());
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

        LOG.log(Level.INFO, "MAC GENERATOR {} Hello!", getUpload().getFile_name());

        try {

            long chunk_id = 1L, tot = 0L;

            boolean mac = false;

            int cbc_per = 0;

            int[] file_mac = new int[]{0, 0, 0, 0};

            HashMap upload_progress = DBTools.selectUploadProgress(_upload.getFile_name(), _upload.getMa().getFull_email());

            if (upload_progress != null) {

                if (upload_progress.get("meta_mac") != null) {

                    String[] temp_meta_mac = ((String) upload_progress.get("meta_mac")).split("#");

                    tot = Long.parseLong(temp_meta_mac[0]);

                    chunk_id = Long.parseLong(temp_meta_mac[1]);

                    file_mac = bin2i32a(BASE642Bin(temp_meta_mac[2]));

                    cbc_per = (int) ((((double) tot) / _upload.getFile_size()) * 100);

                    _upload.getView().updateCBC("CBC-MAC " + cbc_per + "%");
                }
            }

            int[] file_iv = bin2i32a(_upload.getByte_file_iv()), int_block, mac_iv = CryptTools.AES_ZERO_IV_I32A;

            Cipher cryptor = genCrypter("AES", "AES/CBC/NoPadding", _upload.getByte_file_key(), i32a2bin(mac_iv));

            int[] chunk_mac = new int[4];
            byte[] byte_block = new byte[16];
            byte[] chunk_bytes;

            try {
                while (!_exit && !_upload.isStopped() && !_upload.getMain_panel().isExit()) {

                    int reads;

                    long chunk_offset = ChunkWriterManager.calculateChunkOffset(chunk_id, 1);

                    long chunk_size = ChunkWriterManager.calculateChunkSize(chunk_id, _upload.getFile_size(), chunk_offset, 1);

                    ChunkWriterManager.checkChunkID(chunk_id, _upload.getFile_size(), chunk_offset);

                    while (!CHUNK_QUEUE.containsKey(chunk_offset)) {
                        MiscTools.pause(1000);
                    }

                    try {

                        chunk_mac[0] = file_iv[0];
                        chunk_mac[1] = file_iv[1];
                        chunk_mac[2] = file_iv[0];
                        chunk_mac[3] = file_iv[1];

                        long conta_chunk = 0L;

                        try (ByteArrayOutputStream baos = CHUNK_QUEUE.remove(chunk_offset)) {
                            chunk_bytes = baos.toByteArray();
                        }

                        try (ByteArrayInputStream bais = new ByteArrayInputStream(chunk_bytes)) {
                            while (conta_chunk < chunk_size && (reads = bais.read(byte_block)) != -1) {

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

                                tot += reads;

                            }
                        }

                        for (int i = 0; i < file_mac.length; i++) {
                            file_mac[i] ^= chunk_mac[i];
                        }

                        file_mac = bin2i32a(cryptor.doFinal(i32a2bin(file_mac)));

                    } catch (IllegalBlockSizeException | BadPaddingException ex) {
                        LOG.log(Level.FATAL, "Failed to generate MAC! {}", ex.getMessage());
                    }

                    chunk_id++;

                    int new_cbc_per = (int) ((((double) tot) / _upload.getFile_size()) * 100);

                    if (new_cbc_per != cbc_per) {
                        _upload.getView().updateCBC("CBC-MAC " + new_cbc_per + "%");
                        cbc_per = new_cbc_per;
                    }

                }

                mac = (tot == _upload.getFile_size());

            } catch (ChunkInvalidException e) {

                mac = true;
            }

            _upload.setTemp_mac_data(tot + "#" + chunk_id + "#" + Bin2BASE64(i32a2bin(file_mac)));

            if (mac) {

                int[] meta_mac = {file_mac[0] ^ file_mac[1], file_mac[2] ^ file_mac[3]};

                _upload.setFile_meta_mac(meta_mac);

                LOG.log(Level.INFO, "MAC GENERATOR {} finished MAC CALCULATION. Waiting workers to finish uploading (if any)...", getUpload().getFile_name());

            }

            while (!_exit && !_upload.isStopped() && !_upload.getChunkworkers().isEmpty()) {
                while (_upload.getMain_panel().isExit()) {
                    _upload.secureNotifyWorkers();
                    secureWait();
                }

                secureWait();
            }

            _upload.secureNotify();

            LOG.log(Level.INFO, "MAC GENERATOR {} BYE BYE...", getUpload().getFile_name());

        } catch (Exception ex) {
            LOG.log(Level.FATAL, "Generic exception in run! {}", ex.getMessage());
        }

    }

}
