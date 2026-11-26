# Known gaps, future work and feature ideas

All the work still open, in one place. The source carries no inline `TODO`; a gap is an entry here.

- **A known gap** is a section under its area with **Where** and **Approach**. Closing it deletes the entry.
- **A feature idea** is one line under "Feature ideas": what it is for and where it stands, with a link to its
  brainstorm or feature request once one exists. It leaves the list when its feature request ships.

## Feature ideas

- **Balance after the next-gen towers** (`docs/features/FEATURE-balance-after-next-gen-towers.md`): the second pass,
  played by the built towers: the game's absolute difficulty, the floor and ceiling in play, lives, the bosses.
  Request written; the towers are built.
- **The White tower** (`docs/features/shipped/FEATURE-tower-visual-polish.md`): a third Aerie identity, pure and white; its
  look (`TowerLook.WHITE`, eagles for birds) comes with the visual polish, unused on purpose. It needs a name, a node
  and a missile of its own. Idea only.
- **Call-to-arms banner**: wherever an enemy will call reinforcements (a Relay, a Knight boss, a Keeper), a banner on
  the board, clickable for what will come and when, with a bar to the next call and a count of the calls left.
  Idea only.

- **A tenacity debuff**: a debuff that lowers tenacity; it lengthens every debuff applied afterwards and multiplies
  pool damage, so it belongs on a crowd-control tower, with small per-stack values. Idea only.

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

### The zones loadout is over budget in the frame build and the paint

**Where:** `td.PerformanceHarness zones` reports a frame-build allocation p99 of about 540-546 KB against 512 KB and
a paint p99 of about 11 ms against 10 ms (measured at `c687a965`; the allocation read 517 KB before), so the harness
fails in that loadout. The default loadout stays within budget.
**Approach:** profile it with `--jfr`, find what the zone frames allocate per disc (`ZoneFrameBuilder`,
`ZoneOutlineTracer`) and build those without a copy per frame; for the paint, see what of the zone patches and the
flames is painted live rather than kept (`KeptPatches`, `Stamps`).

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
- **Where the visitor is still a cast:** `DevControls` (observing, clearing effects, healing) and the balance
  tools' `RunRecorder` and `TowerWorkTally` (definition id, abilities cast) visit an `EnemyMob` only to reach
  `DefinedEnemyMob`. Each needs the real mob, not a query every mob should answer, so they stay until sealing.

### Nothing finds production code that only tests call

The build has no dead-code check, and IntelliJ's on-the-fly inspection counts a test's call as a use, so
a method only its own test calls looks alive.

- **Where:** the whole of `src/main/java`.
- **Approach:** to find such members, run IntelliJ's
  whole-project inspection *Unused declaration* with its entry point option "When entry points are in test sources, mark callees as used" unchecked; most of what
  it lists are read-only seams tests watch a tower through (`getSplashRadius`, `spotOf`), which stay.

## Design follow-ups, each done with the change that touches it

From the design review (`docs/plans/shipped/design-review/README.md`, section 7.4; the F labels are its).
Each entry names its trigger: do it in the same work, as its own commits, not before.

### A board look is a switch in the painter, not a table row (ADR 0018)

**Trigger:** the next change to a marker's, a zone's or a tower effect's look. (Design review F16-A.) Enemy
bodies and towers are done: `EnemyLook`/`EnemyLooks` and `TowerLook`/`TowerLooks` are the pattern to follow.

`td.ui.render.Palette` is still one flat enum for every other look: markers, zones, beams and projectiles,
terms, interface colours. `Java2DFrameRenderer.colorFor` maps every constant to a colour, and the shape
switches cover only their own slice of the enum (`paintZoneDetail` with a `default` that draws nothing), so
every look change edits the painter.

- **Where:** `Palette`; `Java2DFrameRenderer` (`colorFor` and the per-slice switches); the draw records
  that carry a `Palette` (`BeamDraw`, `CannonballDraw` and others in `td.ui.render`);
  the frame builders that choose one (`EnemyFrameBuilder.markerPaletteFor`, `TowerEffectFrameBuilder`,
  `ProjectileFrameBuilder`, `ZoneFrameBuilder`).
