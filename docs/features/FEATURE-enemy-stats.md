# Feature Request: Enemy Stats

**Status: implemented.** Plan: `docs/plans/enemy-stats.md`. Deferred: tower detection levels
(stealth is one level), and Momentum's kill-triggered fire-rate burst (`TODO.md`).

## Summary

Give every enemy an RPG-style stat sheet (armor, magic resist, move speed, resilience, spirit
and a few others). Traits, effects, rank and enemy auras then change an enemy only by modifying
those stats, and damage and movement are computed from the resolved stats by one formula. Towers
get a matching set of attacker stats (penetration, crit chance and crit damage, detection).
Two player-facing views make the numbers readable: a richer hover in the wave preview, and a
live inspector for an enemy the player selects on the board.

The goal is to make future mechanics cheaper to add and easier to reason about. The questions
"where in the hit does this multiply in?" and "how does it combine with the others?" get answered
once, in the formula, instead of once per feature. About half of the missing primitives in
`FEATURE-tower-specialization-abilities.md` become stat modifiers instead of new pipeline steps.

## Current state (what exists today)

- **Traits are behaviour hooks, not numbers.** `Trait` has `onHit`, `speedFactor`,
  `isValidTarget` and `blocksEffect`, plus `marker()`. Built-in traits: `PercentResistTrait`
  and `FlatResistTrait` (per hit, scopable to physical or magic), `HurtSpeedTrait`,
  `CriticalImmunityTrait`, `BurnImmunityTrait` and `FreezeImmunityTrait`. `AdaptiveResist` is a
  `TraitTemplate` that resolves once at spawn from `GameWorld.damageTally()`. A trait instance
  is shared by every mob of a definition and holds no per-mob state.
- **The hit pipeline is a sequence of steps.** `doDamage` applies traits' `absorb`, then
  `ActiveEffects.applyShield`, then `Damage.cappedAt` remaining health. Each new modifier has so
  far been a new step, and its position in the sequence was a separate decision.
- **Crit is rolled by the tower** (`AbstractTower.rollCritical`) with one project-wide crit
  multiplier (`Damage.asCritical()`). A burning target doubles crit chance for every tower
  through a direct `EffectKind.BURN` check in that method.
- **Speed**: the intrinsic `speed` is recomputed on every hit from `baseSpeed` × spawn
  multiplier × each trait's `speedFactor`. Effect multipliers are applied when speed is read.
- **Effects** (`td.effect`): `SLOW` (quadratic recovery, one superseded application kept),
  `BURN` (decaying fuel pool apportioned across contributors), `FREEZE` (hard stop, instant
  release), `SHIELD` (percentage damage reduction, optionally restricted to one `DamageType`),
  `INVISIBLE`, and `HEAL` (a `healPerTick()` query the mob applies to itself). Immunity is a
  trait's `blocksEffect`, checked before `ActiveEffects` sees the effect. No haste effect
  exists.
- **Towers** already have an additive buff algebra, `TowerBuff` (damage, range, fire rate,
  bounty, crit chance). A negative fire-rate bonus lengthens the cooldown.
- **`projectile` never depends on `tower`**, and `effect` depends on neither `tower` nor `enemy`.
- **Ranks**: `GRUNT`, `SOLDIER`, `VETERAN`, `ELITE`, `BOSS`.
- **Wave preview hover already exists**: `PanelEnemy.mouseOver` passes the hovered preview mob
  to `EnemyInfoText`, which shows name, description, rank, max health and bounty. Preview mobs
  live in a throwaway `GameWorld` with a no-op host.
- **Board clicks** go to `GameEngine.mouseClicked`, which can select a tower. A selected
  tower's status is refreshed from the EDT render pulse (`PanelTowerInfo.refreshSelected`).
  There is no enemy selection.
- **Leaking** charges a constant cost through `EconomyDelta.leak`.

## What this feature adds

### Enemy stats

