package io.github.made2kode.sportradar.scoreboard;

/** Raised when an operation targets a match that is not active. */
public final class MatchNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MatchNotFoundException(MatchId matchId) {
        super("Active match not found: " + matchId.value());
    }
}
