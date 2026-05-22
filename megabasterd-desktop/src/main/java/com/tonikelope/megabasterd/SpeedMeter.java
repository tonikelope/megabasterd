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

import static com.tonikelope.megabasterd.MiscTools.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;

/**
 *
 * @author tonikelope
 */
public class SpeedMeter implements Runnable {

    public static final int SLEEP = 3000;
    public static final int CHUNK_SPEED_QUEUE_MAX_SIZE = 20;
    private static final Logger LOG = Logger.getLogger(SpeedMeter.class.getName());
    private final JLabel _speed_label;
    private final JLabel _rem_label;
    private final TransferenceManager _trans_manager;
    private final ConcurrentHashMap<Transference, HashMap> _transferences;
    private long _speed_counter;
    private long _speed_acumulator;
    private volatile long _max_avg_global_speed;

    SpeedMeter(TransferenceManager trans_manager, JLabel sp_label, JLabel rem_label) {
        _speed_label = sp_label;
        _rem_label = rem_label;
        _trans_manager = trans_manager;
        _transferences = new ConcurrentHashMap<>();
        _speed_counter = 0L;
        _speed_acumulator = 0L;
        _max_avg_global_speed = 0L;
    }

    private long _getAvgGlobalSpeed() {
        return Math.round((double) _speed_acumulator / _speed_counter);
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
        final boolean[] visible_state = {false};

        GUIRun(() -> {
            _speed_label.setVisible(true);
            _rem_label.setVisible(true);
            _speed_label.setText("");
            _rem_label.setText("");
        });

        do {

            try {

                final java.util.LinkedHashMap<TransferenceView, String> per_row_text = new java.util.LinkedHashMap<>();
                final String[] new_speed_label = {null};
                final String[] new_rem_label = {null};
                final boolean[] clear_both = {false};

                if (!_transferences.isEmpty()) {

                    visible_state[0] = true;

                    long global_speed = 0L;

                    for (Map.Entry<Transference, HashMap> trans_info : _transferences.entrySet()) {

                        long trans_sp = calcTransferenceSpeed(trans_info.getKey(), trans_info.getValue());

                        if (trans_sp >= 0) {
                            global_speed += trans_sp;
                        }

                        TransferenceView view = trans_info.getKey().getView();

                        if (view != null) {
                            per_row_text.put(view, trans_sp > 0 ? (formatBytes(trans_sp) + "/s") : "------");
                        }
                    }

                    long global_size = _trans_manager.get_total_size();

                    long global_progress = _trans_manager.get_total_progress();

                    if (global_speed > 0) {

                        _speed_counter++;
                        _speed_acumulator += global_speed;

                        long avg_global_speed = _getAvgGlobalSpeed();

                        if (avg_global_speed > _max_avg_global_speed) {
                            _max_avg_global_speed = avg_global_speed;
                        }

                        new_speed_label[0] = formatBytes(global_speed) + "/s";

                        new_rem_label[0] = formatBytes(global_progress) + "/" + formatBytes(global_size) + " @ " + formatBytes(avg_global_speed) + "/s @ " + calcRemTime(avg_global_speed > 0 ? (long) Math.floor((global_size - global_progress) / (double) avg_global_speed) : 0);

                    } else {

                        new_speed_label[0] = "------";
                        new_rem_label[0] = formatBytes(global_progress) + "/" + formatBytes(global_size) + " @ --d --:--:--";

                    }

                } else if (visible_state[0]) {

                    clear_both[0] = true;
                    visible_state[0] = false;
                }

                GUIRun(() -> {
                    for (Map.Entry<TransferenceView, String> e : per_row_text.entrySet()) {
                        e.getKey().updateSpeed(e.getValue(), true);
                    }
                    if (clear_both[0]) {
                        _speed_label.setText("");
                        _rem_label.setText("");
                    } else {
                        if (new_speed_label[0] != null && !new_speed_label[0].equals(_speed_label.getText())) {
                            _speed_label.setText(new_speed_label[0]);
                        }
                        if (new_rem_label[0] != null && !new_rem_label[0].equals(_rem_label.getText())) {
                            _rem_label.setText(new_rem_label[0]);
                        }
                    }
                });

                Thread.sleep(SLEEP);

            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                LOG.log(Level.FINE, "SpeedMeter interrupted");
                return;
            }

        } while (true);
    }
}
