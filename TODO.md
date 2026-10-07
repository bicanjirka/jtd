# Notes written by hand, to be processed later - write nicer detailed TODO entries from it

I would like to add feature ideas to this todo as well to have all possible work idead at one place.

# Known gaps and future work

Extracted from inline `TODO` comments (and one unmarked-but-real gap) found throughout the codebase during a
documentation cleanup pass. Each item below replaces the original comment; the source no longer carries these notes, so
this file is the single place to look for outstanding design/feature gaps.

## Feature request implementation order

The tower rework, in this order (each needs the ones before it):

1. `FEATURE-tower-progression.md` (implemented): Attune -> Awaken -> Transcendent, level IV,
   the extra head node, two special slots, the exclusive-choice mark, price rules.
2. `FEATURE-xp-and-purpose-gates.md` (implemented): XP from bounty and one purpose gate per tower
   replace kill and damage gates.
3. `FEATURE-sniper-and-sonar.md` (implemented): crit per tower, the hit / periodic rule, the
   first debuffs and the effect rules every tower follows.
4. `FEATURE-splash-stormcaller-and-hexer.md` (implemented): Dazed and the hex pool.
5. `FEATURE-pulse-and-seeker.md` (implemented): Silenced, Anchored, Unraveled, Brittle, the nest.
6. `FEATURE-ground-zones-mortar-and-cinder.md` (implemented): ground zones, the Mortar and the Cinder, and
   the fire-and-ice rules.
7. `FEATURE-aura-and-balance-pass.md` (implemented, except the repricing): the Aura, harness pricing, display
   names, the README.

Enemies come after, from `docs/ideas/enemies-brainstorm.md`.

