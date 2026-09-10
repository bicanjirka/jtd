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
mvn test          # run the test suite
mvn -q compile    # fast syntax/type check

# run a single test
mvn test -Dtest=GameEngineTest#placingATowerOnABuildableCellChargesCreditsAndOccupiesTheCell
```

## Playing

The game opens on a level-select screen. Pick a level to start it; each level brings its own
board size, path, wave list, starting credits and starting lives.

Buy a tower from the toolbar on the right (or with `q`–`t`), then click a buildable
cell to place it. Clicking a placed tower selects it and shows its stats. Start each wave
yourself when you are ready — waves do not auto-advance.

### Controls

| Key | Action |
|-----|--------|
| `q` `w` `e` `r` `t` | Select tower 1–5 for placement |
| `Esc` | Cancel tower placement |
| `s` | Start the next wave |
| `p` | Pause / unpause |
| `f` | Cycle tick speed (normal → fast → super fast → normal) |
| `m` | Back to the level-select menu (asks to confirm mid-level) |

Mouse: move to preview placement, click to place or to select a placed tower.

### Towers

| Tower | Price | Behaviour |
|-------|-------|-----------|
| Triangle | 10 | Single target, hits whichever enemy in range is furthest along the path |
| Circle | 15 | Picks a random target in range and deals splash damage falling off with distance |
| Sunshine | 20 | Long range; a beam sweeps around it once every 2s, hitting everything it passes |
| Stardust | 25 | Short range; damages everything in range at once, ghosts included |
| Power | 20 | Passive — boosts the damage and range of nearby towers; several stack |

### Enemies

| Enemy | Behaviour |
|-------|-----------|
| Circle | Plain mob, no special ability |
| Square | Absorbs part of every hit |
| Triangle | Speeds up as it loses health |
| Ghost | Invisible to single-target towers; only area damage reaches it |
| Empty | Not a real enemy — a spacer that opens a timing gap inside a wave |

### Levels

Three levels ship with the game: **Classic Loop** (the original 20×15 winding path, 17
waves), **Zigzag Gauntlet** (a smaller board with smoothly curved corners and only 3 lives)
and **Wild Bezier Sweep** (long Bezier curves that swing wide of the authored corners).

Levels are defined as Java constants in `td.level.BuiltInLevelCatalog`, so adding one today
means a code change and a rebuild. See `TODO.md` for the planned file-based catalog.

## Logging

Each run writes its own log file under `logs/jTD-<timestamp>.log`, alongside console output.
The default level is `INFO`; per-tick and per-render detail is at `DEBUG` and is off by
default — raise the root level in `src/main/resources/logback.xml` for a deep-dive session.
Old run logs are pruned on startup. Tests use `src/test/resources/logback-test.xml` and stay
quiet.

> A passing test run still prints one `WARN` and a stack trace: `WaveScriptTest` deliberately
> feeds an unparseable token through the wave parser. Check `Failures: 0`, not the absence of
> output.

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
  tower/    the tower hierarchy, targeting and upgrade buffs
  ui/       Swing presentation, render commands and the Java2D backend
  util/     GameWorld (the composition root) and GameHost
  wave/     path geometry, smoothing and wave composition
```

The one structural rule worth knowing before changing anything: **the game engine is
headless.** `GameEngine` and the domain packages own all game state and never touch Swing;
`TowerDefense` and `td.ui` own all presentation. That is what lets the whole simulation be
driven and asserted from tests with no display.

## Documentation

- **`CLAUDE.md`** — the full architecture guide: the headless/Swing boundary, the threading
  model, code style, test conventions and the wave mini-language. Read this before making
  non-trivial changes.
- **`src/main/java/td/<package>/CLAUDE.md`** — per-package notes for the packages whose
  internals have invariants worth stating up front.
- **`TODO.md`** — the single source of truth for known gaps and future work. Inline `TODO`
  comments are deliberately not used; add an entry here instead.

## Version

`pom.xml`'s `<version>` is the single source of truth. It is filtered into
`src/main/resources/version.properties` at build time and read back at startup, so cutting a
release only means editing the pom.
