package com.tonikelope.megabasterd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public final class StreamChunk {

    private static final Logger LOG = Logger.getLogger(StreamChunk.class.getName());

    private final long _offset;
    private final long _size;
    private final String _url;
    private final ByteArrayOutInputStream _data_os;
    private boolean _writable;

    public StreamChunk(long offset, long size, String url) throws ChunkInvalidException, IOException {

        if (offset < 0 || size < 0 || url == null) {
            throw new ChunkInvalidException("Offset: " + offset + " Size: " + size);
        }

        _offset = offset;
        _size = size;
        _url = url + "/" + _offset + "-" + (_offset + _size - 1);
        _data_os = new ByteArrayOutInputStream((int) _size);
        _writable = true;
    }

    public ByteArrayInputStream getInputStream() {
        _writable = false;
        return _data_os.toInputStream();
    }

    public long getOffset() {
        return _offset;
    }

    public long getSize() {
        return _size;
    }

    public String getUrl() {
        return _url;
    }

    public ByteArrayOutputStream getOutputStream() throws IOException {

        if (!_writable) {

            throw new IOException("Chunk outputstream is not available!");
        }
        return _data_os;
    }

    public class ByteArrayOutInputStream extends ByteArrayOutputStream {

        public ByteArrayOutInputStream(int size) {
            super(size);
        }

        /**
         * Get an input stream based on the contents of this output stream. Do
         * not use the output stream after calling this method.
         *
         * @return an {@link InputStream}
         */
        public ByteArrayInputStream toInputStream() {
            return new ByteArrayInputStream(this.buf, 0, this.count);
        }
    }

}
