package com.tonikelope.megabasterd.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SettingsSnapshot {

    private final Map<String, String> values;

    public SettingsSnapshot(Map<String, String> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values != null ? values : Collections.emptyMap()));
    }

    public Map<String, String> values() {
        return values;
    }
}
