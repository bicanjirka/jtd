# jTD — a Swing tower defense game

A desktop tower-defense game written in plain Java and Swing. Enemies walk a fixed path
across a grid board; you spend credits placing towers on the buildable cells beside it and
try to kill everything before it laps the path and costs you a life.

All of the artwork is vector — every enemy, tower, beam and path marker is a
`java.awt.Shape` built and painted in code. There are no image assets in the project.

## Requirements

- **JDK 26** (the build targets release 26)
- **Maven 3.9+**

## Build and run

```bash
mvn package               # build target/jTD.jar
java -jar target/jTD.jar  # run the game
```

The jar is shaded, so it bundles its SLF4J/Logback dependencies and needs no extra classpath.

Other useful commands:

```bash
mvn verify        # rules check + test suite — the standing check before a commit
mvn test          # run the test suite
mvn -q compile    # fast syntax/type check

# run a single test
mvn test -Dtest=GameEngineTest#placingATowerOnABuildableCellChargesCreditsAndOccupiesTheCell
```

## Playing

The game opens on a level-select screen. Pick a level to start it; each level brings its own
board size, path, wave list, starting credits and starting lives.

Buy a tower from the toolbar on the right (or with `q`–`t`), then click a buildable
cell to place it. Clicking a placed tower selects it and shows its stats and its own upgrade
tree — three slots, `base`/`head`/`special`, each offering a small number of nodes to buy.
Start each wave yourself when you are ready — waves do not auto-advance.

### Controls

| Key                             | Action                                                                                               |
|---------------------------------|------------------------------------------------------------------------------------------------------|
| `q` `w` `e` `r` `t` `y` `u` `i` | Select tower 1–8 for placement                                                                       |
| `Esc`                           | Cancel tower placement                                                                               |
| `1`–`9`                         | With a tower selected, buy the correspondingly-numbered upgrade node it currently offers             |
| `s`                             | Start the next wave                                                                                  |
| `p`                             | Pause / unpause                                                                                      |
| `f`                             | Cycle tick speed (normal → fast → super fast → normal)                                               |
| `m`                             | Back to the level-select menu (asks to confirm mid-level)                                            |
| `n`                             | Debug: clear the current wave with no penalty and start the next one                                 |
| `x`                             | Debug: spawn one instance of the next enemy type in the level's catalog, cycling through all of them |
| `c`                             | Debug: grant a lump sum of credits                                                                   |

Mouse: move to preview placement, click to place or to select a placed tower, or click an
upgrade-tree button to buy that node. Click a moving enemy (when not placing a tower) to inspect
it: the side panel shows its live health, stats, effects and traits, updating even while paused,
and keeps a "Killed" or "Leaked" line after it's gone until your next click. Hovering an enemy in
the wave preview shows the same stat block for that enemy type.

The `n`/`x`/`c` debug keys are always available, not gated behind a build flag — they exist to
make manual playtesting cheap (see `docs/features/FEATURE-playtesting-and-balance-tooling.md`). There's also
`td.BalanceHarness`, a headless batch simulation: it drives a level to completion with a fixed
tower loadout and no human input, then prints lives lost, ticks-to-clear per wave, and each
tower's kills/damage dealt.

```bash
mvn -q package -DskipTests
mvn -q dependency:build-classpath -Dmdep.outputFile=target/runtime-classpath.txt
java -cp "target/classes;$(cat target/runtime-classpath.txt)" td.BalanceHarness
```

Run with no arguments it plays one built-in loadout against Curly Path; edit `main()` to try
a different loadout or level, since v1 has no config format for either.

### Towers

