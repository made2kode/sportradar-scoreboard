package io.github.made2kode.sportradar.scoreboard;

final class TestFixtures {

    private static final MatchId DEFAULT_MATCH_ID = new MatchId(1);
    private static final String DEFAULT_HOME_TEAM = "Poland";
    private static final String DEFAULT_AWAY_TEAM = "Germany";
    private static final long DEFAULT_START_ORDER = 1;

    private TestFixtures() {}

    static Match aMatch() {
        return aMatch(
                DEFAULT_MATCH_ID,
                DEFAULT_HOME_TEAM,
                DEFAULT_AWAY_TEAM,
                DEFAULT_START_ORDER);
    }

    static Match aMatch(
            MatchId id,
            String homeTeam,
            String awayTeam,
            long startOrder) {
        return Match.start(id, homeTeam, awayTeam, startOrder);
    }

    static MatchSnapshot aMatchSnapshot(int homeScore, int awayScore, long version) {
        return new MatchSnapshot(
                DEFAULT_MATCH_ID,
                DEFAULT_HOME_TEAM,
                DEFAULT_AWAY_TEAM,
                homeScore,
                awayScore,
                version);
    }
}
