package megabasterd;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import static java.util.logging.Logger.getLogger;
import static megabasterd.MiscTools.formatBytes;

public final class SpeedMeter implements Runnable, SecureSingleThreadNotifiable {

    public static final int SLEEP = 3000;
    public static final int MAX_SPEED_REC = 20;
    private long _progress;
    private final Transference _transference;
    private final GlobalSpeedMeter _gspeed;
    private volatile long _lastSpeed;
    private volatile boolean _exit;
    private final Object _secure_notify_lock;
    private boolean _notified;
    private final Queue<Long> _speeds;

    SpeedMeter(Transference transference, GlobalSpeedMeter gspeed) {
        _notified = false;
        _secure_notify_lock = new Object();
        _transference = transference;
        _progress = transference.getProgress();
        _lastSpeed = 0;
        _gspeed = gspeed;
        _exit = false;
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
                    _exit = true;
                    getLogger(SpeedMeter.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            _notified = false;
        }
    }

    public void setExit(boolean exit) {
        _exit = exit;
    }

    public long getLastSpeed() {
        return _lastSpeed;
    }

    public void setLastSpeed(long speed) {
        _lastSpeed = speed;
    }

    public void updateProgress() {

        _progress = _transference.getProgress();
    }

    private long calcAverageSpeed(long sp) {

        _speeds.add(sp);

        if (_speeds.size() > MAX_SPEED_REC) {

            _speeds.poll();
        }

        double total = 0, weight = 0.1, total_weight = 0;

        for (Long speed : _speeds) {

            total_weight += weight;

            total += weight * speed;

            weight += 0.1;
        }

        sp = Math.round(total / total_weight);

        return sp;
    }

    @Override
    public void run() {
        System.out.println("SpeedMeter hello!");

        long last_progress = _progress, sp, avgSp;
        int no_data_count;

        _transference.getView().updateSpeed("------", true);
        _transference.getView().updateRemainingTime("--d --:--:--", true);

        try {
            no_data_count = 0;

            while (!_exit) {
                Thread.sleep(SpeedMeter.SLEEP * (no_data_count + 1));

                if (!_exit) {
                    updateProgress();

                    if (_transference.isPaused()) {

                        _transference.getView().updateSpeed("------", true);

                        _transference.getView().updateRemainingTime("--d --:--:--", true);

                        setLastSpeed(0);

                        _gspeed.secureNotify();

                        secureWait();

                    } else if (_progress > last_progress) {

                        double sleep_time = ((double) SpeedMeter.SLEEP * (no_data_count + 1)) / 1000;

                        double current_speed = (_progress - last_progress) / sleep_time;

                        last_progress = _progress;

                        sp = Math.round(current_speed);

                        avgSp = calcAverageSpeed(sp);

                        if (sp > 0) {

                            _transference.getView().updateSpeed(formatBytes(sp) + "/s", true);

                            _transference.getView().updateRemainingTime(calculateRemTime((long) Math.floor((_transference.getFile_size() - _progress) / avgSp)), true);

                            setLastSpeed(sp);

                            _gspeed.secureNotify();
                        }

                        no_data_count = 0;

                    } else {

                        _transference.getView().updateSpeed("------", true);

                        _transference.getView().updateRemainingTime("--d --:--:--", true);

                        setLastSpeed(0);

                        _gspeed.secureNotify();

                        no_data_count++;
                    }
                }
            }
        } catch (InterruptedException ex) {

        }

    }

    private String calculateRemTime(long seconds) {
        int days = (int) TimeUnit.SECONDS.toDays(seconds);

        long hours = TimeUnit.SECONDS.toHours(seconds)
                - TimeUnit.DAYS.toHours(days);

        long minutes = TimeUnit.SECONDS.toMinutes(seconds)
                - TimeUnit.DAYS.toMinutes(days)
                - TimeUnit.HOURS.toMinutes(hours);

        long secs = TimeUnit.SECONDS.toSeconds(seconds)
                - TimeUnit.DAYS.toSeconds(days)
                - TimeUnit.HOURS.toSeconds(hours)
                - TimeUnit.MINUTES.toSeconds(minutes);

        return String.format("%dd %d:%02d:%02d", days, hours, minutes, secs);
    }
}
