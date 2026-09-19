# Feature Request: Multiple Enemy Paths Per Level

**Status: implemented**, as two commits following this doc's plan: data model/runtime/rendering
first, then Wild Bezier Sweep's second path and the round-sync integration tests. This document
is kept as the record of the request and the decisions behind it; see `td/wave/CLAUDE.md`'s
"Multiple paths and rounds" section and the new `td/level/CLAUDE.md` for the durable rules that
came out of it. Two things landed slightly differently than described below, both because
building the feature surfaced them, not because the request changed:

- **`EnemyMob.getProgression()` was rescaled to a fraction of a mob's own path length**, not
  raw pixels. Not called out in the original request: `FurthestAlongPathSelector` compares this
  value across candidates that can now sit on different paths of different total lengths, and
  raw pixel distance stopped being a fair comparison the moment that became possible.
- **The wave-info panel's per-path rows also carry their own health/reward/level labels**, not
  just a swatch and an enemy strip, since those numbers genuinely differ per path per round once
  a round is more than one wave.

## Summary

Today a `LevelDefinition` has exactly one path, one path-wide list of waves, and every enemy
walks it at whatever speed its own `EnemyDefinition` says. This feature lets a level define
**more than one path**, each with:

- its **own wave list** — its own enemies, counts, health, price and difficulty per round;
- its **own color**, so the static trail dots and the moving chevrons read as visually distinct
  lanes;
- its **own base speed multiplier**, so a whole path can be authored faster or slower than the
  default (`1×`) without touching any enemy definition.

A `WaveDefinition` also gains its **own, independently optional speed multiplier**, so a
specific round on a specific path can be called out as faster than the path's own default —
the two multipliers compose.

The path count is not capped anywhere in the design; no level ships with more than two today,
and none needs to. **Every path affects tower buildability identically** — no tower is buildable
on any path, exactly like today's single-path rule, just applied per path and unioned.

The wave-preview panel (`PanelWaveInfo`) currently shows one strip of enemy icons for the
current wave and one for the next. With multiple paths it shows **one strip per path, stacked
under each other**, each tagged with a small colored dot-and-chevron swatch matching that path's
actual on-board color — so a player can tell at a glance which enemies are about to come down
which lane, which is the whole point of the feature.

## Current state (what exists today)

- **One path per level, authored as a corner list.** `LevelDefinition` (`td.level.LevelDefinition`) holds
  `List<Point> path`, `List<WaveDefinition> waves`, and one `PathSmoothing smoothing` for the whole level.
  `PathBuilder.build(corners, smoothing, scale)` is the only place a corner list becomes a pixel-space
  `Path` (`td.wave.Path`), run through that one smoothing strategy.
- **One path, one wave list, correlated in one snapshot.** `LoadedLevel` (`td.util.LoadedLevel`) bundles
  `cells`, `board`, `path`, `catalog` and `waves` (`List<Wave>`) as the one thing `GameWorld.installLevel`
  publishes atomically. `GameWorld.getPath()`/`setPath()` expose the single path; the `setPath`/`setBoard`
  pair is explicitly documented as a single-threaded convenience for `PanelEnemy`'s preview world and for
  tests, not for the real simulation.
- **One wave counter for the whole level.** `GameEngine` holds one `volatile int wave` and one
  `volatile boolean waveReady`. `nextWave()` starts `installed.waveAt(this.wave)`, spawns it via
  `Wave.spawn()`, seeds `EnemyRoster`'s alive count from that one wave's `enemyCount()`, and increments
  `wave`. `WaveProgress` (`td.wave.WaveProgress`) reports one `current`/`next` `Optional<Wave>` pair, read
  from one `LoadedLevel` snapshot.
- **An enemy binds to "the" path unconditionally.** `AbstractEnemyMob`'s constructor calls
  `ArcLengthPath.of(gameWorld.getPath())` directly — there is no notion of "which path" anywhere in the
  enemy hierarchy.
