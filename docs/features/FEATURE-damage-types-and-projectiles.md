# Feature Request: Damage Types, Projectile Types and On-Hit Effects

**Status: implemented.** `Damage` carries `DamageType`; `td.effect` (`Effect`/`EffectKind`/
`ActiveEffects`/`DamageSink`) shipped as the shared primitive this document called for;
`td.projectile` (`AbstractProjectile`/`CannonballProjectile`/`MissileProjectile`/
`ProjectileRoster`) and the Mortar/Seeker/Cinder towers shipped as the v1 content. Acid and
damage-type resistance were deferred past v1 as decided below and are tracked in `TODO.md`
("Acid is not implemented...", "Damage-type resistance doesn't exist yet"), not lost.

**Priority: Phase 2 — after tower upgrades.** This feature exists in part to give phase 1 (in-place tower upgrades)
something real to grant — an upgrade path that applies slow or
burn needs those effects to exist first. It precedes enemy traits/effects (phase 3), which
in turn needs this feature's damage-type tag and burning effect for its magic-shield and
burning-immunity traits. See Product review notes, below, for scope-cutting recommendations
within this feature that haven't yet been confirmed as decisions.

## Summary

Widen how a tower delivers and types its damage. Today every tower's hit resolves
instantly, in the same tick it fires, as a single untyped `int` amount. This feature adds (a) **projectiles as real,
time-lived entities** — a cannonball that travels a straight line
to a fixed destination, and a missile that homes in on its target — alongside the existing
instant-hit and continuous-sweep towers; (b) a **cone targeting shape** (a flamethrower-like
wedge) as a new addition to the existing splash and area shapes; (c) a **damage type**
(starting with physical vs. magic) attached to every hit, so an enemy can resist one type
and not another; and (d) **on-hit effects** applied to the enemy after damage lands — slow,
freeze, stun, burning damage-over-time, acid damage-over-time, and critical (bonus,
chance-based) damage.

## Current state (what exists today)

*(This section is a snapshot from before this feature's own implementation and before the
later tower rename and the enemy-traits data-driven migration — `TowerOne`–`Four` are now
`SniperTower`/`SplashTower`/`SonarTower`/`PulseTower`, and `EnemyMobSquare`/`EnemyMobTriangle`
are now the `ARMORED`/`FRENZIED` `EnemyDefinition`s built through `Trait`. The "there is no
cone shape today" claim below also no longer holds — `CinderTower`'s cone delivery shipped as
this feature's own v1 content, described under What this feature adds/V1 Scope.)*

- **`Damage`** (`td.damage.Damage`) is a single-field record: an `int amount`, clamped at
  zero by its compact constructor, with `none()` as the identity and `plus`/`scaledBy` as
  its only combinators. It carries no notion of damage *type* at all.
- **No projectile ever exists as a simulation entity.** Every tower resolves its hit inside
  its own `doTick`, in the same tick it decides to fire:
    - `TowerOne` — instant single-target hit on whichever visible enemy is furthest along the
      path (`FurthestAlongPathSelector` over `InRangeTargetQuery`).
    - `TowerTwo` — instant hit on one random visible enemy, then an instant, distance-
      falloff splash (`1 - (d/spreadRadius)²`) around that point.
    - `TowerThree` — no cooldown at all; a beam *bearing* sweeps continuously (`SonarSweep`), and any enemy whose
      bearing the swept arc crosses that tick is hit —
      see `td/tower/CLAUDE.md` for why hits are decided against the arc swept since the last
      tick, not the beam's instantaneous angle.
    - `TowerFour` — instant hit on everything in range, every tick, including invisible
      enemies, as long as at least one non-ghost is present.
    - The `Beam`/`Splash`/`Pulse`/`Aura` draws in `td.ui.render` (`TowerEffectFrameBuilder`)
      are purely cosmetic depictions of an already-resolved hit — they have no simulation
      lifecycle, no independent position over multiple ticks, and no ability to miss, travel,
      or be dodged.
- **Targeting shapes that exist today**: a single furthest-along-path target (`TowerOne`), a
  point splash with radius falloff (`TowerTwo`), a full-circle continuous sweep (`TowerThree`), and a flat "everything
  in range" area (`TowerFour`). **There is no cone
  shape today** — worth flagging explicitly, since a cone is one of the shapes this request
  asks for as new, alongside splash and full-range AoE, which already exist in a form.
- **No damage-type resistance exists.** `EnemyMobSquare.absorb()` reduces *any* incoming
  `Damage` by a flat, level-scaled fraction, regardless of source — there's no way today for
  an enemy to resist, say, magic damage specifically while remaining fully vulnerable to
  physical.
- **No post-hit effect exists.** There is no slow, freeze, stun, or damage-over-time
  anywhere in the codebase. The closest existing thing is `EnemyMobTriangle`'s hardcoded
  speed-up-as-you're-hurt curve, which is a *reaction to taking damage*, not a *type of
  damage or a targeted status effect* — it's baked directly into that one leaf class and
  isn't reusable.
- Targeting itself (`td.tower.targeting`: `TargetQuery`, `InRangeTargetQuery`,
  `OfTypeTargetQuery`, `TargetSelector`) is already a clean, composable layer independent of
  a tower's *delivery mechanism* — it decides *who* is a legal target, not *how* the hit
  gets to them. That split is a genuine asset this feature can build on directly.

## What this feature adds

- **Projectile entities with travel time**:
    - A **cannonball-style** projectile: spawned at the tower's position, aimed at the
      target's position *at the moment of firing*, and travels in a straight line at a finite
      speed to that fixed destination — it does not re-aim in flight, so a fast-moving enemy
      can dodge it by the time it arrives.
    - A **missile-style** projectile: spawned the same way, but re-aims each tick toward its
      target's *current* live position (homing) until it reaches (or the target is no longer
      valid).
    - Both need a defined resolution: single-target hit on arrival, or an AoE/splash on
      arrival (reusing the existing splash-falloff shape).
