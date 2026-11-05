# Known gaps, future work and feature ideas

All the work still open, in one place. The source carries no inline `TODO`; a gap is an entry here.

- **A known gap** is a section under its area with **Where** and **Approach**. Closing it deletes the entry.
- **A feature idea** is one line under "Feature ideas": what it is for and where it stands, with a link to its
  brainstorm or feature request once one exists. It leaves the list when its feature request ships.

## Feature ideas

- **Tower visual revamp** (`docs/features/FEATURE-tower-visual-revamp.md`): every tower drawn as a foundation that
  shows its upgrades and a head that aims and fires, a calmer board, shots with flight. Draft: the Sniper and the
  Burst are written; the other six towers and the open questions remain before planning.

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

`FEATURE-balance-verification.md` (first version implemented): headless players, a tower lab and a
balance report with a check for one change, to tune the placeholder entries below with. Its first
findings are in the doc.

`FEATURE-obelisk-support-role.md` (implemented, except the levels' retune; see Levels below): the Obelisk is a
support tower, and the balance tools price support. Its tree is being rebuilt as a kill box:
`FEATURE-obelisk-kill-box.md`.

`FEATURE-curly-path-retune.md` (implemented, but for the anti-sustain check; see Tower features): Curly Path's
eighteen waves rebuilt around five signature checks and the Warden, with the tools to prove each check. The level has
since been reworked into The Amber Hive, with waves of its own.

`docs/plans/shipped/stat-driven-buffs.md` (phases 1 to 9) and `FEATURE-agility-and-dodge.md` are implemented.

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

### A hard-control kind without a ladder fails in play

`DiminishingReturns.of` handles only some kinds and throws on the rest, while the mob asks it for every kind in the
hard-control category. A new kind in that category compiles, then throws the first time it is applied.

- **Where:** `td.effect.DiminishingReturns.of`, `td.enemy.DefinedEnemyMob.ladderFor`.
- **Approach:** make the ladder part of what a hard-control kind must declare, so a kind without one can't be built.

### A command queue would make the simulation a true single writer

Not a defect - an option, recorded with the condition that would make it worth taking.

Every mutation originating on the EDT (placing or selling a tower, buying an upgrade path or
downgrading one, loading a level, requesting a wave) currently reaches the simulation by one of three ad-hoc
routes: a volatile flag `doTick` consumes (`requestNextWave`), direct mutation under a stopped
loop (`loadLevel`), or direct mutation while the loop runs (tower placement). Each is
individually safe, but there are three of them, and the third is why `AbstractTower`,
`Cell` and `GameWorld` all carry publication rules of their own. A downgrade already hands
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

### `HitReceiver` is a wide interface too

`HitReceiver` carries everything a hit, an effect or a tower's hit bonus asks of an enemy.

- **Where:** `td.enemy.HitReceiver` (and `FakeEnemyMob`).
- **Approach:** as for `Tower`: split along the lines its consumers already use (a hit, an effect, a
  query a bonus reads) when one is actually hurt by the width.

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
  (`EnemyInspection` is already built from the mob's parts).
- **`FakeEnemyMob` answers effects from a real `ActiveEffects`** (design review F18), done with the
  fakes' question above. Today the fake keeps its own simplified record of applied effects, so a change
  to stacking, caps or refreshing in `ActiveEffects` never reaches the tests that use the fake. Keep the
  record of what was applied for assertions, but answer every effect query from an `ActiveEffects` built
  the way `DefinedEnemyMob` builds one (`effect` does not depend on `enemy`, so the fake may hold one).
  Proof: change the stack cap rule in `ActiveEffects` and see a test on the fake fail. Some tests may
  rely on the simplified model; rewrite them on `EnemyFixtures.liveMob`, never by editing an expected
  value.

## Design follow-ups, each done with the change that touches it

From the design review (`docs/plans/shipped/design-review/README.md`, section 7.4; the F labels are its).
Each entry names its trigger: do it in the same work, as its own commits, not before.

### A board look is a switch in the painter, not a table row (ADR 0018)

**Trigger:** the tower looks with `FEATURE-tower-visual-revamp.md`. (Design review F16-A.) Enemy bodies
are done: `EnemyLook` and `EnemyLooks` are the pattern to follow.

`td.ui.render.Palette` is one flat enum for every other look: tower bodies and heads, markers, terms,
interface colours. `Java2DFrameRenderer.colorFor` maps every constant to a colour, and two shape switches
each cover only their own slice of the enum (`towerBodyShape`, `turretHeadShape`), with a `default` that
throws. So a new tower without its shape compiles, passes `mvn verify` and fails while painting, and every
look change edits the painter.

- **Where:** `Palette`; `Java2DFrameRenderer` (`colorFor` and the four switches); the draw records
  that carry a `Palette` (`BeamDraw`, `CannonballDraw` and others in `td.ui.render`);
  the frame builders that choose one (`EnemyFrameBuilder.markerPaletteFor`, the
  tower builders); `AsciiBoardRenderer.towerChar`.
- **Approach:** ADR 0018 is the decision; read it first. One role per commit:
  1. An AWT-free role enum in `td.ui.render` (`TowerLook`), one constant per look.
  2. A table in `td.ui` (`TowerLooks`): an `EnumMap` from role to its colour and shapes, built once,
     that throws `IllegalStateException` naming any role without a row when the class loads.
  3. The draw record's field changes from `Palette` to the role, with the frame builder that sets it
     and `AsciiBoardRenderer`.
  4. The painter looks the row up. That role's `Palette` constants, its `colorFor` cases and its
     shape switch go.
  5. `mvn verify`, a `PreviewBoard` render of the same scene before and after, and
     `td.PerformanceHarness` (draw records and builders change).

  Then markers, then the rest. When `Palette` is gone, so is `colorFor`; update
  `ui/CLAUDE.md` (the colour rule and the exhaustive-switch list) and `effect/CLAUDE.md` ("Adding a
  kind", step 4) in the commit that makes their lines false.

### An attack can read the stats its shot already spent

**Trigger:** the next change to how a tower attacks; at the latest the visual revamp's delayed hits
(a Sniper hit landing when its tracer arrives, the Burst's lobbed ball, Ricochet bounces in flight).
(Design review F15b.)

`AbstractTower.beginShot()` spends the tower's next-shot charges (a `TowerStatus` granted with
`grantStatus`, such as a primed round), republishes the live stats without them, and returns the
`TowerStats` the attack should use. Every attacker but the Sniper and one Burst path ignores that
return value and reads the live stats (`damageCurrent()`, `stats()`), which is right only while no
charge was spent. The Burst's blast lost its bonus exactly this way until it was fixed. A hit that
lands ticks after the shot widens the gap: by then the live stats may have changed for other reasons
too.

- **Where:** `AbstractTower.beginShot`; its callers in `SniperTower`, `SplashTower`, `MortarTower`,
  `CinderTower`, `PulseTower`, `SeekerTower` and `SonarTower`; the rule in `tower/CLAUDE.md` that
  starts "Every attack starts with `beginShot()`".
- **Approach:** `beginShot()` returns a `Shot` record (the attack's `TowerStats` and its shot
  number). Every attack path takes it, and anything in flight (a projectile, a delayed hit) stores it,
  so a hit uses the stats it was fired with. The Mortar reads some stats when its shell lands, by its
  spec: keep that, read from the stored `Shot`. One tower per commit, each with a test first on the
  model of `SplashTowerTest.aNextShotDamageStatusLandsOnTheBlastItIsSpentBy` (grant a next-shot damage
  status, fire, assert the hit carries it). When no attack path reads the live stats any more, delete
  the `tower/CLAUDE.md` rule. Run `td.PerformanceHarness`: one `Shot` per attack is fine.

### A tower's price is recorded by a constructor duty

**Trigger:** the next change to `TowerFactory`.

`AbstractTower`'s constructor reads `TowerRoster.priceOf`, which grows with each copy on the board, so
a tower must be built after its price is charged and before `TowerRoster.add`. Only
`tower/CLAUDE.md` says so; `TowerPlacement` keeps the order by hand.

- **Where:** `TowerFactory.createTower`, `AbstractTower`'s constructor (the price field),
  `TowerPlacement`'s place branch, the first rule in `tower/CLAUDE.md`.
- **Approach:** the least: move the duty into `createTower`'s Javadoc, its one place of use, and delete
  the `tower/CLAUDE.md` lines. Better, if that change touches the constructors anyway: pass the price
  paid into `createTower`, so the order can't be got wrong (the toolbar's preview passes the list
  price).

### The wave-script suggestions parse the grammar a second time; ranks are a token switch

**Trigger:** the next change to the wave-script grammar: a new keyword, rank or token. (Design review
F10 and F11.)

`WaveScriptSuggestions.positionAfter` walks the typed tokens with its own small state machine
(`Position`, `ShapeState`) to know what may come next. `WaveScriptGrammar`, the real parser, knows the
same, so a new keyword has to be taught to both. Ranks are string constants and two switches in
`WaveScript` (`isRankToken`, `rankFor`), while spawn shapes are rows of the `ShapeKeyword` enum.

- **Where:** `td.wave.WaveScriptSuggestions` (`positionAfter`, `RANKS`, `SHAPES`), `WaveScriptGrammar`,
  `WaveScript` (`isRankToken`, `rankFor`, the rank `*_TOKEN` constants, `RESERVED_TOKENS`),
  `ShapeKeyword` as the model, `wave/CLAUDE.md`.
- **Approach:** ranks first, it is small: a package-private `RankKeyword` enum shaped like
  `ShapeKeyword` (token, `Rank`, `of(String)`), which `isRankToken`, `rankFor` and the suggestions'
  `RANKS` read. Then the grammar answers what is pending after a typed prefix (a rank seen, a shape
  waiting for its member count), and `WaveScriptSuggestions` asks it instead of `positionAfter`.
  `WaveScriptSuggestionsTest` and `WaveScriptTest` must pass unchanged: the behaviour stays. Done when
  a new keyword is one row plus the grammar.

### A hard crowd-control kind's diminishing-returns ladder is a switch that throws in play

**Trigger:** adding the next `EffectCategory.HARD_CC` kind. (Design review F14.)

`DiminishingReturns.of(kind)` and `stepsOf(kind)` switch on the kind: Freeze and Daze have ladders,
and `of`'s `default` throws. A new hard crowd-control kind without a case compiles and passes every
test that doesn't apply it, then throws the first time it lands on an enemy in play.

- **Where:** `td.effect.DiminishingReturns` (`FREEZE_STEPS`, `DAZED_STEPS`, `of`, `stepsOf`),
  `EffectRule` (its factory and `withX` copies), the `FREEZE` and `DAZED` rows of `EffectKind`,
  `EffectSentences` (reads `stepsOf`), `effect/CLAUDE.md` ("Adding a kind").
- **Approach:** the ladder becomes rule data: a fluent `EffectRule.diminishing(float... steps)`
  (stored as a `List<Float>`, empty by default), set on the `FREEZE` and `DAZED` rows with today's
  steps. `DiminishingReturns.of` takes the rule's steps, `EffectSentences` reads the rule, and the
  switches and `stepsOf` go. A rule every entry keeps, in `EffectKindTest`: every `HARD_CC` kind has a
  ladder ending in 0, and no other kind has one. The numbers move, they don't change, so no balance
  check.

## Balance tools

### `TowerLabTest` can time out

Its class setup runs the whole lab, which takes kinds from the built-in catalog, so new content slows a test whose own
level has two enemies. `measureASmallLevel` takes 8-10 s on a cold JVM against the 5 s limit, so the test fails every
time when run alone and sometimes in `mvn verify` (once in 5 full runs), where its 5 tests then don't run. A longer
`@Timeout` on that setup would stop the failures, but no test uses one today and it deviates from the 5 s rule. Two of
its tests also name built-in nodes. The comments in `junit-platform.properties` (slowest test 0.9 s) and `pom.xml` (whole
suite 4.5 s) are out of date: the last full run's slowest test took 2.5 s and its classes added up to about 26 s.

- **Where:** `TowerLabTest`, `TowerLab.probedKinds`.
- **Approach:** each test measures only its own tower and node, on definitions built in the test, with no shared setup
  over the whole lab. A profile found `RunRecorder.afterTick` (16-20%) and `EnemyCatalog.builtIn` (about 6%) as savings,
  about a quarter at most, which isn't enough alone.

### The lab can't read a line past about 3.33x, and upgrades look overpriced

Measured 2026-11-05 and not acted on: game prices and the lab are unchanged. Upgrade prices are a guess, and the
competent player spends little on upgrades early and buys few distinct nodes. Today's game on the three levels, 4 seeds:

| Level | Holds | With no upgrades | Spent on upgrades (early / middle / late) | Nodes in every play |
|---|---:|---:|---|---:|
| The Amber Hive | 89 ±8% | 67 ±3% | 10% / 43% / 73% | 12 |
| The Storm Ridge | 96 ±8% | 58 ±5% | 19% / 33% / 70% | 10 |
| The Last Hour | 23 ±2% | 26 ±2% | 0% / 39% / 56% | 3 |

**The ceiling.** `TowerLab.DUEL_SHARE` tunes the bare tower to land 0.3 of a duel wave, and a share can't pass 1, so a
whole line reads at most about 3.33x. Once a line is there, the next node's factor reads about 1.00: Dragon's Breath
takes its line from 47 to 86 lives saved on The Amber Hive (army of one of each tower), and the player sees no gain. `NODE_FACTOR_CAP` is a second,
separate limit on what the player believes. Lines the lab read at 3x or more, ranked against lives saved: 0.73 and 0.33
on the first two levels.

**One-change tests**, The Amber Hive only, 3 seeds, so a lead and not a result:

| Change | Holds | Spent on upgrades (early / middle / late) |
|---|---:|---|
| Today | 89 ±11% | 10% / 43% / 73% |
| Every node at half price | 115 ±20% | 58% / 70% / 71% |
| No XP requirements (purpose gates stayed) | 100 ±0% | 10% / 43% / 73% |
| `DUEL_SHARE` 0.1 | 101 ±1% | 4% / 59% / 68% |

**Fair prices**, from `td.PurchaseHarness` (`0,1,2 8 all 3`), in percent of the tower's list price. The harness closes
with this table, but the levels disagree (Head II reads 74, 33 and 254), so the means give a direction, not prices. Awaken and
Transcendent do nothing alone, so the lines they open have to pay for them. Only 10-19 lines of about 160 per level beat
another tower, which saves 28, 5.2 and 3.6 lives per 100 credits on the three levels.

| Tier | Now | Fair (mean) |
|---|---:|---:|
| Head I / II / III / IV | 100 / 150 / 250 / 400 | 70 / 120 / 44 / 117 |
| Extra I / II / III / IV | 80 / 120 / 200 / 300 | 30 / 25 / 11 / 5 |
| Special | 400 | 23 |
| Range I / II / III | 60 / 120 / 200 | 33 / 23 / -12 |
| Attune / Awaken / Transcendent | 80 / 100 / 400 | 22 / 0 / 0 |

Its limits: a fixed army bought in advance with gates ignored, so it overwhelms (over 1,000 lives lost) and a copy
probably looks better than it would late in a real game. Noise is up to a few lives (Attune and Awaken, which should
read about nothing, spread by ±3.5 on The Amber Hive), so only 39%, 18% and 34% of lines stand clear of it.

**Rejected: a fixed baseline for damage ratios** (one long straight path, a stacked crowd of plain enemies that never
die). Against lives saved with an army of one of each tower, 6 seeds, on the first two levels, its ratios did no better
than today's duels: rank within a tower 0.73 against 0.79, then 0.75 against 0.75, and a fair price from either is off
by 2-3x. A damage ratio can't give a price: Absolute Zero is worth
about 650 credits by lives saved and lists at 444, while its ratio of 223x implies about 7,000. It did rank the strongest
lines better (0.85 and 0.82), which the ceiling explains, and it adds a second environment and hand-made Scorcher and
Aura rules.

- **Where:** `TowerLab.DUEL_SHARE`, `UpgradeTier`'s percents, `td.PurchaseHarness`, `td.FairPriceTable`.
- **Approach:** compare four setups with today on all three levels through `BalanceReport --seeds 4 --levels 0,1,2`,
  about 8 minutes each: `DUEL_SHARE` 0.1; every node at half price; the tiers below; the tiers below with `DUEL_SHARE`
  0.1. Measured tiers, with small floors where the mean reads about nothing: Range 35 / 25 / 25, Attune 20, Awaken 20,
  Transcendent 50, Head 70 / 120 / 50 / 115, Extra 30 / 25 / 15 / 15, Special 25. Try each setup through a temporary edit
  that is never committed. Keep the ceiling fix if it holds on all three levels. Change prices only if the player then
  upgrades earlier and holds more; the levels will need retuning, since they were tuned at today's prices. The Last Hour
  holds far too little for prices to be its main problem, so judge it apart.

### What the balance tools cannot see yet

`docs/plans/shipped/balance-verification-review.md` ranks these; each is a gap in `td.balance`, not in the game.

- **What a node denies an enemy that heals or shields:** the lab measures every node on plain mobs, so the players
  never buy Dead Zone, Null Field, EMP or the Nullifier for what they deny a Mender. Measuring each node against the
  sustaining kinds beside a partner Sniper was tried and dropped: it quadrupled the lab's time and mostly found
  damage against those kinds, not denial.
  - **Where:** `TowerLab.profileOf`, `CompetentPlayer.upgradeOptions`.
  - **Approach:** only once some node denies enough to matter (see "No tower denies heals or shields along a whole
    lane"); measure it then on that node alone.

## Levels

### A sweep moves one number at a time

`--sweep clear` and `--sweep bounty` each hold the other at the level's own value, so the pairing that pays a clean
player what kills paid before has to be read across two tables rather than swept.

- **Where:** `td.balance.LevelKnob`, `BalanceReport.printSweep`.
- **Approach:** when the clear bonuses are tuned, try each table's income column first; add a two-knob grid only if
  that cannot find the pairing.

### The three reworked levels are untuned

The Amber Hive, The Storm Ridge and The Last Hour were rebuilt with new enemies, bosses and waves and have only been
playtested by hand, not measured; the clear bonuses The Amber Hive and The Storm Ridge pay are placeholders too. Every
figure for Curly Path, Zigzag Path or Twisted Hourglass elsewhere in this
file was measured on the old levels.

- **Where:** the wave scripts, starting credits and enemy numbers in `td.level.CurlyPathLevel`, `ZigZagPathLevel`,
  `TwistedHourglassLevel`, `WardenEnemy`, `StormTwinsEnemy` and `KeeperEnemy`.
- **Approach:** after the playtests, `BalanceReport --levels 0,1,2 --seeds 12 --strong --variety`; retune by wave
  composition first, then the bosses' numbers. Check that the bosses' counters (AoE against Hivemind, a kill zone
  where the storm's lanes cross, a freeze, silence or heal denial against Rewind) show in the reports' purchases.

### The level-select screen fits six levels

Every card sits in one row, because a second row leaves a card too little height at the menu's size for its preview,
name, description and stats; a seventh level would make each card too narrow for its stats.

- **Where:** `td.ui.PanelLevelSelect` (`CARDS_PER_ROW`), `td.TowerDefense` (`MENU_WIDTH`, `MENU_HEIGHT`)
- **Approach:** past six, page the row with `HudButton` arrows (and the digit keys moving the page), or switch to a
  list of one-line rows - preview thumbnail, name, stats - with the description in the hovered row only.

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

### The Scorcher's DPS row reads low when its waves keep time with the burn's pulses

The settled formula takes a top-up to wait half a pulse window on average. A wave period that is a whole number of
pulses lands every top-up on a pulse, so nothing decays unseen and the pool deals more than the row says, most on a short
burn. Any other whole period visits every phase and the formula holds.

- **Where:** `ActiveEffects.settledPoolPerSecond` (`keptUntilPulse`).
- **Approach:** drop the wait when the period is a multiple of `POOL_PULSE_TICKS`, against a simulated pool at such a
  period.

### No tower denies heals or shields along a whole lane

No wave needs anti-sustain yet. On The Amber Hive (8 seeds) the strong player brings no anti-sustain node to the Brood
Nurse waves 11, 12 and 15 and does not leak heavily on them, and removing other roles matters far more on the wave
where it does lose: denied anti-sustain it leaks heavily on wave 16 in 5 seeds of 8 against 4 with nothing denied,
and denied single-target, area or anti-physical in all 8. Those leaks are mostly Drones and the Warden, not Nurses.
This shows the waves can be beaten without it, not that denial is weak: the players never price denial (see "What a
node denies" under Balance tools), and a free Dead Zone Obelisk could not show a gain on waves that already hold.

The earlier measure on the old Curly Path, before the restoration rework: Dead Zone was on an enemy for about 140
enemy-ticks against 1,100 under heals, since it works only inside its field. The answers are either local (a field) or
hit-by-hit (EMP, Nullifier), and raw damage beat a heal-heavy wave sooner than any of them.

- **Where:** `PulseTower`'s Ward head (`FieldMode.DEAD_ZONE`, `SILENCE`), `SoulDrain`, the Hexer's Blight and
  Inversion, `CauterizeMechanic`, `EmpPayload`, the Nullifier; `td.balance.Role.ANTI_SUSTAIN`.
- **Approach:** decide first whether any wave should require it. If one should, a design pass: one answer that reaches
  a stretch of path wide enough to matter, then prove it on that wave with
  `td.balance.WaveCheckReport --checks W:sustain --parts strong,strip`. The wave has to pressure the strong player, or
  the free Obelisk has nothing to improve. Iron Crossing and The Last Hour's Rewind are not measured.

### Upgrade-tree node numbers are unbalanced placeholders

Every tower's `UpgradeNode`s carry real prices and stat bonuses, but none of them have been played
against actual waves. Prices start from `UpgradeTier`'s multiples of the tower's list price (the table in
`docs/features/shipped/FEATURE-tower-progression.md`); the stat bonuses still follow the placeholder rule of the
first trees (an unquantified "+X" in the source doc became +25%, an unquantified crit bonus +10%). The
`ClusterCondition` thresholds are unverified guesses, and `UpgradeTier`'s XP table is checked only
against the bounty arithmetic in `docs/features/shipped/FEATURE-xp-and-purpose-gates.md`, not against play.
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
  against Combustion's 28.4 and Dragon's Breath's 25.7.

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
  lost lives. Momentum is still far ahead.
- **Where:** `AttackProfile.DEFAULT_CRIT_MULTIPLIER`, `SniperTower.CRIT_CHANCE` and
  `CRIT_MULTIPLIER`, the Momentum node in `SniperTower`, `CritStreakMechanic`, and
  `WardenEnemy`'s `OnCriticalHitTakenTrigger` ability.
- **Approach:** raise the five other Sniper specials until a build with each saves about what Momentum's does,
  checking each with `td.BuildHarness`; every number here is a named constant.
