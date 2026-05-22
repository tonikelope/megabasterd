package com.tonikelope.megabasterd.core;

import java.util.EnumMap;
import java.util.Map;

final class DefaultStreamingProxyService implements StreamingProxyService {

    private final Map<StreamingProxyComponent, StreamingProxyStatus> statuses;
    private final CoreEventPublisher events;

    DefaultStreamingProxyService(CoreEventPublisher events) {
        this.events = events;
        this.statuses = new EnumMap<>(StreamingProxyComponent.class);
        for (StreamingProxyComponent component : StreamingProxyComponent.values()) {
            statuses.put(component, StreamingProxyStatus.off(component, ""));
        }
    }

    @Override
    public synchronized StreamingProxyStatus status(StreamingProxyComponent component) {
        StreamingProxyComponent safeComponent = component != null ? component : StreamingProxyComponent.STREAMING_SERVER;
        return statuses.get(safeComponent);
    }

    @Override
    public synchronized void updateStatus(StreamingProxyStatus status) {
        if (status == null) {
            return;
        }
        statuses.put(status.component(), status);
        if (events != null) {
            events.publish(new StreamingProxyEvent(status));
        }
    }
}
