package com.tonikelope.megabasterd.core;

public interface DownloadService {

    DownloadId add(DownloadRequest request);

    void pause(DownloadId id);

    void resume(DownloadId id);

    void cancel(DownloadId id);

    void restart(DownloadId id);

    void close(DownloadId id);

    DownloadSnapshot get(DownloadId id);
}
