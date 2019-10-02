package com.tonikelope.megabasterd;

import static java.awt.Toolkit.getDefaultToolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import static java.lang.Thread.sleep;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public final class ClipboardSpy implements Runnable, ClipboardOwner, SecureSingleThreadNotifiable, ClipboardChangeObservable {

    private static final int SLEEP = 250;

    private final Clipboard _sysClip;

    private boolean _notified;

    private final ConcurrentLinkedQueue<ClipboardChangeObserver> _observers;

    private Transferable _contents;

    private final Object _secure_notify_lock;

    private volatile boolean _enabled;

    public ClipboardSpy() {
        _sysClip = getDefaultToolkit().getSystemClipboard();
        _notified = false;
        _enabled = false;
        _contents = null;
        _secure_notify_lock = new Object();
        _observers = new ConcurrentLinkedQueue<>();
    }

    @Override
    public Transferable getContents() {
        return _contents;
    }

    private void _setEnabled(boolean enabled) {

        _enabled = enabled;

        if (_enabled) {

            _contents = getClipboardContents();

            notifyChangeToMyObservers();

            gainOwnership(_contents);

            LOG.log(Level.INFO, "{0} Monitoring clipboard ON...", Thread.currentThread().getName());

        } else {
            LOG.log(Level.INFO, "{0} Monitoring clipboard OFF...", Thread.currentThread().getName());
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
                    LOG.log(SEVERE, ex.getMessage());
                }
            }

            _notified = false;
        }
    }

    @Override
    public void run() {

        secureWait();
    }

    @Override
    public void lostOwnership(Clipboard c, Transferable t) {

        if (_enabled) {

            _contents = getClipboardContents();

            notifyChangeToMyObservers();

            gainOwnership(_contents);
        }
    }

    private Transferable getClipboardContents() {

        boolean error;

        Transferable c = null;

        do {
            error = false;

            try {

                c = _sysClip.getContents(this);

            } catch (Exception ex) {

                error = true;

                try {
                    sleep(SLEEP);
                } catch (InterruptedException ex1) {
                    LOG.log(SEVERE, ex1.getMessage());
                }
            }

        } while (error);

        return c;
    }

    private void gainOwnership(Transferable t) {

        boolean error;

        do {
            error = false;

            try {

                _sysClip.setContents(t, this);

            } catch (Exception ex) {

                error = true;

                try {
                    sleep(SLEEP);
                } catch (InterruptedException ex1) {
                    LOG.log(SEVERE, ex1.getMessage());
                }
            }

        } while (error);

    }

    @Override
    public void attachObserver(ClipboardChangeObserver observer) {

        if (!_observers.contains(observer)) {

            _observers.add(observer);
        }

        if (!_observers.isEmpty() && !_enabled) {

            _setEnabled(true);
        }
    }

    @Override
    public void detachObserver(ClipboardChangeObserver observer) {

        if (_observers.contains(observer)) {

            _observers.remove(observer);

            if (_observers.isEmpty() && _enabled) {

                _setEnabled(false);
            }
        }
    }

    @Override
    public void notifyChangeToMyObservers() {

        for (ClipboardChangeObserver o : _observers) {

            o.notifyClipboardChange();
        }
    }
    private static final Logger LOG = Logger.getLogger(ClipboardSpy.class.getName());

}
