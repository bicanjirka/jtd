# Plan: Enemy Stats (`docs/features/FEATURE-enemy-stats.md`)

## Context

Every new enemy mechanic has so far added its own step to the hit pipeline (`Trait.onHit` →
`ActiveEffects.applyShield` → `cappedAt`), its own speed hook, and its own immunity trait. The
feature replaces this with one stat sheet per enemy and one hit formula. Traits and effects then
only contribute modifiers. Towers get a matching attack profile. On top of that it adds disruption
zones, a richer wave-preview hover and a live enemy inspector. The work is split into seven
phases, each shippable and each committed on its own.

**Product answers (from this session):** migrated stats are converted from today's traits, and
ranks scale only health (plus automatic freeze DR at ELITE and BOSS). Resilience is linear: each
point removes 1% of crit chance taken and 1% of crit bonus, and 100 means crit-immune.
Disruption touches fire rate and range only; its first carrier is a new built-in enemy that no
level wave uses yet. Towers start with a crit multiplier of 1.5 and 0 penetration; the unblocked
stub nodes get wired in phase 7.
**Defaults I chose:** stealth has a single level, so no tower detection stat yet (YAGNI). The
inspector draws only a selection ring on the board.

**Code findings that change the doc's risk list:**
- The crit roll already happens at impact: projectile towers call `AbstractTower.dealDamage` from
  their impact callbacks, and the Sniper is instant. Moving the roll into the enemy's `doDamage`
  keeps the same timing, so the Sniper beam and `OnCriticalHitTakenTrigger` are unaffected.
- Burn ticks can crit today, because their sink calls `dealDamage`. This is kept: the sink binds
  the tower's attack profile.
- The minimum landed damage with plating is **0** (`Damage` clamps at 0), and that stays.
- `PercentResistTrait(f)` stores the fraction that is *kept*, so armor = `100·(1−f)/f`
  (0.5 → 100, 0.8 → 25).

## Architecture (shared by all phases)

- **New neutral package `td.stat`.** It depends only on `td.damage`, and `effect`, `enemy` and
  `tower` may all depend on it.
  - `EnemyStat` enum: `ARMOR, MAGIC_RESIST, PHYSICAL_PLATING, MAGIC_PLATING, MOVE_SPEED,
    PHYSICAL_DAMAGE_TAKEN, MAGIC_DAMAGE_TAKEN, RESILIENCE, CRIT_CHANCE_TAKEN, SPIRIT,
    REGENERATION, SLOW_RESIST, BURN_RESIST, FREEZE_RESIST, STEALTH, FREEZE_DR`. Each constant
    carries its default base value and its clamp. Typed lookups: `mitigationFor(DamageType)`,
    `platingFor`, `damageTakenFor`.
  - `StatModifier(flat, percentAdd, multiply, setTo)`: `none()` identity; `plus` adds flat and
    percent, multiplies the multipliers, and takes the **minimum** of any "set" values. Freeze
    sets speed to 0, and a future reveal sets stealth to 0 over invisible's 1: both follow that
    one rule. A stat resolves as `(base + Σflat)·(1 + Σpct)·Πmult`, then a set value if any,
    then its clamp.
  - `StatModifiers`: an immutable bundle `EnemyStat → StatModifier` with `none()`, `of`, `and`
    and `plus`. Static traits return a constant bundle, so resolving allocates nothing.
  - `StatSheet` (`@ThreadConfined(GAME_LOOP)`, one per mob): base values, reusable accumulator
    arrays, `invalidate()`, and a lazy `value(stat)`. Spirit resolves first. The restorative
    modifiers (`HEAL` regeneration, `SHIELD` damage-taken) are scaled by `max(0, 1 + spirit/100)`.
