# `td.enemy` — the enemy mob hierarchy

Read the root `CLAUDE.md` first; this file only covers what is specific to this package.

## Shape

`EnemyMob` is the interface every consumer (towers, targeting queries, renderers) sees.
`AbstractEnemyMob` holds everything shared: position, movement along the path, health, death
and the fade animation's timing.

**`DefinedEnemyMob` is the only concrete `EnemyMob` implementation.** Its behavior comes
entirely from the `EnemyDefinition` it was built from — a name/id, base stats, a
`BodyArchetype`/`MovementBehavior` pair for rendering, and composable `Trait`s/`Ability`s —
never from a per-type Java override. `EnemyCatalog.spawn(id, ...)` builds one of these from
whichever definition is registered under `id`; `BuiltInEnemies` holds the four basic built-in
`EnemyDefinition`s (`SIMPLE`/`ARMORED`/`FRENZIED`/`GHOST`) plus the Warden boss's six-stage
chain (`WARDEN_1`/`WARDEN_EGG_1`/`WARDEN_2`/`WARDEN_EGG_2`/`WARDEN_3`/`WARDEN_EGG_3`), which
`EnemyCatalog.builtIn()` pre-registers under their wave-script ids. `e`, the wave
mini-language's spacer token, no longer spawns a mob of any kind — `td.wave.WaveScript`
recognizes it before ever consulting a catalog and it produces zero enemies (see
`td/wave/CLAUDE.md`).

`EnemyMobVisitor` reflects this: it has exactly one method, `visitDefined`, kept as a visitor
rather than collapsed into a plain call so a second non-data-driven mob can be added later
without reopening every call site — see its own doc comment.

**`EnemyFactory` still exists, but only as a global-catalog test convenience.** Its
`getEnemy(String, ...)`/`isEnemy(String)` are stable, unchanged signatures that delegate to a
freshly built `EnemyCatalog.builtIn()` - that's what keeps every enemy-behavior test that
predates this feature (`PercentResistTraitTest`, `AbstractEnemyMobTest`, and others) working
unchanged. Real gameplay spawning (`WaveScript`/`Wave`/`GameEngine.loadLevel`) goes through
`EnemyCatalog` directly, not this class, since it needs per-level catalog scoping
`EnemyFactory` doesn't offer. The old `EnemyFactory.Enemy` enum and `identifyEnemy` are gone -
nothing needs a closed enumeration of ids anymore.

**`EnemyCatalog.ids()` lists every registered id, in registration order** (it is backed by a
`LinkedHashMap`, not a `HashMap`, specifically so this order is stable) —
`GameEngine.debugSpawnNextCatalogEnemy()` is the one caller, cycling through it to let a
debug keybinding spawn every enemy type a level's catalog knows about, one press at a time.

**A level cannot yet register its own custom or cloned enemy.** `EnemyCatalog.builtIn()` is
the only catalog any level gets - `GameEngine.loadLevel` builds one fresh per level load and
every wave resolves its tokens against it, but `LevelDefinition` has no field yet for a
level's own registrations. The Warden's six-definition chain ships as *global* built-in
content specifically to avoid needing that field yet (see `docs/features/FEATURE-enemy-traits-and-effects.md`) -
the wave mini-language and `EnemyCatalog` are already fully able to resolve a custom id the
moment something registers one (see `WaveScriptTest`'s
`aPerLevelCustomIdResolvesTheSameWayABuiltInDoes`); no *level* does yet.

## Traits, abilities, and level-scaling