| Stat | Meaning | Takes over from |
|---|---|---|
| Max health | as today, scaled by rank | - |
| Armor | percentage reduction of physical damage, diminishing (hyperbolic) | physical `PercentResistTrait` |
| Magic resist | the same for magic damage | magic `PercentResistTrait` |
| Plating | flat reduction per hit | `FlatResistTrait` |
| Move speed | movement speed | `HurtSpeedTrait`, slow and freeze multipliers |
| Damage taken | multiplier on incoming damage, optionally restricted to one damage type | `SHIELD` (below 1); future `VULNERABLE` (above 1) |
| Resilience | lowers crit chance taken and crit damage taken | `CriticalImmunityTrait` (resilience at the cap) |
| Crit chance taken | multiplier on an attacker's crit chance | the hard-coded "burning doubles crit chance" check |
| Spirit | scales healing and shields the enemy receives | - |
| Regeneration | health per tick | the `HEAL` effect becomes a regeneration modifier |
| Effect resistance | per effect kind, `0..1`; scales that effect's duration | `BurnImmunityTrait`, `FreezeImmunityTrait` (resistance 1) |
| Stealth | whether towers can target it | `INVISIBLE` sets stealth; a reveal clears it |

### Effect resistance and control diminishing returns

- **One rule for every effect kind: resistance scales the authored duration.** 50% freeze
  resistance halves a freeze. For `BURN`, a shorter duration means faster decay, and total burn
  damage scales with duration, so 50% burn resistance roughly halves total burn damage with no
  special case. A resistance of 1 or more blocks the effect.
- **Control diminishing returns (DR)** applies to `FREEZE` only. Successive fresh freezes on
  the same mob last 100%, then 50%, then 25%, then the mob is immune until the DR window ends.
  The window resets 10 s after the most recent freeze.
- Only a **fresh** freeze (the mob is not currently frozen) advances a DR step. A freeze
  reapplied while one is active uses the current step and does not advance it, so two
  missiles landing together don't use up two steps.
- DR is granted by a trait, and `ELITE` and `BOSS` enemies get it automatically.
- Combined: `duration = authored × (1 - resistance[kind]) × drStep`. A result under one tick
  counts as blocked.

### Attacker stats (towers)

Armor penetration (percentage, then flat), magic penetration, crit chance, crit damage
multiplier and detection. This covers the specialization nodes that need a partial or full
resistance bypass (Marksman's Eye, Momentum), a per-node crit multiplier (Fifth Shot,
Momentum), and resistance-aware damage (Piercing Tone). Penetration can lower effective armor
to 0 but never below; only an armor-shred modifier on the enemy can make it negative.

### Disruption

Enemies can carry an ability that weakens towers within a radius (for example lower fire rate
or range) while in range. It combines additively with tower buffs such as Aura's, and the
effective fire-rate bonus never goes below -0.75, so a tower is never stalled completely. The
radius is drawn as a ring on the enemy, and affected towers show a status marker.

### Player-facing: wave preview hover

The existing hover text grows into a stat block with display-friendly values, for example
"Armor 100 (-50% physical)". It adds magic resist, plating, speed, resilience, spirit,
regeneration, effect resistances, freeze DR, stealth, and a line per trait. An adaptive trait
is shown as its possible range ("Adaptive: up to X armor, depending on your damage mix"),
because the preview mob can't know the live damage tally.

### Player-facing: live enemy inspector

- Clicking a moving enemy on the board, when not placing a tower, selects it. The click
  target is generous (body radius + 50%, at least 12 px), and the nearest alive enemy wins.
- The description panel then shows its current and max health, the current value of every
  stat, active effects with their remaining time, freeze DR state and traits. It updates live
  as the game runs, including while paused.
- The selected enemy gets a selection ring.
- Selecting an enemy deselects any tower, and the reverse. An upgrade-node hover still takes
  over the panel temporarily, as it does today.
- Lifecycle:
  - When the enemy dies or leaks, the panel keeps its last state with a "Killed" or "Leaked"
    line until the next click.
  - A hatch (the enemy is replaced by its spawns) clears the selection.
  - An invisible enemy can't be newly selected, but an existing selection stays.
  - Level teardown clears the selection.

### Out of scope

- **Bounty multiplier**: bounty stays as it is (tower-side `TowerBuff.bountyBonus` is
  unaffected).
- **Leak cost**: every enemy leak keeps costing the same constant amount.
- **Tenacity**: per-kind effect resistance already covers it. If authoring gets repetitive, a
  factory that sets both `SLOW` and `FREEZE` resistance is enough. It is not a new stat.
