# 0011. A saved game holds only what exists between rounds

- Status: Accepted
- Date: 2026-10-20

## Context
Players and the balance tools want to continue from a point in a level. A save of the full
mid-round state would need a deep copy of every stateful class, growing with each feature.

## Decision
- A save (`GameSave`, written as text by `GameSaveText`) exists only between rounds. It holds:
  - the level and the next wave;
  - the economy;
  - each tower's type and cell, its upgrades in purchase order, and its tally.
- It holds no enemy, projectile or zone, no fight state (charges, missile stock, aim) and no
  randomness.
- `GameEngine.restore` rebuilds towers through placement and `buyUpgrade` in build order, gates
  waived, so each tower records the price it was bought for. Loading names what it couldn't rebuild.
- The balance tools start plays from such saves (`WaveCheckReport --save-before`, `--from`). The
  strong player's rehearsals still replay from the first tick. Rehearsing from a save mispredicted
  rounds, because the fight state a save drops matters: on eight Curly Path seeds the player sold
  more, its margin fell from 99% to 90%, and it was no faster.

## Consequences
- A save is for playing on from a point, not for reproducing a play tick for tick.
- Node ids become part of the format (0016).

## Rejected
- *Full-state snapshots.* They grow with every feature, for a use nobody has.
