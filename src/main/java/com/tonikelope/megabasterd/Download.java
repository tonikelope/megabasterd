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
import static com.tonikelope.megabasterd.DBTools.*;
import static com.tonikelope.megabasterd.MainPanel.*;
import static com.tonikelope.megabasterd.MiscTools.*;
import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Long.valueOf;
import static java.lang.Thread.sleep;
import java.nio.channels.FileChannel;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import static java.util.concurrent.Executors.newCachedThreadPool;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.JComponent;

/**
 *
 * @author tonikelope
 */
public class Download implements Transference, Runnable, SecureSingleThreadNotifiable {

    public static final boolean VERIFY_CBC_MAC_DEFAULT = true;
    public static final boolean USE_SLOTS_DEFAULT = true;
    public static final int WORKERS_DEFAULT = 6;
    public static final boolean USE_MEGA_ACCOUNT_DOWN = false;
    public static final boolean DEFAULT_CLIPBOARD_LINK_MONITOR = true;
    public static final int CHUNK_SIZE_MULTI = 20;
    private static final Logger LOG = Logger.getLogger(Download.class.getName());

    private final MainPanel _main_panel;
    private volatile DownloadView _view;
    private volatile ProgressMeter _progress_meter;
    private final Object _secure_notify_lock;
    private final Object _progress_lock;
    private final Object _workers_lock;
    private final Object _chunkid_lock;
    private final Object _dl_url_lock;
    private final Object _turbo_proxy_lock;
    private volatile boolean _notified;
    private final String _url;
    private final String _download_path;
    private final String _custom_chunks_dir;
    private volatile String _file_name;
    private volatile String _file_key;
    private volatile Long _file_size;
    private volatile String _file_pass;
    private volatile String _file_noexpire;
    private volatile boolean _frozen;
    /**
     * Wall-clock millis of the most recent HTTP 509 seen by ANY worker of this
     * download. Hoisted out of per-worker state so a single worker hitting 509
     * flips SmartProxy mode for all other workers of the same download (the
     * previous per-worker design meant 7 of 8 workers would keep
     * direct-hammering MEGA after worker A had already switched to a proxy). -1
     * == no 509 seen this download. (#751 / C4)
     */
    private volatile long _509_burst_timestamp = -1;
    private final boolean _use_slots;
    private volatile int _slots;
    private final boolean _restart;
    private final ArrayList<ChunkDownloader> _chunkworkers;
    private final ExecutorService _thread_pool;
    private volatile boolean _exit;
    private volatile boolean _pause;
    private final ConcurrentLinkedQueue<Long> _partialProgressQueue;
    private volatile long _progress;
    private volatile ChunkWriterManager _chunkmanager;
    private volatile String _last_download_url;
    private volatile boolean _provision_ok;
    private boolean _auto_retry_on_error;
    private volatile int _paused_workers;
    private File _file;
    private boolean _checking_cbc;
    private boolean _retrying_request;
    private volatile Double _progress_bar_rate;
    private OutputStream _output_stream;
    private String _status_error;
    private final ConcurrentLinkedQueue<Long> _rejectedChunkIds;
    private volatile long _last_chunk_id_dispatched;
    private final MegaAPI _ma;
    private volatile boolean _canceled;
    private volatile boolean _turbo;
    private volatile boolean _closed;
    private volatile boolean _finalizing;
    private boolean _totals_finalized;
    private final Object _progress_watchdog_lock;
    private final boolean _priority;
    private volatile boolean global_cancel = false;

    public void setGlobal_cancel(boolean global_cancel) {
        this.global_cancel = global_cancel;
    }

    public String getStatus_error() {
        return _status_error;
    }

    public Download(MainPanel main_panel, MegaAPI ma, String url, String download_path, String file_name, String file_key, Long file_size, String file_pass, String file_noexpire, boolean use_slots, boolean restart, String custom_chunks_dir, boolean priority) {

        _priority = priority;
        _paused_workers = 0;
        _ma = ma;
        _frozen = main_panel.isInit_paused();
        _last_chunk_id_dispatched = 0L;
        _canceled = false;
        _auto_retry_on_error = true;
        _status_error = null;
        _retrying_request = false;
        _checking_cbc = false;
        _finalizing = false;
        _closed = false;
        _pause = false;
        _exit = false;
        _progress_watchdog_lock = new Object();
        _last_download_url = null;
        _provision_ok = false;
        _progress = 0L;
        _notified = false;
        _main_panel = main_panel;
        _url = url;
        _download_path = download_path;
        _file_name = file_name;
        _file_key = file_key;
        _file_size = file_size;
        _file_pass = file_pass;
        _file_noexpire = file_noexpire;
        _use_slots = use_slots;
        _restart = restart;
        _secure_notify_lock = new Object();
        _progress_lock = new Object();
        _workers_lock = new Object();
        _chunkid_lock = new Object();
        _dl_url_lock = new Object();
        _turbo_proxy_lock = new Object();
        _chunkworkers = new ArrayList<>();
        _partialProgressQueue = new ConcurrentLinkedQueue<>();
        _rejectedChunkIds = new ConcurrentLinkedQueue<>();
        _thread_pool = newCachedThreadPool();
        _view = new DownloadView(this);
        _progress_meter = new ProgressMeter(this);
        _custom_chunks_dir = custom_chunks_dir;
        _turbo = false;
    }

    public Download(Download download) {

        _priority = download.isPriority();
        _paused_workers = 0;
        _ma = download.getMa();
        _last_chunk_id_dispatched = 0L;
        _canceled = false;
        _status_error = null;
        _finalizing = false;
        _retrying_request = false;
        _auto_retry_on_error = true;
        _closed = false;
        _checking_cbc = false;
        _pause = false;
        _exit = false;
        _progress_watchdog_lock = new Object();
        _last_download_url = null;
        _provision_ok = false;
        _progress = 0L;
        _notified = false;
        _main_panel = download.getMain_panel();
        _url = download.getUrl();
        _download_path = download.getDownload_path();
        _file_name = download.getFile_name();
        _file_key = download.getFile_key();
        _file_size = download.getFile_size();
        _file_pass = download.getFile_pass();
        _file_noexpire = download.getFile_noexpire();
        _use_slots = download.getMain_panel().isUse_slots_down();
        _restart = true;
        _secure_notify_lock = new Object();
        _progress_lock = new Object();
        _workers_lock = new Object();
        _chunkid_lock = new Object();
        _dl_url_lock = new Object();
        _turbo_proxy_lock = new Object();
        _chunkworkers = new ArrayList<>();
        _partialProgressQueue = new ConcurrentLinkedQueue<>();
        _rejectedChunkIds = new ConcurrentLinkedQueue<>();
        _thread_pool = newCachedThreadPool();
        _view = new DownloadView(this);
        _progress_meter = new ProgressMeter(this);
        _custom_chunks_dir = download.getCustom_chunks_dir();
        _turbo = false;

    }

