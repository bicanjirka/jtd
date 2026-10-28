# `td.enemy`

## Model

- `DefinedEnemyMob` is the only concrete `EnemyMob`. Everything about an enemy comes from its
  `EnemyDefinition` (stats, `BodyArchetype`, `MovementBehavior`, `Trait`s, `Ability`s), never
  from a Java override. Build one with `EnemyDefinition.of(...)` plus `withX` copies.
- A new enemy is a new definition, not a mob class: global content is one `XxxEnemy` class holding
  everything it needs (its ladder, its spawnlings, its eggs), with a `registerIn(EnemyCatalog)` that
  `EnemyCatalog.builtIn()` calls in id order; one-level content goes in `LevelDefinition.customEnemies()` /
  `customRankedEnemies()`. Write values straight into the factory calls; a private helper method (`MenderEnemy.heal`)
  takes the place of a constant that several steps share. An enemy a boss spawns is a
  definition of its own, never the boss's ladder, which would be a spawn cycle.
- A ladder is authored contiguously upward from `Rank.GRUNT`; a gap or a backward step is a
  `GameStartupException`, and asking for an unauthored rank falls back to the highest authored one.
  A unique enemy (`EnemyCatalog.registerUnique`) has no ladder: it is Boss rank only, whatever is asked
  for, and so is the rank its mobs carry (`RankedEnemy.spawnRank`). A spawn ability that must not
  inherit a Boss caster's rank fixes one (`SpawnEnemiesAction.atRank`).
  A built-in ladder derives its health and bounty from a `RankGrowth` (`startingAt(grunt, growth)`)
  for the ranks it skips up to `BOSS`; a rank step's `withHealthAndPrice` overrides both for that
  rank alone. A unique enemy that leaks ends the level (`EnemyDefinition.ALL_LIVES`). Every
  enemy gains something of its identity from Elite on.
- Stat, trait, ability, effect, each thing is exactly one:
  - A **base stat** is a number every enemy has on its sheet. It is authored with its named method
    (`withArmor`, `withMagicResist`, `withPlating`, `withInsulation`, `withResilience`,
    `withAgility`, `withSpirit`; plating and insulation in points), and a rank step replaces it. A step that authors
    most of them uses `withStats`, which sets all seven, so it repeats the inherited ones. Never a trait.
  - A **trait** is passive identity an enemy has or hasn't: an effect resistance, regeneration, a stat
    that follows its state or adapts to the level. A trait that only adds a constant to a stat is a
    base stat instead.
  - An **ability** is a trigger and an action: it happens at a moment.
  - An **effect** is a timed status put on an enemy by a tower or an ability.
- Single-rank enemy: `EnemyCatalog.register(EnemyDefinition)`. A variant of an existing one:
  `EnemyCatalog.cloneAndAdjust(baseId, newId, adjust)`, which clones every rank from the
  original's own definition. `adjust` is a `UnaryOperator` (rank-blind) or a
  `BiFunction<Rank, EnemyDefinition, ...>` (rank-aware). `register` also rejects spawn-ability
  cycles.
- Every mob spawns through `EnemyCatalog`. `EnemyCatalog.ids()` keeps registration order (the
  debug spawn cycles through it).
- A consumer that only aims takes `EnemyTarget`; one that only hits takes `HitReceiver`.
- Never branch on a mob's concrete type; use `EnemyMobVisitor`. Switching on the sealed
  `BodyArchetype`/`MovementBehavior`/`AbilityTrigger`/`AbilityAction` types is fine, but prefer
  adding a query method (like `EffectTemplate.kind()`) over another switch on
  `AbilityAction`/`EffectTarget`.
- A mob is built by one constructor and its birth fields are `final`. `DefinedEnemyMob` composes
  its parts (`PathMotion`, `MobMoments`, `MobHealth`, `ActiveEffects`); don't reintroduce a base class.

## Traits and abilities

- A `Trait` instance is shared by every mob of a definition, so it holds no per-mob state; per-mob
  input arrives as `TraitContext`. `Trait.marker()` has no default, on purpose.
