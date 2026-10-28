# `td.tower` (+ `targeting`, `upgrade`, `proc`, `aura`, `cinder`, `mortar`, `pulse`, `seeker`, `sniper`, `sonar`, `splash`)

## Towers

- `AbstractTower` holds shared state; leaves are `final`, constructed only via `TowerFactory`,
  and their constructor just calls `super(...)` (derived board fields are already `final`).
- A tower records its price from `TowerRoster.priceOf` in its constructor (each copy on the board
  costs more), so build it after charging that price and before `TowerRoster.add`.
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
- Each tower's own constraints are in its sub-package's `CLAUDE.md` (`td/tower/sonar/`, `splash/`,
  `seeker/`, `pulse/`, `mortar/`, `cinder/`, `aura/`): read it before editing that tower's leaf class here.
- A tower whose fork renames it overrides `displayName()`; the id and `TowerFactory.Type` stay.
- A missile's speed and size are a tower stat (`ProjectileStats`), shown as `TowerStatRow.PROJECTILE_SPEED`
  (cells a second, so it holds at any board scale) and `PROJECTILE_SIZE`, and drawn from the same record.
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
  `beginTick`, and folds in as negative attack speed and range in cells, times `DISRUPTION_TAKEN`. Blinding fields
  (`GameWorld.zones().disruptionAt`) add to it a miss chance: `AbstractTower.landHit` rolls it for every hit, and a miss
  reads as a dodge.
- A kill this tower makes runs `onKill(EnemyMob)` (the mob still carries the effects it died
  under); a kill made from inside the hook triggers nothing.
- A temporary or next-shot self-buff is a `TowerStatus` granted with `grantStatus` (another tower's
  with `receiveStatus`): the same `TowerTerm` restarts unless it keeps a running copy, different terms stack. A named buff, debuff or
  mechanic is a `TowerTerm` (title, side, sentence from effect mentions).
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

- Slots `CORE` and `BASE` (always `StandardBaseSlot.nodes`: Attune -> Awaken -> Transcendent, and the
  Range line), `HEAD` (two chains, plus an extra node that excludes nothing) and `SPECIAL` (one
  set of up to three, or one set per chain). Core nodes keep their `base.` ids, which saves store. Content per tower comes from
  `docs/features/FEATURE-tower-specialization-abilities.md` and the tower-rework feature requests.
- Build a node at its step: `UpgradeTier.HEAD_2.node(id, name, PRICE)` sets its slot, its price
  (a multiple of the list price), its XP (the gate table) and the Core node that opens it;
  `after(previous)` adds its line. Then `withModifiers`/`withGate`. `requires`
  decides whether a node is offered. Once offered it needs its `xp` and its `gate`, a purpose or
  layout condition (`always()`, `ClusterCondition`, `TranscendentCondition`) whose `progress()`
  feeds the UI. `UpgradeNode.gateMet` is the one check for both: never test `gate()` alone.
- Exclusivity is an `ExclusiveChoice` on the tree (`oneOf` for chain roots and IV-A | IV-B,
  `specials` for a special set), never `slotEmpty` in `requires`: the tree enforces it and the
  panel draws it. Pass `StandardBaseSlot.nodes` a tree's level III heads; without any, the tree
  has no Transcendent and no Range III.
- A node's number key comes from its place in the tree (`UpgradeTree.keyOf`): 1 the Core chain, 2 Range,
  3 the extra line, 4 and 5 a choice's two sides, 6-8 a special set. A tree that leaves a node
  without a key (a three-way head choice, four specials opened alike) is rejected.
- Never hand-write a node's bonus into a tower's description: `UpgradeNode.bonuses()` derives it
  and the Upgrades panel shows every node's.
- A mechanic says what it adds (`describe()`), built from its own constants; a node carries no note but the
  Core chain's structural text. A node's hover lists `Tower.bonusesOf(node)`: the node's data rows,
  then its mechanics' words. A number a mechanic's words quote lives in the mechanic, or in the delegate or effect
  rule it describes, never in the tower class.
- `buyUpgrade` (one node) and `buyPath` (a node with every node it needs, one charge) are the only ways in,
  and `downgrade` the only way out; all three are check-and-act (they return `false` without effect).
  Don't pre-check affordability. It publishes `TowerStats` before `UpgradeState`.
- A shipped node's id never changes, even when its slot or name does: saved games store the ids a tower owns
  (`TowerSave.nodes`). The Core chain keeps its `base.*` ids.
- Pass `StandardBaseSlot.nodes` the tower's base range (Range nodes add a share of it, in cells) and a
  `BaseSlotMechanics` for what Attune, Awaken and Range III add to a tower.
- A node is data: `withModifiers` (its own stats, flat, through `TowerModifiers`), `withEmission`
  (what an aura gives). A number a node changes is a
  `TowerStat` when the tower has it from the start or it belongs to the Attune mechanic; any other
  number belongs to the mechanic a node adds, in that mechanic's spec, set by its mechanic. Never add a
  field a node mutates. `TowerStatCatalogueTest` rejects a node that changes a stat its tower
  doesn't set.
- An upgrade that applies a generic effect, grants a status, changes a hit conditionally, picks a target
  or flips a capability is data on its node: `withProc`, `withHitBonus`, `withTargeting` (the first
  owned node that sets one decides), `withCapability`. Every attacking tower applies
  `hitAdjustment(...)` to each hit it computes and reports each landed attack with `report(...)` after
  its mechanics settled it; hit bonuses add. A hit aimed at one enemy is `asAimed()` and lands through `landHit`;
  a dodge is reported with `reportDodge(...)` instead, never settled by mechanics that answer a landed hit.
- Never check `owns(...)` in tower code except for the Core chain; Checkstyle rejects it.
- A node that adds a mechanic brings a `*Mechanic` class, in the tower's own package. A static `MechanicCatalogue`
  says which mechanics each node brings, and the tower's `OwnedMechanics` builds its own. Mechanics run in
  purchase order through `refineSpec` (and the tower's per-hit hooks); they are pure (no tower, no
  `GameWorld`).
- Mechanics widen or narrow whom a tower may hit through its `Reach`. A dead zone goes on the `Reach`
  (`withDeadZone`), which applies it last, so it holds whatever order the mechanics were bought in;
  never `and` one onto the query.
- The UI reads the tree's queries and `upgrades()`, and changes nodes only through `buyUpgrade`, `buyPath`
  and `downgrade`; the tower is the source of truth.

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
6. `targetingLabel()` and `baseApplies()`, and, for the shop only, a `description()` saying what no row does.
7. `README.md`'s tower table. The toolbar icon reuses the board paint code, so it needs no art.
8. A `CLAUDE.md` in the tower's sub-package for its own constraints, and its name in this file's pointer.
