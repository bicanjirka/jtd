# `td.tower` (+ `targeting`, `upgrade`, `aura`, `proc`, `sniper`, `sonar`, `splash`)

## Towers

- `AbstractTower` holds shared state; leaves are `final`, constructed only via `TowerFactory`,
  and their constructor just calls `super(...)` (derived board fields are already `final`).
- A tower records its price from `TowerRoster.priceOf` in its constructor (each copy on the board
  adds 15%), so build it after charging that price and before `TowerRoster.add`.
- A tower's class name names the behaviour, not the shape. Its UI name is `TowerFactory.Type.displayName()`, chosen
  separately and display only: the constant, the class, the node ids and the hotkey keep the class's word.
- Every hit goes through `AbstractTower.dealDamage`, never `enemy.doDamage`. It accumulates the
  damage that *landed* (what `doDamage` returns) and won't count a kill on an already-dead mob,
  and it stamps the tower's own `AttackOrigin` on the attack, so an effect can tell this tower's
  hits from another's (`charge`).
- After `doCleanup` (sell or teardown), `dealDamage` is a no-op, because a burn the tower applied
  keeps ticking. A tower that subscribes to anything unsubscribes in `doCleanup`.
- Range checks compare squared distances (`rangeReal2()`); no `Math.sqrt` in per-tick scans.
- A tower that hits every tick passes `coolDownMax = 0`; one whose cadence is not a cooldown (the Sonar's
  spin) also overrides `cadence()`.
- Every tower's cadence is its `FireClock` run at `attackRate()`, never a cooldown counted in whole
  ticks: the clock keeps the fraction a rate leaves over, so a buff is exact (a 10% faster Pulse hits 22
  times in 20 ticks, a second hit in the tick the remembered tenths reach a whole). A shot that waits
  longer or shorter than the base spends more or less than one period. The Sonar divides its seconds per
  revolution by the rate instead, and no counter that carries an attack rate may round to a tick.
- `SonarTower` hits what its `SonarBeam` crossed this tick: a spinning beam the arc swept since
  last tick (`SonarSweep`, half-open), never the instantaneous angle. Beams are pure geometry
  (bearings, never enemies), and a perk that changes the beam reshapes it from where it is. The
  head is drawn from `sweepRadiansAt`, the same angle that decides hits. Whatever the beam renews
  each pass lasts `SonarSpec.untilNextPass()`.
- `SplashTower` falloff is `1 - (d/radius)²` from the primary, floored by `BLAST_EDGE`. The blast
  radius is `AREA_RADIUS`: range doesn't move it, auras never emit it, only its own nodes change it,
  and every distance the payload uses (an arc's reach) grows with it. The blast lands and Saturates
  first, then the payload runs.
- A tower whose fork renames it overrides `displayName()`; the id and `TowerFactory.Type` stay.
- A missile's speed and size are a tower stat (`ProjectileStats`), shown as `TowerStatRow.PROJECTILE_SPEED`
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
- Mirror Field listens to the roster's `ShieldListener` while the rule is owned (and drops it in
  `doCleanup`): what a shield takes of any hit on an enemy that was inside the field's last tick comes back
  as periodic damage that goes through shields (`AttackProfile.withIgnoredShields`), and is never mirrored again.
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
  it. Its dead zone is `DEAD_ZONE`, a fixed distance on the `Reach` that range never moves. Bracketing is read at
  the landing, from the spec as it stands then, and `BracketTracker` is the only memory of where the
  last shell fell. Every shell Cracks plating; a shell's size is a tower stat that follows its damage.
  A shell's type (`ShellPlan.typeOf` its number) is fixed when it is fired, so it is drawn in its look
  in flight; a special shell leaves one zone where it lands, owned by the Mortar (`ZoneOwner`), and
  shrapnel never does. Shrapnel hands its shell's zone effect to the enemies it hits directly through
  `ZoneEffects.touch`, the same pulse a zone gives. Bomblets are placed along the enemy path from the
  point of it nearest the landing (`PathLine`), never by geometry around the impact. Every blast leaves
  a `BlastMark` the board draws as a fading flash; a nuke adds its own `NukeFlash`. Predictive Fire
  asks the target where it will stand (`EnemyTarget.positionAfter`) and re-aims a few times, since flight
  time depends on where the shell is going; a Barrage's later shells re-pick their target when they fire.
