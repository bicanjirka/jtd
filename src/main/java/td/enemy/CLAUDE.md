# `td.enemy` — the enemy mob hierarchy

Read the root `CLAUDE.md` first; this file only covers what is specific to this package.

## Shape

`EnemyMob` is the interface every consumer (towers, targeting queries, renderers) sees.
`AbstractEnemyMob` holds everything shared: position, movement along the path, health, death
and the fade animation's timing.

There are only two concrete `EnemyMob` implementations, deliberately unequal in kind:

- **`DefinedEnemyMob`** is the one real, data-driven enemy. Its behavior comes entirely from
  the `EnemyDefinition` it was built from — a name/id, base stats, a `BodyArchetype`/
  `MovementBehavior` pair for rendering, and composable `Trait`s/`Ability`s — never from a
  per-type Java override. `EnemyFactory.Enemy.create()` builds one of these for every
  wave-script letter except `e`; `BuiltInEnemies` holds the four built-in `EnemyDefinition`s
  (`CIRCLE`/`SQUARE`/`TRIANGLE`/`GHOST`) it uses.
- **`EnemyMobEmpty`** stays its own tiny, hand-written class — a wave-timing spacer that never
  ticks, is never a valid target, and is never drawn. It doesn't fit the trait/ability model
  because it isn't really an enemy at all; forcing it through would need a "never do anything,
  ever" trait for a use case of exactly one. See its own doc comment.

`EnemyMobVisitor` reflects this: it has exactly two methods, `visitDefined`/`visitEmpty`, not
one per enemy *type* — see its own doc comment for why that's still a real, compiler-enforced
safety net despite there being only one real concrete class to visit.

**The wave-script/level-authoring side of this is not built yet.** `EnemyCatalog`/
`EnemyDefinition`'s `id`-based lookup exists and is fully tested, but `WaveScript`/`Wave`/
`GameEngine.loadLevel` still go through `EnemyFactory.Enemy` exactly as before — a level
cannot yet register a custom or cloned enemy. That's `FEATURE-enemy-traits-and-effects.md`'s
next phase, not this one.

## Traits, abilities, and level-scaling

A `Trait` is a passive, always-on modifier: `onHit` (resistance, generalizing what used to be
`EnemyMobSquare`'s hardcoded `absorb` override), `speedFactor` (a hurt-speed curve,
generalizing what used to be `EnemyMobTriangle`'s), `isValidTarget` (see the gotcha below —
**not** what makes Ghost invisible). `PercentResistTrait`/`HurtSpeedTrait` are the two built-in
implementations, reused (not subclassed) by `BuiltInEnemies.SQUARE`/`TRIANGLE`.

**A `Trait` instance is shared across every mob built from the same `EnemyDefinition`,
regardless of which wave's `level` spawned it** — `TraitContext(level, healthFraction)` is
passed into every `Trait` method for exactly this reason. Don't be tempted to bake a level into
a `Trait` at construction time; `PercentResistTrait(0.8f, 0.05f)` means "the formula," not "the
formula at some fixed level."

An `Ability` pairs a closed `AbilityTrigger` (periodic, once-after-a-delay, health-threshold-
crossed, on-death, time-since-last-hit) with a closed `AbilityAction` (apply an effect, or
spawn more enemies) — see `AbilityEvaluator`'s own doc comment for how firing is decided, and
`EnemyCatalog.register`'s doc comment for the spawn-graph cycle check a `SpawnEnemiesAction`
chain has to pass. No built-in enemy has an ability yet — the Warden boss (a later phase) is
what actually exercises this.

## Invariants worth knowing before you change anything here