| Tower  | Price | Behaviour                                                                                                                                           |
|--------|-------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| Sniper | 10    | Single target, hits whichever enemy in range is furthest along the path                                                                             |
| Splash | 15    | Picks a random target in range and deals splash damage falling off with distance                                                                    |
| Sonar  | 20    | Long range; a beam sweeps around it once every 2s, hitting everything it passes                                                                     |
| Pulse  | 25    | Short range; damages everything in range at once, ghosts included                                                                                   |
| Aura   | 20    | Passive — boosts the damage and range of nearby towers; several stack                                                                               |
| Mortar | 30    | Lobs a slow, unguided shell at whichever enemy in range is furthest along the path; splashes physical damage and slows everything the blast reaches |
| Seeker | 35    | Fires a homing missile that re-aims each tick and retargets if its target dies; deals magic damage and freezes whichever mob it actually hits       |
| Cinder | 28    | No cooldown; a slowly-reorienting flame cone burns everything currently caught in it, ghosts included                                               |

Every attack tower — Sniper, Splash, Sonar, Pulse, Mortar, Seeker and Cinder — also
offers two permanent, mutually-exclusive upgrade paths, shown as buttons in its info panel
once selected. A path is gated by its own condition (an affordable price alone, a cluster of
towers built nearby, the tower having dealt enough damage, or having racked up enough kills)
— choosing one is a one-time, irreversible specialization for that specific tower, marked on
the board by a coloured ring around it. The Aura tower is passive and offers none, but draws a
faint line to every tower it's currently amplifying.

### Enemies

| Enemy      | Looks like    | Behaviour                                                                                                                                                                                                                                                                                                         |
|------------|---------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Simple     | circle        | Plain mob, no special ability                                                                                                                                                                                                                                                                                     |
| Armored    | square        | Absorbs part of every hit; immune to critical hits                                                                                                                                                                                                                                                                |
| Frenzied   | triangle      | Speeds up as it loses health                                                                                                                                                                                                                                                                                      |
| Ghost      | tinted circle | Turns invisible to single-target towers for ~10s the first time it's hit (area damage still reaches it); at Elite and Boss rank, also permanently shrouds every other ally near it                                                                                                                              |
| Mender     | cross         | Deals no damage of its own; periodically restores health to every other ally near it                                                                                                                                                                                                                             |
| Jammer     | square        | Jams every tower near it: they fire more slowly and see less far while it's in range (drawn as a pink ring; affected towers show a pink marker). Not in any level's waves yet.                                                                                                                                  |
| Empty      | —             | Not a real enemy — a spacer that opens a timing gap inside a wave                                                                                                                                                                                                                                                 |
| The Warden | large spiked crest | A boss: armored, periodically summons reinforcements and shields itself and nearby allies, and shields itself again whenever it survives a critical hit. On death it leaves behind a stationary egg that hatches back into a weaker Warden if not destroyed in time — the fight only ends once an egg is defeated before it hatches. Appears as the final wave of Curly Path. |
| Reaver     | triangle, path-facing | Speeds up as it's hurt, shrugs off a flat amount of every hit, shields itself once badly wounded, and splits into two Simple mobs on death. Appears in Twisted Hourglass's first wave — a level-authored enemy, registered only for that level rather than globally (see `td.level.LevelDefinition.customEnemies`). |

### Rank

Every enemy spawns at one of five named ranks — **Grunt, Soldier, Veteran, Elite, Boss** — shown
as a badge on its body rather than as a number. A wave declares the rank its slots default to;
any slot can name a different rank inline, before the enemy id (`elite c`) or before a spawn
shape (`elite swarm 4 c`). Health and bounty both rise with rank, and a kill's score is weighted
by it too — a Boss-rank kill is worth disproportionately more than a Grunt-rank one of the same
bounty. Not every enemy defines all five ranks; asking for one an enemy doesn't have silently
uses its own highest defined rank instead. Elite and Boss Simple and Mender enemies carry adaptive
armor: it resists whichever damage type (physical or magic) has landed most this level, from not
at all at an even split up to full strength against a one-type defence. Every Elite and Boss
enemy also shrugs off repeated freezes: within 10 seconds of the last one, each fresh freeze
lasts half as long as the one before, and a fourth doesn't take hold at all.

### Spawn shapes

A wave's script can shape how a slot spawns, not just what it spawns — write the shape's
keyword before the enemy id (`armored warden1`), or before a member count and the enemy id for
the shapes that take one (`swarm 4 c`):

