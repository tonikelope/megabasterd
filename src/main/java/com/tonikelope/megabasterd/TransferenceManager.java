package com.tonikelope.megabasterd;

import static com.tonikelope.megabasterd.MainPanel.*;
import java.awt.Component;
import java.awt.TrayIcon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;
import javax.swing.JPanel;

/**
 *
 * @author tonikelope
 */
abstract public class TransferenceManager implements Runnable, SecureSingleThreadNotifiable {

    public static final int MAX_WAIT_QUEUE = 1000;
    public static final int MAX_PROVISION_WORKERS = 25;
    private static final Logger LOG = Logger.getLogger(TransferenceManager.class.getName());

    private final ConcurrentLinkedQueue<Object> _transference_preprocess_global_queue;
    private final ConcurrentLinkedQueue<Runnable> _transference_preprocess_queue;
    private final ConcurrentLinkedQueue<Transference> _transference_provision_queue;
    private final ConcurrentLinkedQueue<Transference> _transference_waitstart_queue;
    private final ConcurrentLinkedQueue<Transference> _transference_waitstart_aux_queue;
    private final ConcurrentLinkedQueue<Transference> _transference_remove_queue;
    private final ConcurrentLinkedQueue<Transference> _transference_finished_queue;
    private final ConcurrentLinkedQueue<Transference> _transference_running_list;

    private final javax.swing.JPanel _scroll_panel;
    private final javax.swing.JLabel _status;
    private final javax.swing.JButton _close_all_button;
    private final javax.swing.JButton _pause_all_button;
    private final javax.swing.MenuElement _clean_all_menu;
    private int _max_running_trans;
    private final MainPanel _main_panel;
    private final Object _secure_notify_lock;
    private final Object _wait_queue_lock;
    private final Object _pause_all_lock;
    private boolean _notified;
    private volatile boolean _removing_transferences;
    private volatile boolean _provisioning_transferences;
    private volatile boolean _starting_transferences;
    private volatile boolean _preprocessing_transferences;
    private volatile boolean _paused_all;
    protected volatile boolean _frozen;
    private boolean _tray_icon_finish;
    protected volatile long _total_size;
    protected final Object _total_size_lock;
    protected volatile long _total_progress;
    protected final Object _total_progress_lock;
    protected final Object _transference_queue_sort_lock;
    private volatile Boolean _sort_wait_start_queue;

    public TransferenceManager(MainPanel main_panel, int max_running_trans, javax.swing.JLabel status, javax.swing.JPanel scroll_panel, javax.swing.JButton close_all_button, javax.swing.JButton pause_all_button, javax.swing.MenuElement clean_all_menu) {
        _notified = false;
        _paused_all = false;
        _frozen = false;
        _removing_transferences = false;
        _provisioning_transferences = false;
        _starting_transferences = false;
        _preprocessing_transferences = false;
        _tray_icon_finish = false;
        _main_panel = main_panel;
        _max_running_trans = max_running_trans;
        _scroll_panel = scroll_panel;
        _status = status;
        _pause_all_lock = new Object();
        _close_all_button = close_all_button;
        _pause_all_button = pause_all_button;
        _clean_all_menu = clean_all_menu;
        _total_size = 0L;
        _total_progress = 0L;
        _secure_notify_lock = new Object();
        _total_size_lock = new Object();
        _total_progress_lock = new Object();
        _transference_queue_sort_lock = new Object();
        _wait_queue_lock = new Object();
        _sort_wait_start_queue = true;
        _transference_preprocess_global_queue = new ConcurrentLinkedQueue<>();
        _transference_waitstart_queue = new ConcurrentLinkedQueue<>();
        _transference_waitstart_aux_queue = new ConcurrentLinkedQueue<>();
        _transference_provision_queue = new ConcurrentLinkedQueue<>();
        _transference_remove_queue = new ConcurrentLinkedQueue<>();
        _transference_finished_queue = new ConcurrentLinkedQueue<>();
        _transference_running_list = new ConcurrentLinkedQueue<>();
        _transference_preprocess_queue = new ConcurrentLinkedQueue<>();
    }

    public Boolean getSort_wait_start_queue() {
        return _sort_wait_start_queue;
    }

    public void setSort_wait_start_queue(Boolean sort_wait_start_queue) {
        this._sort_wait_start_queue = sort_wait_start_queue;
    }

    public void setPaused_all(boolean _paused_all) {
        this._paused_all = _paused_all;
    }

    public boolean isFrozen() {
        return _frozen;
    }

