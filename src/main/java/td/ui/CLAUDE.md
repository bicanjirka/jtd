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

**`buildFrame` runs on the `game-loop` thread, not the EDT.** It walks live simulation state,
so it has to run on the thread that owns it; the `RenderFrame` it returns is the immutable
snapshot that crosses over, published through one `volatile` field in `TowerDefense`.
`TowerDefense.paintBoard` only paints what was published - it builds no frame and touches no
domain object. Keep it that way: moving frame-building back onto the EDT reintroduces the
data race this split exists to remove (root `CLAUDE.md` 3).

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

| Builder                   | Describes                                                                                   | Dispatch                                    |
|---------------------------|---------------------------------------------------------------------------------------------|---------------------------------------------|
| `CellFrameBuilder`        | placement/selection highlights                                                              | plain getters — only one `Cell` impl exists |
| `EnemyFrameBuilder`       | enemy bodies (each carrying its own `RankBadge` and cloak transition), death fades, status-effect markers (`buildMarkers()`), critical-hit sparks (`buildCritSparks()`), and shield/support-aura rings, gain/loss/cast/spawn pulses, and trait markers (all three via `buildOverlays()`) | `EnemyMobVisitor`                           |
| `TowerSpriteFrameBuilder` | tower base + animated turret head                                                           | `TowerVisitor`                              |
| `TowerEffectFrameBuilder` | beams, splash, pulse, aura, cone                                                            | `TowerVisitor`                              |
| `ProjectileFrameBuilder`  | in-flight shells and missiles                                                               | `ProjectileVisitor`                         |
| `PathMarkerFrameBuilder`  | the path's static trail and moving chevrons                                                 | none — pure geometry                        |

A status-effect marker is deliberately its own `RenderFrame` list (`statusMarkers`), not a
third permitted `EnemyDraw` subtype — `EnemyDraw`'s contract is "an enemy is either an alive
body or a fading corpse, never both, never neither," and a marker is neither of those on its
own. That split is also why `EnemyFrameBuilder.buildMarkers()` is a second output method
alongside `build()`, the same shape `TowerSpriteFrameBuilder` already uses for
`build()`/`buildHeads()`.

**The marker row caps at `EnemyFrameBuilder.MAX_VISIBLE_MARKERS` (3).** A 4th+ simultaneous
effect collapses into one `Palette.STATUS_MARKER_OVERFLOW` marker rather than growing the row
further — a heavily-buffed enemy in a packed wave still reads at a glance instead of sprouting
an unbounded strip of icons. This is a deliberately simpler stand-in for an exact "+N" count (which would need real text
rendering, a new render primitive nothing else here has) — a
future pass could add that without changing the cap itself.

**A critical hit's spark (`CritSparkDraw`, `RenderFrame.critSparks`) is a third, timed
`EnemyFrameBuilder` output, not a `StatusMarkerDraw`.** A crit is a one-shot event, not an
ongoing status - it is shaped like `EnemyFadeDraw` instead (position, scale, a `fadeProgress`),
keyed off `AbstractEnemyMob.ticksSinceCriticalHit(gameTime)` the same deferred-capture way a
death fade is keyed off `ticksSinceDeath`, and fades out over `EnemyFrameBuilder
.CRIT_SPARK_DURATION_TICKS`.

**Invisibility's transition is a component on the body itself, not a fourth marker/spark
list.** `EnemyBodyDraw.cloakProgress()` (0 = fully solid, 1 = fully cloaked) ramps over
`EnemyFrameBuilder.CLOAK_FADE_DURATION_TICKS` on either side of an `EffectKind.INVISIBLE`
gain/loss, derived from `AbstractEnemyMob.ticksSinceEffectGained`/`ticksSinceEffectLost` (backed
by `td.effect.EffectTransitions` - see `td/effect/CLAUDE.md`). `Java2DFrameRenderer.paintEnemyBody`
folds it into the body's alpha via `scaleAlpha`, composing with (not replacing) the existing
health-fraction alpha from `healthColor` - a badly wounded, cloaked mob reads as both at once. A
gain/loss transition for one of the other four kinds still goes through the ordinary marker row
(the dot appearing/disappearing *is* its transition); invisibility gets its own body-level
treatment because it is the one kind that changes what a mob's silhouette itself should look like,
not just what is decorated around it.

