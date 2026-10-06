# `td.ui` (+ `render`)

No gameplay logic here. The UI observes engine listeners; the engine reaches the UI only via
`GameHost`.

## Render pipeline

`BoardRenderer.buildFrame(gameTime, interpolationAlpha, animationSeconds)` runs on the
`game-loop` thread and returns an immutable, AWT-free `RenderFrame`. `TowerDefense` publishes it
through one `volatile`, and `TowerDefense.paintBoard` only paints what was published. Don't move
frame building onto the EDT. `AsciiBoardRenderer` is a real second backend (DEBUG logging), which
keeps the seam honest.

- One frame builder per domain type, dispatching through that type's visitor (`EnemyFrameBuilder`,
  `TowerSpriteFrameBuilder`, `TowerEffectFrameBuilder`, `ProjectileFrameBuilder`, ...). Builders
  return `null` for "nothing to draw".
- Every colour, shape and stroke for board content lives in `Java2DFrameRenderer`, keyed by a
  `Palette` role, never by domain type. There are no image assets; all art is vector.
- A new enemy-centred visual goes into the existing sealed `EnemyOverlayDraw` hierarchy (rings,
  pulses, trait markers, ice crystal), not a new `RenderFrame` list. Tower transients likewise go
  into `TowerEffectDraw`. Invisibility fades the body itself (`EnemyBodyDraw.cloakProgress`).
- Timed visuals follow one idiom: the entity records the tick something happened
  (`ticksSinceDeath`, `ticksSinceCriticalHit`, `ticksSinceEffectGained`/`Lost`), and the frame
  builder turns it into a 0..1 progress. Draw records carry progress, never stored animation
  state.
- `Panel*` previews reuse `Java2DFrameRenderer`'s paint methods (`PanelEnemy` →
  `paintEnemies`) rather than drawing their own copy.
- Exhaustive switches with no `default` (`bodyPaletteFor`, `colorFor`, the sealed draw
  switches): fix a compile error by adding the case, never a `default`. Switching on these
  sealed draw records is fine; switching on `EnemyMob`/`Tower`/`Cell` is not.
- Status and trait marker rows cap at `MAX_VISIBLE_MARKERS`, then show one overflow marker. Status
  markers go one per category once more effects are active than fit, each counting the other kinds
  of its category.
- Rank badges and the `SPECIAL` halo are painted upright (not rotated with facing).
- Tower bodies are one closed `Shape` each. The sonar head is the one exception, with its own
  `paintSonarSweep`. Toolbar icons reuse `towerBodyShape` via `renderTowerIcon`.

## Two clocks

- `interpolationAlpha` (between the last two ticks) for anything backed by per-tick domain state:
  enemy/projectile position, turret heading, a Cinder wave's travel. It respects pause and
  fast-forward.
- `animationSeconds` (wall clock) for pure cosmetics: spinning heads, the aura pulse, path
  chevrons. It keeps running while paused.
- Dead enemies are drawn without interpolation.

## Swing

- Every control is a `HudButton`/`HudToggleButton`, every text input a `HudTextField` (Swing's
  basic UI, never the platform's), and every border comes from `Hud`. Controls
  override `paintComponent` without calling `super`, so the look-and-feel paints nothing but its
  listeners still work. The one allowed tweak is a control's foreground (text) colour.
- Every component is `@ThreadConfined(EVENT_DISPATCH_THREAD)`. Nothing here is touched from tick
  code; per-frame cosmetics run on the EDT render pulse (`TowerDefense.repaintPublishedFrame`).
- Components are built once and shown/hidden or refreshed, never rebuilt per level (a
  `CardLayout` for menu vs board; overlays toggled by visibility; `PanelWaveInfo`/
  `PanelUpgradeTree` share one cell and are toggled).
- A panel talks upward through a `Runnable` setter with a no-op default
  (`BoardOverlays.onBackToMenu`), never by reaching for the frame or a sibling panel.
- `GameBoard.paint` skips `super.paint()`; the renderer's background fill is what clears the
  frame.
- `BoardOverlays` and `GameBoard` share one `GridBagLayout` cell with identical constraints.
  `TowerDefense.requestRender` repaints the *container* (repainting only the board erases the
  overlay). Overlay translucency is painted in `paintComponent`; an alpha background colour on an
  opaque component doesn't blend.
- Info-pane content is an AWT-free `InfoSheet` that a `*SheetText` builds from a domain
  inspection (`EnemyInspection`, `TowerInspection`): one row per thing on the board, with the
  glyph and colour the board draws for it, each fitting one line. `InfoSheetDocument` alone lays
  a sheet out. The live enemy sheet is built with the frame and travels in
  `RenderFrame.enemyInspection`; the EDT only shows it, and only after a board click asked for a
  selection.
- Verify a board-look change from a `PreviewBoard` render, a panel or control change from a
  `run-jtd` screenshot.
