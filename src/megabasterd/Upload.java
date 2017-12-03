package megabasterd;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import static megabasterd.MainPanel.*;
import static megabasterd.MiscTools.*;

/**
 *
 * @author tonikelope
 */
public final class Upload implements Transference, Runnable, SecureSingleThreadNotifiable {

    public static final boolean USE_SLOTS_DEFAULT = false;
    public static final int WORKERS_DEFAULT = 6;
    private final MainPanel _main_panel;
    private volatile UploadView _view;
    private volatile ProgressMeter _progress_meter;
    private String _exit_message;
    private String _dir_name;
    private volatile boolean _exit;
    private final int _slots;
    private final Object _secure_notify_lock;
    private final Object _workers_lock;
    private final Object _chunkid_lock;
    private byte[] _byte_file_key;
    private String _fatal_error;
    private volatile long _progress;
    private byte[] _byte_file_iv;
    private final ConcurrentLinkedQueue<Long> _rejectedChunkIds;
    private long _last_chunk_id_dispatched;
    private final ConcurrentLinkedQueue<Integer> _partialProgressQueue;
    private final ExecutorService _thread_pool;
    private int[] _file_meta_mac;
    private boolean _finishing_upload;
    private String _fid;
    private boolean _notified;
    private volatile String _completion_handle;
    private int _paused_workers;
    private Double _progress_bar_rate;
    private volatile boolean _pause;
    private final ArrayList<ChunkUploader> _chunkworkers;
    private long _file_size;
    private UploadMACGenerator _mac_generator;
    private boolean _create_dir;
    private boolean _provision_ok;
    private boolean _status_error;
    private String _file_link;
    private int[] _saved_file_mac;
    private final MegaAPI _ma;
    private final String _file_name;
    private final String _parent_node;
    private int[] _ul_key;
    private String _ul_url;
    private final String _root_node;
    private final byte[] _share_key;
    private final String _folder_link;
    private final boolean _use_slots;
    private final boolean _restart;

    public Upload(MainPanel main_panel, MegaAPI ma, String filename, String parent_node, int[] ul_key, String ul_url, String root_node, byte[] share_key, String folder_link, boolean use_slots, int slots, boolean restart) {

        _saved_file_mac = new int[]{0, 0, 0, 0};
        _notified = false;
        _provision_ok = true;
        _status_error = false;
        _main_panel = main_panel;
        _ma = ma;
        _file_name = filename;
        _parent_node = parent_node;
        _ul_key = ul_key;
        _ul_url = ul_url;
        _root_node = root_node;
        _share_key = share_key;
        _folder_link = folder_link;
        _use_slots = use_slots;
        _slots = slots;
        _restart = restart;
        _completion_handle = null;
        _secure_notify_lock = new Object();
        _workers_lock = new Object();
        _chunkid_lock = new Object();
        _chunkworkers = new ArrayList<>();
        _partialProgressQueue = new ConcurrentLinkedQueue<>();
        _rejectedChunkIds = new ConcurrentLinkedQueue<>();
        _thread_pool = Executors.newCachedThreadPool();
        _view = new UploadView(this);
        _progress_meter = new ProgressMeter(this);
    }

    public Object getWorkers_lock() {
        return _workers_lock;
    }

    public String getDir_name() {
        return _dir_name;
    }

    public boolean isExit() {
        return _exit;
    }

    public int getSlots() {
        return _slots;
    }

    public Object getSecure_notify_lock() {
        return _secure_notify_lock;
    }

    public byte[] getByte_file_key() {
        return _byte_file_key;
    }

    public String getFatal_error() {
        return _fatal_error;
    }

    @Override
    public long getProgress() {
        return _progress;
    }

    public byte[] getByte_file_iv() {
        return _byte_file_iv;
    }

    public ConcurrentLinkedQueue<Long> getRejectedChunkIds() {
        return _rejectedChunkIds;
    }

    public long getLast_chunk_id_dispatched() {
        return _last_chunk_id_dispatched;
    }

    public ConcurrentLinkedQueue<Integer> getPartialProgressQueue() {
        return _partialProgressQueue;
    }

