# `td.effect` — the shared timed status-effect primitive

Read the root `CLAUDE.md` first; this file only covers what is specific to this package.

## Shape

`Effect` is the runtime value: six kinds (`SLOW`, `BURN`, `FREEZE`, `SHIELD`, `INVISIBLE`,
`HEAL`), one record, no per-kind subclassing - every field has a meaningful identity value for
every kind, so resolving a mob's active effects never branches on which kind it's holding.
`ActiveEffects` holds at most one live `Effect` per `EffectKind` on one mob, keyed by kind so a
reapplication is a refresh, not a stack - **with one exception: `SLOW` additionally keeps a
single superseded application in the background**, so a weaker top-up isn't discarded outright
when it loses to a stronger one already active - see "Slow's recovery curve and bounded stack"
below. Both `Effect` and `ActiveEffects` are deliberately unaware of `td.enemy` - `DamageSink` is
the one seam a producer (a tower, or an enemy's own ability) binds at the moment an effect is
created, which is what lets this package sit below `td.enemy` in the dependency graph rather than
needing to depend on it.

## Slow's recovery curve and bounded stack, and burn's fuel pool

**`SLOW` recovers speed along a quadratic ease-in curve instead of snapping to full speed at
expiry.** `Effect.authoredDurationTicks` (set equal to the authored duration at creation, never
decremented) and `remainingTicks` (which still counts down) together give `ActiveEffects` a
progress `x = (authoredDurationTicks - remainingTicks) / authoredDurationTicks` at any tick;
`ActiveEffects.slowCurrentMultiplier` computes `minSpeedMultiplier + ratio * x²` from it, where
`ratio = 1 - effect.speedMultiplier()`. `FREEZE` is untouched by this - it stays a hard,
non-gradated stop, read directly from its own stored `speedMultiplier` (always `0f`).

**A `SLOW` reapplication that loses to the current winner isn't discarded - it becomes (or
displaces) a single superseded slot**, tracked in `ActiveEffects.slowSuperseded`, outside the
ordinary `active` map. The superseded application's own clock keeps ticking down in the
background every `tick()` call regardless of what happens to the winner; if the winner's own
duration elapses, the superseded application is promoted into its place and resumes its curve
from wherever its own clock has actually reached, never restarted from `x = 0`. A new
application that beats the current winner bumps the old winner into the superseded slot,
dropping whatever was there before; one that loses to the winner is compared against the
existing superseded slot the same way, and only the stronger of the two survives there - see
`ActiveEffects.applySlow`.

**`BURN` is an additive, decaying fuel pool (`Effect.fuelLevel()`), not a flat per-tick amount,
and it is credited per contributor, not through one shared sink.** The pool is a list of
`BurnContribution(DamageSink sink, float amount)` - one per still-contributing application,
`Effect.burnFuel` - rather than a single scalar; `fuelLevel()` sums them for anything that only
cares about the pool's current total. Each tick deals `Math.round(fuelLevel())` total, but that
total is split across every contribution in proportion to its own current `amount`, using
largest-remainder apportionment (`ActiveEffects.apportionBurnDamage`) so the parts always sum to
exactly the rounded total - a single contributor reduces to today's whole-pool behaviour. Every
contribution is then decayed individually by `alpha = e^(-3 / authoredDurationTicks)` - the
currently-active application's own duration, kept fixed across any later top-up; decay is
multiplicative, so decaying each contribution separately by the same `alpha` leaves the pool's
total exactly where decaying it as one pooled number would have. A reapplication doesn't fold
into the existing contributions - it appends one more, `BurnContribution(incomingSink, deltaL)`,
where `deltaL = L0 * (1 - fuelLevel() / lmax)` (`L0` the new application's own intensity, `lmax`
`2 × Effect.peakBurnL0` - the strongest single application's own intensity this mob has ever
taken, which only ever grows) - a pool near the cap barely grows from another application, an
empty or low one grows close to the new application's full intensity. This is what lets a second
tower's own top-up be credited to *that* tower's `dealDamage` instead of vanishing into whichever
tower's application happened to be first - see `ActiveEffects.applyBurn`/`tickBurn`. The effect
self-terminates once a tick's rounded total would be zero, rather than on a duration countdown.

This is the one place a *bounded* cap (the single superseded `SLOW` slot, and burn's fuel-pool
cap) is load-bearing for correctness rather than only for balance - the rest of this package
still holds "balance is a content-authoring discipline, not an engine limit."

`EffectTemplate` (sealed: `ShieldTemplate`, `InvisibleTemplate`, `HealTemplate`) is the
*authored* counterpart - what an `Ability` carries as data, before a `DamageSink` is bound.
`SLOW`/`BURN`/`FREEZE` have no template today: nothing yet authors them on an enemy's own
ability, only a tower applies them directly via `Effect.slow`/`.burn`/`.freeze`. Add one the same
shape as `ShieldTemplate`/`HealTemplate` if an ability ever needs to.

