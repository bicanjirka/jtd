# 0021. A wave starts from its save

- Status: Accepted, not yet implemented
- Date: 2026-11-09

## Context
ADR 0011 kept saves to what exists between rounds and dropped the fight state, so a loaded game played
"as the saved one could have". That has two costs:

- **Rehearsals mispredict.** Rehearsing from such a save mispredicted rounds: on eight Curly Path seeds,
  the margin fell from 99% to 90%.
- **Idle time changes the state.** Ticks run between waves, and the fight state keeps changing while the
  player builds.

Players want to restart a wave and retry a lost one, and the balance tools want to start a test at any
wave. Both need the state at a wave boundary to be whole. A full snapshot of mid-round state is still
not worth its cost (0011).

## Decision
- **A wave ends clean.** When its last enemy dies, its zones, projectiles, the towers' fight state and
  the statuses enemies put on towers all end.
- **Every wave starts with every tower ready**, in the state waiting with nothing in reach would reach,
  whatever time passed.
- **The game between two waves is entirely its save.** The save holds:
  - what the player chose (towers, upgrades in purchase order, settings set on a tower);
  - what the player sees (economy, tallies, round history);
  - the randomness the next wave will draw from.
- **A save replays tick for tick.** Loading it and playing on with the same inputs reproduces the play
  that continued from it.
- **There is still no save during a round.**

## Consequences
- The balance tools and headless players can start from any wave and trust the result. The strong
  player's rehearsals can start from saves.
- A tower mechanic that builds up during a wave must end at the wave's end, or be a player choice the
  save keeps. A new kind of fight state never needs a save field.
- Value that used to carry from one wave into the next is gone, so balance moves.

## Rejected
- *Keep 0011's lossy save.* It mispredicts, and the board a wave starts from would depend on how long
  the player waited.
- *Save the fight state.* That is a full-state snapshot by another name, growing with every tower.
