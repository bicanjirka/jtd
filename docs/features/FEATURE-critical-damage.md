# Feature Request: Critical Damage

**Priority: unblocks two already-deferred items.** `TODO.md`'s "Critical damage is not implemented" entry has carried a
settled design recommendation (a pre-hit, chance-based multiplier rolled as a tower stat) since the damage-types
feature's product review, but no code. `FEATURE-enemy-traits-and-effects.md`'s V1 Scope explicitly dropped any
trait or ability keyed off a critical hit "because critical damage doesn't exist yet" — this feature is what makes
that content buildable, and folds it in as its own payoff rather than leaving it a second, later request.

## Summary

Give a tower's hit a chance to land for bonus damage, and let enemies react to that the same way they already react
to damage type and to on-hit effects. Concretely, this is four connected pieces:

1. **The crit roll itself** — a chance/multiplier pair, resolved once per hit, before the enemy ever sees the
   `Damage`.
2. **A `Damage` that says whether a landed hit was critical** — the minimum needed for anything downstream (a
   trait, an ability, the renderer) to react specifically to a crit rather than to "a big hit."
3. **A way to *get* crit chance** — an upgrade-path axis, exercised by real content, not left theoretical.
4. **Enemy-side content that reacts to a crit** — an immunity trait and an ability trigger, the two examples the
   traits-and-effects doc named and deferred.

Every piece here is additive to a currently-zero default: no existing tower rolls a crit today (chance is `0`
everywhere until an upgrade path grants it), so no existing balance or test changes as a side effect of landing the
mechanism itself — the same "mechanical migration, not a balance change" shape `Damage`'s original widening to
carry `DamageType` had.

## Current state (what exists today)

- **`Damage`** (`td.damage.Damage`) is a two-field record: `amount` (clamped at zero) and `type`. It carries no
  notion of *how* a hit was produced, only what it is.
- **`AbstractTower.dealDamage(EnemyMob, Damage)`** (`td.tower.AbstractTower`) is the one place every tower's hit
  passes through — `damageDealt`/`killCount` accounting lives here, and a leaf never calls `enemy.doDamage`
  directly. It has no crit chance or multiplier of any kind today; `TODO.md`'s entry recommends this as the roll
  site.
- **`TowerBuff`** (`td.tower.buff.TowerBuff`) carries four additive axes — `damageBonus`, `rangeBonus`,
  `fireRateBonus`, `bountyBonus` — each defaulting to `0` at `none()`, combined via `AbstractTower.recalculateStats()`
  from every Aura tower in range plus the tower's own chosen `UpgradePath`. Nothing occupies a fifth axis yet.
- **`Trait`** (`td.enemy.Trait`) already has the exact hook a crit-reactive trait needs:
  `onHit(Damage incoming, TraitContext context) -> Damage`, folded in sequence by `DefinedEnemyMob.absorb`. It just
  has nothing to branch on, since no `Damage` is ever marked critical.
- **`AbilityTrigger`** (`td.enemy.AbilityTrigger`) is a sealed set of five kinds evaluated once per `doTick` by
  `AbilityEvaluator`, exercised end-to-end by the Warden. `OnDeathTrigger`'s shape — an edge-triggered read of a
  boolean the mob computes about itself (`AbilityContext.justDied()`) — is the direct precedent for a sixth kind
  keyed off "did this mob just take a critical hit."
- **The death-fade rendering precedent** (`AbstractEnemyMob.deathTick`/`ticksSinceDeath`/`fadeAlpha`,
  `EnemyFrameBuilder.body`) is the direct precedent for a crit's own transient visual: a mob records the tick a
  crit landed, and a frame builder draws a fading marker for a fixed window afterward — the same shape, a different
  trigger.
- **No floating damage numbers or hit-flash mechanism exists anywhere in `td.ui.render`.** The closest things are
  `StatusMarkerDraw` (a *persistent*, capped-at-3 icon row keyed off `activeEffectKinds()` — wrong shape for a
  one-shot event) and `EnemyFadeDraw` (a one-shot, timed animation keyed off death — the right shape, reused below).