- **Approach:** ADR 0018 is the decision; read it first. One role per commit, markers first:
  1. An AWT-free role enum in `td.ui.render`, one constant per look.
  2. A table in `td.ui`: an `EnumMap` from role to its colour and shapes, built once, that throws
     `IllegalStateException` naming any role without a row when the class loads.
  3. The draw record's field changes from `Palette` to the role, with the frame builder that sets it.
  4. The painter looks the row up. That role's `Palette` constants, its `colorFor` cases and its
     shape switch go; a colour text also uses stays a `Palette` tone whose `colorFor` case reads the row,
     as the tower tones do.
  5. `mvn verify`, a `PreviewBoard` render of the same scene before and after, and
     `td.PerformanceHarness` (draw records and builders change).

  When `Palette` holds only interface tones, update `ui/CLAUDE.md` (the colour rule and the exhaustive-switch
  list) and `effect/CLAUDE.md` ("Adding a kind", step 4) in the commit that makes their lines false.

### An attack can read the stats its shot already spent

**Trigger:** the next change to the Obelisk's field. (Design review F15b.)

`AbstractTower.beginShot()` spends the tower's next-shot charges (a `TowerStatus` granted with
`grantStatus`, such as a primed round), republishes the live stats without them, and returns the
`TowerStats` the attack should use. The Sniper's bullet, the Mortar's shell, the Burst's stone and the Aerie's missile
carry those stats to where they land, and the Scorcher keeps its stream's; the Obelisk's field
still reads the live stats (`damageCurrent()`, `stats()`), which is right only while no charge was
spent. Only the Sniper and the Burst grant themselves charges today, so nothing is lost in play yet.

- **Where:** the caller of `AbstractTower.beginShot` in `PulseTower`.
- **Approach:** as the Sniper's `Bullet` and the Mortar's `Shell` do: what the field deals keeps the
  `TowerStats` its shot began with, with a test first on the model of
  `MortarTowerTest.aShellLandsWithTheStatsItWasFiredWithThoughTheChargeItSpentIsGone`.

### A tower's price is recorded by a constructor duty

**Trigger:** the next change to the leaf towers' constructors.

`AbstractTower`'s constructor reads `TowerRoster.priceOf`, which grows with each copy on the board, so
a tower must be built after its price is charged and before `TowerRoster.add`. Only `createTower`'s
Javadoc says so; `TowerPlacement` keeps the order by hand.

- **Where:** `TowerFactory.createTower`, `AbstractTower`'s constructor (the price field),
  `TowerPlacement`'s place branch.
- **Approach:** when a change touches the leaf constructors anyway: pass the price paid into `createTower`,
  so the order can't be got wrong (the toolbar's preview passes the list price). That also lets a tower
  rebuilt from a save be built at its saved price, so `AbstractTower.price` can be `final` again and
  `restore(TowerRecord)` stops setting it. Tests build towers with `new` in about 200 places: route them
  through a `TowerFixtures` factory first, so the new parameter lands in one place.

### A tower's fight state is written twice and reset by hand

**Trigger:** each tower's turn in the next-gen bundle (`FEATURE-next-gen.md`), which edits its leaf class anyway.

A leaf tower's fight state (its `FireClock`, targets, counters, stores) starts in its field initialisers
and is set again, to the same values, in `endFight()`. Only `tower/CLAUDE.md` says that a new field a
fight changes resets there, so a field that misses it carries one wave into the next.

- **Where:** the leaf towers' fight fields and `endFight()` overrides; the rule in `tower/CLAUDE.md`.
- **Approach:** gather each leaf's fight fields into a small `*Fight` holder that its constructor and
  `endFight()` both build with `new`, so a new fight field is reset by being in it. Delete the
  `tower/CLAUDE.md` rule once every leaf has one.

### A query around a point cuts the point in some places and rounds it in others

`InRangeTargetQuery` is centred on a whole pixel. Most callers cut a `double` to it (`everyone(Vec2, range)`), but
where a shell lands, where a missile retargets and where a zone stands round it, so an enemy at the very edge of a
radius is in or out depending on which tower asks.

- **Where:** `InRangeTargetQuery` (its `int` centre), `MortarTower.onImpact`, `SeekerTower.Guide.retarget`,
  `ZoneRoster` (two queries).
- **Approach:** centre the query on a `Vec2` and drop the `int` factories. Who stands at an edge changes, so it is a
  behaviour change in its own commit: run the tests and the harness, and expect seeded balance runs to move a little.

### The Sundial's shadow keeps its heading from one wave to the next

`PulseTower.shadow` is never reset: each turn starts where the last one stopped, the first of a wave included, and a
save doesn't keep it, so a loaded game's first turn starts at heading 0 where a played-on one doesn't. Which enemy
the first sweep reaches first depends on it.

- **Where:** `PulseTower` (`shadow`, `endFight`, `takeRestPose`).
- **Approach:** put the shadow at one heading as a wave starts (`takeRestPose`, at `restHeading()` like a turret),
  with a test that two waves' first turns start alike.

