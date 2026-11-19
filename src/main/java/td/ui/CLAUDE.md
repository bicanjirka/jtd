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
  a zone is a patch, not an enemy overlay or a tower effect. Zones of one kind never stack in play, so they draw as one
  `ZonePatchDraw` with one even fill and one outline; what stands on a disc (a mine's casing, flames) stays per zone.
  An Obelisk's field is a tower's range, not a zone, but lies on the ground the same way: one patch per stone.
- Every colour, shape and stroke for board content lives in `td.ui`, keyed by a role, never by domain
  type: an enemy body's in its `EnemyLooks` row (keyed by `EnemyLook`), a tower's colour and tier
  colours in its `TowerLooks` row, its icon in its `TowerIcons` row and its foundation and head in its
  `TowerFoundations` row (all keyed by `TowerLook`), the rest in `Java2DFrameRenderer` (keyed by `Palette`). A
  tower's colour in text is a `Palette` tone (`bodyPaletteFor`) whose `colorFor` case reads its row. There
  are no image assets; all art is vector.
- A new enemy-centred visual goes into the existing sealed `EnemyOverlayDraw` hierarchy (rings,
  pulses, hex runes, dots, badge, affix pips, bars, egg, bursts, tints, ice, emissions, the Toll ring, the Silence
  mark), not a new `RenderFrame` list. Tower transients likewise go
  into `TowerEffectDraw`. A bar under an enemy is a row in `EnemyFrameBuilder.bars` and a colour in
  `EnemyLooks.barColor`, with no drawing of its own. Invisibility fades the body itself (`EnemyBodyDraw.cloakProgress`). `EnemyEffects` alone paints
  what an effect draws on an enemy, one draw record per effect and every particle a pure function of the clock and the
  enemy's seed, so nothing is stored and frame cost doesn't grow with particles. Emissions run on the wall clock; bursts
  follow game time, since they report an event. A cloaked enemy's art fades with it, except a burn, which reveals it.
- Timed visuals follow one idiom: the entity records the tick something happened
  (`ticksSinceDeath`, `ticksSinceCriticalHit`, `effectLostWithin`), answered as an
  `OptionalInt` that is empty when it never happened, and the frame builder turns it into a 0..1
  progress. A query the frame build makes for every kind of every enemy takes its window, so it
  builds no answer outside it. Draw records carry progress, never stored animation
  state.
- `Panel*` previews reuse `Java2DFrameRenderer`'s paint methods (`PanelEnemy` →
  `paintEnemies`) rather than drawing their own copy.
- Exhaustive switches with no `default` (`lookFor`, `bodyPaletteFor`, `colorFor`, the sealed draw
  switches): fix a compile error by adding the case, never a `default`. Switching on these
  sealed draw records is fine; switching on `EnemyMob`/`Tower`/`Cell` is not.
- Rank badges are painted upright (not rotated with facing). A rank badge is one size on
  every enemy.
- An effect with no picture of its own earns at most one dot, by `EnemyFrameBuilder.dotFor`; a dot says only its group,
  and the Info pane says which effect.
