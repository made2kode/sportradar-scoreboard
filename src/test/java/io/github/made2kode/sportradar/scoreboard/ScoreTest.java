package io.github.made2kode.sportradar.scoreboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ScoreTest {

    @Test
    void calculatesTotal() {
        assertEquals(12, new Score(10, 2).total());
    }

    @Test
    void rejectsNegativeScores() {
        assertThrows(IllegalArgumentException.class, () -> new Score(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Score(0, -1));
    }

    @Test
    void rejectsATotalThatCannotBeRepresented() {
        assertThrows(ArithmeticException.class, () -> new Score(Integer.MAX_VALUE, 1));
    }
}
