package jp.ac.jec.cm0127.a0127rps;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private static final String STATE_WIN_COUNT = "win_count";

    // A small state machine keeps repeated taps from starting overlapping rounds.
    private enum GameState {
        CHOOSING,
        READY,
        PLAYING,
        RESULT
    }

    private static final long COUNTDOWN_STEP_MS = 450L;
    private static final float UNSELECTED_HAND_ALPHA = 0.35f;
    private static final long HAND_FOCUS_ANIMATION_MS = 180L;
    private static final float CPU_RESULT_START_OFFSET_DP = 300f;
    private static final long CPU_RESULT_ANIMATION_MS = 400L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final RpsAnimations animations = new RpsAnimations();
    private AnimatorSet bestRecordAnimator;
    private RpsSoundPlayer soundPlayer;
    private Runnable countdownRunnable;
    private GameState gameState = GameState.CHOOSING;
    private int selectedItem = -1;
    private int winCount;
    private int bestWinCount;
    private ImageView[] items;
    private final Random random = new Random();
    private final int[] itemImages = {
            R.drawable.j_gu02,
            R.drawable.j_ch02,
            R.drawable.j_pa02,
    };
    private final int[] winMessages = {
            R.string.format_win_standard,
            R.string.format_win_read,
    };
    private final int[] loseMessages = {
            R.string.lose,
            R.string.lose_close,
    };
    private final int[] drawMessages = {
            R.string.draw,
            R.string.draw_again,
    };
    private final int[] countdownMessages = {
            R.string.countdown_jan,
            R.string.countdown_ken,
            R.string.countdown_pon,
    };
    private TextView txtInfo;
    private TextView txtBestStreak;
    private TextView txtStreakTitle;
    private Button btnStart;
    private Button btnNext;
    private ViewGroup contentArea;
    private View handArea;
    private View cpuResult;
    private ImageView imgCpu;

    // Initializes the screen, saved settings, game data, and click handlers.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Apply saved visual settings before inflating views to avoid visible flashes.
        RpsSettingsMenu.applySavedLanguage(this);
        RpsSettingsMenu.applySavedTheme(this);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        soundPlayer = new RpsSoundPlayer(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtInfo = findViewById(R.id.txtInfo);
        txtBestStreak = findViewById(R.id.txtBestStreak);
        txtStreakTitle = findViewById(R.id.txtStreakTitle);
        btnStart = findViewById(R.id.btnStart);
        btnNext = findViewById(R.id.btnNext);
        contentArea = findViewById(R.id.contentArea);
        handArea = findViewById(R.id.handArea);
        cpuResult = findViewById(R.id.cpuResult);
        imgCpu = findViewById(R.id.imgCpu);
        configureCpuResultPosition();

        findViewById(R.id.btnSettings).setOnClickListener(
                anchor -> RpsSettingsMenu.show(this, anchor, this::resetBestRecord)
        );

        items = new ImageView[]{
                findViewById(R.id.imgGu),
                findViewById(R.id.imgCh),
                findViewById(R.id.imgPa),
        };

        bestWinCount = RpsPreferences.getBestWinCount(this);
        if (savedInstanceState != null) {
            winCount = savedInstanceState.getInt(STATE_WIN_COUNT, 0);
        }
        updateBestStreakView();

        View.OnClickListener itemSelectListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectItem(v);
            }
        };

        for (ImageView item : items) {
            item.setOnClickListener(itemSelectListener);
        }

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (gameState != GameState.READY) {
                    return;
                }
                // Lock the round immediately so fast taps cannot restart the countdown.
                gameState = GameState.PLAYING;

                soundPlayer.playThrow();
                final int cpu = random.nextInt(3);
                imgCpu.setImageResource(itemImages[cpu]);

                prepareRound();
                startCountdown(cpu);
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (gameState != GameState.RESULT) {
                    return;
                }
                resetGameView();
            }
        });

        resetGameView();
        restoreCurrentStreakView();
    }

    // Clears the saved best streak and updates the best record label.
    private void resetBestRecord() {
        cancelBestRecordAnimation();
        bestWinCount = 0;
        RpsPreferences.resetBestWinCount(this);
        updateBestStreakView();
    }

    // Releases pending animations, countdown tasks, and sound resources.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelCountdown();
        cancelBestRecordAnimation();
        animations.cancelResult();
        if (soundPlayer != null) {
            soundPlayer.release();
        }
    }

    // Saves the current streak when Android recreates this Activity.
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_WIN_COUNT, winCount);
        super.onSaveInstanceState(outState);
    }

    // Handles the player's hand selection before a round starts.
    private void selectItem(View selectedView) {
        if (gameState != GameState.CHOOSING && gameState != GameState.READY) {
            return;
        }

        for (int i = 0; i < items.length; i++) {
            if (items[i] == selectedView) {
                items[i].setBackgroundResource(R.drawable.bg_hand_selected);
                selectedItem = i;
                animations.animateSelectedHand(items[i]);
            } else {
                items[i].setBackgroundResource(R.drawable.bg_hand_normal);
                animations.resetHandScale(items[i]);
            }
        }

        gameState = GameState.READY;
        btnStart.setVisibility(View.VISIBLE);
        btnStart.setText(R.string.start);
        btnStart.setEnabled(true);
        btnNext.setVisibility(View.GONE);
        btnNext.setEnabled(false);
    }

    // Resets the screen to the initial hand-selection state.
    private void resetGameView() {
        gameState = GameState.CHOOSING;
        selectedItem = -1;
        cancelCountdown();
        animations.resetResult(txtInfo);
        txtInfo.setText(getString(R.string.default_message));
        txtInfo.setTextColor(getColor(R.color.rps_text_primary));
        hideStreakInfo();
        hideCpuResult();
        btnStart.setVisibility(View.VISIBLE);
        btnStart.setText(R.string.select_hand_prompt);
        btnStart.setEnabled(false);
        btnNext.setVisibility(View.GONE);
        btnNext.setEnabled(false);
        for (ImageView jankenImg : items) {
            jankenImg.setEnabled(true);
            jankenImg.setAlpha(1f);
            jankenImg.setBackgroundResource(R.drawable.bg_hand_normal);
            animations.resetHandScale(jankenImg);
        }
    }

    // Restores the streak title after an Activity recreation.
    private void restoreCurrentStreakView() {
        if (winCount > 0) {
            showStreakInfo();
        }
    }

    // Locks the chosen hand and prepares the UI for countdown.
    private void prepareRound() {
        cancelCountdown();
        animations.resetResult(txtInfo);
        hideStreakInfo();
        hideCpuResult();
        btnStart.setEnabled(false);
        btnNext.setVisibility(View.GONE);
        btnNext.setEnabled(false);
        for (int i = 0; i < items.length; i++) {
            ImageView jankenImg = items[i];
            jankenImg.setEnabled(false);
            // Keep the player's chosen hand clear while dimming the other two choices.
            jankenImg.animate()
                    .alpha(i == selectedItem ? 1f : UNSELECTED_HAND_ALPHA)
                    .setDuration(HAND_FOCUS_ANIMATION_MS)
                    .start();
        }
    }

    // Starts the janken countdown from the first step.
    private void startCountdown(int cpu) {
        showCountdownStep(cpu, 0);
    }

    // Shows one countdown step, then schedules the next one.
    private void showCountdownStep(int cpu, int step) {
        if (gameState != GameState.PLAYING) {
            return;
        }

        if (step == countdownMessages.length) {
            countdownRunnable = null;
            showRoundResult(cpu);
            return;
        }

        animations.resetResult(txtInfo);
        txtInfo.setText(countdownMessages[step]);
        txtInfo.setTextColor(getColor(R.color.rps_text_primary));
        soundPlayer.playCountdownStep(step);

        // Advance the janken chant one step at a time before revealing the CPU hand.
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                showCountdownStep(cpu, step + 1);
            }
        };
        handler.postDelayed(countdownRunnable, COUNTDOWN_STEP_MS);
    }

    // Reveals the CPU hand before showing the round result.
    private void showRoundResult(int cpu) {
        if (gameState != GameState.PLAYING) {
            return;
        }

        animations.resetResult(txtInfo);
        txtInfo.setAlpha(0f);
        btnStart.setVisibility(View.GONE);
        btnStart.setEnabled(false);
        // Reveal the CPU hand first, then show the result text for a clearer rhythm.
        showCpuResult(new Runnable() {
            @Override
            public void run() {
                if (gameState != GameState.PLAYING) {
                    return;
                }

                judgeJanken(cpu);
                gameState = GameState.RESULT;
                btnNext.setVisibility(View.VISIBLE);
                btnNext.setEnabled(true);
            }
        });
    }

    // Cancels any countdown callback waiting in the main thread queue.
    private void cancelCountdown() {
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
            countdownRunnable = null;
        }
    }

    // Judges the round and updates result text, streaks, sounds, and animations.
    private void judgeJanken(int cpu) {
        animations.resetResult(txtInfo);
        RpsGameRules.Result result = RpsGameRules.judge(selectedItem, cpu);
        winCount = RpsGameRules.nextWinCount(winCount, result);

        if (result == RpsGameRules.Result.DRAW) {
            txtInfo.setText(getString(getRandomMessage(drawMessages)));
            txtInfo.setTextColor(getColor(R.color.rps_result_draw));
            hideStreakInfo();
            soundPlayer.playDraw();
            animations.animateDraw(txtInfo);
        } else if (result == RpsGameRules.Result.WIN) {
            txtInfo.setText(getString(getRandomMessage(winMessages), winCount));
            txtInfo.setTextColor(getColor(R.color.rps_result_win));
            updateBestWinCount();
            showStreakInfo();
            soundPlayer.playWin();
            animations.animateWin(txtInfo);
        } else {
            txtInfo.setText(getString(getRandomMessage(loseMessages)));
            txtInfo.setTextColor(getColor(R.color.rps_result_lose));
            hideStreakInfo();
            soundPlayer.playLose();
            animations.animateLose(txtInfo);
        }
    }

    // Picks one message resource from a small result-message list.
    private int getRandomMessage(int[] messages) {
        return messages[random.nextInt(messages.length)];
    }

    // Saves a new best streak only when the current streak beats the old record.
    private void updateBestWinCount() {
        int updatedBestWinCount = RpsGameRules.nextBestWinCount(bestWinCount, winCount);
        if (updatedBestWinCount == bestWinCount) {
            return;
        }

        bestWinCount = updatedBestWinCount;
        RpsPreferences.setBestWinCount(this, bestWinCount);
        showNewBestRecord();
    }

    // Displays the normal best streak label.
    private void updateBestStreakView() {
        txtBestStreak.setText(getString(R.string.format_best_streak, bestWinCount));
        txtBestStreak.setTextColor(getColor(R.color.rps_text_muted));
        txtBestStreak.setAlpha(1f);
    }

    // Shows a short NEW! flash when the best streak is updated.
    private void showNewBestRecord() {
        cancelBestRecordAnimation();
        txtBestStreak.setText(getString(R.string.format_best_streak_new, bestWinCount));
        txtBestStreak.setTextColor(getColor(R.color.rps_result_win));
        txtBestStreak.setAlpha(1f);

        // Flash the best record briefly, then restore the normal record label.
        ObjectAnimator flash = ObjectAnimator.ofFloat(
                txtBestStreak,
                "alpha",
                1f,
                0.25f,
                1f,
                0.4f,
                1f
        );

        AnimatorSet animation = new AnimatorSet();
        bestRecordAnimator = animation;
        animation.setDuration(900);
        animation.playTogether(flash);
        animation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                if (bestRecordAnimator != animation) {
                    return;
                }

                bestRecordAnimator = null;
                updateBestStreakView();
            }
        });
        animation.start();
    }

    // Safely stops the best-record flash animation.
    private void cancelBestRecordAnimation() {
        if (bestRecordAnimator == null) {
            return;
        }

        AnimatorSet animation = bestRecordAnimator;
        bestRecordAnimator = null;
        animation.cancel();
    }

    // Displays the streak title that matches the current win count.
    private void showStreakInfo() {
        if (winCount >= 5) {
            txtStreakTitle.setText(getString(R.string.streak_title_god_hand));
        } else if (winCount >= 3) {
            txtStreakTitle.setText(getString(R.string.streak_title_master));
        } else {
            txtStreakTitle.setText(getString(R.string.streak_title_beginner));
        }

        txtStreakTitle.setVisibility(View.VISIBLE);
    }

    // Hides the streak title when the streak is not active.
    private void hideStreakInfo() {
        txtStreakTitle.setVisibility(View.GONE);
    }

    // Animates the CPU result block into view, then runs the next step.
    private void showCpuResult(Runnable onRevealComplete) {
        cpuResult.animate().setListener(null);
        cpuResult.animate().cancel();
        cpuResult.setAlpha(0f);
        resetCpuResultStartPosition();
        cpuResult.setVisibility(View.VISIBLE);
        cpuResult.animate()
                .alpha(1f)
                .translationX(0f)
                .translationY(0f)
                .setDuration(CPU_RESULT_ANIMATION_MS)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        cpuResult.animate().setListener(null);
                        onRevealComplete.run();
                    }
                })
                .start();
    }

    // Hides and resets the CPU result block for the next round.
    private void hideCpuResult() {
        cpuResult.animate().setListener(null);
        cpuResult.animate().cancel();
        cpuResult.setVisibility(View.GONE);
        cpuResult.setAlpha(0f);
        resetCpuResultStartPosition();
    }

    // Moves the CPU result block above the player's hands after layout inflation.
    private void configureCpuResultPosition() {
        // Place the CPU result above the player's hands without complicating the XML layout.
        contentArea.removeView(cpuResult);
        int handAreaIndex = contentArea.indexOfChild(handArea);
        contentArea.addView(cpuResult, handAreaIndex);

        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) cpuResult.getLayoutParams();
        params.topMargin = Math.round(dpToPx(36f));
        cpuResult.setLayoutParams(params);
    }

    // Places the CPU result block offscreen so it can slide in from the left.
    private void resetCpuResultStartPosition() {
        float offset = dpToPx(CPU_RESULT_START_OFFSET_DP);
        cpuResult.setTranslationX(-offset);
        cpuResult.setTranslationY(0f);
    }

    // Converts density-independent pixels to actual screen pixels.
    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

}
