package jp.ac.jec.cm0127.a0127rps;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class RpsGameRulesTest {
    private static final int ROCK = 0;
    private static final int SCISSORS = 1;
    private static final int PAPER = 2;

    @Test
    public void judge_allWinningCombinations_returnWin() {
        assertEquals(RpsGameRules.Result.WIN, RpsGameRules.judge(ROCK, SCISSORS));
        assertEquals(RpsGameRules.Result.WIN, RpsGameRules.judge(SCISSORS, PAPER));
        assertEquals(RpsGameRules.Result.WIN, RpsGameRules.judge(PAPER, ROCK));
    }

    @Test
    public void judge_allLosingCombinations_returnLose() {
        assertEquals(RpsGameRules.Result.LOSE, RpsGameRules.judge(ROCK, PAPER));
        assertEquals(RpsGameRules.Result.LOSE, RpsGameRules.judge(SCISSORS, ROCK));
        assertEquals(RpsGameRules.Result.LOSE, RpsGameRules.judge(PAPER, SCISSORS));
    }

    @Test
    public void judge_matchingHands_returnDraw() {
        assertEquals(RpsGameRules.Result.DRAW, RpsGameRules.judge(ROCK, ROCK));
        assertEquals(RpsGameRules.Result.DRAW, RpsGameRules.judge(SCISSORS, SCISSORS));
        assertEquals(RpsGameRules.Result.DRAW, RpsGameRules.judge(PAPER, PAPER));
    }

    @Test
    public void judge_invalidHand_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> RpsGameRules.judge(-1, ROCK));
        assertThrows(IllegalArgumentException.class, () -> RpsGameRules.judge(ROCK, 3));
    }

    @Test
    public void nextWinCount_winIncrementsCurrentStreak() {
        assertEquals(1, RpsGameRules.nextWinCount(0, RpsGameRules.Result.WIN));
        assertEquals(4, RpsGameRules.nextWinCount(3, RpsGameRules.Result.WIN));
    }

    @Test
    public void nextWinCount_lossOrDraw_resetsStreak() {
        assertEquals(0, RpsGameRules.nextWinCount(5, RpsGameRules.Result.LOSE));
        assertEquals(0, RpsGameRules.nextWinCount(5, RpsGameRules.Result.DRAW));
    }

    @Test
    public void nextBestWinCount_onlyUpdatesForNewRecord() {
        assertEquals(5, RpsGameRules.nextBestWinCount(5, 3));
        assertEquals(5, RpsGameRules.nextBestWinCount(5, 5));
        assertEquals(6, RpsGameRules.nextBestWinCount(5, 6));
    }
}
