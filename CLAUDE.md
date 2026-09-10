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

## Working in phases

When a task is planned as multiple phases, commit after each phase completes rather than
waiting until the whole task is done. This applies to every multi-phase plan, not just the
one it was first requested for.

## Documentation map, and keeping it true

- **This file** is the always-loaded architecture guide. It stays the place for anything
  cross-cutting: the headless/Swing boundary, the threading model, code style, test
  conventions, the wave mini-language.
- **`src/main/java/td/<package>/CLAUDE.md`** exists for the packages whose internals carry
  invariants worth stating up front — `enemy`, `tower`, `wave`, `ui` and `economy`. These
  load automatically when working inside that directory, so they are trusted; each one
  covers construction/lifecycle ordering, what must not be "simplified", and the checklist
  for adding a new type to that package. Don't create one for a package that has nothing to
  say beyond this file.
- **`README.md`** is the human-facing entry point: what the game is, how to build and run
  it, controls, and the tower/enemy/level tables.
- **`TODO.md`** is the single source of truth for known gaps.

**Keep these current in the same commit as the code change, not afterwards.** If a refactor
moves a class, changes a construction order, renames a collaborator, or invalidates a
"never do X" note, update the affected `CLAUDE.md` — root or per-package — as part of that
change. This is the same discipline `TODO.md` already gets ("close a gap, delete its entry
in the same commit"), and it applies without being asked: a stale package doc is worse than
no package doc, precisely because it is loaded and believed.

## Architecture: the headless/Swing boundary

This is the one structural rule that matters, and it is the result of a deliberate refactor still in progress. Respect it.

- **`GameEngine` owns game state and input semantics.** It never constructs a window, never touches `Graphics2D`, and never requires a display. It can be built, driven, and asserted on entirely from a test.
- **`TowerDefense` (a `JFrame`) and `td.ui` own presentation.** Layout, painting, `MouseEvent`/`KeyEvent` handling, and translating screen coordinates into the board-relative pixel coordinates `GameEngine.mouseClicked`/`highlightCell` expect.
- **`GameHost` is the engine's only channel back to the UI** (`enemyDied`, `setInfoText`, `clearCell`). `GameWorld` calls through it; it does not know about Swing.
- **`td.ui` itself splits describing a frame from drawing one.** `BoardRenderer.buildFrame(gameTime, interpolationAlpha, animationSeconds)` walks the engine/context and returns an immutable `td.ui.render.RenderFrame` — cell/enemy/tower draw-command records with zero `java.awt` import anywhere in that package. A backend turns that into output: `Java2DFrameRenderer` is the real one (the only class in `td.ui` that imports `java.awt`); `AsciiBoardRenderer` is a second, deliberately minimal one used for headless `DEBUG` logging (see Logging), which exists specifically to prove the split is a real seam rather than an aspirational one. `Tower`/`EnemyMob` still dispatch into the frame builders via `TowerVisitor`/`EnemyMobVisitor` (see the no-`instanceof` rule below) — only the last step, turning a `RenderFrame` into pixels, changed shape.
- **All game art is vector, drawn by code — there are no image assets.** Enemies, path markers, tower effects (beams/splash/pulse/aura) and tower sprites are all `java.awt.Shape`s built and painted in `Java2DFrameRenderer`, keyed off `td.ui.render.Palette`. A tower's sprite is itself two layered pieces: a static base (`TowerSpriteDraw`, `paintTowerBody`/`towerBodyShape`) and an animated turret head on top (`TurretHeadDraw`, `paintTurretHead`/`turretHeadShape`) — an aiming tower's head reads its own `td.tower.TurretAim` (advanced in `doTick`, interpolated at render time the same way `EnemyFrameBuilder` interpolates enemy position); a spinning or pulsing tower's head is a pure function of `GameLoop.animationSeconds`, needing no domain state at all (see `TowerSpriteFrameBuilder`'s per-visit-method constants). Adding a new tower or enemy's art is: (1) add a `Palette` constant naming its colour role; (2) add its `Shape`/colour case to `Java2DFrameRenderer` — `enemyShape`/`colorFor` for an enemy; `towerBodyShape`/`turretHeadShape`/`colorFor` (and `TowerSpriteFrameBuilder.bodyPaletteFor`, an exhaustive switch with no `default`) for a tower's base/head; a new `TowerEffectDraw` record (see `AuraDraw`) for a tower's transient effect; (3) wire the new domain class into the existing `TowerVisitor`/`EnemyMobVisitor` dispatch the same way every other type already is. The compiler catches a missing step at every one of those switches. Tower toolbar icons (`PanelTowerSelector`) reuse the exact same base+head paint code via `Java2DFrameRenderer.renderTowerIcon` (at a fixed representative heading/scale, since a static icon has no target or animation clock), so a tower's board look and its icon can never drift apart.