A `Trait` is a passive, always-on modifier: `onHit` (resistance, folded in sequence by
`DefinedEnemyMob.absorb`), `speedFactor` (a hurt-speed curve applied to `baseSpeed`),
`isValidTarget` (see the gotcha below — **not** what makes Ghost invisible). `PercentResistTrait`/`HurtSpeedTrait`/
`FlatResistTrait`/`CriticalImmunityTrait`
are the four built-in implementations, reused (not subclassed) by `BuiltInEnemies.ARMORED`/
`FRENZIED`/the Warden stages - `FlatResistTrait` is deliberately a *flat per-hit* reduction,
not a depleting shield pool, since a pool that's "used up" over one mob's lifetime needs
per-mob mutable trait state nothing else here has (see its own doc comment).
`CriticalImmunityTrait` strips a critical hit's bonus via `Damage.stripCritical()` rather than
reducing the amount by some fraction of its own, so it stays exact regardless of which tower's
roll produced the bonus - `ARMORED` carries it alongside its percent resistance.

**A `Trait` instance is shared across every mob built from the same `EnemyDefinition`,
regardless of which wave's `level` spawned it** — `TraitContext(level, healthFraction)` is
passed into every `Trait` method for exactly this reason. Don't be tempted to bake a level into
a `Trait` at construction time; `PercentResistTrait(0.8f, 0.05f)` means "the formula," not "the
formula at some fixed level."

An `Ability` pairs a closed `AbilityTrigger` (periodic, once-after-a-delay, health-threshold-
crossed, on-death, time-since-last-hit, on-critical-hit-taken) with a closed `AbilityAction`
(apply an effect, or spawn more enemies) — see `AbilityEvaluator`'s own doc comment for how
firing is decided, and `EnemyCatalog.register`'s doc comment for the spawn-graph cycle check a
`SpawnEnemiesAction` chain has to pass. The Warden boss is what actually exercises this - see
"Ability execution", below, for how a live `DefinedEnemyMob` drives it.
`OnCriticalHitTakenTrigger` is the one trigger that fires repeatably rather than once ever -
"survived another crit" is a recurring event, not a one-time transition the way death or a
health threshold is, so it needs no `AbilityState` bookkeeping: `AbilityContext.justTookCriticalHit()`
is itself already edge-triggered, captured the same deferred way `justDied()`/`deathTick` are -
see the death-timing note below, which `AbstractEnemyMob.criticalHitTick` reuses verbatim.

## Ability execution

`DefinedEnemyMob` evaluates its own `definition.abilities()` once per `doTick`, each against a
private `MobAbilityContext` (an inner class - it needs this mob's own `GameWorld`, position and
`level`, which `AbilityEvaluator` itself never sees). Three things are easy to get wrong here:

**A mob's own `AbilityState` list is built once, in its constructor, parallel to
`definition.abilities()`** - a `PeriodicTrigger`'s countdown, a `TimeSinceLastHitTrigger`'s
"waiting for a hit to re-arm" flag, and so on are all per- *mob* state, unlike the shared,
stateless `Trait`s above.

**`ticksSinceLastHit` starts at `0`, not "a very long time."** A fresh spawn hasn't been hit
yet, but that must not trivially satisfy a `TimeSinceLastHitTrigger`'s window on its very first
tick - the idle timer counts from spawn exactly like it counts from an actual hit. Getting this
wrong (an earlier pass through this feature did) makes every such ability fire immediately on
spawn instead of after real idle time - `WardenChainTest` exists specifically to catch this
class of bug end-to-end, not just each trigger kind in isolation.

**Ability evaluation must fire at most once on a mob's death tick, never during its fade.**
`doTick` checks `ticksSinceDeath(gameTime) == 0` - *not* a "was this mob already dead" flag -
to decide whether this is the exact tick `deathTick` was captured, because a tower can kill a
mob during the tower phase of a game tick, *after* that mob's own `doTick` already ran for that
tick (see the root `CLAUDE.md` §3 (Threading) on tick ordering) - so "`dead` just became
true" and "`deathTick` was just captured" are not necessarily the same tick, and only the
latter must gate an `OnDeathTrigger` firing exactly once. Getting this wrong lets a dead mob's *other* abilities (a
`OnceTrigger`, say) keep evaluating throughout its fade window and
possibly fire late - which is exactly how the boss egg could wrongly hatch after being
legitimately killed, if evaluation ran on every fade tick instead of stopping after the one
death-transition tick.

