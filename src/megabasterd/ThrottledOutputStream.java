package megabasterd;

import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author tonikelope
 */
public final class ThrottledOutputStream extends OutputStream {

    private final OutputStream _rawStream;

    private final StreamThrottlerSupervisor _stream_supervisor;

    private Integer slice_size;

    public ThrottledOutputStream(OutputStream rawStream, StreamThrottlerSupervisor stream_supervisor) {

        _rawStream = rawStream;

        _stream_supervisor = stream_supervisor;

        slice_size = null;

    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {

        if (_stream_supervisor.getMaxBytesPerSecOutput() > 0) {

            int writeLen = 0;

            do {

                throttle(len - writeLen);

                if (slice_size != null) {

                    _rawStream.write(b, off + writeLen, slice_size);

                    writeLen += slice_size;

                } else {

                    _rawStream.write(b, off + writeLen, len - writeLen);

                    writeLen = len;
                }

            } while (writeLen < len);

        } else {

            _rawStream.write(b, off, len);
        }
    }

    @Override
    public void write(int i) throws IOException {

        if (_stream_supervisor.getMaxBytesPerSecOutput() > 0) {

            throttle(1);

            _rawStream.write(i);

        } else {

            _rawStream.write(i);
        }
    }

    private void throttle(int size) throws IOException {

        slice_size = null;

        while (_stream_supervisor.getMaxBytesPerSecOutput() > 0 && (_stream_supervisor.isQueue_swapping() || (slice_size = _stream_supervisor.getOutput_slice_queue().poll()) == null)) {

            _stream_supervisor.secureWait();
        }

        if (slice_size != null && size < slice_size) {

            if (!_stream_supervisor.isQueue_swapping()) {

                _stream_supervisor.getOutput_slice_queue().add(slice_size - size);

                _stream_supervisor.secureNotifyAll();
            }

            slice_size = size;
        }
    }

}
