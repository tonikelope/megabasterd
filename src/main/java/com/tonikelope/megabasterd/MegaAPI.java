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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ConnectionClosedException;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.NoHttpResponseException;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLException;
import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import static com.tonikelope.megabasterd.CryptTools.AES_ZERO_IV;
import static com.tonikelope.megabasterd.CryptTools.MEGAPrepareMasterKey;
import static com.tonikelope.megabasterd.CryptTools.MEGAUserHash;
import static com.tonikelope.megabasterd.CryptTools.aes_cbc_decrypt_noPadding;
import static com.tonikelope.megabasterd.CryptTools.aes_cbc_encrypt_noPadding;
import static com.tonikelope.megabasterd.CryptTools.aes_cbc_encrypt_pkcs7;
import static com.tonikelope.megabasterd.CryptTools.aes_ecb_decrypt_noPadding;
import static com.tonikelope.megabasterd.CryptTools.aes_ecb_encrypt_noPadding;
import static com.tonikelope.megabasterd.CryptTools.initMEGALinkKey;
import static com.tonikelope.megabasterd.CryptTools.rsaDecrypt;
import static com.tonikelope.megabasterd.MainPanel.getProxy_manager;
import static com.tonikelope.megabasterd.MiscTools.Bin2UrlBASE64;
import static com.tonikelope.megabasterd.MiscTools.UrlBASE642Bin;
import static com.tonikelope.megabasterd.MiscTools.bin2i32a;
import static com.tonikelope.megabasterd.MiscTools.cleanFilename;
import static com.tonikelope.megabasterd.MiscTools.findFirstRegex;
import static com.tonikelope.megabasterd.MiscTools.genID;
import static com.tonikelope.megabasterd.MiscTools.genRandomByteArray;
import static com.tonikelope.megabasterd.MiscTools.getWaitTimeExpBackOff;
import static com.tonikelope.megabasterd.MiscTools.i32a2bin;
import static com.tonikelope.megabasterd.MiscTools.mpi2big;

/**
 *
 * @author tonikelope
 */
public class MegaAPI implements Serializable {

    private static final Logger LOG = LogManager.getLogger(MegaAPI.class);
    public static final String API_URL = "https://g.api.mega.co.nz";
    public static String API_KEY = null;
    public static final int REQ_ID_LENGTH = 10;
    public static final Integer[] MEGA_ERROR_NO_EXCEPTION_CODES = {-1, -3};
    public static final int PBKDF2_ITERATIONS = 100000;
    public static final int PBKDF2_OUTPUT_BIT_LENGTH = 256;
    private static final ArrayList<String> _excluded_proxy_list = new ArrayList<>();

    public static int checkMEGAError(String data) {
        String error = findFirstRegex("^\\[?(\\-[0-9]+)\\]?$", data, 1);

        return error != null ? Integer.parseInt(error) : 0;
    }

    private long _sequenceNumber;

    private String _sid;

    private int[] _master_key;

