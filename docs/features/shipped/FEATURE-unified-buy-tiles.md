# Feature Request: One look for upgrade nodes and tower tiles

Status: **implemented** (2026-11-19: `07a4ecc0`). A tile and a node share `BuyTileLook`: a tile is drawn at the
tree's full scale (1.5×), so the two match while the tree has its room and a tile stays that size when a short
window shrinks the tree. The tree's short-of-cash red became the HUD's.

## Summary

An upgrade node and a tower in the Towers bar are the same thing to the player: something to buy, with a picture
and a price. Today they are drawn two different ways. Give them one look: one size, one border, one price, one set
of states.

## Current state (what exists today)

- **A tower tile** in the Towers bar (`PanelTowerSelector`) is a `HudButton` with a tile face
  (`ControlFace.tile`): a rounded square bordered in the tower's colour, with the tower's 32 px sprite and its
  placement key in a chip in the corner. Its price is a label under the tile, bold 9 pt (`Hud.PRICE_FONT`). When the
  player can't afford it, its border dims and its price turns red. Hover brightens the border and opens the tower's
  sheet. The tile is about 46 px.
- **An upgrade node** is painted by `UpgradeTreePainter`, not a Swing control. It is 30 design units: round for a stat,
  square for an ability. Its glyph is in the tower's colour. Its price is a small tag on its bottom edge, neutral
  white, red when it can't be afforded and grey when it isn't available. An owned node has a tick on that tag, a
  tinted fill and a border in the tower's colour. The other member of a cluster is hatched, a locked tier is greyed,
  and a node that sells back in full carries a cash mark. Nodes have no key chip; a tier's Buy singles button has
  one.
- At 1.5×, the size `FEATURE-upgrade-panel-full-size.md` asks for, a node is about 45 px, about the size of a tower
  tile.
- `src/main/java/td/ui/CLAUDE.md`: a control varies only by its foreground colour and its `ControlFace` (a button or
  a tile) and its key chip; the upgrade tree uses `Hud`'s greys like every other control. `docs/UI.md` holds the
  colour rules.

## What this feature adds

- **One look for anything bought by a click**: a tower tile and an upgrade node share their size on screen, border
  weight, corner, hover brightening, price placement, price font and the price's colours (neutral, red when it
  can't be afforded, grey when it isn't available).
- **The price sits on the tile's bottom edge as a tag**, as on the nodes today, in both places. The line under the
  tower tiles goes.
- **What only one of them has stays theirs**: a tower tile keeps its key chip and its sprite; a node keeps its
  shape (round stat, square ability), its owned tick and fill, the hatching, the locked grey and the cash mark.
- **The same states read the same way**: can't afford, not available and hovered look alike on both.

### Out of scope

- The Upgrades panel's size and place (`FEATURE-upgrade-panel-full-size.md`).
- The hover sheets.
- Board sprites and the tower icons themselves.
- The other controls (`HudButton` buttons, the speed buttons, the level cards).

## Interconnections

- `FEATURE-upgrade-panel-full-size.md` brings the nodes to about the tower tiles' size. Building it first gives this
  one equal sizes to start from.
- `FEATURE-upgrade-panel-retune.md` approved the node look (price tag on the edge, owned tick). That look is
  the base here.
- `FEATURE-key-hints-and-hud-unification.md` and `FEATURE-hud-identity.md` set today's controls and
  tiles.

## Constraints and open risks

- **Two painters.** A tile is a Swing control with the look-and-feel bypassed; a node is painted inside one
  component at the tree's scale. One look has to come out of both at every scale the tree draws at, and at every
  HiDPI scale.
- **Shapes mean something in the tree.** A tower tile is a rounded square, which in the tree means an ability. The
  Towers bar is far from the tree and holds only towers, but the overlap is a fact to weigh.
- **The Towers bar's height** today includes the price line. Moving the price onto the tile changes the bar's height
  and so the board's room.
- A key chip shows the binding its key runs, never a copy (`td.ui` rule); the tower tiles' chips stay bound to
  `placementKey`.
- Standing requirements: one look on every OS, painted from vectors; one component with no per-owner modes.

## Decisions made

- The node's approved look is the base. The tower tile takes the price tag on its edge.
- The tile keeps its key chip; nodes don't gain one.
- Each keeps the marks only it has (listed above).

## Open questions

None blocking.
