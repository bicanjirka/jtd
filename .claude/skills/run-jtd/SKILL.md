---
name: run-jtd
description: Build, run and drive jTD (the Swing tower-defense app) to check what tests can't - Swing wiring (clicks, hover, panel text and layout) in the live window, and board visuals rendered headless. Use when asked to start jTD, screenshot it, click something in it, or visually verify a UI or board-rendering change.
---

## Pick the cheapest check

1. A rule or a number: a test (`GameEngineTest`), or `td.BalanceHarness`. Not this skill.
2. How the board looks (shapes, markers, beams, rings): `PreviewBoard` / `PreviewEnemy`.
   Headless, deterministic, one PNG.
3. Swing wiring (input routing, hover, panel text and layout, selection, keys): `Driver`, the
   live window.

Never re-check live what a test already asserts. One `Driver` pass per feature, after its last
phase, as one script. Assert with `text`, `state` and `enemies`; screenshot only what must be
seen, and prefer `ssboard` crops. When a step misbehaves, read `GOTCHAS.md` in this directory.

## Build (once per session)

```bash
mvn -q package -DskipTests
mvn -q dependency:build-classpath -Dmdep.outputFile=target/runtime-classpath.txt
javac -cp target/classes -d .claude/skills/run-jtd .claude/skills/run-jtd/*.java
CP="target/classes;.claude/skills/run-jtd;$(cat target/runtime-classpath.txt)"
```

`Driver` needs a real display; the previews don't. Put screenshots under
`.claude/skills/run-jtd/shots/` (gitignored).

## Driver - the live window

`cat <<'EOF' | java -cp "$CP" Driver` with one command per line; `#` starts a comment; it exits
on `quit` or end of input.

| command | what it does |
|---|---|
| `level <n>` | Loads built-in level `n` (0 Curly Path, 1 Zigzag Path, 2 Twisted Hourglass) and waits until it is installed. |
| `menu` | Back to level select, skipping the confirm dialog. Asynchronous: `sleep` after it. |
| `list [filter]` | Clickable and hoverable components as `index: Class "label"`; a filter keeps matching lines with their real indices (`list panelenemy`, `list jtextpane`). |
| `click <n>` | Clicks component `n`. |
| `hover <n> [dx dy]` | Moves the pointer onto component `n` so its hover fires; `dx dy` picks a point inside it, such as a wave-preview slot. |
| `text <n>` | Prints a text component's full content. Use it on the Info pane instead of a screenshot. |
| `key <c>` | Presses a key or chord through `Robot`: `q w e r t y u i` build a tower, `p` pause, `f` speed, `s` next wave; `ctrl+shift+d` opens the dev panel; `alt+<letter>` runs a dev panel control (`alt+h` lists them in the Info pane); `enter`, `esc`, `tab`, `space`. Debug `n` skips a wave, `x` spawns the next catalog id, `c` grants credits - only while the dev panel is open. |
| `type <text>` | Replaces the focused text field's content, such as a dev panel field after `key alt+s`; `key enter` then applies it. |
| `devlabels` | Prints the dev panel's labels: its status line, the script problem and the pointer and clicked cells. |
| `boardclick <x> <y>` | Clicks board cell (x, y): places the tower chosen with `key`, or selects a tower. |
| `clickenemy [n]` | Clicks where the `n`-th alive enemy is now and prints its effects. |
| `enemies` | One line per enemy: alive index, id, rank, health, board px, effects, fate. |
| `spawn <id> [rank]` | Spawns a catalog id (a wave token, such as `s` for Armored) at the path start. |
| `effect <kind> [ticks]` | Puts an effect (`chill`, `burn`, `poison`, `freeze`, `shield`, `invisible`, `heal`, `vulnerable`, `revealed`, `sundered`, `exposed`, `marked`, `priority`, `resonating`, `fractured`, `dazed`, `saturated`) on every alive enemy. |
| `kill` | Deals lethal damage to every enemy through the real hit path, so on-death abilities fire. |
| `setcredits <n>`, `setlives <n>` | Economy cheats. |
| `waitfor ticks <n> [ms]` | Waits until the game clock advances `n` ticks (20 per second; pausing stops it). |
| `waitfor alive <n> [ms]` | Waits until exactly `n` enemies are alive. |
| `waitfor text <n> <substring>` | Waits until text component `n` contains the substring. |
| `ss <path>` | Screenshots the whole window. |
| `ssboard <path> <x> <y> <w> <h> [zoom]` | Screenshots a block of board cells, zoomed (x3 by default), for glyphs and markers. |
| `state` | Prints game time, grid size, wave, credits, lives and score. |
| `sleep <ms>` | Last resort; prefer `waitfor`. |
| `quit` | Exits. |

`waitfor` gives up after 20 s by default and prints `TIMEOUT` with the current values.

```bash
cat <<'EOF' | java -cp "$CP" Driver
level 0
setlives 1000
spawn c elite
waitfor ticks 40
clickenemy 0
text 58
ssboard .claude/skills/run-jtd/shots/selected.png 0 9 4 4
quit
EOF
```

## PreviewBoard - a board scene, headless

`java -cp "$CP" PreviewBoard` reads commands the same way: `levels`, `level <n>`,
`credits <n>`, `lives <n>`, `place <tower> <x> <y>` (a `TowerFactory.Type` name, any case),
`upgrade <x> <y> <node name>` (buys through the real mechanism, so tick a fight to clear its gate first,
or `gates off` to waive every XP and purpose gate as the dev panel does),
`spawn <id> [rank]`, `effect <kind> [ticks]` (on every enemy), `wave`, `tick <n>`, `kill`,
`grid [off]` (the dev cell grid), `hoverplace <tower> <x> <y>` (the placement highlight on a cell),
`render <path>`, `state`, `quit`. `tick` advances
the real `GameEngine`, and `render` paints through the real `BoardRenderer` /
`Java2DFrameRenderer` pipeline. Two spawns with no `tick` between them land on the same pixel.

```bash
cat <<'EOF' | java -cp "$CP" PreviewBoard
level 0
credits 5000
place sniper 6 11
spawn s elite
tick 60
render .claude/skills/run-jtd/shots/scene.png
quit
EOF
```

## PreviewEnemy - one enemy's body and rank badge

`java -cp "$CP" PreviewEnemy <id> [rank] [out.png] [scale=64]`. It draws no markers or rings;
use `PreviewBoard` for those.

Both previews reach only `EnemyCatalog.builtIn()` ids. For a level's own or a made-up enemy,
write a scratch class that registers it with `EnemyCatalog.cloneAndAdjust` and renders like
`PreviewBoard.render`.
