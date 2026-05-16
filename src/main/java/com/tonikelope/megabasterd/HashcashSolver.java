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

import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Solver for MEGA's X-Hashcash anti-bot proof-of-work challenge.
 *
 * Since 2024 MEGA's /cs endpoint may respond to a login (or other) request with
 * HTTP 402 "Payment Required" plus an X-Hashcash header like:
 *
 * X-Hashcash:
 * 1:192:1778778076:VEZuswYJ22eveR5NVuFepExSEAuQCxQTnlvRpQkW9IR0D03EGZD3DvD3tfOAVArD
 * ^v ^e ^ts ^token (64 chars b64url = 48 bytes)
 *
 * The client must find a 32-bit nonce whose SHA-256 satisfies a threshold, then
 * re-send the same POST with header:
 *
 * X-Hashcash: 1:<token>:<base64-of-nonce-bytes-big-endian>
 *
 * Algorithm taken verbatim from the official MEGA SDK
 * (https://github.com/meganz/sdk, src/hashcash.cpp).
 *
 * Layout of the message fed to SHA-256 (kBufSize = 12,582,916 bytes): [4 bytes
 * nonce big-endian] [token (48 bytes)] [token] [token] ... 262144 copies
 *
 * Threshold = (((easiness & 63) << 1) + 1) << ((easiness >> 6) * 7 + 3)
 * Condition: first 4 bytes of SHA-256(message), read as big-endian uint32, must
 * be <= threshold.
 *
 * For easiness=192 the threshold is 0x01000000 so we expect ~256 hashes on
 * average. Each hash processes ~12 MB so a single solve runs in roughly 1-3
 * seconds on a desktop CPU with multi-threading.
 *
 * @author tonikelope
 */
public final class HashcashSolver {

    private static final Logger LOG = Logger.getLogger(HashcashSolver.class.getName());

    // Constants below match meganz/sdk src/hashcash.cpp.
    private static final int TOKEN_BYTES = 48;
    private static final int PREFIX_BYTES = 4;
    private static final int REPEAT = 262144;
    private static final int BUF_SIZE = PREFIX_BYTES + REPEAT * TOKEN_BYTES; // 12 582 916 bytes
    private static final int MAX_WORKERS = 8;

    /**
     * Hard upper bound so we never lock up an entire login attempt. The SDK
     * gives gencash 5 minutes; we use the same default. easiness=192 normally
     * finishes in single-digit seconds with 4+ cores.
     */
    public static final long DEFAULT_SOLVE_TIMEOUT_MS = 5L * 60_000L;

    private HashcashSolver() {
    }

    /**
     * Parse the full challenge header value as returned by MEGA, solve the
     * proof-of-work, and return the value that should be sent back in the
     * X-Hashcash request header on the retried POST:
     *
     * "1:&lt;token&gt;:&lt;nonce-base64&gt;"
     *
     * Note: the SDK only requires {version=1, easiness, token}. The timestamp
     * field is informational and is NOT mixed into the hash.
     *
     * @throws IllegalArgumentException if the header is malformed.
     * @throws Exception if SHA-256 is unavailable or the solve times out.
     */
    public static String buildSolutionHeader(String challenge) throws Exception {
        return buildSolutionHeader(challenge, DEFAULT_SOLVE_TIMEOUT_MS);
    }

    public static String buildSolutionHeader(String challenge, long timeoutMs) throws Exception {
        if (challenge == null) {
            throw new IllegalArgumentException("hashcash challenge is null");
        }
        String[] parts = challenge.split(":");
        if (parts.length != 4) {
            throw new IllegalArgumentException("hashcash header must have 4 fields, got " + parts.length + ": " + challenge);
        }
        if (!"1".equals(parts[0])) {
            throw new IllegalArgumentException("hashcash version not supported: " + parts[0]);
        }
        int easiness;
        try {
            easiness = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("hashcash easiness not numeric: " + parts[1]);
        }
        if (easiness < 0 || easiness > 255) {
            throw new IllegalArgumentException("hashcash easiness out of [0,255]: " + easiness);
        }
        String token = parts[3];
        if (token.length() != 64) {
            throw new IllegalArgumentException("hashcash token must be 64 b64url chars, got " + token.length());
        }

        byte[] tokenBin = MiscTools.UrlBASE642Bin(token);
        if (tokenBin.length != TOKEN_BYTES) {
            throw new IllegalArgumentException("hashcash token must decode to 48 bytes, got " + tokenBin.length);
        }

        long start = System.currentTimeMillis();
        String nonceB64 = solve(tokenBin, easiness, timeoutMs);
        long elapsed = System.currentTimeMillis() - start;
        LOG.log(Level.INFO, "X-Hashcash solved: easiness={0} elapsed_ms={1}",
                new Object[]{easiness, elapsed});

        return "1:" + token + ":" + nonceB64;
    }

    /**
     * Solve the proof-of-work. Returns the base64url-encoded 4-byte nonce
     * (big-endian) that makes the SHA-256 hash satisfy the threshold.
     */
    private static String solve(byte[] tokenBin, int easiness, long timeoutMs) throws Exception {
        // Threshold (uint32) -- comparison is against the first 4 bytes of
        // SHA-256 interpreted as a big-endian uint32. For easiness=192 this
        // evaluates to 0x01000000 (about one solution per 256 attempts).
        long threshold = (long) ((((easiness & 63) << 1) + 1) << (((easiness >>> 6) & 3) * 7 + 3)) & 0xFFFFFFFFL;

        // Build the ~12 MB buffer once -- workers share it read-only and
        // each worker keeps its own 64-byte first-block scratch space.
        byte[] buf = new byte[BUF_SIZE];
        System.arraycopy(tokenBin, 0, buf, PREFIX_BYTES, TOKEN_BYTES);
        int filled = TOKEN_BYTES;
        while (filled < (long) REPEAT * TOKEN_BYTES) {
            int copy = (int) Math.min(filled, (long) REPEAT * TOKEN_BYTES - filled);
            System.arraycopy(buf, PREFIX_BYTES, buf, PREFIX_BYTES + filled, copy);
            filled += copy;
        }

        int workers = Math.min(MAX_WORKERS, Math.max(1, Runtime.getRuntime().availableProcessors()));
        AtomicBoolean stop = new AtomicBoolean(false);
        AtomicReference<String> winner = new AtomicReference<>(null);

        ExecutorService pool = Executors.newFixedThreadPool(workers, r -> {
            Thread t = new Thread(r, "HashcashSolver");
            t.setDaemon(true);
            return t;
        });
        try {
            for (int w = 0; w < workers; w++) {
                final int start = w;
                final int stride = workers;
                pool.submit(() -> {
                    try {
                        String sol = worker(buf, threshold, start, stride, stop);
                        if (sol != null) {
                            if (winner.compareAndSet(null, sol)) {
                                stop.set(true);
                            }
                        }
                    } catch (Exception ex) {
                        LOG.log(Level.WARNING, "hashcash worker failed", ex);
                    }
                });
            }
            pool.shutdown();
            if (!pool.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS)) {
                stop.set(true);
                pool.shutdownNow();
                throw new RuntimeException("Hashcash solve timed out after " + timeoutMs + " ms");
            }
        } finally {
            stop.set(true);
            if (!pool.isTerminated()) {
                pool.shutdownNow();
            }
        }

        String sol = winner.get();
        if (sol == null) {
            throw new RuntimeException("Hashcash solve exhausted nonce space without success");
        }
        return sol;
    }

    private static String worker(byte[] tokenArea, long threshold, int start, int stride, AtomicBoolean stop) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] block = new byte[64];
        // The first 64-byte SHA block is [nonce(4) | tokenArea[4..63]]. Pre-fill
        // the constant tail so the inner loop only writes the 4-byte nonce.
        System.arraycopy(tokenArea, PREFIX_BYTES, block, PREFIX_BYTES, 64 - PREFIX_BYTES);

        final long maxNonce = 1L << 32;
        for (long n = start; n < maxNonce; n += stride) {
            if (stop.get()) {
                return null;
            }
            block[0] = (byte) ((n >> 24) & 0xff);
            block[1] = (byte) ((n >> 16) & 0xff);
            block[2] = (byte) ((n >> 8) & 0xff);
            block[3] = (byte) (n & 0xff);

            sha.reset();
            sha.update(block, 0, 64);
            sha.update(tokenArea, 64, BUF_SIZE - 64);
            byte[] hash = sha.digest();

            long firstWord = ((hash[0] & 0xffL) << 24)
                    | ((hash[1] & 0xffL) << 16)
                    | ((hash[2] & 0xffL) << 8)
                    | (hash[3] & 0xffL);

            if (firstWord <= threshold) {
                byte[] nonce4 = new byte[]{block[0], block[1], block[2], block[3]};
                return MiscTools.Bin2UrlBASE64(nonce4);
            }
        }
        return null;
    }
}
