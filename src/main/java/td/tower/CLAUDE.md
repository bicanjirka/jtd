# `td.tower` — towers, targeting and upgrade buffs

Read the root `CLAUDE.md` first; this file only covers what is specific to this package and
its two subpackages (`targeting`, `buff`).

## Shape

`Tower` is the interface; `AbstractTower` holds position, price, base/current damage and
range, the upgrade-tower list, and the shared `dealDamage` accounting. The five leaf classes
are `final` and are constructed only through `TowerFactory`:

| Class | Name in the UI | Targeting |
|---|---|---|
| `TowerOne` | Triangle | one enemy, furthest along the path |
| `TowerTwo` | Circle | one random enemy, plus distance-falloff splash |
| `TowerThree` | Sunshine | sonar scan: a beam sweeps the circle, hitting whatever it passes |
| `TowerFour` | Stardust | everything in range at once, ghosts included |
| `TowerAura` | Aura | passive; buffs neighbouring towers, never attacks |

## Invariants worth knowing before you change anything here

**`doInit(context, x, y)` must be the last thing a leaf constructor does.** It converts
cell coordinates to pixels and derives `rangeReal`/`rangeReal2` from the board scale, so any
field a subclass computes from the board (e.g. `TowerTwo.spreadRadius`) has to be set before
it, and anything that reads `centerX`/`centerY` (e.g. `TowerAura.scanTowers`) or
registers a listener (e.g. `TowerThree`'s wave subscription) has to run after it.

**Every hit goes through `AbstractTower.dealDamage`, never `enemy.doDamage` directly.** It
is what keeps `damageDealt`/`killCount` honest, in two ways that are easy to get wrong:

- A shot into an enemy another tower already killed this tick is a no-op in the mob, and
  must not be counted as a second kill.
- `doDamage` **returns the damage that actually landed**, which is not the amount passed in
  — a square resists part of every hit, a mob that is not currently a valid target takes
  none of it, and a killing blow is capped at the health that was left. `damageDealt`
  accumulates the return value. Adding the argument instead makes a tower over-report
  against exactly the enemies it performs worst on, and credits it for overkill.

**A tower that subscribes to anything must unsubscribe in `doCleanup`.** `TowerThree`
registers as a `WaveStartListener`, `TowerAura` as a `TowerListener`; both remove
themselves in `doCleanup`, which `TowerRoster` calls on sell *and* on level teardown. A
missed unsubscribe leaks the tower into the next level.

**`TowerThree` decides hits against a swept *arc*, never the beam's instantaneous angle.**
The scan moves about a fifth of a radian per tick, so it is essentially never exactly on an
enemy when a tick is sampled — "is this enemy at the beam's angle" would miss nearly
everything. `SonarSweep.sweptThisTick` asks whether a bearing lies in the arc covered since
the previous tick, and the arc is half-open so a stationary enemy is hit once per revolution
rather than twice at the boundary. It targets by absolute bearing, so where an enemy sits
decides when it is hit, not where it happens to be in the wave's array.

**`TowerThree`'s turret head must be drawn from the same scan angle that decides its hits**
(`sweepRadiansAt`), not from `animationSeconds` like the other spinning heads. A cosmetic
spin would drift out of step and the tower would appear to shoot enemies it is not facing.

**`rangeReal2` is the squared range** and every range check compares squared distances.
Don't introduce a `Math.sqrt` into a per-tick scan.

**`TowerTwo`'s splash falls off as `1 - (d/radius)²`**, where `d` is measured from the mob
that was hit, not from the tower. That curve is flat near the centre and steep at the rim —
half-way out still takes 75% — so it is much more forgiving than a linear falloff would be.
The same `spreadRadius` bounds the splash query and divides the falloff, which is what keeps
the result positive for everything the query returns. `spreadRadius` is fixed at construction
and, unlike damage and range, is deliberately not touched by upgrade buffs.

## Targeting (`td.tower.targeting`)

Filtering and selection are deliberately separate, composable pieces. A tower composes
them; it does not hand-roll a scan over `EnemyRegistry.getEnemies()`.

- `TargetQuery` — "which enemies are legal targets right now", as a fresh immutable
  snapshot. `and` intersects; `all()` is the identity, `none()` the absorber (it
  short-circuits without evaluating the other side).
- `TargetSelector` — picks at most one out of a candidate list
  (`FurthestAlongPathSelector`, `RandomSelector`).

A tower whose cadence is geometric rather than a cooldown composes a query with its own
sweep instead of a selector — see `TowerThree` filtering by range and type through
`InRangeTargetQuery`, then deciding hits with `SonarSweep`.

Implementations take an `EnemyRegistry`, never a `GameWorld` — the read-only slice is all
they need.

`InRangeTargetQuery` has no public constructor: use `anyType` or `ofType`. Passing a
`null` type to mean "any" is exactly the modelled-absence problem the style guide's rule 8
forbids.

## Upgrade stacking (`td.tower.buff`)

`TowerBuff` is the algebra: `none()` is the identity, `combine` is additive, and a tower's
total buff is a `reduce` over its `TowerAura`s. Buff strength is per-aura-tower
(`TowerAura`'s `power` constructor argument), not a shared static — that is what lets
two aura towers of different strengths stack correctly.

`AbstractTower.calcDamageRange()` recomputes `damageCurrent`/`rangeReal` from that reduce.
It must be called on every change to the upgrade list; `registerTower`/`unregisterTower`
already do.

## Adding a new tower

1. Add the leaf class (make it `final`), extending `AbstractTower`, composing
   `td.tower.targeting` pieces rather than writing a new scan.
2. Add a constant to `TowerFactory.type` with its price, and its `createTower` branch.
3. Add a `visit…` method to `TowerVisitor`. The compiler then points you at every place
   that needs the new tower's art: `td.ui.TowerSpriteFrameBuilder` (base and turret head)
   and `td.ui.TowerEffectFrameBuilder` (its transient effect).
4. Add `Palette` constants and their cases in `TowerSpriteFrameBuilder.bodyPaletteFor`
   (an exhaustive switch with no `default`) and in `td.ui.Java2DFrameRenderer`'s
   `towerBodyShape` / `turretHeadShape` / `colorFor`.
5. Add it to `README.md`'s tower table.

The toolbar icon needs no separate art — `PanelTowerSelector` renders it through the same
paint code at a fixed pose, so a tower's board look and its icon cannot drift apart.

**Never branch on a tower's concrete type with `instanceof`.** Use `TowerVisitor`. The
existing `switch (t.getType())` blocks in `AbstractTower.registerTower` and
`TowerAura.scanTowers` are not per-type behaviour — they only ask the role question "is
this an aura tower or not", and adding a sixth tower needs no new branch in either. Keep
them that way rather than growing them into a per-type dispatch.
