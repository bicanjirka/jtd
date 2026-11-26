# jTD — Tower Defense

Swing tower-defense game. This codebase is yours: what it's for and how it's built are your call,
and you'll keep building on it.
Before a structural choice (a new type, package, interface, listener, switch, parameter, state
field or text format), read `docs/DESIGN.md`. Before reversing a structure, check `docs/adr/` for
the decision behind it. `docs/ARCHITECTURE.md` is an overview of the system as it is now.

```bash
mvn verify                # Spotless + Checkstyle + tests - run before every commit
mvn -q compile exec:java -Dexec.mainClass=td.PerformanceHarness   # performance budget; add -Dexec.args=zones for the ground-zone loadout, --jfr to profile it (target/perf.jfr; top CPU and allocation per phase)
mvn test-compile pitest:mutationCoverage -Dpit.targetClasses='td.tower.*' -Dpit.targetTests='td.tower.*'   # mutation testing on demand, never in verify: narrow both to the package you changed; report in target/pit-reports/index.html
```

Mechanical rules live in `checkstyle.xml` / `checkstyle-imports.xml`; `mvn verify` names any
violation. They are not restated here.

## Working here

- Multi-phase plan: commit after each phase. Its verification section lists live checks only for
  the end of the feature, each naming what no test can assert.
- Verify with the cheapest thing that can prove it: a test (`GameEngineTest` for rules) →
  `PreviewBoard`/`PreviewEnemy` render for how the board looks → `td.balance.BalanceReport` for
  numbers → the live game (`run-jtd` Driver) only for Swing wiring: input routing, panel text
  and layout. Never re-check live what a test already asserts. A check that proves a change is
  not a test to keep: keep it only if "Tests" below asks for it.
- One live `run-jtd` pass per feature, after its last phase, in the same session.
- To find out why something happens at runtime, when the IDE is open: the debugger (`idea` MCP server; the
  `ij-debugger` skill when the session lists it), not debug code added and reverted. In a live game, a logpoint that suspends nothing;
  `run-jtd` has the attach recipe. Enemies have Observe traces first.
- A test of a figure a panel shows builds its input the way the panel does, not through a
  convenient factory.
- Performance budget (p99, the built-in level with the most buildable cells, every one a tower): tick ≤ 1 ms,
  frame build ≤ 1 ms, ≤ 512 KB allocated per frame build, paint ≤ 10 ms (headless at 1x). `td.PerformanceHarness`
  exits non-zero when over; run it after changing per-tick, per-frame or painting code. Tests: 5 s each, 60 s per fork.
- Feature requests live in `docs/features/`, one doc per feature. Content tables (towers,
  enemies, levels) live in `docs/CONTENT.md`, the rules and controls in `docs/GAMEPLAY.md`, the
  wave-script tables in `docs/WAVE-SCRIPTS.md`; `README.md` is the landing page.
- Known gaps go in `TODO.md` (with **Where** and **Approach**), never an inline TODO. Closing a
  gap deletes its entry in the same commit. Feature ideas not yet shipped are one line each under
  its "Feature ideas".
- Comments only for a non-obvious *why*. Don't name levels, content or other classes in them,
  and don't restate the code: no number, count or threshold the code beside it already holds
  ("once badly hurt", not "at 65% health"), so a tuning edit can't make the comment false.
- The design rules are heuristics: a deviation states its reason in the commit body.
- `CLAUDE.md` files hold constraints only - no history, no feature narrative. A change that
  makes a line in one false fixes it in the same commit.
- Commit subject lines are imperative mood, capitalized, no trailing period (`Add X`, not
  `added X` or `X added`).
- Rename types, methods and fields with the IDE's `rename_refactoring` (`idea` MCP server) when the
  IDE is open, not by find-and-replace.

## Packages (`td.*`)

