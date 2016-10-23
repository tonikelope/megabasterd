package megabasterd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 *
 * @author tonikelope
 * 
 * EXPERIMENTAL!!
 */
public class KissHttpURLConnection {
    
    private URL _url;
    private int _status_code;
    private final Map<String,List<String>> _response_headers;
    private final Map<String,List<String>> _request_headers;
    private Socket _socket;
    private final CookieManager _cookie_manager;
    private boolean _cookies_put;
    
    public KissHttpURLConnection(URL url) {
        
        _url = url;
        _response_headers = new HashMap<>();
        _request_headers = new HashMap<>();
        _cookie_manager = new CookieManager();
        _cookies_put=false;
        _socket = null;
        _status_code = -1;
    }

    public CookieManager getCookie_manager() {
        return _cookie_manager;
    }
    
    public InputStream getInputStream() throws IOException {
        
        try {
            
            if(_status_code == -1) {
                
                _parseStatusCode();
            }
            
            if(_response_headers.isEmpty()) {
                
                _parseResponseHeaders();
            }
            
            if(!_cookies_put) {
                
                _cookie_manager.put(_url.toURI(), _response_headers);
                _cookies_put = true;
            }
            
        } catch (URISyntaxException ex) {
            Logger.getLogger(KissHttpURLConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return _socket.getInputStream();
    }
    
    public OutputStream getOutputStream() throws IOException {
        
        return _socket.getOutputStream();
    }
   
    public Map<String,List<String>> getRequest_headers() {
        
        return _request_headers;
    }
    
    public Map<String,List<String>> getResponse_headers() {

        return _response_headers;
    }
    
    public String getRequestHeader(String key) {
        
        return getRequestHeader(key, 0);
    }
    
    public String getRequestHeader(String key, int pos) {
        
        return _request_headers.containsKey(_capitalizeHeaderKey(key))?_request_headers.get(_capitalizeHeaderKey(key)).get(pos):null;
    }
    
    public String getResponseHeader(String key) {
        
        return getResponseHeader(key, 0);
    }
    
    public String getResponseHeader(String key, int pos) {
        
        return _response_headers.containsKey(_capitalizeHeaderKey(key))?_response_headers.get(_capitalizeHeaderKey(key)).get(pos):null;
    }
    
    public void setRequestHeader(String key, String val) {
   
        key = _capitalizeHeaderKey(key);

        if(!_request_headers.containsKey(key)) {
            
            List list;
            
            _request_headers.put(key, list = new ArrayList<>());
            
            list.add(val);
            
        } else {
            
            _request_headers.get(key).add(val);
        }
    }

    public void removeRequestHeader(String key) {
        
        _request_headers.remove(_capitalizeHeaderKey(key));
        
    }
    
    public void removeRequestHeader(String key, String value) {
        
        key = _capitalizeHeaderKey(key);
        
        _request_headers.get(key).remove(value);
        
        if(_request_headers.get(key).isEmpty()) {
            
            _request_headers.remove(key);
        }
        
    }

    public URL getUrl() {
        return _url;
    }

    public void setUrl(URL url) {
        
        _url = url;       
        
        try {
            close();
        } catch (IOException ex) {
            Logger.getLogger(KissHttpURLConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int getStatus_code() {
        
        if(_status_code == -1) {
            
            try {
                _parseStatusCode();
            } catch (IOException ex) {
                Logger.getLogger(KissHttpURLConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return _status_code;
    }

    public void close() throws IOException {
        
        if(_socket != null && !_socket.isClosed()) {
            
            _socket.close();
        }
        
        _socket = null;
    }

    private void _parseStatusCode() throws IOException {
        
        int b;
        
        ByteArrayOutputStream byte_res = new ByteArrayOutputStream();
        
        do{
          
            b = _socket.getInputStream().read();
            
            if(b != -1) {
                byte_res.write(b);
            }
            
        }while(b!=-1 && b != 0x0A);
        
        String temp = new String(byte_res.toByteArray()).trim();
      
        Pattern header_pattern = Pattern.compile("HTTP*/*[0-9]+\\.[0-9]+ +([0-9]+)");
        
        Matcher matcher = header_pattern.matcher(temp);
        
        if(matcher.find()) {
            
            _status_code = Integer.parseInt(matcher.group(1));
            
        } else {
            
            throw new IOException("BAD HTTP STATUS CODE!");
        }
        
    }
    
    private void _parseResponseHeaders() {
        
        try {
            
            String temp;
            
            Pattern header_pattern = Pattern.compile("([^:]+):(.*)");
        
            do {
                
                int b;
        
                ByteArrayOutputStream byte_res = new ByteArrayOutputStream();

                do{

                    b = _socket.getInputStream().read();

                    byte_res.write(b);

                }while(b != -1 && b != 0x0A);
                
                temp = new String(byte_res.toByteArray()).trim();
                
                if(temp.length() > 0) {
                    
                    Matcher matcher = header_pattern.matcher(temp);
                
                    if(matcher.find()) {
                        
                        String key = _capitalizeHeaderKey(matcher.group(1).trim());

                        if(!_response_headers.containsKey(key)) {

                            List list;

                            _response_headers.put(key, (list = new ArrayList<>()));

                            list.add(matcher.group(2).trim());

                        } else {

                            List list = _response_headers.get(key);

                            list.add(matcher.group(2).trim());
                        }
                    }
                }

            }while(temp.length() > 0);
            
        } catch (IOException ex) {
            Logger.getLogger(KissHttpURLConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public void doGET() {
        
        try {
            _initSocket();
            
            _status_code = -1;

            if(!_response_headers.isEmpty() && !_cookies_put) {
                
                _cookie_manager.put(_url.toURI(), _response_headers);
                _cookies_put = true;
            }
            
            _response_headers.clear();

            ByteArrayOutputStream byte_req = new ByteArrayOutputStream();
            
            byte_req.write(("GET " + (_url.getFile().isEmpty()?"/":_url.getFile()) + " HTTP/1.1\r\n").getBytes());
            
            byte_req.write(("Host: "+_url.getHost()+"\r\n").getBytes());
            
            byte_req.write(("Connection: close\r\n").getBytes());
            
            for(Map.Entry pair : _request_headers.entrySet()) {
                
                for(Object s:(List)pair.getValue()) {
                    
                    byte_req.write((pair.getKey()+":"+(String)s+"\r\n").getBytes());
                }
            }
            
            for(List list : _cookie_manager.get(_url.toURI(), _request_headers).values()) {
                
                for(Object s:list) {
                    
                    byte_req.write(("Cookie: "+(String)s + "\r\n").getBytes());
                }
            }
            
            byte_req.write("\r\n".getBytes());
            
            _socket.getOutputStream().write(byte_req.toByteArray());

            _cookie_manager.put(_url.toURI(), _response_headers);
            
            _cookies_put = true;

        } catch (IOException | URISyntaxException ex) {
            
            Logger.getLogger(KissHttpURLConnection.class.getName()).log(Level.SEVERE, null, ex);
            
        }
    }
    
    
    public void doPOST(long length) {
        
        try {
           
            _initSocket();
            
            _status_code = -1;

            if(!_response_headers.isEmpty() && !_cookies_put) {
                
                _cookie_manager.put(_url.toURI(),_response_headers);
                
                _cookies_put = true;
            }
            
            _response_headers.clear();
            
            ByteArrayOutputStream byte_req = new ByteArrayOutputStream();
            
            byte_req.write(("POST " + (_url.getFile().isEmpty()?"/":_url.getFile()) + " HTTP/1.1\r\n").getBytes());
            
            byte_req.write(("Host: "+_url.getHost()+"\r\n").getBytes());
            
            byte_req.write(("Connection: close\r\n").getBytes());
            
            byte_req.write(("Content-Length: " + length + "\r\n").getBytes());

            for(Map.Entry pair : _request_headers.entrySet()) {
                
                for(Object s:(List)pair.getValue()) {
                    
                    byte_req.write((pair.getKey()+":"+(String)s+"\r\n").getBytes());
                }
            }
            
            for(List list : _cookie_manager.get(_url.toURI(), _request_headers).values()) {
                
                for(Object s:list) {
                    
                    byte_req.write(("Cookie: "+(String)s + "\r\n").getBytes());
                }
            }
           
            byte_req.write("\r\n".getBytes());
           
            _socket.getOutputStream().write(byte_req.toByteArray());
            
            _cookies_put = false;

        } catch (IOException | URISyntaxException ex) {
            
            Logger.getLogger(KissHttpURLConnection.class.getName()).log(Level.SEVERE, null, ex);
            
        }
    }
    
    private void _initSocket() {
        
        if(_socket == null){
            
            if(_url.getProtocol().equals("http")) {
                
                try {
                    
                    _socket = new Socket(_url.getHost(), _url.getPort()!=-1?_url.getPort():_url.getDefaultPort());
 
                } catch (UnknownHostException ex) {
                    Logger.getLogger(KissHttpURLConnection.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(KissHttpURLConnection.class.getName()).log(Level.SEVERE, null, ex);
                }
                
            } else if(_url.getProtocol().equals("https")) {

                try {

                    SSLSocketFactory factory = (SSLSocketFactory)SSLSocketFactory.getDefault();

                    _socket = (SSLSocket)factory.createSocket(_url.getHost(), _url.getPort()!=-1?_url.getPort():_url.getDefaultPort());
                    
                    ((SSLSocket)_socket).setUseClientMode(true);
                    
                    ((SSLSocket)_socket).startHandshake();
                    
                } catch (IOException ex) {
                    Logger.getLogger(KissHttpURLConnection.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    private String _capitalizeHeaderKey(String value) {
  
        StringBuilder builder = new StringBuilder(value);
        
        for(int i=0; i<builder.length(); i++) {
            
            if(i == 0 || builder.charAt(i-1) == '-') {
                
                builder.setCharAt(i, Character.toUpperCase(builder.charAt(i)));
            }
        }
        
        return builder.toString();
    }
    
}
