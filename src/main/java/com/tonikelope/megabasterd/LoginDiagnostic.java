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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HttpsURLConnection;

/**
 * Standalone forensic login tool. For each account in a txt file, replays
 * the full MegaBasterd login flow at the HTTP level and dumps everything
 * that came back: request body (redacted), URL (redacted), HTTP status,
 * response headers, response body (redacted), parsed JSON with the shape
 * and field lengths, derivation method used, timings. One JSON file per
 * account, so you can diff a working account against a failing one and
 * see exactly what's different in MEGA's response.
 *
 * The tool does NOT decrypt anything past the response — its job is to
 * capture what came back, not to validate the session. It also does not
 * touch MegaBasterd's production login path, so a failure in here never
 * affects the GUI.
 *
 * Run:
 *   java -cp MegaBasterd-8.25-jar-with-dependencies.jar \
 *        com.tonikelope.megabasterd.LoginDiagnostic accounts.txt [outdir]
 *
 * accounts.txt:
 *   # comments
 *   user@example.com:password
 *   user@example.com:password:123456
 *
 * Output:
 *   outdir/summary.txt                   one line per account
 *   outdir/01-u***r@example.com.json     full capture for account #1
 *   outdir/02-...
 *
 * Redaction rules:
 *   - password: never recorded, anywhere.
 *   - mfa (pincode) in request body: "[REDACTED]".
 *   - uh (user_hash) in request body: "[REDACTED-len-N]".
 *   - sid in URL: "[REDACTED]".
 *   - sensitive blobs in response (k, privk, csid, keys, pubk, sek, kc,
 *     pubkc, sigPubkCu25519, sigPubkEd25519): replaced with
 *     "[REDACTED-len-N-sha8-XXXXXXXX]". sha8 lets you spot when two
 *     accounts have an identical blob (would be a bug).
 *   - everything else (v, ach, flags, mcsm, ts, ...): kept verbatim.
 *
 * Emails are also partially redacted in the report (u***r@dominio.com).
 *
 * @author tonikelope
 */
public final class LoginDiagnostic {

