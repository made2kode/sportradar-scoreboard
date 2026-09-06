package io.github.made2kode.sportradar.scoreboard;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class InMemoryMatchRepository implements MatchRepository {

    private final ConcurrentMap<MatchId, Match> matches = new ConcurrentHashMap<>();

    @Override
    public void add(Match match) {
        Objects.requireNonNull(match, "match");
        if (matches.putIfAbsent(match.id(), match) != null) {
            throw new IllegalStateException("Duplicate match id: " + match.id().value());
        }
    }

    @Override
    public Match save(Match updatedMatch, long expectedVersion) {
        Objects.requireNonNull(updatedMatch, "updatedMatch");
        return matches.compute(updatedMatch.id(), (id, current) -> {
            Match existing = requireCurrent(id, current);
            requireVersion(existing, expectedVersion);
            requireNextVersion(existing, updatedMatch);
            return updatedMatch;
        });
    }

    @Override
    public void remove(MatchId matchId, long expectedVersion) {
        Objects.requireNonNull(matchId, "matchId");
        matches.compute(matchId, (id, current) -> {
            Match existing = requireCurrent(id, current);
            requireVersion(existing, expectedVersion);
            return null;
        });
    }

    @Override
    public Optional<Match> findById(MatchId matchId) {
        return Optional.ofNullable(matches.get(Objects.requireNonNull(matchId, "matchId")));
    }

    @Override
    public Collection<Match> findAll() {
        return List.copyOf(matches.values());
    }

    private static Match requireCurrent(MatchId matchId, @Nullable Match current) {
        if (current == null) {
            throw new MatchNotFoundException(matchId);
        }
        return current;
    }

    private static void requireVersion(Match match, long expectedVersion) {
        if (expectedVersion < 0) {
            throw new IllegalArgumentException("Expected version cannot be negative");
        }
        if (match.version() != expectedVersion) {
            throw new OptimisticLockException(match.id(), expectedVersion, match.version());
        }
    }

    private static void requireNextVersion(Match existing, Match updated) {
        long requiredVersion = Math.incrementExact(existing.version());

        if (updated.version() != requiredVersion) {
            throw new IllegalArgumentException(
                    "Updated match version must be %d but was %d"
                            .formatted(requiredVersion, updated.version())
            );
        }
    }
}
