package megabasterd;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import static java.util.logging.Logger.getLogger;
import javax.swing.JLabel;
import static megabasterd.MiscTools.formatBytes;
import static megabasterd.MiscTools.swingReflectionInvoke;

public final class GlobalSpeedMeter implements Runnable, SecureNotifiable
{
    private final JLabel _speed_label;
    private final ConcurrentLinkedQueue<SpeedMeter> _speedmeters;
    private final Object _secure_notify_lock;
    private boolean _notified;

  
    GlobalSpeedMeter(JLabel sp_label)
    {
        _notified = false;
        _secure_notify_lock = new Object();
        _speed_label = sp_label;
        _speedmeters = new ConcurrentLinkedQueue<>();
    }
    
    @Override
    public void secureNotify()
    {
        synchronized(_secure_notify_lock) {
      
            _notified = true;
            
            _secure_notify_lock.notify();
        }
    }
    
    @Override
    public void secureWait() {
        
        synchronized(_secure_notify_lock)
        {
            while(!_notified) {

                try {
                    _secure_notify_lock.wait();
                } catch (InterruptedException ex) {
                    getLogger(GlobalSpeedMeter.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            _notified = false;
        }
    }
    
    @Override
    public void secureNotifyAll() {
        
        synchronized(_secure_notify_lock) {
            
            _notified = true;
      
            _secure_notify_lock.notifyAll();
        }
    }
    
    public void attachSpeedMeter(SpeedMeter speed) {
        _speedmeters.add(speed);
    }
    
    public void detachSpeedMeter(SpeedMeter speed) {
        _speedmeters.remove(speed);
    }
    
    private long calcSpeed() {
    
        long sp = 0;

        for(SpeedMeter speed:_speedmeters)
        {
            sp+=speed.getLastSpeed();
        }

        return sp;
    }

    
    @Override
    public void run()
    { 
        long sp;
  
        swingReflectionInvoke("setText", _speed_label, "------");
        swingReflectionInvoke("setVisible", _speed_label, true);
        
        while(true)
        {
            secureWait();

            sp = calcSpeed();

            if(sp > 0) {

                swingReflectionInvoke("setText", _speed_label, formatBytes(sp)+"/s");

            }
            else
            {
                swingReflectionInvoke("setText", _speed_label, "------");

            }
        }
        
    }
}
