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
    public static volatile String API_KEY = null;
    // Fallback application key when the user hasn't configured one. This is
    // the public app key shipped in MEGAcmd (meganz/MEGAcmd src/megacmd.cpp).
    // The official MEGA SDK comment says "Applications using the MEGA API must
    // present a valid application key", and indeed without &ak=... MEGA's
    // load balancer 402-throttles aggressively (see GH #614 NPE chain and the
    // mail045.mrface.com / mega.myz.info reports). MegaBasterd was missing
    // this since the original JSON reverse-engineering predated &ak/&v=3.
    public static final String DEFAULT_APP_KEY = "BdARkQSQ";
    public static final int REQ_ID_LENGTH = 10;
    public static final Integer[] MEGA_ERROR_NO_EXCEPTION_CODES = {-1, -3};
    public static final int PBKDF2_ITERATIONS = 100000;
    public static final int PBKDF2_OUTPUT_BIT_LENGTH = 256;
    public static final int MAX_RAW_REQUEST_RETRIES = 30;
    public static final int MAX_THUMBNAIL_UPLOAD_RETRIES = 5;
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

    /**
     * Most recent MEGA API error code observed by RAW_REQUEST for this MegaAPI
     * instance. Lets callers that swallowed the exception (e.g.
     * {@link #getQuota()} returns null on failure) still surface a descriptive
     * popup via {@link MegaErrorMessages#showPopup}. 0 means "no error since
     * reset". Transient codes ({@link #MEGA_ERROR_NO_EXCEPTION_CODES}) are also
     * stored so diagnostics show what just happened. (#751 / D)
     */
    private transient volatile int _last_api_error_code = 0;

    public int getLastApiErrorCode() {
        return _last_api_error_code;
    }

    public void resetLastApiErrorCode() {
        _last_api_error_code = 0;
    }

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

    /**
     * Standard query parameters that MEGA's API expects on every /cs request.
     * Was {@code &v=3 &ak=... &lang=...}; the {@code &v=3} part has been
     * DROPPED. With v=3 MEGA's `p` (put/createDir) endpoint returns the
     * new-protocol async shape {@code [<request_completion_token>, []]}
     * instead of the legacy synchronous {@code [{node_object}]} that
     * MegaBasterd's createDir was written for. The async shape expects the
     * caller to subsequently poll the {@code sc} action-packet channel for
     * the actual node data -- MegaBasterd has no plumbing for that, so the
     * upload silently produced an "Upload aborted" with an opaque Jackson
     * error in the DEBUG LOG (raw body
     * {@code [["!Q|am'a", []]]}). The commit that added v=3 (2ec9de2 in
     * master) claimed no behavioural change; in practice it broke uploads
     * for any account that goes through the createDir path. Keeping
     * &ak= (which fixes MEGA's 402 throttling of unknown clients) and
     * &lang= (which is optional).
     */
    private static String _apiStdParams() {
        StringBuilder sb = new StringBuilder();
        String ak = (API_KEY != null && !API_KEY.isEmpty()) ? API_KEY : DEFAULT_APP_KEY;
        sb.append("&ak=").append(ak);
        String lang = MainPanel.getLanguage();
        if (lang != null && !lang.isEmpty()) {
            sb.append("&lang=").append(lang);
        }
        return sb.toString();
    }

    /**
     * Context tag for log lines. Includes the thread name and the account email
     * if known (login() sets _full_email; before login this is null). Used so
     * that bug reports with multiple accounts can be diagnosed.
     */
    private String _ctx() {
        String who = _full_email != null ? _full_email : (_email != null ? _email : "(pre-login)");
        return Thread.currentThread().getName() + " account=" + who;
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

        URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + _apiStdParams());

        String res = RAW_REQUEST(request, url_api);

        ObjectMapper objectMapper = new ObjectMapper();

        HashMap[] res_map = objectMapper.readValue(res, HashMap[].class);

        // Defensive parsing: historically these three fields (k, privk, csid)
        // were always present in the `us` response. Recent MEGA accounts that
        // have only ever been touched via the official SDK (MEGAcmd/MEGAsync)
        // sometimes come back without one or more of them, or with the
        // master key embedded in a signed `keys` blob instead of plain `k`.
        // Without these explicit checks, the legacy code below explodes with
        // an opaque NPE inside UrlBASE642Bin / decryptKey and the user has no
        // way to know which field was missing. Surface the shape of the
        // response (redacted) so the cause is diagnosable from the log.
        String k = (String) res_map[0].get("k");
        String privk = (String) res_map[0].get("privk");
        String csid = (String) res_map[0].get("csid");

        if (k == null || privk == null || csid == null) {
            LOG.log(Level.SEVERE, "{0} `us` response is missing required field(s). Response shape: {1}",
                    new Object[]{_ctx(), _describeLoginResponse(res_map[0])});

            if (k == null && res_map[0].get("keys") != null) {
                // Account v3: master key is wrapped inside a signed Ed25519/Cu25519
                // `keys` blob, not exposed as plain `k`. The legacy reverse-engineered
                // path here can't decode it; only the official SDK can. This is a
                // strong candidate for the "cuentas que solo logean tras pasar por
                // MEGAcmd" bug.
                throw new MegaAPIException(-1001, "MEGA returned a `keys` blob (account v3) but no plain `k` field. "
                        + "This account format is not supported by MegaBasterd's legacy login path. "
                        + "Workaround: log in once via MEGAcmd or MEGAsync, then try again.");
            }

            if (k == null) {
                throw new MegaAPIException(-1002, "MEGA `us` response is missing `k` (encrypted master key). Account may not be fully bootstrapped on the server side.");
            }
            if (privk == null) {
                throw new MegaAPIException(-1003, "MEGA `us` response is missing `privk` (encrypted RSA private key). Account RSA keypair may not be published yet.");
            }
            if (csid == null) {
                throw new MegaAPIException(-1004, "MEGA `us` response is missing `csid` (encrypted session id). Session establishment failed.");
            }
        }

        _master_key = bin2i32a(decryptKey(UrlBASE642Bin(k), i32a2bin(_password_aes)));

        int[] enc_rsa_priv_key = bin2i32a(UrlBASE642Bin(privk));

        byte[] privk_byte = decryptKey(i32a2bin(enc_rsa_priv_key), i32a2bin(_master_key));

        _rsa_priv_key = _extractRSAPrivKey(privk_byte);

        byte[] raw_sid = rsaDecrypt(mpi2big(UrlBASE642Bin(csid)), _rsa_priv_key[0], _rsa_priv_key[1], _rsa_priv_key[2]);

        _sid = Bin2UrlBASE64(Arrays.copyOfRange(raw_sid, 0, 43));

        fetchNodes();
    }

    /**
     * Build a redacted summary of a login response so we can diagnose why a
     * login failed without leaking the encrypted key material itself. Reports
     * which keys are present and their lengths. Used only on the error path of
     * _realLogin().
     */
    private static String _describeLoginResponse(HashMap response) {
        if (response == null) {
            return "(null response)";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        // Iterate in insertion order so the log is stable across runs.
        for (Object keyObj : response.keySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            String key = String.valueOf(keyObj);
            Object val = response.get(keyObj);
            if (val == null) {
                sb.append(key).append("=null");
            } else if (val instanceof String) {
                // Don't leak the value, just its length. Long strings here
                // are all encrypted blobs (k, privk, csid, keys, pubk, ...).
                sb.append(key).append("=str(").append(((String) val).length()).append(")");
            } else if (val instanceof Number) {
                // Scalars like `v` (account version) are safe to log verbatim.
                sb.append(key).append("=").append(val);
            } else {
                sb.append(key).append("=").append(val.getClass().getSimpleName());
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private void _readAccountVersionAndSalt() throws Exception {

        String request = "[{\"a\":\"us0\",\"user\":\"" + _email + "\"}]";

        URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + _apiStdParams());

        String res = RAW_REQUEST(request, url_api);

        ObjectMapper objectMapper = new ObjectMapper();

        HashMap[] res_map = objectMapper.readValue(res, HashMap[].class);

        _account_version = (Integer) res_map[0].get("v");

        _salt = (String) res_map[0].get("s");

    }

    public boolean check2FA(String email) throws Exception {

        // Strip MegaBasterd's local-only "#alias" suffix before talking to
        // MEGA. The mfag endpoint rejects "bob@mail.com#whatever" as an
        // unknown user (-9), which propagated up as MegaAPIException and
        // failed the save/edit of any aliased account. login() and
        // fastLogin() already do this split; check2FA had been missing it
        // since 5.81 (2019). See issue #737.
        String[] email_split = email.split(" *# *");

        String request = "[{\"a\":\"mfag\",\"e\":\"" + email_split[0] + "\"}]";

        URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + _apiStdParams());

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

            url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : "") + _apiStdParams());

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

            LOG.log(Level.SEVERE, _ctx(), ex);
        }

        return quota;
    }

    public void fetchNodes() throws IOException {

        String request = "[{\"a\":\"f\", \"c\":1}]";

        URL url_api;

        try {

            url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : "") + _apiStdParams());

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
            LOG.log(Level.SEVERE, _ctx(), ex);
        }

    }

    private String RAW_REQUEST(String request, URL url_api) throws MegaAPIException {

        String response = null, current_smart_proxy = null;

        int mega_error = 0, http_error = 0, conta_error = 0, http_status;

        boolean empty_response = false, smart_proxy_socks = false;

        // X-Hashcash state for MEGA's anti-bot proof-of-work challenge.
        // When MEGA responds 402 with an X-Hashcash header, we solve the
        // challenge and re-send the exact same POST with X-Hashcash set
        // to the solution. See HashcashSolver and meganz/sdk's
        // src/hashcash.cpp. Without this, accounts that MEGA decides to
        // challenge (heuristic on IP/UA/account state) fail to log in
        // unless a real SDK client (MEGAcmd, MEGAsync) has recently
        // touched the same IP and "warmed" the rate-limiter.
        String pending_hashcash_header = null;
        boolean hashcash_just_solved = false;

        HttpsURLConnection con = null;

        ArrayList<String> excluded_proxy_list = new ArrayList<>();

        do {

            SmartMegaProxyManager proxy_manager = MainPanel.getProxy_manager();

            try {

                // SmartProxy routing condition for the /cs endpoint. Was
                // keyed off HTTP 509 only; -17 (MEGA EOVERQUOTA, in the JSON
                // body of an HTTP 200) was never reachable here because the
                // mega_error check further down lobbed it straight up as an
                // exception. With the "synthesise 509 on -17 when SmartProxy
                // is enabled" branch added below, http_error == 509 now also
                // covers the JSON-body quota case, so we don't need an extra
                // disjunct here. (#760)
                if ((current_smart_proxy != null || http_error == 509) && MainPanel.isUse_smart_proxy() && proxy_manager != null && !MainPanel.isUse_proxy()) {

                    if (current_smart_proxy != null && (http_error != 0 || empty_response)) {

                        proxy_manager.blockProxy(current_smart_proxy, "HTTP " + String.valueOf(http_error));

                        // getProxy() returns null when every proxy is banned /
                        // excluded and the refresh-retry loop is exhausted.
                        // Indexing [0]/[1] unconditionally NPEs the RAW_REQUEST
                        // -- exactly during a 509 storm, which is when SmartProxy
                        // is supposed to save us. Same defensive pattern as
                        // ChunkDownloader.java:362+. (#752)
                        String[] smart_proxy = proxy_manager.getProxy(excluded_proxy_list);
                        if (smart_proxy != null) {
                            current_smart_proxy = smart_proxy[0];
                            smart_proxy_socks = smart_proxy[1].equals("socks");
                        } else {
                            LOG.log(Level.WARNING, "{0} SmartProxy exhausted (every proxy excluded/banned) -- falling back to direct for this /cs retry", _ctx());
                            current_smart_proxy = null;
                        }

                    } else if (current_smart_proxy == null) {

                        String[] smart_proxy = proxy_manager.getProxy(excluded_proxy_list);
                        if (smart_proxy != null) {
                            current_smart_proxy = smart_proxy[0];
                            smart_proxy_socks = smart_proxy[1].equals("socks");
                        } else {
                            LOG.log(Level.WARNING, "{0} SmartProxy exhausted (no usable proxy) -- falling back to direct for this /cs retry", _ctx());
                            current_smart_proxy = null;
                        }
                    }

                    if (current_smart_proxy != null) {

                        // Parse the proxy entry defensively. A garbage entry
                        // would otherwise throw NumberFormatException out of
                        // the try/catch (only IOException/SSLException are
                        // caught) and kill the RAW_REQUEST. Treat malformed
                        // as banned + direct fallback for this retry. Mirrors
                        // ChunkDownloader.java:400+. (#752)
                        String[] proxy_info = current_smart_proxy.split(":");
                        int proxy_port = -1;
                        if (proxy_info.length == 2) {
                            try {
                                int p = Integer.parseInt(proxy_info[1]);
                                if (p >= 1 && p <= 65535) {
                                    proxy_port = p;
                                }
                            } catch (NumberFormatException ignore) {
                            }
                        }

                        if (proxy_port < 0) {
                            LOG.log(Level.WARNING, "{0} malformed smart proxy entry {1} -- banning + direct fallback", new Object[]{_ctx(), current_smart_proxy});
                            proxy_manager.blockProxy(current_smart_proxy, "Malformed entry");
                            excluded_proxy_list.add(current_smart_proxy);
                            current_smart_proxy = null;
                            con = (HttpsURLConnection) url_api.openConnection();
                        } else {
                            Proxy proxy = new Proxy(smart_proxy_socks ? Proxy.Type.SOCKS : Proxy.Type.HTTP, new InetSocketAddress(proxy_info[0], proxy_port));
                            con = (HttpsURLConnection) url_api.openConnection(proxy);
                        }

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

                // If we just solved a hashcash challenge in the previous
                // iteration, attach the solution. MEGA will only honor it
                // on the immediate retry; we clear it after sending so
                // subsequent unrelated retries don't keep sending stale
                // solutions.
                if (pending_hashcash_header != null) {
                    con.setRequestProperty("X-Hashcash", pending_hashcash_header);
                }

                con.setUseCaches(false);

                con.setRequestMethod("POST");

                con.setDoOutput(true);

                con.getOutputStream().write(request.getBytes("UTF-8"));

                con.getOutputStream().close();

                http_status = con.getResponseCode();

                hashcash_just_solved = false;

                if (http_status != 200) {

                    LOG.log(Level.WARNING, "{0} request body: {1}  url: {2}", new Object[]{_ctx(), _redactSensitive(request), _redactUrl(url_api.toString())});

                    LOG.log(Level.WARNING, "{0} HTTP error: {1}", new Object[]{_ctx(), http_status});

                    http_error = http_status;

                    // 402 with X-Hashcash means MEGA wants proof-of-work
                    // before processing this request. Solve it now and
                    // queue the solution for the next iteration. If the
                    // previous request already carried a solution (i.e.
                    // we just retried with hashcash and STILL got 402),
                    // clear the pending header so we don't keep replaying
                    // a wrong/stale solution -- MEGA will issue a fresh
                    // challenge.
                    if (http_status == 402) {
                        String challenge = con.getHeaderField("X-Hashcash");
                        pending_hashcash_header = null;
                        if (challenge != null && !challenge.isEmpty()) {
                            try {
                                pending_hashcash_header = HashcashSolver.buildSolutionHeader(challenge);
                                hashcash_just_solved = true;
                                LOG.log(Level.INFO, "{0} solved X-Hashcash challenge, retrying immediately", _ctx());
                            } catch (Exception hc_ex) {
                                LOG.log(Level.WARNING, _ctx() + " failed to solve X-Hashcash challenge", hc_ex);
                            }
                        }
                    }

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

                            if (mega_error != 0) {
                                // Stash for callers that swallow the exception
                                // (e.g. getQuota returns null on failure) so a
                                // UI hook can pop a friendly description. (#751 / D)
                                _last_api_error_code = mega_error;
                            }

                            if (mega_error != 0 && !Arrays.asList(MEGA_ERROR_NO_EXCEPTION_CODES).contains(mega_error)) {

                                // -17 EOVERQUOTA with SmartProxy active: the
                                // bandwidth-quota that drives -17 on anonymous
                                // link downloads (the common case in
                                // MegaBasterd) is tied to the egress IP, not
                                // the account. Bubbling -17 up here would
                                // trip Download.getMegaFileDownloadUrl ->
                                // stopDownloader -> 60 s auto-retry countdown
                                // -> restart -> -17 again, forever, with
                                // SmartProxy never engaged because that path
                                // never reached the ChunkDownloader where the
                                // wake-from-backoff fix landed for #758.
                                //
                                // Treat -17 as a synthetic HTTP 509 instead:
                                // both the SmartProxy enable condition above
                                // and the do-while continue condition below
                                // already key off http_error == 509, so the
                                // next iteration of this loop picks a proxy
                                // and reissues the /cs request through it.
                                // Bounded by MAX_RAW_REQUEST_RETRIES so a
                                // genuinely dead pool (or an account-scoped
                                // -17 that SmartProxy can't bypass) still
                                // throws within ~30 attempts. (#760)
                                if (mega_error == -17
                                        && MainPanel.isUse_smart_proxy()
                                        && proxy_manager != null
                                        && !MainPanel.isUse_proxy()
                                        && conta_error < MAX_RAW_REQUEST_RETRIES) {

                                    LOG.log(Level.WARNING, "{0} MEGA -17 EOVERQUOTA; SmartProxy enabled -- rotating /cs through proxy pool (attempt {1}/{2})",
                                            new Object[]{_ctx(), conta_error + 1, MAX_RAW_REQUEST_RETRIES});

                                    // Flip http_error so the routing /
                                    // continue predicates trigger. Clear the
                                    // parsed body so the post-loop return
                                    // doesn't hand a -17-bearing response
                                    // back to the caller. Bump conta_error
                                    // explicitly because the backoff branch
                                    // below is gated on http_error != 509
                                    // and would otherwise leave the counter
                                    // at zero forever.
                                    http_error = 509;
                                    response = null;
                                    empty_response = false;
                                    conta_error++;

                                } else {

                                    throw new MegaAPIException(mega_error);
                                }

                            }

                        } else {

                            empty_response = true;
                        }
                    }

                }

            } catch (SSLException ssl_ex) {

                empty_response = true;

                LOG.log(Level.SEVERE, _ctx() + " SSLException on " + _redactUrl(url_api.toString()), ssl_ex);

            } catch (IOException ex) {

                LOG.log(Level.SEVERE, _ctx() + " IOException on " + _redactUrl(url_api.toString()), ex);

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

                if (hashcash_just_solved) {
                    // We resolved the proof-of-work challenge inline. Retry
                    // immediately without exponential backoff -- MEGA is
                    // waiting for us, not throttling us. Also do not bump
                    // conta_error: a successfully-solved challenge is not
                    // a failed attempt.
                    LOG.log(Level.INFO, "{0} retrying with X-Hashcash solution (no backoff)", _ctx());
                } else {
                    LOG.log(Level.WARNING, "{0} retry #{1} (http={2} mega_error={3} empty={4})  waiting backoff...",
                            new Object[]{_ctx(), conta_error + 1, http_error, mega_error, empty_response});

                    try {
                        Thread.sleep(getWaitTimeExpBackOff(conta_error++) * 1000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        LOG.log(Level.FINE, "{0} retry sleep interrupted", _ctx());
                    }
                }
            }

        } while ((http_error == 402 || http_error == 500 || http_error == 503 || empty_response || mega_error != 0 || (http_error == 509 && MainPanel.isUse_smart_proxy() && !MainPanel.isUse_proxy()))
                && conta_error < MAX_RAW_REQUEST_RETRIES);

        if (response == null) {
            // The retry loop covers 402 (MEGA throttle / load-balancer
            // back-pressure), 500 internal, 503 service unavailable, empty
            // response, mega_error, and 509-with-smart-proxy. Anything else
            // (403 forbidden, 404 not found, persistent 402 past the cap, ...)
            // falls through. Surface as a MegaAPIException.
            LOG.log(Level.WARNING, "{0} RAW_REQUEST giving up: http={1} mega_error={2} retries={3} url={4}",
                    new Object[]{_ctx(), http_error, mega_error, conta_error, _redactUrl(url_api.toString())});
            // Keep the exception code in the MEGA-protocol space (mega_error
            // when set, else -1). Smuggling HTTP-status as a negative
            // MEGA code (e.g. -402) collided with real MEGA codes and
            // confused callers that match against FATAL_API_ERROR_CODES.
            int code = mega_error != 0 ? mega_error : -1;
            throw new MegaAPIException(code, "MEGA API request failed (HTTP " + http_error + ", account=" + (_full_email != null ? _full_email : "(pre-login)") + ")");
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

            url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : "") + _apiStdParams() + "&n=" + folder_id);

        } else {

            request = "[{\"a\":\"g\", \"g\":\"1\", \"p\":\"" + file_id + "\"}]";
            url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : "") + _apiStdParams());
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

            url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : "") + _apiStdParams() + "&n=" + folder_id);

        } else {

            request = "[{\"a\":\"g\", \"p\":\"" + file_id + "\"}]";

            url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : "") + _apiStdParams());
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
            LOG.log(Level.SEVERE, _ctx(), ex);
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
            LOG.log(Level.SEVERE, _ctx(), ex);
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

            URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : "") + _apiStdParams());

            String res = RAW_REQUEST(request, url_api);

            ObjectMapper objectMapper = new ObjectMapper();

            HashMap[] res_map = objectMapper.readValue(res, HashMap[].class);

            ul_url = (String) res_map[0].get("p");

        } catch (MegaAPIException mae) {

            throw mae;

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, _ctx(), ex);
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

            // No "ssl":1 here on purpose: the attribute (thumbnail) data is already AES-CBC encrypted
            // with the file key, so it travels over plain HTTP just like the file chunks. Forcing TLS
            // against MEGA's gfs storage nodes was the only HTTPS hop in the upload pipeline and caused
            // intermittent SSLHandshakeException (handshake_failure) that silently dropped thumbnails.
            String request = "[{\"a\":\"ufa\", \"s\":" + String.valueOf(file_bytes[0].length) + "}, {\"a\":\"ufa\", \"s\":" + String.valueOf(file_bytes[1].length) + "}]";

            URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : "") + _apiStdParams());

            String res = RAW_REQUEST(request, url_api);

            ObjectMapper objectMapper = new ObjectMapper();

            HashMap[] res_map = objectMapper.readValue(res, HashMap[].class);

            ul_url[0] = (String) res_map[0].get("p");

            ul_url[1] = (String) res_map[1].get("p");

            int h = 0;

            for (String u : ul_url) {

                URL url = new URL(u);

                int conta_error = 0;

                while (true) {

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

                        break;

                    } catch (IOException ex) {

                        if (upload.isStopped() || ++conta_error >= MAX_THUMBNAIL_UPLOAD_RETRIES) {
                            throw ex;
                        }

                        long wait_time = MiscTools.getWaitTimeExpBackOff(conta_error);

                        LOG.log(Level.WARNING, "{0} Thumbnail upload error ({1}), retrying in {2} secs... ({3}/{4})", new Object[]{Thread.currentThread().getName(), ex.getMessage(), wait_time, conta_error, MAX_THUMBNAIL_UPLOAD_RETRIES});

                        MiscTools.pausar(wait_time * 1000);

                    } finally {
                        if (con != null) {
                            con.disconnect();
                        }
                    }
                }

                h++;
            }

            request = "[{\"a\":\"pfa\", \"fa\":\"0*" + hash[0] + "/1*" + hash[1] + "\", \"n\":\"" + node_handle + "\"}]";

            url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : "") + _apiStdParams());

            res = RAW_REQUEST(request, url_api);

            objectMapper = new ObjectMapper();

            String[] resp = objectMapper.readValue(res, String[].class);

            return (String) resp[0];

        } catch (MegaAPIException mae) {

            throw mae;

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, _ctx(), ex);
        }

        return "";
    }

    public HashMap<String, Object> finishUploadFile(String fbasename, int[] ul_key, int[] fkey, int[] meta_mac, String completion_handle, String mega_parent, byte[] master_key, String root_node, byte[] share_key) throws MegaAPIException {

        HashMap[] res_map = null;

        try {

            byte[] enc_att = _encAttr("{\"n\":\"" + fbasename + "\"}", i32a2bin(Arrays.copyOfRange(ul_key, 0, 4)));

            URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : "") + _apiStdParams());

            String request = "[{\"a\":\"p\", \"t\":\"" + mega_parent + "\", \"n\":[{\"h\":\"" + completion_handle + "\", \"t\":0, \"a\":\"" + Bin2UrlBASE64(enc_att) + "\", \"k\":\"" + Bin2UrlBASE64(encryptKey(i32a2bin(fkey), master_key)) + "\"}], \"i\":\"" + _req_id + "\", \"cr\" : [ [\"" + root_node + "\"] , [\"" + completion_handle + "\"] , [0,0, \"" + Bin2UrlBASE64(encryptKey(i32a2bin(fkey), share_key)) + "\"]]}]";

            String res = RAW_REQUEST(request, url_api);

            ObjectMapper objectMapper = new ObjectMapper();

            res_map = objectMapper.readValue(res, HashMap[].class);

        } catch (MegaAPIException mae) {

            throw mae;

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, _ctx(), ex);
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

            URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : "") + _apiStdParams());

            String request = "[{\"a\":\"p\", \"t\":\"" + parent_node + "\", \"n\":[{\"h\":\"xxxxxxxx\",\"t\":1,\"a\":\"" + Bin2UrlBASE64(enc_att) + "\",\"k\":\"" + Bin2UrlBASE64(enc_node_key) + "\"}],\"i\":\"" + _req_id + "\"}]";

            String res = RAW_REQUEST(request, url_api);

            // Handle the per-target wrapped error shape `[[-N]]` that MEGA can
            // return for "p" requests (e.g. parent node was deleted between
            // login and createDir). checkMEGAError in RAW_REQUEST only matches
            // top-level `-N` / `[-N]`, so a `[[-9]]` slipped through to Jackson
            // and exploded with a MismatchedInputException + visible stack
            // trace, surfacing as a confusing "uploads fail" symptom.
            int wrapped = _checkWrappedMEGAError(res);
            if (wrapped != 0) {
                _last_api_error_code = wrapped;
                LOG.log(Level.WARNING, "{0} createDir: MEGA returned wrapped error {1}",
                        new Object[]{_ctx(), wrapped});
                return null;
            }

            ObjectMapper objectMapper = new ObjectMapper();

            try {
                res_map = objectMapper.readValue(res, HashMap[].class);
            } catch (com.fasterxml.jackson.databind.exc.MismatchedInputException jex) {
                // MEGA returned something that doesn't fit `[{...}]`. Common
                // case: the per-target wrapped shape `[[{...}]]` (Jackson 2.18
                // refuses to coerce that into HashMap[], where 2.15 used to
                // muddle through with a partial result). Try unwrapping one
                // level via JsonNode before giving up.
                LOG.log(Level.WARNING, "{0} createDir: HashMap[] parse rejected, attempting one-level unwrap. Raw body (truncated): {1}",
                        new Object[]{_ctx(), _truncateForLog(res, 500)});
                try {
                    com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(res);
                    if (root.isArray() && root.size() > 0 && root.get(0).isArray()) {
                        com.fasterxml.jackson.databind.JsonNode inner = root.get(0);
                        res_map = objectMapper.treeToValue(inner, HashMap[].class);
                        LOG.log(Level.INFO, "{0} createDir: unwrapped one extra MEGA array layer successfully", _ctx());
                    } else {
                        LOG.log(Level.SEVERE, "{0} createDir: unsupported response shape; raw body (truncated): {1}",
                                new Object[]{_ctx(), _truncateForLog(res, 500)});
                    }
                } catch (Exception ex2) {
                    LOG.log(Level.SEVERE, "{0} createDir: unwrap attempt also failed: {1}; raw body (truncated): {2}",
                            new Object[]{_ctx(), ex2.getMessage(), _truncateForLog(res, 500)});
                }
            }

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, _ctx(), ex);
        }

        return res_map != null && res_map.length > 0 ? res_map[0] : null;

    }

    private static String _truncateForLog(String s, int max) {
        if (s == null) {
            return "(null)";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "...(" + (s.length() - max) + " more chars)";
    }

    /**
     * Returns N (negative) if the response is a doubly-wrapped per-target
     * error like {@code [[-9]]}, else 0. Centralised so other "p"-style
     * callers (createDirInsideAnotherSharedDir, finishUploadFile, etc.) can
     * apply the same guard without duplicating the regex. (#751 follow-up)
     */
    static int _checkWrappedMEGAError(String data) {
        if (data == null) {
            return 0;
        }
        String m = findFirstRegex("^\\[\\s*\\[\\s*(\\-[0-9]+)\\s*\\]\\s*\\]$", data, 1);
        if (m == null) {
            return 0;
        }
        try {
            return Integer.parseInt(m);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public HashMap<String, Object> createDirInsideAnotherSharedDir(String name, String parent_node, byte[] node_key, byte[] master_key, String root_node, byte[] share_key) {

        HashMap[] res_map = null;

        try {

            byte[] enc_att = _encAttr("{\"n\":\"" + name + "\"}", node_key);

            byte[] enc_node_key = encryptKey(node_key, master_key);

            byte[] enc_node_key_s = encryptKey(node_key, share_key);

            URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : "") + _apiStdParams());

            String request = "[{\"a\":\"p\", \"t\":\"" + parent_node + "\", \"n\":[{\"h\":\"xxxxxxxx\",\"t\":1,\"a\":\"" + Bin2UrlBASE64(enc_att) + "\",\"k\":\"" + Bin2UrlBASE64(enc_node_key) + "\"}],\"i\":\"" + _req_id + "\", \"cr\" : [ [\"" + root_node + "\"] , [\"xxxxxxxx\"] , [0,0, \"" + Bin2UrlBASE64(enc_node_key_s) + "\"]]}]";

            String res = RAW_REQUEST(request, url_api);

            ObjectMapper objectMapper = new ObjectMapper();

            res_map = objectMapper.readValue(res, HashMap[].class);

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, _ctx(), ex);
        }

        return res_map != null ? res_map[0] : null;

    }

    public String getPublicFileLink(String node, byte[] node_key) {

        String public_link = null;

        try {

            String file_id;

            List res_map;

            String request = "[{\"a\":\"l\", \"n\":\"" + node + "\"}]";

            URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : "") + _apiStdParams());

            String res = RAW_REQUEST(request, url_api);

            ObjectMapper objectMapper = new ObjectMapper();

            res_map = objectMapper.readValue(res, List.class);

            file_id = (String) res_map.get(0);

            public_link = "https://mega.nz/#!" + file_id + "!" + Bin2UrlBASE64(node_key);

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, _ctx(), ex);
        }

        return public_link;
    }

    public String getPublicFolderLink(String node, byte[] node_key) {

        String public_link = null;

        try {

            String folder_id;

            List res_map;

            String request = "[{\"a\":\"l\", \"n\":\"" + node + "\", \"i\":\"" + _req_id + "\"}]";

            URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : "") + _apiStdParams());

            String res = RAW_REQUEST(request, url_api);

            ObjectMapper objectMapper = new ObjectMapper();

            res_map = objectMapper.readValue(res, List.class);

            folder_id = (String) res_map.get(0);

            public_link = "https://mega.nz/#F!" + folder_id + "!" + Bin2UrlBASE64(node_key);

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, _ctx(), ex);
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

            URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + (_sid != null ? "&sid=" + _sid : "") + _apiStdParams());

            return RAW_REQUEST(request, url_api);

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, _ctx(), ex);
        }

        return null;
    }

    public String cryptoHandleauth(String h) {

        String ch = null;

        try {

            ch = Bin2UrlBASE64(encryptKey((h + h).getBytes("UTF-8"), i32a2bin(getMaster_key())));

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, _ctx(), ex);
        }

        return ch;
    }

    private static String _folderCachePath(String folder_id) {
        String safe_id = HashString("sha1", folder_id == null ? "" : folder_id);
        return System.getProperty("java.io.tmpdir") + File.separator + "megabasterd_folder_cache_" + safe_id;
    }

    public static final long FOLDER_CACHE_MAX_AGE_MS = 24L * 60L * 60L * 1000L;

    private static boolean isAlwaysReloadMegaFoldersEnabled() {
        return "yes".equals(DBTools.selectSettingValue("always_reload_mega_folders"));
    }

    public boolean existsCachedFolderNodes(String folder_id) {

        if (isAlwaysReloadMegaFoldersEnabled()) {
            return false;
        }

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
                LOG.log(Level.SEVERE, _ctx(), ex);
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
            LOG.log(Level.SEVERE, _ctx(), ex);
        }
    }

    public HashMap<String, Object> getFolderNodes(String folder_id, String folder_key, JProgressBar bar, boolean cache) throws Exception {

        HashMap<String, Object> folder_nodes = null;
        String res = null;

        if (cache && !isAlwaysReloadMegaFoldersEnabled()) {
            res = getCachedFolderNodes(folder_id);
        }

        if (res == null) {
            String request = "[{\"a\":\"f\", \"c\":\"1\", \"r\":\"1\", \"ca\":\"1\"}]";
            URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + "&n=" + folder_id + _apiStdParams());
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
            } else {
                // Silent skip turned the folder dialog into a partial/empty
                // tree with no clue why; surface a WARNING so users hitting
                // odd "missing files" cases have something to report.
                LOG.log(Level.WARNING, "MEGA FOLDER {0}: node {1} dropped (no segment of k=''{2}'' decrypted ''a'')",
                        new Object[]{folder_id, node.get("h"), full_k});
            }
        }

        return folder_nodes;
    }

    public HashMap<String, Object> getFolderNodes4(String folder_id, String folder_key, JProgressBar bar, boolean cache) throws Exception {

        HashMap<String, Object> folder_nodes = null;
        String res = null;

        if (cache && !isAlwaysReloadMegaFoldersEnabled()) {
            res = getCachedFolderNodes(folder_id);
        }

        if (res == null) {
            String request = "[{\"a\":\"f\", \"c\":\"1\", \"r\":\"1\", \"ca\":\"1\"}]";
            URL url_api = new URL(API_URL + "/cs?id=" + _nextSeqno() + "&n=" + folder_id + _apiStdParams());
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

        // Track which #F* links failed to convert so the caller can decide
        // whether to drop them or report them to the user instead of silently
        // losing the input (previously: if the regex didn't match, folder_id /
        // folder_key / file_id were all null and the link got buried under the
        // sentinel "null:null" key, which then failed at getFolderNodes time
        // and yielded an empty nlinks list -- the caller's removeAll() then
        // wiped the original #F* link with nothing to replace it).
        ArrayList<String> nlinks = new ArrayList<>();
        ArrayList<String> malformed_originals = new ArrayList<>();

        for (String link : links) {

            String folder_id = findFirstRegex("#F\\*[^!]+!([^!]+)", link, 1);

            String folder_key = findFirstRegex("#F\\*[^!]+![^!]+!([^!]+)", link, 1);

            String file_id = findFirstRegex("#F\\*([^!]+)", link, 1);

            if (folder_id == null || folder_key == null || file_id == null) {
                LOG.log(Level.WARNING, "#F* link did not match expected shape, skipping: {0}", link);
                malformed_originals.add(link);
                continue;
            }

            map.computeIfAbsent(folder_id + ":" + folder_key, k -> new ArrayList<>()).add(file_id);
        }

        // If the user is closing the app mid-batch, skip the cache-prompt
        // loop entirely -- the surviving #F* originals are preserved via
        // malformed_originals + the caller's preprocess_global_queue, so
        // they'll be picked up on the next launch.
        MainPanel main_panel = MainPanelView.getINSTANCE() != null ? MainPanelView.getINSTANCE().getMain_panel() : null;
        boolean exiting = main_panel != null && main_panel.isExit();

        for (Map.Entry<String, ArrayList<String>> entry : map.entrySet()) {

            if (exiting) {
                break;
            }

            String[] folder_parts = entry.getKey().split(":");

            int r = -1;

            if (existsCachedFolderNodes(folder_parts[0])) {
                final int[] rr = {-1};
                final String fid = folder_parts[0];
                MiscTools.GUIRunAndWait(() -> {
                    rr[0] = JOptionPane.showConfirmDialog(MainPanelView.getINSTANCE(), I18n.tr("ui.confirm.folder_cache.message_with_id", fid), I18n.tr("ui.confirm.folder_cache.title"), JOptionPane.YES_NO_OPTION);
                });
                r = rr[0];
            }

            try {
                nlinks.addAll(getNLinksFromFolder(folder_parts[0], folder_parts[1], entry.getValue(), (r == 0)));
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, _ctx(), ex);
            }

        }

        // Echo back malformed originals so the caller's
        // urls.removeAll(folder_file_links); urls.addAll(nlinks)
        // pattern does not silently destroy them.
        nlinks.addAll(malformed_originals);

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
                } else {
                    // Silently dropping a file id the user explicitly pasted
                    // (because MEGA didn't return it, e.g. the file was
                    // deleted or the folder key didn't unwrap it) is the
                    // exact thing that makes folder-link downloads "look
                    // like nothing happened". Surface it.
                    LOG.log(Level.WARNING, "MEGA folder {0}: file id {1} not present or has no key, dropped",
                            new Object[]{folder_id, file_id});
                }
            }

        } else {

            throw new Exception("getFolderNodes returned null for folder " + folder_id);
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
            LOG.log(Level.SEVERE, _ctx(), ex);
        }

        return null;
    }

}