    public boolean isPriority() {
        return _priority;
    }

    public boolean isCanceled() {
        return (_canceled && !global_cancel);
    }

    public boolean isTurbo() {
        return _turbo;
    }

    public String getCustom_chunks_dir() {
        return _custom_chunks_dir;
    }

    public long getLast_chunk_id_dispatched() {
        return _last_chunk_id_dispatched;
    }

    public long calculateLastWrittenChunk(long temp_file_size) {
        if (temp_file_size > 3584 * 1024) {
            return 7 + (long) Math.floor((double) (temp_file_size - 3584 * 1024) / (1024 * 1024 * (this.isUse_slots() ? Download.CHUNK_SIZE_MULTI : 1)));
        } else {
            long i = 0, tot = 0;

            while (tot < temp_file_size) {
                i++;
                tot += i * 128 * 1024;
            }

            return i;
        }
    }

    public void disableTurboMode() {
        synchronized (_turbo_proxy_lock) {
            if (_turbo) {
                _turbo = false;
                MiscTools.GUIRun(() -> {

                    getView().getSpeed_label().setForeground(new Color(0, 128, 255));

                });
            }
        }
    }

    public void enableTurboMode() {

        synchronized (_turbo_proxy_lock) {

            if (!_turbo) {

                _turbo = true;

                if (!_finalizing) {
                    Download tthis = this;

                    MiscTools.GUIRun(() -> {

                        getView().getSpeed_label().setForeground(new Color(255, 102, 0));

                    });

                    synchronized (_workers_lock) {

                        for (int t = getChunkworkers().size(); t <= Transference.MAX_WORKERS; t++) {

                            ChunkDownloader c = new ChunkDownloader(t, tthis);

                            _chunkworkers.add(c);

                            _thread_pool.execute(c);
                        }

                    }

                    MiscTools.GUIRun(() -> {
                        getView().getSlots_spinner().setValue(Transference.MAX_WORKERS);

                        getView().getSlots_spinner().setEnabled(true);
                    });
                }
            }

        }

    }

    public ConcurrentLinkedQueue<Long> getRejectedChunkIds() {
        return _rejectedChunkIds;
    }

    public Object getWorkers_lock() {
        return _workers_lock;
    }

    public boolean isChecking_cbc() {
        return _checking_cbc;
    }

    public boolean isRetrying_request() {
        return _retrying_request;
    }

    public boolean isExit() {
        return _exit;
    }

    public boolean isPause() {
        return _pause;
    }

    public void setExit(boolean exit) {
        _exit = exit;
    }

    public void setPause(boolean pause) {
        _pause = pause;
    }

    public ChunkWriterManager getChunkmanager() {
        return _chunkmanager;
    }

    public String getFile_key() {
        return _file_key;
    }

    @Override
    public long getProgress() {
        return _progress;
    }

    public OutputStream getOutput_stream() {
        return _output_stream;
    }

    public ArrayList<ChunkDownloader> getChunkworkers() {

        synchronized (_workers_lock) {
            return new ArrayList<>(_chunkworkers);
        }
    }

    public void setPaused_workers(int paused_workers) {
        _paused_workers = paused_workers;
    }

    public String getUrl() {
        return _url;
    }

    public String getDownload_path() {
        return _download_path;
    }

    @Override
    public String getFile_name() {
        return _file_name;
    }

    public String getFile_pass() {
        return _file_pass;
    }

    public String getFile_noexpire() {
        return _file_noexpire;
    }

    public boolean isUse_slots() {
        return _use_slots;
    }

    public int getSlots() {
        return _slots;
    }

    public void setLast_chunk_id_dispatched(long last_chunk_id_dispatched) {
        _last_chunk_id_dispatched = last_chunk_id_dispatched;
    }

    public boolean isProvision_ok() {
        return _provision_ok;
    }

