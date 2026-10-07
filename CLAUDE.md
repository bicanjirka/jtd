# jTD — Tower Defense

Swing tower-defense game. Java 25, Maven. Runtime deps: SLF4J + Logback. Tests: JUnit 5 + AssertJ.
The *why* behind these rules is in `docs/ARCHITECTURE.md`; most tasks don't need it.

```bash
mvn verify                # Spotless + Checkstyle + tests - run before every commit
mvn -q compile            # fast type check
mvn spotless:apply        # fix formatting
mvn test -Dtest=GameEngineTest#someSentenceName
mvn package && java -jar target/jTD.jar
mvn -q compile exec:java -Dexec.mainClass=td.PerformanceHarness   # performance budget
mvn -q compile exec:java -Dexec.mainClass=td.PurchaseHarness -Dexec.args="2 12 sniper"   # purchases compared: level, seeds, tower
```

Mechanical rules live in `checkstyle.xml` / `checkstyle-imports.xml`; `mvn verify` names any
violation. They are not restated here.

## Working here

- Multi-phase plan: commit after each phase. Its verification section lists live checks only for
  the end of the feature, each naming what no test can assert.
- Verify with the cheapest thing that can prove it: a test (`GameEngineTest` for rules) →
  `PreviewBoard`/`PreviewEnemy` render for how the board looks → `td.BalanceHarness` for
  numbers → the live game (`run-jtd` Driver) only for Swing wiring: input routing, panel text
  and layout. Never re-check live what a test already asserts.
- One live `run-jtd` pass per feature, after its last phase, in the same session.
- A test for UI text builds its input the way the panel does, not through a convenient factory.
- Performance budget (p99, last built-in level, every buildable cell a tower): tick ≤ 1 ms,
  frame build ≤ 1 ms, ≤ 512 KB allocated per frame build. `td.PerformanceHarness` exits non-zero
  when over; run it after changing per-tick or per-frame code. Tests: 5 s each, 60 s per fork.
- Feature requests live in `docs/features/`, one doc per feature. Content tables (towers,
  enemies, levels, controls) live in `README.md`.
- Known gaps go in `TODO.md` (with **Where** and **Approach**), never an inline TODO. Closing a
  gap deletes its entry in the same commit.
- Comments only for a non-obvious *why*. Don't name levels, content or other classes in them,
  and don't restate the code.
- `CLAUDE.md` files hold constraints only - no history, no feature narrative. A change that
  makes a line in one false fixes it in the same commit.
- Commit subject lines are imperative mood, capitalized, no trailing period (`Add X`, not
  `added X` or `X added`).

## Packages (`td.*`)

`board` (scale, cell↔pixel math) · `cell` (squares, buildability) · `damage` · `economy` ·
`effect` (timed status effects) · `enemy` · `level` · `projectile` · `stat` (enemy stat sheet) ·
`tower` (+ `targeting`, `buff`, `upgrade`, `sniper`, `sonar`, `splash`) · `ui` (+ `render`) ·
`util` (`GameWorld`, `GameHost`, `Threads`) · `wave` (path geometry, wave scripts) · `zone` (ground
zones that keep working after the shot that made them). `GameEngine`,
`GameLoop`, `TowerDefense` and `Main` sit in `td`.

## Boundaries

- **The engine is headless.** `GameEngine`, `GameWorld` and the domain packages own all game
  state and input semantics, and never touch Swing/AWT. New gameplay logic goes there, never in
  `TowerDefense`.
- **`GameHost` is the engine's only channel to the UI.** If the engine needs something from the
  UI, add a `GameHost` method rather than reaching for a Swing type; never widen it
  speculatively. Use `GameHost.noOp()`, never `null`.
- **`td.ui.render` is AWT-free.** `BoardRenderer.buildFrame` describes a frame as immutable draw
  commands; `Java2DFrameRenderer` alone paints them. `Panel*` components may use AWT for layout
  and their own previews; board content always goes through the pipeline.
- **Depend on the narrowest thing.** `GameWorld` hands out collaborators
  (`world.economy().doPay(n)`, `world.towers().all()`); a consumer needing one takes that type,
  one using most of the world takes `GameWorld`. `projectile` never depends on `tower`; `effect`
  belongs to neither `tower` nor `enemy`; `stat` depends only on `damage`.

## Threading

