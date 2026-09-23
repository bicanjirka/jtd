# Feature Request: Effect Diminishing Returns

**Status: proposed, not yet built.** Nothing described below exists in the codebase today —
`td.effect` currently applies a flat magnitude for an effect's whole duration and removes it
outright the instant its tick counter hits zero (see Current state). This formalizes the
hand-written idea in `TODO.md`'s "add diminishing return to slow and burn effects" note; once
this document is planned and implemented, that note should be removed from `TODO.md` in the
same commit, per this project's own convention for closing a gap.

## Summary

Make a slowed enemy recover speed gradually instead of snapping back to full speed the instant
its slow expires, and make a burning enemy's damage taper off over its burn instead of ticking a
flat amount every tick. Both replace an on/off step function with a continuous curve, and both
change how reapplying the same kind of effect while one is already active combines with it.

## Current state (what exists today)

- `Effect` (`src/main/java/td/effect/Effect.java:34`) is one record shared by all six
  `EffectKind`s. A slow/freeze carries a `speedMultiplier`; a burn carries a `damagePerTick`
  (`Damage`). Neither carries anything describing *how* that value should change over the
  effect's life — only `remainingTicks`, a single counter that only ever counts down
  (`Effect.withRemainingTicks`, `Effect.java:86`).
- `ActiveEffects` (`src/main/java/td/effect/ActiveEffects.java`) keeps **at most one `Effect`
  per `EffectKind` per mob**, in an `EnumMap` (`ActiveEffects.java:22`). There is no concept of
  multiple simultaneous same-kind effects.
- Reapplying a kind that is already active goes through `strongerOf`
  (`ActiveEffects.java:24-27`): `magnitude(...)` picks the higher-magnitude of the two
  (`1 - speedMultiplier` for `SLOW`/`FREEZE`, `damagePerTick().amount()` for `BURN`,
  `ActiveEffects.java:33-42`) and keeps *that* effect, but the **stored duration becomes
  `max(existing.remainingTicks, new.remainingTicks)` regardless of which one won on
  magnitude** (`ActiveEffects.java:26`). This applies identically whether the reapplication
  comes from the same source or a different one — there is no source tracking anywhere in this
  package.
- `ActiveEffects.tick()` (`ActiveEffects.java:109-124`) applies one tick of damage-over-time
  through `Effect.sink()` at the effect's flat `damagePerTick`, decrements `remainingTicks` by
  one, and removes the entry outright once it hits zero. There is no decay function, no ramp,
  no partial-strength tail anywhere in the codebase today — an effect is at its full authored
  strength for every tick of its duration, then gone.
- `FREEZE` is its own `EffectKind` (`EffectKind.java:9`), not a parametrized slow —
  `Effect.freeze` (`Effect.java:42-44`) hardcodes `speedMultiplier = 0f`. Its doc comment
  (`Effect.java:19-23`) already frames it as "deliberately one speed-to-zero effect rather than
  a separate stun mechanic." This document leaves `FREEZE` untouched: full stop while active,
  instant full release on expiry, exactly as today.
- Two towers author these effects today, unchanged in shape by anything below:
  `MortarTower.java:126` (`Effect.slow(this.slowMultiplier, this.slowDurationTicks, ...)`) and
  `CinderTower.java:90` (`Effect.burn(Damage.magic(this.damageCurrent()), BURN_DURATION_TICKS,
  ...)`, with `BURN_DURATION_TICKS = 60` — 3 seconds at the project's fixed 20-tick/s rate,
  `CinderTower.java:41`). `SeekerTower.java:109` applies `Effect.freeze`, out of scope here.
- `td/effect/CLAUDE.md` documents `ActiveEffects` as exposing "only queries, never its internal
  map" and states the package's one-effect-per-kind model as a deliberate simplification, not
  yet as a constraint this feature needs to revisit.

## What this feature adds

### Slow: a quadratic ease-in recovery curve

Within a slow's duration, speed recovers along a curve instead of staying flat until instant
expiry. Let `x = elapsedTicks / totalDurationTicks`, clamped to `[0, 1]`, and let `ratio` be the
slow's authored magnitude (`0..1`, i.e. `1 - speedMultiplier` today):

```
factor = x * x
currentSpeedMultiplier = minSpeedMultiplier + (maxSpeedMultiplier - minSpeedMultiplier) * factor
minSpeedMultiplier = 1 - ratio      (fully slowed at x = 0)
maxSpeedMultiplier = 1              (fully recovered, exactly, at x = 1)
```

