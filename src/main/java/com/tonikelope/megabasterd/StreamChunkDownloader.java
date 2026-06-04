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

import static com.tonikelope.megabasterd.MainPanel.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public class StreamChunkDownloader implements Runnable {

    private static final Logger LOG = Logger.getLogger(StreamChunkDownloader.class.getName());

    private final int _id;
    private final StreamChunkManager _chunkmanager;
    private volatile boolean _exit;

    public StreamChunkDownloader(int id, StreamChunkManager chunkmanager) {
        _id = id;
        _chunkmanager = chunkmanager;
        _exit = false;
    }

    public void setExit(boolean exit) {
        _exit = exit;
    }

    /**
     * Returns the port number for a "host:port" smart-proxy entry, or -1 if the
     * entry is malformed (no colon, non-numeric port, or out of range).
     * Centralises the defensive parse so both the smart-proxy branch and the
     * fallback path in run() can use the same guard. (#751)
     */
    private static int parseProxyPort(String proxy) {
        String[] parts = proxy.split(":");
        if (parts.length != 2) {
            return -1;
        }
        try {
            int p = Integer.parseInt(parts[1]);
            return (p >= 1 && p <= 65535) ? p : -1;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    @Override
    public void run() {

        LOG.log(Level.INFO, "{0} Worker [{1}]: let''s do some work!", new Object[]{Thread.currentThread().getName(), _id});

        HttpURLConnection con = null;

        try {

            String url = _chunkmanager.getUrl();

            int http_error = 0;

            String current_smart_proxy = null;

            boolean smart_proxy_socks = false;

            long offset = -1;

            SmartMegaProxyManager proxy_manager = MainPanel.getProxy_manager();

            ArrayList<String> excluded_proxy_list = new ArrayList<>();

            if (MainPanel.isUse_smart_proxy() && proxy_manager != null && proxy_manager.isForce_smart_proxy()) {

                // getProxy() returns null when no proxy is usable. Was indexed
                // unconditionally -> NPE at startup if the remote list is empty
                // or every entry is banned. (#751)
                String[] smart_proxy = proxy_manager.getProxy(excluded_proxy_list);

                if (smart_proxy != null) {
                    current_smart_proxy = smart_proxy[0];
                    smart_proxy_socks = smart_proxy[1].equals("socks");
                } else {
                    LOG.log(Level.WARNING, "{0} StreamWorker [{1}] SmartProxy force-mode: no proxies available -- starting direct", new Object[]{Thread.currentThread().getName(), _id});
                }
            }

            while (!_exit && !_chunkmanager.isExit()) {

                // Re-read the manager every iteration so enabling SmartProxy at
                // runtime is observed by an already-running stream worker
                // (mirrors ChunkDownloader.run / ChunkDownloaderMono.run).
                // Capturing it once before the loop (the line ~84 read) left
                // proxy_manager null for the worker's whole life when streaming
                // started with SmartProxy OFF -- and the routing branch below
                // did not null-guard it, so the first 509 after a runtime enable
                // NPE'd at proxy_manager.getProxy()/blockProxy(). (#778)
                proxy_manager = MainPanel.getProxy_manager();

                while (!_exit && !_chunkmanager.isExit() && _chunkmanager.getChunk_queue().size() >= StreamChunkManager.BUFFER_CHUNKS_SIZE) {

                    LOG.log(Level.INFO, "{0} Worker [{1}]: Chunk buffer is full. I pause myself.", new Object[]{Thread.currentThread().getName(), _id});

                    _chunkmanager.secureWait();
                }

                if (http_error == 0) {

                    offset = _chunkmanager.nextOffset();

                } else if (http_error == 403) {

                    url = _chunkmanager.getUrl();
                }

                if (offset >= 0) {

                    StreamChunk chunk_stream = new StreamChunk(offset, _chunkmanager.calculateChunkSize(offset), url);

                    if ((current_smart_proxy != null || http_error == 509) && MainPanel.isUse_smart_proxy() && proxy_manager != null && !MainPanel.isUse_proxy()) {

                        if (current_smart_proxy != null && http_error != 0) {

                            proxy_manager.blockProxy(current_smart_proxy, "HTTP " + String.valueOf(http_error));

                            String[] smart_proxy = proxy_manager.getProxy(excluded_proxy_list);

                            if (smart_proxy != null) {
                                current_smart_proxy = smart_proxy[0];
                                smart_proxy_socks = smart_proxy[1].equals("socks");
                            } else {
                                LOG.log(Level.WARNING, "{0} StreamWorker [{1}] SmartProxy exhausted -- falling back to direct", new Object[]{Thread.currentThread().getName(), _id});
                                current_smart_proxy = null;
                                // Reset so the next iteration re-evaluates the
                                // full pool instead of locking onto direct.
                                // Mirrors ChunkDownloader. (#778)
                                excluded_proxy_list.clear();
                            }

                        } else if (current_smart_proxy == null) {

                            String[] smart_proxy = proxy_manager.getProxy(excluded_proxy_list);

                            if (smart_proxy != null) {
                                current_smart_proxy = smart_proxy[0];
                                smart_proxy_socks = smart_proxy[1].equals("socks");
                            } else {
                                LOG.log(Level.WARNING, "{0} StreamWorker [{1}] SmartProxy exhausted -- falling back to direct", new Object[]{Thread.currentThread().getName(), _id});
                                current_smart_proxy = null;
                                excluded_proxy_list.clear();
                            }

                        }

                        if (current_smart_proxy != null) {

                            int proxy_port = parseProxyPort(current_smart_proxy);

                            if (proxy_port < 0) {
                                LOG.log(Level.WARNING, "{0} StreamWorker [{1}] malformed smart proxy entry {2} -- banning + direct fallback",
                                        new Object[]{Thread.currentThread().getName(), _id, current_smart_proxy});
                                proxy_manager.blockProxy(current_smart_proxy, "Malformed entry");
                                excluded_proxy_list.add(current_smart_proxy);
                                current_smart_proxy = null;
                                URL chunk_url = new URL(chunk_stream.getUrl());
                                con = (HttpURLConnection) chunk_url.openConnection();
                            } else {
                                String[] proxy_info = current_smart_proxy.split(":");
                                Proxy proxy = new Proxy(smart_proxy_socks ? Proxy.Type.SOCKS : Proxy.Type.HTTP, new InetSocketAddress(proxy_info[0], proxy_port));
                                URL chunk_url = new URL(chunk_stream.getUrl());
                                con = (HttpURLConnection) chunk_url.openConnection(proxy);
                            }

                        } else {

                            URL chunk_url = new URL(chunk_stream.getUrl());

                            con = (HttpURLConnection) chunk_url.openConnection();
                        }

                    } else {

                        URL chunk_url = new URL(chunk_stream.getUrl());

                        if (MainPanel.isUse_proxy()) {

                            con = (HttpURLConnection) chunk_url.openConnection(new Proxy(smart_proxy_socks ? Proxy.Type.SOCKS : Proxy.Type.HTTP, new InetSocketAddress(MainPanel.getProxy_host(), MainPanel.getProxy_port())));

                            if (MainPanel.getProxy_user() != null && !"".equals(MainPanel.getProxy_user())) {

                                con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((MainPanel.getProxy_user() + ":" + MainPanel.getProxy_pass()).getBytes("UTF-8")));
                            }
                        } else {

                            if (current_smart_proxy != null) {

                                int proxy_port = parseProxyPort(current_smart_proxy);

                                if (proxy_port < 0) {
                                    LOG.log(Level.WARNING, "{0} StreamWorker [{1}] malformed smart proxy entry {2} -- banning + direct fallback",
                                            new Object[]{Thread.currentThread().getName(), _id, current_smart_proxy});
                                    proxy_manager.blockProxy(current_smart_proxy, "Malformed entry");
                                    excluded_proxy_list.add(current_smart_proxy);
                                    current_smart_proxy = null;
                                    con = (HttpURLConnection) chunk_url.openConnection();
                                } else {
                                    String[] proxy_info = current_smart_proxy.split(":");
                                    Proxy proxy = new Proxy(smart_proxy_socks ? Proxy.Type.SOCKS : Proxy.Type.HTTP, new InetSocketAddress(proxy_info[0], proxy_port));
                                    con = (HttpURLConnection) chunk_url.openConnection(proxy);
                                }

                            } else {
                                con = (HttpURLConnection) chunk_url.openConnection();
                            }
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

                    LOG.log(Level.INFO, "{0} Worker [{1}]: offset: {2} size: {3}", new Object[]{Thread.currentThread().getName(), _id, offset, chunk_stream.getSize()});

                    http_error = 0;

                    try {

                        if (!_exit) {

                            http_status = con.getResponseCode();

                            if (http_status != 200) {

                                LOG.log(Level.INFO, "{0} Failed : HTTP error code : {1}", new Object[]{Thread.currentThread().getName(), http_status});

                                http_error = http_status;

                                MiscTools.drainAndCloseErrorStream(con);

                            } else {

                                try (InputStream is = con.getInputStream()) {

                                    int chunk_writes = 0;

                                    while (!_exit && !_chunkmanager.isExit() && chunk_writes < chunk_stream.getSize() && (reads = is.read(buffer, 0, Math.min((int) (chunk_stream.getSize() - chunk_writes), buffer.length))) != -1) {

                                        chunk_stream.getOutputStream().write(buffer, 0, reads);

                                        chunk_writes += reads;
                                    }

                                    if (chunk_stream.getSize() == chunk_writes) {

                                        LOG.log(Level.INFO, "{0} Worker [{1}] has downloaded chunk [{2}]!", new Object[]{Thread.currentThread().getName(), _id, chunk_stream.getOffset()});

                                        _chunkmanager.getChunk_queue().put(chunk_stream.getOffset(), chunk_stream);

                                        _chunkmanager.secureNotifyAll();

                                        current_smart_proxy = null;

                                        excluded_proxy_list.clear();
                                    }
                                }
                            }
                        }

                    } catch (IOException ex) {
                        LOG.log(Level.SEVERE, ex.getMessage());
                    } finally {
                        con.disconnect();
                    }

                } else {

                    _exit = true;
                }
            }

        } catch (IOException | URISyntaxException | ChunkInvalidException | InterruptedException ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        } catch (OutOfMemoryError | Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        }

        _chunkmanager.secureNotifyAll();

        LOG.log(Level.INFO, "{0} Worker [{1}]: bye bye", new Object[]{Thread.currentThread().getName(), _id});
    }

}
