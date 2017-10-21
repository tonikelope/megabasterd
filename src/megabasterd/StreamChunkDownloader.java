/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package megabasterd;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import static megabasterd.MainPanel.THROTTLE_SLICE_SIZE;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 *
 * @author tonikelope
 */
public class StreamChunkDownloader implements Runnable {

    private final int _id;
    private final StreamChunkWriter _chunkwriter;
    private final String _url;
    private volatile boolean _exit;

    public StreamChunkDownloader(int id, String url, StreamChunkWriter chunkwriter) {
        _id = id;
        _url = url;
        _chunkwriter = chunkwriter;
        _exit = false;
    }

    public void setExit(boolean _exit) {
        this._exit = _exit;
    }

    @Override
    public void run() {

        StreamChunk chunk_stream;
        int reads, http_status;
        byte[] buffer = new byte[THROTTLE_SLICE_SIZE];
        InputStream is;
        boolean error;

        System.out.println(Thread.currentThread().getName() + " Worker [" + _id + "]: let's do some work!");

        try (CloseableHttpClient httpclient = MiscTools.getApacheKissHttpClient()) {

            error = false;

            long offset = -1;

            while (!_exit && !_chunkwriter.isExit()) {

                while (!_exit && !_chunkwriter.isExit() && _chunkwriter.getChunk_queue().size() >= StreamChunkWriter.BUFFER_CHUNKS_SIZE) {

                    System.out.println(Thread.currentThread().getName() + " Worker [" + _id + "]: El búffer de chunks está lleno. Me duermo.");

                    _chunkwriter.secureWait();
                }

                if (!error) {

                    offset = _chunkwriter.nextOffset();
                }

                if (offset >= 0) {

                    chunk_stream = new StreamChunk(offset, _chunkwriter.calculateChunkSize(offset), _url);

                    System.out.println(Thread.currentThread().getName() + " Worker [" + _id + "]: offset: " + offset + " size: " + chunk_stream.getSize());

                    HttpGet httpget = new HttpGet(new URI(chunk_stream.getUrl()));

                    error = false;

                    try (CloseableHttpResponse httpresponse = httpclient.execute(httpget)) {

                        if (!_exit) {

                            is = httpresponse.getEntity().getContent();

                            http_status = httpresponse.getStatusLine().getStatusCode();

                            if (http_status != HttpStatus.SC_OK) {

                                System.out.println("Failed : HTTP error code : " + http_status);

                                error = true;

                                if (http_status == 509) {
                                    _exit = true;
                                    _chunkwriter.setExit(true);
                                }

                            } else {

                                while (!_exit && !_chunkwriter.isExit() && (reads = is.read(buffer)) != -1) {

                                    chunk_stream.getOutputStream().write(buffer, 0, reads);
                                }

                                is.close();

                                if (chunk_stream.getSize() != chunk_stream.getOutputStream().size()) {
                                    error = true;
                                }
                            }

                            if (!error) {

                                System.out.println(Thread.currentThread().getName() + " Worker [" + _id + "] has downloaded chunk [" + chunk_stream.getOffset() + "]!");

                                _chunkwriter.getChunk_queue().put(chunk_stream.getOffset(), chunk_stream);

                                _chunkwriter.secureNotifyAll();
                            }

                        }

                    } catch (IOException ex) {

                        error = true;

                        getLogger(ChunkDownloader.class.getName()).log(Level.SEVERE, null, ex);
                    }

                } else {

                    _exit = true;
                }
            }

        } catch (IOException | URISyntaxException ex) {
            getLogger(ChunkDownloader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ChunkInvalidException ex) {
            Logger.getLogger(StreamChunkDownloader.class.getName()).log(Level.SEVERE, null, ex);
        }

        _chunkwriter.secureNotifyAll();

        System.out.println("Worker [" + _id + "]: bye bye");
    }

}