## Product review notes

**Why a fixed, global crit multiplier for v1, not a per-tower one.** `TODO.md`'s recommendation left the
chance/multiplier pair as "base fields on `AbstractTower`, or per-leaf." Making the *multiplier* vary per tower (or
per upgrade path) means a `CriticalImmunityTrait` can't undo a crit's bonus without first knowing which multiplier
produced it — coupling an enemy trait to tower-side configuration. Fixing the multiplier once, the same discipline
`TickRate` already applies to the tick rate ("the tick rate lives once... don't restate it anywhere else"), lets a
trait strip a crit's bonus by inverting one project-wide constant. Only *chance* varies per tower (via the upgrade
path below); the multiplier is a `Damage`-owned constant everyone agrees on. If a later feature genuinely needs a
tower-specific multiplier, that's a small, isolated widening of this same constant into a `TowerBuff` axis — not a
reason to build it now against no concrete content that needs it.

**Why `Damage` gains one `boolean`, not a split base/bonus pair.** An alternative shape — storing a hit's base
amount and its critical bonus as two separate fields, so a trait can strip the bonus without knowing any multiplier
at all — is a real algebra improvement, but it is a much larger change to a value type seven towers and four
effect/trait files already construct and scale, for a benefit (multiplier-agnostic stripping) the fixed-constant
decision above already gets for free at a fraction of the cost. Recommendation: one `boolean critical` field, plus
one instance method that strips a crit using the known constant (see Shape of the solution). Revisit only if a
second consumer ever needs to know a hit's pre-crit amount for a reason other than immunity.

**Why only one tower's content in v1.** Extending every one of the seven attack towers' fourteen already-shipped
upgrade paths with a crit-chance bonus would re-theme content nobody asked to change. Recommendation: exercise the
new `TowerBuff` axis on exactly one existing path — see Boundary — the same "narrow, real content over broad,
thin content" choice `FEATURE-enemy-traits-and-effects.md` made picking one boss over several shallow ones. The axis
itself is not tower-specific, so any future path can use it without further plumbing.

## Decisions made

- **Crit chance and multiplier are a pre-hit tower stat, not a `td.effect.Effect`.** Confirms `TODO.md`'s existing
  recommendation: a crit is resolved once, at the moment of the hit, never applied to the enemy afterward the way a
  timed status effect is.
- **The multiplier is one project-wide constant** (`Damage.CRITICAL_MULTIPLIER`, placeholder `1.5f`); only chance
  varies, and only via `TowerBuff`'s new `critChanceBonus` axis (default `0`, so every tower not granted any stays
  at exactly today's behavior).
- **`SniperTower`'s existing `Veteran` path is v1's exercising content.** It is money-independent and kill-count
  gated — "a marksman who's landed enough kills starts placing shots that count extra" is a direct reading of the
  path's existing identity, and needs no rename. No other tower's paths change in v1.
- **The immunity trait strips a crit's bonus by inverting the fixed multiplier**, not by tracking a hit's pre-crit
  amount — see Product review notes. It is a new, independent `Trait`, not a change to `PercentResistTrait`/
  `FlatResistTrait`.
- **The new `AbilityTrigger` fires every time a crit lands, not once ever** — unlike `OnceTrigger`/`OnDeathTrigger`/
  `HealthThresholdTrigger`, "survived a critical hit" is a repeatable event over a mob's life (the Warden's own
  periodic reinforcement ability already establishes that not every trigger is fire-once).
- **The crit's on-board visual is a new, timed marker at the hit location** (a "spark"), not a floating damage
  number. A floating number needs real text rendering — a render primitive this codebase doesn't have anywhere yet
  (`td/ui/CLAUDE.md` already flags this exact gap for the status-marker overflow badge) — and building it only for
  this feature is disproportionate. A spark reuses the death-fade shape exactly: a timed marker keyed off a tick
  recorded on the mob.