The `game-loop` thread (`GameLoop`) runs the simulation *and* builds each frame; the EDT only
paints a built frame and runs Swing. Tick code never touches Swing: it goes through a listener
the UI observes, or the UI polls from the EDT render pulse. Swing is built and mutated on the
EDT (`Main` uses `invokeAndWait`). Cells go the other way: the EDT owns them and the
frame build reads them.

- The owning thread publishes; the other reads only what was published. A class with a
  non-final, non-volatile field carries `@ThreadConfined(owner)`.
- Correlated fields cross as **one immutable snapshot behind one `volatile`** (`RenderFrame`,
  `LoadedLevel`, `TowerStats`) and are read through **one accessor** (`GameWorld.level()`,
  `GameEngine.waveProgress()`, `EconomyLedger.state()`). Only an independent scalar may be a
  bare `volatile`.
- Publish finished objects: fill a local, then assign. A concurrent collection doesn't make
  `clear()` + `addAll()` atomic.
- `Threads.assertEventDispatchThread()` / `assertNotEventDispatchThread()` at thread-sensitive
  entry points.
- `GameLoop.stop()` joins without bound: never call it on the EDT. Level teardown goes through
  `TowerDefense.stopLoopThen`.
- `EconomyLedger` computes inside `synchronized (this)` and fires listeners outside it. `doPay`
  is the atomic check-and-charge; never `if (canPay(n)) doPay(n)`.
- Listener and live-entity lists are `CopyOnWriteArrayList`.
- `GameEngine.doTick` order is fixed: enemies, projectiles, zones, towers. Falling behind runs one
  tick and resyncs, never a burst. The tick rate exists only in `TickRate`.

## Code style

- **Value types** are immutable records with named factories (`Damage.physical(400)`,
  `EconomyDelta.kill(bounty)`). Absence is a value (`none()`, `empty()`) or `Optional`, never
  `null`. The one exception: `td.ui` frame builders return `null` for "no draw command".
- Values that combine get an algebra: operation, identity, absorber if any (`Damage`,
  `TowerBuff`, `TargetQuery`).
- A record with 5+ components has a narrow factory and grows by fluent `withX` copies, never a
  wider constructor (`PathDefinition`, `EnemyDefinition`). `td.ui.render` draw records are
  exempt.
- Randomness comes from an injected `RandomSource` (`GameWorld.random()`), never
  `Math.random()`, so `BalanceHarness` runs reproduce.
- Services: constructor injection into `final` fields; one constructor, no init step. Small
  classes; interfaces of 1-5 methods. Inherit only for a closed set of variants, with `final`
  leaves and `private` base-class state.
- Branch on domain type with a visitor (`TowerVisitor`, `EnemyMobVisitor`,
  `ProjectileVisitor`) or a `switch` over a sealed type, never `instanceof`.
- Compose instead of hand-rolling: a tower's targeting combines `td.tower.targeting` filter and
  selector pieces. Streams to transform, `reduce` to combine.
- Names put the role noun last and say what a thing does: `SniperTower`,
  `FurthestAlongPathSelector`.
- `this.` on every field access. Field order: constants, collaborators, mutable state.
  `@Serial` on `serialVersionUID`.
- Never expose an internal array or live collection; offer the queries callers need, or return a
  fresh copy.
- Catch `Exception`, never `Throwable`/`Error`, and never an unchecked exception as control
  flow (a `NumberFormatException` around a parse is fine). Log or rethrow; never swallow.
  Unloadable content throws `GameStartupException`, which only `Main` catches, except the dev
  panel's wave-script check, whose input is typed.

## Tests

- AssertJ `assertThat`. Package-private classes and methods.
- Method names are behaviour sentences (`placingOnAPathCellIsRejectedAndCostsNothing`): no
  `test`/`should` prefix, no underscores, no `@DisplayName`, no `@Nested`.
- Hand-written fakes, no Mockito. Name them by role (`FakeGameHost`, `FakeEnemyMob`), one per
  role: beside its tests, or in `td.fixtures` once a second package needs it. Reuse, never copy.
- Headless and clock-free: drive the public API and call `doTick(t)` with explicit ticks.
- Arrange / act / assert separated by blank lines, no `// given` comments.
- Build wide records through their factory or `td.fixtures`, not a full positional literal
  (except in that record's own test).
- `GameEngineTest` is the integration surface; new gameplay rules should be provable there.

## UI

One look on every OS. Every control is a `HudButton`/`HudToggleButton` and paints itself;
nothing inherits the platform look-and-feel. Panel borders come from `Hud`. Prefer a glyph
(`►`) over drawn artwork for a simple control.
