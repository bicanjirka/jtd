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
- A trait changes a mob only through `modifiers(context)` (a `td.stat` bundle), never through a
  new hook on the hit or speed path. Event-shaped behaviour is an ability.
- A trait slot holds a `TraitTemplate`: a plain `Trait` resolves to itself, an adaptive one
  (`AdaptiveResist`) reads `GameWorld.damageTally()`. The mob resolves every slot once, in its
  constructor (`EnemyDefinition.traitsFor`), and keeps that list. Spawn-time variation is a new
  template, never a new `EnemyDefinition` component.
- `withAdditionalTraits`/`withAdditionalAbilities` replace an entry with the same
  `TraitId.named(...)` id and stack anonymous ones. Name an entry only when a later step replaces
  it (e.g. `"armor"`, which a later rank step replaces).
- Built-in traits include `PercentResistTrait` (armor or magic resist, authored as the fraction
  kept), `FlatResistTrait` (plating), both scopable with `physicalOnly`/`magicOnly`,
  `HurtSpeedTrait`, `CriticalImmunityTrait` (full resilience), `EffectResistTrait` (`immuneTo`
  at 1). Every enemy diminishes hard-CC durations; there is no opt-in.
- `DefinedEnemyMob.applyEffect` shortens the effect by its resistance and freeze diminishing
  returns before `ActiveEffects` sees it; there is no separate immunity check. The one exception
  is `INVERSION`: it turns a heal into damage and a shield into one hit, credited to its caster,
  before anything else.
- `AbilityEvaluator` decides when a trigger fires; actions execute through
  `MobAbilityContext` (one per caster per tick), which reaches the world through
  `GameWorld.enemies()` (`EnemySpawner.add`/`replace`); spawned mobs are built by
  `AbilitySpawnFactory`. Per-mob `AbilityState` (countdowns,
  fire-once flags) is built in the constructor, parallel to `definition.abilities()`.
- `ticksSinceLastHit` starts at `0`, so an idle trigger counts from spawn, not "forever".
  `WardenChainTest` covers the ability timing end to end.
- A mob under an effect that `stopsEnemy()` (frozen or Dazed) doesn't evaluate abilities on live
  ticks, but still does on its death tick.
- A tower can kill a mob *after* that mob's own `doTick` ran this tick. So death, crit-taken and
  damage-taken moments are marked pending in `MobMoments` and stamped on the next `doTick`, and death-tick abilities fire only when
  `ticksSinceDeath(gameTime) == 0`: exactly once, never during the fade. Until that capture,
  a dead mob's `ticksSinceDeath` is `-1`; callers must handle it (`EnemyFrameBuilder.fadeProgress` clamps).
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
  *before* `ActiveEffects.tick()`, and returns early if a damage-over-time tick killed the mob.
- `doDamage(damage, attacker)` lands a hit through `HitResolution` (a crit roll for hits only, and
  none at resilience 100; a guaranteed crit skips the roll, otherwise it rolls against resilience
  and crit chance taken; penetration, mitigation, plating, damage taken, cap at health) and
  returns what landed. Callers report that return value, not the input.
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

## Adding a `BodyArchetype`

1. Add the constant.
2. Add its cases in `EnemyFrameBuilder.paletteFor` and `Java2DFrameRenderer.colorFor`; the
   compiler forces these.
3. Add its shape in `Java2DFrameRenderer.enemyShape` and its fade case in `paintEnemyFade`. The
   compiler does **not** force these (they have a throwing `default`).
4. Add its token to `td/wave/CLAUDE.md`'s table and to `README.md`.

A new `Trait` also needs a `TraitMarker` constant (naming the stats whose info-panel rows stand
in for it) and its palette cases in `EnemyFrameBuilder.traitMarkerPaletteFor`/
`Java2DFrameRenderer.colorFor`, and a `describe()` `TraitLine` short enough for one row (an
adaptive template describes the most it can reach).

## Display

- The UI shows an enemy only through `EnemyInspection` (`DefinedEnemyMob.inspect()`), an
  immutable snapshot taken on the thread that owns the mob, never by reading live stats.
- `EnemySelection` (world-owned) takes requests from any thread but resolves them only on the
  game-loop thread, once per frame build. A killed or leaked selection keeps its snapshot; one
  that left the roster alive (a hatch) is dropped.
