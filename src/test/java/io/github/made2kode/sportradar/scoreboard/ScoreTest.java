package io.github.made2kode.sportradar.scoreboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ScoreTest {

    @Test
    void calculates_total() {
        assertEquals(12, new Score(10, 2).total());
    }

    @Test
    void rejects_negative_scores() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new Score(-1, 0)),
                () -> assertThrows(IllegalArgumentException.class, () -> new Score(0, -1)));
    }

    @Test
    void rejects_total_that_cannot_be_represented() {
        assertThrows(ArithmeticException.class, () -> new Score(Integer.MAX_VALUE, 1));
    }
}
