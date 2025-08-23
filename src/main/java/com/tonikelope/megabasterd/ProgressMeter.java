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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author tonikelope
 */
public class ProgressMeter implements Runnable, SecureSingleThreadNotifiable {

    private static final Logger LOG = LogManager.getLogger(ProgressMeter.class);

    private final Transference _transference;
    private volatile boolean _exit;
    private final Object _secure_notify_lock;
    private volatile boolean _notified;
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
                    _secure_notify_lock.wait(1000);
                } catch (InterruptedException ex) {
                    _exit = true;
                    LOG.fatal("Sleep interrupted!", ex);
                }
            }

            _notified = false;
        }
    }

    @Override
    public void run() {
        LOG.info("ProgressMeter hello! {}", _transference.getFile_name());

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

        LOG.info("ProgressMeter bye bye! {}", _transference.getFile_name());

    }

}
