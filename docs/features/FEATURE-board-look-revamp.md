# Feature Request: Board look revamp

## Summary

Move the board from bright icons on a black slab toward the stylised, painterly look of *The Witness* and
*Firewatch*: a lit place with one palette per level, coloured light, layered values and quiet atmosphere, where loud
colour means something just happened. Along with it, the tools that make a visual change cheap to judge, motion
included, and a budget that sees what painting costs.

## Current state (what exists today)

- **Floors are near-black.** Every level's floor is `#06`–`#10` per channel (`docs/UI.md` §5), and `UI.md` rule 1
  keeps the board the quietest surface. The tower-looks feature accepted that shadows "barely show" on it
  (`shipped/FEATURE-next-gen-tower-looks.md`, "Shadows").
- **Shading ignores the scene.** One light from the top left; a standing part has three tones, its shade mixed 45%
  toward black and its rim 38% toward white, the same on every level. "No gradient but a glow."
- **One hue per thing.** `UI.md` gives every tower and effect its own identity hue at least 10 CIEDE2000 from its
  siblings. Enemies don't keep that today: 38 enemy pairs sit under 10.
- **Saturation.** Tower identities sit between 0.074 and 0.157 OKLCH chroma. Eleven enemy bodies are above that
  (Courier 0.22, Magma 0.21, Molten Core 0.20, Warden, Warden Egg, Eclipse, Salamander 0.19, then Hornet, Ember,
  Herald, Drone). Zone fields are saturated discs with hard outlines.
- **Contrast.** `UI.md` §7: a shape on the board needs 3:1 on the lightest floor. On today's floors every tower clears
  5.9 and every enemy 3.17.
- **Rasters.** ADR 0003: no image assets, all art vector. The ground is already drawn once into a kept image, and
  every body, foundation and head is a kept picture.
- **Motion.** Moments run on the tick clock, idle life on the wall clock. Easing is written by hand in four places.
  How long a moment's picture lasts is kept by the domain (the Mortar's blast marks are kept "as long as the longest
  detonation is drawn"), so a visual's length is a gameplay-package edit. Tower poses snap to four steps.
- **Previews.** `PreviewBoard` renders one still PNG. Every GIF in the repo comes from a lab that re-draws the game's
  looks (`PolishLooks`; the Mortar lab shadows `TowerFoundations`).
- **Budget.** `td.PerformanceHarness` gates the tick and the frame build (on The Amber Hive: build p99 0.46 ms of 1,
  allocation p99 418 of 512 KB). Painting is not measured. On 2026-11-03 the budgets read green while the live game
  painted at about a quarter of 60 fps.
- **Art sheets** live in `docs/features/shipped/next-gen-art`; `ui/CLAUDE.md` points at a path without `shipped/`.
  Review is `git diff --stat` on PNGs.

## What this feature adds

### The target look

Distilled from what the two games do, translated to a 2D board:

1. One palette per level, not one per object; an entity's hue sits in or beside the level's family.
2. Values in distinct layers: ground, what stands, effects, signals. A grey, blurred render shows them as bands.
3. Atmosphere is coloured: the board hazes toward the far (north) edge in the level's air colour.
4. Light has colour: lit faces lean toward the key light, shaded faces toward the level's shadow colour.
5. The minimum information per object: it reads at the smallest zoom, and every part carries meaning.
6. Saturation is spent: what persists is calm; full chroma marks a moment, a signal or the selection.
7. Shapes separate by value, not by outlines; round marks on the ground mean an area that acts.
8. Big fields get gradients, objects stay flat.

Out of reach in Java2D and not wanted: volumetric fog, per-pixel lighting, dynamic soft shadows from moving lights.

Sources: Jane Ng, "The Art of Firewatch" (GDC 2015), and coverage of it; Luis Antonio in "The Witness artist reminds
us to keep it simple" (Game Developer, 2014); Fletcher Studio, "The Witness: designing video game environments"
(2017).

### The board

- **Lifted floors.** Each level's floor becomes a mid-dark ground in its own hue, lighter and hazier toward the north,
  instead of near-black. Path marks and shadows are retuned to read on it. Mock:
  [floor options](board-look-revamp-art/floor-options.png) (today, lifted, dark with haze, each beside its grey
  "squint" view).
- **Light per level.** Each level has a key light, a shadow colour and a haze colour, fixed for the level, with a
  house default for a level that names none. A night level is a mid-dark blue or violet, not black.
- **Identity under the light.** A thing's top face keeps its exact identity colour, so the toolbar, the Info pane and
  the board agree. Its shade takes the level's shadow colour, with a darkening of its own so sides keep their depth,
  and its rim takes the key light. Mock: [tones](board-look-revamp-art/tones-options.png) (column a).
- **Fields.** A ground zone or range area has a soft gradient fill and a quiet, thin edge in the saturation band; its
  edge stays, because it says where the zone acts. The range circle and other lines on the ground are thin and soft
  too (owner, 2026-11-25), a hairline over a faint halo.
- **Ground circles.** A round mark lying on the ground means an area that acts. The path trail's dots become dashes
  or chevrons. Round bodies and plinths, which stand, are unaffected.
- **Saturation band.** Everything that persists on the board (bodies, idle life, zone fields and what stands on them)
  stays at or under the towers' highest chroma. Moments (a shot, an impact, a burst, a rise), signals and the
  selection may go to full chroma. The eleven enemies above the band and the zone fields are retuned.

### The tools

- **Recording motion.** The board preview records a stretch of play from the real game, as a GIF and as a frame
  strip (one PNG of frames side by side, readable by an agent and in a diff). A "squint" option renders any still or
  recording grey and blurred, for the value check.