**`ShieldTemplate` can be scoped to one `td.damage.DamageType`.** Its `restrictedTo` field (empty
by default, meaning "absorbs both") is set by its own `physicalOnly`/`magicOnly` factories, and
carried onto the `Effect` it builds via `Effect.withShieldRestrictedTo`; `ActiveEffects.applyShield`
passes a hit of the other type through unabsorbed. No built-in ability authors a typed shield yet -
see `TODO.md`'s "A typed shield has no concrete user yet".

**`ActiveEffects` exposes only queries, never its internal map.** `activeKinds()` (a snapshot,
for the UI marker row), `speedMultiplier()`, `isInvisible()`, `applyShield(Damage)`,
`healPerTick()` and `apply(Effect)`/`tick()` are the whole surface. Nothing outside this package
reaches into which `Effect` is stored under which kind.

`EffectTransitions` is a separate, per-mob holder answering a different question than
`ActiveEffects` does: not "which kinds are active right now" but "when did each kind last change."
`AbstractEnemyMob` owns one alongside its `ActiveEffects` and feeds it `activeKinds()`'s output
every tick (see "Ordering, once per tick" below) - this is what lets `td.ui.EnemyFrameBuilder`
render a gain/loss transition (the Ghost's cloak fade today) without either package tracking a
mob's effect history a second time.

## Healing is a query, not a sink

Every damaging kind (`BURN`) deals its damage *through* a `DamageSink` bound at creation time, so
a damage-over-time tick is credited to whichever tower applied it exactly like an instant hit is
- for `BURN` specifically, each of a mob's several `BurnContribution`s carries its own sink, so a
tick's damage is credited to every contributing tower in proportion to what it added, not only to
whichever tower's application happened to apply first (see "Slow's recovery curve and bounded
stack, and burn's fuel pool" above). `HEAL` deliberately does **not** work this way: `Damage`'s own compact
constructor clamps every amount at zero specifically so a "healing hit" can never exist, and a
heal credits nobody the way a damage-over-time tick credits its tower. `Effect.healPerTick`
is instead a plain `int` a mob reads directly via `ActiveEffects.healPerTick()` and applies to
its own health field itself - the effect-side counterpart to `applyShield(Damage)`, which is the
same shape: a query the mob calls, not a mutation this holder performs on the mob's behalf.
`Effect.heal(...)`'s constructor still takes a `DamageSink`, unused, purely so every factory on
this record has the same shape and `withRemainingTicks` needs no per-kind branching.

## Ordering, once per tick

**A heal must be read before `ActiveEffects.tick()` runs, not after** - the same reasoning
`AbstractEnemyMob.doTick` already applies to `speedMultiplier()`: an effect entering the last
tick of its duration must still act *this* tick, and `tick()` removes an expiring effect before
returning. Reading `healPerTick()` afterward would silently shorten a heal's last tick the same
way reading `speedMultiplier()` afterward would silently shorten a slow's.

**A heal is capped at the mob's own max health and skipped entirely once the mob is dead** -
`AbstractEnemyMob` enforces both: the cap by `Math.min(healthMax, health + healPerTick)`, and the
dead-skip for free, since the heal application only runs inside `doTick`'s live-mob branch,
which a dead mob never reaches (a dead mob takes the sibling branch that captures `deathTick`
instead). A heal must never resurrect a mob already fading.

**`EffectTransitions.observe` runs after `ActiveEffects.tick()`, but before `doTick`'s
dead-return check that follows it.** It records, per `EffectKind`, the tick a kind was last
gained or lost - the "entity records a tick, a frame builder derives a progress from it" idiom
`deathTick`/`criticalHitTick` already use, applied to *any* effect transition rather than one
fixed event. After `tick()` so an effect that just expired is seen lost on the tick it actually
expired, rather than one tick late; before the dead-return so a damage-over-time tick that kills
the mob this same tick still records whatever it lost (e.g. a `SHIELD` it was holding) instead of
silently skipping the observation forever. It is *not* placed beside the `criticalHitPending`/
`damageTakenPending` captures near the top of `doTick`, because those also run while a mob is
inactive or already dead - states in which `ActiveEffects` never ticks at all, so there would be
nothing there to diff.

## Adding a new effect kind

1. Add the constant to `EffectKind`.
2. Add its case to `Effect`'s static factory (a new named factory, following `heal`/`shield`'s
   shape) and to `ActiveEffects.magnitude` - the compiler forces the second, since that switch
   has no `default`.
3. If the kind is authorable by an ability rather than only applied directly by a tower, add an
   `EffectTemplate` implementation (its `kind()` names the `EffectKind` it produces).
4. Add its `Palette.STATUS_MARKER_*` role and the matching cases in
   `td.ui.EnemyFrameBuilder.markerPaletteFor` and `td.ui.Java2DFrameRenderer.colorFor` - both
   exhaustive with no `default`, so a new kind is a compile error until its marker art exists.
