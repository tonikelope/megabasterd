package megabasterd;

import static java.awt.Toolkit.getDefaultToolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import static java.lang.Thread.sleep;
import java.util.concurrent.ConcurrentLinkedQueue;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Logger.getLogger;
import static megabasterd.MainPanel.THREAD_POOL;

public final class ClipboardSpy implements Runnable, ClipboardOwner, SecureNotifiable, ClipboardChangeObservable {

    private static final int SLEEP = 250;

    private final Clipboard _sysClip;

    private boolean _notified;

    private final ConcurrentLinkedQueue<ClipboardChangeObserver> _observers;

    private Transferable _contents;

    private final Object _secure_notify_lock;
    
    private volatile boolean _gaining_ownership;

    public ClipboardSpy() {
        _sysClip = getDefaultToolkit().getSystemClipboard();
        _notified = false;
        _contents = null;
        _gaining_ownership = false;
        _secure_notify_lock = new Object();
        _observers = new ConcurrentLinkedQueue<>();
    }

    @Override
    public Transferable getContents() {
        return _contents;
    }

    public Clipboard getSysClip() {
        return _sysClip;
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
                    getLogger(ClipboardSpy.class.getName()).log(SEVERE, null, ex);
                }
            }

            _notified = false;
        }
    }

    @Override
    public void secureNotifyAll() {

        synchronized (_secure_notify_lock) {

            _notified = true;

            _secure_notify_lock.notifyAll();
        }
    }

    @Override
    public void run() {

        _contents = getClipboardContents();

        gainOwnership(_contents);

        System.out.println("Spying clipboard...");

        secureWait();
    }

    @Override
    public void lostOwnership(Clipboard c, Transferable t) {

        if(!_gaining_ownership)
        {
            _gaining_ownership = true;
            
            THREAD_POOL.execute(new Runnable() {
            @Override
            public void run() {
                
                _contents = getClipboardContents();

                notifyChangeToMyObservers();

                gainOwnership(_contents);
                
                _gaining_ownership = false;
            }});
        }
    }

    private Transferable getClipboardContents() {

        try{
            sleep(SLEEP);
        } catch (InterruptedException ex1) {
            getLogger(ClipboardSpy.class.getName()).log(SEVERE, null, ex1);
        }
        
        boolean error;

        Transferable c = null;

        do {
            error = false;

            try {

                c = _sysClip.getContents(null);

            } catch (Exception ex) {

                error = true;

                try {
                    sleep(SLEEP);
                } catch (InterruptedException ex1) {
                    getLogger(ClipboardSpy.class.getName()).log(SEVERE, null, ex1);
                }
            }

        } while (error);

        return c;
    }

    private void gainOwnership(Transferable t) {

        try{
            sleep(SLEEP);
        } catch (InterruptedException ex1) {
            getLogger(ClipboardSpy.class.getName()).log(SEVERE, null, ex1);
        }
        
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
                    getLogger(ClipboardSpy.class.getName()).log(SEVERE, null, ex1);
                }
            }

        } while (error);

    }

    @Override
    public void attachObserver(ClipboardChangeObserver observer) {

        if (!_observers.contains(observer)) {

            _observers.add(observer);
        }
    }

    @Override
    public void detachObserver(ClipboardChangeObserver observer) {

        _observers.remove(observer);

    }

    @Override
    public void notifyChangeToMyObservers() {

        for (ClipboardChangeObserver o : _observers) {

            o.notifyClipboardChange();
        }
    }

}
