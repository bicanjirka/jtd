---
name: run-jtd
description: Build, run, and visually drive jTD (the Swing tower-defense desktop app). Use when asked to start jTD, take a screenshot of its UI, click something in the game, run its tests, or verify a gameplay/UI change actually works end to end.
---

jTD is a native Java Swing desktop app (`td.Main` -> `new TowerDefense()`), not a browser or
Electron app - there is no DOM and no `chromium-cli`. Drive it in-process instead: launch
`Driver.java` (in this directory), which constructs the real `TowerDefense` JFrame inside its
own `main()` and then reads one command per line from stdin (`list`, `click <n>`, `key <c>`,
`boardclick <x> <y>`, `level <n>`, `menu`, `setcredits <n>`, `setlives <n>`, `ss <path>`,
`state`, `sleep <ms>`, `quit`). All paths below are relative to the repo root
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

| command | what it does |
|---|---|
| `list` | Prints every clickable component (buttons, and plain panels with their own mouse listener like the level-select cards) as `<index>: <ClassName> "<label>" visible=<bool>`, in a stable DFS order. |
| `click <n>` | Clicks the component at that `list` index - `doClick()` for a real button, a `Robot` click at its on-screen center for anything else (e.g. a level card). |
| `key <char>` | Types one character via `Robot`, for jTD's frame-level keyboard shortcuts (`q`/`w`/`e`/`r`/`t` build, `p` pause, `f` speed, `s` next wave) - these have no button, only a `KeyListener` on the JFrame. |
| `boardclick <x> <y>` | Clicks board cell `(x, y)` - the board itself isn't a Swing button/mouse-listener component `click(int)` can reach, so this reflects into the private `gameBoard` field and `GameWorld.getBoard().scale()` to compute the on-screen point instead. Use it after `key` selects a tower type, to actually place it. |
| `level <n>` | Selects level `n` from the built-in catalog (0 = Classic Loop, 1 = Zigzag Gauntlet) by reflecting into the private `levelCatalog` field and `startSelectedLevel(LevelDefinition)`, instead of clicking a `PanelLevelSelect` card - see Gotchas for why `click` on a card is unreliable here. Works whether or not a level is already loaded (`startSelectedLevel` is safe to call repeatedly - it tears down and rebuilds). |
| `menu` | Returns to the level-select menu by reflecting into the private `returnToMenu()` - deliberately *not* `requestReturnToMenu()`, which can pop a real confirm `JOptionPane` mid-level that this driver has no way to answer and would hang on. Testing that dialog needs a human or a `Robot` click on it, not this driver. |
| `setcredits <n>` | Cheat: sets credits to exactly `n` via `GameWorld.apply(EconomyDelta.credits(...))`, instead of buying/selling towers to reach a target value indirectly. Goes through the same `GameWorld.apply` path a real kill or purchase uses, so affordability/UI reactions (e.g. toolbar buttons graying out) still fire normally. |
| `setlives <n>` | Cheat: sets lives to exactly `n` the same way, for testing near-game-over/game-over states without playing a level down to it. |
| `ss <path>` | Screenshots just the app window (via `Robot`, using the live `JFrame`'s own `getLocationOnScreen()`/`getSize()`) and writes a PNG. |
| `state` | Reflects into the private `engine`/`context` fields and prints cell-grid size, wave progress, credits/lives/score - for asserting on outcomes without eyeballing a screenshot. |
| `sleep <ms>` | Pause between commands - Swing needs a beat to repaint/relayout after a click before the next screenshot is meaningful. |
| `quit` | Turns off always-on-top and exits cleanly. |

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

221 tests, all pass. A green run still prints one `WARN` + stack trace from `WaveTest`
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
