package com.tonikelope.megabasterd;

import static com.tonikelope.megabasterd.MiscTools.*;
import java.awt.Component;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public final class UploadManager extends TransferenceManager {

    private static final Logger LOG = Logger.getLogger(UploadManager.class.getName());

    private final ConcurrentLinkedQueue<Upload> _finishing_uploads_queue;

    private final Object _log_file_lock;

    public UploadManager(MainPanel main_panel) {

        super(main_panel, main_panel.getMax_ul(), main_panel.getView().getStatus_up_label(), main_panel.getView().getjPanel_scroll_up(), main_panel.getView().getClose_all_finished_up_button(), main_panel.getView().getPause_all_up_button(), main_panel.getView().getClean_all_up_menu());
        _finishing_uploads_queue = new ConcurrentLinkedQueue<>();

        _log_file_lock = new Object();
    }

    public Object getLog_file_lock() {
        return _log_file_lock;
    }

    public ConcurrentLinkedQueue<Upload> getFinishing_uploads_queue() {
        return _finishing_uploads_queue;
    }

    @Override
    public void provision(final Transference upload) {
        swingInvoke(
                new Runnable() {
            @Override
            public void run() {
                getScroll_panel().add(((Upload) upload).getView());
            }
        });

        ((Upload) upload).provisionIt();

        if (((Upload) upload).isProvision_ok()) {

            increment_total_size(upload.getFile_size());

            if (upload.isRestart()) {
                synchronized (getWait_queue_lock()) {

                    ConcurrentLinkedQueue<Transference> aux = new ConcurrentLinkedQueue<>();

                    aux.addAll(getTransference_waitstart_queue());

                    getTransference_waitstart_queue().clear();

                    getTransference_waitstart_queue().add(upload);

                    getTransference_waitstart_queue().addAll(aux);

                    for (final Transference t1 : getTransference_waitstart_queue()) {

                        swingInvoke(
                                new Runnable() {
                            @Override
                            public void run() {
                                getScroll_panel().remove((Component) t1.getView());
                                getScroll_panel().add((Component) t1.getView());
                            }
                        });

                    }

                    for (final Transference t2 : getTransference_finished_queue()) {

                        swingInvoke(
                                new Runnable() {
                            @Override
                            public void run() {
                                getScroll_panel().remove((Component) t2.getView());
                                getScroll_panel().add((Component) t2.getView());
                            }
                        });
                    }

                }
            } else {
                getTransference_waitstart_queue().add(upload);
            }

        } else {

            getTransference_finished_queue().add(upload);
        }

        secureNotify();
    }

    @Override
    public void remove(Transference[] uploads) {

        ArrayList<String[]> delete_up = new ArrayList<>();

        for (final Transference u : uploads) {

            swingInvoke(
                    new Runnable() {
                @Override
                public void run() {
                    getScroll_panel().remove(((Upload) u).getView());
                }
            });

            getTransference_waitstart_queue().remove(u);

            if (getTransference_waitstart_queue().isEmpty()) {
                _frozen = false;
            }

            getTransference_running_list().remove(u);

            getTransference_finished_queue().remove(u);

            increment_total_size(-1 * u.getFile_size());

            increment_total_progress(-1 * u.getProgress());

            delete_up.add(new String[]{u.getFile_name(), ((Upload) u).getMa().getFull_email()});
        }

        try {
            DBTools.deleteUploads(delete_up.toArray(new String[delete_up.size()][]));
        } catch (SQLException ex) {
            LOG.log(SEVERE, null, ex);
        }

        secureNotify();
    }

}
