# Feature Request: Playtesting and Balance Tooling

**Priority: not sequenced against the other three feature requests.** This is enabling
infrastructure, not player-facing content, and has no hard dependency forcing it before or
after any of them. It is best read alongside `FEATURE-tower-upgrades.md`,
`FEATURE-damage-types-and-projectiles.md` and `FEATURE-enemy-traits-and-effects.md`, all
three of which ship (or plan to ship) numeric content whose tuning currently has no faster
path than manual play.

## Summary

Two related capabilities, proposed together because they attack the same problem from two
ends. First, a **debug/cheat capability** reachable from the running game — jump straight to
a specific wave, spawn any `EnemyCatalog`-registered enemy on demand, toggle infinite
credits — so testing a change doesn't require replaying everything that precedes it. Second,
a **headless batch balance-simulation harness** that drives `GameEngine` the same way a test
does — loads a level, applies a tower loadout, runs many ticks at full speed with no window —
and reports outcome numbers (lives lost, time-to-clear, damage dealt per tower) instead of
requiring a human to watch and judge. Together they turn "does this number feel right" from a
multi-minute manual playthrough into a few-second interactive check or an automated script.

## Current state (what exists today)

- `TickSpeed` already supports arbitrary fast-forward multipliers (`PanelGameConsole`'s four
  speed buttons; `GameLoop` accepts any non-negative double), so simulation *speed* is not
  the bottleneck — *reaching* a specific moment in a level is.
- There is no way to start a level anywhere other than its first wave, and no way to spawn an
  enemy except through a wave's own script. Testing anything late-game (a boss's late-wave
  ability kit, a level's final wave) means replaying everything before it, every iteration.