This needs a new **total duration** tracked alongside `remainingTicks` — today's `Effect`/
`ActiveEffects` only track the countdown, which is enough to know *when* an effect expires but
not *how far through* it currently is; `x` needs both.

Worked example, `ratio = 0.6`, `totalDurationTicks = 100`:

| `x` | `factor = x²` | `currentSpeedMultiplier` |
|-----|---------------|---------------------------|
| 0.00 | 0.0000 | 0.400 |
| 0.25 | 0.0625 | 0.4375 |
| 0.50 | 0.2500 | 0.550 |
| 0.75 | 0.5625 | 0.7375 |
| 1.00 | 1.0000 | 1.000 |

The recovery is barely noticeable over the first quarter (0.400 → 0.4375) and steepest over the
last quarter (0.7375 → 1.000) — the "little at the start, faster at the end" shape the
hand-written `TODO.md` note asked for.

### Slow stacking: baseline unchanged, one edge case resolved

The existing stacking rule is **not changing**: the higher-magnitude application wins, and its
duration extends to the longer of the two. What's unresolved today is this specific scenario: a
weak-but-long slow is already active (`ratio = 0.2`, `200` ticks remaining) when a
strong-but-short slow lands on top (`ratio = 0.8`, `20` ticks). Today's rule picks the strong
magnitude but stretches its duration to `200` ticks — the strong slow ends up lasting *ten
times* longer than it was authored for, which reads as a bug, not a feature.

**Proposed resolution** (not finalized — flagged for review during planning, see Constraints
and open risks): don't collapse a reapplication to a single scalar effect. Instead, retain, per
kind, the currently-winning (highest-magnitude) application **and** a small bounded set of
superseded applications that still have remaining ticks (e.g. top 2, so this can't grow
unbounded from many towers hitting the same mob). Each superseded application keeps running its
own clock in the background while suppressed. When the current winner's own authored duration
elapses, if a superseded application still has ticks left, it becomes active again — resuming
its own recovery curve from whatever elapsed/total point it has reached by then, not restarting
from `x = 0` — rather than the slow vanishing outright or the strong application's duration
having been artificially stretched.

Worked example under this proposal, using the scenario above: the weak slow (`ratio = 0.2`,
`total = 200`) is applied at tick 0. At tick 0 the strong slow (`ratio = 0.8`, `total = 20`)
lands on top and becomes the active winner; the weak slow is retained as superseded, its clock
still running. At tick 20, the strong slow's own 20-tick duration completes (`x = 1`, fully
recovered) and is removed. The weak slow resumes as the winner with `elapsed = 20`,
`x = 20 / 200 = 0.1`, `factor = 0.01`, `minSpeedMultiplier = 0.8`:
`currentSpeedMultiplier = 0.8 + 0.2 * 0.01 = 0.802` — it picks up almost exactly where its own
curve would have been had it never been superseded, and keeps recovering to full speed at its
own tick 200, not tick 220.

This is the **single biggest structural change this feature needs to `ActiveEffects`**: today
it holds one `Effect` per `EffectKind`; this proposes a small bounded stack per kind for `SLOW`
specifically. See Constraints and open risks.

### Burn: an additive, decaying fuel-pool model

Replace burn's flat magnitude/duration entirely with a decaying fuel pool `L(t)`:

