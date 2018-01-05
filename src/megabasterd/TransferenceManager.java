package megabasterd;

import java.awt.Component;
import java.awt.TrayIcon;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import static megabasterd.MainPanel.*;
import static megabasterd.MiscTools.*;

/**
 *
 * @author tonikelope
 */
abstract public class TransferenceManager implements Runnable, SecureSingleThreadNotifiable {

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
    private boolean _notified;
    private volatile boolean _removing_transferences;
    private volatile boolean _provisioning_transferences;
    private volatile boolean _starting_transferences;
    private volatile boolean _preprocessing_transferences;
    private boolean _tray_icon_finish;
    protected long _total_transferences_size;

    public TransferenceManager(MainPanel main_panel, int max_running_trans, javax.swing.JLabel status, javax.swing.JPanel scroll_panel, javax.swing.JButton close_all_button, javax.swing.JButton pause_all_button, javax.swing.MenuElement clean_all_menu) {
        _notified = false;
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
        _total_transferences_size = 0L;
        _secure_notify_lock = new Object();
        _queue_sort_lock = new Object();
        _transference_preprocess_global_queue = new ConcurrentLinkedQueue<>();
        _transference_waitstart_queue = new ConcurrentLinkedQueue<>();
        _transference_provision_queue = new ConcurrentLinkedQueue<>();
        _transference_remove_queue = new ConcurrentLinkedQueue<>();
        _transference_finished_queue = new ConcurrentLinkedQueue<>();
        _transference_running_list = new ConcurrentLinkedQueue<>();
        _transference_preprocess_queue = new ConcurrentLinkedQueue<>();
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

    public long getTotal_transferences_size() {
        return _total_transferences_size;
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
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
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

    public void closeAllPreProWaiting() {
        _transference_preprocess_queue.clear();

        _transference_preprocess_global_queue.clear();

        _transference_provision_queue.clear();

        _transference_remove_queue.addAll(new ArrayList(getTransference_waitstart_queue()));

        getTransference_waitstart_queue().clear();

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
        for (Transference transference : _transference_running_list) {

            if (!transference.isPaused()) {

                transference.pause();
            }
        }

        secureNotify();
    }

    protected void sortTransferenceStartQueue() {

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

    private void _updateView() {

        swingInvoke(
                new Runnable() {
            @Override
            public void run() {
                if (!_transference_running_list.isEmpty()) {

                    boolean show_pause_all = false;

                    for (Transference trans : _transference_running_list) {

                        if ((show_pause_all = !trans.isPaused())) {

                            break;
                        }
                    }

                    _pause_all_button.setVisible(show_pause_all);

                } else {

                    _pause_all_button.setVisible(false);
                }

                _clean_all_menu.getComponent().setEnabled(!_transference_preprocess_queue.isEmpty() || !_transference_provision_queue.isEmpty() || !getTransference_waitstart_queue().isEmpty());

                if (!_transference_finished_queue.isEmpty() && _isOKFinishedInQueue()) {

                    _close_all_button.setText("Close all OK finished");

                    _close_all_button.setVisible(true);

                } else {

                    _close_all_button.setVisible(false);
                }

                _status.setText(_genStatus());

                _main_panel.getView().revalidate();

                _main_panel.getView().repaint();
            }
        });

    }

    private String _genStatus() {

        int pre = _transference_preprocess_global_queue.size();

        int prov = _transference_provision_queue.size();

        int rem = _transference_remove_queue.size();

        int wait = getTransference_waitstart_queue().size();

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

                        while (!getTransference_preprocess_queue().isEmpty()) {
                            Runnable run = getTransference_preprocess_queue().poll();

                            if (run != null) {

                                run.run();
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

                        while (!getTransference_provision_queue().isEmpty()) {
                            Transference transference = getTransference_provision_queue().poll();

                            if (transference != null) {

                                provision(transference);

                            }
                        }

                        setProvisioning_transferences(false);

                        secureNotify();

                    }
                });

            }

            if (!isRemoving_transferences() && !isStarting_transferences() && !getTransference_waitstart_queue().isEmpty() && getTransference_running_list().size() < _max_running_trans) {

                setStarting_transferences(true);

                THREAD_POOL.execute(new Runnable() {
                    @Override
                    public void run() {

                        while (!getTransference_waitstart_queue().isEmpty() && getTransference_running_list().size() < _max_running_trans) {

                            Transference transference = getTransference_waitstart_queue().poll();

                            if (transference != null) {

                                start(transference);
                            }
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

}
