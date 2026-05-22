package com.tonikelope.megabasterd.core;

public interface MegaBasterdCore {

    static MegaBasterdCore start(CoreConfig config) {
        return new DefaultMegaBasterdCore(config);
    }

    String version();

    CoreHealth health();

    CoreConfig config();

    CoreEventPublisher events();

    SettingsService settings();

    void shutdown();
}