**An ability-driven spawn appears where the spawning mob was, not at the path's start.**
`AbstractEnemyMob`'s constructor always starts a fresh mob at `distanceIntoLap` `0`; a
`SpawnEnemiesAction`'s execution calls the new mob's own `spawnAtSamePositionAs`/
`jumpToDistance` afterward to relocate it - without that, the Warden's egg would visibly
teleport to the path's start instead of appearing where the Warden died.

**`SpawnEnemiesAction`'s `count`/`consumesSelf` combination is only proven for
`count == 1`.** Every v1 use is `count == 1` (a single reinforcement, or the one egg/next-stage
Warden); `consumesSelf == true` with `count > 1` has no defined meaning (this mob can only be
replaced by one thing) and isn't validated against.

## Invariants worth knowing before you change anything here

**A mob is built by one constructor, and every field it is born with is `final`.** There is no
`doInit`: the two-phase form left a mob mutable and reachable half-built, and made the ordering
a prose rule a leaf had to remember. `DefinedEnemyMob` applies `healthDivisor` (generalizing
Ghost's old flat `/5`) inside its `super(...)` call, so the base class never sees the
un-adjusted value, and computes `bodyScale` after it returns. The compiler enforces all of it —
the same move `td.tower.AbstractTower` already made.

**Mutable state here is `private`, and a leaf reaches it through an accessor.** `AbstractEnemyMob`
once exposed fourteen `protected` mutable fields, which meant no invariant it declared could
survive a subclass. `setSpeed` is the one mutator a leaf needs (`DefinedEnemyMob.doDamage`
recomputes intrinsic speed from its traits); the rest is read through the existing getters.

**Ghost's invisibility does not go through `Trait.isValidTarget`.** Single-target towers filter
by `EnemyMob.Type` (see `td.tower.targeting.OfTypeTargetQuery`/`InRangeTargetQuery.ofType`), a
separate, pre-existing mechanism `EnemyDefinition.mobType()` feeds directly —
`DefinedEnemyMob`'s constructor sets `this.type = definition.mobType()`. `Trait.isValidTarget`
is real API, just not what Ghost's migration needed; it stays available for a future trait that
makes a mob untargetable through some other means. If a future ability-applied temporary
invisibility effect (`td.effect.EffectKind.INVISIBLE`, already built) is ever wired to actually
hide a mob from targeting, it will need to feed the *same* `type`-based mechanism, not
`isValidTarget` — nothing does this yet, and no v1 content needs it to.

**Facing is never derived from a per-tick pixel delta.** Enemies move at sub-pixel speeds (`1.28` px/tick is every v1
built-in's `baseSpeed`), so an `atan2` over one tick's movement
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

**Reaching the path's end is a despawn, not a wrap.** `AbstractEnemyMob.leak()` charges the
player an `EconomyDelta.leak`, then kills the mob through the exact same `dead`/fade/
`EnemyRoster.reportDeath()` path a combat kill uses, just with a leak penalty instead of
`EconomyDelta.kill` — no bounty, and no second lap where a tower could still kill it for one.
A leaked mob's `x`/`y` are left exactly where they were the tick before, so the fade has
something real to draw from instead of a teleport to the path's start.

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

**A spawn shape's permanent damage-taken reduction (Elite's 50%) is folded into `absorb`
*after* every `Trait.onHit`, via `Damage.scaledBy`, so it composes with a definition's own
resistance rather than overriding it.** It is a `SpawnParameters` field
(`damageTakenMultiplier`), not a `Trait` and not a `td.effect.Effect` — see `SpawnShape`'s own
doc comment for why a spawn-shape-wide, permanent reduction needs a mechanism neither of those
two is shaped for. `DefinedEnemyMob.activeEffectKinds()` still adds `EffectKind.SHIELD` to the
mob's status markers whenever this multiplier is active, purely so the player sees the same
shield glyph a real timed shield effect would show — the one place this package deliberately
blurs a cosmetic marker and a real `Effect`, and only for that marker; `absorb` above is the
actual mechanism.

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
`definition.baseSpeed() * shapeSpeedMultiplier * (product of every Trait.speedFactor)` on every
hit through `setSpeed`, folding in the spawning `SpawnShape`'s own speed multiplier (a boss's
50%) alongside the traits' — a one-time `setSpeed` at construction would be silently wiped by
the first hit, the same trap a trait's own multiplier would fall into if it weren't folded into
this recomputation — while `getSpeed()` and movement both additionally fold in every currently
active `td.effect.Effect`'s speed multiplier via `ActiveEffects.speedMultiplier()`. A slow or
freeze therefore never gets permanently baked into `speed`, and is never wiped out the next time
a trait recomputes it. `doTick` reads that multiplier *before* calling `ActiveEffects.tick()`,
not after — the tick call both applies this tick's damage-over-time and decrements durations,
and an effect entering the last tick of its duration must still suppress this tick's movement,
not just its damage. A damage-over-time tick that kills the mob sets `dead` synchronously (its
sink calls back into `doDamage`), so `doTick` checks `dead` and returns before movement runs —
there is nothing left to move.

