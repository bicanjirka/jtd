# 0010. Balance is measured by headless players on the real level

- Status: Accepted
- Date: 2026-09-17 (recorded; the players came later)

## Context
A fixed loadout with unlimited credits measured nothing a player meets. Lives lost didn't move when
a burn doubled, and purchase rankings flipped with the budget. The full design is in
`docs/features/FEATURE-playtesting-and-balance-tooling.md`.

## Decision
`td.balance` plays the real game through `GameEngine` with `GameHost.noOp()`:
- **One play gives two answers.** A play (`LevelPlay`) runs with unlimited lives and judges the
  level by the leak that would have lost it. Up to that leak the play is identical, so one play
  gives both the verdict and how badly it was missed.
- **Observing costs the engine nothing.** The recorder reads only the engine's public state after
  every tick.
- **Logs survive game changes.** Actions are logged by round and offset into the round, so a log
  replays on a changed game. The engine and the player have separate seeded sources, so a decision
  never shifts what the game rolls.
- **The lab measures instead of computing.** Behaviour lives in mechanics no formula reproduces, so
  the lab runs probes that never die (to rank cells) and duels against real enemies (to rank kinds
  and nodes).
- **The competent player values a purchase** by the enemy health it turns from likely leaking to
  likely dying.
- **The margin, not the win rate, is the headline.** With few lives and a deterministic player, a
  win rate is 0% or 100%.
- **Harnesses place towers through the real input path** (`startPlacing`, `mouseClicked`), then
  check the grid afterwards rather than trusting a return value: `TowerPlacement.mouseClicked`
  leaves placement mode whether or not it built anything. `td.PerformanceHarness` is the timing
  sibling.
- **`td.PurchaseHarness` prices purchases by their result on the level**, so support and effects
  are measured like damage. It builds an army of one tower of each type, then plays each candidate
  (another copy, or a node with everything leading to it) to the end on many seeds with unlimited
  lives. Its resolution is limited: at its default seeds it takes about fifteen seconds per tower
  type, and an effect worth less than about a life is lost in its noise.

The dev panel is the in-game counterpart of the `run-jtd` Driver: one control, with one shortcut,
per thing a playtester would otherwise script.
- Its cheats live in the headless `td.DevControls`, which queues every change to enemies and towers
  for the game-loop thread. So they work while paused, and there is no second writer.
- Its selection row reads a `DevSelection` snapshot that `DevControls` publishes from the game loop,
  since only that thread may read the enemy selection. The panel polls it on the render pulse.
- Its shortcuts go through a `KeyEventDispatcher`, because the frame itself usually holds the focus
  and a `Window` runs no key bindings.
- The debug keys behind it live on `GameEngine`, not `TowerDefense`, because they are engine rules,
  tested like any other in `GameEngineTest`.

## Consequences
- Balance claims come with numbers from `BalanceReport` (seeds, margins, intervals). Content
  changes add no tests.
- Engine changes for the tools are playtest rules and hooks, never tool-specific engine paths.

## Rejected
- *A fixed loadout.* It measures the loadout, not the level.
- *Summed damage as a purchase's value.* The player spent where enemies already died twice over.
