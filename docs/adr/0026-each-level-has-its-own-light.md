# 0026. Each level has its own light

- Status: Accepted
- Date: 2026-11-25

## Context
Every board was a near-black floor with the same white light on everything that stood on it. Levels differed only by
a motif a few values above the floor, so a screenshot didn't say which level it was, and the darkest ground left no
room for value layers: towers, enemies and effects were all simply "bright on black". The board-look revamp
(`docs/features/shipped/FEATURE-board-look-revamp.md`) asks for a lit place with one palette per level.

## Decision
- A level's `BoardGround` carries a `BoardLight`: a key colour, a shadow colour and an air colour, with a haze
  strength. It lives in `td.board`, AWT-free, next to the floor it belongs to; a level that names none gets the house
  light (white key, black shadow, no haze), which is how every board looked before.
- `SceneLight` in `td.ui` applies it: a thing's top face keeps its exact identity colour, its shade leans toward the
  shadow colour and its rim toward the key. Cast shadows take the shadow colour. The light is part of every kept
  picture's key, so lighting costs nothing a frame.
- Panels, the toolbar and the art sheets paint unlit (`SceneLight.none()`): a tower's colour is the same on the board,
  in its tile and in the Info pane.
- Readability is measured, not tested (ADR 0025): `LookChecks` prints whether what stands clears 3:1 against each
  level's lightest floor under that level's light.

## Consequences
- A new level picks a floor, motif, light and haze together, and runs the look report.
- Kept pictures multiply by the levels played in one session, a handful at most.

## Rejected
- *Tinting whole sprites by the light*: identity colours would differ per level and from the panels.
- *A global light with per-level floors only*: a warm key on a cold level reads as wrong, and the rim light is where
  the level's hue reaches the things on it.
- *The light as a lighting pass over the painted frame*: a full-board composite per frame, against the paint budget.
