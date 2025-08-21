/*
 __  __                  _               _               _ 
|  \/  | ___  __ _  __ _| |__   __ _ ___| |_ ___ _ __ __| |
| |\/| |/ _ \/ _` |/ _` | '_ \ / _` / __| __/ _ \ '__/ _` |
| |  | |  __/ (_| | (_| | |_) | (_| \__ \ ||  __/ | | (_| |
|_|  |_|\___|\__, |\__,_|_.__/ \__,_|___/\__\___|_|  \__,_|
             |___/                                         
© Perpetrated by tonikelope since 2016
 */
package com.tonikelope.megabasterd;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static com.tonikelope.megabasterd.MainPanel.DEFAULT_BYTE_BUFFER_SIZE;

/**
 *
 * @author tonikelope
 */
public class StreamChunkDownloader implements Runnable {

    private static final Logger LOG = LogManager.getLogger(StreamChunkDownloader.class);

    private final int _id;
    private final StreamChunkManager _chunkManager;
    private volatile boolean _exit;

    public StreamChunkDownloader(int id, StreamChunkManager chunkManager) {
        _id = id;
        _chunkManager = chunkManager;
        _exit = false;
    }

    public void setExit(boolean exit) {
        _exit = exit;
    }

    @Override
    public void run() {

        LOG.info("Worker [{}]: let''s do some work!", _id);

        HttpURLConnection con;

        try {

            String url = _chunkManager.getUrl();

            int http_error = 0;

            String current_smart_proxy = null;

            boolean smart_proxy_socks = false;

            long offset = -1;

            SmartMegaProxyManager proxy_manager = MainPanel.getProxy_manager();

            ArrayList<String> excluded_proxy_list = new ArrayList<>();

            if (MainPanel.isUse_smart_proxy() && proxy_manager != null && proxy_manager.isForce_smart_proxy()) {

                String[] smart_proxy = proxy_manager.getProxy(excluded_proxy_list);

                current_smart_proxy = smart_proxy[0];

                smart_proxy_socks = smart_proxy[1].equals("socks");
            }

            while (!_exit && !_chunkManager.isExit()) {

                while (!_exit && !_chunkManager.isExit() && _chunkManager.getChunk_queue().size() >= StreamChunkManager.BUFFER_CHUNKS_SIZE) {

                    LOG.info("Worker [{}]: Chunk buffer is full. I pause myself.", _id);

                    _chunkManager.secureWait();
                }

                if (http_error == 0) {

                    offset = _chunkManager.nextOffset();

                } else if (http_error == 403) {

                    url = _chunkManager.getUrl();
                }

                if (offset >= 0) {

                    StreamChunk chunk_stream = new StreamChunk(offset, _chunkManager.calculateChunkSize(offset), url);

                    URL chunk_url = new URL(chunk_stream.getUrl());

                    if ((current_smart_proxy != null || http_error == 509) && MainPanel.isUse_smart_proxy() && !MainPanel.isUse_proxy()) {

                        if (current_smart_proxy != null && http_error != 0) {

                            proxy_manager.blockProxy(current_smart_proxy, "HTTP " + http_error);

                            String[] smart_proxy = proxy_manager.getProxy(excluded_proxy_list);

                            current_smart_proxy = smart_proxy[0];

                            smart_proxy_socks = smart_proxy[1].equals("socks");

                        } else if (current_smart_proxy == null) {

                            String[] smart_proxy = proxy_manager.getProxy(excluded_proxy_list);

                            current_smart_proxy = smart_proxy[0];

                            smart_proxy_socks = smart_proxy[1].equals("socks");

                        }

                        if (current_smart_proxy != null) {

                            String[] proxy_info = current_smart_proxy.split(":");

                            Proxy proxy = new Proxy(smart_proxy_socks ? Proxy.Type.SOCKS : Proxy.Type.HTTP, new InetSocketAddress(proxy_info[0], Integer.parseInt(proxy_info[1])));

                            con = (HttpURLConnection) chunk_url.openConnection(proxy);

                        } else {


                            con = (HttpURLConnection) chunk_url.openConnection();
                        }

                    } else {



                        if (MainPanel.isUse_proxy()) {

                            con = (HttpURLConnection) chunk_url.openConnection(new Proxy(smart_proxy_socks ? Proxy.Type.SOCKS : Proxy.Type.HTTP, new InetSocketAddress(MainPanel.getProxy_host(), MainPanel.getProxy_port())));

                            if (MainPanel.getProxy_user() != null && !MainPanel.getProxy_user().isEmpty()) {
                                con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((MainPanel.getProxy_user() + ":" + MainPanel.getProxy_pass()).getBytes(StandardCharsets.UTF_8)));
                            }
                        } else if (current_smart_proxy != null) {

                            String[] proxy_info = current_smart_proxy.split(":");

                            Proxy proxy = new Proxy(smart_proxy_socks ? Proxy.Type.SOCKS : Proxy.Type.HTTP, new InetSocketAddress(proxy_info[0], Integer.parseInt(proxy_info[1])));

                            con = (HttpURLConnection) chunk_url.openConnection(proxy);

                        } else {
                            con = (HttpURLConnection) chunk_url.openConnection();
                        }
                    }

                    if (current_smart_proxy != null && proxy_manager != null) {
                        con.setConnectTimeout(proxy_manager.getProxy_timeout());
                        con.setReadTimeout(proxy_manager.getProxy_timeout() * 2);
                    }

                    con.setUseCaches(false);

                    con.setRequestProperty("User-Agent", MainPanel.DEFAULT_USER_AGENT);

                    int reads, http_status;

                    byte[] buffer = new byte[DEFAULT_BYTE_BUFFER_SIZE];

                    LOG.info("Worker [{}]: offset: {} size: {}", _id, offset, chunk_stream.getSize());

                    http_error = 0;

                    try {

                        if (!_exit) {

                            http_status = con.getResponseCode();

                            if (http_status != 200) {

                                LOG.info("Failed : HTTP error code : {}", http_status);

                                http_error = http_status;

                            } else {

                                try (InputStream is = con.getInputStream()) {

                                    int chunk_writes = 0;

                                    while (!_exit && !_chunkManager.isExit() && chunk_writes < chunk_stream.getSize() && (reads = is.read(buffer, 0, Math.min((int) (chunk_stream.getSize() - chunk_writes), buffer.length))) != -1) {

                                        chunk_stream.getOutputStream().write(buffer, 0, reads);

                                        chunk_writes += reads;
                                    }

                                    if (chunk_stream.getSize() == chunk_writes) {

                                        LOG.info("Worker [{}] has downloaded chunk [{}]!", _id, chunk_stream.getOffset());

                                        _chunkManager.getChunk_queue().put(chunk_stream.getOffset(), chunk_stream);

                                        _chunkManager.secureNotifyAll();

                                        current_smart_proxy = null;

                                        excluded_proxy_list.clear();
                                    }
                                }
                            }
                        }

                    } catch (IOException ex) {
                        LOG.fatal("IO Exception in run! {}", ex.getMessage());
                    } finally {
                        con.disconnect();
                    }
                } else {
                    _exit = true;
                }
            }

        } catch (OutOfMemoryError | Exception ex) {
            LOG.fatal("Generic exception caught in run! {}", ex.getMessage());
        }

        _chunkManager.secureNotifyAll();

        LOG.info("Worker [{}]: bye bye", _id);
    }

}