- **Agility / movement inertia** is deferred until a haste effect exists. It would override
  two final decisions of `FEATURE-effect-diminishing-returns.md` (the quadratic slow recovery
  and freeze's instant release).
- Evasion, mass, hitbox size and threat.
- Balance preservation: the game isn't balanced yet, so migrated numbers only need to be
  sensible, not equivalent.

## Design proposals (non-binding)

These came out of the refinement discussion and are recorded so planning doesn't have to
re-derive them. They are proposals; planning may replace any of them.

1. **Armor formula**: damage multiplier `100 / (100 + armor)`. Negative armor (shred only)
   gives `2 - 100 / (100 - armor)`. No rank term, since rank already scales health. Converting
   a percentage resist `r` gives `armor = 100·r / (1 - r)` (50% = 100, 25% ≈ 33).
2. **Modifier algebra**: a modifier has `(flat, percentAdd, multiply)`, an identity (`none()`)
   and a "set to" form for absolutes (freeze sets speed to 0). A stat resolves as
   `(base + Σflat) × (1 + Σpct) × Πmult`, then clamps. Stacking rules stay with the source:
   Vulnerable knows it caps at three stacks, and the stat sees only the result.
3. **Damage-taken sources multiply** with each other, so a 50% shield and +45% vulnerability
   give 0.725, not 0.95. Stacks of one source add up inside that source.
4. **Hit order**: raw damage → crit roll (attacker crit chance × crit chance taken, reduced by
   resilience; crit damage reduced by resilience) → penetration → armor or magic resist →
   plating → damage taken → cap at remaining health.
5. **Clamps**: speed ≥ 0. Damage taken ≥ 0.1, so there is no accidental invulnerability; real
   immunity is authored explicitly. Effect resistance stays within `0..1`. Plating keeps
   today's minimum landed damage (planning to confirm what that is).
6. **Traits become modifier providers.** `Trait` shrinks to `marker()` plus something like
   `modifiers(context)`, and `onHit`/`speedFactor`/`blocksEffect` are removed once every trait
   is migrated. Event-shaped behaviour stays in abilities. `HurtSpeedTrait` becomes a
   conditional modifier re-evaluated on every hit, matching today's recompute-on-hit rule.
   `AdaptiveResist` keeps resolving once at spawn, now to an armor or magic-resist modifier.
   Existing trait factories (`PercentResistTrait.physical(0.5)`) can stay as thin factories of
   modifier bundles, so existing tests keep their outcomes.
7. **Resolved stats are cached per mob** and recomputed only when a modifier is added or
   removed (a dirty flag), with no allocation on the per-hit path.
8. **A neutral stat package** that `effect`, `enemy` and `tower` all depend on, so `effect`
   still knows nothing about `enemy` or `tower`.
9. **Attacker stats travel with the hit** as a small immutable attack profile, because
   `projectile` must not read them back from `tower`.
10. **The crit roll moves into enemy-side hit resolution**, because resilience and crit chance
    taken belong to the defender.
11. **Effects emit modifiers.** Burn contributes crit chance taken ×2, freeze sets speed to 0,
    slow multiplies speed along its existing curve, shield lowers damage taken, heal adds
    regeneration, and invisible sets stealth. The effects' own time behaviour (curves, fuel
    pool, superseded slow) is unchanged.
12. **Control DR state lives per mob** next to `ActiveEffects`, because traits are shared and
    stateless; the trait only switches DR on.
13. **Disruption** is a negative `TowerBuff` from zones that enemies put into a world-owned
    list, which towers query the same way they collect aura buffs. Neither `enemy` nor `tower`
    needs to know the other.
14. **Live inspector threading**: the EDT stores the selected mob in a volatile field. The
    game-loop thread builds an immutable inspection snapshot during frame build and puts it
    into `RenderFrame`, and the EDT render pulse reads it from the published frame. The EDT
    never reads live mob state. The selection ring goes through `EnemyOverlayDraw`.
15. **The "immune" marker is derived** from effect resistance ≥ 1, since the immunity trait
    types disappear.

## Interconnections

