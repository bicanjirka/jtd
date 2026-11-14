# 0003. Frames are described as AWT-free records and painted by one backend; all art is vector

- Status: Accepted
- Date: 2026-09-17 (recorded)

## Context
Painting had read live domain objects, which made a frame both thread-unsafe and impossible to
build or check without a display.

## Decision
- `BoardRenderer.buildFrame` describes a frame as an immutable `RenderFrame` of draw records in
  `td.ui.render`, which is AWT-free. One frame builder per domain type does the describing,
  dispatching through that type's visitor. `Java2DFrameRenderer` alone turns records into pixels.
- `AsciiBoardRenderer` is a second, real backend (it logs board state at `DEBUG`). It keeps the
  seam honest: if a frame can't be described without AWT, the change is in the wrong place.
- There are no image assets. Every enemy, tower, effect and path marker is built in code from
  vector shapes. A tower is a foundation that never moves plus, for a look that aims, a head that
  reads its `TurretAim`, interpolated like enemy positions. Amended 2026-11-14 for the next-gen
  looks: a foundation is a stack of lit parts per tier (`TowerFoundations`), drawn once from those
  vectors into a picture the painter keeps (`TowerSprites`), as an enemy body is; the frame names
  only the look and the tier. Before, each base was one closed `Shape` with one shared paint.
- Status-effect markers are their own `RenderFrame` list, not another kind of `EnemyDraw`.
  `EnemyDraw`'s contract is that an enemy is either a live body or a fading corpse, never both and
  never neither, and a marker is neither.
- Toolbar icons reuse the tower body shape, so board and icon can't drift apart in shape. They are
  painted differently on purpose: one flat colour (a translucent fill smudges at icon size), the
  body only (a head has no target in an icon), on a dark tile (some board colours vanish on light
  faces).

## Consequences
- A frame can be built and checked headless (`PreviewBoard`, `td.PerformanceHarness`).
- Art changes are code changes, and they are reviewed and diffed like code.

## Rejected
- *Sprites or image files.* They need an asset pipeline, and they would mean a second look per tower
  in the toolbar.
