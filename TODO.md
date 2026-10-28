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

`FEATURE-balance-verification.md` (first version implemented): headless players, a tower lab and a
balance report with a check for one change, to tune the placeholder entries below with. Its first
findings are in the doc.

`FEATURE-obelisk-support-role.md` (implemented, except the levels' retune; see Levels below): the Obelisk is a
support tower, and the balance tools price support. Its tree is being rebuilt as a kill box:
`FEATURE-obelisk-kill-box.md`.

`FEATURE-curly-path-retune.md` (implemented, but for the anti-sustain check; see Tower features): Curly Path's
eighteen waves rebuilt around five signature checks and the Warden, with the tools to prove each check. Its lane has
since been redrawn, so the checks need proving again (see Levels).

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

Every mutation originating on the EDT (placing or selling a tower, buying an upgrade path or
downgrading one, loading a level, requesting a wave) currently reaches the simulation by one of three ad-hoc
routes: a volatile flag `doTick` consumes (`requestNextWave`), direct mutation under a stopped
loop (`loadLevel`), or direct mutation while the loop runs (tower placement). Each is
individually safe, but there are three of them, and the third is why `AbstractTower`,
`CellNormal` and `GameWorld` all carry publication rules of their own. A downgrade already hands
what the tower holds for a removed node to the tick thread (`AbstractTower.removedSinceTick`).

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
- **The trigger:** recording live play. Headless runs already replay exactly from an action log
  (`td.balance.ScriptedPlayer`), since they have one thread; a person's session cannot be recorded
  the same way while the EDT mutates the world directly. If recorded human sessions become a goal,
  this stops being indirection and starts being the feature - do it then, and not before.

### `Tower` is a forty-method interface

`CLAUDE.md`'s code style asks for interfaces of one to five methods. `Tower` has over forty:
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

### `HitReceiver` and `Cell` are wide interfaces too

`HitReceiver` carries what a hit needs and what only the enemy inspector reads. `Cell` is wider than
five methods, but all of them are the square's own position, buildability and tower, so it stays.

- **Where:** `td.enemy.HitReceiver` (and `FakeEnemyMob`).
- **Approach:** shrink it before splitting: the inspection built from the mob's parts takes the
  display-only queries off it (`docs/plans/design-review/README.md` 7.4, F17). Split what is left
  only if a consumer is still hurt by the width.

### Settle a finished walk outside the mob

`DefinedEnemyMob.kill` and `leak` pay the bounty, charge the lives and report the walk themselves, so
the mob knows the economy's rules.

- **Where:** `DefinedEnemyMob.kill`/`leak`, `EnemyRoster.reportDeath`.
- **Approach:** `EnemyWalk` carries its fate, score and lives on leak; a `WalkSettlement` listener in
  `economy` applies them.

### `td.util` mixes leaves and the world

`td.util` holds small leaf helpers beside `GameWorld` and `GameHost`, so importing a helper looks the
same as depending on the whole world.

- **Where:** `td.util`.
- **Approach:** split it into leaves and a `td.world` package. Almost every package imports it, so do it
  only together with a full layer map of the packages, which has been decided against for now.

### Path geometry lives in `td.wave`

`Vec2`, `Point`, the `Path*` types, `ArcLengthPath` and the smoothing are geometry, not wave scripts,
yet everything that moves along a path imports `td.wave` for them.

- **Where:** `td.wave`.
- **Approach:** move them into a geometry package. About 70 files import them, so do it only with the
  same layer map.

### `EnemyMob`/`Tower`/`Projectile` use Visitor, not a sealed type + switch

Sealing would drop the `accept()` methods and the visitor interfaces, but `permits` cannot name
test classes, so the hand-written `EnemyMob` and `AbstractTower` fakes would stop compiling.

- **Where:** `EnemyMobVisitor`, `TowerVisitor`, `ProjectileVisitor` and their `td.ui` frame
  builders.
- **Approach:** only if the fakes go away first (tests built on real mobs/towers from
  `td.fixtures`). `Projectile` has no fakes but alone would leave two dispatch styles.
- **Where the visitor is a cast:** `DevControls` and `EnemySelection` visit an `EnemyMob` only to
  reach `DefinedEnemyMob`'s fate and inspection. Lift `fate()` and the inspection onto `EnemyMob`
  (with the inspection built from the mob's parts, F17), and give `FakeEnemyMob` a real
  `ActiveEffects` (F18) so the fake is cheaper to keep (`docs/plans/design-review/README.md` 7.4).

## Balance tools

### What the balance tools cannot see yet

`docs/plans/balance-verification-review.md` ranks these; each is a gap in `td.balance`, not in the game.

- **Measures the recorder does not take:** damage wasted on overkill and shields, time each enemy spends in
  each tower's reach, damage per enemy kind per tower.
  - **Where:** `RunRecorder`.
  - **Approach:** poll them like the rest, tool-side; overkill needs the hit result `AbstractTower.dealDamage`
    already has.
- **What a node denies an enemy that heals or shields:** the lab measures every node on plain mobs, so the players
  never buy Dead Zone, Null Field, EMP or the Nullifier for what they deny a Mender. Measuring each node against the
  sustaining kinds beside a partner Sniper was tried and dropped: it quadrupled the lab's time and mostly found
  damage against those kinds, not denial.
  - **Where:** `TowerLab.profileOf`, `CompetentPlayer.upgradeOptions`.
  - **Approach:** only once some node denies enough to matter (see "No tower denies heals or shields along a whole
    lane"); measure it then on that node alone.

## Levels

### The built-in levels are not tuned to the support Obelisk

After the Obelisk became a support tower (`FEATURE-obelisk-support-role.md`, last findings, saved as
`target/balance/ob-phase4`), the competent player holds 100% of Curly Path, in band, but only 50% of Zigzag Path and
40% of Twisted Hourglass. Twisted Hourglass also loses 3 of its 4 lives to the Reaver in round 1, and it sends
shrouded groups down three lanes, so one Obelisk per lane is the only affordable reveal (banning it x0.03).

- **Where:** the wave scripts in `td.level.ZigZagPathLevel` and `td.level.TwistedHourglassLevel`.
- **Approach:** a feature request of its own; retune by wave composition, not by enemy health. Compare each change
  with `BalanceReport --seeds 12 --strong --variety --compare ob-phase4`.

### Curly Path's waves were tuned on its old path

Curly Path's lane was redrawn (one entrance loop, a wide bend, an inward hook out over the top edge) after its waves
were tuned on the old double spiral, and its first wave changed with it. The new lane is about 9% shorter (69 cells
against 76), and the cell that sees the most of it sees 15-22% less than the old spiral's best cell did, so the level
is likely harder. Every Curly Path figure elsewhere in this file, in `FEATURE-curly-path-retune.md` and in
`docs/ARCHITECTURE.md` was measured on the old path.

- **Where:** the wave scripts and starting credits in `td.level.CurlyPathLevel`.
- **Approach:** re-prove the retune's five signature checks and the Warden with
  `td.balance.WaveCheckReport --level 0` (the checks from `FEATURE-curly-path-retune.md`), then
  `BalanceReport --levels 0 --strong` against a baseline saved from the old path; retune by wave composition and
  bounty, not by moving the lane back.

### Twisted Hourglass's waves were tuned on its old lanes

Twisted Hourglass's teal and amber lanes now cross at the waist and swap exits, and the board grew a row (9×15).
Before, teal ran about 30 cells and amber 18; now both run about 24, so amber's waves get more of the board and
teal's less. Every Twisted Hourglass figure elsewhere in this file was measured on the old lanes.

- **Where:** the wave scripts and starting credits in `td.level.TwistedHourglassLevel`.
- **Approach:** `BalanceReport --levels 2 --seeds 12 --strong --variety` against a baseline saved from the old
  lanes; retune by wave composition, not by moving the lanes back.

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

### No tower denies heals or shields along a whole lane

Curly Path's wave 12 was meant to need anti-sustain, and no wave could: the strong player denied every anti-sustain
node (Ward, Soul Drain, Tar, the nuke's fallout, the Hexer's hexes, Cauterize, EMP, Nullifier, Shatter Tone) leaks
the Elite Menders no more often than it does with them, and a free Dead Zone Obelisk on its best cell changes nothing.
Dead Zone works, but only inside its field: in a duel wave of Elite Menders it is on an enemy for about 140 enemy-ticks
against 1,100 under heals, so the heals land everywhere else. The answers are either local (a field) or hit-by-hit
(EMP, Nullifier), and raw damage beats a heal-heavy wave sooner than any of them.

- **Where:** `PulseTower`'s Ward head (`FieldMode.DEAD_ZONE`, `SILENCE`), `SoulDrain`, the Hexer's Blight and
  Inversion, `CauterizeMechanic`, `EmpPayload`, the Nullifier.
- **Approach:** a design pass of its own: one answer that reaches a stretch of path wide enough to matter (a Ley
  Line's beam carrying Dead Zone, a Sickened that lowers spirit for long enough to shrink every heal), then check
  Curly Path's wave 12 with `td.balance.WaveCheckReport --checks 12:sustain --parts strong,strip`.

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
so read their rows with the saved column. The Sniper's Marksman's Eye specials (Hollow Point, Fourth Shot, Shatter
Shot) saved nothing until the Sniper's retune; see the crit entry below.

- Frenzy and Momentum's haste stack since the stat-driven plan (up to +200% attack speed); check the
  Focused Optics chain with Momentum.
- **Where:** `UpgradeTier`'s price multiples, and the `private static final UpgradeNode` constants in every
  leaf under `td.tower` (`SniperTower`/`SplashTower`/`SonarTower`/`PulseTower`/`MortarTower`/`SeekerTower`/
  `CinderTower`/`AuraTower`).
- **Approach:** play each of the built-in levels with every node bought at least once, and adjust price/stat-bonus/
  condition-threshold values until each node feels like a meaningful, roughly-comparable-in-power choice rather than
  a strictly-better-or-worse one. No code or architecture change needed — every number here is already a named
  constant, not embedded in logic. `td.balance.BalanceReport` (the lab's node factors, `--save` and `--compare`)
  and the dev panel (`README.md`) make this cheap to actually do.

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
  and Combustion save 6 to 11). A retune (Flashpoint +50% crit and 8 Scorched, Inferno Ring without its range cut,
  Pyromancer's Mark +40% magic taken) moved them little: four copies on Zigzag Path save 25.1 and 22.1 lives
  against Combustion's 28.4 and Dragon's Breath's 25.7 (`docs/ideas/sniper-and-scorcher-identity.md`).

- **Where:** the `private static final` constants in `MortarTower`, `CinderTower`, `td.zone.Zone`,
  `td.zone.ZoneEffects`, `td.tower.mortar.ShellType` and the perks under `td.tower.mortar` and
  `td.tower.cinder`, and `ActiveEffects.BURN_REVEAL_FUEL`.
- **Approach:** tune through play or `td.balance.BalanceReport` with the other placeholder entries. The zone roster
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

- Cracked takes a flat 5 points of plating and insulation (its `EffectKind.CRACKED` row), a starting value.
- **Where:** the named constants in `td.effect.ActiveEffects`, `AbstractTower` and each tower.
- **Approach:** tune through play or `td.balance.BalanceReport` with the other placeholder entries; decide
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
  architecture change needed, every number here is already a named constant. `td.balance.BalanceReport`
  and the dev panel (`README.md`) make this cheap to actually do: its wave script box spawns the Warden chain's
  stages on demand without playing up to them first.

### Critical-damage numbers are unbalanced placeholders

Every tower crits for x1.5 (`AttackProfile.DEFAULT_CRIT_MULTIPLIER`) except the Sniper, born with
5% at x2.0. Crit damage bonuses add to the multiplier, so the Sniper's crit reaches about x3.0
(Tradecraft's streak +0.5, the Aura's Keen Edge +0.5), and Momentum multiplies a charged shot by 3
on top: about x9 in one hit. Momentum was x5 (about x15); `td.BuildHarness` (40 seeds, 4 Snipers on
Zigzag Path and Twisted Hourglass) showed the best builds saving the same lives with it at x3 or x5
(Railgun with Momentum 9.6 and 10.5, Railgun with Headhunter 9.2), while Momentum on its own went from
6.6 to 4.1 lives saved on Twisted Hourglass, still the best special by far. The Warden's
on-crit-survived shield (30% for 100 ticks) is from the first crit pass.

- Since the Sniper's retune (Steady Aim kept across targets, fading a stack 3 s after the last shot, Marksman's Eye +20% damage a stack, the Marksman's
  Eye specials keeping its own targeting, stronger specials), four copies on Zigzag Path save 7.1 to 9.3 lives with
  Hollow Point, Fourth Shot, Shatter Shot, Ricochet or Headhunter, and 17.6 with Momentum; before, the first three
  lost lives. Momentum is still far ahead (`docs/ideas/sniper-and-scorcher-identity.md`, S4).
- **Where:** `AttackProfile.DEFAULT_CRIT_MULTIPLIER`, `SniperTower.CRIT_CHANCE` and
  `CRIT_MULTIPLIER`, the Momentum node in `SniperTower`, `CritStreakMechanic`, and
  `WardenEnemy`'s `OnCriticalHitTakenTrigger` ability.
- **Approach:** raise the five other Sniper specials until a build with each saves about what Momentum's does,
  checking each with `td.BuildHarness`; every number here is a named constant.
