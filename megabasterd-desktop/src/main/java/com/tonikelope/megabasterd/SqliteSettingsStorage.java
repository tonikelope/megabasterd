package com.tonikelope.megabasterd;

import com.tonikelope.megabasterd.core.SettingsStorage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

final class SqliteSettingsStorage implements SettingsStorage {

    @Override
    public synchronized String get(String key) throws Exception {
        String value = null;

        try (Connection conn = SqliteSingleton.getInstance().getConn();
                PreparedStatement ps = conn.prepareStatement("SELECT value from settings WHERE key=?")) {

            ps.setString(1, key);

            try (ResultSet res = ps.executeQuery()) {
                if (res.next()) {
                    value = res.getString(1);
                }
            }
        }

        return value;
    }

    @Override
    public synchronized Map<String, String> loadAll() throws Exception {
        Map<String, String> settings = new LinkedHashMap<>();

        try (Connection conn = SqliteSingleton.getInstance().getConn();
                Statement stat = conn.createStatement();
                ResultSet res = stat.executeQuery("SELECT * FROM settings")) {

            while (res.next()) {
                settings.put(res.getString("key"), res.getString("value"));
            }
        }

        return settings;
    }

    @Override
    public synchronized void put(String key, String value) throws Exception {
        try (Connection conn = SqliteSingleton.getInstance().getConn();
                PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO settings (key,value) VALUES (?, ?)")) {

            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }

    @Override
    public synchronized void putAll(Map<String, String> values) throws Exception {
        try (Connection conn = SqliteSingleton.getInstance().getConn();
                PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO settings (key,value) VALUES (?, ?)")) {

            for (Map.Entry<String, String> entry : values.entrySet()) {
                ps.setString(1, entry.getKey());
                ps.setString(2, entry.getValue());
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }
}
