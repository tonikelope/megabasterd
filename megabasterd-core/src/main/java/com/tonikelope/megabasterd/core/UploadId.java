package com.tonikelope.megabasterd.core;

import java.util.Objects;
import java.util.UUID;

public final class UploadId {

    private final String value;

    private UploadId(String value) {
        this.value = value;
    }

    public static UploadId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Upload id cannot be blank");
        }
        return new UploadId(value);
    }

    public static UploadId random() {
        return new UploadId(UUID.randomUUID().toString());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UploadId)) {
            return false;
        }
        UploadId uploadId = (UploadId) o;
        return Objects.equals(value, uploadId.value);
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
