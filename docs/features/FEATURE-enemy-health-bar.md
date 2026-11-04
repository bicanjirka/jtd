# Feature Request: Enemy health bar

> **Status: built; its live check on a dense wave comes with the enemy and effect visuals.** Mock: <https://claude.ai/artifact/Ad29S1ti5eq1swojPPQ5tC>, the bars under the
> enemies, at the game's real sizes. The bar is drawn the way the mock draws it.

## Summary

A player should be able to read how hurt an enemy is at a glance, on a body that keeps its colour. A thin bar under the
enemy replaces health shown as opacity, and it appears only once the enemy is hurt, so a fresh wave carries no bars.

The bar is built as the first use of one general way to show a bar under an enemy, so later features add a bar by saying
what it measures and its colour.

The goal stays visual clarity and less clutter. Where a bar and clarity pull apart, clarity wins.

## Current state (what exists today)

- **Health is opacity.** `paintEnemyBody` fills the body at its health fraction and strokes it solid, so a hurt enemy
  loses its colour and shows the floor through it.
- **No bar exists on the board.** `TODO.md` lists a board health bar as a feature idea, deferred from the restoration
  rework.
- The Info pane already shows the selected enemy's health in detail.

## What this feature adds

### The health bar

| Property | Value |
|---|---|
| When | Only while the enemy is below full health |
| Where | Centred under the body |
| Size | On screen at the default window: 2 px high, 90% of the body's width, and never under 10 px wide |
| Fill | `#E8E4DA`, one neutral colour at every health level |
| Track | Black at 75% alpha, the bar's full width plus 1 px on every side, so it reads on every floor and lane |
| Direction | The fill shrinks toward the left |

Tune by eye in the real game, starting from these values.

The bar stays upright and clear of the body however the body turns or pulses: its size and place don't follow a body's
pulse, and a body that reaches past its scale doesn't cover it.

### Solid bodies

Health no longer changes a body's opacity. A hurt enemy keeps its full colour, and the bar alone reports its health.

### A general way to show a bar under an enemy

- A bar is a fill from empty to full and a colour. Adding a kind of bar means naming those two things, with no new drawing
  code for the kind.
- Several bars on one enemy stack downward in a fixed order, with the same width and a shared track, so they read as one
  element rather than a pile.
- Showing a bar and its position are decided in one place, so every bar on every enemy follows the same rules.

### Out of scope

- A shield bar and every other kind of bar: `FEATURE-enemy-and-effect-visuals.md` adds its own through this feature.
- Effects drawn on the enemy, markers, rank badges and bodies: also that request.
- Bars on towers.
- Any rule, number or balance change.

## Interconnections

- **The order of the work:** enemy looks move to tables first (ADR 0018, a step of the enemy and effect request, with no
  visible change), then every body gets its new size (the same request), then this feature, then the rest of that request.
  So the bar is built on the tables and judged at the final sizes.
- **`FEATURE-enemy-and-effect-visuals.md`** needs this feature: flames and ice need a solid body, and its shield bar uses
  the stack this feature provides.
- **The Info pane's health bar and shield strip** report the same numbers, so the board and the pane must not disagree.

## Constraints and open risks

- **Splash damage is the worst case:** it can hurt a whole wave at once, so a swarm carries many bars. Judge the look on a
  dense wave as well as on one enemy.
- **A cloaked enemy must not be given away:** the bar fades with the body, and shows only as much as the body does.
- **A dying enemy** shows no bar through its death.
- **Sizes are small.** Board pixels are fixed and only the view zooms: about 1.22 at the default window, down to 0.5. With
  the new body sizes, at the default window a plain enemy's radius is about 7 to 8 px, a mite's about 5.5 and the Warden's
  about 27. The bar must stay readable at the smallest zoom, and it is judged at these sizes, never enlarged.
- **Bodies reach past their scale:** up to 1.3 to 1.45 times it on the Warden egg, Gale, Hive Guard and Salamander, some
  of which turn. Check the bar against all 44 bodies.
- **Performance budget:** this adds to the frame build for every hurt enemy; run `td.PerformanceHarness` after it. Painting
  is outside the harness, so profile it live on a dense wave.
- **Facts that break:** `EnemyBodyDraw` carries the health fraction and `Java2DFrameRenderer.healthColor` uses it; no test
  reads it. Enemy previews paint through the same body code, so they turn solid too. `ui/CLAUDE.md` changes if a line
  about the body or overlays becomes false.
- **Standing rules:** vector art only, `td.ui.render` stays AWT-free, and looks are verified with `PreviewBoard`.

## Decisions made

- A thin bar once hurt, with solid bodies. A fade was rejected: flames and ice over a see-through body read worst, and a
  fade can't be judged precisely.
- It is drawn the way the mock draws it, in one neutral colour. The minimum width is 10 px, not the first mock's 16, which
  outweighed a swarm. At the smallest zoom a bar is about 5 px wide and 1 px high, and that is accepted.
- It ships as its own feature, with the general bar under the enemy as part of it.
- The shield bar, with all shields combined, belongs to the enemy and effect request.

## Open questions

None.

---

*After planning and implementation, update this document rather than deleting it: mark it implemented, prune resolved
open questions, and move it to `docs/features/shipped/`.*
