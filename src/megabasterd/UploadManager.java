package megabasterd;

import java.awt.Component;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;
import static megabasterd.MiscTools.*;

/**
 *
 * @author tonikelope
 */
public final class UploadManager extends TransferenceManager {

    public UploadManager(MainPanel main_panel) {

        super(main_panel, main_panel.getMax_ul(), main_panel.getView().getStatus_up_label(), main_panel.getView().getjPanel_scroll_up(), main_panel.getView().getClose_all_finished_up_button(), main_panel.getView().getPause_all_up_button(), main_panel.getView().getClean_all_up_menu());

    }

    @Override
    public void provision(Transference upload) {
        getScroll_panel().add(((Upload) upload).getView());

        ((Upload) upload).provisionIt();

        if (((Upload) upload).isProvision_ok()) {

            _total_transferences_size += upload.getFile_size();

            getTransference_waitstart_queue().add(upload);

            if (getTransference_provision_queue().isEmpty()) {

                sortTransferenceStartQueue();

                for (Transference up : getTransference_waitstart_queue()) {

                    getScroll_panel().remove((Component) up.getView());
                    getScroll_panel().add((Component) up.getView());
                }

                for (Transference up : getTransference_finished_queue()) {

                    getScroll_panel().remove((Component) up.getView());
                    getScroll_panel().add((Component) up.getView());
                }
            }
        } else {

            getTransference_finished_queue().add(upload);
        }

        secureNotify();
    }

    @Override
    public void remove(Transference[] uploads) {

        ArrayList<String[]> delete_up = new ArrayList<>();

        for (Transference u : uploads) {

            getScroll_panel().remove(((Upload) u).getView());

            getTransference_waitstart_queue().remove(u);

            getTransference_running_list().remove(u);

            getTransference_finished_queue().remove(u);

            _total_transferences_size -= u.getFile_size();

            delete_up.add(new String[]{u.getFile_name(), ((Upload) u).getMa().getFull_email()});

            try {

                File temp_file = new File("." + HashString("SHA-1", u.getFile_name()));

                if (temp_file.exists()) {

                    temp_file.delete();
                }

            } catch (UnsupportedEncodingException | NoSuchAlgorithmException ex) {
                Logger.getLogger(getClass().getName()).log(SEVERE, null, ex);
            }

        }

        try {
            DBTools.deleteUploads(delete_up.toArray(new String[delete_up.size()][]));
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(SEVERE, null, ex);
        }

        secureNotify();
    }

}
