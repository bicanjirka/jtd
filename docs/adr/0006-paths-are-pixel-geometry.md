# 0006. Paths are pixel geometry with one arc-length answer; buildability follows the final shape

- Status: Accepted
- Date: 2026-09-17 (recorded)

## Context
Levels are authored in cells but enemies walk curves. Movement and the path markers both need
"where is distance *d* along the path", and buildable cells must match what the player sees.

## Decision
- `Point` (integer cells) is used only for authoring. `Vec2` (pixels) is the only thing a `Path`
  stores. `PathBuilder` is the one conversion: cell centres, then the level's `PathSmoothing`, then
  a `PathNormal`. Consecutive corners may be any distance and angle apart.
- `PathSmoothing` is a per-level strategy. `none()` is the identity. `AbstractCornerSmoothing`
  walks the interior corners and pulls back a fraction of the shorter leg, capped so that two
  pullbacks never cross; a subclass supplies only one corner's curve (an exact arc, or a cheaper
  quadratic Bézier).
- `ArcLengthPath` is the single answer to "where is distance *d*": enemy movement (`PathMotion`)
  and the path markers both use it, so both move at real arc-length pace.
- Buildability comes from the final geometry. `PathCoverage.unbuildableCells` supersamples each
  cell against the path's corridor, and multi-path levels use the union.

## Consequences
- A curved level needs no special case anywhere. For axis-aligned paths, coverage reproduces the
  authored cells (`PathCoverageTest`).

## Rejected
- *Expanding a path into per-cell steps*, or a fixed tick count per segment: pace would depend on
  segment length and on corners.
