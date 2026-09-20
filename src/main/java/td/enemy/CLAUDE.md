# `td.enemy` — the enemy mob hierarchy

Read the root `CLAUDE.md` first; this file only covers what is specific to this package.

## Shape

`EnemyMob` is the interface every consumer (towers, targeting queries, renderers) sees.
`AbstractEnemyMob` holds everything shared: position, movement along the path, health, death
and the fade animation's timing.

**`DefinedEnemyMob` is the only concrete `EnemyMob` implementation.** Its behavior comes
entirely from the `EnemyDefinition` it was built from — a name/id, base stats, a
`BodyArchetype`/`MovementBehavior` pair for rendering, and composable `Trait`s/`Ability`s —
never from a per-type Java override. `EnemyCatalog.spawn(id, ..., rank)` builds one of these
from whichever definition `id`'s `RankedEnemy` resolves `rank` to; `BuiltInEnemies` holds the
five basic built-in ladders (`SIMPLE`/`ARMORED`/`FRENZIED`/`GHOST`/`MENDER`, each a full `RankedEnemy`)
plus the Warden boss's six-stage chain (`WARDEN_1`/`WARDEN_EGG_1`/`WARDEN_2`/`WARDEN_EGG_2`/
`WARDEN_3`/`WARDEN_EGG_3`, six single-rank `EnemyDefinition`s), which `EnemyCatalog.builtIn()`
pre-registers under their wave-script ids. `e`, the wave mini-language's spacer token, no
longer spawns a mob of any kind — `td.wave.WaveScript` recognizes it before ever consulting a
catalog and it produces zero enemies (see `td/wave/CLAUDE.md`).

**`RankedEnemy` is one enemy id's rank ladder — a `Rank` to `EnemyDefinition` mapping, authored
progressively.** `RankedEnemy.startingAt(grunt)` defines `Rank.GRUNT` from scratch; each
`.thenAt(next, change)` step receives the *previous* rank's own resulting definition and returns
the next one, so a step that only changes health (say) keeps everything else - traits included -
unchanged from the rank before it (`EnemyDefinition.withHealthAndPrice` is the ordinary shape
such a step takes). Ranks are authored contiguously upward from `GRUNT`, one at a time; skipping,
repeating or going backward is a `GameStartupException` at registration time. Not every enemy
reaches `Rank.BOSS` - `BuiltInEnemies.SIMPLE` is this package's demonstration ladder, adding an
identified `"shield"` trait at `Rank.ELITE` and replacing it with a stronger one at `Rank.BOSS`,
following `docs/features/FEATURE-enemy-rank-system.md`'s own pseudocode.

