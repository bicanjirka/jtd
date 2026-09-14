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

### Wave-entry spawn delay is a hardcoded constant

`AbstractEnemyMob.doInit()` converts an enemy's `delay` (its position within a wave) to tick-count via
`Math.round(22.4f * delay / this.speed)`. The `22.4f` is a magic constant with no way to override it per-wave.

- **Where:** `AbstractEnemyMob.doInit()`
- **Approach:** add a `delay`-scaling field to `WaveDefinition` (or `Wave`) that defaults to `22.4f`, and extend the wave
  mini-language (see `WaveScript.parse`'s `c`/`e`/`t`/... token grammar) with a token — e.g. a `w<number>` prefix — that
  lets a wave definition override the spacing between spawns before listing enemies.

## Levels

### Only a Java-code level catalog exists

`LevelCatalog` is the abstraction levels are meant to be sourced through, but `BuiltInLevelCatalog` (levels defined as
Java code, e.g. `LevelDefinition` constants built from a hand-authored `List<Point>` of corners) is the only
implementation. There is no way to add or edit a level without a code change and a rebuild.

- **Where:** `td.level.LevelCatalog`, `td.level.BuiltInLevelCatalog`
- **Approach:** add a `FileLevelCatalog implements LevelCatalog` that parses level files (format TBD — JSON is the
  obvious choice given `LevelDefinition`'s shape) from a resources or config directory into `LevelDefinition`s. A
  malformed level file should throw `td.util.GameStartupException`, which `Main` already catches as the one
  fatal-startup boundary, rather than adding a second error-handling path.

## Tower features

### Upgrade-path numbers are unbalanced placeholders

The 8 upgrade paths (`TowerOne`/`TowerTwo`/`TowerThree`/`TowerFour`, two each) all have real prices and stat bonuses,
but none of them have been played against actual waves — the numbers were chosen to be plausible, not tuned. The
`ClusterCondition`/`DamageDealtCondition`/`KillCountCondition` thresholds are similarly unverified guesses at what a
reasonable mid-level of investment looks like.

- **Where:** the `private static final UpgradePath` constants in `TowerOne`, `TowerTwo`, `TowerThree`, `TowerFour`.
- **Approach:** play each of the built-in levels with every path chosen at least once, and adjust price/stat-bonus/
  condition-threshold values until each path feels like a meaningful, roughly-comparable-in-power choice rather than
  a strictly-better-or-worse one. No code or architecture change needed — every number here is already a named
  constant, not embedded in logic.

### Cinder's flame cone has no visual effect yet

`TowerCinder`'s wedge of damage has no on-board visual at all - `TowerEffectFrameBuilder.visitTowerCinder` returns
`null` rather than drawing anything. This is a sequencing gap, not a design decision: a cone needs a new
`TowerEffectDraw` record (a `ConeDraw`, alongside `BeamDraw`/`SplashDraw`/`PulseDraw`/`AuraDraw`) that doesn't exist
yet, and lands in the same rendering pass as the new `ProjectileDraw` hierarchy for `TowerMortar`'s shells and
`TowerSeeker`'s missiles - none of the three new towers' attacks are visible on the board until that pass.

- **Where:** `td.ui.TowerEffectFrameBuilder.visitTowerCinder`
- **Approach:** add `ConeDraw(Palette, float originX, float originY, float headingRadians, float radius, float
  arcRadians, float alpha)` to `td.ui.render.TowerEffectDraw`'s `permits` clause, add its case to
  `Java2DFrameRenderer.paintTowerEffect`, and emit it here using `tower.getTurretAim().currentRadians()` (the same
  heading `InWedgeTargetQuery` already decides hits against) and the tower's own half-width/range constants.

### New tower numbers are unbalanced placeholders

`TowerMortar`, `TowerSeeker` and `TowerCinder`'s price, damage, range, cooldown, splash radius, and slow/freeze/burn
magnitudes and durations were chosen to be plausible, not tuned - the same situation the upgrade-path numbers above
were in before their own balance pass.

- **Where:** the `public static final` constants and effect-duration constants in `TowerMortar`, `TowerSeeker`,
  `TowerCinder`.
- **Approach:** play each of the built-in levels with all three new towers, and adjust values until each feels like a
  meaningful, roughly-comparable-in-power choice next to the existing four attack towers. No code or architecture
  change needed - every number here is already a named constant.

### Rotating tower sprites

Towers don't rotate their sprite image to visually face their current target (enemy mobs already do this via
`AbstractEnemyMobDirectional`/`AbstractEnemyMobRotor`). This was a speculative "nice to have," not a committed design.

- **Where:** `td.ui.TowerSpriteFrameBuilder`, `td.ui.render.TowerSpriteDraw`
- **Approach:** if pursued, reuse the facing-angle approach already implemented for directional/rotor enemies
  (`AbstractEnemyMobRotor.getFacingRadians()`/`AbstractEnemyMobDirectional.getFacingRadians()`); needs per-tower
  "facing" state updated whenever a tower picks a new target (`TowerOne.findEnemy()`, `TowerTwo`'s find methods,
  etc.) and exposed as a getter, then threaded through as a new `facingRadians` field on `TowerSpriteDraw` (currently
  unused by towers - `EnemyBodyDraw`/`EnemyFadeDraw` already carry one) and applied as a rotation in
  `Java2DFrameRenderer.paintTowerSprite()` alongside rotated sprite art for each tower.
