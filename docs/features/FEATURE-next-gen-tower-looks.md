# Feature Request: Next-gen Tower Looks

**Status:** draft, 2026-11-10, reviewed with the whole bundle 2026-11-13. Part of `FEATURE-next-gen.md`, whose appendix
holds the visual request as given; this doc owns its "all towers" parts, keyed to tiers.

## Summary

Make the towers far nicer to look at and the board calmer. Every tower becomes a small piece of art in two parts: a
**foundation** that never moves and visibly grows with each tier the tower reaches, and a **head** that aims and
reacts when it fires or reloads. Upgrades stop being marks around the tower (pips, chevrons, halos, rank diamonds)
and become the tower's own shape, its ammunition and its cues. The toolbar gets new icons to match. The art follows
the style of the term icons, effect pictograms and header strip: vector, few colours, one clear silhouette. Each
tower's own look is in its doc; this doc holds what every tower shares.

## Current state (what exists today)

Every tower is drawn by `TowerSpriteFrameBuilder` (sprite and head) and `TowerEffectFrameBuilder`
(transients), painted by `Java2DFrameRenderer`:

| Layer | Today | Where |
|---|---|---|
| Body | One closed flat shape per type (`towerBodyShape`): Sniper triangle, Burst ring, Radar spiral, Obelisk star, Beacon pulsar, Mortar diamond, Hive kite, Scorcher flame. Fill at alpha 130/255, solid outline. 0.42 of a cell. Never turns. | `paintTowerBody` |
| Head | One shape per type (`turretHeadShape`), 0.24 of a cell, rotated to the aim. Sniper: a short bar (gold once Transcendent). Burst: a square block. Obelisk: a spinning star. Beacon: a pulsing circle (0.8x to 1.25x). Radar: its sweep. | `paintTurretHead` |
| Slot pips | One dot per owned node under the tower, grouped by slot in slot colours; Transcendent's pip is a gold diamond. | `paintSlotPips` |
| Ready chevrons | One up chevron per slot with an affordable, ungated node, in that slot's colour, above the tower. | `paintSlotReadyChevrons` |
| Enchant halo | A ring that pulses for as long as the tower owns any special. | `paintEnchantHalo` |
| Transcendent halo | A ring of gold dashes, one turn every 12 s. | `paintTranscendentHalo` |
| Rank pips | One silver diamond per rank above Recruit (Seasoned, Expert, Hero), stacked beside the tower. | `paintRankPips` |
| Rank-up glow | A ring that widens and fades over 30 ticks after a rank-up. | `paintRankUpGlow` |
| Jammed | A small marker in the top-right corner while disrupted. | `TowerStatusDraw` |
| Selection | A pink range circle. | `paintTowerSprite` |

- Always-running motion: the Beacon's two breathing rings, pulsing head and a line to every tower it buffs; the
  Obelisk's three ripple rings and spinning head; the Radar's sweep; the enchant halo; the Transcendent halo. Cosmetic
  loops run on `animationSeconds`, so they keep moving while paused.