    public ExecutorService getThread_pool() {
        return _thread_pool;
    }

    public String getFid() {
        return _fid;
    }

    public boolean isNotified() {
        return _notified;
    }

    public String getCompletion_handle() {
        return _completion_handle;
    }

    public int getPaused_workers() {
        return _paused_workers;
    }

    public Double getProgress_bar_rate() {
        return _progress_bar_rate;
    }

    public boolean isPause() {
        return _pause;
    }

    public ArrayList<ChunkUploader> getChunkworkers() {

        synchronized (_workers_lock) {
            return _chunkworkers;
        }

    }

    @Override
    public long getFile_size() {
        return _file_size;
    }

    public UploadMACGenerator getMac_generator() {
        return _mac_generator;
    }

    public boolean isCreate_dir() {
        return _create_dir;
    }

    public boolean isProvision_ok() {
        return _provision_ok;
    }

    public boolean isStatus_error() {
        return _status_error;
    }

    public String getFile_link() {
        return _file_link;
    }

    public int[] getSaved_file_mac() {
        return _saved_file_mac;
    }

    public MegaAPI getMa() {
        return _ma;
    }

    @Override
    public String getFile_name() {
        return _file_name;
    }

    public String getParent_node() {
        return _parent_node;
    }

    public int[] getUl_key() {
        return _ul_key;
    }

    public String getUl_url() {
        return _ul_url;
    }

    public String getRoot_node() {
        return _root_node;
    }

    public byte[] getShare_key() {
        return _share_key;
    }

    public String getFolder_link() {
        return _folder_link;
    }

    public boolean isUse_slots() {
        return _use_slots;
    }

    public boolean isRestart() {
        return _restart;
    }

    public void setCompletion_handle(String completion_handle) {
        _completion_handle = completion_handle;
    }

    public void setFile_meta_mac(int[] file_meta_mac) {
        _file_meta_mac = file_meta_mac;
    }

    public void setPaused_workers(int paused_workers) {
        _paused_workers = paused_workers;
    }

    @Override
    public ProgressMeter getProgress_meter() {

        while (_progress_meter == null) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException ex) {
                Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return _progress_meter;
    }

