package com.tonikelope.megabasterd;

import static com.tonikelope.megabasterd.MainPanel.*;
import static com.tonikelope.megabasterd.MiscTools.*;
import java.awt.Component;
import java.awt.TrayIcon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ConcurrentLinkedQueue;
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

    private final ConcurrentLinkedQueue<Object> _transference_preprocess_global_queue;
    private final ConcurrentLinkedQueue<Runnable> _transference_preprocess_queue;
    private final ConcurrentLinkedQueue<Transference> _transference_provision_queue;
    private final ConcurrentLinkedQueue<Transference> _transference_waitstart_queue;
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
    private final Object _queue_sort_lock;
    private final Object _wait_queue_lock;
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
        _close_all_button = close_all_button;
        _pause_all_button = pause_all_button;
        _clean_all_menu = clean_all_menu;
        _total_size = 0L;
        _total_progress = 0L;
        _secure_notify_lock = new Object();
        _total_size_lock = new Object();
        _total_progress_lock = new Object();
        _queue_sort_lock = new Object();
        _wait_queue_lock = new Object();
        _transference_preprocess_global_queue = new ConcurrentLinkedQueue<>();
        _transference_waitstart_queue = new ConcurrentLinkedQueue<>();
        _transference_provision_queue = new ConcurrentLinkedQueue<>();
        _transference_remove_queue = new ConcurrentLinkedQueue<>();
        _transference_finished_queue = new ConcurrentLinkedQueue<>();
        _transference_running_list = new ConcurrentLinkedQueue<>();
        _transference_preprocess_queue = new ConcurrentLinkedQueue<>();
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

    public void setPaused_all(boolean paused_all) {
        _paused_all = paused_all;
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

    public Object getQueue_sort_lock() {
        return _queue_sort_lock;
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
                    _secure_notify_lock.wait();
                } catch (InterruptedException ex) {
                    LOG.log(Level.SEVERE, null, ex);
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
        synchronized (_queue_sort_lock) {
            return _transference_waitstart_queue;
        }
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

        for (Transference t : _transference_finished_queue) {

            if (!t.isStatusError()) {

                _transference_finished_queue.remove(t);
                _transference_remove_queue.add(t);
            }
        }

        secureNotify();
    }

    public int calcTotalSlotsCount() {

        int slots = 0;

        for (Transference trans : _transference_running_list) {

            slots += trans.getSlotsCount();
        }

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

            swingInvoke(
                    new Runnable() {
                @Override
                public void run() {

                    for (final Transference t : getTransference_waitstart_queue()) {

                        getScroll_panel().remove((Component) t.getView());
                        getScroll_panel().add((Component) t.getView());

                    }

                    for (final Transference t : getTransference_finished_queue()) {

                        getScroll_panel().remove((Component) t.getView());
                        getScroll_panel().add((Component) t.getView());
                    }
                }
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

            swingInvoke(
                    new Runnable() {
                @Override
                public void run() {

                    for (final Transference t : getTransference_waitstart_queue()) {

                        getScroll_panel().remove((Component) t.getView());
                        getScroll_panel().add((Component) t.getView());

                    }

                    for (final Transference t : getTransference_finished_queue()) {

                        getScroll_panel().remove((Component) t.getView());
                        getScroll_panel().add((Component) t.getView());
                    }
                }
            });

            _frozen = false;
        }

        secureNotify();
    }

    public void start(final Transference transference) {

        _transference_running_list.add(transference);

        swingInvoke(
                new Runnable() {
            @Override
            public void run() {
                getScroll_panel().add((Component) transference.getView(), 0);
            }
        });

        transference.start();

        secureNotify();
    }

    public void pauseAll() {

        _paused_all = !_paused_all;

        for (Transference transference : _transference_running_list) {

            transference.pause();
        }

        secureNotify();
    }

    protected void sortTransferenceWaitStartQueue() {

        synchronized (_queue_sort_lock) {

            ArrayList<Transference> trans_list = new ArrayList(getTransference_waitstart_queue());

            trans_list.sort(new Comparator<Transference>() {

                @Override
                public int compare(Transference o1, Transference o2) {

                    return o1.getFile_name().compareToIgnoreCase(o2.getFile_name());
                }
            });

            getTransference_waitstart_queue().clear();

            getTransference_waitstart_queue().addAll(trans_list);
        }
    }

    protected void unfreezeTransferenceWaitStartQueue() {

        synchronized (_queue_sort_lock) {

            ArrayList<Transference> trans_list = new ArrayList(getTransference_waitstart_queue());

            for (Transference t : trans_list) {
                t.unfreeze();
            }

            getTransference_waitstart_queue().clear();

            getTransference_waitstart_queue().addAll(trans_list);

            _frozen = false;
        }

        secureNotify();
    }

    private void _updateView() {

        swingInvoke(
                new Runnable() {
            @Override
            public void run() {

                if (_paused_all) {
                    _pause_all_button.setText("RESUME ALL");
                } else {
                    _pause_all_button.setText("PAUSE ALL");
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
            }
        });

    }

    private String _genStatus() {

        int pre = _transference_preprocess_global_queue.size();

        int prov = _transference_provision_queue.size();

        int rem = _transference_remove_queue.size();

        int wait = _transference_waitstart_queue.size();

        int run = _transference_running_list.size();

        int finish = _transference_finished_queue.size();

        if (!_tray_icon_finish && finish > 0 && pre + prov + wait + run == 0 && !_main_panel.getView().isVisible()) {

            _tray_icon_finish = true;

            _main_panel.getTrayicon().displayMessage("MegaBasterd says:", "All your transferences have finished", TrayIcon.MessageType.INFO);
        }

        return (pre + prov + rem + wait + run + finish > 0) ? "Pre: " + pre + " / Pro: " + prov + " / Wait: " + wait + " / Run: " + run + " / Finish: " + finish + " / Rem: " + rem : "";
    }

    private boolean _isOKFinishedInQueue() {

        for (Transference t : _transference_finished_queue) {

            if (!t.isStatusError()) {

                return true;
            }
        }

        return false;
    }

    @Override
    public void run() {

        while (true) {

            if (!isRemoving_transferences() && !getTransference_remove_queue().isEmpty()) {

                setRemoving_transferences(true);

                THREAD_POOL.execute(new Runnable() {
                    @Override
                    public void run() {

                        Thread.currentThread().setPriority(Math.max(Thread.currentThread().getPriority() - 1, Thread.MIN_PRIORITY));

                        if (!getTransference_remove_queue().isEmpty()) {

                            ArrayList<Transference> transferences = new ArrayList(getTransference_remove_queue());

                            getTransference_remove_queue().clear();

                            remove(transferences.toArray(new Transference[transferences.size()]));
                        }

                        setRemoving_transferences(false);

                        secureNotify();
                    }
                });
            }

            if (!isPreprocessing_transferences() && !getTransference_preprocess_queue().isEmpty()) {

                setPreprocessing_transferences(true);

                THREAD_POOL.execute(new Runnable() {
                    @Override
                    public void run() {

                        Thread.currentThread().setPriority(Math.max(Thread.currentThread().getPriority() - 1, Thread.MIN_PRIORITY));

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

                        secureNotify();

                    }
                });
            }

            if (!isRemoving_transferences() && !isProvisioning_transferences() && !getTransference_provision_queue().isEmpty()) {

                setProvisioning_transferences(true);

                _tray_icon_finish = false;

                THREAD_POOL.execute(new Runnable() {
                    @Override
                    public void run() {

                        Thread.currentThread().setPriority(Math.max(Thread.currentThread().getPriority() - 1, Thread.MIN_PRIORITY));

                        while (!getTransference_provision_queue().isEmpty()) {
                            Transference transference = getTransference_provision_queue().poll();

                            if (transference != null) {

                                provision(transference);

                            }
                        }

                        _frozen = false;

                        setProvisioning_transferences(false);

                        secureNotify();

                    }
                });

            }

            if (!_frozen && !_main_panel.isExit() && !_paused_all && !isRemoving_transferences() && !isStarting_transferences() && !getTransference_waitstart_queue().isEmpty() && getTransference_running_list().size() < _max_running_trans) {

                setStarting_transferences(true);

                THREAD_POOL.execute(new Runnable() {
                    @Override
                    public void run() {

                        Thread.currentThread().setPriority(Math.max(Thread.currentThread().getPriority() - 1, Thread.MIN_PRIORITY));

                        while (!_frozen && !getTransference_waitstart_queue().isEmpty() && getTransference_running_list().size() < _max_running_trans) {

                            Transference transference = getTransference_waitstart_queue().peek();

                            if (transference != null && !transference.isFrozen()) {

                                getTransference_waitstart_queue().poll();

                                start(transference);

                            } else {

                                _frozen = true;

                            }
                        }

                        synchronized (getWait_queue_lock()) {
                            getWait_queue_lock().notifyAll();
                        }

                        setStarting_transferences(false);

                        secureNotify();
                    }
                });
            }

            secureWait();

            _updateView();
        }

    }

    private static final Logger LOG = Logger.getLogger(TransferenceManager.class.getName());

}