- **A cone-shaped targeting/damage area** (flamethrower-like): a wedge extending from the
  tower in the direction of a target, hitting everything the wedge currently covers —
  conceptually closer to `TowerThree`'s continuous, cooldown-free delivery than to a
  discrete projectile, but shaped as a static or slowly-reorienting wedge rather than a
  360° sweep.
- **A damage type** on every hit, starting with physical and magic, structured so more types
  can be added later without another foundational rewrite.
- **On-hit effects**, applied to an enemy after a hit lands: slow (temporary speed
  multiplier below 1), freeze (temporary speed multiplier of 0, or a distinct "cannot move"
  state), stun (distinct from freeze in that it should presumably also suppress
  ability triggers from feature 1, once abilities exist), burning damage-over-time, acid
  damage-over-time (a second, presumably differently-themed DoT — worth clarifying how it
  differs from burning beyond flavor and color), and critical damage (a chance-based bonus-
  damage hit, itself something other systems may want to react to — see feature 1's
  critical-damage-immunity trait and post-crit-shield ability).

## Interconnections with the other two feature requests

- **Enemy traits/effects (feature 1) is the consumer of this feature's vocabulary.** Two
  traits explicitly requested there — a *magic* shield and *burning-effect immunity* —
  cannot be built until this feature's damage-type tag and burning-DoT effect exist. The
  reverse dependency also exists: a physical/magic *shield* trait and a *critical-immunity*
  trait need somewhere to hook into this feature's hit resolution (after damage type is
  known, before or after the on-hit effect is applied) — that hook needs to be designed
  jointly with feature 1, not bolted on afterward.
- **Decided: effects share one representation across features 1, 2 and 3** (see feature 1's
  document). A slow/burn/stun applied by a tower here is mechanically the same kind of thing
  as an enemy's own ability applying a speed boost to itself: a timed, visible,
  stacking-or-not modifier attached to a live `EnemyMob`. That shared primitive is to be
  designed once, before feature-specific work starts on any of the three requests, so this
  feature's on-hit effects and feature 1's ability effects land as producers of the same
  type rather than as two separate systems with duplicated duration/stacking logic and
  duplicated (and possibly inconsistent) UI markers.
