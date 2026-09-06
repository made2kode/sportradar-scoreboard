package io.github.made2kode.sportradar.scoreboard;

import java.util.Objects;

final class Match {

    private final MatchId id;
    private final TeamName homeTeam;
    private final TeamName awayTeam;
    private final Score score;
    private final long startOrder;
    private final long version;

    private Match(
            MatchId id,
            TeamName homeTeam,
            TeamName awayTeam,
            Score score,
            long startOrder,
            long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.homeTeam = Objects.requireNonNull(homeTeam, "homeTeam");
        this.awayTeam = Objects.requireNonNull(awayTeam, "awayTeam");
        this.score = Objects.requireNonNull(score, "score");
        this.startOrder = startOrder;
        this.version = version;
    }

    static Match start(MatchId id, String homeTeam, String awayTeam, long startOrder) {
        TeamName home = new TeamName(homeTeam);
        TeamName away = new TeamName(awayTeam);
        if (home.equals(away)) {
            throw new IllegalArgumentException("A team cannot play against itself");
        }
        return new Match(id, home, away, Score.ZERO, startOrder, 0);
    }

    Match updateScore(Score newScore) {
        return new Match(
                id,
                homeTeam,
                awayTeam,
                newScore,
                startOrder,
                Math.incrementExact(version));
    }

    MatchId id() {
        return id;
    }

    long version() {
        return version;
    }

    long startOrder() {
        return startOrder;
    }

    int totalScore() {
        return score.total();
    }

    MatchSnapshot snapshot() {
        return new MatchSnapshot(
                id,
                homeTeam.value(),
                awayTeam.value(),
                score.home(),
                score.away(),
                version);
    }
}
