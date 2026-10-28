# `td.tower.mortar`

Constraints for `MortarTower` (in `td.tower`) and its mechanics in this package. The
rules shared by every tower are in `td/tower/CLAUDE.md`.

- The Mortar's shell is slow and unguided: it flies to where its target stood, so a fast enemy dodges
  it. Its dead zone is `DEAD_ZONE`, a fixed distance on the `Reach` that range never moves. Bracketing is read at
  the landing, from the spec as it stands then, and `BracketTracker` is the only memory of where the
  last shell fell. Every shell Cracks plating; a shell's size is a tower stat that follows its damage.
  A shell's type (`ShellPlan.typeOf` its number) is fixed when it is fired, so it is drawn in its look
  in flight; a special shell leaves one zone where it lands, owned by the Mortar (`ZoneOwner`), and
  shrapnel never does. Shrapnel hands its shell's zone effect to the enemies it hits directly through
  `ZoneEffects.touch`, the same pulse a zone gives. Bomblets are placed along the enemy path from the
  point of it nearest the landing (`PathLine`), never by geometry around the impact. Every blast leaves
  a `BlastMark` the board draws as a fading flash; a nuke adds its own `NukeFlash`. Predictive Fire
  asks the target where it will stand (`EnemyTarget.positionAfter`) and re-aims a few times, since flight
  time depends on where the shell is going; a Barrage's later shells re-pick their target when they fire.
