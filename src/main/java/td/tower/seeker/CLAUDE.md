# `td.tower.seeker`

Constraints for `SeekerTower` (in `td.tower`) and its mechanics in this package. The
rules shared by every tower are in `td/tower/CLAUDE.md`.

- The nest loads from the `FireClock` whether or not an enemy is in reach, so it is full at every wave start; what it
  holds is a list of `MissileKind`s fixed as each missile is made (`BroodSpec.kindOf`), so a missile is drawn in its
  colour in the nest and in flight. Every missile the tower makes (a load, a hatch, a Swarm's) takes the next place in
  one order, which starts over each wave; it never rolls the shared random.
- A Release's missiles are as many as the nest holds. A Hatching's sit on top of the nest's own (`SeekerNest`): they
  leave first and take none of its room, so the nest keeps loading.
- Stored missiles launch a gap apart, one target pick per missile, from the tower's centre along the ring's tangent.
  The ring turns only while an enemy is in reach and starts each fight at rest.
- A missile in flight asks the tower through its `MissileGuide`: it keeps its pick unless that enemy was frozen since,
  turns to the soonest-out unfrozen enemy near it, stays on its pick when there is none, and flies home to reload
  only when no enemy is left. It lands with the stats its shot began with.
- A hit freezes unless a shield took all of it. Each new freeze is counted for the release and for Feeding; a release
  that falls due fires on the tower's next tick, never from inside the hit.
- The `FrozenLedger` only watches the enemies the tower froze, for the tick their freeze ends, death included: what
  rides the ice (`IceRider`) acts around the freezes the ledger holds or as one ends, never on a freeze another tower
  made.
- How strong the ice is lives on the effect (`IceKind`), so a Ruby freeze holds an enemy immune to freezing through
  `Effect.isKeptOffBy`, not through a check in the tower.
