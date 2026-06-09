package jp.ac.jec.cm0127.a0127rps;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.widget.ImageView;
import android.widget.TextView;

final class RpsAnimations {
    private static final float SELECTED_HAND_SCALE = 1.2f;

    private AnimatorSet resultAnimator;

    // Restores the result text before showing a new countdown or result.
    void resetResult(TextView resultView) {
        // Clear any previous result animation before reusing the same TextView.
        cancelResult();
        resultView.setAlpha(1f);
        resultView.setScaleX(1f);
        resultView.setScaleY(1f);
        resultView.setTranslationX(0f);
    }

    // Plays a short scale pulse for a winning result.
    void animateWin(TextView resultView) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(resultView, "scaleX", 1f, 1.16f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(resultView, "scaleY", 1f, 1.16f, 1f);
        playTogether(360, scaleX, scaleY);
    }

    // Plays a small horizontal shake for a losing result.
    void animateLose(TextView resultView) {
        ObjectAnimator shake = ObjectAnimator.ofFloat(
                resultView,
                "translationX",
                0f,
                -18f,
                18f,
                -12f,
                12f,
                0f
        );
        playTogether(360, shake);
    }

    // Flashes the result text for a draw.
    void animateDraw(TextView resultView) {
        ObjectAnimator flash = ObjectAnimator.ofFloat(
                resultView,
                "alpha",
                1f,
                0.25f,
                1f,
                0.45f,
                1f
        );
        playTogether(460, flash);
    }

    // Gives the selected hand a quick tap feedback animation.
    void animateSelectedHand(ImageView hand) {
        hand.animate().cancel();
        hand.animate()
                .scaleX(SELECTED_HAND_SCALE)
                .scaleY(SELECTED_HAND_SCALE)
                .setDuration(120)
                .withEndAction(() -> hand.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .start())
                .start();
    }

    // Cancels hand feedback and restores the image scale.
    void resetHandScale(ImageView hand) {
        hand.animate().cancel();
        hand.setScaleX(1f);
        hand.setScaleY(1f);
    }

    // Stops the current result animation if one is active.
    void cancelResult() {
        if (resultAnimator != null) {
            resultAnimator.cancel();
            resultAnimator = null;
        }
    }

    // Runs one or more result animations together with shared cleanup.
    private void playTogether(long durationMs, ObjectAnimator... animators) {
        // Only one result animation should be active at a time.
        cancelResult();
        resultAnimator = new AnimatorSet();
        resultAnimator.setDuration(durationMs);
        resultAnimator.playTogether(animators);
        resultAnimator.start();
    }
}
