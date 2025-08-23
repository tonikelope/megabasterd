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

import com.tonikelope.megabasterd.db.KDBTools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.awt.Toolkit.getDefaultToolkit;
import static java.lang.Thread.sleep;


/**
 *
 * @author tonikelope
 */
public class ClipboardSpy implements Runnable, ClipboardOwner, SecureSingleThreadNotifiable, ClipboardChangeObservable {

    private static final Logger LOG = LogManager.getLogger(ClipboardSpy.class);

    private static final int SLEEP = 250;

    private final Clipboard _sysClip;

    private volatile boolean _notified;

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

        boolean monitor_clipboard = true;

        String monitor_clipboard_string = KDBTools.selectSettingValue("clipboardspy");

        if (monitor_clipboard_string != null) {
            monitor_clipboard = monitor_clipboard_string.equals("yes");
        }

        if (_enabled && monitor_clipboard) {
            _contents = getClipboardContents();
            notifyChangeToMyObservers();
            gainOwnership(_contents);
            LOG.info("Monitoring clipboard ON...");
        } else if (monitor_clipboard) {
            LOG.info("Monitoring clipboard OFF...");
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
                    _secure_notify_lock.wait(1000);
                } catch (InterruptedException ex) {
                    LOG.fatal("Sleep interrupted! {}", ex.getMessage());
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
                    LOG.fatal(ex1.getMessage());
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
                    LOG.fatal(ex1.getMessage());
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

        _observers.forEach(ClipboardChangeObserver::notifyClipboardChange);
    }

}
