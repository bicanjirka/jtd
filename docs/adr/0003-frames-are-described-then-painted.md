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
  Amended again the same day: the frame also names a `TowerPose` (the cues a tower holds, its recoil
  and reload), and the look's builder turns it into parts. A head as one fixed part list had no
  place for a longer barrel, a loaded shell or a primed shot, and a tower that does not aim had no
  moving part at all.
- Status-effect markers are their own `RenderFrame` list, not another kind of `EnemyDraw`.
  `EnemyDraw`'s contract is that an enemy is either a live body or a fading corpse, never both and
  never neither, and a marker is neither.
- A toolbar icon is a flat mark of its own per look (`TowerIcons`): the tower's silhouette in its
  colour and its moving part in a lighter tone of it, on a dark tile (some board colours vanish on
  light faces). Amended 2026-11-14: icons reused the one closed body shape while a tower was one
  shape; a foundation of lit parts is unreadable at icon size, so the mark is drawn for that size.
  The Upgrades panel's tier thumbnails are the foundations themselves.

## Consequences
- A frame can be built and checked headless (`PreviewBoard`, `td.PerformanceHarness`).
- Art changes are code changes, and they are reviewed and diffed like code.

## Rejected
- *Sprites or image files.* They need an asset pipeline, and they would mean a second look per tower
  in the toolbar.
