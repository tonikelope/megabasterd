package com.tonikelope.megabasterd;

import static com.tonikelope.megabasterd.MiscTools.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import org.apache.commons.collections4.queue.CircularFifoQueue;

/**
 *
 * @author tonikelope
 */
public final class SpeedMeter implements Runnable {

    public static final int SLEEP = 3000;
    public static final int CHUNK_SPEED_QUEUE_MAX_SIZE = 10;
    private static final Logger LOG = Logger.getLogger(SpeedMeter.class.getName());
    private final JLabel _speed_label;
    private final JLabel _rem_label;
    private final TransferenceManager _trans_manager;
    private final ConcurrentHashMap<Transference, HashMap> _transferences;
    private long _speed_counter;
    private long _speed_acumulator;
    private volatile long _max_avg_global_speed;
    private volatile long _avg_chunk_speed;
    private final CircularFifoQueue _chunk_speed_queue;

    SpeedMeter(TransferenceManager trans_manager, JLabel sp_label, JLabel rem_label) {
        _speed_label = sp_label;
        _rem_label = rem_label;
        _trans_manager = trans_manager;
        _transferences = new ConcurrentHashMap<>();
        _chunk_speed_queue = new CircularFifoQueue(CHUNK_SPEED_QUEUE_MAX_SIZE);
        _speed_counter = 0L;
        _speed_acumulator = 0L;
        _max_avg_global_speed = 0L;
        _avg_chunk_speed = -1;
    }

    private long _getAvgGlobalSpeed() {
        return Math.round((double) _speed_acumulator / _speed_counter);
    }

    public void setAvg_chunk_speed(long _avg_chunk_speed) {
        this._avg_chunk_speed = _avg_chunk_speed;
    }

    public long getAvg_chunk_speed() {
        return _avg_chunk_speed;
    }

    public void update_avg_chunk_speed(long speed) {

        synchronized (_chunk_speed_queue) {

            this._chunk_speed_queue.add(speed);

            if (_chunk_speed_queue.size() == _chunk_speed_queue.maxSize()) {

                long acumulador = 0;

                Iterator i = _chunk_speed_queue.iterator();

                while (i.hasNext()) {

                    acumulador += (long) i.next();
                }

                _avg_chunk_speed = Math.round(((double) acumulador) / _chunk_speed_queue.size());
            }
        }
    }

    public void attachTransference(Transference transference) {

        HashMap<String, Object> properties = new HashMap<>();

        properties.put("last_progress", transference.getProgress());
        properties.put("no_data_count", 0);

        _transferences.put(transference, properties);

    }

    public void detachTransference(Transference transference) {

        if (_transferences.containsKey(transference)) {
            _transferences.remove(transference);
        }

    }

    public long getMaxAvgGlobalSpeed() {

        return _max_avg_global_speed;
    }

    private String calcRemTime(long seconds) {
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

    private long calcTransferenceSpeed(Transference transference, HashMap properties) {

        long sp, progress = transference.getProgress(), last_progress = (long) properties.get("last_progress");

        int no_data_count = (int) properties.get("no_data_count");

        if (transference.isPaused()) {

            sp = 0;

        } else if (progress > last_progress) {

            double sleep_time = ((double) SLEEP * (no_data_count + 1)) / 1000;

            double current_speed = (progress - last_progress) / sleep_time;

            sp = last_progress > 0 ? Math.round(current_speed) : 0;

            last_progress = progress;

            no_data_count = 0;

        } else if (transference instanceof Download) {

            sp = -1;

            no_data_count++;

        } else {

            sp = 0;

            no_data_count++;
        }

        properties.put("last_progress", last_progress);

        properties.put("no_data_count", no_data_count);

        _transferences.put(transference, properties);

        return sp;
    }

    @Override
    public void run() {
        long global_speed, global_progress, global_size;
        boolean visible = false;

        _speed_label.setVisible(true);
        _rem_label.setVisible(true);
        _speed_label.setText("");
        _rem_label.setText("");

        do {

            try {

                if (!_transferences.isEmpty()) {

                    visible = true;

                    global_speed = 0L;

                    for (Map.Entry<Transference, HashMap> trans_info : _transferences.entrySet()) {

                        long trans_sp = calcTransferenceSpeed(trans_info.getKey(), trans_info.getValue());

                        if (trans_sp >= 0) {
                            global_speed += trans_sp;
                        }

                        if (trans_sp > 0) {

                            trans_info.getKey().getView().updateSpeed(formatBytes(trans_sp) + "/s", true);

                        } else {

                            trans_info.getKey().getView().updateSpeed("------", true);

                        }
                    }

                    global_size = _trans_manager.get_total_size();

                    global_progress = _trans_manager.get_total_progress();

                    if (global_speed > 0) {

                        _speed_counter++;
                        _speed_acumulator += global_speed;

                        long avg_global_speed = _getAvgGlobalSpeed();

                        if (avg_global_speed > _max_avg_global_speed) {
                            _max_avg_global_speed = avg_global_speed;
                        }

                        _speed_label.setText(formatBytes(global_speed) + "/s");

                        _rem_label.setText(formatBytes(global_progress) + "/" + formatBytes(global_size) + " @ " + formatBytes(avg_global_speed) + "/s @ " + calcRemTime((long) Math.floor((global_size - global_progress) / avg_global_speed)));

                    } else {

                        _speed_label.setText("------");
                        _rem_label.setText(formatBytes(global_progress) + "/" + formatBytes(global_size) + " @ --d --:--:--");

                    }

                } else if (visible) {

                    _speed_label.setText("");
                    _rem_label.setText("");
                    visible = false;
                }

                Thread.sleep(SLEEP);

            } catch (InterruptedException ex) {
                LOG.log(Level.SEVERE, ex.getMessage());
            }

        } while (true);
    }
}
