# `td.tower.pulse`

Constraints for `PulseTower` (in `td.tower`) and its mechanics in this package. The
rules shared by every tower are in `td/tower/CLAUDE.md`.

- The field touches `Reach.everyone`, never only the visible; the shadow hits only what can be seen. Toll is
  held through `HitReceiver.holdStacks` every tick an enemy is inside, so it is shared by every field and never
  counted by the tower. Toll adds no damage of its own: what it scales (the drag, Frailty) reads the enemy's
  stacks.
- A rule the field holds inside it (drag, Silence, Daze) is an effect refreshed every tick the enemy is inside,
  never a flag on the enemy. What a visit remembers (time inside for Gravity) lives in the tower, keyed by the
  enemy in a fixed-order map, and is dropped when the enemy leaves or the wave ends.
- A turning beam hits what `SonarSweep` swept since the last tick (half-open), never the instantaneous angle, and
  the head is drawn from the same angle that decides hits. A beam is pure geometry: bearings, never enemies.
