# Feature Request: Second missile damage as a stat

Status: requested.

## Summary

The damage a Ruby or Shade missile adds on top of the Aerie's own becomes a tower stat, like the Mortar's Heavy
damage, so the player sees it, nodes can change it and its points can't be mixed with damage units again.

## Current state (what exists today)

- `MissileKind` holds a flat bonus in points (Ruby +40, Shade +40, Frost 0) that `damageWith` converts with
  `DamageUnits.ofPoints` and adds to the tower's damage. The hit's adjustment then applies, and a Shatter bursts with
  the same figure (`SeekerTower.onImpact`, `SeekerTower.shatter`).
- The bonus first shipped unconverted (`1151400f`), so it added 0.4 points instead of 40; `d474c370` added the
  conversion. The tests still state their expected hit by repeating the code's sum, conversion included, and they
  read the bonus off the built-in kinds.
- The bonus appears on no screen: the Ruby and Shade texts say "more damage" without a figure, and neither node has a
  stat row.
- The Mortar's Heavy shell already has this shape as a stat: `TowerStat.HEAVY_DAMAGE` (`DAMAGE_PER_HIT`, "what a
  Heavy deals past the Mortar's damage"), set in points through `TowerModifiers.heavyDamage` and the base stats.
- Ruby and Shade are one pick-one cluster ("Second missile"), so a tower holds at most one second kind.

## What this feature adds

- The second missile's damage is a stat that the Info pane and the Ruby and Shade nodes show in points ("+40
  damage").
- Ruby and Shade keep +40 each; Frost adds nothing. A Ruby or Shade hit and its Shatter deal exactly what they do
  after `d474c370`.
- Nodes can change the figure, so the balancing agent tunes it in the tree.
- The tests set the bonus themselves and expect the hit in points, without reading the built-in kinds.

## Constraints and open risks

- `td/tower/CLAUDE.md` asks for any flat damage a shot or missile kind adds to be a stat of this kind; the root
  `CLAUDE.md` asks for damage tests in points, from a bonus the test sets itself.
- A missile lands with the stats it was launched with, and a Shatter uses the freeze's stats: the bonus must follow
  the same rule.
- A missile kind does everything differently from its own definition, so the tower never compares kinds.
- One stat or one per kind is open: the cluster makes one enough today, but two kinds could later need different
  figures.

## Decisions made

- Ruby and Shade each stay at +40 points; numbers are the balancing agent's.
- No save-format change is expected: stats are rebuilt from the nodes owned.

## Open questions

1. **One stat or two.** A single second-missile figure, or one for Ruby and one for Shade?
