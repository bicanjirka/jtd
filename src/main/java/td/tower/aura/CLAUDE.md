# `td.tower.aura`

Constraints for `AuraTower` (in `td.tower`), the Beacon, and its mechanics in this package. The
rules shared by every tower are in `td/tower/CLAUDE.md`.

- The light is the Beacon's one circle: every enemy in it is Revealed (`REVEAL_TIME` after it leaves), and every
  other tower whose centre is in it is Lit. A Beacon never lights another Beacon. What a Lit tower receives
  (`buffFor`) is only a share of the jam and the blinding (`DISRUPTION_TAKEN`, a multiplier), never damage, range,
  fire rate or crit. Lights that overlap stack, each as if it stood alone.
- The mechanics are `AuraMechanic`s over one `LightSpec`: which enemy the Spotlight holds, how the Lit towers learn,
  what the light's edge does, and Prism. A number they quote lives in the mechanic or in `Spotlight` / `PrismMark`.
- The Spotlight holds its enemy until it dies or leaves the light, then picks again; Priority, Glare's Expose and a
  timed Prism mark are refreshed every tick the enemy stays, so they end a moment after it leaves.
- Prism's stacked marks land on top of what the enemy carries, up to the mark's own cap, as it enters each light, and
  again while it stays once the mark has worn off it; never every tick. The light's Static and Soulbrand are tags in
  the Beacon's own `TagLedger`s, settled by the Burst's rules (`StaticMechanic`, `SoulbrandMechanic`); they pay no
  currency, and their damage is the Beacon's.
- Daylight reaches the enemies only through `GameWorld.daylight()`: the Beacon adds its circle in the towers phase,
  and an enemy reads it on its next tick.
- Breakthrough's XP is the one XP that does not come from `ExperienceAwarder`: the Beacon pays it on the tick a Lit
  tower's `tierRise()` happens. Each tower opens each tier once, so the chain ends by itself.
