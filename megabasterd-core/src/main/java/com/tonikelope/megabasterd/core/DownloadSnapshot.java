package com.tonikelope.megabasterd.core;

public final class DownloadSnapshot {

    private final DownloadId id;
    private final String url;
    private final String downloadPath;
    private final String fileName;
    private final Long fileSize;
    private final long progress;
    private final DownloadState state;
    private final String statusText;

    public DownloadSnapshot(DownloadId id, String url, String downloadPath, String fileName,
            Long fileSize, long progress, DownloadState state, String statusText) {
        this.id = id;
        this.url = url;
        this.downloadPath = downloadPath;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.progress = progress;
        this.state = state != null ? state : DownloadState.UNKNOWN;
        this.statusText = statusText != null ? statusText : "";
    }

    public DownloadId id() {
        return id;
    }

    public String url() {
        return url;
    }

    public String downloadPath() {
        return downloadPath;
    }

    public String fileName() {
        return fileName;
    }

    public Long fileSize() {
        return fileSize;
    }

    public long progress() {
        return progress;
    }

    public DownloadState state() {
        return state;
    }

    public String statusText() {
        return statusText;
    }
}
