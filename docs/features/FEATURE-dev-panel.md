# Dev Panel (handoff)

**Status: in progress, v1.** Engine side committed (`9660564`). UI side committed after a green
`mvn verify`, but not yet run live. Pick up at "Remaining steps" step 3.

## What it is

A hidden playtesting panel, toggled with **Ctrl+Shift+D**, shown as a strip between the board and
the tower buttons. Only the developer plays, so it ships in the jar behind the shortcut.

## Decisions (settled with the user)

- Shortcut Ctrl+Shift+D (root-pane binding, `WHEN_IN_FOCUSED_WINDOW`, so it works while a panel
  text field has focus). Listed in the controls text (`TowerDefense.statusMessage`).
- The bare debug keys `n` (skip wave), `x` (spawn next catalog enemy), `c` (+1000 credits) work
  **only while the panel is open**.
- Placement: strip between board and tower bar; the window grows by the panel's height when it
  opens and shrinks when it closes.
- v1 only. Dropped from v1: a 10x speed (super fast is already 16.7x), a separate "spawn enemy"
  field (a one-token wave script does it, and supports paths).
- Must not break `run-jtd`: the panel is added **last** to `jPanel_game`, so the Driver's `list`
  indices for everything else stay the same.

## UI layout

```
Dev · ctrl+shift+d
Credits [______] [Set]   Lives [___] [Set]   [∞ Lives] [Free build]
Spawn [_____ wave script _____] [Rank: grunt] [Path 1/1] [Go]
[Skip wave]  Jump to [__] [Go]  [Restart]  [Kill all] [Clear all] [Step]
Tower: XP [____] [Grant] [Ignore gates] [Reset]      <status / error text>
```

Enter in a text field runs that row's action. Rank and Path are cycling buttons.

## Design (as built)

- **`td.DevControls`** (headless, committed): set credits/lives; infinite lives and free build as
  an `EconomyListener` that tops lives / credits (to 1,000,000) back up; `spawnWave(script, rank,
  path)` parses with `WaveScript` (a parse failure is reported, not fatal) and queues a `Wave` on
  the chosen path; `killAll` (pays bounty and XP through a real hit), `clearAll` (pays nothing);
  `grantXpToSelected`; `setUpgradeGatesIgnored`; `pathCount()`; `selectedTower()`. Enemy and tower
  changes queue and run in `runPending()` on the game-loop thread.
- **`td.util.PlaytestRules`** on `GameWorld` (committed): `upgradeGatesIgnored`, read by
  `UpgradeNode.gateMet`. `UpgradeOffer` now carries `gateMet` as a component.
- **`GameEngine.debugJumpToWave(n)`** (committed), beside `debugSkipCurrentWave`.
- **`td.ui.HudTextField`** (uncommitted): `JTextField` on `BasicTextFieldUI`, HUD colours.
- **`td.ui.PanelDev`** (uncommitted): calls `DevControls` directly; lifecycle actions go up via
  `onSkipWave`, `onJumpToWave`, `onRestart`, `onStep`, `onResetTower`.
- **`TowerDefense`** (uncommitted): `devControls` field and economy listener; `runPending()` at
  the start of `buildAndPublishFrame` (runs while paused too); `toggleDevPanel`,
  `debugJumpToWave`, `restartLevel` (re-installs `currentLevel`), `stepOneTick` (pauses, then one
  tick), `rebuildSelectedTower` (sell, place a fresh copy through `engine.mouseClicked`, select
  it: resets upgrades and XP); `debugKeyTyped` gated on the panel; layout: dev panel `gridy=1`,
  tower selector `gridy=2`, console `gridheight=3`; panel hidden on return to menu; re-adds its
  height on level install.

## Remaining steps

1. `mvn verify`; fix anything Checkstyle/Spotless flags.
2. Commit the UI ("Add the dev panel behind Ctrl+Shift+D").
3. `run-jtd`: add a Driver command `devpanel` that presses Ctrl+Shift+D through `Robot`;
   document it in `SKILL.md`, and note there that `key n/x/c` need the panel open.
4. One live `run-jtd` pass: open the panel, check the layout fits the narrowest level width and
   that the window grows/shrinks, Enter in fields, set credits, spawn `3 elite swarm 4 c` on path
   2 of Twisted Hourglass, kill all, jump to wave, step while paused, grant XP and Reset on a
   selected tower, a bad script shows its error. Fix layout from screenshots.
5. Docs in the same commit as the change they describe:
   - `README.md` controls table: replace the `n`/`x`/`c` rows with Ctrl+Shift+D and say the debug
     keys work only while the panel is open.
   - Root `CLAUDE.md`: "Unloadable content throws `GameStartupException`, which only `Main`
     catches" now has one exception, the dev panel's wave script box.
   - `td/ui/CLAUDE.md`: a text input is a `HudTextField` (basic UI, never the platform look).
   - `docs/ARCHITECTURE.md` §9 mentions the `n`/`x`/`c` keybindings; add the dev panel.
6. Mark this doc implemented.

## Later (v2 ideas, not started)

DPS meter per tower, apply an effect to every enemy, freeze enemies in place, add deeds to the
selected tower, range / health overlays, copy the loadout as `TowerPlacementSpec` lines for
`BalanceHarness`.