- The Cinder never deals direct damage: each wave sets an enemy alight once, and `ignite` is the one place
  that decides how strong the pool starts (Stoke steps, then a crit ignition's factor, which it rolls with
  `HitReceiver.rollsCrit` since an ignition is no hit). The `Burning` ledger is the only memory of whom it
  has set alight: Stoke steps live there and go when the burn has been out longer than the grace. What its
  pools do to an enemy (hold more, mark faster, cauterize, heat, mark for magic, burst at a freeze) is a
  `PoolTuning` the pool carries, never state the Cinder keeps on the enemy; a Thermal Shock's chill and an
  Everburn top-up are the Cinder's own work in `tendBurning`. Bellows reads the attack speed in what the tower
  receives (`TowerStats.received()`), so it needs no Aura-specific code.
- A Splash's mines and a Hexer's cursed clouds are zones it makes (`Zone.mine`, `Zone.cloud`); a mine
  blasts through the same `blast` a shot does, centred where it lay, with the enemy that stepped on it as
  the primary, and the Splash keeps its own list of mines to replace the oldest and to take them off with it.
- The Aura gives `buffFor`: damage and range at `AURA_POWER` (plus Kinship), half the disruption, and
  every owned node's emission; Chosen scales it all ×3. Its mechanics (Kinship, the XP mentoring,
  Chosen, Rally) are `AuraPerk`s over an `AuraSpec`.
- The Aura never reaches into another tower's state except through `Tower.receiveStatus` (Rally).
  Withering Field is an interval in the buff (`WITHER_INTERVAL`) that each tower spends on its own first
  hit after it, never on periodic damage. Chosen's hero is derived from XP on every call, so
  `AuraTower.doTick` republishes every tower's stats when the hero changes. Rally's spot is the path point
  nearest the Aura, found once.
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

- A tower's stats are `TowerStat` keys resolved by the shared `td.stat` formula into one immutable
  `TowerStats` (correlated fields, read by the tick thread). Read them through `stats()` or the
  conveniences (`damageCurrent()`, `rangeReal()`, `rangeReal2()`, `attackRate()`).
- Layers: base, owned nodes (`UpgradeState.totalModifiers`), held statuses, then received
  emissions (`buffFor`), then disruption × `DISRUPTION_TAKEN`. An aura never receives an emission.
- What a tower receives is **derived, never stored**: a fold of every tower's `buffFor(this)`.
  `recalculateStats()` runs on every tower whenever the roster or any upgrade changes, and in the
  towers phase whenever a tower that `emitsBuffs()` republished in `beginTick`.
- Author bonuses with `TowerModifiers`. Attack speed is a fraction that adds: `rate = 1 + Σ`, held
  to `[-0.75, 9]`. Every tower's cadence is its `FireClock` advanced by `attackRate()`; the Sonar
  divides its seconds per revolution by it.
- Enemy disruption reaches a tower only through `GameWorld.disruptions()`, sampled at its centre in
  `beginTick`, and folds in as negative attack speed and range in cells, times `DISRUPTION_TAKEN`.
- A kill this tower makes runs `onKill(EnemyMob)` (the mob still carries the effects it died
  under); a kill made from inside the hook triggers nothing.
- A temporary or next-shot self-buff is a `TowerStatus` granted with `grantStatus` (another tower's
  with `receiveStatus`): the same id restarts unless it keeps a running copy, different ids stack.
  Every attack starts with `beginShot()`, which spends shot charges and returns the stats for that
  attack; use them, not `damageCurrent()`.
- How long a tower's effects and timed buffs last is a stat (`EFFECT_DURATION`, `BUFF_DURATION`).
  Every effect goes through `applyEffect`, which scales it; what a tower leaves on the ground takes
  its lifetime through `lengthened(ticks)`.
- `dealDamage` sends the hit with `TowerStats.attack()`; a special shot passes a one-off profile.
  Damage that ticks goes through `dealPeriodicDamage` and never crits. The hit that starts a pool
  scales it by `potencyOfHit`.

## Experience

- XP comes only from `ExperienceAwarder`, once per finished walk (`WalkEndListener`): the bounty
  to every tower that `reached` the mob, and once to every tower buffing one of those. Never per
  hit, per kill or per tick.
- `reached` is decided at walk end, never tracked per tick: the mob went live after the tower was
  built (`entryOrdinal` against the count the tower recorded) and its walked stretch passed within
  `TowerStats.reachReal()` (range before disruption). A tower with no reach on the path (the Aura)
  returns `false` and earns through the towers it buffs.
