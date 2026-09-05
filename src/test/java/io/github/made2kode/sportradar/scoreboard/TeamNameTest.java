package io.github.made2kode.sportradar.scoreboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TeamNameTest {

    @Test
    void stripsSurroundingWhitespace() {
        assertEquals("Poland", new TeamName("  Poland  ").value());
    }

    @Test
    void rejectsNullAndBlankNames() {
        assertThrows(NullPointerException.class, () -> new TeamName(null));
        assertThrows(IllegalArgumentException.class, () -> new TeamName(" \t "));
    }
}
