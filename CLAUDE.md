# jTD — Tower Defense

A Swing tower-defense game. Java 26, Maven. Runtime dependencies: SLF4J + Logback
for logging (see Logging below). JUnit 5 + AssertJ are test-scope only.

## Commands

```bash
mvn test                  # run the test suite
mvn package               # build target/jTD.jar (main class: td.Main)
java -jar target/jTD.jar  # run the game
mvn -q compile            # fast syntax/type check
```

Run a single test: `mvn test -Dtest=GameEngineTest#placingATowerOnABuildableCellChargesCreditsAndOccupiesTheCell`

## Architecture: the headless/Swing boundary

This is the one structural rule that matters, and it is the result of a deliberate refactor still in progress. Respect it.

- **`GameEngine` owns game state and input semantics.** It never constructs a window, never touches `Graphics2D`, and never requires a display. It can be built, driven, and asserted on entirely from a test.
- **`TowerDefense` (a `JFrame`) and `td.ui` own presentation.** Layout, painting, `MouseEvent`/`KeyEvent` handling, and translating screen coordinates into the board-relative pixel coordinates `GameEngine.mouseClicked`/`highlightCell` expect.
- **`GameHost` is the engine's only channel back to the UI** (`enemyDied`, `setInfoText`, `clearCell`). `Context` calls through it; it does not know about Swing.

**When adding gameplay logic, put it in `GameEngine`/`Context`/the domain packages, not in `TowerDefense`.** `TowerDefense` is a shrinking legacy shell — every new rule placed there is a rule that cannot be tested. If a change needs something from the UI, add a method to `GameHost` rather than reaching for a Swing type from engine code.

Domain packages under `td.*`: `cell` (board squares, buildability), `enemy` (mob hierarchy + `EnemyFactory`), `tower` (tower hierarchy + `TowerFactory`; `tower.targeting` holds the shared target-scanning abstractions every tower composes instead of hand-rolling), `wave` (path geometry, wave composition), `util` (`Context`, `Cache`, listener interfaces).

`Context` is the shared mutable world — credits, score, lives, the tower list, the enemy array — and the listener hub (`ContextListener`, `TowerListener`, `WaveStartListener`).

## Threading model

`GameLoop` runs on a dedicated daemon thread named `game-loop`, with two independent fixed-timestep accumulators:

- `onTick` runs a variable number of times per interval — zero while `TickSpeed.PAUSED`, several in a row when fast-forwarding.
- `onRender` fires on a flat ~60fps real-time cadence regardless of tick speed, so the board keeps redrawing while paused.

**Tick code does not run on the Event Dispatch Thread.** Rendering is handed to the EDT via `SwingUtilities.invokeLater` — that is the deliberate safe-publication idiom here, not `repaint()`'s internal synchronization. Consequences:

- `Context`'s listener lists and `towers` are `CopyOnWriteArrayList` on purpose. Keep them that way; don't "optimize" to `ArrayList`.
- Never touch Swing components from tick code. Route through a listener that the UI observes.
- When the loop falls behind (debugger pause, long GC) it runs **one** tick and resyncs rather than bursting the backlog. Preserve that.

Tick speed is a plain multiplier — `TickSpeed` presets are a convenience, and any non-negative double is valid, so an arbitrary-speed control needs no engine change.

## Conventions

- **No wildcard imports.** Enforced via `.idea/codeStyles/Project.xml`. This is not stylistic: the project already hit a real `java.util.List` / `java.awt.List` collision that only compiled because an explicit import shadowed a wildcard.
- **`this.` prefix on instance field access.** Used consistently across the codebase; match it.
- **Fields ordered** roughly: constants, injected/final collaborators, mutable state — for
  stateful engine/service classes. A new value type (see Code style below) has no third
  bucket: every field is `private final`.
- Prefer `record` for value carriers (see `GameEngine.WaveDefinition`).
- `@Serial` on `serialVersionUID` in Swing classes.

## Code style: staff-level Java, per policy-management

