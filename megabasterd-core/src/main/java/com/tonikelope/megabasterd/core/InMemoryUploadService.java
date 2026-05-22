package com.tonikelope.megabasterd.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class InMemoryUploadService implements UploadService {

    private final Map<UploadId, UploadSnapshot> uploads = new ConcurrentHashMap<>();

    @Override
    public UploadId add(UploadRequest request) {
        UploadId id = UploadId.random();
        uploads.put(id, new UploadSnapshot(id, request.fileName(), request.accountEmail(),
                request.parentNode(), request.rootNode(), request.folderLink(), null, null,
                0L, UploadState.QUEUED, ""));
        return id;
    }

    @Override
    public void pause(UploadId id) {
        updateState(id, UploadState.PAUSED);
    }

    @Override
    public void resume(UploadId id) {
        updateState(id, UploadState.RUNNING);
    }

    @Override
    public void cancel(UploadId id) {
        updateState(id, UploadState.CANCELED);
    }

    @Override
    public void restart(UploadId id) {
        updateState(id, UploadState.QUEUED);
    }

    @Override
    public void close(UploadId id) {
        uploads.remove(id);
    }

    @Override
    public UploadSnapshot get(UploadId id) {
        return uploads.get(id);
    }

    private void updateState(UploadId id, UploadState state) {
        UploadSnapshot old = uploads.get(id);
        if (old != null) {
            uploads.put(id, new UploadSnapshot(old.id(), old.fileName(), old.accountEmail(),
                    old.parentNode(), old.rootNode(), old.folderLink(), old.fileLink(),
                    old.fileSize(), old.progress(), state, old.statusText()));
        }
    }
}
