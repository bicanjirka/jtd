# `td.tower` (+ `targeting`, `buff`, `upgrade`, `sniper`, `sonar`, `splash`)

## Towers

- `AbstractTower` holds shared state; leaves are `final`, constructed only via `TowerFactory`,
  and their constructor just calls `super(...)` (derived board fields are already `final`).
- A tower records its price from `TowerRoster.priceOf` in its constructor (each copy on the board
  adds 15%), so build it after charging that price and before `TowerRoster.add`.
- A tower's class name and its UI name are the same word and name the behaviour, not the shape.
- Every hit goes through `AbstractTower.dealDamage`, never `enemy.doDamage`. It accumulates the
  damage that *landed* (what `doDamage` returns) and won't count a kill on an already-dead mob,
  and it stamps the tower's own `AttackOrigin` on the attack, so an effect can tell this tower's
  hits from another's (`charge`).
- After `doCleanup` (sell or teardown), `dealDamage` is a no-op, because a burn the tower applied
  keeps ticking. A tower that subscribes to anything unsubscribes in `doCleanup`.
- Range checks compare squared distances (`rangeReal2()`); no `Math.sqrt` in per-tick scans.
- A tower with no cooldown cadence passes `coolDownMax = 0` and overrides `cadence()`.
- `SonarTower` hits what its `SonarBeam` crossed this tick: a spinning beam the arc swept since
  last tick (`SonarSweep`, half-open), never the instantaneous angle. Beams are pure geometry
  (bearings, never enemies), and a perk that changes the beam reshapes it from where it is. The
  head is drawn from `sweepRadiansAt`, the same angle that decides hits. Whatever the beam renews
  each pass lasts `SonarSpec.untilNextPass()`.
