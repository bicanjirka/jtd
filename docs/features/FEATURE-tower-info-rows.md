# Feature Request: Tower info as rows

## Summary

Give towers the same info-panel treatment enemies got: every stat, behaviour and upgrade is
one line, marked with the glyph and colour the board already draws for it, with values in a
right-aligned column. This covers the tower's shop description, the selected tower's status, an
upgrade's hover text, and the Upgrades panel itself. Today all four are plain text blocks or
bare buttons, and the selected-tower view overflows the pane the moment a tower is built.

## Current state (what exists today)

**The enemy side is the reference.** Commits `a6729a0` and `00e146c` replaced the enemy text with
an AWT-free `EnemySheet` (header, health bar, rows, prose) that `EnemyStatText` builds from an
`EnemyInspection`. `EnemySheetDocument` lays it out in the Info `JTextPane`, with glyphs painted
by `Java2DFrameRenderer` (`paintRowGlyph`, `paintEnemyGlyph`, `paintRankBadgeGlyph`) as vector
`PaintedIcon`s. The pane opens every text from the top and lays a sheet out again when a
scrollbar changes its width. A trait and the stats it sets share one row, so nothing is said
twice.

**Tower text is four separate things, all plain text or buttons:**

| View | Built by | Shown when |
|---|---|---|
| Shop description | `Tower.getInfoString()` (every leaf overrides it: `"<Name> tower\n\n" + super + extra lines`) | Hovering or choosing a toolbar button (`PanelTowerSelector` → `TowerDefense.setInfoText`) |
| Selected-tower status | `Tower.getStatusString()` (same override pattern), plus `AbstractTower.ownedNodesBlock()` and `upgradeNodesBlock()` | A tower is selected; rebuilt every render pulse, because kills and damage dealt change without an event |
| Upgrade hover | `UpgradeNode.describe()`: `"<Name> (<gate>): <bonuses>"` | The pointer is over an upgrade button (`PanelTowerInfo.showUpgradeHover`) |
| Upgrades panel | `PanelUpgradeTree`: a `JLabel` header per slot, up to three `HudButton`s per slot | Replaces the wave preview while a tower is selected |

**What a live check showed** (Splash tower, freshly built, default window size):

- **Selected status** opens with a scrollbar. It has 13 lines, 3 of them blank, and the Sell
  button takes its own row. The tower-specific lines ("Splash radius 1.75", "Targets random")
  come *after* the "Upgrades:" block, so they sit below the fold at the very bottom. The leaf
  appends them after `super.getStatusString()`, which already ends with the upgrades.
- **The status repeats the Upgrades panel.** Its "Upgrades:" block lists every offered node
  with ✔/✘ and gate progress. The panel below shows the same nodes as buttons, and the
  owned-nodes block repeats the panel's slot headers ("BASE: Range").
- **Upgrade hover** is one line of mixed jargon: "Range (money only): +15% range". Its price is
  only on the button, and it doesn't say what the stat is now or would become.
- **Shop description** has 8 lines, with a blank line between the base stats and the behaviour
  lines.
- **Upgrades panel:** headers read "BASE —", "HEAD (locked)" and "SPECIAL (locked)". A button
  reads "1 Range ( $9 )". It is disabled when its gate is unmet or the player can't afford it,
  and nothing says which. Gate progress (e.g. "7/10 kills") appears only in the Info text. A
  button's number is the key that buys it.

**Units are mixed.** Range and splash radius are in cells ("3.2"), damage is in points, fire
rate is per second, and enemy speed is now in px/s.

**Board visuals that exist for towers and could be reused:**
- Tower body glyphs, already reused for the toolbar icons (`renderTowerIcon`).
- Upgrade-slot pips and "ready" chevrons in one colour per slot (`TOWER_UPGRADE_BASE` green,
  `TOWER_UPGRADE_HEAD` amber, `TOWER_UPGRADE_SPECIAL` purple).
- The pulsing enchant halo for an owned `SPECIAL` node.
- Aura link lines.
- The disruption marker on a tower a Jammer weakens.
- The effect colours for slow, burn and freeze, and the physical and magic colours (orange and
  blue) that the enemy rows use.

**Rules already in place:**
- `td/tower/CLAUDE.md`: never hand-write a node's bonus into a description, because
  `UpgradeNode.describe()` derives it, and the UI reads only `offeredUpgrades()` and
  `upgrades()`.
- Root `CLAUDE.md`: a UI-text test builds its input the way the panel does.

**Tests that assert today's strings:** `AbstractTowerTest` (9 references), `SonarTowerTest` (2),
`UpgradeNodeTest` and the `UpgradeCondition` tests (`describe()`/`progress()` wording).

## What this feature adds

Player-visible behaviour, view by view.

