# Feature Request: One look across the game

## Summary

The game's parts were tuned one at a time, and it shows. The Info panel and the Upgrades panel next to it
use different fonts and text sizes. The borders on buttons, tower tiles, upgrade nodes and Info icons are four
different weights, and controls show "selected" in three different ways. Make the whole UI read as one surface:
one font, a small set of text sizes, one rule for border weight, corners and selection, and the Upgrades panel drawn
at the game's own size instead of being zoomed.

## Current state (what exists today)

Measured on the 1262 × 740 window, where the Upgrades panel draws at its full 1.5×.

**Fonts and text sizes**

- The Info pane (`SheetPane`, also used by the tooltips) never sets a font. It takes the platform look-and-feel's
  text font, which is Tahoma 11 on Windows and something else on other systems.
- Everything painted by hand uses `Dialog`. The level select screen (10–30 px) and the board overlays use
  `SansSerif`.
- The confirmation popup (`ConfirmPopup`, such as a downgrade's) holds an Info sheet and two buttons. The term
  popups opened from a mention are tooltip sheets.
- The Upgrades panel's fonts are in design units and scale with the panel. At 1.5×:
  - the gate label "XP 0 / 60" is 13.5 px;
  - cluster names and price tags are 12 px (the tags bold);
  - the key chip is 10.5 px;
  - tier numerals are 16.5 px.
  
  Before the full-size feature raised them, these fonts were 6 to 8 design units.
- Panel titles and the title line's XP are Dialog 11 (`Hud.LABEL_FONT`).
- Other sizes on the game screen:
  - the Info icons' row labels ("Buffs", "Applies") are 10;
  - the Info pane's bars are 10 and 9;
  - the HUD's key chips are 8 bold, the tower price font 9 bold;
  - the status icons' values are 13 bold;
  - the Waves panel's captions are 8 bold, tracked.
- So the Info rows (11 px) sit right above Upgrades text of 12–13.5 px.

**Border weights, on screen**

| Element | Size | Idle border | Selected or owned |
|---|---|---|---|
| Header buttons, speed buttons, Sell | about 24 px tall | 1 px | selected fills with its colour |
| Info icons (Buffs, Applies), `TermIconPainter` | 20 px | 1.25 px | — |
| Tower build tiles (`BuyTileLook` at 1.5×) | 45 px | 2.4 px | 3 px, plus a fill |
| Upgrade nodes (`BuyTileLook` at the tree's scale) | 45 px at 1.5× | 2.4 px | 3 px, plus a tinted fill |
| Node rings, links | — | 1.4–1.8 units (2.1–2.7 px) | — |
| Price tags in the tree, the tree's key chip | — | 0.8 units (1.2 px) | — |
| HUD key chips | 12 px | 1 px | — |

Borders of 2.4 px, 2.1 px or 1.2 px fall between pixels, so antialiasing smears them and the edge reads heavier and
softer than a whole-pixel line. The Info icons' frames look crisp by comparison.

**Corners**

- Buttons are square-cornered rectangles.
- Info icons have an arc of 4 px.
- Tower tiles and upgrade nodes have an arc of 9 px (6 units at 1.5×).
- The HUD's key chips have an arc of 6 px, the tree's price tags 4 units (6 px).

**Selection and key chips**

- "Selected" looks three different ways:
  - a button fills solid with its colour (the speed buttons turn yellow, the only filled control on the screen);
  - a tower tile thickens its border and fills;
  - an upgrade node gets a ring and a tinted fill.
- Key chips come in three styles:
  - the HUD's ("M", "Space") has a grey border and white ink;
  - a tower tile's ("Q") is in the tower's colour;
  - the numeral strip's in the Upgrades panel is dark with its own states.

**Sizes**

- Info icons are 20 px (`TermIconPainter.ROW`), header status icons 16 px, mentions in text 13 px.
- A tower tile and an upgrade node are 45 px at 1.5×. A tower tile is always 45 px; a node shrinks with the tree.

**The Upgrades panel's scale**

- The tree is laid out in design units, 300 wide. It draws at up to 1.5× (`BuyTileLook.SCALE`) and scales down when
  the column is short, so its text, borders and nodes change size with the window.
- A four-tier tree is about 231 units tall, about 350 px at 1.5×.
- The right-hand column is 462 px wide (`PanelGameConsole.WIDTH`) to hold 300 units at 1.5×. The window opens at
  1262 × 740, with an 800 px board.
- At 740 px the column has about 655 px for the Info panel and the Upgrades panel together.
- Nodes are placed in slot units, so the tree's spacing follows its node size and pitch.

## What this feature adds

- **One font family** everywhere: the game screen, the level select screen, the dev panel, the confirmation popups
  and the tooltips and term popups. It is set by the game rather than the platform, so nothing follows the
  operating system.
- **A short type scale** that every panel uses:
  - a caption size for panel titles and chips;
  - one body size for Info rows, tooltip text, price tags, cluster names and gate labels;
  - its bold for prices and headings;
  - a heading size for the tier numerals and the like.
  
  Text next to text of the same role is the same size.
- **One border rule**:
  - every idle border is a thin whole-pixel line: buttons, tower tiles, upgrade nodes, Info icons, chips, tags;
  - selected, owned or active is one step heavier, also whole-pixel;
  - tower tiles and nodes get noticeably thinner borders than today, about as light as the Info icons'.
- **One corner family**: a small radius for chips and tags, one radius for icons, tiles, nodes and buttons.
- **One way to show "selected"**: the accent border plus a tinted fill, on buttons and tiles alike. The speed
  buttons stop being the one solid fill. A control's accent keeps coming from where it does today: a tower's
  colour on its tile and nodes, the level's theme colour on the speed buttons and the other themed parts.
- **One key-chip style** everywhere a key is shown, with the states the numeral strip's chip already has:
  short of cash, done, locked.
- **Bigger Info icons**: the Buffs and Applies icons, and anything else drawn at the Info row size, grow (to about
  24 px) and keep the frame they have now.
- **Body text at 12 px**: the Info rows and every sheet grow from 11 to 12; the Upgrades panel's text comes down
  to it.
- **The Upgrades panel at the game's own size.** Its text, borders and chips match the rest of the screen exactly,
  not through a zoom factor. It has one fixed size: it never shrinks with the window, and it doesn't grow either.
- **Tower tiles and upgrade nodes stay one look and one size, 40 px**, now that the tree no longer scales. The
  Towers bar and the Upgrades panel get shorter.
- **The board gets the room the panel frees.** If the tree comes out narrower than today's column, the column
  narrows to fit it and the board grows.
- **A minimum window height** at which the Upgrades panel and the Info panel both fit. The window can't be made
  shorter than that.

### Out of scope

- The board: tower sprites, enemies, projectiles, effects, overlays drawn on the board.
- Colours and the palette.
- The tower and term pictograms themselves (only the size they are shown at).
- The layout of panels, except what fixing the Upgrades panel's size forces on the column and the window.

## Interconnections

- **Reverses parts of `shipped/FEATURE-upgrade-panel-full-size.md`**:
  - "1.5× is the target and roughly the cap" and "on a shorter window it scales down as today" give way to one
    fixed size;
  - "larger small fonts … to about 9 design units" gives way to the shared type scale;
  - the 462 px column and the 1262 × 740 window were sized for 1.5× and may change.
- **Revises `shipped/FEATURE-unified-buy-tiles.md`**: "a tile is drawn at the tree's full scale (1.5×)" no longer
  applies. The one look stays.
- `shipped/FEATURE-key-hints-and-hud-unification.md` and `shipped/FEATURE-hud-identity.md` set today's controls,
  chips and tiles. This feature continues them.
- The gate line and numeral-strip button (`e9ea8355`) brought in the strip's key chip states, which become the one
  chip style.

## Constraints and open risks

- **The column's height.** The Upgrades panel never shrinks, so the Info panel gets what's left of the column. At
  740 px that's about 655 px for both. The minimum window height has to hold the tallest tree and the Info panel's
  tallest sheet with Sell, or say what the Info panel does when its sheet is longer.
- **The minimum window and the screen.** A 1600 × 1000 window was clipped at about 1536 × 864 on the monitor this
  is played on. The minimum has to stay well inside that.
- **The level select screen's big type.** Its title and level names (19–30 px) are display sizes. The type scale
  needs a display step for them, or they drop to the heading size.
- **The Info pane's text size drives its two-column fit.** A larger body size widens and lengthens every stat row.
  The two columns and the Sell button have to keep fitting the Info panel's share.
- **HiDPI.** On a Windows display scaled to 125% or 150%, a 1 px logical line is not one device pixel. "Crisp" has
  to hold at the scales the game is played at, not only at 100%.
- **Thinner borders carry less colour.** A tower tile's border is its tower's colour, and a thinner line shows less
  of it. The tiles must still be told apart at a glance.
- **Hit areas.** Hovering and clicking the tree are mapped through its scale today. Removing the scale must not
  move what a pointer hits.
- **Tests that lay out by size.** Icon rows (`IconRowLayout`), the tree's layout rules and the Info pane's columns
  depend on sizes this feature changes.
- **Standing requirements:**
  - one look on every OS, painted rather than taken from the look-and-feel;
  - one component, no per-owner modes;
  - a key chip shows the binding its key runs;
  - verify from screenshots of the running game.

## Decisions made

- The Upgrades panel is no longer zoomed. It is drawn at the game's own size and never smaller.
- Tower tiles and upgrade nodes get thinner borders. The Info icons' frame is the reference for the weight.
- The Info icons get bigger.
- The scope is the whole UI: the game screen (header, Info, Upgrades, Waves, Towers bar), the level select screen,
  the dev panel, the confirmation popups, the tooltips and the term popups. The board is not in scope.
- Tower tiles and upgrade nodes are 40 px.
- The body text is 12 px.
- Width the Upgrades panel frees goes to the board: the column narrows.
- A short window is prevented by a minimum window height, not handled by shrinking anything.
- The speed buttons take the shared selected look. They, and the other themed parts, keep the level's theme colour
  as their accent.

## Open questions

None blocking.
