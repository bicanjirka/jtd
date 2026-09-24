# `td.effect`

- `Effect` is one record for every `EffectKind`, with no per-kind subclass; every field has an
  identity value for every kind. This package never depends on `td.enemy`: the producer binds a
  `DamageSink` when it creates the effect.
- `ActiveEffects` keeps one live effect per kind, so reapplying refreshes rather than stacks. The
  exception is `SLOW`, which also keeps one superseded application ticking in the background.
  When the winner expires, the superseded one resumes from its own clock (`applySlow`).
  `ActiveEffects` exposes queries only, never its map.
- `SLOW` eases back to full speed quadratically (`authoredDurationTicks` vs `remainingTicks`).
  `FREEZE` is a hard stop.
- `BURN` is a decaying fuel pool made of one `BurnContribution` per application, each with its
  own sink. Tick damage is apportioned across contributors (largest remainder), so every tower
  gets credit for its share. A top-up adds `L0 * (1 - fuel / lmax)`. The burn ends when a tick
  would round to zero.
- `HEAL` is a query (`healPerTick()`) the mob applies to itself, not a sink: `Damage` can't be
  negative. The mob caps it at max health and never heals a dead mob.
- `EffectTemplate` (sealed) is the authored form an ability carries. `SLOW`/`BURN`/`FREEZE`
  have no template because only towers apply them.
- `ShieldTemplate` can be restricted to one `DamageType` (`physicalOnly`/`magicOnly`).
- `EffectTransitions` records when each kind was last gained or lost; the UI derives
  transitions from it.

## Per-tick order (in `DefinedEnemyMob.doTick`)

1. Read `speedMultiplier()` and `healPerTick()` **before** `ActiveEffects.tick()`, since `tick()`
   removes an effect that is on its last tick.
2. `tick()`.
3. `EffectTransitions.observe` **after** `tick()` but **before** the dead-return, so an expiry is
   seen on time and a lethal DoT tick still records its losses.

## Adding a kind

1. `EffectKind` constant.
2. A named `Effect` factory, plus its case in `ActiveEffects.magnitude` (the compiler forces it).
3. An `EffectTemplate` if abilities should author it.
4. A `Palette.STATUS_MARKER_*` role and its cases in `EnemyFrameBuilder.markerPaletteFor` and
   `Java2DFrameRenderer.colorFor` (the compiler forces these).
