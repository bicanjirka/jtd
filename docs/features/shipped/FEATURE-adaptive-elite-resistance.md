# Feature Request: Adaptive Elite Resistance

**Status: implemented.**

## Summary

An elite's armor resists whichever damage type the player has been dealing most, instead of
every type alike. It is a balancing pressure: a defence built on one damage type finds elites
increasingly resistant to it, so the player has to diversify damage sources, or elites become
effectively unkillable.

## Current state

- Elite and boss ranks of the Simple and Mender ladders add an untyped `"armor"`
  `PercentResistTrait` that scales every hit, physical or magic.
- The `armored` spawn shape adds a physical-only flat resist under the same `"armor"` id, so on
  an elite it *replaced* the rank's armor instead of adding to it.

## Decisions

1. **Window: the whole level.** Every hit that lands during the current level counts, whatever
   dealt it (towers, projectiles, burn). It resets when a level loads. Simple and predictable;
   the cost is that a late change of strategy takes a while to register.
2. **Strength scales with dominance.** Let `share` be the dominant type's fraction of all landed
   damage. At a 50/50 split (or before any damage has landed) the armor does nothing; at 100%
   one type it is the rank's full armor value, restricted to that type. In between it is linear:
   `factor = 1 - (2·share - 1)·(1 - full)`. No cliff, so every bit of diversification pays.
3. **Chosen at spawn.** Each elite reads the mix once, when it is built, and keeps that
   resistance for its life. What an elite resists is fixed and can be shown, and the resisted
   type never flips mid-fight.
4. **Scope: the ranks that had untyped armor.** Simple and Mender at elite and boss rank.
   Deliberately typed armor (the Armored ladder's physical plating, the Warden and its eggs)
   stays fixed.
5. **The `armored` spawn shape stacks.** Its resist is always added, never replacing an existing
   armor trait, so an `armored` elite keeps its adaptive armor too.
6. **The `armored` spawn shape adapts too, physical only.** Instead of a flat resist it blocks
   physical damage by the level's physical share: 20% at a 50% share or less (and before any
   damage), 80% at 100%, linear in between. No cap on stacking with other armor; a later
   armor-piercing mechanic is the counter.
7. **One trait model.** Adaptive armor is an ordinary trait slot, resolved per mob at spawn, not
   a separate component, so it composes, stacks and inherits like any other trait.

## Constraints

- Engine-only: the damage tally lives in the headless world, never in the UI.
- Deterministic: the choice depends only on landed damage, so `BalanceHarness` runs reproduce.
- Within the performance budget in `CLAUDE.md` (the tally is one small record per landed hit).
- Visible: an elite's marker row shows which type its armor resists.
