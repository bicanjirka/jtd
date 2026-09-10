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
| `TowerThree` | Sunshine | index-stable round robin over everything in range, then recharges |
| `TowerFour` | Stardust | everything in range at once, ghosts included |
| `TowerUpgrade` | Power | passive; buffs neighbouring towers, never attacks |

## Invariants worth knowing before you change anything here

**`doInit(context, x, y)` must be the last thing a leaf constructor does.** It converts
cell coordinates to pixels and derives `rangeReal`/`rangeReal2` from the board scale, so any
field a subclass computes from the board (e.g. `TowerTwo.spreadRadius`) has to be set before
it, and anything that reads `centerX`/`centerY` (e.g. `TowerUpgrade.scanTowers`,
`TowerThree.waveStarted`) has to run after it.

**Every hit goes through `AbstractTower.dealDamage`, never `enemy.doDamage` directly.** It
is what keeps `damageDealt`/`killCount` honest: a shot into an enemy another tower already
killed this tick is a no-op in the mob, and must not be counted as a second kill.

**A tower that subscribes to anything must unsubscribe in `doCleanup`.** `TowerThree`
registers as a `WaveStartListener`, `TowerUpgrade` as a `TowerListener`; both remove
themselves in `doCleanup`, which `TowerRoster` calls on sell *and* on level teardown. A
missed unsubscribe leaks the tower into the next level.

**`TowerThree` holds enemy *indices*, not enemy references.** Its round robin is stable
against the roster's per-wave array, which is why it resets those arrays on `waveStarted`.
That is also why `NextTargetQuery` exists as a separate interface from `TargetQuery` — a
query returning a fresh `List` snapshot cannot offer index stability.

**`rangeReal2` is the squared range** and every range check compares squared distances.
Don't introduce a `Math.sqrt` into a per-tick scan.

## Targeting (`td.tower.targeting`)

Filtering and selection are deliberately separate, composable pieces. A tower composes
them; it does not hand-roll a scan over `EnemyRegistry.getEnemies()`.

- `TargetQuery` — "which enemies are legal targets right now", as a fresh immutable
  snapshot. `and` intersects; `all()` is the identity, `none()` the absorber (it
  short-circuits without evaluating the other side).
- `TargetSelector` — picks at most one out of a candidate list
  (`FurthestAlongPathSelector`, `RandomSelector`).
- `NextTargetQuery` — the index-stable round-robin shape, for `TowerThree` only.

Implementations take an `EnemyRegistry`, never a `GameWorld` — the read-only slice is all
they need.

`InRangeTargetQuery` has no public constructor: use `anyType` or `ofType`. Passing a
`null` type to mean "any" is exactly the modelled-absence problem the style guide's rule 8
forbids.

## Upgrade stacking (`td.tower.buff`)

`TowerBuff` is the algebra: `none()` is the identity, `combine` is additive, and a tower's
total buff is a `reduce` over its `TowerUpgrade`s. Buff strength is per-upgrade-tower
(`TowerUpgrade`'s `power` constructor argument), not a shared static — that is what lets
two upgrade towers of different strengths stack correctly.

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
`TowerUpgrade.scanTowers` are not per-type behaviour — they only ask the role question "is
this an upgrade tower or not", and adding a sixth tower needs no new branch in either. Keep
them that way rather than growing them into a per-type dispatch.
