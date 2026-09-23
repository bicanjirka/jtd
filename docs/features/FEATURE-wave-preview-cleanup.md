# Feature Request: Wave-Preview Panel Cleanup

**Status: not yet built.** This document describes a small UI-polish change to the existing
current/next-wave preview panel; nothing below has been implemented.

## Summary

Three small, related visual fixes to the current/next-wave preview panel (`PanelWaveInfo`), so
it reads as a bounded HUD panel like its siblings and drops a wasted, content-free row.

## Current state (what exists today)

- `PanelWaveInfo` (`src/main/java/td/ui/PanelWaveInfo.java:27`) is the "Current & Next Wave"
  sidebar panel: a round-number label, a `curSide` column of per-path rows, a second round-number
  label, and a `nextSide` column of per-path rows (`initComponents`, lines 115-154). It already
  carries one *outer* titled border for the whole panel —
  `setBorder(Hud.panelBorder("Current & Next Wave"))` (line 118) — but there is no border
  separating the "current" half from the "next" half; the two just sit in the same box six pixels
  apart (`Insets(6, 0, 2, 0)` before the next-round label, line 143).
- Each path within a side is one `PathWaveRow` (`src/main/java/td/ui/PathWaveRow.java:35`), one
  stacked per path under `curSide`/`nextSide` (`PanelWaveInfo.resize`, lines 94-113). Its
  `initComponents` (lines 75-96) puts a `Swatch` at `gridy = 0` and the embedded `PanelEnemy`
  composition strip at `gridy = 1` — **the swatch is confirmed to be alone on its own row**, with
  nothing else sharing that row, directly above the enemy strip.
- That "swatch" is not a plain color rectangle. `Swatch.paintComponent`
  (`PathWaveRow.java:117-129`) already draws a `PathMarkerShape.DOT` **and** a
  `PathMarkerShape.CHEVRON`, both `PathMarkerBrightness.MOVING`, in the path's own `PathColor`,
  through `Java2DFrameRenderer.paintPathMarkers` — the exact same AWT-free draw-command mechanism
  and the exact same chevron shape (`Java2DFrameRenderer.chevronShape`,
  `src/main/java/td/ui/Java2DFrameRenderer.java:131`) that the board's own moving path trail uses
  (`PathMarkerFrameBuilder.java:41`, `MarkerStyle.of(PathMarkerShape.CHEVRON,
  PathMarkerBrightness.MOVING, ...)`). **This row is where this project's precedent for a
  "moving path element" chevron already lives** — it is a vector `Shape` rendered through the
  frame pipeline, not a Swing text character. Nothing in `td.ui` today draws a `►` glyph as plain
  text (searched; no hit) — the `►`/`▮▮` mentioned in `td/ui/CLAUDE.md` as a UI-glyph precedent is
  for `HudButton`-style controls, a different mechanism from this panel's marker.
- `PanelEnemy` (`src/main/java/td/ui/PanelEnemy.java:42`), the embedded strip, has a fixed
  preferred size of 195×40 (`getPreferredSize`, line 84) and is laid out with
  `fill = HORIZONTAL, weightx = 0.01` inside `PathWaveRow` (lines 89-95), so it stretches to fill
  whatever row width it's given rather than demanding exactly 195px.
- `PanelWaveInfo` sits directly below `PanelTowerInfo` in `PanelGameConsole`'s fixed-width
  sidebar column (`PanelGameConsole.java:326-346`): `panelTowerInfo` is added with
  `weighty = 0.1` (grows to fill available space), `panelWaveInfo` is added right after with
  `anchor = GridBagConstraints.PAGE_END` and no `weighty` set (defaults to `0`) — confirmed
  bottom-docked, sized to its own content. `PanelTowerInfo`'s own `setMaximumSize`/
  `setMinimumSize`/`setPreferredSize` (`PanelTowerInfo.java:142-144`) fix the column at 200px wide.
