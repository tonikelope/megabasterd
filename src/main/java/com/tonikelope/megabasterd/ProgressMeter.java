package com.tonikelope.megabasterd;

import java.util.logging.Level;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public final class ProgressMeter implements Runnable, SecureSingleThreadNotifiable {

    private final Transference _transference;
    private volatile boolean _exit;
    private final Object _secure_notify_lock;
    private boolean _notified;
    private long _progress;

    ProgressMeter(Transference transference) {
        _notified = false;
        _secure_notify_lock = new Object();
        _transference = transference;
        _progress = 0;
        _exit = false;
    }

    public void setExit(boolean value) {
        _exit = value;
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
                    LOG.log(SEVERE, null, ex);
                }
            }

            _notified = false;
        }
    }

    @Override
    public void run() {
        LOG.log(Level.INFO, "{0} ProgressMeter hello!", Thread.currentThread().getName());

        _progress = _transference.getProgress();

        while (!_exit || !_transference.getPartialProgress().isEmpty()) {
            Long reads;

            while ((reads = _transference.getPartialProgress().poll()) != null) {
                _progress += reads;
                _transference.setProgress(_progress);
            }

            if (!_exit) {
                secureWait();
            }
        }

    }
    private static final Logger LOG = Logger.getLogger(ProgressMeter.class.getName());

}
