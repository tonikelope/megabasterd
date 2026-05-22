package com.tonikelope.megabasterd.core;

public interface UploadService {

    UploadId add(UploadRequest request);

    void pause(UploadId id);

    void resume(UploadId id);

    void cancel(UploadId id);

    void restart(UploadId id);

    void close(UploadId id);

    UploadSnapshot get(UploadId id);
}