- Each tick, damage dealt equals the current fuel level `L` (in the simplest faithful reading —
  `Effect.burn`'s existing magnitude is already expressed as `damagePerTick().amount()`), then
  `L` decays: `L ← L * α`.
- **Per-tick decay multiplier**: `α = e^(-3 / T)`, where `T` is the burn's *authored* duration
  in ticks. This reaches `e^-3 ≈ 4.98%` of the original fuel right around when the authored
  duration would have elapsed under the old flat model — a burn "runs out" at roughly the same
  point a player would expect from its stated duration, even though the curve never reaches
  exactly zero.
- **Reapplying adds fuel, with diminishing returns based on how full the pool already is**:
  `ΔL = L0 * (1 - Lcurrent / Lmax)`, where `L0` is the new application's own authored peak
  intensity (its `damagePerTick` value) and `Lmax` is a cap. A pool near `Lmax` barely grows
  from another application; an empty or low pool grows close to the new application's full
  `L0`.

Worked example, single application, `L0 = 10`, `T = 60` (matching `CinderTower`'s current
`BURN_DURATION_TICKS`), so `α = e^(-3/60) = e^-0.05 ≈ 0.9512`:

| tick | `L` before tick | damage dealt (rounded) | `L` after decay |
|------|------------------|-------------------------|-------------------|
| 0 | 10.000 | 10 | 9.512 |
| 1 | 9.512 | 10 | 9.048 |
| 2 | 9.048 | 9 | 8.607 |
| ... | ... | ... | ... |
| 60 | — | — | `10 * e^-3 ≈ 0.498` (≈5% remaining) |

Stacking example: at tick 10 of the run above, `L ≈ 10 * 0.9512^10 ≈ 5.98`. A second
application lands, `L0' = 8`. Using an illustrative `Lmax = 20` (see Open questions):
`ΔL = 8 * (1 - 5.98 / 20) = 8 * 0.701 ≈ 5.61`, so `L` jumps to `5.98 + 5.61 ≈ 11.59` — a real
boost, but well short of simply adding two full applications together (`10 + 8 = 18`), because
the pool was already more than a quarter full.

This stacking rule — additive fuel with diminishing-returns refill — is **deliberately
different** from slow's max-magnitude/max-duration model. That asymmetry is intentional, not an
inconsistency to reconcile: a slow is a rate (how much an enemy's speed is reduced right now),
where "the stronger one wins" is the only sensible way to combine two, while a burn is
literally a fuel level, where "adding more fuel" is the natural combination and a flat magnitude
comparison would throw away real information (a second, smaller burn landing on an already
large one should still matter a little, not be discarded outright).

### Scope

Confirmed scoped entirely to `td.effect` — `Effect`, `EffectKind`, `ActiveEffects`, and
possibly `EffectTemplate` (`src/main/java/td/effect/EffectTemplate.java`) if it needs to carry
the new authored-duration/`L0`/`Lmax` shape once an ability authors a burn or slow the same way
a tower does today. Towers keep authoring `Effect.slow(...)`/`Effect.burn(...)` with the same
intensity-plus-duration shape they use today (`MortarTower.java:126`, `CinderTower.java:90`);
none of this needs any change in `td.tower`.

## Interconnections

- **`FEATURE-enemy-traits-and-effects.md`** designed `td.effect` as "a shared, neutral
  primitive" any tower or ability can produce or react to. This feature changes that
  primitive's internal behavior for two of its six kinds; anything reading `Effect`/
  `ActiveEffects` state directly rather than through their existing query methods (there is
  none today, per `td/effect/CLAUDE.md`'s "exposes only queries" rule) is unaffected by
  construction.
- **`FEATURE-critical-damage.md`'s burn addendum** reads `enemy.activeEffectKinds().contains
  (EffectKind.BURN)` to double crit chance against a burning target — a presence check, not a
  magnitude read. Nothing in this feature changes what "burning" means as a boolean, so that
  addendum is unaffected as long as `activeKinds()`/`activeEffectKinds()` still reports `BURN`
  as present for as long as the fuel pool hasn't been removed.
- **`FEATURE-effect-visuals.md`** governs the on-board marker row, which today renders each
  active `EffectKind` as one binary coloured dot (`EnemyFrameBuilder`, capped at three visible
  plus an overflow glyph). This feature introduces the first *continuously varying* effect
  state (fuel level, recovery progress) the marker row has ever had to represent; whether the
  marker should reflect that intensity, or stay binary, is a real open question — see Open
  questions.
- Directly formalizes `TODO.md`'s hand-written "add diminishing return to slow and burn
  effects" note, which also asked for freeze to stay a hard on/off effect (honored above) and
  for both curves to stack "not infinitely." The bounded-superseded-set proposal above and the
  fuel-pool cap (`Lmax`) are this feature's answer to that constraint.

## Constraints and open risks

- **`ActiveEffects`'s core invariant — at most one `Effect` per `EffectKind` per mob — is what
  this feature's slow-stacking proposal breaks**, for `SLOW` specifically (not for any other
  kind). That is a structural change to a class `td/effect/CLAUDE.md` currently documents as
  holding "at most one live `Effect` per `EffectKind`," and needs explicit review during
  planning rather than being treated as a routine addition — see Open questions.
- **A correlated set of new fields is needed per effect** (authored total duration for slow;
  fuel level, authored `L0`, and decay rate for burn) that don't exist on `Effect` today. Per
  the root `CLAUDE.md`'s value-type rules, a record widening this way grows through a narrow
  named factory or `withX` copy, not a wider positional constructor — a design constraint for
  planning, not a decision this document makes.
- **`ActiveEffects.tick()` runs on the `game-loop` thread, once per tick, per live mob**
  (root `CLAUDE.md`'s threading model). A curve evaluation (a multiply and a compare for slow;
  an `exp`/multiply for burn's decay) needs to stay cheap and allocation-light in that hot path,
  the same performance concern `TODO.md`'s "no performance budget" item already flags for this
  codebase generally.
- **Burn damage has to become an `int` again every tick.** `Damage`'s constructor clamps and
  stores an `int` amount (`Damage.java:23,36-38`); a continuously decaying `float` fuel level
  has to be rounded down to a `Damage` each tick the same way `scaledBy`/`asCritical` already
  round (`Math.round`, `Damage.java:78,95`). This is also what makes the fuel pool's true
  asymptotic "never reaches zero" behavior a real termination problem in practice — see Open
  questions.
- **No engine-enforced cap on effects stacking exists today** (`FEATURE-enemy-traits-and-
  effects.md`'s Decisions made: "balance is a content-authoring discipline, not an engine
  limit"). This feature is the first place a *bounded* cap (the superseded-slow-application
  limit, and burn's `Lmax`) is actually load-bearing for correctness, not just for balance —
  worth flagging since it's a small precedent shift for the package.

## Decisions made

- **The quadratic ease-in slow-recovery curve, as specified above, is final** — not open for a
  different curve shape.
- **The additive, diminishing-returns burn fuel-pool model, as specified above, is final** —
  not open for a different combination rule.
- **Freeze is explicitly out of scope and stays exactly as it is today**: a hard, binary
  speed-to-zero effect with instant release, never a parametrized slow.
- **Slow's baseline stacking rule (max magnitude, duration extends to the longer) is not
  changing.** Only the specific weak-long/strong-short edge case is being resolved, via the
  proposed bounded-superseded-set mechanism above — presented as a proposal to validate during
  planning, not as a settled data-model decision.
- **Burn and slow are allowed to combine differently from each other on purpose.** This
  asymmetry (additive fuel for burn, max-wins for slow) is a deliberate design choice, not an
  inconsistency a future pass should reconcile.
- **Scope is confined to `td.effect`.** No tower-authoring call site changes shape; a tower
  still hands an intensity and a duration to `Effect.slow`/`Effect.burn` exactly as it does
  today.

## Open questions

1. **Is the proposed bounded-superseded-application-stack resolution for slow's edge case the
   right one?** It's presented above as a concrete proposal, not a final decision — it's the
   single biggest structural change this feature needs to `ActiveEffects` (see Constraints and
   open risks) and needs explicit sign-off during planning before implementation starts. In
   particular: how many superseded applications should be retained (proposed: 2) and whether a
   superseded application's clock should keep counting down in the background while suppressed
   (assumed above, since "resuming from its own elapsed/total point" only makes sense if time
   passed for it too) or freeze until it becomes active again — the document above assumes the
   former but this isn't yet confirmed.
2. **`Lmax`'s exact derivation is open.** Proposed default: a fixed multiple (e.g. `2×`) of the
   strongest single `L0` ever applied to that mob so far, rather than one fixed absolute
   constant across every enemy/tower combination — but this is a tunable to decide during
   implementation, not a final number.
3. **The termination rule for a decaying-but-never-zero fuel pool is open.** Two candidate
   floors: end the effect once `L` drops below some small fraction of the strongest `L0` ever
   applied (e.g. 5%, matching the `α` derivation's own ~5%-at-authored-duration target), or once
   a tick's rounded damage (`Math.round(L)`) reaches `0`. Either is a reasonable default to
   propose; neither is finalized, and the two can disagree (a large enough `L0` could round to a
   nonzero `Damage` well past a 5%-of-peak floor). Needs a concrete number and a concrete rule
   during implementation.
4. **Which decay rate applies after a second, differently-authored-duration burn stacks onto an
   in-progress pool?** The formula above names a single `T` per burn, but once two applications
   with different authored durations have merged additively into one `L`, it's unspecified
   whether `α` should be recomputed from the newest application's `T`, kept from the original,
   or blended. Not resolved here; needs a decision during planning.
5. **Should the marker row (`FEATURE-effect-visuals.md`) represent slow/burn intensity now that
   both vary continuously, instead of the current binary presence dot?** Out of scope for this
   document's own delivery either way, but worth deciding explicitly rather than leaving the
   marker silently stale relative to what the effect is actually doing.
