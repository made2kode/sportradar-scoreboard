package io.github.made2kode.sportradar.scoreboard;

import org.junit.jupiter.api.Test;

import static io.github.made2kode.sportradar.scoreboard.TestFixtures.aMatch;
import static io.github.made2kode.sportradar.scoreboard.TestFixtures.aMatchSnapshot;
import static org.junit.jupiter.api.Assertions.*;

class MatchTest {

    @Test
    void throws_when_match_id_is_not_positive() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new MatchId(-1)),
                () -> assertThrows(IllegalArgumentException.class, () -> new MatchId(0)));
    }

    @Test
    void starts_with_zero_score_and_version() {
        Match match = Match.start(
                new MatchId(7), "Poland", "Germany", 3);

        assertEquals(new MatchSnapshot(new MatchId(7), "Poland", "Germany", 0, 0, 0),
                match.snapshot());
    }

    @Test
    void updating_creates_new_aggregate_version_without_mutating_original() {
        Match original = aMatch();

        Match updated = original.updateScore(new Score(2, 1));

        assertAll(
                () -> assertNotSame(original, updated),
                () -> assertEquals(0, original.version()),
                () -> assertEquals(aMatchSnapshot(0, 0, 0), original.snapshot()),
                () -> assertEquals(aMatchSnapshot(2, 1, 1), updated.snapshot()));
    }

    @Test
    void rejects_match_against_same_team() {
        var matchId = new MatchId(1);
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Match.start(matchId, "Poland", "Poland", 1));

        assertEquals("A team cannot play against itself", exception.getMessage());
    }
}
