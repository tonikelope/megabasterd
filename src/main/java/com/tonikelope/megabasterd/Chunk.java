package com.tonikelope.megabasterd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import static java.lang.String.valueOf;

/**
 *
 * @author tonikelope
 */
public final class Chunk {

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

    private final long _id;
    private final long _offset;
    private final long _size;
    private final ByteArrayOutInputStream _data_os;
    private final String _url;
    private final int _size_multi;
    private boolean _writable;

    public Chunk(long id, long file_size, String file_url) throws ChunkInvalidException, IOException, OutOfMemoryError {

        _writable = true;

        _size_multi = 1;

        _id = id;

        _offset = calculateOffset(_id, _size_multi);

        Chunk.checkChunkID(_id, file_size, _offset);

        _size = calculateSize(_id, file_size, _offset, _size_multi);

        _url = Chunk.genUrl(file_url, file_size, _offset, _size);

        _data_os = new ByteArrayOutInputStream((int) _size);
    }

    public Chunk(long id, long file_size, String file_url, int size_multi) throws ChunkInvalidException, OutOfMemoryError {

        _writable = true;

        _size_multi = size_multi;

        _id = id;

        _offset = calculateOffset(_id, _size_multi);

        Chunk.checkChunkID(_id, file_size, _offset);

        _size = Chunk.calculateSize(_id, file_size, _offset, _size_multi);

        _url = Chunk.genUrl(file_url, file_size, _offset, _size);

        _data_os = new ByteArrayOutInputStream((int) _size);
    }

    public int getSize_multi() {
        return _size_multi;
    }

    public long getOffset() {
        return _offset;
    }

    public ByteArrayOutputStream getOutputStream() throws IOException {

        if (!_writable) {

            throw new IOException("Chunk outputstream is not available!");
        }

        return _data_os;
    }

    public long getId() {
        return _id;
    }

    public long getSize() {
        return _size;
    }

    public String getUrl() {
        return _url;
    }

    public ByteArrayInputStream getInputStream() {

        _writable = false;

        return _data_os.toInputStream();
    }

    public static long calculateSize(long chunk_id, long file_size, long offset, int size_multi) {
        long chunk_size = (chunk_id >= 1 && chunk_id <= 7) ? chunk_id * 128 * 1024 : 1024 * 1024 * size_multi;

        if (offset + chunk_size > file_size) {
            chunk_size = file_size - offset;
        }

        return chunk_size;
    }

    public static long calculateOffset(long chunk_id, int size_multi) {
        long[] offs = {0, 128, 384, 768, 1280, 1920, 2688};

        return (chunk_id <= 7 ? offs[(int) chunk_id - 1] : (3584 + (chunk_id - 8) * 1024 * size_multi)) * 1024;
    }

    public static String genUrl(String file_url, long file_size, long offset, long chunk_size) {
        return file_url != null ? file_url + "/" + offset + (offset + chunk_size == file_size ? "" : "-" + (offset + chunk_size - 1)) : null;
    }

    public static void checkChunkID(long chunk_id, long file_size, long offset) throws ChunkInvalidException {

        if (file_size > 0) {
            if (offset >= file_size) {
                throw new ChunkInvalidException(valueOf(chunk_id));
            }

        } else {

            if (chunk_id > 1) {

                throw new ChunkInvalidException(valueOf(chunk_id));
            }
        }
    }

}
