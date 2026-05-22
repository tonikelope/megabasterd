package com.tonikelope.megabasterd.core;

import java.nio.file.Path;

public final class CoreConfig {

    private final CorePaths paths;
    private final SettingsStorage settingsStorage;

    private CoreConfig(CorePaths paths, SettingsStorage settingsStorage) {
        this.paths = paths;
        this.settingsStorage = settingsStorage;
    }

    public static CoreConfig defaults() {
        return new CoreConfig(CorePaths.defaults(), null);
    }

    public static CoreConfig of(CorePaths paths) {
        return new CoreConfig(paths != null ? paths : CorePaths.defaults(), null);
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

    public CoreConfig withSettingsStorage(SettingsStorage settingsStorage) {
        return new CoreConfig(paths, settingsStorage);
    }
}