- `Hud.outlineBorder()` (`src/main/java/td/ui/Hud.java:58-60`) is the plain, untitled one-pixel
  line every control and panel border is built from; `Hud.panelBorder(String title)`
  (lines 65-68) wraps it in a `TitledBorder`. `PanelGameConsole` already nests bordered
  sub-panels inside a larger container this same way — `jPanel_gameInfo`/`jPanel_gameButtons`
  each carry their own `Hud.panelBorder` ("Status"/"Speed") nested inside the outer console
  panel (`PanelGameConsole.java:160`, `267`) — the direct precedent for giving `PanelWaveInfo`'s
  current/next halves their own borders rather than relying on the outer one alone.

## What this feature adds

1. **A border around the current-wave section and a border around the next-wave section**,
   each reading as its own bounded panel the way `jPanel_gameInfo`/`jPanel_gameButtons` already
   do inside `PanelGameConsole` — nested inside `PanelWaveInfo`'s existing outer
   "Current & Next Wave" border, not replacing it. Since each section already carries its own
   round-number label as a heading, the added border should be the plain, untitled
   `Hud.outlineBorder()`, not a second `Hud.panelBorder(title)` that would duplicate that label.
2. **The path-color-swatch-only row is removed.** A `PathWaveRow` no longer has a row containing
   nothing but the marker.
3. **The path's color moves onto the same row as the enemy-composition strip**, as a small
   chevron to its left, reusing the *existing* moving-chevron mechanism identified above
   (`PathMarkerShape.CHEVRON` / `PathMarkerBrightness.MOVING` via
   `Java2DFrameRenderer.paintPathMarkers`) rather than introducing a new, second way of drawing a
   colored glyph. The row's static dot is dropped — the ask calls for one small chevron, not the
   dot-and-chevron pair the swatch draws today — so `PathWaveRow` ends up one row shorter and one
   marker simpler than it is now.

Net effect on `PathWaveRow`'s size: one fewer row (the swatch's own line), a few pixels less
width available to `PanelEnemy` on the remaining row (the chevron plus its spacing now shares
that row instead of getting a full line to itself), and the same total information content.

## Constraints and open risks

- **UI uniformity (root `CLAUDE.md` §8, `no-etched-border`).** Any border added here must come
  from `Hud` (`outlineBorder()`/`panelBorder()`), never a bespoke or platform-default one — this
  is already established by every other panel in `td.ui`.
- **`td.ui.render` stays AWT-free (root `CLAUDE.md` §2.4, `render-has-no-awt`); board content
  goes through the frame pipeline.** The chevron already satisfies this by construction, since it
  reuses `PathMarkerDraw`/`Java2DFrameRenderer.paintPathMarkers` rather than a Swing-drawn
  character — this request should not regress that by drawing the glyph as plain text instead.
- **Fixed-width sidebar.** The column is 200px wide end-to-end; moving the chevron onto the
  enemy strip's row narrows the room available to `PanelEnemy` slightly. `PanelEnemy` already
  scales its preview mobs to whatever width it's given (`recalculateSize`,
  `PanelEnemy.java:87-98`), so this is a minor visual tightening, not a layout break — but it's
  worth a look in the `run-jtd` screenshot pass.
- **Visual verification is required, not optional** (root `CLAUDE.md` §8) — this is a pure
  look-and-feel change; it must be checked from an actual screenshot of the running game via the
  `run-jtd` skill, not just from reading the code.

## Decisions made

- Both the current-wave and next-wave sections get their own border (not just the panel-wide
  one that already exists), since the ask is for each to "read as a bounded panel."
- The border is untitled (`Hud.outlineBorder()`), since the existing round-number label already
  labels each section and a second title would be redundant.
- The swatch's dot is dropped along with its row; only the chevron carries the path color
  forward, placed to the left of the enemy strip on its row.

## Open questions

- Exact chevron size/spacing against `PanelEnemy`'s strip, and whether the chevron sits inside
  `PathWaveRow`'s own layout or inside a small wrapper shared with the strip — left to
  implementation, verified visually.
- Whether the per-section border should hug just `curSide`/`nextSide`, or wrap the round label
  together with its side as one bordered group — either reads as "a bounded panel"; pick
  whichever looks right in a screenshot.

---

*After planning and implementation, update this document rather than deleting it (see the root
`CLAUDE.md`'s documentation map): mark it implemented, prune resolved open questions, and either
promote deferred scope to a new request or note it's still wanted for a later version.*
