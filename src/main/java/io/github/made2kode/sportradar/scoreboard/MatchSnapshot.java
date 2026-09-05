package io.github.made2kode.sportradar.scoreboard;

import java.util.Objects;

/** Immutable view of an active match at a particular aggregate version. */
public record MatchSnapshot(
        MatchId id,
        String homeTeam,
        String awayTeam,
        int homeScore,
        int awayScore,
        long version) {

    public MatchSnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(homeTeam, "homeTeam");
        Objects.requireNonNull(awayTeam, "awayTeam");
        if (homeScore < 0 || awayScore < 0) {
            throw new IllegalArgumentException("Scores cannot be negative");
        }
        Math.addExact(homeScore, awayScore);
        if (version < 0) {
            throw new IllegalArgumentException("Version cannot be negative");
        }
    }

    /** Returns the sum used as the primary summary ordering key. */
    public int totalScore() {
        return Math.addExact(homeScore, awayScore);
    }
}
