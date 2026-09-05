# Live Football World Cup Scoreboard

A small Java 21 library for managing football matches that are currently in progress. It has no
framework or persistence dependency and keeps the public API deliberately compact.

## Build and reports

Run the complete verification lifecycle with Maven 3.9 or newer:

```shell
mvn clean verify
```

The build runs the JUnit test suite and creates:

- machine-readable Surefire results in `target/surefire-reports/`;
- an HTML test report in `target/reports/tests.html`;
- HTML coverage in `target/reports/coverage/index.html`;
- XML coverage in `target/reports/coverage/jacoco.xml`.

## Usage

```java
Scoreboard scoreboard = new Scoreboard();

MatchSnapshot match = scoreboard.startMatch("Poland", "Germany");
match = scoreboard.updateScore(match.id(), 2, 1, match.version());

List<MatchSnapshot> summary = scoreboard.getSummary();
Optional<MatchSnapshot> latest = scoreboard.getMatch(match.id());

scoreboard.finishMatch(match.id(), match.version());
```

`getSummary()` orders active matches by total score descending. Matches tied on total score are
ordered by start order descending, so the most recently started match comes first.

## Additional operation: `getMatch`

`getMatch(MatchId)` is the single operation added beyond the four required lifecycle and summary
operations. It returns the latest immutable snapshot of a known active match, or `Optional.empty()`
after that match has finished. A live-data consumer commonly needs to refresh one match before
submitting an optimistic write; forcing it to fetch and scan the complete summary would make that
workflow less direct. The operation is introduced in a dedicated git commit.

## Concurrency and optimistic locking

`Scoreboard` is safe for concurrent method calls. Active aggregates live in a
`ConcurrentHashMap`, and writes use its atomic, per-key `compute` operation. As a result, unrelated
matches can be updated independently while competing writes to one match are serialized.

Every `Match` aggregate has a zero-based `version`. `updateScore` and `finishMatch` require the
version observed by the caller. A successful score update creates a new immutable aggregate and
increments the version. If another writer has already changed it, the operation raises
`OptimisticLockException`, which exposes the match id plus the expected and actual versions. This
prevents a delayed feed update from silently overwriting newer data.

`getSummary()` copies the weakly consistent `ConcurrentHashMap` view into immutable snapshots and
then sorts it. It is safe during concurrent writes and never exposes mutable state, but it is not a
transactional point-in-time view across all matches. A global lock could provide that property at
the cost of blocking otherwise independent match updates; the live scoreboard requirements do not
justify that trade-off.

## Domain model and reasoning

- `Match` is an immutable aggregate root with a private constructor. New matches can only be made
  through the `start` factory, and score changes are explicit state transitions.
- `MatchId`, `TeamName`, and `Score` are small value objects. They keep validation close to the data
  whose validity they define.
- `MatchSnapshot` is the immutable public projection. Clients cannot construct or mutate internal
  aggregates.
- `Scoreboard` is the application facade. `MatchRepository` separates use cases from the
  thread-safe, in-memory storage mechanism without introducing a framework.
- Summary sorting is performed on demand in `O(n log n)`. The expected number of simultaneously
  active World Cup matches is small, so a continuously maintained sorted index would add more
  consistency work than value.
- A monotonic start sequence models "most recently started" directly and makes tie-breaking
  deterministic without timestamps or a `Clock` dependency.

## Assumptions and validation

- Team names are non-null and non-blank. Surrounding whitespace is stripped; comparison remains
  case-sensitive.
- A team cannot play itself, but tournament scheduling constraints do not prevent a team from
  appearing in two active matches.
- Scores and versions cannot be negative. Scores may decrease because live sports feeds sometimes
  correct events, for example after a VAR decision.
- The score total must fit in a signed Java `int`.
- Finishing a match removes it. This library models a live scoreboard, not match history.
- Match ids and start order are monotonic within one `Scoreboard` instance but are neither globally
  unique nor promised to be gapless.
- Updating or finishing a missing match raises `MatchNotFoundException`.

## Version changes

The build pins the [Versions Maven Plugin](https://www.mojohaus.org/versions/versions-maven-plugin/)
so project versions can be changed consistently without backup POM files:

```shell
mvn versions:set -DnewVersion=0.2.0-SNAPSHOT
```

The plugin changes the Maven project version; aggregate `version` fields are runtime optimistic
locking tokens and are unrelated.

## Trade-offs

The implementation favors a small, reviewable library over Spring, a database, domain events,
CQRS, or a general-purpose repository hierarchy. Per-match linearizable writes and weakly
consistent cross-match reads are intentional. Durability, distributed id generation, retries, and
feed ordering belong at an integration boundary if the library later runs across processes.