New and modified gameplay code follows the style codified in the sibling
`../policy-management` repo — `README.md` (ten rules + five composition patterns + SOLID
map) and `TESTING.md` (test-writing rules). Read those before writing non-trivial code;
this section states only how the rules land on jTD's existing shape, not what they say.

**The rules split by what kind of class you're writing — this is not optional, it's how
the two documents avoid contradicting each other:**

- **Value types** (data passed around and compared: `WaveDefinition`, `Point`, any new
  DTO-shaped class) get the full treatment — rule 5 (immutable, `private final`), rule 7
  (named static factory over a public constructor), rule 8 (no `null`, model absence
  explicitly). No exceptions here; a new mutable value class is a regression.
- **Stateful engine/service classes** (`Context`, `GameEngine`, `GameLoop`, `Cache`) are
  exempt from rule 5 by nature — they exist to hold and mutate live simulation state, and
  the Threading model above is a hard requirement that overrides the style guide where the
  two would otherwise conflict (e.g. `Context.towers` stays a mutable
  `CopyOnWriteArrayList` — that is a concurrency requirement, not legacy debt to "fix"
  toward immutability). What the style guide *does* still apply to these classes:
  constructor injection (already the norm — `Context(GameHost)`,
  `AbstractTower(type, price, damage, range)`), narrow interfaces, and — see below —
  keeping duplicated branching logic out of them.

**Already aligned — keep doing this:**

- **No `instanceof` type-switching anywhere in `src/main/java`** (verified by grep) —
  matches rule 9 already. If a future feature needs to branch on concrete enemy/tower
  type, reach for a visitor over `EnemyMob`/`Tower` rather than writing the first
  `instanceof` chain in this codebase.
- `Point` and `WaveDefinition` are already `record`s — rule 5/7 with zero gap.
- `GameHost` and the listener interfaces (`ContextListener`, `TowerListener`,
  `WaveStartListener`) are already narrow, role-named interfaces. Widen `GameHost` only
  when the UI genuinely needs a new callback (see Architecture above) — don't add
  speculative methods.
- This project's test conventions (see Tests below) already are the style's test rules:
  full-sentence method names, hand-built fakes over mocks, AssertJ throughout. Nothing to
  change; don't introduce Mockito or `@DisplayName` to "improve" this.
- All five leaf `Tower*` classes and all five leaf `EnemyMob*` classes are `final`. This
  still isn't the style's literal rule-4 exception (nothing dispatches over them with a
  visitor — they're plain virtual `paint()`/`doTick()` calls, a reasonable choice for a
  game loop), but the variant set is at least genuinely closed now, which is the part that
  was previously just asserted without being true.
