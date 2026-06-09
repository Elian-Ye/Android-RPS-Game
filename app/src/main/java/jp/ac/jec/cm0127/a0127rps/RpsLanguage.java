package jp.ac.jec.cm0127.a0127rps;

import android.content.Context;
import android.os.LocaleList;

import java.util.Locale;

enum RpsLanguage {
    ENGLISH("en", R.string.language_english),
    JAPANESE("ja", R.string.language_japanese),
    TRADITIONAL_CHINESE("zh-TW", R.string.language_traditional_chinese);

    // Add future languages here, then provide a matching values-xx/strings.xml file.
    private static final RpsLanguage FALLBACK = ENGLISH;

    private final String languageTag;
    private final int labelResId;

    RpsLanguage(String languageTag, int labelResId) {
        this.languageTag = languageTag;
        this.labelResId = labelResId;
    }

    String getLanguageTag() {
        return languageTag;
    }

    int getLabelResId() {
        return labelResId;
    }

    static RpsLanguage getSystemDefault(Context context) {
        LocaleList locales = context.getResources().getConfiguration().getLocales();
        for (int i = 0; i < locales.size(); i++) {
            RpsLanguage language = fromLocale(locales.get(i));
            if (language != null) {
                return language;
            }
        }

        return FALLBACK;
    }

    static RpsLanguage fromLanguageTag(String languageTag) {
        for (RpsLanguage language : values()) {
            if (language.languageTag.equals(languageTag)) {
                return language;
            }
        }

        return FALLBACK;
    }

    private static RpsLanguage fromLocale(Locale locale) {
        String language = locale.getLanguage();
        if (Locale.JAPANESE.getLanguage().equals(language)) {
            return JAPANESE;
        }

        if (Locale.ENGLISH.getLanguage().equals(language)) {
            return ENGLISH;
        }

        if (Locale.CHINESE.getLanguage().equals(language)) {
            return TRADITIONAL_CHINESE;
        }

        return null;
    }
}