    public boolean no_transferences() {
        return getTransference_preprocess_queue().isEmpty() && getTransference_provision_queue().isEmpty() && getTransference_waitstart_queue().isEmpty() && getTransference_running_list().isEmpty();
    }

    public boolean isPaused_all() {
        return _paused_all;
    }

    public Object getWait_queue_lock() {
        return _wait_queue_lock;
    }

    public ConcurrentLinkedQueue<Object> getTransference_preprocess_global_queue() {
        return _transference_preprocess_global_queue;
    }

    abstract public void provision(Transference transference);

    abstract public void remove(Transference[] transference);

    public boolean isRemoving_transferences() {

        return _removing_transferences;
    }

    public void setRemoving_transferences(boolean removing) {
        _removing_transferences = removing;
    }

    public long get_total_size() {

        synchronized (_total_size_lock) {
            return _total_size;
        }
    }

    public void increment_total_size(long val) {

        synchronized (_total_size_lock) {

            _total_size += val;
        }
    }

    public long get_total_progress() {

        synchronized (_total_progress_lock) {
            return _total_progress;
        }
    }

    public void increment_total_progress(long val) {

        synchronized (_total_progress_lock) {

            _total_progress += val;
        }
    }

    public boolean isProvisioning_transferences() {
        return _provisioning_transferences;
    }

    public void setProvisioning_transferences(boolean provisioning) {
        _provisioning_transferences = provisioning;
    }

    public boolean isStarting_transferences() {
        return _starting_transferences;
    }

    public void setStarting_transferences(boolean starting) {
        _starting_transferences = starting;
    }

    public void setPreprocessing_transferences(boolean preprocessing) {
        _preprocessing_transferences = preprocessing;
    }

    public ConcurrentLinkedQueue<Runnable> getTransference_preprocess_queue() {
        return _transference_preprocess_queue;
    }