**`EnemyOverlayDraw` (`RenderFrame.enemyOverlays`) is one sealed hierarchy for every
enemy-centred ring, reused rather than grown into a new list per case.** `EnemyRingDraw` covers
both the shield bubble (while `EffectKind.SHIELD` is active, at
`EnemyFrameBuilder.SHIELD_BUBBLE_SCALE_FRACTION` times the body's own scale) and a support-aura
ring (`td.enemy.EnemyDefinition.supportAura()`'s radius, for a definition that projects an effect
onto nearby allies - the Ghost Elite's shroud, the Warden's call-to-arms, the Mender's heal).
This is the same move `TowerEffectDraw` already made for beams/splash/pulse/aura/cone: one list,
one exhaustive `Java2DFrameRenderer.paintEnemyOverlay` switch, rather than a `RenderFrame`
component per new overlay kind. Both rings are static (no `animationSeconds` involved) because
each reflects an ongoing state rather than a one-shot event - `EnemyFrameBuilder`'s constructor
still takes only `gameTime`/`interpolationAlpha`, deliberately not widened for this.

**`EffectPulseDraw`, the hierarchy's other member, is one shape for every one-shot transition: a
gain, a loss, an ability cast, or an ability-driven spawn's arrival.** It carries a target
`radius` and a 0..1 `progress`, never a stored current radius/alpha - the backend grows
(`PulseDirection.OUTWARD`) or shrinks (`INWARD`) the drawn ring from `progress` and fades it out
the same way, mirroring `CritSparkDraw`'s own shape. `EnemyFrameBuilder.pulses()` emits one for
every `EffectKind` (except `INVISIBLE`, whose transition is `cloakProgress` on the body itself)
that was just gained or lost per `AbstractEnemyMob.ticksSinceEffectGained`/`ticksSinceEffectLost`,
one for `AbstractEnemyMob.lastAbilityCast()` (see that record's own doc comment for why a cast
needs capturing on the caster at all, not just relying on each recipient's own gain pulse), and
one for `ticksSinceAbilitySpawn` - all three share `EFFECT_PULSE_DURATION_TICKS` and the same
grow/shrink-and-fade treatment, just keyed off a different tick and a different `Palette` role
(`Palette.SPAWN_BURST` for the spawn case, `markerPaletteFor(kind)` for the other two).

**`TraitMarkerDraw`, the hierarchy's third member, is a second, permanent marker row - hollow
diamonds below the body, mirroring the timed status row's filled ones above it.** One per
`td.enemy.Trait` a mob carries (`trait.marker()` names its glyph - see `Trait`'s own doc comment
for why that method is deliberately non-default), capped at the same `MAX_VISIBLE_MARKERS` and
collapsing into `Palette.TRAIT_MARKER_OVERFLOW` past it, for the same legibility reason. Hollow
rather than filled is the one deliberate visual difference from the status row, so the two are
never confused at a glance: a trait is permanent for this mob's whole lifetime, a status marker
is not.

## Two independent clocks, and which one to use

`buildFrame` receives both, and picking the wrong one is the most likely mistake in this
package:

- **`interpolationAlpha`** — where this frame lands between the last two *simulation* ticks (`[0, 1)`). Use it for
  anything reading domain state that advances per tick: an alive
  enemy's position, an aiming turret's heading, a projectile's position (`ProjectileFrameBuilder`
  lerps `getPrevX/Y()`/`getX/Y()` exactly like `EnemyFrameBuilder` does), or a cone tower's
  wedge heading (`TowerEffectFrameBuilder.visitCinderTower` reads `radiansAt(interpolationAlpha)`,
  the same heading its turret head renders at — not `TurretAim.currentRadians()`, which is what
  `InWedgeTargetQuery` uses to decide hits, a tick-boundary value rather than a rendering one).
  It respects pause and fast-forward.
- **`animationSeconds`** — monotonic wall-clock seconds. Use it for cosmetic animation with
  no domain state behind it: spinning turret heads, the Aura tower's pulse and ring, the
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

**Every enemy carries a rank badge, `EnemyBodyDraw.badge()` (a `RankBadge`), painted upright just
above the body - never rotated with `facingRadians`, the same reasoning `paintUpgradeAccent`'s
ring already follows for a tower's upgrade path.** `EnemyFrameBuilder`'s `badgeFor(Rank)` is the
one place a `Rank` maps to a badge; `Java2DFrameRenderer.paintRankBadge` is the one place a badge
becomes pixels. `RankBadge.NONE` (Grunt) paints nothing at all. Two of the five glyphs reuse
existing vocabulary - `chevronShape` (extracted from the path trail's own moving marker) for
Soldier/Veteran's one/two stripes, `starShape` for Elite. **Boss's skull is this package's first
representational glyph** - everything else here is a geometric primitive (circle, square,
triangle, spiral, star, pulsar); `skullShape` builds it from `java.awt.geom.Area` boolean ops
(a cranium fused with a jaw, minus two eye sockets and a nose notch) rather than one closed path.

Several switches here are deliberately exhaustive with **no `default`** —
`TowerSpriteFrameBuilder.bodyPaletteFor`, `Java2DFrameRenderer.colorFor`, the sealed
`EnemyDraw`/`TowerEffectDraw` switches. Adding a type is then a compile error until its art
exists, instead of a silently missing sprite. Do not add a `default` branch to "fix" a
compile error; add the missing case.

`Java2DFrameRenderer` pattern-matches over the **sealed** `EnemyDraw`/`TowerEffectDraw`
hierarchies. That is a compiler-checked switch over a closed set of DTOs the renderer
itself defines, and is the one narrow exception to the project's no-`instanceof` rule. It
is not license to switch on `EnemyMob`, `Tower` or `Cell`.

Toolbar icons go through `renderTowerIcon`, which reuses the same `towerBodyShape` the board
does — so a tower's board look and its icon can never diverge in *shape*. How it is painted
does differ, deliberately: the icon is one flat colour at half the tile, with no turret head,
on a dark rounded backing. Those are all decisions about a 32px glyph rather than about the
board, and `renderTowerIcon`'s own doc comment explains each.

Every tower body is a single closed `Shape` (triangle, circle, spiral, star, pulsar). Keep it
that way: the moment one needs two colours it needs its own paint method, and the shared
fill-then-outline path stops being shared.

Turret heads mostly work the same way, through `turretHeadShape`. The sonar head is the one
exception — a radar sweep is read from its fading trail, which needs several wedges at
different alphas, so it has its own `paintSonarSweep` and `turretHeadShape` throws for it.
It is also sized to stay inside its own tile: a head reaching across neighbouring cells reads
as a weapon with reach rather than as an instrument.

A tower that has permanently chosen an upgrade path (see `td.tower.upgrade`) gets a thin
ring drawn just outside its body — `Java2DFrameRenderer.paintUpgradeAccent`, fed by
`TowerSpriteDraw.accent()`. This is deliberately **not** one more per-tower-per-path shape:
there are only two accent roles, `Palette.TOWER_UPGRADE_PATH_A`/`_B`, shared across every
tower type and keyed off *which slot* the tower specialized into (`TowerSpriteFrameBuilder.accentPaletteFor` does `tower.availablePaths().indexOf(tower
.getChosenPath())`, not a per-path identity lookup) — so the accent is one consistent
two-colour visual language the player learns once, not eight colours to memorize. It is
also always a circle regardless of the body's own shape, for the same reason the selection
ring is: legibility matters more than matching the body's silhouette here.

## The HUD look is ours, not the platform's

**Every clickable control is a `HudButton` or a `HudToggleButton`, and every panel border
comes from `Hud`.** Do not add a bare `JButton`, and do not style one by hand.

`Hud` owns the palette, the fonts, the one-pixel outline and the painting of every control.
The controls override `paintComponent` and never call `super`, so the look-and-feel paints
none of them — that is deliberate, and there are two reasons it has to stay that way:

- The same build must look identical on Windows, macOS and Metal. Default Swing button chrome
  does not.
- The L&F picks disabled-text and disabled-icon colours for a *light* button face. Against
  this game's black panels those have come out invisible more than once, which is a bug you
  cannot see in the code — only in a screenshot.

The L&F's *listeners* stay installed, so pressed/rollover/selected still track the mouse
normally. Only painting is taken over.

**The one sanctioned exception is a control's text colour**, used by `PanelTowerInfo` to
set its sell button's text apart from its upgrade-path buttons (destructive vs. constructive
action) via the ordinary `Component.setForeground` — `Hud.paintControl`'s
`paintCentredText` already reads a button's own foreground colour rather than a hardcoded
one, so this needs no change to `Hud`/`HudButton` and no bypass of their painting. This is
narrow on purpose: border, fill, hover/press states and font all still come from `Hud`
untouched. Don't read it as license to style a control by hand more broadly — the rule
above still holds for anything else.

Prefer a glyph character (`►`, `▮▮`) over drawn artwork for a simple control. **Verify any
UI change from an actual screenshot of the running game** — see the `run-jtd` skill.

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
  constraints and they stop overlapping — they end up side by side, and whichever gets cell (0,0) pushes the other out
  of view.
- `TowerDefense.requestRender` repaints the *container*, not `GameBoard`. A `JPanel` reports
  that its children never overlap, so repainting the board alone paints over the overlay
  without painting it back and the message vanishes on the next frame.

The overlay layer is transparent and only its inner plate paints anything, so the final
board stays readable underneath. Translucency there has to be painted in `paintComponent` —
an alpha-channel *background colour* on an opaque component does not blend, which is what
made the original overlay solid black.

**Nothing here may be touched from tick code.** Rendering reaches the EDT via
`SwingUtilities.invokeLater` — see the root `CLAUDE.md` §3 (Threading). This was broken once:
`TowerDefense.doTick` called `PanelWaveInfo.doTick` straight through to two `JPanel`s on the
`game-loop` thread. Anything cosmetic that wants to advance every frame belongs on the EDT
render pulse (`TowerDefense.repaintPublishedFrame`), which runs at a flat ~60fps and is where
the wave preview is driven from now — the simulation's cadence is not the right one for it
anyway.

**Every component here declares `@ThreadConfined(EVENT_DISPATCH_THREAD)`.** A Swing component
holds mutable state by nature, and `scripts/VerifyRules.java` requires any class that does to
name its owning thread. A new `Panel*` needs the annotation or the build fails.