- `GameEngine` is already fully headless and test-driven — `GameEngineTest` exercises it via
  `doTick(t)` with no window, no real clock, and no Swing dependency at all (root
  `CLAUDE.md`'s "headless/Swing boundary"). This is precisely the property a batch
  simulation tool needs, and it already has it for free.
- Two `TODO.md` entries ("Upgrade-path numbers are unbalanced placeholders", "New tower
  numbers are unbalanced placeholders") are the direct, current cost of not having either
  capability: both features' own phased implementation plans ended with a "balance pass"
  step that never actually happened — it became a `TODO.md` entry instead.
- `FEATURE-enemy-traits-and-effects.md`'s V1 Scope carries the same "balance numbers are
  placeholders, tune later" caveat for its boss encounter and its migrated traits — a third
  round of the same pattern, unless something changes.

## What this feature adds

- **A debug/cheat capability**, most plausibly reachable via a keybinding or a hidden dev
  panel (see Open questions): jump to a specific wave of the current level; spawn a named
  `EnemyCatalog` id at the path's start (or at the cursor); toggle infinite credits. Scoped
  narrowly — three small, independent actions, not a general console or scripting layer.
- **A headless batch balance-simulation harness**: given a `LevelDefinition`, a tower loadout (type, placement, and
  chosen upgrade path per tower), and a tick budget, run the simulation
  to completion (or timeout) and report lives lost, ticks-to-clear per wave, and damage dealt
  per tower. Runs as a small standalone tool or a test-scoped utility, not shipped in the
  player-facing jar.

## Interconnections with the other three feature requests

- **Directly serves all three**: it's what actually makes the "balance pass" phase in each
  of their phased implementation plans achievable in practice, rather than an aspiration
  that quietly becomes a `TODO.md` entry.
- **The debug spawn capability depends on `EnemyCatalog` becoming an open, string-keyed
  registry** — which `FEATURE-enemy-traits-and-effects.md` already builds for its own
  reasons. This feature is best sequenced after (or alongside the tail of) that one, not
  before, so it can ride that infrastructure rather than duplicating it.
- **The batch harness has no hard dependency on any of the other three** — it can be built
  against the game as it exists today (5 enemies, 4 towers, no upgrades) and only gets more
  valuable as each of the other three features lands more numeric content for it to help
  tune.

## Architectural implications

- **The debug capability is the first thing in this codebase that needs to mutate engine
  state outside the normal input surface** (mouse clicks, `doTick`). Jumping to a wave means
  either fast-forwarding the simulation for real (slower, but reuses all existing logic
  untouched) or a new `GameEngine` entry point that skips state forward directly (fast, but
  needs to reproduce whatever a real fast-forward would have produced — economy accrual, any
  per-wave state — correctly). This is a real design decision, not a formality — see Open
  questions.
- **A debug-spawned enemy needs a defined interaction with wave-completion tracking.** Given
  `FEATURE-enemy-traits-and-effects.md` already needs spawned-enemy roster bookkeeping to
  work correctly for its own ability-driven spawns, a debug spawn can likely reuse that same
  path — but it probably should *not* block wave completion the way an ability-spawned enemy
  correctly does, since a developer spawning something to look at it shouldn't also have to
  kill it to proceed.
- **The batch harness needs a tower-loadout description as data** (type, cell position,
  chosen upgrade path), distinct from anything that exists today — `GameEngineTest` currently
  builds towers ad hoc, one test at a time, in Java. A small, reusable loadout value type is
  new, modest scope.
- **Both capabilities should stay entirely out of the shipped player-facing jar's normal
  path.** A debug capability reachable in a released build is a design decision on its own —
  some TD games ship a sandbox/debug mode deliberately, most don't — see Open questions.

## Risks and costs

- A debug capability that fast-forwards by "really" ticking through skipped waves at extreme
  speed could still be slow for a late-level test if the multiplier is capped anywhere in the
  existing `TickSpeed` machinery — worth checking before assuming this is free.
- A debug capability that instead jumps state directly needs to be kept honestly in sync with
  whatever `loadLevel`/wave-start logic real play goes through, or it becomes its own second,
  subtly different code path to maintain — a classic source of "works in the debug tool,
  breaks in real play" bugs.
- The batch harness's output (lives lost, time-to-clear, damage-per-tower) is a *proxy* for
  "is this balanced," not a definition of it — a human playtest pass is still the real judge
  of whether a mechanic feels good. This tool makes iterating on numbers cheap; it doesn't
  replace the judgment call.
- **Scope creep risk**: "debug tooling" invites growth well beyond what's proposed here (a
  full console, a level editor, a live number-tweaking panel). This document deliberately
  keeps to three narrow debug actions and one narrow batch tool — not a general dev-console
  platform.

## Product review notes

A senior-product-owner pass, reviewing all three other feature requests together against the
stated goal of keeping enemies and levels cheap to playtest and tune, is what surfaced this
gap: `FEATURE-tower-upgrades.md` and `FEATURE-damage-types-and-projectiles.md` both ended
their own phased plans with an unretired "balance pass" step, and both left a `TODO.md` entry
behind instead of actually tuning their numbers. Without something like this feature,
`FEATURE-enemy-traits-and-effects.md`'s own balance pass (its boss encounter's stage count,
hatch timer, and stat curve; the migrated traits' magnitudes) is likely to follow the same
pattern a third time. This document is written up as a future feature request, not scoped or
committed to a timeline, per that review's outcome — see Decisions made.

## Decisions made

- **The debug capability is an always-available keybinding, not gated behind a build flag or
  a hidden mode.** This project has no feature-flag or debug/release-build machinery today (root `CLAUDE.md`: "don't use
  feature flags or backwards-compatibility shims"), and jTD has
  no distribution boundary a hidden cheat would be protecting against — it's a hobby project,
  not something shipped to end users who shouldn't see developer tools. Adding build-variant
  infrastructure to hide three keybindings would be new complexity in service of a concern
  that doesn't exist here.
- **"Jump to wave" skips state directly; it does not fast-forward the simulation for real.**
  Real fast-forwarding still requires the developer to build/manage towers to actually clear
  every skipped wave — exactly the replay cost this feature exists to remove. Skipping needs
  its own correctness case (see Shape of the solution), but that's the entire point: a second,
  deliberately simple path, not a variant of real play.
- **The batch harness is a standalone class with its own `main()`**, in `src/main/java`
  alongside `Main`/`GameEngine` rather than under a test source root — it's a tool a developer
  runs on demand and reads output from, not an assertion `mvn test` should gate on. This
  mirrors the `run-jtd` skill's own `Driver.java`, which already established the same shape
  for driving `TowerDefense` headlessly from outside the normal window.
- **The debug spawn action requires a level to already be loaded.** Spawning needs a path and
  a board to place the enemy on; neither exists at the menu. No separate "spawn in isolation"
  mode is in scope.
- **"Toggle infinite credits" becomes "grant a large lump sum of credits," not a persistent
  toggle.** A true toggle needs a hook into every future spend to keep re-topping-up credits —
  real complexity for a purely developer-facing convenience. A repeatable one-shot grant (the
  same `EconomyDelta`-based mechanism `run-jtd`'s own `setcredits` cheat already uses) gives
  the same practical outcome: press the key again whenever more money is wanted.
- **A debug-spawned enemy is allowed to count toward the current wave's alive total**, rather
  than needing a second, non-counting roster-add path. The doc's original architectural-
  implications note worried this would force a developer to kill whatever they spawned just
  to keep playing — but "skip to next wave" (this same feature) already unconditionally
  clears the roster with no penalty, so it's already the escape hatch this would have needed
  to build separately. One fewer new code path for the same outcome.

## V1 Scope

With the decisions above settled, this section pins down what a first version actually
contains: concrete content, the shape of the solution, and a phased implementation order.

### Boundary

- **Three debug keybindings**, reachable the moment a level is loaded (mid-level, paused or
  not): skip the current wave and immediately start the next one; spawn one instance of an
  enemy id, cycling through every id the level's current `EnemyCatalog` has registered, one
  per press; grant a large lump sum of credits. No numeric/text entry UI — cycling and
  repeatable presses cover the need without building input widgets a dev tool doesn't
  otherwise need.
- **The batch harness takes a `LevelDefinition` and a list of tower placements** (type + cell
  only — no upgrade-path selection in v1, deferred as a real but separable follow-up once
  placement-driven simulation is proven) **and a tick budget**, runs to completion or the
  budget, and prints lives lost, ticks-to-clear per wave, and each tower's damage dealt/kill
  count to the console.
- **Not in scope**: any UI for the harness (console output only); upgrade-path selection in a
  loadout; a "spawn in isolation, no level loaded" mode; any change to `EnemyRoster`'s
  counting semantics; a persistent infinite-credits toggle.

### Shape of the solution

- **The debug capability drives `GameEngine` through new, narrow entry points**, the same
  spirit as its existing `mouseClicked`/`startPlacing`/`nextWave` surface — not a side channel
  that reaches into `GameWorld` from outside the engine's own API.
    - `debugSkipToCurrentWaveEnd()`: clears the live roster the same no-penalty way level
      teardown already does (`GameWorld.clearEnemies()` — no `GameHost.enemyDied` call, no
      economy effect), then forces `waveReady` true and calls the existing `nextWave()`. Correct
      by construction from primitives the engine already has, rather than a second interpretation
      of "what does clearing a wave mean" — directly answers the risk this doc's own Risks and
      costs section raised about a skip path silently drifting from real play.
    - `debugSpawnNextCatalogEnemy()`: reads the level's `EnemyCatalog`, advances a stored
      cursor through its registered ids in a stable order, and spawns that id via the same
      `EnemyCatalog.spawn`/`GameWorld.addEnemy` path an ability-driven spawn already uses (see
      `FEATURE-enemy-traits-and-effects.md`) — at the path's start (`delay = 0`), using the
      definition's own `baseHealth`/`price` fields (the same ones the Warden chain already
      relies on for exactly this "spawned outside a wave" case) rather than needing a
      wave-in-progress's numbers.
    - `debugGrantCredits(int amount)`: `gameWorld.apply(EconomyDelta.credits(amount))` — the
      exact mechanism `run-jtd`'s `setcredits` cheat already proves out via reflection; this
      makes the same capability reachable from inside the real game, not just the test driver.
    - `TowerDefense`'s existing `KeyListener` gains three more cases alongside its current
      `q`/`w`/.../`s`/`m`/`p`/`f` set — exact keys chosen during implementation to avoid any
      collision with that live list.
- **The batch harness (`td.BalanceHarness`, a new class next to `Main`) drives `GameEngine`
  through its real input surface**, not by constructing `Tower`/`EnemyMob` objects by hand:
  `startPlacing(type, range)` + `mouseClicked(pixelX, pixelY)` per placement (converting a
  loadout's cell coordinates via the level's `BoardGeometry`, the same conversion
  `TowerPlacement` already does), then a plain `doTick(t)` loop up to the tick budget,
  reading final state from `GameWorld`'s existing getters (`getLives()`, `getScore()`, each
  tower's own damage/kill accounting) rather than needing new instrumentation. Uses
  `GameHost.noOp()` — the harness reads outcome from `GameWorld`'s state after the run, not
  from host callbacks during it, so the already-existing no-op host is enough.
- **Both pieces are provable headlessly**, consistent with this project's test conventions —
  the debug `GameEngine` methods get `GameEngineTest`-style coverage (skip clears with no
  penalty; spawn works before any wave has started; spawn cycles deterministically) before
  the keybindings wiring them up needs a `run-jtd` visual check at all.

### Phased implementation order

Per this project's standing "commit after each phase" convention:

1. **Debug capability**: the three `GameEngine` methods, headless-tested, then the three
   `TowerDefense` keybindings wiring them up — verified visually via `run-jtd` since keyboard
   input is UI-layer, per this project's UI requirement.
2. **Batch balance-simulation harness**: `td.BalanceHarness`, its loadout value type, and a
   first real run against Classic Loop reported in this feature's own commit message as
   proof it produces sane numbers — not a tuning pass on the other three features' content
   yet (that's its own follow-up work once the tool exists, tracked in `TODO.md` alongside
   the placeholder-number entries it exists to help close).

## Open questions

None blocking — see V1 Scope, above.