- **`FEATURE-tower-specialization-abilities.md`**: this feature supplies primitives #1
  (Vulnerable as damage taken), #2 (penetration), #3 (per-attacker crit multiplier), #6
  (reveal as stealth) and #11 (resistance-aware scaling). It also answers that document's open
  questions #3 (composition order) and #7 (crit multiplier). Vulnerable's own stacking
  questions (#1, #2, #5 there) stay open, since stacking belongs to the effect. Toxic Bloom
  (#10) can additionally lower spirit as an anti-heal.
- **`FEATURE-effect-diminishing-returns.md`**: its final decisions (quadratic slow recovery,
  burn fuel pool, freeze as a hard stop with instant release) are kept. Resistance and DR only
  shorten authored durations.
- **`FEATURE-critical-damage.md`**: the "burning doubles crit chance" rule moves from a check
  in the tower into a modifier that the burn effect emits. The crit roll moves to the enemy
  side.
- **`FEATURE-adaptive-elite-resistance.md`**: adaptive armor now resolves to armor or magic
  resist values.
- **`FEATURE-enemy-rank-system.md`**: `ELITE` and `BOSS` gain freeze DR automatically.

## Constraints and open risks

- **Hot path**: stats are resolved on every hit and speed on every tick. The performance
  budget (tick ≤ 1 ms, frame build ≤ 1 ms, ≤ 512 KB per frame) applies; `PerformanceHarness`
  must pass after each shippable part.
- **Size**: this is the largest feature so far. Enemy stats with the trait migration, effect
  resistance and DR, attacker stats, disruption, the preview hover and the live inspector
  should each be shippable and verifiable on their own.
- **Crit timing** moves from fire time to hit resolution. The Sniper is instant, so its crit
  beam should be unaffected, but any visual or trigger that relies on knowing a projectile's
  crit at fire time (Mortar, Seeker, `OnCriticalHitTakenTrigger`) has to be checked.
- **Package boundaries**: `projectile` never depends on `tower`, and `effect` belongs to
  neither `tower` nor `enemy`. Attacker stats and effect-emitted modifiers must respect both.
- **Headless engine and threading**: stats, DR and selection are engine state. The inspector
  must follow the "owning thread publishes one immutable snapshot" rule.
- **UI uniformity**: the inspector, the richer hover and the disruption and selection visuals
  use the existing `Hud` style and render pipeline. They need visual verification from a
  screenshot.
- **Test churn**: many tests build enemies with the current resist and immunity traits.
  Keeping their factories as modifier bundles avoids mass edits.
- **Docs**: `td/enemy/CLAUDE.md` and `td/effect/CLAUDE.md` describe `onHit`/`absorb`/
  `applyShield`, `blocksEffect` and the immunity traits. They change in the same commits as
  the code. `README.md`'s enemy and trait tables change with the migrated traits.
- **Preview accuracy**: preview mobs resolve against a throwaway world, so anything that
  depends on live world state (adaptive traits) can't be shown exactly there.

## Decisions made

1. Tenacity is dropped; per-kind effect resistance covers it. Agility/inertia is deferred until
   a haste effect exists.
2. Control DR applies to `FREEZE` only: steps of 100% → 50% → 25% → immune, a 10 s reset
   window, only a fresh freeze advances a step, and `ELITE` and `BOSS` get it automatically.
3. Effect resistance scales authored duration for every effect kind, including burn; 1 means
   immune. It replaces the burn and freeze immunity traits.
4. Armor and magic resist use the hyperbolic formula without a rank term.
5. Traits become modifier providers; the per-hit and per-speed hook methods go away.
6. Modifiers combine as flat + percentage + multiplier; damage-taken sources multiply with
   each other.
7. The hit order and clamps are as listed in the design proposals (#4, #5).
8. The crit roll moves into enemy-side hit resolution, with a crit multiplier per attacker.
9. `HEAL` becomes a regeneration modifier, scaled by spirit.
10. One feature request covers enemy stats, attacker stats, disruption and both player
    views, with each part shippable on its own.
11. Disruption is a negative tower buff within an enemy's radius, with the fire-rate bonus
    floored at -0.75.
12. The live inspector uses a generous click radius, reads a snapshot published with the frame,
    and keeps a final "Killed"/"Leaked" state until the next click.
13. The bounty multiplier and leak cost stats are cut. Every leak costs the same constant.
14. Balance equivalence is not required; the game isn't balanced yet.

## Open questions

Resolved during planning: migrated stats are converted from the old traits and ranks scale only
health (plus freeze diminishing returns at Elite and Boss); resilience is linear (1 point = 1% of
crit chance and bonus, 100 = immune); stealth has a single level for now, so towers have no
detection stat; towers start at a 1.5 crit multiplier and no penetration, which upgrade nodes
raise; disruption touches fire rate and range, first carried by the Jammer; the board shows only
a selection ring for an inspected enemy.

1. Starting values for the built-in enemies are placeholders like every other number in the
   game; tune them with the other balance entries in `TODO.md`.

---

*After planning and implementation, update this document rather than deleting it (see the
root `CLAUDE.md`'s documentation map): mark it implemented, prune resolved open questions, and
either promote deferred scope to a new request or note it's still wanted for a later version.*
