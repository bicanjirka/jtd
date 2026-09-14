# `td.tower` — towers, targeting, aura buffs and upgrade paths

Read the root `CLAUDE.md` first; this file only covers what is specific to this package and
its three subpackages (`targeting`, `buff`, `upgrade`).

## Shape

`Tower` is the interface; `AbstractTower` holds position, price, base/current damage/range/
fire-rate, the list of nearby Aura towers buffing it, this tower's own permanently-chosen
upgrade path (if any), and the shared `dealDamage` accounting. The five leaf classes are
`final` and are constructed only through `TowerFactory`:

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

**A sold or cleared tower must stop accounting for damage, including damage it applied
before being removed.** `doCleanup` sets a `removed` flag, and `dealDamage` is a no-op once
it's set. This matters because a damage-over-time `td.effect.Effect` (a burn) a tower applied
is bound to that tower's own `dealDamage` and keeps ticking on the enemy for several ticks
after the tower itself might be sold — without the guard, a sold tower would keep inflating
`damageDealt`/`killCount` and paying its upgrade path's bounty bonus on an object the player
has already been refunded for.

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
the result positive for everything the query returns. `spreadRadius` is set at construction
and, unlike damage and range, is untouched by an Aura tower's buff — its only way to change
is `TowerTwo`'s own "Siege" upgrade path bumping it once via `onUpgradePathChosen` (see
below), not any live, continuously-recomputed algebra.

## Targeting (`td.tower.targeting`)

Filtering and selection are deliberately separate, composable pieces. A tower composes
them; it does not hand-roll a scan over `EnemyRegistry.getEnemies()`.

- `TargetQuery` — "which enemies are legal targets right now", as a fresh immutable
  snapshot. `and` intersects; `all()` is the identity, `none()` the absorber (it
  short-circuits without evaluating the other side).
- `TargetSelector` — picks at most one out of a candidate list
  (`FurthestAlongPathSelector`, `RandomSelector`, `NearestSelector`).

A tower whose cadence is geometric rather than a cooldown composes a query with its own
sweep instead of a selector — see `TowerThree` filtering by range and type through
`InRangeTargetQuery`, then deciding hits with `SonarSweep`.

Implementations take an `EnemyRegistry`, never a `GameWorld` — the read-only slice is all
they need. This is also what lets `td.projectile.MissileProjectile` reuse
`InRangeTargetQuery`/`NearestSelector` directly for its own retargeting, without needing to
depend on any concrete tower.

`NearestSelector` is centred on an arbitrary point, not a tower's own position — a homing
missile retargets around *its own current location*, which is the one case in this codebase
where "nearest" means nearest to something other than the object doing the asking.

`InRangeTargetQuery` has no public constructor: use `anyType` or `ofType`. Passing a
`null` type to mean "any" is exactly the modelled-absence problem the style guide's rule 8
forbids.

## Aura buff stacking (`td.tower.buff`)

