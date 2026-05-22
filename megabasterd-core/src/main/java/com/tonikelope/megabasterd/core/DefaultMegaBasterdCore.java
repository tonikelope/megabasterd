package com.tonikelope.megabasterd.core;

final class DefaultMegaBasterdCore implements MegaBasterdCore {

    private final CoreConfig config;
    private final long startedAtMillis;
    private volatile boolean running;

    DefaultMegaBasterdCore(CoreConfig config) {
        this.config = config != null ? config : CoreConfig.defaults();
        this.startedAtMillis = System.currentTimeMillis();
        this.running = true;
    }

    @Override
    public String version() {
        return CoreVersion.VERSION;
    }

    @Override
    public CoreHealth health() {
        return new CoreHealth(running, startedAtMillis, System.currentTimeMillis());
    }

    @Override
    public CoreConfig config() {
        return config;
    }

    @Override
    public void shutdown() {
        running = false;
    }
}
