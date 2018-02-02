package com.tonikelope.megabasterd;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import static com.tonikelope.megabasterd.MiscTools.*;

/**
 *
 * @author tonikelope
 */
public final class GlobalSpeedMeter implements Runnable {

    public static final int SLEEP = 3000;
    private final JLabel _speed_label;
    private final JLabel _rem_label;
    private final TransferenceManager _trans_manager;
    private final ConcurrentHashMap<Transference, HashMap> _transferences;

    GlobalSpeedMeter(TransferenceManager trans_manager, JLabel sp_label, JLabel rem_label) {
        _speed_label = sp_label;
        _rem_label = rem_label;
        _trans_manager = trans_manager;
        _transferences = new ConcurrentHashMap<>();
    }

    public void attachTransference(Transference transference) {

        HashMap<String, Object> properties = new HashMap<>();

        properties.put("last_progress", -1L);
        properties.put("no_data_count", 0);

        _transferences.put(transference, properties);

    }

    public void detachTransference(Transference transference) {

        if (_transferences.containsKey(transference)) {
            _transferences.remove(transference);
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

    private long calcTransferenceSpeed(Transference transference, HashMap properties) {

        long sp, progress = transference.getProgress(), last_progress = (long) properties.get("last_progress");
        int no_data_count = (int) properties.get("no_data_count");

        if (transference.isPaused()) {

            transference.getView().updateSpeed("------", true);

            sp = 0L;

        } else if (progress > last_progress) {

            double sleep_time = ((double) SLEEP * (no_data_count + 1)) / 1000;

            double current_speed = (progress - last_progress) / sleep_time;

            sp = last_progress > 0 ? Math.round(current_speed) : 0;

            if (sp > 0) {

                transference.getView().updateSpeed(formatBytes(sp) + "/s", true);
            }

            last_progress = progress;

            no_data_count = 0;

        } else if (transference instanceof Download && ((Download) transference).isError509()) {

            transference.getView().updateSpeed("BANDWIDTH LIMIT ERROR!", true);

            sp = 0L;

            no_data_count++;

        } else {

            transference.getView().updateSpeed("------", true);

            sp = 0L;

            no_data_count++;
        }

        properties.put("last_progress", last_progress);

        properties.put("no_data_count", no_data_count);

        _transferences.put(transference, properties);

        return sp;
    }

    @Override
    public void run() {
        long sp, progress;
        boolean visible = false;

        _speed_label.setVisible(true);
        _rem_label.setVisible(true);
        _speed_label.setText("");
        _rem_label.setText("");

        do {

            try {

                if (!_transferences.isEmpty()) {

                    visible = true;

                    sp = 0L;

                    progress = 0L;

                    for (Map.Entry<Transference, HashMap> entry : _transferences.entrySet()) {

                        sp += calcTransferenceSpeed(entry.getKey(), entry.getValue());
                        progress += entry.getKey().getProgress();
                    }

                    for (Transference transference : _trans_manager.getTransference_finished_queue()) {

                        if (!transference.isStatusError()) {

                            progress += transference.getProgress();
                        }
                    }

                    if (sp > 0) {

                        _speed_label.setText(formatBytes(sp) + "/s");
                        _rem_label.setText(formatBytes(progress) + "/" + formatBytes(_trans_manager.getTotal_transferences_size()) + " @ " + calculateRemTime((long) Math.floor((_trans_manager.getTotal_transferences_size() - progress) / sp)));

                    } else {

                        _speed_label.setText("------");
                        _rem_label.setText(formatBytes(progress) + "/" + formatBytes(_trans_manager.getTotal_transferences_size()) + " @ --d --:--:--");

                    }

                } else if (visible) {

                    _speed_label.setText("");
                    _rem_label.setText("");
                    visible = false;
                }

                Thread.sleep(SLEEP);

            } catch (InterruptedException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }

        } while (true);
    }
}
