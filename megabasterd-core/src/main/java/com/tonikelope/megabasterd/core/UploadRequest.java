package com.tonikelope.megabasterd.core;

public final class UploadRequest {

    private final String fileName;
    private final String accountEmail;
    private final String parentNode;
    private final int[] uploadKey;
    private final String uploadUrl;
    private final String rootNode;
    private final byte[] shareKey;
    private final String folderLink;
    private final boolean priority;

    private UploadRequest(Builder builder) {
        if (builder.fileName == null || builder.fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Upload file name cannot be blank");
        }
        this.fileName = builder.fileName;
        this.accountEmail = builder.accountEmail;
        this.parentNode = builder.parentNode;
        this.uploadKey = builder.uploadKey != null ? builder.uploadKey.clone() : null;
        this.uploadUrl = builder.uploadUrl;
        this.rootNode = builder.rootNode;
        this.shareKey = builder.shareKey != null ? builder.shareKey.clone() : null;
        this.folderLink = builder.folderLink;
        this.priority = builder.priority;
    }

    public static Builder builder(String fileName) {
        return new Builder(fileName);
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

    public int[] uploadKey() {
        return uploadKey != null ? uploadKey.clone() : null;
    }

    public String uploadUrl() {
        return uploadUrl;
    }

    public String rootNode() {
        return rootNode;
    }

    public byte[] shareKey() {
        return shareKey != null ? shareKey.clone() : null;
    }

    public String folderLink() {
        return folderLink;
    }

    public boolean priority() {
        return priority;
    }

    public static final class Builder {

        private final String fileName;
        private String accountEmail;
        private String parentNode;
        private int[] uploadKey;
        private String uploadUrl;
        private String rootNode;
        private byte[] shareKey;
        private String folderLink;
        private boolean priority;

        private Builder(String fileName) {
            this.fileName = fileName;
        }

        public Builder accountEmail(String accountEmail) {
            this.accountEmail = accountEmail;
            return this;
        }

        public Builder parentNode(String parentNode) {
            this.parentNode = parentNode;
            return this;
        }

        public Builder uploadKey(int[] uploadKey) {
            this.uploadKey = uploadKey != null ? uploadKey.clone() : null;
            return this;
        }

        public Builder uploadUrl(String uploadUrl) {
            this.uploadUrl = uploadUrl;
            return this;
        }

        public Builder rootNode(String rootNode) {
            this.rootNode = rootNode;
            return this;
        }

        public Builder shareKey(byte[] shareKey) {
            this.shareKey = shareKey != null ? shareKey.clone() : null;
            return this;
        }

        public Builder folderLink(String folderLink) {
            this.folderLink = folderLink;
            return this;
        }

        public Builder priority(boolean priority) {
            this.priority = priority;
            return this;
        }

        public UploadRequest build() {
            return new UploadRequest(this);
        }
    }
}
