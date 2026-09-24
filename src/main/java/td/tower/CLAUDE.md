# `td.tower` (+ `targeting`, `buff`, `upgrade`)

## Towers

- `AbstractTower` holds shared state; leaves are `final`, constructed only via `TowerFactory`,
  and their constructor just calls `super(...)` (derived board fields are already `final`).
- A tower's class name and its UI name are the same word and name the behaviour, not the shape.
- Every hit goes through `AbstractTower.dealDamage`, never `enemy.doDamage`. It accumulates the
  damage that *landed* (what `doDamage` returns) and won't count a kill on an already-dead mob.
- After `doCleanup` (sell or teardown), `dealDamage` is a no-op, because a burn the tower applied
  keeps ticking. A tower that subscribes to anything (e.g. `SonarTower` as a
  `WaveStartListener`) unsubscribes in `doCleanup`.
- Range checks compare squared distances (`rangeReal2()`); no `Math.sqrt` in per-tick scans.
- A tower with no cooldown cadence passes `coolDownMax = 0` and overrides `rateLine(int)`.
- `SonarTower` hits what lies in the arc swept since last tick (`SonarSweep`, half-open), never
  the instantaneous beam angle. Its head is drawn from `sweepRadiansAt`, the same angle that
  decides hits.
- `SplashTower` falloff is `1 - (d/spreadRadius)²` from the hit mob. `spreadRadius` isn't
  buffed; only its own upgrade node changes it.
- Never `instanceof`/cast a tower. Use `TowerVisitor`, or ask the tower (`Tower.buffFor`).
  `AuraTower.buffs`' single "is this an aura" check stays the only role check.

## Targeting

- Compose a `TargetQuery` (filter; `and`, identity `all()`, absorber `none()`) with a
  `TargetSelector` (picks one). Don't hand-roll a scan over `EnemyRegistry.getEnemies()`.
- Targeting takes an `EnemyRegistry`, never `GameWorld`, so `td.projectile` can reuse it.
- `InRangeTargetQuery` only through `anyType`/`ofType`. `NearestSelector` is centred on any point
  (missiles retarget around themselves). `RandomSelector` takes a `RandomSource`.
- `InWedgeTargetQuery` tests the *current* heading from `TurretAim.currentRadians()`, the same
  angle the head is drawn at. It is deliberately not built on `SonarSweep`.

## Stats and buffs

- `recalculateStats()` publishes one immutable `TowerStats` (correlated fields, read by the tick
  thread). Read it through `damageCurrent()`/`coolDownCurrent()`/`rangeReal()`/`rangeReal2()` or
  `stats()`.
- The buff a tower receives is **derived, never stored**: a fold of every tower's
  `buffFor(this)` plus its own owned nodes' `totalBuff()`. No aura↔client index on either side
  (`AuraTower.buffedTowers()` is also derived). Call `recalculateStats()` on every tower whenever
  the roster or any upgrade changes; `TowerRoster.clear()` needn't.
- `TowerBuff` axes: damage, range, fireRate, bounty, critChance, armorPenetration,
  magicPenetration. Build one from a single-axis factory plus `withX`
  (`TowerBuff.damage(0.3f).withRange(0.1f)`), not from `none()`.
- Cooldown has a base/current split like damage and range (`coolDownMax` vs `coolDownCurrent()`).
- `dealDamage` sends the hit with `TowerStats.attack()` (crit chance and multiplier, penetration);
  the target rolls the crit. A tower never reads the target's stats to adjust its own hit.

## Upgrade tree

- Slots `BASE` (always `StandardBaseSlot` range + Awaken), `HEAD` (two exclusive chains) and
  `SPECIAL` (one to three exclusive roots, gated on Awaken). Content per tower comes from
  `docs/features/FEATURE-tower-specialization-abilities.md`; nodes waiting on a missing primitive
  are no-op hooks with a `TODO.md` entry.
- Build nodes with `UpgradeNode.of(...)` plus `withBuff`/`withRequires`/`withGate`/
  `withExtraEffect`. `requires` decides whether a node is offered (`UpgradeCondition.owns(id)`,
  `slotEmpty(slot)`, `StandardBaseSlot.opens(slot)`, combined with `and`/`or`). `gate` is the
  performance condition to clear once offered (`always()`, `KillCountCondition`,
  `DamageDealtCondition`, `ClusterCondition`), and its `progress()` feeds the UI.
- Never hand-write a node's bonus into a tower's description: `UpgradeNode.describe()` derives it
  and `upgradeNodesBlock()` lists offered nodes automatically.
- A bonus outside `TowerBuff`'s axes goes in `onUpgradeBought`, matching the node by `equals` (not
  reference), and the same constant carries a matching `extraEffect` phrase.
- `buyUpgrade` is the only entry point and is check-and-charge (returns `false` without effect).
  Don't pre-check affordability. It publishes `TowerStats` before `UpgradeState`.
- The UI reads `offeredUpgrades()`/`upgrades()` only; the tower is the source of truth.

## Adding a tower

1. A `final` leaf composing `td.tower.targeting` pieces, passing a `TowerBaseStats` to
   `super(...)` (`withCritChance`/`withCritMultiplier` for innate crit). A passive tower
   overrides `isPassive()`.
2. A `TowerFactory.Type` constant and its `createTower` branch.
3. A `TowerVisitor` method; the compiler then leads to `TowerSpriteFrameBuilder` and
   `TowerEffectFrameBuilder`.
4. `Palette` constants plus cases in `TowerSpriteFrameBuilder.bodyPaletteFor` and
   `Java2DFrameRenderer`'s `towerBodyShape`/`turretHeadShape`/`colorFor`.
5. `upgradeTree()`: `StandardBaseSlot.rangeNode`/`awakenNode`, two `HEAD` chains whose roots
   require `StandardBaseSlot.opens(HEAD)`, and 1-3 `SPECIAL` roots requiring `opens(SPECIAL)`.
6. `README.md`'s tower table. The toolbar icon reuses the board paint code, so it needs no art.
