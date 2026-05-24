/*
 __  __                  _               _               _
|  \/  | ___  __ _  __ _| |__   __ _ ___| |_ ___ _ __ __| |
| |\/| |/ _ \/ _` |/ _` | '_ \ / _` / __| __/ _ \ '__/ _` |
| |  | |  __/ (_| | (_| | |_) | (_| \__ \ ||  __/ | | (_| |
|_|  |_|\___|\__, |\__,_|_.__/ \__,_|___/\__\___|_|  \__,_|
             |___/
 */
package com.tonikelope.megabasterd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tag-based i18n entry point. Translations live in
 * {@code src/main/resources/i18n/messages*.properties} and are loaded as
 * UTF-8 (not ISO-8859-1 like the JDK default on Java 8) so contributors can
 * edit the bundles in any modern editor without escaping.
 *
 * <p>Two public lookup methods:
 * <ul>
 *   <li>{@link #tr(String)} -- straight key -&gt; localized text</li>
 *   <li>{@link #tr(String, Object...)} -- key + {@link MessageFormat} args</li>
 * </ul>
 *
 * <p>{@link LabelTranslatorSingleton#translate(String)} is preserved as a
 * legacy shim that maps "English literal" -&gt; key by inverting the English
 * bundle, so call sites and NetBeans {@code .form} files keep working
 * unmodified.
 */
public final class I18n {

    public static final String BUNDLE_BASENAME = "i18n.messages";

    private static final Logger LOG = Logger.getLogger(I18n.class.getName());
    private static final ResourceBundle.Control UTF8_CONTROL = new Utf8Control();
    private static final Map<String, Locale> LEGACY_CODE_TO_LOCALE = new HashMap<>();

    static {
        LEGACY_CODE_TO_LOCALE.put("EN", Locale.ROOT);
        LEGACY_CODE_TO_LOCALE.put("ES", new Locale("es"));
        LEGACY_CODE_TO_LOCALE.put("IT", new Locale("it"));
        LEGACY_CODE_TO_LOCALE.put("GE", new Locale("de"));
        LEGACY_CODE_TO_LOCALE.put("HU", new Locale("hu"));
        LEGACY_CODE_TO_LOCALE.put("TU", new Locale("tr"));
        LEGACY_CODE_TO_LOCALE.put("CH", new Locale("zh"));
        LEGACY_CODE_TO_LOCALE.put("VI", new Locale("vi"));
    }

    private static volatile Locale currentLocale = Locale.ROOT;
    private static volatile ResourceBundle bundle = loadBundle(Locale.ROOT);
    private static volatile ResourceBundle defaultBundle = loadBundle(Locale.ROOT);
    private static volatile ResourceBundle overrideBundle; // optional on-disk override

    private I18n() {
    }

    /**
     * Translate a key to the current locale.
     *
     * <p>Lookup order: external override file (if loaded) -&gt; active locale
     * bundle -&gt; English root bundle -&gt; the key itself.
     */
    public static String tr(String key) {
        if (key == null) {
            return null;
        }
        ResourceBundle ov = overrideBundle;
        if (ov != null) {
            try {
                return ov.getString(key);
            } catch (MissingResourceException ignored) {
            }
        }
        ResourceBundle b = bundle;
        try {
            return b.getString(key);
        } catch (MissingResourceException ignored) {
        }
        try {
            return defaultBundle.getString(key);
        } catch (MissingResourceException ignored) {
        }
        return key;
    }

    /** Translate with MessageFormat substitution. */
    public static String tr(String key, Object... args) {
        String pattern = tr(key);
        if (args == null || args.length == 0) {
            return pattern;
        }
        return MessageFormat.format(pattern, args);
    }

    /**
     * Switch the active locale. Triggers a bundle reload; existing
     * {@link #tr(String)} call sites will see new translations on next call.
     */
    public static synchronized void setLocale(Locale locale) {
        Locale target = locale != null ? locale : Locale.ROOT;
        ResourceBundle.clearCache(I18n.class.getClassLoader());
        currentLocale = target;
        bundle = loadBundle(target);
        defaultBundle = loadBundle(Locale.ROOT);
    }

    /** Map a legacy MegaBasterd language code (EN/ES/GE/...) to a Locale. */
    public static Locale localeFromLegacyCode(String code) {
        if (code == null) {
            return Locale.ROOT;
        }
        Locale l = LEGACY_CODE_TO_LOCALE.get(code);
        return l != null ? l : Locale.ROOT;
    }

    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    /** Exposed for the legacy shim that needs to invert the English bundle. */
    static ResourceBundle defaultBundle() {
        return defaultBundle;
    }

    /** Exposed for the legacy shim. */
    static ResourceBundle activeBundle() {
        return bundle;
    }

    /**
     * Load (or clear) an on-disk UTF-8 {@code .properties} file whose entries
     * take precedence over the embedded bundle for the active locale. Intended
     * for translators iterating on their language file without rebuilding the
     * JAR (see issue #766). Passing {@code null}, a missing path, or any I/O
     * failure clears the override.
     */
    public static synchronized void setExternalLanguageFile(Path file) {
        if (file == null) {
            overrideBundle = null;
            return;
        }
        if (!Files.isRegularFile(file)) {
            LOG.log(Level.WARNING, "External language file not found: {0}", file);
            overrideBundle = null;
            return;
        }
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            overrideBundle = new PropertyResourceBundle(r);
            LOG.log(Level.INFO, "External language override loaded from {0}", file);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Failed to load external language file {0}: {1}",
                    new Object[]{file, ex.getMessage()});
            overrideBundle = null;
        }
    }

    /** Whether an external override file is currently active. */
    public static boolean hasExternalLanguageFile() {
        return overrideBundle != null;
    }

    private static ResourceBundle loadBundle(Locale locale) {
        return ResourceBundle.getBundle(BUNDLE_BASENAME, locale, UTF8_CONTROL);
    }

    /** UTF-8 {@code .properties} reader. Java 8's default is ISO-8859-1. */
    private static final class Utf8Control extends ResourceBundle.Control {

        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                ClassLoader loader, boolean reload) throws IOException {
            if (!"java.properties".equals(format)) {
                return null;
            }
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            InputStream stream = loader.getResourceAsStream(resourceName);
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return new PropertyResourceBundle(reader);
            }
        }
    }
}
