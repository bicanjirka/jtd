# `td.enemy`

## Model

- `DefinedEnemyMob` is the only concrete `EnemyMob`. Everything about an enemy comes from its
  `EnemyDefinition` (stats, `BodyArchetype`, `MovementBehavior`, `Trait`s, `Ability`s), never
  from a Java override. Build one with `EnemyDefinition.of(...)` plus `withX` copies.
- A new enemy is a new definition, not a class: global content in `BuiltInEnemies` (registered in
  `EnemyCatalog.builtIn()`), one-level content in `LevelDefinition.customEnemies()` /
  `customRankedEnemies()`.
- `RankedEnemy` ladders are authored contiguously upward from `Rank.GRUNT`
  (`startingAt(...).thenAt(...)`); each step receives the previous rank's definition. Asking for
  an unauthored rank falls back to the highest authored one. `cloneAs` clones every rank, each
  from the original ladder's own definition for that rank.
- Gameplay spawns through `EnemyCatalog`. `EnemyFactory` is only a test convenience over
  `EnemyCatalog.builtIn()`. `EnemyCatalog.ids()` keeps registration order (the debug spawn
  cycles through it).
- Never branch on a mob's concrete type; use `EnemyMobVisitor`. Switching on the sealed
  `BodyArchetype`/`MovementBehavior`/`AbilityTrigger`/`AbilityAction` types is fine, but prefer
  adding a query method (like `EffectTemplate.kind()`) over another switch on
  `AbilityAction`/`EffectTarget`.
- A mob is built by one constructor and its birth fields are `final`; base-class mutable state is
  `private` (`setSpeed` is the one mutator a leaf needs).

## Traits and abilities

- A `Trait` instance is shared by every mob of a definition, so it holds no per-mob state; per-mob
  input arrives as `TraitContext`. `Trait.marker()` has no default, on purpose.
- Name a trait/ability (`TraitId.named`) only when a later composition step replaces it by id;
  otherwise it stays anonymous.
- Effect immunity is `Trait.blocksEffect`, checked in `DefinedEnemyMob.applyEffect` before
  `ActiveEffects` sees the effect.
- Per-mob `AbilityState` is built in the constructor, parallel to `definition.abilities()`.
- `ticksSinceLastHit` starts at `0`, so an idle trigger counts from spawn, not "forever".
- A frozen mob (`isIncapacitated()`) doesn't evaluate abilities on live ticks, but still does on
  its death tick.
- A tower can kill a mob *after* that mob's own `doTick` ran this tick. So death, crit-taken and
  damage-taken moments are captured on the next `doTick`, and death-tick abilities fire only when
  `ticksSinceDeath(gameTime) == 0`: exactly once, never during the fade.
- An ability spawn appears at the caster's position (`spawnAtSamePositionAs`), on the caster's
  path and at the caster's rank. It uses `AbilitySpawnShape` (not `td.wave.SpawnShape`);
  `delaySpacingSlots` staggers members so they don't stack. `consumesSelf` is only defined for
  one member. Spawns call `recordAbilitySpawn` and casts call `recordAbilityCast`; the UI draws
  rings from both.
- Invisibility is effect-driven: `effectiveType()` reports `Type.INVISIBLE` while
  `ActiveEffects.isInvisible()`. It does not go through `Trait.isValidTarget`.
- `EnemyDefinition.supportAura()` derives the aura ring the UI draws from radius-targeted
  abilities; the UI must not walk abilities itself.

## Movement and damage

- Movement is arc-length distance along `td.wave.ArcLengthPath`. A `baseSpeed` of `0` means
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
