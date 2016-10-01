package megabasterd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import static java.util.logging.Logger.getLogger;
import java.util.zip.GZIPInputStream;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.swing.JOptionPane;
import static megabasterd.MiscTools.BASE642Bin;
import static megabasterd.MiscTools.Bin2BASE64;
import static megabasterd.MiscTools.Bin2UrlBASE64;
import static megabasterd.MiscTools.cleanFilePath;
import static megabasterd.MiscTools.cleanFilename;
import static megabasterd.MiscTools.findFirstRegex;
import org.codehaus.jackson.map.ObjectMapper;


/**
 *
 * @author tonikelope
 */
public final class MegaCrypterAPI {
    
    private static String _rawRequest(String request, URL url_api) throws IOException, MegaCrypterAPIException {
        
        HttpURLConnection conn = (HttpURLConnection) url_api.openConnection();
        conn.setConnectTimeout(MainPanel.CONNECTION_TIMEOUT);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", MainPanel.USER_AGENT);
        conn.setRequestProperty("Connection", "close");
        
        OutputStream out;
        out = conn.getOutputStream();
	out.write(request.getBytes());
        out.close();
        
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
        {    
            throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
        }
        
        String content_encoding = conn.getContentEncoding();
            
        InputStream is=(content_encoding!=null && content_encoding.equals("gzip"))?new GZIPInputStream(conn.getInputStream()):conn.getInputStream();

        ByteArrayOutputStream byte_res = new ByteArrayOutputStream();

        byte[] buffer = new byte[16*1024];

        int reads;

        while( (reads=is.read(buffer)) != -1 ) {

            byte_res.write(buffer, 0, reads);
        }

        String response = new String(byte_res.toByteArray());

        conn.disconnect();

        int mc_error;
        
        if((mc_error=MegaCrypterAPI.checkMCError(response))!=0)
        {
            throw new MegaCrypterAPIException(String.valueOf(mc_error));
        }
        
        return response;
        
    }
    
