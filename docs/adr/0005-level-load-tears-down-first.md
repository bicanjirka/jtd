# 0005. Loading a level tears the old one down first, in a fixed order

- Status: Accepted
- Date: 2026-09-17 (recorded)

## Context
The game boots into a level-select screen (a `CardLayout` with a menu card and a game card).
Players return to the menu after a win, a loss or mid-level (`m`, behind a confirmation that is
skipped once the level has ended); both routes go through `TowerDefense.requestReturnToMenu()`.
They can then pick any level, so nothing may survive from the previous run.

## Decision
- `GameEngine.loadLevel()` unloads the outgoing level as its first step, before installing the new
  board geometry and grid, which makes it idempotent. `TowerDefense.returnToMenu()` clears no
  engine state of its own.
- The teardown order is fixed:
  1. Projectiles first: they hold no geometry, but must not reach the next level's ticks.
  2. Towers next, while the outgoing geometry is still installed, since clearing a tower maps its
     position back to a cell through the current geometry and grid.
  3. Enemies last. The level outcome is reset too, so an empty roster after teardown never reads
     as a cleared final wave.
- Idempotence is not thread-safety. Loading while a tick is in flight is prevented separately:
  `GameLoop.stop()` joins its thread first (0007), and level teardown goes through
  `TowerDefense.stopLoopThen`.
- Levels come from a `LevelCatalog`. The built-in one holds code-defined `LevelDefinition`s; a
  file-based catalog would implement the same interface.

## Consequences
- Any entry point can call `loadLevel` without preparing state first.
- A new roster gets a place in the teardown order, chosen by what it holds.

## Rejected
- *Clearing state in the menu transition.* Every entry point would have to remember it.
