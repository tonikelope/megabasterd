package megabasterd;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import static java.util.logging.Logger.getLogger;
import static megabasterd.MainPanel.THROTTLE_SLICE_SIZE;
import static megabasterd.MainPanel.USER_AGENT;
import static megabasterd.MiscTools.getWaitTimeExpBackOff;
import static megabasterd.MiscTools.swingReflectionInvoke;


/**
 *
 * @author tonikelope
 */
public class ChunkDownloader implements Runnable, SecureNotifiable {
    
    private final int _id;
    private final Download _download;
    private volatile boolean _exit;
    private final Object _secure_notify_lock;
    private boolean _notified;

    
    public ChunkDownloader(int id, Download download)
    {
        _notified = false;
        _exit = false;
        _secure_notify_lock = new Object();
        _id = id;
        _download = download;
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
    
    @Override
    public void secureNotify()
    {
        synchronized(_secure_notify_lock) {
      
            _notified = true;
            
            _secure_notify_lock.notify();
        }
    }
    
    @Override
    public void secureWait() {
        
        synchronized(_secure_notify_lock)
        {
            while(!_notified) {

                try {
                    _secure_notify_lock.wait();
                } catch (InterruptedException ex) {
                    getLogger(ChunkDownloader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            _notified = false;
        }
    }
    
    @Override
    public void secureNotifyAll() {
        
        synchronized(_secure_notify_lock) {
            
            _notified = true;
      
            _secure_notify_lock.notifyAll();
        }
    }
    
    @Override
    public void run()
    {
        String worker_url=null;
        Chunk chunk;
        int reads, conta_error, http_status;
        byte[] buffer = new byte[THROTTLE_SLICE_SIZE];
        InputStream is;
        boolean error;
        
        System.out.println("Worker ["+_id+"]: let's do some work!");

        try
        {
            conta_error = 0;
            
            error = false;
            
            while(!_exit && !_download.isStopped())
            {
                if(worker_url == null || error) {
                    
                    worker_url=_download.getDownloadUrlForWorker();
                }
                
                chunk = new Chunk(_download.nextChunkId(), _download.getFile_size(), worker_url);

                URL url = new URL(chunk.getUrl());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(MainPanel.CONNECTION_TIMEOUT);
                conn.setReadTimeout(MainPanel.CONNECTION_TIMEOUT);
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setRequestProperty("Connection", "close");
                
                error = false;

                try{

                    if(!_exit && !_download.isStopped()) {
                        
                        is = new ThrottledInputStream(conn.getInputStream(), _download.getMain_panel().getStream_supervisor());

                        http_status = conn.getResponseCode();

                        if ( http_status != HttpURLConnection.HTTP_OK )
                        {   
                            System.out.println("Failed : HTTP error code : " + http_status);
                            
                            error = true;

                        } else {

                            while(!_exit && !_download.isStopped() && !_download.getChunkwriter().isExit() && chunk.getOutputStream().size() < chunk.getSize() && (reads=is.read(buffer))!=-1 ) 
                            {
                                chunk.getOutputStream().write(buffer, 0, reads);

                                _download.getPartialProgressQueue().add(reads);
                                
                                _download.getProgress_meter().secureNotify();

                                if(_download.isPaused() && !_download.isStopped()) {

                                    _download.pause_worker();

                                    secureWait();
                                }
                            }

                            is.close();
                            
                            if(chunk.getOutputStream().size() < chunk.getSize()) {
                                
                                if(chunk.getOutputStream().size() > 0)
                                {
                                    _download.getPartialProgressQueue().add(-1*chunk.getOutputStream().size());
                                    
                                   _download.getProgress_meter().secureNotify();

                                }

                                error = true;
                            }
                        }

                        if(error && !_download.isStopped()) {
                            
                            _download.rejectChunkId(chunk.getId());
                            
                            conta_error++;

                            Thread.sleep(getWaitTimeExpBackOff(conta_error)*1000);

                        } else if(!error) {
                            
                            System.out.println("Worker ["+_id+"] has downloaded chunk ["+chunk.getId()+"]!");

                            _download.getChunkwriter().getChunk_queue().put(chunk.getId(), chunk);

                            _download.getChunkwriter().secureNotify();
                            
                            conta_error = 0;
                        }
                        
                    } else if(_exit) {
                        
                       _download.rejectChunkId(chunk.getId());
                    }
               }
               catch (IOException ex) 
               {
                    error = true;
           
                    _download.rejectChunkId(chunk.getId());

                    if(chunk.getOutputStream().size() > 0)
                    {
                        _download.getPartialProgressQueue().add(-1*chunk.getOutputStream().size());

                        _download.getProgress_meter().secureNotify();
                    }

                    getLogger(ChunkDownloader.class.getName()).log(Level.SEVERE, null, ex);
     
               } catch (InterruptedException ex) {
                    getLogger(ChunkDownloader.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    conn.disconnect();
                }
            }
        
        }catch(ChunkInvalidIdException e) {
        
        }catch (IOException ex) {
            _download.emergencyStopDownloader(ex.getMessage());
            getLogger(ChunkDownloader.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if(!_exit) {
            
            swingReflectionInvoke("setEnabled", _download.getView().getSlots_spinner(), false);
            
            swingReflectionInvoke("setText", _download.getView().getSlot_status_label(), "");
            
            _download.setFinishing_download(true);
            
        } else if(!_download.isFinishing_download()) {
            
            swingReflectionInvoke("setEnabled", _download.getView().getSlots_spinner(), true);
            
            swingReflectionInvoke("setText", _download.getView().getSlot_status_label(), "");
        }

        _download.stopThisSlot(this);

        _download.getChunkwriter().secureNotify();
        
        System.out.println("Worker ["+_id+"]: bye bye");
    }

    

}
