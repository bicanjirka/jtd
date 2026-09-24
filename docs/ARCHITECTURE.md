# jTD — architecture notes

This is the *why* document. It is **not** loaded automatically, and nothing in it is a rule.
The rules live in the root [`CLAUDE.md`](../CLAUDE.md) and in the per-package `CLAUDE.md`
files; this file exists so those can stay short. Here you will find rationale, history,
rejected alternatives and worked examples — the things that explain a constraint but must not
be mistaken for one.

If a paragraph here ever contradicts `CLAUDE.md`, `CLAUDE.md` wins and this file is stale.

---

## Contents

1. [The headless/Swing boundary](#1-the-headlessswing-boundary)
2. [The render pipeline, and why the art is vector](#2-the-render-pipeline-and-why-the-art-is-vector)
3. [`GameWorld` and the `Context` decomposition](#3-gameworld-and-the-context-decomposition)
4. [Levels, and the round trip to the menu](#4-levels-and-the-round-trip-to-the-menu)
5. [Path geometry and smoothing](#5-path-geometry-and-smoothing)
6. [Threading: what the rule is protecting against](#6-threading-what-the-rule-is-protecting-against)
7. [The algebras (operation + combinator + identity + absorber)](#7-the-algebras)
8. [Logging setup](#8-logging-setup)
9. [Playtesting and balance tooling](#9-playtesting-and-balance-tooling)
10. [Lean CLAUDE.md files, standard tooling for rules](#10-lean-claudemd-files-standard-tooling-for-rules)
11. [Style lineage](#11-style-lineage)
12. [Reducing test churn from feature work](#12-reducing-test-churn-from-feature-work)

---

## 1. The headless/Swing boundary

This is the one structural rule that matters, and it is the result of a deliberate refactor
still in progress.

`TowerDefense` began as a single `JFrame` that owned the window, the game state, the input
handling and the painting. Every gameplay rule lived in a Swing class, which meant no
gameplay rule could be tested without a display. The refactor pulled state and input
semantics out into `GameEngine`, leaving `TowerDefense` as the wiring between the two worlds.

The split as it stands:

- **`GameEngine`** owns game state and input semantics. It never constructs a window, never
  touches `Graphics2D`, and never requires a display. It can be built, driven, and asserted
  on entirely from a test.
- **`TowerDefense` (a `JFrame`) and `td.ui`** own presentation: layout, painting,
  `MouseEvent`/`KeyEvent` handling, and translating screen coordinates into the
  board-relative pixel coordinates `GameEngine.mouseClicked`/`highlightCell` expect.
- **`GameHost`** is the engine's only channel back to the UI (`setInfoText`, `clearCell`).
  `GameWorld` calls `setInfoText`; `TowerRoster` is handed the host directly and calls
  `clearCell`. None of them knows about Swing. Wave re-arming and the level's end
  (`GameEngine.outcome()`) are decided by the engine at the end of each tick and polled by the
  UI, so every host - the window, tests, the balance harness - plays by the same rules.

`TowerDefense` is the one class deliberately on both sides of the boundary. It is *not*
shrinking, and calling it that would be wishful: it is around 700 lines and grew slightly
during the threading work, since splitting frame-building from painting added code here. What
holds is the rule, not the trend — a gameplay rule placed there is a rule that cannot be
tested, which is the whole reason the boundary exists and why `GameEngineTest` is the
integration surface rather than some UI-driven harness.

`GameHost.noOp()` exists for genuinely display-less worlds — the toolbar's preview towers,
the wave-preview panel's off-board enemies — so a caller never has to pass `null` and hope
nothing calls through it.

## 2. The render pipeline, and why the art is vector

`td.ui` splits *describing* a frame from *drawing* one:

```
BoardRenderer.buildFrame(gameTime, interpolationAlpha, animationSeconds)
    -> walks the engine, dispatching per object through visitors
    -> RenderFrame  (immutable, AWT-free draw-command records)
        -> Java2DFrameRenderer.paint(g2, frame)   pixels, the real backend
        -> AsciiBoardRenderer                     text, for headless DEBUG logging
```

`AsciiBoardRenderer` is not a toy. It exists so the split is a real seam rather than an
aspirational one: if a change makes it impossible to describe a frame without AWT, the change
is in the wrong place. It is also genuinely useful — at `DEBUG`, board state is dumped to the
log after every tick, which is a display-free way to see enemy and tower positions.

Each domain type has one *frame builder* that describes it and knows nothing about pixels:

| Builder                   | Describes                                                                   | Dispatch                                    |
|---------------------------|-----------------------------------------------------------------------------|---------------------------------------------|
| `CellFrameBuilder`        | placement/selection highlights                                              | plain getters — only one `Cell` impl exists |
| `EnemyFrameBuilder`       | enemy bodies, death fades, and (via `buildMarkers()`) status-effect markers | `EnemyMobVisitor`                           |
| `TowerSpriteFrameBuilder` | tower base + animated turret head                                           | `TowerVisitor`                              |
| `TowerEffectFrameBuilder` | beams, splash, pulse, aura, cone                                            | `TowerVisitor`                              |
| `ProjectileFrameBuilder`  | in-flight shells and missiles                                               | `ProjectileVisitor`                         |
| `PathMarkerFrameBuilder`  | the path's static trail and moving chevrons                                 | none — pure geometry                        |

A status-effect marker is deliberately its own `RenderFrame` list (`statusMarkers`), not a
third permitted `EnemyDraw` subtype. `EnemyDraw`'s contract is "an enemy is either an alive
body or a fading corpse, never both, never neither," and a marker is neither of those on its
own.

### All game art is vector, drawn by code

There are no image assets. Enemies, path markers, tower effects (beams/splash/pulse/aura) and
tower sprites are all `java.awt.Shape`s built and painted in `Java2DFrameRenderer`, keyed off
`td.ui.render.Palette`.

A tower's sprite is two layered pieces: a static base (`TowerSpriteDraw`) and an animated
turret head on top (`TurretHeadDraw`). An aiming tower's head reads its own `td.tower.TurretAim`
— advanced in `doTick`, interpolated at render time the same way `EnemyFrameBuilder`
interpolates enemy position. A spinning or pulsing tower's head is a pure function of
`GameLoop.animationSeconds`, needing no domain state at all.

Each tower's base is one flat symbol naming what it does — triangle, circle, spiral, star,
pulsar, diamond, kite, flame — and each is a single closed `Shape`, so all eight go through
the same fill-then-outline paint with no per-tower special case.

### Why toolbar icons are painted differently from the board

`PanelTowerSelector` reuses `Java2DFrameRenderer.towerBodyShape` via `renderTowerIcon`, so a
tower's board look and its icon can never drift apart in *shape*. The *painting* deliberately
differs, for three reasons found the hard way:

- An icon is one flat colour at half the tile. At 32px, a translucent fill under a brighter
  outline muddies into a smudge, where a single solid glyph reads cleanly.
- It draws the body **only**. The turret head carries real information on the board (where an
  aiming tower points) but has neither target nor animation clock in an icon.
- It sits on a dark rounded tile, because these are board colours: the aura tower's white is
  invisible against the platform look-and-feel's light button face.

## 3. `GameWorld` and the `Context` decomposition

`GameWorld` (in `td.util`) used to be a single god object named `Context` owning five
unrelated jobs directly. Each is now its own independently testable class:

| Collaborator                                         | Package         | Owns                                                                                                                           |
|------------------------------------------------------|-----------------|--------------------------------------------------------------------------------------------------------------------------------|
| `BoardGeometry`                                      | `td.board`      | scale, board size, cell↔pixel conversion (an immutable value, replaced wholesale on `setBoard`)                                |
| `EconomyLedger`                                      | `td.economy`    | the `EconomyState` (credits/score/lives) and `EconomyListener` notification                                                    |
| `EnemyRoster` (implements `EnemyRegistry`)           | `td.enemy`      | the live per-wave enemy list and its alive count                                                                               |
| `TowerRoster`                                        | `td.tower`      | the tower list and buy/sell/clear                                                                                              |
| `ProjectileRoster` (implements `ProjectileRegistry`) | `td.projectile` | the live in-flight shells/missiles                                                                                             |
| `WaveAnnouncer`                                      | `td.wave`       | the `WaveStartListener` hub (`TowerThree` is the only subscriber, clearing the hit markers its scan left on the previous wave) |

`GameWorld` **hands these out rather than wrapping them**, and additionally owns four fields
of its own: `board`, `path`, `enemyCatalog` and `random`. It is therefore not a pure
composition root — it is a composition root plus a little level-scoped state.

### Why it stopped being a facade

The first decomposition split the old `Context` god object into those collaborators, which
made each piece testable, and then routed every one of them back through **forty-one
delegating pass-through methods** on `GameWorld`. That relocated the coupling rather than
removing it: `world.doPay(n)` and `world.getTowers()` read as "this class uses the world",
and there was no way to see from a call site, or to count, how much of it any class actually
touched.

Measuring settled it. `td.tower` used eleven distinct `GameWorld` methods and `td.enemy`
eight — not the "three or four" an earlier note in this file assumed. So the fix was not to
invent narrow interfaces for constructors that genuinely need several capabilities; it was to
stop hiding which ones. `GameWorld` now exposes `economy()`, `enemies()`, `towers()`,
`projectiles()` and `waves()`, and a call site says `context.economy().doPay(n)`. The surface
went from 41 methods to 14, and the dependency is legible at every use.

A consumer that needs exactly one collaborator takes it directly and never sees `GameWorld`
at all — `td.tower.targeting`'s query classes and `BoardRenderer` take an `EnemyRegistry`.

### Where narrowing deliberately stops

`UpgradeCondition.isSatisfied(Tower, GameWorld)` keeps the whole world on purpose. It is a
strategy interface whose implementations need different slices: `ClusterCondition` reads the
board geometry *and* the tower roster, while `DamageDealtCondition` and `KillCountCondition`
read neither. Widening the signature to two or three narrow parameters would make three
implementations carry arguments they never use, to help one. That is ISP applied as ritual
rather than as judgement, and the interface is better as it is.

`startWave(Wave)` is the one remaining method that coordinates two collaborators instead of
handing one out — it seeds the roster's alive count *before* announcing the start, so no
listener can observe a stale count. Ordering like that is a real responsibility, not a
pass-through.

## 4. Levels, and the round trip to the menu

`TowerDefense` boots into a level-select landing screen, not straight into gameplay. The
content pane is a `CardLayout` with a "menu" card (`td.ui.PanelLevelSelect`, one clickable
stacked card per level) and a "game" card holding the board and side panels. Nothing ticks
and no board is shown until a level is actually chosen.

Levels come from a `LevelCatalog`. `BuiltInLevelCatalog` is the only implementation today,
sourcing levels as Java-code `LevelDefinition` constants; a future file-based catalog
implements the same interface (see `TODO.md`).

### Why `loadLevel` is idempotent

Each game-over/game-won overlay has a "Back to menu" button, and `m` returns to the menu
mid-level (behind a confirm dialog, skipped once the level has already ended). Both route
through `TowerDefense.requestReturnToMenu()`. The player can then pick any level, including a
different one, with no state left over from the previous run.

This works because `GameEngine.loadLevel()` unloads the outgoing level as its first step, *before* installing the new
board geometry and grid. The ordering inside `unloadCurrentLevel`
matters:

1. `GameWorld.projectiles().clear()` first. A projectile in flight holds no reference to board
   geometry or the cell grid, so unlike towers and enemies it has no ordering constraint of
   its own — but it should still not survive into the next level's tick loop.
2. `TowerRoster.clear()` next, **while the outgoing geometry is still installed**: it maps
   each tower's pixel position back to a cell through the *current* `BoardGeometry` and calls
   back into the *current* `cellGrid`.
3. `EnemyRoster.clear()` last. `loadLevel` also resets the level outcome, so an empty roster
   from teardown never reads as a cleared final wave.

`TowerDefense.returnToMenu()` itself clears no engine state at all. That is the payoff of
`loadLevel()`'s contract.

**Idempotency is not thread-safety.** These two are different properties and were conflated
in an earlier version of this documentation. `loadLevel` being safe to call twice says
nothing about it being safe to call *while a tick is in flight*; that is handled separately,
by `GameLoop.stop()` joining its thread before returning. See §6.

## 5. Path geometry and smoothing

`td.wave.Path`/`PathNormal` store a path as continuous pixel-space points (`Vec2`, not the
integer, cell-coordinate `Point`). That split is deliberate: `Point` is only ever used for
level authoring, `Vec2` only for the pixel geometry a `Path` actually walks.

`PathBuilder` is the one place a level's raw cell-coordinate `List<Point>` becomes a `Path`:
convert each cell to its pixel center, run the result through the level's `PathSmoothing`
strategy, then populate a `PathNormal`. There is no per-cell expansion step — consecutive
corners may sit at any distance and any angle, joined by one straight leg.

`PathSmoothing` (in `td.wave.smoothing`) is a pluggable per-level strategy.
`PathSmoothing.none()` is the identity. `AbstractCornerSmoothing` is the shared template both
real strategies build on: it walks each interior corner, pulls back a fraction (`cornerPull`,
capped at 0.5 so two nearby corners' pullbacks can never cross) of the shorter adjacent leg,
and delegates only the curve itself to the subclass.

- `ArcCornerSmoothing` is an exact circular fillet, tangent to both legs.
- `QuadraticBezierSmoothing` is cheaper and not exactly tangent, but visually similar — it is
  what the two curved built-in levels use.

### Buildability follows the real geometry

Cell buildability is computed from the final (possibly curved) geometry, not from a fixed
list of authored cells. `PathCoverage.unbuildableCells` supersamples each grid cell in the
path's bounding box and marks it unbuildable if enough of its area falls within the path's
corridor width. `GameEngine.markUnbuildableCells` is the single caller.

For an unsmoothed, axis-aligned grid path this reproduces exactly the same cells a naive
"mark the listed cells" approach would — `PathCoverageTest` proves this for both built-in
levels — but a smoothed path's buildable set correctly reflects its actual curved shape.

### One answer to "where is distance *d* along this path"

`ArcLengthPath` wraps a `Path`'s points with cumulative distance, resolving any distance
travelled to an exact position and facing by interpolation within the segment it falls in. It
is the single shared implementation behind both enemy movement (`PathMotion`, which
advances a pixels-per-tick `distanceIntoLap` accumulator each tick — real arc-length distance,
not a fixed tick-count per segment regardless of its length) and the animated path-marker
overlay (`PathMarkerFrameBuilder`). Both therefore move at a consistent real-world pace along
whatever geometry the path actually has, curved or not.

## 6. Threading: what the rule is protecting against

`GameLoop` runs on a dedicated daemon thread named `game-loop`, with two independent
fixed-timestep accumulators:

- `onTick` runs a variable number of times per interval — zero while `TickSpeed.PAUSED`,
  several in a row when fast-forwarding.
- `onRender` fires on a flat ~60fps real-time cadence regardless of tick speed, so the board
  keeps redrawing while paused.

`GameEngine.doTick` ticks in a fixed order: **enemies, then in-flight projectiles, then
towers.** Projectiles sit between the other two deliberately — a homing missile aims at this
tick's enemy positions, not last tick's, and a projectile a tower spawns while its own loop
runs below is left for the *next* tick to advance rather than moving twice in the tick it was
fired.

### Why the snapshot rule exists

Until the threading pass of September 2026, the Event Dispatch Thread walked live domain
objects during painting: `BoardRenderer.buildFrame` ran on the EDT and read
`AbstractEnemyMob.x`, `.y`, `.health`, `.dead` and `.prevX/prevY` — plain, non-volatile fields
written on the `game-loop` thread. `PanelTowerInfo.refreshSelected` did the same for
`AbstractTower.damageCurrent`, `.damageDealt` and `.killCount`.

The documentation at the time claimed thread-safety on the strength of three mitigations that
were each individually correct and collectively insufficient:

- `CopyOnWriteArrayList` for the roster and listener lists. This publishes the *list
  structure* — which objects exist — and says nothing about the mutable fields inside those
  objects.
- One `synchronized` block in `EconomyLedger`. Correct, and scoped to the economy only.
- `SwingUtilities.invokeLater` for the render handoff. This publishes the *request to
  repaint*, not the state the repaint then goes and reads for itself.

Under the Java Memory Model those field reads had no happens-before edge, and non-volatile
64-bit reads (`double x, y`) are explicitly permitted to tear per JLS 17.7. On x86 this is
mostly benign; on a weakly-ordered CPU it is not — and this project's standing requirement is
to behave identically on every platform.

The fix was structural rather than a dusting of `volatile`: **whichever thread owns a piece
of mutable state publishes it, and the other thread reads only what was published.** Note the
phrasing — it is not "the simulation thread owns everything". A tower's buffed stats are
recalculated on the EDT and read by tick code, so the ownership runs the other way there; see
the tower-stats case below.

For the board, `RenderFrame` already existed as exactly the right snapshot type, so the change
was to build it on the `game-loop` thread — `GameLoop` now calls both of its callbacks there
and has no Swing dependency at all — and publish it through one `volatile` field that
`paintBoard` reads.

The tower info panel was the same bug for a different set of fields, and part of it got a *different* answer. Its kill
count and damage dealt are independent readouts in a text panel
with no invariant tying them together, so they are published `volatile`. That is not mere
tidiness — `damageDealt` is a `long`, whose non-volatile read may tear. Its damage, range and
cooldown are a different matter and are *not* volatile scalars: they have to agree with one
another, so they became a snapshot. That distinction is the next section, and it was got wrong
once already.

The general lesson, which is the part worth keeping: **a concurrent collection makes the
collection safe, not its contents.** Reach for "who owns this state, and what do the other
threads get instead of it" before reaching for a keyword — and when the answer looks like
`volatile`, check first whether the fields are correlated. If they are, the answer is a
snapshot.

### The tower-stats case, and why it is a snapshot too

`AbstractTower.recalculateStats()` runs on the EDT - an aura tower registering, an upgrade
path being bought - and produces five numbers that are meaningless apart: current damage,
current range, current cooldown, and the pixel and squared-pixel forms of that range. Tick
code reads all five.

This was originally five separate fields, and the first threading pass marked three of them
`volatile`, which made each read fresh without making the set coherent: a tick could fire with
the new damage and the previous cooldown, or scan using `rangeReal` and `rangeReal2` from
different generations. That is the "half-updated object" the snapshot rule exists to prevent,
applied wrongly in the very pass that wrote the rule — `volatile` looked sufficient because the
fields are individually simple.

They now live in one immutable `TowerStats` swapped through a single volatile reference. The
useful test, when deciding between the two mechanisms, is whether the fields have to agree
with one another: a tower's kill count and its damage dealt do not (they are two independent
readouts in a text panel), but its damage and its cooldown do.

### Other threading decisions

- `GameLoop.stop()` joins the loop thread (bounded, and skipped when called from that thread
  itself) before returning. Without it, `TowerDefense.startSelectedLevel` would clear the
  rosters and swap the cell grid, board geometry and path out from under a tick still in
  flight — and the resulting exception would land in the per-tick catch and read as a
  mysterious one-off ERROR line rather than the race it was.
- When the loop falls behind (debugger pause, long GC) it runs **one** tick and resyncs
  rather than bursting the backlog. Bursting produces a visible fast-forward after every
  hiccup.
- `ProjectileRoster.doTick` collects finished projectiles into a plain list while iterating
  and calls `removeAll` afterward rather than removing through an iterator.
  `CopyOnWriteArrayList`'s iterator doesn't support `remove()`, and that exception would
  otherwise be swallowed by `GameLoop`'s per-tick failure handling instead of failing loudly
  at the call site.
- `EconomyLedger` fires listener notifications *outside* its lock because listeners re-enter
  the ledger and touch Swing. Holding the lock across a callout is how this deadlocks.
- `GameLoop.start()` is idempotent and restartable. A second call while running is a no-op (logged, not a second
  thread); a call after `stop()` resets the accumulators, interpolation
  alpha, tick counter and circuit breaker before starting a fresh thread — needed because
  returning to the menu stops the loop and the next level starts the same instance again.
  `animationSeconds` is the one exception, left monotonic on purpose since it is wall-clock
  cosmetic animation, not simulation state tied to any one level.
- `run()` pins itself to the thread `start()` launched it on, so a thread still finishing its
  last iteration when a fast `stop()`+`start()` replaces it cannot mistake the new run's
  `running = true` for its own.
- Tick speed is a plain multiplier. `TickSpeed` presets are a convenience; any non-negative
  double is valid, so an arbitrary-speed control needs no engine change.

### Why there is no MDC tick tag

`GameLoop` used to tag every log line with the current tick number via SLF4J's MDC. MDC is
thread-local and the loop only sets it on the dedicated `game-loop` thread, so the vast
majority of log lines — anything from Swing event handling on the EDT, or from startup and
shutdown on the main thread — showed a permanently empty `tick=` field. Call sites that
actually run on the loop thread and care which tick they are in log the tick number as an
explicit parameter instead.

## 7. The algebras

Pattern: *operation + combinator + identity + absorber*. It is applied at every place two
values of the same kind get combined, replacing hand-rolled arithmetic that used to be spread
across several mutations.

- **`td.economy.EconomyDelta`/`EconomyState`** model a kill or a leak as one value with
  `plus`/`after` and an identity `none()`. `EconomyLedger.apply(EconomyDelta)` fires exactly
  one `economyChanged` notification per event, where the old `addScore`/`doReceive`/
  `removeLife` trio fired up to two and left score with no notification at all.
- **`td.tower.buff.TowerBuff`** replaces `AbstractTower`'s
  `1f + power * upgTowers.size()` with a `reduce` over each nearby aura tower's own `buff()`,
  so aura towers of different strengths can finally stack — `power` used to be a single
  `static final` shared by every `TowerAura`.
- **`td.tower.targeting.TargetQuery`** has a default `and` combinator plus `all()` (the
  identity — `all().and(x)` matches exactly what `x` matches) and `none()` (the absorber — it
  overrides `and` to return itself without ever evaluating the other side).
- **`td.damage.Damage`** gives every hit an identity (`none()`) and a combinator (`plus`).
  Its compact constructor clamps every construction path at zero, so a falloff or resistance
  calculation can never produce a negative, healing hit. It also carries a `DamageType`: a
  zero-amount `Damage` is `plus`'s identity regardless of either side's type, but combining
  two non-zero damages of different types throws, since there is no sensible way to merge
  them. `cappedAt(int)` caps the amount while preserving type, which is what
  `DefinedEnemyMob.doDamage` applies to `absorb`'s result — re-wrapping from the incoming hit
  instead would silently discard whatever type `absorb` chose.
- **`td.wave.smoothing.PathSmoothing.none()`** is the identity for path smoothing, so a level
  with no smoothing configured just gets this rather than a null or special-cased field.

## 8. Logging setup

SLF4J + Logback.

- **Runtime config**: `src/main/resources/logback.xml`. `Main.main` sets the
  `jtd.logTimestamp` system property as its very first statement, before any other class
  touches SLF4J, so each run writes its own file: `logs/jTD-<yyyyMMdd_HHmmss>.log`, alongside
  a console appender. Default level is `INFO`; tick-by-tick, render and fire-by-fire detail is
  logged at `DEBUG` and is off by default. Raise `logback.xml`'s root level (or point
  `-Dlogback.configurationFile` at an alternate config) for a deep-dive session. `Main` also
  prunes old run-logs beyond a retention cap on startup.
- **Test config**: `src/test/resources/logback-test.xml`. Logback prefers this file over
  `logback.xml` when both are on the classpath, so `mvn test` stays quiet (root level `WARN`)
  and never touches `logs/`.
- **`GameLoop` has a safety net**: `onTick`/`onRender` exceptions are caught, logged at
  `ERROR`, and skipped rather than killing the dedicated thread outright; a circuit breaker
  stops the loop after 10 consecutive tick failures.
- **Fatal startup failures** throw `td.util.GameStartupException`, caught exactly once in
  `Main`, which logs at `ERROR` and exits non-zero — one fatal boundary, rather than a
  constructor showing a dialog and exiting on its own.

## 9. Playtesting and balance tooling

See [`features/FEATURE-playtesting-and-balance-tooling.md`](features/FEATURE-playtesting-and-balance-tooling.md)
for the full design.

`td.BalanceHarness` drives `GameEngine` (with `GameHost.noOp()`) through a level with
a fixed `List<TowerPlacementSpec>` loadout, reporting lives lost, ticks-to-clear per wave, and
each tower's cumulative kills and damage.

It also places towers through the same `startPlacing`/`mouseClicked` path the real mouse
listener uses, verifying success via the cell grid afterward rather than trusting a return
value, since `TowerPlacement.mouseClicked` always exits placement mode whether or not it
actually built anything.

The three debug methods it shares with the `n`/`x`/`c` keybindings live on `GameEngine`, not
`TowerDefense`, per the headless/Swing boundary — they are ordinary engine rules and are unit
tested the same way every other `GameEngineTest` case is.

`td.PerformanceHarness` is its timing sibling: it fills every buildable cell of the last
built-in level with towers, plays the level through once to warm the JIT and five more times
measured, and reports p50/p99/max tick time, frame-build time (`BoardRenderer.buildFrame`, which
is AWT-free and so runs headless) and bytes allocated per frame build.

For visual work, the `run-jtd` Claude Code skill also offers `PreviewBoard`/`PreviewEnemy` —
headless dev tools that compose a board or a single enemy scene and render it straight to a PNG,
without opening a window or driving the real UI. They exist so an agent can check a rendering
change quickly, the same way `BalanceHarness` lets one check a numeric one. Full CLI usage and
worked examples live in `.claude/skills/run-jtd/SKILL.md`.

## 10. Lean CLAUDE.md files, standard tooling for rules

Every `CLAUDE.md` used to back its rules with `scripts/VerifyRules.java`, a hand-rolled checker
of about 800 lines. That included checks that policed the docs themselves: every backticked
type had to be a real file, and every check had to be cited in `CLAUDE.md`. The per-package docs
grew to 142 KB. A small task loaded tens of thousands of tokens of documentation, and a rename
meant editing prose. That cost was too high for a hobby project.

So the rules moved to Checkstyle (`checkstyle.xml`, `checkstyle-imports.xml`); the doc-policing
checks were dropped; and every `CLAUDE.md` was cut down to operative constraints (root 20.7 KB to
6.5 KB, packages 142 KB to 22 KB). The long-form package docs are still in git:
`git show 7a69e5c:src/main/java/td/<pkg>/CLAUDE.md`. They were never moved into per-package
architecture files, because a second doc per package is one more thing to keep in step.

## 11. Style lineage

The code style here was originally described by reference to a sibling repository,
`../policy-management`, which was treated as a normative dependency: "read those before
writing non-trivial code."

That reference has been **removed**, and the rules jTD actually uses are now stated directly
in `CLAUDE.md`. The reason is a governance one rather than a disagreement with the style: a
mandatory instruction pointing at a local filesystem path outside the repository cannot be
satisfied by a fresh clone, by a build server, or by a contributor on another machine. An
instruction that cannot be followed gets skipped silently while the work still claims to
comply — which is worse than having no instruction at all.

The substance survives. The ten rules, the composition patterns and the testing conventions
that jTD genuinely applies are inlined in `CLAUDE.md`, in their jTD-specific form, with the
exceptions this codebase actually takes stated alongside them instead of being discovered
later as contradictions.

## 12. Reducing test churn from feature work

A review of recent feature commits found that a large share of the time went into editing
tests that had nothing to do with the feature - a signature changed, and every call site had to
follow. Measured over the 40 commits leading up to this one: test-diff lines averaged 0.55 per
production line, but in 12 of the 40 the test diff was *larger* than the production diff,
peaking at 4.5x (`a675939`, adding column/drip delay spacing) and 3.3x (`61dd95e`, scrambling
`RandomSource.seeded`). Classifying the hunks of the ten highest-churn commits: roughly 500
lines were genuinely new tests, roughly 570 were assertions rewritten because behaviour really
changed, and roughly **810 - the largest bucket, about 40% - were mechanical**: a call site
rewritten only because a constructor grew an argument or something was renamed. Representative
examples: `SpawnParameters.of(2, 1.28f, 50, 3, 1f, 1f)` becoming `...1f, 1f, 0.0)` when a
component was added; `LevelDefinitionTest`'s entire body rewritten when `path`/`waves` became a
`paths` list.

Two root causes, found by comparing the records that absorbed a feature change for free against
the ones that didn't:

- **Wide positional value types.** `EnemyDefinition` had grown to 12 components with no factory
  at all, so every test built one from a 12-argument literal; `TowerBuff` was constructed raw at
  19 call sites; `LevelDefinition`'s `unsmoothed`/`singlePath` factories were 8- and 9-argument
  aliases rather than a narrower shape with defaults. Meanwhile `PathDefinition`,
  `SpawnParameters` and `LoadedLevel` - each already built from a narrow required-args factory
  plus fluent `withX` copies - absorbed the *same kind* of change (the multiple-paths feature)
  with zero call-site edits.
- **No shared test-fixture layer.** 26 test files each wrote `new GameWorld(new
  RecordingGameHost())` by hand, 12 redeclared a `SCALE = 32` constant, and helpers like
  `straightPath`, `cellCenter`, `towerAt` and `flyProjectilesToCompletion` were copy-pasted
  verbatim across four to six files each. Every copy was a separate edit whenever the thing it
  built changed shape.

The fix landed in three commits, one per phase, each independently green:

1. Gave `EnemyDefinition`, `TowerBuff` and `LevelDefinition` the same narrow-factory-plus-`withX`
   shape `PathDefinition` already had, and rewrote their production call sites
   (`BuiltInEnemies`, the tower leaves, `BuiltInLevelCatalog`) through it.
2. Added `td.fixtures` (`WorldFixtures`, `BoardFixtures`, `LevelFixtures`, `TowerFixtures`,
   `EnemyFixtures`) and migrated roughly thirty affected test files onto it, deleting each local
   duplicate.
3. Added two checks to `scripts/VerifyRules.java` - `wide-values-have-a-narrow-entry-point` and
   `no-wide-value-literals-in-tests` - so the pattern is enforced rather than left to review.
   Getting the tree clean for the second check surfaced the last holdouts (a few remaining
   12-argument `EnemyDefinition` and 5-argument `TowerBuff` literals in tests not otherwise
   touched by phases 1-2), fixed in the same commit.

**Rejected alternatives**, each considered and set aside in favour of the above:

- *Migrating fixtures opportunistically* (only when a future change already touches a given test
  file) rather than all at once. Rejected because a half-migrated suite has all the same-package
  ambiguity as before - a new test can't tell by looking whether the shared fixture or the local
  copy is the one to extend - for months, for a saving that a single, verified, three-phase pass
  captures immediately.
- *Fixing only the two or three highest-arity types* (`EnemyDefinition`, `TowerBuff`) and leaving
  `LevelDefinition`'s positional factories alone. Rejected because `LevelDefinition.unsmoothed`/
  `singlePath` were exactly the shape (a wide factory standing in for a narrower one) that
  caused the pain in the first place, just one level removed from the record itself.
- *An advisory report instead of a failing check* - printing a blast-radius inventory (types
  constructed directly in more than N test files) the way `reportRuleInventory` already prints a
  rule-coverage count, without failing the build. Rejected for the same reason every other rule
  in `scripts/VerifyRules.java` is a check and not a paragraph: a rule that only exists as prose
  can be wrong, can drift, and invites exactly the kind of explanatory paragraph this section is
  - see `CLAUDE.md` §10's own argument for the general case.

The scoping choice worth remembering if this needs revisiting: `wide-values-have-a-narrow-entry-
point` deliberately excludes `td.ui.render`. Its draw records (`RenderFrame`, `TowerSpriteDraw`,
`EnemyFadeDraw` and the rest) have 5+ components too, but each is built exactly once per frame by
its own dedicated frame-builder class - there is no scattered-call-site blast radius for a
factory to reduce, and forcing one on would-be-single-caller DTOs is an abstraction with no
payoff. The check's exemption follows the same package boundary `render-has-no-awt` already
draws, rather than inventing a new one.

Superseded in part by section 10: the wide-record rule is now a Checkstyle `MatchXpath` check,
and `no-wide-value-literals-in-tests` was dropped.
