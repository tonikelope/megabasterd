package com.tonikelope.megabasterd.core;

public final class StreamingProxyEvent implements CoreEvent {

    private final long timestampMillis;
    private final StreamingProxyStatus status;

    public StreamingProxyEvent(StreamingProxyStatus status) {
        this(System.currentTimeMillis(), status);
    }

    public StreamingProxyEvent(long timestampMillis, StreamingProxyStatus status) {
        this.timestampMillis = timestampMillis;
        this.status = status != null ? status
                : StreamingProxyStatus.off(StreamingProxyComponent.STREAMING_SERVER, "");
    }

    @Override
    public String type() {
        return "streaming-proxy";
    }

    @Override
    public long timestampMillis() {
        return timestampMillis;
    }

    public StreamingProxyStatus status() {
        return status;
    }
}
