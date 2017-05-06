package megabasterd;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import static java.util.logging.Logger.getLogger;
import javax.swing.JLabel;
import static megabasterd.MiscTools.formatBytes;
import static megabasterd.MiscTools.swingReflectionInvoke;

public final class GlobalSpeedMeter implements Runnable, SecureSingleThreadNotifiable {

    public static final int MAX_SPEED_REC = 40;
    private final JLabel _speed_label;
    private final ConcurrentLinkedQueue<SpeedMeter> _speedmeters;
    private final Object _secure_notify_lock;
    private boolean _notified;
    private final Queue<Long> _speeds;

    GlobalSpeedMeter(JLabel sp_label) {
        _notified = false;
        _secure_notify_lock = new Object();
        _speed_label = sp_label;
        _speedmeters = new ConcurrentLinkedQueue<>();
        _speeds = new ArrayDeque<>();
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
                    getLogger(GlobalSpeedMeter.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            _notified = false;
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

        for (SpeedMeter speed : _speedmeters) {
            sp += speed.getLastSpeed();
        }
        
        _speeds.add(sp);

        if (_speeds.size() > MAX_SPEED_REC) {

            _speeds.poll();
        }

        double total = 0, weight = 0.1, total_weight = 0;

        for (Long speed : _speeds) {

            total += (double)speed * weight;
            total_weight += weight;
            weight *= 2;
        }
        
        sp = Math.round(total / total_weight);

        return sp;
    }

    @Override
    public void run() {
        long sp;

        swingReflectionInvoke("setText", _speed_label, "------");
        swingReflectionInvoke("setVisible", _speed_label, true);

        while (true) {
            secureWait();

            sp = calcSpeed();

            if (sp > 0) {

                swingReflectionInvoke("setText", _speed_label, formatBytes(sp) + "/s");

            } else {
                swingReflectionInvoke("setText", _speed_label, "------");

            }
        }

    }
}
