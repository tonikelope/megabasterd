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

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpEntity;

import static com.tonikelope.megabasterd.MainPanel.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public class ChunkDownloader implements Runnable, SecureSingleThreadNotifiable {

    public static final int SMART_PROXY_RECHECK_509_TIME = 3600;
    private static final Logger LOG = Logger.getLogger(ChunkDownloader.class.getName());
    private final int _id;
    private final Download _download;
    private volatile boolean _exit;
    private final Object _secure_notify_lock;
    private volatile boolean _error_wait;
    private volatile boolean _chunk_exception;
    private volatile boolean _notified;
    private final ArrayList<String> _excluded_proxy_list = new ArrayList<>();
    private volatile boolean _reset_current_chunk;
    private volatile InputStream _chunk_inputstream = null;
    private volatile long _509_timestamp = -1;
    private final HashMap<FastMegaHttpClientBuilder.FMEventType, Function0<Unit>> clientListenerMap;

    private String _current_smart_proxy;

    public void RESET_CURRENT_CHUNK() {

        if (_chunk_inputstream != null) {

            this._reset_current_chunk = true;

            try {
                _chunk_inputstream.close();
            } catch (IOException ex) {
                Logger.getLogger(ChunkDownloader.class.getName()).log(Level.SEVERE, null, ex);
            }

            _chunk_inputstream = null;
        }
    }

    public ChunkDownloader(int id, Download download) {
        _notified = false;
        _exit = false;
        _chunk_exception = false;
        _secure_notify_lock = new Object();
        _id = id;
        _download = download;
        _current_smart_proxy = null;
        _error_wait = false;
        _reset_current_chunk = false;
        clientListenerMap = new HashMap<>() {{
            put(FastMegaHttpClientBuilder.FMEventType.CURRENT_SMART_PROXY_ERRORED, () -> {
                if (!timeoutError.get() && httpError.get() != 429) {
                    MainPanel.getProxy_manager().blockProxy(_current_smart_proxy, timeoutError.get() ? "TIMEOUT!" : "HTTP " + httpError.get());
                } else if (timeoutError.get()) {
                    _excluded_proxy_list.add(_current_smart_proxy);
                    LOG.log(Level.WARNING, "{0} Worker [{1}] PROXY {2} TIMEOUT", new Object[]{Thread.currentThread().getName(), _id, _current_smart_proxy});
                } else {
                    _excluded_proxy_list.add(_current_smart_proxy);
                    LOG.log(Level.WARNING, "{0} Worker [{1}] PROXY {2} TOO MANY CONNECTIONS", new Object[]{Thread.currentThread().getName(), _id, _current_smart_proxy});
                }
                return Unit.INSTANCE;
            });

            put(FastMegaHttpClientBuilder.FMEventType.SMART_PROXY_NULL, () -> {
                LOG.log(Level.INFO, "{0} Worker [{1}] SmartProxy getProxy returned NULL! {2}", new Object[]{Thread.currentThread().getName(), _id, _download.getFile_name()});
                return Unit.INSTANCE;
            });

            put(FastMegaHttpClientBuilder.FMEventType.WILL_USE_SMART_PROXY, () -> {
                if (!download.isTurbo()) download.enableTurboMode();
                return Unit.INSTANCE;
            });
        }};
    }

    public boolean isChunk_exception() {
        return _chunk_exception;
    }

    public String getCurrent_smart_proxy() {
        return _current_smart_proxy;
    }

    public void setExit(boolean exit) {
        _exit = exit;
    }

    public boolean isExit() {
        return _exit;
    }

    public Download getDownload() {
        return _download;
    }

    public int getId() {
        return _id;
    }

    public boolean isError_wait() {
        return _error_wait;
    }

    public void setError_wait(boolean error_wait) {
        _error_wait = error_wait;
    }

    @Override
    public void secureNotify() {
        synchronized (_secure_notify_lock) {

            _notified = true;

            _secure_notify_lock.notify();
        }
    }

    @Override
    public void secureWait() {

        synchronized (_secure_notify_lock) {
            while (!_notified) {

                try {
                    _secure_notify_lock.wait(1000);
                } catch (InterruptedException ex) {
                    _exit = true;
                    LOG.log(Level.SEVERE, ex.getMessage());
                }
            }

            _notified = false;
        }
    }

    private final AtomicBoolean timeoutError = new AtomicBoolean(false);
    private final AtomicInteger httpError = new AtomicInteger(0);

    private final FastMegaHttpClientBuilder.MegaHttpProxyConfiguration chunkDownloaderProxyConfig = new FastMegaHttpClientBuilder.MegaHttpProxyConfiguration(
        FastMegaHttpClientBuilder.FMProxyType.SMART,
        () -> _excluded_proxy_list,
        /* Extra smart conditions */ () -> httpError.get() == 509 || (_509_timestamp != -1 && _509_timestamp + SMART_PROXY_RECHECK_509_TIME * 1000 > System.currentTimeMillis()),
        /* Re-use smart condition */ () ->  httpError.get() != 429,
        true,
        false,
        (newCurrentSmartProxy) -> {
            _current_smart_proxy = newCurrentSmartProxy;
            return Unit.INSTANCE;
        }
    );

    private CloseableHttpClient createChunkDownloaderClient(HttpGet request) {
        return new FastMegaHttpClientBuilder<>(
            request,
            RequestConfig.custom(),
            chunkDownloaderProxyConfig,
            clientListenerMap
        ).withProperty(FastMegaHttpClientBuilder.FMProperty.NO_CACHE).build();
    }

    private byte[] getScaledBuffer() {
        // Dynamically scale buffer
        int bufferSize = DEFAULT_BYTE_BUFFER_SIZE;
        long fileSize = _download.getFile_size();
        if (fileSize > 1_000_000_000) { // 1 GB
            // Realistically we _could_ go higher here, but there are diminishing returns
            // after scaling above 512 KB due to HTTP overhead and memory usage.
            bufferSize = 512 * 1024; // 512 KB
        } else if (fileSize > 100_000_000) { // 100 MB
            bufferSize = 256 * 1024; // 256 KB
        } else if (fileSize > 10_000_000) { // 10 MB
            bufferSize = 128 * 1024; // 128 KB
        } else if (fileSize > 1_000_000) { // 1 MB
            bufferSize = 64 * 1024; // 64 KB
        }
        return new byte[bufferSize];
    }

    @Override
    public void run() {

        LOG.log(Level.INFO, "{0} Worker [{1}]: let''s do some work! {2}", new Object[]{Thread.currentThread().getName(), _id, _download.getFile_name()});

        try {
            timeoutError.set(false);
            httpError.set(0);
            AtomicInteger httpStatus = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            AtomicBoolean chunkError = new AtomicBoolean(false);
            String worker_url = null;
            SmartMegaProxyManager proxy_manager = MainPanel.getProxy_manager();
            byte[] buffer = getScaledBuffer();

            while (!_download.getMain_panel().isExit() && !_exit && !_download.isStopped()) {
                if (_download.isPaused() && !_download.isStopped() && !_download.getChunkmanager().isExit()) {
                    _download.pause_worker();
                    secureWait();
                }

                if (httpError.get() == 509 && (_509_timestamp == -1 || _509_timestamp + SMART_PROXY_RECHECK_509_TIME * 1000 < System.currentTimeMillis())) {
                    _509_timestamp = System.currentTimeMillis();
                }

                if (worker_url == null || httpError.get() == 403) {
                    worker_url = _download.getDownloadUrlForWorker();
                }

                long chunk_id = _download.nextChunkId();
                long chunk_offset = ChunkWriterManager.calculateChunkOffset(chunk_id, Download.CHUNK_SIZE_MULTI);
                long chunk_size = ChunkWriterManager.calculateChunkSize(chunk_id, _download.getFile_size(), chunk_offset, Download.CHUNK_SIZE_MULTI);

                ChunkWriterManager.checkChunkID(chunk_id, _download.getFile_size(), chunk_offset);

                String chunk_url = ChunkWriterManager.genChunkUrl(worker_url, _download.getFile_size(), chunk_offset, chunk_size);

                if (httpError.get() == 509 && MainPanel.isRun_command()) {
                    MainPanel.run_external_command();
                } else if (httpError.get() != 509 && MainPanel.LAST_EXTERNAL_COMMAND_TIMESTAMP != -1) {
                    MainPanel.LAST_EXTERNAL_COMMAND_TIMESTAMP = -1;
                }

                long chunk_reads = 0;
                chunkError.set(true);
                timeoutError.set(false);
                httpError.set(0);
                File tmp_chunk_file = null, chunk_file = null;

                LOG.log(Level.INFO, "{0} Worker [{1}] is downloading chunk [{2}]! {3}", new Object[]{Thread.currentThread().getName(), _id, chunk_id, _download.getFile_name()});

                HttpGet getRequest = new HttpGet(chunk_url);
                try (
                        CloseableHttpClient client = createChunkDownloaderClient(getRequest);
                        CloseableHttpResponse response = client.execute(getRequest)
                ) {

                    HttpEntity entity = response.getEntity();

                    if (!_exit && !_download.isStopped()) {

                        httpStatus.set(response.getCode());

                        if (httpStatus.get() != 200) {

                            LOG.log(Level.INFO, "{0} Worker [{1}] Failed chunk download : HTTP error code : {2} {3}", new Object[]{Thread.currentThread().getName(), _id, httpStatus.get(), _download.getFile_name()});

                            httpError.set(httpStatus.get());

                        } else {

                            chunk_file = new File(_download.getChunkmanager().getChunks_dir() + "/" + MiscTools.HashString("sha1", _download.getUrl()) + ".chunk" + chunk_id);

                            if (!chunk_file.exists() || chunk_file.length() != chunk_size) {

                                tmp_chunk_file = new File(_download.getChunkmanager().getChunks_dir() + "/" + MiscTools.HashString("sha1", _download.getUrl()) + ".chunk" + chunk_id + ".tmp");

                                InputStream rawStream = entity.getContent();

                                _chunk_inputstream = new KThrottledInputStream(rawStream, _download.getMain_panel().getStream_supervisor()) {
                                    @Override
                                    public void close() throws IOException {
                                        super.close();
                                        response.close();
                                    }
                                };

                                Path tmpChunkFilePath = tmp_chunk_file.toPath();
                                try (AsynchronousFileChannel asyncFileChannel = AsynchronousFileChannel.open(tmpChunkFilePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
                                    ByteBuffer byteBuffer = ByteBuffer.allocate(buffer.length);
                                    long position = 0;

                                    while (!_reset_current_chunk && !_exit && !_download.isStopped() && !_download.getChunkmanager().isExit() && chunk_reads < chunk_size) {
                                        int bytesRead = _chunk_inputstream.read(buffer, 0, Math.min((int) (chunk_size - chunk_reads), buffer.length));
                                        if (bytesRead == -1) break;

                                        byteBuffer.clear();
                                        byteBuffer.put(buffer, 0, bytesRead);
                                        byteBuffer.flip();

                                        Future<Integer> writeResult = asyncFileChannel.write(byteBuffer, position);
                                        writeResult.get();

                                        position += bytesRead;
                                        chunk_reads += bytesRead;

                                        _download.getPartialProgress().add((long) bytesRead);
                                        _download.getProgress_meter().secureNotify();

                                        if (!_reset_current_chunk && _download.isPaused() && !_exit && !_download.isStopped() && !_download.getChunkmanager().isExit() && chunk_reads < chunk_size) {

                                            _download.pause_worker();
                                            secureWait();
                                        }
                                    }
                                }

                            } else {

                                LOG.log(Level.INFO, "{0} Worker [{1}] has RECOVERED PREVIOUS chunk [{2}]! {3}", new Object[]{Thread.currentThread().getName(), _id, chunk_id, _download.getFile_name()});

                                chunk_reads = chunk_size;

                                _download.getPartialProgress().add(chunk_size);

                                _download.getProgress_meter().secureNotify();
                            }

                        }

                        if (chunk_reads == chunk_size) {

                            LOG.log(Level.INFO, "{0} Worker [{1}] has OK DOWNLOADED (" + (_current_smart_proxy != null ? "smartproxy" : "direct") + ") chunk [{2}]! {3}", new Object[]{Thread.currentThread().getName(), _id, chunk_id, _download.getFile_name()});

                            if (tmp_chunk_file != null && chunk_file != null && (!chunk_file.exists() || chunk_file.length() != chunk_size)) {

                                if (chunk_file.exists()) {
                                    chunk_file.delete();
                                }

                                tmp_chunk_file.renameTo(chunk_file);
                            }

                            chunkError.set(false);

                            errorCount.set(0);

                            httpError.set(0);

                            if (_current_smart_proxy != null && _509_timestamp != -1) {

                                _509_timestamp = -1;

                                if (_download.isTurbo()) {
                                    _download.disableTurboMode();
                                }
                            }

                            _excluded_proxy_list.clear();

                            _download.getChunkmanager().secureNotify();
                        }
                    }

                } catch (IOException | IllegalStateException ex) {

                    if (ex instanceof SocketTimeoutException) {
                        timeoutError.set(true);
                        LOG.log(Level.WARNING, "{0} Worker [{1}] TIMEOUT downloading chunk [{2}]! {3}", new Object[]{Thread.currentThread().getName(), _id, chunk_id, _download.getFile_name()});

                    } else {
                        LOG.log(Level.SEVERE, ex.getMessage());
                    }

                } finally {

                    if (_chunk_inputstream != null) {

                        try {
                            _chunk_inputstream.close();
                        } catch (IOException ex) {
                            Logger.getLogger(ChunkDownloader.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        _chunk_inputstream = null;
                    }

                    if (chunkError.get()) {

                        LOG.log(Level.INFO, "{0} Worker [{1}] has FAILED downloading (" + (_current_smart_proxy != null ? "smartproxy" : "direct") + ") chunk [{2}]! {3}", new Object[]{Thread.currentThread().getName(), _id, chunk_id, _download.getFile_name()});

                        if (tmp_chunk_file != null && tmp_chunk_file.exists()) {
                            tmp_chunk_file.delete();
                        }

                        _download.rejectChunkId(chunk_id);

                        if (chunk_reads > 0) {
                            _download.getPartialProgress().add(-1 * chunk_reads);
                            _download.getProgress_meter().secureNotify();
                        }

                        if (!_exit && !_download.isStopped() && !timeoutError.get() && _current_smart_proxy == null && httpError.get() != 509 && httpError.get() != 403 && httpError.get() != 503) {

                            _error_wait = true;

                            _download.getView().updateSlotsStatus();

                            try {
                                Thread.sleep(MiscTools.getWaitTimeExpBackOff(errorCount.addAndGet(1)) * 1000);
                            } catch (InterruptedException exc) {
                            }

                            _error_wait = false;

                            _download.getView().updateSlotsStatus();

                        } else if (httpError.get() == 503 && _current_smart_proxy == null && !_download.isTurbo()) {
                            setExit(true);
                        }

                    } else if (proxy_manager != null && proxy_manager.isReset_slot_proxy()) {
                        _current_smart_proxy = null;
                    }

                    if (_reset_current_chunk) {
                        LOG.log(Level.WARNING, "Worker [{0}] FORCE RESET CHUNK [{1}]! {2}", new Object[]{_id, chunk_id, _download.getFile_name()});
                        _current_smart_proxy = null;
                        _reset_current_chunk = false;
                    }
                }
            }

        } catch (ChunkInvalidException e) {

            _chunk_exception = true;

        } catch (OutOfMemoryError | Exception error) {

            _download.stopDownloader(error.getMessage());

            LOG.log(Level.SEVERE, error.getMessage());

        }

        _download.stopThisSlot(this);

        _download.getChunkmanager().secureNotify();

        LOG.log(Level.INFO, "{0} ChunkDownloader [{1}] {2}: bye bye", new Object[]{Thread.currentThread().getName(), _id, _download.getFile_name()});
    }
}
