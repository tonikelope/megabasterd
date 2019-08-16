package com.tonikelope.megabasterd;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public final class ThrottledOutputStream extends OutputStream {

    private final OutputStream _rawStream;

    private final StreamThrottlerSupervisor _stream_supervisor;

    private Integer _slice_size;

    public ThrottledOutputStream(OutputStream rawStream, StreamThrottlerSupervisor stream_supervisor) {

        _rawStream = rawStream;

        _stream_supervisor = stream_supervisor;

        _slice_size = null;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {

        if (_stream_supervisor.getMaxBytesPerSecOutput() > 0) {

            int written = 0;

            do {

                throttle(len - written);

                _rawStream.write(b, off + written, _slice_size != null ? _slice_size : len - written);

                written += _slice_size != null ? _slice_size : len - written;

            } while (written < len);

        } else {
            _rawStream.write(b, off, len);
        }
    }

    @Override
    public void write(int i) throws IOException {

        if (_stream_supervisor.getMaxBytesPerSecOutput() > 0) {

            throttle(1);
        }

        _rawStream.write(i);
    }

    private void throttle(int req_slice_size) throws IOException {

        _slice_size = null;

        while (_stream_supervisor.getMaxBytesPerSecOutput() > 0 && (_stream_supervisor.isQueue_swapping() || (_slice_size = _stream_supervisor.getOutput_slice_queue().poll()) == null)) {

            _stream_supervisor.secureWait();
        }

        if (_slice_size != null && req_slice_size < _slice_size) {

            if (!_stream_supervisor.isQueue_swapping()) {

                _stream_supervisor.getOutput_slice_queue().add(_slice_size - req_slice_size);

                _stream_supervisor.secureNotifyAll();
            }

            _slice_size = req_slice_size;
        }
    }
    private static final Logger LOG = Logger.getLogger(ThrottledOutputStream.class.getName());

}