**Asking a `RankedEnemy` for a rank it never authored is never an authoring error.**
`RankedEnemy.definitionFor(requested)`/`.effectiveRank(requested)` silently fall back to that
enemy's own highest authored rank instead - an enemy that only defines up through `Rank.VETERAN`
spawns at `Rank.VETERAN` for a wave slot asking for `Rank.BOSS`. `EnemyCatalog.register
(EnemyDefinition)` is the convenience for an enemy that only ever needs `Rank.GRUNT` - the
Warden chain wraps a definition this way, as a single-rank ladder where every requested rank
resolves to the same one; a per-level custom enemy that needs a real ladder registers a
`RankedEnemy` directly instead (see `td/level/CLAUDE.md`'s `customRankedEnemies`).

**`RankedEnemy.cloneAs(newId, adjust)` clones a whole ladder, not just one rank.** `adjust` runs
over every rank the original authored, so "a Square with double the usual resistance for this
one level" comes out the same shape as the original - one rank if that's all the original
defines, five if it defines five - rather than flattening a real ladder down to a single cloned
rank. It's overloaded on what `adjust` is: a plain `UnaryOperator<EnemyDefinition>` is rank-blind
(a uniform change, applied the same way to every rank - the result still differs per rank, since
each rank's own input already did), while a `BiFunction<Rank, EnemyDefinition, EnemyDefinition>`
sees each rank alongside its definition, so it can branch on rank - "give Veteran and every rank
above it a gold shield, leave Grunt and Soldier alone" is `(rank, d) -> rank.compareTo(Rank
.VETERAN) >= 0 ? d.withAdditionalTraits(...) : d`. Either way each rank is cloned from *that
ladder's own* definition for that rank, never from the previous rank's already-adjusted clone.
`EnemyCatalog.cloneAndAdjust(baseId, newId, adjust)` is the catalog-level entry point, overloaded
the same way: resolves `baseId`'s `RankedEnemy`, clones it, and registers the result under
`newId`.

**`EnemyDefinition.of(id, displayName, baseHealth, price, baseSpeed, archetype)`** is the
required shape every definition has - no description, `EnemyMob.Type.NORMAL`, a `FixedMovement`
pace, no health divisor, no traits, no abilities. A definition that needs any of those calls the
matching fluent `withDescription`/`withMobType`/`withMovement`/`withHealthDivisor`/`withTraits`/
`withAbilities` copy instead - see every constant in `BuiltInEnemies` for the pattern. This is
what let the Warden's six-stage chain (12 components each, several shared verbatim across
stages) read as "what's different about this stage" instead of a 12-argument positional literal
each; adding a 13th component later costs one new `withX` method, not an edit to every existing
definition.

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

**A level can register its own custom or cloned enemy.** `GameEngine.loadLevel` builds a
fresh `EnemyCatalog.builtIn()` per level load, then registers every entry of both
`LevelDefinition.customEnemies()` (single-rank) and `.customRankedEnemies()` (a real ladder)
into it before any wave's tokens are parsed - a wave-script token then names a custom id exactly
like it names a built-in one, with no separate syntax for the two (see `td/level/CLAUDE.md` and
`WaveScriptTest.aPerLevelCustomIdResolvesTheSameWayABuiltInDoes`). `BuiltInLevelCatalog`'s
Reaver is the ranked case: a full five-rank ladder authored the same way a built-in's is, just
registered per-level instead of globally. The Warden's six-definition chain still ships as
*global* built-in content rather than using either list - it needs to be available regardless of
which level reaches it, and predates both fields.

## Traits, abilities, and rank

A `Trait` is a passive, always-on modifier: `onHit` (resistance, folded in sequence by
`DefinedEnemyMob.absorb`), `speedFactor` (a hurt-speed curve applied to `baseSpeed`),
`isValidTarget` (see the gotcha below — **not** what makes Ghost invisible; Ghost's
invisibility is ability-driven, see "Invisibility" below), and `blocksEffect` (whether this
trait rejects an incoming `td.effect.EffectKind` outright before it is ever applied - see
"Effect immunity", below). `PercentResistTrait`/`HurtSpeedTrait`/`FlatResistTrait`/
`CriticalImmunityTrait`/`BurnImmunityTrait`/`FreezeImmunityTrait` are the six built-in
implementations, reused (not subclassed) by `BuiltInEnemies.ARMORED`/`FRENZIED`/the Warden
stages/eggs - `FlatResistTrait` is deliberately a *flat per-hit* reduction, not a depleting
shield pool, since a pool that's "used up" over one mob's lifetime needs per-mob mutable trait
state nothing else here has (see its own doc comment). `CriticalImmunityTrait` strips a
critical hit's bonus via `Damage.stripCritical()` rather than reducing the amount by some
fraction of its own, so it stays exact regardless of which tower's roll produced the bonus -
`ARMORED` carries it alongside its percent resistance.

**`PercentResistTrait`/`FlatResistTrait` can each be scoped to one `td.damage.DamageType`.**
Their `restrictedTo` field (empty by default, meaning "resists both") is set by their own
`physicalOnly`/`magicOnly` factories; `onHit` passes a hit of the other type through unchanged.
`td.wave.SpawnShape`'s `armored` shape is physical-only, so a magic-damage tower ignores it
entirely rather than being blunted the way a physical one is.

**Effect immunity blocks an incoming status effect outright, before `ActiveEffects` ever
sees it - it does not reduce an already-landed hit the way `onHit` does.**
`BurnImmunityTrait`/`FreezeImmunityTrait` are the two built-ins (`BuiltInEnemies.WARDEN_EGG_2`
carries both); `DefinedEnemyMob.applyEffect` overrides `AbstractEnemyMob.applyEffect` to consult
every trait's `blocksEffect(effect.kind())` first and silently drop the effect if any trait says
yes, only delegating to `super.applyEffect` otherwise. A damage-over-time kind blocked this way
never even reaches `ActiveEffects.tick()`'s sink call, unlike a `Trait.onHit` resistance, which
only ever reduces an instant hit's amount.

**A frozen mob cannot cast an ability.** `DefinedEnemyMob.isIncapacitated()` - currently just
"has an active `EffectKind.FREEZE`" - gates the live-tick call to `evaluateAbilities`, so every
trigger (periodic countdowns included) simply stops progressing for as long as the mob is
frozen, rather than only suppressing the resulting cast. It is *not* consulted on the
death-tick `evaluateAbilities` call: dying still spawns whatever an `OnDeathTrigger` carries
regardless of what the killing hit also applied. Named for the general condition rather than
the one effect that causes it today, so a future stun-like effect only needs adding to this one
check.

**Every `Trait` names its own on-board glyph via `marker()`.** Unlike `onHit`/`isValidTarget`/
`speedFactor`, this method is deliberately non-default: a new `Trait` implementation is a
compile error until it names a `TraitMarker`, the same discipline `EffectTemplate.kind()` gives
a new effect template. `td.ui.EnemyFrameBuilder` draws one hollow diamond per trait in a row
below the body - see `td/ui/CLAUDE.md`.

**A `Trait` instance is shared across every mob built from the same `EnemyDefinition`.**
`TraitContext(healthFraction)` is passed into every `Trait` method for whatever per-mob state a
formula needs (currently just health fraction) — it carries no `Rank`, because a stronger
resistance at a higher rank is authored as a different concrete `PercentResistTrait` instance,
not the same instance read at a different rank.

**A `Trait`/`Ability` carries its identity separately, through `IdentifiedTrait`/
`IdentifiedAbility`, not as a field on itself.** `EnemyDefinition.traitSlots()`/`abilitySlots()`
are the identity-carrying lists a definition actually stores; `traits()`/`abilities()` are the
plain, identity-free views every runtime consumer (damage resistance, speed curves, ability
evaluation) reads instead. `TraitId.named("shield")` lets a later composition step —
`EnemyDefinition.withAdditionalTraits`/`withAdditionalAbilities` — replace an earlier
same-identified entry in place instead of stacking a second one beside it; `TraitId.anonymous()`
(the default `withTraits`/`withAbilities` wrap) is a fresh, guaranteed-unique token per call,
deliberately not the trait/ability instance itself, since every built-in implementation here is a
structurally-equal record and two unrelated same-shaped instances would otherwise collide.
Composition is always scoped to one definition's own list, so two different enemies reusing the
same name (e.g. both calling a trait `"shield"`) never collide with each other.

An `Ability` pairs a closed `AbilityTrigger` (periodic, once-after-a-delay, health-threshold-
crossed, on-death, time-since-last-hit, on-critical-hit-taken, on-first-damage-taken) with a
closed `AbilityAction` (apply an effect, or spawn more enemies) — see `AbilityEvaluator`'s own
doc comment for how firing is decided, and `EnemyCatalog.register`'s doc comment for the
spawn-graph cycle check a `SpawnEnemiesAction` chain has to pass. The Warden boss and
`BuiltInEnemies.GHOST` are what actually exercise this - see "Ability execution", below, for how
a live `DefinedEnemyMob` drives it.
`OnCriticalHitTakenTrigger` is the one trigger that fires repeatably rather than once ever -
"survived another crit" is a recurring event, not a one-time transition the way death or a
health threshold is, so it needs no `AbilityState` bookkeeping: `AbilityContext.justTookCriticalHit()`
is itself already edge-triggered, captured the same deferred way `justDied()`/`deathTick` are -
see the death-timing note below, which `AbstractEnemyMob.criticalHitTick` reuses verbatim.
`OnFirstDamageTakenTrigger` is the fire-once counterpart of the same underlying signal -
`AbilityContext.justTookDamage()` is edge-triggered exactly like `justTookCriticalHit()` (same
deferred capture, via `AbstractEnemyMob.damageTakenTick`, of *any* landed hit rather than only a
critical one), but `AbilityEvaluator.fireOnFirstDamageTaken` gates it with an `AbilityState`
`fired` flag the same way `OnDeathTrigger` is gated, so it can never fire a second time.

## Ability execution

`DefinedEnemyMob` evaluates its own `definition.abilities()` once per `doTick`, each against a
private `MobAbilityContext` (an inner class - it needs this mob's own `GameWorld`, position and
`rank`, which `AbilityEvaluator` itself never sees). Three things are easy to get wrong here:

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

**An ability-driven spawn appears where the spawning mob was, not at the path's start, and on
the same path.** `AbstractEnemyMob`'s constructor always starts a fresh mob at `distanceIntoLap`
`0` on `spawnParameters.pathIndex()`; a `SpawnEnemiesAction`'s execution calls the new mob's own
`spawnAtSamePositionAs`/`jumpToDistance` afterward to relocate it, and passes
`DefinedEnemyMob.this.getPathIndex()` into `SpawnParameters.atSlot(...)` when building it -
without the first, the Warden's egg would visibly teleport to the path's start instead of
appearing where the Warden died; without the second, it would relocate onto path 0's geometry
regardless of which path the Warden was actually walking. It also passes
`DefinedEnemyMob.this.rank` into the new mob's own constructor - an ability-driven spawn (a
reinforcement, an egg) inherits its parent's rank, the same precedent path inheritance already
set.

**`SpawnEnemiesAction` carries an `AbilitySpawnShape`, not a bare count.** Member count,
per-member size/health/bounty multipliers and an optional trait override compose the same way
`td.wave.SpawnShape` does for a wave slot - but as its own type, not that one: `SpawnShape` also
carries a `SpawnSpread` and slot-relative delay spacing defined against a wave slot's position on
the path, and an ability spawn has no slot - it places every member at the caster's own live
position (`spawnAtSamePositionAs`), where a path-relative formation offset has nothing to rotate
against. `AbilitySpawnShape.delaySpacingSlots` staggers members in time instead: member `i`'s
slot position is `i * delaySpacingSlots`, converted to a tick delay through the same
`SpawnParameters.atSlot`/`.of` conversion a wave slot uses, so a member after the first starts
`isInactive()` (undrawn, untargetable) and only becomes live once its own delay elapses -
without this, every member of a multi-member spawn would appear stacked on the exact same pixel
on the same tick. The `shape.members()`/`consumesSelf` combination is only proven for
`shape.members() == 1`. Every v1 use with `consumesSelf == true` is exactly that (a single
reinforcement, or the one egg/next-stage Warden); `consumesSelf == true` with more than one
member has no defined meaning (this mob can only be replaced by one thing) and isn't validated
against.

**Two more moments get recorded for the UI, both set directly with no deferred-capture step -
`AbilityCast`/`ticksSinceAbilitySpawn`.** `MobAbilityContext.applyEffect` already has `gameTime`
in hand when a cast resolves, so it calls `AbstractEnemyMob.recordAbilityCast` synchronously,
unlike `deathTick`/`criticalHitTick`/`damageTakenTick`'s deferred capture - applying an effect
cannot land during another phase of the tick the way a hit can, so there is no cross-phase gap to
defer across. `SpawnEnemiesAction`'s execution calls the new mob's own `recordAbilitySpawn`
right after `spawnAtSamePositionAs` for the same reason - both are `td.ui.EnemyFrameBuilder`'s
inputs for a brief arrival/cast ring; see `AbilityCast`'s own doc comment for why a cast needs
recording on the caster at all.

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

**Invisibility does not go through `Trait.isValidTarget`, and no enemy authors it natively.**
Single-target towers filter by `EnemyMob.Type` (see
`td.tower.targeting.OfTypeTargetQuery`/`InRangeTargetQuery.ofType`); `EnemyDefinition.mobType()`
sets a mob's *authored* type (`DefinedEnemyMob`'s constructor sets `this.type` from it), but
`AbstractEnemyMob.effectiveType()` — the method `validTarget(Type)` actually reads — reports
`Type.INVISIBLE` instead, for *any* mob, whenever `ActiveEffects.isInvisible()` is true,
regardless of that mob's own authored type. Invisibility is therefore purely
ability/effect-driven: `BuiltInEnemies.GHOST` carries no `withMobType` at all (an ordinary
`Type.NORMAL` peon) and instead composes an `OnFirstDamageTakenTrigger` ability that applies
`td.effect.InvisibleTemplate` to itself the first time it's hit, plus (at `Rank.ELITE` and
`Rank.BOSS`) a `PeriodicTrigger` "shroud" ability that applies the same template with a
`RadiusTarget` — `RadiusTarget`'s own "every *other* valid-target enemy" semantics is what keeps
the shrouding mob itself excluded, no special-casing needed. Any enemy can be made invisible
this way; `Trait.isValidTarget` stays available for a future trait that makes a mob untargetable
through some other means entirely.

**`EnemyDefinition.supportAura()` derives what a definition projects onto allies from its
abilities, rather than a UI walking `abilities()` itself.** It returns the `EffectKind`/radius of
the largest `RadiusTarget`-targeted ability a definition carries, as an `Optional<SupportAura>`
(empty for a definition with no radius-targeted ability at all) - `td.ui.EnemyFrameBuilder` reads
this to draw a support-aura ring at the ability's real reach, so the Ghost Elite's shroud, the
Warden's call-to-arms and the Mender's heal all get the same visual with no per-enemy UI
special-casing. This is a third place that pattern-matches over the sealed `AbilityAction`/
`EffectTarget` pair, alongside `AbilityEvaluator` and `EnemyCatalog` (see `AbilityAction`'s own
doc comment) - not license to add a fourth casually; prefer teaching `EffectTemplate`/
`AbilityAction` a new query method the way `EffectTemplate.kind()` already exists for exactly
this purpose, over adding another switch.

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
of a hit; `AbstractEnemyMob.doDamage` then runs that result through its own
`ActiveEffects.applyShield` before capping, so a timed `EffectKind.SHIELD` (`ShieldTemplate`'s
percentage reduction) composes with a `Trait`'s permanent resistance rather than replacing it —
the two are independent mechanisms applied in sequence, same as `absorb`'s own trait chain. A
mob that is not a valid target takes none of it at all; and the result is capped at the health
the mob had left, so a killing blow reports only what it actually removed rather than its
overkill. That cap is also why a dead mob sits at exactly zero health instead of going negative.
Anything reporting damage figures — `AbstractTower.dealDamage` is the only such caller today —
must use the return value. `DefinedEnemyMob.doDamage` propagates it after recomputing `speed`
from the traits' `speedFactor`.

**The health cap is applied after `absorb` and `applyShield`, via `Damage.cappedAt`, never by
re-wrapping the incoming hit.** A `Trait.onHit` is free to change a hit's `DamageType` as well
as its amount; capping from the original `damage` argument instead would silently discard
whatever type it chose.

**A spawn shape modifies what a spawned enemy *is*, not a separate multiplier mechanism.**
`SpawnShape.traitOverride()` (only `armored` sets it) is composed onto the slot's
`EnemyDefinition` via `EnemyDefinition.withAdditionalTraits` before `td.wave.Wave` builds any
mob from it - by the time `DefinedEnemyMob`'s constructor runs, the attached trait is just
another entry in `definition.traits()`, folded into `absorb` like any other. There is no
separate "shape-wide damage multiplier" field or mechanism any more, and no cosmetic-marker
special case for it either - a spawn shape's trait shows up exactly the way a rank-authored one
does, because it *is* one.

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
hit through `setSpeed`, folding in `shapeSpeedMultiplier` alongside the traits' —
`spawnParameters.speedMultiplier()`, which by the time `td.wave.Wave` builds it is already the
*product* of the spawning `SpawnShape`'s own multiplier, that path's
`PathDefinition.speedMultiplier()`, and that round's `WaveDefinition.speedMultiplier()` — a
fast path or a called-out fast round needs no mechanism beyond this one composed number. A
one-time `setSpeed` at construction would be silently wiped by the first hit, the same trap any
of those three multipliers would fall into if they weren't folded into this recomputation —
while `getSpeed()` and movement both additionally fold in every currently active
`td.effect.Effect`'s speed multiplier via `ActiveEffects.speedMultiplier()`. A slow or freeze
therefore never gets permanently baked into `speed`, and is never wiped out the next time a
trait recomputes it. `doTick` reads that multiplier *before* calling `ActiveEffects.tick()`,
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
`EnemyDefinition` and register it - either directly (a single-rank ladder, via `EnemyCatalog
.register(EnemyDefinition)`) or as a full `RankedEnemy` ladder if it should scale with rank.
Content meant for every level goes in `BuiltInEnemies` and `EnemyCatalog.builtIn()` - the
Warden's six stages are exactly the single-rank case: no new Java class, just six
`EnemyDefinition`s composing `FlatResistTrait` and the `Ability`s in
`BuiltInEnemies.wardenAbilities`, registered single-rank since the fight is already staged
through its own six hand-authored definitions rather than through rank. `BuiltInEnemies.SIMPLE`
is the ladder case - see `RankedEnemy`'s own doc comment above. Content meant for one level
instead goes on that level's own `LevelDefinition.customEnemies()` (see `td/level/CLAUDE.md`) -
`BuiltInLevelCatalog`'s Twisted Hourglass is this case, registered single-rank like the Warden
chain.

**Adding a genuinely new `BodyArchetype`** (a shape nothing existing uses) is still a fixed,
compiler-enforced checklist, same spirit as before:

1. Add the constant to `BodyArchetype`.
2. Add its case to `EnemyFrameBuilder.paletteFor` (a `Palette` role) and to
   `Java2DFrameRenderer.colorFor` — the compiler forces both, since each switches on `Palette`
   with no `default`.
3. Add that `Palette` constant's shape (`Java2DFrameRenderer.enemyShape`) and its case in the
   fade switch inside `paintEnemyFade` — **not** compiler-enforced (both switches carry a
   `default -> throw new IllegalStateException(...)` instead), so do this in the same change as
   steps 1-2, not "later."
4. Document the new letter in `td/wave/CLAUDE.md`'s token table and in
   `README.md`'s enemy table — **not** the root `CLAUDE.md`, which explicitly disclaims listing
   ids (§10, "the list of ids lives in `td/wave/CLAUDE.md`, not here").

**Adding a genuinely new `Trait` or `Ability`** is ordinary Java: implement the interface (see
`PercentResistTrait`/`HurtSpeedTrait` for the shape), reuse it from any `EnemyDefinition`. A new
`Trait` also needs a `TraitMarker` constant and its glyph colour - `marker()` is deliberately
non-default, so the compiler forces both that and the matching cases in
`td.ui.EnemyFrameBuilder.traitMarkerPaletteFor`/`Java2DFrameRenderer.colorFor` (via
`td.ui.render.Palette`) before the new trait compiles at all.

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
