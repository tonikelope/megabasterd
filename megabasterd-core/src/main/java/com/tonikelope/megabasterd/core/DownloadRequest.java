package com.tonikelope.megabasterd.core;

public final class DownloadRequest {

    private final String url;
    private final String downloadPath;
    private final String fileName;
    private final String fileKey;
    private final Long fileSize;
    private final String filePass;
    private final String fileNoExpire;
    private final String accountEmail;
    private final boolean useSlots;
    private final boolean restart;
    private final String customChunksDir;
    private final boolean priority;

    private DownloadRequest(Builder builder) {
        if (builder.url == null || builder.url.trim().isEmpty()) {
            throw new IllegalArgumentException("Download url cannot be blank");
        }
        if (builder.downloadPath == null || builder.downloadPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Download path cannot be blank");
        }
        this.url = builder.url;
        this.downloadPath = builder.downloadPath;
        this.fileName = builder.fileName;
        this.fileKey = builder.fileKey;
        this.fileSize = builder.fileSize;
        this.filePass = builder.filePass;
        this.fileNoExpire = builder.fileNoExpire;
        this.accountEmail = builder.accountEmail;
        this.useSlots = builder.useSlots;
        this.restart = builder.restart;
        this.customChunksDir = builder.customChunksDir;
        this.priority = builder.priority;
    }

    public static Builder builder(String url, String downloadPath) {
        return new Builder(url, downloadPath);
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

    public String fileKey() {
        return fileKey;
    }

    public Long fileSize() {
        return fileSize;
    }

    public String filePass() {
        return filePass;
    }

    public String fileNoExpire() {
        return fileNoExpire;
    }

    public String accountEmail() {
        return accountEmail;
    }

    public boolean useSlots() {
        return useSlots;
    }

    public boolean restart() {
        return restart;
    }

    public String customChunksDir() {
        return customChunksDir;
    }

    public boolean priority() {
        return priority;
    }

    public static final class Builder {

        private final String url;
        private final String downloadPath;
        private String fileName;
        private String fileKey;
        private Long fileSize;
        private String filePass;
        private String fileNoExpire;
        private String accountEmail;
        private boolean useSlots = true;
        private boolean restart;
        private String customChunksDir;
        private boolean priority;

        private Builder(String url, String downloadPath) {
            this.url = url;
            this.downloadPath = downloadPath;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder fileKey(String fileKey) {
            this.fileKey = fileKey;
            return this;
        }

        public Builder fileSize(Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public Builder filePass(String filePass) {
            this.filePass = filePass;
            return this;
        }

        public Builder fileNoExpire(String fileNoExpire) {
            this.fileNoExpire = fileNoExpire;
            return this;
        }

        public Builder accountEmail(String accountEmail) {
            this.accountEmail = accountEmail;
            return this;
        }

        public Builder useSlots(boolean useSlots) {
            this.useSlots = useSlots;
            return this;
        }

        public Builder restart(boolean restart) {
            this.restart = restart;
            return this;
        }

        public Builder customChunksDir(String customChunksDir) {
            this.customChunksDir = customChunksDir;
            return this;
        }

        public Builder priority(boolean priority) {
            this.priority = priority;
            return this;
        }

        public DownloadRequest build() {
            return new DownloadRequest(this);
        }
    }
}
