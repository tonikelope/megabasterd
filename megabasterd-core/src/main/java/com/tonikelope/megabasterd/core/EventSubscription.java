package com.tonikelope.megabasterd.core;

public interface EventSubscription extends AutoCloseable {

    @Override
    void close();
}
