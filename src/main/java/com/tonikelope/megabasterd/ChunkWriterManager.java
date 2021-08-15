package com.tonikelope.megabasterd;

import static com.tonikelope.megabasterd.CryptTools.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import static java.lang.String.valueOf;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;

/**
 *
 * @author tonikelope
 */
public class ChunkWriterManager implements Runnable, SecureSingleThreadNotifiable {

    private static final Logger LOG = Logger.getLogger(ChunkWriterManager.class.getName());

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
        long chunk_size = (chunk_id >= 1 && chunk_id <= 7) ? chunk_id * 128 * 1024 : 1024 * 1024 * size_multi;

        if (offset + chunk_size > file_size) {
            chunk_size = file_size - offset;
        }

        return chunk_size;
    }
    private volatile long _last_chunk_id_written;
    private volatile long _bytes_written;
    private final long _file_size;
    private final Download _download;
    private final byte[] _byte_file_key;
    private final byte[] _byte_iv;
    private volatile boolean _exit;
    private final Object _secure_notify_lock;
    private boolean _notified;
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

        if (_download.getProgress() == 0) {

            _last_chunk_id_written = 0;

            _bytes_written = 0;

        } else {
            _last_chunk_id_written = _download.getLast_chunk_id_dispatched();

            _bytes_written = _download.getProgress();
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
                    LOG.log(Level.SEVERE, ex.getMessage());
                }
            }

            _notified = false;
        }
    }

    public boolean isExit() {
        return _exit;
    }

    public long getLast_chunk_id_written() {
        return _last_chunk_id_written;
    }

    private String _create_chunks_temp_dir() {

        File chunks_temp_dir = new File((_download.getCustom_chunks_dir() != null ? _download.getCustom_chunks_dir() : _download.getDownload_path()) + "/.mb_chunks_" + _download.getFile_key());

        chunks_temp_dir.mkdirs();

        return chunks_temp_dir.getAbsolutePath();
    }

    public void delete_chunks_temp_dir() {

        try {
            MiscTools.deleteDirectoryRecursion(Paths.get(getChunks_dir()));
        } catch (IOException ex) {
            Logger.getLogger(ChunkWriterManager.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
    }

    @Override
    public void run() {

        LOG.log(Level.INFO, "{0} ChunkWriterManager: let's do some work! {1}", new Object[]{Thread.currentThread().getName(), _download.getFile_name()});
        LOG.log(Level.INFO, "{0} ChunkWriterManager LAST CHUNK WRITTEN -> [{1}] {2} {3}...", new Object[]{Thread.currentThread().getName(), _last_chunk_id_written, _bytes_written, _download.getFile_name()});
        boolean download_finished = false;
        if (_file_size > 0) {
            while (!_exit && (!_download.isStopped() || !_download.getChunkworkers().isEmpty()) && _bytes_written < _file_size) {

                if (!download_finished && _download.getProgress() == _file_size) {

                    _download.getMain_panel().getDownload_manager().getTransference_running_list().remove(_download);
                    _download.getMain_panel().getDownload_manager().secureNotify();

                    _download.getView().printStatusNormal("Download finished. Joining file chunks, please wait...");
                    _download.getView().getPause_button().setVisible(false);
                    _download.getMain_panel().getGlobal_dl_speed().detachTransference(_download);
                    _download.getView().getSpeed_label().setVisible(false);
                    _download.getView().getSlots_label().setVisible(false);
                    _download.getView().getSlot_status_label().setVisible(false);
                    _download.getView().getSlots_spinner().setVisible(false);
                    download_finished = true;
                }

                boolean chunk_io_error;

                do {
                    synchronized (ChunkWriterManager.class) {
                        chunk_io_error = false;

                        try {

                            File chunk_file = new File(getChunks_dir() + "/" + new File(_download.getFile_name()).getName() + ".chunk" + String.valueOf(_last_chunk_id_written + 1));

                            while (chunk_file.exists() && chunk_file.canRead() && chunk_file.canWrite() && chunk_file.length() > 0) {

                                if (!download_finished && _download.getProgress() == _file_size) {

                                    _download.getMain_panel().getDownload_manager().getTransference_running_list().remove(_download);
                                    _download.getMain_panel().getDownload_manager().secureNotify();

                                    _download.getView().printStatusNormal("Download finished. Joining file chunks, please wait...");
                                    _download.getView().getPause_button().setVisible(false);
                                    _download.getMain_panel().getGlobal_dl_speed().detachTransference(_download);
                                    _download.getView().getSpeed_label().setVisible(false);
                                    _download.getView().getSlots_label().setVisible(false);
                                    _download.getView().getSlot_status_label().setVisible(false);
                                    _download.getView().getSlots_spinner().setVisible(false);
                                    download_finished = true;
                                }

                                byte[] buffer = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];

                                int reads;

                                try (CipherInputStream cis = new CipherInputStream(new BufferedInputStream(new FileInputStream(chunk_file)), genDecrypter("AES", "AES/CTR/NoPadding", _byte_file_key, forwardMEGALinkKeyIV(_byte_iv, _bytes_written)))) {
                                    while ((reads = cis.read(buffer)) != -1) {
                                        _download.getOutput_stream().write(buffer, 0, reads);
                                    }
                                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException ex) {
                                    LOG.log(Level.SEVERE, ex.getMessage());
                                }

                                _bytes_written += chunk_file.length();

                                _last_chunk_id_written++;

                                LOG.log(Level.INFO, "{0} ChunkWriterManager has written to disk chunk [{1}] {2} {3} {4}...", new Object[]{Thread.currentThread().getName(), _last_chunk_id_written, _bytes_written, _download.calculateLastWrittenChunk(_bytes_written), _download.getFile_name()});

                                chunk_file.delete();

                                chunk_file = new File(getChunks_dir() + "/" + new File(_download.getFile_name()).getName() + ".chunk" + String.valueOf(_last_chunk_id_written + 1));

                            }
                        } catch (IOException ex) {
                            chunk_io_error = true;
                            LOG.log(Level.WARNING, ex.getMessage());
                            MiscTools.pausar(1000);
                        }
                    }
                } while (chunk_io_error);

                if (!_exit && (!_download.isStopped() || !_download.getChunkworkers().isEmpty()) && _bytes_written < _file_size) {

                    LOG.log(Level.INFO, "{0} ChunkWriterManager waiting for chunk [{1}] {2}...", new Object[]{Thread.currentThread().getName(), _last_chunk_id_written + 1, _download.getFile_name()});

                    secureWait();

                }

            }

            if (_bytes_written == _file_size) {
                delete_chunks_temp_dir();
            }
        }

        _exit = true;

        _download.secureNotify();

        LOG.log(Level.INFO, "{0} ChunkWriterManager: bye bye {1}", new Object[]{Thread.currentThread().getName(), _download.getFile_name()});
    }

}
