# Feature Request: Key hints and HUD unification

## Summary

Every control in the game shows its keyboard shortcut as a small key chip, the way the upgrade nodes already show
their digit, and every control shares one look. Today the HUD is drawn by three systems that agree on nothing but the
background, and most shortcuts are invisible: listed in the welcome text or a tooltip, or not at all.

## Current state (what exists today)

**Three ways of drawing a control**

| | `Hud.paintControl` (tower buy, speed, dev, Sell, popups) | `UpgradeTreePainter` (upgrade nodes) | `PanelLevelSelect` (level cards) |
|---|---|---|---|
| What it is | `HudButton`/`HudToggleButton`, one generic face | Shapes painted on one canvas, no Swing controls | Clickable panels with their own painting |
| Neutrals | Green-tinted (`FOREGROUND 220,255,220`, `BORDER_IDLE 78,104,78`) | True greys (`LINK_IDLE 58,58,58`, sealed/locked greys) | Own set (`CARD_BACKGROUND 12,13,12`, `STAT_BORDER 42,46,42`) |
| Shape | Square, 1 px border | 7 px corners, 2 px border in the slot colour | Cards |
| States | enabled, rollover (fill tint), pressed, selected (fill `38,56,38`, barely visible) | owned, buyable (glow), short of cash (red price), waiting (ring), needs previous (dashed), locked out (hatched); hover is a white 1 px ring outside | hover fill |
| Marks | none | key chip overhanging the top-left corner in the slot colour, price, sealed badge, tier numeral | stats |

**Keys today** (`TowerDefense.keyTyped`, `TowerDefense.dispatchShortcut`, `PanelDev.bind`)

| Key | Does | Visible where |
|---|---|---|
| `q w e r t y u i` | pick a tower to place (`TowerFactory.Type.placementKey`) | welcome text, shop sheet's Shortcut row |
| `1`-`9` | buy the selected tower's upgrade by number (`UpgradeTree.keyOf`) | chip on the node |
| `p` | toggle pause | welcome text |
| `s` | send the next wave | welcome text |
| `f` | cycle normal → fast → super fast | welcome text |
| `m` | back to menu (asks mid-level) | welcome text |
| `Esc` | cancel placing; cancel a `ConfirmPopup` | welcome text; popup hint line |
| `Ctrl+Shift+D` | dev panel | welcome text, dev panel title |
| `n` `x` `c` (dev panel open) | skip wave (same as Alt+N), spawn next catalogue enemy, grant debug credits | nowhere |
| `Alt+…` / `Alt+Shift+…` | every dev panel control | Alt+H help |

