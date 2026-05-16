package com.tonikelope.megabasterd;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reflection-based smoke test for {@code SmartMegaProxyManager.parseProxyEntry}.
 * Covers the historical [*]IP:PORT[@b64u:b64p] syntax AND the
 * scheme-prefixed forms added by issue #753 (http://, https://, socks://,
 * socks4://, socks4a://, socks5://).
 *
 * Run from the repo root (UTF-8 stdout to avoid Windows cp1252 mojibake):
 *   mvn -q compile
 *   javac -d target/classes scripts/SmartProxyParserSmoke.java
 *   java -cp target/classes com.tonikelope.megabasterd.SmartProxyParserSmoke
 *
 * Exit code is the count of failed cases (0 = all green).
 */
public class SmartProxyParserSmoke {

    private static int failures = 0;
    private static Method parseProxyEntry;
    private static PrintStream out;

    public static void main(String[] args) throws Exception {
        out = new PrintStream(System.out, true, "UTF-8");
        parseProxyEntry = SmartMegaProxyManager.class.getDeclaredMethod(
                "parseProxyEntry", String.class, String.class, Map.class, Map.class);
        parseProxyEntry.setAccessible(true);

        // ---- Historical syntax (pre-#753): must still work ----
        assertEntry("bare IP:PORT",
                "1.2.3.4:8080", "1.2.3.4:8080", /*socks*/ false, /*auth*/ null);
        assertEntry("bare with leading whitespace",
                "   1.2.3.4:8080   ", "1.2.3.4:8080", false, null);
        assertEntry("*-prefixed SOCKS",
                "*1.2.3.4:1080", "1.2.3.4:1080", true, null);
        assertEntry("IP:PORT@b64u:b64p auth",
                "1.2.3.4:8080@dXNlcg==:cGFzcw==", "1.2.3.4:8080", false, "dXNlcg==:cGFzcw==");
        assertEntry("SOCKS auth combined",
                "*1.2.3.4:1080@dXNlcg==:cGFzcw==", "1.2.3.4:1080", true, "dXNlcg==:cGFzcw==");
        assertEntry("auth with '/' in b64 (regression #753 audit)",
                "1.2.3.4:8080@VGhpcw==:c2Vj/cmV0", "1.2.3.4:8080", false, "VGhpcw==:c2Vj/cmV0");
        assertSkip("malformed: stray @",
                "user@pass@1.2.3.4:8080");
        assertSkip("malformed: no port",
                "1.2.3.4");
        assertSkip("malformed: port out of range",
                "1.2.3.4:999999");

        // ---- #URL line: must be silently skipped (handled elsewhere) ----
        assertSkippedSilently("#URL line",
                "#https://example.com/proxies.txt");

        // ---- Blank / null: silently skipped ----
        assertSkippedSilently("blank line", "");
        assertSkippedSilently("whitespace-only line", "   ");
        assertSkippedSilently("null line", null);

        // ---- Scheme prefixes (NEW, #753) ----
        assertEntry("http:// scheme",
                "http://1.2.3.4:8080", "1.2.3.4:8080", false, null);
        assertEntry("https:// scheme",
                "https://1.2.3.4:8443", "1.2.3.4:8443", false, null);
        assertEntry("socks:// scheme",
                "socks://1.2.3.4:1080", "1.2.3.4:1080", true, null);
        assertEntry("socks4:// scheme",
                "socks4://1.2.3.4:1080", "1.2.3.4:1080", true, null);
        assertEntry("socks4a:// scheme",
                "socks4a://1.2.3.4:1080", "1.2.3.4:1080", true, null);
        assertEntry("socks5:// scheme",
                "socks5://1.2.3.4:1080", "1.2.3.4:1080", true, null);
        assertEntry("scheme is case-insensitive",
                "HTTP://1.2.3.4:8080", "1.2.3.4:8080", false, null);
        assertEntry("scheme + trailing slash",
                "http://1.2.3.4:8080/", "1.2.3.4:8080", false, null);
        assertEntry("scheme + path",
                "http://1.2.3.4:8080/something", "1.2.3.4:8080", false, null);
        assertEntry("*scheme combination",
                "*socks5://1.2.3.4:1080", "1.2.3.4:1080", true, null);
        assertEntry("scheme + legacy auth (b64u:b64p)",
                "http://1.2.3.4:8080@dXNlcg==:cGFzcw==", "1.2.3.4:8080", false, "dXNlcg==:cGFzcw==");

        // ---- Hardened against URL-style user:pass@host:port (#753 audit) ----
        // A line like http://user:1234@1.2.3.4:8080 must NOT be silently
        // mis-parsed as host="user:1234" auth="1.2.3.4:8080". Either we
        // detect the ambiguity and reject, or we parse it correctly --
        // EITHER way "user:1234" must NOT end up as a stored host.
        assertNoBadHost("URL-style user:pass@host:port must not store user:1234 as host",
                "http://user:1234@1.2.3.4:8080", "user:1234");
        // And the legacy reading must still work when parts[1] is clearly
        // not host:port (typical b64 with padding).
        assertEntry("scheme + legacy auth with padded b64",
                "http://1.2.3.4:8080@dXNlcjE=:cGFzczE=", "1.2.3.4:8080", false, "dXNlcjE=:cGFzczE=");

        // ---- Malformed scheme entries ----
        assertSkip("scheme without port",
                "http://1.2.3.4");
        assertSkip("scheme without host",
                "http://");

        out.println();
        out.println("Total failures: " + failures);
        System.exit(failures);
    }

