package megabasterd;

import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;
import static megabasterd.MiscTools.formatBytes;

public final class SpeedMeter implements Runnable {

    public static final int SPEED_BUFFER_MAX_SIZE = 12;
    private final Transference _transference;
    private final GlobalSpeedMeter _gspeed;
    private volatile long _lastSpeed;
    private volatile boolean _exit;
    private final ArrayDeque<Long> _speeds;
    private volatile boolean _clearSpeedBuffer;

    SpeedMeter(Transference transference, GlobalSpeedMeter gspeed) {
        _transference = transference;
        _lastSpeed = 0;
        _gspeed = gspeed;
        _exit = false;
        _speeds = new ArrayDeque<>();
    }

    public Transference getTransference() {
        return _transference;
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

    private long calcAverageSpeed(long sp) {

        if (_clearSpeedBuffer) {

            _speeds.clear();

            _clearSpeedBuffer = false;
        }

        _speeds.add(sp);

        if (_speeds.size() > SPEED_BUFFER_MAX_SIZE) {

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

        long last_progress, progress, sp, avgSp, sleepTime, wakeupTime;

        _gspeed.attachSpeedMeter(this);

        _transference.getView().updateSpeed("------", true);

        _transference.getView().updateRemainingTime("--d --:--:--", true);

        last_progress = _transference.getProgress();

        while (!_exit) {

            if (!_exit) {

                sleepTime = System.currentTimeMillis();

                _gspeed.secureMultiWait();

                wakeupTime = System.currentTimeMillis();

                progress = _transference.getProgress();

                if (_transference.isPaused()) {

                    _transference.getView().updateSpeed("------", true);

                    _transference.getView().updateRemainingTime("--d --:--:--", true);

                    _gspeed.getSpeedMap().put(this, 0L);

                    _gspeed.secureNotify();

                } else if (progress > last_progress) {

                    double current_speed = (progress - last_progress) / (((double) (wakeupTime - sleepTime)) / 1000);

                    last_progress = progress;

                    sp = Math.round(current_speed);

                    avgSp = calcAverageSpeed(sp);

                    if (sp > 0) {

                        _transference.getView().updateSpeed(formatBytes(sp) + "/s", true);

                        _transference.getView().updateRemainingTime(calculateRemTime((long) Math.floor((_transference.getFile_size() - progress) / avgSp)), true);

                        _gspeed.getSpeedMap().put(this, sp);

                        _gspeed.secureNotify();
                    }

                } else {

                    _transference.getView().updateSpeed("------", true);

                    _transference.getView().updateRemainingTime("--d --:--:--", true);

                    _gspeed.getSpeedMap().put(this, 0L);

                    _gspeed.secureNotify();
                }
            }
        }

        _gspeed.detachSpeedMeter(this);
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