- **One motion vocabulary.** Shared easing curves and a named timing scale; every timed visual takes both from it. The
  scale is in seconds, so an animation feels the same at 1x whichever clock drives it.
- **A paint budget.** The performance harness also paints the frames it builds and fails when painting goes over,
  in both its loadouts.
- **Art sheets.** A living copy of the sheets moves to `docs/art/`, with a README pointing at `UI.md`'s look section.
  Rendering them again reports, per sheet, the share of pixels that changed and writes a diff image under `target/`.
  The copies under `shipped/` stay as the record of what shipped.
- **No duplicate labs.** A lab never re-draws a look the game already has. The lab looks that copy shipped art are
  deleted once recording covers them.

### Visual timing leaves gameplay code

How long a visual lasts is a presentation choice: changing it must not edit a tower, enemy or projectile class. The
domain remembers what happened and when, for one bounded window, and presentation decides every duration within it.

### Out of scope

- Hand-made image files of any kind, and film grain or paper texture.
- Light that changes during a level (time of day across rounds).
- Golden-image tests in `mvn verify`.
- The interface chrome: panels keep `UI.md`'s ramp.

## Interconnections

- `docs/UI.md` (colour rules, floors, the checks in §7), ADR 0003 (no image assets), and
  `shipped/FEATURE-next-gen-tower-looks.md` (art direction, "no gradient but a glow") are what this request
  overturns in part.
- The tower-retune and rebalance work touches no colour, but the board screenshots it relies on will change.
- `.claude/skills/run-jtd` (the preview commands) and its art labs.

## Constraints and open risks

- **Contrast on lifted floors.** On the mock's far-edge floor (`#5A4A32`) the Obelisk (2.5:1) and the Burst (2.8:1)
  fail 3:1, so the haze cannot go that light. Capped where the weakest tower still clears 3:1 (about `#4D3F2A` on the
  Hive), eleven enemy bodies fall under 3:1 (the Warden lowest, 1.65), while their key-lit rims reach 3.0 to 4.5.
  This was measured on the Hive's warm key only; cooler keys on other levels are not checked yet.
- **A thin rim at the smallest zoom** may be one pixel and stop reading.
- **The crude mock** showed the amber path chevrons vanishing on an amber floor, and towers close to the ground in
  value: the floor alone does not carry the look.
- **The frame-build allocation budget** has about 18% headroom; painting has no number yet.
- **Kept pictures** are keyed by look, size, heading and zoom; a level's light now changes them too, once per level.
- **Saves** hold only between rounds (ADR 0011); nothing about a moment outlives a round.
- The engine stays headless, `td.ui.render` stays AWT-free, and the UI stays identical on every OS.

## Decisions made

1. Floors lift to a mid-dark ground in the level's hue, lighter and hazier toward the north.
2. Light is set per level (key, shadow, haze), with a house default, fixed for the level.
3. A thing's top face keeps its exact identity colour; shade and rim take the level's light.
4. One readability rule for everything that stands on the board: its body **or** its key-lit rim clears 3:1 against
   the floor's lightest stop, checked for every level's light. The floor's haze is capped by it, and the rim has a
   minimum width.
5. Saturation band: what persists stays at or under the towers' highest chroma; moments, signals and the selection
   may go full. "A moment" is what runs on the tick clock and ends; everything else persists.
6. The 10 CIEDE2000 sibling distance applies to towers and effects. Enemies differ by shape first.
7. Rasters generated in code are allowed, kept per level and zoom; gradients for fields only, objects flat; no image
   files, no grain.
8. Fields: soft gradient fill and a quiet edge; the range circle and rings are thin, soft hairlines. A round mark on the ground means an
   area that acts.
9. One shared motion vocabulary: curves and a named timing scale in seconds.
10. Recording writes a GIF and a frame strip; "squint" works on stills and recordings; labs never re-draw shipped
    looks.
11. Visual timing leaves gameplay code: the domain remembers moments for one bounded window, presentation owns every
    duration.
12. Tower pose steps are judged from a recording of a Sniper and a Mortar firing at 1x and 2.5x zoom; if they step
    visibly, the recoil stops being a snapped pose. Until then they stay at four.
13. Painting is gated: p99 at most 10 ms at 1x, measured headless, in both harness loadouts; live JFR is the
    tiebreaker near the limit. The tick and build budgets don't change. (4 ms was the first aim; the game already
    painted at 9.5 ms p99, and with its kept pictures still needs 6.7 ms on a full board and 9.1 ms with zones, the
    rest being animated art.)
14. Living art sheets in `docs/art/`, with a changed-pixel report per sheet; `shipped/` stays frozen.
15. Order: the tools first; then the whole look on The Amber Hive (the harness's level), reviewed by the owner; then
    the other five levels; then visual timing out of gameplay code. Levels not yet converted keep their floors and
    take the house light. One live `run-jtd` pass at the end.
16. The look rules land in `docs/UI.md`; what a tool can check (the readability rule per level light, the saturation
    band, the sibling distance) becomes a report the art-sheet tool prints, not a test (ADR 0025: looks get none); ADR 0003 gets a dated amendment; per-level
    light and visual timing each get a decision record when built.

## Open questions

1. **The house light and each level's light, floor stops and haze**: chosen by rendering in the pilot, not here.
2. **The window the domain remembers moments for**: at least the longest moment drawn; its length follows from the
   timing scale.
3. **Path trail**: dashes or chevrons only, decided by rendering.
4. **The timing scale's names and values**: decided from the curves already in the game.
