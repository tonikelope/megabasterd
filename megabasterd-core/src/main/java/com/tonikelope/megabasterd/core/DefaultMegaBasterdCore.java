package com.tonikelope.megabasterd.core;

final class DefaultMegaBasterdCore implements MegaBasterdCore {

    private final CoreConfig config;
    private final CoreEventPublisher events;
    private final SettingsService settings;
    private final AccountService accounts;
    private final DownloadService downloads;
    private final UploadService uploads;
    private final long startedAtMillis;
    private volatile boolean running;

    DefaultMegaBasterdCore(CoreConfig config) {
        this.config = config != null ? config : CoreConfig.defaults();
        this.events = new DefaultCoreEventPublisher();
        this.settings = new DefaultSettingsService(this.config.settingsStorage(), events);
        this.accounts = new DefaultAccountService(this.config.accountStorage(), events);
        this.downloads = this.config.downloadService() != null ? this.config.downloadService() : new InMemoryDownloadService();
        this.uploads = this.config.uploadService() != null ? this.config.uploadService() : new InMemoryUploadService();
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
    public CoreEventPublisher events() {
        return events;
    }

    @Override
    public SettingsService settings() {
        return settings;
    }

    @Override
    public AccountService accounts() {
        return accounts;
    }

    @Override
    public DownloadService downloads() {
        return downloads;
    }

    @Override
    public UploadService uploads() {
        return uploads;
    }

    @Override
    public void shutdown() {
        running = false;
        events.publish(LogEvent.info("core", "MegaBasterd core stopped"));
    }
}