    private static final long SLEEP_BETWEEN_ACCOUNTS_MS = 2000L;
    private static final int CONSECUTIVE_509_ABORT = 3;
    private static final int REQ_ID_LENGTH = 10;
    private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:61.0) Gecko/20100101 Firefox/61.0";

    /**
     * Response fields whose values are encrypted blobs or session material.
     * Recorded as "[REDACTED-len-N-sha8-XXXXXXXX]" so they cannot leak
     * anything useful but their length and identity (sha8) survive for
     * comparison between accounts.
     */
    private static final Set<String> SENSITIVE_RESPONSE_KEYS = new HashSet<>(Arrays.asList(
            "k", "privk", "csid", "keys", "pubk", "sek", "kc", "pubkc",
            "sigPubkCu25519", "sigPubkEd25519", "ach", "tsid", "sid"
    ));

    private static final SecureRandom RNG = new SecureRandom();
    private static long seqno = RNG.nextLong() & 0xffffffffL;

    private LoginDiagnostic() {
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -cp <jar> com.tonikelope.megabasterd.LoginDiagnostic <accounts.txt> [outdir]");
            System.err.println("       Each line of accounts.txt: email:password   or   email:password:pincode");
            System.exit(2);
        }
        Path accountsFile = Paths.get(args[0]);
        Path outDir = args.length >= 2
                ? Paths.get(args[1])
                : Paths.get("login-diagnostic-out-" + System.currentTimeMillis());

        List<String> rawLines;
        try {
            rawLines = Files.readAllLines(accountsFile, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            System.err.println("Cannot read " + accountsFile + ": " + ex.getMessage());
            System.exit(1);
            return;
        }

        try {
            Files.createDirectories(outDir);
        } catch (IOException ex) {
            System.err.println("Cannot create output dir " + outDir + ": " + ex.getMessage());
            System.exit(1);
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        List<String> summary = new ArrayList<>();
        summary.add("MegaBasterd login forensic report");
        summary.add("api: " + MegaAPI.API_URL);
        summary.add("started: " + Instant.now());
        summary.add("");

        int total = 0, captured = 0, http_failures = 0, consecutive509 = 0;

        for (String raw : rawLines) {
            String line = raw == null ? "" : raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            // Accept both `#` and `:` as separators because account dumps in
            // the wild use either. We pick the first one that appears AFTER
            // the email's `@` so the local part of the address can't be
            // mistaken for the password separator.
            int at = line.indexOf('@');
            int sep = -1;
            char sepChar = 0;
            if (at >= 0) {
                int sHash = line.indexOf('#', at);
                int sColon = line.indexOf(':', at);
                if (sHash >= 0 && (sColon < 0 || sHash < sColon)) {
                    sep = sHash;
                    sepChar = '#';
                } else if (sColon >= 0) {
                    sep = sColon;
                    sepChar = ':';
                }
            }
            if (sep < 0) {
                summary.add("[SKIP] malformed line (no separator)");
                continue;
            }
            String email = line.substring(0, sep).trim();
            String rest = line.substring(sep + 1);
            String password;
            String pincode = null;
            int sep2 = rest.indexOf(sepChar);
            if (sep2 < 0) {
                password = rest;
            } else {
                password = rest.substring(0, sep2);
                pincode = rest.substring(sep2 + 1).trim();
            }
            if (pincode != null && pincode.isEmpty()) {
                pincode = null;
            }

            total++;
            String redactedEmail = redactEmail(email);
            String tag = String.format("[%02d] %s", total, redactedEmail);
            System.out.println(tag + " capturing...");

            Map<String, Object> report = captureAccount(email, password, pincode);
            captured++;

            // Write per-account JSON
            String safeEmail = redactedEmail.replaceAll("[^A-Za-z0-9._-]", "_");
            Path jsonPath = outDir.resolve(String.format("%02d-%s.json", total, safeEmail));
            try {
                mapper.writeValue(jsonPath.toFile(), report);
            } catch (IOException ex) {
                System.err.println("Could not write " + jsonPath + ": " + ex.getMessage());
            }

            // Summary line
            String summaryLine = formatSummaryLine(tag, report);
            summary.add(summaryLine);
            System.out.println("    " + summaryLine);

            // Stop bombarding MEGA if we're being rate-limited consistently.
            if (Boolean.TRUE.equals(report.get("any_http_failure"))) {
                http_failures++;
                if (containsHttp509(report)) {
                    consecutive509++;
                    if (consecutive509 >= CONSECUTIVE_509_ABORT) {
                        String abort = "ABORTING: " + CONSECUTIVE_509_ABORT + " consecutive 509 from MEGA (rate-limited). Wait and resume later.";
                        summary.add(abort);
                        System.err.println(abort);
                        break;
                    }
                } else {
                    consecutive509 = 0;
                }
            } else {
                consecutive509 = 0;
            }

            try {
                Thread.sleep(SLEEP_BETWEEN_ACCOUNTS_MS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        summary.add("");
        summary.add("total: " + total + " | captured: " + captured + " | http_failures: " + http_failures);
        summary.add("finished: " + Instant.now());
        try {
            Files.write(outDir.resolve("summary.txt"), summary, StandardCharsets.UTF_8);
            System.out.println("\nReport written to " + outDir.toAbsolutePath());
        } catch (IOException ex) {
            System.err.println("Cannot write summary: " + ex.getMessage());
        }
    }

    /**
     * Capture every step of the login flow for one account. Returns a map
     * that JSON-serializes cleanly and contains: request bodies (redacted),
     * URLs (redacted), HTTP statuses, response headers, response bodies
     * (redacted), parsed JSON of the response (with sensitive blobs
     * replaced), key-derivation method used, timings, and a final
     * diagnosis hint.
     */
    private static Map<String, Object> captureAccount(String email, String password, String pincode) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("email_redacted", redactEmail(email));
        r.put("has_pincode", pincode != null);
        r.put("timestamp_start", Instant.now().toString());
        r.put("api_url", MegaAPI.API_URL);
        r.put("user_agent", USER_AGENT);
        r.put("api_key", currentAppKey());

        boolean anyHttpFailure = false;

        // ---- Step 1: us0 (prelogin -- get account version + salt) ----
        String us0Body = "[{\"a\":\"us0\",\"user\":\"" + email + "\"}]";
        String us0Url = MegaAPI.API_URL + "/cs?id=" + nextSeqno() + stdParams();
        Map<String, Object> step1 = doHttpRequest("us0", us0Url, us0Body, null);
        r.put("step_1_us0", step1);
        if (Boolean.TRUE.equals(step1.get("http_failure"))) {
            anyHttpFailure = true;
        }

        Integer accountVersion = null;
        String salt = null;
        Object us0Parsed = step1.get("_raw_parsed_DO_NOT_SERIALIZE");
        if (us0Parsed instanceof List && !((List<?>) us0Parsed).isEmpty()
                && ((List<?>) us0Parsed).get(0) instanceof Map) {
            Map<?, ?> us0Obj = (Map<?, ?>) ((List<?>) us0Parsed).get(0);
            Object vObj = us0Obj.get("v");
            if (vObj instanceof Integer) {
                accountVersion = (Integer) vObj;
            }
            Object sObj = us0Obj.get("s");
            if (sObj instanceof String) {
                salt = (String) sObj;
            }
        }

        // ---- Step 2: derive password_aes and user_hash ----
        Map<String, Object> step2 = new LinkedHashMap<>();
        step2.put("account_version", accountVersion);
        step2.put("salt_len", salt == null ? null : salt.length());
        String userHash = null;
        try {
            if (accountVersion == null) {
                step2.put("error", "us0 did not return account version; cannot derive");
            } else if (accountVersion == 1) {
                step2.put("method", "v1: MEGAPrepareMasterKey + MEGAUserHash (legacy stringhash)");
                int[] passwordAes = CryptTools.MEGAPrepareMasterKey(MiscTools.bin2i32a(password.getBytes(StandardCharsets.UTF_8)));
                userHash = CryptTools.MEGAUserHash(email.toLowerCase().getBytes(StandardCharsets.UTF_8), passwordAes);
            } else if (accountVersion == 2) {
                step2.put("method", "v2: PBKDF2-HMAC-SHA512 100k iter, split 16+16");
                if (salt == null) {
                    step2.put("error", "v2 account but salt missing in us0 response");
                } else {
                    byte[] pbkdf2 = CryptTools.PBKDF2HMACSHA512(password, MiscTools.UrlBASE642Bin(salt),
                            MegaAPI.PBKDF2_ITERATIONS, MegaAPI.PBKDF2_OUTPUT_BIT_LENGTH);
                    userHash = MiscTools.Bin2UrlBASE64(Arrays.copyOfRange(pbkdf2, 16, 32));
                }
            } else {
                step2.put("method", "UNKNOWN ACCOUNT VERSION");
                step2.put("error", "Account version " + accountVersion + " is not handled by MegaBasterd's login path");
            }
            step2.put("user_hash_len", userHash == null ? null : userHash.length());
        } catch (Exception ex) {
            step2.put("exception", ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
        r.put("step_2_derive", step2);

        // ---- Step 3: us (the actual login) ----
        Map<String, Object> step3;
        if (userHash == null) {
            step3 = new LinkedHashMap<>();
            step3.put("skipped", "no user_hash, cannot send `us`");
        } else {
            String usBody;
            if (pincode != null) {
                usBody = "[{\"a\":\"us\", \"mfa\":\"" + pincode + "\", \"user\":\"" + email + "\",\"uh\":\"" + userHash + "\"}]";
            } else {
                usBody = "[{\"a\":\"us\",\"user\":\"" + email + "\",\"uh\":\"" + userHash + "\"}]";
            }
            String usUrl = MegaAPI.API_URL + "/cs?id=" + nextSeqno() + stdParams();
            step3 = doHttpRequest("us", usUrl, usBody, userHash);
            if (Boolean.TRUE.equals(step3.get("http_failure"))) {
                anyHttpFailure = true;
            }
        }
        r.put("step_3_us", step3);

        // ---- Diagnosis: based on what came back, would MegaBasterd succeed? ----
        Map<String, Object> diag = diagnose(step1, step3);
        r.put("diagnosis", diag);

        // Strip the raw-parsed stashes before returning. These were only kept
        // for the diagnosis logic above; serializing them would leak the
        // encrypted blob values to the report file.
        step1.remove("_raw_parsed_DO_NOT_SERIALIZE");
        step3.remove("_raw_parsed_DO_NOT_SERIALIZE");

        r.put("any_http_failure", anyHttpFailure);
        r.put("timestamp_end", Instant.now().toString());
        return r;
    }

    /**
     * Issue a single HTTP POST and capture everything we can see about the
     * request and the response, with sensitive material redacted. If the
     * server responds 402 with an X-Hashcash header we solve the
     * proof-of-work and transparently retry the same POST once, recording
     * both attempts so the report shows the hashcash interaction.
     */
    private static Map<String, Object> doHttpRequest(String label, String url, String body, String userHashToRedact) {
        return doHttpRequest(label, url, body, userHashToRedact, null);
    }

    private static Map<String, Object> doHttpRequest(String label, String url, String body, String userHashToRedact, String hashcashSolution) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("label", label);
        step.put("url_redacted", redactUrl(url));
        step.put("request_body_redacted", redactRequestBody(body, userHashToRedact));
        step.put("request_body_len", body.length());
        if (hashcashSolution != null) {
            step.put("sent_with_hashcash", true);
        }

        long start = System.nanoTime();
        HttpsURLConnection con = null;
        try {
            con = (HttpsURLConnection) new URL(url).openConnection();
            con.setRequestProperty("Content-type", "text/plain;charset=UTF-8");
            con.setRequestProperty("Accept-Encoding", "gzip");
            con.setRequestProperty("User-Agent", USER_AGENT);
            if (hashcashSolution != null) {
                con.setRequestProperty("X-Hashcash", hashcashSolution);
            }
            con.setUseCaches(false);
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            try (OutputStream os = con.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int status = con.getResponseCode();
            step.put("http_status", status);
            step.put("response_message", con.getResponseMessage());
            step.put("response_headers", captureHeaders(con));

            // If MEGA challenged us with hashcash and we haven't already
            // sent a solution this request, solve it and retry once. The
            // retry result replaces this step's body fields; the original
            // 402 metadata is preserved under "hashcash_challenge".
            if (status == 402 && hashcashSolution == null) {
                String challenge = con.getHeaderField("X-Hashcash");
                if (challenge != null && !challenge.isEmpty()) {
                    Map<String, Object> challengeMeta = new LinkedHashMap<>();
                    challengeMeta.put("challenge_header", challenge);
                    long hcStart = System.currentTimeMillis();
                    try {
                        String solution = HashcashSolver.buildSolutionHeader(challenge);
                        long hcElapsed = System.currentTimeMillis() - hcStart;
                        challengeMeta.put("solved_in_ms", hcElapsed);
                        challengeMeta.put("solution_preview", solution.length() > 100 ? solution.substring(0, 80) + "...(len " + solution.length() + ")" : solution);
                        step.put("hashcash_challenge", challengeMeta);
                        con.disconnect();
                        // Recursive retry with the solution attached.
                        Map<String, Object> retried = doHttpRequest(label + "_retry_with_hashcash", url, body, userHashToRedact, solution);
                        // Merge: the retry's response replaces this step's
                        // response, but we keep this step's hashcash_challenge
                        // metadata for the report.
                        retried.put("hashcash_challenge", challengeMeta);
                        retried.put("first_attempt_http_status", 402);
                        return retried;
                    } catch (Exception hcEx) {
                        challengeMeta.put("solve_error", hcEx.getClass().getSimpleName() + ": " + hcEx.getMessage());
                        step.put("hashcash_challenge", challengeMeta);
                    }
                }
            }

            byte[] respBytes;
            try (InputStream is = pickStream(con, status)) {
                respBytes = is == null ? new byte[0] : readAll(is);
            }
            boolean gzip = "gzip".equalsIgnoreCase(con.getContentEncoding());
            if (gzip && respBytes.length > 0) {
                // pickStream already wrapped in GZIP for success; for error
                // stream we may need to decompress manually. Try and fall
                // back to raw on failure.
                try {
                    respBytes = ungzip(respBytes);
                } catch (IOException ignore) {
                }
            }
            String respText = new String(respBytes, StandardCharsets.UTF_8);
            step.put("response_body_len", respText.length());
            step.put("response_body_was_gzip", gzip);

            // Try to parse as JSON, then redact sensitive fields in the parsed view.
            Object parsed = null;
            String parseError = null;
            try {
                if (!respText.isEmpty()) {
                    parsed = new ObjectMapper().readValue(respText, Object.class);
                }
            } catch (Exception ex) {
                parseError = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            }
            if (parsed != null) {
                // CRITICAL: only the redacted view is serialized. The raw
                // parsed JSON contains encrypted blobs (k, privk, csid, ...)
                // that must never reach the report file. The raw view is
                // stashed under a key with a leading "_" so captureAccount()
                // can read it for the diagnosis logic and then strip it
                // before serializing.
                step.put("_raw_parsed_DO_NOT_SERIALIZE", parsed);
                step.put("parsed_response", redactParsed(parsed));
                step.put("response_field_summary", summarizeFields(parsed));
            } else {
                step.put("response_body_redacted_raw", redactRawResponse(respText));
                if (parseError != null) {
                    step.put("parse_error", parseError);
                }
            }

            // mega numeric error code (e.g. -9, -16) is a single-element
            // array like [-9] or a bare -9. Surface it.
            Integer megaErr = extractMegaError(respText);
            step.put("mega_error_code", megaErr);

            step.put("http_failure", status != 200);
        } catch (IOException ex) {
            step.put("io_exception", ex.getClass().getSimpleName() + ": " + ex.getMessage());
            step.put("http_failure", true);
        } finally {
            long dur = (System.nanoTime() - start) / 1_000_000L;
            step.put("duration_ms", dur);
            if (con != null) {
                con.disconnect();
            }
        }
        return step;
    }

    private static Map<String, List<String>> captureHeaders(HttpsURLConnection con) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        Map<String, List<String>> headers = con.getHeaderFields();
        if (headers == null) {
            return out;
        }
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            // The HTTP status line itself comes back under a null key.
            String name = e.getKey() == null ? "_status_line" : e.getKey();
            out.put(name, e.getValue());
        }
        return out;
    }

    private static InputStream pickStream(HttpsURLConnection con, int status) throws IOException {
        InputStream is;
        if (status >= 200 && status < 400) {
            is = con.getInputStream();
        } else {
            is = con.getErrorStream();
            if (is == null) {
                return null;
            }
        }
        if ("gzip".equalsIgnoreCase(con.getContentEncoding())) {
            return new GZIPInputStream(is);
        }
        return is;
    }

    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static byte[] ungzip(byte[] data) throws IOException {
        try (GZIPInputStream gz = new GZIPInputStream(new java.io.ByteArrayInputStream(data))) {
            return readAll(gz);
        }
    }

    /**
     * Look at what came back from us0 and us, and offer a one-line hint
     * about whether MegaBasterd's legacy login path would have produced a
     * SID. Categories match the MegaAPIException codes added in _realLogin.
     */
    private static Map<String, Object> diagnose(Map<String, Object> step1, Map<String, Object> step3) {
        Map<String, Object> d = new LinkedHashMap<>();
        Object us0Parsed = step1.get("_raw_parsed_DO_NOT_SERIALIZE");
        if (!(us0Parsed instanceof List) || ((List<?>) us0Parsed).isEmpty()
                || !(((List<?>) us0Parsed).get(0) instanceof Map)) {
            d.put("verdict", "UNKNOWN: us0 did not return a parseable object");
            return d;
        }
        Object us3Parsed = step3 == null ? null : step3.get("_raw_parsed_DO_NOT_SERIALIZE");
        if (us3Parsed == null) {
            d.put("verdict", "FAIL_BEFORE_US: us request not made or returned non-JSON");
            d.put("us_mega_error_code", step3 == null ? null : step3.get("mega_error_code"));
            return d;
        }
        if (!(us3Parsed instanceof List) || ((List<?>) us3Parsed).isEmpty()
                || !(((List<?>) us3Parsed).get(0) instanceof Map)) {
            d.put("verdict", "FAIL_US_NON_OBJECT");
            d.put("us_mega_error_code", step3.get("mega_error_code"));
            return d;
        }
        Map<?, ?> usObj = (Map<?, ?>) ((List<?>) us3Parsed).get(0);
        boolean hasK = usObj.get("k") != null;
        boolean hasPrivk = usObj.get("privk") != null;
        boolean hasCsid = usObj.get("csid") != null;
        boolean hasKeys = usObj.get("keys") != null;
        boolean hasPubk = usObj.get("pubk") != null;
        boolean hasSek = usObj.get("sek") != null;
        boolean hasTsid = usObj.get("tsid") != null;

        d.put("has_k", hasK);
        d.put("has_privk", hasPrivk);
        d.put("has_csid", hasCsid);
        d.put("has_keys_blob", hasKeys);
        d.put("has_pubk", hasPubk);
        d.put("has_sek", hasSek);
        d.put("has_tsid", hasTsid);

        if (hasK && hasPrivk && hasCsid) {
            d.put("verdict", "WOULD_SUCCEED: legacy login path has everything it needs");
        } else if (!hasK && hasKeys) {
            d.put("verdict", "FAIL_ACCOUNT_V3: master key in `keys` blob, legacy path cannot decode (this is the MEGAcmd-needed pattern)");
        } else if (!hasK) {
            d.put("verdict", "FAIL_K_MISSING: account not fully bootstrapped server-side");
        } else if (!hasPrivk) {
            d.put("verdict", "FAIL_PRIVK_MISSING: RSA keypair not published");
        } else if (!hasCsid) {
            d.put("verdict", "FAIL_CSID_MISSING: session creation refused (tsid=" + hasTsid + ")");
        } else {
            d.put("verdict", "UNKNOWN");
        }
        return d;
    }

    // -------- redaction & helpers --------

    private static String redactEmail(String email) {
        if (email == null) {
            return "(null)";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) {
            return local.charAt(0) + "*" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }

    private static String redactUrl(String url) {
        return url.replaceAll("(?i)([?&](sid|sek)=)[^&]+", "$1[REDACTED]");
    }

    private static String redactRequestBody(String body, String userHash) {
        String out = body
                .replaceAll("(?i)\"mfa\"\\s*:\\s*\"[^\"]+\"", "\"mfa\":\"[REDACTED]\"");
        if (userHash != null && !userHash.isEmpty()) {
            out = out.replace(userHash, "[REDACTED-uh-len-" + userHash.length() + "]");
        }
        return out;
    }

    /**
     * Walk the parsed JSON and replace any sensitive value with a fingerprint.
     */
    @SuppressWarnings("unchecked")
    private static Object redactParsed(Object node) {
        if (node instanceof Map) {
            Map<String, Object> src = (Map<String, Object>) node;
            Map<String, Object> dst = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : src.entrySet()) {
                String key = e.getKey();
                Object val = e.getValue();
                if (SENSITIVE_RESPONSE_KEYS.contains(key) && val instanceof String) {
                    dst.put(key, fingerprint((String) val));
                } else {
                    dst.put(key, redactParsed(val));
                }
            }
            return dst;
        }
        if (node instanceof List) {
            List<Object> src = (List<Object>) node;
            List<Object> dst = new ArrayList<>(src.size());
            for (Object x : src) {
                dst.add(redactParsed(x));
            }
            return dst;
        }
        return node;
    }

    private static String fingerprint(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 4; i++) {
                sb.append(String.format("%02x", h[i] & 0xff));
            }
            return "[REDACTED-len-" + value.length() + "-sha8-" + sb + "]";
        } catch (Exception ex) {
            return "[REDACTED-len-" + value.length() + "]";
        }
    }

    /**
     * Summarize keys present at the top level of the response so it's
     * trivial to scan a summary.txt and spot field-level differences.
     */
    private static Map<String, Object> summarizeFields(Object parsed) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (parsed instanceof List && !((List<?>) parsed).isEmpty()
                && ((List<?>) parsed).get(0) instanceof Map) {
            Map<?, ?> top = (Map<?, ?>) ((List<?>) parsed).get(0);
            List<String> present = new ArrayList<>();
            List<String> scalars = new ArrayList<>();
            for (Map.Entry<?, ?> e : top.entrySet()) {
                String k = String.valueOf(e.getKey());
                Object v = e.getValue();
                if (v == null) {
                    present.add(k + "=null");
                } else if (v instanceof String) {
                    present.add(k + "=str(" + ((String) v).length() + ")");
                } else if (v instanceof Number || v instanceof Boolean) {
                    scalars.add(k + "=" + v);
                } else {
                    present.add(k + "=" + v.getClass().getSimpleName());
                }
            }
            out.put("fields", present);
            out.put("scalars", scalars);
        }
        return out;
    }

    private static String redactRawResponse(String body) {
        // For non-JSON or unparseable bodies (e.g. plain "-9"). Truncate if huge.
        if (body == null) {
            return null;
        }
        if (body.length() > 4096) {
            return body.substring(0, 4096) + "...[truncated " + (body.length() - 4096) + " more chars]";
        }
        return body;
    }

    private static Integer extractMegaError(String body) {
        if (body == null) {
            return null;
        }
        String trimmed = body.trim();
        if (trimmed.startsWith("[")) {
            trimmed = trimmed.substring(1, Math.max(1, trimmed.length() - 1)).trim();
        }
        try {
            int v = Integer.parseInt(trimmed);
            return v < 0 ? v : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean containsHttp509(Map<String, Object> report) {
        for (String stepKey : new String[]{"step_1_us0", "step_3_us"}) {
            Object step = report.get(stepKey);
            if (step instanceof Map) {
                Object status = ((Map<String, Object>) step).get("http_status");
                if (status instanceof Integer && (Integer) status == 509) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static String formatSummaryLine(String tag, Map<String, Object> report) {
        Object diag = report.get("diagnosis");
        String verdict = "?";
        if (diag instanceof Map) {
            Object v = ((Map<String, Object>) diag).get("verdict");
            if (v != null) {
                verdict = v.toString();
            }
        }
        Object step2 = report.get("step_2_derive");
        Object accVer = step2 instanceof Map ? ((Map<String, Object>) step2).get("account_version") : null;
        return tag + " v=" + accVer + " -> " + verdict;
    }

    // -------- helpers replicated from MegaAPI (kept private; this tool
    // -------- runs without MainPanel.* state initialized) --------

    private static synchronized String nextSeqno() {
        return String.valueOf(seqno++);
    }

    private static String stdParams() {
        StringBuilder sb = new StringBuilder("&v=3");
        sb.append("&ak=").append(currentAppKey());
        return sb.toString();
    }

    private static String currentAppKey() {
        return (MegaAPI.API_KEY != null && !MegaAPI.API_KEY.isEmpty())
                ? MegaAPI.API_KEY : MegaAPI.DEFAULT_APP_KEY;
    }
}
