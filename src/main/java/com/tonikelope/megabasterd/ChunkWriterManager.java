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

import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import static com.tonikelope.megabasterd.CryptTools.forwardMEGALinkKeyIV;
import static com.tonikelope.megabasterd.CryptTools.genDecrypter;
import static com.tonikelope.megabasterd.CryptTools.initMEGALinkKey;
import static com.tonikelope.megabasterd.CryptTools.initMEGALinkKeyIV;
import static java.lang.String.valueOf;

/**
 *
 * @author tonikelope
 */
public class ChunkWriterManager implements Runnable, SecureSingleThreadNotifiable {

    private static final Logger LOG = LogManager.getLogger(ChunkWriterManager.class);

    private static final ReentrantLock JOIN_CHUNKS_LOCK = new ReentrantLock();

    public static long calculateChunkOffset(long chunk_id, int size_multi) {
        long[] offs = {0, 128, 384, 768, 1280, 1920, 2688};

        return (chunk_id <= 7 ? offs[(int) chunk_id - 1] : (3584 + (chunk_id - 8) * 1024 * size_multi)) * 1024;
    }

    public static String genChunkUrl(String file_url, long file_size, long offset, long chunk_size) {
        return file_url != null ? file_url + "/" + offset + (offset + chunk_size == file_size ? "" : "-" + (offset + chunk_size - 1)) : null;
    }

    public static void checkChunkID(long chunk_id, long file_size, long offset) throws ChunkInvalidException {

        if (file_size > 0) {
            if (offset >= file_size) {
                throw new ChunkInvalidException(valueOf(chunk_id));
            }

        } else {

            if (chunk_id > 1) {

                throw new ChunkInvalidException(valueOf(chunk_id));
            }
        }
    }

    public static long calculateChunkSize(long chunk_id, long file_size, long offset, int size_multi) {
        long chunk_size = (chunk_id >= 1 && chunk_id <= 7) ? chunk_id * 128 * 1024 : 1024L * 1024L * size_multi;

        if (offset + chunk_size > file_size) {
            chunk_size = file_size - offset;
        }

        return chunk_size;
    }
    private final AtomicLong _last_chunk_id_written = new AtomicLong(0);
    private final AtomicLong _bytes_written = new AtomicLong(0);
    private final long _file_size;
    private final Download _download;
    private final byte[] _byte_file_key;
    private final byte[] _byte_iv;
    private volatile boolean _exit;
    private final Object _secure_notify_lock;
    private volatile boolean _notified;
    private final String _chunks_dir;

    public ChunkWriterManager(Download downloader) throws Exception {
        _notified = false;
        _exit = false;
        _download = downloader;
        _chunks_dir = _create_chunks_temp_dir();
        _secure_notify_lock = new Object();
        _file_size = _download.getFile_size();
        _byte_file_key = initMEGALinkKey(_download.getFile_key());
        _byte_iv = initMEGALinkKeyIV(_download.getFile_key());

        if (_download.getProgress() != 0) {
            _last_chunk_id_written.set(_download.getLast_chunk_id_dispatched());
            _bytes_written.set(_download.getProgress());
        }
    }

