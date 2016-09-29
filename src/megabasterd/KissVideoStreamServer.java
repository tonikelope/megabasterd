package megabasterd;

import com.sun.net.httpserver.HttpServer;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static megabasterd.MiscTools.findFirstRegex;
import static megabasterd.MiscTools.getWaitTimeExpBackOff;
import static megabasterd.MiscTools.swingReflectionInvoke;


public final class KissVideoStreamServer {
    
    public static final int TIMEOUT=30000;
    public static final int DEFAULT_PORT=1337;
    public static final int EXP_BACKOFF_BASE=2;
    public static final int EXP_BACKOFF_SECS_RETRY=1;
    public static final int EXP_BACKOFF_MAX_WAIT_TIME=128;
    private HttpServer _httpserver;
    private final MainPanelView _main_panel;
    private final ConcurrentHashMap<String, String[]> _link_cache;
    private final ConcurrentHashMap<Thread, Boolean> _working;
    private final ContentType _ctype;
    private KissVideoStreamServerHandler _http_handler;
    public KissVideoStreamServer(MainPanelView panel) {
        _main_panel = panel;
        _link_cache = new ConcurrentHashMap();
        _working = new ConcurrentHashMap();
        _ctype = new ContentType();
    }
    
    public KissVideoStreamServerHandler getHandler()
    {
        return _http_handler;
    }
    
    public MainPanelView getPanel()
    {
        return _main_panel;
    }
    
    public ContentType getCtype()
    {
        return _ctype;
    }
    
    public ConcurrentHashMap getStreaming()
    {
        return _working;
    }
    
    public boolean isWorking()
    {
        return !_working.isEmpty();
    }
   
    
    public void start(int port, String context) throws IOException
    {
        _httpserver = HttpServer.create(new InetSocketAddress(port), 0);
        printStatusOK("Kissvideostreamer on localhost:"+DEFAULT_PORT+" (Waiting for request...)");
        _httpserver.createContext(context, (_http_handler = new KissVideoStreamServerHandler(this, _main_panel)));
        _httpserver.setExecutor(Executors.newCachedThreadPool());
        _httpserver.start();
    }
    
    public void stop()
    {
        _httpserver.stop(0);
    }
    
    public void printStatusError(String message)
    {
        swingReflectionInvoke("setForeground", _main_panel.getKiss_server_status(), Color.red);
        swingReflectionInvoke("setText", _main_panel.getKiss_server_status(), message);
    }
    
    public void printStatusOK(String message)
    {
        swingReflectionInvoke("setForeground", _main_panel.getKiss_server_status(), new Color(0,128,0));
        swingReflectionInvoke("setText", _main_panel.getKiss_server_status(), message);
    }
    
    public String[] getFromLinkCache(String link)
    {
        return _link_cache.containsKey(link)?_link_cache.get(link):null;
    }
    
    public void updateLinkCache(String link, String[] info) {
        
        _link_cache.put(link, info);
    }
    
    public void removeFromLinkCache(String link) {
        _link_cache.remove(link);
    }
    
   public String[] getMegaFileMetadata(String link, MainPanelView panel) throws IOException, InterruptedException
   {
        
       
        String[] file_info=null;
        int retry=0, error_code=0;
        boolean error;

        do
        {
            error=false;

            try
            {
                
                    
                    if( findFirstRegex("://mega(\\.co)?\\.nz/", link, 0) != null)
                {
                    MegaAPI ma = new MegaAPI();
                    
                    file_info = ma.getMegaFileMetadata(link);
                }    
                else
                {
                    file_info = MegaCrypterAPI.getMegaFileMetadata(link, panel);    
                } 
                    
                
            }
            catch(MegaAPIException | MegaCrypterAPIException e)
            {
                error=true;

                error_code = Integer.parseInt(e.getMessage());

                switch(error_code)
                { 
                    case -2:
                        throw new IOException("Mega link is not valid!");
                        
                    case -14:
                        throw new IOException("Mega link is not valid!");
                        
                     case 22:
                        throw new IOException("MegaCrypter link is not valid!");

                    case 23:
                        throw new IOException("MegaCrypter link is blocked!");

                    case 24:
                        throw new IOException("MegaCrypter link has expired!");
                        
                    default:

                        for(long i=getWaitTimeExpBackOff(retry++); i>0; i--)
                        {
                            if(error_code == -18)
                            {
                                printStatusError("File temporarily unavailable! (Retrying in "+i+" secs...)");
                            }
                            else
                            {
                                printStatusError("Mega/MC APIException error "+e.getMessage()+" (Retrying in "+i+" secs...)");
                            }

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {}
                        }
                }
            } catch(Exception ex) {
                
            }
          

        }while(error);
        
        return file_info;
    }
        
