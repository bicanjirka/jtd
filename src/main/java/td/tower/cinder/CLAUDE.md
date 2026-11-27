# `td.tower.cinder`

Constraints for `CinderTower` (in `td.tower`) and its mechanics in this package. The
rules shared by every tower are in `td/tower/CLAUDE.md`.

- The Scorcher deals no direct damage: while it streams, every enemy in its cone takes Flame as fuel for its burn,
  each tick, and `pour` is the one place that decides how much (the Core, Thermal Shock, Meltdown). The stream is no
  hit: it never crits, rolls nothing and reports no hit. What the stream leaves on an enemy lives in its pools and its share of
  Singe, never in state the tower keeps about it; the tower remembers nothing about whom a stream has touched.
- A stream runs its length once started, into the air if its target leaves; the reload is paid on the fire clock
  after it, so attack speed shortens the reload alone. A tower's stats for a stream are taken once, as it starts.
- Soulfire pours its own pool in place of the burn; Flashover reads an enemy's burning pools together.
