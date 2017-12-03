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
import static megabasterd.MainPanel.*;
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
    private volatile boolean _exit;

    public StreamChunkDownloader(int id, StreamChunkWriter chunkwriter) {
        _id = id;
        _chunkwriter = chunkwriter;
        _exit = false;
    }

    public void setExit(boolean exit) {
        _exit = exit;
    }

    @Override
    public void run() {

        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Worker [{1}]: let''s do some work!", new Object[]{Thread.currentThread().getName(), _id});

        CloseableHttpClient httpclient = null;

        try {

            String url = _chunkwriter.getUrl();

            boolean error = false;
            boolean error509 = false;
            String current_proxy = null;

            long offset = -1;

            while (!_exit && !_chunkwriter.isExit()) {

                while (!_exit && !_chunkwriter.isExit() && _chunkwriter.getChunk_queue().size() >= StreamChunkWriter.BUFFER_CHUNKS_SIZE) {

                    Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Worker [{1}]: Chunk buffer is full. I pause myself.", new Object[]{Thread.currentThread().getName(), _id});

                    _chunkwriter.secureWait();
                }

                if (_chunkwriter.getServer().isUse_smart_proxy() && !_chunkwriter.getServer().getMain_panel().isUse_smart_proxy()) {

                    _chunkwriter.getServer().setUse_smart_proxy(false);
                }

                if (httpclient == null || error || _chunkwriter.getServer().isUse_smart_proxy()) {

                    if (error509 && !_chunkwriter.getServer().isUse_smart_proxy()) {
                        _chunkwriter.getServer().setUse_smart_proxy(true);
                    }

                    if (_chunkwriter.getServer().isUse_smart_proxy() && !MainPanel.isUse_proxy()) {

                        if (error && current_proxy != null) {

                            Logger.getLogger(getClass().getName()).log(Level.WARNING, "{0} Worker [{1}]: excluding proxy -> {2}", new Object[]{Thread.currentThread().getName(), _id, current_proxy});

                            _chunkwriter.getServer().getExcluded_proxies().add(current_proxy);
                        }

                        current_proxy = _chunkwriter.getServer().getMain_panel().getProxy_manager().getRandomProxy(_chunkwriter.getServer().getExcluded_proxies());

                        if (httpclient != null) {
                            try {
                                httpclient.close();
                            } catch (IOException ex) {
                                Logger.getLogger(StreamChunkDownloader.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }

                        httpclient = current_proxy != null ? MiscTools.getApacheKissHttpClientSmartProxy(current_proxy) : MiscTools.getApacheKissHttpClient();

                    } else if (httpclient == null) {

                        httpclient = MiscTools.getApacheKissHttpClient();
                    }
                }

                if (!error) {

                    offset = _chunkwriter.nextOffset();

                } else {

                    url = _chunkwriter.getUrl();
                }

                if (offset >= 0) {

                    int reads, http_status;

                    byte[] buffer = new byte[THROTTLE_SLICE_SIZE];

                    StreamChunk chunk_stream = new StreamChunk(offset, _chunkwriter.calculateChunkSize(offset), url);

                    Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Worker [{1}]: offset: {2} size: {3}", new Object[]{Thread.currentThread().getName(), _id, offset, chunk_stream.getSize()});

                    HttpGet httpget = new HttpGet(new URI(chunk_stream.getUrl()));

                    error = false;
                    
                    error509=false;

                    try (CloseableHttpResponse httpresponse = httpclient.execute(httpget)) {

                        if (!_exit) {

                            InputStream is = httpresponse.getEntity().getContent();

                            http_status = httpresponse.getStatusLine().getStatusCode();

                            if (http_status != HttpStatus.SC_OK) {

                                Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Failed : HTTP error code : {1}", new Object[]{Thread.currentThread().getName(), http_status});

                                error = true;

                                if (http_status == 509) {

                                    error509 = true;
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

                                Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Worker [{1}] has downloaded chunk [{2}]!", new Object[]{Thread.currentThread().getName(), _id, chunk_stream.getOffset()});

                                _chunkwriter.getChunk_queue().put(chunk_stream.getOffset(), chunk_stream);

                                _chunkwriter.secureNotifyAll();
                            }

                        }

                    } catch (IOException ex) {

                        error = true;

                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                    }

                } else {

                    _exit = true;
                }
            }

        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (ChunkInvalidException | InterruptedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(StreamChunkDownloader.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (IOException ex) {
                    Logger.getLogger(StreamChunkDownloader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        _chunkwriter.secureNotifyAll();

        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Worker [{1}]: bye bye", new Object[]{Thread.currentThread().getName(), _id});
    }

}
