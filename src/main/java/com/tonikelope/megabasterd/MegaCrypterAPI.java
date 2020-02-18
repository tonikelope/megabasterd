package com.tonikelope.megabasterd;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import static com.tonikelope.megabasterd.CryptTools.*;
import static com.tonikelope.megabasterd.MiscTools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
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

/**
 *
 * @author tonikelope
 */
public class MegaCrypterAPI {

    public static final Set<String> PASS_CACHE = new HashSet<>();
    public static final Object PASS_LOCK = new Object();
    private static final Logger LOG = Logger.getLogger(MegaCrypterAPI.class.getName());

    private static String _rawRequest(String request, URL url_api) throws MegaCrypterAPIException {

        String response = null;

        HttpURLConnection con = null;

        try {

            if (MainPanel.isUse_proxy()) {

                con = (HttpURLConnection) url_api.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(MainPanel.getProxy_host(), MainPanel.getProxy_port())));

                if (MainPanel.getProxy_user() != null && !"".equals(MainPanel.getProxy_user())) {

                    con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((MainPanel.getProxy_user() + ":" + MainPanel.getProxy_pass()).getBytes("UTF-8")));
                }
            } else {

                con = (HttpURLConnection) url_api.openConnection();
            }

            con.setRequestProperty("Content-type", "application/json");

            con.setUseCaches(false);

            con.setRequestProperty("User-Agent", MainPanel.DEFAULT_USER_AGENT);

            con.setRequestMethod("POST");

            con.setDoOutput(true);

            con.getOutputStream().write(request.getBytes("UTF-8"));

            con.getOutputStream().close();

            if (con.getResponseCode() != 200) {
                Logger.getLogger(MegaCrypterAPI.class.getName()).log(Level.INFO, "{0} Failed : HTTP error code : {1}", new Object[]{Thread.currentThread().getName(), con.getResponseCode()});

            } else {

                try (InputStream is = con.getInputStream(); ByteArrayOutputStream byte_res = new ByteArrayOutputStream()) {

                    byte[] buffer = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];

                    int reads;

                    while ((reads = is.read(buffer)) != -1) {

                        byte_res.write(buffer, 0, reads);
                    }

                    response = new String(byte_res.toByteArray(), "UTF-8");

                    if (response.length() > 0) {

                        int mc_error;

                        if ((mc_error = MegaCrypterAPI.checkMCError(response)) != 0) {
                            throw new MegaCrypterAPIException(mc_error);

                        }
                    }
                }
            }

        } catch (IOException ex) {
            Logger.getLogger(MegaCrypterAPI.class.getName()).log(Level.SEVERE, ex.getMessage());
        } finally {

            if (con != null) {
                con.disconnect();
            }
        }

        return response;

    }

    public static String getMegaFileDownloadUrl(String link, String pass_hash, String noexpire_token, String sid, String reverse) throws IOException, MegaCrypterAPIException {
        String request = "{\"m\":\"dl\", \"link\": \"" + link + "\"" + (noexpire_token != null ? ", \"noexpire\": \"" + noexpire_token + "\"" : "") + (sid != null ? ", \"sid\": \"" + sid + "\"" : "") + (reverse != null ? ", \"reverse\": \"" + reverse + "\"" : "") + "}";

        URL url_api = new URL(findFirstRegex("https?://[^/]+", link, 0) + "/api");

        String res = MegaCrypterAPI._rawRequest(request, url_api);

        ObjectMapper objectMapper = new ObjectMapper();

        HashMap res_map = objectMapper.readValue(res, HashMap.class);

        String dl_url = (String) res_map.get("url");

        if (pass_hash != null) {
            try {
                String pass = (String) res_map.get("pass");

                byte[] decrypted_url = aes_cbc_decrypt_pkcs7(BASE642Bin(dl_url), BASE642Bin(pass_hash), BASE642Bin(pass));

                dl_url = new String(decrypted_url, "UTF-8");

            } catch (Exception ex) {
                throw new MegaCrypterAPIException(25);
            }
        }

        if (dl_url == null || "".equals(dl_url)) {
            throw new MegaCrypterAPIException(-101);
        }

        return dl_url;
    }

    public static String[] getMegaFileMetadata(String link, MainPanelView panel, String reverse) throws MegaCrypterAPIException, MalformedURLException, IOException {
        String request = "{\"m\":\"info\", \"link\": \"" + link + "\"" + (reverse != null ? ", \"reverse\": \"" + reverse + "\"" : "") + "}";

        URL url_api = new URL(findFirstRegex("https?://[^/]+", link, 0) + "/api");

        String res = MegaCrypterAPI._rawRequest(request, url_api);

        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

        objectMapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);

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

        if (pass != null) {
            String[] pass_items = pass.split("#");

            if (pass_items.length != 4) {
                throw new MegaCrypterAPIException(25);
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

                            info_key = PBKDF2HMACSHA256(password, salt, (int) Math.pow(2, iterations), 256);

                            decrypter = genDecrypter("AES", "AES/CBC/PKCS5Padding", info_key, iv);

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

                throw new MegaCrypterAPIException(25);

            } else {

                try {

                    decrypter = genDecrypter("AES", "AES/CBC/PKCS5Padding", info_key, iv);

                    byte[] decrypted_key = decrypter.doFinal(BASE642Bin(fkey));

                    fkey = Bin2UrlBASE64(decrypted_key);

                    decrypter = genDecrypter("AES", "AES/CBC/PKCS5Padding", info_key, iv);

                    byte[] decrypted_name = decrypter.doFinal(BASE642Bin(fname));

                    fname = new String(decrypted_name, "UTF-8");

                    if (fpath != null) {
                        byte[] decrypted_fpath = decrypter.doFinal(BASE642Bin(fpath));

                        fpath = new String(decrypted_fpath, "UTF-8");
                    }

                    pass = Bin2BASE64(info_key);

                } catch (Exception ex) {

                    throw new MegaCrypterAPIException(25);

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