## V1 Scope

### Boundary

- `Damage` gains one `boolean critical` field and one project-wide `CRITICAL_MULTIPLIER` constant. Every existing
  `Damage.physical`/`magic` call site is unchanged (both stay non-critical factories); `scaledBy`/`cappedAt`
  preserve the flag through `absorb`'s resist-trait pipeline, the same way they already preserve `type`.
- `AbstractTower.dealDamage` rolls the crit for every tower, using `this.context.random()` — no per-leaf `doTick`
  changes needed, since every existing call site already funnels through this one method.
- `TowerBuff` gains a fifth axis, `critChanceBonus` (default `0`, purely additive like the other four).
  `TowerStats` gains a `critChance` field computed the same way `damage`/`range`/`coolDown` already are.
- **Exactly one path grants it**: `SniperTower.VETERAN` gains a `critChanceBonus` alongside its existing
  damage/range/bounty bonus. No other tower's upgrade content changes.
- **One new `Trait`**: `CriticalImmunityTrait`, reusable by any `EnemyDefinition` the same way
  `PercentResistTrait`/`FlatResistTrait` already are. **Not** wired onto any of today's five migrated enemies or
  the Warden in v1 — see Open questions on whether it should be.
- **One new `AbilityTrigger`**: `OnCriticalHitTakenTrigger` (no parameters — it fires on the tick a crit lands,
  full stop), paired with the existing `ApplyEffectAction`/`ShieldTemplate` to produce "gain a shield after
  surviving a critical hit," the exact example both `TODO.md`'s history and the traits-and-effects doc named.
  **Not** wired onto the Warden or any new boss in v1 — see Open questions.
- **One new render primitive**: a crit-spark marker, timed and fading like a death-fade, drawn wherever a crit
  landed. No new UI panel, no floating text, no numeric damage display.
