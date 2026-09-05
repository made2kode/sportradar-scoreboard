package io.github.made2kode.sportradar.scoreboard;

import java.util.Collection;
import java.util.Optional;
import java.util.function.UnaryOperator;

interface MatchRepository {

    void add(Match match);

    Match update(MatchId matchId, long expectedVersion, UnaryOperator<Match> transition);

    void remove(MatchId matchId, long expectedVersion);

    Optional<Match> findById(MatchId matchId);

    Collection<Match> findAll();
}
