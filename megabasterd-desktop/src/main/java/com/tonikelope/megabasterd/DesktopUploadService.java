package com.tonikelope.megabasterd;

import com.tonikelope.megabasterd.core.UploadId;
import com.tonikelope.megabasterd.core.UploadRequest;
import com.tonikelope.megabasterd.core.UploadService;
import com.tonikelope.megabasterd.core.UploadSnapshot;
import com.tonikelope.megabasterd.core.UploadState;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class DesktopUploadService implements UploadService {

    private final MainPanel mainPanel;
    private final Map<UploadId, Upload> uploads;

    DesktopUploadService(MainPanel mainPanel) {
        this.mainPanel = mainPanel;
        this.uploads = new ConcurrentHashMap<>();
    }

    @Override
    public UploadId add(UploadRequest request) {
        return add(request, null);
    }

    public UploadId add(UploadRequest request, MegaAPI megaApi) {
        Upload upload = new Upload(mainPanel, megaApi, request.fileName(), request.parentNode(),
                request.uploadKey(), request.uploadUrl(), request.rootNode(), request.shareKey(),
                request.folderLink(), request.priority());
        return enqueue(upload);
    }

    public UploadId enqueue(Upload upload) {
        UploadId id = upload.getCoreUploadId();
        uploads.put(id, upload);
        mainPanel.getUpload_manager().getTransference_provision_queue().add(upload);
        mainPanel.getUpload_manager().secureNotify();
        return id;
    }

    @Override
    public void pause(UploadId id) {
        Upload upload = uploads.get(id);
        if (upload != null && !upload.isPause()) {
            upload.pause();
        }
    }

    @Override
    public void resume(UploadId id) {
        Upload upload = uploads.get(id);
        if (upload != null && upload.isPause()) {
            upload.pause();
        }
    }

    @Override
    public void cancel(UploadId id) {
        Upload upload = uploads.get(id);
        if (upload != null) {
            upload.stop();
        }
    }

    @Override
    public void restart(UploadId id) {
        Upload upload = uploads.get(id);
        if (upload != null) {
            upload.restart();
        }
    }

    @Override
    public void close(UploadId id) {
        Upload upload = uploads.remove(id);
        if (upload != null) {
            upload.close();
        }
    }

    @Override
    public UploadSnapshot get(UploadId id) {
        Upload upload = uploads.get(id);
        return upload != null ? snapshot(upload) : null;
    }

    private UploadSnapshot snapshot(Upload upload) {
        Long fileSize = null;
        try {
            fileSize = upload.getFile_size();
        } catch (RuntimeException ex) {
            fileSize = null;
        }

        String accountEmail = upload.getMa() != null ? upload.getMa().getFull_email() : null;
        return new UploadSnapshot(upload.getCoreUploadId(), upload.getFile_name(), accountEmail,
                upload.getParent_node(), upload.getRoot_node(), upload.getFolder_link(),
                upload.getFile_link(), fileSize, upload.getProgress(), state(upload),
                upload.getStatus_error());
    }

    private UploadState state(Upload upload) {
        if (upload.isStatusError()) {
            return UploadState.FAILED;
        }
        if (upload.isCanceled()) {
            return UploadState.CANCELED;
        }
        if (upload.isClosed()) {
            return UploadState.FINISHED;
        }
        if (mainPanel.getUpload_manager().getTransference_finished_queue().contains(upload)) {
            return UploadState.FINISHED;
        }
        if (upload.isPause()) {
            return UploadState.PAUSED;
        }
        if (mainPanel.getUpload_manager().getTransference_running_list().contains(upload)) {
            return UploadState.RUNNING;
        }
        return UploadState.QUEUED;
    }
}