   public String getMegaFileDownloadUrl(String link, String pass_hash, String noexpire_token) throws IOException, InterruptedException
   {
       
       
        String dl_url=null;
        int retry=0, error_code;
        boolean error;
                
        do
        {
            error=false;
            
            try
            {
                 

                     
                     if( findFirstRegex("://mega(\\.co)?\\.nz/", link, 0) != null)
                    {
                        MegaAPI ma = new MegaAPI();

                        dl_url = ma.getMegaFileDownloadUrl(link);
                    }    
                    else
                    {
                        dl_url = MegaCrypterAPI.getMegaFileDownloadUrl(link,pass_hash,noexpire_token); 
                    }
                 
                
            }
            catch(MegaAPIException e)
            {
                error=true;

                error_code = Integer.parseInt(e.getMessage());

                    for(long i=getWaitTimeExpBackOff(retry++); i>0; i--)
                    {
                        if(error_code == -18)
                        {
                            printStatusError("File temporarily unavailable! (Retrying in "+i+" secs...)");
                        }
                        else
                        {
                            printStatusError("MegaAPIException error "+e.getMessage()+" (Retrying in "+i+" secs...)");
                        }

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {}
                    }
            }
            catch(MegaCrypterAPIException e)
            {
                error=true;

                error_code = Integer.parseInt(e.getMessage());
                
                switch(error_code)
                { 
                    case 22:
                        throw new IOException("MegaCrypter link is not valid!");

                    case 23:
                        throw new IOException("MegaCrypter link is blocked!");

                    case 24:
                        throw new IOException("MegaCrypter link has expired!");
                        
                    default:
                        for(long i=getWaitTimeExpBackOff(retry++); i>0; i--)
                        {
                            if(error_code == -18)
                            {
                                printStatusError("File temporarily unavailable! (Retrying in "+i+" secs...)");
                            }
                            else
                            {
                                printStatusError("MegaCrypterAPIException error "+e.getMessage()+" (Retrying in "+i+" secs...)");
                            }

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {}
                        }
                }
            }

        }while(error);
        
        return dl_url;
    }
   
    public boolean checkDownloadUrl(String string_url)
    {
        try {
            URL url = new URL(string_url+"/0-0");
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(TIMEOUT);
            connection.setRequestProperty("User-Agent", MainPanel.USER_AGENT);
            connection.setRequestProperty("Connection", "close");
            try (InputStream is = connection.getInputStream()) {
                while(is.read()!=-1);
            }
             
            return true;
            
        }catch (Exception ex) {
            
            return false;
        }        
    }
    
    public long[] parseRangeHeader(String header)
    {
        Pattern pattern = Pattern.compile("bytes\\=([0-9]+)\\-([0-9]+)?");
        
        Matcher matcher = pattern.matcher(header);
        
        long[] ranges=new long[2];
        
        if(matcher.find())
        {
            ranges[0] = Long.valueOf(matcher.group(1));
        
            if(matcher.group(2)!=null) {
                ranges[1] = Long.valueOf(matcher.group(2));
            } else
            {
                ranges[1]=-1;
            }
        }

        return ranges;
    }
    
    public String cookRangeUrl(String url, long[] ranges, int sync_bytes)
    {
        return url+"/"+String.valueOf(ranges[0]-sync_bytes)+(ranges[1]>=0?"-"+String.valueOf(ranges[1]):"");
    }
    
    public void restoreMainWindow() {
        
        _main_panel.setExtendedState(javax.swing.JFrame.NORMAL);
        swingReflectionInvoke("setVisible", _main_panel, true);
    }

}

