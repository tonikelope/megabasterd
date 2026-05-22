package com.tonikelope.megabasterd.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

final class DefaultSettingsService implements SettingsService {

    private static final Logger LOG = Logger.getLogger(DefaultSettingsService.class.getName());

    private final SettingsStorage storage;
    private final CoreEventPublisher events;

    DefaultSettingsService(SettingsStorage storage, CoreEventPublisher events) {
        this.storage = storage != null ? storage : new InMemorySettingsStorage();
        this.events = events;
    }

    @Override
    public String get(String key) {
        try {
            String value = storage.get(key);
            publish(SettingsEvent.Action.LOADED, key);
            return value;
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public SettingsSnapshot snapshot() {
        try {
            SettingsSnapshot snapshot = new SettingsSnapshot(storage.loadAll());
            publish(SettingsEvent.Action.LOADED, "");
            return snapshot;
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            return new SettingsSnapshot(null);
        }
    }

    @Override
    public void put(String key, String value) {
        try {
            storage.put(key, value);
            publish(SettingsEvent.Action.SAVED, key);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void putAll(Map<String, ?> values) {
        Map<String, String> normalized = new LinkedHashMap<>();

        if (values != null) {
            for (Map.Entry<String, ?> entry : values.entrySet()) {
                normalized.put(entry.getKey(), entry.getValue() != null ? (String) entry.getValue() : null);
            }
        }

        try {
            storage.putAll(normalized);
            publish(SettingsEvent.Action.SAVED, "");
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new IllegalStateException(ex);
        }
    }

    private void publish(SettingsEvent.Action action, String key) {
        if (events != null) {
            events.publish(new SettingsEvent(action, key));
        }
    }
}