    public void setMax_running_trans(int max_running_trans) {
        _max_running_trans = max_running_trans;
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
                    LOG.log(Level.SEVERE, ex.getMessage());
                }
            }

            _notified = false;
        }
    }

    public MainPanel getMain_panel() {
        return _main_panel;
    }

    public boolean isPreprocessing_transferences() {

        return _preprocessing_transferences;
    }

    public ConcurrentLinkedQueue<Transference> getTransference_provision_queue() {

        return _transference_provision_queue;

    }

    public ConcurrentLinkedQueue<Transference> getTransference_waitstart_queue() {

        return _transference_waitstart_queue;

    }

    public ConcurrentLinkedQueue<Transference> getTransference_remove_queue() {

        return _transference_remove_queue;

    }

    public ConcurrentLinkedQueue<Transference> getTransference_finished_queue() {

        return _transference_finished_queue;

    }

    public ConcurrentLinkedQueue<Transference> getTransference_running_list() {

        return _transference_running_list;

    }

    public JPanel getScroll_panel() {
        return _scroll_panel;
    }

    public void closeAllFinished() {

        _transference_finished_queue.stream().filter((t) -> (!t.isStatusError() && !t.isCanceled())).map((t) -> {
            _transference_finished_queue.remove(t);
            return t;
        }).forEachOrdered((t) -> {
            _transference_remove_queue.add(t);
        });

        secureNotify();
    }

    public int calcTotalSlotsCount() {

        int slots = 0;

        slots = _transference_running_list.stream().map((trans) -> trans.getSlotsCount()).reduce(slots, Integer::sum);

        return slots;

    }

    public void closeAllPreProWaiting() {
        _transference_preprocess_queue.clear();

        _transference_preprocess_global_queue.clear();

        _transference_provision_queue.clear();

        _transference_remove_queue.addAll(new ArrayList(getTransference_waitstart_queue()));

        getTransference_waitstart_queue().clear();

        synchronized (getWait_queue_lock()) {
            getWait_queue_lock().notifyAll();
        }

        secureNotify();
    }

    public void topWaitQueue(Transference t) {

        synchronized (getWait_queue_lock()) {

            ArrayList<Transference> wait_array = new ArrayList();

            wait_array.add(t);

            for (Transference t1 : getTransference_waitstart_queue()) {

                if (t1 != t) {
                    wait_array.add(t1);
                }
            }

            getTransference_waitstart_queue().clear();

            getTransference_waitstart_queue().addAll(wait_array);

            getTransference_waitstart_queue().forEach((t1) -> {
                MiscTools.GUIRun(() -> {
                    getScroll_panel().remove((Component) t1.getView());
                    getScroll_panel().add((Component) t1.getView());
                });
            });
            getTransference_finished_queue().forEach((t1) -> {
                MiscTools.GUIRun(() -> {
                    getScroll_panel().remove((Component) t1.getView());
                    getScroll_panel().add((Component) t1.getView());
                });
            });

            _frozen = false;
        }

        secureNotify();
    }

    public void bottomWaitQueue(Transference t) {

        synchronized (getWait_queue_lock()) {

            ArrayList<Transference> wait_array = new ArrayList();

            for (Transference t1 : getTransference_waitstart_queue()) {

                if (t1 != t) {
                    wait_array.add(t1);
                }
            }

            wait_array.add(t);

            getTransference_waitstart_queue().clear();

            getTransference_waitstart_queue().addAll(wait_array);

            getTransference_waitstart_queue().forEach((t1) -> {
                MiscTools.GUIRun(() -> {
                    getScroll_panel().remove((Component) t1.getView());
                    getScroll_panel().add((Component) t1.getView());
                });
            });
            getTransference_finished_queue().forEach((t1) -> {
                MiscTools.GUIRun(() -> {
                    getScroll_panel().remove((Component) t1.getView());
                    getScroll_panel().add((Component) t1.getView());
                });
            });

            _frozen = false;
        }

        secureNotify();

    }

    public void upWaitQueue(Transference t) {

        synchronized (getWait_queue_lock()) {

            ArrayList<Transference> wait_array = new ArrayList(getTransference_waitstart_queue());

            int pos = 0;

            for (Transference t1 : wait_array) {

                if (t1 == t) {
                    break;
                }

                pos++;
            }

            if (pos > 0) {
                Collections.swap(wait_array, pos, pos - 1);
            }

            getTransference_waitstart_queue().clear();

            getTransference_waitstart_queue().addAll(wait_array);

            getTransference_waitstart_queue().forEach((t1) -> {
                MiscTools.GUIRun(() -> {
                    getScroll_panel().remove((Component) t1.getView());
                    getScroll_panel().add((Component) t1.getView());
                });
            });
            getTransference_finished_queue().forEach((t1) -> {
                MiscTools.GUIRun(() -> {
                    getScroll_panel().remove((Component) t1.getView());
                    getScroll_panel().add((Component) t1.getView());
                });
            });

            _frozen = false;
        }

        secureNotify();

    }

    public void downWaitQueue(Transference t) {

        synchronized (getWait_queue_lock()) {

            ArrayList<Transference> wait_array = new ArrayList(getTransference_waitstart_queue());

            int pos = 0;

            for (Transference t1 : wait_array) {

                if (t1 == t) {
                    break;
                }

                pos++;
            }

            if (pos < wait_array.size() - 1) {
                Collections.swap(wait_array, pos, pos + 1);
            }

            getTransference_waitstart_queue().clear();

            getTransference_waitstart_queue().addAll(wait_array);

            getTransference_waitstart_queue().forEach((t1) -> {
                MiscTools.GUIRun(() -> {
                    getScroll_panel().remove((Component) t1.getView());
                    getScroll_panel().add((Component) t1.getView());
                });
            });
            getTransference_finished_queue().forEach((t2) -> {
                MiscTools.GUIRun(() -> {
                    getScroll_panel().remove((Component) t2.getView());
                    getScroll_panel().add((Component) t2.getView());
                });
            });

            _frozen = false;
        }

        secureNotify();
    }

    public void start(final Transference transference) {

        _transference_running_list.add(transference);

        transference.start();
    }

    public void pauseAll() {

        _transference_running_list.forEach((transference) -> {

            if (!transference.isPaused()) {
                transference.pause();
            }

        });

        secureNotify();

        THREAD_POOL.execute(() -> {

            boolean running;

            do {

                running = false;

                for (Transference t : _transference_running_list) {
                    if (t.getPausedWorkers() != t.getTotWorkers()) {
                        running = true;
                        break;
                    }
                }

                if (running) {
                    synchronized (_pause_all_lock) {

                        try {
                            _pause_all_lock.wait(1000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(TransferenceManager.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }
                }

            } while (running);

            _paused_all = true;

            MiscTools.GUIRun(() -> {

                _pause_all_button.setText(LabelTranslatorSingleton.getInstance().translate("RESUME ALL"));
                _pause_all_button.setEnabled(true);

            });

            secureNotify();

        });

    }

    public void resumeAll() {

        _transference_running_list.forEach((transference) -> {

            if (transference.isPaused()) {
                transference.pause();
            }

        });

        _paused_all = false;

        MiscTools.GUIRun(() -> {

            _pause_all_button.setText(LabelTranslatorSingleton.getInstance().translate("PAUSE ALL"));

            _pause_all_button.setEnabled(true);

        });

        secureNotify();
    }

    public ConcurrentLinkedQueue<Transference> getTransference_waitstart_aux_queue() {
        return _transference_waitstart_aux_queue;
    }

    protected void sortTransferenceQueue(ConcurrentLinkedQueue<Transference> queue) {

        synchronized (_transference_queue_sort_lock) {

            ArrayList<Transference> trans_list = new ArrayList(queue);

            trans_list.sort((Transference o1, Transference o2) -> MiscTools.naturalCompare(o1.getFile_name(), o2.getFile_name(), true));

            queue.clear();

            queue.addAll(trans_list);
        }
    }

    protected void unfreezeTransferenceWaitStartQueue() {

        synchronized (getTransference_waitstart_aux_queue()) {

            getTransference_waitstart_queue().forEach((t) -> {
                t.unfreeze();
            });

            _frozen = false;
        }

        secureNotify();
    }

    private void _updateView() {

        MiscTools.GUIRun(() -> {
            if (_paused_all) {
                _pause_all_button.setText(LabelTranslatorSingleton.getInstance().translate("RESUME ALL"));
            } else {
                _pause_all_button.setText(LabelTranslatorSingleton.getInstance().translate("PAUSE ALL"));
            }

            _pause_all_button.setVisible(!getTransference_running_list().isEmpty());

            _clean_all_menu.getComponent().setEnabled(!_transference_preprocess_queue.isEmpty() || !_transference_provision_queue.isEmpty() || !getTransference_waitstart_queue().isEmpty());

            if (!_transference_finished_queue.isEmpty() && _isOKFinishedInQueue()) {

                _close_all_button.setText(LabelTranslatorSingleton.getInstance().translate("Clear finished"));

                _close_all_button.setVisible(true);

            } else {

                _close_all_button.setVisible(false);
            }

            _status.setText(_genStatus());

            _main_panel.getView().getUnfreeze_transferences_button().setVisible(_main_panel.getDownload_manager().isFrozen() || _main_panel.getUpload_manager().isFrozen());

            _main_panel.getView().revalidate();

            _main_panel.getView().repaint();
        });
    }

    private String _genStatus() {

        int pre = _transference_preprocess_global_queue.size();

        int prov = _transference_provision_queue.size();

        int rem = _transference_remove_queue.size();

        int wait = _transference_waitstart_queue.size() + _transference_waitstart_aux_queue.size();

        int run = _transference_running_list.size();

        int finish = _transference_finished_queue.size();

        if (!_tray_icon_finish && finish > 0 && pre + prov + wait + run == 0 && !_main_panel.getView().isVisible()) {

            _tray_icon_finish = true;

            _main_panel.getTrayicon().displayMessage("MegaBasterd says:", "All your transferences have finished", TrayIcon.MessageType.INFO);
        }

        return (pre + prov + rem + wait + run + finish > 0) ? "Pre: " + pre + " / Pro: " + prov + " / Wait: " + wait + " / Run: " + run + " / Finish: " + finish + " / Rem: " + rem : "";
    }

    private boolean _isOKFinishedInQueue() {

        return _transference_finished_queue.stream().anyMatch((t) -> (!t.isStatusError() && !t.isCanceled()));
    }

    @Override
    public void run() {

        while (true) {

            if (!isRemoving_transferences() && !getTransference_remove_queue().isEmpty()) {

                setRemoving_transferences(true);

                THREAD_POOL.execute(() -> {

                    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

                    if (!getTransference_remove_queue().isEmpty()) {

                        ArrayList<Transference> transferences = new ArrayList(getTransference_remove_queue());

                        getTransference_remove_queue().clear();

                        remove(transferences.toArray(new Transference[transferences.size()]));
                    }

                    setRemoving_transferences(false);

                    secureNotify();
                });
            }

            if (!isPreprocessing_transferences() && !getTransference_preprocess_queue().isEmpty()) {

                setPreprocessing_transferences(true);

                if (isPaused_all()) {

                    _paused_all = false;
                }

                THREAD_POOL.execute(() -> {

                    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

                    while (!getTransference_preprocess_queue().isEmpty()) {
                        Runnable run = getTransference_preprocess_queue().poll();

                        if (run != null) {

                            boolean run_error;

                            do {
                                run_error = false;

                                try {
                                    run.run();
                                } catch (Exception ex) {
                                    run_error = true;
                                    LOG.log(SEVERE, null, ex);
                                }
                            } while (run_error);
                        }
                    }

                    setPreprocessing_transferences(false);

                    synchronized (getTransference_preprocess_queue()) {
                        getTransference_preprocess_queue().notifyAll();
                    }

                    secureNotify();
                });
            }

            if (!isRemoving_transferences() && !isProvisioning_transferences() && !getTransference_provision_queue().isEmpty()) {

                setProvisioning_transferences(true);

                _tray_icon_finish = false;

                THREAD_POOL.execute(() -> {
                    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

                    ExecutorService executor = Executors.newFixedThreadPool(MAX_PROVISION_WORKERS);

                    BoundedExecutor bounded_executor = new BoundedExecutor(executor, MAX_PROVISION_WORKERS);

                    while (!getTransference_provision_queue().isEmpty() || isPreprocessing_transferences()) {

                        if (getTransference_waitstart_aux_queue().size() < MAX_WAIT_QUEUE && getTransference_waitstart_queue().size() < MAX_WAIT_QUEUE) {

                            Transference transference = getTransference_provision_queue().poll();

                            if (transference != null) {

                                boolean error;

                                do {
                                    error = false;

                                    try {
                                        bounded_executor.submitTask(() -> {
                                            provision(transference);
                                        });
                                    } catch (InterruptedException ex) {
                                        Logger.getLogger(TransferenceManager.class.getName()).log(Level.SEVERE, null, ex);
                                        error = true;
                                        MiscTools.pausar(1000);
                                    }
                                } while (error);
                            }
                        }

                        if (isPreprocessing_transferences() || getTransference_waitstart_aux_queue().size() >= MAX_WAIT_QUEUE || getTransference_waitstart_queue().size() >= MAX_WAIT_QUEUE) {

                            synchronized (getTransference_preprocess_queue()) {
                                try {
                                    getTransference_preprocess_queue().wait(1000);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(TransferenceManager.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    }

                    executor.shutdown();

                    while (!executor.isTerminated()) {
                        MiscTools.pausar(1000);
                    }

                    synchronized (_transference_queue_sort_lock) {

                        if (getSort_wait_start_queue()) {
                            sortTransferenceQueue(getTransference_waitstart_aux_queue());
                        }

                        if (getTransference_waitstart_aux_queue().peek() != null && getTransference_waitstart_aux_queue().peek().isPriority()) {

                            ArrayList<Transference> trans_list = new ArrayList(getTransference_waitstart_queue());

                            trans_list.addAll(0, getTransference_waitstart_aux_queue());

                            getTransference_waitstart_queue().clear();

                            getTransference_waitstart_queue().addAll(trans_list);

                        } else {
                            getTransference_waitstart_queue().addAll(getTransference_waitstart_aux_queue());
                        }

                        getTransference_waitstart_aux_queue().clear();

                        getTransference_waitstart_queue().forEach((t) -> {
                            MiscTools.GUIRun(() -> {
                                getScroll_panel().remove((Component) t.getView());
                                getScroll_panel().add((Component) t.getView());
                            });
                        });

                        sortTransferenceQueue(getTransference_finished_queue());

                        getTransference_finished_queue().forEach((t) -> {
                            MiscTools.GUIRun(() -> {
                                getScroll_panel().remove((Component) t.getView());
                                getScroll_panel().add((Component) t.getView());
                            });
                        });

                    }

                    _frozen = false;
                    setSort_wait_start_queue(true);
                    setProvisioning_transferences(false);
                    secureNotify();
                });

            }

            if (!_frozen && !_main_panel.isExit() && !_paused_all && !isRemoving_transferences() && !isStarting_transferences() && (!getTransference_waitstart_queue().isEmpty() || !getTransference_waitstart_aux_queue().isEmpty()) && getTransference_running_list().size() < _max_running_trans) {

                setStarting_transferences(true);

                THREAD_POOL.execute(() -> {

                    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

                    while (!_frozen && !_main_panel.isExit() && !_paused_all && (!getTransference_waitstart_queue().isEmpty() || !getTransference_waitstart_aux_queue().isEmpty()) && getTransference_running_list().size() < _max_running_trans) {

                        synchronized (_transference_queue_sort_lock) {
                            Transference transference = getTransference_waitstart_queue().peek();

                            if (transference == null) {
                                transference = getTransference_waitstart_aux_queue().peek();
                            }

                            if (transference != null && !transference.isFrozen()) {

                                getTransference_waitstart_queue().remove(transference);

                                getTransference_waitstart_aux_queue().remove(transference);

                                start(transference);

                            } else {

                                _frozen = true;

                            }
                        }
                    }

                    synchronized (getWait_queue_lock()) {
                        getWait_queue_lock().notifyAll();
                    }

                    setStarting_transferences(false);

                    secureNotify();
                });
            }

            secureWait();

            _updateView();
        }

    }

}
