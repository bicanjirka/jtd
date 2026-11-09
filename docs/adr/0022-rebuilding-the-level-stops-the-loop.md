# 0022. Rebuilding the level stops the loop, as an exception to 0019

- Status: Accepted
- Date: 2026-11-09

## Context
ADR 0019 makes every EDT command to the engine a request the next tick applies, and rejects stopping
the loop (`stopLoopThen`) for debug commands. Restarting a wave (0021) and loading a saved game don't
change engine state alone: they tear the level down and rebuild it. Rebuilding places towers on the
cells, and the EDT owns the cells. A tick applying the rebuild would write them from the game loop.

## Decision
- A command that rebuilds the level (restart a wave, retry or replay one, load a saved game) stops the
  loop, has the engine rebuild on the EDT, and starts the loop again. `TowerDefense.rebuildThen` is
  the one path.
- Every other EDT command stays a request (0019).

## Consequences
- The cells keep one owner.
- A rebuild stalls the game for as long as it takes. It is rare, and a player asked for it, so the
  stall is part of the action rather than a glitch in play.

## Rejected
- *A request the next tick applies*: the tick would write the EDT's cells.
- *Handing the cells to the game loop for a rebuild*: a second owner for one rare action.
