package io.github.made2kode.sportradar.scoreboard;

import org.junit.jupiter.api.Test;

import static io.github.made2kode.sportradar.scoreboard.TestFixtures.aMatch;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryMatchRepositoryTest {

    @Test
    void rejects_duplicate_match_id_without_replacing_existing_match() {
        InMemoryMatchRepository repository = new InMemoryMatchRepository();
        MatchId matchId = new MatchId(1);
        Match existing = aMatch();
        Match duplicate = aMatch(matchId, "Spain", "Brazil", 2);
        repository.add(existing);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> repository.add(duplicate));

        assertAll(
                () -> assertEquals("Duplicate match id: 1", exception.getMessage()),
                () -> assertSame(existing, repository.findById(matchId).orElseThrow()));
    }

    @Test
    void rejects_update_that_is_not_the_next_aggregate_version() {
        InMemoryMatchRepository repository = new InMemoryMatchRepository();
        Match original = aMatch();
        Match firstUpdate = original.updateScore(new Score(1, 0));
        Match staleSibling = original.updateScore(new Score(0, 1));

        repository.add(original);
        repository.save(firstUpdate, 0);

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.save(staleSibling, 1)
        );
    }
}
