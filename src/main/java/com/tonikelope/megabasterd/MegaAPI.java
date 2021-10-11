package com.tonikelope.megabasterd;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import static com.tonikelope.megabasterd.CryptTools.*;
import static com.tonikelope.megabasterd.MiscTools.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;

/**
 *
 * @author tonikelope
 */
public class MegaAPI implements Serializable {

    public static final String API_URL = "https://g.api.mega.co.nz";
    public static String API_KEY = null;
    public static final int REQ_ID_LENGTH = 10;
    public static final Integer[] MEGA_ERROR_NO_EXCEPTION_CODES = {-1, -3};
    public static final int PBKDF2_ITERATIONS = 100000;
    public static final int PBKDF2_OUTPUT_BIT_LENGTH = 256;
    private static final Logger LOG = Logger.getLogger(MegaAPI.class.getName());

    public static int checkMEGAError(String data) {
        String error = findFirstRegex("^\\[?(\\-[0-9]+)\\]?$", data, 1);

        return error != null ? Integer.parseInt(error) : 0;
    }

    private long _seqno;

    private String _sid;

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

        Random randomno = new Random();
        _seqno = randomno.nextLong() & 0xffffffffL;
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

        URL url_api = new URL(API_URL + "/cs?id=" + String.valueOf(_seqno) + (API_KEY != null ? "&ak=" + API_KEY : ""));

        String res = _rawRequest(request, url_api);

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

        URL url_api = new URL(API_URL + "/cs?id=" + String.valueOf(_seqno) + (API_KEY != null ? "&ak=" + API_KEY : ""));

        String res = _rawRequest(request, url_api);

        ObjectMapper objectMapper = new ObjectMapper();

        HashMap[] res_map = objectMapper.readValue(res, HashMap[].class);

        _account_version = (Integer) res_map[0].get("v");

