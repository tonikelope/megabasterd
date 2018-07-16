package com.tonikelope.megabasterd;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static com.tonikelope.megabasterd.MainPanel.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

/**
 *
 * @author tonikelope
 */
public class StreamChunkDownloader implements Runnable {

    private final int _id;
    private final StreamChunkWriter _chunkwriter;
    private volatile boolean _exit;
    private SmartMegaProxyManager _proxy_manager;

    public StreamChunkDownloader(int id, StreamChunkWriter chunkwriter) {
        _id = id;
        _chunkwriter = chunkwriter;
        _proxy_manager = null;
        _exit = false;
    }

    public void setExit(boolean exit) {
        _exit = exit;
    }

    @Override
    public void run() {

        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Worker [{1}]: let''s do some work!", new Object[]{Thread.currentThread().getName(), _id});

        if (MainPanel.isUse_smart_proxy()) {

            _proxy_manager = new SmartMegaProxyManager(MainPanel.getUse_smart_proxy_url());
        }

        HttpURLConnection con = null;

        try {

            String url = _chunkwriter.getUrl();

            boolean error = false, error509 = false;

            String current_proxy = null;

            long offset = -1;

            while (!_exit && !_chunkwriter.isExit()) {

                if (MainPanel.isUse_smart_proxy() && _proxy_manager == null) {

                    _proxy_manager = new SmartMegaProxyManager(MainPanel.getUse_smart_proxy_url());

                } else {

                    _proxy_manager = null;

                }

                while (!_exit && !_chunkwriter.isExit() && _chunkwriter.getChunk_queue().size() >= StreamChunkWriter.BUFFER_CHUNKS_SIZE) {

                    Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Worker [{1}]: Chunk buffer is full. I pause myself.", new Object[]{Thread.currentThread().getName(), _id});

                    _chunkwriter.secureWait();
                }

                if (!error) {

                    offset = _chunkwriter.nextOffset();

                } else {

                    url = _chunkwriter.getUrl();
                }

                if (offset >= 0) {

                    StreamChunk chunk_stream = new StreamChunk(offset, _chunkwriter.calculateChunkSize(offset), url);

                    if (con == null || error) {

                        if (error509 && MainPanel.isUse_smart_proxy() && !MainPanel.isUse_proxy()) {

                            if (error && current_proxy != null) {

                                _proxy_manager.blockProxy(current_proxy);
                            }

                            current_proxy = _proxy_manager.getFastestProxy();

                            if (current_proxy != null) {

                                String[] proxy_info = current_proxy.split(":");

                                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy_info[0], Integer.parseInt(proxy_info[1])));

                                URL chunk_url = new URL(chunk_stream.getUrl());

                                con = (HttpURLConnection) chunk_url.openConnection(proxy);

                            } else {

                                URL chunk_url = new URL(chunk_stream.getUrl());

                                con = (HttpURLConnection) chunk_url.openConnection();
                            }

                        } else if (con == null) {

                            URL chunk_url = new URL(chunk_stream.getUrl());

                            if (MainPanel.isUse_proxy()) {

                                con = (HttpURLConnection) chunk_url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(MainPanel.getProxy_host(), MainPanel.getProxy_port())));

                                if (MainPanel.getProxy_user() != null && !"".equals(MainPanel.getProxy_user())) {

                                    con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((MainPanel.getProxy_user() + ":" + MainPanel.getProxy_pass()).getBytes()));
                                }
                            } else {

                                con = (HttpURLConnection) chunk_url.openConnection();
                            }
                        }
                    } else {

                        URL chunk_url = new URL(chunk_stream.getUrl());

                        if (MainPanel.isUse_proxy()) {

                            con = (HttpURLConnection) chunk_url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(MainPanel.getProxy_host(), MainPanel.getProxy_port())));

                            if (MainPanel.getProxy_user() != null && !"".equals(MainPanel.getProxy_user())) {

                                con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((MainPanel.getProxy_user() + ":" + MainPanel.getProxy_pass()).getBytes()));
                            }
                        } else {

                            con = (HttpURLConnection) chunk_url.openConnection();
                        }
                    }

                    con.setConnectTimeout(Transference.HTTP_TIMEOUT);

                    con.setReadTimeout(Transference.HTTP_TIMEOUT);

                    con.setRequestProperty("User-Agent", MainPanel.DEFAULT_USER_AGENT);

                    int reads, http_status;

                    byte[] buffer = new byte[DEFAULT_BYTE_BUFFER_SIZE];

                    Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Worker [{1}]: offset: {2} size: {3}", new Object[]{Thread.currentThread().getName(), _id, offset, chunk_stream.getSize()});

                    error = false;

                    error509 = false;

                    try {

                        if (!_exit) {

                            InputStream is = con.getInputStream();

                            http_status = con.getResponseCode();

                            if (http_status != 200) {

                                Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Failed : HTTP error code : {1}", new Object[]{Thread.currentThread().getName(), http_status});

                                error = true;

                                error509 = (http_status == 509);

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
                    } finally {
                        if (con != null) {
                            con.disconnect();
                        }
                    }

                } else {

                    _exit = true;
                }
            }

        } catch (IOException | URISyntaxException | ChunkInvalidException | InterruptedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }

        _chunkwriter.secureNotifyAll();

        Logger.getLogger(getClass().getName()).log(Level.INFO, "{0} Worker [{1}]: bye bye", new Object[]{Thread.currentThread().getName(), _id});
    }

}