- **Tower upgrades (feature 3) explicitly wants to grant existing towers new on-hit effects**
  (a tower upgraded to apply slow or burn) and new damage-related bonuses (increased damage,
  a damage-taken-increase aura). Feature 3 therefore depends on this feature's damage-type
  and effect vocabulary existing (at least in prototype form) before "an upgraded tower now
  burns its target" is buildable. It also implies that whatever new fields land on `Damage`
  or on a tower's fire method need to be *upgradeable per-instance*, not just fixed per
  tower type at construction.

## Architectural implications

- **`Damage` becoming typed is a foundational, wide-blast-radius change.** It is constructed
  at every `dealDamage` call site (all four current towers) and consumed by every
  `absorb` override and by `AbstractTower.dealDamage`'s damage/kill accounting. Whether it
  becomes a tagged record (`Damage(int amount, DamageType type)`) or a small sealed
  hierarchy (`PhysicalDamage`/`MagicDamage`) has real downstream consequences: a sealed
  hierarchy fits this codebase's existing preference for closed, exhaustive switches (see
  `td.ui.render`'s sealed `EnemyDraw`/`TowerEffectDraw`) but is more invasive to combine
  with `Damage.plus` (combining two different-typed damages into one value stops making
  obvious sense) than a tagged record with a `type` field would be.
