package io.github.made2kode.sportradar.scoreboard;

import java.io.Serial;
import java.io.Serializable;

/** Stable identity of a match managed by a {@link Scoreboard}. */
public record MatchId(long value) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public MatchId {
        if (value <= 0) {
            throw new IllegalArgumentException("Match id must be positive");
        }
    }
}
