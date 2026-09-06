package io.github.made2kode.sportradar.scoreboard;

/**
 * Raised when a write is based on a stale aggregate version.
 */
public final class OptimisticLockException extends RuntimeException {

    private final MatchId matchId;
    private final long expectedVersion;
    private final long actualVersion;

    public OptimisticLockException(MatchId matchId, long expectedVersion, long actualVersion) {
        super("Stale match version for %d: expected %d but was %d"
                .formatted(matchId.value(), expectedVersion, actualVersion));
        this.matchId = matchId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }

    public MatchId matchId() {
        return matchId;
    }

    public long expectedVersion() {
        return expectedVersion;
    }

    public long actualVersion() {
        return actualVersion;
    }
}