- **Not in scope**: a per-tower or per-path crit *multiplier* (fixed globally instead — see Decisions made); a
  second attack tower's paths gaining `critChanceBonus`; any change to `PercentResistTrait`/`FlatResistTrait`;
  retrofitting the immunity trait or the new ability onto existing content (both ship as reusable, unexercised-by-
  default building blocks unless Open questions below resolve otherwise, exactly as `DamageTypeResistTrait` was
  already left in `TODO.md`'s own precedent — a trait can exist and be correct without every mob using it).

### Shape of the solution

- **`Damage`** widens to `Damage(int amount, DamageType type, boolean critical)`. `physical(int)`/`magic(int)` stay
  two-argument, non-critical factories — no existing call site changes. A new instance method,
  `asCritical()` (`this.scaledBy(Damage.CRITICAL_MULTIPLIER)` with `critical = true`), is what
  `AbstractTower.dealDamage` calls when its roll succeeds. A new `stripCritical()` (`critical ? this.scaledBy(1f /
  CRITICAL_MULTIPLIER) with critical = false : this`) is what `CriticalImmunityTrait.onHit` calls. `scaledBy`/
  `cappedAt` widen to copy `critical` through unchanged — resisting or capping a critical hit still leaves it a
  (smaller) critical hit, which matters for the ability trigger below: a hit `PercentResistTrait` shrank is still
  the crit the mob "survived."
- **`AbstractTower.dealDamage(EnemyMob enemy, Damage damage)`** rolls once, before calling `enemy.doDamage`:
  `float chance = this.stats().critChance(); Damage actual = (chance > 0 &&
  this.context.random().nextFloat() < chance) ? damage.asCritical() : damage;`. A tower with `critChance == 0`
  (everything not carrying the new path) takes the same branch it always has, at the cost of one comparison —
  the "mechanical migration" this project already did once for `DamageType`.
- **`TowerBuff`** gains `critChanceBonus` as its fifth field, defaulting to `0` at `none()`, additive in `combine`
  like the other four, with a `critChanceFor(float base)` accessor mirroring `rangeFor`/`fireRateFor` (base is
  always `0` in v1, since no tower has innate crit chance — only a chosen path grants it, exactly like
  `bountyBonus` today). `TowerStats.of` widens to compute and carry it.
- **`SniperTower.VETERAN`**'s `TowerBuff` construction widens from four arguments to five, adding a placeholder
  crit-chance bonus (numbers are a later balance pass, same as every other feature doc's proposed content).
- **`CriticalImmunityTrait`** (new, `td.enemy`, no fields — or one if a partial-immunity variant is wanted, see
  Open questions): `onHit(Damage incoming, TraitContext context)` returns `incoming.stripCritical()`. Placed
  alongside `PercentResistTrait`/`FlatResistTrait`, folded into `absorb`'s existing per-trait loop with no change
  to `DefinedEnemyMob`.
- **`OnCriticalHitTakenTrigger`** (new, `td.enemy`, a parameterless record) joins `AbilityTrigger`'s `permits`
  list. `AbilityState.forTrigger`'s exhaustive switch (no `default`) forces the new case — `0`, unused, the same as
  `HealthThresholdTrigger`/`OnDeathTrigger`/`TimeSinceLastHitTrigger` today. `AbilityEvaluator.shouldFire` gains
  `case OnCriticalHitTakenTrigger ignored -> context.justTookCriticalHit();` — no `AbilityState` bookkeeping needed,
  since the context method itself is already edge-triggered (see below).
- **`AbilityContext`** gains `boolean justTookCriticalHit()`, alongside the existing `justDied()`.
  **`AbstractEnemyMob`** gains a `criticalHitTick` field (`-1` sentinel), captured on the *next* `doTick` after a
  critical hit landed — not synchronously inside `doDamage`, which has no `gameTime` to record — mirroring exactly
  how `deathTick` is already captured one tick after `dead` becomes true when a tower kills a mob during the tower
  phase of a tick after that mob's own `doTick` already ran (see `td/enemy/CLAUDE.md`'s death-timing invariant,
  which this reuses verbatim for the crit case). `justTookCriticalHit()` reads `criticalHitTick == gameTime` the
  same way `justDied()` reads `ticksSinceDeath(gameTime) == 0`.
- **Rendering**: `EnemyFrameBuilder.body` gains a third emission alongside the existing body/marker pair — while
  `gameTime - mob.criticalHitTick() <= CRIT_SPARK_DURATION_TICKS`, it adds a new `CritSparkDraw` (new,
  `td.ui.render`, an `EnemyFadeDraw`-shaped record: position, scale, a fade fraction computed the same way
  `fadeAlpha` computes one) to a new `RenderFrame.critSparks` list. `Java2DFrameRenderer` gets one new case for it —
  a small burst/asterisk `Shape`, one new `Palette` role, no image assets, per this project's vector-only rule.
- **`AbstractTower.getStatusString()`** appends a `"Crit chance: N%\n"` line, shown only when `critChance() > 0` —
  the same conditional-line pattern `chosenPath.map(...).orElse("")` already uses for the "Specialized: …" line.

### Phased implementation order

Per this project's standing "commit after each phase" convention:

1. **`Damage` foundation**: the `critical` field, `CRITICAL_MULTIPLIER` constant, `asCritical()`/`stripCritical()`,
   `scaledBy`/`cappedAt` preserving the flag. Provable headlessly against `Damage` alone; no gameplay wired yet.
2. **The roll and the stat plumbing**: `TowerBuff.critChanceBonus`, `TowerStats.critChance`,
   `AbstractTower.dealDamage`'s roll, `SniperTower.VETERAN`'s new bonus, the status-string line. Provable via
   `GameEngineTest`-style coverage with a seeded `RandomSource` (per this project's "randomness is injected" rule)
   forcing a crit or forcing none.
3. **Enemy-side reactions**: `CriticalImmunityTrait`, `OnCriticalHitTakenTrigger`, `AbilityContext
   .justTookCriticalHit()`/`AbstractEnemyMob.criticalHitTick`, `AbilityEvaluator`'s new case. Provable headlessly
   against a fake ability context and a trait test mirroring `PercentResistTraitTest`'s shape — neither needs
   rendering to be correct.
4. **Rendering**: `CritSparkDraw`, `RenderFrame.critSparks`, `EnemyFrameBuilder`'s new emission, the `Palette` role
   and `Java2DFrameRenderer` shape/colour cases. Verified visually via the `run-jtd` skill, per this project's UI
   requirement.
5. **Documentation and balance pass**: update `td/tower/CLAUDE.md`'s Aura-buff-stacking section for the fifth
   `TowerBuff` axis, `td/enemy/CLAUDE.md` for the new trait/trigger, `td/ui/CLAUDE.md`'s render-pipeline table for
   the new list/builder emission, tune the placeholder chance/multiplier numbers via actual play, and close
   `TODO.md`'s "Critical damage is not implemented" entry.

## Open questions

Resolved during implementation, recorded here rather than left open:

1. **Both shipped, on concrete content.** `CriticalImmunityTrait` went onto `ARMORED` (alongside its existing
   percent resistance — an armored mob shrugging off a precisely placed shot reads as the same idea) and
   `OnCriticalHitTakenTrigger` went onto the Warden as a sixth ability (a self-shield, reusing the same
   `ShieldTemplate`/`SelfTarget` shape its two existing shield abilities already use) — the "obvious payoff" this
   question named, rather than landing as unexercised building blocks.
2. **Left as placeholders, not tuned.** The `1.5×` multiplier, Veteran's `15%` crit-chance bonus, and the Warden's
   new `30%`/100-tick shield are all illustrative, the same as every other number this feature and its predecessors
   introduced. Tracked in `TODO.md`'s "Critical-damage numbers are unbalanced placeholders" entry rather than
   guessed at here.
3. **Not resolved by a screenshot** — a live crit didn't land during the `run-jtd` verification pass in the time
   available (10 tower kills plus a 15% per-shot roll is a real grind for an automated pass), so whether the spark
   reads clearly at board scale is still an open call for the next person to actually see one land in play.

## Addendum: burning doubles crit chance

A follow-up request, added after v1 shipped: a burning enemy should be twice as likely to take a critical hit -
the first interaction between the damage-types feature's status effects and this feature's crit roll.

**Universal, not a new upgrade path.** The doubling applies to every tower's roll against every burning target,
the moment either side has any nonzero ingredients (a tower with crit chance, a target that's burning) - not a
perk gated behind its own price or condition. This matches how `td.effect.Effect` is already documented as "a
shared, neutral primitive" any tower or ability can produce or react to, rather than something owned by whichever
tower happens to apply burn (`CinderTower` today). A tower with no crit chance still rolls nothing, same as
before; a burning target only ever *helps* a roll that was already possible, never creates one from zero.

**Shape:** `AbstractTower.rollCritical` takes the target `EnemyMob` as well as the `Damage`, and checks
`enemy.activeEffectKinds().contains(EffectKind.BURN)` - a query this interface already exposes publicly (the
status-marker UI already reads it) - no new plumbing needed on the enemy side. When burning, the effective chance
is `Math.min(1f, critChance() * BURN_CRIT_CHANCE_MULTIPLIER)` before the roll; `BURN_CRIT_CHANCE_MULTIPLIER = 2f`
lives once, as a constant on `AbstractTower`, the same discipline `Damage.CRITICAL_MULTIPLIER` already follows.
The doubling is invisible to `getStatusString()`'s displayed "Crit chance: N%" line - that stays the tower's own
base chance, since the game has no per-target UI to show a chance that depends on whichever enemy happens to be
in the crosshairs right now; this is a documented, deliberate scope cut, not an oversight.

Not in scope: any other `EffectKind` interacting with crit chance (freeze, slow, shield, invisible), and any
change to how burn itself is applied - this only reads whether it is currently active.

