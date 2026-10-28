# `td.tower.splash`

Constraints for `SplashTower` (in `td.tower`) (and its Stormcaller and Hexer forks) and its mechanics in this package. The
rules shared by every tower are in `td/tower/CLAUDE.md`.

- `SplashTower` falloff is `1 - (d/radius)²` from the primary, floored by `BLAST_EDGE`. The blast
  radius is `AREA_RADIUS`: range doesn't move it, auras never emit it, only its own nodes change it,
  and every distance the payload uses (an arc's reach) grows with it. The blast lands and Saturates
  first, then the payload runs.
- A Hexer's hex is an enemy effect in `EffectCategory.HEX`, applied through `applyEffect`, and that
  effect is the hex's only timer (spirit paces it). The Hexer's `HexKeeper` only watches it: what
  a hex pays out when it ends or its carrier dies is settled at the start of the Hexer's tick,
  never by the enemy. The keeper reaches its caster only through `HexCaster`, so the dev panel's
  caster pays hexes out by the same rules. Each hex picks its own target in one pass per cast, never per tick.
- What a hex shares or releases in range (Sympathy, Reckoning) reaches `SHARE_CELLS` grown by the
  blast radius bonus, and only enemies the ledger still watches. A share tops the receiver up to
  what the carrier has and never adds on top, so two carriers sharing back and forth settle
  instead of climbing.
- A Splash's mines and a Hexer's cursed clouds are zones it makes (`Zone.mine`, `Zone.cloud`); a mine
  blasts through the same `blast` a shot does, centred where it lay, with the enemy that stepped on it as
  the primary, and the Splash keeps its own list of mines to replace the oldest and to take them off with it.