- A trait changes a mob only through `modifiers(context)` (a `td.stat` bundle), never through a
  new hook on the hit or speed path. Event-shaped behaviour is an ability.
- A trait slot holds a `TraitTemplate`: a plain `Trait` resolves to itself, an adaptive one
  (`AdaptiveResist`) reads `GameWorld.damageTally()`. The mob resolves every slot once, in its
  constructor (`EnemyDefinition.traitsFor`), and keeps that list. Spawn-time variation is a new
  template, never a new `EnemyDefinition` component.
- `withAdditionalTraits`/`withAdditionalAbilities` replace an entry with the same
  `TraitId.named(...)` id and stack anonymous ones. Name an entry only when a later step replaces
  it (e.g. `"armor"`, which a later rank step replaces).
- Every enemy diminishes hard-CC durations; there is no opt-in.
- `DefinedEnemyMob.applyEffect` shortens the effect by its resistance and freeze diminishing
  returns before `ActiveEffects` sees it; there is no separate immunity check. The one exception
  is `INVERSION`: it turns a heal into damage and a shield into one hit, credited to its caster,
  before anything else.
- An ability that leaves ground behind it is a `LeaveZoneAction` (`td.zone` does the rest); never a new mob or a
  hand-rolled tick.
- `AbilityEvaluator` decides when a trigger fires; actions execute through
  `MobAbilityContext` (one per caster per tick), which reaches the world through
  `GameWorld.enemies()` (`EnemySpawner.add`/`replace`); spawned mobs are built by
  `AbilitySpawnFactory`. Per-mob `AbilityState` (countdowns,
  fire-once flags) is built in the constructor, parallel to `definition.abilities()`.
- `ticksSinceLastHit` starts at `0`, so an idle trigger counts from spawn, not "forever".
  `DefinedEnemyMobAbilityTest` covers the ability timing end to end.
- A mob that `isSilenced()` (frozen, Dazed or `SILENCED`) doesn't evaluate abilities on live
  ticks, but still does on its death tick; its motion and the aura it carries run regardless.
- A tower can kill a mob *after* that mob's own `doTick` ran this tick. So death, crit-taken and
  damage-taken moments are marked pending in `MobMoments` and stamped on the next `doTick`, and death-tick abilities fire only when
  `ticksSinceDeath(gameTime)` is 0: exactly once, never during the fade. Until that capture, a dead
  mob's `ticksSinceDeath` is empty, like any moment that hasn't happened.
- `AbilitySpawnFactory` builds an ability spawn at the caster's position, on the caster's
  path and at the caster's rank. It uses `AbilitySpawnShape` (not `td.wave.SpawnShape`);
  `delaySpacingSlots` staggers members so they don't stack. `consumesSelf` is only defined for
  one member. Spawns call `recordAbilitySpawn` and casts call `recordAbilityCast`; the UI draws
  rings from both.
- Invisibility is an `INVISIBLE` effect that raises `STEALTH`; `DefinedEnemyMob.isHidden()` is the
  only place that reads it. A reveal sets `STEALTH` to 0, which beats invisibility's 1.
- `EnemyDefinition.supportAura()` derives the aura ring the UI draws from radius-targeted
  abilities; the UI must not walk abilities itself.

## Movement and damage

- Movement (`PathMotion`) is arc-length distance along `td.wave.ArcLengthPath`. A `baseSpeed` of `0` means
  stationary. A degenerate path (fewer than 2 points) is valid: the mob holds still.
- Facing is never derived from a per-tick pixel delta (sub-pixel speeds make `atan2` collapse to
  zero); use the path's facing.
- Each mob owns a `StatSheet` fed by its traits and `ActiveEffects`. `MOVE_SPEED`'s base is
  `baseSpeed` × spawn multiplier. The sheet is invalidated on a hit, on applying an effect, and
  after a tick that ticked effects or changed health. `doTick` reads speed and regeneration
  *before* `ActiveEffects.tick()` (which removes an effect on its last tick), invalidates the sheet
  after it, runs `EffectTransitions.observe` after it but before returning early for a mob a
  damage-over-time tick killed, so an expiry is seen on time and a lethal tick still records its
  losses.
