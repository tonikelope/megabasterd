package megabasterd;

import java.awt.Component;
import java.sql.SQLException;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Logger.getLogger;
import static megabasterd.DBTools.deleteDownload;
import static megabasterd.MainPanel.THREAD_POOL;

public final class DownloadManager extends TransferenceManager {

    public DownloadManager(MainPanel main_panel) {

        super(main_panel, main_panel.getMax_dl(), main_panel.getView().getStatus_down_label(), main_panel.getView().getjPanel_scroll_down(), main_panel.getView().getClose_all_finished_down_button(), main_panel.getView().getPause_all_down_button(), main_panel.getView().getClean_all_down_menu());
    }

    @Override
    public void remove(Transference download) {

        getScroll_panel().remove(((Download) download).getView());

        getTransference_waitstart_queue().remove(download);

        getTransference_running_list().remove(download);

        getTransference_finished_queue().remove(download);

        if (((Download) download).isProvision_ok()) {

            try {
                deleteDownload(((Download) download).getUrl());
            } catch (SQLException ex) {
                getLogger(DownloadManager.class.getName()).log(SEVERE, null, ex);
            }
        }

        secureNotify();
    }

    @Override
    public void provision(final Transference download) {
        getScroll_panel().add(((Download) download).getView());

        try {

            _provision((Download) download, false);

            secureNotify();

        } catch (MegaAPIException | MegaCrypterAPIException ex) {

            System.out.println("Provision failed! Retrying in separated thread...");

            THREAD_POOL.execute(new Runnable() {
                @Override
                public void run() {

                    try {

                        _provision((Download) download, true);

                    } catch (MegaAPIException | MegaCrypterAPIException ex1) {

                        getLogger(DownloadManager.class.getName()).log(SEVERE, null, ex1);
                    }

                    secureNotify();

                }
            });
        }

    }

    private void _provision(Download download, boolean retry) throws MegaAPIException, MegaCrypterAPIException {

        download.provisionIt(retry);

        if (download.isProvision_ok()) {

            getTransference_waitstart_queue().add(download);

            if (getTransference_provision_queue().isEmpty()) {

                sortTransferenceStartQueue();

                for (Transference down : getTransference_waitstart_queue()) {

                    getScroll_panel().remove((Component) down.getView());
                    getScroll_panel().add((Component) down.getView());
                }

                for (Transference down : getTransference_finished_queue()) {

                    getScroll_panel().remove((Component) down.getView());
                    getScroll_panel().add((Component) down.getView());
                }
            }

        } else {

            getTransference_finished_queue().add(download);
        }
    }

}
