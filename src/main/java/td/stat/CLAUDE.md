# `td.stat`

- Neutral ground for stats: depends only on `td.damage` (and `td.util` annotations), so `effect`,
  `enemy` and `tower` can all use it without knowing each other. The core is generic over a
  `StatKey` enum (`EnemyStat`, `TowerStat`): `StatModifier(s)`, `BaseStats`, `StatAccumulator`.
  `StatSheet`/`StatView` stay enemy-only, cached and allocation-free; `ResolvedStats` is the
  immutable form for snapshots.
- A stat resolves as `(base + Σflat) * (1 + Σpercent) * Πmultiply`, plus restorative parts scaled
  by spirit and `HEALING_RECEIVED`, replaced by the lowest set value if any, held to its lowest `capAtShareOfBase` (a share of
  the *base* value, so it also holds against anything that raised it), then clamped to the
  `EnemyStat`'s range. Spirit and healing received resolve first.
- `StatModifier.plus` adds flats and percents, multiplies multipliers and keeps the minimum set
  value; `none()` is the identity (no set value is `+∞`). Stacking rules (caps, refresh) stay with
  the source; the stat sees only the result.
- A per-tick-varying contribution uses the `StatAccumulator` primitives, not a new
  `StatModifier`. A constant contribution is a constant `StatModifiers` bundle.
- `StatSheet` caches until `invalidate()` and resolves into its own arrays: nothing on that path
  allocates. Whoever changes an input (a hit, an effect applied or ticked) invalidates.
- `DisruptionField` is cleared at the start of every tick's enemies phase; live, on-board enemies
  with a `DisruptionAura` add their zone, and towers sample it after. It is the only link
  between them.
- A new stat: an `EnemyStat` constant with its default and clamp. A new stat that a hit reads goes
  into `td.enemy.HitResolution`, the only place the hit formula lives.
