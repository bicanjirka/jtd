# `td.tower.sniper`

Constraints for `SniperTower` (in `td.tower`) and its mechanics in this package. The
rules shared by every tower are in `td/tower/CLAUDE.md`.

- Steady Aim is a node: the bare tower holds no stacks. Owned, it builds with every shot, whichever enemy it
  is fired at, and `AimFocus` is the only memory of that rhythm: a shot is fired under the stacks the shots
  before it built (`AimLock`), never its own. The cap and the gain per shot are tower stats, so a node changes
  them as data. A counted shot (every n-th) is
  a proc that grants a next-shot status; a new kind of round is also a term in `ROUNDS`, which is how its
  bullet and the rifle show it.
- A shot is one `SniperShot`: the owned mechanics `shape` it in purchase order, and it flies as a `Bullet`
  carrying the stats it was fired with. A bullet whose target died in flight is lost, never re-aimed. When
  it lands, the mechanics `settle` what it counts as, the procs hear of it once (`report`), then the
  mechanics `react`. The wait after a shot is spent by that shot's own attack-speed bonus.
- A mechanic reaches the tower only through `ShotActions`. An execution is not a hit: it never crits and
  spends no mark. A ricochet's hop lands as the shot it came from but can't crit, and nothing triggers off
  it: it is neither settled nor reported.
- Whom it may shoot and whom it picks is a `SniperSpec`; the first owned node that sets an aim decides.