    private static void assertEntry(String label, String raw, String expectedAddr,
            boolean expectedSocks, String expectedAuth) {
        Map<String, Long[]> list = new LinkedHashMap<>();
        Map<String, String> auth = new LinkedHashMap<>();
        try {
            parseProxyEntry.invoke(null, raw, "test", list, auth);
        } catch (Exception e) {
            fail(label, "threw " + e.getCause());
            return;
        }
        if (list.size() != 1) {
            fail(label, "expected 1 entry, got " + list.size() + " (entries=" + list.keySet() + ")");
            return;
        }
        String addr = list.keySet().iterator().next();
        if (!addr.equals(expectedAddr)) {
            fail(label, "expected addr=[" + expectedAddr + "], got [" + addr + "]");
            return;
        }
        Long[] meta = list.get(addr);
        boolean isSocks = meta[1] != null && meta[1].longValue() != -1L;
        if (isSocks != expectedSocks) {
            fail(label, "expected socks=" + expectedSocks + ", got " + isSocks);
            return;
        }
        String storedAuth = auth.get(addr);
        if (expectedAuth == null) {
            if (storedAuth != null) {
                fail(label, "expected no auth, got [" + storedAuth + "]");
                return;
            }
        } else if (!expectedAuth.equals(storedAuth)) {
            fail(label, "expected auth=[" + expectedAuth + "], got [" + storedAuth + "]");
            return;
        }
        out.println("OK   " + label);
    }

    /**
     * Asserts the line was rejected (malformed). The parser writes a
     * warning to the log and returns without populating either map.
     */
    private static void assertSkip(String label, String raw) {
        Map<String, Long[]> list = new LinkedHashMap<>();
        Map<String, String> auth = new LinkedHashMap<>();
        try {
            parseProxyEntry.invoke(null, raw, "test", list, auth);
        } catch (Exception e) {
            fail(label, "threw " + e.getCause());
            return;
        }
        if (!list.isEmpty()) {
            fail(label, "expected skip, but got entries=" + list.keySet());
            return;
        }
        out.println("OK   " + label + " (skipped as expected)");
    }

    /** Same as assertSkip but used for legitimate non-entry lines (#URL, blank). */
    private static void assertSkippedSilently(String label, String raw) {
        assertSkip(label, raw);
    }

    /** Asserts the parser did NOT store the given bad address as a host key. */
    private static void assertNoBadHost(String label, String raw, String forbidden) {
        Map<String, Long[]> list = new LinkedHashMap<>();
        Map<String, String> auth = new LinkedHashMap<>();
        try {
            parseProxyEntry.invoke(null, raw, "test", list, auth);
        } catch (Exception e) {
            fail(label, "threw " + e.getCause());
            return;
        }
        if (list.containsKey(forbidden)) {
            fail(label, "parser stored forbidden host [" + forbidden + "] (entries=" + list.keySet() + ", auth=" + auth + ")");
            return;
        }
        out.println("OK   " + label);
    }

    private static void fail(String label, String why) {
        out.println("FAIL " + label + " -- " + why);
        failures++;
    }
}
