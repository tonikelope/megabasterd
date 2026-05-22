package com.tonikelope.megabasterd.core;

import java.util.LinkedHashMap;
import java.util.Map;

final class InMemorySettingsStorage implements SettingsStorage {

    private final Map<String, String> values = new LinkedHashMap<>();

    @Override
    public synchronized String get(String key) {
        return values.get(key);
    }

    @Override
    public synchronized Map<String, String> loadAll() {
        return new LinkedHashMap<>(values);
    }

    @Override
    public synchronized void put(String key, String value) {
        values.put(key, value);
    }

    @Override
    public synchronized void putAll(Map<String, String> values) {
        if (values != null) {
            this.values.putAll(values);
        }
    }
}
