# Known gaps and future work

Extracted from inline `TODO` comments (and one unmarked-but-real gap) found throughout the codebase during a
documentation cleanup pass. Each item below replaces the original comment; the source no longer carries these notes, so
this file is the single place to look for outstanding design/feature gaps.

## Architecture and correctness

Findings from the architecture audits of 2026-09-17, highest-severity first. The threading
group and the external audit's critical/high/moderate findings have landed; what remains is
listed below.

### No performance budget, because nothing has been measured

`CLAUDE.md` states no allocation or frame-time budget, and deliberately does not invent one: a
number picked without measuring is worse than none. What is known is only shape, not size - the
render pulse builds a whole `RenderFrame` (every cell, enemy, tower, projectile and path marker
as records) sixty times a second on the `game-loop` thread, and `EnemyRegistry.getEnemies()`
copies its array on every call, several times per frame plus once per tower per tick.

- **Where:** `td.TowerDefense.buildAndPublishFrame`, `td.ui.BoardRenderer.buildFrame`,
  `td.enemy.EnemyRoster.getEnemies`.
- **Approach:** measure first - p99 frame-build time and allocation per frame on the largest
  built-in level with a full board of towers, which `td.BalanceHarness` is already the right
  harness for. Set a budget from the measurement, then state it in `CLAUDE.md` with the command
  that checks it. Do not state a budget before there is a number behind it.

### A command queue would make the simulation a true single writer

Not a defect - an option, recorded with the condition that would make it worth taking.

Every mutation originating on the EDT (placing or selling a tower, choosing an upgrade path,
loading a level, requesting a wave) currently reaches the simulation by one of three ad-hoc
routes: a volatile flag `doTick` consumes (`requestNextWave`), direct mutation under a stopped
loop (`loadLevel`), or direct mutation while the loop runs (tower placement). Each is
individually safe, but there are three of them, and the third is why `AbstractTower`,
`CellNormal` and `GameWorld` all carry publication rules of their own.

The second route is now genuinely guaranteed rather than best-effort: `GameLoop.stop()` joins
without bound and `TowerDefense.stopLoopThen` runs it off the EDT, so `loadLevel` provably has
no tick in flight. The third is the one a queue would still replace.

A single `ConcurrentLinkedQueue<Runnable>` drained at the top of `doTick` would replace all
three: the simulation thread becomes the only writer, most of the volatile markers become
unnecessary, and every mutation lands on a deterministic tick boundary.

- **Where:** `td.GameEngine.doTick`, `td.TowerPlacement`, `td.TowerDefense`'s listeners.
- **Why not yet:** buying a tower deducts credits and reports success synchronously, and the
  UI uses that answer immediately. Through a queue, click feedback waits for the next tick -
  up to 50ms at normal speed, and unbounded while paused, where the queue never drains. That
  needs optimistic UI or a second synchronous path for purchases, which puts two mechanisms
  back.
- **The trigger:** deterministic replay. A recorded command log is what would let
  `td.BalanceHarness` produce runs that are comparable to each other and reproducible across
  machines. If that becomes a goal, this stops being indirection and starts being the feature
  - do it then, and not before.

### `Tower` is a twenty-three-method interface

`CLAUDE.md` §5 rule 6 asks for interfaces of one to five methods. `Tower` has twenty-three:
identity, position, rendering, selection, economy, damage accounting, upgrade paths and buff
contribution, all on one type. It is what every consumer depends on, so every consumer depends
on all of it — `PanelTowerInfo` wants the upgrade and accounting half, `BoardRenderer` wants
position and the visitor, `AuraTower` wants position and type.

- **Where:** `td.tower.Tower`, and its consumers in `td.ui` and `td.tower`.
- **Approach:** split along the lines the consumers already use rather than inventing a
  taxonomy — most likely a small `TowerView` (position, type, range, visitor) that rendering
  and targeting take, leaving the mutating lifecycle on `Tower`. Do it when a consumer is
  actually hurt by the width, not as a tidying exercise: a wide interface with one
  implementation hierarchy costs far less than a wrong split.

### A tower's class name and its in-game name describe different things

The classes are named for behaviour (`SniperTower`, `SplashTower`, `SonarTower`, `PulseTower`)
and the UI still calls them Triangle, Circle, Sunshine and Stardust — the shapes the player
sees on the board. `td/tower/CLAUDE.md`'s table maps the two, so nothing is ambiguous, but a
reader of `SniperTower.getInfoString` does meet the string `"Triangle tower"`.

