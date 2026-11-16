# Feature Request: Tower visual polish

**Status:** art prepared 2026-11-16, not implemented. The art was checked by its author on stills and on single
frames; the user has not reviewed it, and the animation fixes of the last pass were rendered but not looked at
again ("Open questions").

## Summary

Make several towers taller, more distinct and more alive. All of it is aesthetic: no rule, number, range or hit
changes. The art is drawn already, in the game's own part vocabulary, in an art lab; this request is to move it into
the game. `FEATURE-next-gen-final-visual-review.md` is skipped (the user, 2026-11-16).

This doc names where each piece goes, which the template leaves to planning: the user asked for implementation and
verification instructions with the art.

## Current state (what exists today)

- A tower is a stack of vector parts built in `td.ui.TowerFoundations` from a tier and a `TowerPose`, painted by
  `td.ui.TowerSprites`; `FEATURE-next-gen-tower-looks.md` ("Building a tower's look") is the recipe.
- The art lab for this request, package `td.ui`, outside `src`:

  | File (`.claude/skills/run-jtd/td/ui/`) | Holds |
  |---|---|
  | `PolishLooks.java` | every new look as a builder; code marked COPY repeats what is private in `TowerFoundations` and is not moved |
  | `PolishLife.java` | everything painted live: idle life and moments, in board pixels |
  | `PolishSheets.java` | renders the sheets and the GIFs |

- The art, in `tower-polish-art/`:

  | Sheet | Shows |
  |---|---|
  | [looks](tower-polish-art/looks.png) | every changed tower at every tier, now against new, at 3x, 1x and 0.5x |
  | [lineup](tower-polish-art/lineup.png) | every tower side by side at each tier: the heights |
  | [closeups](tower-polish-art/closeups.png) | the heads and the tall towers at 7x |
  | [heads](tower-polish-art/heads.png) | the dragon's head at 16 headings, every pose a new builder reads |
  | [strips](tower-polish-art/strips.png) | eight moments of every animation |
  | `*.gif` (not in the repository) | every animation in motion; render them with the command below |

  ```bash
  # after mvn -q compile, with target/runtime-classpath.txt in place (run-jtd's SKILL.md)
  CP="target/classes;.claude/skills/run-jtd;$(cat target/runtime-classpath.txt)"
  javac -cp "$CP" -d target/polish-lab .claude/skills/run-jtd/td/ui/TowerArtGame.java .claude/skills/run-jtd/td/ui/Polish*.java
  java -Djava.awt.headless=true -cp "target/polish-lab;$CP" td.ui.PolishSheets docs/features/tower-polish-art          # all
  java -Djava.awt.headless=true -cp "target/polish-lab;$CP" td.ui.PolishSheets docs/features/tower-polish-art looks strips
  ```

  Names: `looks lineup heads closeups strips tops idle-stormcaller idle-hexer idle-aerie idle-aerie-tiers
  idle-obelisk orbit-options moment-knell moment-sundial moment-sniper moment-mortar moment-scorcher dragon-turn`.
- The Burst's fork is picked in Tier I's gate, so a Burst without a fork never passes Tier 0 in play, and the
  `SPLASH` row draws the Stormcaller above Tier 0.
- The Aerie has two identities, Ruby and Shade. There is no third: no node, no missile kind.
- The nest's ring is a circle round the tower's centre lifted to `Foundation.nest`, as wide as the footprint, and
  turns only with `SeekerTower.ringRadians()`: it stands still while the tower idles.
- The Sundial is a wedge (`ShadowDraw`), narrow at the stone and wider at its tip.
- A knell is `pose.fired()`: the rune flashes; the frame builder adds a ring.
- Soulfire and Dragon's Breath are one cluster, so no tower holds both.

### The perspective

Not written down before this request. The board is a plan-oblique drawing: the ground is drawn true to plan, and a
height `h` rises `0.42 h` straight up the screen (`TowerSprites.LIFT`). The nearest real camera is an orthographic
one about 65 degrees above the horizon (25 from straight down), which would draw heights at 0.42 and depth at 0.91;
the game rounds the depth to 1. A level circle is therefore a circle on screen. The nest and the birds are flattened
on purpose (`PolishLife.ORBIT_SQUASH`, 0.62) so they read as circling a spire; `orbit-options.gif` shows 1.0, 0.62
and 0.42 side by side.

A mark on a wall (a rune, a slit, a window) is a line or flat part at height 0 drawn in screen measures: a point `v`
up a face at `faceY` stands at `y = faceY - 0.42 v`.

## What this feature adds

### Looks

