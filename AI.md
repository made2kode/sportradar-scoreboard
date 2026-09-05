# AI-assisted development record

## How AI was used

AI was used as a design and implementation partner: to inspect the supplied PDF, challenge open
requirements, shape the API and domain boundaries, generate an initial implementation and JUnit
tests, and run Maven verification. Every proposed behavior was reduced to an explicit assumption
in `README.md` or an executable test; no generated result was accepted solely because it came from
AI.

## Prompt history and context

The full earlier discussion was supplied to the implementation session as `historia.txt`. Its
relevant user prompts and decisions, in chronological order, were:

1. Analyze the coding task, its real scope, technology choices, ambiguities, edge cases, and likely
   evaluation criteria.
2. Explore a restrained DDD design with `Match` as the aggregate root, an in-memory repository,
   value objects, an application facade, and no framework infrastructure.
3. Prefer Kotlin-data-class-like immutability in Java: an update should create a new state rather
   than mutate the existing aggregate.
4. Use a private aggregate constructor and named factory/domain methods instead of `Cloneable` or
   a public, general-purpose `copy` method.
5. Start the project with JUnit tests, `ConcurrentHashMap`, a `version` field on the aggregate, a
   custom optimistic-lock exception, test reports, and a library/plugin for bumping the Maven
   project version.

The assistant proposed and the user retained these important choices:

- Java records for value objects and public snapshots, but a final class with a private constructor
  for the aggregate root;
- score corrections are allowed, while negative values are not;
- finished matches are removed rather than retained as history;
- ordering uses a monotonic sequence rather than wall-clock time;
- public optimistic locking makes concurrent-write conflicts visible instead of silently retrying
  and hiding lost updates.

## Artifacts and references

- `ODDS and Data - JAVA Coding Task.pdf`: authoritative product requirements and acceptance-order
  example. Text extraction was checked against rendered pages.
- `historia.txt`: prior reasoning and prompt context supplied by the user.
- Official Maven Surefire Report, JaCoCo, and MojoHaus Versions Maven Plugin documentation guided
  the reporting and version-management configuration.

## Verification performed

The generated code is compiled with Java 21 and `-Xlint:all`. `mvn verify` runs domain, facade,
acceptance-order, validation, immutability, lifecycle, and deterministic concurrency tests. The
same lifecycle produces human-readable test and code-coverage reports for review.