- **Where:** each leaf's `getInfoString`/`getStatusString`, `TowerDefense.statusMessage`,
  `README.md`'s tower table.
- **Approach:** a product decision, not a refactor. Either rename the player-facing strings to
  match the behaviour (which arguably tells a player more than the shape they can already see),
  or keep the shape names deliberately and leave the table as the bridge. Do not change them
  halfway.

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
  constant, not embedded in logic. `td.BalanceHarness` and the `n`/`x`/`c` debug keybindings (see the root
  `CLAUDE.md`'s "Playtesting and balance tooling") now make this cheap to actually do.

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
  already a named constant, not embedded in logic. `td.BalanceHarness` and the `n`/`x`/`c` debug keybindings (see
  the root `CLAUDE.md`'s "Playtesting and balance tooling") now make this cheap to actually do.
- **Evidence gathered, not yet acted on:** a one-tower-vs-one-captive-target comparison (all 7 attack towers, same
  position/level/2000-tick budget, single very-tanky enemy so none of them run out of target) found raw damage-per-
  credit-spent of `first` 16000, `second` 6827, `third` 4000, `cinder` 4270, `fourth` 3520, `mortar` 2393, `seeker`
  1697 - `mortar`/`seeker` are the two weakest of all seven, including both pre-existing splash/AoE towers, even
  though this single-target setup already under-counts `mortar`'s splash and `cinder`'s ghost-hitting value and
  over-counts nothing in their favor. Some of that gap is `mortar`'s unguided shell missing a moving target rather
  than raw output, which this comparison can't separate out - worth a real multi-enemy/formation test before
  concluding `mortar`'s price or damage needs to move, not just this single-target number on its own.
- **Formation follow-up, also gathered, also not yet acted on:** ran the multi-enemy test the entry above called
  for - all 7 attack towers, same solo position/board, but this time a single wave of many ordinary (not tanky)
  Circles spaced by the wave mini-language's default (no-spacer) spawn delay, which packs them into a dense moving
  column rather than a lone captive target. Two tick-scale runs at the same density (40 Circles, one straight
  corridor, one tower defending alone, no other help - a deliberately harsh solo-defense scenario, harsher than any
  real level's multi-tower setup): at moderate hp (800, comparable to `BuiltInLevelCatalog`'s own mid-game waves)
  nobody could solo-clear the column in 5000 ticks, but raw damage dealt reordered the field completely - `third`
  81680, `fourth` 70400, `second` 57290, `first` 35200, `mortar` 30094, `cinder` 29641, `seeker` 2931 (dmg-per-
  credit, not comparable in magnitude to the captive-target numbers above since the scenario differs, only in
  relative order). `mortar` climbed from tied-worst to solidly mid-pack once splash actually had neighbors to hit,
  confirming the captive-target test under-sold it as suspected. `seeker` got dramatically *worse*, not better -
  roughly 10-30x behind every other tower, including `first`, its closest single-target-only relative. At a lower
  hp (150) where a solo tower can plausibly clear the whole column, `second`/`third`/`fourth`/`mortar`/`cinder` all
  fully cleared 40 Circles (`third`/`fourth` fastest at ~2400 ticks, needing the fewest leaks along the way);
  `first` only managed 7/40 kills and never cleared; `seeker` killed *zero* and never cleared, even when the same
  test was re-run against a much smaller column of just 10 - ruling out "the group was too big" as the explanation.
  The likely cause isn't `seeker`'s reliability (it never misses) but its raw throughput: at damage/cooldown =
  1800/60 = 30 per tick, it is the lowest-DPS attack tower in the game, well below even `first`'s 4000/39 ≈ 103 per
  tick, and unlike `mortar`, `second`, `third`, `fourth` or `cinder` it has no splash/sweep/continuous-AoE
  multiplier to make up the gap once more than one target needs killing per unit time - every one of the other six
  towers has *some* way to hit more than one enemy per action; `seeker` alone does not. (`seeker`'s own freeze
  effect reordering the "furthest along path" ranking after every hit - each just-hit target instantly falls out of
  the lead once frozen, so the tower never gets to land a second shot on an already-damaged target - is a plausible
  compounding factor worth a follow-up ablation, but the plain DPS gap above is already sufficient to explain the
  result on its own.) Net read: `mortar`'s numbers may not need to move much, since real gameplay routinely bunches
  same-type enemies (`"10 c"`, dense runs inside `"s t s c g c t c s g t c s g c t s g t c"`, etc.) where its splash
  already earns its keep; `seeker` looks like the tower that actually needs attention, either a straightforward
  damage/cooldown buff or - possibly a better fit given it's explicitly the guaranteed-hit, never-misses tower - a
  narrower intended role (e.g. a single tough priority target, boss-adjacent) rather than a general crowd-clear
  price point.
- **`seeker`'s damage/cooldown buffed, its role deliberately left alone:** decided to take the straightforward-buff
  direction above, not the reroll - `TowerSeeker.damage` 1800 -> 2600 and its `coolDownMax` 60 -> 45 (dmg/tick
  30 -> ~58), keeping guaranteed-hit reliability and freeze CC as its reason to exist rather than adding any
  splash/multi-target mechanic. Re-ran the same formation test against the buffed numbers: dmg-per-credit in the
  moderate-hp (800) 40-Circle scenario roughly doubled (2931 -> 5720), and it went from killing nothing at all
  against a 10-Circle column to landing one confirmed kill - a real improvement, but it's still the clear last place
  of all seven (`cinder`, the next-lowest, is still ~5x ahead, versus ~17x before the buff), because a pure
  single-target tower's dmg-per-credit in a *packed-formation* stress test specifically will never match one with
  any splash/sweep/continuous-AoE mechanic no matter how far its own numbers move - that gap is structural to the
  scenario, not a sign the buff was sized wrong. The formation test also confirmed the freeze-reordering mechanic
  flagged above as "plausible, unconfirmed": doubling `seeker`'s output didn't proportionally raise its kill count,
  because each hit still knocks its target out of the "furthest along path" lead it needs to be re-selected and
  finished off - only a targeting-behavior change (out of scope here; the ask was numbers only) would fix that, so
  a future pass could reconsider it if `seeker` still feels weak after this buff lands in real play.

## Damage types

### Acid is not implemented as a second damage-over-time effect

The original damage-types request named acid alongside burn as a second damage-over-time effect; v1 shipped only
slow, burn and freeze (see `docs/features/FEATURE-damage-types-and-projectiles.md`'s Decisions and V1 Scope), deferring acid rather
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
badge `docs/features/FEATURE-enemy-traits-and-effects.md`'s V1 Scope originally called for. The render frame
model has no text-drawing primitive today (every draw command is a coloured `Shape`), so
showing an actual number would be new render infrastructure, not a tweak to this one marker.

- **Where:** `td.ui.EnemyFrameBuilder.markers()`, `td.ui.render.Palette.STATUS_MARKER_OVERFLOW`.
- **Approach:** add a small text-drawing `RenderFrame` primitive (a `Palette` role isn't enough
  on its own - it needs the string/number itself, so a new sealed draw record carrying the
  count) and a `Java2DFrameRenderer.drawString` case for it, then have the overflow marker
  carry `activeCount - MAX_VISIBLE_MARKERS` instead of being a fixed, countless glyph.

### Enemy traits/abilities numbers are unbalanced placeholders

Every number introduced by the data-driven enemy model - `PercentResistTrait`/
`HurtSpeedTrait`'s migrated Square/Triangle factors, and the Warden/egg chain's health, price,
ability intervals, shield percentages/radii and `EGG_HATCH_DELAY_TICKS` - was chosen to be
plausible, not tuned, the same situation the tower-upgrade and new-tower-numbers entries above
were in before their own balance passes. **One exception:** the Warden's `FlatResistTrait`
value has been tuned (15 -> `BuiltInEnemies.WARDEN_FLAT_RESIST` = 100) against the actual
per-hit/per-tick damage scale every attack tower deals (150-4000, see the head-to-head data in
the new-tower-numbers entry above) - 15 was negligible against any of them (0.375%-10% of a
single hit), making the Warden's armor mechanically inert regardless of which tower fought it.

- **Where:** `BuiltInEnemies` (all trait/ability constants), `PercentResistTrait`,
  `HurtSpeedTrait`, `FlatResistTrait`.
- **Approach:** play Classic Loop through to the Warden encounter (and the other two levels,
  once they get their own late-game content) repeatedly, adjusting values until the chain and
  the migrated traits feel meaningfully tuned rather than placeholder guesses - no code or
  architecture change needed, every number here is already a named constant. `td.BalanceHarness`
  and the `n`/`x`/`c` debug keybindings (see the root `CLAUDE.md`'s "Playtesting and balance
  tooling") now make this cheap to actually do - `x` specifically can spawn the Warden chain's
  stages on demand without playing to wave 18 first.