        _salt = (String) res_map[0].get("s");

    }

    public boolean check2FA(String email) throws Exception {

        String request = "[{\"a\":\"mfag\",\"e\":\"" + email + "\"}]";

        URL url_api = new URL(API_URL + "/cs?id=" + String.valueOf(_seqno) + (API_KEY != null ? "&ak=" + API_KEY : ""));

        String res = _rawRequest(request, url_api);

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

            url_api = new URL(API_URL + "/cs?id=" + String.valueOf(_seqno) + (_sid != null ? "&sid=" + _sid : "") + (API_KEY != null ? "&ak=" + API_KEY : ""));

            String res = _rawRequest(request, url_api);

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

            url_api = new URL(API_URL + "/cs?id=" + String.valueOf(_seqno) + (_sid != null ? "&sid=" + _sid : "") + (API_KEY != null ? "&ak=" + API_KEY : ""));

            String res = _rawRequest(request, url_api);

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

    private String _rawRequest(String request, URL url_api) throws MegaAPIException {

        String response = null, current_smart_proxy = null;

        int mega_error = 0, http_error = 0, conta_error = 0, http_status;

        boolean empty_response = false, smart_proxy_socks = false;

        HttpsURLConnection con = null;

        ArrayList<String> excluded_proxy_list = new ArrayList<>();

        SmartMegaProxyManager proxy_manager = MainPanel.getProxy_manager();

        if (MainPanel.FORCE_SMART_PROXY) {

            String[] smart_proxy = proxy_manager.getProxy(excluded_proxy_list);

            current_smart_proxy = smart_proxy[0];

            smart_proxy_socks = smart_proxy[1].equals("socks");

        }

        do {

            try {

                if ((current_smart_proxy != null || http_error == 509) && MainPanel.isUse_smart_proxy() && !MainPanel.isUse_proxy()) {

                    if (current_smart_proxy != null && (http_error != 0 || empty_response)) {

                        if (http_error == 509) {
                            proxy_manager.blockProxy(current_smart_proxy);
                        }

                        excluded_proxy_list.add(current_smart_proxy);

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

                con.setRequestProperty("User-Agent", MainPanel.DEFAULT_USER_AGENT);

                con.setUseCaches(false);

                con.setRequestMethod("POST");

                con.setDoOutput(true);

                con.getOutputStream().write(request.getBytes("UTF-8"));

                con.getOutputStream().close();

                http_status = con.getResponseCode();

                if (http_status != 200) {

                    LOG.log(Level.WARNING, "{0} {1} {2}", new Object[]{Thread.currentThread().getName(), request, url_api.toString()});

                    LOG.log(Level.WARNING, "{0} Failed : HTTP error code : {1}", new Object[]{Thread.currentThread().getName(), http_status});

                    http_error = http_status;

                } else {

                    try (InputStream is = con.getInputStream(); ByteArrayOutputStream byte_res = new ByteArrayOutputStream()) {

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

                if (con != null) {
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

        } while (empty_response || mega_error != 0 || (http_error == 509 && MainPanel.isUse_smart_proxy() && !MainPanel.isUse_proxy()));

        _seqno++;

        return response;

    }

    public String getMegaFileDownloadUrl(String link) throws MegaAPIException, MalformedURLException, IOException {

        String file_id = findFirstRegex("#.*?!([^!]+)", link, 1);

        String request;

        URL url_api;

        if (findFirstRegex("#N!", link, 0) != null) {
            String folder_id = findFirstRegex("###n=(.+)$", link, 1);

            request = "[{\"a\":\"g\", \"g\":\"1\", \"n\":\"" + file_id + "\"}]";

            url_api = new URL(API_URL + "/cs?id=" + String.valueOf(_seqno) + (_sid != null ? "&sid=" + _sid : "") + (API_KEY != null ? "&ak=" + API_KEY : "") + "&n=" + folder_id);

        } else {

            request = "[{\"a\":\"g\", \"g\":\"1\", \"p\":\"" + file_id + "\"}]";
            url_api = new URL(API_URL + "/cs?id=" + String.valueOf(_seqno) + (_sid != null ? "&sid=" + _sid : "") + (API_KEY != null ? "&ak=" + API_KEY : ""));
        }

        String data = _rawRequest(request, url_api);

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

            url_api = new URL(API_URL + "/cs?id=" + String.valueOf(_seqno) + (_sid != null ? "&sid=" + _sid : "") + (API_KEY != null ? "&ak=" + API_KEY : "") + "&n=" + folder_id);

        } else {

            request = "[{\"a\":\"g\", \"p\":\"" + file_id + "\"}]";

            url_api = new URL(API_URL + "/cs?id=" + String.valueOf(_seqno) + (_sid != null ? "&sid=" + _sid : "") + (API_KEY != null ? "&ak=" + API_KEY : ""));
        }

        String data = _rawRequest(request, url_api);

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
            LOG.log(Level.SEVERE, ex.getMessage());

        }

        return res_map;
    }

    public String initUploadFile(String filename) throws MegaAPIException {

        String ul_url = null;

        try {

            File f = new File(filename);

            String request = "[{\"a\":\"u\", \"s\":" + String.valueOf(f.length()) + "}]";

            URL url_api = new URL(API_URL + "/cs?id=" + String.valueOf(_seqno) + (_sid != null ? "&sid=" + _sid : "") + (API_KEY != null ? "&ak=" + API_KEY : ""));

            String res = _rawRequest(request, url_api);

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

    public HashMap<String, Object> finishUploadFile(String fbasename, int[] ul_key, int[] fkey, int[] meta_mac, String completion_handle, String mega_parent, byte[] master_key, String root_node, byte[] share_key) throws MegaAPIException {

        HashMap[] res_map = null;

        try {

            byte[] enc_att = _encAttr("{\"n\":\"" + fbasename + "\"}", i32a2bin(Arrays.copyOfRange(ul_key, 0, 4)));

            URL url_api = new URL(API_URL + "/cs?id=" + String.valueOf(_seqno) + (_sid != null ? "&sid=" + _sid : "") + (API_KEY != null ? "&ak=" + API_KEY : ""));

            String request = "[{\"a\":\"p\", \"t\":\"" + mega_parent + "\", \"n\":[{\"h\":\"" + completion_handle + "\", \"t\":0, \"a\":\"" + Bin2UrlBASE64(enc_att) + "\", \"k\":\"" + Bin2UrlBASE64(encryptKey(i32a2bin(fkey), master_key)) + "\"}], \"i\":\"" + _req_id + "\", \"cr\" : [ [\"" + root_node + "\"] , [\"" + completion_handle + "\"] , [0,0, \"" + Bin2UrlBASE64(encryptKey(i32a2bin(fkey), share_key)) + "\"]]}]";

            String res = _rawRequest(request, url_api);

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

            URL url_api = new URL(API_URL + "/cs?id=" + String.valueOf(_seqno) + (_sid != null ? "&sid=" + _sid : "") + (API_KEY != null ? "&ak=" + API_KEY : ""));

            String request = "[{\"a\":\"p\", \"t\":\"" + parent_node + "\", \"n\":[{\"h\":\"xxxxxxxx\",\"t\":1,\"a\":\"" + Bin2UrlBASE64(enc_att) + "\",\"k\":\"" + Bin2UrlBASE64(enc_node_key) + "\"}],\"i\":\"" + _req_id + "\"}]";

            String res = _rawRequest(request, url_api);

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

            URL url_api = new URL(API_URL + "/cs?id=" + String.valueOf(_seqno) + (_sid != null ? "&sid=" + _sid : "") + (API_KEY != null ? "&ak=" + API_KEY : ""));

            String request = "[{\"a\":\"p\", \"t\":\"" + parent_node + "\", \"n\":[{\"h\":\"xxxxxxxx\",\"t\":1,\"a\":\"" + Bin2UrlBASE64(enc_att) + "\",\"k\":\"" + Bin2UrlBASE64(enc_node_key) + "\"}],\"i\":\"" + _req_id + "\", \"cr\" : [ [\"" + root_node + "\"] , [\"xxxxxxxx\"] , [0,0, \"" + Bin2UrlBASE64(enc_node_key_s) + "\"]]}]";

            String res = _rawRequest(request, url_api);

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

            URL url_api = new URL(API_URL + "/cs?id=" + String.valueOf(_seqno) + (_sid != null ? "&sid=" + _sid : "") + (API_KEY != null ? "&ak=" + API_KEY : ""));

            String res = _rawRequest(request, url_api);

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

            URL url_api = new URL(API_URL + "/cs?id=" + String.valueOf(_seqno) + (_sid != null ? "&sid=" + _sid : "") + (API_KEY != null ? "&ak=" + API_KEY : ""));

            String res = _rawRequest(request, url_api);

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

            String request = "[{\"a\":\"s2\",\"n\":\"" + node + "\",\"s\":[{\"u\":\"EXP\",\"r\":0}],\"i\":\"" + _req_id + "\",\"ok\":\"" + ok + "\",\"ha\":\"" + ha + "\",\"cr\":[[\"" + node + "\"],[\"" + node + "\"],[0,0,\"" + enc_nk + "\"]]}]";

            URL url_api = new URL(API_URL + "/cs?id=" + String.valueOf(_seqno) + (_sid != null ? "&sid=" + _sid : "") + (API_KEY != null ? "&ak=" + API_KEY : ""));

            return _rawRequest(request, url_api);

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

    public HashMap<String, Object> getFolderNodes(String folder_id, String folder_key) throws Exception {

        HashMap<String, Object> folder_nodes = null;

        String request = "[{\"a\":\"f\", \"c\":\"1\", \"r\":\"1\"}]";

        URL url_api = new URL(API_URL + "/cs?id=" + String.valueOf(_seqno) + (API_KEY != null ? "&ak=" + API_KEY : "") + "&n=" + folder_id);

        String res = _rawRequest(request, url_api);

        if (res != null) {

            ObjectMapper objectMapper = new ObjectMapper();

            HashMap[] res_map = objectMapper.readValue(res, HashMap[].class);

            folder_nodes = new HashMap<>();

            for (Object o : (Iterable<? extends Object>) res_map[0].get("f")) {

                HashMap<String, Object> node = (HashMap<String, Object>) o;

                String[] node_k = ((String) node.get("k")).split(":");

                if (node_k.length == 2 && node_k[0] != "" && node_k[1] != "") {

                    try {

                        String dec_node_k = Bin2UrlBASE64(decryptKey(UrlBASE642Bin(node_k[1]), _urlBase64KeyDecode(folder_key)));

                        HashMap at = _decAttr((String) node.get("a"), _urlBase64KeyDecode(dec_node_k));

                        HashMap<String, Object> the_node = new HashMap<>();

                        the_node.put("type", node.get("t"));

                        the_node.put("parent", node.get("p"));

                        the_node.put("key", dec_node_k);

                        if (node.get("s") != null) {

                            if (node.get("s") instanceof Integer) {

                                long size = ((Number) node.get("s")).longValue();
                                the_node.put("size", size);

                            } else if (node.get("s") instanceof Long) {

                                long size = (Long) node.get("s");
                                the_node.put("size", size);
                            }
                        }

                        the_node.put("name", at.get("n"));

                        the_node.put("h", node.get("h"));

                        folder_nodes.put((String) node.get("h"), the_node);

                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "WARNING: node key is not valid " + (String) node.get("k"));
                    }

                } else {
                    LOG.log(Level.WARNING, "WARNING: node key is not valid " + (String) node.get("k"));
                }

            }

        } else {

            throw new Exception();
        }

        return folder_nodes;
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
