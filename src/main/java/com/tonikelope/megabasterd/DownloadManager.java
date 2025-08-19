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

import static com.tonikelope.megabasterd.DBTools.*;
import static com.tonikelope.megabasterd.MainPanel.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author tonikelope
 */
public class DownloadManager extends TransferenceManager {

    private static final Logger LOG = Logger.getLogger(DownloadManager.class.getName());

    private static final ExecutorService DB_EXECUTOR =
    Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "DB-IO");
        t.setDaemon(true);
        return t;
    });
    
    public DownloadManager(MainPanel main_panel) {

        super(main_panel, main_panel.getMax_dl(), main_panel.getView().getStatus_down_label(), null, main_panel.getView().getClose_all_finished_down_button(), main_panel.getView().getPause_all_down_button(), main_panel.getView().getClean_all_down_menu());
    }

    public synchronized void forceResetAllChunks() {
        THREAD_POOL.execute(() -> {

            LinkedBlockingQueue<Transference> transference_running_list = getMain_panel().getDownload_manager().getTransference_running_list();

            if (!transference_running_list.isEmpty()) {
                transference_running_list.forEach((transference) -> {

                    ArrayList<ChunkDownloader> chunkWorkers = ((Download) transference).getChunkworkers();

                    chunkWorkers.forEach(ChunkDownloader::RESET_CURRENT_CHUNK);

                });

                MiscTools.GUIRun(() -> getMain_panel().getView().getForce_chunk_reset_button().setEnabled(true));

                JOptionPane.showMessageDialog(getMain_panel().getView(), LabelTranslatorSingleton.getInstance().translate("CURRENT DOWNLOAD CHUNKS RESET!"));
            }

        });
    }

    public int copyAllLinksToClipboard() {

        int total = 0;

        ArrayList<String> links = new ArrayList<>();

        String out = "***PROVISIONING DOWNLOADS***\r\n\r\n";

        for (Transference t : _transference_provision_queue) {

            links.add(((Download) t).getUrl());
        }

        out += String.join("\r\n", links);

        total += links.size();

        links.clear();

        out += "\r\n\r\n***WAITING DOWNLOADS***\r\n\r\n";

        for (Transference t : _transference_waitstart_aux_queue) {

            links.add(((Download) t).getUrl());
        }

        for (Transference t : _transference_waitstart_queue) {

            links.add(((Download) t).getUrl());
        }

        out += String.join("\r\n", links);

        total += links.size();

        links.clear();

        out += "\r\n\r\n***RUNNING DOWNLOADS***\r\n\r\n";

        for (Transference t : _transference_running_list) {

            links.add(((Download) t).getUrl());
        }

        out += String.join("\r\n", links);

        total += links.size();

        links.clear();

        out += "\r\n\r\n***FINISHED DOWNLOADS***\r\n\r\n";

        for (Transference t : _transference_finished_queue) {

            links.add(((Download) t).getUrl());
        }

        out += String.join("\r\n", links);

        total += links.size();

        MiscTools.copyTextToClipboard(out);

        return total;

    }

    @Override
    public void remove(Transference[] downloads) {

        ArrayList<String> delete_down = new ArrayList<>();

        for (final Transference d : downloads) {

            MiscTools.GUIRun(() -> getDownload_panel().removeDownload((Download) d));

            getTransference_waitstart_queue().remove(d);
            getTransference_running_list().remove(d);
            getTransference_finished_queue().remove(d);

            if (((Download) d).isProvision_ok()) {

                increment_total_size(-1 * d.getFile_size());

                increment_total_progress(-1 * d.getProgress());

                if (!d.isCanceled() || d.isClosed()) {
                    delete_down.add(((Download) d).getUrl());
                }
            }
        }

        final String[] urlsToDelete = delete_down.toArray(new String[0]);
        DB_EXECUTOR.execute(() -> {
            try {
                deleteDownloads(urlsToDelete);
            } catch (SQLException ex) {
                LOG.log(SEVERE, null, ex);
            } 
        });

        secureNotify();
    }

    @Override
    public void provision(final Transference download) {
        MiscTools.GUIRun(() -> getDownload_panel().addDownload((Download) download));

        try {

            _provision((Download) download, false);

            secureNotify();

        } catch (APIException ex) {

            LOG.log(Level.INFO, "{0} Provision failed! Retrying in separated thread...", Thread.currentThread().getName());

            THREAD_POOL.execute(() -> {
                try {

                    _provision((Download) download, true);

                } catch (APIException ex1) {

                    LOG.log(SEVERE, null, ex1);
                }

                secureNotify();
            });
        }

    }

    private void _provision(Download download, boolean retry) throws APIException {

        download.provisionIt(retry);

        if (download.isProvision_ok()) {

            increment_total_size(download.getFile_size());

            getTransference_waitstart_aux_queue().add(download);

        } else {
            getTransference_finished_queue().add(download);
        }
    }

}
