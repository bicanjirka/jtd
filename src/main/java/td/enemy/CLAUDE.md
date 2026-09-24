# `td.enemy`

## Model

- `DefinedEnemyMob` is the only concrete `EnemyMob`. Everything about an enemy comes from its
  `EnemyDefinition` (stats, `BodyArchetype`, `MovementBehavior`, `Trait`s, `Ability`s), never
  from a Java override. Build one with `EnemyDefinition.of(...)` plus `withX` copies.
- A new enemy is a new definition, not a class: global content in `BuiltInEnemies` (registered in
  `EnemyCatalog.builtIn()`), one-level content in `LevelDefinition.customEnemies()` /
  `customRankedEnemies()`.
- `RankedEnemy` ladders are authored contiguously upward from `Rank.GRUNT`
  (`startingAt(...).thenAt(...)`); each step receives the previous rank's definition (typically
  `withHealthAndPrice`). A gap or a backward step is a `GameStartupException`. Asking for an
  unauthored rank falls back to the highest authored one. `BuiltInEnemies.SIMPLE` is the
  reference ladder.
- Single-rank enemy: `EnemyCatalog.register(EnemyDefinition)`. A variant of an existing one:
  `EnemyCatalog.cloneAndAdjust(baseId, newId, adjust)`, which clones every rank from the
  original's own definition. `adjust` is a `UnaryOperator` (rank-blind) or a
  `BiFunction<Rank, EnemyDefinition, ...>` (rank-aware). `register` also rejects spawn-ability
  cycles.
- Gameplay spawns through `EnemyCatalog`. `EnemyFactory` is only a test convenience over
  `EnemyCatalog.builtIn()`. `EnemyCatalog.ids()` keeps registration order (the debug spawn
  cycles through it).
- A consumer that only aims takes `EnemyTarget`; one that only hits takes `HitReceiver`.
- Never branch on a mob's concrete type; use `EnemyMobVisitor`. Switching on the sealed
  `BodyArchetype`/`MovementBehavior`/`AbilityTrigger`/`AbilityAction` types is fine, but prefer
  adding a query method (like `EffectTemplate.kind()`) over another switch on
  `AbilityAction`/`EffectTarget`.
- A mob is built by one constructor and its birth fields are `final`. `DefinedEnemyMob` composes
  its parts (`PathMotion`, `MobMoments`, `ActiveEffects`); don't reintroduce a base class.

## Traits and abilities

- A `Trait` instance is shared by every mob of a definition, so it holds no per-mob state; per-mob
  input arrives as `TraitContext`. `Trait.marker()` has no default, on purpose.
- `withAdditionalTraits`/`withAdditionalAbilities` replace an entry with the same
  `TraitId.named(...)` id and stack anonymous ones. Name an entry only when a later step replaces
  it (e.g. `"armor"`, which the `armored` spawn shape overrides).
- Built-in traits include `PercentResistTrait`/`FlatResistTrait` (per hit, scopable with
  `physicalOnly`/`magicOnly`), `HurtSpeedTrait`, and crit/burn/freeze immunities.
- Effect immunity is `Trait.blocksEffect`, checked in `DefinedEnemyMob.applyEffect` before
  `ActiveEffects` sees the effect.
- `AbilityEvaluator` decides when a trigger fires; actions execute through
  `MobAbilityContext` (one per caster per tick), which reaches the world through
  `GameWorld.enemies()` (`EnemySpawner.add`/`replace`); spawned mobs are built by
  `AbilitySpawnFactory`. Per-mob `AbilityState` (countdowns,
  fire-once flags) is built in the constructor, parallel to `definition.abilities()`.
- `ticksSinceLastHit` starts at `0`, so an idle trigger counts from spawn, not "forever".
  `WardenChainTest` covers the ability timing end to end.
- A frozen mob doesn't evaluate abilities on live ticks, but still does on
  its death tick.
- A tower can kill a mob *after* that mob's own `doTick` ran this tick. So death, crit-taken and
  damage-taken moments are marked pending in `MobMoments` and stamped on the next `doTick`, and death-tick abilities fire only when
  `ticksSinceDeath(gameTime) == 0`: exactly once, never during the fade. Until that capture,
  a dead mob's `ticksSinceDeath` is `-1`; callers must handle it (`EnemyFrameBuilder.fadeProgress` clamps).
- `AbilitySpawnFactory` builds an ability spawn at the caster's position, on the caster's
  path and at the caster's rank. It uses `AbilitySpawnShape` (not `td.wave.SpawnShape`);
  `delaySpacingSlots` staggers members so they don't stack. `consumesSelf` is only defined for
  one member. Spawns call `recordAbilitySpawn` and casts call `recordAbilityCast`; the UI draws
  rings from both.
- Invisibility is effect-driven: `effectiveType()` reports `Type.INVISIBLE` while
  `ActiveEffects.isInvisible()`. It does not go through `Trait.isValidTarget`.
- `EnemyDefinition.supportAura()` derives the aura ring the UI draws from radius-targeted
  abilities; the UI must not walk abilities itself.

## Movement and damage

- Movement (`PathMotion`) is arc-length distance along `td.wave.ArcLengthPath`. A `baseSpeed` of `0` means
  stationary. A degenerate path (fewer than 2 points) is valid: the mob holds still.
- Facing is never derived from a per-tick pixel delta (sub-pixel speeds make `atan2` collapse to
  zero); use the path's facing.
- The `speed` field is intrinsic: recomputed on every hit from `baseSpeed` × spawn multiplier ×
  every trait's `speedFactor`. Effect multipliers are applied on read, never baked in. `doTick`
  reads the effect multiplier *before* `ActiveEffects.tick()`, and returns early if a
  damage-over-time tick killed the mob.
- `doDamage` returns the damage that actually landed: traits' `absorb`, then
  `ActiveEffects.applyShield`, then `Damage.cappedAt` remaining health, which keeps a type a trait
  changed. Callers report that return value, not the input.
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
- `reportDeath()` calls `GameHost.enemyDied`; `clear()` and `replace()` (a hatch) do not, so
  going back to the menu can't trigger "you won" and a hatch earns no bounty.
- Dead mobs stay in the list for their fade: use `aliveCount()` for "is the wave over".
- `getEnemies()` returns a fresh snapshot (a `CopyOnWriteArrayList` behind it), so don't rely on
  reference identity.

## Adding a `BodyArchetype`

1. Add the constant.
2. Add its cases in `EnemyFrameBuilder.paletteFor` and `Java2DFrameRenderer.colorFor`; the
   compiler forces these.
3. Add its shape in `Java2DFrameRenderer.enemyShape` and its fade case in `paintEnemyFade`. The
   compiler does **not** force these (they have a throwing `default`).
4. Add its token to `td/wave/CLAUDE.md`'s table and to `README.md`.

A new `Trait` also needs a `TraitMarker` constant and its palette cases in
`EnemyFrameBuilder.traitMarkerPaletteFor`/`Java2DFrameRenderer.colorFor`.
