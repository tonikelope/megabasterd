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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

//https://github.com/tonikelope/megabasterd/issues/397

/*
    THANK YOU VERY MUCH TO:

    (ITALIAN) https://github.com/bovirus

    (CHINESE) https://github.com/linkea131

    (HUNGARIAN) https://github.com/Roschach96

    (GERMAN) https://github.com/NieckLikesCode

    (TURKISH) https://github.com/FabrieI

    (VIETNAMESE) https://github.com/rattybox

 */
/**
 * Legacy compatibility shim over the modern {@link I18n} ResourceBundle.
 *
 * <p>Pre-8.38 the entire translation table was hardcoded as ~2,140
 * {@code _addTranslation("English", "Localized")} calls grouped by per-language
 * methods inside this file. That table has been extracted to
 * {@code src/main/resources/i18n/messages*.properties} (see
 * {@link I18n#BUNDLE_BASENAME}). This class now exists only so that existing
 * call sites such as
 * {@code LabelTranslatorSingleton.getInstance().translate("Cancel")} and
 * NetBeans {@code .form} files (translated by walking the component tree and
 * calling {@code translate(label.getText())}) keep working without code edits.
 *
 * <p>The shim works by inverting the English bundle at construction time
 * (English literal -&gt; key), then delegating {@link #translate(String)} to
 * {@link I18n#tr(String)}.
 *
 * <p>New code should call {@link I18n#tr(String)} / {@link I18n#tr(String, Object...)}
 * with stable keys instead of English literals.
 */
public class LabelTranslatorSingleton {

    public static LabelTranslatorSingleton getInstance() {
        return LazyHolder.INSTANCE;
    }

    private final Map<String, String> _english_to_key;

    private LabelTranslatorSingleton() {

        // Resolve the active locale from MainPanel's legacy language code
        // (EN/ES/IT/GE/HU/TU/CH/VI) and prime the ResourceBundle cache.
        Locale locale = I18n.localeFromLegacyCode(MainPanel.getLanguage());
        I18n.setLocale(locale);

        // Build the English-literal -> key inverse index from the canonical
        // (Locale.ROOT) bundle. This is what makes the legacy
        // translate("English text") call sites keep working.
        _english_to_key = new HashMap<>();
        ResourceBundle eng = I18n.defaultBundle();
        Enumeration<String> keys = eng.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            String english = eng.getString(key);
            // putIfAbsent semantics: first key wins on (improbable) collisions
            if (!_english_to_key.containsKey(english)) {
                _english_to_key.put(english, key);
            }
        }
    }

    /**
     * Legacy lookup. Accepts an English literal (the historical key format)
     * and returns its localized counterpart. Unknown strings are returned
     * verbatim so dynamic / untranslated content still renders.
     */
    public String translate(String orig) {
        if (orig == null) {
            return null;
        }
        String key = _english_to_key.get(orig);
        if (key == null) {
            return orig;
        }
        return I18n.tr(key);
    }

    private static class LazyHolder {

        private static final LabelTranslatorSingleton INSTANCE = new LabelTranslatorSingleton();

        private LazyHolder() {
        }
    }
}
