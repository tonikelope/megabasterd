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
                    Thread.currentThread().interrupt();
                    LOG.log(Level.FINE, "secureWait interrupted");
                    return;
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

        File chunks_temp_dir = new File((_download.getCustom_chunks_dir() != null ? _download.getCustom_chunks_dir() : _download.getDownload_path()) + "/.MEGABASTERD_CHUNKS_" + MiscTools.HashString("sha1", _download.getUrl()));

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

    private void finishDownload() {
        _download.getMain_panel().getDownload_manager().getTransference_running_list().remove(_download);
        _download.getMain_panel().getDownload_manager().secureNotify();
        _download.getView().printStatusNormal("Download finished. Joining file chunks, please wait...");
        _download.getMain_panel().getGlobal_dl_speed().detachTransference(_download);

        MiscTools.GUIRun(() -> {
            _download.getView().getPause_button().setVisible(false);
            _download.getView().getSpeed_label().setVisible(false);
            _download.getView().getSlots_label().setVisible(false);
            _download.getView().getSlot_status_label().setVisible(false);
            _download.getView().getSlots_spinner().setVisible(false);
        });

    }

    @Override
    public void run() {

        LOG.log(Level.INFO, "{0} ChunkWriterManager: let's do some work! {1}", new Object[]{Thread.currentThread().getName(), _download.getFile_name()});
        LOG.log(Level.INFO, "{0} ChunkWriterManager LAST CHUNK WRITTEN -> [{1}] {2} {3}...", new Object[]{Thread.currentThread().getName(), _last_chunk_id_written, _bytes_written, _download.getFile_name()});
        boolean download_finished = false;
        if (_file_size > 0) {

            try {

                while (!_exit && (!_download.isStopped() || !_download.getChunkworkers().isEmpty()) && _bytes_written < _file_size) {

                    if (!download_finished && _download.getProgress() == _file_size) {

                        finishDownload();
                        download_finished = true;
                    }

                    boolean chunk_io_error;

                    do {

                        chunk_io_error = false;

                        try {

                            long next_chunk_id = _last_chunk_id_written + 1;
                            File chunk_file = new File(getChunks_dir() + "/" + MiscTools.HashString("sha1", _download.getUrl()) + ".chunk" + String.valueOf(next_chunk_id));

                            while (chunk_file.exists() && chunk_file.canRead() && chunk_file.canWrite() && chunk_file.length() > 0) {

                                if (!download_finished && _download.getProgress() == _file_size) {

                                    finishDownload();
                                    download_finished = true;
                                }

                                // Snapshot the file length ONCE per iteration so the
                                // size we feed into CTR-IV advancement matches the
                                // size we'll consume from the file. Reading length()
                                // twice (here and again after the cipher loop) opened
                                // a window where a truncated / partially-written chunk
                                // would advance _bytes_written by a wrong amount,
                                // shifting every subsequent chunk's CTR counter and
                                // breaking the CBC-MAC at the very end (#749).
                                long disk_chunk_size = chunk_file.length();

                                // Validate the on-disk chunk against the size MEGA's
                                // protocol mandates for this chunk_id + file_size +
                                // offset. ChunkDownloader writes ".tmp" and renames
                                // when chunk_reads == chunk_size, so a mismatch here
                                // means the file was truncated / corrupted between
                                // rename and consume (handle leak, AV interception,
                                // partial flush, ...). Treat as a missing chunk:
                                // delete the bad bytes and wait for ChunkDownloader
                                // to re-download via the rejected-id path.
                                long chunk_offset = calculateChunkOffset(next_chunk_id, Download.CHUNK_SIZE_MULTI);
                                long expected_chunk_size = calculateChunkSize(next_chunk_id, _file_size, chunk_offset, Download.CHUNK_SIZE_MULTI);

                                if (disk_chunk_size != expected_chunk_size) {
                                    LOG.log(Level.WARNING, "{0} ChunkWriterManager BAD chunk [{1}] on disk: size={2} expected={3} -- deleting and waiting for re-download {4}",
                                            new Object[]{Thread.currentThread().getName(), next_chunk_id, disk_chunk_size, expected_chunk_size, _download.getFile_name()});

                                    if (!chunk_file.delete()) {
                                        LOG.log(Level.SEVERE, "{0} ChunkWriterManager could NOT delete bad chunk file {1} -- aborting download to avoid silent corruption",
                                                new Object[]{Thread.currentThread().getName(), chunk_file});
                                        _download.stopDownloader("Corrupt chunk file could not be evicted: " + chunk_file.getName());
                                        _exit = true;
                                        return;
                                    }

                                    // The chunk was already produced + counted in
                                    // _partialProgressQueue, so undo the progress
                                    // contribution before requeueing. Without this
                                    // the UI's progress would overshoot the file
                                    // size on retry.
                                    _download.getPartialProgress().add(-disk_chunk_size);
                                    _download.getProgress_meter().secureNotify();

                                    _download.rejectChunkId(next_chunk_id);

                                    // Wake any worker queued on a slot so the
                                    // rejected id gets picked up promptly.
                                    // getChunkworkers() already returns a defensive
                                    // copy under _workers_lock so iteration is safe.
                                    _download.getChunkworkers().forEach(SecureSingleThreadNotifiable::secureNotify);

                                    // Break out of the inner while; the outer
                                    // do/while will re-check the next chunk slot
                                    // and we'll wait on secureWait until the
                                    // re-downloaded chunk lands.
                                    break;
                                }

                                byte[] buffer = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];

                                int reads;

                                try (CipherInputStream cis = new CipherInputStream(new BufferedInputStream(new FileInputStream(chunk_file)), genDecrypter("AES", "AES/CTR/NoPadding", _byte_file_key, forwardMEGALinkKeyIV(_byte_iv, _bytes_written)))) {
                                    while ((reads = cis.read(buffer)) != -1) {
                                        _download.getOutput_stream().write(buffer, 0, reads);
                                    }
                                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException ex) {
                                    // AES init failure is not transient; swallowing it and
                                    // advancing _bytes_written would punch a hole in the output
                                    // file (subsequent chunks would decrypt at the wrong offset
                                    // and the CBC-MAC check would catch it as DAMAGED, but only
                                    // after wasting the whole download). Abort hard instead.
                                    LOG.log(Level.SEVERE, "{0} ChunkWriterManager AES init failed, aborting download: {1}",
                                            new Object[]{Thread.currentThread().getName(), ex.getMessage()});
                                    _download.stopDownloader("AES init failed: " + ex.getMessage());
                                    _exit = true;
                                    return;
                                }

                                _bytes_written += disk_chunk_size;

                                _last_chunk_id_written++;

                                LOG.log(Level.INFO, "{0} ChunkWriterManager has written to disk chunk [{1}] {2} {3} {4}...", new Object[]{Thread.currentThread().getName(), _last_chunk_id_written, _bytes_written, _download.calculateLastWrittenChunk(_bytes_written), _download.getFile_name()});

                                if (!chunk_file.delete()) {
                                    // Not fatal: _last_chunk_id_written has
                                    // already advanced so the next loop iteration
                                    // looks at .chunk{N+1} and won't re-read this
                                    // file. The orphan will linger on disk but
                                    // the output stream is correct.
                                    LOG.log(Level.WARNING, "{0} ChunkWriterManager failed to delete consumed chunk file {1}",
                                            new Object[]{Thread.currentThread().getName(), chunk_file});
                                }

                                next_chunk_id = _last_chunk_id_written + 1;
                                chunk_file = new File(getChunks_dir() + "/" + MiscTools.HashString("sha1", _download.getUrl()) + ".chunk" + String.valueOf(next_chunk_id));
                            }

                        } catch (IOException ex) {
                            chunk_io_error = true;
                            LOG.log(Level.WARNING, ex.getMessage());
                            MiscTools.pausar(1000);
                        }

                    } while (chunk_io_error);

                    if (!_exit && (!_download.isStopped() || !_download.getChunkworkers().isEmpty()) && _bytes_written < _file_size) {

                        LOG.log(Level.INFO, "{0} ChunkWriterManager waiting for chunk [{1}] {2}...", new Object[]{Thread.currentThread().getName(), _last_chunk_id_written + 1, _download.getFile_name()});

                        secureWait();
                    }
                }

            } catch (RuntimeException ex) {
                LOG.log(Level.SEVERE, "{0} ChunkWriterManager unexpected error {1}", new Object[]{Thread.currentThread().getName(), ex.getMessage()});
                throw ex;
            }

            if (_bytes_written == _file_size && MiscTools.isDirEmpty(Paths.get(getChunks_dir()))) {
                delete_chunks_temp_dir();
            }
        }

        _exit = true;

        _download.secureNotify();

        LOG.log(Level.INFO, "{0} ChunkWriterManager: bye bye {1}", new Object[]{Thread.currentThread().getName(), _download.getFile_name()});
    }

}
