package megabasterd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import static java.util.logging.Logger.getLogger;
import java.util.zip.GZIPInputStream;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import static megabasterd.MiscTools.getWaitTimeExpBackOff;



/**
 *
 * @author tonikelope
 */
public final class ChunkUploader implements Runnable, SecureNotifiable {
    
    private final int _id;
    private final Upload _upload;
    private volatile boolean _exit;
    private final Object _secure_notify_lock;
    private volatile boolean _error_wait;
    private boolean _notified;

    public ChunkUploader(int id, Upload upload)
    {
        _notified = false;
        _secure_notify_lock = new Object();
        _id = id;
        _upload = upload;
        _exit = false;
        _error_wait = false;
    }
    
    public void setExit(boolean exit) {
        _exit = exit;
    }

    public boolean isError_wait() {
        return _error_wait;
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
                    getLogger(ChunkUploader.class.getName()).log(Level.SEVERE, null, ex);
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
    
    public int getId() {
        return _id;
    }

    public boolean isExit() {
        return _exit;
    }
    
    
    
    @Override
    public void run()
    {
        System.out.println("ChunkUploader "+_id+" hello!");
        
        String worker_url=_upload.getUl_url();
        Chunk chunk;
        int reads, to_read, conta_error, re, http_status, tot_bytes_up;
        byte[] buffer = new byte[MainPanel.THROTTLE_SLICE_SIZE];
        boolean error;
        OutputStream out;
    

        try
        {
            RandomAccessFile f = new RandomAccessFile(_upload.getFile_name(), "r");
            
            conta_error = 0;
            
            while(!_exit && !_upload.isStopped())
            { 
                chunk = new Chunk(_upload.nextChunkId(), _upload.getFile_size(), worker_url);

                f.seek(chunk.getOffset());
                
                do
                {
                    to_read = chunk.getSize() - chunk.getOutputStream().size() >= buffer.length?buffer.length:(int)(chunk.getSize() - chunk.getOutputStream().size());

                    re=f.read(buffer, 0, to_read);

                    chunk.getOutputStream().write(buffer, 0, re);

                }while(!_exit && !_upload.isStopped() && chunk.getOutputStream().size()<chunk.getSize());
                
                URL url = new URL(chunk.getUrl());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(MainPanel.CONNECTION_TIMEOUT);
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("User-Agent", MegaAPI.USER_AGENT);
                conn.setRequestProperty("Connection", "close");
                conn.setFixedLengthStreamingMode(chunk.getSize());
                
                tot_bytes_up=0;
                
                error = false;
                
                try{

                    if(!_exit && !_upload.isStopped()) {
                        
                        CipherInputStream cis = new CipherInputStream(chunk.getInputStream(), CryptTools.genCrypter("AES", "AES/CTR/NoPadding", _upload.getByte_file_key(), CryptTools.forwardMEGALinkKeyIV(_upload.getByte_file_iv(), chunk.getOffset())));

                        out = new ThrottledOutputStream(conn.getOutputStream(), _upload.getMain_panel().getStream_supervisor());
                        
                        System.out.println(" Subiendo chunk "+chunk.getId()+" desde worker "+ _id +"...");

                        while( !_exit && !_upload.isStopped() && (reads=cis.read(buffer))!=-1 )
                        {
                            out.write(buffer, 0, reads);

                            _upload.getPartialProgress().add(reads);

                            _upload.getProgress_meter().secureNotify();
                            
                            tot_bytes_up+=reads;
                            
                            if(_upload.isPaused() && !_upload.isStopped()) {

                                _upload.pause_worker();

                                secureWait();
                            }
                        }
                        
                        if(!_upload.isStopped()) {
                            
                            http_status = conn.getResponseCode();

                            if (http_status != HttpURLConnection.HTTP_OK )
                            {   
                                System.out.println("Failed : HTTP error code : " + http_status);

                                error = true;

                            } else {
                                
                                if(tot_bytes_up < chunk.getSize())
                                {
                                    if(tot_bytes_up > 0) {
                                        
                                        _upload.getPartialProgress().add(-1*tot_bytes_up);

                                        _upload.getProgress_meter().secureNotify();
                                    }
                                    
                                    error = true;
                                    
                                } else {
                                    
                                    if(_upload.getCompletion_handle() == null) {
                                        
                                        String content_encoding = conn.getContentEncoding();
            
                                        InputStream is=(content_encoding!=null && content_encoding.equals("gzip"))?new GZIPInputStream(conn.getInputStream()):conn.getInputStream();

                                        ByteArrayOutputStream byte_res = new ByteArrayOutputStream();

                                        while( (reads=is.read(buffer)) != -1 ) {

                                            byte_res.write(buffer, 0, reads);
                                        }

                                        String response = new String(byte_res.toByteArray());

                                        if(response.length() > 0) {

                                            if( MegaAPI.checkMEGAError(response) != 0 )
                                            {
                                                error = true;

                                            } else {

                                                System.out.println("Completion handle -> "+response);

                                                _upload.setCompletion_handle(response);
                                            }
                                        } 
                                    }
                                }
                            }
                            
                            if(error && !_upload.isStopped()) {

                                _upload.rejectChunkId(chunk.getId());
                            
                                conta_error++;

                                if(!_exit) {
                                    
                                    _error_wait = true;
                                    
                                    _upload.getView().updateSlotsStatus();

                                    Thread.sleep(getWaitTimeExpBackOff(conta_error)*1000);
                                    
                                    _error_wait = false;
                                    
                                    _upload.getView().updateSlotsStatus();
                                }

                            } else if(!error) {
                                
                                System.out.println(" Worker "+_id+" ha subido chunk "+chunk.getId());

                                _upload.getMac_generator().getChunk_queue().put(chunk.getId(), chunk);

                                _upload.getMac_generator().secureNotify();
                                
                                conta_error = 0;
                            }
                            
                        }
                        
                    } else if(_exit) {
                        
                        _upload.rejectChunkId(chunk.getId());
                    }
               }
               catch (IOException ex) 
               {      
                   _upload.rejectChunkId(chunk.getId());
                   
                    if(tot_bytes_up > 0) {
                                        
                        _upload.getPartialProgress().add(-1*tot_bytes_up);

                        _upload.getProgress_meter().secureNotify();
                    }

                    getLogger(ChunkUploader.class.getName()).log(Level.SEVERE, null, ex);
                    
                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | InterruptedException ex) {
                    getLogger(ChunkUploader.class.getName()).log(Level.SEVERE, null, ex);
                    
                } finally {
                    conn.disconnect();
                }
                
            }
        
        }catch(ChunkInvalidIdException e) {
            
        } catch (IOException ex) {
            
            _upload.emergencyStopUploader(ex.getMessage());
            
            getLogger(ChunkUploader.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        _upload.stopThisSlot(this);

        _upload.getMac_generator().secureNotify();

        System.out.println("ChunkUploader "+_id+" bye bye...");
    }

}
