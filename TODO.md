# Known gaps and future work

Extracted from inline `TODO` comments (and one unmarked-but-real gap) found throughout the codebase during a
documentation cleanup pass. Each item below replaces the original comment; the source no longer carries these notes, so
this file is the single place to look for outstanding design/feature gaps.

## Gameplay / balance

### Zero-price wave penalty is a placeholder

When an enemy whose `price` is `0` reaches the end of the path, `AbstractEnemyMob.doTick()` docks a flat 10 points from
the score instead of the enemy's price (since 0 would dock nothing). That `10` was never a designed value — just a
stand-in.

- **Where:** `AbstractEnemyMob.doTick()`
- **Approach:** either give price-0 "filler" enemies (see `EnemyFactory.Enemy.Empty`) an explicit configurable penalty,
  or decide that reaching the end with a price-0 enemy shouldn't penalize the player at all and drop the branch.

### Ghost health doesn't scale with level

`EnemyMobGhost.doInit()` always divides incoming health by a flat `5`, unlike its body size (`bodyScale`), which already
scales with `level`. Health reduction should probably scale the same way so ghosts stay balanced at higher levels.

- **Where:** `EnemyMobGhost.doInit()`
- **Approach:** replace the flat `/5` with a level-scaled divisor, mirroring the `(this.level < 6) ? (7 - level) : (2)`
  pattern already used for `bodyScale` in the same method (or a deliberately different curve, if `5` was chosen for a
  reason that's no longer documented — worth play-testing either way).

## Movement / pathing

### Diagonal movement needs distance-based progression

Enemy movement (`AbstractEnemyMob.segmentProgression`) is a fixed-point counter from 0–999 representing "how far along
the current path segment," incremented by `speed` each tick. This only works cleanly for axis-aligned segments; the
original author's own note flagged that switching to a real float-based, distance-normalized progression (to support
diagonal path segments) raises a speed-scaling problem: segments of different lengths would need `speed` to be
interpreted as a real per-tick distance rather than "1/1000th of whatever segment we're on," which changes how tower
range and enemy speed balance interact.

- **Where:** `AbstractEnemyMob` (`segment`, `segmentProgression`, `resetPosition()`, `doTick()`), `PathNormal`/`Path`
- **Approach:** this is a small architectural change, not a one-line fix. Suggest prototyping it against a single
  diagonal test path first, and re-tuning enemy `speed` constants (`speedBase`/`speedMax` in each `EnemyMob*` subclass)
  afterward since their current values are tuned for the fixed-point/axis-aligned model.

### Wave-entry spawn delay is a hardcoded constant

`AbstractEnemyMob.doInit()` converts an enemy's `delay` (its position within a wave) to tick-count via
`Math.round(700f * delay / this.speed)`. The `700f` is a magic constant with no way to override it per-wave.

- **Where:** `AbstractEnemyMob.doInit()`
- **Approach:** add a `delay`-scaling field to `Context` (or `Wave`) that defaults to `700f`, and extend the wave
  mini-language (see `Wave.finalise()`'s `c`/`e`/`t`/... token grammar) with a token — e.g. a `w<number>` prefix — that
  lets a wave definition override the spacing between spawns before listing enemies.

## Tower features

### No per-tower kill/damage stats

Towers have no memory of how much damage they've dealt or how many kills they've gotten — there's no way to show "this
tower has killed 40 enemies" in the UI.

- **Where:** `AbstractTower` and subclasses (`TowerOne`..`TowerFour`, `TowerUpgrade`)
- **Approach:** add `damageDealt`/`killCount` fields to `AbstractTower`, increment them at each `EnemyMob.doDamage()`
  call site inside the tower subclasses (each subclass already knows when it lands a hit), and surface the totals via
  `getStatusString()`.

### Tower upgrade doesn't gate on affordability

`PanelTowerInfo.moneyChanged()` currently just refreshes the displayed tower status text/price when money changes;
there's no logic checking or indicating whether the player can afford to upgrade the currently-selected tower (the
original comment on this method said it was "not used yet — here for future upgrade-affordability checks", but that
check was never implemented, and there's no "upgrade" action to gate in the first place).

- **Where:** `PanelTowerInfo.moneyChanged()`
- **Approach:** this depends on a real upgrade-purchase flow existing first (currently the only "upgrade" mechanic is
  placing a separate `TowerUpgrade` tower nearby, not upgrading an existing tower in place) — worth deciding whether
  in-place tower upgrades are even a wanted feature before building the affordability check.

### Rotating tower sprites

Towers don't rotate their sprite image to visually face their current target (enemy mobs already do this via
`AbstractEnemyMobDirectional`/`AbstractEnemyMobRotor`). This was a speculative "nice to have," not a committed design.

- **Where:** `td.ui.TowerSpritePainter`
- **Approach:** if pursued, reuse the facing-angle approach already implemented for directional/rotor enemies
  (`AbstractEnemyMobRotor.getFacingRadians()`/`AbstractEnemyMobDirectional.getFacingRadians()`); needs per-tower
  "facing" state updated whenever a tower picks a new target (`TowerOne.findEnemy()`, `TowerTwo`'s find methods,
  etc.) and exposed as a getter, then applied as a rotation in `TowerSpritePainter` alongside rotated sprite art for
  each tower.

### Fast-forward doesn't single-step while paused

`jButton_fastActionPerformed()` (the ">>" button) just calls `this.setSpeed(TickSpeed.FAST)` — it doesn't tick the
simulation forward even once if the game is currently paused. The underlying feature request (single-step the game one
tick while paused) was never finished.

- **Where:** `TowerDefence.jButton_fastActionPerformed()`, `TowerDefence.doGameTick()`
- **Approach:** `doGameTick()` no longer has a pause check of its own (pause is now `TickSpeed.PAUSED`, which `GameLoop`
  already skips calling `onTick` for), so the >> handler can just call `this.doGameTick()` directly, once, when
  `this.currentSpeed == TickSpeed.PAUSED`, to act as a manual single-step control.

## Rendering / engine

### Missing background image has no fallback

`TowerDefense`'s constructor only sets `this.backGround` if `Cache.hasBufImg("bg")` is true; the `else` branch is empty,
so if the background image ever fails to load, nothing is drawn there instead of a visible placeholder.

- **Where:** `TowerDefense` constructor, `BoardRenderer.paint()`
- **Approach:** draw a plain filled rectangle (matching the board's background color) as a fallback in
  `BoardRenderer.paint()` when the background image passed in is `null`, rather than leaving the constructor's `else`
  branch empty.

