package io.github.made2kode.sportradar.scoreboard;

/**
 * Stable identity of a match managed by a {@link Scoreboard}.
 */
public record MatchId(long value) {

    public MatchId {
        if (value <= 0) {
            throw new IllegalArgumentException("Match id must be positive");
        }
    }
}
