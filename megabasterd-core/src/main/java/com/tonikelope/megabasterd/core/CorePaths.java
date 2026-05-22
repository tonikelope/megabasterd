package com.tonikelope.megabasterd.core;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class CorePaths {

    private final Path homeDirectory;

    private CorePaths(Path homeDirectory) {
        this.homeDirectory = homeDirectory;
    }

    public static CorePaths defaults() {
        return of(Paths.get(System.getProperty("user.home")));
    }

    public static CorePaths of(Path homeDirectory) {
        if (homeDirectory == null) {
            return defaults();
        }

        return new CorePaths(homeDirectory);
    }

    public Path homeDirectory() {
        return homeDirectory;
    }
}