- **Projectiles need a new first-class simulation entity and roster**, analogous to
  `EnemyRoster`/`TowerRoster`: something is spawned when a tower fires, needs its own
  `doTick`-style advance-then-resolve lifecycle, needs interpolated position for rendering (matching the
  `interpolationAlpha` pattern `EnemyFrameBuilder` and turret-aim already
  use), and needs to be cleaned up on level teardown the same careful way
  `TowerRoster.clear()`/`EnemyRoster.clear()` already are (see the root `CLAUDE.md`'s Levels
  section on `GameEngine.loadLevel`'s idempotency contract). This is a new `GameWorld`
  collaborator, not a small addition to an existing one.
- **A target dying or leaking mid-flight needs a defined rule.** A homing missile whose
  target dies before arrival, or a cannonball whose target has already moved off the aimed-
  at point, needs explicit behavior (fizzle harmlessly, retarget to the nearest enemy,
  detonate at the empty destination) — today's instant-hit model has no such case because
  nothing is ever "in flight."
- **Rendering needs new draw-command types.** `td.ui.render`'s `TowerEffectDraw` sealed
  hierarchy (`BeamDraw`/`SplashDraw`/`PulseDraw`/`AuraDraw`) currently models only *cosmetic, already-resolved* effects.
  A projectile with its own multi-tick position needs
  its own draw command (and its own frame-builder logic, likely a sibling of
  `TowerEffectFrameBuilder` rather than an addition to it, since a projectile is closer in
  shape to an enemy — a moving, interpolatable entity — than to a beam flash). A cone needs
  its own shape description too; `SonarSweep`'s bearing-in-arc math is the closest existing
  precedent but is built for a continuously rotating full sweep, not a static or slowly-
  reorienting wedge.
- **On-hit effects need a per-tick resolution step on the enemy side** — DoT ticking down
  health, slow/freeze/stun counting down duration — which lands in `AbstractEnemyMob.doTick`
  or a new hook it calls, on the `game-loop` thread, and needs to stay cheap per enemy per
  tick (see the Threading model in the root `CLAUDE.md`).

## Risks and costs

- **`Damage`'s shape change is the single highest-blast-radius change in this whole set of
  three requests** — every tower and every enemy resistance calculation touches it.
  Whatever shape is chosen should be settled first, since features 1 and 3 both build on it.
- **New failure modes need explicit rules**: a homing missile's target dying in flight, a
  cannonball missing because its target moved, two DoTs of the same kind stacking or
  refreshing, stun interacting with an ability precondition from feature 1 (does a stunned
  enemy's "speed up below 50% health" ability still fire?). None of these have an existing
  answer to extend from.
- **Balance**: falloff curves, DoT tick rates and total damage, freeze/stun duration versus
  enemy speed, and critical-hit chance/multiplier all need tuning, and all combine
  multiplicatively with feature 1's traits (resistances, immunities) once both exist.
- **Cone geometry is new work**, not a reuse of an existing shape — it needs a point-in-
  wedge test and needs to respect board edges the same way `EnemyMob.validTarget` already
  clips at `gameWorld.getBoard().maxX()/maxY()`.
- **Test surface**: projectiles in flight, homing re-targeting, and DoT/slow/freeze/stun
  timing all need to be provable headlessly and clock-free per this project's test
  conventions (`doTick(t)` with explicit tick numbers, no real-time dependence) — that's
  achievable, but it's a materially larger test surface than the current instant-hit towers
  need.

## Product review notes

A senior-product-owner pass over this request, with a genre lens, surfaced points worth
weighing before scoping a first version — not yet confirmed decisions, unlike the section
below:

- **"Critical damage" is miscategorized in the original request.** It's listed alongside
  slow/freeze/burn/acid as "an effect applied after the enemy is damaged," but mechanically
  it isn't one: slow/burn/acid are things that happen *to the enemy, after* a hit lands,
  with duration; a critical hit is a *pre-hit*, chance-based damage multiplier on the *attacker's* roll. Recommendation:
  model it as a tower stat (crit chance/multiplier),
  not as a post-hit effect alongside the DoTs and control effects.
- **Stun and freeze are mechanically identical today, and arguably should ship as one
  effect for now.** An enemy's only action in this game is moving along the path — there's
  no other action for a "stun" to interrupt that a "freeze" (speed = 0) doesn't already
  cover. The two names only earn separate mechanics once feature 1's abilities exist for
  stun to suppress that freeze wouldn't. Recommendation: ship one speed-to-zero effect now (call it either), and only
  split it into two once there's an ability for stun to
  meaningfully suppress.
- **Six on-hit effects at once is a lot for a first version.** Recommendation: cut to slow
    + burn only for the initial ship — the two most genre-standard, lowest-risk, and most
      build-diversifying (a "control" playstyle distinct from raw DPS). Freeze/stun/acid/crit
      can follow once the shared effect plumbing and UI have proven out on two effects.
- **Whether cannonball/missile/cone are new tower types or retrofits onto the existing four
  is an open, and consequential, product question** (already tracked as open question 5,
  below) — retrofitting risks making the four existing, visually distinct towers feel
  samey once they share a delivery layer; new towers keep their identity but grow the
  roster and the balance surface again.

## Decisions made

- Effects (on-hit here, ability-produced in feature 1, aura-produced in feature 3) share
  one primitive, designed up front before feature-specific work begins on any of the three
  requests (see Interconnections, above).
- Sequencing: this feature ships **second**, after tower upgrades and before enemy
  traits/effects (see Priority, above).
- **`Damage` becomes a tagged record**: `Damage(int amount, DamageType type)`, not a sealed
  hierarchy. This keeps `plus`/`scaledBy` meaningful for two same-typed damage values, at
  the cost of needing an explicit rule for combining two *different*-typed `Damage` values (see Shape of the solution,
  below) — a cost the product review judged smaller than losing
  the existing combinator algebra would be.
- **v1 on-hit effects are slow, burn, and freeze** — critical damage is dropped from this
  feature entirely and, per the product review's recommendation, becomes a tower *stat*
  (chance/multiplier on the attacker's roll) if and when it's built, not a post-hit effect;
  acid is deferred past v1 (see V1 Scope, below, for why). Freeze ships as one hard
  speed-to-zero effect; "stun" as a separate name/mechanic stays deferred until feature 1's
  abilities exist for it to meaningfully suppress that freeze wouldn't already cover.
- **Cannonball, missile and cone delivery are new tower types, not retrofits** onto
  `TowerOne`–`TowerFour`. This keeps the four existing towers' identity intact rather than
  risking them feeling samey once they share a delivery layer.
- **A homing missile whose target dies or leaks mid-flight retargets to the nearest
  remaining enemy** rather than fizzling or detonating in place.
- **Damage-type resistance is out of scope for v1.** Every hit carries a `DamageType`, but
  no enemy differentiates by it yet — `EnemyMobSquare.absorb()` and its siblings stay
  type-blind. The tag exists purely as infrastructure for feature 3's shield/resistance
  traits to consume later; wiring an actual resistance now would be scope feature 3 owns.
- **The shared effect primitive lives in a neutral top-level `td.effect` package**, not
  `td.enemy.effect` as an earlier draft of this document proposed. Towers *produce* effects
  and enemies *hold* them, so neither package should own the type — the same reasoning that
  keeps `td.damage` neutral rather than living under `td.tower` or `td.enemy`.
- **The three new towers get two upgrade paths each**, the same as `TowerOne`–`TowerFour`,
  rather than shipping unupgradeable like the Aura tower. Folded into this feature's own
  phased implementation rather than deferred as a separate follow-up.
- **Burn damage is credited to the tower that applied it.** A burn tick routes back through
  `AbstractTower.dealDamage` via a callback the effect carries, so a tower's `damageDealt`/
  `killCount` readouts and its damage-dealt/kill-count upgrade gates stay reachable even
  though the damage lands several ticks after the tower fired.

## V1 Scope

With the decisions above settled, this section pins down what a first version actually
contains: concrete content, the shape of the solution, and a phased implementation order.

### Boundary

- `Damage` widens to `Damage(int amount, DamageType type)` with `DamageType { PHYSICAL,
  MAGIC }`. Every existing tower's `dealDamage` call site is updated to pass a canonical
  type for that tower (physical by default, since none of the four are thematically
  magical today) — a mechanical migration, not a balance change.
- **Three new towers**, one per delivery mechanism, each producing exactly one of the three
  v1 effects so every effect is exercised by real content rather than left theoretical:
  a cannonball tower (straight-line, fixed-destination projectile, splash on arrival), a
  missile tower (homing projectile, single-target on arrival, retargets per the decision
  above), and a cone tower (continuous flamethrower-style wedge, no cooldown — closer in
  spirit to `TowerThree`'s `SonarSweep` than to a discrete shot).
- The shared status-effect primitive (slow/burn/freeze) is built once, generic over
  producer — this feature is its first consumer, but feature 1's abilities and feature 3's
  aura-produced effects are expected to reuse the same type without modification.
- No damage-type resistance, no acid, no critical damage, no retrofit of `TowerOne`–`Four`.
  All four stay explicitly out of v1 per the decisions above.

### Proposed content (illustrative — numbers, names and type/effect pairings are placeholders for a later balance/theme pass)

| Tower            | Delivery                                                                     | Damage type | On-hit effect                 | Resolution                                                                                                                                                                        |
|------------------|------------------------------------------------------------------------------|-------------|-------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Mortar** tower | Cannonball — straight line to target's position at fire time, doesn't re-aim | Physical    | Slow (concussive blast)       | Splash on arrival, reusing `TowerTwo`'s existing falloff shape                                                                                                                    |
| **Seeker** tower | Missile — homing, re-aims each tick at the live target position              | Magic       | Freeze (cryo warhead)         | Single-target on arrival, retargets to nearest enemy if the original target dies/leaks in flight                                                                                  |
| **Cinder** tower | Cone — continuous wedge, no cooldown, slowly reorients toward its target     | Magic       | Burn (flame damage-over-time) | Every enemy currently inside the wedge is hit each tick, mirroring `TowerThree`'s "decide against the arc swept since last tick" approach but for a wedge instead of a full sweep |

Pairing swapped from an earlier draft of this table (Seeker↔burn, Cinder↔freeze): a
cooldown-free cone applying freeze to everything that enters its wedge is a permanent hard
lock on anything caught in it, and putting the hard crowd-control on a single-target shot
instead is both safer to balance and reads better thematically (a flamethrower burns, a cryo
missile freezes).

### Shape of the solution

- **`Damage.plus` requires matching `DamageType`s.** Combining a physical and a magic hit
  into one value has no sensible meaning, so `plus` should assert/throw on a type mismatch
  rather than silently pick one side's type — the same "clamp at zero" discipline `Damage`'s
  compact constructor already applies to amount extends naturally to type.
- **A new shared status-effect primitive**, `td.effect.Effect` (kind — `SLOW`/`BURN`/
  `FREEZE` — magnitude, remaining-ticks) held as a small list on
  `AbstractEnemyMob` and resolved once per `doTick`: DoT effects deal damage and decrement,
  speed-affecting effects fold into the same speed calculation `EnemyMobTriangle`'s
  hurt-curve already touches. **Reapplying an effect that's already active refreshes its
  duration rather than stacking its magnitude** — simplest rule that avoids runaway stacking
  and needs no new algebra beyond what's already decided (effects are a shared primitive,
  not yet another `combine`/`none()` pair, since duration-refresh isn't a commutative
  combination the way `TowerBuff`/`EconomyDelta` are).
- **A new `ProjectileRoster` `GameWorld` collaborator**, analogous to `EnemyRoster`/
  `TowerRoster`: an `AbstractProjectile` base with `CannonballProjectile`/`MissileProjectile`
  leaves, dispatched through a `ProjectileVisitor` the same way `EnemyMobVisitor`/
  `TowerVisitor` already work (keeping the no-`instanceof` rule intact), a `doTick`-style
  advance-then-resolve lifecycle, interpolated position for rendering (matching
  `interpolationAlpha`, the same pattern `EnemyFrameBuilder` and turret-aim use), and a
  `clear()` on level teardown following `TowerRoster.clear()`/`EnemyRoster.clear()`'s
  precedent (see the root `CLAUDE.md`'s Levels section).
- **The cone delivery needs no projectile entity at all** — it resolves directly each tick
  like `TowerThree`, via a new point-in-wedge test alongside `td.tower.targeting`'s existing
  `InRangeTargetQuery`/`OfTypeTargetQuery`, not a new roster.
- **Rendering needs a new sealed `ProjectileDraw` hierarchy** in `td.ui.render`
  (`CannonballDraw`/`MissileDraw`) with its own frame builder (a sibling of
  `TowerEffectFrameBuilder`, closer in shape to `EnemyFrameBuilder` since a projectile is a
  moving, interpolatable entity), a new `ConeDraw` added to the existing `TowerEffectDraw`
  hierarchy alongside `BeamDraw`/`SplashDraw`/`PulseDraw`/`AuraDraw`, and a small vector
  marker per active status effect on an affected enemy (per the root `CLAUDE.md`'s "all game
  art is vector, drawn by code" rule — no image assets).
- **The three new towers get sprite art following `td/tower/CLAUDE.md`'s existing
  checklist** (a `Palette` role, a shape in `Java2DFrameRenderer`, wiring into
  `TowerVisitor`) — the same fixed process as any new tower, not new process.

### Phased implementation order

Per this project's standing "commit after each phase" convention:

1. **`Damage` foundation**: widen to the tagged record, add `DamageType`, update every
   existing `dealDamage` call site and `absorb` override's signature (behavior unchanged —
   see Boundary above). Provable headlessly; no new gameplay yet.
2. **Shared status-effect primitive**: `td.effect.Effect`, the per-tick resolution hook on
   `AbstractEnemyMob`, refresh-not-stack semantics. Provable headlessly with a test-only
   applier — no producer wired yet.
3. **Projectile entity and roster**: `AbstractProjectile`/`CannonballProjectile`/
   `MissileProjectile`, `ProjectileRoster` as a new `GameWorld` collaborator, arrival
   resolution (single-target and splash), the missile retarget-on-death rule. Provable
   headlessly, no rendering yet.
4. **Cone delivery**: the point-in-wedge test and continuous, no-cooldown resolution
   modeled on `TowerThree`/`SonarSweep`.
5. **Wire the three new towers end-to-end** (Mortar/Seeker/Cinder from the content table
   above), each composing steps 1–4 and producing its designated effect.
6. **Rendering**: the `ProjectileDraw` hierarchy and frame builder, `ConeDraw`, per-effect
   status markers on enemies, and the three towers' sprite art. Verified visually via the
   `run-jtd` skill, per this project's UI requirement.
7. **Balance pass and doc cleanup**: tune the placeholder numbers/pairings above via actual
   play, update `td/tower/CLAUDE.md`'s tower table and `README.md`, and fold this feature's
   `TODO.md`-worthy leftovers (if any) into that file.

## Open questions

1. Is "acid" meant to be a second, mechanically distinct DoT (different scaling, different
   interaction with armor/shields) or primarily a different visual/flavor on the same
   burning-DoT mechanism? Deferred past v1, but worth settling before it's built so it
   doesn't get built twice.
2. Does the proposed content table above (which tower gets which delivery mechanism, damage
   type and effect) match the intended feel, or should any pairing change before
   implementation starts?
