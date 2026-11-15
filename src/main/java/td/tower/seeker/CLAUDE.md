# `td.tower.seeker`

Constraints for `SeekerTower` (in `td.tower`) and its mechanics in this package. The
rules shared by every tower are in `td/tower/CLAUDE.md`.

- The nest loads from the `FireClock` whether or not an enemy is in reach, so it is full at every wave start; what it
  holds is a list of `MissileKind`s drawn as each missile loads (`BroodSpec.draw`), so a missile is drawn in its colour
  in the nest and in flight. A draw rolls the shared random only when the tower has a second missile.
- Stored missiles launch a gap apart, one target pick per missile, from the tower's centre along the ring's tangent.
  The ring turns only while an enemy is in reach and starts each fight at rest.
- A missile in flight asks the tower through its `MissileGuide`: it keeps its pick unless that enemy was frozen since,
  turns to the soonest-out unfrozen enemy near it, and flies home to reload when there is none. It lands with the
  stats its shot began with.
- A hit freezes unless a shield took all of it. Each new freeze is counted for the release and for Feeding; a release
  that falls due fires on the tower's next tick, never from inside the hit.
- The `FrozenLedger` only watches the enemies the tower froze, for the tick their freeze ends, death included: what
  rides the ice (`IceRider`) acts around the freezes the ledger holds or as one ends, never on a freeze another tower
  made.
- How strong the ice is lives on the effect (`IceKind`), so a Ruby freeze holds an enemy immune to freezing through
  `Effect.isKeptOffBy`, not through a check in the tower.
