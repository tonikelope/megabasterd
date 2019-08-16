package com.tonikelope.megabasterd;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public final class ThrottledInputStream extends InputStream {

    private final InputStream _rawStream;

    private final StreamThrottlerSupervisor _stream_supervisor;

    private Integer _slice_size;

    private boolean _stream_finish;

    public ThrottledInputStream(InputStream rawStream, StreamThrottlerSupervisor stream_supervisor) {

        _rawStream = rawStream;

        _stream_supervisor = stream_supervisor;

        _stream_finish = false;

        _slice_size = null;
    }

    @Override
    public int read() throws IOException {

        int r;

        if (_stream_supervisor.getMaxBytesPerSecInput() > 0) {

            if (!_stream_finish) {

                throttle(1);

                r = _rawStream.read();

                if (r == -1) {

                    _stream_finish = true;
                }

                return r;

            } else {

                return -1;
            }

        } else {

            r = _rawStream.read();

            if (r == -1) {

                _stream_finish = true;
            }

            return r;
        }

    }

    @Override
    public int read(byte[] b) throws IOException {

        int readLen, len = b.length;

        if (_stream_supervisor.getMaxBytesPerSecInput() > 0) {

            if (!_stream_finish) {

                throttle(len);

                readLen = _rawStream.read(b, 0, _slice_size != null ? _slice_size : len);

                if (readLen == -1) {

                    _stream_finish = true;

                } else if (_slice_size != null && readLen < _slice_size && !_stream_supervisor.isQueue_swapping()) {

                    _stream_supervisor.getInput_slice_queue().add(_slice_size - readLen);

                    _stream_supervisor.secureNotifyAll();
                }

                return readLen;

            } else {

                return -1;
            }

        } else {

            return _rawStream.read(b);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {

        int readLen;

        if (_stream_supervisor.getMaxBytesPerSecInput() > 0) {

            if (!_stream_finish) {

                throttle(len);

                readLen = _rawStream.read(b, off, _slice_size != null ? _slice_size : len);

                if (readLen == -1) {

                    _stream_finish = true;

                } else if (_slice_size != null && readLen < _slice_size && !_stream_supervisor.isQueue_swapping()) {

                    _stream_supervisor.getInput_slice_queue().add(_slice_size - readLen);

                    _stream_supervisor.secureNotifyAll();
                }

                return readLen;

            } else {

                return -1;
            }

        } else {

            return _rawStream.read(b, off, len);
        }
    }

    @Override
    public void reset() throws IOException {

        _stream_finish = false;

        _rawStream.reset();
    }

    private void throttle(int req_slice_size) throws IOException {

        _slice_size = null;

        while (_stream_supervisor.getMaxBytesPerSecInput() > 0 && (_stream_supervisor.isQueue_swapping() || (_slice_size = _stream_supervisor.getInput_slice_queue().poll()) == null)) {

            _stream_supervisor.secureWait();
        }

        if (_slice_size != null && req_slice_size < _slice_size) {

            if (!_stream_supervisor.isQueue_swapping()) {

                _stream_supervisor.getInput_slice_queue().add(_slice_size - req_slice_size);

                _stream_supervisor.secureNotifyAll();
            }

            _slice_size = req_slice_size;
        }
    }
    private static final Logger LOG = Logger.getLogger(ThrottledInputStream.class.getName());

}
