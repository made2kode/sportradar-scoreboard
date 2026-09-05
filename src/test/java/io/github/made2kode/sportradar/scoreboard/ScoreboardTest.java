package io.github.made2kode.sportradar.scoreboard;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class ScoreboardTest {

    @Test
    void startsMultipleMatchesAtZero() {
        Scoreboard scoreboard = new Scoreboard();

        MatchSnapshot first = scoreboard.startMatch("Mexico", "Canada");
        MatchSnapshot second = scoreboard.startMatch("Spain", "Brazil");

        assertEquals(new MatchId(1), first.id());
        assertEquals(new MatchId(2), second.id());
        assertEquals(0, first.homeScore());
        assertEquals(0, first.awayScore());
        assertEquals(0, first.version());
        assertEquals(2, scoreboard.getSummary().size());
    }

    @Test
    void updatesScoreAndIncrementsVersion() {
        Scoreboard scoreboard = new Scoreboard();
        MatchSnapshot started = scoreboard.startMatch("Poland", "Germany");

        MatchSnapshot updated = scoreboard.updateScore(started.id(), 2, 1, started.version());

        assertEquals(2, updated.homeScore());
        assertEquals(1, updated.awayScore());
        assertEquals(1, updated.version());
    }

    @Test
    void acceptsScoreCorrections() {
        Scoreboard scoreboard = new Scoreboard();
        MatchSnapshot started = scoreboard.startMatch("Poland", "Germany");
        MatchSnapshot scored = scoreboard.updateScore(started.id(), 2, 1, started.version());

        MatchSnapshot corrected = scoreboard.updateScore(scored.id(), 1, 1, scored.version());

        assertEquals(1, corrected.homeScore());
        assertEquals(1, corrected.awayScore());
        assertEquals(2, corrected.version());
    }

    @Test
    void rejectsAStaleUpdateWithVersionDetails() {
        Scoreboard scoreboard = new Scoreboard();
        MatchSnapshot started = scoreboard.startMatch("Poland", "Germany");
        scoreboard.updateScore(started.id(), 1, 0, started.version());

        OptimisticLockException exception = assertThrows(
                OptimisticLockException.class,
                () -> scoreboard.updateScore(started.id(), 2, 0, started.version()));

        assertEquals(started.id(), exception.matchId());
        assertEquals(0, exception.expectedVersion());
        assertEquals(1, exception.actualVersion());
    }

    @Test
    void onlyOneConcurrentWriterCanUseTheSameVersion() throws Exception {
        Scoreboard scoreboard = new Scoreboard();
        MatchSnapshot started = scoreboard.startMatch("Poland", "Germany");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Object> first = executor.submit(
                    () -> updateAfter(start, scoreboard, started, 1, 0));
            Future<Object> second = executor.submit(
                    () -> updateAfter(start, scoreboard, started, 0, 1));

            start.countDown();
            List<Object> results = List.of(first.get(5, SECONDS), second.get(5, SECONDS));

            assertEquals(1, results.stream().filter(MatchSnapshot.class::isInstance).count());
            assertEquals(1, results.stream().filter(OptimisticLockException.class::isInstance).count());
            assertEquals(1, scoreboard.getSummary().getFirst().version());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, SECONDS));
        }
    }

    @Test
    void finishesAnActiveMatch() {
        Scoreboard scoreboard = new Scoreboard();
        MatchSnapshot started = scoreboard.startMatch("Poland", "Germany");

        scoreboard.finishMatch(started.id(), started.version());

        assertTrue(scoreboard.getSummary().isEmpty());
        assertThrows(
                MatchNotFoundException.class,
                () -> scoreboard.updateScore(started.id(), 1, 0, started.version()));
    }

    @Test
    void rejectsFinishingAStaleVersion() {
        Scoreboard scoreboard = new Scoreboard();
        MatchSnapshot started = scoreboard.startMatch("Poland", "Germany");
        MatchSnapshot updated = scoreboard.updateScore(started.id(), 1, 0, started.version());

        assertThrows(
                OptimisticLockException.class,
                () -> scoreboard.finishMatch(started.id(), started.version()));

        scoreboard.finishMatch(updated.id(), updated.version());
        assertTrue(scoreboard.getSummary().isEmpty());
    }

    @Test
    void reproducesTheRequiredSummaryOrdering() {
        Scoreboard scoreboard = new Scoreboard();
        MatchSnapshot mexico = scoreboard.startMatch("Mexico", "Canada");
        MatchSnapshot spain = scoreboard.startMatch("Spain", "Brazil");
        MatchSnapshot germany = scoreboard.startMatch("Germany", "France");
        MatchSnapshot uruguay = scoreboard.startMatch("Uruguay", "Italy");
        MatchSnapshot argentina = scoreboard.startMatch("Argentina", "Australia");

        scoreboard.updateScore(mexico.id(), 0, 5, mexico.version());
        scoreboard.updateScore(spain.id(), 10, 2, spain.version());
        scoreboard.updateScore(germany.id(), 2, 2, germany.version());
        scoreboard.updateScore(uruguay.id(), 6, 6, uruguay.version());
        scoreboard.updateScore(argentina.id(), 3, 1, argentina.version());

        assertEquals(
                List.of("Uruguay", "Spain", "Mexico", "Argentina", "Germany"),
                scoreboard.getSummary().stream().map(MatchSnapshot::homeTeam).toList());
    }

    @Test
    void summaryCannotBeModifiedByTheCaller() {
        Scoreboard scoreboard = new Scoreboard();
        scoreboard.startMatch("Poland", "Germany");

        List<MatchSnapshot> summary = scoreboard.getSummary();

        assertThrows(UnsupportedOperationException.class, summary::clear);
    }

    @Test
    void rejectsInvalidInput() {
        Scoreboard scoreboard = new Scoreboard();

        assertThrows(NullPointerException.class, () -> scoreboard.startMatch(null, "Germany"));
        assertThrows(IllegalArgumentException.class, () -> scoreboard.startMatch(" ", "Germany"));
        assertThrows(IllegalArgumentException.class, () -> scoreboard.startMatch("Poland", "Poland"));
        MatchSnapshot match = scoreboard.startMatch("Poland", "Germany");
        assertThrows(
                IllegalArgumentException.class,
                () -> scoreboard.updateScore(match.id(), -1, 0, match.version()));
        assertThrows(
                IllegalArgumentException.class,
                () -> scoreboard.updateScore(match.id(), 1, 0, -1));
    }

    @Test
    void reportsUnknownMatches() {
        Scoreboard scoreboard = new Scoreboard();
        MatchId unknown = new MatchId(404);

        assertThrows(MatchNotFoundException.class, () -> scoreboard.finishMatch(unknown, 0));
        assertThrows(
                MatchNotFoundException.class,
                () -> scoreboard.updateScore(unknown, 1, 0, 0));
    }

    private static Object updateAfter(
            CountDownLatch start,
            Scoreboard scoreboard,
            MatchSnapshot match,
            int homeScore,
            int awayScore) throws InterruptedException {
        start.await();
        try {
            return scoreboard.updateScore(match.id(), homeScore, awayScore, match.version());
        } catch (OptimisticLockException exception) {
            return exception;
        }
    }
}
