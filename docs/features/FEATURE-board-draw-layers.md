# Feature Request: Board draw layers

Status: requested.

## Summary

The board is a lit place seen from above and a little to the south: what stands on it shows its side down the screen.
Its paint order doesn't follow that depth. It paints kind by kind, and inside a kind in whatever order things were
built. So flat ground marks cover enemies and towers, an enemy's ice, tint or shield outline is painted over the tower
in front of it, and a spell's ground ring can cover a nuke's mushroom cloud. Every visual on the board (ground, towers,
enemies, everything on them, every shot and effect, every mark) should take one place in one order: first by how high
it is, then by how near the viewer it stands. The goal is clarity, not a 3D engine.

## Current state (what exists today)

- `Java2DFrameRenderer.paint` paints `RenderFrame`'s lists one after another: ground, cell grid, path marks, cell
  highlight, zone patches with what stands on them, tower ground (shadows), **all** enemy bodies and corpses, crit
  sparks, **all** towers (back to front), **all** enemy overlays, upgrade arrows, **all** tower effects, projectiles,
  the nuke's flash.
- What that order does wrong:
  - **Tower effects** (`TowerEffectDraw`, 32 kinds: blasts, spell blasts, beams, tracers, bullets, flames, the
    Sundial's shadow, wards, frost wakes, the Beacon's light, the nest and birds, muzzle smoke, casings, steam, status
    and refund marks...) are one list, ordered by tower placement (`world.towers().all()`) and then by the order a
    visit method adds them. A Mortar placed before a Burst has its nuke painted under the Burst's later spell blast.
    The whole list paints after towers, enemies and bars, so flat ground marks cover all of them.
  - **Enemies vs towers**: every enemy body paints before every tower, so an enemy south of a tower (nearer the viewer)
    is still covered by it. Every enemy overlay then paints after every tower, so the ice, tint, shield outline or burst
    of that same covered enemy is painted over the tower, detached from the body it belongs to.
  - **What stands on a zone** (flames, a mine's casing) paints before every enemy, wherever the enemy stands.
  - **Projectiles** (missiles, their smoke, shells) paint after everything but the flash, wherever they are.
- Some pieces already follow depth: towers are painted back to front; `BlastArt`'s cloud puffs are sorted by depth and
  its cloud bank splits into a far and a near half; `EnemyEmissionDraw` is split into a part under the body and a part
  over it.
- `docs/UI.md` §8 rule 2 already names bands of value, "ground, what stands, effects, signals". The paint order doesn't
  keep to them.
- Lines in `td/ui/CLAUDE.md` that state today's order and would change: "Ground zones are the one board list drawn under
  the enemies", "Towers are painted back to front, after the enemies, and the enemy overlays after them, so a covered
  enemy's bars and marks still show", "The nest circles the top of its tower, so no missile is drawn behind the tower",
  the new-visual rules that point at `EnemyOverlayDraw` and `TowerEffectDraw`.
- `PanelEnemy` and other panels reuse `Java2DFrameRenderer`'s paint methods (`paintEnemies`) for previews.

## What this feature adds

**One order for the whole board.** Every visual has a **height** and a **foot**, the point on the ground under it. The
board paints height by height, bottom to top, and inside a height from the far edge (top of the screen) to the near
edge, so the nearer thing is on top. Ties keep the order they were built in, so nothing flickers.

The heights:

1. **Ground**: everything flat. The floor, path marks, the cell highlight and range circles, zone patches, tower
   shadows, splash discs, spell blasts (discs, rings, sigils), a nuke's surge and shock ring, frost wakes, the
   Sundial's shadow, ward outlines, the Beacon's light edge, aura rings. Newer ground marks lie over older ones.
2. **Standing**: towers and enemies, sorted together by their feet, each with everything that belongs to it painted
   with it at its foot:
   - an enemy: its body or fading corpse, what an effect sends off it (under and over the body), its tint, ice, shield
     outline, cast and spawn pulses, bursts, a lightning shock, an execution, a crit spark, the Beacon's light on it;
   - a tower: its sprite and what moves at it (muzzle smoke, foot dust, pump steam, a shell carried to the muzzle, a
     spent casing, steam, embers);
   - what stands on a zone (flames, a mine's casing), at the zone's foot.
   An enemy walking in front of a tower is painted over it, and behind it under it, its ice and shield with it.
3. **Low air**: what flies or rises a little, between towers and enemies: beams, tracers, bullets, the laser, flame
   streams, stones, shells, spell orbs, curses, a Mortar shell's blast.
4. **Signals**: what the player reads: enemy bars, dots, rank badges, affix pips, hex runes, Toll and Silence marks,
   the "miss" mark, the selection ring, tower status and refund marks, upgrade arrows. Never hidden by anything that
   stands or flies low.
5. **High air**: what towers over the board: the Aerie's nest and birds, missiles and their smoke, a Thunderstrike's
   bolt, a nuke's fireball, stem and mushroom cloud.
6. **Flash**: the nuke's white-out, over everything (as today).

A visual sits at one height as a whole. Only where that reads wrong is it split across two heights; the nuke is the
one known case (its surge on the ground, its cloud in the high air). The list above places every kind drawn today;
planning confirms each, and a new visual names its height.

Out of scope: real 3D, shadows cast by effects, any change to how a visual looks other than where it falls in the
order, previews in panels (they keep painting their one subject).

## Constraints and open risks

- **Performance budget**: frame build ≤ 1 ms and ≤ 512 KB allocated per frame, paint ≤ 10 ms. One ordering over every
  visual on a full board (every buildable cell a tower) must stay inside it; `td.PerformanceHarness` checks it, also
  with `beacons` and `zones`.
- `td.ui.render` stays AWT-free and immutable; the frame is built on the game-loop thread and only painted on the EDT.
  `AsciiBoardRenderer` must still read the frame.
- Kept pictures (`Stamps`, `Glows`, `KeptPatches`, tower and enemy sprites) are keyed by what they draw; reordering must
  not multiply them. Zone patches stay one kept patch per kind.
- An enemy's decorations that today live in a separate list must still be built once per enemy; a cloaked enemy's art
  still fades with it.
- A visual split across heights paints each part from the same progress and seed, so the parts stay in step.
- Per ADR 0025 this decides nothing in play: no tests. Checked once with `PreviewBoard` stills and a `record` strip (an
  enemy walking past a tower, a frozen and shielded enemy behind a tower, a nuke and a spell blast landing together in
  both placement orders, flames under a crowd) and a `squint` render for UI.md §8 rule 2.
- The `td/ui/CLAUDE.md` lines above and UI.md §8 are rewritten in the same commit; the board's paint order is a
  structure worth an ADR.

## Decisions made

- The whole board, not only effects: towers, enemies, zones, shots, marks and effects all take their place in one
  order.
- Heights are a short fixed list, not a free depth number per visual. Nearer-is-on-top inside every height.
- Towers and enemies sort together; signals stay readable because they are a height of their own.
- What towers over the board (a nuke's cloud) covers signals for its moment.
- No change to the art itself; a visual is split across two heights only where one height reads wrong.

## Open questions

- Does the Aerie's nest belong to High air or to its tower in Standing? It circles the top of its own tower.
- A beam or tracer runs from a tower to an enemy: is its foot its nearer end, or does Low air simply keep build order?

---

*After planning and implementation, update this document rather than deleting it: mark it implemented, prune
resolved open questions, and note deferred scope.*
