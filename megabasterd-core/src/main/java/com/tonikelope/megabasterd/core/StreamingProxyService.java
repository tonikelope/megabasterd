package com.tonikelope.megabasterd.core;

public interface StreamingProxyService {

    StreamingProxyStatus status(StreamingProxyComponent component);

    void updateStatus(StreamingProxyStatus status);
}