- The toolbar icon (`renderTowerIcon`) is the body shape alone, one solid colour, 32 px, no head ("a two-tone glyph
  smudges at icon size").
- Primed-shot buffs are `TowerStatus`es and different ones stack, so two can ride one shot. Attack-speed buffs are
  statuses too. All show in the tower pane's Buffs row (`TowerBuffState`).
- Timed visuals follow one idiom: the entity records the tick something happened and the frame builder turns it into a
  0..1 progress.
- Ranks (`TowerRank`: Recruit, Seasoned, Expert, Hero at 50, 150 and 300 XP) grant nothing; they only show how much
  of the fight a tower has seen.
- Every effect the next-gen towers keep already has a marker colour (`EnemyFrameBuilder.markerPaletteFor`,
  `shipped/FEATURE-effect-visuals.md`): Sunder, Crack, Scorch, Bleed, Expose, Priority, Reveal, Chill, Freeze, Daze,
  Doom, Blight, Inversion, Vulnerable, Soulfire, Toll. The Obelisk's doc finds that Silence shows only in the enemy's
  pane.
- An earlier art mock, "jTD Tower Revamp Previews" (https://claude.ai/artifact/So7xnUnVE6o6f3iUhmyUYH), tried two
  directions (instruments around a core; a grounded workshop) and set freeform tier shapes against one shared tier
  grammar at 32 px cells. It is inspiration, not a decision.

## What this feature adds

### Anatomy of a tower

- **Foundation**: the static part (base, body, plinth). It never moves or turns. Its shape and complexity grow a step
  per tier, four steps for Tiers 0 to III, each clearly richer than the one before, so a Tier III tower reads as a
  prize. Fuller than today: a more opaque fill.
- **Head**: the moving part. It aims where it does today and has its own animation for firing and reloading, in the
  tower's style (recoil, a flare, a charge building). A tower that doesn't aim (the Aerie, the Obelisk, the Beacon)
  has its own moving part instead, named in its doc.
- **Identity looks.** A tower whose tree has an identity, a fork (the Burst) or a pick that changes what it is
  (Soulfire, Ruby or Shade, the Obelisk's stones), changes its look with it: the Burst becomes another tower, the
  others change material, colour or rune and keep their silhouette.
- **Upgrades that change how a tower fires or what it fires** (its ammunition) may change the head or the shot, each
  in its own way, chosen per upgrade.
- **Tower buffs** that change the next shot or the fire rate show on the head while held, and they combine: a head
  holding two primed shots shows both.
- **A tier unlock** is a moment on the board: the tower grows into its new foundation (open question 2).

### A calmer board

- Gone: slot pips, per-slot ready chevrons, the enchant halo, the Transcendent halo, the gold Transcendent barrel, and
  the Beacon's lines to the towers it buffs.
- **Upgrade available**: one small green up arrow on the tower when it has an affordable upgrade in an open tier,
  or a tier it can unlock. One mark, whatever the tier.
- **XP ranks go**: the tier is the tower's rank and the foundation shows it. The rank diamonds and the rank-up
  glow leave the board, and the pane shows XP toward the next gate (open question 1).
- Idle towers stay still or nearly so. Motion means something happened: a shot, a reload, a buff, an effect landing.
  Always-on loops (aura rings, ripples, pulsing heads, a nest circling an empty board) are cut back or replaced per
  tower.
- Kept: the selection range circle, the Jammed marker.

### Enemy marks the new towers need

Next-gen moves most of its look onto the towers, but some new debuffs need a mark on the enemy, beside today's
markers and in their style:

| Mark | From | Needs |
|---|---|---|
| Silence | the Obelisk's Ward, the Aerie's Shade, the Hexer's Choking Blight | a small icon above the head while it lasts: new for every source |
| Toll | the Obelisk, the Beacon's Prism | today's marker, filling toward full; one small ripple per stack gained |
| Blight | the Hexer | its overlay thickens as the slow grows |
| A hexed enemy | the Hexer | an overlay that reads as a curse, replacing or growing out of today's rune row |
| Ruby freeze | the Aerie | today's freeze crystal and thaw shards, recoloured ruby red |
| Shade freeze | the Aerie | today's freeze crystal and thaw shards, recoloured smoky dark with a pale green edge |
| Static, Soulbrand stacks | the Stormcaller, the Hexer | none (open question 7) |
| Frailty, Drag | the Obelisk | none: both are Toll's, and Toll shows |

### Toolbar icons

- A new icon per tower that matches its new board look: at most two colours, a simple, distinctive silhouette
  (foundation plus head, or one of them), as plain as today's. Seven icons: the Radar's goes.
- The shop sells the base tower, so forks and identities (Stormcaller, Hexer, Ruby, Shade) don't get toolbar icons.

### Palette

The tower colours have been in the game since its start; every one is reviewed and softened to fit the new look,
through `docs/UI.md`'s checks. Each tower also gets its tier colours for the Upgrades panel; the drafts set Tier 0 and
Tier I only (open question 6).

## Interconnections

- `FEATURE-next-gen-upgrade-tiers.md`: the foundation follows the tier; the tier unlock is drawn here.
- `FEATURE-next-gen-shot-flight.md`: what a shot looks like in flight.
- `docs/UI.md` holds what each colour already means. `shipped/FEATURE-hud-identity.md` made the chrome neutral, which
  leaves the pure primaries of the Sniper, Burst, Sonar and Pulse as the loudest colours on screen. A softer Sniper
  green lands on healing's, so each new value gets the guide's checks.
- `shipped/FEATURE-effect-icons-and-terms.md` set the icon style this art follows and made primed shots and
  attack-speed buffs named tower buffs; their head cues draw from the same state.
- `shipped/FEATURE-effect-visuals.md` set the enemy marker colours; marks a tower leaves on an enemy sit beside them.
- The ground grounds a tower leaves (the Mortar's payloads, the Obelisk's field, the Aerie's Ice Wake) draw as one
  patch per kind (`ZonePatchDraw`), as the Obelisk's doc decides.
- The README's tower section describes the pips, the special glow and the rank diamonds.

## Constraints and open risks

- **Frame budget**: every buildable cell a tower on the last level, frame build ≤ 1 ms and ≤ 512 KB
  (`td.PerformanceHarness`). Richer sprites, layered foundations and in-flight shots multiply draw commands per tower;
  the budget's worst case becomes a board of Tier III towers.
- **Readability at board size**: a tower is under half a cell today (0.42), a head a quarter. Each tier's shape must
  still read at the smallest board scale, distinct from the tower's other tiers and from other towers.
- **Two clocks**: animation driven by game state (recoil, flight, reload) must use the tick clock so it pauses and
  speeds with the game; wall-clock loops keep running while paused, which is what the calm board wants less of.
- The renderer holds every shape and colour, keyed by `Palette` roles, never by domain type; there are no image assets.
  The forked Burst looks are the most complex vector art in the game so far.
- **Tower looks become tables first** (ADR 0018, `TODO.md` "A board look is a switch in the painter"): that entry
  is closed before any new art is drawn. How the tables are shaped is the plan's.
- The board can show a primed shot, a target's rank or a running Frenzy only through state the tower publishes for the
  frame build; the engine stays headless.
- Removing slot pips removes the only on-board hint of how far a tower is upgraded; the foundation has to carry that.
- The tower pane's and the toolbar's previews draw the same art, so a richer sprite has to read at icon size too.

## Decisions made

- Remove pips, chevrons, halos and the gold barrel; one green up arrow says "an upgrade is ready".
- Foundation = tier, four visibly richer steps; head = aim and fire animation; upgrades that change firing or
  ammunition may add their own touch.
- An identity changes the look: the Burst's fork becomes another tower; a pick changes material, colour or rune.
- New toolbar icons, at most two colours, none for forks or identities.
- Look tables before new art.
- Ranks go (the user, 2026-11-13): no `TowerRank`, no diamonds, no rank-up glow.
- Ranks go (the user, 2026-11-13): no `TowerRank`, no diamonds, no rank-up glow.
- Every tower's art is approved on a `PreviewBoard` sheet before it is coded (`FEATURE-next-gen.md`).

## Open questions

⭐ marks the preferred option.

1. ✅ **XP ranks.** Dropped (the user, 2026-11-13: "drop, redone by the visual revamp anyway"). The tier is the
   tower's rank, and the foundation shows it. The rank diamonds and the rank-up glow go, and the pane shows XP
   toward the next gate.
2. ✅ **The tier unlock on the board.** The first option (the user, 2026-11-13); the art pass draws it.
   - ⭐ The tower grows: the new foundation builds up out of the old one over about half a second on the tick clock,
     with one ring of the tier's colour running out and fading. Motion that means something happened.
   - A flash, then the new foundation.
   - Only the new foundation appearing.
3. ✅ **How richly a tower is drawn.** The first option (the user, 2026-11-13); the art pass draws it. Today a tower is one flat shape at alpha 130 with an outline.
   - ⭐ Flat vector with tonal shading: each colour in two or three tones (shade, body, highlight), one light from the
     top left on every tower, and a soft contact shadow under the foundation. Still no gradients but glows, still no
     image assets.
   - Flat as today, only more opaque, and richer by shape alone.
4. **A tower's footprint.** The user (2026-11-13): yes to the growing footprint, "try it visually and evaluate";
   the art pass draws both. Pips, chevrons and halos no longer need room around the tower.
   - ⭐ The foundation grows with the tier, from about 0.55 of a cell at Tier 0 to about 0.8 at Tier III, so the tier
     also reads as size; the head stays a quarter of a cell.
   - The same size at every tier (0.42 today, or larger for all), the tier read by shape alone.
5. ✅ **One tier grammar or each tower its own.** The first option (the user, 2026-11-13). The Sniper's, the Obelisk's and the Beacon's docs draw freeform steps
   (triangles to a hexagon of triangles; a stone to a ring of stones; a post to a lighthouse); the mock's shared
   grammar drew the same orbits on every tower.
   - ⭐ Freeform steps, with one cue every tower shares: the number of plinth steps (or rings) under the foundation
     equals the tier, so any tower's tier reads the same way at a glance.
   - Wholly freeform: four shapes per tower to learn, the most character.
   - Wholly shared: one silhouette per tower with the same tier rings on all, the easiest to read.
6. ✅ **Tier colours for Tiers II and III.** The first option (the user, 2026-11-13); the art pass sets the values. Each draft's Tier I colour is a lighter, warmer step of its Tier 0.
   - ⭐ Continue that ramp for Tiers II and III per tower, set with the palette review (`docs/UI.md`'s checks).
   - One shared colour per tier for every tower (a bronze, silver, gold ladder): readable across towers, but the
     towers' panels lose their own colour.
7. ✅ **Passive stacks on the enemy.** Every enemy a Stormcaller or a Hexer hits carries a Static or Soulbrand
   stack. No mark for the stacks: a whole wave would wear one. The discharge and the flying soul are the visible
   moments, and the enemy pane lists the stacks (final review, autonomously).
8. ✅ **Range nodes.** The request had range upgrades make a tower read bigger or taller; next-gen ties the
   foundation to the tier. The tier alone changes the foundation; a range node lengthens the head's barrel or nozzle
   a little where the tower has one, and the selection circle shows the reach (final review, autonomously).
