package jp.ac.jec.cm0127.a0127rps;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

final class RpsSettingsMenu {
    private static final float POPUP_WIDTH_DP = 220f;
    private static final float POPUP_OFFSET_DP = 4f;
    private static final float POPUP_ELEVATION_DP = 8f;

    private RpsSettingsMenu() {
    }

    static void applySavedTheme(Context context) {
        applyTheme(RpsPreferences.isDarkModeEnabled(context));
    }

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

    private static void applyTheme(boolean darkModeEnabled) {
        AppCompatDelegate.setDefaultNightMode(
                darkModeEnabled
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private static float dpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }
}
