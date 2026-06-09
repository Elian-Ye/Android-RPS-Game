package jp.ac.jec.cm0127.a0127rps;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.os.LocaleListCompat;

final class RpsSettingsMenu {
    private static final float POPUP_WIDTH_DP = 220f;
    private static final float POPUP_OFFSET_DP = 4f;
    private static final float POPUP_ELEVATION_DP = 8f;

    private RpsSettingsMenu() {
    }

    // Applies the saved dark mode setting when the Activity starts.
    static void applySavedTheme(Context context) {
        applyTheme(RpsPreferences.isDarkModeEnabled(context));
    }

    // Applies the saved or system-matched language when the Activity starts.
    static void applySavedLanguage(Context context) {
        applyLanguage(RpsPreferences.getLanguage(context));
    }

    // Shows the compact settings popup anchored to the settings button.
    static void show(AppCompatActivity activity, View anchor, Runnable onResetRecord) {
        // Use a small anchored popup instead of a full dialog to keep the main UI lightweight.
        FrameLayout parent = new FrameLayout(activity);
        View settingsView = activity.getLayoutInflater()
                .inflate(R.layout.dialog_settings, parent, false);
        int popupWidth = Math.round(dpToPx(activity, POPUP_WIDTH_DP));
        PopupWindow popup = new PopupWindow(
                settingsView,
                popupWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setElevation(dpToPx(activity, POPUP_ELEVATION_DP));
        popup.setOutsideTouchable(true);

        SwitchCompat switchDarkMode = settingsView.findViewById(R.id.switchDarkMode);
        switchDarkMode.setChecked(RpsPreferences.isDarkModeEnabled(activity));
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            RpsPreferences.setDarkModeEnabled(activity, isChecked);
            popup.dismiss();
            applyTheme(isChecked);
        });

        Button btnLanguage = settingsView.findViewById(R.id.btnLanguage);
        updateLanguageButton(activity, btnLanguage);
        btnLanguage.setOnClickListener(v -> showLanguageMenu(activity, btnLanguage, popup));

        settingsView.findViewById(R.id.btnResetRecord).setOnClickListener(v -> {
            popup.dismiss();
            showResetRecordConfirmation(activity, onResetRecord);
        });

        popup.showAsDropDown(
                anchor,
                anchor.getWidth() - popupWidth,
                Math.round(dpToPx(activity, POPUP_OFFSET_DP))
        );
    }

    // Updates the language row with the currently selected language label.
    private static void updateLanguageButton(Context context, Button btnLanguage) {
        RpsLanguage selectedLanguage = RpsPreferences.getLanguage(context);
        String languageLabel = context.getString(selectedLanguage.getLabelResId());
        btnLanguage.setText(context.getString(R.string.format_language, languageLabel));
    }

    // Shows a single-choice language dropdown and asks for confirmation on changes.
    private static void showLanguageMenu(
            AppCompatActivity activity,
            View anchor,
            PopupWindow settingsPopup
    ) {
        PopupMenu menu = new PopupMenu(activity, anchor);
        RpsLanguage[] languages = RpsLanguage.values();
        RpsLanguage selectedLanguage = RpsPreferences.getLanguage(activity);

        for (int i = 0; i < languages.length; i++) {
            menu.getMenu()
                    .add(0, i, i, activity.getString(languages[i].getLabelResId()))
                    .setCheckable(true)
                    .setChecked(languages[i] == selectedLanguage);
        }

        menu.getMenu().setGroupCheckable(0, true, true);
        menu.setOnMenuItemClickListener(item -> {
            RpsLanguage language = languages[item.getItemId()];
            if (language == selectedLanguage) {
                return true;
            }

            settingsPopup.dismiss();
            showLanguageConfirmation(activity, language);
            return true;
        });
        menu.show();
    }

    // Confirms the selected language before saving and applying it.
    private static void showLanguageConfirmation(AppCompatActivity activity, RpsLanguage language) {
        String languageLabel = activity.getString(language.getLabelResId());
        new AlertDialog.Builder(activity)
                .setTitle(R.string.change_language)
                .setMessage(activity.getString(
                        R.string.format_change_language_confirmation,
                        languageLabel
                ))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.apply, (dialogInterface, which) -> {
                    RpsPreferences.setLanguage(activity, language);
                    applyLanguage(language);
                })
                .show();
    }

    // Confirms the destructive best-record reset before running it.
    private static void showResetRecordConfirmation(
            AppCompatActivity activity,
            Runnable onResetRecord
    ) {
        // Resetting the best record is destructive, so it always asks for confirmation.
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.reset_record)
                .setMessage(R.string.reset_record_confirmation)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.reset, (dialogInterface, which) -> onResetRecord.run())
                .create();
        dialog.setOnShowListener(ignored ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(activity.getColor(R.color.rps_result_lose))
        );
        dialog.show();
    }

    // Applies light or dark mode through AppCompat.
    private static void applyTheme(boolean darkModeEnabled) {
        AppCompatDelegate.setDefaultNightMode(
                darkModeEnabled
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    // Applies the selected app language through AppCompat locales.
    private static void applyLanguage(RpsLanguage language) {
        AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(language.getLanguageTag())
        );
    }

    // Converts dp values used by popup sizing into actual pixels.
    private static float dpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }
}
