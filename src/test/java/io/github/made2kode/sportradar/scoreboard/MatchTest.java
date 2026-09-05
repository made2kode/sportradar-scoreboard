package io.github.made2kode.sportradar.scoreboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MatchTest {

    @Test
    void startsWithZeroScoreAndVersion() {
        Match match = Match.start(
                new MatchId(7), new TeamName("Poland"), new TeamName("Germany"), 3);

        assertEquals(new MatchSnapshot(new MatchId(7), "Poland", "Germany", 0, 0, 0),
                match.snapshot());
    }

    @Test
    void updatingCreatesANewAggregateVersionWithoutMutatingTheOriginal() {
        Match original = Match.start(
                new MatchId(1), new TeamName("Poland"), new TeamName("Germany"), 1);

        Match updated = original.updateScore(new Score(2, 1));

        assertNotSame(original, updated);
        assertEquals(0, original.version());
        assertEquals(new MatchSnapshot(new MatchId(1), "Poland", "Germany", 0, 0, 0),
                original.snapshot());
        assertEquals(new MatchSnapshot(new MatchId(1), "Poland", "Germany", 2, 1, 1),
                updated.snapshot());
    }

    @Test
    void rejectsAMatchAgainstTheSameTeam() {
        TeamName poland = new TeamName("Poland");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Match.start(new MatchId(1), poland, poland, 1));

        assertEquals("A team cannot play against itself", exception.getMessage());
    }
}
