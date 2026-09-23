# Feature Request: A distinct ice-crystal visual for Freeze

> Not yet built. Nothing described below exists in the codebase today; this document proposes it.

## Summary

`EffectKind.FREEZE` currently renders as a slightly lighter blue than `EffectKind.SLOW`'s own
marker dot - the same glyph, the same row, the same colour family. A player can't tell "this
enemy is frozen solid" from "this enemy is merely slowed" without checking the numbers. Give
Freeze its own ice-cube/frozen-shard look so the two read apart at a glance, as already asked for
in `TODO.md`'s raw notes.

## Current state (what exists today)

- `EnemyFrameBuilder.markerPaletteFor` (`src/main/java/td/ui/EnemyFrameBuilder.java:114-123`) maps
  `SLOW` to `Palette.STATUS_MARKER_SLOW` and `FREEZE` to `Palette.STATUS_MARKER_FREEZE` - two
  different `Palette` roles, but both flow through the exact same drawing code.
- `Java2DFrameRenderer.colorFor` (`src/main/java/td/ui/Java2DFrameRenderer.java:435,437`) resolves
  those roles to `new Color(120, 120, 255)` (Slow, a muted blue-violet) and
  `new Color(150, 220, 255)` (Freeze, a lighter icy blue) - close relatives of the same colour,
  not a distinguishable pair.
- Both are painted by the one `paintStatusMarker` method
  (`Java2DFrameRenderer.java:719-725`), which fills the same `diamondShape` for every
  `EffectKind`, positioned in the same capped, three-wide marker row above the body
  (`EnemyFrameBuilder.java:307-320`, `MAX_VISIBLE_MARKERS = 3` at line 44). `StatusMarkerDraw`
  (`src/main/java/td/ui/render/StatusMarkerDraw.java`) carries only `(palette, x, y, scale)` - no
  shape field - so nothing about a marker's *shape* varies today, only its fill colour.
- Freeze is gameplay-real, not cosmetic-only: `Effect.freeze` sets `speedMultiplier` to `0f`
  (`src/main/java/td/effect/Effect.java:19-22,42-43`), a full stop, "modelled as a slow with a
  speedMultiplier of 0" per that file's own doc comment - stronger than any `SLOW` effect can be,
  which is exactly what today's near-identical visual fails to communicate.
- There is precedent for an ongoing effect getting a body-level treatment beyond the marker row:
  while `EffectKind.SHIELD` is active, `EnemyFrameBuilder.overlays`
  (`EnemyFrameBuilder.java:222-228`) adds an `EnemyRingDraw` - a static stroked ring at
  `scale * SHIELD_BUBBLE_SCALE_FRACTION` - to the `EnemyOverlayDraw` sealed hierarchy
  (`src/main/java/td/ui/render/EnemyOverlayDraw.java`, permitting `EnemyRingDraw`,
  `EffectPulseDraw`, `TraitMarkerDraw` today), painted by `paintEnemyRing`
  (`Java2DFrameRenderer.java:767-776`). Freeze has no equivalent; it only ever gets the one marker
  dot.
- Confirmed per `src/main/java/td/ui/CLAUDE.md` ("All art is vector..."): this project has no
  image assets anywhere. Every visual is a `java.awt.Shape` built and painted by
  `Java2DFrameRenderer` - the only class in `td.ui` allowed to import `java.awt`
  (root `CLAUDE.md` §2.4, `render-has-no-awt`). A concrete example of the kind of shape-
  construction code a new visual would sit alongside: `paintCritSpark`
  (`Java2DFrameRenderer.java:733-740`, a java.awt.geom.GeneralPath star built via `starShape`) and
  `paintEnemyRing` (`Java2DFrameRenderer.java:767-776`, a stroked java.awt.geom.Ellipse2D) - both
  small, self-contained methods keyed off one `Palette` role and one draw record.

## What this feature adds

- An enemy currently under `EffectKind.FREEZE` gets an additional, clearly ice-themed visual -
  an angular crystal/shard shape at or around its body - so a frozen enemy is unmistakable from a
  slowed one even in a crowd, without reading either enemy's stats.
- No change to Freeze's gameplay effect, to `SLOW`'s own visual, or to any other `EffectKind`'s
  marker.
- Out of scope: `TODO.md`'s separate raw note about making status-effect visuals a fixed pixel
  size instead of scaling with the enemy's own body scale - that is a different, broader request
  and isn't resolved here.

## Interconnections

- Formalizes a raw note already sitting in `TODO.md`: "change freeze visual to look like an ice
  cube/frozen shard around the enemy, it has to be clearly distinguishable from the slow/chill
  effect."
