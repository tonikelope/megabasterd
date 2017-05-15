package megabasterd;

import java.io.IOException;
import java.io.InputStream;

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

        if (_stream_supervisor.getMaxBytesPerSecInput() > 0) {

            if (!_stream_finish) {

                int r;

                throttle(1);

                if (_slice_size != null) {

                    r = _rawStream.read();

                    if (r == -1) {

                        _stream_finish = true;
                    }

                    return r;

                } else {

                    return _rawStream.read();
                }

            } else {

                return -1;
            }

        } else {

            return _rawStream.read();
        }

    }

    @Override
    public int read(byte[] b) throws IOException {

        if (_stream_supervisor.getMaxBytesPerSecInput() > 0) {

            if (!_stream_finish) {

                int readLen = 0, readSlice, len = b.length, r = 0;

                do {

                    throttle(len - readLen);

                    if (_slice_size != null) {

                        readSlice = 0;

                        do {
                            r = _rawStream.read(b, readLen + readSlice, _slice_size - readSlice);

                            if (r != -1) {

                                readSlice += r;

                            } else {

                                _stream_finish = true;
                            }

                        } while (r != -1 && readSlice < _slice_size);

                        readLen += readSlice;

                    } else {

                        return _rawStream.read(b);
                    }

                } while (r != -1 && readLen < len);

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

        if (_stream_supervisor.getMaxBytesPerSecInput() > 0) {

            if (!_stream_finish) {

                int readLen = 0, r = 0;

                do {

                    throttle(len - readLen);

                    if (_slice_size != null) {

                        int readSlice = 0;

                        do {
                            r = _rawStream.read(b, off + readSlice + readLen, _slice_size - readSlice);

                            if (r != -1) {

                                readSlice += r;

                            } else {

                                _stream_finish = true;
                            }

                        } while (r != -1 && readSlice < _slice_size);

                        readLen += readSlice;

                    } else {

                        r = _rawStream.read(b, off + readLen, len - readLen);

                        if (r != -1) {

                            readLen += r;

                        } else {

                            _stream_finish = true;
                        }
                    }

                } while (r != -1 && readLen < len);

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
    
    @Override
    public void close() throws IOException {

        _rawStream.close();

    }

    private void throttle(int size) throws IOException {

        _slice_size = null;

        while (_stream_supervisor.getMaxBytesPerSecInput() > 0 && (_stream_supervisor.isQueue_swapping() || (_slice_size = _stream_supervisor.getInput_slice_queue().poll()) == null)) {

            _stream_supervisor.secureMultiWait();
        }

        if (_slice_size != null && size < _slice_size) {

            if (!_stream_supervisor.isQueue_swapping()) {

                _stream_supervisor.getInput_slice_queue().add(_slice_size - size);

                _stream_supervisor.secureMultiNotifyAll();
            }

            _slice_size = size;
        }
    }

}
