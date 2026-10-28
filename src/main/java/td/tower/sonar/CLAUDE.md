# `td.tower.sonar`

Constraints for `SonarTower` (in `td.tower`) and its mechanics in this package. The
rules shared by every tower are in `td/tower/CLAUDE.md`.

- `SonarTower` hits what its `SonarBeam` crossed this tick: a spinning beam the arc swept since
  last tick (`SonarSweep`, half-open), never the instantaneous angle. Beams are pure geometry
  (bearings, never enemies), and a mechanic that changes the beam reshapes it from where it is. The
  head is drawn from `sweepRadiansAt`, the same angle that decides hits. Whatever the beam renews
  each pass lasts `SonarSpec.untilNextPass()`.
