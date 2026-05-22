package com.tonikelope.megabasterd.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class InMemoryDownloadService implements DownloadService {

    private final Map<DownloadId, DownloadSnapshot> downloads = new ConcurrentHashMap<>();

    @Override
    public DownloadId add(DownloadRequest request) {
        DownloadId id = DownloadId.random();
        downloads.put(id, new DownloadSnapshot(id, request.url(), request.downloadPath(),
                request.fileName(), request.fileSize(), 0L, DownloadState.QUEUED, ""));
        return id;
    }

    @Override
    public void pause(DownloadId id) {
        updateState(id, DownloadState.PAUSED);
    }

    @Override
    public void resume(DownloadId id) {
        updateState(id, DownloadState.RUNNING);
    }

    @Override
    public void cancel(DownloadId id) {
        updateState(id, DownloadState.CANCELED);
    }

    @Override
    public void restart(DownloadId id) {
        updateState(id, DownloadState.QUEUED);
    }

    @Override
    public void close(DownloadId id) {
        downloads.remove(id);
    }

    @Override
    public DownloadSnapshot get(DownloadId id) {
        return downloads.get(id);
    }

    private void updateState(DownloadId id, DownloadState state) {
        DownloadSnapshot old = downloads.get(id);
        if (old != null) {
            downloads.put(id, new DownloadSnapshot(old.id(), old.url(), old.downloadPath(),
                    old.fileName(), old.fileSize(), old.progress(), state, old.statusText()));
        }
    }
}
