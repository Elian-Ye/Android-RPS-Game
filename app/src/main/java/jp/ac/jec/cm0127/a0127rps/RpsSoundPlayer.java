package jp.ac.jec.cm0127.a0127rps;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;

final class RpsSoundPlayer {
    private static final int VOLUME = 70;
    private static final int SAMPLE_RATE = 44100;
    // The victory sound is generated in code so the project does not need audio assets.
    private static final int WIN_NOTE_DURATION_MS = 95;
    private static final int WIN_NOTE_GAP_MS = 25;
    private static final int WIN_TAIL_MS = 80;
    private static final int WIN_RELEASE_DELAY_MS = 520;
    private static final double WIN_VOLUME = 0.32;
    private static final int COUNTDOWN_BEEP_DURATION_MS = 42;
    private static final int COUNTDOWN_FIRST_BEEP_DELAY_MS = 90;
    private static final int COUNTDOWN_BEEP_RELEASE_DELAY_MS = 120;
    private static final double COUNTDOWN_BEEP_VOLUME = 0.16;
    private static final double[] WIN_NOTE_FREQUENCIES = {
            523.25,
            659.25,
            783.99,
            1046.50
    };
    private static final double[] COUNTDOWN_BEEP_FREQUENCIES = {
            659.25,
            739.99,
            880.00
    };

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final byte[] victoryMelody = createVictoryMelody();
    private final byte[][] countdownBeeps = createCountdownBeeps();

