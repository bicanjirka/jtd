# `td.effect`

- `Effect` is one record for every `EffectKind`, with no per-kind subclass; every field has an
  identity value for every kind. This package never depends on `td.enemy`: the producer binds a
  `DamageSink` when it creates the effect.
- `ActiveEffects` keeps one live effect per kind, so reapplying refreshes rather than stacks. The
  exceptions are the decaying kinds (`EffectKind.isDecaying()`: `CHILL`, `BURN`, `POISON`), which
  add up as a level, and `VULNERABLE`, which stacks. `ActiveEffects` exposes queries only, never
  its map.
- `CHILL` is a level (the share of speed lost, capped at 0.8) made of one `FuelContribution` per
  application, each fading linearly on its own slope. An application only adds what fits under the
  cap. It also cuts a burn's damage by up to half at the cap. `FREEZE` is a hard stop.
- `BURN` and `POISON` are each a decaying fuel pool of one `FuelContribution` per application, each
  with its own sink (`EffectKind.isFuelPool()`). Tick damage is apportioned across contributors
  (largest remainder), so every tower gets credit for its share. A top-up adds
  `L0 * (1 - fuel / lmax)`. A pool ends when a tick would round to zero. The two kinds are separate
  pools, so they stack with each other. A pool earns a stack of its lasting mark (`SCORCHED` for
  burn, `SICKENED` for poison, `EffectKind.debuffEarned()`) when it starts and another every
  `STACK_INTERVAL_TICKS` while it lasts; a top-up earns none.
- `SCORCHED` and `SICKENED` are real effects that only `ActiveEffects` creates. Each stack lowers
  `RESILIENCE` or `SPIRIT` by one. They outlast the pool, are never cleared by another effect
  (a freeze puts out the burn, not its mark) and lose a stack every `STACK_DECAY_INTERVAL_TICKS`
  scaled by the enemy's spirit factor (`tick(spiritFactor)`; `0` at spirit -100 means never).
- `VULNERABLE` stacks (cap 3) on the enemy, whichever tower applied them, on one shared clock that
  any application refreshes; a full stack only refreshes.
- Effects change a mob only through `contributeTo(StatAccumulator)`: chill and poison on
  `MOVE_SPEED` (multiplying), freeze setting it to zero, shield and vulnerable on damage taken, heal
  on `REGENERATION`, invisible and revealed on `STEALTH` (revealed sets it to 0, which beats
  invisibility's 1), scorched stacks on `RESILIENCE` (-1 each) and sickened stacks on `SPIRIT` (-1 each), both floored
  by the stat's range. Shield and heal go in as restorative, so spirit scales them.
  The mob applies regeneration to itself, capped at max health; `Damage` can't be negative.
- Resistance and diminishing returns only shorten an authored duration, never change an effect's
  curve: `duration = authored * (1 - resist[kind]) * freezeStep`, and under one tick is blocked.
  `EffectKind.resistedBy()` names the resisting stat. Every kind in `EffectCategory.HARD_CC`
  diminishes, on every enemy (`FreezeDiminishing`, per mob, only a fresh application advances a
  step).
- `EffectInteractions` is the one table of how an active kind acts on another: what it keeps out (a
  frozen enemy cannot burn) and what applying it consumes (freezing removes a burn, and a chill,
  which also makes the freeze last longer by the chill's level). Add a rule there, never a
  condition in `ActiveEffects`.
- `EffectKind.category()` groups kinds for diminishing returns and the inspector; it never changes
  numbers.
- `EffectTemplate` (sealed) is the authored form an ability carries. Only kinds an enemy ability
  applies have one; the rest are applied by towers.
- `ShieldTemplate` can be restricted to one `DamageType` (`physicalOnly`/`magicOnly`).
- `EffectTransitions` records when each kind was last gained or lost; the UI derives
  transitions from it.

## Per-tick order (in `DefinedEnemyMob.doTick`)

1. Read resolved speed and regeneration **before** `ActiveEffects.tick()`, since `tick()`
   removes an effect that is on its last tick; invalidate the stat sheet after it.
2. `tick()`.
3. `EffectTransitions.observe` **after** `tick()` but **before** the dead-return, so an expiry is
   seen on time and a lethal DoT tick still records its losses.

## Adding a kind

1. `EffectKind` constant, and its cases in `category()` and `resistedBy()`.
2. A named `Effect` factory, plus its cases in `ActiveEffects.magnitude` and `contributeTo` (the
   compiler forces them).
3. An `EffectTemplate` if abilities should author it.
4. A `Palette.STATUS_MARKER_*` role and its cases in `EnemyFrameBuilder.markerPaletteFor`,
   `Java2DFrameRenderer.colorFor` and `EnemyStatText.effectRow` (the compiler forces these).
