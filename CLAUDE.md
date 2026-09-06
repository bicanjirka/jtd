# jTD — Tower Defense

A Swing tower-defense game. Java 26, Maven, no runtime dependencies (JUnit 5 + AssertJ are test-scope only).

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

Domain packages under `td.*`: `cell` (board squares, buildability), `enemy` (mob hierarchy + `EnemyFactory`), `tower` (tower hierarchy + `TowerFactory`), `wave` (path geometry, wave composition), `util` (`Context`, `Cache`, listener interfaces).

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
- **Fields ordered** roughly: constants, injected/final collaborators, mutable state.
- Prefer `record` for value carriers (see `GameEngine.WaveDefinition`).
- `@Serial` on `serialVersionUID` in Swing classes.

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

## Gotchas

- **A green test run still prints a `NumberFormatException` stack trace.** `WaveTest` feeds an unparseable token (`"?"`) through the wave language, and `Wave.finalise()` handles it by calling `printStackTrace()` and defaulting the count to 1. Expected output on a passing run — check `Tests run: … Failures: 0`, not the presence of a trace.
- **`Cache` is an eagerly-initialized singleton that calls `System.exit(0)` if any image fails to load** (after showing a `JOptionPane`). `Context`'s constructor calls `Cache.getInstance()`, so *every* test that builds a `Context` loads the real image resources. Renaming or removing anything under `src/main/resources/td/images/` will take down the test suite, not just the game.
- **`Context.enemies` is a public mutable `EnemyMob[]`**, and every tower re-implements its own linear scan over it. This is a known wart with a documented fix in `TODO.md`; don't add a sixth hand-rolled scan without reading that entry first.
- **Version is duplicated** between `pom.xml` and `TowerDefense.VERSION` (currently `1.4` in both). There's no single source of truth — update both when cutting a release.

## Known gaps

`TODO.md` is the single source of truth for outstanding design and feature gaps. It was deliberately populated by extracting inline `TODO` comments out of the source, and each entry carries a **Where** and an **Approach**.

**Do not reintroduce inline `TODO` comments.** If you find a new gap, add an entry to `TODO.md` in the existing format instead. If you close a gap, delete its entry in the same commit.
