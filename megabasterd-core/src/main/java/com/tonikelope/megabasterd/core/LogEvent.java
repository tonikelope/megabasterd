package com.tonikelope.megabasterd.core;

public final class LogEvent implements CoreEvent {

    public enum Level {
        DEBUG,
        INFO,
        WARNING,
        ERROR
    }

    private final long timestampMillis;
    private final Level level;
    private final String source;
    private final String message;

    public LogEvent(Level level, String source, String message) {
        this(System.currentTimeMillis(), level, source, message);
    }

    public LogEvent(long timestampMillis, Level level, String source, String message) {
        this.timestampMillis = timestampMillis;
        this.level = level != null ? level : Level.INFO;
        this.source = source != null ? source : "";
        this.message = message != null ? message : "";
    }

    public static LogEvent info(String source, String message) {
        return new LogEvent(Level.INFO, source, message);
    }

    @Override
    public String type() {
        return "log";
    }

    @Override
    public long timestampMillis() {
        return timestampMillis;
    }

    public Level level() {
        return level;
    }

    public String source() {
        return source;
    }

    public String message() {
        return message;
    }
}