Every earlier feature request has shipped (see each doc's own status line) except the draft
`FEATURE-effect-interactions.md`, whose scope has landed in part (vulnerable, poison, revealed,
freeze-burn, universal freeze diminishing returns) and is closed out in its own status line.

Implemented, for reference: `FEATURE-enemy-spawn-types.md`, `FEATURE-multiple-enemy-paths.md`,
`FEATURE-playtesting-and-balance-tooling.md`, `FEATURE-enemy-rank-system.md`,
`FEATURE-effect-visuals.md`, `FEATURE-tower-upgrades.md`, `FEATURE-critical-damage.md`,
`FEATURE-damage-types-and-projectiles.md`, `FEATURE-enemy-traits-and-effects.md`,
`FEATURE-wave-preview-cleanup.md`, `FEATURE-effect-diminishing-returns.md`,
`FEATURE-tower-upgrade-trees.md`.

## Architecture and correctness

Findings from the architecture audits of 2026-09-17, highest-severity first. The threading
group and the external audit's critical/high/moderate findings have landed; what remains is
listed below.

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

`CLAUDE.md`'s code style asks for interfaces of one to five methods. `Tower` has twenty-three:
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

### `EnemyMob`/`Tower`/`Projectile` use Visitor, not a sealed type + switch

Sealing would drop the `accept()` methods and the visitor interfaces, but `permits` cannot name
test classes, so the hand-written `EnemyMob` and `AbstractTower` fakes would stop compiling.

- **Where:** `EnemyMobVisitor`, `TowerVisitor`, `ProjectileVisitor` and their `td.ui` frame
  builders.
- **Approach:** only if the fakes go away first (tests built on real mobs/towers from
  `td.fixtures`). `Projectile` has no fakes but alone would leave two dispatch styles.

## Movement / pathing

### Wave-entry spawn delay is a hardcoded constant

`SpawnParameters.DELAY_TICKS_PER_SLOT` (a `private static final float`, consumed inside
`SpawnParameters.atSlot(...)`/`.of(...)`) converts an enemy's slot position within a wave to a
tick-count countdown via `Math.round(DELAY_TICKS_PER_SLOT * slotPosition / speed)`. The `22.4f`
value is a magic constant with no way to override it per-wave.

- **Where:** `td.enemy.SpawnParameters`
- **Approach:** add a `delay`-scaling field to `WaveDefinition` (or `Wave`) that defaults to `22.4f`, and extend the
  wave
  mini-language (see `WaveScript.parse`'s spawn-shape/spacer token grammar — `td/wave/CLAUDE.md`) with a token — e.g. a
  `w<number>` prefix — that
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

### The Pulse's Mirror Field and Soul Drain only approximate the feature doc

`FEATURE-pulse-and-seeker.md` has Mirror Field return "damage a shield absorbs inside" from every
source, and Soul Drain at -100 spirit keep "stacked debuffs from ever wearing off". As built,
Mirror Field mirrors only the Pulse's own ticks, as a second hit the shield takes its share of
again, and Soul Drain's spirit slows debuff timers to the floor of the spirit pace (stack debuffs
stop, timed ones crawl at a quarter pace, less under Toll).

- **Where:** `PulseTower.hitWithField` (Mirror Field), `ActiveEffects.tick` and its
  `MIN_DEBUFF_PACE` (Soul Drain).
- **Approach:** for Mirror Field, have `HitResolution` report the amount a shield took and let a
  tower subscribe to it for the enemies inside; for Soul Drain, let a pace of zero stop a timed
  debuff when spirit is at its floor, which needs the README's pace rule changed with it.

### Fallout keeps shields out as well as heals

`FEATURE-ground-zones-mortar-and-cinder.md` has Fallout drain spirit and block healing. As built it holds
heals *and* shields off, by the Dead Zone's effect, so the inspector names it a dead zone.

- **Where:** `td.zone.ZoneEffects` (the `FALLOUT` case), `td.effect.EffectInteractions`' held-off table.
- **Approach:** give Fallout an effect kind of its own that holds off `HEAL` only.

### The Cinder's Stoke is not drawn

The feature doc draws each Stoke step as a brighter core in the flame; only the wave's colour shows its
look, and the info rows give the steps' size.

- **Where:** `td.ui.TowerEffectFrameBuilder.visitCinderTower`, `td.ui.render.ConeDraw`.
- **Approach:** carry the steps the enemy nearest the wave carries, or the wave's best, on the wave and
  paint a brighter inner band of the cone for each.

### A cursed cloud holds its debuffs instead of letting them run down

A Hexer's cloud gives what stands in it the debuffs the dead enemy had, with the time they had left when
it died, on every pulse, so they stay as long as an enemy does and run out only after it leaves.

- **Where:** `td.zone.Zone.touch` and the carried effects of `Zone.cloud`.
- **Approach:** have the zone count its carried effects' time down with its own age, so a debuff on an
  enemy that stays still runs out.

### Enemies inside the Pulse's field don't flicker

The feature doc draws the enemies inside a field flickering; only the field's rings, coloured by
its rules, are drawn.

- **Where:** `td.ui.EnemyFrameBuilder`, driven by a field effect on the enemy (`CORRODED`, `UNDERTOW`,
  `SILENCED` or `KILL_ZONE`).
- **Approach:** add an enemy overlay draw that alternates the body's opacity while one of those
  effects is active, so it needs no state beyond the effect.

### A global, buy-once upgrade for a whole tower type doesn't exist

`FEATURE-tower-upgrades.md` named a second kind of upgrade alongside the four per-instance ones
that shipped: bought once, it retroactively applies to every tower of a type already on the
field and to every one built afterward. Deferred because nothing today reaches back into an
already-built tower to change its base behavior, and nothing persists a per-type flag across a
tower purchase — both real, new mechanisms, not a variation on the per-instance model.

- **Where:** a new field on `GameWorld` (or wherever per-level persistent state would live),
  consulted by `TowerFactory` at construction and by something that re-walks `TowerRoster.all()`
  at purchase time.
- **Approach:** see `FEATURE-tower-upgrades.md`'s "Architectural implications" section for the
  concrete blockers (level-teardown/reload correctness in particular — a purchased global
  upgrade must not leak into the next level or the next run, the same discipline
  `TowerRoster.clear()`/`EnemyRoster.clear()` already follow). Settle where that persistent state
  lives before writing the retroactive-apply logic.

### Bounty for any kill inside an aura, not just the aura tower's own kills

Also named and deferred in `FEATURE-tower-upgrades.md`: an aura that pays out bonus bounty for
*any* kill scored inside its radius, not only the aura-owning tower's own. `EconomyDelta.kill`
is computed once, inside the dying enemy's own kill path, with no notion of which tower killed
it or which auras cover that location — a new coupling between `td.enemy` and `td.tower` that
the codebase has deliberately avoided so far (towers query enemies, never the reverse).

- **Where:** `DefinedEnemyMob`'s kill/death path (`td.enemy`), `EconomyDelta.kill` (`td.economy`).
- **Approach:** see `FEATURE-tower-upgrades.md`'s "Architectural implications" and "Risks and
  costs" sections. Either pass the killing tower's position outward from the death path so an
  aura-owning tower can be consulted, or add a new aura-query step in the kill path — both are a
  real, new cross-package coupling and should be designed deliberately, not bolted on.

### Upgrade-tree node numbers are unbalanced placeholders

Every tower's `UpgradeNode`s carry real prices and stat bonuses, but none of them have been played
against actual waves. Prices start from `UpgradeTier`'s multiples of the tower's list price (the table in
`docs/features/FEATURE-tower-progression.md`); the stat bonuses still follow the placeholder rule of the
first trees (an unquantified "+X" in the source doc became +25%, an unquantified crit bonus +10%). The
`ClusterCondition` thresholds are unverified guesses, and `UpgradeTier`'s XP table is checked only
against the bounty arithmetic in `docs/features/FEATURE-xp-and-purpose-gates.md`, not against play.
Feature 7's balance pass sets the final numbers.

`td.PurchaseHarness` (Twisted Hourglass, 12 seeds, an army of one of each tower) now measures them: lives
saved per 100 credits. Another copy of a tower is the best buy for every tower but the Aura and the Hive (a
Sniper copy saves 24 lives per 100 credits for $17), and an upgrade node saves between 0.02x and 0.6x of that
(Focused Optics III 0.24x, Railgun 0.17x, Spotter Uplink 0.02x). Nodes that act on enemies rather than on
damage (Hollow Point, Shatter Shot, Fifth Shot) save nothing within the noise of about one life. The harness
puts no limit on cells, so a copy is never refused for lack of room, which is what the 0.7x to 1x target in
`FEATURE-aura-and-balance-pass.md` assumes. Do not reprice from these numbers alone: decide first how a copy
is limited (cells, or the +15% surcharge) and how effect nodes are measured.

- **Where:** `UpgradeTier`'s price multiples, and the `private static final UpgradeNode` constants in every
  leaf under `td.tower` (`SniperTower`/`SplashTower`/`SonarTower`/`PulseTower`/`MortarTower`/`SeekerTower`/
  `CinderTower`/`AuraTower`).
- **Approach:** play each of the built-in levels with every node bought at least once, and adjust price/stat-bonus/
  condition-threshold values until each node feels like a meaningful, roughly-comparable-in-power choice rather than
  a strictly-better-or-worse one. No code or architecture change needed — every number here is already a named
  constant, not embedded in logic. `td.BalanceHarness` and the `n`/`x`/`c` debug keybindings (see `docs/ARCHITECTURE.md` section 9) now make this cheap to actually do.

### The Mortar's, Cinder's and zones' numbers are unbalanced placeholders

The Mortar's and Cinder's prices, damage, cooldowns, radii and node bonuses come from
`FEATURE-ground-zones-mortar-and-cinder.md`, and the numbers that doc leaves open were chosen to be
plausible, not tuned: a zone's radius, length and pulse (twice a second), tar's 40% slow and poison
share, frost's chill and the 2 s before it freezes, fallout's Sickened stacks, napalm's and tar's damage
share of the Mortar, how much shrapnel, bomblets and bleeding do, the nuke's flash, Lingering Flames'
patch, the burn level above which a fire reveals an invisible enemy, and the Cinder's Thermal Shock
chill. Nothing has been played against waves, and the Mortar's slow shell is untested against fast
enemies.

- **Where:** the `private static final` constants in `MortarTower`, `CinderTower`, `td.zone.Zone`,
  `td.zone.ZoneEffects`, `td.tower.mortar.ShellType` and the perks under `td.tower.mortar` and
  `td.tower.cinder`, and `ActiveEffects.BURN_REVEAL_FUEL`.
- **Approach:** tune through play or `td.BalanceHarness` with the other placeholder entries. Check the
  per-tick budget (`td.PerformanceHarness`) with a Barrage of Napalm shells and Lingering Flames on every
  wave, since the zone roster has no cap and the doc leaves one to the implementation.

### Effect and specialization numbers are unbalanced placeholders

Every number the effect-interactions and specialization work introduced was chosen to be plausible,
not tuned: vulnerable (+15% per stack, cap 3, 4 s), the chill cap (80%), how far a chill cuts a burn (half
at the cap), poison's damage (10% of weapon damage per tick, 4 s), its slow (up to 30%) and the half-second
stack interval of burning and poisoned, revealed (3 s from Wide Band, 2 s from Resonant Field II),
Momentum's cooldown cut and 5 s, the shatter and explosion share and radius, shrapnel's ring width and
share, Warding Field's 10% chance, Withering Field's interval, Long Reach II's scaling, and Splash's three
blasts. Burning's resilience loss (-1 per stack) and poisoned's spirit loss (-1 per stack) grow without
limit up to their stat floors of -100. Scorched and Sickened lose a stack per second at neutral spirit
(one per 20 ticks, `ActiveEffects.STACK_DECAY_INTERVAL_TICKS`), so steady burning nets one stack per
second and needs about 100 s to reach the floor; at -100 spirit they never wear off.

**Not as designed - to iterate on:** stack decay currently runs all the time, including while a burn or
poison is still earning stacks. A burning enemy earns 2 stacks per second and loses 1, so the net gain is 1
per second. That is a side effect of the first implementation, not the intended design, and the behaviour
wanted here is still to be settled: for example decay paused while the pool is active, decay only once it has
ended, or a different rate. Sickened lowering spirit, which slows the decay of its own stacks and of Scorched,
interacts with whichever is chosen.

- **Where:** the named constants in `td.effect.ActiveEffects`, `AbstractTower` and each tower.
- **Approach:** tune through play or `td.BalanceHarness` with the other placeholder entries; decide
  whether a long burn's resilience loss (crit damage up to double at the floor) is too strong.

## Damage types

### Acid is not implemented as a second damage-over-time effect

The original damage-types request named acid alongside burn as a second damage-over-time effect; v1 shipped only
slow, burn and freeze (see `docs/features/FEATURE-damage-types-and-projectiles.md`'s Decisions and V1 Scope), deferring
acid rather
than dropping it.

- **Where:** `td.effect` (`EffectKind`, `Effect`) has no `ACID` case; nothing produces one.
- **Approach:** first settle the feature doc's open question — is acid meant to be mechanically distinct from burn
  (different scaling, a different interaction with a future armor/shield trait) or primarily a different visual on
  the same damage-over-time mechanism? Only then add an `EffectKind.ACID` case and an `Effect.acid(...)` factory,
  mirroring `Effect.burn(...)`.

### A typed shield has no concrete user yet

`ShieldTemplate`/`Effect`/`ActiveEffects.contributeTo` can all restrict a shield to one `DamageType`
(`ShieldTemplate.physicalOnly`/`.magicOnly`), mirroring the same restriction `PercentResistTrait`/
`FlatResistTrait` already carry - but no built-in ability actually authors one. This has precedent:
`td/effect/CLAUDE.md` already records that `SLOW`/`BURN`/`FREEZE` ship with no `EffectTemplate`
authoring them either.

- **Where:** `td.effect.ShieldTemplate.physicalOnly`/`.magicOnly`, `ActiveEffects.contributeTo`.
- **Approach:** give a concrete enemy ability a typed shield so it is play-verified, not just
  unit-tested - the Warden's `reshield`/`callToArms` (`BuiltInEnemies.WARDEN_STANDING_ABILITIES`)
  are the obvious carriers.

### Rotating tower sprites

Towers don't rotate their sprite image to visually face their current target (enemy mobs already
do this — see below). This was a speculative "nice to have," not a committed design.

- **Where:** `td.ui.TowerSpriteFrameBuilder`, `td.ui.render.TowerSpriteDraw`
- **Approach:** if pursued, note the facing-angle precedent on the enemy side no longer looks the
  way this entry originally described: mob movement is composed via `MovementBehavior`
  implementations (`FixedMovement`/`PulseMovement`/`PathDirectionalMovement`/`RotorMovement`,
  `td.enemy`), and the current facing logic is `DefinedEnemyMob.getFacingRadians()` — a `switch`
  over `this.definition.movement()`'s sealed type. Towers have no equivalent composed-movement
  model, so there's no direct mechanism to reuse from there; a tower's facing would instead need
  to be derived from its own chosen target, per tower. There's no shared targeting hook either -
  targeting is composed per-tower via `td.tower.targeting` pieces (e.g. the Sniper folds its perks
  into a `SniperSpec` whose `Reach` and aim pick the target; see `td/tower/CLAUDE.md`'s
  "Targeting" section) — so
  this needs new per-tower "facing" state updated wherever each tower's `doTick` calls its
  selector, exposed as a getter, then threaded through as a new `facingRadians` field on
  `TowerSpriteDraw` (currently absent — `EnemyBodyDraw`/`EnemyFadeDraw` already carry one) and
  applied as a rotation in `Java2DFrameRenderer.paintTowerSprite()` alongside rotated sprite art
  for each tower.

## Enemy features

### Enemy traits/abilities numbers are unbalanced placeholders

Every number introduced by the data-driven enemy model - `PercentResistTrait`/
`HurtSpeedTrait`'s migrated Square/Triangle factors, and the Warden/egg chain's health, price,
ability intervals, shield percentages/radii and `EGG_HATCH_DELAY_TICKS` - was chosen to be
plausible, not tuned, the same situation the tower-upgrade and new-tower-numbers entries above
were in before their own balance passes. **One exception:** the Warden's `FlatResistTrait`
value has been tuned (15 -> `BuiltInEnemies.WARDEN_FLAT_RESIST` = 200) against the actual
per-hit/per-tick damage scale every attack tower deals (150-4000, see the head-to-head data in
the new-tower-numbers entry above) - 15 was negligible against any of them (0.375%-10% of a
single hit), making the Warden's armor mechanically inert regardless of which tower fought it.

- **Where:** `BuiltInEnemies` (all trait/ability constants), `PercentResistTrait`,
  `HurtSpeedTrait`, `FlatResistTrait`.
- **Approach:** play Curly Path through to the Warden encounter (and the other two levels,
  once they get their own late-game content) repeatedly, adjusting values until the chain and
  the migrated traits feel meaningfully tuned rather than placeholder guesses - no code or
  architecture change needed, every number here is already a named constant. `td.BalanceHarness`
  and the `n`/`x`/`c` debug keybindings (see `docs/ARCHITECTURE.md` section 9) now make this cheap to actually do - `x` specifically can spawn the Warden chain's
  stages on demand without playing to wave 18 first.

### Critical-damage numbers are unbalanced placeholders

Every tower crits for x1.5 (`AttackProfile.DEFAULT_CRIT_MULTIPLIER`) except the Sniper, born with
5% at x2.0. Crit damage bonuses add to the multiplier, so the Sniper's crit reaches about x3.0
(Tradecraft's streak +0.5, the Aura's Keen Edge +0.5 once feature 7 lands), and Momentum
multiplies a charged shot by 5 on top: about x15 in one hit. The Warden's on-crit-survived shield
(30% for 100 ticks) is from the first crit pass. On `td.BalanceHarness`'s default loadout (two
Snipers and a Splash on Curly Path, 5000 ticks) the reworked Sniper clears 5 waves where the old
one cleared 4, its lead copy dealing 194.2k damage for 22 kills against 158.3k for 20.

