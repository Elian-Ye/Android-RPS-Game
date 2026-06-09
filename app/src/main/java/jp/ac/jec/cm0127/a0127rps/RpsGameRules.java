package jp.ac.jec.cm0127.a0127rps;

public final class RpsGameRules {
    public enum Result {
        WIN,
        LOSE,
        DRAW
    }

    // Hand order is rock, scissors, paper. Each hand beats the next one.
    private static final int HAND_COUNT = 3;

    private RpsGameRules() {
        // This class only exposes static rule helpers.
    }

    public static Result judge(int playerHand, int cpuHand) {
        validateHand(playerHand);
        validateHand(cpuHand);

        if (playerHand == cpuHand) {
            return Result.DRAW;
        }

        // With this hand order, the next hand is always the one the player beats.
        return (playerHand + 1) % HAND_COUNT == cpuHand
                ? Result.WIN
                : Result.LOSE;
    }

    public static int nextWinCount(int currentWinCount, Result result) {
        // Any non-winning result breaks the current streak.
        return result == Result.WIN ? currentWinCount + 1 : 0;
    }

    public static int nextBestWinCount(int bestWinCount, int currentWinCount) {
        // The best record only changes when the current streak is higher.
        return Math.max(bestWinCount, currentWinCount);
    }

    private static void validateHand(int hand) {
        // Fail fast if game state ever passes an invalid hand value.
        if (hand < 0 || hand >= HAND_COUNT) {
            throw new IllegalArgumentException("Hand must be between 0 and 2.");
        }
    }
}