- The speed buttons and their keys disagree: ▶ sets normal speed *and* sends a ready wave (`p` only unpauses); ▶▶
  steps one tick while paused (`f` doesn't). The strip shows no active speed, only ▶ swapping to ❚❚ while a wave runs.
- An unaffordable tower button is disabled and its icon greyed, so a click does nothing, but its key still starts
  placing. `TowerPlacement.mouseClicked` refuses an unaffordable placement silently (a log line) and ends placing.
- Greying hides the tower colours: Mortar, Scorcher, Hive and Obelisk look grey whenever cash is short.
- Enter in a dev panel field already runs its button (`PanelDev.submitOnEnter`): Credits/Lives → Set, Wave → Go,
  Health % → Set, Wave script → Spawn, XP → Grant, effect and buff values → Apply. Nothing shows it.
- The speed buttons (`PanelHeader.speedButtons`) and Pin line (`PanelTowerInfo`) use `setToolTipText`, which
  `td.ui/CLAUDE.md` forbids for game content.
- Sell acts at once, no confirmation. Pin line shows only for an Obelisk.
- The level-select screen has three levels and no keys.
- `README.md`'s controls table lists the keys above.

## What this feature adds

### One look: tiles and buttons

- A **tile** is something you buy or pick: a tower buy button or an upgrade node. A **button** is a command: speed,
  Sell, menu, dev panel, popups. Both share neutrals, the key chip, fonts and hover.
- Neutrals are the green-tinted Hud greys everywhere: the tree's grey links and sealed/locked nodes and the level
  cards move onto them.
- Buttons are square; tiles have 7 px corners.
- Hover tints the fill and brightens the border on every control, the level cards included. The tree's white hover
  ring goes; its path and drop highlights stay.
- Selected (the active speed, the tower being placed, a dev toggle that is on) is the selected fill plus a bright
  border.
- Font sizes stay as today: chip 8 bold, price 9 bold, label 11, glyph 14, title 16.

### Tower tiles

- Border in the tower's own colour, dimmed when short of cash; the price under the tile, red when short; its key chip.
- An unaffordable tower stays in colour and is pickable by click or key, so its range can be previewed.
- Placing it without the cash keeps placing mode on, flashes the cash status icon and tints the hover ghost as
  "can't afford"; the click after the cash arrives builds it.
- Any purchase refused for cash (a tower or an upgrade, by click or key) flashes the cash icon.

### Key chips

- An outlined keycap inside the top-left corner of the control, in the control's accent: the tower or slot colour on a
  tile, neutral on a button. A dimmed tile dims its chip. The upgrade nodes' chips move inside their corner too.
- Game controls show their chips always. The dev panel shows its chips only while Alt is held.

### Keys

| Control | Key (chip) |
|---|---|
| Tower tiles | `q w e r t y u i` (unchanged) |
| Upgrade nodes | `1`-`9` (unchanged) |
| ❚❚ | `Space`: when paused or a wave is ready, go at the last play speed (normal at level start) and send the wave; otherwise pause |
| ▶ ▶▶ ▶▶▶ | `D` `F` `G` |
| Sell | `Delete` (chip `Del`), sells at once like the click |
| Menu button (new, beside the title) | `M`: opens the existing abandon `ConfirmPopup` |
| Confirm popup | `Enter` confirms (chip ↵), Cancel shows `Esc` |
| Back to menu (won/lost overlay) | `M` |
| Level cards | `1`-`9` start that level at once, like a click; a card past the ninth has no key |
| Dev panel controls | their Alt keys, chips while Alt is held |
| Dev field's button | ↵ while its field has focus |

- `p`, `s` and the `f` cycle retire, and so do the bare dev keys `n`, `x` and `c`.
- Every player key has a control that shows its chip or a line where it applies: `Esc` (cancel placing) is said in
  the shop sheet while placing, `Ctrl+Shift+D` stays in the welcome text.

### Speed strip

- Four buttons always visible, ❚❚ ▶ ▶▶ ▶▶▶, as a radio group: the active speed is selected. Pressing any play speed
  also sends a ready wave; pressing the selected one again sends it.
- ▶▶ always means fast; single-stepping stays the dev panel's Step.
- Once a level is won or lost the speed buttons are disabled, their chips dimmed.
- Their tooltips come from `TooltipLayer`; so does Pin line's.

### Text

- The welcome text keeps only what no chip shows (`Esc`, `Ctrl+Shift+D`) and says keys are shown on the buttons.
- The shop sheet drops its Shortcut row.
- `README.md`'s controls table follows the new keys.

### Out of scope

- A setting to hide chips.
- Keys for Pin line or the dev panel toggle as a visible button.

## Interconnections

- `FEATURE-tower-upgrade-visual-revamp.md` set the tree's look; this request changes its neutrals, hover and chip
  position.
- `FEATURE-hover-popups.md` owns tooltips; the speed buttons and Pin line join it.

## Constraints and open risks

- Each chip must read the binding its key runs. Today the bindings live in four places (`TowerFactory.Type`,
  `UpgradeTree.keyOf`, `PanelDev`'s shortcut list, the switch in `TowerDefense.keyTyped`), so a chip and its key can
  drift apart.
- Swing clips a control to its bounds, so a chip cannot overhang a control's edge the way the tree's does.
- The app runs on the system look-and-feel. On Windows a lone Alt press can trigger Alt handling; holding Alt for dev
  chips must not take focus from a dev text field or the board.
- Space and Delete reach the game only while no text field holds focus.
- `td.ui/CLAUDE.md` states that only lives and wave flash, and describes the tree's hover and the controls' look; those
  lines change with this feature.
- The run-jtd Driver addresses components by index (`TowerDefense.initComponents` adds the dev panel last for that
  reason); a new header button and a fourth speed button shift what it sees.
- The bare dev keys' engine hooks (`debugSpawnNextCatalogEnemy`, `debugGrantCredits`) may lose their last caller.
- Standing requirements: one look on every OS, every control a `HudButton`/`HudToggleButton` painting itself, a glyph
  over artwork for a simple control, the engine headless.

## Decisions made

- One request; the look lands before the chips.
- Every row of *What this feature adds* above was chosen from options weighed in the grilling: tile vs
  button, green-tinted neutrals, square buttons with 7 px tiles, tower-colour tile borders with the price under,
  outlined keycap chips in the accent colour, fill-tint hover, the key table, Alt-reveal for the dev panel only,
  pickable unaffordable towers with the cash flash, the four-button radio speed strip.

## Open questions

None.