`TowerBuff` is the algebra: `none()` is the identity, `combine` is additive, and a tower's
total buff is a `reduce` over its nearby `TowerAura`s **combined with its own chosen
upgrade path's bonus** (see below) — `AbstractTower.calcDamageRange()` does both in one
fold, which is what lets a specialization and an Aura tower's buff stack for free. Buff
strength is per-aura-tower (`TowerAura`'s `power` constructor argument), not a shared
static — that is what lets two aura towers of different strengths stack correctly.

`TowerBuff` carries four independent bonus axes — `damageBonus`, `rangeBonus`,
`fireRateBonus`, `bountyBonus` — each defaulting to 0 at `none()`. An Aura tower's own
`buff()` only ever sets the first two; the latter two exist for upgrade paths (below) to use.

`AbstractTower.calcDamageRange()` recomputes `damageCurrent`/`rangeCurrent`/
`coolDownCurrent` from that reduce. It must be called on every change to either input;
`registerTower`/`unregisterTower` (the Aura-tower side) and `chooseUpgradePath` (the
path side) already do.

**A tower's fire rate has a base/current split just like damage and range.**
`coolDownMax` is the base cooldown a leaf sets at construction; `coolDownCurrent` is what
tick code actually resets `coolDown` to after firing, and is `coolDownMax` shortened by
`TowerBuff.fireRateFor`. `rateLine(int)` takes whichever one the caller means to describe
(`getInfoString` passes `coolDownMax`, `getStatusString` passes `coolDownCurrent`) rather
than assuming which is wanted the way the old zero-argument version did.

## In-place upgrade paths (`td.tower.upgrade`)

A tower can permanently specialize into one of its own `availablePaths()` — at most once,
ever, for that tower instance. This is a different mechanic from the Aura tower's buff
above: an Aura tower buffs *other* towers continuously from outside; a chosen upgrade path
changes what *this* tower itself is, once, and stays changed for its lifetime.

- `UpgradePath` — a tower's specialization: a display name, a price (paid the same way
  buying a tower is), a `TowerBuff` stat bonus, and the `UpgradeCondition` gating it.
- `UpgradeCondition` — "is this path currently available", independent of affordability.
  `always()` is the identity (the "money only" gate — the path is limited by price alone).
  `ClusterCondition`, `DamageDealtCondition`, `KillCountCondition` read a tower's own
  position/stats and, for `ClusterCondition`, the roster via `GameWorld`.
- `AbstractTower.chooseUpgradePath(path)` is the one entry point: it validates `path` is
  actually one of this tower's own `availablePaths()` and that none has been chosen yet,
  pays its price via `context.doPay`, sets `chosenPath`, calls the `onUpgradePathChosen`
  hook (a no-op unless a leaf overrides it - see below), and recomputes
  `calcDamageRange()`. It returns `false` without effect on any failure, mirroring
  `GameWorld.doPay`'s check-and-charge-in-one-call contract - never gate a call to it on a
  separate affordability check first.
- **A path's bonus that isn't expressible through `TowerBuff` is applied via
  `onUpgradePathChosen`, not through the shared algebra.** `TowerTwo`'s `spreadRadius` and
  `TowerThree`'s sweep rate are each touched by only one tower's one path, so they are the
  exception, not the rule - a leaf overriding this hook mutates its own field directly,
  matched by reference against its own private `UpgradePath` constants rather than by a
  string/id (keeps the match type-safe and avoids a stringly-typed switch).
- `AbstractTower.availablePaths()` defaults to `List.of()` - only a tower with real content
  (added per-leaf, not part of this shared mechanism) overrides it. The Aura tower does not
  override it and offers no paths of its own for v1.
- The UI (`td.ui.PanelTowerInfo`) and the render accent ring
  (`Java2DFrameRenderer.paintUpgradeAccent`, see `td/ui/CLAUDE.md`) both key off
  `availablePaths()`/`getChosenPath()` alone - a tower's own domain state is the single
  source of truth for what's choosable and what's already chosen, not any UI-side tracking.

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
6. If it offers upgrade paths, override `availablePaths()` with its (currently: exactly
   two) `UpgradePath`s, and `onUpgradePathChosen` only if one of them bumps a stat outside
   `TowerBuff`'s four axes. No new `Palette` role is needed for this: the specialization
   ring's two roles are shared across every tower type (see `td/ui/CLAUDE.md`).

The toolbar icon needs no separate art — `PanelTowerSelector` renders it through the same
paint code at a fixed pose, so a tower's board look and its icon cannot drift apart.

**Never branch on a tower's concrete type with `instanceof`.** Use `TowerVisitor`. The
existing `switch (t.getType())` blocks in `AbstractTower.registerTower` and
`TowerAura.scanTowers` are not per-type behaviour — they only ask the role question "is
this an aura tower or not", and adding a sixth tower needs no new branch in either. Keep
them that way rather than growing them into a per-type dispatch.
