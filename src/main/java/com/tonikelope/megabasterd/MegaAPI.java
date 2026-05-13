/*
 __  __                  _               _               _ 
|  \/  | ___  __ _  __ _| |__   __ _ ___| |_ ___ _ __ __| |
| |\/| |/ _ \/ _` |/ _` | '_ \ / _` / __| __/ _ \ '__/ _` |
| |  | |  __/ (_| | (_| | |_) | (_| \__ \ ||  __/ | | (_| |
|_|  |_|\___|\__, |\__,_|_.__/ \__,_|___/\__\___|_|  \__,_|
             |___/                                         
© Perpetrated by tonikelope since 2016
 */
package com.tonikelope.megabasterd;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import static com.tonikelope.megabasterd.CryptTools.*;
import static com.tonikelope.megabasterd.MiscTools.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

/**
 *
 * @author tonikelope
 */
public class MegaAPI implements Serializable {

    private static final class ResolvedNodeKey {

        private final String _decoded_key;
        private final HashMap _attributes;

        private ResolvedNodeKey(String decoded_key, HashMap attributes) {
            _decoded_key = decoded_key;
            _attributes = attributes;
        }
    }

    public static final String API_URL = "https://g.api.mega.co.nz";
    public static String API_KEY = null;
    public static final int REQ_ID_LENGTH = 10;
    public static final Integer[] MEGA_ERROR_NO_EXCEPTION_CODES = {-1, -3};
    public static final int PBKDF2_ITERATIONS = 100000;
    public static final int PBKDF2_OUTPUT_BIT_LENGTH = 256;
    public static final int MAX_RAW_REQUEST_RETRIES = 30;
    private static final Logger LOG = Logger.getLogger(MegaAPI.class.getName());

    public static int checkMEGAError(String data) {
        String error = findFirstRegex("^\\[?(\\-[0-9]+)\\]?$", data, 1);

        return error != null ? Integer.parseInt(error) : 0;
    }

    private long _seqno;

    private volatile String _sid;

    private int[] _master_key;

    private BigInteger[] _rsa_priv_key;

    private int[] _password_aes;

    private String _user_hash;

    private String _root_id;

    private String _inbox_id;

    private String _email;

    private String _full_email;

    private String _trashbin_id;

    private String _req_id;

    private int _account_version;

    private String _salt;

    public MegaAPI() {
        _req_id = null;
        _trashbin_id = null;
        _full_email = null;
        _email = null;
        _inbox_id = null;
        _root_id = null;
        _user_hash = null;
        _password_aes = null;
        _rsa_priv_key = null;
        _master_key = null;
        _salt = null;
        _sid = null;
        _account_version = -1;
        _req_id = genID(REQ_ID_LENGTH);

        _seqno = new java.security.SecureRandom().nextLong() & 0xffffffffL;
    }

    private synchronized String _nextSeqno() {
        return String.valueOf(_seqno++);
    }


    private static String _redactUrl(String url) {
        if (url == null) {
            return null;
        }
        return url.replaceAll("(?i)([?&](sid|sek)=)[^&]+", "$1[REDACTED]");
    }

    private static String _redactSensitive(String body) {
        if (body == null) {
            return null;
        }
        return body
                .replaceAll("(?i)\"uh\"\\s*:\\s*\"[^\"]+\"", "\"uh\":\"[REDACTED]\"")
                .replaceAll("(?i)\"mfa\"\\s*:\\s*\"[^\"]+\"", "\"mfa\":\"[REDACTED]\"");
    }

    public int getAccount_version() {
        return _account_version;
    }

    public String getFull_email() {
        return _full_email;
    }

    public String getEmail() {
        return _email;
    }

    public int[] getPassword_aes() {
        return _password_aes;
    }

    public String getUser_hash() {
        return _user_hash;
    }

    public String getSid() {
        return _sid;
    }

    public int[] getMaster_key() {
        return _master_key;
    }

    public BigInteger[] getRsa_priv_key() {
        return _rsa_priv_key;
    }

    public String getRoot_id() {
        return _root_id;
    }

    public String getInbox_id() {
        return _inbox_id;
    }

    public String getTrashbin_id() {
        return _trashbin_id;
    }

    private void _realLogin(String pincode) throws Exception {

        String request;

        if (pincode != null) {
            request = "[{\"a\":\"us\", \"mfa\":\"" + pincode + "\", \"user\":\"" + _email + "\",\"uh\":\"" + _user_hash + "\"}]";
        } else {
            request = "[{\"a\":\"us\",\"user\":\"" + _email + "\",\"uh\":\"" + _user_hash + "\"}]";
        }

        URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno());

        String res = RAW_REQUEST(request, url_api);

        ObjectMapper objectMapper = new ObjectMapper();

        HashMap[] res_map = objectMapper.readValue(res, HashMap[].class);

        String k = (String) res_map[0].get("k");

        String privk = (String) res_map[0].get("privk");

        _master_key = bin2i32a(decryptKey(UrlBASE642Bin(k), i32a2bin(_password_aes)));

        String csid = (String) res_map[0].get("csid");

        if (csid != null) {

            int[] enc_rsa_priv_key = bin2i32a(UrlBASE642Bin(privk));

            byte[] privk_byte = decryptKey(i32a2bin(enc_rsa_priv_key), i32a2bin(_master_key));

            _rsa_priv_key = _extractRSAPrivKey(privk_byte);

            byte[] raw_sid = rsaDecrypt(mpi2big(UrlBASE642Bin(csid)), _rsa_priv_key[0], _rsa_priv_key[1], _rsa_priv_key[2]);

            _sid = Bin2UrlBASE64(Arrays.copyOfRange(raw_sid, 0, 43));
        }