- **Speed already has a working multiplier mechanism, just not one path/wave can reach yet.**
  `SpawnParameters.speedMultiplier` is folded once into `DefinedEnemyMob`'s stored `shapeSpeedMultiplier`
  field at construction, and `doDamage` recomputes `speed` from
  `definition.baseSpeed() * shapeSpeedMultiplier * traitFactors` on every hit — which is what stops a
  `SpawnShape.boss()`'s 50% slow from being wiped by the first hit it takes (`td/enemy/CLAUDE.md`). Today
  only a `SpawnShape` ever contributes to that multiplier.
- **Color is not data anywhere.** `PathMarkerDraw` (`td.ui.render.PathMarkerDraw`) carries a `Palette`
  value, and `Java2DFrameRenderer.colorFor` maps `Palette.PATH_MARKER_STATIC`/`PATH_MARKER_MOVING` to
  `withAlpha(Color.WHITE, 40)`/`withAlpha(Color.WHITE, 100)` — literally white at two fixed alphas, with no
  per-instance color anywhere in the model.
- **Buildability is computed once, from one path.** `GameEngine.markUnbuildableCells` calls
  `PathCoverage.unbuildableCells(path.points(), ...)` once per level load and disables every returned cell.
- **The wave-preview panel shows exactly one strip per side.** `PanelWaveInfo` has exactly two `PanelEnemy`
  fields (`panelEnemy_cur`, `panelEnemy_next`), each fed from exactly one `Wave` by `setWaveCur`/
  `setWaveNext`.
- **The level-select preview draws one path.** `PanelLevelSelect` calls
  `PathBuilder.build(this.level.path(), this.level.smoothing(), 1)` and maps its points into the thumbnail's
  bounds with `mapToBounds`, which computes its own min/max bounds from whatever point list it is handed.

## What this feature adds

### 1. A level authors a list of paths, not one path

`LevelDefinition` drops its `path`/`waves`/`smoothing` fields in favor of `List<PathDefinition> paths`
(minimum one). `PathDefinition` (new, in `td.wave`) is the per-lane value type:

```java
PathDefinition.of(corners, waves)                     // color = white, speed = 1x, no smoothing
PathDefinition.smoothed(corners, waves, smoothing)     // same defaults, an explicit PathSmoothing
    .withColor(PathColor.of(220, 70, 70))              // optional, defaults to white
    .withSpeed(1.3f)                                   // optional, defaults to 1x
```

Per the developer note driving this request, `color` and `speed` are **not** constructor
parameters — they are optional, fluent `withX` methods that default to today's behavior
(white, `1×`) when left unset, the same "with"-copy shape `LoadedLevel.withBoard`/`withPath`/
`withCatalog` already use in this codebase. `smoothing` stays a named static factory rather than
a third `withX`, matching `LevelDefinition.unsmoothed`'s existing precedent — it wasn't part of
the request, and every path already needs `corners`/`waves` up front to be constructible at all,
so there's no "optional to add later" story for it the way there is for color and speed.

`PathColor` (new, in `td.wave`) is a plain three-`int` (0–255) value type — `PathColor.of(r, g, b)`,
clamped in its compact constructor, plus `PathColor.DEFAULT` (255, 255, 255, matching today's
white). It carries no `java.awt` dependency, so it is exactly as safe to reference from
`td.ui.render` as `CellDraw` already references `td.cell.Cell` — domain types are fine there;
only `java.awt` itself is excluded (`render-has-no-awt`).

### 2. Wave progression stays one counter, synchronized across paths, per round

This is the load-bearing design decision, so it is stated plainly: **every path in a level must
define the same number of waves**, and a "wave" in the existing HUD/gating sense becomes a
**round** — starting round *N* spawns path 1's wave *N* and path 2's wave *N* (and so on)
together, and the round is cleared only once every enemy from every path in it is dead or has
leaked.

This is not a new mechanism bolted on top of the existing one — it falls out of reusing what is
already there unchanged:

- `GameEngine` keeps its single `volatile int wave` / `volatile boolean waveReady` exactly as
  today. `nextWave()` now spawns from `installed.pathAt(i).waveAt(this.wave)` for every path
  `i`, concatenates every path's `spawn()` result into one array, and hands that whole array to
  `EnemyRoster.setEnemies` in one call, the same as it hands one path's array today.
  `GameWorld.startWave` seeds the roster's alive count from the **sum** of every path's
  `enemyCount()` at that round.
- `EnemyRoster`'s alive count is already a single number the whole roster shares, regardless of
  which enemy decrements it. Mixing two paths' mobs into one roster call makes "the round is
  clear" fall out of code that has not changed at all.
- The space bar / "next wave" button, the pause-while-a-wave-runs check
  (`!engine.isWaveReady()`), and `debugSkipCurrentWave()` all key off the same one flag and are
  untouched.

The alternative — fully independent per-path progression, where path 1 might be on round 6
while path 2 is still clearing round 3 — was considered and rejected for this request. It would
need a second, per-path readiness gate, a redesign of the single "next wave" affordance into
one-per-path, and a wave-count invariant that no longer exists (paths could legitimately have
different lengths). The user's own description of the wanted UI — **stacked panels, one per
path, under the existing single current/next round display** — is itself the synchronized-round
model: one round, several simultaneous panels describing what that round spawns on each lane.

`LevelDefinition`'s compact constructor gains the validation this decision requires: every
`PathDefinition` in `paths` must report the same `waves().size()`, checked the same way the
existing `path.size() < 2` check already throws `IllegalArgumentException` — an authoring
mistake in hand-written `LevelDefinition` code, not loaded content, so it fits the precedent
already there rather than `GameStartupException`.

`WaveProgress` changes shape to match: `current`/`next` become `List<Wave>` (one entry per
path, in path order) instead of `Optional<Wave>`. `index`/`count`/`hasNextWave()`/
`currentNumber()`/`nextNumber()` are untouched — they describe the round, which is still one
number.

### 3. An enemy binds to its own path, not "the" path

`LoadedLevel.path`/`.waves` become one `List<PathRuntime> paths`, where `PathRuntime` (new, in
`td.util`, alongside `LoadedLevel`) bundles exactly the three things that travel together for
one lane: the built `Path`, its `List<Wave>`, and its `PathColor`. This is the same "correlated
fields cross as one snapshot" discipline `LoadedLevel` itself already follows — a lane's path
geometry, its waves, and its color are never independently swapped.

`Wave` gains two constructor fields: `pathIndex` (which lane it belongs to) and `speedMultiplier`
(the path's own `withSpeed` value composed with its `WaveDefinition`'s own, resolved once by
`GameEngine.loadLevel` — see below). `SpawnParameters` gains `pathIndex`, defaulting to `0` in
`SpawnParameters.atSlot` (today's only caller, and `PanelEnemy`'s single-path preview world,
both single-lane by construction). `AbstractEnemyMob`'s constructor resolves
`gameWorld.level().pathAt(spawnParameters.pathIndex()).path()` instead of
`gameWorld.getPath()` — one line changed, nothing else in the movement code cares which path it
resolved to.