    @Override
    public ProgressMeter getProgress_meter() {

        while (_progress_meter == null) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException ex) {
                LOG.log(Level.SEVERE, ex.getMessage());
            }
        }

        return _progress_meter;
    }

    @Override
    public DownloadView getView() {

        while (_view == null) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException ex) {
                LOG.log(Level.SEVERE, ex.getMessage());
            }
        }

        return this._view;
    }

    @Override
    public MainPanel getMain_panel() {
        return _main_panel;
    }

    @Override
    public void start() {

        THREAD_POOL.execute(this);
    }

    @Override
    public void stop() {

        if (!isExit()) {
            _canceled = true;
            stopDownloader();
        }
    }

    @Override
    public void pause() {

        if (isPause()) {

            setPause(false);

            setPaused_workers(0);

            synchronized (_workers_lock) {

                getChunkworkers().forEach((downloader) -> {
                    downloader.secureNotify();
                });
            }

            getView().resume();

            _main_panel.getDownload_manager().setPaused_all(false);

        } else {

            setPause(true);

            getView().pause();
        }

        _main_panel.getDownload_manager().secureNotify();
    }

    public MegaAPI getMa() {
        return _ma;
    }

    @Override
    public void restart() {

        Download new_download = new Download(this);

        getMain_panel().getDownload_manager().getTransference_remove_queue().add(this);

        getMain_panel().getDownload_manager().getTransference_provision_queue().add(new_download);

        getMain_panel().getDownload_manager().secureNotify();
    }

    @Override
    public boolean isPaused() {
        return isPause();
    }

    @Override
    public boolean isStopped() {
        return isExit();
    }

    @Override
    public void checkSlotsAndWorkers() {

        if (!isExit() && !this._finalizing) {

            synchronized (_workers_lock) {

                int sl = getView().getSlots();

                int cworkers = getChunkworkers().size();

                if (sl != cworkers) {

                    if (sl > cworkers) {

                        startSlot();

                    } else {

                        stopLastStartedSlot();
                    }
                }
            }
        }
    }

    @Override
    public void close() {

        _closed = true;

        // Always delete from DB when the user closes the row, regardless of
        // whether the download ever successfully provisioned. Previously this
        // was gated on _provision_ok, so a download that failed at provision
        // (expired link, deleted file on MEGA, ...) stayed in the downloads
        // table forever and got resurrected on every restart via
        // resumeDownloads -> selectDownloads. Closes #699.
        try {
            deleteDownload(_url);
        } catch (SQLException ex) {
            LOG.log(SEVERE, null, ex);
        }

        _main_panel.getDownload_manager().getTransference_remove_queue().add(this);

        _main_panel.getDownload_manager().secureNotify();
    }

    @Override
    public ConcurrentLinkedQueue<Long> getPartialProgress() {
        return _partialProgressQueue;
    }

    @Override
    public long getFile_size() {
        return _file_size;
    }

    @Override
    public void run() {

        MiscTools.GUIRun(() -> {
            getView().getQueue_down_button().setVisible(false);
            getView().getQueue_up_button().setVisible(false);
            getView().getQueue_top_button().setVisible(false);
            getView().getQueue_bottom_button().setVisible(false);
            getView().getClose_button().setVisible(false);
            getView().getCopy_link_button().setVisible(true);
            getView().getOpen_folder_button().setVisible(true);
        });

        getView().printStatusNormal("Starting download, please wait...");

        try {

            FileStore fs = Files.getFileStore(Paths.get(_download_path));

            if (fs.getUsableSpace() < _file_size) {
                _status_error = "NO DISK SPACE AVAILABLE!";
                _exit = true;
            }

            if (!_exit) {

                String filename = _download_path + "/" + _file_name;

                _file = new File(filename);

                if (_file.getParent() != null) {
                    File path = new File(_file.getParent());

                    path.mkdirs();
                }

                if (!_file.exists() || _file.length() != _file_size) {

                    if (_file.exists()) {
                        String suffix = "_" + MiscTools.genID(8);
                        int dot = _file_name.lastIndexOf('.');
                        if (dot > 0 && dot > _file_name.length() - 16) {
                            _file_name = _file_name.substring(0, dot) + suffix + _file_name.substring(dot);
                        } else {
                            _file_name = _file_name + suffix;
                        }

                        filename = _download_path + "/" + _file_name;

                        _file = new File(filename);
                    }

                    getView().printStatusNormal("Starting download (retrieving MEGA temp link), please wait...");

                    _last_download_url = getMegaFileDownloadUrl(_url);

                    if (!_exit) {

                        String temp_filename = (getCustom_chunks_dir() != null ? getCustom_chunks_dir() : _download_path) + "/" + _file_name + ".mctemp";

                        _file = new File(temp_filename);

                        if (_file.getParent() != null) {
                            File path = new File(_file.getParent());

                            path.mkdirs();
                        }

                        if (_file.exists()) {
                            getView().printStatusNormal("File exists, resuming download...");

                            long max_size = calculateMaxTempFileSize(_file.length());

                            if (max_size != _file.length()) {

                                LOG.log(Level.INFO, "{0} Downloader truncating mctemp file {1} -> {2} ", new Object[]{Thread.currentThread().getName(), _file.length(), max_size});

                                getView().printStatusNormal("Truncating temp file...");

                                try (FileChannel out_truncate = new FileOutputStream(temp_filename, true).getChannel()) {
                                    out_truncate.truncate(max_size);
                                }
                            }

                            setProgress(_file.length());

                            _last_chunk_id_dispatched = calculateLastWrittenChunk(_progress);

                        } else {
                            setProgress(0);
                        }

                        _output_stream = new BufferedOutputStream(new FileOutputStream(_file, (_progress > 0)));

                        _thread_pool.execute(getProgress_meter());

                        getMain_panel().getGlobal_dl_speed().attachTransference(this);

                        synchronized (_workers_lock) {

                            if (_use_slots) {

                                _chunkmanager = new ChunkWriterManager(this);

                                _thread_pool.execute(_chunkmanager);

                                _slots = getMain_panel().getDefault_slots_down();

                                final int initial_slots = _slots;
                                MiscTools.GUIRun(() -> _view.getSlots_spinner().setValue(initial_slots));

                                for (int t = 1; t <= _slots; t++) {
                                    ChunkDownloader c = new ChunkDownloader(t, this);

                                    _chunkworkers.add(c);

                                    _thread_pool.execute(c);
                                }

                                MiscTools.GUIRun(() -> {
                                    for (JComponent c : new JComponent[]{getView().getSlots_label(), getView().getSlots_spinner(), getView().getSlot_status_label()}) {

                                        c.setVisible(true);
                                    }
                                });

                            } else {

                                ChunkDownloaderMono c = new ChunkDownloaderMono(this);

                                _chunkworkers.add(c);

                                _thread_pool.execute(c);

                                MiscTools.GUIRun(() -> {
                                    for (JComponent c1 : new JComponent[]{getView().getSlots_label(), getView().getSlots_spinner(), getView().getSlot_status_label()}) {
                                        c1.setVisible(false);
                                    }
                                });
                            }
                        }

                        getView().printStatusNormal(LabelTranslatorSingleton.getInstance().translate("Downloading file from mega ") + (_ma.getFull_email() != null ? "(" + _ma.getFull_email() + ")" : "") + " ...");

                        MiscTools.GUIRun(() -> {
                            for (JComponent c : new JComponent[]{getView().getPause_button(), getView().getProgress_pbar()}) {

                                c.setVisible(true);
                            }
                        });

                        THREAD_POOL.execute(() -> {

                            // PROGRESS WATCHDOG. Slices its wait into 10 s chunks
                            // so it can pick the right stall-timeout based on
                            // live worker state: when at least one chunk worker
                            // is in 509 backoff, use the shorter
                            // QUOTA_STALL_TIMEOUT_DEFAULT (default 180 s) instead
                            // of the generic 600 s. Quota stalls almost always
                            // clear within a minute once the user activates a
                            // VPN; making them wait 10 min for the auto-retry
                            // path is a bad default. (#751)
                            LOG.log(Level.INFO, "{0} PROGRESS WATCHDOG HELLO!", Thread.currentThread().getName());

                            final long generic_timeout_ms = PROGRESS_WATCHDOG_TIMEOUT * 1000L;
                            String quota_stall_setting = selectSettingValue("quota_stall_timeout");
                            int quota_stall_secs = QUOTA_STALL_TIMEOUT_DEFAULT;
                            if (quota_stall_setting != null) {
                                try {
                                    int v = Integer.parseInt(quota_stall_setting);
                                    if (v >= 30 && v <= 3600) {
                                        quota_stall_secs = v;
                                    }
                                } catch (NumberFormatException ignore) {
                                }
                            }
                            final long quota_timeout_ms = quota_stall_secs * 1000L;

                            long last_progress = getProgress();
                            long stall_started_ms = System.currentTimeMillis();
                            String triggered_reason = null;

                            while (!isExit() && !_thread_pool.isShutdown() && getProgress() < getFile_size()) {

                                synchronized (_progress_watchdog_lock) {
                                    try {
                                        _progress_watchdog_lock.wait(10_000L);
                                    } catch (InterruptedException ex) {
                                        Thread.currentThread().interrupt();
                                        Logger.getLogger(Download.class.getName()).log(Level.FINE, "watchdog wait interrupted");
                                        break;
                                    }
                                }

                                if (isPaused()) {
                                    // Don't accumulate stall time while paused.
                                    stall_started_ms = System.currentTimeMillis();
                                    last_progress = getProgress();
                                    continue;
                                }

                                long current_progress = getProgress();
                                if (current_progress > last_progress) {
                                    stall_started_ms = System.currentTimeMillis();
                                    last_progress = current_progress;
                                    continue;
                                }

                                // No progress this slice. Pick the timeout.
                                boolean any_509 = false;
                                synchronized (_workers_lock) {
                                    for (ChunkDownloader cd : _chunkworkers) {
                                        if (cd.isIn_509_backoff()) {
                                            any_509 = true;
                                            break;
                                        }
                                    }
                                }

                                long timeout_ms = any_509 ? quota_timeout_ms : generic_timeout_ms;
                                long stalled_ms = System.currentTimeMillis() - stall_started_ms;

                                if (stalled_ms >= timeout_ms) {
                                    if (any_509) {
                                        triggered_reason = "QUOTA STALL TIMEOUT (no progress for " + (stalled_ms / 1000) + "s on 509)";
                                    } else {
                                        triggered_reason = "PROGRESS WATCHDOG TIMEOUT!";
                                    }
                                    break;
                                }
                            }

                            if (triggered_reason != null && !isExit() && !_thread_pool.isShutdown() && _status_error == null && getProgress() < getFile_size()) {
                                // Flag for auto-restart so the download re-arms itself a few seconds
                                // after the watchdog fires. The most common cause of a stale-progress
                                // timeout is MEGA HTTP 509 (bandwidth quota) -- the user often clears
                                // it by switching IP (VPN). Without auto-retry the download stays in
                                // FATAL ERROR until the user manually clicks Restart on every row
                                // (or restarts the whole app to re-provision from the queue). (#751)
                                _auto_retry_on_error = true;
                                stopDownloader(triggered_reason);
                            }

                            LOG.log(Level.INFO, "{0} PROGRESS WATCHDOG BYE BYE!", Thread.currentThread().getName());

                        });

                        secureWait();

                        LOG.log(Level.INFO, "{0} Chunkdownloaders finished!", Thread.currentThread().getName());

                        getProgress_meter().setExit(true);

                        getProgress_meter().secureNotify();

                        try {

                            _thread_pool.shutdown();

                            LOG.log(Level.INFO, "{0} Waiting all threads to finish...", Thread.currentThread().getName());

                            _thread_pool.awaitTermination(MAX_WAIT_WORKERS_SHUTDOWN, TimeUnit.SECONDS);

                        } catch (InterruptedException ex) {
                            LOG.log(Level.SEVERE, ex.getMessage());
                        }

                        if (!_thread_pool.isTerminated()) {

                            LOG.log(Level.INFO, "{0} Closing thread pool ''mecag\u00fcen'' style...", Thread.currentThread().getName());

                            _thread_pool.shutdownNow();
                        }

                        LOG.log(Level.INFO, "{0} Downloader thread pool finished!", Thread.currentThread().getName());

                        getMain_panel().getGlobal_dl_speed().detachTransference(this);

                        _output_stream.close();

                        MiscTools.GUIRun(() -> {
                            for (JComponent c : new JComponent[]{getView().getSpeed_label(), getView().getPause_button(), getView().getStop_button(), getView().getSlots_label(), getView().getSlots_spinner(), getView().getKeep_temp_checkbox()}) {

                                c.setVisible(false);
                            }
                        });

                        if (_progress == _file_size) {

                            if (_file.length() != _file_size) {

                                throw new IOException("El tamaño del fichero es incorrecto!");
                            }

                            Files.move(Paths.get(_file.getAbsolutePath()), Paths.get(filename), StandardCopyOption.REPLACE_EXISTING);

                            if (_custom_chunks_dir != null) {

                                File temp_parent_download_dir = new File(temp_filename).getParentFile();

                                while (!temp_parent_download_dir.getAbsolutePath().equals(_custom_chunks_dir) && temp_parent_download_dir.listFiles().length == 0) {
                                    temp_parent_download_dir.delete();
                                    temp_parent_download_dir = temp_parent_download_dir.getParentFile();
                                }

                            }

                            String verify_file = selectSettingValue("verify_down_file");

                            boolean do_verify = (verify_file == null) ? VERIFY_CBC_MAC_DEFAULT : verify_file.equals("yes");

                            if (do_verify) {
                                _checking_cbc = true;

                                getView().printStatusNormal("Waiting to check file integrity...");

                                setProgress(0);

                                getView().printStatusNormal("Checking file integrity, please wait...");

                                MiscTools.GUIRun(() -> {
                                    getView().getStop_button().setVisible(true);

                                    getView().getStop_button().setText(LabelTranslatorSingleton.getInstance().translate("CANCEL CHECK"));
                                });

                                getMain_panel().getDownload_manager().getTransference_running_list().remove(this);

                                getMain_panel().getDownload_manager().secureNotify();

                                if (verifyFileCBCMAC(filename)) {

                                    getView().printStatusOK("File successfully downloaded! (Integrity check PASSED)");

                                } else if (!_exit) {

                                    _status_error = "BAD NEWS :( File is DAMAGED! (deleted)";

                                    try {
                                        java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(filename));
                                    } catch (IOException del_ex) {
                                        LOG.log(Level.SEVERE, "Failed to delete corrupted file {0}: {1}", new Object[]{filename, del_ex.getMessage()});
                                    }

                                    getView().printStatusError(_status_error);

                                } else {

                                    getView().printStatusOK("File successfully downloaded! (but integrity check CANCELED)");

                                }

                                MiscTools.GUIRun(() -> {
                                    getView().getStop_button().setVisible(false);
                                });

                            } else {

                                getView().printStatusOK("File successfully downloaded!");

                            }

                        } else if (_status_error != null) {

                            getView().hideAllExceptStatus();

                            getView().printStatusError(_status_error);

                        } else if (_canceled) {

                            getView().hideAllExceptStatus();

                            getView().printStatusNormal("Download CANCELED!");

                        } else {

                            getView().hideAllExceptStatus();

                            _status_error = "UNEXPECTED ERROR!";

                            getView().printStatusError(_status_error);
                        }

                    } else if (_status_error != null) {

                        getView().hideAllExceptStatus();

                        getView().printStatusError(_status_error != null ? _status_error : "ERROR");

                    } else if (_canceled) {

                        getView().hideAllExceptStatus();

                        getView().printStatusNormal("Download CANCELED!");

                    } else {

                        getView().hideAllExceptStatus();

                        _status_error = "UNEXPECTED ERROR!";

                        getView().printStatusError(_status_error);
                    }

                } else {
                    getView().hideAllExceptStatus();

                    _status_error = "FILE WITH SAME NAME AND SIZE ALREADY EXISTS";

                    _auto_retry_on_error = false;

                    getView().printStatusError(_status_error);
                }

            } else if (_status_error != null) {

                getView().hideAllExceptStatus();

                getView().printStatusError(_status_error);

            } else if (_canceled) {

                getView().hideAllExceptStatus();

                getView().printStatusNormal("Download CANCELED!");

            } else {

                getView().hideAllExceptStatus();

                _status_error = "UNEXPECTED ERROR!";

                getView().printStatusError(_status_error);
            }

        } catch (Exception ex) {
            _status_error = "I/O ERROR " + ex.getMessage();

            getView().printStatusError(_status_error);

            LOG.log(Level.SEVERE, ex.getMessage());
        } finally {

            if (_output_stream != null) {
                try {
                    _output_stream.close();
                } catch (IOException close_ex) {
                    LOG.log(Level.FINE, "Closing output stream on exit: {0}", close_ex.getMessage());
                }
            }

            try {
                getMain_panel().getGlobal_dl_speed().detachTransference(this);
            } catch (Exception ignore) {
            }
        }

        if (_file != null && !getView().isKeepTempFileSelected()) {
            _file.delete();

            if (getChunkmanager() != null) {

                getChunkmanager().delete_chunks_temp_dir();

                File parent_download_dir = new File(getDownload_path() + "/" + getFile_name()).getParentFile();

                while (!parent_download_dir.getAbsolutePath().equals(getDownload_path()) && parent_download_dir.listFiles().length == 0) {
                    parent_download_dir.delete();
                    parent_download_dir = parent_download_dir.getParentFile();
                }

                if (!(new File(getDownload_path() + "/" + getFile_name()).getParentFile().exists())) {

                    getView().getOpen_folder_button().setEnabled(false);
                }
            }
        }

        if ((_status_error == null && !_canceled) || global_cancel || !_auto_retry_on_error) {

            try {
                deleteDownload(_url);
            } catch (SQLException ex) {
                LOG.log(SEVERE, null, ex);
            }

        }

        getMain_panel().getDownload_manager().getTransference_running_list().remove(this);

        getMain_panel().getDownload_manager().getTransference_finished_queue().add(this);

        MiscTools.GUIRun(() -> {
            getMain_panel().getDownload_manager().getScroll_panel().remove(getView());

            getMain_panel().getDownload_manager().getScroll_panel().add(getView());
        });

        getMain_panel().getDownload_manager().secureNotify();

        MiscTools.GUIRun(() -> {
            getView().getClose_button().setVisible(true);

            if ((_status_error != null || _canceled) && isProvision_ok() && !global_cancel) {

                getView().getRestart_button().setVisible(true);

            } else if (!global_cancel) {

                getView().getClose_button().setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-ok-30.png")));
            }
        });

        if (_status_error != null && !_canceled && _auto_retry_on_error) {
            THREAD_POOL.execute(() -> {
                for (int i = 3; !_closed && i > 0; i--) {
                    final int j = i;
                    MiscTools.GUIRun(() -> {
                        getView().getRestart_button().setText("Restart (" + String.valueOf(j) + " secs...)");
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        LOG.log(Level.SEVERE, ex.getMessage());
                    }
                }
                if (!_closed) {
                    LOG.log(Level.INFO, "{0} Downloader {1} AUTO RESTARTING DOWNLOAD...", new Object[]{Thread.currentThread().getName(), getFile_name()});
                    restart();
                }
            });
        } else {
            getMain_panel().getDownload_manager().setAll_finished(false);
        }

        _exit = true;

        if (_status_error != null && !_canceled && getMain_panel().getDownload_manager().no_transferences() && getMain_panel().getUpload_manager().no_transferences() && (!getMain_panel().getDownload_manager().getTransference_finished_queue().isEmpty() || !getMain_panel().getUpload_manager().getTransference_finished_queue().isEmpty()) && getMain_panel().getView().getAuto_close_menu().isSelected()) {
            System.exit(0);
        }

        synchronized (_progress_watchdog_lock) {
            _progress_watchdog_lock.notifyAll();
        }

        LOG.log(Level.INFO, "{0}{1} Downloader: bye bye", new Object[]{Thread.currentThread().getName(), _file_name});
    }

    public void provisionIt(boolean retry) throws APIException {

        getView().printStatusNormal("Provisioning download, please wait...");

        MiscTools.GUIRun(() -> {
            getView().getCopy_link_button().setVisible(true);
            getView().getOpen_folder_button().setVisible(true);
        });

        String[] file_info;

        _provision_ok = false;

        try {
            if (_file_name == null) {

                //New single file links
                file_info = getMegaFileMetadata(_url, getMain_panel().getView(), retry);

                if (file_info != null) {

                    _file_name = file_info[0];

                    _file_size = valueOf(file_info[1]);

                    _file_key = file_info[2];

                    if (file_info.length == 5) {

                        _file_pass = file_info[3];

                        _file_noexpire = file_info[4];
                    }

                    String filename = _download_path + "/" + _file_name;

                    File file = new File(filename);

                    if (file.exists() && file.length() != _file_size) {
                        _file_name = _file_name.replaceFirst("\\..*$", "_" + MiscTools.genID(8) + "_$0");
                    }

                    if (_closed) {
                        // User clicked Close while we were still provisioning.
                        // Don't re-insert the row we're about to delete.
                        return;
                    }

                    try {

                        // Use returning-name variant: if insertDownload had to
                        // pick a different filename to dodge UNIQUE(path,filename),
                        // we MUST adopt that name -- otherwise the DB row and the
                        // file we'll write to disk disagree and a sibling download
                        // with the same display name clobbers the same .mctemp.
                        _file_name = DBTools.insertDownloadReturningName(_url, _ma.getFull_email(), _download_path, _file_name, _file_key, _file_size, _file_pass, _file_noexpire, _custom_chunks_dir);

                        _provision_ok = true;

                    } catch (SQLException ex) {

                        _status_error = "Error registering download: " + ex.getMessage() + " file is already downloading?";
                    }

                }
            } else {

                String filename = _download_path + "/" + _file_name;

                File file = new File(filename);

                File temp_file = new File(filename + ".mctemp");

                if (file.exists() && !temp_file.exists() && file.length() != _file_size) {
                    _file_name = _file_name.replaceFirst("\\..*$", "_" + MiscTools.genID(8) + "_$0");
                }

                if (_closed) {
                    // User clicked Close while we were still provisioning.
                    return;
                }

                //Resuming single file links and new/resuming folder links
                try {

                    deleteDownload(_url); //If resuming

                    _file_name = DBTools.insertDownloadReturningName(_url, _ma.getFull_email(), _download_path, _file_name, _file_key, _file_size, _file_pass, _file_noexpire, _custom_chunks_dir);

                    _provision_ok = true;

                } catch (SQLException ex) {

                    _status_error = "Error registering download: " + ex.getMessage();

                }
            }

        } catch (APIException ex) {

            throw ex;

        } catch (NumberFormatException ex) {

            _status_error = ex.getMessage();
        }

        if (!_provision_ok) {

            if (_status_error == null) {
                _status_error = "PROVISION FAILED";
            }

            if (_file_name != null) {
                MiscTools.GUIRun(() -> {
                    getView().getFile_name_label().setVisible(true);

                    getView().getFile_name_label().setText(truncateText(new File(_download_path + "/" + _file_name).getName(), 150));

                    getView().getFile_name_label().setToolTipText(_download_path + "/" + _file_name);

                    getView().getFile_size_label().setVisible(true);

                    getView().getFile_size_label().setText(formatBytes(_file_size));
                });
            }

            getView().hideAllExceptStatus();

            getView().printStatusError(_status_error);

            MiscTools.GUIRun(() -> {
                getView().getClose_button().setVisible(true);
            });

        } else {

            _progress_bar_rate = MAX_VALUE / (double) _file_size;

            getView().printStatusNormal(_frozen ? "(FROZEN) Waiting to start..." : "Waiting to start...");

            MiscTools.GUIRun(() -> {
                getView().getFile_name_label().setVisible(true);

                getView().getFile_name_label().setText(truncateText(new File(_download_path + "/" + _file_name).getName(), 150));

                getView().getFile_name_label().setToolTipText(_download_path + "/" + _file_name);

                getView().getFile_size_label().setVisible(true);

                getView().getFile_size_label().setText(formatBytes(_file_size));
            });

            MiscTools.GUIRun(() -> {
                getView().getClose_button().setVisible(true);
                getView().getQueue_up_button().setVisible(true);
                getView().getQueue_down_button().setVisible(true);
                getView().getQueue_top_button().setVisible(true);
                getView().getQueue_bottom_button().setVisible(true);
            });

        }

    }

    public void pause_worker() {

        synchronized (_workers_lock) {

            if (++_paused_workers >= _chunkworkers.size() && !_exit) {

                getView().printStatusNormal("Download paused!");

                MiscTools.GUIRun(() -> {
                    getView().getPause_button().setText(LabelTranslatorSingleton.getInstance().translate("RESUME DOWNLOAD"));
                    getView().getPause_button().setEnabled(true);
                });

            }
        }
    }

    public void pause_worker_mono() {

        getView().printStatusNormal("Download paused!");

        MiscTools.GUIRun(() -> {
            getView().getPause_button().setText(LabelTranslatorSingleton.getInstance().translate("RESUME DOWNLOAD"));
            getView().getPause_button().setEnabled(true);
        });

    }

    public String getDownloadUrlForWorker() {

        synchronized (_dl_url_lock) {

            if (_last_download_url != null && checkMegaDownloadUrl(_last_download_url)) {

                return _last_download_url;
            }

            // Cap the worker-url refetch loop. Without this, a permanently
            // broken MEGA link / network outage kept this synchronized block
            // spinning forever -- and every other worker queued on
            // _dl_url_lock, so the user could not close the download even
            // after clicking Stop. 32 retries with exp backoff gives ~tens
            // of minutes before giving up, which is more than enough for
            // any transient hiccup.
            final int MAX_URL_RETRY = 32;

            boolean error;

            int conta_error = 0;

            String download_url;

            do {

                if (_exit || _main_panel.isExit()) {
                    return _last_download_url;
                }

                error = false;

                try {
                    if (findFirstRegex("://mega(\\.co)?\\.nz/", _url, 0) != null) {

                        download_url = _ma.getMegaFileDownloadUrl(_url);

                    } else {
                        download_url = MegaCrypterAPI.getMegaFileDownloadUrl(_url, _file_pass, _file_noexpire, _ma.getSid(), getMain_panel().getMega_proxy_server() != null ? (getMain_panel().getMega_proxy_server().getPort() + ":" + Bin2BASE64(("megacrypter:" + getMain_panel().getMega_proxy_server().getPassword()).getBytes("UTF-8")) + ":" + MiscTools.getMyPublicIP()) : null);
                    }

                    if (checkMegaDownloadUrl(download_url)) {

                        _last_download_url = download_url;

                    } else {

                        error = true;
                    }

                } catch (Exception ex) {

                    error = true;

                    if (++conta_error >= MAX_URL_RETRY) {
                        LOG.log(Level.SEVERE, "{0} getDownloadUrlForWorker giving up after {1} retries: {2}",
                                new Object[]{Thread.currentThread().getName(), conta_error, ex.getMessage()});
                        return null;
                    }

                    try {
                        Thread.sleep(getWaitTimeExpBackOff(conta_error) * 1000);
                    } catch (InterruptedException ex2) {
                        Thread.currentThread().interrupt();
                        return _last_download_url;
                    }
                }

            } while (error);

            return _last_download_url;

        }
    }

    public void startSlot() {

        if (!_exit) {

            synchronized (_workers_lock) {

                int chunk_id = _chunkworkers.size() + 1;

                ChunkDownloader c = new ChunkDownloader(chunk_id, this);

                _chunkworkers.add(c);

                try {

                    _thread_pool.execute(c);

                } catch (java.util.concurrent.RejectedExecutionException e) {
                    LOG.log(Level.INFO, e.getMessage());
                }
            }
        }
    }

    public void stopLastStartedSlot() {

        if (!_exit) {

            synchronized (_workers_lock) {

                if (!_chunkworkers.isEmpty()) {

                    MiscTools.GUIRun(() -> {
                        getView().getSlots_spinner().setEnabled(false);
                    });

                    int i = _chunkworkers.size() - 1;

                    while (i >= 0) {

                        ChunkDownloader chundownloader = _chunkworkers.get(i);

                        if (!chundownloader.isExit()) {

                            chundownloader.setExit(true);

                            chundownloader.secureNotify();

                            _view.updateSlotsStatus();

                            break;

                        } else {

                            i--;
                        }
                    }
                }
            }
        }
    }

    public void stopThisSlot(ChunkDownloader chunkdownloader) {

        synchronized (_workers_lock) {

            if (_chunkworkers.remove(chunkdownloader) && !_exit) {

                if (_use_slots) {

                    if (chunkdownloader.isChunk_exception() || getMain_panel().isExit()) {

                        _finalizing = true;

                        MiscTools.GUIRun(() -> {
                            getView().getSlots_spinner().setEnabled(false);

                            getView().getSlots_spinner().setValue((int) getView().getSlots_spinner().getValue() - 1);
                        });

                    } else if (!_finalizing) {
                        MiscTools.GUIRun(() -> {
                            getView().getSlots_spinner().setEnabled(true);
                        });
                    }

                    getView().updateSlotsStatus();
                }

                if (!_exit && isPause() && _paused_workers >= _chunkworkers.size()) {

                    getView().printStatusNormal("Download paused!");

                    MiscTools.GUIRun(() -> {
                        getView().getPause_button().setText(LabelTranslatorSingleton.getInstance().translate("RESUME DOWNLOAD"));

                        getView().getPause_button().setEnabled(true);
                    });

                }
            }
        }

    }

    private boolean verifyFileCBCMAC(String filename) throws FileNotFoundException, Exception, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        int old_thread_priority = Thread.currentThread().getPriority();

        int[] int_key = bin2i32a(UrlBASE642Bin(_file_key));
        int[] iv = new int[]{int_key[4], int_key[5]};
        int[] meta_mac = new int[]{int_key[6], int_key[7]};
        int[] file_mac = {0, 0, 0, 0};
        int[] cbc_iv = {0, 0, 0, 0};

        byte[] byte_file_key = initMEGALinkKey(getFile_key());

        Cipher cryptor = genCrypter("AES", "AES/CBC/NoPadding", byte_file_key, i32a2bin(cbc_iv));

        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(filename))) {

            long chunk_id = 1L;
            long tot = 0L;
            byte[] byte_block = new byte[16];
            int[] int_block;
            int reads;
            int[] chunk_mac = new int[4];

            try {
                while (!_exit) {

                    long chunk_offset = ChunkWriterManager.calculateChunkOffset(chunk_id, 1);

                    long chunk_size = ChunkWriterManager.calculateChunkSize(chunk_id, this.getFile_size(), chunk_offset, 1);

                    ChunkWriterManager.checkChunkID(chunk_id, this.getFile_size(), chunk_offset);

                    tot += chunk_size;

                    chunk_mac[0] = iv[0];
                    chunk_mac[1] = iv[1];
                    chunk_mac[2] = iv[0];
                    chunk_mac[3] = iv[1];

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
                    }

                    for (int i = 0; i < file_mac.length; i++) {
                        file_mac[i] ^= chunk_mac[i];
                    }

                    file_mac = bin2i32a(cryptor.doFinal(i32a2bin(file_mac)));

                    setProgress(tot);

                    chunk_id++;

                }

            } catch (ChunkInvalidException e) {

            }

            int[] cbc = {file_mac[0] ^ file_mac[1], file_mac[2] ^ file_mac[3]};

            return (cbc[0] == meta_mac[0] && cbc[1] == meta_mac[1]);
        }
    }

    public void stopDownloader() {

        if (!_exit) {

            _exit = true;

            if (isRetrying_request()) {

                getView().stop("Retrying cancelled! " + truncateText(_url, 80));

            } else if (isChecking_cbc()) {

                getView().stop("Verification cancelled! " + truncateText(_file_name, 80));

            } else {

                getView().stop("Stopping download, please wait...");

                synchronized (_workers_lock) {

                    _chunkworkers.forEach((downloader) -> {
                        downloader.secureNotify();
                    });
                }

                secureNotify();
            }
        }
    }

    public void stopDownloader(String reason) {

        _status_error = (reason != null ? LabelTranslatorSingleton.getInstance().translate("FATAL ERROR! ") + reason : LabelTranslatorSingleton.getInstance().translate("FATAL ERROR! "));

        stopDownloader();
    }

    public long calculateMaxTempFileSize(long size) {
        if (size > 3584 * 1024) {
            long reminder = (size - 3584 * 1024) % (1024 * 1024 * (isUse_slots() ? Download.CHUNK_SIZE_MULTI : 1));

            return reminder == 0 ? size : (size - reminder);
        } else {
            long i = 0, tot = 0;

            while (tot < size) {
                i++;
                tot += i * 128 * 1024;
            }

            return tot == size ? size : (tot - i * 128 * 1024);
        }
    }

    public String[] getMegaFileMetadata(String link, MainPanelView panel, boolean retry_request) throws APIException {

        String[] file_info = null;
        int retry = 0, error_code;
        boolean error;

        do {
            error = false;

            try {

                if (findFirstRegex("://mega(\\.co)?\\.nz/", link, 0) != null) {

                    file_info = _ma.getMegaFileMetadata(link);

                } else {

                    file_info = MegaCrypterAPI.getMegaFileMetadata(link, panel, getMain_panel().getMega_proxy_server() != null ? (getMain_panel().getMega_proxy_server().getPort() + ":" + Bin2BASE64(("megacrypter:" + getMain_panel().getMega_proxy_server().getPassword()).getBytes("UTF-8"))) : null);
                }

            } catch (APIException ex) {

                error = true;

                _status_error = ex.getMessage();

                error_code = ex.getCode();

                if (error_code == -16) {
                    _status_error = "ERROR: MEGA FILE BLOCKED/DELETED";
                }

                if (Arrays.asList(FATAL_API_ERROR_CODES).contains(error_code)) {

                    _auto_retry_on_error = Arrays.asList(FATAL_API_ERROR_CODES_WITH_RETRY).contains(error_code);

                    // Surface a friendly explanation popup. Dedup'd so two
                    // downloads hitting the same -16 / -8 don't pop twice.
                    // Source.LINK so -9 / -16 etc. get the link-context copy
                    // ("file deleted / blocked") instead of the
                    // account-context one ("account doesn't exist"). (#751 / D)
                    MegaErrorMessages.showPopup(getMain_panel().getView(), error_code, link,
                            "while fetching MEGA file metadata",
                            MegaErrorMessages.Source.LINK);

                    stopDownloader(error_code == -16 ? _status_error : ex.getMessage() + " " + truncateText(link, 80));

                } else {

                    if (!retry_request) {

                        throw ex;
                    }

                    _retrying_request = true;

                    MiscTools.GUIRun(() -> {

                        getView().getStop_button().setVisible(true);

                        getView().getStop_button().setText(LabelTranslatorSingleton.getInstance().translate("CANCEL RETRY"));
                    });

                    for (long i = getWaitTimeExpBackOff(retry++); i > 0 && !_exit; i--) {
                        if (error_code == -18) {
                            getView().printStatusError(LabelTranslatorSingleton.getInstance().translate("File temporarily unavailable! (Retrying in ") + i + LabelTranslatorSingleton.getInstance().translate(" secs...)"));
                        } else {
                            getView().printStatusError("Mega/MC APIException error " + ex.getMessage() + LabelTranslatorSingleton.getInstance().translate(" (Retrying in ") + i + LabelTranslatorSingleton.getInstance().translate(" secs...)"));
                        }

                        try {
                            sleep(1000);
                        } catch (InterruptedException ex2) {
                        }
                    }
                }

            } catch (Exception ex) {

                if (!(ex instanceof APIException)) {
                    stopDownloader("Mega link is not valid! " + truncateText(link, 80));
                }
            }

        } while (!_exit && error);

        if (!_exit && !error) {

            _auto_retry_on_error = true;

            MiscTools.GUIRun(() -> {
                getView().getStop_button().setText(LabelTranslatorSingleton.getInstance().translate("CANCEL DOWNLOAD"));
                getView().getStop_button().setVisible(false);
            });

        }

        _retrying_request = false;

        return file_info;

    }

    public String getMegaFileDownloadUrl(String link) throws IOException, InterruptedException {

        String dl_url = null;
        int retry = 0, error_code;
        boolean error;

        do {
            error = false;

            try {
                if (findFirstRegex("://mega(\\.co)?\\.nz/", _url, 0) != null) {

                    dl_url = _ma.getMegaFileDownloadUrl(link);

                } else {
                    dl_url = MegaCrypterAPI.getMegaFileDownloadUrl(link, _file_pass, _file_noexpire, _ma.getSid(), getMain_panel().getMega_proxy_server() != null ? (getMain_panel().getMega_proxy_server().getPort() + ":" + Bin2BASE64(("megacrypter:" + getMain_panel().getMega_proxy_server().getPassword()).getBytes("UTF-8")) + ":" + MiscTools.getMyPublicIP()) : null);
                }

            } catch (APIException ex) {
                error = true;

                error_code = ex.getCode();

                if (error_code == -16) {
                    _status_error = "ERROR: MEGA FILE BLOCKED/DELETED";
                }

                if (Arrays.asList(FATAL_API_ERROR_CODES).contains(error_code)) {

                    _auto_retry_on_error = Arrays.asList(FATAL_API_ERROR_CODES_WITH_RETRY).contains(error_code);

                    // Surface a friendly explanation popup. Dedup'd so two
                    // downloads hitting the same -16 / -8 don't pop twice.
                    // Source.LINK so -9 / -16 etc. get the link-context copy
                    // ("file deleted / blocked") instead of the
                    // account-context one ("account doesn't exist"). (#751 / D)
                    MegaErrorMessages.showPopup(getMain_panel().getView(), error_code, link,
                            "while fetching MEGA file metadata",
                            MegaErrorMessages.Source.LINK);

                    stopDownloader(error_code == -16 ? _status_error : ex.getMessage() + " " + truncateText(link, 80));

                } else {

                    _retrying_request = true;

                    MiscTools.GUIRun(() -> {
                        getView().getStop_button().setVisible(true);

                        getView().getStop_button().setText(LabelTranslatorSingleton.getInstance().translate("CANCEL RETRY"));
                    });

                    for (long i = getWaitTimeExpBackOff(retry++); i > 0 && !_exit; i--) {
                        if (error_code == -18) {
                            getView().printStatusError("File temporarily unavailable! (Retrying in " + i + " secs...)");
                        } else {
                            getView().printStatusError("Mega/MC APIException error " + ex.getMessage() + " (Retrying in " + i + " secs...)");
                        }

                        try {
                            sleep(1000);
                        } catch (InterruptedException ex2) {
                        }
                    }
                }
            }

        } while (!_exit && error);

        if (!_exit && !error) {

            _auto_retry_on_error = true;

            MiscTools.GUIRun(() -> {
                getView().getStop_button().setText(LabelTranslatorSingleton.getInstance().translate("CANCEL DOWNLOAD"));
                getView().getStop_button().setVisible(false);
            });

        }

        _retrying_request = false;

        return dl_url;
    }

    public long nextChunkId() throws ChunkInvalidException {

        synchronized (_chunkid_lock) {

            if (_main_panel.isExit()) {
                throw new ChunkInvalidException(null);
            }

            Long next_id;

            if ((next_id = _rejectedChunkIds.poll()) != null) {
                return next_id;
            } else {
                return ++_last_chunk_id_dispatched;
            }
        }

    }

    public void rejectChunkId(long chunk_id) {
        _rejectedChunkIds.add(chunk_id);
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

    @Override
    public void setProgress(long progress) {

        synchronized (_progress_lock) {

            if (_totals_finalized) {
                return;
            }

            long old_progress = _progress;

            _progress = progress;

            getMain_panel().getDownload_manager().increment_total_progress(_progress - old_progress);

            if (_file_size == null || _file_size <= 0) {
                return;
            }

            int old_percent_progress = (int) Math.floor(((double) old_progress / _file_size) * 100);

            int new_percent_progress = (int) Math.floor(((double) progress / _file_size) * 100);

            if (new_percent_progress == 100 && progress != _file_size) {
                new_percent_progress = 99;
            }

            if (new_percent_progress > old_percent_progress) {
                getView().updateProgressBar(_progress, _progress_bar_rate);
            }
        }
    }

    public void finalizeTotals() {

        synchronized (_progress_lock) {

            if (_totals_finalized) {
                return;
            }

            _totals_finalized = true;

            if (_file_size != null) {
                getMain_panel().getDownload_manager().increment_total_size(-_file_size);
            }

            getMain_panel().getDownload_manager().increment_total_progress(-_progress);
        }
    }

    @Override
    public boolean isStatusError() {
        return _status_error != null;
    }

    @Override
    public int getSlotsCount() {
        return getChunkworkers().size();
    }

    @Override
    public boolean isFrozen() {
        return this._frozen;
    }

    /**
     * Shared 509-burst timestamp for all workers of this download. See the
     * field comment above. Reads + writes are unsynchronized volatile -- a race
     * that lets two workers each set the timestamp to slightly different "now"
     * values is fine (we only care that SOMEONE saw 509 recently). (#751 / C4)
     */
    public long get509BurstTimestamp() {
        return _509_burst_timestamp;
    }

    public void set509BurstTimestamp(long ts) {
        _509_burst_timestamp = ts;
    }

    public void reset509BurstTimestamp() {
        _509_burst_timestamp = -1;
    }

    @Override
    public void unfreeze() {

        getView().printStatusNormal(getView().getStatus_label().getText().replaceFirst("^\\([^)]+\\) ", ""));

        _frozen = false;
    }

    @Override
    public void upWaitQueue() {
        _main_panel.getDownload_manager().upWaitQueue(this);
    }

    @Override
    public void downWaitQueue() {
        _main_panel.getDownload_manager().downWaitQueue(this);
    }

    @Override
    public void bottomWaitQueue() {
        _main_panel.getDownload_manager().bottomWaitQueue(this);
    }

    @Override
    public void topWaitQueue() {
        _main_panel.getDownload_manager().topWaitQueue(this);
    }

    @Override
    public boolean isRestart() {
        return _restart;
    }

    @Override
    public boolean isClosed() {
        return _closed;
    }

    @Override
    public int getPausedWorkers() {
        return _paused_workers;
    }

    @Override
    public int getTotWorkers() {
        return getChunkworkers().size();
    }

}