| Shape   | Keyword  | What one slot produces                                                |
|---------|----------|-------------------------------------------------------------------------|
| Normal  | (none)   | One enemy on the path centre                                            |
| Armored | `armored`| One enemy with extra physical-only armor, on top of any armor it already has: it blocks 20% of physical damage, rising to 80% as the level's landed damage leans fully physical — no size, speed, health or bounty change |
| Swarm   | `swarm`  | *N* enemies at 50% size, scattered off-path, sharing one spawn's bounty and health |
| Line    | `line`   | *N* enemies spread evenly across the path's width, abreast              |
| Flank   | `flank`  | Two enemies hugging opposite edges of the path                          |
| Column  | `column` | *N* enemies in a tight single file, closer together than *N* separate spawns |
| Drip    | `drip`   | *N* enemies stretched over more time than *N* separate spawns           |

### Levels

Three levels ship with the game: **Curly Path** (a 20×15 board whose single lane spirals
through two tight loops before unwinding into a zigzag, 17 waves), **Zigzag Path** (a smaller,
16×11 board where a fuchsia lane hooks back on itself and a lime lane cuts across it twice, 8
waves per lane, only 3 lives) and **Twisted Hourglass** (a tight, 9×14 portrait board where three
Bezier lanes - crimson, teal and amber - twist through a pinched waist so the layout reads as an
hourglass, 10 waves per lane). A level can define more than one enemy path, each with its own color, waves
and pace; Zigzag Path and Twisted Hourglass are the two built-in levels that do. A level's
paths all run the same number of waves as synchronized rounds - starting a round spawns every
path's wave for it together. A level can also register its own enemy on top of the ones above,
scoped to that level only; Twisted Hourglass's Reaver (see the Enemies table) is the one
built-in level that does.

Levels are defined as Java constants in `td.level.BuiltInLevelCatalog`, so adding one today
means a code change and a rebuild. See `TODO.md` for the planned file-based catalog.

## Logging

Each run writes its own log file under `logs/jTD-<timestamp>.log`, alongside console output.
The default level is `INFO`; per-tick and per-render detail is at `DEBUG` and is off by
default — raise the root level in `src/main/resources/logback.xml` for a deep-dive session.
Old run logs are pruned on startup. Tests use `src/test/resources/logback-test.xml` and stay
quiet.

## Project layout

```
src/main/java/td/
  GameEngine, GameLoop, TowerDefense   entry points and the loop
  board/    a level's pixel scale and cell↔pixel math
  cell/     board squares and buildability
  damage/   the damage value type
  economy/  credits, score and lives
  enemy/    the enemy mob hierarchy
  level/    level definitions and the level catalog
  tower/    the tower hierarchy, targeting, aura buffs and upgrade paths
  ui/       Swing presentation, render commands and the Java2D backend
  util/     GameWorld (the composition root) and GameHost
  wave/     path geometry, smoothing and wave composition
```

The one structural rule worth knowing before changing anything: **the game engine is
headless.** `GameEngine` and the domain packages own all game state and never touch Swing;
`TowerDefense` and `td.ui` own all presentation. That is what lets the whole simulation be
driven and asserted from tests with no display.

## Documentation

- **`CLAUDE.md`** — the constraints: the headless/Swing boundary, the threading rule, code
  style, conventions, test conventions and the wave mini-language. Read this before making
  non-trivial changes. It deliberately holds rules only, no rationale.
- **`docs/ARCHITECTURE.md`** — the *why* behind those constraints: how the render pipeline is
  layered, why the art is vector, how levels load and unload, what the threading rule is
  protecting against, and the design history.
- **`docs/features/`** — design documents for features that have shipped, kept as a record.
- **`src/main/java/td/<package>/CLAUDE.md`** — per-package notes for the packages whose
  internals have invariants worth stating up front.
- **`TODO.md`** — the single source of truth for known gaps and future work. Inline `TODO`
  comments are deliberately not used; add an entry here instead.

## Version

`pom.xml`'s `<version>` is the single source of truth. It is filtered into
`src/main/resources/version.properties` at build time and read back at startup, so cutting a
release only means editing the pom.
