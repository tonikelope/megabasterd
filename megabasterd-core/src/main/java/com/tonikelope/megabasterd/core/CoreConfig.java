package com.tonikelope.megabasterd.core;

import java.nio.file.Path;

public final class CoreConfig {

    private final CorePaths paths;
    private final SettingsStorage settingsStorage;
    private final DownloadService downloadService;
    private final UploadService uploadService;

    private CoreConfig(CorePaths paths, SettingsStorage settingsStorage,
            DownloadService downloadService, UploadService uploadService) {
        this.paths = paths;
        this.settingsStorage = settingsStorage;
        this.downloadService = downloadService;
        this.uploadService = uploadService;
    }

    public static CoreConfig defaults() {
        return new CoreConfig(CorePaths.defaults(), null, null, null);
    }

    public static CoreConfig of(CorePaths paths) {
        return new CoreConfig(paths != null ? paths : CorePaths.defaults(), null, null, null);
    }

    public static CoreConfig forHomeDirectory(Path homeDirectory) {
        return of(CorePaths.of(homeDirectory));
    }

    public CorePaths paths() {
        return paths;
    }

    public SettingsStorage settingsStorage() {
        return settingsStorage;
    }

    public DownloadService downloadService() {
        return downloadService;
    }

    public UploadService uploadService() {
        return uploadService;
    }

    public CoreConfig withSettingsStorage(SettingsStorage settingsStorage) {
        return new CoreConfig(paths, settingsStorage, downloadService, uploadService);
    }

    public CoreConfig withDownloadService(DownloadService downloadService) {
        return new CoreConfig(paths, settingsStorage, downloadService, uploadService);
    }

    public CoreConfig withUploadService(UploadService uploadService) {
        return new CoreConfig(paths, settingsStorage, downloadService, uploadService);
    }
}