**Spawn delay is computed before a mob exists, not inside its constructor.** `SpawnParameters`
(built by `td.wave.Wave`, or `SpawnParameters.atSlot` for the identity case) converts a slot
position into a tick countdown; `AbstractEnemyMob`'s constructor just takes that countdown and
starts inactive whenever it is greater than zero — keyed off the *converted* ticks, not the raw
slot position that produced them, since a fractional per-member delay (`SpawnShape`'s column/drip
spacing) can round down to zero ticks from a nonzero position, and `doTick`'s inactive branch
only ever counts down from a positive `delay`.

**A formation offset is a fixed world-space vector, computed once, never recomputed from the
path's current tangent.** `spawnParameters.localOffset()` is relative to the mob's own
spawn-facing direction (`x` forward, `y` lateral); the constructor rotates it by
`arcLengthPath.poseAt(0).facingRadians()` - the spawn point's own facing, since every mob's
`distanceIntoLap` starts at `0` regardless of delay or shape - exactly once, and stores the
result as the final `offsetX`/`offsetY` fields. `AbstractEnemyMob.updatePosition()` is still the
only writer of x/y, and a shaped spawn (`SpawnShape`'s Swarm/Line/Flank) does not move
differently from a normal one - it just adds those two fixed numbers to whatever centreline
position `poseAt` reports this tick, through `clampOntoBoard`. **This was not always
true and the bug it replaced is worth knowing about**: an earlier version measured the offset
from the path's *current* facing on every tick, which made a shaped member's position pivot
around the centreline as the path curved (distorting its own effective speed through a turn) and
jump outright at an unrounded corner, where the facing itself is discontinuous. A fixed vector
has neither problem - translating a curve by a constant doesn't change its arc-length speed, and
there is no discontinuity in a constant to inherit. `clampOntoBoard` clamps the offset onto the
board **only while the centreline position is itself already on the board** - that is still what
stops a formation near an interior edge from silently walking off the `validTarget` bounds check
while visibly on its way to the exit, without it an off-path member would leak a life without
ever being targetable. But at a level's authored off-board spawn/despawn buffer (`x = -1`, or one
past the far edge - see `td.wave.Point`'s doc comment), the centreline position is *itself*
off-board, and `clampOntoBoard` leaves it alone there: clamping it too would have every enemy pop
into view already sitting at the edge instead of visibly walking in from, and out to, off-screen.
A degenerate (empty) path has no tangent to rotate the offset by in the first
place, so it defaults to `0` there and the offset applies unrotated - not a special case, just
what the same formula gives you.

## Adding a new enemy

Two different things can mean "a new enemy," with very different cost:

**Reusing an existing `BodyArchetype`/`MovementBehavior`/`Trait` combination** (the common
case, and the entire point of this model) needs no new Java class at all: add a new
`EnemyDefinition` and register it under its wave-script id - today that means adding it to
`BuiltInEnemies` and `EnemyCatalog.builtIn()`, since no level can register its own yet (see
above); once that lands, a level-scoped registration is exactly as valid. The Warden's six
stages are exactly this: no new Java class, just six `EnemyDefinition`s composing
`FlatResistTrait` and the `Ability`s in `BuiltInEnemies.wardenAbilities`.

**Adding a genuinely new `BodyArchetype`** (a shape nothing existing uses) is still a fixed,
compiler-enforced checklist, same spirit as before:

1. Add the constant to `BodyArchetype`.
2. Add its case to `EnemyFrameBuilder.paletteFor` (a `Palette` role) — the compiler forces
   this, since `BodyArchetype`'s switch there has no `default`.
3. Add that `Palette` constant's shape (`Java2DFrameRenderer.enemyShape`) and colour (`colorFor`, and the fade switch in
   `paintEnemyFade`) — **not** compiler-enforced (those
   switches fall back to a runtime exception / silently skip rather than fail to compile), so
   do this in the same change as step 1-2, not "later."