**When adding gameplay logic, put it in `GameEngine`/`GameWorld`/the domain packages, not in `TowerDefense`.** `TowerDefense` is a shrinking legacy shell — every new rule placed there is a rule that cannot be tested. If a change needs something from the UI, add a method to `GameHost` rather than reaching for a Swing type from engine code.

Domain packages under `td.*`: `board` (`BoardGeometry` — a level's pixel scale and cell
dimensions as one immutable value, with the cell↔pixel math every consumer used to hand-roll),
`cell` (board squares, buildability), `damage` (the `Damage`
value type towers deal to enemies), `economy` (`EconomyDelta`/`EconomyState`, the
credits/score/lives algebra, and `EconomyLedger` — see below — that's built on it),
`enemy` (mob hierarchy + `EnemyFactory` + `EnemyRegistry`/`EnemyRoster` — see below),
`level` (`LevelDefinition` — a level's board size, path, waves, starting economy and
`PathSmoothing` strategy as one immutable value; `path` is just the level's corners, in
authored order, at any angle — `PathBuilder` turns them into pixel-space directly, no
per-cell expansion step; `LevelCatalog`/`BuiltInLevelCatalog` is where levels are sourced
from — see Levels below), `tower` (tower
hierarchy + `TowerFactory` + `TowerRoster`/`TowerListener` — see below; `tower.targeting` holds
the shared target-scanning abstractions every tower composes instead of hand-rolling; `tower.buff`
holds `TowerBuff`, the
upgrade-stacking algebra), `wave` (path geometry and wave composition, plus `WaveAnnouncer`/
`WaveStartListener` — see Path geometry
below), `util` (`GameWorld`, `GameHost`).

**`GameWorld` (in `util`) is a composition root, not a state holder.** It used to be a single
god object (`Context`) owning five unrelated jobs directly; each is now its own independently
testable class, and every one of `GameWorld`'s methods just delegates to one of them:

| Collaborator | Package | Owns |
|---|---|---|
| `BoardGeometry` | `td.board` | scale, board size, cell↔pixel conversion (an immutable value, replaced wholesale on `setBoard`) |
| `EconomyLedger` | `td.economy` | the `EconomyState` (credits/score/lives) and `EconomyListener` notification — see Threading model |
| `EnemyRoster` (implements `EnemyRegistry`) | `td.enemy` | the live per-wave `EnemyMob[]` and death reporting to `GameHost` |
| `TowerRoster` | `td.tower` | the tower list, buy/sell/clear, and `TowerListener` notification |
| `WaveAnnouncer` | `td.wave` | the `WaveStartListener` hub (`TowerThree` is the only subscriber, resetting its round-robin index) |

A consumer that only needs one of these should depend on it directly rather than on the whole
`GameWorld` — e.g. `td.tower.targeting`'s query classes and `BoardRenderer` take an
`EnemyRegistry`, not a `GameWorld`, since `getEnemies()` is all they ever used. `Tower`/
`EnemyMob`/`Wave` still take the full `GameWorld` in their constructors since they genuinely
need several of these together (board geometry, economy, path, enemy registry, death
reporting) — that's a legitimate use of the composition root, not a shortcut around ISP.

## Levels

`TowerDefense` boots into a level-select landing screen, not straight into gameplay: the
content pane is a `CardLayout` with a "menu" card (`td.ui.PanelLevelSelect`, one clickable
stacked card per level showing its name and description) and a "game" card holding the board
and side panels. Levels come from a `LevelCatalog` — `BuiltInLevelCatalog` is the only
implementation today, sourcing levels as Java-code `LevelDefinition` constants; a future
file-based catalog implements the same interface (see `TODO.md`). Selecting a card calls
`TowerDefense.startSelectedLevel(LevelDefinition)`, which is what now does the level load, board
sizing and `GameLoop.start()` that used to happen unconditionally in the constructor — nothing
ticks and no board is shown until a level is actually chosen. `GameEngine.loadLevel(LevelDefinition)`
is the one entry point that turns a level into live engine state (grid, path, waves,
`GameWorld.startEconomy(credits, lives)`); each level owns its board size, path and waves and now
its own starting credits *and* lives, so levels don't inherit each other's economy.

`startSelectedLevel` is re-enterable, not one-shot: each game-over/game-won overlay has a "Back
to menu" button, and `m` returns to the menu mid-level (behind a confirm dialog, skipped once the
level has already ended), both routed through `TowerDefense.requestReturnToMenu()`. The player
can then pick any level, including a different one, with no state left over from the previous
run. This works because `GameEngine.loadLevel()` is itself idempotent — it is always safe to
call, from any prior state, not just once per process. It unloads the outgoing level as its
first step, *before* installing the new board geometry and grid: `TowerRoster.clear()` maps
each tower's pixel position back to a cell through the *current* `BoardGeometry` and calls back
into the *current* `cellGrid`, so it must run while those are still the outgoing level's, not
the new one's. `EnemyRoster.clear()` (renamed from `removeAll()`) deliberately does not notify
`GameHost.enemyDied` — tearing a level down is not a death, and the old name's notification
could spuriously trigger the "won" overlay via `TowerDefense.enemyDied`. `TowerDefense.returnToMenu()`
itself clears no engine state at all; that is the payoff of `loadLevel()`'s contract. See
`GameLoop`'s own restart contract in Threading model below, which the same round trip relies on.

