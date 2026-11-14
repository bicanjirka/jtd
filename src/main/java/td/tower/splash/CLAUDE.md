# `td.tower.splash`

Constraints for `SplashTower` (in `td.tower`) (and its Stormcaller and Hexer forks) and its mechanics in this package. The
rules shared by every tower are in `td/tower/CLAUDE.md`.

- A stone is unguided: it flies to where its target stood when thrown, with the stats of its `beginShot`, and lands
  on everyone within `BLAST_EDGE`, hidden enemies included: the core (`AREA_RADIUS`) at full damage, the rest at the
  edge share. Every landing, jump, curse, spark and soul waits out its own flight and lands on the tick it arrives,
  never earlier.
- The fork's passive is the `Passive` the spec carries: Static (Stormcaller) or Soulbrand (Hexer). A hit that reached
  health (landed above zero) tags its enemy in the tower's own `TagLedger`, one stack per tower; the stack is settled
  at the start of the tower's tick and pays by the enemy's max health (`PassivePayout`), when the spark or soul arrives.
  What a cast deals tags nothing.
- The currency is earned only through `earnAbilities`: the stones' credited damage by `SplashSpec.damagePerUnit`, and
  the passive's payouts. Abilities are node mechanics with an `AbilityCharge`; their casts go through `CastActions`,
  never a tower method a mechanic calls itself.
- An Arc starts from where a charged stone landed, carries the stats that stone was thrown with, and jumps hop by hop
  with a pause between.
- A Hexer's hex is an enemy effect in `EffectCategory.HEX`, applied through `applyEffect` once its curse arrives, and
  that effect is the hex's only timer. The Hexer's `HexKeeper` only watches it: what a hex pays out when it ends, and
  where it jumps when its carrier dies, is settled at the start of the Hexer's tick, never by the enemy. The keeper
  reaches its caster only through `HexCaster`, so the dev panel's caster pays hexes out by the same rules. Each hex
  picks its own targets in one pass per cast, never per tick.
- A jumping hex carries itself as it was: its time left in authored ticks (what tenacity did on the dead enemy taken
  out) and the slow it had grown to as its start.