### A cache kept until what it was worked out for changes is written six ways

Each keeps the last answer with the key it was worked out for and works it out again when the key moves: by identity
(`AbstractTower.offeredUpgrades`, `cachedForUpgrades`, `AuraTower.spec`) or by equality (`MortarTower.loadableKinds`,
`shellChances`), behind a `volatile`, some with `null` for "none yet".

- **Where:** those five methods and their record holders (`OfferedUpgrades`, `InspectionCache`, `FoldedSpec`,
  `LoadableKinds`, `ShellOdds`).
- **Approach:** one small holder (the key, the value, one `volatile`) with `get(key, compute)`, the key compared as
  each site does now; `AbstractTower`'s `null` goes with it.

### Ability radii are in pixels, the rest of the engine in cells

`RadiusTarget`, the radii a cast reaches and the aura rings drawn from them are pixels, while zones, towers and the
text that describes them speak in cells, so every ability radius is divided by the cell size wherever it is shown.

- **Where:** `RadiusTarget`, the `RadiusTarget(...)` call sites in the enemy and level classes, `AbilityText`,
  `EnemyDefinition.supportAura` and what reads it.
- **Approach:** make `RadiusTarget` take cells and convert once where the mob measures distance, then let the
  compiler find every call site; no save or wave-script format holds a radius.

### The round tables undercount a cheap build once a higher tier's bonus is open

`TowerRounds` picks a round's best defence from builds the bench measured at their own top tier, so a build of tier
0 still counts without tier I's free bonus in a round whose XP has opened tier I. Harmless while no tower sets a tier
bonus; once one does, the Rounds tables read early cheap defences too low.