- **Where:** `AttackProfile.DEFAULT_CRIT_MULTIPLIER`, `SniperTower.CRIT_CHANCE` and
  `CRIT_MULTIPLIER`, `MomentumPerk.DAMAGE_FACTOR`, `CritStreakPerk`, and
  `BuiltInEnemies.WARDEN_STANDING_ABILITIES`'s `OnCriticalHitTakenTrigger` ability.
- **Approach:** tune in the feature 7 balance pass, with Keen Edge in play. Try Momentum at x3
  first; every number here is a named constant.

## UI

### A hatched egg leaves its stale stats in the inspector

Selecting a Warden egg and letting it hatch clears the selection ring, but the panel keeps the
egg's last stats with no status line, unlike a kill (`Killed`) or a leak (`Leaked`).

- **Where:** `td.enemy.EnemyInspection.Fate`, the hatch path in `SpawnEnemiesAction`.
- **Approach:** give a mob replaced by its own spawn a fate (e.g. `Hatched`) so the inspector
  says why it stopped updating.

### A gated node's name is cut short by its gate's progress

An offered node that waits on a purpose gate shows the gate's progress where its price goes
("Steady Aim shots 0/20"), and at the panel's default width that text crowds the node's own name
down to "Focu…". Short gates ("Pings 0/25", "0/2 nearby towers") fit.

- **Where:** `td.ui.PanelUpgradeTree`'s offer buttons, `UpgradeSheetText`'s gate label.
- **Approach:** keep the name whole and let the right-hand text give way first (truncate or
  wrap it onto a second line), or shorten long deed names to fit.
