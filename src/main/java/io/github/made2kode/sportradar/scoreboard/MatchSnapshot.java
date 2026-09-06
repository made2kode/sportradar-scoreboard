package io.github.made2kode.sportradar.scoreboard;

/**
 * Immutable view of an active match at a particular aggregate version.
 */
public record MatchSnapshot(
        MatchId id,
        String homeTeam,
        String awayTeam,
        int homeScore,
        int awayScore,
        long version) {}
