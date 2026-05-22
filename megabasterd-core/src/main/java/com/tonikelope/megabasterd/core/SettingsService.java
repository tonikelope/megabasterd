package com.tonikelope.megabasterd.core;

import java.util.Map;

public interface SettingsService {

    String get(String key);

    SettingsSnapshot snapshot();

    void put(String key, String value);

    void putAll(Map<String, ?> values);
}
