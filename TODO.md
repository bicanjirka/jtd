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

### New tower numbers are unbalanced placeholders

`TowerMortar`, `TowerSeeker` and `TowerCinder`'s price, damage, range, cooldown, splash radius, and slow/freeze/burn
magnitudes and durations were chosen to be plausible, not tuned - the same situation the upgrade-path numbers above
were in before their own balance pass. The same is true of their own 6 upgrade paths (2 each): prices, stat bonuses
and condition thresholds are equally unverified guesses.

- **Where:** the `public static final` constants and effect-duration fields in `TowerMortar`, `TowerSeeker`,
  `TowerCinder`, and the `private static final UpgradePath` constants in each.
- **Approach:** play each of the built-in levels with all three new towers (and each of their upgrade paths chosen at
  least once), and adjust values until each feels like a meaningful, roughly-comparable-in-power choice next to the
  existing four attack towers and their own paths. No code or architecture change needed - every number here is
  already a named constant, not embedded in logic.

## Damage types

### Acid is not implemented as a second damage-over-time effect

The original damage-types request named acid alongside burn as a second damage-over-time effect; v1 shipped only
slow, burn and freeze (see `FEATURE-damage-types-and-projectiles.md`'s Decisions and V1 Scope), deferring acid rather
than dropping it.

- **Where:** `td.effect` (`EffectKind`, `Effect`) has no `ACID` case; nothing produces one.
- **Approach:** first settle the feature doc's open question — is acid meant to be mechanically distinct from burn
  (different scaling, a different interaction with a future armor/shield trait) or primarily a different visual on
  the same damage-over-time mechanism? Only then add an `EffectKind.ACID` case and an `Effect.acid(...)` factory,
  mirroring `Effect.burn(...)`.

### Critical damage is not implemented

The original request listed critical (chance-based bonus damage) as a post-hit effect alongside slow/burn/freeze; a
product-review pass concluded it isn't mechanically one — slow/burn/freeze happen to the enemy after a hit lands, but
a critical hit is a pre-hit, chance-based multiplier on the attacker's own roll — and recommended modeling it as a
tower stat instead. That recommendation was accepted but never built.

- **Where:** no code yet — `td.tower.AbstractTower` has no crit chance/multiplier of any kind.
- **Approach:** add a chance/multiplier pair (base fields on `AbstractTower`, or per-leaf like `TowerTwo`'s
  `spreadRadius`) rolled at the point `dealDamage` is called, scaling the `Damage` passed in before the enemy ever
  sees it. Deliberately **not** a `td.effect.Effect` — a crit is resolved once, at the moment of the hit, not applied
  to the enemy afterward the way a status effect is.

### Damage-type resistance doesn't exist yet

Every hit carries a `DamageType` (`PHYSICAL`/`MAGIC`), but no enemy differentiates by it. The
enemy-traits feature has since landed the mechanism this was waiting on - `Trait.onHit(Damage,
TraitContext)` already receives the incoming `Damage` (type included) and is free to branch on
`incoming.type()` - but no concrete trait actually does: `PercentResistTrait`/`FlatResistTrait`
(Square's and the Warden's) both reduce every hit uniformly regardless of type. A magic shield
or a physical shield (the original request's examples) is now a small, self-contained addition
- a new `Trait` implementation, not a `Damage`/`absorb` change - rather than infrastructure
work, which is what this entry used to track.

- **Where:** a new `td.enemy.Trait` implementation (no existing file needs to change).
- **Approach:** e.g. `DamageTypeResistTrait(DamageType resisted, float fraction)` whose
  `onHit` scales `incoming` only when `incoming.type() == resisted`, passing everything else
  through unchanged - mirrors `PercentResistTrait`'s shape closely enough to copy its pattern
  directly. Needs a concrete enemy to carry it (none of the four built-ins or the Warden chain
  do) before it's provably exercised, not just compiled.

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

## Enemy features

### The Warden boss shares Square's exact art

`BuiltInEnemies.WARDEN_1`/`WARDEN_2`/`WARDEN_3` reuse `BodyArchetype.SQUARE`, so a Warden is
the same pink flat square every regular Square enemy is - only a deliberately high `level`
parameter on its wave slot (`BuiltInLevelCatalog`'s `warden1` entry) makes it render larger,
via the existing level-scaled body-size formula every Square already has. That's a size-only
substitute for a real boss look, not actual distinct art, and risks the exact legibility
problem this feature's own product-review notes warned against (a player should be able to
tell at a glance that this is the boss, not just "a big Square").

- **Where:** `BuiltInEnemies.WARDEN_1`/`WARDEN_2`/`WARDEN_3`/`WARDEN_EGG_1`/`WARDEN_EGG_2`/
  `WARDEN_EGG_3`, `BuiltInLevelCatalog`'s `warden1` wave entry.
- **Approach:** give the Warden its own `BodyArchetype` (and the egg, if `EGG`'s shared
  circle-tint isn't distinct enough once seen next to Circle/Ghost in practice), following
  `td/enemy/CLAUDE.md`'s "Adding a genuinely new `BodyArchetype`" checklist - a new `Palette`
  role plus its shape/colour cases in `Java2DFrameRenderer`. Drop the `level=10` workaround
  once real size/shape comes from the archetype itself rather than a borrowed formula.

### The effect-marker overflow indicator has no count

`EnemyFrameBuilder`'s marker row caps at 3 visible status-effect icons; a 4th+ simultaneous
effect collapses into one generic `Palette.STATUS_MARKER_OVERFLOW` marker rather than the "+N"
badge `FEATURE-enemy-traits-and-effects.md`'s V1 Scope originally called for. The render frame
model has no text-drawing primitive today (every draw command is a coloured `Shape`), so
showing an actual number would be new render infrastructure, not a tweak to this one marker.

- **Where:** `td.ui.EnemyFrameBuilder.markers()`, `td.ui.render.Palette.STATUS_MARKER_OVERFLOW`.
- **Approach:** add a small text-drawing `RenderFrame` primitive (a `Palette` role isn't enough
  on its own - it needs the string/number itself, so a new sealed draw record carrying the
  count) and a `Java2DFrameRenderer.drawString` case for it, then have the overflow marker
  carry `activeCount - MAX_VISIBLE_MARKERS` instead of being a fixed, countless glyph.

### Enemy traits/abilities numbers are unbalanced placeholders

Every number introduced by the data-driven enemy model - `PercentResistTrait`/
`HurtSpeedTrait`'s migrated Square/Triangle factors, `FlatResistTrait`'s flat reduction, and
the entire Warden/egg chain's health, price, ability intervals, shield percentages/radii and
`EGG_HATCH_DELAY_TICKS` - was chosen to be plausible, not tuned, the same situation the
tower-upgrade and new-tower-numbers entries above were in before their own balance passes.

- **Where:** `BuiltInEnemies` (all trait/ability constants), `PercentResistTrait`,
  `HurtSpeedTrait`, `FlatResistTrait`.
- **Approach:** play Classic Loop through to the Warden encounter (and the other two levels,
  once they get their own late-game content) repeatedly, adjusting values until the chain and
  the migrated traits feel meaningfully tuned rather than placeholder guesses - no code or
  architecture change needed, every number here is already a named constant.