| Tower | New look | Lab | In the game |
|---|---|---|---|
| Burst without a fork, Tiers I to III | the stone thrower grown into a siege engine: taller frame, a counterweight, a walled yard with a banner | `thrower(tier, pose)` | the `SPLASH` row above Tier 0 |
| Stormcaller | a tesla coil: primary drum, a tall secondary striped in two coppers, a steel top-load carrying the ball | `stormcaller`, `ball` | replaces `stormcaller` |
| Hexer | an oracle's ball of violet glass on a twisted iron stem, cradled by claws (bone at Tier III) | `hexer` | replaces `hexer` |
| Scorcher with Dragon's Breath | a dragon's head for a turret head: neck, crest, a jaw that drops while it streams, horns | `dragonHead` | the head of `scorcher` when the pose has `DRAGONS_BREATH` |
| Soulfire | a soul cage: banded barrel, two blades and a fang round a pale flame, a small skull behind | `soulHead` | the head of `scorcher` when `soul` |
| Mortar | a larger shell (`BORE_SHELL` 0.05), a loading cradle, a reload in four moments | `mortarHead` | replaces `mortarHead` and the constant |
| Aerie | the same spire, narrowing as it climbs, the tallest tower at every tier; its perch ring near its top | `aerie`, `perch` | the `SEEKER` row |
| Ruby | a geode broken open: a cluster of leaning crystals round one great shard, an ember under them | `ruby`, `crystalShard` | the `RUBY` row |
| Shade | a dark tower: buttress fins, a shaft that steps in with spikes, two horns with nothing between them, pale green slits | `shade` | the `SHADE` row |
| White (new) | a white tower: marble shaft over small turrets, a balcony, a silver-blue roof, a gold needle and pennant | `white` | a new `TowerLook.WHITE` with its `TowerLooks` row (`0xe6eaf2`, the Aerie's tier ramp) and `TowerIcons` row |
| Obelisk, every rune | a menhir: rough, leaning, an uneven top, no pyramidion; runes cut into its south face, the great rune of its look at mid height | `obelisk`, `runes`, `greatRune`, the `MENHIR_*` constants | replaces `obelisk` and `rune` |

Heights of the tops, in cells, Tiers 0 to III: Stormcaller -, 0.99, 1.32, 1.60; Hexer -, 1.05, 1.35, 1.65; Aerie
1.08, 1.55, 2.02, 2.49; Ruby (from II) 1.74, 2.15; Shade 1.94, 2.35; White 1.91, 2.28; Obelisk 0.72, 0.99, 1.26,
1.53. `PolishSheets tops` prints them.

### Idle life (the wall clock)

| What | Lab | In the game |
|---|---|---|
| Tesla arcs jump from the Stormcaller's ball | `Life.SPARKS`, `PolishLife.sparks` | a new `TowerFoundations.Life`, a case in `TowerSprites.paintLights` |
| Motes and wisps turn inside the Hexer's ball | `Life.SWIRL`, `PolishLife.swirl` | the same |
| A wave of light climbs the Obelisk's runes | `Life.RUNE`, `PolishLife.rune` | the same; the rune's height on screen is its place in the wave |
| The nest's missiles circle the top of the Aerie on a flattened ring, also while it idles | `PolishLife.nest`, `NEST_RADIUS_CELLS`, `NEST_TURN`, `ORBIT_SQUASH` | `Java2DFrameRenderer.paintNest`; the phase is the tower's own plus `lifeSeconds * NEST_TURN` |
| Birds circle the top of every Aerie: terns (plain), ember birds (Ruby), crows (Shade), eagles (White) | `PolishLife.birds`, `Flock` | a tower effect from the Aerie's visit method, drawn whether or not the nest holds a missile |

### Moments (the tick clock)

| What | Lab | In the game |
|---|---|---|
| A spent cartridge flies from the Sniper's breech after a shot | `PolishLife.casing`, `CASING_SECONDS` | a tower effect, its progress from the ticks since the shot |
| The Mortar's reload: the bore smokes, a shell lies on the cradle, the arm carries it over, it drops in and glints | `mortarHead` reads `pose.reload()` by quarters | nothing new: the pose is snapped to quarters already |
| The Scorcher lets off steam when its reload starts | `PolishLife.steam`, `STEAM_SECONDS` | a tower effect at `TowerSprites.muzzle`, its progress from the ticks since the stream ended |
| A knell throws the Obelisk from side to side and turns its runes white | `obelisk` reads `pose.recoil()` through `KNELL_SHAKE` | the visit method sends `reloading(progress since the knell)` instead of `fired()` |
| The Sundial is the stone's own shadow: the menhir's outline on the ground, a dark body, a violet veil, a lit leading edge | `PolishLife.sundial`, `PolishLooks.menhirShadow` | the path `Java2DFrameRenderer.paintShadow` fills; reach, heading, fade and the Umbra rule stay |

### How to implement

1. Move a builder as it stands. Its measures, colours and pose reads are final; `Build`, `plinth`, `stoneRing` and
   the small helpers it calls exist in `TowerFoundations` under the same names. `scaled`, `mirrored`, `ball`,
   `crystalShard`, `perch` and the constants marked NEW move with it.
2. A look whose body reads the pose stays `Row.posed`. The Mortar and the Scorcher stay as they are: only their
   heads change.
3. A new life is a `Life` constant and a case beside `BREATH`; its painter is the `PolishLife` method, which
   already takes board pixels and `scale`. Its seed is the tower's own (what `lifeSeconds` is offset by).
4. A moment is a tower effect whose progress the frame builder derives from a tick the tower recorded
   (`td/ui/CLAUDE.md`, "Timed visuals follow one idiom"); the `PolishLife` method paints it from that progress.
5. Every live painter is a pure function of its clock or progress and a seed: store nothing between frames.
6. Out of scope, and needed before two looks can show in play: a way for a Burst to pass Tier 0 without a fork; a
   node, a missile and a name for the White tower.

### Verification after implementation

| Check | How |
|---|---|
| Each look matches its drawing | render the game's sheets (`next-gen-art/README.md`) and set `catalogue.png` and `heads.png` beside `looks.png` and `heads.png` here; every other changed sheet must be one this request explains |
| The lab and the game build the same parts | `TowerArtGame.dump` of the game's look against the same dump of the lab's `Built`, for every look, tier and pose on `heads.png` |
| The table rules hold | `TowerFoundationsTest`, `TowerPoseTest` |
| A cue reaches its picture | a `BoardRendererTest` per new pose read: a knell in, a recoiling pose out; Dragon's Breath in, the cue out |
| Idle life runs on the wall clock, moments on the tick clock | one live `run-jtd` pass: pause, and the sparks, the ball, the runes, the birds and the nest keep moving while a cartridge, steam, a knell and the Sundial hold |
| Each animation matches | the live pass beside `strips.png` and the GIFs |
| The nest circles the top | at Tier III the ring's centre stands at the spire's upper end, never at the cell's centre |
| A tall tower hides nothing it must not | an enemy on the path north of a Tier III Aerie still shows its bars and marks |
| Cost | `td.PerformanceHarness`, both loadouts; the sheets' `bench`; a live JFR of the painter on a board of Aeries and Stormcallers, since birds, sparks and the nest are painted live for every tower (`jtd-paint-is-outside-the-harness`) |
| Kept pictures stay few | count the poses a fight makes: the Mortar's reload and the knell add quarter steps only |
| Docs | `src/main/java/td/ui/CLAUDE.md` (one idle light, what is painted live, the nest), `next-gen-art/README.md`, `README.md`'s tower table, each tower's Look section |

## Interconnections

- `FEATURE-next-gen-tower-looks.md` owns the art direction. This request overrides three of its decisions: an idle
  tower carries one small light (now also sparks, a ball, runes, birds, a turning nest); an identity keeps its
  tower's silhouette (Ruby, Shade and White each have their own); the nest does not run on an empty board.
- `FEATURE-next-gen-seeker-tower.md`, `-splash-tower.md`, `-cinder-tower.md`, `-pulse-tower.md`, `-mortar-tower.md`,
  `-sniper-tower.md`: their Look sections describe what this replaces.
- `FEATURE-next-gen-final-visual-review.md` is skipped; its checks on cost and paint order are in the table above.

## Constraints and open risks

- Towers are taller than the looks doc allowed for: a Tier III Aerie rises about a cell above its own, so it covers
  more of what is north of it.
- Birds, sparks, the ball and the nest are live paint for every tower that has them, on every frame; painting is
  outside `td.PerformanceHarness`.
- At the board's own size a bird is a few pixels and a rune a dot; they are drawn to read as motion there, not as
  shapes.
- A mark on a wall is not foreshortened as its wall is (`RUNE_SQUASH`), or a rune would not read.
- `TowerLook.WHITE` is a look no tower wears until its node exists.
- The flattened orbit does not follow the board's projection, in which a level circle stays a circle.

## Decisions made

- The art is lab builders and sheets, moved into the game unchanged; no image assets (the user, 2026-11-16).
- The Obelisk loses both its pyramidion and the rune on its top (the user, 2026-11-16).
- Shade follows Barad-dur, without the eye: stepped, spiked, two horns. Orthanc's smooth shaft was too close to
  the White tower at this size.
- The dragon's head is prisms at their own heights turned in plan, so it turns in space with the 64 heading
  pictures a head already has; no sprite sheet.
- The knell's shake rides the pose's recoil, so it costs four kept pictures and no new draw field.
- The orbit's centre is the spire's top, so its far half stands above the tip and nothing has to be painted behind
  the tower.
- Beyond the request: Ruby, Shade and White each got a silhouette of their own; the Soulfire head carries a skull;
  the Mortar got a cradle and a loader's arm; the Obelisk's standing stones became rough stones; every Aerie look
  keeps a Release-ready cue; the Aerie's shards and the Stormcaller's insulators grew with their towers.

## Open questions

- The user's review of the art is owed. The last pass (bolder sparks, winged birds, a wider nest ring, a wider
  Sundial shadow with a lit edge) was rendered and not looked at.
- How flat the orbit is drawn: 1.0, 0.62 (drawn) or 0.42 (`orbit-options.gif`).
- The White tower's name, and whether it gets a missile of its own.
- Should the dragon's head also change the Scorcher's body (scales, a tail)?
