package jp.ac.jec.cm0127.a0127rps;

import android.content.Context;
import android.content.SharedPreferences;

final class RpsPreferences {
    private static final String PREFS_NAME = "rps_prefs";
    // Keep preference keys in one place so UI code does not depend on raw strings.
    private static final String KEY_BEST_WIN_COUNT = "best_win_count";
    private static final String KEY_DARK_MODE = "dark_mode";

    private RpsPreferences() {
    }

    static int getBestWinCount(Context context) {
        return preferences(context).getInt(KEY_BEST_WIN_COUNT, 0);
    }

    static void setBestWinCount(Context context, int winCount) {
        preferences(context).edit().putInt(KEY_BEST_WIN_COUNT, winCount).apply();
    }

    static void resetBestWinCount(Context context) {
        preferences(context).edit().remove(KEY_BEST_WIN_COUNT).apply();
    }

    static boolean isDarkModeEnabled(Context context) {
        return preferences(context).getBoolean(KEY_DARK_MODE, false);
    }

    static void setDarkModeEnabled(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
