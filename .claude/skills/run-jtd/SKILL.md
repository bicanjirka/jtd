---
name: run-jtd
description: Build, run, and visually drive jTD (the Swing tower-defense desktop app). Use when asked to start jTD, take a screenshot of its UI, click something in the game, run its tests, or verify a gameplay/UI change actually works end to end.
model: haiku
context: fork
---

jTD is a native Java Swing desktop app (`td.Main` -> `new TowerDefense()`), not a browser or
Electron app - there is no DOM and no `chromium-cli`. Drive it in-process instead: launch
`Driver.java` (in this directory), which constructs the real `TowerDefense` JFrame inside its
own `main()` and then reads one command per line from stdin (`list`, `click <n>`,
`hover <n>`, `key <c>`, `boardclick <x> <y>`, `level <n>`, `menu`, `setcredits <n>`,
`setlives <n>`, `spawn <id>`, `kill`, `ss <path>`, `state`, `sleep <ms>`, `quit`). All paths below are relative to the repo root
(`C:\Users\juras\dev\jTD`).

## Prerequisites

JDK 26 and Maven, already on PATH in this environment (`mvn -version` / `java -version` to
check). No OS packages needed - this was verified on Windows with a real display attached, not
headless; `java.awt.Robot` and screen capture need an actual display (physical, RDP, or a
Windows equivalent of Xvfb), which this driver has not been tested without.

## Build

```bash
mvn -q package -DskipTests
```

Produces `target/classes` (what the driver runs against) and `target/jTD.jar`.

The driver needs jTD's runtime dependencies (SLF4J, Logback) on its classpath. Get the exact
jars Maven resolves - don't hand-guess versions under `~/.m2`, since a glob can match the wrong
jar (a `-sources.jar` in one case) or a stale version:

```bash
mvn -q dependency:build-classpath -Dmdep.outputFile=target/runtime-classpath.txt
```

Compile the driver against the already-built classes:

```bash
javac -cp target/classes -d .claude/skills/run-jtd .claude/skills/run-jtd/Driver.java
```

## Run (agent path)

```bash
CP="target/classes;.claude/skills/run-jtd;$(cat target/runtime-classpath.txt)"
java -cp "$CP" Driver
```

(On Windows the classpath separator is `;`, matching what `dependency:build-classpath` already
wrote into `runtime-classpath.txt`.)

It launches the real window and waits for commands on stdin, one per line. Pipe a command file
or a heredoc in; the process exits on `quit` or EOF.

```bash
cat <<'EOF' | java -cp "$CP" Driver
list
ss .claude/skills/run-jtd/shots/menu.png
click 0
sleep 300
ss .claude/skills/run-jtd/shots/game.png
state
quit
EOF
```

