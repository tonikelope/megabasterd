package megabasterd;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import static megabasterd.KissVideoStreamServer.DEFAULT_PORT;
import static megabasterd.MiscTools.findFirstRegex;


public final class KissVideoStreamServerHandler implements HttpHandler {
    
    private final KissVideoStreamServer _httpserver;
    
    private final MainPanelView _view;
    
    private String _file_name;
    
    private long _file_size;
    
    private String _file_key;
    
    private String _pass_hash;
    
    private String _noexpire_token;
        
        public KissVideoStreamServerHandler(KissVideoStreamServer server, MainPanelView view) {
           
            _httpserver = server;
            _view = view;
        }
    
        @Override
        public void handle(HttpExchange xchg) throws IOException {
           
            _httpserver.getStreaming().put(Thread.currentThread(), true);
            
            long clength;
            
            OutputStream os;
            
            CipherInputStream cis = null;
            
            String httpmethod = xchg.getRequestMethod();
            
            _httpserver.printStatusOK("Kissvideostreamer (Request received! Dispatching it...)");
                
            Headers reqheaders=xchg.getRequestHeaders();
                
            Headers resheaders = xchg.getResponseHeaders();
            
            String url_path = xchg.getRequestURI().getPath();
            
            String link = url_path.substring(url_path.indexOf("/video/")+7);
               
            if(link.indexOf("mega/") == 0)
            {
                link = link.replaceAll("mega/", "https://mega.co.nz/#");
            }
            else
            {
                String mc_host = findFirstRegex("^[^/]+/", link, 0);

                link = "http://" + mc_host + link;
            }
               
            _httpserver.printStatusOK("Kissvideostreamer (Retrieving file metadata...)");

            String[] cache_info, file_info=null;

            cache_info = _httpserver.getFromLinkCache(link);

            if(cache_info!=null) { 

                 file_info = new String[6];

                 System.arraycopy( cache_info, 0, file_info, 0, cache_info.length );
           
            } else {

                try {
                    
                    file_info = _httpserver.getMegaFileMetadata(link, _view);
                    
                } catch (InterruptedException ex) {
                    Logger.getLogger(KissVideoStreamServerHandler.class.getName()).log(Level.SEVERE, null, ex);
                }

                cache_info = new String[6];

                System.arraycopy( file_info, 0, cache_info, 0, file_info.length );

                cache_info[5]=null;
            }
               
                _file_name = file_info[0];

                _file_size = Long.parseLong(file_info[1]);

                _file_key = file_info[2];

                if(file_info.length >= 5)
                {
                    _pass_hash = file_info[3];

                    _noexpire_token = file_info[4];

                } else {
                    _pass_hash = null;

                    _noexpire_token = null;
                }
               
                String file_ext = _file_name.substring(_file_name.lastIndexOf('.')+1).toLowerCase();

                URLConnection urlConn;
                
            try{
                
                if(httpmethod.equals("HEAD")) {
                
                resheaders.add("Accept-Ranges", "bytes");
                
                resheaders.add("transferMode.dlna.org", "Streaming");
                
                resheaders.add("contentFeatures.dlna.org", "DLNA.ORG_OP=01;DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000");
                
                resheaders.add("Content-Type", _httpserver.getCtype().getMIME(file_ext));
                
                resheaders.add("Content-Length", String.valueOf(_file_size));
                
                resheaders.add("Connection", "close");
                
                xchg.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
                
            } else if(httpmethod.equals("GET")) {
                
                    resheaders.add("Accept-Ranges", "bytes");

                    resheaders.add("transferMode.dlna.org", "Streaming");

                    resheaders.add("contentFeatures.dlna.org", "DLNA.ORG_OP=01;DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000");

                    resheaders.add("Content-Type", _httpserver.getCtype().getMIME(file_ext));

                    resheaders.add("Connection", "close");
          
                    byte[] buffer = new byte[16*1024];
               
                    int reads;
               
                    _httpserver.printStatusOK("Kissvideostreamer (Retrieving mega temp url...)");

                    String temp_url;

                    if(cache_info[5]!=null) {

                        temp_url = cache_info[5];

                        if(!_httpserver.checkDownloadUrl(temp_url)) {

                            temp_url = _httpserver.getMegaFileDownloadUrl(link,_pass_hash,_noexpire_token);

                            cache_info[5] = temp_url;

                            _httpserver.updateLinkCache(link, cache_info);
                        }

                    } else {
                        temp_url = _httpserver.getMegaFileDownloadUrl(link,_pass_hash,_noexpire_token);

                        cache_info[5] = temp_url;

                        _httpserver.updateLinkCache(link, cache_info);
                    }
      
                    _httpserver.printStatusOK("Kissvideostreamer (Connecting...)");

                    long[] ranges=new long[2];

                    int sync_bytes=0;

                    String header_range=null;

                    InputStream is;

                    URL url;

                    if(reqheaders.containsKey("Range"))
                    {
                        header_range = "Range";

                    } else if(reqheaders.containsKey("range")) {

                        header_range = "range";
                    }
                   
                    if(header_range != null)
                    {
                        List<String> ranges_raw = reqheaders.get(header_range);

                        String range_header=ranges_raw.get(0);

                        ranges = _httpserver.parseRangeHeader(range_header);

                        sync_bytes = (int)ranges[0] % 16;

                        if(ranges[1]>=0 && ranges[1]>=ranges[0]) {

                            clength = ranges[1]-ranges[0]+1;

                        } else {

                            clength = _file_size - ranges[0];
                        }

                        resheaders.add("Content-Range", "bytes "+ranges[0]+"-"+(ranges[1]>=0?ranges[1]:(_file_size-1))+"/"+_file_size);

                        xchg.sendResponseHeaders(HttpURLConnection.HTTP_PARTIAL, clength);

                        url = new URL(_httpserver.cookRangeUrl(temp_url, ranges, sync_bytes));

                    } else {

                        xchg.sendResponseHeaders(HttpURLConnection.HTTP_OK, _file_size);

                        url = new URL(temp_url);
                    }
                     
                    urlConn = url.openConnection();
                    urlConn.setConnectTimeout(KissVideoStreamServer.TIMEOUT);
                    urlConn.setRequestProperty("User-Agent", MainPanel.USER_AGENT);
                    urlConn.setRequestProperty("Connection", "close");
                    is = urlConn.getInputStream();

                    byte[] iv = CryptTools.initMEGALinkKeyIV(_file_key);

                    try {

                       cis = new CipherInputStream(is, CryptTools.genDecrypter("AES", "AES/CTR/NoPadding", CryptTools.initMEGALinkKey(_file_key), (header_range!=null && (ranges[0]-sync_bytes)>0)?CryptTools.forwardMEGALinkKeyIV(iv, ranges[0]-sync_bytes):iv));

                    } catch (    NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException ex) {
                       Logger.getLogger(KissVideoStreamServer.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    os = xchg.getResponseBody();

                    _httpserver.printStatusOK("Kissvideostreamer (Streaming file "+_file_name+" ...)");

                    //Skip sync bytes
                    cis.skip(sync_bytes);
                    
                    while((reads=cis.read(buffer))!=-1) {
                        
                        os.write(buffer, 0, reads);
                    }

            }
        }
        catch(Exception ex)
        {
           
        }
        finally
        {         
            if(cis!=null) {
                cis.close();
            }
            
            xchg.close();
  
            _httpserver.printStatusOK("Kissvideostreamer on localhost:"+DEFAULT_PORT+" (Waiting for request...)");
            
            _httpserver.getStreaming().remove(Thread.currentThread());
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(KissVideoStreamServerHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            if(!_httpserver.isWorking()) {
                
                _httpserver.restoreMainWindow();
            }
        }
   }
}