    public static String getMegaFileDownloadUrl(String link, String pass_hash, String noexpire_token) throws IOException, MegaCrypterAPIException
    {
        String request = noexpire_token != null?"{\"m\":\"dl\", \"link\": \""+link+"\", \"noexpire\": \""+noexpire_token+"\"}":"{\"m\":\"dl\", \"link\": \""+link+"\"}";
      
        URL url_api = new URL(findFirstRegex("https?://[^/]+", link, 0)+"/api");
        
        String res = MegaCrypterAPI._rawRequest(request, url_api);

        ObjectMapper objectMapper = new ObjectMapper();

        HashMap res_map = objectMapper.readValue(res, HashMap.class);

        String dl_url = (String)res_map.get("url");
        
        if(pass_hash != null)
        {
            try {
                String pass = (String)res_map.get("pass");
                
                byte[] iv = BASE642Bin(pass);
                
                Cipher decrypter = CryptTools.genDecrypter("AES", "AES/CBC/PKCS5Padding", BASE642Bin(pass_hash),iv);
                
                byte[] decrypted_url = decrypter.doFinal(BASE642Bin(dl_url));
                
                dl_url = new String(decrypted_url);
                
            } catch (Exception ex) {
                getLogger(MegaCrypterAPI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return dl_url;
    }
    
    public static String[] getMegaFileMetadata(String link, MainPanelView panel) throws Exception, MegaCrypterAPIException
    {
        String request = "{\"m\":\"info\", \"link\": \""+link+"\"}";
        
        URL url_api = new URL(findFirstRegex("https?://[^/]+", link, 0)+"/api");
        
        String res = MegaCrypterAPI._rawRequest(request, url_api);
        
        ObjectMapper objectMapper = new ObjectMapper();

        HashMap res_map = objectMapper.readValue(res, HashMap.class);

        String fname = cleanFilename((String)res_map.get("name"));
        
        String fpath=null;

        Object fpath_val = res_map.get("path");
        
        if(fpath_val instanceof Boolean) {
            
            fpath = null;
            
        } else if (fpath_val instanceof String) {
            
            fpath = cleanFilePath((String)fpath_val);
        }

        String file_size;
        
        try {
                
            file_size = String.valueOf(res_map.get("size"));
                
        } catch(java.lang.ClassCastException ex) {
                
            file_size = String.valueOf(res_map.get("size"));
        }

        String fkey = (String)res_map.get("key");
        
        String noexpire_token=null;
        
        Object expire_val = res_map.get("expire");
        
        if(expire_val instanceof Boolean) {
            
            noexpire_token = null;
            
        } else if (expire_val instanceof String) {
            
            String aux[] = ((String) expire_val).split("#");
            
            noexpire_token = aux[1];
        }
        
        String pass=null;
        
        Object pass_val = res_map.get("pass");
        
        if(pass_val instanceof Boolean) {
            
            pass = null;
            
        } else if (expire_val instanceof String) {
            
            pass = (String)pass_val;
        }
     
        System.out.println(noexpire_token);

        if(pass != null)
        {
            String[] pass_items = pass.split("#");
            
            if(pass_items.length != 4)
            {
                throw new MegaCrypterAPIException("Bad password data!");
            }

            int iterations = Integer.parseInt(pass_items[0]);
            
            byte[] key_check = BASE642Bin(pass_items[1]);
            
            byte[] salt = BASE642Bin(pass_items[2]);
            
            byte[] iv = BASE642Bin(pass_items[3]);
            
            String password;

            byte[] info_key = null;
            
            boolean bad_pass;
            
            Cipher decrypter;

            try {
                    do
                    {   
                        bad_pass = false;
                        
                        password = JOptionPane.showInputDialog(panel, "Enter password:");
                        
                        if(password!=null) {
                            
                            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                            
                            KeySpec ks = new PBEKeySpec(password.toCharArray(), salt, (int)Math.pow(2, iterations), 256);
                            
                            try {
                                
                                info_key=f.generateSecret(ks).getEncoded();
                                
                                decrypter = CryptTools.genDecrypter("AES", "AES/CBC/PKCS5Padding", info_key, iv);

                                try {

                                    bad_pass = !Arrays.equals(info_key, decrypter.doFinal(key_check));

                                } catch (IllegalBlockSizeException | BadPaddingException ex) {

                                    bad_pass=true;
                                }
                                
                            } catch (InvalidKeySpecException ex) {
                                
                                bad_pass=true;
                            }
                        }
 
                    }while(password!=null && bad_pass);

                    if(password==null)
                    {
                        return null;
                    }
                    else
                    {
                        decrypter = CryptTools.genDecrypter("AES", "AES/CBC/PKCS5Padding", info_key, iv);

                        byte[] decrypted_key = decrypter.doFinal(BASE642Bin(fkey));

                        fkey = Bin2UrlBASE64(decrypted_key);
                        
                        decrypter = CryptTools.genDecrypter("AES", "AES/CBC/PKCS5Padding", info_key, iv);
                        
                        byte[] decrypted_name = decrypter.doFinal(BASE642Bin(fname));

                        fname = new String(decrypted_name);
                        
                        if(fpath != null)
                        {
                            byte[] decrypted_fpath = decrypter.doFinal(BASE642Bin(fpath));

                            fpath = new String(decrypted_fpath);
                        }
                        
                        pass=Bin2BASE64(info_key);
                        
                    }

                    } catch (Exception ex) {
                        getLogger(MegaCrypterAPI.class.getName()).log(Level.SEVERE, null, ex);
                    }
        }
        
        if(fpath != null)
        {
            fname = fpath+fname;
        }

        String file_data[] = {fname, file_size, fkey, pass, noexpire_token};

        return file_data;
    }
    
    private static int checkMCError(String data)
    {
        String error = findFirstRegex("\"error\" *: *([0-9-]+)", data, 1);

        return error != null?Integer.parseInt(error):0;
    }

    private MegaCrypterAPI() {
    }
}