**`doInit` must be called from the constructor, and `super.doInit` first.** `DefinedEnemyMob`'s
own `doInit` override reads `this.definition` (set before the call) and applies
`healthDivisor` (generalizing Ghost's old flat `/5`) on the way *down* into `super.doInit`, so
the base class never sees the un-adjusted value.

**Ghost's invisibility does not go through `Trait.isValidTarget`.** Single-target towers filter
by `EnemyMob.type` (see `td.tower.targeting.OfTypeTargetQuery`/`InRangeTargetQuery.ofType`), a
separate, pre-existing mechanism `EnemyDefinition.mobType()` feeds directly —
`DefinedEnemyMob`'s constructor sets `this.type = definition.mobType()`. `Trait.isValidTarget`
is real API, just not what Ghost's migration needed; it stays available for a future trait that
makes a mob untargetable through some other means. If a future ability-applied temporary
invisibility effect (`td.effect.EffectKind.INVISIBLE`, already built) is ever wired to actually
hide a mob from targeting, it will need to feed the *same* `type`-based mechanism, not
`isValidTarget` — nothing does this yet, and no v1 content needs it to.

**Facing is never derived from a per-tick pixel delta.** Enemies move at sub-pixel speeds
(`1.28` px/tick is every v1 built-in's `baseSpeed`), so an `atan2` over one tick's movement
intermittently collapses to zero. A `PathDirectionalMovement` mob reads the path's exact
geometric facing (`getPathFacingRadians()`) instead — no v1 built-in uses this behavior yet,
but the mechanism is there. Don't "simplify" it back to a pixel-delta `atan2`.

**Movement is real arc-length distance, not a per-segment tick budget.** `doTick` advances a
`distanceIntoLap` accumulator by `speed` pixels and resolves it through the shared
`td.wave.ArcLengthPath`. That is why a curved or diagonal path moves enemies at the same
real-world pace as a straight one.

**A stationary mob needs no special "doesn't move" flag.** `EnemyDefinition.baseSpeed()` of
`0` already means `distanceIntoLap` never accumulates. No v1 built-in uses this yet — it's the
boss egg's mechanism (a later phase).

**Reaching the path's end is a wrap, not a despawn.** `distanceIntoLap` wraps back to zero, the
player is charged an `EconomyDelta.leak`, and `prevX`/`prevY` are resynced to the new position
— without that resync the renderer interpolates across the whole board for one frame and draws
a streak.

**`doDamage` returns the damage that actually landed, not what was passed in.** `absorb` (which
`DefinedEnemyMob` implements by folding every `Trait.onHit` in sequence) lets a mob resist part
of a hit; a mob that is not a valid target takes none of it at all; and the result is capped at
the health the mob had left, so a killing blow reports only what it actually removed rather
than its overkill. That cap is also why a dead mob sits at exactly zero health instead of going
negative. Anything reporting damage figures — `AbstractTower.dealDamage` is the only such
caller today — must use the return value. `DefinedEnemyMob.doDamage` propagates it after
recomputing `speed` from the traits' `speedFactor`.

**The health cap is applied to `absorb`'s result, via `Damage.cappedAt`, never by re-wrapping
the incoming hit.** A `Trait.onHit` is free to change a hit's `DamageType` as well as its
amount; capping from the original `damage` argument instead would silently discard whatever
type it chose.

**Death timing is captured in `doTick`, not lazily at paint time.** `doDamage` sets `dead`; the
next `doTick` records `deathTick`. The fade therefore advances with the simulation clock, so it
runs at the same rate while fast-forwarding as the rest of the game. `ticksSinceDeath` can
legitimately return `-1` for an already-dead mob whose `doTick` has not run yet — `fadeAlpha`
clamps for exactly this reason.

**A degenerate path is a supported state, not a bug.** A `GameWorld` before any level loads has
a path with fewer than two points; `ArcLengthPath.of` returns empty and the mob holds still at
the path's first point (or the origin). Keep that branch.

**Speed has an intrinsic/effective split, like `AbstractTower`'s base/current damage.** The
`speed` field stays the *intrinsic* value — `DefinedEnemyMob.doDamage` recomputes it fresh from
`definition.baseSpeed() * (product of every Trait.speedFactor)` on every hit, and `doInit`'s
spawn-delay calculation reads it before any damage lands — while `getSpeed()` and movement both
additionally fold in every currently active `td.effect.Effect`'s speed multiplier via
`ActiveEffects.speedMultiplier()`. A slow or freeze therefore never gets permanently baked into
`speed`, and is never wiped out the next time a trait recomputes it. `doTick` reads that
multiplier *before* calling `ActiveEffects.tick()`, not after — the tick call both applies this
tick's damage-over-time and decrements durations, and an effect entering the last tick of its
duration must still suppress this tick's movement, not just its damage. A damage-over-time tick
that kills the mob sets `dead` synchronously (its sink calls back into `doDamage`), so `doTick`
checks `dead` and returns before movement runs — there is nothing left to move.

**`EnemyMobEmpty` needs no special-casing for effects.** It overrides `doTick` with an empty
body and never calls `super`, but it also reports `validTarget() == false` always, and every
targeting query filters on that — nothing can ever apply an effect to it in the first place.

## Adding a new enemy

Two different things can mean "a new enemy," with very different cost:

**Reusing an existing `BodyArchetype`/`MovementBehavior`/`Trait` combination** (the common
case, and the entire point of this model) needs no new Java class at all: add a new
`EnemyDefinition` (today, to `BuiltInEnemies`; once the catalog phase lands, to a level's own
registration) composing what already exists, and a branch in `EnemyFactory.Enemy` to reach it
by a wave-script letter.

**Adding a genuinely new `BodyArchetype`** (a shape nothing existing uses) is still a fixed,
compiler-enforced checklist, same spirit as before:

1. Add the constant to `BodyArchetype`.
2. Add its case to `EnemyFrameBuilder.paletteFor` (a `Palette` role) — the compiler forces
   this, since `BodyArchetype`'s switch there has no `default`.
3. Add that `Palette` constant's shape (`Java2DFrameRenderer.enemyShape`) and colour
   (`colorFor`, and the fade switch in `paintEnemyFade`) — **not** compiler-enforced (those
   switches fall back to a runtime exception / silently skip rather than fail to compile), so
   do this in the same change as step 1-2, not "later."
4. Document the new letter in the root `CLAUDE.md`'s wave mini-language table and in
   `README.md`'s enemy table.

**Adding a genuinely new `Trait` or `Ability`** is ordinary Java: implement the interface (see
`PercentResistTrait`/`HurtSpeedTrait` for the shape), reuse it from any `EnemyDefinition`.

**Never branch on a mob's concrete type with `instanceof` or a `switch`.** Use
`EnemyMobVisitor`. `EnemyFrameBuilder`/`EnemyCatalog`/`AbilityEvaluator` switching on the
*sealed* `BodyArchetype`/`MovementBehavior`/`AbilityTrigger`/`AbilityAction` types is the
narrow, compiler-checked exception this project already carves out for sealed DTO hierarchies
(root `CLAUDE.md`) — not license to switch on `EnemyMob`'s own concrete type.

## Roster

`EnemyRoster` owns the live per-wave array and the alive count; `EnemyRegistry` is the
read-only slice of it that targeting queries and renderers depend on — depend on the narrow
interface, not the roster, unless you actually need to mutate.

`clear()` (level teardown) deliberately does **not** call `GameHost.enemyDied`, while
`remove()` (an actual kill) does. Renaming or merging those two would make returning to the
menu spuriously trigger the "you won" overlay.

**`EnemyRoster` cannot spawn yet.** It's still a bare array with no `add`. `EnemySpawner` is
the narrow interface `AbilityEvaluator` already depends on for this, tested against a fake —
`EnemyRoster` becomes its production implementation (and likely moves to a
`CopyOnWriteArrayList` backing store, matching `TowerRoster`/`ProjectileRoster`'s precedent for
the same game-loop-writes/EDT-reads shape) once something real needs to spawn, not before.