- **Where:** `TowerRounds.best`, `TowerBench.buildsOf` (each build's `xp`).
- **Approach:** have the bench measure each stage's whole build at every higher tier's XP as well (one more row per
  stage and tier), and let `best` take the row at the round's open tier.

### Brittle, Mark, Charge and Unravel are dead content

`BRITTLE`, `MARKED`, `CHARGED` and `UNRAVELED` are effect kinds with rules, sentences, pictograms and tests, but no
built-in tower, node or enemy ability applies them; only the dev panel and tests reach them.

- **Where:** `EffectKind` rows `BRITTLE`, `MARKED`, `CHARGED`, `UNRAVELED`; `Effect.brittle`, `Effect.marked`,
  `Effect.charged`, `Effect.unraveled`, `StackingDebuff.UNRAVELED`; `ProcPayload.Charge`; `DevEffects`.
- **Approach:** Unravel gets its source in the next feature; keep it. Give the other three a source (a node, a proc
  or an ability) when their tower is next reworked, or delete the kind with its sentence, pictogram and tests; do not
  leave a kind half-wired once it has a source.

## Balance tools

### The balance-tools skill still documents the retired tools

`jtd-balance-tools` describes the Python paper tools (`tree_calc.py`, `node_sim.py`, `area_sim.py`, `reference.json`)
and the old harnesses (`BalanceReport`, `WaveCheckReport`, `PurchaseHarness`, `BuildHarness`), which the measured
pipeline (`TowerBench`, `TowerReport`, `WavePowerReport`, `PlayCheck`) replaced. An agent that loads it tunes with the
wrong yardstick; `tower-retune` tells it to ignore them meanwhile.

- **Where:** `.claude/skills/jtd-balance-tools/`, the superseded classes in `td.balance` and `td`.
- **Approach:** delete the Python tools and the harnesses nothing else uses, rewrite the skill around the pipeline
  in `docs/BALANCE.md`, and drop the line in `tower-retune` that warns against them.

### No test runs the whole lab

`TowerLabTest` runs each measurement alone, because `TowerLab.of` on even a small level takes most of a test's limit, and
more as content grows. So a mistake in how `of` puts the measurements together shows only when a balance tool runs.

- **Where:** `TowerLab.of`.
- **Approach:** a test of `of` needs the lab to cost far less on a small level: it takes every kind from the built-in
  catalog, whatever the level sends. Measuring only what the level sends would also change the numbers the balance tools
  compare, so judge that with them. A profile found `RunRecorder.afterTick` (16-20%) and `EnemyCatalog.builtIn` (about
  6%) as savings, about a quarter at most, which isn't enough alone.

### Keeping a hiding kind revealed lowers what the lab says a Sniper lands on it

On a small test level a Sniper's factor against a kind that turns invisible when hit is 0.96, and 0.89 with every enemy
kept revealed, so the lab rates a reveal as a loss for that partner. Perhaps a hit enemy that vanishes sends the next shot
to a fresh one, where a revealed one takes shots it no longer needs. Not looked into, and not checked on a built-in level.

- **Where:** `TowerLab.Duel.revealedFactor`, `TowerLab.revealedFactor` and what reads it in `CompetentPlayer`.
- **Approach:** trace one duel of each with a debugger logpoint on the Sniper's shots. If it is overkill, decide whether
  a duel at a health that low is the right measure of a reveal.

### The lab can't read a line past about 3.33x, and upgrades look overpriced

Measured 2026-11-05 on the slot tree, before upgrade tiers replaced it, and not acted on; the per-tier rows below
name tiers that no longer exist, so re-measure on the tiers before using them. Upgrade prices are a guess, and the
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

- **Where:** `TowerLab.DUEL_SHARE`, the node prices in each tower's tree, `td.PurchaseHarness`, `td.FairPriceTable`.
- **Approach:** compare four setups with today on all three levels through `BalanceReport --seeds 4 --levels 0,2,3`,
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

### Only Iron Crossing is tuned on the measured towers

Iron Crossing's waves and starting credits follow the room line on the yardstick of measured towers
(`docs/BALANCE.md`). The other five levels were tuned against the paper reference tower that yardstick replaced:
measured, their banks are two to six times what their rounds need, their first rounds leave credits for seven or
more towers, and their bosses' rounds stand far under their lines (the Warden, The Labyrinth's long rounds, The
Rift's lone walker).

- **Where:** the wave scripts, starting credits and clear bonuses in `td.level.*Level` other than
  `IronCrossingLevel`; `WardenEnemy`, `StormTwinsEnemy`, `KeeperEnemy` and the other bosses' numbers.
- **Approach:** per level, `WavePowerReport --levels <it>` and the targets in `docs/BALANCE.md`: cut the starting
  credits to about 1.4 times the first round's need, raise ranks in place of counts until room follows 1.4 down to
  1.0, drop the clear bonuses the line no longer needs; then the play check.

### Iron Crossing pays about twice what its late rounds ask

The play check's best effort clears the level with no leak and has 733 of the 1990 credits paid before the last
round unspent as it starts (37%; the slack verdict warns over 5%). Its idle credits by round: 30 39 32 39 59 56 43,
then 181 275 363 620 757 283 605 733. Rounds 1 to 7 take what the level has paid (round 7, two Veteran Ghosts on the
rush lane, is held only after an earlier round is replayed); rounds 8 to 15 do not. The wave stage's yardstick
overstates the need there about twofold (2325 at round 15 against 1257 spent in the whole play): it buys average
towers that serve one lane, and the play buys counters, stands them where the lanes cross and puts 71% into
upgrades. Not done with the play check because the enemies' higher ranks are to be retuned first, which moves
every round's strength.

- **Where:** `IronCrossingLevel` rounds 8 to 15; the room line in `docs/BALANCE.md` ("The targets");
  `WavePowerReport` (the line the `even` column follows); `PlayCheck.slackVerdict` is the judge.
- **Approach:** after the enemy retune below, harder for the same pay: rank (Veteran to Elite) and affixes
  (warding, armored, steadfast, unstoppable) in place of counts, about 1.6 to 1.8 times the strength of rounds 8
  to 15 as they stand. Loop `WavePowerReport --levels iron`, then `PlayCheck --players best --verbose`, until the
  best effort still clears with no leak and its `idle` column stays under about 0.1 from round 8 on and under 0.05
  at the last round. Then set the room line to what those rounds measure (about 1.4 falling to about 0.55, not to
  1.0), and run the full `PlayCheck`: bare towers and every single tower must still lose. The Mortar alone loses
  only in round 10 on three leaks, so watch it first.

### The rank retune has not reached the tower sheets

Every ladder and level enemy now gives its higher ranks stronger stats, traits and abilities, and `EnemyPowerReport`
and `WavePowerReport` were rerun on it; the sheets downstream were not. The tower bench's defence scenarios are
Veteran Knights, Juggernauts, Templars, Ghosts and Runners, so tower worths and prices are measured against the old
enemies. The Warden, the Keeper of Hours and Talos were left as they were.

- **Where:** `TowerBench`, `TowerReport`, `TowerPriceWriter`, `PlayCheck`; the Warden in `WardenEnemy`, the Keeper in
  `KeeperEnemy`, Talos in `LabyrinthLevel`.
- **Approach:** run the pipeline in order (`TowerBench`, `TowerReport`, `TowerPriceWriter`, then `WavePowerReport`
  and `PlayCheck`) and read what moved. For a boss, change it and run `EnemyPowerReport --kinds <it>`; the verdict
  only asks that it not fall below the Walker of its rank, and its bounty is written against `fair`, under it.

### A round always pays its full bounty

A level's last round pays for nothing: no round is left to spend it on. A wave should take a pay ratio (1 by
default, 0 for nothing) that scales the bounty of its kills, for the last round to use.