- **Hit formula** in one place, `td.enemy.HitResolution` (pure, static, unit-tested):
  crit roll (`critChance × critChanceTaken × (1 − res/100)`, rolled only when > 0, so towers
  without crit don't consume random draws) → crit bonus `(mult − 1)·(1 − res/100)` →
  penetration (positive armor only: `max(0, armor·(1−pct) − flat)`) → armor/MR multiplier
  `100/(100+a)` (negative `a`: `2 − 100/(100−a)`) → minus plating → × damage taken (clamped
  ≥ 0.1) → `cappedAt(health)`.
- **`td.damage.AttackProfile`**: crit chance, crit multiplier, armor penetration (percent and
  flat), magic penetration (percent and flat). Narrow factory `none()`/`of(critChance)` plus
  `withX`. It travels with the hit, so `projectile` never reads `tower`.
- **Effects emit modifiers.** `ActiveEffects` contributes to the sheet: slow multiplies
  `MOVE_SPEED` along its existing curve, freeze sets it to 0, shield multiplies the
  damage-taken stat for each type it covers, heal adds `REGENERATION`, invisible sets `STEALTH`
  to 1, and burn multiplies `CRIT_CHANCE_TAKEN` by 2. The time behaviour (slow curve and
  superseded slot, burn fuel pool) doesn't change.

## Phase 1: Stat core and trait migration (no crit changes)

- Add `td.stat` (above) plus a short `td/stat/CLAUDE.md` with the algebra, the clamps and the
  "depends only on `damage`" rule.
- `Trait`: add `StatModifiers modifiers(TraitContext)`. Remove `speedFactor` and
  `isValidTarget`; nothing overrides the latter. `onHit` stays only for `CriticalImmunityTrait`
  until phase 2, and `blocksEffect` stays until phase 3.
- Migrate the traits and keep their factories, so existing tests keep compiling:
  - `PercentResistTrait` becomes armor and/or magic resist. An unrestricted one sets both.
  - `FlatResistTrait` becomes plating for its type, or both types when unrestricted.
  - `HurtSpeedTrait` multiplies `MOVE_SPEED` by a factor of the health fraction and is
    re-evaluated on every hit.
  - `AdaptiveResist.resolvedFor` returns a `PercentResistTrait`, so it now resolves to armor or
    magic resist with no further change.
- `EnemyDefinition`: add `withStat(EnemyStat, float)` for authored base values (no built-in uses
  it yet beyond the conversions).
- `DefinedEnemyMob`:
  - It owns a `StatSheet`. The `speed` field and `shapeSpeedMultiplier` fold become the
    `MOVE_SPEED` base, `baseSpeed × shape multiplier`.
  - `doDamage` uses `HitResolution` with `AttackProfile.none()`, and the tower still rolls the
    crit in this phase.
  - Regeneration replaces `healPerTick()`.
  - `effectiveType()` reads `STEALTH ≥ 1`.
  - The sheet is invalidated on a hit, on applying an effect, and after `ActiveEffects.tick()`.
    Keep the rule that this tick's speed and regeneration are read **before** `tick()`.
- `ActiveEffects`: add `contributeTo(...)`. Remove `speedMultiplier`, `applyShield`,
  `healPerTick` and `isInvisible`.
- Docs in the same commit: `td/enemy/CLAUDE.md` ("Movement and damage", traits),
  `td/effect/CLAUDE.md` (per-tick order, heal-as-query), the root `CLAUDE.md` package list
  (`stat`) and its boundary line, and `README.md`'s trait table.
- Tests: `StatModifierTest` (algebra, identity, min-set), `HitResolutionTest` (armor curve,
  negative armor, plating floor 0, damage-taken clamp). The 4 test files that call
  `trait.onHit` directly move to `modifiers()` or to mob-level assertions. `GameEngineTest`:
  "a 50% physical-resist enemy takes half of a physical hit and all of a magic one", "a shielded
  hurt-speed enemy…".

## Phase 2: Attack profile and crit moved to the defender

- Add `AttackProfile`.
  - `HitReceiver.doDamage(Damage, AttackProfile)`, keeping a default `doDamage(Damage)` that
    passes `none()`. That avoids editing about 44 test call sites.
- `Damage`:
  - Remove `CRITICAL_MULTIPLIER` and `stripCritical`.
  - `asCritical(float multiplier)`.
- `AbstractTower`:
  - `dealDamage` builds the profile from `TowerStats`: add a crit multiplier, and
    `TowerBaseStats.withCritMultiplier`, default 1.5.
  - Delete `rollCritical` and `BURN_CRIT_CHANCE_MULTIPLIER`; burn now emits crit chance taken ×2.
  - `TowerBuff` gains penetration axes through `withArmorPenetration`/`withMagicPenetration`.
- `CriticalImmunityTrait` becomes `RESILIENCE` +100. Remove `Trait.onHit`.
- Docs: the `td/tower/CLAUDE.md` crit bullet and `TowerBuff` axes line; the `td.enemy` hit
  bullet.
- Tests: resilience 100 never crits; 50 resilience halves chance and bonus; burning doubles crit
  chance taken; penetration floors at 0 and doesn't affect negative armor; a DoT tick can still
  crit. Existing crit tests move to per-profile multipliers.

## Phase 3: Effect resistance and freeze DR

- `EffectKind.resistedBy()` returns `Optional<EnemyStat>` (SLOW, BURN, FREEZE).
  `Effect.withDurationScaledBy(f)` scales both the remaining and the authored ticks, so burn
  decays faster.
- `td.effect.FreezeDiminishing` is per-mob, next to `ActiveEffects`. Its steps are 1 → 0.5 →
  0.25 → immune, and the window resets 10 s (via `TickRate`) after the last freeze. Only a fresh
  freeze advances a step. It uses the mob's own tick counter, since `applyEffect` has no game
  time.
- `DefinedEnemyMob.applyEffect`: `duration = authored × (1 − resist) × drStep`. A result under
  one tick is blocked. `FREEZE_DR` comes from `FreezeDiminishingTrait` (sets it to 1) or
  automatically from `Rank.ELITE`/`BOSS`.
- `BurnImmunityTrait`/`FreezeImmunityTrait` become `EffectResistTrait(stat, amount)`, with
  `immuneTo(...)` factories. Its marker is the existing immune glyph at ≥ 1 and a new
  resist glyph below that. Remove `Trait.blocksEffect`.
- Docs: `td/effect/CLAUDE.md` (resistance and DR rule), `td/enemy/CLAUDE.md`, `README.md`.
- Tests (in `GameEngineTest`):
  - a 50% freeze resist halves a freeze;
  - repeated fresh freezes last 100/50/25/0%;
  - a reapplication while frozen doesn't advance the step;
  - the window resets after 10 s;
  - an elite gets DR without the trait;
  - 50% burn resist roughly halves total burn damage.

## Phase 4: Disruption

- `td.stat.DisruptionZone(x, y, radius, fireRatePenalty, rangePenalty)`, collected in a
  world-owned `DisruptionField` (`GameWorld.disruptions()`). It is game-loop confined and
  cleared at the start of each tick's enemies phase. A live mob whose definition has
  `withDisruption(DisruptionAura)` adds its zone in `doTick`.
- Towers phase: each tower samples the field at its centre (squared distances). It
  recalculates stats only when the summed penalty changed, adding `TowerBuff.fireRate(-p)
  .withRange(-q)`. `TowerBuff.fireRateFor` and `rangeFor` floor the combined bonus at −0.75.
- New built-in enemy (for example `JAMMER`) on an existing `BodyArchetype`, registered in
  `EnemyCatalog` but in no level wave; it can be spawned through the debug spawn.
- Visuals: a ring on the enemy (`EnemyRingDraw` with a new `Palette` role) and a disrupted
  status marker on affected towers (`TowerEffectDraw`).
- Docs: the `td/tower/CLAUDE.md` stats-and-buffs section and `README.md`'s enemy table.
- Tests: in range, the fire rate and range drop; out of range, they restore; the floor holds at
  −0.75; an Aura buff and a disruption add up.

## Phase 5: Wave-preview stat block

- `td.enemy.EnemyInspection`: an immutable snapshot built by `DefinedEnemyMob.inspect()`. It
  holds name, rank, health and max, bounty, resolved stat values, active effects with their
  remaining ticks, DR state, trait lines and fate.
- `TraitTemplate.describe()` gives one line per trait. `AdaptiveResist` describes its range
  ("Adaptive: up to X armor, depending on your damage mix").
- `td.ui.EnemyStatText` is a pure formatter: "Armor 100 (−50% physical)". It omits stats at
  their default. `EnemyInfoText` uses it.
- Tests: formatter unit tests. Verify with a screenshot.

## Phase 6: Live enemy inspector

- Engine state, headless:
  - `GameEngine.requestEnemySelectionAt(x, y)` and `clearEnemySelection()` are called on the
    EDT and only set a volatile pending request.
  - On the game-loop thread, at the start of every frame build (which also runs while paused),
    the engine resolves the request. The nearest alive, non-stealthed mob within
    `max(1.5 × body radius, 12 px)` wins; a miss clears the selection.
  - Once selected, a mob stays selected while it turns invisible.
  - Death and leak keep the last snapshot with a "Killed"/"Leaked" line. The mob records its
    fate in `die()`/`leak()`.
  - A hatch (mob replaced while alive) clears the selection, and so does `loadLevel`.
- Publishing: the frame build formats the inspection text with `EnemyStatText` into a new
  `RenderFrame` component (`Optional<String>`, AWT- and domain-free). The EDT never touches a
  live mob. `EnemyFrameBuilder` adds a selection `EnemyRingDraw`.
- `TowerDefense` click: after the existing `unSelectTower()`, a tower hit calls
  `clearEnemySelection()`; otherwise, when not placing, it requests an enemy selection.
  `repaintPublishedFrame` feeds the text to `PanelTowerInfo`. Upgrade hover keeps precedence
  through the existing `hovering` flag.
- Docs: `td/ui/CLAUDE.md` (inspection text travels in the frame).
- Tests (`GameEngineTest`): nearest wins; click radius; an invisible mob isn't pickable but stays
  selected; killed/leaked fate; a hatch clears; a level load clears.

## Phase 7: Wire the unblocked specialization nodes

- **Marksman's Eye II:** 50% armor penetration. A shield is damage taken, not resistance, so it
  is no longer bypassed; I'll note this in the specialization doc.
- **Fifth Shot:** every 5th shot has a profile with crit chance 1, and the tower's crit
  multiplier becomes 2.5.
- **Momentum:** after a crit, the next shot deals ×5 with 100% armor penetration and 100%
  plating penetration. Its "+100% fire rate on kill" part still waits on primitive #4.
- **Piercing Tone:** bonus magic damage scaled by the target's physical mitigation, up to a cap,
  through a new `HitReceiver` query.
- `TODO.md`:
  - delete the bypass, per-node crit and resistance-aware entries;
  - update the Approach of the VULNERABLE, reveal and kill-pulse entries, which now point at the
    damage-taken and stealth stats.
- Close the feature doc: mark it implemented and prune the resolved open questions.

## Critical files

`td/enemy/{DefinedEnemyMob,Trait,TraitTemplate,PercentResistTrait,FlatResistTrait,HurtSpeedTrait,
CriticalImmunityTrait,AdaptiveResist,HitReceiver,EnemyDefinition,BuiltInEnemies,Rank}.java`,
`td/effect/{ActiveEffects,Effect,EffectKind}.java`, `td/damage/Damage.java`,
`td/tower/{AbstractTower,TowerStats,TowerBaseStats,SniperTower,SonarTower}.java`,
`td/tower/buff/TowerBuff.java`, `td/GameEngine.java`, `td/TowerDefense.java`,
`td/ui/{EnemyInfoText,EnemyFrameBuilder,PanelTowerInfo}.java`, `td/ui/render/RenderFrame.java`,
`td/util/GameWorld.java`, plus the new `td/stat/*`.

## Verification (each phase)

- `mvn verify` before each commit.
- `mvn -q compile exec:java -Dexec.mainClass=td.PerformanceHarness` after phases 1, 2, 3, 4
  and 6. Stat resolution is on the per-tick and per-hit path and must stay allocation-free.
- `td.BalanceHarness` on one level after phases 1–3, to check that the numbers are sensible
  (not equivalent).
- `run-jtd` screenshots:
  - phase 4: debug-spawn the new enemy, check the ring and the tower marker;
  - phase 5: the preview hover;
  - phase 6: click an enemy, watch live updates while paused, kill it and see "Killed", and
    check that selecting a tower deselects it.
- One commit per phase, with docs in the same commit.
