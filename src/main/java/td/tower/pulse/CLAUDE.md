# `td.tower.pulse`

Constraints for `PulseTower` (in `td.tower`) and its mechanics in this package. The
rules shared by every tower are in `td/tower/CLAUDE.md`.

- The Pulse's field touches `Reach.everyone`, never only the visible. Toll is earned by time inside
  (`TollTracker` counts it per enemy) and held by an ordinary `TOLL` effect the field refreshes each
  tick while the enemy is inside, so a visit is exactly the life of that effect. Toll never adds
  damage: it is the strength (`FieldStrength.at`) the field tops its numeric effects up to. The
  field's damage changes only through stats; Interference reads the other fields once a second
  through a `TowerVisitor`.
- A rule the Pulse holds inside its field (`FieldMode`, the drag in `HoldSpec`) is an effect refreshed every
  tick the enemy is inside, never a flag on the enemy; `FieldHolds` remembers what this field gave, so it lowers
  only that. A Ley Line is pure geometry (`LeyLine`): each field re-picks its host and its casters once a second,
  and applies its own rules to the enemies on every line it casts or hosts, without damage or Toll. Event Horizon counts the deaths of enemies that were inside the
  tick before and resets on the wave announcer; the tower subscribes lazily and unsubscribes in
  `doCleanup`.
