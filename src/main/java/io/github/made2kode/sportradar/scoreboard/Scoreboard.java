package io.github.made2kode.sportradar.scoreboard;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** Thread-safe facade for managing active football matches in memory. */
public final class Scoreboard {

    private static final Comparator<Match> SUMMARY_ORDER = Comparator
            .comparingInt(Match::totalScore)
            .reversed()
            .thenComparing(Comparator.comparingLong(Match::startOrder).reversed());

    private final MatchRepository repository;
    private final AtomicLong idSequence = new AtomicLong();
    private final AtomicLong startOrderSequence = new AtomicLong();

    public Scoreboard() {
        this(new InMemoryMatchRepository());
    }

    Scoreboard(MatchRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /** Starts an active match at 0:0 and version 0. */
    public MatchSnapshot startMatch(String homeTeam, String awayTeam) {
        Match match = Match.start(
                new MatchId(next(idSequence)),
                new TeamName(homeTeam),
                new TeamName(awayTeam),
                next(startOrderSequence));
        repository.add(match);
        return match.snapshot();
    }

    /**
     * Replaces the current score when {@code expectedVersion} is still current.
     * A successful update returns a snapshot whose version is incremented by one.
     */
    public MatchSnapshot updateScore(
            MatchId matchId,
            int homeScore,
            int awayScore,
            long expectedVersion) {
        Score newScore = new Score(homeScore, awayScore);
        return repository
                .update(matchId, expectedVersion, match -> match.updateScore(newScore))
                .snapshot();
    }

    /** Finishes and removes an active match when its version is still current. */
    public void finishMatch(MatchId matchId, long expectedVersion) {
        repository.remove(matchId, expectedVersion);
    }

    /**
     * Returns immutable active-match snapshots ordered by total score descending,
     * then by most recent start first.
     */
    public List<MatchSnapshot> getSummary() {
        return repository.findAll().stream()
                .sorted(SUMMARY_ORDER)
                .map(Match::snapshot)
                .toList();
    }

    /** Returns the latest snapshot of one active match, if it is still in progress. */
    public Optional<MatchSnapshot> getMatch(MatchId matchId) {
        return repository.findById(matchId).map(Match::snapshot);
    }

    private static long next(AtomicLong sequence) {
        return sequence.updateAndGet(Math::incrementExact);
    }
}
