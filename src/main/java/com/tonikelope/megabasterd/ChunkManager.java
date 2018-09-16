package com.tonikelope.megabasterd;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import static com.tonikelope.megabasterd.CryptTools.*;
import java.io.File;
import java.io.FileInputStream;

/**
 *
 * @author tonikelope
 */
public final class ChunkManager implements Runnable, SecureSingleThreadNotifiable {

    private long _last_chunk_id_written;
    private long _bytes_written;
    private final long _file_size;
    private final ConcurrentHashMap<Long, Chunk> _chunk_queue;
    private final Download _download;
    private final byte[] _byte_file_key;
    private final byte[] _byte_iv;
    private volatile boolean _exit;
    private final ConcurrentLinkedQueue<Long> _rejectedChunkIds;
    private final Object _secure_notify_lock;
    private boolean _notified;

    public ChunkManager(Download downloader) throws Exception {
        _notified = false;
        _exit = false;
        _download = downloader;
        _secure_notify_lock = new Object();
        _file_size = _download.getFile_size();
        _byte_file_key = initMEGALinkKey(_download.getFile_key());
        _byte_iv = initMEGALinkKeyIV(_download.getFile_key());
        _chunk_queue = new ConcurrentHashMap();
        _rejectedChunkIds = new ConcurrentLinkedQueue<>();

        if (_download.getProgress() == 0) {
            _download.setLast_chunk_id_dispatched(0);

            _last_chunk_id_written = 0;

            _bytes_written = 0;
        } else {
            _last_chunk_id_written = calculateLastWrittenChunk(_download.getProgress());

            _download.setLast_chunk_id_dispatched(_last_chunk_id_written);

            _bytes_written = _download.getProgress();
        }
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

    public byte[] getByte_file_key() {
        return _byte_file_key;
    }

    public ConcurrentLinkedQueue getRejectedChunkIds() {
        return _rejectedChunkIds;
    }

    public boolean isExit() {
        return _exit;
    }

    public long getBytes_written() {
        return _bytes_written;
    }

    public long getLast_chunk_id_written() {
        return _last_chunk_id_written;
    }

    public ConcurrentHashMap getChunk_queue() {
        return _chunk_queue;
    }

    @Override
    public void run() {

        try {

            Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Filewriter: let''s do some work!", Thread.currentThread().getName());

            if (_file_size > 0) {
                while (!_exit && (!_download.isStopped() || !_download.getChunkworkers().isEmpty()) && _bytes_written < _file_size) {

                    File chunk_file = new File(_download.getDownload_path() + "/" + _download.getFile_name() + ".chunk" + String.valueOf(_last_chunk_id_written + 1));

                    while (_chunk_queue.containsKey(_last_chunk_id_written + 1) || chunk_file.exists()) {

                        if (_chunk_queue.containsKey(_last_chunk_id_written + 1)) {
                            Chunk current_chunk = _chunk_queue.remove(_last_chunk_id_written + 1);
                        }

                        byte[] buffer = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];

                        int reads;

                        try (CipherInputStream cis = new CipherInputStream(new FileInputStream(chunk_file), genDecrypter("AES", "AES/CTR/NoPadding", _byte_file_key, forwardMEGALinkKeyIV(_byte_iv, _bytes_written)))) {
                            while ((reads = cis.read(buffer)) != -1) {
                                _download.getOutput_stream().write(buffer, 0, reads);
                            }
                        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException ex) {
                            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                        }

                        _bytes_written += chunk_file.length();

                        _last_chunk_id_written++;

                        chunk_file.delete();

                        chunk_file = new File(_download.getDownload_path() + "/" + _download.getFile_name() + ".chunk" + String.valueOf(_last_chunk_id_written + 1));
                    }

                    if (!_exit && (!_download.isStopped() || !_download.getChunkworkers().isEmpty()) && _bytes_written < _file_size) {

                        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Filewriter waiting for chunk [{1}]...", new Object[]{Thread.currentThread().getName(), _last_chunk_id_written});

                        secureWait();
                    }
                }
            }

        } catch (IOException ex) {

            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            _download.stopDownloader(ex.getMessage());
        }

        _exit = true;

        _download.secureNotify();

        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Filewriter: bye bye{1}", new Object[]{Thread.currentThread().getName(), _download.getFile().getName()});
    }

    private long calculateLastWrittenChunk(long temp_file_size) {
        if (temp_file_size > 3584 * 1024) {
            return 7 + (long) Math.ceil((float) (temp_file_size - 3584 * 1024) / (1024 * 1024 * (_download.isUse_slots() ? Download.CHUNK_SIZE_MULTI : 1)));
        } else {
            int i = 0, tot = 0;

            while (tot < temp_file_size) {
                i++;
                tot += i * 128 * 1024;
            }

            return i;
        }
    }
}
