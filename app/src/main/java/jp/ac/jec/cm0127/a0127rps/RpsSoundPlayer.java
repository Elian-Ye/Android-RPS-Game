package jp.ac.jec.cm0127.a0127rps;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.SparseBooleanArray;

final class RpsSoundPlayer {
    private static final int MAX_STREAMS = 4;
    private static final float VOLUME = 1f;

    private final SoundPool soundPool;
    private final SparseBooleanArray loadedSounds = new SparseBooleanArray();
    private final int throwSoundId;
    private final int[] countdownSoundIds;
    private final int winSoundId;
    private final int loseSoundId;
    private final int drawSoundId;

    RpsSoundPlayer(Context context) {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(MAX_STREAMS)
                .setAudioAttributes(audioAttributes)
                .build();
        soundPool.setOnLoadCompleteListener((pool, sampleId, status) ->
                loadedSounds.put(sampleId, status == 0));

        // Short raw sounds keep this class focused on playback instead of audio synthesis.
        throwSoundId = soundPool.load(context, R.raw.throw_sound, 1);
        countdownSoundIds = new int[]{
                soundPool.load(context, R.raw.countdown_1, 1),
                soundPool.load(context, R.raw.countdown_2, 1),
                soundPool.load(context, R.raw.countdown_3, 1),
        };
        winSoundId = soundPool.load(context, R.raw.win, 1);
        loseSoundId = soundPool.load(context, R.raw.lose, 1);
        drawSoundId = soundPool.load(context, R.raw.draw, 1);
    }

    void playThrow() {
        play(throwSoundId);
    }

    void playCountdownStep(int step) {
        int soundIndex = Math.max(0, Math.min(step, countdownSoundIds.length - 1));
        play(countdownSoundIds[soundIndex]);
    }

    void playWin() {
        play(winSoundId);
    }

    void playLose() {
        play(loseSoundId);
    }

    void playDraw() {
        play(drawSoundId);
    }

    void release() {
        loadedSounds.clear();
        soundPool.release();
    }

    private void play(int soundId) {
        if (!loadedSounds.get(soundId, false)) {
            return;
        }

        soundPool.play(soundId, VOLUME, VOLUME, 1, 0, 1f);
    }
}
