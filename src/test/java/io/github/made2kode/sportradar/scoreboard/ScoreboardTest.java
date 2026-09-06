package io.github.made2kode.sportradar.scoreboard;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertAll;
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

    private final Scoreboard scoreboard = new Scoreboard();

    @Test
    void starts_multiple_matches_at_zero() {
        MatchSnapshot first = scoreboard.startMatch("Mexico", "Canada");
        MatchSnapshot second = scoreboard.startMatch("Spain", "Brazil");

        assertAll(
                () -> assertEquals(new MatchId(1), first.id()),
                () -> assertEquals(new MatchId(2), second.id()),
                () -> assertEquals(0, first.homeScore()),
                () -> assertEquals(0, first.awayScore()),
                () -> assertEquals(0, first.version()),
                () -> assertEquals(2, scoreboard.getSummary().size()));
    }

    @Test
    void updates_score_and_increments_version() {
        MatchSnapshot started = start_match();

        MatchSnapshot updated = scoreboard.updateScore(started.id(), 2, 1, started.version());

        assertAll(
                () -> assertEquals(2, updated.homeScore()),
                () -> assertEquals(1, updated.awayScore()),
                () -> assertEquals(1, updated.version()));
    }

    @Test
    void accepts_score_corrections() {
        MatchSnapshot started = start_match();
        MatchSnapshot scored = scoreboard.updateScore(started.id(), 2, 1, started.version());

        MatchSnapshot corrected = scoreboard.updateScore(scored.id(), 1, 1, scored.version());

        assertAll(
                () -> assertEquals(1, corrected.homeScore()),
                () -> assertEquals(1, corrected.awayScore()),
                () -> assertEquals(2, corrected.version()));
    }

    @Test
    void rejects_stale_update_with_version_details() {
        MatchSnapshot started = start_match();
        scoreboard.updateScore(started.id(), 1, 0, started.version());

        OptimisticLockException exception = assertThrows(
                OptimisticLockException.class,
                () -> scoreboard.updateScore(started.id(), 2, 0, started.version()));

        assertAll(
                () -> assertEquals(started.id(), exception.matchId()),
                () -> assertEquals(0, exception.expectedVersion()),
                () -> assertEquals(1, exception.actualVersion()));
    }

    @Test
    void allows_only_one_concurrent_writer_for_same_version() throws Exception {
        MatchSnapshot started = start_match();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Object> first = executor.submit(
                    () -> update_after(start, scoreboard, started, 1, 0));
            Future<Object> second = executor.submit(
                    () -> update_after(start, scoreboard, started, 0, 1));

            start.countDown();
            List<Object> results = List.of(first.get(5, SECONDS), second.get(5, SECONDS));

            assertAll(
                    () -> assertEquals(
                            1, results.stream().filter(MatchSnapshot.class::isInstance).count()),
                    () -> assertEquals(
                            1,
                            results.stream()
                                    .filter(OptimisticLockException.class::isInstance)
                                    .count()),
                    () -> assertEquals(1, scoreboard.getSummary().getFirst().version()));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, SECONDS));
        }
    }

    @Test
    void finishes_active_match() {
        MatchSnapshot started = start_match();

        scoreboard.finishMatch(started.id(), started.version());

        assertTrue(scoreboard.getSummary().isEmpty());
        assertThrows(
                MatchNotFoundException.class,
                () -> scoreboard.updateScore(started.id(), 1, 0, started.version()));
    }

    @Test
    void rejects_finishing_stale_version() {
        MatchSnapshot started = start_match();
        MatchSnapshot updated = scoreboard.updateScore(started.id(), 1, 0, started.version());

        assertThrows(
                OptimisticLockException.class,
                () -> scoreboard.finishMatch(started.id(), started.version()));

        scoreboard.finishMatch(updated.id(), updated.version());
        assertTrue(scoreboard.getSummary().isEmpty());
    }

    @Test
    void reproduces_required_summary_ordering() {
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
    void summary_cannot_be_modified_by_caller() {
        start_match();

        List<MatchSnapshot> summary = scoreboard.getSummary();

        assertThrows(UnsupportedOperationException.class, summary::clear);
    }

    @Test
    void gets_latest_state_of_active_match() {
        MatchSnapshot started = start_match();
        MatchSnapshot updated = scoreboard.updateScore(started.id(), 2, 1, started.version());

        assertEquals(updated, scoreboard.getMatch(started.id()).orElseThrow());

        scoreboard.finishMatch(updated.id(), updated.version());
        assertTrue(scoreboard.getMatch(updated.id()).isEmpty());
    }

    @Test
    void rejects_null_home_team() {
        assertThrows(NullPointerException.class, () -> scoreboard.startMatch(null, "Germany"));
    }

    @Test
    void rejects_blank_home_team() {
        assertThrows(IllegalArgumentException.class, () -> scoreboard.startMatch(" ", "Germany"));
    }

    @Test
    void rejects_match_against_same_team() {
        assertThrows(IllegalArgumentException.class, () -> scoreboard.startMatch("Poland", "Poland"));
    }

    @Test
    void rejects_negative_score() {
        MatchSnapshot match = start_match();

        assertThrows(
                IllegalArgumentException.class,
                () -> scoreboard.updateScore(match.id(), -1, 0, match.version()));
    }

    @Test
    void rejects_negative_expected_version() {
        MatchSnapshot match = start_match();

        assertThrows(
                IllegalArgumentException.class,
                () -> scoreboard.updateScore(match.id(), 1, 0, -1));
    }

    @Test
    void reports_unknown_matches() {
        MatchId unknown = new MatchId(404);

        assertThrows(MatchNotFoundException.class, () -> scoreboard.finishMatch(unknown, 0));
        assertThrows(
                MatchNotFoundException.class,
                () -> scoreboard.updateScore(unknown, 1, 0, 0));
    }

    private MatchSnapshot start_match() {
        return scoreboard.startMatch("Poland", "Germany");
    }

    private static Object update_after(
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
