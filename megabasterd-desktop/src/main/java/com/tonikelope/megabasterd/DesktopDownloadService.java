package com.tonikelope.megabasterd;

import com.tonikelope.megabasterd.core.DownloadId;
import com.tonikelope.megabasterd.core.DownloadRequest;
import com.tonikelope.megabasterd.core.DownloadService;
import com.tonikelope.megabasterd.core.DownloadSnapshot;
import com.tonikelope.megabasterd.core.DownloadState;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class DesktopDownloadService implements DownloadService {

    private final MainPanel mainPanel;
    private final Map<DownloadId, Download> downloads;

    DesktopDownloadService(MainPanel mainPanel) {
        this.mainPanel = mainPanel;
        this.downloads = new ConcurrentHashMap<>();
    }

    @Override
    public DownloadId add(DownloadRequest request) {
        return add(request, new MegaAPI());
    }

    public DownloadId add(DownloadRequest request, MegaAPI megaApi) {
        MegaAPI api = megaApi != null ? megaApi : new MegaAPI();
        Download download = new Download(mainPanel, api, request.url(), request.downloadPath(),
                request.fileName(), request.fileKey(), request.fileSize(), request.filePass(),
                request.fileNoExpire(), request.useSlots(), request.restart(),
                request.customChunksDir(), request.priority());
        return enqueue(download);
    }

    public DownloadId enqueue(Download download) {
        DownloadId id = download.getCoreDownloadId();
        downloads.put(id, download);
        mainPanel.getDownload_manager().getTransference_provision_queue().add(download);
        mainPanel.getDownload_manager().secureNotify();
        return id;
    }

    @Override
    public void pause(DownloadId id) {
        Download download = downloads.get(id);
        if (download != null && !download.isPause()) {
            download.pause();
        }
    }

    @Override
    public void resume(DownloadId id) {
        Download download = downloads.get(id);
        if (download != null && download.isPause()) {
            download.pause();
        }
    }

    @Override
    public void cancel(DownloadId id) {
        Download download = downloads.get(id);
        if (download != null) {
            download.stop();
        }
    }

    @Override
    public void restart(DownloadId id) {
        Download download = downloads.get(id);
        if (download != null) {
            download.restart();
        }
    }

    @Override
    public void close(DownloadId id) {
        Download download = downloads.remove(id);
        if (download != null) {
            download.close();
        }
    }

    @Override
    public DownloadSnapshot get(DownloadId id) {
        Download download = downloads.get(id);
        return download != null ? snapshot(download) : null;
    }

    private DownloadSnapshot snapshot(Download download) {
        Long fileSize = null;
        try {
            fileSize = download.getFile_size();
        } catch (RuntimeException ex) {
            fileSize = null;
        }

        return new DownloadSnapshot(download.getCoreDownloadId(), download.getUrl(),
                download.getDownload_path(), download.getFile_name(), fileSize,
                download.getProgress(), state(download), download.getStatus_error());
    }

    private DownloadState state(Download download) {
        if (download.isStatusError()) {
            return DownloadState.FAILED;
        }
        if (download.isCanceled()) {
            return DownloadState.CANCELED;
        }
        if (download.isClosed()) {
            return DownloadState.FINISHED;
        }
        if (mainPanel.getDownload_manager().getTransference_finished_queue().contains(download)) {
            return DownloadState.FINISHED;
        }
        if (download.isPause()) {
            return DownloadState.PAUSED;
        }
        if (mainPanel.getDownload_manager().getTransference_running_list().contains(download)) {
            return DownloadState.RUNNING;
        }
        return DownloadState.QUEUED;
    }
}
