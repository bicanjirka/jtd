# `td.wave` — path geometry and wave composition

Read the root `CLAUDE.md` first; this file only covers what is specific to this package and
its `smoothing` subpackage.

The package holds two loosely related things that share a name only by history: the
**geometry** enemies walk, and the **composition** of a wave.

## Geometry

**`Point` and `Vec2` are not interchangeable, and the split is load-bearing.** `Point` is
integer *cell* coordinates and appears only in level authoring (`LevelDefinition.path`) and
in `PathCoverage`'s result. `Vec2` is continuous *pixel* coordinates and is the only thing a
`Path` ever stores. If you find yourself converting between them outside `PathBuilder` or
`PathCoverage`, something is in the wrong layer.

The pipeline, in order:

```
List<Point>  --PathBuilder-->  pixel centers  --PathSmoothing-->  Path (List<Vec2>)
                                                                    |
                        ArcLengthPath.of(path) <---------------------+---> PathCoverage
                        (movement + marker overlay)                        (buildability)
```

`PathBuilder.build` is the *only* place a level's corner list becomes a `Path`. There is no
per-cell expansion step: consecutive corners may sit at any distance and any angle, and are
joined by one straight leg.

**`ArcLengthPath` is the single shared answer to "where is distance `d` along this path".**
Both `AbstractEnemyMob`'s movement and `td.ui.PathMarkerFrameBuilder`'s overlay go through
it, which is what keeps them at a consistent pace on curved and diagonal geometry. It is
`Optional`-returning — a path with fewer than two points or zero length has nothing to
measure, and callers handle that rather than being handed a broken instance.

`poseAt` clamps out-of-range distances to the nearest endpoint rather than extrapolating.

**Buildability is computed from the final geometry, not from the authored cell list.**
`PathCoverage.unbuildableCells` supersamples each cell in the path's bounding box and marks
it unbuildable when enough of its area falls inside a one-cell-wide corridor. For an
unsmoothed axis-aligned path this reproduces the naive "mark the listed cells" result
exactly (`PathCoverageTest` proves it for the built-in levels); for a smoothed path it
correctly follows the curve. It takes plain `scale`/`width`/`height` rather than a
`Cell[][]` on purpose, which is what keeps it and its tests independent of the cell machinery.

### Smoothing (`td.wave.smoothing`)

`PathSmoothing` is a per-level strategy. `none()` is the identity — a level that wants no
smoothing gets this, not a `null` field. `AbstractCornerSmoothing` is the shared template
both real strategies build on; a subclass supplies only the curve for one corner.

`cornerPull` is a *fraction* of the shorter adjacent leg, capped at 0.5, so two nearby
corners' pullbacks can never cross. Degenerate corners (already straight, or a near-total
reversal) are passed through unrounded rather than forced into a curve the construction
cannot handle.

`ArcCornerSmoothing` is an exact circular fillet, tangent to both legs.
`QuadraticBezierSmoothing` is cheaper and not exactly tangent, but visually similar — it is
what the two curved built-in levels use.

## Wave composition

**The token table.** These are the ids `EnemyCatalog.builtIn()` registers, and this is the
canonical list - the root `CLAUDE.md` documents the grammar but deliberately not the content,
so adding an enemy does not touch the always-loaded file. Add a row here and to `README.md`'s
enemy table when you register a new id.

| Token | Enemy |
|-------|-------|
| `c` | Circle |
| `s` | Square |
| `t` | Triangle |
| `g` | Ghost |
| `e` | Empty - the reserved spacer; counts toward spawn timing, not toward the enemy count |
| `warden1` | The Warden boss - the only id in its six-stage chain a wave spawns directly |

Parsing and instantiation are deliberately separate:

- `WaveScript.parse(tokens, catalog)` turns a token string into a `WaveContent` against a
  given `td.enemy.EnemyCatalog`. No `GameWorld` involved, so it is trivially testable. Every
  non-reserved token is looked up the same way regardless of whether it names a built-in or
  a per-level custom/cloned definition — there is no separate syntax for the two, only
  whether the id happens to be registered in the catalog passed in. `e` is the one reserved
  token (the spacer), recognized before any catalog lookup. A token this can't recognize as
  the spacer, a registered id, or an integer repeat count **fails the parse** with a
  `GameStartupException` — a wave the author did not write is content corruption, and
  recovering from it silently produced a level that was subtly not the authored one. Blank
  tokens are whitespace rather than content and are skipped, which is what lets `"".split(" ")`
  and any run of spaces parse cleanly.
- `WaveContent` is the parsed result: one `WaveSlot` per spawn slot, in order, repeat counts
  already flattened. `WaveSlot` is a closed pair - `EnemySlot(EnemyDefinition)` for a real
  enemy, `EmptySlot()` for the spacer, which keeps its slot (it counts toward spawn *timing*)
  but is excluded from `enemyCount()`/`enemySet()`.
- `Wave` takes that content and does only the world-bound instantiation, one `EnemyMob` per
  slot (a `DefinedEnemyMob` for an `EnemySlot`, an `EnemyMobEmpty` for an `EmptySlot`), with
  delay equal to the slot's index.

The token grammar itself is documented in the root `CLAUDE.md` §9. `GameEngine.loadLevel`
builds one `EnemyCatalog.builtIn()` per level load and passes it to every wave's `parse`
call - a level does not yet register its own custom/cloned definitions into it (that's a
later phase); today `builtIn()` is the only catalog any level actually gets.

## Wave start broadcast

`WaveAnnouncer`/`WaveStartListener` are just the "a wave started" hub, kept separate from
the economy and the rosters despite once living bundled with them in `GameWorld`.
`TowerThree` is the only subscriber, dropping the hit markers its scan left on the previous
wave's enemies. Its listener list is `CopyOnWriteArrayList` because it
is fired from the `game-loop` thread — see the root `CLAUDE.md` §3 (Threading).