- **Where:** `WaveDefinition` (beside `withClearBonus`), where a kill's bounty is paid in the engine (the playtest
  rules' bounty percent already scales it for a whole play), the wave preview's pay line, `WavePowerReport`'s
  `bounty` and `pays`, the save format if a round's pay is stored.
- **Approach:** `WaveDefinition.withPay(ratio)`; the engine multiplies a kill's bounty by its wave's ratio, spawns
  included; the preview shows the scaled pay. It changes no verdict by itself: the play check already leaves the
  last round's pay out of the slack, and the credits idle before the last round were paid by rounds 8 to 14, not
  by the last one, so it does not replace the entry above.

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

### A tier's paid unlock is still in the upgrade tree

Every gate opens on XP alone (`UpgradeTreeCatalogueTest` holds the built-in trees to it) and the Upgrades panel has
no place for a price at a gate, but the tree can still make a paid tier's unlock node.

- **Where:** `UpgradeTier.unlockPrice`, `UpgradeTree` (`unlockOf`, `isPaidOpen`, `isUnlock`, `unlocksHeld`),
  `PathPlanner`, `UpgradeOffer`, `TowerBench`, and the tests built on
  `TierTrees.PAID_TIERS`.
- **Approach:** delete the unlock with the next change to tier gates, moving `TierTrees` to standard tiers; or, if a
  paid gate comes back, give the panel's floor strip a pay button.

### Thick Mix may be meant for the Normals behind a payload, not every Normal

With Thick Mix every Normal shell carries the loaded payload, and the magazine bands every Normal to match. A
narrower rule, a Normal carrying it only when it follows a payload shell in the magazine, may read better: the band
would then tell shells apart rather than mark them all. It would weaken the node, which was priced on every Normal
carrying.

- **Where:** `MortarTower.carriedPayload` (the rule, read at firing, at impact and for `profileOf`),
  `TowerSheetText.magazineLines` (which slots are banded).
- **Approach:** measure first: the node's worth under each rule with the tuning harnesses, at today's price. Change the
  rule only if the narrower one is still worth its price or the price moves with it; the carry then follows the
  shell, decided when it is drawn, so the pane and the shot agree.

### The Obelisk's smallest cues are not drawn

The feature doc's look has three cues the board does not show yet: a thin ripple from an enemy each time it gains a
stack of Toll, a small flash on an enemy the Sundial's shadow hits (larger inside an Umbra field), and Deep Toll's
rune glowing hotter while it holds an enemy past a plain field's Toll. The stone, its field, the veil, the knell and
the Toll ring all draw.

- **Where:** `td.ui.EnemyFrameBuilder` (the Toll ring), `td.ui.TowerEffectFrameBuilder.visitPulseTower`,
  `td.ui.TowerSpriteFrameBuilder.poseOf(PulseTower, int)`, `td.ui.TowerFoundations.obelisk`.
- **Approach:** the ripple and the flash follow the timed-visual idiom: the enemy records the tick it last gained a
  stack and the tower the tick and place of each shadow hit, answered as an `OptionalInt`; Deep Toll's glow is a
  `TowerCue` the pose sends when the tower's `PulseSpec.deepToll()` and the field holds an enemy past the plain cap.

### Dragon's Breath streams in the Scorcher's own cone

The feature doc draws Dragon's Breath as a cone a third as wide and half again as long, with a short breath between
streams; the node lengthens the stream and shortens the reload, but the cone it pours into and the one drawn are the
tower's own.

- **Where:** `td.tower.cinder.DragonsBreathMechanic`, `CinderTower.halfWidthRadians` and its reach,
  `td.ui.TowerEffectFrameBuilder.streamTongues`.
- **Approach:** a cone and reach factor on `CinderSpec` from the mechanic, read by the cone test and the stream's
  look alike, so what is drawn is what pours; then weigh it again with `tree_calc.py`, since a narrower cone reaches
  fewer enemies.

### Flashover's threshold is a stopgap

