/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
public class ChunkUploaderMono extends ChunkUploader {
    
    public ChunkUploaderMono(int id, Upload upload) {
        super(id, upload);
    }
    
    @Override
    public void run()
    {
        System.out.println("ChunkUploader "+getId()+" hello!");
        
        String worker_url=getUpload().getUl_url();
        Chunk chunk;
        int reads, to_read, conta_error, re, http_status, tot_bytes_up=0;
        byte[] buffer = new byte[MainPanel.THROTTLE_SLICE_SIZE];
        boolean error = false;
        OutputStream out=null;
        HttpURLConnection conn=null;
    
        try
        {
            RandomAccessFile f = new RandomAccessFile(getUpload().getFile_name(), "r");
            
            conta_error = 0;
            
            URL url = null;
            
            while(!isExit() && !getUpload().isStopped())
            { 
                chunk = new Chunk(getUpload().nextChunkId(), getUpload().getFile_size(), worker_url);

                f.seek(chunk.getOffset());
                
                do
                {
                    to_read = chunk.getSize() - chunk.getOutputStream().size() >= buffer.length?buffer.length:(int)(chunk.getSize() - chunk.getOutputStream().size());

                    re=f.read(buffer, 0, to_read);

                    chunk.getOutputStream().write(buffer, 0, re);

                }while(!isExit() && !getUpload().isStopped() && chunk.getOutputStream().size()<chunk.getSize());
                
                if(url == null || error) {
                
                    url = new URL(worker_url+"/"+chunk.getOffset());
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(MainPanel.CONNECTION_TIMEOUT);
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("User-Agent", MegaAPI.USER_AGENT);
                    conn.setRequestProperty("Connection", "close");
                    conn.setFixedLengthStreamingMode(getUpload().getFile_size()-chunk.getOffset());
                    out = new ThrottledOutputStream(conn.getOutputStream(), getUpload().getMain_panel().getStream_supervisor());
                }
                
                tot_bytes_up=0;
                
                error = false;
                
                try{

                    if(!isExit() && !getUpload().isStopped()) {
                        
                        CipherInputStream cis = new CipherInputStream(chunk.getInputStream(), CryptTools.genCrypter("AES", "AES/CTR/NoPadding", getUpload().getByte_file_key(), CryptTools.forwardMEGALinkKeyIV(getUpload().getByte_file_iv(), chunk.getOffset())));

                        System.out.println(" Subiendo chunk "+chunk.getId()+" desde worker "+ getId() +"...");

                        while( !isExit() && !getUpload().isStopped() && (reads=cis.read(buffer))!=-1 && out != null )
                        {
                            out.write(buffer, 0, reads);

                            getUpload().getPartialProgress().add(reads);

                            getUpload().getProgress_meter().secureNotify();
                            
                            tot_bytes_up+=reads;
                            
                            if(getUpload().isPaused() && !getUpload().isStopped()) {

                                getUpload().pause_worker();

                                secureWait();
                            }
                        }
                        
                        if(!getUpload().isStopped()) {
                            
                            if(tot_bytes_up < chunk.getSize())
                            {
                                if(tot_bytes_up > 0) {

                                    getUpload().getPartialProgress().add(-1*tot_bytes_up);

                                    getUpload().getProgress_meter().secureNotify();
                                }

                                error = true;
                            }

                            if(error && !getUpload().isStopped()) {

                                getUpload().rejectChunkId(chunk.getId());
                            
                                conta_error++;

                                if(!isExit()) {
                                    
                                    setError_wait(true);

                                    Thread.sleep(getWaitTimeExpBackOff(conta_error)*1000);
                                    
                                    setError_wait(false);
                                }

                            } else if(!error) {
                                
                                System.out.println(" Worker "+getId()+" ha subido chunk "+chunk.getId());

                                getUpload().getMac_generator().getChunk_queue().put(chunk.getId(), chunk);

                                getUpload().getMac_generator().secureNotify();
                                
                                conta_error = 0;
                            }
                            
                        }
                        
                    } else if(isExit()) {
                        
                        getUpload().rejectChunkId(chunk.getId());
                    }
               }
               catch (IOException ex) 
               {      
                   error = true;
                   
                   getUpload().rejectChunkId(chunk.getId());
                   
                    if(tot_bytes_up > 0) {
                                        
                        getUpload().getPartialProgress().add(-1*tot_bytes_up);

                        getUpload().getProgress_meter().secureNotify();
                    }

                    getLogger(ChunkUploader.class.getName()).log(Level.SEVERE, null, ex);
                    
                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | InterruptedException ex) {
                    getLogger(ChunkUploader.class.getName()).log(Level.SEVERE, null, ex);
                    
                }
                
            if(!error && chunk.getOffset() + tot_bytes_up == getUpload().getFile_size() && conn != null) {
                
                http_status = conn.getResponseCode();
            
                if (http_status != HttpURLConnection.HTTP_OK )
                {   
                    throw new IOException("UPLOAD FAILED! (HTTP STATUS: "+ http_status+")");

                } else if(!(conn.getContentLengthLong() > 0 || conn.getContentLengthLong() == -1)) {
                    
                    throw new IOException("UPLOAD FAILED! (Empty completion handle!)");
                    
                } else {

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
                                throw new IOException("UPLOAD FAILED! (MEGA ERROR: "+ MegaAPI.checkMEGAError(response)+")");

                            } else {

                                System.out.println("Completion handle -> "+response);

                                getUpload().setCompletion_handle(response);
                            }
                       } 
                }
                
            }
        }
        
        }catch(ChunkInvalidIdException e) {
            
        } catch (IOException ex) {
            
            getUpload().emergencyStopUploader(ex.getMessage());
            
            getLogger(ChunkUploader.class.getName()).log(Level.SEVERE, null, ex);
            
        } finally {
            
            if(conn!=null) {
                
                conn.disconnect();
            }
        }
        
        getUpload().stopThisSlot(this);

        getUpload().getMac_generator().secureNotify();

        System.out.println("ChunkUploader "+getId()+" bye bye...");
    }
    
}