    @Override
    public UploadView getView() {

        while (_view == null) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException ex) {
                Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return this._view;
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

    public void provisionIt() {

        printStatus("Provisioning upload, please wait...");

        String exit_msg = null;

        File the_file = new File(_file_name);

        _provision_ok = false;

        if (!the_file.exists()) {

            exit_msg = "ERROR: FILE NOT FOUND -> " + _file_name;

        } else {

            try {
                _file_size = the_file.length();

                File temp_file;

                temp_file = new File("." + HashString("SHA-1", _file_name));

                if (_ul_key != null && temp_file.exists() && temp_file.length() > 0) {

                    FileInputStream fis = new FileInputStream(temp_file);

                    byte[] data = new byte[(int) temp_file.length()];

                    fis.read(data);

                    String[] fdata = new String(data).split("\\|");

                    _progress = Long.parseLong(fdata[0]);

                    _last_chunk_id_dispatched = calculateLastUploadedChunk(_progress);

                    _saved_file_mac = bin2i32a(BASE642Bin(fdata[1]));

                } else if (temp_file.exists()) {

                    temp_file.delete();
                }

                if (_ul_key == null || _restart) {

                    try {

                        _ul_key = _ma.genUploadKey();

                        DBTools.insertUpload(_file_name, _ma.getFull_email(), _parent_node, Bin2BASE64(i32a2bin(_ul_key)), _root_node, Bin2BASE64(_share_key), _folder_link);

                        _provision_ok = true;

                    } catch (SQLException ex) {

                        exit_msg = ex.getMessage();
                    }

                } else {

                    _provision_ok = true;
                }

            } catch (Exception ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (!_provision_ok) {

            _status_error = true;

            getView().hideAllExceptStatus();

            if (_fatal_error != null) {

                printStatusError(_fatal_error);

            } else if (exit_msg != null) {

                printStatusError(exit_msg);
            }

            swingReflectionInvoke("setVisible", getView().getRestart_button(), true);

        } else {

            printStatus("Waiting to start (" + _ma.getFull_email() + ") ...");

            swingReflectionInvoke("setVisible", getView().getFile_name_label(), true);

            swingReflectionInvoke("setText", getView().getFile_name_label(), _file_name);

            swingReflectionInvoke("setText", getView().getFile_name_label(), truncateText(_file_name, 100));

            swingReflectionInvoke("setToolTipText", getView().getFile_name_label(), _file_name);

            swingReflectionInvoke("setVisible", getView().getFile_size_label(), true);

            swingReflectionInvoke("setText", getView().getFile_size_label(), formatBytes(_file_size));
        }

        swingReflectionInvoke("setVisible", getView().getClose_button(), true);
    }

    @Override
    public void start() {

        THREAD_POOL.execute(this);
    }

    @Override
    public void stop() {
        if (!isExit()) {
            stopUploader();
        }
    }

    @Override
    public void pause() {

        if (isPaused()) {

            setPause(false);

            synchronized (_workers_lock) {

                for (ChunkUploader uploader : getChunkworkers()) {

                    uploader.secureNotify();
                }
            }

            setPaused_workers(0);

            getView().resume();

        } else {

            setPause(true);

            getView().pause();
        }

        getMain_panel().getUpload_manager().secureNotify();
    }

    @Override
    public void restart() {

        Upload new_upload = new Upload(getMain_panel(), getMa(), getFile_name(), getParent_node(), getUl_key(), getUl_url(), getRoot_node(), getShare_key(), getFolder_link(), getMain_panel().isUse_slots_up(), getMain_panel().getDefault_slots_up(), true);

        getMain_panel().getUpload_manager().getTransference_remove_queue().add(this);

        getMain_panel().getUpload_manager().getTransference_provision_queue().add(new_upload);

        getMain_panel().getUpload_manager().secureNotify();
    }

    @Override
    public void close() {

        getMain_panel().getUpload_manager().getTransference_remove_queue().add(this);

        getMain_panel().getUpload_manager().secureNotify();
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

        if (!isExit()) {

            synchronized (_workers_lock) {

                int sl = (int) swingReflectionInvokeAndWaitForReturn("getValue", getView().getSlots_spinner());

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
    public ConcurrentLinkedQueue<Integer> getPartialProgress() {
        return getPartialProgressQueue();
    }

    @Override
    public MainPanel getMain_panel() {
        return _main_panel;
    }

    public void startSlot() {

        if (!_exit) {

            synchronized (_workers_lock) {

                int chunkthiser_id = _chunkworkers.size() + 1;

                ChunkUploader c = new ChunkUploader(chunkthiser_id, this);

                _chunkworkers.add(c);

                try {

                    Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Starting chunkuploader from startslot()...", Thread.currentThread().getName());

                    _thread_pool.execute(c);

                } catch (java.util.concurrent.RejectedExecutionException e) {
                    Logger.getLogger(getClass().getName()).log(Level.INFO, e.getMessage());
                }

            }

        }
    }

    public void setPause(boolean pause) {
        _pause = pause;
    }

    public void stopLastStartedSlot() {

        if (!_exit) {

            synchronized (_workers_lock) {

                if (!_chunkworkers.isEmpty()) {

                    swingReflectionInvoke("setEnabled", getView().getSlots_spinner(), false);

                    int i = _chunkworkers.size() - 1;

                    while (i >= 0) {

                        ChunkUploader chunkuploader = _chunkworkers.get(i);

                        if (!chunkuploader.isExit()) {

                            chunkuploader.setExit(true);

                            chunkuploader.secureNotify();

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

    public void rejectChunkId(long chunk_id) {
        _rejectedChunkIds.add(chunk_id);
    }

    @Override
    public void run() {

        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Uploader hello! {1}", new Object[]{Thread.currentThread().getName(), this.getFile_name()});

        swingReflectionInvoke("setVisible", getView().getClose_button(), false);

        printStatus("Starting upload, please wait...");

        if (!_exit) {
            if (_ul_url == null || _restart) {

                int conta_error = 0;

                do {
                    _ul_url = _ma.initUploadFile(_file_name);
                    long wait_time = MiscTools.getWaitTimeExpBackOff(++conta_error);
                    Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Uploader {1} Upload URL is null, retrying in {2} secs...", new Object[]{Thread.currentThread().getName(), this.getFile_name(), wait_time});
                    try {
                        Thread.sleep(wait_time * 1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } while (_ul_url == null && !_exit);

                if (_ul_url != null) {

                    try {

                        DBTools.updateUploadUrl(_file_name, _ma.getFull_email(), _ul_url);
                    } catch (SQLException ex) {
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

            if (!_exit && _ul_url != null) {

                int[] file_iv = {_ul_key[4], _ul_key[5], 0, 0};

                _byte_file_key = i32a2bin(Arrays.copyOfRange(_ul_key, 0, 4));

                _byte_file_iv = i32a2bin(file_iv);

                swingReflectionInvoke("setMinimum", getView().getProgress_pbar(), 0);
                swingReflectionInvoke("setMaximum", getView().getProgress_pbar(), Integer.MAX_VALUE);
                swingReflectionInvoke("setStringPainted", getView().getProgress_pbar(), true);

                if (_file_size > 0) {

                    _progress_bar_rate = Integer.MAX_VALUE / (double) _file_size;

                    swingReflectionInvoke("setValue", getView().getProgress_pbar(), 0);

                } else {

                    swingReflectionInvoke("setValue", getView().getProgress_pbar(), Integer.MAX_VALUE);
                }

                _thread_pool.execute(getProgress_meter());

                getMain_panel().getGlobal_up_speed().attachTransference(this);

                _mac_generator = new UploadMACGenerator(this);

                _thread_pool.execute(_mac_generator);

                synchronized (_workers_lock) {

                    if (_use_slots) {

                        for (int t = 1; t <= _slots; t++) {
                            ChunkUploader c = new ChunkUploader(t, this);

                            _chunkworkers.add(c);

                            Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Starting chunkuploader {1} ...", new Object[]{Thread.currentThread().getName(), t});

                            _thread_pool.execute(c);
                        }

                        swingReflectionInvoke("setVisible", getView().getSlots_label(), true);

                        swingReflectionInvoke("setVisible", getView().getSlots_spinner(), true);

                        swingReflectionInvoke("setVisible", getView().getSlot_status_label(), true);

                    } else {

                        ChunkUploaderMono c = new ChunkUploaderMono(this);

                        _chunkworkers.add(c);

                        _thread_pool.execute(c);

                        swingReflectionInvoke("setVisible", getView().getSlots_label(), false);

                        swingReflectionInvoke("setVisible", getView().getSlots_spinner(), false);

                        swingReflectionInvoke("setVisible", getView().getSlot_status_label(), false);
                    }
                }

                printStatus("Uploading file to mega (" + _ma.getFull_email() + ") ...");

                getMain_panel().getUpload_manager().secureNotify();

                swingReflectionInvoke("setVisible", getView().getPause_button(), true);

                swingReflectionInvoke("setVisible", getView().getProgress_pbar(), true);

                secureWait();

                _thread_pool.shutdown();

                Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Chunkuploaders finished! {1}", new Object[]{Thread.currentThread().getName(), this.getFile_name()});

                getProgress_meter().setExit(true);

                getProgress_meter().secureNotify();

                try {

                    Logger.getLogger(getClass().getName()).log(Level.INFO, "{0}Waiting for all threads to finish {1}...", new Object[]{Thread.currentThread().getName(), this.getFile_name()});

                    _thread_pool.awaitTermination(MAX_WAIT_WORKERS_SHUTDOWN, TimeUnit.SECONDS);

                } catch (InterruptedException ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                }

                if (!_thread_pool.isTerminated()) {

                    Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Closing thread pool in ''mecag\u00fcen'' style {1}...", new Object[]{Thread.currentThread().getName(), this.getFile_name()});

                    _thread_pool.shutdownNow();
                }

                Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Uploader thread pool finished! {1}", new Object[]{Thread.currentThread().getName(), this.getFile_name()});

                getMain_panel().getGlobal_up_speed().detachTransference(this);

                swingReflectionInvoke("setVisible", new Object[]{getView().getSpeed_label(), getView().getPause_button(), getView().getStop_button(), getView().getSlots_label(), getView().getSlots_spinner()}, false);

                getMain_panel().getUpload_manager().secureNotify();

                if (!_exit) {

                    if (_completion_handle != null) {

                        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Uploader creating NEW MEGA NODE {1}...", new Object[]{Thread.currentThread().getName(), this.getFile_name()});

                        printStatus("Creating new MEGA node ... ***DO NOT EXIT MEGABASTERD NOW***");

                        File f = new File(_file_name);

                        HashMap<String, Object> upload_res;

                        int[] ul_key = _ul_key;

                        int[] node_key = {ul_key[0] ^ ul_key[4], ul_key[1] ^ ul_key[5], ul_key[2] ^ _file_meta_mac[0], ul_key[3] ^ _file_meta_mac[1], ul_key[4], ul_key[5], _file_meta_mac[0], _file_meta_mac[1]};

                        upload_res = _ma.finishUploadFile(f.getName(), ul_key, node_key, _file_meta_mac, _completion_handle, _parent_node, i32a2bin(_ma.getMaster_key()), _root_node, _share_key);

                        List files = (List) upload_res.get("f");

                        _fid = (String) ((Map<String, Object>) files.get(0)).get("h");

                        _exit_message = "File successfully uploaded! (" + _ma.getFull_email() + ")";

                        try {

                            _file_link = _ma.getPublicFileLink(_fid, i32a2bin(node_key));

                            swingReflectionInvoke("setEnabled", getView().getFile_link_button(), true);

                        } catch (Exception ex) {
                            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                        }

                        printStatusOK(_exit_message);

                    } else {

                        getView().hideAllExceptStatus();

                        _exit_message = "UPLOAD FAILED! (Empty completion handle!)";

                        printStatusError(_exit_message);

                        _status_error = true;
                    }

                } else if (_fatal_error != null) {

                    getView().hideAllExceptStatus();

                    printStatusError(_fatal_error);

                    _status_error = true;

                } else {

                    getView().hideAllExceptStatus();

                    _exit_message = "Upload CANCELED!";

                    printStatusError(_exit_message);

                    _status_error = true;
                }

            } else if (_fatal_error != null) {
                getView().hideAllExceptStatus();

                printStatusError(_fatal_error);

                _status_error = true;
            } else {
                getView().hideAllExceptStatus();

                _exit_message = "Upload CANCELED!";

                printStatusError(_exit_message);

                _status_error = true;
            }

        } else if (_fatal_error != null) {
            getView().hideAllExceptStatus();

            _exit_message = _fatal_error;

            printStatusError(_fatal_error);

            _status_error = true;
        } else {
            getView().hideAllExceptStatus();

            _exit_message = "Upload CANCELED!";

            printStatusError(_exit_message);

            _status_error = true;

        }

        if (!_exit) {

            if (!_status_error) {

                try {
                    DBTools.deleteUpload(_file_name, _ma.getFull_email());
                } catch (SQLException ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                }
            }

            getMain_panel().getUpload_manager().getTransference_running_list().remove(this);

            getMain_panel().getUpload_manager().getTransference_finished_queue().add(this);

            getMain_panel().getUpload_manager().getScroll_panel().remove(getView());

            getMain_panel().getUpload_manager().getScroll_panel().add(getView());

            getMain_panel().getUpload_manager().secureNotify();
        }

        swingReflectionInvoke("setVisible", getView().getClose_button(), true);

        if (_status_error) {
            swingReflectionInvoke("setVisible", getView().getRestart_button(), true);
        }

        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Uploader finished with message ->  {1} {2}...", new Object[]{Thread.currentThread().getName(), _exit_message, this.getFile_name()});

        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Uploader {1} BYE BYE", new Object[]{Thread.currentThread().getName(), this.getFile_name()});
    }

    public void pause_worker() {

        synchronized (_workers_lock) {

            if (++_paused_workers >= _chunkworkers.size() && !_exit) {

                printStatus("Upload paused!");
                swingReflectionInvoke("setText", getView().getPause_button(), "RESUME UPLOAD");
                swingReflectionInvoke("setEnabled", getView().getPause_button(), true);
            }
        }

    }

    public void stopThisSlot(ChunkUploader chunkuploader) {

        synchronized (_workers_lock) {

            if (_chunkworkers.remove(chunkuploader) && !_exit) {

                if (!chunkuploader.isExit()) {

                    _finishing_upload = true;

                    if (_use_slots) {

                        swingReflectionInvoke("setEnabled", getView().getSlots_spinner(), false);

                        swingReflectionInvokeAndWait("setValue", getView().getSlots_spinner(), (int) swingReflectionInvokeAndWaitForReturn("getValue", getView().getSlots_spinner()) - 1);
                    }

                } else if (!_finishing_upload && _use_slots) {

                    swingReflectionInvoke("setEnabled", getView().getSlots_spinner(), true);
                }

                if (!_exit && _pause && _paused_workers == _chunkworkers.size()) {

                    printStatus("Upload paused!");
                    swingReflectionInvoke("setText", getView().getPause_button(), "RESUME UPLOAD");
                    swingReflectionInvoke("setEnabled", getView().getPause_button(), true);
                }

                if (_use_slots) {
                    getView().updateSlotsStatus();
                }
            }
        }
    }

    public void emergencyStopUploader(String reason) {
        if (!_exit && _fatal_error == null) {
            _fatal_error = reason != null ? reason : "FATAL ERROR!";

            stopUploader();
        }
    }

    public long nextChunkId() {

        synchronized (_chunkid_lock) {

            Long next_id;

            if ((next_id = _rejectedChunkIds.poll()) != null) {
                return next_id;
            } else {
                return ++_last_chunk_id_dispatched;
            }
        }
    }

    public void setExit(boolean exit) {
        _exit = exit;
    }

    public void stopUploader() {

        if (!_exit) {
            _exit = true;

            try {
                DBTools.deleteUpload(_file_name, _ma.getFull_email());
            } catch (SQLException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }

            getMain_panel().getUpload_manager().getTransference_running_list().remove(this);

            if (_provision_ok) {

                getMain_panel().getUpload_manager().getTransference_finished_queue().add(this);
            }

            getMain_panel().getUpload_manager().getScroll_panel().remove(getView());

            getMain_panel().getUpload_manager().getScroll_panel().add(getView());

            getMain_panel().getUpload_manager().secureNotify();

            getView().stop("Stopping upload safely, please wait...");

            synchronized (_workers_lock) {

                for (ChunkUploader uploader : _chunkworkers) {

                    uploader.secureNotify();
                }
            }

            secureNotify();
        }
    }

    private void printStatusError(String message) {
        swingReflectionInvoke("setForeground", getView().getStatus_label(), Color.red);
        swingReflectionInvoke("setText", getView().getStatus_label(), message);
    }

    private void printStatusOK(String message) {
        swingReflectionInvoke("setForeground", getView().getStatus_label(), new Color(0, 128, 0));
        swingReflectionInvoke("setText", getView().getStatus_label(), message);
    }

    private void printStatus(String message) {
        swingReflectionInvoke("setForeground", getView().getStatus_label(), Color.BLACK);
        swingReflectionInvoke("setText", getView().getStatus_label(), message);
    }

    @Override
    public void setProgress(long progress) {
        _progress = progress;
        getView().updateProgressBar(_progress, _progress_bar_rate);
    }

    @Override
    public boolean isStatusError() {
        return _status_error;
    }

    public long calculateLastUploadedChunk(long bytes_read) {
        if (bytes_read > 3584 * 1024) {
            return 7 + (long) Math.ceil((bytes_read - 3584 * 1024) / (1024 * 1024));
        } else {
            int i = 0, tot = 0;

            while (tot < bytes_read) {
                i++;
                tot += i * 128 * 1024;
            }

            return i;
        }
    }

}