Since pools last 4 s, Flashover's threshold is 250 fuel, raised from 100 only so it keeps about the share of a stream
it fired before; 100 would make it fire on almost every stream, even outside the Core. 250 is a hidden number the player can't read off the board.

- **Where:** `td.tower.cinder.FlashoverMechanic.HOT_FUEL_POINTS` and its sentence.
- **Approach:** in the Scorcher's balance, make it "a burn whose bar is full flashes over" (400 fuel). It reuses the
  burn bar the player already sees, so it drops a number. Simulated on one enemy standing in the cone, it keeps
  deep builds (Core, Long Stream II and Bellows: 62% of the stream against 52% today) and several Scorchers on one
  enemy, and drops a mid-size Core build (19% today) to none; weigh that loss there. Pools have no cap, so fuel
  still rises past a full bar.

### Effect duration has no source and reaches only some effects

Nothing gives a tower the `EFFECT_DURATION` stat: no node, emission, status or dev grant raises or lowers it from its
base of 100%, and `TowerModifiers.effectDuration` has no caller. Yet `AbstractTower.applyEffect` scales every effect
by it, so the first thing to grant it would quietly lengthen far more than intended. It stretches every timed effect
and every share (Crack, Singe, Expose, Vulnerable, a heal, a shield). It never reaches a pool, which lasts as long as
its fuel, nor a shared count (Sunder, Scorch, Toll and the rest), whose 5 s hold and fade step are the same for every
source. The info pane still shows the row on a tower that applies only those.

- **Where:** `AbstractTower.applyEffect`, `TowerStat.EFFECT_DURATION`, `TowerModifiers.effectDuration`,
  `td.tower.TowerStatRow`'s Effect duration row.
