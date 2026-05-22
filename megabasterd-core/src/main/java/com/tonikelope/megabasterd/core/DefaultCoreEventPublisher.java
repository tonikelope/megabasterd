package com.tonikelope.megabasterd.core;

import java.util.concurrent.CopyOnWriteArrayList;

final class DefaultCoreEventPublisher implements CoreEventPublisher {

    private final CopyOnWriteArrayList<CoreEventListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public EventSubscription subscribe(CoreEventListener listener) {
        if (listener == null) {
            return new EventSubscription() {
                @Override
                public void close() {
                }
            };
        }
        listeners.addIfAbsent(listener);
        return new ListenerSubscription(listener);
    }

    @Override
    public void publish(CoreEvent event) {
        if (event == null) {
            return;
        }
        for (CoreEventListener listener : listeners) {
            try {
                listener.onCoreEvent(event);
            } catch (Throwable ignore) {
                // Event listeners must not break core or legacy desktop flows.
            }
        }
    }

    private final class ListenerSubscription implements EventSubscription {

        private final CoreEventListener listener;
        private volatile boolean closed;

        private ListenerSubscription(CoreEventListener listener) {
            this.listener = listener;
        }

        @Override
        public void close() {
            if (!closed) {
                listeners.remove(listener);
                closed = true;
            }
        }
    }
}