4. Document the new letter in the root `CLAUDE.md` §9 and in
   `README.md`'s enemy table.

**Adding a genuinely new `Trait` or `Ability`** is ordinary Java: implement the interface (see
`PercentResistTrait`/`HurtSpeedTrait` for the shape), reuse it from any `EnemyDefinition`.

**Never branch on a mob's concrete type with `instanceof` or a `switch`.** Use
`EnemyMobVisitor`. `EnemyFrameBuilder`/`EnemyCatalog`/`AbilityEvaluator` switching on the *sealed* `BodyArchetype`/
`MovementBehavior`/`AbilityTrigger`/`AbilityAction` types is the
narrow, compiler-checked exception this project already carves out for sealed DTO hierarchies (root `CLAUDE.md`) — not
license to switch on `EnemyMob`'s own concrete type.

## Roster

`EnemyRoster` owns the live per-wave array and the alive count; `EnemyRegistry` is the
read-only slice of it that targeting queries and renderers depend on — depend on the narrow
interface, not the roster, unless you actually need to mutate.

`clear()` (level teardown) deliberately does **not** call `GameHost.enemyDied`, while
`reportDeath()` (an actual kill, or a leak - see "Reaching the path's end", above) does. Merging
the two would make returning to the menu spuriously trigger the "you won" overlay.

**`reportDeath()` does not remove anything from the live list** - a dead mob stays in it for its
death fade, so `getEnemies().length` is the wave's *slot* count, not how many are still alive.
`aliveCount()` is the real "is anything left" query, backed by the same count `reportDeath()`
decrements; `BalanceHarness`'s run loop is what actually needed this distinction; use it instead
of reaching for the list's length whenever the question is "has the wave finished," not
"what's currently drawable."

**`EnemyRoster` implements `EnemySpawner`** (`add`/`replace`), backed by a
`CopyOnWriteArrayList` rather than the bare array it used to be - `add`/`replace` are called
from an ability's execution on the `game-loop` thread (a reinforcement, an egg hatch) while
`getEnemies()` is read from the EDT for rendering, the same shape `TowerRoster`/
`ProjectileRoster` already have. `getEnemies()` therefore returns a fresh array snapshot every
call, not the same reference `setEnemies` was handed - don't rely on reference identity.
`replace(outgoing, incoming)` does **not** call `GameHost.enemyDied` for `outgoing`, matching
`clear()`'s precedent: a hatch is a transformation, not a kill, so it earns no bounty/score/
kill-count credit. `GameWorld.enemies().add`/`replace` are the seam a live mob's own ability
execution actually calls - see "Ability execution", above.
