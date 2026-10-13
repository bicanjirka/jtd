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

`docs/plans/stat-driven-buffs.md` (phases 1 to 9) and `FEATURE-agility-and-dodge.md` are implemented.

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

`td.PurchaseHarness` measures them: lives saved per 100 credits, on Twisted Hourglass with 12 seeds. The +15%
per-copy surcharge is the limit on copies, and the harness now measures it at a realistic price. With an army of one
of each tower a copy is the best buy for all but the Aura and the Hive (a Sniper copy saves 27 lives per 100 credits
for $17; an upgrade node saves 0.02x to 0.6x of that). With an army of four of each (`-Dexec.args="2 12 sniper 4"`)
a copy costs 1.6x and saves little: a Sniper copy 2.1 (nodes 0.1x to 4.9x of it), a Burst, Radar or Hive copy
nothing within the noise of about one life, an Obelisk copy 3.5, a Mortar copy 5.4 (nodes 0.8x to 1.3x, the
`FEATURE-aura-and-balance-pass.md` target of 0.7x to 1x). **The Scorcher was the exception** (its copy saved 17 and no node beat 0.6x of that); its tree got a balance pass, see
the Cinder entry below. Reprice from
the four-copy table, not the one-copy one; the cheapest nodes (Range at $9 to $18) top it by dividing by a tiny cost,
so read their rows with the saved column. Nodes that act on enemies rather than on damage (Hollow Point, Shatter
Shot, Fifth Shot) save nothing within the noise of about one life.

- Frenzy and Momentum's haste stack since the stat-driven plan (up to +200% attack speed); check the
  Focused Optics chain with Momentum.
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

The Cinder had a pass with `td.BuildHarness` (40 seeds, the three built-in levels): Soulfire now burns every wave
beside the ordinary burn, which doubles the damage on a target (before it split the damage over two pools each
refreshed half as often, and dealt less than burn alone); Combustion bursts when an enemy's burning pools together
grow hotter than one wave could make them, since a pool almost never reaches its cap; Dragon's Breath no longer
cuts the damage on a target by a third; White Flame, Flashpoint, Heat, Everburn and Pyromancer's Mark are stronger.
What it showed and what is left:

- A node on its own pays little at $1500 (one upgraded Scorcher and plain copies for the rest); a last head node with
  a special pays well (Dragon's Breath with Searing Flame saves 11 lives on Twisted Hourglass, 6 on Zigzag Path, and
  nothing on the easy Curly Path). At $600 upgrading does not pay yet on any build: the path to head IV costs about
  $410 for a $28 tower.
- The fuel line (Kindling, Cauterize, Heat, Everburn) saves nothing measurable in this loadout. Scorched and Sickened
  only move crit damage and heals, and no wave here has healers, so judge it with a crit-heavy defence and a Mender
  wave, not with this table.
- Flashpoint and Inferno Ring are the weakest last head nodes (2 to 6 lives with a special where Dragon's Breath
  and Combustion save 6 to 11).

- **Where:** the `private static final` constants in `MortarTower`, `CinderTower`, `td.zone.Zone`,
  `td.zone.ZoneEffects`, `td.tower.mortar.ShellType` and the perks under `td.tower.mortar` and
  `td.tower.cinder`, and `ActiveEffects.BURN_REVEAL_FUEL`.
- **Approach:** tune through play or `td.BalanceHarness` with the other placeholder entries. The zone roster
  has no cap; `td.PerformanceHarness zones` (Barrage of Napalm and Lingering Flames on every tower) peaks at
  67 zones and sits at about a fifth of the tick budget, so a cap is not needed yet. Re-run it after raising
  a zone's lifetime or radius.

### Effect and specialization numbers are unbalanced placeholders

Every number the effect-interactions and specialization work introduced was chosen to be plausible,
not tuned: vulnerable (+15% per stack, cap 3, 4 s), the chill cap (80%), how far a chill cuts a burn (half
at the cap), poison's damage (10% of weapon damage per tick, 4 s), its slow (up to 30%) and the half-second
stack interval of burning and poisoned, revealed (3 s from Wide Band, 2 s from Resonant Field II),
Momentum's cooldown cut and 5 s, the shatter and explosion share and radius, shrapnel's ring width and
share, Warding Field's 10% chance, Withering Field's interval, Long Reach II's scaling, and Splash's three
blasts. Burning's resilience loss (-1 per stack) and poisoned's spirit loss (-1 per stack) grow without
limit up to their stat floors of -100. Scorched and Sickened hold while a pool that earns them is active, then lose a stack per second at neutral
spirit (one per 20 ticks, `ActiveEffects.STACK_DECAY_INTERVAL_TICKS`); at -100 spirit they never wear off.
A burning enemy earns 2 stacks per second, so it reaches the floor in about 50 s of steady burning.

- Cracked takes a flat 5 plating points (its `EffectKind.CRACKED` row), a starting value.
- **Where:** the named constants in `td.effect.ActiveEffects`, `AbstractTower` and each tower.
- **Approach:** tune through play or `td.BalanceHarness` with the other placeholder entries; decide
  whether a long burn's resilience loss (crit damage up to double at the floor) is too strong.

## Enemy features

### Enemy traits/abilities numbers are unbalanced placeholders

Every number introduced by the data-driven enemy model - `ArmorTrait`/
`HurtSpeedTrait`'s migrated Square/Triangle factors, and the Warden/egg chain's health, price,
ability intervals, shield percentages/radii and `EGG_HATCH_DELAY_TICKS` - was chosen to be
plausible, not tuned, the same situation the tower-upgrade and new-tower-numbers entries above
were in before their own balance passes. **One exception:** the Warden's `FlatResistTrait`
value has been tuned (15 -> 200) against the actual
per-hit/per-tick damage scale every attack tower deals (150-4000, see the head-to-head data in
the new-tower-numbers entry above) - 15 was negligible against any of them (0.375%-10% of a
single hit), making the Warden's armor mechanically inert regardless of which tower fought it.

- Heals are starting values from the stat-driven plan, meant to be visibly significant: the Mender's ally
  heal (0.125 to 2 points a tick by rank) and self-heal (0.35 to 1.4), the Warden's neglect heal (15% of the
  stage's health over 5 s), the Frenzied boss's threshold heal (1 a tick for 10 s) and the Frenzy
  Spawnling's death heal (0.5 a tick for 2 s).
- Shields are absorb pools with starting values: the Warden's magic self-shield (10% of the stage's
  health), its half-health ally shield (60 points) and crit-survived shield (3% of the stage's health), the
  Shieldbearer's ally shield (20 to 320 points by rank) and the Reaver's panic shield (10% of its health).
- **Where:** the enemy classes (`WardenEnemy` and the others in `td.enemy`), `ArmorTrait`,
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
  `WardenEnemy`'s `OnCriticalHitTakenTrigger` ability.
- **Approach:** tune in the feature 7 balance pass, with Keen Edge in play. Try Momentum at x3
  first; every number here is a named constant.
