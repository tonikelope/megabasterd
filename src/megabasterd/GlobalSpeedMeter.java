package megabasterd;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import javax.swing.JLabel;
import static megabasterd.MiscTools.formatBytes;
import static megabasterd.MiscTools.swingReflectionInvokeAndWait;

public final class GlobalSpeedMeter implements Runnable, SecureSingleThreadNotifiable, SecureMultiThreadNotifiable {

    public static final int SLEEP = 5000;
    private final JLabel _speed_label;
    private final ConcurrentHashMap<Thread, Boolean> _notified_threads;
    private final ConcurrentLinkedQueue<SpeedMeter> _speedmeters;
    private final ConcurrentHashMap<SpeedMeter, Long> _speedMap;
    private final Object _secure_notify_lock;
    private final Object _secure_notify_lock_multi;
    private boolean _notified;

    GlobalSpeedMeter(JLabel sp_label) {
        _notified = false;
        _secure_notify_lock = new Object();
        _secure_notify_lock_multi = new Object();
        _speed_label = sp_label;
        _speedmeters = new ConcurrentLinkedQueue<>();
        _speedMap = new ConcurrentHashMap<>();
        _notified_threads = new ConcurrentHashMap<>();
    }

    public ConcurrentHashMap<SpeedMeter, Long> getSpeedQueue() {
        return _speedMap;
    }

    @Override
    public void secureMultiWait() {

        synchronized (_secure_notify_lock_multi) {

            Thread current_thread = Thread.currentThread();

            if (!_notified_threads.containsKey(current_thread)) {

                _notified_threads.put(current_thread, false);
            }

            while (!_notified_threads.get(current_thread)) {

                try {
                    _secure_notify_lock_multi.wait();
                } catch (InterruptedException ex) {
                    getLogger(StreamThrottlerSupervisor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            _notified_threads.put(current_thread, false);
        }
    }

    @Override
    public void secureMultiNotifyAll() {

        synchronized (_secure_notify_lock_multi) {

            for (Map.Entry<Thread, Boolean> entry : _notified_threads.entrySet()) {

                entry.setValue(true);
            }

            _secure_notify_lock_multi.notifyAll();
        }
    }

    public void attachSpeedMeter(SpeedMeter speed) {
        _speedmeters.add(speed);
    }

    public void detachSpeedMeter(SpeedMeter speed) {
        
        if(_speedmeters.contains(speed)) {
            _speedmeters.remove(speed);
        }
        
        if(_speedMap.containsKey(speed)) {
            _speedMap.remove(speed);
        }
        
        secureNotify();
    }
    
    public long getProgress() {

        long progress = 0;

        for (SpeedMeter speed : _speedmeters) {
            progress+=speed.getTransference().getProgress();
        }
        
        return progress;
    }
    
    private void resetSpeedMap() {
        
        for(SpeedMeter speedMeter:_speedmeters) {
            
            _speedMap.put(speedMeter, -1L);
        }
    }

    @Override
    public void run() {
        long sp;

        swingReflectionInvokeAndWait("setText", _speed_label, "------");
        swingReflectionInvokeAndWait("setVisible", _speed_label, true);

        while (true) {
            
            try {
                
                Thread.sleep(SLEEP);
                
                resetSpeedMap();
                
                secureMultiNotifyAll();
                
                sp=0;
                
                while(!this._speedMap.isEmpty())
                {
                    Iterator<Map.Entry<SpeedMeter,Long>> it = _speedMap.entrySet().iterator();
                    
                    while(it.hasNext()) {
                        
                        Map.Entry<SpeedMeter,Long> entry = it.next();
                        
                        if(entry.getValue() != -1) {
                            
                            sp+=entry.getValue();
                            
                            it.remove();
                        }
                    }
                    
                    if(!this._speedMap.isEmpty()) {
                        
                        this.secureWait();
                    }
                }
                
                if (sp > 0) {
                    
                    swingReflectionInvokeAndWait("setText", _speed_label, formatBytes(sp) + "/s");
                    
                } else {
                    
                    swingReflectionInvokeAndWait("setText", _speed_label, "------");
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(GlobalSpeedMeter.class.getName()).log(Level.SEVERE, null, ex);
            }
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
                    getLogger(SpeedMeter.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            _notified = false;
        }
    }

}