**Speed composition needs no new mechanism at all.** `Wave.spawnShaped` already multiplies
`shape.speedMultiplier()` into the value it hands `SpawnParameters.of(...)`; it now multiplies in
`this.speedMultiplier` (the wave's own resolved path×wave product) at the same call site. That
product is what `DefinedEnemyMob` stores as `shapeSpeedMultiplier` and reuses on every
`doDamage` recompute — the exact mechanism that already keeps a `SpawnShape.boss()`'s slow from
being wiped by the first hit protects a fast path's speed the same way, for free.

### 4. Buildability unions across every path

`GameEngine.markUnbuildableCells` calls `PathCoverage.unbuildableCells` once per path (using
each path's own built `Path`) and disables the union of every returned cell set.
`PathCoverage` itself needs no change — it already takes a plain polyline and knows nothing
about how many paths exist.

### 5. Path color reaches the renderer

`PathMarkerFrameBuilder.build` takes a `PathColor` alongside the `Path` it already takes, and
`BoardRenderer.buildFrame` calls it once per `PathRuntime` in `level.paths()`, concatenating
every path's markers into the one `RenderFrame.pathMarkers` list — a `RenderFrame` describes the
whole board, and it already collects heterogeneous per-object draws that way.

`PathMarkerDraw` gains a `PathColor color` field. `Java2DFrameRenderer.colorFor` stops hard-
coding `Color.WHITE`: `Palette.PATH_MARKER_STATIC`/`PATH_MARKER_MOVING` keep meaning exactly what
they mean today — the **alpha** role (40 for the static trail, 100 for the moving chevron) — and
the paint call combines that alpha with the marker's own `color` instead of white. The dot/
chevron shapes, their spacing, and their animation are completely unchanged; only the hue
changes, and only because real data now flows into a place that used to hardcode one.

### 6. The wave-info panel shows one strip per path

`PanelWaveInfo` stops holding exactly two `PanelEnemy` fields. It holds two *lists* of
`PanelEnemy` — current and next — sized to the installed level's path count and rebuilt
(added/removed, per the package's "built once, refreshed in place" convention applying at the
level-load granularity, not the frame granularity) whenever a level with a different path count
loads. `setWaveCur`/`setWaveNext` take the `List<Wave>` `WaveProgress` now reports and feed one
`Wave` to each strip, in path order, stacked under each other exactly as the request describes.

Each strip gets a small swatch identifying its path: a static dot and a moving chevron, painted
in that path's actual `PathColor`, reusing `Java2DFrameRenderer`'s existing marker-shape drawing
rather than inventing a second visual language for "which lane is this" — the same dot/chevron
the player already reads on the board. This was the part flagged as "use a different approach if
dots/chevrons turn out to be unreasonably hard": they don't — `PanelEnemy` already owns a
`Java2DFrameRenderer` instance and already paints through it (`paintEnemies`), so painting one
more small shape through the same backend is additive, not a new pipeline.

For a single-path level (Classic Loop, Zigzag Gauntlet) this collapses to exactly one strip per
side, indistinguishable from today's behavior — no swatch is need to distinguish one lane from
itself, though painting it anyway (in the path's default white) costs nothing and keeps the
panel's shape uniform across every level rather than special-casing the one-path case.

### 7. The level-select preview draws every path

`PanelLevelSelect`'s thumbnail currently computes `mapToBounds` from one path's points. With
multiple paths it must compute one shared bounds from the **union** of every path's points first
(so two differently-routed paths share one coordinate frame and don't get independently
rescaled relative to each other), then map and stroke each path's own points through that shared
bounds, in that path's `PathColor` rather than the card's single `accent` color. This isn't
separately requested, but it falls directly out of `PanelLevelSelect` needing to compile against
`LevelDefinition.paths()` instead of `.path()` at all, and leaving it drawing only path index 0
would silently hide the feature everywhere a player first sees a level.

### 8. Wild Bezier Sweep gets a second path

The existing corner list and its ten `WaveDefinition`s become path 0, unchanged in content,
speed, and color (default white, `1×`) — visually identical to today. A new path 1 is added with
its own corner list (routed so its off-board entry/exit points sit on different board edges than
path 0's, so the two lanes don't visually converge at the screen boundary), its own ten
`WaveDefinition`s (same round count as path 0, per the synchronized-round invariant, but free to
differ in enemies/hp/price/level per round), a non-white `PathColor`, and a `withSpeed` distinct
from `1×` — concretely exercising every piece: color, speed, and the stacked wave panel.

## Architectural implications

| Area | Change |
|---|---|
| `td.wave` | New value types `PathDefinition`, `PathColor`. `LevelDefinition` (`td.level`) drops `path`/`waves`/`smoothing` for `List<PathDefinition> paths`, with a same-round-count invariant in its compact constructor. `Wave` gains `pathIndex` and `speedMultiplier` constructor fields. `WaveProgress.current`/`.next` become `List<Wave>`. |
| `td.util` | `LoadedLevel.path`/`.waves` become `List<PathRuntime> paths` (new record: `Path`, `List<Wave>`, `PathColor`); gains `pathAt(index)`/`pathCount()`/`waveCount()` (the shared round count) replacing `waveAt`/`waveCount`. `GameWorld.getPath()`/`setPath()` keep their single-lane convenience shape for `PanelEnemy`'s preview world and tests, now reading/writing path index `0`. |
| `td.enemy` | `SpawnParameters` gains `pathIndex` (default `0` in `atSlot`). `AbstractEnemyMob` resolves its path through `gameWorld.level().pathAt(...)` instead of `gameWorld.getPath()`. No change to the speed-multiplier recomputation mechanism itself — it already composes whatever `SpawnParameters.speedMultiplier` carries. |
| `GameEngine` | `loadLevel` builds one `Wave` per `(path, round)` pair instead of one per round, resolving each `Wave`'s `speedMultiplier` from `pathDef.speed() × waveDefinition.speedMultiplier()`; `markUnbuildableCells` unions `PathCoverage.unbuildableCells` across every path. `nextWave()`/`startWave` aggregate across paths; the single round counter and readiness gate are otherwise untouched. |
| `td.ui.render` | `PathMarkerDraw` gains a `PathColor` field. `Java2DFrameRenderer.colorFor`'s `PATH_MARKER_STATIC`/`PATH_MARKER_MOVING` cases stop hardcoding `Color.WHITE`, contributing only the alpha now. |
| `td.ui` | `PathMarkerFrameBuilder.build` takes a `PathColor`; `BoardRenderer.buildFrame` calls it once per path and concatenates. `PanelWaveInfo` holds a list of `PanelEnemy` per side, sized to the level's path count, each with a path-colored dot/chevron swatch. `PanelLevelSelect` maps every path through one shared bounds and strokes each in its own color. |
| `td.level` | `BuiltInLevelCatalog`'s `WILD_BEZIER_SWEEP` gets a second `PathDefinition`. Classic Loop and Zigzag Gauntlet each become a single-entry `paths` list with no other change. |

## Risks and traps found during research

**1. The same-round-count invariant is the thing most likely to be gotten wrong by hand-authored
content.** A `WILD_BEZIER_SWEEP` with nine waves on path 0 and ten on path 1 must fail loudly at
construction, not spawn `null`/nothing for the missing round on the shorter path — this is
exactly what `LevelDefinition`'s compact constructor must check before this feature has any
runtime behavior to test.

**2. A leaked or killed mob on path 2 must decrement the same alive count a path-1 mob does.**
This falls out of feeding one concatenated array to `EnemyRoster.setEnemies`/`setCount`, but it
is the single line that makes or breaks "the round clears only when both lanes are done" — get
the aggregation wrong (e.g., seed the roster from only one path's `enemyCount()`) and the round
either clears while the other lane's enemies are still walking, or never clears at all.

**3. `AbstractEnemyMob`'s off-board spawn/despawn buffer is per-path geometry, not per-level.**
Each path's own corners already control where it enters and exits off-board; the new Bezier
path must be authored so its own entry/exit points don't sit on top of path 0's, or the two
lanes will visually merge at the point enemies pop into view. This is an authoring concern for
the new corner list, not a code change.

**4. The level-select thumbnail's bounds must be computed once, over every path, not once per
path.** `mapToBounds` today receives one path's points and derives its own min/max from them.
Calling it once per path independently would let two differently-sized paths each fill the same
thumbnail box at different scales, which draws them in two different, incompatible coordinate
systems on top of each other — not simply which case-by-case.

**5. `PanelWaveInfo`'s strip list must resize on level load, not on every frame.** Per
`td/ui/CLAUDE.md`'s existing convention ("components are built once and shown/hidden or
refreshed, not rebuilt per level" — read here as "per frame"), the number of `PanelEnemy`
strips changes only when a level with a different path count loads, which already happens on
the EDT through the existing level-install path; nothing about this needs the `game-loop`
thread to know panel counts exist.

## Design decisions this request makes

**Synchronized rounds, not independent per-path progression.** Covered above under item 2 — the
deciding factor is that it reuses the existing single wave counter, readiness gate, and roster
alive-count entirely unchanged, and it is what the requested stacked-panel UI already implies.

**Color and speed are real per-path/per-wave data, not a small closed set of `Palette` roles.**
`Palette` is deliberately a fixed enum of *design-language* roles shared across the whole game
(`td/ui/CLAUDE.md`); a path's color is level-authored *content*, the same category of thing an
enemy's hp or a wave's price already is, so it belongs in the domain value types (`PathColor`,
carried on `PathDefinition`/`PathRuntime`/`PathMarkerDraw`) rather than in the enum that names
"a splash tower's beam" or "a shield status marker." This is also what keeps the path count
genuinely uncapped: two more `Palette` constants would still work for exactly two paths, but a
third path would need a third pair of constants added to an enum that otherwise never grows
per-level-authored content.

**Optional per-path/per-wave configuration is a fluent `withX` method, not a constructor
parameter.** Matches the explicit developer preference driving this request, and reuses the
`withBoard`/`withPath`/`withCatalog` shape `LoadedLevel` already established for "this value,
but with one field replaced."

**A mob carries a path *index*, not a `Path` reference.** `LoadedLevel`/`GameWorld.level()`
stays the single source of truth for what a path currently is; a mob asking for
`gameWorld.level().pathAt(index)` on each construction reads whatever is currently installed,
the same way it already reads `gameWorld.getPath()` today, rather than a mob and the world
disagreeing about a path if a level were ever reloaded mid-flight (not possible for a live
mob today, since a level reload clears the roster first, but the index form is one line
simpler than passing a `Path` and unglues a mob's identity from a specific pointer into a list).

## Phased implementation order

Following the standing rule (root `CLAUDE.md` §1): each phase ends green and is its own commit.

1. **Data model.** `PathColor`, `PathDefinition`, `PathRuntime`; `LevelDefinition.paths` and its
   round-count invariant; `Wave.pathIndex`/`speedMultiplier`; `SpawnParameters.pathIndex`.
   `BuiltInLevelCatalog`'s three levels updated to the new shape (Bezier still single-path at
   this point). No behavior change yet — every existing level still has exactly one path.
2. **Multi-path runtime.** `GameEngine.loadLevel`/`markUnbuildableCells`/`nextWave`/`startWave`
   aggregate across `LoadedLevel.paths()`; `AbstractEnemyMob` resolves its path by index;
   `WaveProgress` reports lists. Provable in `GameEngineTest` with a two-path test level before
   any level ships one.
3. **Rendering.** `PathColor` threaded through `PathMarkerDraw`/`PathMarkerFrameBuilder`/
   `BoardRenderer`; `Java2DFrameRenderer.colorFor` stops hardcoding white.
4. **Wave-info panel and level-select preview.** `PanelWaveInfo`'s per-path strip list and
   swatch; `PanelLevelSelect`'s shared-bounds, per-path-colored preview. Verified visually via
   the `run-jtd` skill.
5. **Content.** Wild Bezier Sweep's second path, authored with a real color and speed. Verified
   in-game, not just by the level loading without throwing.
6. **Docs.** `td/wave/CLAUDE.md`'s wave-composition section gets the round/path model;
   `README.md`'s level table notes path counts; this document's status line updated to
   "implemented."
