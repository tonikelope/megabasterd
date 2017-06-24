package megabasterd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.swing.JOptionPane;
import static megabasterd.MiscTools.BASE642Bin;
import static megabasterd.MiscTools.Bin2BASE64;
import static megabasterd.MiscTools.Bin2UrlBASE64;
import static megabasterd.MiscTools.cleanFilePath;
import static megabasterd.MiscTools.cleanFilename;
import static megabasterd.MiscTools.findFirstRegex;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.jackson.map.ObjectMapper;

/**
 *
 * @author tonikelope
 */
public final class MegaCrypterAPI {

    public static final Set<String> PASS_CACHE = new HashSet<>();
    public static final Object PASS_LOCK = new Object();

    private static String _rawRequest(String request, URL url_api) throws IOException, MegaCrypterAPIException {

        String response = null;

        try (CloseableHttpClient httpclient = MiscTools.getApacheKissHttpClient()) {

            HttpPost httppost;

            try {
                httppost = new HttpPost(url_api.toURI());

                httppost.setHeader("Content-type", "application/json");

                httppost.setHeader("Custom-User-Agent", MainPanel.DEFAULT_USER_AGENT);

                httppost.setEntity(new StringEntity(request));

                try (CloseableHttpResponse httpresponse = httpclient.execute(httppost)) {

                    if (httpresponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                        System.out.println("Failed : HTTP error code : " + httpresponse.getStatusLine().getStatusCode());

                    } else {

                        InputStream is = httpresponse.getEntity().getContent();

                        try (ByteArrayOutputStream byte_res = new ByteArrayOutputStream()) {

                            byte[] buffer = new byte[16 * 1024];

                            int reads;

                            while ((reads = is.read(buffer)) != -1) {

                                byte_res.write(buffer, 0, reads);
                            }

                            response = new String(byte_res.toByteArray());

                            if (response.length() > 0) {

                                int mc_error;

                                if ((mc_error = MegaCrypterAPI.checkMCError(response)) != 0) {
                                    throw new MegaCrypterAPIException(String.valueOf(mc_error));

                                }
                            }
                        }
                    }
                }

            } catch (URISyntaxException ex) {
                Logger.getLogger(MegaCrypterAPI.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        return response;

    }

    public static String getMegaFileDownloadUrl(String link, String pass_hash, String noexpire_token, String sid) throws IOException, MegaCrypterAPIException {
        String request = "{\"m\":\"dl\", \"link\": \"" + link + "\"" + (noexpire_token != null ?", \"noexpire\": \"" + noexpire_token + "\"":"") + (sid!=null?", \"sid\": \""+sid+"\"":"") + "}";

        System.out.println(request);
        
        URL url_api = new URL(findFirstRegex("https?://[^/]+", link, 0) + "/api");

        String res = MegaCrypterAPI._rawRequest(request, url_api);

        ObjectMapper objectMapper = new ObjectMapper();

        HashMap res_map = objectMapper.readValue(res, HashMap.class);

        String dl_url = (String) res_map.get("url");

        if (pass_hash != null) {
            try {
                String pass = (String) res_map.get("pass");

                byte[] iv = BASE642Bin(pass);

                Cipher decrypter = CryptTools.genDecrypter("AES", "AES/CBC/PKCS5Padding", BASE642Bin(pass_hash), iv);

                byte[] decrypted_url = decrypter.doFinal(BASE642Bin(dl_url));

                dl_url = new String(decrypted_url);

            } catch (Exception ex) {
                throw new MegaCrypterAPIException("25");
            }
        }

        return dl_url;
    }

    public static String[] getMegaFileMetadata(String link, MainPanelView panel) throws MegaCrypterAPIException, MalformedURLException, IOException {
        String request = "{\"m\":\"info\", \"link\": \"" + link + "\"}";

        URL url_api = new URL(findFirstRegex("https?://[^/]+", link, 0) + "/api");

        String res = MegaCrypterAPI._rawRequest(request, url_api);

        ObjectMapper objectMapper = new ObjectMapper();

        HashMap res_map = objectMapper.readValue(res, HashMap.class);

        String fname = cleanFilename((String) res_map.get("name"));

        String fpath = null;

        Object fpath_val = res_map.get("path");

        if (fpath_val instanceof Boolean) {

            fpath = null;

        } else if (fpath_val instanceof String) {

            fpath = cleanFilePath((String) fpath_val);
        }

        String file_size;

        try {

            file_size = String.valueOf(res_map.get("size"));

        } catch (java.lang.ClassCastException ex) {

            file_size = String.valueOf(res_map.get("size"));
        }

        String fkey = (String) res_map.get("key");

        String noexpire_token = null;

        Object expire_val = res_map.get("expire");

        if (expire_val instanceof Boolean) {

            noexpire_token = null;

        } else if (expire_val instanceof String) {

            String aux[] = ((String) expire_val).split("#");

            noexpire_token = aux[1];
        }

        Object pass_val = res_map.get("pass");

        String pass = null;

        if (pass_val instanceof Boolean) {

            pass = null;

        } else if (pass_val instanceof String) {

            pass = (String) pass_val;
        }

        System.out.println("PASS: " + pass);

        System.out.println(noexpire_token);

        if (pass != null) {
            String[] pass_items = pass.split("#");

            if (pass_items.length != 4) {
                throw new MegaCrypterAPIException("25");
            }

            int iterations = Integer.parseInt(pass_items[0]);

            byte[] key_check = BASE642Bin(pass_items[1]);

            byte[] salt = BASE642Bin(pass_items[2]);

            byte[] iv = BASE642Bin(pass_items[3]);

            String password;

            byte[] info_key = null;

            boolean bad_pass;

            Cipher decrypter;

            synchronized (PASS_LOCK) {

                LinkedList<String> pass_list = new LinkedList(PASS_CACHE);

                do {
                    bad_pass = true;

                    if ((password = pass_list.poll()) == null) {

                        password = JOptionPane.showInputDialog(panel, "Enter password for MegaCrypter link:");
                    }

                    if (password != null) {

                        try {

                            info_key = CryptTools.PBKDF2HMACSHA256(password, salt, (int) Math.pow(2, iterations));

                            decrypter = CryptTools.genDecrypter("AES", "AES/CBC/PKCS5Padding", info_key, iv);

                            bad_pass = !Arrays.equals(info_key, decrypter.doFinal(key_check));

                            if (!bad_pass) {

                                PASS_CACHE.add(password);
                            }

                        } catch (Exception ex) {

                            bad_pass = true;
                        }
                    }

                } while (password != null && bad_pass);
            }

            if (bad_pass) {

                throw new MegaCrypterAPIException("25");

            } else {

                try {

                    decrypter = CryptTools.genDecrypter("AES", "AES/CBC/PKCS5Padding", info_key, iv);

                    byte[] decrypted_key = decrypter.doFinal(BASE642Bin(fkey));

                    fkey = Bin2UrlBASE64(decrypted_key);

                    decrypter = CryptTools.genDecrypter("AES", "AES/CBC/PKCS5Padding", info_key, iv);

                    byte[] decrypted_name = decrypter.doFinal(BASE642Bin(fname));

                    fname = new String(decrypted_name);

                    if (fpath != null) {
                        byte[] decrypted_fpath = decrypter.doFinal(BASE642Bin(fpath));

                        fpath = new String(decrypted_fpath);
                    }

                    pass = Bin2BASE64(info_key);

                } catch (Exception ex) {

                    throw new MegaCrypterAPIException("25");

                }
            }
        }

        if (fpath != null) {
            fname = fpath + fname;
        }

        String file_data[] = {fname, file_size, fkey, pass, noexpire_token};

        return file_data;
    }

    private static int checkMCError(String data) {
        String error = findFirstRegex("\"error\" *: *([0-9-]+)", data, 1);

        return error != null ? Integer.parseInt(error) : 0;
    }

    private MegaCrypterAPI() {
    }
}
