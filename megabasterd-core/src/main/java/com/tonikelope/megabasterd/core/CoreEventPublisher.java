package com.tonikelope.megabasterd.core;

public interface CoreEventPublisher {

    EventSubscription subscribe(CoreEventListener listener);

    void publish(CoreEvent event);
}