- Tower targeting is centralized in `td.tower.targeting` (`TargetQuery`/
  `InRangeTargetQuery`, `TargetSelector`/`FurthestAlongPathSelector`/`RandomSelector`,
  `NextTargetQuery`/`InRangeAfterIndexQuery` for `TowerThree`'s index-stable round robin).
  This is pattern D done for real: filtering and selection are separate, swappable, unit-
  tested pieces instead of four hand-rolled scans with an `if` buried in each one. Add a
  new tower by composing these, not by writing a fifth scan.

**Partially aligned:**

- `Context` is still the shared mutable god object described above — `enemies` is now
  encapsulated behind `getEnemies()`/`setEnemies()` (never `null`), but `credits`/`score`/
  `lives` remain plain mutable fields with ad hoc setters, no algebra. That's a much larger
  change than this file asks anyone to take on speculatively; leave it as known shape,
  not a gap to opportunistically "fix" mid-way through an unrelated change.

## Tests

JUnit 5 + AssertJ. `assertThat(...)`, never JUnit's bare assertions.

- **Test classes and methods are package-private**, not `public`.
- **Method names are full sentences**: `placingOnAPathCellIsRejectedAndCostsNothing`, `enemyReachingTheEndOfThePathCostsALife`. Describe the behavior and its consequence, not the method under test.
- **Tests are headless and clock-free.** Drive the engine through its public API and call `doTick(t)` with explicit tick numbers; never rely on the real game loop's timing or open a window.
- Test doubles live beside the tests they serve and are named for their role: `FakeGameHost`, `RecordingGameHost`, `RecordingCell`.
- `GameEngineTest` is the integration surface — it exercises the same entry points `TowerDefense`'s listeners call. New gameplay rules should be provable there.

## Wave mini-language

Wave contents are a space-separated token string parsed in `Wave.finalise()`. Tokens are enemy letters, each optionally preceded by a repeat count:

| Token | Enemy |
|-------|-------|
| `c` | Circle |
| `s` | Square |
| `t` | Triangle |
| `g` | Ghost |
| `e` | Empty (spacer — counts toward spawn timing, not toward the enemy count) |

`"3 s e 4 c"` = three Squares, one spacer, four Circles. A count applies only to the token immediately following it and resets to 1 afterward. Levels are defined as `WaveDefinition(enemies, hp, price, level)` — see `TowerDefense.DEFAULT_WAVES`.

## Logging

SLF4J + Logback. Every logging class gets `private static final Logger LOG =
LoggerFactory.getLogger(ClassName.class);` as its first field, and logs with
SLF4J's parameterized style (`LOG.info("...{}...", value)`), never string
concatenation.

- **Runtime config**: `src/main/resources/logback.xml`. `Main.main` sets the
  `jtd.logTimestamp` system property (as its very first statement, before any
  other class touches SLF4J) so each run writes its own file:
  `logs/jTD-<yyyyMMdd_HHmmss>.log`, alongside a console appender. Default
  level is `INFO`; tick-by-tick/render/fire-by-fire detail is logged at
  `DEBUG` and is off by default — raise `logback.xml`'s root level (or point
  `-Dlogback.configurationFile` at an alternate config) for a deep-dive
  session. `Main` also prunes old run-logs beyond a retention cap on startup.
- **Test config**: `src/test/resources/logback-test.xml` — Logback prefers
  this file over `logback.xml` when both are on the classpath, so `mvn test`
  stays quiet (root level `WARN`) and never touches `logs/`.
- **`GameLoop` has a safety net**: `onTick`/`onRender` exceptions are caught,
  logged at `ERROR`, and skipped rather than killing the dedicated
  `game-loop` thread outright; a circuit breaker stops the loop after 10
  consecutive tick failures. The current tick number is tagged into every log
  line via MDC (`%X{tick}` in the pattern).
- **Fatal startup failures** (e.g. `Cache` failing to load an image) throw
  `td.util.GameStartupException`, caught exactly once in `Main`, which logs
  at `ERROR` and exits non-zero — the one fatal boundary, rather than a
  singleton constructor showing a dialog and exiting itself.

## Gotchas

- **A green test run still prints a `WARN` line and stack trace.** `WaveTest` feeds an unparseable token (`"?"`) through the wave language, and `Wave.finalise()` handles it with `LOG.warn(...)` (via `logback-test.xml`, `WARN` is the one level still visible during tests, and Logback prints the passed exception's trace below the message) and defaults the count to 1. Expected output on a passing run — check `Tests run: … Failures: 0`, not the presence of that output.
- **`Cache` is an eagerly-initialized singleton that throws `GameStartupException` if any image fails to load** — caught in `Main`, which logs it and exits non-zero. `Context`'s constructor calls `Cache.getInstance()`, so *every* test that builds a `Context` loads the real image resources. Renaming or removing anything under `src/main/resources/td/images/` will take down the test suite, not just the game.
- **Version is duplicated** between `pom.xml` and `TowerDefense.VERSION` (currently `1.4` in both). There's no single source of truth — update both when cutting a release.

## Known gaps

`TODO.md` is the single source of truth for outstanding design and feature gaps. It was deliberately populated by extracting inline `TODO` comments out of the source, and each entry carries a **Where** and an **Approach**.

**Do not reintroduce inline `TODO` comments.** If you find a new gap, add an entry to `TODO.md` in the existing format instead. If you close a gap, delete its entry in the same commit.
