package io.github.made2kode.sportradar.scoreboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TeamNameTest {

    @Test
    void strips_surrounding_whitespace() {
        assertEquals("Poland", new TeamName("  Poland  ").value());
    }

    @Test
    void rejects_null_name() {
        assertThrows(NullPointerException.class, () -> new TeamName(null));
    }

    @Test
    void rejects_blank_name() {
        assertThrows(IllegalArgumentException.class, () -> new TeamName(" \t "));
    }
}
