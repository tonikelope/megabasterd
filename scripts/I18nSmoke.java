package com.tonikelope.megabasterd;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * One-shot runtime smoke test:
 *  - I18n.setLocale(legacy code) -> ResourceBundle loads via Utf8Control
 *  - LabelTranslatorSingleton.translate(English literal) -> localized text
 *
 * Run from the repo root (UTF-8 stdout to avoid Windows cp1252 mojibake):
 *   mvn -q -pl megabasterd-desktop -am compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
 *   javac -cp "megabasterd-desktop/target/classes:megabasterd-core/target/classes:$(cat megabasterd-desktop/target/classpath.txt)" -d megabasterd-desktop/target/classes scripts/I18nSmoke.java
 *   java -cp "megabasterd-desktop/target/classes:megabasterd-core/target/classes:$(cat megabasterd-desktop/target/classpath.txt)" com.tonikelope.megabasterd.I18nSmoke
 *
 * NOT shipped: lives in scripts/ but compiled into the desktop module's
 * target/classes for the duration of this verification, then deleted.
 */
public class I18nSmoke {

    public static void main(String[] args) throws Exception {
        PrintStream out = new PrintStream(System.out, true, "UTF-8");

        String[][] cases = {
            {"EN", "en"}, {"ES", "es"}, {"IT", "it"}, {"GE", "de"},
            {"HU", "hu"}, {"TU", "tr"}, {"CH", "zh"}, {"VI", "vi"},
        };

        String[] probes = {"COPY ALL", "Cancel", "Yes", "Settings", "EXIT"};

        for (String[] c : cases) {
            String legacy = c[0];
            Locale loc = I18n.localeFromLegacyCode(legacy);
            I18n.setLocale(loc);
            out.println("=== " + legacy + " -> " + loc + " ===");
            for (String p : probes) {
                // direct key path
                String key = inverseLookup(p);
                String viaI18n = key != null ? I18n.tr(key) : "(no key)";
                out.printf("  %-12s key=%-20s tr=%s%n", p, key, viaI18n);
            }
        }

        // Verify the legacy shim end-to-end (constructs singleton, which itself
        // reads MainPanel.getLanguage() == null in this isolated harness -> ROOT).
        out.println();
        out.println("=== legacy shim (no MainPanel -> ROOT/English) ===");
        for (String p : probes) {
            out.printf("  translate(%-12s) = %s%n", "\"" + p + "\"",
                    LabelTranslatorSingleton.getInstance().translate(p));
        }
    }

    private static String inverseLookup(String englishLiteral) {
        java.util.ResourceBundle b = java.util.ResourceBundle.getBundle(
                I18n.BUNDLE_BASENAME, Locale.ROOT, new I18nSmokeUtf8Control());
        java.util.Enumeration<String> keys = b.getKeys();
        while (keys.hasMoreElements()) {
            String k = keys.nextElement();
            if (englishLiteral.equals(b.getString(k))) {
                return k;
            }
        }
        return null;
    }

    private static class I18nSmokeUtf8Control extends java.util.ResourceBundle.Control {
        @Override
        public java.util.ResourceBundle newBundle(String baseName, Locale locale,
                String format, ClassLoader loader, boolean reload)
                throws java.io.IOException {
            if (!"java.properties".equals(format)) return null;
            String resourceName = toResourceName(toBundleName(baseName, locale), "properties");
            java.io.InputStream s = loader.getResourceAsStream(resourceName);
            if (s == null) return null;
            try (java.io.Reader r = new java.io.InputStreamReader(s, StandardCharsets.UTF_8)) {
                return new java.util.PropertyResourceBundle(r);
            }
        }
    }
}