- `takeHit(damage, attacker)` lands a hit through `HitResolution`, the one hit formula, then the
  shield pool and the cap at health, and returns a `HitResult`; callers credit `credited()`, and
  `doDamage` is its `landed()`. A dodge (`HitResult.dodge()`) changes nothing else, not even the
  idle timer.
- A spawn shape's trait override is composed into the definition before the mob is built. There
  is no separate multiplier mechanism.
- Spawn delay: the mob starts inactive iff its *converted* tick delay is > 0.
- A formation offset is rotated once, by the spawn point's facing, and stored as final
  `offsetX`/`offsetY`. Never recompute it from the current tangent (members pivot and jump at
  corners). `clampOntoBoard` clamps only while the centreline is on the board, so enemies can
  still walk in from, and out to, off-screen.
- Reaching the path's end is a despawn through the normal death/fade path, charged as
  `EconomyDelta.leak`, with the position left where it was.

## Roster

- Depend on `EnemyRegistry` (read-only) unless you mutate.
- `reportDeath(walk)` decrements the alive count and tells every `WalkEndListener` the walk
  ended (towers earn XP from it); `replace()` (a hatch) does neither, so a hatch earns no bounty
  and does not shorten the wave.
- A mob takes its `entryOrdinal` from `recordEntry()` the moment it goes live (after its spawn
  delay), not when it is built. `walkedWithin` covers only the path walked since it appeared.
- Dead mobs stay in the list for their fade: use `aliveCount()` for "is the wave over".
- `getEnemies()` returns a fresh snapshot (a `CopyOnWriteArrayList` behind it), so don't rely on
  reference identity.

## Observation

- Only the dev panel starts observing an enemy (`EnemyObservation.observe`); what an observed enemy
  spawns or hatches into is observed too. A wave, a level or a save never does.
- A mob builds a `MobEvent` only after `observer.isOn()`: an unobserved mob must cost nothing. A
  change to its health, effects or hits that a trace should show reports through its `MobObserver`,
  and an effect change through `reportEffects`, which diffs (`EffectDiff`) rather than naming one.
- A cause is named by `EnemyObservation.nameOf`: the tower behind an origin, else what runs
  `during` (an enemy's `#id` while its abilities run, a dev panel `dev:<action>`), else `-`.
- `HitResolution.steps` reuses `resolve`'s step functions with the crit already decided; never a
  second formula or a second roll.

## Adding a `BodyArchetype`

1. Add the constant.
2. Add its cases in `EnemyFrameBuilder.paletteFor` and `Java2DFrameRenderer.colorFor`; the
   compiler forces these.
3. Add its shape in `Java2DFrameRenderer.enemyShape` and its fade case in `paintEnemyFade`. The
   compiler does **not** force these (they have a throwing `default`).
4. Add its id to `README.md`'s "Wave script ids" table and its row to the enemy table.

A new `Trait` also needs a `TraitMarker` constant and its palette cases in
`EnemyFrameBuilder.traitMarkerPaletteFor`/`Java2DFrameRenderer.colorFor`, and an `entry()`: the term its icon shows and
its sentence (an adaptive template describes the most it can reach). A new ability names its `AbilityTerm`.

## Display

- The UI shows an enemy only through `EnemyInspection` (`DefinedEnemyMob.inspect()`), an
  immutable snapshot taken on the thread that owns the mob, never by reading live stats.
- `EnemyInspection.of` reads the mob's parts (`liveEffects()`, package-private queries), so a new
  inspector field adds no public query to the mob.
- `EnemySelection` (world-owned) takes requests from any thread but resolves them only on the
  game-loop thread, once per frame build. A killed or leaked selection keeps its snapshot; one
  that left the roster still alive is dropped. A hatch marks the mob `HATCHED` first, so its
  snapshot stays like a kill's or a leak's.
- `StatRows.shows` decides which stats an inspection shows.