- Sits inside territory `docs/features/FEATURE-effect-visuals.md` already staked out: that
  feature is what built the `EnemyOverlayDraw` hierarchy (the shield bubble, the support-aura
  ring, gain/loss/cast/spawn pulses) and its own "everything stays in the existing visual
  language: thin vector rings, brief flashes, translucency" constraint. This request's proposed
  shape (see below) extends that same hierarchy rather than inventing a new one, and should stay
  inside that same restrained visual language.
- Adjacent to, but does not resolve, `TODO.md`'s other raw note about every status/trait/ability
  visual scaling with the enemy's own body size (tiny on a swarm mob, huge on a boss) - whatever
  shape this feature adds will inherit that same scaling convention every existing overlay uses,
  unless that separate request changes the convention first.

## Constraints and open risks

- **Vector-only, AWT-free frame model.** The new visual must be a `Shape` built in
  `Java2DFrameRenderer` (root `CLAUDE.md` §2.4, `render-has-no-awt`); any new draw record added to
  `td.ui.render` may only carry position/scale/palette-shaped data, never a java.awt.Shape or
  java.awt.Color value itself.
- **Marker-row crowding.** The existing row caps at three markers plus an overflow glyph
  (`EnemyFrameBuilder.MAX_VISIBLE_MARKERS`). If Freeze's new visual is *also* a marker-row entry,
  it competes for that same capped row exactly like every other kind; a body-level overlay (the
  route `SHIELD`'s bubble already takes) sidesteps the cap entirely, which is presumably why
  `FEATURE-effect-visuals.md` put the shield bubble there instead of relying on its dot alone.
- **Standing UI requirement.** Self-painted, OS-independent, and any look-and-feel change has to
  be verified from an actual screenshot of the running game (the `run-jtd` skill) - a look-and-
  feel defect like "still reads as basically the same blue dot" is invisible in code and in
  tests.
- **Engine stays headless; this is render-only.** `EffectKind.FREEZE`'s actual semantics
  (`speedMultiplier = 0f`) do not change. No gameplay logic belongs in `td.ui`
  (root `CLAUDE.md` §2.2).

## Decisions made

- **Ships as a body-level overlay, not just a recoloured marker dot.** A faceted ice-crystal/
  shard shape drawn around or over the enemy's body while `FREEZE` is active, added to the same
  `EnemyOverlayDraw` sealed hierarchy `EnemyRingDraw`/`EffectPulseDraw`/`TraitMarkerDraw` already
  share - plugging into `EnemyFrameBuilder.overlays()` (`EnemyFrameBuilder.java:222-228`, right
  alongside the existing `SHIELD` check) and a new `Java2DFrameRenderer.paintX` case dispatched
  from `paintEnemyOverlay` (`Java2DFrameRenderer.java:742-748`). Static, not timed - like the
  shield bubble, this reflects an ongoing state, not a one-shot transition, so it needs no
  grow/fade animation of its own (a gain/loss pulse for Freeze, via the existing
  `EffectPulseDraw`/`pulses()` machinery, already fires independently and is untouched by this
  feature).
- **Rough geometry** (exact vertices are an implementation call, not this document's): a small
  cluster of two or three overlapping angular polygons - a java.awt.geom.GeneralPath-built
  faceted crystal,
  the same construction style `starShape`/`diamondShape`/`kiteShape` already use
  (`Java2DFrameRenderer.java:244-347`) - roughly 1.2-1.5x the body's own scale (the same order of
  magnitude as `SHIELD_BUBBLE_SCALE_FRACTION`), centred on the body so it reads as "encasing" the
  enemy rather than floating beside it the way the marker row does.
- **Colour: a new `Palette` role, icy blue-white**, deliberately whiter and brighter than either
  of today's two look-alike blues (Slow's `(120, 120, 255)`, Freeze's current marker
  `(150, 220, 255)`) - a translucent fill plus a near-white facet-line stroke, so it reads as
  "solid ice" rather than another dot in the same blue family. `STATUS_MARKER_FREEZE` and its
  existing marker-row dot are left untouched by this decision; the new role is additive.
- **The existing marker-row dot stays**, unchanged in shape, position and cap behaviour, so the
  effect list stays scannable and consistent with every other kind. The new crystal is additive to
  it, not a replacement for the row's own bookkeeping - see Open questions on whether this is
  the right call long-term.

## Open questions

- Does the marker-row dot for `FREEZE` stay indefinitely once the body overlay exists (redundant,
  but lower-risk and consistent with every other kind), or should it be dropped/recoloured once
  the crystal is the primary signal, so a frozen enemy doesn't carry two blue-family indicators
  at once?
- Exact facet count, polygon vertices, and final RGB values are left to implementation - this
  document sketches them concretely enough to build from, not pixel-perfect.
