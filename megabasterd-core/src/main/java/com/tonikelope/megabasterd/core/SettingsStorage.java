package com.tonikelope.megabasterd.core;

import java.util.Map;

public interface SettingsStorage {

    String get(String key) throws Exception;

    Map<String, String> loadAll() throws Exception;

    void put(String key, String value) throws Exception;

    void putAll(Map<String, String> values) throws Exception;
}
