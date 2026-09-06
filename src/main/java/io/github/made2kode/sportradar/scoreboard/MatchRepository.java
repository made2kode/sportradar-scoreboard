package io.github.made2kode.sportradar.scoreboard;

import java.util.Collection;
import java.util.Optional;

interface MatchRepository {

    void add(Match match);

    Match save(Match updatedMatch, long expectedVersion);

    void remove(MatchId matchId, long expectedVersion);

    Optional<Match> findById(MatchId matchId);

    Collection<Match> findAll();
}