        fetchNodes();
    }

    private void _readAccountVersionAndSalt() throws Exception {

        String request = "[{\"a\":\"us0\",\"user\":\"" + _email + "\"}]";

        URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno());

        String res = RAW_REQUEST(request, url_api);

        ObjectMapper objectMapper = new ObjectMapper();

        HashMap[] res_map = objectMapper.readValue(res, HashMap[].class);

        _account_version = (Integer) res_map[0].get("v");

        _salt = (String) res_map[0].get("s");

    }

    public boolean check2FA(String email) throws Exception {

        String request = "[{\"a\":\"mfag\",\"e\":\"" + email + "\"}]";

        URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno());

        String res = RAW_REQUEST(request, url_api);

        ObjectMapper objectMapper = new ObjectMapper();

        Integer[] res_map = objectMapper.readValue(res, Integer[].class);

        return (res_map[0] == 1);

    }

    public void login(String email, String password, String pincode) throws Exception {

        _full_email = email;

        String[] email_split = email.split(" *# *");

        _email = email_split[0];

        if (_account_version == -1) {
            _readAccountVersionAndSalt();
        }

        if (_account_version == 1) {

            _password_aes = MEGAPrepareMasterKey(bin2i32a(password.getBytes("UTF-8")));

            _user_hash = MEGAUserHash(_email.toLowerCase().getBytes("UTF-8"), _password_aes);

        } else {

            byte[] pbkdf2_key = CryptTools.PBKDF2HMACSHA512(password, MiscTools.UrlBASE642Bin(_salt), PBKDF2_ITERATIONS, PBKDF2_OUTPUT_BIT_LENGTH);

            _password_aes = bin2i32a(Arrays.copyOfRange(pbkdf2_key, 0, 16));

            _user_hash = MiscTools.Bin2UrlBASE64(Arrays.copyOfRange(pbkdf2_key, 16, 32));
        }

        _realLogin(pincode);
    }

    public void fastLogin(String email, int[] password_aes, String user_hash, String pincode) throws Exception {

        _full_email = email;

        String[] email_split = email.split(" *# *");

        _email = email_split[0];

        if (_account_version == -1) {
            _readAccountVersionAndSalt();
        }

        _password_aes = password_aes;

        _user_hash = user_hash;

        _realLogin(pincode);
    }

    public Long[] getQuota() {

        Long[] quota = null;

        try {
            String request = "[{\"a\": \"uq\", \"xfer\": 1, \"strg\": 1}]";

            URL url_api;

            url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : ""));

            String res = RAW_REQUEST(request, url_api);

            ObjectMapper objectMapper = new ObjectMapper();

            HashMap[] res_map = objectMapper.readValue(res, HashMap[].class);

            quota = new Long[2];

            if (res_map[0].get("cstrg") instanceof Integer) {

                quota[0] = ((Number) res_map[0].get("cstrg")).longValue();

            } else if (res_map[0].get("cstrg") instanceof Long) {

                quota[0] = (Long) res_map[0].get("cstrg");
            }

            if (res_map[0].get("mstrg") instanceof Integer) {

                quota[1] = ((Number) res_map[0].get("mstrg")).longValue();

            } else if (res_map[0].get("mstrg") instanceof Long) {

                quota[1] = (Long) res_map[0].get("mstrg");
            }

        } catch (Exception ex) {

            LOG.log(Level.SEVERE, ex.getMessage());
        }

        return quota;
    }

    public void fetchNodes() throws IOException {

        String request = "[{\"a\":\"f\", \"c\":1}]";

        URL url_api;

        try {

            url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : ""));

            String res = RAW_REQUEST(request, url_api);

            ObjectMapper objectMapper = new ObjectMapper();

            HashMap[] res_map = objectMapper.readValue(res, HashMap[].class);

            for (Object o : (Iterable<? extends Object>) res_map[0].get("f")) {

                HashMap element = (HashMap<String, Object>) o;

                int file_type = (int) element.get("t");

                switch (file_type) {

                    case 2:
                        _root_id = (String) element.get("h");
                        break;
                    case 3:
                        _inbox_id = (String) element.get("h");
                        break;
                    case 4:
                        _trashbin_id = (String) element.get("h");
                        break;
                    default:
                        break;
                }
            }

        } catch (IOException | MegaAPIException ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        }

    }

    private String RAW_REQUEST(String request, URL url_api) throws MegaAPIException {

        String response = null, current_smart_proxy = null;

        int mega_error = 0, http_error = 0, conta_error = 0, http_status;

        boolean empty_response = false, smart_proxy_socks = false;

        HttpsURLConnection con = null;

        ArrayList<String> excluded_proxy_list = new ArrayList<>();

        do {

            SmartMegaProxyManager proxy_manager = MainPanel.getProxy_manager();

            try {

                if ((current_smart_proxy != null || http_error == 509) && MainPanel.isUse_smart_proxy() && proxy_manager != null && !MainPanel.isUse_proxy()) {

                    if (current_smart_proxy != null && (http_error != 0 || empty_response)) {

                        proxy_manager.blockProxy(current_smart_proxy, "HTTP " + String.valueOf(http_error));

                        String[] smart_proxy = proxy_manager.getProxy(excluded_proxy_list);

                        current_smart_proxy = smart_proxy[0];

                        smart_proxy_socks = smart_proxy[1].equals("socks");

                    } else if (current_smart_proxy == null) {

                        String[] smart_proxy = proxy_manager.getProxy(excluded_proxy_list);

                        current_smart_proxy = smart_proxy[0];

                        smart_proxy_socks = smart_proxy[1].equals("socks");
                    }

                    if (current_smart_proxy != null) {

                        String[] proxy_info = current_smart_proxy.split(":");

                        Proxy proxy = new Proxy(smart_proxy_socks ? Proxy.Type.SOCKS : Proxy.Type.HTTP, new InetSocketAddress(proxy_info[0], Integer.parseInt(proxy_info[1])));

                        con = (HttpsURLConnection) url_api.openConnection(proxy);

                    } else {

                        con = (HttpsURLConnection) url_api.openConnection();
                    }

                } else {

                    if (MainPanel.isUse_proxy()) {

                        con = (HttpsURLConnection) url_api.openConnection(new Proxy(smart_proxy_socks ? Proxy.Type.SOCKS : Proxy.Type.HTTP, new InetSocketAddress(MainPanel.getProxy_host(), MainPanel.getProxy_port())));

                        if (MainPanel.getProxy_user() != null && !"".equals(MainPanel.getProxy_user())) {

                            con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((MainPanel.getProxy_user() + ":" + MainPanel.getProxy_pass()).getBytes("UTF-8")));
                        }
                    } else {

                        con = (HttpsURLConnection) url_api.openConnection();
                    }

                }

                http_error = 0;

                mega_error = 0;

                empty_response = false;

                con.setRequestProperty("Content-type", "text/plain;charset=UTF-8");

                con.setRequestProperty("Accept-Encoding", "gzip");

                con.setRequestProperty("User-Agent", MainPanel.DEFAULT_USER_AGENT);

                con.setUseCaches(false);

                con.setRequestMethod("POST");

                con.setDoOutput(true);

                con.getOutputStream().write(request.getBytes("UTF-8"));

                con.getOutputStream().close();

                http_status = con.getResponseCode();

                if (http_status != 200) {

                    LOG.log(Level.WARNING, "{0} {1} {2}", new Object[]{Thread.currentThread().getName(), _redactSensitive(request), _redactUrl(url_api.toString())});

                    LOG.log(Level.WARNING, "{0} Failed : HTTP error code : {1}", new Object[]{Thread.currentThread().getName(), http_status});

                    http_error = http_status;

                    MiscTools.drainAndCloseErrorStream(con);

                } else {

                    try (InputStream is = "gzip".equals(con.getContentEncoding()) ? new GZIPInputStream(con.getInputStream()) : con.getInputStream(); ByteArrayOutputStream byte_res = new ByteArrayOutputStream()) {

                        byte[] buffer = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];

                        int reads;

                        while ((reads = is.read(buffer)) != -1) {

                            byte_res.write(buffer, 0, reads);
                        }

                        response = new String(byte_res.toByteArray(), "UTF-8");

                        if (response.length() > 0) {

                            mega_error = checkMEGAError(response);

                            if (mega_error != 0 && !Arrays.asList(MEGA_ERROR_NO_EXCEPTION_CODES).contains(mega_error)) {

                                throw new MegaAPIException(mega_error);

                            }

                        } else {

                            empty_response = true;
                        }
                    }

                }

            } catch (SSLException ssl_ex) {

                empty_response = true;

                Logger.getLogger(MegaAPI.class.getName()).log(Level.SEVERE, ssl_ex.getMessage());

            } catch (IOException ex) {

                Logger.getLogger(MegaAPI.class.getName()).log(Level.SEVERE, ex.getMessage());

            } finally {

                // Do not call con.disconnect() on the happy path -- it
                // forcibly closes the TCP/TLS socket and defeats keepalive.
                // Only disconnect on errors so the pool can reuse the
                // connection for the next request.
                if (con != null && (http_error != 0 || empty_response || mega_error != 0)) {
                    con.disconnect();
                }

            }

            if ((empty_response || mega_error != 0 || http_error != 0) && http_error != 509) {

                LOG.log(Level.WARNING, "{0} MegaAPI ERROR {1} Waiting for retry...", new Object[]{Thread.currentThread().getName(), String.valueOf(mega_error)});

                try {
                    Thread.sleep(getWaitTimeExpBackOff(conta_error++) * 1000);
                } catch (InterruptedException ex) {
                    LOG.log(Level.SEVERE, ex.getMessage());
                }

            }

        } while ((http_error == 500 || empty_response || mega_error != 0 || (http_error == 509 && MainPanel.isUse_smart_proxy() && !MainPanel.isUse_proxy()))
                && conta_error < MAX_RAW_REQUEST_RETRIES);

        if (response == null && (http_error != 0 || empty_response || mega_error != 0)) {
            LOG.log(Level.WARNING, "{0} RAW_REQUEST giving up after {1} retries (http_error={2} mega_error={3})", new Object[]{Thread.currentThread().getName(), conta_error, http_error, mega_error});
        }

        return response;

    }

    public String getMegaFileDownloadUrl(String link) throws MegaAPIException, MalformedURLException, IOException {

        String file_id = findFirstRegex("#.*?!([^!]+)", link, 1);

        String request;

        URL url_api;

        if (findFirstRegex("#N!", link, 0) != null) {
            String folder_id = findFirstRegex("###n=(.+)$", link, 1);

            request = "[{\"a\":\"g\", \"g\":\"1\", \"n\":\"" + file_id + "\"}]";

            url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : "") + "&n=" + folder_id);

        } else {

            request = "[{\"a\":\"g\", \"g\":\"1\", \"p\":\"" + file_id + "\"}]";
            url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : ""));
        }

        String data = RAW_REQUEST(request, url_api);

        ObjectMapper objectMapper = new ObjectMapper();

        HashMap[] res_map = objectMapper.readValue(data, HashMap[].class);

        String download_url = (String) res_map[0].get("g");

        if (download_url == null || "".equals(download_url)) {
            throw new MegaAPIException(-101);
        }

        return download_url;
    }

    public String[] getMegaFileMetadata(String link) throws MegaAPIException, MalformedURLException, IOException {

        String file_id = findFirstRegex("#.*?!([^!]+)", link, 1);

        String file_key = findFirstRegex("#.*?![^!]+!([^!#]+)", link, 1);

        String request;

        URL url_api;

        if (findFirstRegex("#N!", link, 0) != null) {
            String folder_id = findFirstRegex("###n=(.+)$", link, 1);

            request = "[{\"a\":\"g\", \"g\":\"1\", \"n\":\"" + file_id + "\"}]";

            url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : "") + "&n=" + folder_id);

        } else {

            request = "[{\"a\":\"g\", \"p\":\"" + file_id + "\"}]";

            url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : ""));
        }

        String data = RAW_REQUEST(request, url_api);

        ObjectMapper objectMapper = new ObjectMapper();

        HashMap[] res_map = objectMapper.readValue(data, HashMap[].class);

        String fsize = String.valueOf(res_map[0].get("s"));

        String at = (String) res_map[0].get("at");

        String[] file_data = null;

        HashMap att_map = _decAttr(at, initMEGALinkKey(file_key));

        if (att_map != null) {

            String fname = cleanFilename((String) att_map.get("n"));

            file_data = new String[]{fname, fsize, file_key};

        } else {

            throw new MegaAPIException(-14);
        }

        return file_data;
    }

    private byte[] _encThumbAttr(byte[] attr_byte, byte[] key) {

        try {

            return aes_cbc_encrypt_pkcs7(attr_byte, key, AES_ZERO_IV);

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        }

        return null;
    }

    private byte[] _encAttr(String attr, byte[] key) {

        byte[] ret = null;

        try {

            byte[] attr_byte = ("MEGA" + attr).getBytes("UTF-8");

            int l = (int) (16 * Math.ceil((double) attr_byte.length / 16));

            byte[] new_attr_byte = Arrays.copyOfRange(attr_byte, 0, l);

            ret = aes_cbc_encrypt_nopadding(new_attr_byte, key, AES_ZERO_IV);

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        }

        return ret;
    }

    private HashMap _decAttr(String encAttr, byte[] key) {
        HashMap res_map = null;
        byte[] decrypted_at;
        try {
            decrypted_at = aes_cbc_decrypt_nopadding(UrlBASE642Bin(encAttr), key, AES_ZERO_IV);
            String att = new String(decrypted_at, "UTF-8").replaceAll("\0+$", "").replaceAll("^MEGA", "");
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            objectMapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
            res_map = objectMapper.readValue(att, HashMap.class);
        } catch (Exception ex) {
            // Silenciamos el log porque en el bucle Yellowstone es normal que falle
            LOG.log(Level.FINE, "Decryption trial failed: {0}", ex.getMessage());
        }
        return res_map;
    }

    private ArrayList<String> _extractNodeKeyCandidates(String raw_node_key) {

        ArrayList<String> candidates = new ArrayList<>();

        if (raw_node_key != null) {

            for (String entry : raw_node_key.split("/")) {

                String[] parts = entry.split(":", 2);

                if (parts.length == 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
                    candidates.add(parts[1]);
                }
            }
        }

        return candidates;
    }

    private String _extractNodeKeyForHandle(String raw_node_key, String handle) {

        if (raw_node_key != null && handle != null) {

            for (String entry : raw_node_key.split("/")) {

                String[] parts = entry.split(":", 2);

                if (parts.length == 2 && handle.equals(parts[0]) && !parts[1].isEmpty()) {
                    return parts[1];
                }
            }
        }

        return null;
    }

    private ResolvedNodeKey _tryResolveNodeKey(String enc_node_key, byte[] folder_key_bytes, String attr) {

        try {

            String dec_node_k = Bin2UrlBASE64(decryptKey(UrlBASE642Bin(enc_node_key), folder_key_bytes));

            HashMap at = _decAttr(attr, _urlBase64KeyDecode(dec_node_k));

            if (at != null && at.get("n") instanceof String) {
                return new ResolvedNodeKey(dec_node_k, at);
            }

        } catch (Exception ex) {
            LOG.log(Level.FINE, "Skipping invalid MEGA node key candidate", ex);
        }

        return null;
    }

    private ResolvedNodeKey _resolveNodeKey(String raw_node_key, String root_handle, String folder_key, String attr) {

        if (attr == null) {
            return null;
        }

        try {

            byte[] folder_key_bytes = _urlBase64KeyDecode(folder_key);

            String selected_node_key = _extractNodeKeyForHandle(raw_node_key, root_handle);

            if (selected_node_key != null) {

                ResolvedNodeKey resolved_node_key = _tryResolveNodeKey(selected_node_key, folder_key_bytes, attr);

                if (resolved_node_key != null) {
                    return resolved_node_key;
                }
            }

            for (String enc_node_key : _extractNodeKeyCandidates(raw_node_key)) {

                if (selected_node_key != null && selected_node_key.equals(enc_node_key)) {
                    continue;
                }

                ResolvedNodeKey resolved_node_key = _tryResolveNodeKey(enc_node_key, folder_key_bytes, attr);

                if (resolved_node_key != null) {
                    return resolved_node_key;
                }
            }

        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Unable to resolve MEGA node key", ex);
        }

        return null;
    }

    public String initUploadFile(String filename) throws MegaAPIException {

        String ul_url = null;

        try {

            File f = new File(filename);

            String request = "[{\"a\":\"u\", \"s\":" + String.valueOf(f.length()) + "}]";

            URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : ""));

            String res = RAW_REQUEST(request, url_api);

            ObjectMapper objectMapper = new ObjectMapper();

            HashMap[] res_map = objectMapper.readValue(res, HashMap[].class);

            ul_url = (String) res_map[0].get("p");

        } catch (MegaAPIException mae) {

            throw mae;

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        }

        return ul_url;
    }

    public String uploadThumbnails(Upload upload, String node_handle, String filename0, String filename1) throws MegaAPIException {

        String[] ul_url = new String[2];

        String[] hash = new String[2];

        try {

            File[] files = new File[2];

            files[0] = new File(filename0);

            byte[][] file_bytes = new byte[2][];

            file_bytes[0] = _encThumbAttr(Files.readAllBytes(files[0].toPath()), upload.getByte_file_key());

            files[1] = new File(filename1);

            file_bytes[1] = _encThumbAttr(Files.readAllBytes(files[1].toPath()), upload.getByte_file_key());

            String request = "[{\"a\":\"ufa\", \"s\":" + String.valueOf(file_bytes[0].length) + ", \"ssl\":1}, {\"a\":\"ufa\", \"s\":" + String.valueOf(file_bytes[1].length) + ", \"ssl\":1}]";

            URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : ""));

            String res = RAW_REQUEST(request, url_api);

            ObjectMapper objectMapper = new ObjectMapper();

            HashMap[] res_map = objectMapper.readValue(res, HashMap[].class);

            ul_url[0] = (String) res_map[0].get("p");

            ul_url[1] = (String) res_map[1].get("p");

            int h = 0;

            for (String u : ul_url) {

                URL url = new URL(u);

                HttpURLConnection con = null;

                try {

                    con = (HttpURLConnection) url.openConnection();

                    con.setConnectTimeout(Transference.HTTP_CONNECT_TIMEOUT);

                    con.setReadTimeout(Transference.HTTP_READ_TIMEOUT);

                    con.setRequestMethod("POST");

                    con.setDoOutput(true);

                    con.setUseCaches(false);

                    con.setRequestProperty("User-Agent", MainPanel.DEFAULT_USER_AGENT);

                    byte[] buffer = new byte[8192];

                    int reads;

                    try (OutputStream out = new ThrottledOutputStream(con.getOutputStream(), upload.getMain_panel().getStream_supervisor())) {

                        out.write(file_bytes[h]);
                    }

                    int status = con.getResponseCode();

                    if (status != 200) {
                        MiscTools.drainAndCloseErrorStream(con);
                        throw new IOException("Thumbnail upload failed: HTTP " + status);
                    }

                    try (InputStream is = con.getInputStream(); ByteArrayOutputStream byte_res = new ByteArrayOutputStream()) {

                        while ((reads = is.read(buffer)) != -1) {
                            byte_res.write(buffer, 0, reads);
                        }

                        hash[h] = MiscTools.Bin2UrlBASE64(byte_res.toByteArray());

                    }

                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
                }

                h++;
            }

            request = "[{\"a\":\"pfa\", \"fa\":\"0*" + hash[0] + "/1*" + hash[1] + "\", \"n\":\"" + node_handle + "\"}]";

            url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : ""));

            res = RAW_REQUEST(request, url_api);

            objectMapper = new ObjectMapper();

            String[] resp = objectMapper.readValue(res, String[].class);

            return (String) resp[0];

        } catch (MegaAPIException mae) {

            throw mae;

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        }

        return "";
    }

    public HashMap<String, Object> finishUploadFile(String fbasename, int[] ul_key, int[] fkey, int[] meta_mac, String completion_handle, String mega_parent, byte[] master_key, String root_node, byte[] share_key) throws MegaAPIException {

        HashMap[] res_map = null;

        try {

            byte[] enc_att = _encAttr("{\"n\":\"" + fbasename + "\"}", i32a2bin(Arrays.copyOfRange(ul_key, 0, 4)));

            URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : ""));

            String request = "[{\"a\":\"p\", \"t\":\"" + mega_parent + "\", \"n\":[{\"h\":\"" + completion_handle + "\", \"t\":0, \"a\":\"" + Bin2UrlBASE64(enc_att) + "\", \"k\":\"" + Bin2UrlBASE64(encryptKey(i32a2bin(fkey), master_key)) + "\"}], \"i\":\"" + _req_id + "\", \"cr\" : [ [\"" + root_node + "\"] , [\"" + completion_handle + "\"] , [0,0, \"" + Bin2UrlBASE64(encryptKey(i32a2bin(fkey), share_key)) + "\"]]}]";

            String res = RAW_REQUEST(request, url_api);

            ObjectMapper objectMapper = new ObjectMapper();

            res_map = objectMapper.readValue(res, HashMap[].class);

        } catch (MegaAPIException mae) {

            throw mae;

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        }

        return res_map != null ? res_map[0] : null;
    }

    public byte[] encryptKey(byte[] a, byte[] key) throws Exception {

        return aes_ecb_encrypt_nopadding(a, key);
    }

    public byte[] decryptKey(byte[] a, byte[] key) throws Exception {

        return aes_ecb_decrypt_nopadding(a, key);
    }

    private BigInteger[] _extractRSAPrivKey(byte[] rsa_data) {

        BigInteger[] rsa_key = new BigInteger[4];

        for (int i = 0, offset = 0; i < 4; i++) {

            int l = ((256 * ((((int) rsa_data[offset]) & 0xFF)) + (((int) rsa_data[offset + 1]) & 0xFF) + 7) / 8) + 2;

            rsa_key[i] = mpi2big(Arrays.copyOfRange(rsa_data, offset, offset + l));

            offset += l;
        }

        return rsa_key;
    }

    public HashMap<String, Object> createDir(String name, String parent_node, byte[] node_key, byte[] master_key) {

        HashMap[] res_map = null;

        try {

            byte[] enc_att = _encAttr("{\"n\":\"" + name + "\"}", node_key);

            byte[] enc_node_key = encryptKey(node_key, master_key);

            URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : ""));

            String request = "[{\"a\":\"p\", \"t\":\"" + parent_node + "\", \"n\":[{\"h\":\"xxxxxxxx\",\"t\":1,\"a\":\"" + Bin2UrlBASE64(enc_att) + "\",\"k\":\"" + Bin2UrlBASE64(enc_node_key) + "\"}],\"i\":\"" + _req_id + "\"}]";

            String res = RAW_REQUEST(request, url_api);

            ObjectMapper objectMapper = new ObjectMapper();

            res_map = objectMapper.readValue(res, HashMap[].class);

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        }

        return res_map != null ? res_map[0] : null;

    }

    public HashMap<String, Object> createDirInsideAnotherSharedDir(String name, String parent_node, byte[] node_key, byte[] master_key, String root_node, byte[] share_key) {

        HashMap[] res_map = null;

        try {

            byte[] enc_att = _encAttr("{\"n\":\"" + name + "\"}", node_key);

            byte[] enc_node_key = encryptKey(node_key, master_key);

            byte[] enc_node_key_s = encryptKey(node_key, share_key);

            URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : ""));

            String request = "[{\"a\":\"p\", \"t\":\"" + parent_node + "\", \"n\":[{\"h\":\"xxxxxxxx\",\"t\":1,\"a\":\"" + Bin2UrlBASE64(enc_att) + "\",\"k\":\"" + Bin2UrlBASE64(enc_node_key) + "\"}],\"i\":\"" + _req_id + "\", \"cr\" : [ [\"" + root_node + "\"] , [\"xxxxxxxx\"] , [0,0, \"" + Bin2UrlBASE64(enc_node_key_s) + "\"]]}]";

            String res = RAW_REQUEST(request, url_api);

            ObjectMapper objectMapper = new ObjectMapper();

            res_map = objectMapper.readValue(res, HashMap[].class);

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        }

        return res_map != null ? res_map[0] : null;

    }

    public String getPublicFileLink(String node, byte[] node_key) {

        String public_link = null;

        try {

            String file_id;

            List res_map;

            String request = "[{\"a\":\"l\", \"n\":\"" + node + "\"}]";

            URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : ""));

            String res = RAW_REQUEST(request, url_api);

            ObjectMapper objectMapper = new ObjectMapper();

            res_map = objectMapper.readValue(res, List.class);

            file_id = (String) res_map.get(0);

            public_link = "https://mega.nz/#!" + file_id + "!" + Bin2UrlBASE64(node_key);

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        }

        return public_link;
    }

    public String getPublicFolderLink(String node, byte[] node_key) {

        String public_link = null;

        try {

            String folder_id;

            List res_map;

            String request = "[{\"a\":\"l\", \"n\":\"" + node + "\", \"i\":\"" + _req_id + "\"}]";

            URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : ""));

            String res = RAW_REQUEST(request, url_api);

            ObjectMapper objectMapper = new ObjectMapper();

            res_map = objectMapper.readValue(res, List.class);

            folder_id = (String) res_map.get(0);

            public_link = "https://mega.nz/#F!" + folder_id + "!" + Bin2UrlBASE64(node_key);

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        }

        return public_link;
    }

    public int[] genUploadKey() {

        return bin2i32a(genRandomByteArray(24));
    }

    public byte[] genFolderKey() {

        return genRandomByteArray(16);
    }

    public byte[] genShareKey() {

        return genRandomByteArray(16);
    }

    public String shareFolder(String node, byte[] node_key, byte[] share_key) {

        try {

            String ok = Bin2UrlBASE64(encryptKey(share_key, i32a2bin(getMaster_key())));

            String enc_nk = Bin2UrlBASE64(encryptKey(node_key, share_key));

            String ha = cryptoHandleauth(node);

            //OJO
            String request = "[{\"a\":\"s2\",\"n\":\"" + node + "\",\"s\":[{\"u\":\"EXP\",\"r\":0}],\"i\":\"" + _req_id + "\",\"ok\":\"AAAAAAAAAAAAAAAAAAAAAA\",\"ha\":\"AAAAAAAAAAAAAAAAAAAAAA\",\"cr\":[[\"" + node + "\"],[\"" + node + "\"],[0,0,\"" + enc_nk + "\"]]}]";

            URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : ""));

            return RAW_REQUEST(request, url_api);

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        }

        return null;
    }

    public String cryptoHandleauth(String h) {

        String ch = null;

        try {

            ch = Bin2UrlBASE64(encryptKey((h + h).getBytes("UTF-8"), i32a2bin(getMaster_key())));

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        }

        return ch;
    }

    private static String _folderCachePath(String folder_id) {
        String safe_id = HashString("sha1", folder_id == null ? "" : folder_id);
        return System.getProperty("java.io.tmpdir") + File.separator + "megabasterd_folder_cache_" + safe_id;
    }

    public static final long FOLDER_CACHE_MAX_AGE_MS = 24L * 60L * 60L * 1000L;

    public boolean existsCachedFolderNodes(String folder_id) {

        java.nio.file.Path p = Paths.get(_folderCachePath(folder_id));

        if (!Files.exists(p)) {
            return false;
        }

        try {
            long age = System.currentTimeMillis() - Files.getLastModifiedTime(p).toMillis();
            if (age > FOLDER_CACHE_MAX_AGE_MS) {
                LOG.log(Level.INFO, "Folder cache for {0} is stale ({1}h old), invalidating", new Object[]{folder_id, age / 3600000L});
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignore) {
                }
                return false;
            }
        } catch (IOException ex) {
            return false;
        }

        return true;
    }

    private String getCachedFolderNodes(String folder_id) {

        String file_path = _folderCachePath(folder_id);

        if (Files.exists(Paths.get(file_path))) {

            LOG.log(Level.INFO, "MEGA FOLDER {0} USING CACHED JSON FILE TREE", new Object[]{folder_id});

            try {
                return new String(Files.readAllBytes(Paths.get(file_path)), java.nio.charset.StandardCharsets.UTF_8);
            } catch (IOException ex) {
                Logger.getLogger(MegaAPI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return null;
    }

    private void writeCachedFolderNodes(String folder_id, String res) {
        String file_path = _folderCachePath(folder_id);

        try {
            java.nio.file.Path tmp = Paths.get(file_path + ".tmp");
            Files.write(tmp, res.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            try {
                Files.move(tmp, Paths.get(file_path), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(tmp, Paths.get(file_path), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            Logger.getLogger(MegaAPI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public HashMap<String, Object> getFolderNodes(String folder_id, String folder_key, JProgressBar bar, boolean cache) throws Exception {

        HashMap<String, Object> folder_nodes = null;
        String res = null;

        if (cache) {
            res = getCachedFolderNodes(folder_id);
        }

        if (res == null) {
            String request = "[{\"a\":\"f\", \"c\":\"1\", \"r\":\"1\", \"ca\":\"1\"}]";
            URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + "&n=" + folder_id);
            res = RAW_REQUEST(request, url_api);

            if (res != null) {
                writeCachedFolderNodes(folder_id, res);
            }
        }

        if (res == null) {
            throw new Exception("No response from MEGA");
        }

        LOG.log(Level.INFO, "MEGA FOLDER {0} JSON FILE TREE SIZE -> {1}", new Object[]{folder_id, MiscTools.formatBytes((long) res.length())});

        ObjectMapper objectMapper = new ObjectMapper();
        HashMap[] res_map = objectMapper.readValue(res, HashMap[].class);
        folder_nodes = new HashMap<>();

        List folder_entries = (List) res_map[0].get("f");

        if (bar != null) {
            int s = folder_entries.size();
            MiscTools.GUIRun(() -> {
                bar.setIndeterminate(false);
                bar.setMaximum(s);
                bar.setValue(0);
            });
        }

        int conta_nodo = 0;
        byte[] decodedFolderKey = _urlBase64KeyDecode(folder_key);

        for (Object o : (Iterable<? extends Object>) folder_entries) {
            conta_nodo++;
            final int c = conta_nodo;

            if (bar != null) {
                MiscTools.GUIRun(() -> bar.setValue(c));
            }

            HashMap<String, Object> node = (HashMap<String, Object>) o;
            String full_k = (String) node.get("k");
            if (full_k == null || full_k.isEmpty()) {
                continue;
            }

            String[] segments = full_k.split("/");
            String valid_dec_node_k = null;
            HashMap valid_at = null;

            // Bucle Yellowstone: Probamos segmentos hasta dar con la llave que desencripta el nombre
            for (String segment : segments) {
                String[] node_k_parts = segment.split(":");
                if (node_k_parts.length >= 2) {
                    String potential_k_b64 = node_k_parts[node_k_parts.length - 1];

                    try {
                        byte[] nodeKeyBin = UrlBASE642Bin(potential_k_b64);
                        byte[] decryptedKeyBin = decryptKey(nodeKeyBin, decodedFolderKey);
                        String dec_node_k = Bin2UrlBASE64(decryptedKeyBin);

                        HashMap at = _decAttr((String) node.get("a"), _urlBase64KeyDecode(dec_node_k));

                        if (at != null && at.get("n") != null) {
                            valid_dec_node_k = dec_node_k;
                            valid_at = at;
                            break;
                        }
                    } catch (Exception e) {
                    }
                }
            }

            if (valid_at != null) {
                HashMap<String, Object> the_node = new HashMap<>();
                the_node.put("h", node.get("h"));
                the_node.put("type", node.get("t"));
                the_node.put("parent", node.get("p"));
                the_node.put("key", valid_dec_node_k);
                the_node.put("name", valid_at.get("n"));

                // FIX: Prevenir NullPointerException en JTree para carpetas
                if (node.get("s") != null) {
                    the_node.put("size", ((Number) node.get("s")).longValue());
                } else {
                    the_node.put("size", 0L);
                }

                folder_nodes.put((String) node.get("h"), the_node);
            }
        }

        return folder_nodes;
    }

    public HashMap<String, Object> getFolderNodes4(String folder_id, String folder_key, JProgressBar bar, boolean cache) throws Exception {

        HashMap<String, Object> folder_nodes = null;
        String res = null;

        if (cache) {
            res = getCachedFolderNodes(folder_id);
        }

        if (res == null) {
            String request = "[{\"a\":\"f\", \"c\":\"1\", \"r\":\"1\", \"ca\":\"1\"}]";
            URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + "&n=" + folder_id);
            res = RAW_REQUEST(request, url_api);

            if (res != null) {
                writeCachedFolderNodes(folder_id, res);
            }
        }

        LOG.log(Level.INFO, "MEGA FOLDER {0} JSON FILE TREE SIZE -> {1}", new Object[]{folder_id, MiscTools.formatBytes((long) res.length())});

        ObjectMapper objectMapper = new ObjectMapper();
        HashMap[] res_map = objectMapper.readValue(res, HashMap[].class);
        folder_nodes = new HashMap<>();

        List folder_entries = (List) res_map[0].get("f");

        String root_handle = null;

        if (!folder_entries.isEmpty() && ((HashMap<String, Object>) folder_entries.get(0)).get("h") instanceof String) {
            root_handle = (String) ((HashMap<String, Object>) folder_entries.get(0)).get("h");
        }

        int s = folder_entries.size();

        if (bar != null) {
            MiscTools.GUIRun(() -> {
                bar.setIndeterminate(false);
                bar.setMaximum(s);
                bar.setValue(0);
            });
        }

        int conta_nodo = 0;

        for (Object o : (Iterable<? extends Object>) res_map[0].get("f")) {
            conta_nodo++;
            int c = conta_nodo;

            if (bar != null) {
                MiscTools.GUIRun(() -> {
                    bar.setValue(c);
                });
            }

            HashMap<String, Object> node = (HashMap<String, Object>) o;

            // --- YELLOWSTONE FIX START ---
            String full_k = (String) node.get("k");
            if (full_k == null || full_k.isEmpty()) {
                continue;
            }

            String[] segments = full_k.split("/");
            String valid_dec_node_k = null;
            HashMap valid_at = null;

            // Test each segment (MEGA can send multiple keys if there are shared folders)
            for (String segment : segments) {
                String[] node_k_parts = segment.split(":");

                // If the segment has the format ID:KEY, extract the KEY (the last part)
                if (node_k_parts.length >= 2) {
                    String potential_k_b64 = node_k_parts[node_k_parts.length - 1];

                    try {
                        // Try to decrypt the node key with the folder key
                        byte[] decodedFolderKey = _urlBase64KeyDecode(folder_key);
                        byte[] nodeKeyBin = UrlBASE642Bin(potential_k_b64);
                        byte[] decryptedKeyBin = decryptKey(nodeKeyBin, decodedFolderKey);
                        String dec_node_k = Bin2UrlBASE64(decryptedKeyBin);

                        // Try to decrypt the attributes (file name)
                        HashMap at = _decAttr((String) node.get("a"), _urlBase64KeyDecode(dec_node_k));

                        if (at != null && at.get("n") != null) {
                            valid_dec_node_k = dec_node_k;
                            valid_at = at;
                            break; // Success! We found the working key
                        }
                    } catch (Exception e) {
                        // If this segment fails, try the next one
                    }
                }
            }

            if (valid_at != null) {
                HashMap<String, Object> the_node = new HashMap<>();
                the_node.put("type", node.get("t"));
                the_node.put("parent", node.get("p"));
                the_node.put("key", valid_dec_node_k);

                if (node.get("s") != null) {
                    if (node.get("s") instanceof Integer) {
                        long size = ((Number) node.get("s")).longValue();
                        the_node.put("size", size);
                    } else if (node.get("s") instanceof Long) {
                        long size = (Long) node.get("s");
                        the_node.put("size", size);
                    }
                } else {
                    the_node.put("size", 0L);
                }

                the_node.put("name", valid_at.get("n"));
                the_node.put("h", node.get("h"));

                folder_nodes.put((String) node.get("h"), the_node);
            } else {
                LOG.log(Level.WARNING, "WARNING: Could not decrypt node key for handle " + (String) node.get("h"));
            }
            // --- YELLOWSTONE FIX END ---
        }

        return folder_nodes;
    }

    public ArrayList<String> GENERATE_N_LINKS(Set<String> links) {

        HashMap<String, ArrayList<String>> map = new HashMap<>();

        ArrayList<String> nlinks = new ArrayList<>();

        for (String link : links) {

            String folder_id = findFirstRegex("#F\\*[^!]+!([^!]+)", link, 1);

            String folder_key = findFirstRegex("#F\\*[^!]+![^!]+!([^!]+)", link, 1);

            String file_id = findFirstRegex("#F\\*([^!]+)", link, 1);

            if (!map.containsKey(folder_id + ":" + folder_key)) {

                ArrayList<String> lista = new ArrayList<>();

                lista.add(file_id);

                map.put(folder_id + ":" + folder_key, lista);

            } else {

                map.get(folder_id + ":" + folder_key).add(file_id);

            }
        }

        for (Map.Entry<String, ArrayList<String>> entry : map.entrySet()) {

            String[] folder_parts = entry.getKey().split(":");

            int r = -1;

            if (existsCachedFolderNodes(folder_parts[0])) {
                final int[] rr = {-1};
                final String fid = folder_parts[0];
                MiscTools.GUIRunAndWait(() -> {
                    rr[0] = JOptionPane.showConfirmDialog(MainPanelView.getINSTANCE(), "Do you want to use FOLDER [" + fid + "] CACHED VERSION?\n\n(It could speed up the loading of very large folders)", "FOLDER CACHE", JOptionPane.YES_NO_OPTION);
                });
                r = rr[0];
            }

            try {
                nlinks.addAll(getNLinksFromFolder(folder_parts[0], folder_parts[1], entry.getValue(), (r == 0)));
            } catch (Exception ex) {
                Logger.getLogger(MegaAPI.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        return nlinks;

    }

    public ArrayList<String> getNLinksFromFolder(String folder_id, String folder_key, ArrayList<String> file_ids, boolean cache) throws Exception {

        ArrayList<String> nlinks = new ArrayList<>();

        HashMap<String, Object> folder_nodes = getFolderNodes(folder_id, folder_key, null, cache);

        if (folder_nodes != null) {

            for (String file_id : file_ids) {

                HashMap<String, Object> node = (HashMap<String, Object>) folder_nodes.get(file_id);

                if (node != null && node.get("key") != null) {
                    nlinks.add("https://mega.nz/#N!" + file_id + "!" + (String) node.get("key") + "###n=" + folder_id);
                }
            }

        } else {

            throw new Exception();
        }

        return nlinks;

    }

    private byte[] _urlBase64KeyDecode(String key) {

        try {
            byte[] key_bin = UrlBASE642Bin(key);

            if (key_bin.length < 32) {

                return Arrays.copyOfRange(key_bin, 0, 16);

            } else {

                int[] key_i32a = bin2i32a(Arrays.copyOfRange(key_bin, 0, 32));

                int[] k = {key_i32a[0] ^ key_i32a[4], key_i32a[1] ^ key_i32a[5], key_i32a[2] ^ key_i32a[6], key_i32a[3] ^ key_i32a[7]};

                return i32a2bin(k);
            }

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        }

        return null;
    }

}