- **Approach:** decide what the stat is for before anything grants it: which modes it lengthens (timed and shares
  only, or a pool's fuel too, through `withFuelScaledBy`), and hide the row on a tower whose effects it never
  reaches. If nothing should grant it, delete the stat and its row.

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

- **Where:** `PulseTower`'s Ward (`WardMechanic`, Silence in its field), the Hexer's Blight and
  Backlash, `CauterizeMechanic`, `EmpPayload`, the Nullifier; `td.balance.Role.ANTI_SUSTAIN`.
- **Approach:** decide first whether any wave should require it. If one should, a design pass: one answer that reaches
  a stretch of path wide enough to matter, then prove it on that wave with
  `td.balance.WaveCheckReport --checks W:sustain --parts strong,strip`. The wave has to pressure the strong player, or
  the free Obelisk has nothing to improve. Iron Crossing and The Last Hour's Rewind are not measured.

### Upgrade-tree node numbers are unbalanced placeholders

Every tower's `UpgradeNode`s carry real prices and stat bonuses, but none of them have been played
against actual waves. Prices are each node's own; the stat bonuses still follow the placeholder rule of the
first trees (an unquantified "+X" in the source doc became +25%, an unquantified crit bonus +10%). The tiers' XP and
unlock price (`UpgradeTier.standard()`) are a first guess, not checked against play.
Feature 7's balance pass sets the final numbers.

`td.PurchaseHarness` measures them: lives saved per 100 credits, on Twisted Hourglass with 12 seeds. The
per-copy surcharge is the limit on copies, and the harness now measures it at a realistic price. The copy figures
below were measured at +15% of the list price per copy; the surcharge now compounds at x1.25 per copy, so remeasure
them before repricing. With an army of one
of each tower a copy is the best buy for all but the Aura and the Hive (a Sniper copy saves 27 lives per 100 credits
for $17; an upgrade node saves 0.02x to 0.6x of that). With an army of four of each (`-Dexec.args="2 12 sniper 4"`)
a copy costs 1.6x and saves little: a Sniper copy 2.1 (nodes 0.1x to 4.9x of it), a Burst, Radar or Hive copy
nothing within the noise of about one life, an Obelisk copy 3.5, a Mortar copy 5.4 (nodes 0.8x to 1.3x, the
`FEATURE-aura-and-balance-pass.md` target of 0.7x to 1x). **The Scorcher was the exception** (its copy saved 17 and no node beat 0.6x of that); its tree got a balance pass, see
the Cinder entry below. Reprice from
the four-copy table, not the one-copy one; the cheapest nodes (Range at $9 to $18) top it by dividing by a tiny cost,
so read their rows with the saved column.

- **Where:** `UpgradeTier.standard()`, and the `private static final UpgradeNode` constants in every
  leaf under `td.tower` (`SniperTower`/`SplashTower`/`PulseTower`/`MortarTower`/`SeekerTower`/
  `CinderTower`/`AuraTower`).
- **Approach:** play each of the built-in levels with every node bought at least once, and adjust price and stat-bonus
  values until each node feels like a meaningful, roughly-comparable-in-power choice rather than
  a strictly-better-or-worse one. No code or architecture change needed — every number here is already a named
  constant, not embedded in logic. `td.balance.BalanceReport` (the lab's node factors, `--save` and `--compare`)
  and the dev panel (`README.md`) make this cheap to actually do.

### The Mortar's, Scorcher's and zones' numbers are unbalanced placeholders

The Mortar's prices, damage, cooldowns, radii and node bonuses come from
`FEATURE-ground-zones-mortar-and-cinder.md`, and the numbers that doc leaves open were chosen to be
plausible, not tuned: a zone's radius, length and pulse (twice a second), tar's 40% slow and poison
pulse, frost's chill and the 2 s before it freezes, fallout's Sickened stacks, each shell kind's own points
past a Normal and the Heavy's, how much a Heavy's bleed does, the magazine's weights, each shell kind's flight speed, a
Volley's gap, reload and spacing, and the nuke's flash. Nothing has been played against waves, and the Mortar's slow shell is untested against fast
enemies.

The Scorcher's numbers were weighed on paper (`shipped/FEATURE-next-gen-cinder-tower.md`), never played: its Flame, stream,
reload, cone and range, each node's bonus and price, the Core's width and share, Flashover's threshold, reach and
share, Meltdown's share a point and Thermal Shock's bonus. So were the pool rule every burn, poison and Soulfire
follows (100 fuel in 1.75 s, pulses four times a second) and the fuel the Mortar's ground and the Blight now give,
each set to what one application dealt under the old decaying rule.

- **Where:** the `private static final` constants in `MortarTower`, `CinderTower`, `td.zone.Zone`,
  `td.zone.ZoneEffects`, `td.tower.mortar.ShellKind` and the mechanics under `td.tower.mortar` and
  `td.tower.cinder`.
- **Approach:** tune through play or `td.balance.BalanceReport` with the other placeholder entries. The zone roster
  has no cap; `td.PerformanceHarness zones` (Mortars with Barrage, Napalm and Flare) peaks at
  95 zones and sits at about a fifth of the tick budget, so a cap is not needed yet. Re-run it after raising
  a zone's lifetime or radius.

### Effect and specialization numbers are unbalanced placeholders

Every number the effect-interactions and specialization work introduced was chosen to be plausible,
not tuned: vulnerable (+15% per stack, cap 3), the Blight's poison fuel (a share of the tower's damage) and the half-second
stack interval of burning and poisoned, revealed (3 s from Wide Band, 2 s from Resonant Field II),
Momentum's cooldown cut and 5 s, the shatter and explosion share and radius, shrapnel's ring width and
share, Warding Field's 10% chance, and Long Reach II's scaling. Scorch's resilience loss (-1 per stack) and Sicken's spirit loss (-1 per stack) build to 200 stacks, enough to take
either stat to its floor from any enemy. They hold while a pool is active, then lose a stack per second
(`Effect.FADE_STEP_TICKS`). A burning enemy earns 2 stacks per second, so steady burning floors its resilience in
under two minutes.

- Crack and Singe take a flat 5 points of plating or insulation a stack (their `EffectKind` rows), starting values.
- **Where:** the named constants in `td.effect.ActiveEffects`, `AbstractTower` and each tower.
- **Approach:** tune through play or `td.balance.BalanceReport` with the other placeholder entries; decide
  whether a long burn's resilience loss (crit damage up to double at the floor) is too strong.

## Enemy features

### A swarm's scatter ignores how big its members are

A swarm's members scatter within one fixed radius whatever their size, so a small swarm of a big-bodied enemy
overlaps into one clump: its members are larger than a big swarm's and get no more room. The body size is right
(it shows the health there is to remove); the room is what is missing.

- **Where:** `Wave` (the radius it hands `SpawnSpread.offsetFor`), `SpawnShape.swarm`.
- **Approach:** widen the scatter radius with the member's size instead of shrinking the bodies, held to what the
  path's width allows. Judge it on a `PreviewBoard` render of a small and a full swarm of every body.

### Enemy traits/abilities numbers are unbalanced placeholders

Every number introduced by the data-driven enemy model - `HurtSpeedTrait`'s factors, and the
Warden/egg chain's health, price, ability intervals, shield percentages/radii and
`EGG_HATCH_DELAY_TICKS` - was chosen to be plausible, not tuned, the same situation the
tower-upgrade and new-tower-numbers entries above were in before their own balance passes.

- Heals are starting values, meant to be visibly significant: the Mender's ally heal, self-heal and last rites,
  the Warden's neglect heal, the Runner boss's idle heal and the Frenzy Spawnling's death heal.
- Shields are absorb pools with starting values: the Warden's magic self-shield, its ally shield and crit-survived
  shield, the Warding affix's ally shield and the Reaver's panic shield.
- Every ranked enemy's health and bounty are no longer placeholders: they are weighed against the Walker
  (`docs/BALANCE.md`). The numbers inside a trait or an ability, and every unique boss's, still are.
- **Where:** the enemy classes (`WardenEnemy` and the others in `td.enemy`), `HurtSpeedTrait`.
- **Approach:** play The Amber Hive through to the Warden encounter (and the other two levels,
  once they get their own late-game content) repeatedly, adjusting values until the chain and
  the migrated traits feel meaningfully tuned rather than placeholder guesses - no code or
  architecture change needed, every number here is already a named constant. `td.balance.BalanceReport`
  and the dev panel (`README.md`) make this cheap to actually do: its wave script box spawns the Warden chain's
  stages on demand without playing up to them first.

### Affix numbers are unbalanced placeholders

Every affix number (Hulking's health, speed and size, Elusive's agility per rank, the health shares of
Regenerating, Shielded and Withered, Commanding's Resolve per rank and reach, Vengeful's Sprint per rank)
is a starting value, and the built-in waves that carry one were never rebalanced for it.

- **Where:** `td.enemy.Affix` (its constants and per-rank switches).
- **Approach:** tune the numbers and the affixed waves together with `td.balance.WaveCheckReport`.

### Critical-damage numbers are unbalanced placeholders

Every tower crits for x1.5 (`AttackProfile.DEFAULT_CRIT_MULTIPLIER`), the next-gen Sniper too: its crits
come from Steady Aim's stacks and its tree (Rifling, Keen Eye, Deadeye, the Fourth Shot), whose numbers are
the feature request's paper values and were never measured in play. The Warden's on-crit-survived shield
(30% for 100 ticks) is from the first crit pass.

- **Where:** `AttackProfile.DEFAULT_CRIT_MULTIPLIER`, the Sniper's constants and nodes in `SniperTower`, and
  `WardenEnemy`'s `OnCriticalHitTakenTrigger` ability.
- **Approach:** in the bundle's final balancing, measure each Sniper build with `td.BuildHarness`; every number
  here is a named constant.

### A Larva always pupates at the same moment

A Larva spins its cocoon a fixed time after it appears, so every Larva of a column pupates on the same stretch of
path and the cocoons line up where a player learns to aim.

- **Where:** `OnceTrigger`, `AbilityEvaluator`, the per-mob `AbilityState` built in `DefinedEnemyMob`'s constructor,
  and the Larva in `AmberHiveLevel`.
- **Approach:** a once-trigger over a window of ticks, its moment drawn once per mob at spawn from
  `GameWorld.random()` so headless runs still reproduce; `AbilityText` says "once, after 6 to 14 s". Give the Larva's
  Pupate a window around its present delay.

## Interface

### A tall tower sheet scrolls sideways too

At the default window size a sheet taller than the Info pane (a Mortar's) gets a vertical scrollbar, and the rows laid
out to the pane's full width (stat values, icon rows) then no longer fit beside it, so a horizontal scrollbar shows
as well and the values are cut at the right edge.

- **Where:** `td.ui.InfoSheetDocument` (`this.width`, the full-width icons), `SheetPane.show`, which hands it the width.
- **Approach:** lay the sheet out to the width left beside a vertical scrollbar whenever one may show, or keep the
  scrollbar always shown so the width never changes; check with the Driver at the default size.

### An open tooltip may not grow when its content changes

Reported with the tower visual revamp: with the pointer still on an upgrade node after buying it, the open tooltip
takes the next node's sheet (`TooltipLayer.refreshButtons` -> `TooltipPanel.show`), and a longer text was clipped to
the old size. `TooltipPanel.layOut` sets a new size now, so the bug may be gone, or only its placement may be stale.

- **Where:** `TooltipLayer.refreshButtons`, `TooltipPanel.show`/`layOut`, `TooltipPlacement`.
- **Approach:** reproduce with the run-jtd Driver (hover a node with a short sheet whose next node's is longer, buy it
  with the pointer still there, screenshot). If it still clips or runs off the window, place the panel again after
  `show` changes its size, so it grows and stays inside the window. A live pane's tooltip follows the same path.
