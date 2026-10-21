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
- Ground zones are the one board list drawn under the enemies (`ZoneFrameBuilder`, `RenderFrame.zones`):
  a zone is a patch, not an enemy overlay or a tower effect.
- Every colour, shape and stroke for board content lives in `Java2DFrameRenderer`, keyed by a
  `Palette` role, never by domain type. There are no image assets; all art is vector.
- A new enemy-centred visual goes into the existing sealed `EnemyOverlayDraw` hierarchy (rings,
  pulses, trait markers, ice crystal, hex runes), not a new `RenderFrame` list. Tower transients likewise go
  into `TowerEffectDraw`. Invisibility fades the body itself (`EnemyBodyDraw.cloakProgress`), and a
  field effect on the enemy flickers it (`EnemyBodyDraw.flicker`).
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
- A tower's body never turns: only its turret head aims.

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
- Tooltips are `TooltipPanel`s on the frame's popup layer, owned by one `TooltipLayer`; what is hovered, where a
  tooltip goes and which stay open are answered by `SheetPane.hoverAt`, `TooltipPlacement` and `TooltipChain`.
  Never use `setToolTipText` for game content.
- The header strip (`PanelHeader`) holds the title, the four `StatusIcon`s (wave, lives, score, cash) centred between
  the title and the speed buttons, which stand above the console. A status icon is a tooltip source that opens its tooltip under
  itself (`TooltipLayer.watchBelow`); only lives (on a drop) and wave (on an increase) flash, and a level's first
  numbers are a baseline, never a change.
- An upgrade button paints its perk icon beside its row text; its hover is a tooltip, never the info pane.
- Info-pane content is an AWT-free `InfoSheet` that a `*SheetText` builds from a domain
  inspection (`EnemyInspection`, `TowerInspection`): stat rows, then icon rows (Traits, Buffs,
  Debuffs; on a tower Perks, Buffs, Debuffs, Applies), each icon a `SheetIcon` carrying its tooltip sheet. `InfoSheetDocument` alone lays
  a sheet out. The live enemy sheet is built with the frame and travels in
  `RenderFrame.enemyInspection`; the EDT only shows it, and only after a board click asked for a
  selection.
- A term's icon is an `IconFace` (AWT-free) that `TermIconPainter` alone paints; its art is a `Pictogram` keyed by
  the term's key in `PictogramCatalogue`, its colours come from `TermLooks`. A new term needs a row in a
  `*Pictograms` table (or it shows its monogram) and a case in `TermLooks`.
- Verify a board-look change from a `PreviewBoard` render, a panel or control change from a
  `run-jtd` screenshot.