Screenshots land wherever you point `ss` - `.claude/skills/run-jtd/shots/` above is a
convenient scratch location, already gitignored-worthy (don't commit captured PNGs).

| command              | what it does                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `list`               | Prints every clickable component (buttons, and plain panels with their own mouse listener like the level-select cards) as `<index>: <ClassName> "<label>" visible=<bool>`, in a stable DFS order.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| `click <n>`          | Clicks the component at that `list` index - `doClick()` for a real button, a `Robot` click at its on-screen center for anything else (e.g. a level card).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `hover <n> [dx dy]`  | Moves the pointer onto the component at that `list` index without clicking, so its `mouseEntered` fires - jTD shows a tower's pre-purchase stats on toolbar hover, and `click` can't reach that (a `JToggleButton` takes `click`'s `doClick()` path, which generates no mouse-entered event). Moves away from the target first, since a move to where the pointer already rests fires nothing. Optional `dx dy` target a point inside the component rather than its centre - needed for a panel that maps the pointer's x to an item, such as `PanelEnemy`'s wave preview, where a centre hover lands past every mob. `list` finds hover-only components (those with just a `MouseMotionListener`) as well as clickable ones. |
| `key <char>`         | Types one character via `Robot`, for jTD's frame-level keyboard shortcuts (`q`/`w`/`e`/`r`/`t` build, `p` pause, `f` speed, `s` next wave, plus three debug cheats - `n` skips the current wave with no penalty, `x` spawns the next id in the level's `EnemyCatalog.ids()` cycling order, `c` grants a lump credit sum) - these have no button, only a `KeyListener` on the JFrame. Prefer `spawn <id>` below over counting `x` presses when a specific enemy is the point of the test.                                                                                                                                                                                                                                    |
| `boardclick <x> <y>` | Clicks board cell `(x, y)` - the board itself isn't a Swing button/mouse-listener component `click(int)` can reach, so this reflects into the private `gameBoard` field and `GameWorld.getBoard().scale()` to compute the on-screen point instead. Use it after `key` selects a tower type, to actually place it.                                                                                                                                                                                                                                                                                                                                                                                                             |
| `level <n>`          | Selects level `n` from the built-in catalog (0 = Classic Loop, 1 = Zigzag Gauntlet, 2 = Wild Bezier Sweep) by reflecting into the private `levelCatalog` field and `startSelectedLevel(LevelDefinition)`, instead of clicking a `PanelLevelSelect` card - see Gotchas for why `click` on a card is unreliable here. Works whether or not a level is already loaded (`startSelectedLevel` is safe to call repeatedly - it tears down and rebuilds). **Asynchronous**: it stops the loop on a lifecycle thread and installs the level on a later EDT pulse, so follow it with `sleep 1000` before `ss` or `state`.                                                                                                              |
| `menu`               | Returns to the level-select menu by reflecting into the private `returnToMenu()` - deliberately *not* `requestReturnToMenu()`, which can pop a real confirm `JOptionPane` mid-level that this driver has no way to answer and would hang on. Testing that dialog needs a human or a `Robot` click on it, not this driver. **Asynchronous**, like `level` - `sleep` before asserting.                                                                                                                                                                                                                                                                                                                                          |
| `setcredits <n>`     | Cheat: sets credits to exactly `n` via `GameWorld.apply(EconomyDelta.credits(...))`, instead of buying/selling towers to reach a target value indirectly. Goes through the same `GameWorld.apply` path a real kill or purchase uses, so affordability/UI reactions (e.g. toolbar buttons graying out) still fire normally.                                                                                                                                                                                                                                                                                                                                                                                                    |
| `setlives <n>`       | Cheat: sets lives to exactly `n` the same way, for testing near-game-over/game-over states without playing a level down to it.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `spawn <id>`         | Spawns one instance of an exact `EnemyCatalog` id (e.g. `warden1`) immediately at the path's start - no wave, no towers, no waiting needed. Mirrors the game's own `x` debug key (`GameEngine.debugSpawnNextCatalogEnemy`) but goes straight to the id asked for instead of cycling `EnemyCatalog.ids()` and counting presses. The right tool whenever a test's point is "does this specific enemy render/behave correctly," including a boss or any other late-game/ability-spawned id - see Gotchas for why driving real waves to reach one is a trap. **Caveat**: this places the mob at the path's raw start point (`distanceIntoLap` 0), which some levels (Classic Loop included) author one cell off-board on purpose so a *moving* enemy visibly walks in - a zero-speed definition (the Warden's egg) spawned this way just sits off-screen forever. Use `kill` below to reach it through the real on-death path instead.                                                                                                                                                                    |
| `kill`               | Deals lethal damage to every currently-alive enemy via `EnemyMob.doDamage`, the same path a tower's hit uses - so an on-death ability (the Warden's egg-spawn, which repositions onto the board via `spawnAtSamePositionAs`) fires exactly as it would in real combat. Pair with `spawn warden1` + a short `sleep` (so it visibly walks onto the board first) to reach the Warden's egg the way real play does, rather than via `spawn`'s off-board caveat above.                                                                                                                                                    |
| `ss <path>`          | Screenshots just the app window (via `Robot`, using the live `JFrame`'s own `getLocationOnScreen()`/`getSize()`) and writes a PNG.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `state`              | Reflects into the private `engine`/`context` fields and prints cell-grid size, wave progress, credits/lives/score - for asserting on outcomes without eyeballing a screenshot.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `sleep <ms>`         | Pause between commands - Swing needs a beat to repaint/relayout after a click before the next screenshot is meaningful.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `quit`               | Turns off always-on-top and exits cleanly.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |

## Previewing one enemy's shape/badge (no window, no level)

For "does this rank's body/badge look right" - a shape, a palette, the Boss skull badge -
`Driver.java`'s full round trip (build window, load a level, `spawn`, screenshot, `quit`) is
much more than the question needs. `PreviewEnemy.java` (also in this directory) renders one
enemy directly to a PNG with no `TowerDefense`/`JFrame`/`Robot` at all - it builds a throwaway
`GameWorld` the same way `td.ui.PanelEnemy` already does for its in-game wave-preview strip, and
paints through the same `Java2DFrameRenderer.paintEnemies` the real board uses, so it can't drift
from the real in-game look:

```bash
javac -cp target/classes -d .claude/skills/run-jtd .claude/skills/run-jtd/PreviewEnemy.java
java -cp "target/classes;.claude/skills/run-jtd;$(cat target/runtime-classpath.txt)" PreviewEnemy <id> [rank] [outputPath] [scale]
```

`id` is an `EnemyCatalog.builtIn()` id (the wave-script token, e.g. `s` for Armored - see
`td/wave/CLAUDE.md`'s token table), `rank` defaults to `GRUNT`, `outputPath` defaults to
`.claude/skills/run-jtd/shots/preview_<id>_<rank>.png`, `scale` (px/cell) defaults to `64`.

```bash
java -cp "$CP" PreviewEnemy s boss .claude/skills/run-jtd/shots/armored_boss.png
```

Only covers body + rank badge (what `PanelEnemy` itself shows) - no status-effect markers or
overlay rings, since those need a live simulation tick to have anything to show; that still
needs a real `Driver.java` `spawn`. It also only reaches `EnemyCatalog.builtIn()`'s ids, not a
level's own custom/cloned enemies, which aren't registered anywhere outside that level's load.

## Run (human path)

```bash
java -jar target/jTD.jar
```

Opens the window normally; close it or Ctrl-C the process to stop. No driver, no screenshots -
only useful for a human actually looking at the screen.

## Test

```bash
mvn test
```

231 tests, all pass. A green run still prints one `WARN` + stack trace from `WaveScriptTest`
(deliberately feeds the wave parser a bad token) - expected, not a failure; check
`Tests run: … Failures: 0`, not the console output.

---

## Gotchas

- **Don't drive it from a separate screenshotting process.** The first approach tried this
  session was: launch `java -jar target/jTD.jar` as a background process, then from a *different*
  PowerShell process call `SetForegroundWindow` + `GetWindowRect` + `CopyFromScreen` to bring it
  forward and screenshot it. `SetForegroundWindow` silently no-ops for a background-launched
  process (Windows' foreground-lock protection) - the call returns without error, focus doesn't
  actually change, and the resulting screenshot captures whatever unrelated window happens to be
  on top instead of the app. On a real desktop with other windows open, that's a real risk of
  capturing unrelated (possibly sensitive) on-screen content instead of jTD. `Driver.java` sidesteps
  this entirely by running in the *same* JVM that owns the window, so `game.toFront()` /
  `game.setAlwaysOnTop(true)` is the app raising its own window - not an external process fighting
  the OS - and it reliably works.
- **Cross-process screenshot coordinates can also be DPI-mismatched.** Even after fixing focus,
  a plain (non-DPI-aware) PowerShell process calling `GetWindowRect` (physical pixels) and then
  `CopyFromScreen` (logical pixels, or vice versa depending on awareness) can capture the wrong
  region entirely - one run captured a monitor-sized area offset from the actual window. Getting
  the bounds from the live `JFrame` object in-process (`game.getLocationOnScreen()` /
  `game.getSize()`) avoids this since there's only one process's, one toolkit's, notion of pixels
  involved.
- **Clicking a level-select card via `click <n>`'s `Robot` fallback did not reliably register
  in this environment.** `PanelLevelSelect` cards are plain `JPanel`s with their own
  `MouseListener`, not `AbstractButton`s, so `click` falls back to a `Robot` mouse move/press/
  release at the card's on-screen center - unlike a real `JButton`, where `click` uses
  `doClick()` and that works fine. A screenshot taken right after such a click showed the level
  still hadn't loaded (`state`'s `cellGrid` stayed `null`). Use `level <n>` instead, which
  selects a level by reflection with no `Robot` involved.
- **`list`'s `visible=` column is `Component.isVisible()`, not "currently on screen."** jTD's
  content pane is a `CardLayout` with a "menu" card and a "game" card; both cards' components
  exist in the tree the whole time; `CardLayout` hides the non-current card at the container
  level, not by flipping every descendant's own visibility flag. So e.g. the "Game Over!" overlay
  panel shows `visible=true` in `list` even while the menu card (not the game card) is the one
  actually on screen. Don't use `list`'s visible column as a proxy for "is this actually showing
  right now" - use a screenshot for that.
- **Don't drive real waves with `key s` to reach late-game content, with no towers built.**
  A level's starting lives are small (Classic Loop: 5) and every unopposed enemy in a wave
  leaks one; a script that just presses `s` and sleeps, hoping waves clear on their own, hits
  game-over within the first wave or two and then sits there forever no matter how long it
  sleeps - this is what actually happened trying to reach Classic Loop's wave-18 Warden boss
  this way. Use `spawn <id>` (or `key n`/`key x`, the game's own debug cheats) instead - both
  work the moment a level is loaded, need no wave in progress, and can't trigger game-over.
