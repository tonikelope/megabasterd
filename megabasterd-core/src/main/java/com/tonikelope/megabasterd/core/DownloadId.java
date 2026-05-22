package com.tonikelope.megabasterd.core;

import java.util.Objects;
import java.util.UUID;

public final class DownloadId {

    private final String value;

    private DownloadId(String value) {
        this.value = value;
    }

    public static DownloadId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Download id cannot be blank");
        }
        return new DownloadId(value);
    }

    public static DownloadId random() {
        return new DownloadId(UUID.randomUUID().toString());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DownloadId)) {
            return false;
        }
        DownloadId that = (DownloadId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
