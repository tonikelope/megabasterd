package com.tonikelope.megabasterd.core;

public final class UploadSnapshot {

    private final UploadId id;
    private final String fileName;
    private final String accountEmail;
    private final String parentNode;
    private final String rootNode;
    private final String folderLink;
    private final String fileLink;
    private final Long fileSize;
    private final long progress;
    private final UploadState state;
    private final String statusText;

    public UploadSnapshot(UploadId id, String fileName, String accountEmail, String parentNode,
            String rootNode, String folderLink, String fileLink, Long fileSize, long progress,
            UploadState state, String statusText) {
        this.id = id;
        this.fileName = fileName;
        this.accountEmail = accountEmail;
        this.parentNode = parentNode;
        this.rootNode = rootNode;
        this.folderLink = folderLink;
        this.fileLink = fileLink;
        this.fileSize = fileSize;
        this.progress = progress;
        this.state = state != null ? state : UploadState.UNKNOWN;
        this.statusText = statusText != null ? statusText : "";
    }

    public UploadId id() {
        return id;
    }

    public String fileName() {
        return fileName;
    }

    public String accountEmail() {
        return accountEmail;
    }

    public String parentNode() {
        return parentNode;
    }

    public String rootNode() {
        return rootNode;
    }

    public String folderLink() {
        return folderLink;
    }

    public String fileLink() {
        return fileLink;
    }

    public Long fileSize() {
        return fileSize;
    }

    public long progress() {
        return progress;
    }

    public UploadState state() {
        return state;
    }

    public String statusText() {
        return statusText;
    }
}