## Path geometry and smoothing

`td.wave.Path`/`PathNormal` store a path as continuous pixel-space points (`Vec2`, not the
integer, cell-coordinate `Point` — that split is deliberate: `Point` is only ever used for
level authoring, `Vec2` only for the pixel geometry a `Path` actually walks). `PathBuilder`
is the one place a level's raw cell-coordinate `List<Point>` becomes a `Path`: convert each
cell to its pixel center, run the result through the level's `PathSmoothing` strategy, then
populate a `PathNormal`.

`PathSmoothing` (in `td.wave.smoothing`) is a pluggable per-level strategy —
`PathSmoothing.none()` is the identity, matching this codebase's `none()`-as-identity-element
idiom elsewhere. `AbstractCornerSmoothing` is the shared template both real strategies
(`ArcCornerSmoothing`, a circular-arc fillet; `QuadraticBezierSmoothing`, a Bezier curve using
the corner as its control point) build on: it walks each interior corner, pulls back a
fraction (`cornerPull`, capped at 0.5 so two nearby corners' pullbacks can never cross) of the
shorter adjacent leg, and delegates only the curve itself to the subclass.

Cell buildability is computed from this same final (possibly curved) geometry, not from a
fixed list of authored cells: `PathCoverage.unbuildableCells` supersamples each grid cell in
the path's bounding box and marks it unbuildable if enough of its area falls within the
path's corridor width. `GameEngine.markUnbuildableCells` is the single caller. For an unsmoothed,
axis-aligned grid path this reproduces exactly the same cells a naive "mark the listed cells"
approach would — `PathCoverageTest` proves this for both built-in levels — but a smoothed
path's buildable set correctly reflects its actual curved shape instead.

`ArcLengthPath` (also `td.wave`) wraps a `Path`'s points with cumulative distance, resolving
any distance travelled to an exact position and facing by interpolation within the segment it
falls in. It is the single shared implementation behind both enemy movement
(`AbstractEnemyMob`, which advances a `speed`-in-pixels-per-tick `distanceIntoLap` accumulator
each tick — real arc-length distance, not a fixed tick-count per segment regardless of its
length) and the animated path-marker overlay (`PathMarkerFrameBuilder`) — both move at a
consistent real-world pace along whatever geometry the path actually has, curved or not.

## Threading model

`GameLoop` runs on a dedicated daemon thread named `game-loop`, with two independent fixed-timestep accumulators:

- `onTick` runs a variable number of times per interval — zero while `TickSpeed.PAUSED`, several in a row when fast-forwarding.
- `onRender` fires on a flat ~60fps real-time cadence regardless of tick speed, so the board keeps redrawing while paused.

**Tick code does not run on the Event Dispatch Thread.** Rendering is handed to the EDT via `SwingUtilities.invokeLater` — that is the deliberate safe-publication idiom here, not `repaint()`'s internal synchronization. Consequences:

- `EconomyLedger`'s, `TowerRoster`'s and `WaveAnnouncer`'s listener lists, and `TowerRoster`'s tower list, are `CopyOnWriteArrayList` on purpose. Keep them that way; don't "optimize" to `ArrayList`.
- `EconomyLedger`'s `EconomyState` is written from both the EDT (buying/selling a tower) and the `game-loop` thread (a kill or a leak), so `EconomyLedger.apply`/`doPay` compute the new state inside a `synchronized (this)` block and fire the resulting `EconomyListener.economyChanged` *outside* it — never hold the lock while calling out into listeners, which re-enter `EconomyLedger` and touch Swing.
- Never touch Swing components from tick code. Route through a listener that the UI observes.
- When the loop falls behind (debugger pause, long GC) it runs **one** tick and resyncs rather than bursting the backlog. Preserve that.

`GameLoop.start()` is idempotent and restartable, not one-shot: a second call while already running is a no-op (logged, not a second thread), and a call after `stop()` resets the tick/render accumulators, interpolation alpha, tick counter and consecutive-failure circuit breaker before starting a fresh thread — needed because returning to the level-select menu (see Levels above) stops the loop and the next level's `startSelectedLevel()` starts the same instance again. `animationSeconds` is the one exception, left monotonic on purpose since it is wall-clock cosmetic animation, not simulation state tied to any one level.

Tick speed is a plain multiplier — `TickSpeed` presets are a convenience, and any non-negative double is valid, so an arbitrary-speed control needs no engine change.

## Conventions

- **No wildcard imports.** Enforced via `.idea/codeStyles/Project.xml`. This is not stylistic: the project already hit a real `java.util.List` / `java.awt.List` collision that only compiled because an explicit import shadowed a wildcard.
- **`this.` prefix on instance field access.** Used consistently across the codebase; match it.
- **Fields ordered** roughly: constants, injected/final collaborators, mutable state — for
  stateful engine/service classes. A new value type (see Code style below) has no third
  bucket: every field is `private final`.
- Prefer `record` for value carriers (see `td.level.LevelDefinition`, `td.wave.WaveDefinition`).
- `@Serial` on `serialVersionUID` in Swing classes.

## Code style: staff-level Java, per policy-management

New and modified gameplay code follows the style codified in the sibling
`../policy-management` repo — `README.md` (ten rules + five composition patterns + SOLID
map) and `TESTING.md` (test-writing rules). Read those before writing non-trivial code;
this section states only how the rules land on jTD's existing shape, not what they say.

**The rules split by what kind of class you're writing — this is not optional, it's how
the two documents avoid contradicting each other:**

- **Value types** (data passed around and compared: `WaveDefinition`, `Point`,
  `EconomyDelta`/`EconomyState`, `TowerBuff`, `Damage`, any new DTO-shaped class) get the
  full treatment — rule 5 (immutable, `private final`), rule 7
  (named static factory over a public constructor), rule 8 (no `null`, model absence
  explicitly). No exceptions here; a new mutable value class is a regression.
- **Stateful engine/service classes** (`GameWorld`, `EconomyLedger`, `EnemyRoster`,
  `TowerRoster`, `WaveAnnouncer`, `GameEngine`, `GameLoop`) are
  exempt from rule 5 by nature — they exist to hold and mutate live simulation state, and
  the Threading model above is a hard requirement that overrides the style guide where the
  two would otherwise conflict (e.g. `TowerRoster`'s internal tower list stays a mutable
  `CopyOnWriteArrayList` — that is a concurrency requirement, not legacy debt to "fix"
  toward immutability). What the style guide *does* still apply to these classes:
  constructor injection (already the norm — `GameWorld(GameHost)`,
  `AbstractTower(type, price, damage, range)`), narrow interfaces, and — see below —
  keeping duplicated branching logic out of them.

**Already aligned — keep doing this:**

- **No `instanceof` type-switching anywhere in `src/main/java`** (verified by grep) —
  matches rule 9 already. If a future feature needs to branch on concrete enemy/tower
  type, reach for a visitor over `EnemyMob`/`Tower` rather than writing the first
  `instanceof` chain in this codebase. The one deliberate, narrow exception: `Java2DFrameRenderer`
  pattern-matches over the *sealed* `EnemyDraw`/`TowerEffectDraw` render-command hierarchies
  in `td.ui.render`. That's a compiler-checked switch over a closed set of DTOs the renderer
  itself defines (a new command type is a compile error, not a silently-skipped `default`),
  not a branch on `EnemyMob`/`Tower`'s concrete type — the domain dispatch still goes through
  `EnemyMobVisitor`/`TowerVisitor` untouched. Don't read this as license to `instanceof`/switch
  on `EnemyMob`, `Tower`, or `Cell` themselves; it applies only to sealed types under `td.ui.render`.
- `Point` and `WaveDefinition` are already `record`s — rule 5/7 with zero gap.
- `GameHost` and the listener interfaces (`td.economy.EconomyListener`, `td.tower.TowerListener`,
  `td.wave.WaveStartListener`) are already narrow, role-named interfaces, each living next to
  the class that fires it. Widen `GameHost` only
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
- **Pattern A (algebra: operation + combinator + identity + absorber) is applied at every
  place two values of the same kind get combined**, replacing what used to be hand-rolled
  arithmetic spread across several mutations:
  - `td.economy.EconomyDelta`/`EconomyState` model a kill or a leak as one value with
    `plus`/`after` and an identity `none()` — `EconomyLedger.apply(EconomyDelta)` fires exactly one
    `economyChanged` notification per event, where the old `addScore`/`doReceive`/`removeLife`
    trio fired up to two and left score with no notification at all.
  - `td.tower.buff.TowerBuff` replaces `AbstractTower.calcDamageRange()`'s
    `1f + power * upgTowers.size()` with
    `upgTowers.stream().map(TowerUpgrade::buff).reduce(TowerBuff.none(), TowerBuff::combine)`,
    so upgrade towers of different strengths can finally stack — `power` used to be a single
    `static final` shared by every `TowerUpgrade`.
  - `td.tower.targeting.TargetQuery` gained a default `and` combinator plus `all()` (the
    identity — `all().and(x)` matches exactly what `x` matches) and `none()` (the absorber —
    it overrides `and` to return itself without ever evaluating the other side).
  - `td.damage.Damage` gives every hit dealt to an enemy an identity (`none()`) and a
    combinator (`plus`); its compact constructor clamps every construction path at zero, so
    a falloff or resistance calculation (see `EnemyMobSquare`'s `absorb` override) can never
    produce a negative, healing hit.
  - `EconomyLedger`'s `credits`/`score`/`lives` are one single `EconomyState` field —
    see the Threading model note above about `EconomyLedger.apply`'s synchronized block.
  - `td.wave.smoothing.PathSmoothing.none()` is the identity for path smoothing — a level with
    no smoothing configured just gets this rather than a null/special-cased strategy field.

## Tests

JUnit 5 + AssertJ. `assertThat(...)`, never JUnit's bare assertions.

- **Test classes and methods are package-private**, not `public`.
- **Method names are full sentences**: `placingOnAPathCellIsRejectedAndCostsNothing`, `enemyReachingTheEndOfThePathCostsALife`. Describe the behavior and its consequence, not the method under test.
- **Tests are headless and clock-free.** Drive the engine through its public API and call `doTick(t)` with explicit tick numbers; never rely on the real game loop's timing or open a window.
- Test doubles live beside the tests they serve and are named for their role: `FakeGameHost`, `RecordingGameHost`, `RecordingCell`.
- `GameEngineTest` is the integration surface — it exercises the same entry points `TowerDefense`'s listeners call. New gameplay rules should be provable there.

## Wave mini-language

Wave contents are a space-separated token string. `WaveScript.parse` turns it into a
`WaveContent` (an ordered, GameWorld-free list of `EnemyFactory.Enemy` spawn slots, repeat
counts already flattened out) — `Wave`'s constructor then takes that parsed content and does
only the world-bound instantiation, one `EnemyMob` per slot. Tokens are enemy letters, each
optionally preceded by a repeat count:

| Token | Enemy |
|-------|-------|
| `c` | Circle |
| `s` | Square |
| `t` | Triangle |
| `g` | Ghost |
| `e` | Empty (spacer — counts toward spawn timing, not toward the enemy count) |

`"3 s e 4 c"` = three Squares, one spacer, four Circles. A count applies only to the token immediately following it and resets to 1 afterward. Each wave is a `WaveDefinition(enemies, hp, price, level)`, and a level's full wave list is part of its `LevelDefinition` — see `BuiltInLevelCatalog`.

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
  session. At `DEBUG`, `TowerDefense.doGameTick()` also logs an
  `AsciiBoardRenderer` dump of board state after every tick — a display-free
  way to see enemy/tower positions in the log instead of the window. `Main`
  also prunes old run-logs beyond a retention cap on startup.
- **Test config**: `src/test/resources/logback-test.xml` — Logback prefers
  this file over `logback.xml` when both are on the classpath, so `mvn test`
  stays quiet (root level `WARN`) and never touches `logs/`.
- **`GameLoop` has a safety net**: `onTick`/`onRender` exceptions are caught,
  logged at `ERROR`, and skipped rather than killing the dedicated
  `game-loop` thread outright; a circuit breaker stops the loop after 10
  consecutive tick failures. There is no blanket tick number tagged into
  every log line: `GameLoop` used to do this via MDC, but MDC is thread-local
  and the loop only sets it on the dedicated `game-loop` thread, so the vast
  majority of log lines — anything from Swing event handling on the EDT, or
  from startup/shutdown on the main thread — showed a permanently empty
  `tick=` field. Call sites that actually run on the `game-loop` thread and
  care about which tick they're in (e.g. `GameLoop`'s own tick-failure logs)
  log the tick number as an explicit parameter instead.
- **Fatal startup failures** (e.g. a future file-based `LevelCatalog` failing to load a level
  file — see `LevelCatalog`'s own doc comment) throw `td.util.GameStartupException`, caught
  exactly once in `Main`, which logs at `ERROR` and exits non-zero — the one fatal boundary,
  rather than a singleton constructor showing a dialog and exiting itself.

## Gotchas

- **A green test run still prints a `WARN` line and stack trace.** `WaveScriptTest` feeds an unparseable token (`"?"`) through the wave language, and `WaveScript.parse` handles it with `LOG.warn(...)` (via `logback-test.xml`, `WARN` is the one level still visible during tests, and Logback prints the passed exception's trace below the message) and defaults the count to 1. Expected output on a passing run — check `Tests run: … Failures: 0`, not the presence of that output.
- **Version has a single source of truth**: `pom.xml`'s `<version>` is filtered into `src/main/resources/version.properties` at build time (see `pom.xml`'s `<resources>` block) and `TowerDefense.VERSION` reads it at startup via `TowerDefense.loadVersion()`. Only `pom.xml` needs updating when cutting a release.

## Known gaps

`TODO.md` is the single source of truth for outstanding design and feature gaps. It was deliberately populated by extracting inline `TODO` comments out of the source, and each entry carries a **Where** and an **Approach**.

**Do not reintroduce inline `TODO` comments.** If you find a new gap, add an entry to `TODO.md` in the existing format instead. If you close a gap, delete its entry in the same commit.
