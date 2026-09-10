# `td.ui` — presentation

Read the root `CLAUDE.md` first; this file only covers what is specific to this package and
its `render` subpackage.

Two rules govern everything here, and both are checkable by grep:

1. **No gameplay logic in this package.** Rules go in `GameEngine`/`GameWorld`/the domain
   packages. If the UI needs something from the engine, it observes a listener; if the
   engine needs something from the UI, it goes through `GameHost`.
2. **`td.ui.render` imports no `java.awt` at all**, and `Java2DFrameRenderer` is the only
   class that turns a `RenderFrame` into pixels — every colour, shape and stroke choice for
   board content lives there. The `Panel*` Swing components do import `java.awt` for layout
   and colours, and two of them (`PanelEnemy`, `PanelLevelSelect`) paint small previews of
   their own; that is fine, as long as board content itself keeps going through the frame
   pipeline. `PanelEnemy` already does — it reuses `Java2DFrameRenderer.paintEnemies`.

## The render pipeline

```
BoardRenderer.buildFrame(gameTime, interpolationAlpha, animationSeconds)
    -> walks the engine, dispatching per object through visitors
    -> RenderFrame  (immutable, AWT-free draw-command records)
        -> Java2DFrameRenderer.paint(g2, frame)   pixels, the real backend
        -> AsciiBoardRenderer                     text, for headless DEBUG logging
```

`AsciiBoardRenderer` is not a toy: it exists so the split is a real seam rather than an
aspirational one. If a change makes it impossible to describe a frame without AWT, the
change is in the wrong place.

Each domain type has one *frame builder* that describes it and knows nothing about pixels:

| Builder | Describes | Dispatch |
|---|---|---|
| `CellFrameBuilder` | placement/selection highlights | plain getters — only one `Cell` impl exists |
| `EnemyFrameBuilder` | enemy bodies and death fades | `EnemyMobVisitor` |
| `TowerSpriteFrameBuilder` | tower base + animated turret head | `TowerVisitor` |
| `TowerEffectFrameBuilder` | beams, splash, pulse, aura | `TowerVisitor` |
| `PathMarkerFrameBuilder` | the path's static trail and moving chevrons | none — pure geometry |

## Two independent clocks, and which one to use

`buildFrame` receives both, and picking the wrong one is the most likely mistake in this
package:

- **`interpolationAlpha`** — where this frame lands between the last two *simulation* ticks
  (`[0, 1)`). Use it for anything reading domain state that advances per tick: an alive
  enemy's position, an aiming turret's heading. It respects pause and fast-forward.
- **`animationSeconds`** — monotonic wall-clock seconds. Use it for cosmetic animation with
  no domain state behind it: spinning turret heads, the upgrade tower's pulse and aura, the
  moving path markers. It deliberately keeps running while the game is paused, and does not
  speed up when the game does.

A dead enemy is drawn *without* interpolation. It has stopped moving, so interpolating it
would slide it back and forth every frame between two positions that never change again.

## All art is vector, and adding some is a fixed checklist

There are no image assets. Every shape lives in `Java2DFrameRenderer`, keyed off a
`Palette` role rather than off any domain type — that indirection is what keeps the frame
model AWT-free.

To add a new enemy's or tower's art: add the `Palette` constant, add its shape and colour
cases in `Java2DFrameRenderer`, and wire the domain class into the existing visitor. The
per-type checklists live in `td/enemy/CLAUDE.md` and `td/tower/CLAUDE.md`.

Several switches here are deliberately exhaustive with **no `default`** —
`TowerSpriteFrameBuilder.bodyPaletteFor`, `Java2DFrameRenderer.colorFor`, the sealed
`EnemyDraw`/`TowerEffectDraw` switches. Adding a type is then a compile error until its art
exists, instead of a silently missing sprite. Do not add a `default` branch to "fix" a
compile error; add the missing case.

`Java2DFrameRenderer` pattern-matches over the **sealed** `EnemyDraw`/`TowerEffectDraw`
hierarchies. That is a compiler-checked switch over a closed set of DTOs the renderer
itself defines, and is the one narrow exception to the project's no-`instanceof` rule. It
is not license to switch on `EnemyMob`, `Tower` or `Cell`.

Toolbar icons go through `renderTowerIcon`, which reuses the same `paintTowerBody` call the
board does — so a tower's board look and its icon can never diverge. It draws the body only
and puts it on a dark rounded tile; both choices are about a 32px icon rather than about the
board, and `renderTowerIcon`'s own doc comment explains why.

Every tower body is a single closed `Shape` (triangle, circle, spiral, star, pulsar). Keep it
that way: the moment one needs two colours it needs its own paint method, and the shared
fill-then-outline path stops being shared.

Turret heads mostly work the same way, through `turretHeadShape`. The sonar head is the one
exception — a radar sweep is read from its fading trail, which needs several wedges at
different alphas, so it has its own `paintSonarSweep` and `turretHeadShape` throws for it.
It is also sized to stay inside its own tile: a head reaching across neighbouring cells reads
as a weapon with reach rather than as an instrument.

## Swing panels

`GameBoard`, `PanelGameConsole`, `PanelTowerSelector`, `PanelTowerInfo`, `PanelWaveInfo`,
`PanelEnemy`, `PanelLevelSelect` and `BoardOverlays` are ordinary Swing components. Two
conventions run through them:

- Components are **built once and shown/hidden or refreshed**, not rebuilt per level. The
  level-select screen and the board are two cards of one `CardLayout`; the win/lose overlays
  are permanent children toggled by visibility.
- A panel that needs to tell `TowerDefense` something exposes a `Runnable` setter with a
  no-op default (see `BoardOverlays.onBackToMenu`) rather than reaching upward for the frame.

`GameBoard.paint` overrides Swing's painting wholesale with no `super.paint()` call, which
is why `Java2DFrameRenderer`'s background fill is also the only thing clearing the previous
frame.

`BoardOverlays`' win/lose layers and `GameBoard` are **stacked in the same `GridBagLayout`
cell**, and two things about that are easy to break:

- Both need the same cell and the same `fill`/`weight`. Give either one different
  constraints and they stop overlapping — they end up side by side, and whichever gets cell
  (0,0) pushes the other out of view.
- `TowerDefense.requestRender` repaints the *container*, not `GameBoard`. A `JPanel` reports
  that its children never overlap, so repainting the board alone paints over the overlay
  without painting it back and the message vanishes on the next frame.

The overlay layer is transparent and only its inner plate paints anything, so the final
board stays readable underneath. Translucency there has to be painted in `paintComponent` —
an alpha-channel *background colour* on an opaque component does not blend, which is what
made the original overlay solid black.

**Nothing here may be touched from tick code.** Rendering reaches the EDT via
`SwingUtilities.invokeLater` — see the root `CLAUDE.md`'s Threading model.