    public String getChunks_dir() {
        return _chunks_dir;
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

    public boolean isExit() {
        return _exit;
    }

    private String _create_chunks_temp_dir() {

        File chunks_temp_dir = new File((_download.getCustom_chunks_dir() != null ? _download.getCustom_chunks_dir() : _download.getDownload_path()) + "/.MEGABASTERD_CHUNKS_" + MiscTools.HashString("sha1", _download.getUrl()));

        chunks_temp_dir.mkdirs();

        return chunks_temp_dir.getAbsolutePath();
    }

    public void delete_chunks_temp_dir() {
        try {
            MiscTools.deleteDirectoryRecursion(Paths.get(getChunks_dir()));
        } catch (IOException ex) {
            LOG.fatal("Error deleting temp directory! {}", ex.getMessage());
        }
    }

    private void finishDownload() {
        MiscTools.GUIRun(() -> {
            DownloadView downloadView = _download.getView();
            MainPanel mainPanel = _download.getMain_panel();
            DownloadManager manager = mainPanel.getDownload_manager();

            manager.getTransference_running_list().remove(_download);
            manager.secureNotify();
            downloadView.printStatusNormal("Download finished. Joining file chunks, please wait...");
            downloadView.getPause_button().setVisible(false);
            mainPanel.getGlobal_dl_speed().detachTransference(_download);
            downloadView.getSpeed_label().setVisible(false);
            downloadView.getSlots_label().setVisible(false);
            downloadView.getSlot_status_label().setVisible(false);
            downloadView.getSlots_spinner().setVisible(false);
        });
    }

    @Override
    public void run() {

        LOG.info("ChunkWriterManager: let's do some work! {}", _download.getFile_name());
        LOG.info("ChunkWriterManager LAST CHUNK WRITTEN -> [{}] {} {}...", _last_chunk_id_written, _bytes_written, _download.getFile_name());
        boolean download_finished = false;
        if (_file_size > 0) {

            try {

                while (!_exit && (!_download.isStopped() || !_download.getChunkworkers().isEmpty()) && _bytes_written.get() < _file_size) {

                    if (!JOIN_CHUNKS_LOCK.isHeldByCurrentThread()) {
                        LOG.info("ChunkWriterManager: JOIN LOCK LOCKED FOR {}", _download.getFile_name());
                        JOIN_CHUNKS_LOCK.lock();
                    }

                    if (!download_finished && _download.getProgress() == _file_size) {
                        finishDownload();
                        download_finished = true;
                    }

                    boolean chunk_io_error;

                    do {

                        chunk_io_error = false;

                        try {

                            File chunk_file = new File(getChunks_dir() + "/" + MiscTools.HashString("sha1", _download.getUrl()) + ".chunk" + (_last_chunk_id_written.get() + 1));

                            while (chunk_file.exists() && chunk_file.canRead() && chunk_file.canWrite() && chunk_file.length() > 0) {

                                if (!download_finished && _download.getProgress() == _file_size) {

                                    finishDownload();
                                    download_finished = true;
                                }

                                byte[] buffer = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];

                                int reads;

                                try (CipherInputStream cis = new CipherInputStream(new BufferedInputStream(new FileInputStream(chunk_file)), genDecrypter("AES", "AES/CTR/NoPadding", _byte_file_key, forwardMEGALinkKeyIV(_byte_iv, _bytes_written.get())))) {
                                    while ((reads = cis.read(buffer)) != -1) {
                                        _download.getOutput_stream().write(buffer, 0, reads);
                                    }
                                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException ex) {
                                    LOG.fatal("Cannot read stream! {}", ex.getMessage());
                                }

                                _bytes_written.addAndGet(chunk_file.length());
                                _last_chunk_id_written.addAndGet(1);

                                LOG.info("ChunkWriterManager has written to disk chunk [{}] {} {} {}...", _last_chunk_id_written, _bytes_written, _download.calculateLastWrittenChunk(_bytes_written.get()), _download.getFile_name());

                                chunk_file.delete();

                                chunk_file = new File(getChunks_dir() + "/" + MiscTools.HashString("sha1", _download.getUrl()) + ".chunk" + (_last_chunk_id_written.get() + 1));
                            }

                        } catch (IOException ex) {
                            chunk_io_error = true;
                            LOG.warn("IO Exception writing chunk {}! {}", (_last_chunk_id_written.get() + 1), ex.getMessage());
                            MiscTools.pause(1000);
                        }

                    } while (chunk_io_error);

                    if (!_exit && (!_download.isStopped() || !_download.getChunkworkers().isEmpty()) && _bytes_written.get() < _file_size) {

                        LOG.info("ChunkWriterManager waiting for chunk [{}] {}...", (_last_chunk_id_written.get() + 1), _download.getFile_name());

                        if (JOIN_CHUNKS_LOCK.isHeldByCurrentThread() && JOIN_CHUNKS_LOCK.isLocked()) {
                            LOG.info("ChunkWriterManager: JOIN LOCK RELEASED FOR {}", _download.getFile_name());
                            JOIN_CHUNKS_LOCK.unlock();
                        }

                        secureWait();
                    }
                }

            } finally {
                if (JOIN_CHUNKS_LOCK.isHeldByCurrentThread() && JOIN_CHUNKS_LOCK.isLocked()) {
                    LOG.info("ChunkWriterManager: JOIN LOCK RELEASED FOR {}", _download.getFile_name());
                    JOIN_CHUNKS_LOCK.unlock();
                }
            }

            if (_bytes_written.get() == _file_size && MiscTools.isDirEmpty(Paths.get(getChunks_dir()))) {
                delete_chunks_temp_dir();
            }
        }

        _exit = true;

        _download.secureNotify();

        LOG.info("ChunkWriterManager: bye bye {}", _download.getFile_name());
    }

}
