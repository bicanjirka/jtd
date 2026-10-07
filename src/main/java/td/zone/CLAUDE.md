# `td.zone`

- A `Zone` is a disc with a lifetime that puts its kind's effects on every enemy inside, hidden
  ones included, through its `ZoneOwner` (the tower that made it), so what it does is credited to
  that tower and is periodic: it never crits. A zone outlives its tower; once the tower is sold the
  credit is simply dropped.
- Every zone pulses on the same ticks (twice a second) and once on the tick it is made, so two
  zones of one kind never stack: an enemy standing in both takes the pulse of the one made first.
  Zones combine only through the effects they leave on an enemy, never zone against zone.
- `ZoneRoster.doTick` runs after projectiles and before towers, so a shell that lands this tick
  already pulses. `GameEngine` clears it with the other rosters when a level loads.
- Fallout drains spirit through `SICKENED` stacks and holds heals and shields off with `DEAD_ZONE`, so the
  inspector names it a dead zone.
- A zone's per-enemy memory (a frost zone's time inside) lives on the zone, is keyed by identity
  and is forgotten the pulse an enemy is not inside.
