package jp.ac.jec.cm0127.a0127rps;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.widget.ImageView;
import android.widget.TextView;

final class RpsAnimations {
    private static final float SELECTED_HAND_SCALE = 1.2f;

    private AnimatorSet resultAnimator;

    void resetResult(TextView resultView) {
        // Clear any previous result animation before reusing the same TextView.
        cancelResult();
        resultView.setAlpha(1f);
        resultView.setScaleX(1f);
        resultView.setScaleY(1f);
        resultView.setTranslationX(0f);
    }

    void animateWin(TextView resultView) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(resultView, "scaleX", 1f, 1.16f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(resultView, "scaleY", 1f, 1.16f, 1f);
        playTogether(360, scaleX, scaleY);
    }

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

    void resetHandScale(ImageView hand) {
        hand.animate().cancel();
        hand.setScaleX(1f);
        hand.setScaleY(1f);
    }

    void cancelResult() {
        if (resultAnimator != null) {
            resultAnimator.cancel();
            resultAnimator = null;
        }
    }

    private void playTogether(long durationMs, ObjectAnimator... animators) {
        // Only one result animation should be active at a time.
        cancelResult();
        resultAnimator = new AnimatorSet();
        resultAnimator.setDuration(durationMs);
        resultAnimator.playTogether(animators);
        resultAnimator.start();
    }
}
