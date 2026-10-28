# `td.tower.seeker`

Constraints for `SeekerTower` (in `td.tower`) and its mechanics in this package. The
rules shared by every tower are in `td/tower/CLAUDE.md`.

- The Seeker's nest loads from the cooldown whether or not an enemy is in range, so it fills between
  waves; stored missiles launch `launchGapTicks` apart, one target pick per missile. A spread salvo
  (`SalvoPlanner`) avoids the enemies the salvo in progress has fired at, and a mechanic reacts to a
  landed missile through `Impact`; a rearmed missile is flagged so its freeze rearms nothing. How a
  missile freezes, shatters and crits is spec data (`FreezeSpec`, `ShatterSpec`), not a hook; the
  `FrozenLedger` only watches the enemies the Seeker froze for the tick their freeze ends. A missile's
  payload is decided when it is launched (`PayloadPlan.loadFor` its number), so it is drawn in its
  colour in flight; a plain missile freezes, a payload missile does only what its payload does.
