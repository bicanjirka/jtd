# `td.stat`

- Neutral ground for enemy stats: depends only on `td.damage` (and `td.util` annotations), so
  `effect`, `enemy` and `tower` can all use it without knowing each other.
- A stat resolves as `(base + Σflat) * (1 + Σpercent) * Πmultiply`, plus spirit-scaled restorative
  parts, replaced by the lowest set value if any, then clamped to the `EnemyStat`'s range. Spirit
  resolves first.
- `StatModifier.plus` adds flats and percents, multiplies multipliers and keeps the minimum set
  value; `none()` is the identity (no set value is `+∞`). Stacking rules (caps, refresh) stay with
  the source; the stat sees only the result.
- A per-tick-varying contribution uses the `StatAccumulator` primitives, not a new
  `StatModifier`. A constant contribution is a constant `StatModifiers` bundle.
- `StatSheet` caches until `invalidate()` and resolves into its own arrays: nothing on that path
  allocates. Whoever changes an input (a hit, an effect applied or ticked) invalidates.
- A new stat: an `EnemyStat` constant with its default and clamp. A new stat that a hit reads goes
  into `td.enemy.HitResolution`, the only place the hit formula lives.