    private BigInteger[] _rsa_private_key;

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
        _rsa_private_key = null;
        _master_key = null;
        _salt = null;
        _sid = null;
        _account_version = -1;
        _req_id = genID(REQ_ID_LENGTH);
        _sequenceNumber = new Random().nextLong() & 0xffffffffL;
    }

    private static String _current_smart_proxy = null;

    private static FastMegaHttpClient.MegaHttpProxyConfiguration createMegaDownloadProxyConfig(HttpSet statusSet) {
        return new FastMegaHttpClient.MegaHttpProxyConfiguration(
            FastMegaHttpClient.FMProxyType.SMART,
            () -> _excluded_proxy_list,
            () -> statusSet.httpError.get() == 509,
            /* Extra smart conditions */ () -> statusSet.httpError.get() == 509,
            true,
            false,
            (newCurrentSmartProxy) -> {
                _current_smart_proxy = newCurrentSmartProxy;
                return Unit.INSTANCE;
            }
        );
    }

    private static Map<FastMegaHttpClient.FMEventType, Function0<Unit>> getMegaDownloadClientListenerMap(HttpSet statusSet) {
        return new HashMap<>() {{
            put(FastMegaHttpClient.FMEventType.CURRENT_SMART_PROXY_ERRORED, () -> {
                if (_current_smart_proxy != null && statusSet.httpError.get() != 0) {
                    getProxy_manager().blockProxy(_current_smart_proxy, "HTTP " + statusSet.httpError.get());
                }
                return Unit.INSTANCE;
            });
        }};
    }

    private static class HttpSet {
        AtomicInteger httpError = new AtomicInteger(0);
        AtomicInteger httpStatus = new AtomicInteger(0);
    }

    public boolean checkMegaDownloadUrl(String string_url) throws MalformedURLException {
        if (string_url == null || string_url.isEmpty()) return false;

        HttpSet statusSet = new HttpSet();

        String hostString = findFirstRegex("https?://([^/]+)", string_url, 1);
        if (hostString == null) throw new MalformedURLException("Invalid URL: " + string_url);

        HttpHost httpHost = new HttpHost(hostString);
        URI uri = URI.create("http://" + httpHost.getHostName() + string_url.substring(hostString.length()) + "/0-0");

        do {
            try (
                FastMegaHttpClient<HttpGet> fastClient = new FastMegaHttpClient<>(
                    uri,
                    (pUrl) -> new HttpGet(pUrl.toString()),
                    RequestConfig.custom(),
                    createMegaDownloadProxyConfig(statusSet),
                    getMegaDownloadClientListenerMap(statusSet)
                ).withProperty(FastMegaHttpClient.FMProperty.NO_CACHE);
                ClassicHttpResponse response = fastClient.execute()
            ) {
                statusSet.httpStatus.set(response.getCode());
                if (statusSet.httpStatus.get() != 200) {
                    statusSet.httpError.set(statusSet.httpStatus.get());
                } else statusSet.httpError.set(0);
            }
            catch (Exception ex) {
                if (ex instanceof SocketTimeoutException || ex instanceof SocketException || ex instanceof UnknownHostException ||
                        ex instanceof NoHttpResponseException || ex instanceof ConnectionClosedException || ex instanceof CancellationException
                ) {
                    LOG.info("Connection error reading MEGA URL {}, deferring slightly.", ex.getMessage());
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ignored) { }
                } else {
                    LOG.fatal("FAILED TO CHECK DOWNLOAD URL: {} : {} - {}", uri, ex.getClass(), ex.getMessage());
                }
            }
        } while (statusSet.httpError.get() == 509);

        return statusSet.httpStatus.get() != 403;
    }

    private URI getApiURI(Boolean withSid) {
        return getApiURI(withSid, null);
    }

    private URI getApiURI(Boolean withSid, String folderId) {
        StringBuilder urlBuilder = new StringBuilder(API_URL + "/cs?id=" + _sequenceNumber);
        if (withSid && _sid != null) {
            urlBuilder.append("&sid=").append(_sid);
        }
        if (folderId != null) {
            urlBuilder.append("&n=").append(folderId);
        }
        return URI.create(urlBuilder.toString());
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
        return _rsa_private_key;
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

    private void _realLogin(String pinCode) throws Exception {

        String request;

        if (pinCode != null) {
            request = "[{\"a\":\"us\", \"mfa\":\"" + pinCode + "\", \"user\":\"" + _email + "\",\"uh\":\"" + _user_hash + "\"}]";
        } else {
            request = "[{\"a\":\"us\",\"user\":\"" + _email + "\",\"uh\":\"" + _user_hash + "\"}]";
        }

        String res = RAW_REQUEST(request, getApiURI(false));

        ObjectMapper objectMapper = new ObjectMapper();

        HashMap[] res_map = objectMapper.readValue(res, HashMap[].class);

        String k = (String) res_map[0].get("k");

        String privateKey = (String) res_map[0].get("privk");

        _master_key = bin2i32a(decryptKey(UrlBASE642Bin(k), i32a2bin(_password_aes)));

        String csid = (String) res_map[0].get("csid");

        if (csid != null) {

            int[] enc_rsa_private_key = bin2i32a(UrlBASE642Bin(privateKey));

            byte[] privateKey_byte = decryptKey(i32a2bin(enc_rsa_private_key), i32a2bin(_master_key));

            _rsa_private_key = _extractRSAPrivKey(privateKey_byte);

            byte[] raw_sid = rsaDecrypt(mpi2big(UrlBASE642Bin(csid)), _rsa_private_key[0], _rsa_private_key[1], _rsa_private_key[2]);

            _sid = Bin2UrlBASE64(Arrays.copyOfRange(raw_sid, 0, 43));
        }

        fetchNodes();
    }

    private void _readAccountVersionAndSalt() throws Exception {

        String request = "[{\"a\":\"us0\",\"user\":\"" + _email + "\"}]";

        URI apiUrl = getApiURI(false);

        String res = RAW_REQUEST(request, apiUrl);

        ObjectMapper objectMapper = new ObjectMapper();

        HashMap[] res_map = objectMapper.readValue(res, HashMap[].class);

        _account_version = (Integer) res_map[0].get("v");

        _salt = (String) res_map[0].get("s");

    }

    public boolean check2FA(String email) throws Exception {

        String request = "[{\"a\":\"mfag\",\"e\":\"" + email + "\"}]";

        URI apiUrl = getApiURI(false);

        String res = RAW_REQUEST(request, apiUrl);

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

            _password_aes = MEGAPrepareMasterKey(bin2i32a(password.getBytes(StandardCharsets.UTF_8)));

            _user_hash = MEGAUserHash(_email.toLowerCase().getBytes(StandardCharsets.UTF_8), _password_aes);

        } else {

            byte[] pbkdf2_key = CryptTools.PBKDF2_HMAC_SHA512(password, MiscTools.UrlBASE642Bin(_salt), PBKDF2_ITERATIONS, PBKDF2_OUTPUT_BIT_LENGTH);

            _password_aes = bin2i32a(Arrays.copyOfRange(pbkdf2_key, 0, 16));

            _user_hash = MiscTools.Bin2UrlBASE64(Arrays.copyOfRange(pbkdf2_key, 16, 32));
        }

        _realLogin(pincode);
    }

    public void fastLogin(String email, int[] password_aes, String user_hash, String pinCode) throws Exception {

        _full_email = email;

        String[] email_split = email.split(" *# *");

        _email = email_split[0];

        if (_account_version == -1) {
            _readAccountVersionAndSalt();
        }

        _password_aes = password_aes;

        _user_hash = user_hash;

        _realLogin(pinCode);
    }

    public Long[] getQuota() {

        Long[] quota = null;

        try {
            String request = "[{\"a\": \"uq\", \"xfer\": 1, \"strg\": 1}]";

            URI apiUrl;

            apiUrl = getApiURI(true);

            String res = RAW_REQUEST(request, apiUrl);

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

            LOG.fatal("Failed to get quota! {}", ex.getMessage());
        }

        return quota;
    }

    public void fetchNodes() {

        String request = "[{\"a\":\"f\", \"c\":1}]";

        URI apiUrl;

        try {

            apiUrl = getApiURI(true);

            String res = RAW_REQUEST(request, apiUrl);

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
            LOG.fatal("Exception captured fetching nodes! {}", ex.getMessage());
        }
    }

    private FastMegaHttpClient.MegaHttpProxyConfiguration getMegaRawReqProxyConfig(
        AtomicBoolean emptyResponse, AtomicInteger httpError
    ) {
        return new FastMegaHttpClient.MegaHttpProxyConfiguration(
            FastMegaHttpClient.FMProxyType.SMART,
            () -> _excluded_proxy_list,
            () -> emptyResponse.get() || httpError.get() != 0,
            () -> httpError.get() == 509,
            true,
            false,
            (ignore) -> Unit.INSTANCE
        );
    }

    private String RAW_REQUEST(String requestBody, URI apiUri) throws MegaAPIException {
        String megaResponse = null;
        int megaError, errorCount = 0, httpStatus;
        final AtomicBoolean emptyResponse = new AtomicBoolean(false), timeoutError = new AtomicBoolean(false);
        final AtomicInteger httpError = new AtomicInteger(0);

        do {
            httpError.set(0);
            megaError = 0;
            emptyResponse.set(false);
            timeoutError.set(false);
            try (
                FastMegaHttpClient<HttpPost> fastClient = new FastMegaHttpClient<>(
                    apiUri,
                    (pUri) -> {
                        HttpPost postRequest = new HttpPost(pUri.toString());
                        postRequest.setHeader("Accept-Encoding", "gzip");
                        postRequest.setHeader("Content-type", "text/plain;charset=UTF-8");
                        postRequest.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
                        return postRequest;
                    },
                    RequestConfig.custom(),
                    getMegaRawReqProxyConfig(emptyResponse, httpError),
                    new HashMap<>()
                ).withProperty(FastMegaHttpClient.FMProperty.NO_CACHE);
                ClassicHttpResponse httpResponse = fastClient.execute()
            ) {
                httpStatus = httpResponse.getCode();
                Header contentTypeHeader = httpResponse.getHeader("Content-Encoding");
                boolean isGzip = contentTypeHeader != null && contentTypeHeader.getValue().equals("gzip");

                if (httpStatus != 200) {
                    LOG.warn("{} {}",  requestBody, apiUri.toString());
                    LOG.warn("Failed : HTTP error code: {}", httpStatus);
                    httpError.set(httpStatus);
                } else try(
                    InputStream inputStream = isGzip ? new GZIPInputStream(httpResponse.getEntity().getContent()) : httpResponse.getEntity().getContent();
                    ByteArrayOutputStream byteRes = new ByteArrayOutputStream()
                ) {
                    inputStream.transferTo(byteRes);
                    megaResponse = byteRes.toString(StandardCharsets.UTF_8);
                    if (!megaResponse.isEmpty()) {
                        megaError = checkMEGAError(megaResponse);
                        if (megaError != 0 && !Arrays.asList(MEGA_ERROR_NO_EXCEPTION_CODES).contains(megaError)) {
                            throw new MegaAPIException(megaError);
                        }
                    } else emptyResponse.set(true);
                }
            } catch (SSLException | ProtocolException ex) {
                emptyResponse.set(true);
                LOG.fatal("{} (Empty Response) in RAW_REQUEST! {}", ex.getClass().getName(), ex.getMessage());
            } catch (IOException ex) {
                if (ex instanceof ConnectTimeoutException) {
                    timeoutError.set(true);
                } else LOG.fatal("UNHANDLED {} in RAW_REQUEST! {}", ex.getClass().getName(), ex.getMessage());
            }

            if ((timeoutError.get() || emptyResponse.get() || megaError != 0 || httpError.get() != 0) && httpError.get() != 509) {
                LOG.warn("MegaAPI ERROR {} Waiting for retry...", String.valueOf(megaError));
                try {
                    Thread.sleep(getWaitTimeExpBackOff(errorCount++) * 1000);
                } catch (InterruptedException ex) {
                    LOG.fatal("Back-off sleep interrupted! {}", ex.getMessage());
                }
            }

        } while (httpError.get() == 500 || emptyResponse.get() || megaError != 0 || (httpError.get() == 509 && MainPanel.isUse_smart_proxy() && !MainPanel.isUse_proxy()));

        _sequenceNumber++;

        return megaResponse;

    }

    public String getMegaFileDownloadUrl(String link) throws MegaAPIException, IOException {

        String file_id = findFirstRegex("#.*?!([^!]+)", link, 1);

        String request;

        URI apiUrl;

        if (findFirstRegex("#N!", link, 0) != null) {
            String folder_id = findFirstRegex("###n=(.+)$", link, 1);
            request = "[{\"a\":\"g\", \"g\":\"1\", \"n\":\"" + file_id + "\"}]";
            apiUrl = getApiURI(true, folder_id);
        } else {
            request = "[{\"a\":\"g\", \"g\":\"1\", \"p\":\"" + file_id + "\"}]";
            apiUrl = getApiURI(true);
        }

        String data = RAW_REQUEST(request, apiUrl);

        ObjectMapper objectMapper = new ObjectMapper();

        HashMap[] res_map = objectMapper.readValue(data, HashMap[].class);

        String download_url = (String) res_map[0].get("g");

        if (download_url == null || download_url.isEmpty()) {
            throw new MegaAPIException(-101);
        }

        return download_url;
    }

    public String[] getMegaFileMetadata(String link) throws MegaAPIException, IOException {

        String file_id = findFirstRegex("#.*?!([^!]+)", link, 1);
        String file_key = findFirstRegex("#.*?![^!]+!([^!#]+)", link, 1);

        String request;
        URI apiUri;

        if (findFirstRegex("#N!", link, 0) != null) {
            String folder_id = findFirstRegex("###n=(.+)$", link, 1);
            request = "[{\"a\":\"g\", \"g\":\"1\", \"n\":\"" + file_id + "\"}]";
            apiUri = getApiURI(true, folder_id);
        } else {
            request = "[{\"a\":\"g\", \"p\":\"" + file_id + "\"}]";
            apiUri = getApiURI(true);
        }

        String data = RAW_REQUEST(request, apiUri);

        ObjectMapper objectMapper = new ObjectMapper();

        HashMap[] res_map = objectMapper.readValue(data, HashMap[].class);

        String fsize = String.valueOf(res_map[0].get("s"));

        String at = (String) res_map[0].get("at");

        String[] file_data;

        HashMap<String, String> att_map = _decAttr(at, initMEGALinkKey(file_key));

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
            LOG.fatal("Exception while encoding attributes! {}", ex.getMessage());
        }

        return null;
    }

    private byte[] _encAttr(String attr, byte[] key) {

        byte[] ret = null;

        try {
            byte[] attr_byte = ("MEGA" + attr).getBytes(StandardCharsets.UTF_8);
            int l = (int) (16 * Math.ceil((double) attr_byte.length / 16));
            byte[] new_attr_byte = Arrays.copyOfRange(attr_byte, 0, l);
            ret = aes_cbc_encrypt_noPadding(new_attr_byte, key, AES_ZERO_IV);
        } catch (Exception ex) {
            LOG.fatal("Exception encoding attribute! {}", ex.getMessage());
        }

        return ret;
    }

    private HashMap<String, String> _decAttr(String encAttr, byte[] key) {

        HashMap<String, String> res_map = null;

        byte[] decrypted_at;

        try {

            decrypted_at = aes_cbc_decrypt_noPadding(UrlBASE642Bin(encAttr), key, AES_ZERO_IV);

            String att = new String(decrypted_at, StandardCharsets.UTF_8).replaceAll("\0+$", "").replaceAll("^MEGA", "");

            ObjectMapper objectMapper = new ObjectMapper();

            objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

            // todo migrate to JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER
            objectMapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);

            res_map = objectMapper.readValue(att, new TypeReference<HashMap<String, String>>() {});

        } catch (Exception ex) {
            LOG.fatal("Exception decoding attribute! {}", ex.getMessage());
        }

        return res_map;
    }

    public String initUploadFile(String filename) throws MegaAPIException {

        String ul_url = null;

        try {

            File f = new File(filename);

            String request = "[{\"a\":\"u\", \"s\":" + f.length() + "}]";

            URI apiUrl = getApiURI(true);

            String res = RAW_REQUEST(request, apiUrl);

            ObjectMapper objectMapper = new ObjectMapper();

            HashMap[] res_map = objectMapper.readValue(res, HashMap[].class);

            ul_url = (String) res_map[0].get("p");

        } catch (MegaAPIException mae) {
            throw mae;
        } catch (Exception ex) {
            LOG.fatal("Could not initialize upload! {}", ex.getMessage());
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
            String request = "[{\"a\":\"ufa\", \"s\":" + file_bytes[0].length + ", \"ssl\":1}, {\"a\":\"ufa\", \"s\":" + file_bytes[1].length + ", \"ssl\":1}]";

            URI apiUrl = getApiURI(true);

            String res = RAW_REQUEST(request, apiUrl);

            ObjectMapper objectMapper = new ObjectMapper();

            HashMap[] res_map = objectMapper.readValue(res, HashMap[].class);

            ul_url[0] = (String) res_map[0].get("p");

            ul_url[1] = (String) res_map[1].get("p");

            int h = 0;

            for (String u : ul_url) {

                URL url = URI.create(u).toURL();
                HttpURLConnection con;
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

                try (InputStream is = con.getInputStream(); ByteArrayOutputStream byte_res = new ByteArrayOutputStream()) {
                    while ((reads = is.read(buffer)) != -1) {
                        byte_res.write(buffer, 0, reads);
                    }
                    hash[h] = MiscTools.Bin2UrlBASE64(byte_res.toByteArray());
                }

                h++;
            }

            request = "[{\"a\":\"pfa\", \"fa\":\"0*" + hash[0] + "/1*" + hash[1] + "\", \"n\":\"" + node_handle + "\"}]";

            apiUrl = getApiURI(true);

            res = RAW_REQUEST(request, apiUrl);

            objectMapper = new ObjectMapper();

            String[] resp = objectMapper.readValue(res, String[].class);

            return resp[0];
        } catch (MegaAPIException mae) {
            throw mae;
        } catch (Exception ex) {
            LOG.fatal("Failed to upload thumbnails! {}", ex.getMessage());
        }

        return "";
    }

    public HashMap<String, Object> finishUploadFile(String fBaseName, int[] ul_key, int[] fKey, String completion_handle, String mega_parent, byte[] master_key, String root_node, byte[] share_key) throws MegaAPIException {

        HashMap[] res_map = null;

        try {

            byte[] enc_att = _encAttr("{\"n\":\"" + fBaseName + "\"}", i32a2bin(Arrays.copyOfRange(ul_key, 0, 4)));

            URI apiUrl = getApiURI(true);

            String request = "[{\"a\":\"p\", \"t\":\"" + mega_parent + "\", \"n\":[{\"h\":\"" + completion_handle + "\", \"t\":0, \"a\":\"" + Bin2UrlBASE64(enc_att) + "\", \"k\":\"" + Bin2UrlBASE64(encryptKey(i32a2bin(fKey), master_key)) + "\"}], \"i\":\"" + _req_id + "\", \"cr\" : [ [\"" + root_node + "\"] , [\"" + completion_handle + "\"] , [0,0, \"" + Bin2UrlBASE64(encryptKey(i32a2bin(fKey), share_key)) + "\"]]}]";

            String res = RAW_REQUEST(request, apiUrl);

            ObjectMapper objectMapper = new ObjectMapper();

            res_map = objectMapper.readValue(res, HashMap[].class);

        } catch (MegaAPIException mae) {
            throw mae;
        } catch (Exception ex) {
            LOG.fatal("Generic error in finishUploadFile! {}", ex.getMessage());
        }

        return res_map != null ? res_map[0] : null;
    }

    public byte[] encryptKey(byte[] a, byte[] key) throws Exception {

        return aes_ecb_encrypt_noPadding(a, key);
    }

    public byte[] decryptKey(byte[] a, byte[] key) throws Exception {

        return aes_ecb_decrypt_noPadding(a, key);
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

            URI apiUrl = getApiURI(true);

            String request = "[{\"a\":\"p\", \"t\":\"" + parent_node + "\", \"n\":[{\"h\":\"xxxxxxxx\",\"t\":1,\"a\":\"" + Bin2UrlBASE64(enc_att) + "\",\"k\":\"" + Bin2UrlBASE64(enc_node_key) + "\"}],\"i\":\"" + _req_id + "\"}]";

            String res = RAW_REQUEST(request, apiUrl);

            ObjectMapper objectMapper = new ObjectMapper();

            res_map = objectMapper.readValue(res, HashMap[].class);

        } catch (Exception ex) {
            LOG.fatal("Could not create dir! {}", ex.getMessage());
        }

        return res_map != null ? res_map[0] : null;

    }

    public HashMap<String, Object> createDirInsideAnotherSharedDir(String name, String parent_node, byte[] node_key, byte[] master_key, String root_node, byte[] share_key) {

        HashMap[] res_map = null;

        try {

            byte[] enc_att = _encAttr("{\"n\":\"" + name + "\"}", node_key);

            byte[] enc_node_key = encryptKey(node_key, master_key);

            byte[] enc_node_key_s = encryptKey(node_key, share_key);

            String request = "[{\"a\":\"p\", \"t\":\"" + parent_node + "\", \"n\":[{\"h\":\"xxxxxxxx\",\"t\":1,\"a\":\"" + Bin2UrlBASE64(enc_att) + "\",\"k\":\"" + Bin2UrlBASE64(enc_node_key) + "\"}],\"i\":\"" + _req_id + "\", \"cr\" : [ [\"" + root_node + "\"] , [\"xxxxxxxx\"] , [0,0, \"" + Bin2UrlBASE64(enc_node_key_s) + "\"]]}]";

            String res = RAW_REQUEST(request, getApiURI(true));

            ObjectMapper objectMapper = new ObjectMapper();

            res_map = objectMapper.readValue(res, HashMap[].class);

        } catch (Exception ex) {
            LOG.fatal("Failed to create dir {} inside shared dir {}! {}", name, parent_node, ex.getMessage());
        }

        return res_map != null ? res_map[0] : null;

    }

    public String getPublicFileLink(String node, byte[] node_key) {

        String public_link = null;

        try {

            String file_id;

            List<String> res_map;

            String request = "[{\"a\":\"l\", \"n\":\"" + node + "\"}]";

            String res = RAW_REQUEST(request, getApiURI(true));

            ObjectMapper objectMapper = new ObjectMapper();

            res_map = objectMapper.readValue(res, new TypeReference<>() {});

            file_id = res_map.getFirst();

            public_link = "https://mega.nz/#!" + file_id + "!" + Bin2UrlBASE64(node_key);

        } catch (Exception ex) {
            LOG.fatal("Failed to get public file link! {}", ex.getMessage());
        }

        return public_link;
    }

    public String getPublicFolderLink(String node, byte[] node_key) {

        String public_link = null;

        try {

            String folder_id;

            List res_map;

            String request = "[{\"a\":\"l\", \"n\":\"" + node + "\", \"i\":\"" + _req_id + "\"}]";

            URI apiUrl = getApiURI(true);

            String res = RAW_REQUEST(request, apiUrl);

            ObjectMapper objectMapper = new ObjectMapper();

            res_map = objectMapper.readValue(res, List.class);

            folder_id = (String) res_map.getFirst();

            public_link = "https://mega.nz/#F!" + folder_id + "!" + Bin2UrlBASE64(node_key);

        } catch (Exception ex) {
            LOG.fatal("Failed to get public folder link! {}", ex.getMessage());
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
            String enc_nk = Bin2UrlBASE64(encryptKey(node_key, share_key));
            String request = "[{\"a\":\"s2\",\"n\":\"" + node + "\",\"s\":[{\"u\":\"EXP\",\"r\":0}],\"i\":\"" + _req_id + "\",\"ok\":\"AAAAAAAAAAAAAAAAAAAAAA\",\"ha\":\"AAAAAAAAAAAAAAAAAAAAAA\",\"cr\":[[\"" + node + "\"],[\"" + node + "\"],[0,0,\"" + enc_nk + "\"]]}]";

            URI apiUrl = getApiURI(true);

            return RAW_REQUEST(request, apiUrl);

        } catch (Exception ex) {
            LOG.fatal("Exception sharing folder! {}", ex.getMessage());
        }

        return null;
    }

    public String cryptoHandleAuth(String h) {

        String ch = null;

        try {
            ch = Bin2UrlBASE64(encryptKey((h + h).getBytes(StandardCharsets.UTF_8), i32a2bin(getMaster_key())));
        } catch (Exception ex) {
            LOG.fatal("Failed to handle crypto auth! {}", ex.getMessage());
        }

        return ch;
    }

    public boolean existsCachedFolderNodes(String folder_id) {
        return Files.exists(Paths.get(System.getProperty("java.io.tmpdir") + File.separator + "megabasterd_folder_cache_" + folder_id));
    }

    private String getCachedFolderNodes(String folder_id) {

        String file_path = System.getProperty("java.io.tmpdir") + File.separator + "megabasterd_folder_cache_" + folder_id;

        Path path = Paths.get(file_path);
        if (Files.exists(path)) {
            LOG.info("MEGA FOLDER {} USING CACHED JSON FILE TREE", folder_id);
            try {
                return Files.readString(path);
            } catch (IOException ex) {
                LOG.fatal("IO Exception getting cached folder nodes!", ex);
            }
        }

        return null;
    }

    private void writeCachedFolderNodes(String folder_id, String res) {
        String file_path = System.getProperty("java.io.tmpdir") + File.separator + "megabasterd_folder_cache_" + folder_id;

        try {
            Files.write(Paths.get(file_path), res.getBytes());
        } catch (IOException ex) {
            LOG.fatal("IO Exception writing cached folder nodes!", ex);
        }
    }

    public HashMap<String, Object> getFolderNodes(String folder_id, String folder_key, JProgressBar bar, boolean cache) throws Exception {

        HashMap<String, Object> folder_nodes;

        String res = null;

        if (cache) {
            res = getCachedFolderNodes(folder_id);
        }

        if (res == null) {

            String request = "[{\"a\":\"f\", \"c\":\"1\", \"r\":\"1\", \"ca\":\"1\"}]";

            URI apiUrl = getApiURI(false, folder_id);

            res = RAW_REQUEST(request, apiUrl);

            if (res != null) {
                writeCachedFolderNodes(folder_id, res);
            }
        }

        LOG.info("GET NODES: MEGA FOLDER {} JSON FILE TREE SIZE -> {}", folder_id, MiscTools.formatBytes((long) res.length()));

        ObjectMapper objectMapper = new ObjectMapper();

        HashMap[] res_map = objectMapper.readValue(res, HashMap[].class);

        folder_nodes = new HashMap<>();

        int s = ((List) res_map[0].get("f")).size();

        if (bar != null) {
            MiscTools.GUIRun(() -> {
                bar.setIndeterminate(false);
                bar.setMaximum(s);
                bar.setValue(0);
            });
        }
        int nodeCount = 0;

        for (Object o : (Iterable<?>) res_map[0].get("f")) {

            nodeCount++;

            int c = nodeCount;

            if (bar != null) {
                MiscTools.GUIRun(() -> bar.setValue(c));
            }

            HashMap<String, Object> node = (HashMap<String, Object>) o;

            String[] node_k = ((String) node.get("k")).split(":");

            if (node_k.length == 2 && !Objects.equals(node_k[0], "") && !Objects.equals(node_k[1], "")) {

                try {

                    String dec_node_k = Bin2UrlBASE64(decryptKey(UrlBASE642Bin(node_k[1]), _urlBase64KeyDecode(folder_key)));

                    HashMap<String, String> at = _decAttr((String) node.get("a"), _urlBase64KeyDecode(dec_node_k));

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
                    } else {
                        the_node.put("size", 0L);
                    }

                    the_node.put("name", at.get("n"));

                    the_node.put("h", node.get("h"));

                    folder_nodes.put((String) node.get("h"), the_node);

                } catch (Exception e) {
                    LOG.warn("WARNING: Cannot get nodes, node key is not valid [1] {} {}", node.get("k"), folder_key);
                }

            } else {
                LOG.warn("WARNING: Cannot get nodes, node key is not valid [2] {} {}", node.get("k"), folder_key);
            }

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
                r = JOptionPane.showConfirmDialog(MainPanelView.getINSTANCE(), "Do you want to use FOLDER [" + folder_parts[0] + "] CACHED VERSION?\n\n(It could speed up the loading of very large folders)", "FOLDER CACHE", JOptionPane.YES_NO_OPTION);
            }

            try {
                nlinks.addAll(getNLinksFromFolder(folder_parts[0], folder_parts[1], entry.getValue(), (r == 0)));
            } catch (Exception ex) {
                LOG.fatal("Exceptions iterating over links!", ex);
            }

        }

        return nlinks;

    }

    public ArrayList<String> getNLinksFromFolder(String folder_id, String folder_key, ArrayList<String> file_ids, boolean cache) throws Exception {

        ArrayList<String> nLinks = new ArrayList<>();

        String res = null;

        if (cache) res = getCachedFolderNodes(folder_id);

        if (res == null) {
            String request = "[{\"a\":\"f\", \"c\":\"1\", \"r\":\"1\", \"ca\":\"1\"}]";
            res = RAW_REQUEST(request, getApiURI(false, folder_id));
            if (res != null) {
                writeCachedFolderNodes(folder_id, res);
            }
        }

        LOG.info("GET LINKS: MEGA FOLDER {} JSON FILE TREE SIZE -> {}", folder_id, MiscTools.formatBytes((long) res.length()));

        ObjectMapper objectMapper = new ObjectMapper();

        HashMap[] res_map = objectMapper.readValue(res, HashMap[].class);

        for (Object o : (Iterable<? extends Object>) res_map[0].get("f")) {

            HashMap<String, Object> node = (HashMap<String, Object>) o;

            String[] node_k = ((String) node.get("k")).split(":");

            if (node_k.length == 2 && !Objects.equals(node_k[0], "") && !Objects.equals(node_k[1], "")) {

                try {
                    String dec_node_k = Bin2UrlBASE64(decryptKey(UrlBASE642Bin(node_k[1]), _urlBase64KeyDecode(folder_key)));
                    if (file_ids.contains((String) node.get("h"))) {
                        nLinks.add("https://mega.nz/#N!" + (node.get("h")) + "!" + dec_node_k + "###n=" + folder_id);
                    }

                } catch (Exception e) {
                    LOG.warn("WARNING: Cannot get links, node key is not valid [1] {} {}", node.get("k"), folder_key);
                }
            } else {
                LOG.warn("WARNING: Cannot get links, node key is not valid [2] {} {}", node.get("k"), folder_key);
            }

        }

        return nLinks;

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
            LOG.fatal("Failed to b64 decode with key {}! {}", key, ex.getMessage());
        }

        return null;
    }

}
