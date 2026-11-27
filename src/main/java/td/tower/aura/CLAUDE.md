# `td.tower.aura`

Constraints for `AuraTower` (in `td.tower`), the Beacon, and its mechanics in this package. The
rules shared by every tower are in `td/tower/CLAUDE.md`.

- The light is the Beacon's one circle: every other tower whose centre is in it is Lit, and the Beacon earns XP through
  them from the start. Lit gives nothing until the Lantern node, which also makes every enemy in the light Revealed
  (`REVEAL_TIME` after it leaves). A Beacon never lights another Beacon, and itself only with Self Light (tier II).
  What a Lit tower receives
  (`buffFor`) is only a share of the jam and the blinding (`DISRUPTION_TAKEN`, a multiplier), never damage, range,
  fire rate or crit. Lights that overlap stack, each as if it stood alone.
- The mechanics are `AuraMechanic`s over one `LightSpec`: which enemy the Spotlight holds, how the Lit towers learn,
  what the light's edge does, and Prism. A number they quote lives in the mechanic or in `Spotlight` / `PrismMark`.
- The Spotlight holds its enemy until it dies or leaves the light, then picks again; Priority, Glare's Expose and a
  timed Prism mark are refreshed every tick the enemy stays, so they end a moment after it leaves.
- Prism's stacked marks land on top of what the enemy carries, up to the mark's own cap, as it enters each light, and
  again while it stays once the mark has worn off it; never every tick. Its timed marks are its own share, refreshed
  every tick the enemy stays.
- The light's picture is read from what the tick published (`stirredAt`, `enemiesInLight`, `prismArcs`), never from a
  scan in the frame build. The edge shows for a moment when the light stirs (built, what it lights or what its nodes make
  it do changed), never on a wave's start; an enemy in several lights is lit once, and a crowded frame lights each with
  its pool and rim alone.
- Daylight reaches the enemies only through `GameWorld.daylight()`: the Beacon adds its circle in the towers phase,
  and an enemy reads it on its next tick.
- Breakthrough's and Head Start's XP do not come from `ExperienceAwarder`: the Beacon pays Breakthrough's on the tick a Lit
  tower's `tierRise()` happens. Each tower opens each tier once, so the chain ends by itself. Head Start's is the difference up to its floor every tick, so a tower built or reached later is lifted too.
- Steady Light, Self Light and Head Start are the free tier I, II and III nodes, their mechanics under `UpgradeTree.tierNodeId`. The jam share is the product of the mechanics' factors (`LightSpec.jamTaken`).
