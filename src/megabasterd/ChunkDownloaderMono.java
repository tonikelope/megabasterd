package megabasterd;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import static java.util.logging.Logger.getLogger;
import static megabasterd.MainPanel.THROTTLE_SLICE_SIZE;
import static megabasterd.MiscTools.getWaitTimeExpBackOff;


/**
 *
 * @author tonikelope
 */
public class ChunkDownloaderMono extends ChunkDownloader {

    public ChunkDownloaderMono(int id, Download download) {
        super(id, download);
    }
    
    @Override
    public void run()
    {
        String worker_url=null;
        Chunk chunk;
        int reads, max_reads, conta_error, http_status=200;
        byte[] buffer = new byte[THROTTLE_SLICE_SIZE];
        boolean error;

        System.out.println("Worker ["+getId()+"]: let's do some work!");

        HttpURLConnection conn=null;
        
        try
        {
            conta_error = 0;
            
            error = false;
            
            URL url = null;
            
            InputStream is=null;

            while(!isExit() && !getDownload().isStopped())
            {
                if(worker_url == null || error) {
                    
                    worker_url=getDownload().getDownloadUrlForWorker();
                }
                
                chunk = new Chunk(getDownload().nextChunkId(), getDownload().getFile_size(), worker_url);
                
                if(url == null || error) {
                    
                    url = new URL(worker_url+"/"+chunk.getOffset());
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(MainPanel.CONNECTION_TIMEOUT);
                    conn.setRequestProperty("User-Agent", MegaAPI.USER_AGENT);
                    conn.setRequestProperty("Connection", "close");
                    is = new ThrottledInputStream(conn.getInputStream(), getDownload().getMain_panel().getStream_supervisor());
                    http_status = conn.getResponseCode();
                }
                
                if(http_status != HttpURLConnection.HTTP_OK){
                    
                    System.out.println("Failed : HTTP error code : " + http_status);
                    
                    error  = true;
                    
                } else {
                    
                    error = false;
                
                    try{

                        if(!isExit() && !getDownload().isStopped()) {

                            while(!getDownload().isStopped() && !getDownload().getChunkwriter().isExit() && chunk.getOutputStream().size() < chunk.getSize() && (reads=is.read(buffer, 0, (max_reads=(int)(chunk.getSize() - chunk.getOutputStream().size())) <= buffer.length?max_reads:buffer.length))!=-1 )
                            {
                                chunk.getOutputStream().write(buffer, 0, reads);

                                getDownload().getPartialProgressQueue().add(reads);

                                getDownload().getProgress_meter().secureNotify();
                                
                                if(getDownload().isPaused() && !getDownload().isStopped()) {

                                    getDownload().pause_worker_mono();

                                    secureWait();
                                }
                            }
                            
                            if(chunk.getOutputStream().size() < chunk.getSize()) {

                                if(chunk.getOutputStream().size() > 0)
                                {
                                    getDownload().getPartialProgressQueue().add(-1*chunk.getOutputStream().size());

                                    getDownload().getProgress_meter().secureNotify();
                                }

                                error = true;
                            }
                            
                            if(error && !getDownload().isStopped()) {

                                getDownload().rejectChunkId(chunk.getId());
                                
                                conta_error++;
                                
                                if(!isExit()) {
                                    
                                    setError_wait(true);
                                
                                    getDownload().getView().updateSlotsStatus();

                                    Thread.sleep(getWaitTimeExpBackOff(conta_error)*1000);

                                    setError_wait(false);

                                    getDownload().getView().updateSlotsStatus();
                                }
                                
                            } else if(!error) {
                                
                                getDownload().getChunkwriter().getChunk_queue().put(chunk.getId(), chunk);

                                getDownload().getChunkwriter().secureNotify();
                                
                                conta_error = 0;
                            }
                            
                        } else if(isExit()) {
                        
                            getDownload().rejectChunkId(chunk.getId());
                        }
                   }
                   catch (IOException ex) 
                   {
                        error = true;
       
                        getDownload().rejectChunkId(chunk.getId());

                        if(chunk.getOutputStream().size() > 0)
                        {
                            getDownload().getPartialProgressQueue().add(-1*chunk.getOutputStream().size());

                            getDownload().getProgress_meter().secureNotify();
                        }
                        
                        getLogger(ChunkDownloaderMono.class.getName()).log(Level.SEVERE, null, ex);

                    } catch (InterruptedException ex) {
                        getLogger(ChunkDownloaderMono.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        
        }catch(ChunkInvalidIdException e) {
        
        }catch (MalformedURLException ex) {
            
            getLogger(ChunkDownloaderMono.class.getName()).log(Level.SEVERE, null, ex);
        
        } catch (IOException ex) {
            
            getLogger(ChunkDownloaderMono.class.getName()).log(Level.SEVERE, null, ex);
            
            getDownload().emergencyStopDownloader(ex.getMessage());
            
        } finally {
            
            if(conn!=null) {
                
                conn.disconnect();
            }
        }
        
        getDownload().stopThisSlot(this);
        
        getDownload().getChunkwriter().secureNotify();

        System.out.println("Worker ["+getId()+"]: bye bye");
        
    }
}
