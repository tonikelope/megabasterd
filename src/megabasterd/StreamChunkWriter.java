/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package megabasterd;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author tonikelope
 */
public class StreamChunkWriter implements Runnable, SecureMultiThreadNotifiable {

    public static final int CHUNK_SIZE = 1048576;
    public static final int BUFFER_CHUNKS_SIZE = 20;
    private long _next_offset_required;
    private long _bytes_written;
    private final long _start_offset;
    private final long _end_offset;
    private final String _mega_account;
    private final ConcurrentHashMap<Long, StreamChunk> _chunk_queue;
    private final ConcurrentHashMap<Thread, Boolean> _notified_threads;
    private final PipedOutputStream _pipeos;
    private String _url;
    private final HashMap _file_info;
    private final String _link;
    private final Object _secure_notify_lock;
    private final Object _chunk_offset_lock;
    private final KissVideoStreamServer _server;
    private volatile boolean _exit; 

    public StreamChunkWriter(KissVideoStreamServer server, String link, HashMap file_info, String mega_account, PipedOutputStream pipeos, String url, long start_offset, long end_offset) {
        _server=server;
        _link=link;
        _mega_account=mega_account;
        _file_info=file_info;
        _bytes_written = start_offset;
        _pipeos = pipeos;
        _start_offset = start_offset;
        _end_offset = end_offset;
        _next_offset_required = start_offset;
        _chunk_queue = new ConcurrentHashMap<>();
        _notified_threads = new ConcurrentHashMap<>();
        _secure_notify_lock = new Object();
        _chunk_offset_lock = new Object();
        _url=url;
        _exit = false;
    }

    public String getUrl() throws IOException, InterruptedException {
        
        if(!MiscTools.checkMegaDownloadUrl(_url)) {
            
            _url = _server.getMegaFileDownloadUrl(_link, (String)_file_info.get("pass_hash"), (String)_file_info.get("noexpiretoken"), _mega_account);
            _file_info.put("url", _url);
            _server.getLink_cache().put(_link, _file_info);
        }
            
        return _url;
    }
    
    public boolean isExit() {
        return _exit;
    }

    public ConcurrentHashMap<Long, StreamChunk> getChunk_queue() {
        return _chunk_queue;
    }

    @Override
    public void run() {

        StreamChunk current_chunk;
        byte[] buffer = new byte[16 * 1024];
        int reads;

        try {

            System.out.println("StreamChunkWriter: let's do some work! Start: " + _start_offset + "   End: " + _end_offset);

            while (!_exit && _bytes_written < _end_offset) {

                while (!_exit && _bytes_written < _end_offset && _chunk_queue.containsKey(_bytes_written)) {

                    current_chunk = _chunk_queue.get(_bytes_written);

                    InputStream is = current_chunk.getInputStream();

                    while (!_exit && (reads = is.read(buffer)) != -1) {

                        _pipeos.write(buffer, 0, reads);

                        _bytes_written += reads;
                    }

                    _chunk_queue.remove(current_chunk.getOffset());

                    secureNotifyAll();

                    System.out.println(Thread.currentThread().getName() + " StreamChunkWriter ha escrito " + (_bytes_written) + " / " + _end_offset + " ...");

                }

                if (!_exit && _bytes_written < _end_offset) {

                    System.out.println(Thread.currentThread().getName() + " StreamChunkWriter waiting for offset " + _bytes_written + "...");

                    secureWait();
                }
            }

        } catch (Exception ex) {

            System.out.println(ex.getMessage());
        }

        try {
            _pipeos.close();
        } catch (IOException ex) {
            Logger.getLogger(StreamChunkWriter.class.getName()).log(Level.SEVERE, null, ex);
        }

        _exit = true;

        System.out.println("StreamChunkWriter: bye bye");
    }

    public long nextOffset() {

        synchronized (_chunk_offset_lock) {

            long next_offset = _next_offset_required;

            _next_offset_required = _next_offset_required + CHUNK_SIZE < _end_offset ? _next_offset_required + CHUNK_SIZE : -1;

            return next_offset;
        }
    }

    public long calculateChunkSize(long offset) {

        return offset <= _end_offset ? (Math.min(CHUNK_SIZE, _end_offset - offset + 1)) : -1;
    }

    public void setExit(boolean _exit) {
        this._exit = _exit;
    }

    @Override
    public void secureWait() {

        synchronized (_secure_notify_lock) {

            Thread current_thread = Thread.currentThread();

            if (!_notified_threads.containsKey(current_thread)) {

                _notified_threads.put(current_thread, false);
            }

            while (!_notified_threads.get(current_thread)) {

                try {
                    _secure_notify_lock.wait();
                } catch (InterruptedException ex) {
                    getLogger(StreamThrottlerSupervisor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            _notified_threads.put(current_thread, false);
        }
    }

    @Override
    public void secureNotifyAll() {

        synchronized (_secure_notify_lock) {

            for (Map.Entry<Thread, Boolean> entry : _notified_threads.entrySet()) {

                entry.setValue(true);
            }

            _secure_notify_lock.notifyAll();
        }
    }

}
