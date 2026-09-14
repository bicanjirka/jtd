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
- **A headless batch balance-simulation harness**: given a `LevelDefinition`, a tower loadout
  (type, placement, and chosen upgrade path per tower), and a tick budget, run the simulation
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

None yet — this is an unscoped request, written up for future prioritization, at the same
stage the other three documents started at before their own scoping passes.

## Open questions

1. Reachable how — a keybinding, a hidden dev-mode toggle, or a build-time-only capability
   that doesn't exist in the shipped jar at all? This affects whether the result is "debug
   tooling for the developer" or "a sandbox mode for players," which are different products.
2. Does "jump to wave" fast-forward the simulation for real (slower, but guaranteed correct
   by construction) or skip state directly (fast, but needs its own correctness case built
   and tested)?
3. Should the batch harness live as a standalone `main`-having tool, a Maven exec-plugin
   target, or a set of JUnit tests that print a report — given `mvn test` is already this
   project's one build gate, folding it in there may be the path of least new tooling?
4. Is the debug spawn action expected to work mid-level only, or also from a stopped/menu
   state (spawn-and-immediately-tick to look at one enemy in isolation, without a full level
   loaded at all)?