- **Shop description:**
  - Header: the tower's glyph, its name, and the price on the right.
  - One row per base stat: range, damage (in its damage type's colour), fire rate, and crit
    chance if the tower has any.
  - One row per behaviour: splash radius, what it targets, and any effect it applies (slows,
    burns, freezes) in that effect's colour.
  - Any remaining flavour sentence, dimmed, at the bottom.
- **Selected-tower status:**
  - The same header plus the sell value.
  - Current stats, one row each. A stat changed by upgrades, an aura or disruption is visibly
    marked as changed.
  - Kills and damage dealt as compact rows.
  - The owned upgrade in each slot as a row in that slot's colour.
  - Tower-specific rows, such as splash radius, sit with the other stats, above the upgrades.
  - It must fit the pane, Sell button included, at the default window size, without a
    scrollbar.
- **Upgrade hover:**
  - The node's name and price.
  - Its gate as a ✔/✘ row with progress.
  - One row per bonus or extra effect, in the same words `describe()` derives today.
- **Upgrades panel:**
  - Each slot's header carries its slot colour and shows the owned node.
  - An offered node's button says why it can't be bought yet: gate unmet (with progress) or not
    affordable. Controls stay `HudButton`s and keep their hotkey numbers.

**Out of scope:**
- New stats, balance changes, and anything that changes what an upgrade *does*.
- Tooltips.
- Tower sprites on the board.
- Any panel other than the four above.
- The `Tower` interface split tracked in `TODO.md` (see Interconnections).

## Interconnections

- **`FEATURE-enemy-info-rows.md` (the commits above).** Same pane, same row look, and ideally the same
  layout code and glyph painters. The enemy header, health bar and row kinds are the closest
  precedent.
- **`FEATURE-tower-upgrade-trees.md` and `FEATURE-tower-specialization-abilities.md`** define the
  slots, gates and node wording this feature displays.
- **`TODO.md`, "`Tower` is a twenty-three-method interface"**: `getInfoString`/`getStatusString`
  are two of those methods, and that entry names `PanelTowerInfo` as a consumer. This feature
  changes what the info panel needs from a tower, which is the kind of consumer pressure that
  entry waits for.

## Constraints and open risks

- **Space.** The Info pane is about 190 × 210 px (13 lines at 12 px) and loses a row to the Sell
  button while a tower is selected. The enemy sheet fits with 8–10 rows. The Upgrades panel below
  it is a fixed-height column of up to 9 buttons plus 3 headers.
- **Refresh rate.** The selected status is rebuilt every render pulse on the EDT, because damage
  dealt changes every tick. The pane already skips identical content. A styled document with
  icons is heavier to rebuild than a string.
- **Threading.** The status text is built on the EDT from tower state the game-loop thread
  mutates (kill count, damage dealt, `UpgradeState`), with `TowerStats` as the one published
  snapshot. The enemy side solved the same problem with an immutable inspection snapshot taken on
  the owning thread.
- **Text lives in `td.tower` today.** Enemy formatting moved to `td.ui` while the domain kept
  only short, structured descriptions (`TraitLine`). The tower's leaf overrides and
  `UpgradeNode.describe()` are the equivalent pieces here.
- **UI uniformity** (standing requirement): every control stays a self-painting `HudButton` or
  `HudToggleButton`, borders come from `Hud`, and glyphs come from the renderer rather than a font,
  so the look is the same on every OS. A disabled button's text must stay readable on the dark
  panel.
- **Colour legibility.** Some board colours are dim as text on black (the plating violet on the
  enemy side). Slot colours and damage-type colours will be read as text here.
- **Hotkeys.** Upgrade button numbers are the keys that buy them, and must stay visible and
  stable.

## Decisions made

- All four views are in scope together: shop description, selected-tower status, upgrade hover
  and the Upgrades panel.
- Same visual language as the enemy rows: board glyphs painted by the renderer, labels in the
  glyph's colour (coloured text), values right-aligned, one line per item, bars where a quantity
  has a maximum.
- Timed effects get no countdown bars, as on the enemy side.

## Open questions

1. Once the Upgrades panel shows gate state, should the selected-tower status stop listing
   offered upgrades entirely? The recommendation is yes, since that list is the main source of
   the overflow.
2. How should a changed stat look: base → current ("3.2 → 3.7"), current plus a signed bonus, or
   current tinted by whether it's buffed or debuffed?
3. Units: keep range and radius in cells, or match enemy speed's px/s? Should the two sides agree?
4. Is there a bar anywhere? Candidates are gate progress on a button (7/10 kills) and fire-rate
   cooldown. Neither has an obvious "health" equivalent.
5. Should a jammed tower show the disruption as its own row in the disruption colour, and an
   aura-buffed tower name its buff source?
6. Keep the leaves' flavour sentences ("Sweeps a beam around itself, hitting everything it passes
   over") as dimmed prose, shorten them, or drop them where the rows already say it?
7. On the Upgrades panel, should gate progress be on the button face itself, or only in the hover
   rows?
