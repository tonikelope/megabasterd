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

import java.sql.SQLException;
import java.util.ArrayList;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public class UploadManager extends TransferenceManager {

    private static final Logger LOG = Logger.getLogger(UploadManager.class.getName());

    private final Object _log_file_lock;

    public UploadManager(MainPanel main_panel) {

        super(main_panel, main_panel.getMax_ul(), main_panel.getView().getStatus_up_label(), main_panel.getView().getjPanel_scroll_up(), main_panel.getView().getClose_all_finished_up_button(), main_panel.getView().getPause_all_up_button(), main_panel.getView().getClean_all_up_menu());

        _log_file_lock = new Object();
    }

    public Object getLog_file_lock() {
        return _log_file_lock;
    }

    @Override
    public void provision(final Transference upload) {
        MiscTools.GUIRun(() -> {
            getScroll_panel().add(((Upload) upload).getView());
        });

        try {
            ((Upload) upload).provisionIt();
        } catch (RuntimeException ex) {
            // Belt-and-braces: provisionIt now catches its own runtime
            // exceptions, but if a future change adds a code path that
            // throws, we must NOT leave the BoundedExecutor task without
            // routing the transference to either aux_queue or
            // finished_queue -- otherwise the UI shows a perpetual
            // "Provisioning..." with no way to recover.
            LOG.log(SEVERE, "Upload provisionIt threw -- routing to finished_queue", ex);
        }

        if (((Upload) upload).isProvision_ok()) {

            increment_total_size(upload.getFile_size());

            getTransference_waitstart_aux_queue().add(upload);

        } else {

            getTransference_finished_queue().add(upload);
        }

        secureNotify();
    }

    @Override
    public void remove(Transference[] uploads) {

        ArrayList<String[]> delete_up = new ArrayList<>();
        final java.util.ArrayList<UploadView> views_to_remove = new java.util.ArrayList<>();

        for (final Transference u : uploads) {

            views_to_remove.add(((Upload) u).getView());

            getTransference_waitstart_queue().remove(u);

            getTransference_running_list().remove(u);

            getTransference_finished_queue().remove(u);

            ((Upload) u).finalizeTotals();

            // Always remove the DB row. Same fix as DownloadManager.remove --
            // cancelled-but-not-closed uploads kept getting resurrected on
            // restart. See #699.
            delete_up.add(new String[]{u.getFile_name(), ((Upload) u).getMa().getFull_email()});
        }

        MiscTools.GUIRun(() -> {
            for (UploadView v : views_to_remove) {
                getScroll_panel().remove(v);
            }
            getScroll_panel().revalidate();
            getScroll_panel().repaint();
        });

        try {
            DBTools.deleteUploads(delete_up.toArray(new String[delete_up.size()][]));
        } catch (SQLException ex) {
            LOG.log(SEVERE, null, ex);
        }

        secureNotify();
    }

    public int copyAllLinksToClipboard() {

        int total = 0;

        ArrayList<String> links = new ArrayList<>();

        String out = "";

        for (Transference t : _transference_waitstart_aux_queue) {
            Upload up = (Upload) t;
            links.add(up.getFile_name() + " [" + up.getMa().getEmail() + "] " + (up.getFolder_link() != null ? up.getFolder_link() : ""));
        }

        for (Transference t : _transference_waitstart_queue) {

            Upload up = (Upload) t;
            links.add(up.getFile_name() + " [" + up.getMa().getEmail() + "] " + (up.getFolder_link() != null ? up.getFolder_link() : ""));
        }

        out += String.join("\r\n", links);

        total += links.size();

        links.clear();

        for (Transference t : _transference_running_list) {

            Upload up = (Upload) t;
            links.add(up.getFile_name() + " [" + up.getMa().getEmail() + "] " + (up.getFolder_link() != null ? up.getFolder_link() : "") + (up.getFile_link() != null ? " " + up.getFile_link() : ""));
        }

        out += String.join("\r\n", links);

        total += links.size();

        links.clear();

        for (Transference t : _transference_finished_queue) {

            Upload up = (Upload) t;
            links.add("(UPLOAD FINISHED) " + up.getFile_name() + " [" + up.getMa().getEmail() + "] " + (up.getFolder_link() != null ? up.getFolder_link() : "") + (up.getFile_link() != null ? " " + up.getFile_link() : ""));
        }

        out += String.join("\r\n", links);

        total += links.size();

        MiscTools.copyTextToClipboard(out);

        return total;

    }

}