    private ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME);
    private AudioTrack victoryTrack;
    private AudioTrack countdownTrack;

    void playThrow() {
        playSingleTone(ToneGenerator.TONE_PROP_BEEP, 80);
    }

    void playCountdownStep(int step) {
        cancelScheduledSounds();
        releaseVictoryTrack();
        releaseCountdownTrack();

        int beepIndex = Math.max(0, Math.min(step, countdownBeeps.length - 1));
        long delayMs = step == 0 ? COUNTDOWN_FIRST_BEEP_DELAY_MS : 0L;
        handler.postDelayed(() -> playCountdownBeep(beepIndex), delayMs);
    }

    void playWin() {
        cancelScheduledSounds();
        releaseCountdownTrack();
        playVictoryMelody();
    }

    void playLose() {
        playSingleTone(ToneGenerator.TONE_PROP_NACK, 220);
    }

    void playDraw() {
        playSingleTone(ToneGenerator.TONE_PROP_PROMPT, 160);
    }

    void release() {
        cancelScheduledSounds();
        releaseVictoryTrack();
        releaseCountdownTrack();

        if (toneGenerator == null) {
            return;
        }

        toneGenerator.release();
        toneGenerator = null;
    }

    private void playSingleTone(int toneType, int durationMs) {
        cancelScheduledSounds();
        releaseVictoryTrack();
        releaseCountdownTrack();
        playTone(toneType, durationMs);
    }

    private void cancelScheduledSounds() {
        handler.removeCallbacksAndMessages(null);
    }

    private void playTone(int toneType, int durationMs) {
        if (toneGenerator != null) {
            toneGenerator.startTone(toneType, durationMs);
        }
    }

    private void playVictoryMelody() {
        releaseVictoryTrack();
        releaseCountdownTrack();

        try {
            // MODE_STATIC works well for very short sounds that are fully prepared in memory.
            victoryTrack = createStaticTrack(victoryMelody);
            victoryTrack.write(victoryMelody, 0, victoryMelody.length);
            victoryTrack.play();
            handler.postDelayed(this::releaseVictoryTrack, WIN_RELEASE_DELAY_MS);
        } catch (IllegalArgumentException | IllegalStateException e) {
            releaseVictoryTrack();
            playTone(ToneGenerator.TONE_SUP_CONFIRM, 180);
        }
    }

    private void playCountdownBeep(int beepIndex) {
        releaseCountdownTrack();

        try {
            byte[] beep = countdownBeeps[beepIndex];
            countdownTrack = createStaticTrack(beep);
            countdownTrack.write(beep, 0, beep.length);
            countdownTrack.play();
            handler.postDelayed(this::releaseCountdownTrack, COUNTDOWN_BEEP_RELEASE_DELAY_MS);
        } catch (IllegalArgumentException | IllegalStateException e) {
            releaseCountdownTrack();
            playTone(ToneGenerator.TONE_PROP_BEEP, COUNTDOWN_BEEP_DURATION_MS);
        }
    }

    private AudioTrack createStaticTrack(byte[] audioData) {
        return new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(audioData.length)
                .build();
    }

    private void releaseVictoryTrack() {
        if (victoryTrack == null) {
            return;
        }

        try {
            victoryTrack.stop();
        } catch (IllegalStateException ignored) {
            // The track may already have finished.
        }

        victoryTrack.release();
        victoryTrack = null;
    }

    private void releaseCountdownTrack() {
        if (countdownTrack == null) {
            return;
        }

        try {
            countdownTrack.stop();
        } catch (IllegalStateException ignored) {
            // The track may already have finished.
        }

        countdownTrack.release();
        countdownTrack = null;
    }

    private static byte[] createVictoryMelody() {
        int noteSamples = msToSamples(WIN_NOTE_DURATION_MS);
        int gapSamples = msToSamples(WIN_NOTE_GAP_MS);
        int tailSamples = msToSamples(WIN_TAIL_MS);
        int totalSamples = WIN_NOTE_FREQUENCIES.length * noteSamples
                + (WIN_NOTE_FREQUENCIES.length - 1) * gapSamples
                + tailSamples;
        byte[] audioData = new byte[totalSamples * 2];
        int sampleIndex = 0;

        for (int i = 0; i < WIN_NOTE_FREQUENCIES.length; i++) {
            double frequency = WIN_NOTE_FREQUENCIES[i];

            for (int j = 0; j < noteSamples; j++) {
                double phase = 2.0 * Math.PI * frequency * j / SAMPLE_RATE;
                double wave = Math.sin(phase) + 0.35 * Math.sin(phase * 2.0);
                double envelope = getEnvelope(j, noteSamples);
                // AudioTrack expects 16-bit little-endian PCM data.
                short sample = (short) (wave / 1.35 * Short.MAX_VALUE * WIN_VOLUME * envelope);
                int byteIndex = sampleIndex * 2;
                audioData[byteIndex] = (byte) (sample & 0xff);
                audioData[byteIndex + 1] = (byte) ((sample >> 8) & 0xff);
                sampleIndex++;
            }

            if (i < WIN_NOTE_FREQUENCIES.length - 1) {
                sampleIndex += gapSamples;
            }
        }

        return audioData;
    }

    private static byte[][] createCountdownBeeps() {
        byte[][] beeps = new byte[COUNTDOWN_BEEP_FREQUENCIES.length][];
        for (int i = 0; i < COUNTDOWN_BEEP_FREQUENCIES.length; i++) {
            beeps[i] = createCountdownBeep(COUNTDOWN_BEEP_FREQUENCIES[i]);
        }
        return beeps;
    }

    private static byte[] createCountdownBeep(double frequency) {
        int samples = msToSamples(COUNTDOWN_BEEP_DURATION_MS);
        byte[] audioData = new byte[samples * 2];

        for (int i = 0; i < samples; i++) {
            double phase = 2.0 * Math.PI * frequency * i / SAMPLE_RATE;
            double envelope = getEnvelope(i, samples);
            short sample = (short) (Math.sin(phase) * Short.MAX_VALUE
                    * COUNTDOWN_BEEP_VOLUME * envelope);
            int byteIndex = i * 2;
            audioData[byteIndex] = (byte) (sample & 0xff);
            audioData[byteIndex + 1] = (byte) ((sample >> 8) & 0xff);
        }

        return audioData;
    }

    private static int msToSamples(int durationMs) {
        return SAMPLE_RATE * durationMs / 1000;
    }

    private static double getEnvelope(int sampleIndex, int noteSamples) {
        int fadeSamples = msToSamples(8);
        // A tiny fade-in/fade-out prevents clicks at the edges of generated tones.
        if (sampleIndex < fadeSamples) {
            return (double) sampleIndex / fadeSamples;
        }

        int samplesFromEnd = noteSamples - sampleIndex - 1;
        if (samplesFromEnd < fadeSamples) {
            return (double) samplesFromEnd / fadeSamples;
        }

        return 1.0;
    }
}