- `SplashTower` falloff is `1 - (d/radius)²` from the primary, floored by `BlastSpec.edgeFloor`.
  The blast radius isn't buffed by range; only its own nodes change it, and every distance the
  payload uses (an arc's reach) grows with the same bonus (`BlastSpec.distanceScale()`). The
  blast lands and Saturates first, then the payload runs.
- A tower whose fork renames it overrides `displayName()`; the id and `TowerFactory.Type` stay.
- A missile's speed and size are a tower stat (`ProjectileStats`), shown as `TowerStat.PROJECTILE_SPEED`
  (cells a second, so it holds at any board scale) and `PROJECTILE_SIZE`, and drawn from the same record.
- The Seeker's nest loads from the cooldown whether or not an enemy is in range, so it fills between
  waves; stored missiles launch `launchGapTicks` apart, one target pick per missile. A spread salvo
  (`SalvoPlanner`) avoids the enemies the salvo in progress has fired at, and a perk reacts to a
  landed missile through `Impact`; a rearmed missile is flagged so its freeze rearms nothing. How a
  missile freezes, shatters and crits is spec data (`FreezeSpec`, `ShatterSpec`), not a hook; the
  `FrozenLedger` only watches the enemies the Seeker froze for the tick their freeze ends. A missile's
  payload is decided when it is launched (`PayloadPlan.loadFor` its number), so it is drawn in its
  colour in flight; a plain missile freezes, a payload missile does only what its payload does.
- The Pulse's field touches `Reach.everyone`, never only the visible. Toll is earned by time inside
  (`TollTracker` counts it per enemy) and held by an ordinary `TOLL` effect the field refreshes each
  tick while the enemy is inside, so a visit is exactly the life of that effect.
- A rule the Pulse holds inside its field (`FieldMode`) is an effect refreshed every tick the enemy is
  inside, never a flag on the enemy. Event Horizon counts the deaths of enemies that were inside the
  tick before and resets on the wave announcer; the tower subscribes lazily and unsubscribes in
  `doCleanup`.
- A Hexer's hex is an enemy effect in `EffectCategory.HEX`, applied through `applyEffect`, and that
  effect is the hex's only timer (spirit paces it). The Hexer's `HexLedger` only watches it: what
  a hex pays out when it ends or its carrier dies is settled at the start of the Hexer's tick,
  never by the enemy. Each hex picks its own target in one pass per cast, never per tick.
- What a hex shares or releases in range (Sympathy, Reckoning) reaches `SHARE_CELLS` grown by the
  blast radius bonus, and only enemies the ledger still watches. A share tops the receiver up to
  what the carrier has and never adds on top, so two carriers sharing back and forth settle
  instead of climbing.
- The Mortar's shell is slow and unguided: it flies to where its target stood, so a fast enemy dodges
  it. Its dead zone is a fixed distance on the `Reach` that range never moves. Bracketing is read at
  the landing, from the spec as it stands then, and `BracketTracker` is the only memory of where the
  last shell fell. Every shell Cracks plating; a shell's size is a tower stat that follows its damage.
  A shell's type (`ShellPlan.typeOf` its number) is fixed when it is fired, so it is drawn in its look
  in flight; a special shell leaves one zone where it lands, owned by the Mortar (`ZoneOwner`), and
  shrapnel never does.
- Never `instanceof`/cast a tower. Use `TowerVisitor`, or ask the tower (`Tower.buffFor`).
  `AuraTower.buffs`' single "is this an aura" check stays the only role check.

## Targeting

- Compose a `TargetQuery` (filter; `and`, `or`, identity `all()`, absorber `none()`) with a
  `TargetSelector` (picks one). Don't hand-roll a scan over `EnemyRegistry.getEnemies()`.
- Targeting takes an `EnemyRegistry`, never `GameWorld`, so `td.projectile` can reuse it.
- `InRangeTargetQuery` only through `visible` (what a tower may aim at or be triggered by) or
  `everyone` (what an area effect touches, hidden enemies included). Test invisibility nowhere else:
  a tower picks per action, never per tower kind. `NearestSelector` is centred on any point
  (missiles retarget around themselves). `RandomSelector` takes a `RandomSource`. A tower that picks
  one target wraps its selector in `PreferringSelector.priority`, so an enemy under `PRIORITY` wins
  while in range; the preference is enemy state, never a player choice.
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
- `TowerBuff` axes: damage, range, fireRate, bounty, critChance, critDamage (adds to the tower's
  crit multiplier), armorPenetration, magicPenetration. Build one from a single-axis factory plus `withX`
  (`TowerBuff.damage(0.3f).withRange(0.1f)`), not from `none()`. `combine` adds every axis except
  fireRate, where each bonus cuts the cooldown that remains (they multiply).
- Enemy disruption reaches a tower only through `GameWorld.disruptions()`, sampled at the tower's
  centre in the towers phase (`beginTick`). It folds in as a negative `TowerBuff`, and
  `TowerBuff` floors combined fire-rate and range bonuses at `MIN_BONUS` (-0.75).
- Cooldown has a base/current split like damage and range (`coolDownMax` vs `coolDownCurrent()`).
- A kill this tower makes runs `onKill(EnemyMob)` (the mob still carries the effects it died
  under); a kill made from inside the hook, such as an explosion's, triggers nothing. A
  temporary self-buff goes through `grantTimedBuff` and ends in `beginTick`; it never stacks.
- `dealDamage` sends the hit with `TowerStats.attack()` (crit chance and multiplier, penetration);
  the target rolls the crit. A special shot passes a one-off profile to
  `dealDamage(enemy, damage, attack)` (`withGuaranteedCrit()`, extra penetration) instead of
  adding a hook.
- Damage that ticks (a pool's sink, a field's tick) goes through `dealPeriodicDamage`: periodic
  damage never crits. Every effect goes on through `applyEffect`, which binds that sink, and a
  stacking debuff through `applyStacks(kind)`, which owns its clock. The hit that starts a pool
  scales it by `potencyOfHit`. Damage that scales with the target's protection asks
  `HitReceiver.reductionAgainst`.

## Experience

- XP comes only from `ExperienceAwarder`, once per finished walk (`WalkEndListener`): the bounty
  to every tower that `reached` the mob, and once to every tower buffing one of those. Never per
  hit, per kill or per tick.
- `reached` is decided at walk end, never tracked per tick: the mob went live after the tower was
  built (`entryOrdinal` against the count the tower recorded) and its walked stretch passed within
  `TowerStats.reachReal()` (range before disruption). A tower with no reach on the path (the Aura)
  returns `false` and earns through the towers it buffs.
- `TowerExperience` publishes XP and the rank-up tick as one snapshot; only the game loop earns.
- A tower's deed (the moment it does its job, which its `PurposeCondition` on head III counts) goes
  through `countDeedOfAttack()` (one per attack, however many enemies it hit) or
  `countDeedOfSecond()` (a continuous effect, once a second). Both count only once Attune is owned.

## Upgrade tree

- Slots `BASE` (always `StandardBaseSlot.nodes`: the Range line and Attune -> Awaken ->
  Transcendent), `HEAD` (two chains, plus an extra node that excludes nothing) and `SPECIAL` (one
  set of up to three, or one set per chain). Content per tower comes from
  `docs/features/FEATURE-tower-specialization-abilities.md` and the tower-rework feature requests.
- Build a node at its step: `UpgradeTier.HEAD_2.node(id, name, PRICE)` sets its slot, its price
  (a multiple of the list price), its XP (the gate table) and the base node that opens it;
  `after(previous)` adds its line. Then `withBuff`/`withGate`/`withExtraEffect`. `requires`
  decides whether a node is offered. Once offered it needs its `xp` and its `gate`, a purpose or
  layout condition (`always()`, `ClusterCondition`, `TranscendentCondition`) whose `progress()`
  feeds the UI. `UpgradeNode.gateMet` is the one check for both: never test `gate()` alone.
- Exclusivity is an `ExclusiveChoice` on the tree (`oneOf` for chain roots and IV-A | IV-B,
  `specials` for a special set), never `slotEmpty` in `requires`: the tree enforces it and the
  panel draws it. Pass `StandardBaseSlot.nodes` a tree's level III heads; without any, the tree
  has no Transcendent and no Range III.
- Never hand-write a node's bonus into a tower's description: `UpgradeNode.bonuses()` derives it
  and the Upgrades panel lists offered nodes.
- A bonus outside `TowerBuff`'s axes goes in `onUpgradeBought`, matching the node by `equals` (not
  reference), and the same constant carries a matching `extraEffect` phrase.