- What stands on the board is lit from the top left and shows its side down the screen; what lies on the ground
  (zone patches, path marks, range circles, a tower's shadow) stays flat. A mine goes through `paintStanding` with the
  three `Tones` of its colour.
- An enemy body is painted by `EnemySprites` alone, from its `EnemyLooks.Form` (height, hover, cap, raised detail,
  glint, glow): the stack of parts is drawn once into a kept picture per look, size, heading step and zoom, so a new
  part costs nothing a frame. What changes with every step (the hop, the hover, a glow) is a function of the body's
  place, never of a clock, and is the only thing painted live. Whatever sits on a body's top follows
  `EnemySprites.lift`.
- A tower is painted by `TowerSprites` alone: its foundation for its look, tier and `TowerPose`, the head of a look
  that aims, and the parts that stand in front of that head, each drawn once into a kept picture (a head per heading
  step) and placed whole. Towers are painted back to front, after the enemies, and the enemy overlays after them, so
  a covered enemy's bars and marks still show.
- A foundation never turns: only a head aims. A tier above the last wears the last tier's foundation.
- What a tower holds or is doing reaches its picture only through its `TowerPose`, built in that tower's visit method
  in `TowerSpriteFrameBuilder` and read in its look's builder in `TowerFoundations`: a new cue is a `TowerCue`
  constant, an identity a `TowerLook` row. Never a new draw record, a field on `TowerSpriteDraw` or a painter branch
  for one tower. A pose is snapped to a few steps, so the kept pictures stay few.
- A look whose body is the same in every pose is `Row.aiming`; any other is `Row.posed`.
- Of a tower's own picture only its idle lights (`Build.light`, sparks, a swirl and runes among them), a tier's rise
  and a placed tower settling are painted live; what moves round it (the nest, birds, a flame, a casing, steam) is a
  `TowerEffectDraw`. A live stroke or size is a share of `scale`, never a pixel constant: the art sheets paint with a
  scale of 1.
- Idle life runs on the wall clock (a light, a slow scan, the nest turning, birds circling); a moment (a shot, a
  reload, a rise, a placement, a knell, birds coming or leaving) runs on the tick clock.
- The nest circles the top of its tower, so no missile is drawn behind the tower.
- The Mortar has no head: it is stationary, its barrel part of its posed body.
- The toolbar icon and the Info pane's glyph are the look's `TowerIcons` row.
- The art sheets in `docs/features/next-gen-art` are rendered from these tables. After a change to a look, render
  them again and explain every sheet that changed.
- A lobbed shot's position is its point on the ground; how high it is drawn comes from the one arc in
  `ProjectileFrameBuilder`, never from the shot or its tower. The arc starts at the shot's launch height (a stone at
  the arm that threw it, a shell at the muzzle's one constant height) and climbs by its length on the ground.

## Two clocks

- `interpolationAlpha` (between the last two ticks) for anything backed by per-tick domain state:
  enemy/projectile position, turret heading. It respects pause and
  fast-forward.
- `animationSeconds` (wall clock) for pure cosmetics: idle lights, path
  chevrons. It keeps running while paused.
- Dead enemies are drawn without interpolation.

## Swing

- One look on every OS: nothing inherits the platform look-and-feel. A simple command control takes a
  text glyph (`▶`); any other icon is painted from vectors (a `Pictogram` through `TermIconPainter`, or a
  `PaintedIcon`) so it stays sharp at every HiDPI scale; only a tower tile shows an image, the tower's
  own board sprite rendered through the pipeline. Before choosing a colour, read `docs/UI.md`.
- Every control is a `HudButton`/`HudToggleButton`, every text input a `HudTextField` (Swing's
  basic UI, never the platform's), and every border comes from `Hud`. Controls
  override `paintComponent` without calling `super`, so the look-and-feel paints nothing but its
  listeners still work. A control varies only by its foreground (text) colour and its `ControlFace`: a
  button (a command: square, neutral border) or a tile (something to buy or pick: rounded, bordered in its
  accent), and its key chip. A tile and an upgrade node are one look (`BuyTileLook`): size on screen, border,
  corner and the price tag on the bottom edge. Neutrals are `Hud`'s greys everywhere, the upgrade tree and the
  level cards included; hover brightens a border on every control.
- The loaded level's voice is the one HUD colour that isn't a constant: `TowerDefense.showVoiceOf` hands it to the panel
  titles, the selected speed button and the XP bar when a level installs. Nothing else takes it.
- Every component is `@ThreadConfined(EVENT_DISPATCH_THREAD)`. Nothing here is touched from tick
  code; per-frame cosmetics run on the EDT render pulse (`TowerDefense.repaintPublishedFrame`).
- Components are built once and shown/hidden or refreshed, never rebuilt per level (a
  `CardLayout` for menu vs board; overlays toggled by visibility; `PanelWaveInfo`/
  `PanelUpgradeTree` share one cell and are toggled).
- A panel talks upward through a `Runnable` setter with a no-op default
  (`BoardOverlays.onBackToMenu`), never by reaching for the frame or a sibling panel.
- `GameBoard.paint` skips `super.paint()`; the renderer's background fill is what clears the
  frame.
- The board zooms only in the view: `BoardFitLayout` sizes it to its room and `GameBoard` scales
  the painted frame by its `BoardZoom`. Board pixels never change with the window, and a pointer
  becomes a board pixel only through `GameBoard.boardPixelAt`.
- `BoardOverlays` and `GameBoard` share one `GridBagLayout` cell with identical constraints.
  `TowerDefense.requestRender` repaints the *container* (repainting only the board erases the
  overlay). Overlay translucency is painted in `paintComponent`; an alpha background colour on an
  opaque component doesn't blend.
- Tooltips are `TooltipPanel`s on the frame's popup layer, owned by one `TooltipLayer`; what is hovered, where a
  tooltip goes and which stay open are answered by `SheetPane.hoverAt`, `TooltipPlacement` and `TooltipChain`.
  Never use `setToolTipText` for game content.
- The speed buttons in `PanelHeader` are a radio the owner selects from the live speed; a click only reports. A
  `StatusIcon` opens its tooltip under itself (`TooltipLayer.watchBelow`), and a level's first numbers are a baseline,
  never a change to flash.
- The Upgrades panel paints the whole tree itself (`UpgradeTreePainter`) at the places `UpgradeTreeLayout` takes
  from the tower's hand-made `UpgradeTreePlans` row, scaled to fit, never scrolled. Every tower has a row, and it
  places every node once; nothing in the panel is special to one tower. Two nodes share a slot only when they come
  after different members of one cluster: the slot shows the picked or hovered member's node, else stands empty. A
  node's look is an `UpgradeNodeState`, and
  its hover is a spot tooltip (`watchSpots`) of the node and what a click on it does (`UpgradeHover`).
- A question is a `ConfirmPopup` on the popup layer, never a platform dialog.
- A key chip shows the binding its key runs, never a copy: a tower's `placementKey`, a tier's `GameKey.singlesKey`,
  a `GameKey`, a dev `Shortcut`'s stroke. Game chips always show; dev chips only while Alt is held, drawn over the
  control so nothing moves.
- Info-pane content is an AWT-free `InfoSheet` that a `*SheetText` builds from a domain
  inspection (`EnemyInspection`, `TowerInspection`): stat rows, then icon rows (Traits, Buffs,
  Debuffs; on a tower Perks, Buffs, Debuffs, Applies, with an `AbilityRow` per ability after the Perks and a Mortar's
  `MagazineRow` after those), each icon a
  `SheetIcon` carrying its tooltip sheet. `InfoSheetDocument` alone lays
  a sheet out. The live enemy sheet is built with the frame and travels in
  `RenderFrame.enemyInspection`; the EDT only shows it, and only after a board click asked for a
  selection.
- A hover opens a tooltip and never changes the Info pane; only a click does. The pane shows the selected tower or
  enemy, the tower being placed, or the welcome text. A source with several targets (a strip of preview enemies) is
  watched with `TooltipLayer.watchSpots`.
- Every stat row, on any sheet, is built by `StatRowText` from a `StatBreakdown`: its mark from `StatLooks` (one
  exhaustive switch per stat family, glyph-only colour), its words from the stat's `StatTerm`. A stat named in prose
  is a mention whose hover is the stat's meaning; `StatUniformityTest` holds every stat to the same rules.
- A term's icon is an `IconFace` (AWT-free) that `TermIconPainter` alone paints; its art is a `Pictogram` keyed by
  the term's key in `PictogramCatalogue`, its colours come from `TermLooks`. A new term needs a row in a
  `*Pictograms` table (or it shows its monogram) and a case in `TermLooks`.
- Verify a board-look change from a `PreviewBoard` render, a panel or control change from a
  `run-jtd` screenshot.
