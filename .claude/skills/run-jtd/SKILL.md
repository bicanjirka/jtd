---
name: run-jtd
description: Build, run and drive jTD (the Swing tower-defense app) to check what tests can't - Swing wiring (clicks, hover, panel text and layout) in the live window, and board visuals rendered headless - and trace what happens to an enemy tick by tick (every hit and its reduction steps, effect, heal, cast and its end) in a live game. Use when asked to start jTD, screenshot it, click something in it, visually verify a UI or board-rendering change, or find out why an enemy behaved as it did in play.
---

## Pick the cheapest check

1. A rule or a number: a test (`GameEngineTest`), or `td.balance.BalanceReport`. Not this skill.
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
| `list [filter]` | Clickable and hoverable components as `index: Class "label"`; a filter keeps matching lines with their real indices (`list sheetpane`, `list statusicon`). Indices shift when a panel is added or removed, so look one up before using it. What is open on the popup layer (a confirmation's buttons) is listed last. |
| `click <n>` | Clicks component `n`. |
| `clicktext <text>` | Clicks the first showing button whose text contains `text`, such as a confirmation's `Downgrade`. |
| `hover <n> [dx dy]` | Moves the pointer onto component `n` so its hover fires; `dx dy` picks a point inside it, such as a wave-preview slot. |
| `pointer <x> <y>` | Walks the pointer to window pixel (x, y) in small steps, so panes and popups see it enter and leave. For what has no component of its own, such as a tooltip; read the pixel off an `ss`. |
| `clickat <x> <y>` | `pointer` to window pixel (x, y), then a left click there: for a popup with no index, such as the script field's suggestion list. |
| `rclickat <x> <y>` | `pointer` to window pixel (x, y), then a right click there. |
| `text <n>` | Prints a text component's full content. Use it on the Info pane instead of a screenshot. |
| `key <c>` | Presses a key or chord through `Robot`: `q w e r t y u i` build a tower, `space` pause (or play on and send the waiting wave), `d f g` normal/fast/super fast (each sends a waiting wave), `delete` sells the selected tower, `m` menu; `ctrl+shift+d` opens the dev panel; `alt+<letter>` runs a dev panel control (`alt+h` lists them in the Info pane). A part is one character or a `KeyEvent` name without `VK_` (`enter`, `esc`, `tab`, `space`, `delete`, `period`, `comma`, `left`, `f5`); an unknown name prints `ERROR` and presses nothing. |
| `keydown <k>`, `keyup <k>` | Holds one key down or lets it go, for what shows only while a key is held: `keydown alt`, `ss`, `keyup alt` for the dev panel's chips. |
| `focus` | Prints what has the keyboard focus: a field, or the frame (the board's keys). |
| `type <text>` | Replaces the focused text field's content, such as a dev panel field after `key alt+s`; `key enter` then applies it. |
| `devlabels` | Prints the dev panel's labels: its status line, the script problem and the pointer and clicked cells. |
| `boardclick <x> <y>` | Clicks board cell (x, y): places the tower chosen with `key`, or selects a tower. |
| `clickenemy [n]` | Clicks where the `n`-th alive enemy is now and prints its effects. |
| `enemies` | One line per enemy: alive index, `#id` (the one its trace lines carry), catalog id, rank, health, board px, effects, fate. |
| `trace [#id]` | Prints this run's `OBSERVE` lines from its session log: every observed enemy's, or one id's. |
| `spawn <id> [rank]` | Spawns a catalog id (a wave token, such as `armored` for Armored) at the path start. |
| `effect <kind> [ticks]` | Puts an effect (`chill`, `burn`, `poison`, `freeze`, `shield`, `invisible`, `heal`, `vulnerable`, `revealed`, `sundered`, `exposed`, `marked`, `priority`, `resonating`, `fractured`, `dazed`, `saturated`, `charged`, `doom`, `blight`, `contagion`, `rime`, `ash`, `sprinting`) on every alive enemy. |
| `kill` | Deals lethal damage to every enemy through the real hit path, so on-death abilities fire. |
| `setcredits <n>`, `setlives <n>` | Economy cheats. |
| `waitfor ticks <n> [ms]` | Waits until the game clock advances `n` ticks (20 per second; pausing stops it). |
| `waitfor alive <n> [ms]` | Waits until exactly `n` enemies are alive. |
| `waitfor text <n> <substring>` | Waits until text component `n` contains the substring. |
| `ss <path>` | Screenshots the whole window. |
| `ssboard <path> <x> <y> <w> <h> [zoom]` | Screenshots a block of board cells, zoomed (x3 by default), for glyphs and markers. |
| `resize <w> <h>` | Resizes the window, as dragging its edge would. |
| `state` | Prints game time, grid size, wave, credits, lives and score. |
| `sleep <ms>` | Last resort; prefer `waitfor`. |
| `quit` | Exits. |

`waitfor` gives up after 20 s by default and prints `TIMEOUT` with the current values.

```bash
cat <<'EOF' | java -cp "$CP" Driver
level 0
setlives 1000
spawn armored elite
waitfor ticks 40
clickenemy 0
list SheetPane   # the Info pane's index, which moves whenever a panel is added
text 62
ssboard .claude/skills/run-jtd/shots/selected.png 0 9 4 4
quit
EOF
```

## Tracing an enemy

The dev panel's Observe makes an enemy write one `OBSERVE t=<tick> #<id> <Name> <Rank> <EVENT> ...` line
per event to the session log until its `END`: `START` (a full stat snapshot), `HIT`/`PERIODIC` with every
reduction step, `DODGE`, `EFFECT` (requested, shortened, blocked, gained, refreshed, stacks, ended...),
`HEAL`, `CAST`, `SPAWN`/`HATCH`, `KILLED`, `END` with totals. What it spawns or hatches into is observed
too. Turn it on with `key alt+m` on a selected enemy (`clickenemy` first), or `key alt+shift+s` before
spawning from the panel's script box (`key alt+s`, `type 2 armored`, `key enter`); `key alt+shift+m`
writes a snapshot of every observed enemy. Then `trace` or `trace #12`. While anything is observed the
game may run slower: a trace never drops a line.

```bash
cat <<'EOF' | java -cp "$CP" Driver
level 0
key ctrl+shift+d
key alt+shift+s
key alt+s
type armored
key enter
waitfor ticks 200
trace
quit
EOF
```

## PreviewBoard - a board scene, headless

`java -cp "$CP" PreviewBoard` reads commands the same way: `levels`, `level <n>`,
`credits <n>`, `lives <n>`, `place <tower> <x> <y>` (a `TowerFactory.Type` name, any case),
`upgrade <x> <y> <node name>` (buys through the real mechanism, so tick a fight to clear its gate first,
or `gates off` to waive every XP and purpose gate as the dev panel does),
`spawn <id> [rank]`, `effect <kind> [ticks]` (on every enemy), `zone <kind> <x> <y> [radiusCells] [ticks]`
(a ground zone no tower owns: `burning_ground`, `tar`, `frost_ground`, `fallout`), `wave`, `tick <n>`, `kill`,
`grid [off]` (the dev cell grid), `hoverplace <tower> <x> <y>` (the placement highlight on a cell),
`render <path>`, `state`, `quit`. `tick` advances
the real `GameEngine`, and `render` paints through the real `BoardRenderer` /
`Java2DFrameRenderer` pipeline. Two spawns with no `tick` between them land on the same pixel.

```bash
cat <<'EOF' | java -cp "$CP" PreviewBoard
level 0
credits 5000
place sniper 6 11
spawn simple elite
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

## PreviewIcons - every term's face

`java -cp "$CP" PreviewIcons [out.png]` (default `shots/icons.png`). A contact sheet of every effect, tower
term, trait, ability and perk-line face, each at 40 px and at 20 px with its title under it. Use it to check a
new pictogram or look; the live states (stacks, time sweep, bar, ward) are covered by `TermIconPainterTest`.
