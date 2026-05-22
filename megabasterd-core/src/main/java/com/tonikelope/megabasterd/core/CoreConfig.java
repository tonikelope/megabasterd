package com.tonikelope.megabasterd.core;

import java.nio.file.Path;

public final class CoreConfig {

    private final CorePaths paths;

    private CoreConfig(CorePaths paths) {
        this.paths = paths;
    }

    public static CoreConfig defaults() {
        return new CoreConfig(CorePaths.defaults());
    }

    public static CoreConfig of(CorePaths paths) {
        return new CoreConfig(paths != null ? paths : CorePaths.defaults());
    }

    public static CoreConfig forHomeDirectory(Path homeDirectory) {
        return of(CorePaths.of(homeDirectory));
    }

    public CorePaths paths() {
        return paths;
    }
}
