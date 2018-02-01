package com.tonikelope.megabasterd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 *
 * @author tonikelope
 */
public final class StreamChunk {

    private final long _offset;
    private final long _size;
    private final String _url;
    private final ByteArrayOutputStream _data_os;

    public StreamChunk(long offset, long size, String url) throws ChunkInvalidException {

        if (offset < 0 || size < 0 || url == null) {
            throw new ChunkInvalidException("Offset: " + offset + " Size: " + size);
        }

        _offset = offset;
        _size = size;
        _url = url + "/" + _offset + "-" + (_offset + _size - 1);
        _data_os = new ByteArrayOutputStream((int) _size);
    }

    public ByteArrayInputStream getInputStream() {
        return new ByteArrayInputStream(_data_os.toByteArray());
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

    public ByteArrayOutputStream getOutputStream() {
        return _data_os;
    }

}