- `buyUpgrade` is the only entry point and is check-and-charge (returns `false` without effect).
  Don't pre-check affordability. It publishes `TowerStats` before `UpgradeState`.
- Pass `StandardBaseSlot.nodes` a `BaseSlotPerks` for what Attune and Range III add to a tower.
- A node whose behaviour is more than a `TowerBuff` becomes a perk, in the tower's own package
  (`sniper`, `sonar`, `splash`). A static `PerkCatalogue` says which perks each node brings (several may),
  and the tower's `OwnedPerks` builds its own, as some carry state. Perks run in purchase order:
  first `refineSpec` (whom the tower may hit, how it picks, its rhythm), then per hit. Perks are
  pure (no tower, no `GameWorld`); what they may make the tower do is the tower's actions
  interface.
- Perks widen or narrow whom a tower may hit through its `Reach`. A dead zone goes on the `Reach`
  (`withDeadZone`), which applies it last, so it holds whatever order the perks were bought in;
  never `and` one onto the query.
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
5. `upgradeTree()`: `UpgradeTree.of(StandardBaseSlot.nodes(PRICE, levelThrees...))` with two
   `HEAD` chains built from `UpgradeTier` steps and 1-3 `UpgradeTier.SPECIAL` nodes, plus an
   `ExclusiveChoice` per choice.
6. `behaviours()` (one short row each: targeting, effects applied) and, for the shop only, a
   `description()` saying what no row does.
7. `README.md`'s tower table. The toolbar icon reuses the board paint code, so it needs no art.
