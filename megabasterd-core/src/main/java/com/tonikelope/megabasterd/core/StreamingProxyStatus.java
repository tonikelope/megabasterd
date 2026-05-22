package com.tonikelope.megabasterd.core;

public final class StreamingProxyStatus {

    private final StreamingProxyComponent component;
    private final StreamingProxyState state;
    private final int port;
    private final int activeConnections;
    private final String statusText;

    public StreamingProxyStatus(StreamingProxyComponent component, StreamingProxyState state,
            int port, int activeConnections, String statusText) {
        this.component = component != null ? component : StreamingProxyComponent.STREAMING_SERVER;
        this.state = state != null ? state : StreamingProxyState.OFF;
        this.port = port;
        this.activeConnections = Math.max(0, activeConnections);
        this.statusText = statusText != null ? statusText : "";
    }

    public static StreamingProxyStatus off(StreamingProxyComponent component, String statusText) {
        return new StreamingProxyStatus(component, StreamingProxyState.OFF, -1, 0, statusText);
    }

    public StreamingProxyComponent component() {
        return component;
    }

    public StreamingProxyState state() {
        return state;
    }

    public int port() {
        return port;
    }

    public int activeConnections() {
        return activeConnections;
    }

    public String statusText() {
        return statusText;
    }
}