- What a tower adds to another's XP is derived like a buff: `Tower.xpBonusFor(earner)` summed over every
  tower at award time (Kinship, Tutelage, the Apprentice mark), and `Tower.xpSharedWith(earners)` names the towers a
  tower makes earn what those did (Shared Lessons). The fraction XP cannot hold carries in the earner
  (`earnXp(bounty, bonus)`), so a small bonus is never rounded away.
- `ExperienceAwarder` runs on a tick for every finished walk, so it stays a pass or two over the towers, never
  a pass over towers inside a pass over towers inside a pass over the reached ones.
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
  `after(previous)` adds its line. Then `withModifiers`/`withGate`/`withNote`. `requires`
  decides whether a node is offered. Once offered it needs its `xp` and its `gate`, a purpose or
  layout condition (`always()`, `ClusterCondition`, `TranscendentCondition`) whose `progress()`
  feeds the UI. `UpgradeNode.gateMet` is the one check for both: never test `gate()` alone.
- Exclusivity is an `ExclusiveChoice` on the tree (`oneOf` for chain roots and IV-A | IV-B,
  `specials` for a special set), never `slotEmpty` in `requires`: the tree enforces it and the
  panel draws it. Pass `StandardBaseSlot.nodes` a tree's level III heads; without any, the tree
  has no Transcendent and no Range III.
- Never hand-write a node's bonus into a tower's description: `UpgradeNode.bonuses()` derives it
  and the Upgrades panel lists offered nodes.
- `buyUpgrade` is the only entry point and is check-and-charge (returns `false` without effect).
  Don't pre-check affordability. It publishes `TowerStats` before `UpgradeState`.
- Pass `StandardBaseSlot.nodes` the tower's base range (Range nodes add a share of it, in cells) and a
  `BaseSlotPerks` for what Attune, Awaken and Range III add to a tower.
- A node is data: `withModifiers` (its own stats, flat, through `TowerModifiers`), `withEmission`
  (what an aura gives), `withNote` (words only for what a perk does). A number a node changes is a
  `TowerStat` when the tower has it from the start or it belongs to the Attune mechanic; any other
  number belongs to the mechanic a node adds, in that mechanic's spec, set by its perk. Never add a
  field a node mutates. `TowerStatCatalogueTest` rejects a node that changes a stat its tower
  doesn't set.
- An upgrade that applies a generic effect, grants a status, changes a hit conditionally, picks a target
  or flips a capability is data on its node: `withProc`, `withHitBonus`, `withTargeting` (the first
  owned node that sets one decides), `withCapability`. Every attacking tower applies
  `hitAdjustment(...)` to each hit it computes and reports each landed attack with `report(...)` after
  its perks settled it; hit bonuses add.
- A node that adds a mechanic brings a perk, in the tower's own package. A static `PerkCatalogue`
  says which perks each node brings, and the tower's `OwnedPerks` builds its own. Perks run in
  purchase order through `refineSpec` (and the tower's per-hit hooks); they are pure (no tower, no
  `GameWorld`).
- Perks widen or narrow whom a tower may hit through its `Reach`. A dead zone goes on the `Reach`
  (`withDeadZone`), which applies it last, so it holds whatever order the perks were bought in;
  never `and` one onto the query.
- The UI reads `offeredUpgrades()`/`upgrades()` only; the tower is the source of truth.

## Adding a tower

1. A `final` leaf composing `td.tower.targeting` pieces, passing a `TowerBaseStats` that sets every
   non-universal stat the tower reads to `super(...)` (`withCritChance`/`withCritMultiplier` for
   innate crit). A passive tower
   overrides `isPassive()`.
2. A `TowerFactory.Type` constant and its `createTower` branch.
3. A `TowerVisitor` method; the compiler then leads to `TowerSpriteFrameBuilder` and
   `TowerEffectFrameBuilder`.
4. `Palette` constants plus cases in `TowerSpriteFrameBuilder.bodyPaletteFor` and
   `Java2DFrameRenderer`'s `towerBodyShape`/`turretHeadShape`/`colorFor`.
5. `upgradeTree()`: `UpgradeTree.of(StandardBaseSlot.nodes(PRICE, RANGE, levelThrees...))` with two
   `HEAD` chains built from `UpgradeTier` steps and 1-3 `UpgradeTier.SPECIAL` nodes, plus an
   `ExclusiveChoice` per choice.
6. `behaviours()` (one short row each: targeting, effects applied) and, for the shop only, a
   `description()` saying what no row does.
7. `README.md`'s tower table. The toolbar icon reuses the board paint code, so it needs no art.