`balance` (headless players, the tower lab, the balance report) · `board` (scale, cell↔pixel
math, the ground a board is painted on) · `cell` (squares, buildability) · `damage` · `economy` ·
`effect` (timed status effects) · `enemy` · `level` · `projectile` · `save` (a game between rounds, as
text) · `stat` (enemy stat sheet) · `term` (the game's vocabulary: terms and the text that mentions them) ·
`tower` (+ `targeting`, `upgrade`, `proc`, `ability`, `aura`, `cinder`, `mortar`, `pulse`, `seeker`, `sniper`,
`splash`) · `ui` (+ `render`) ·
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
  one using most of the world takes `GameWorld`. `projectile` uses only `tower.targeting` from
  `tower`; `effect` belongs to neither `tower` nor `enemy`; `stat` depends only on `damage` and `term`.

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
- An EDT command that changes game state is a request the next tick consumes
  (`requestNextWave`), never a write into engine state. A rebuild of the level (load, restart) is the
  exception: it stops the loop through `TowerDefense.rebuildThen`, since it writes the cells (ADR 0022).
- `Threads.assertEventDispatchThread()` / `assertNotEventDispatchThread()` at thread-sensitive
  entry points.
- `GameLoop.stop()` joins without bound: never call it on the EDT. Level teardown goes through
  `TowerDefense.stopLoopThen`.
- `EconomyLedger` computes inside `synchronized (this)` and fires listeners outside it. `doPay`
  is the atomic check-and-charge; never `if (canPay(n)) doPay(n)`.
- Listener and live-entity lists are `CopyOnWriteArrayList`.
- `GameEngine.doTick` order is fixed: enemies, projectiles, zones, playtest hooks, towers. Falling behind runs one
  tick and resyncs, never a burst. The tick rate exists only in `TickRate`.

## Code style

- **Value types** are immutable records with named factories (`Damage.physical(400)`,
  `EconomyDelta.kill(bounty)`). Absence is a value (`none()`, `empty()`) or `Optional`, never
  `null` or a sentinel number. The one exception: `td.ui` frame builders return `null` for "no draw command".
  An `Optional` is read through `map`, `or`, `filter` and `ifPresent`, never `isPresent()` then `get()`.
- A lambda parameter the body doesn't read is `_`.
- A record with 5+ components has a narrow factory and grows by fluent `withX` copies, never a
  wider constructor (`PathDefinition`, `EnemyDefinition`). `td.ui.render` draw records are
  exempt.
- Randomness comes from an injected `RandomSource` (`GameWorld.random()`), never
  `Math.random()`, so headless balance runs reproduce.
- What the simulation iterates keeps a fixed order (`List`, `LinkedHashMap`, `EnumMap`). A `HashSet`, `HashMap` or
  `IdentityHashMap` of objects or enums only answers membership: its order moves with changes that play no part in
  the game, and order reaches who is hit and so the shared random.
- A table or catalogue built once (a tower's mechanics per node, a list of entries) is one factory call that
  lists its entries (`of(entry(...), entry(...))`), never `empty()` followed by a chain of copying `with`
  calls. `withX` adjusts a value that exists; it doesn't assemble one. A call site that needs a type witness
  (`Foo.<T>empty()`) to compile has the wrong shape.
- Services: constructor injection into `final` fields; one constructor, no init step.
  Interfaces of 1-5 methods; the wide domain interfaces in `TODO.md` are the known exceptions.
- Streams to transform, `reduce` to combine.
- Names put the role noun last and say what a thing does: `SniperTower`,
  `FurthestAlongPathSelector`.
- New accessors take the record style (`x()`, `radius()`); never rename an existing `getX()` in
  passing.
- `this.` on every field access. Field order: constants, collaborators, mutable state.
  `@Serial` on `serialVersionUID`.
- Never expose an internal array or live collection; offer the queries callers need, or return a
  fresh copy.
- Catch `Exception`, never `Throwable`/`Error`, and never an unchecked exception as control
  flow (a `NumberFormatException` around a parse is fine). Log or rethrow; never swallow.
  Unloadable content throws `GameStartupException`, which only `Main` catches, except the dev
  panel's wave-script check, whose input is typed.

## Tests

What gets a test (ADR 0025):

- **Test what does harm:** a defect that changes what the game decides (rules, money, lives,
  saves, balance numbers, thread hand-offs, a tick that throws) or what the player may know
  (hidden information, a figure the player decides on). Test it however visible it is.
- **Don't test what decides nothing:** looks, wording, draw order, a paint or frame-build crash.
  Check it once (`PreviewBoard`, a `run-jtd` shot) and keep no test.
- UI behaviour that decides nothing (hover, tooltips, keys) gets a test only when it is rare or
  fiddly enough that normal play would miss it. Layout, hit-test and wrapping math is tested on
  plain values.
- A bug fix gets a test only when the defect is harm; the test states the broken invariant for
  every case, not the one seen.
- A test written to find or prove a change is scratch: delete it before commit unless it meets
  the above. No console output, no casts into private structure.
- Delete a test when a legitimate change would rewrite its expected value, when a content
  rename breaks it, or when another test proves the same.

How a test is written:

- AssertJ `assertThat`. Package-private classes and methods.
- Method names are behaviour sentences (`placingOnAPathCellIsRejectedAndCostsNothing`): no
  `test`/`should` prefix, no underscores, no `@DisplayName`, no `@Nested`.
- Hand-written fakes, no Mockito. Name them by role (`FakeGameHost`, `FakeEnemyMob`), one per
  role: beside its tests, or in `td.fixtures` once a second package needs it. The same for
  helpers. Reuse, never copy.
- Headless and clock-free: drive the public API and call `doTick(t)` with explicit ticks.
- A test that builds a Swing component runs on the EDT (`@ExtendWith(EventDispatchThreadExtension.class)`):
  Swing's own work on the EDT races a test that builds or drives components on its thread.
- Arrange / act / assert separated by blank lines, no `// given` comments.
- Build wide records through their factory or `td.fixtures`, not a full positional literal
  (except in that record's own test).
- `GameEngineTest` is the integration surface: a gameplay rule must be reachable there, and gets
  a test there only for wiring its unit test cannot see.
- **Test mechanics, never content.** Content is data: an enemy's rank ladder, a tower's base
  stats, an upgrade node made only of `NodeEffects`, an `EffectKind`'s rule, a level's waves. A
  mechanic is code: a trigger, action or trait, a node's `Mechanic`, a `HitBonus` or `Proc`
  piece, targeting, how a tower fires. Test a mechanic on a definition built in the test
  (`EnemyFixtures.liveMob`, `FakeTower`); never assert a built-in entry's stats, abilities,
  numbers or wording. A new test never names a built-in node, enemy or level.
- Built-in content is tested only by rules every entry keeps (`BuiltInEnemiesTest`,
  `TowerStatCatalogueTest`, `TermWordingTest`); `BalanceReport` judges its numbers. A content
  change adds no test. When one breaks a test, rewrite that test on a fixture or delete it; never
  edit its expected value. Before deleting a test, make sure the harm it guarded in passing (a
  stat a tower reads, a capability, a proc payload) has a test of its own: a stat a tower reads
  is tested by changing it with `TowerFixtures.buffed`. What decides nothing needs no replacement.
- Expected numbers come from the test's own input or a game rule's formula, never from content
  or from cosmetic constants.
- A damage figure is expected in points, from a bonus or base the test set itself (`TowerFixtures.buffed`), never by
  repeating the code's sum, conversion included: a test that copies the code passes with its bug.
- Each test proves one behaviour no other test proves.
