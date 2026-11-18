# Feature Request: Tower visual polish

**Status:** art final, approved by the user 2026-11-18 after four rounds of review; ready to implement. Nothing is
implemented yet. The implementing session starts at "Instructions for the implementing session".

## Summary

Make several towers taller, more distinct and more alive, and give the Sniper, the Mortar, the Scorcher's flame and
the Burst's stones clearer shots and moments. All of it is aesthetic: no rule, number, range or hit changes. The art
is drawn already, in the game's own part vocabulary, in an art lab; this request is to move it into the game.
`FEATURE-next-gen-final-visual-review.md` is skipped (the user, 2026-11-16).

This doc names where each piece goes, which the template leaves to planning: the user asked for implementation and
verification instructions with the art.

## Current state (what exists today)

- A tower is a stack of vector parts built in `td.ui.TowerFoundations` from a tier and a `TowerPose`, painted by
  `td.ui.TowerSprites`; `FEATURE-next-gen-tower-looks.md` ("Building a tower's look") is the recipe.
- The art lab for this request, package `td.ui`, outside `src`:

  | File (`.claude/skills/run-jtd/td/ui/`) | Holds |
  |---|---|
  | `PolishLooks.java` | every new look as a builder; code marked COPY repeats what is private in `TowerFoundations` and is not moved |
  | `PolishLife.java` | idle life and moments painted live, in board pixels: sparks, ball, runes, nest, birds, cartridge, steam, Sundial, the Mortar's reload arc |
  | `PolishShots.java` | the Sniper's rounds and ricochets, the Headhunter's laser, the execution, Frenzy's embers, the stone's size |
  | `PolishFlame.java` | the Scorcher's flame: merged cones, silhouettes, heat |
  | `PolishSheets.java` | renders the sheets and the GIFs |

- The art, in `tower-polish-art/`:

  | Sheet | Shows |
  |---|---|
  | [looks](tower-polish-art/looks.png) | every changed tower at every tier, now against new, at 3x, 1x and 0.5x |
  | [lineup](tower-polish-art/lineup.png) | every tower side by side at each tier: the heights |
  | [closeups](tower-polish-art/closeups.png) | the heads and the tall towers at 7x |
  | [heads](tower-polish-art/heads.png) | the dragon's head at 16 headings, every pose a new builder reads |
  | [strips](tower-polish-art/strips.png) | eight moments of every animation |
  | [stones](tower-polish-art/stones.png) | the Burst's stone now and at its new size |
  | `*.gif` (not in the repository) | every animation in motion; render them with the command below |

  ```bash
  # after mvn -q compile, with target/runtime-classpath.txt in place (run-jtd's SKILL.md)
  CP="target/classes;.claude/skills/run-jtd;$(cat target/runtime-classpath.txt)"
  javac -cp "$CP" -d target/polish-lab .claude/skills/run-jtd/td/ui/TowerArtGame.java .claude/skills/run-jtd/td/ui/Polish*.java
  java -Djava.awt.headless=true -cp "target/polish-lab;$CP" td.ui.PolishSheets docs/features/tower-polish-art          # all
  java -Djava.awt.headless=true -cp "target/polish-lab;$CP" td.ui.PolishSheets docs/features/tower-polish-art looks strips
  ```

  Names: `looks lineup heads closeups strips stones tops idle-stormcaller idle-hexer idle-aerie idle-aerie-tiers
  idle-obelisk orbit-options flock moment-knell moment-sundial moment-sniper moment-mortar moment-scorcher
  dragon-turn sniper-shots sniper-states flame`.
- The Burst at Tier 0 is a ring of stones round a throwing arm on a dark patch of ground (`TowerFoundations.thrower`):
  it reads as a pit.
- The Burst's fork is picked in Tier I's gate, so a Burst without a fork never passes Tier 0 in play, and the
  `SPLASH` row draws the Stormcaller above Tier 0.
- The Aerie has two identities, Ruby and Shade. There is no third: no node, no missile kind.
- The nest's ring is a circle round the tower's centre lifted to `Foundation.nest`, as wide as the footprint, and
  turns only with `SeekerTower.ringRadians()`: it stands still while the tower idles.
- A ring round a column (the Stormcaller's guard rings, the Aerie's ring) goes through `Build.around`, which puts
  its far half after the column, so the far half is drawn over it.
- The Aerie's ring round its spire carries one cue: it brightens and glows while the next freeze will set off a
  Release (`TowerCue.RELEASE_READY`).
- The Sundial is a wedge (`ShadowDraw`), narrow at the stone and wider at its tip.
- A knell is `pose.fired()`: the rune flashes; the frame builder adds a ring.
- Soulfire and Dragon's Breath are one cluster, so no tower holds both.
- The Mortar is a turret: a barrel on trunnions that turns to its target, a shell showing in its bore once loaded.
- A Sniper's bullet is a `TracerDraw` from the muzzle, green to a white head; a Fourth Shot adds a wide faint white
  bolt; Hollow Point's tail is orange, Silver Round's pale blue, all as thin as a plain shot. A ricochet is always
  the plain green streak. The lens brightens with Steady Aim stacks up to three; Frenzy offsets the head by one
  fixed step and draws three red strokes; the Headhunter draws a crosshair on the lens.
- A bullet flies white only when it carries `GUARANTEED_CRIT` (`SniperTower.Bullet.sureCrit`, the Fourth Shot's);
  every other crit is rolled when the bullet lands and shows only in the fading trace after it.
- An execution is drawn after the kill: a thin white ring on the enemy bursting into eight short white strokes
  (`TowerEffectFrameBuilder.execution`, from `SniperTower.lastExecution()`), easy to miss beside a crit's white trace.
- The flame is three nested tongues per stream (`StreamDraw`, `paintStream`), each stream painted alone, so two
  crossing cones stack their alpha; an enemy in a cone is hidden under the fire.
- The Burst's stone flies as a `CannonballDraw` of size 1: a radius of `PROJECTILE_SIZE` (5 px on a 32 px cell).

### The perspective

Not written down before this request. The board is a plan-oblique drawing: the ground is drawn true to plan, and a
height `h` rises `0.42 h` straight up the screen (`TowerSprites.LIFT`). The nearest real camera is an orthographic
one about 65 degrees above the horizon (25 from straight down), which would draw heights at 0.42 and depth at 0.91;
the game rounds the depth to 1. A level circle is therefore a circle on screen. The nest and the birds are flattened
on purpose (`PolishLife.ORBIT_SQUASH`, 0.62, the user's pick 2026-11-18) so they read as circling a spire.

A mark on a wall (a rune, a slit, a window, the Mortar's legs) is a line or flat part at height 0 drawn in screen
measures: a point `v` up a face at `faceY` stands at `y = faceY - 0.42 v`.

## What this feature adds

### Looks

| Tower | New look | Lab | In the game |
|---|---|---|---|
| Burst without a fork, every tier | a wooden onager, the cheap stone thrower a village builds, set across its cell to throw over the north-east: a plank bed, two rails, a rope skein with a winch at each end, a tall stop frame with a padded crossbar, one arm with a cup, a basket of stones. The arm lies cocked with a stone in its cup, stands slammed into the pad at the throw, and is winched down while it reloads. The tiers grow it and never ring it: iron bands and spoked winches (I), side braces, iron caps and a heap of stones (II), a banner (III). No pit | `thrower(tier, pose)`, `turned`, `beam`, the `THROWER_*` constants | the whole `SPLASH` row, Tier 0 included |
| Stormcaller | a tesla coil: primary drum, a tall secondary striped in two coppers, a steel top-load carrying the ball; the guard rings' far halves go behind the coil. Every tier stands on the riveted iron plate (a smaller one at Tier I): the Burst's stones are gone | `stormcaller`, `ball`, `Build.aroundColumn` | replaces `stormcaller` |
| Hexer | an oracle's ball of violet glass on a twisted iron stem, cradled by claws (bone at Tier III). Every tier has the three iron spokes and standing stones, short and with dim runes at Tier I: the Burst's stones are gone | `hexer` | replaces `hexer` |
| Scorcher with Dragon's Breath | a dragon's head for a turret head: neck, crest, a jaw that drops while it streams, horns. The body stays (the user, 2026-11-18) | `dragonHead` | the head of `scorcher` when the pose has `DRAGONS_BREATH` |
| Soulfire | a soul cage: banded barrel, two blades and a fang round a pale flame, a small skull behind | `soulHead` | the head of `scorcher` when `soul` |
| Mortar | a stationary siege mortar, see "The Mortar" | `siegeMortar`, `leg`, the `MORTAR_*` constants | replaces `mortar` and `mortarHead`; no head |
| Sniper | the same rifle, showing its focus and its Frenzy, see "The Sniper" | `sniperHead`, the `FOCUS_*`, `KEEN_GOLD`, `FRENZY_SHAKE*` constants | replaces `sniperHead` |
| Aerie | the same spire, narrowing as it climbs, the tallest tower at every tier; a dark blue disc round the spire's foot so the pale crystal stands out from its blue base; no ring (the user, 2026-11-18): while a Release is ready its tip turns white and glows, as Ruby's, Shade's and White's do | `aerie`, `AERIE_FOOT*` | the `SEEKER` row; the ring and its arms go |
| Ruby | a geode broken open: a cluster of leaning crystals round one great shard, an ember under them | `ruby`, `crystalShard` | the `RUBY` row |
| Shade | a dark tower: buttress fins, a shaft that steps in with spikes, two horns with nothing between them, pale green slits | `shade` | the `SHADE` row |
| White (new) | a white tower: marble shaft over small turrets, a balcony, a silver-blue roof, a gold needle and pennant | `white` | a new `TowerLook.WHITE` with its `TowerLooks` row (`0xe6eaf2`, the Aerie's tier ramp) and `TowerIcons` row; unused on purpose, ready for a future identity (`TODO.md`) |
| Obelisk, every rune | a menhir: rough, leaning, an uneven top, no pyramidion; runes cut into its south face, the great rune of its look at mid height | `obelisk`, `runes`, `greatRune`, the `MENHIR_*` constants | replaces `obelisk` and `rune` |

Heights of the tops, in cells, Tiers 0 to III: Stormcaller -, 0.99, 1.32, 1.60; Hexer -, 1.05, 1.35, 1.65; Aerie
1.08, 1.55, 2.02, 2.49; Ruby (from II) 1.74, 2.15; Shade 1.94, 2.35; White 1.91, 2.28; Obelisk 0.72, 0.99, 1.26,
1.53; Mortar 0.47, 0.56, 0.63, 0.70 (the muzzle); Burst 0.38, 0.50, 0.62, 0.74 (the stop frame). `PolishSheets
tops` prints them.

### Idle life (the wall clock)

| What | Lab | In the game |
|---|---|---|
| Tesla arcs jump from the Stormcaller's ball | `Life.SPARKS`, `PolishLife.sparks` | a new `TowerFoundations.Life`, a case in `TowerSprites.paintLights` |
| Motes and wisps turn inside the Hexer's ball | `Life.SWIRL`, `PolishLife.swirl` | the same |
| A wave of light climbs the Obelisk's runes | `Life.RUNE`, `PolishLife.rune` | the same; the rune's height on screen is its place in the wave |
| The nest's missiles circle the Aerie's top, also while it idles. The ring's centre is the top of the tower (`Foundation.nest` = `top` for every Aerie look) and its radius 0.30 cells, so its far half passes above the tip and a missile is never behind the tower | `PolishLife.nest`, `NEST_RADIUS_CELLS`, `NEST_TURN`, `ORBIT_SQUASH` | `Java2DFrameRenderer.paintNest`; the phase is the tower's own plus `lifeSeconds * NEST_TURN` |
| Birds circle the top of a quiet Aerie: terns (plain), ember birds (Ruby), crows (Shade), eagles (White). They come in one after another once the tower has been quiet a while, and scatter, climbing and beating faster, when it fights | `PolishLife.birds`, `Flock`, `Presence`, the `BIRD_*` and `FLOCK_*` constants | a tower effect from the Aerie's visit method. Their circling runs on the wall clock; when they come and go runs on the tick clock (`Presence`) |

### Moments (the tick clock)

| What | Lab | In the game |
|---|---|---|
| A spent cartridge flies from the Sniper's breech after a shot | `PolishLife.casing`, `CASING_SECONDS` | a tower effect, its progress from the ticks since the shot |
| The Scorcher lets off steam when its reload starts | `PolishLife.steam`, `STEAM_SECONDS` | a tower effect at `TowerSprites.muzzle`, its progress from the ticks since the stream ended |
| A knell throws the Obelisk from side to side and turns its runes white | `obelisk` reads `pose.recoil()` through `KNELL_SHAKE` | the visit method sends `reloading(progress since the knell)` instead of `fired()` |
| The Sundial is a real shadow, see "The Sundial" | `PolishLife.sundial`, the `SUNDIAL_*` constants | the body of `Java2DFrameRenderer.paintShadow`; reach, heading, fade and the Umbra rule stay |
| The Mortar's reload: a shell lifted off the rack into the muzzle, see "The Mortar" | `PolishLife.mortarLoad`, `MORTAR_ARC_PEAK` | a tower effect, its progress the reload's own, not snapped |
| An execution: the headsman's cut on the enemy the Executioner killed, see "The Sniper" | `PolishShots.execution`, the `EXECUTION_*` constants | the body of `TowerEffectFrameBuilder.execution`, its progress from `SniperTower.lastExecution()`'s tick |
| Frenzy shakes the Sniper's head and red embers rise off it | `sniperHead(.., shake, ..)`, `PolishShots.frenzy` | the shake a pose step, the embers a tower effect |

### The Sundial

The floors are near black, so a shadow alone would not show. The low sun lights a band of the ground along the
shadow's line (the violet wedge the game draws today, wider and softer, which the user preferred to the stone's
outline), and the stone's shadow lies dark down the middle of it. As the shadow of a sun-lit stone it is sharp and
as wide as the stone at its foot, narrows toward the stone's top, and its blurred edge (the penumbra) widens and pales
with distance. Each soft edge is six nested layers. The leading side, where the hit lands, keeps a faint lit line.
An Umbra stone inside its own field lights the band darker and bluer, as today.

### The Mortar

A stationary, ground-anchored siege mortar standing in the middle of its cell, as every tower does: a bedplate, four
splayed legs braced across (twice from Tier II), and a thick, wide-mouthed barrel fixed at a steep angle to the north.
Nothing turns. A long barrel (`LONG_BARREL`) adds a slice. No sandbags and no battlements, so nothing hides the rack
(the user, 2026-11-18): from Tier I it stands on a plank deck (`MORTAR_DECK`), iron-edged and bolted from Tier II, and
at Tier III a low iron rail on posts runs round the back half only.

- **Scale:** every shell on the tower (racked, carried, loaded) is the in-flight shell's own art
  (`TowerFoundations.shell`, `TowerSprites.paintShell`) at `MORTAR_SHELL`, about a third of its size in flight, so the
  rack fits the cell and the barrel stays in proportion (the user, 2026-11-18). The bore is sized to it.
- **The rack:** a small wooden rack on a crate (`MORTAR_RACK_HEIGHT`) in front of the mortar holding one shell of every kind the tower can load, in the
  kinds' order: three on the bottom row, the rest lying on top (at most five: Normal, Heavy, Nuke under Fusion, its
  payload, Flare). It shows what the tower has unlocked, whatever its magazine holds now, and it never empties: it is
  the stock, not the magazine. A racked shell is drawn without its glow, which would hide its neighbours.
- **The shot:** the barrel is thrown down its own axis (`pose.recoil()`), the muzzle flashes.
- **A single load:** the bore smokes for the first quarter of the reload; in its middle half a shell of the front
  shell's kind is lifted off that kind's place on the rack and carried on a smooth parabola into the muzzle; from the
  last quarter its top shows above the muzzle until the shot, with a glint once ready.
- **Barrage and Carpet Bombing (`VOLLEY`):** three shells, one a quarter from the shot, each off its own kind's place,
  loaded back to front: the shell fired last goes in first and the front shell last, so the front shell's top is the
  one showing when the volley fires. Between, the muzzle shows the shell that went in last.
- The barrel and the rack read the pose by quarters (`mortarLoadedCount`, `mortarShowing`); only the shell in the air
  is live paint.

### The Sniper

| What | Lab | Picture |
|---|---|---|
| A bullet by round | `PolishShots.shot`, `Round` | plain: thin, green to a white head; a crit's trace after landing: white; Fourth Shot: bold, red round a white-hot core, a wide red glow; Hollow Point: bold copper to a hot orange head, a warm glow; Silver Round: bold silver-blue round a white core, a cold glow, glitter left behind |
| A ricochet | `PolishShots.ricochet` | drawn as the round that made it on the game's bowed arc. A ricochet follows a crit, so a plain shot hops white |
| Steady Aim stacks | `sniperHead` | the lens brightens per stack to a star from three, as today; from `FOCUS_STACKS` (5) the scope is focused: a green halo and ring, sight marks either side, a breathing green light at the muzzle |
| Full stacks under Keen Eye | `sniperHead(.., keenFocus)` | the lens a golden eye: a gold ring and iris with a slit pupil, long gold rays, a breathing gold glow, a gold line down the barrel |
| A loaded Fourth Shot | `sniperHead` | the muzzle burns red round a white core, larger than today |
| Frenzy | `sniperHead(.., shake, ..)`, `FRENZY_SHAKE`, `PolishShots.frenzy` | the whole head jumps between four offsets every `FRENZY_SHAKE_TICKS` ticks, a red glow under it, red strokes off it, and red embers rise above the turret and go out |
| Headhunter aiming at an Elite or a Boss | `PolishShots.laser` | a very thin red laser from the scope's lens to the enemy, a faint shimmering glow along it, a red dot where it lands; the lens crosshair stays |
| Executioner, a kill it made | `PolishShots.execution`, `EXECUTION_*` | only after the Executioner killed: a crit left the enemy under its line and the node executed it. The headsman's cut: a blade of white light slashes down across the enemy's body in the first instant, red at its edges. Below it, on the ground round the enemy's feet, a dark stain spreads and a crimson ring snaps out, both flattened onto the ground as a lobbed shell's shadow is (`EXECUTION_GROUND_SQUASH`, the renderer's `SHELL_SHADOW_FLATTENING`). Red-hot shards burst off both sides of the cut, fall, and go out where they land, over `EXECUTION_SECONDS`. It needs the enemy's ground point and its body's height. Nothing is drawn while the Sniper aims or a bullet flies: no mark promises a kill (the user, 2026-11-18). It replaces today's thin white ring and strokes |

### The flame

Every stream of the frame is painted in one pass, so two cones that cross merge into one flame instead of stacking
their alpha: the outer and middle layers of every stream are joined into one shape each and filled once; each
stream's core stays its own. Flashovers stay the game's small tongues. A cone's solid fire covers its whole range
and its ragged tips lick past it, shorter toward its sides, so no enemy it burns stands outside the drawn flame.

- **An enemy in the jet** looks as a body standing in a jet of flame does, instead of being hidden:
  - its own outline as a semi-transparent dark silhouette over the fire;
  - heat pooling against its front, a soft hot glow on the side facing the nozzle;
  - a dimmer wake trailing downstream behind it, where its body blocks the fire (`WAKE_*`);
  - the hot rim: one flat line round its whole outline in the colour of its heat, thicker the hotter it burns, over
    a flat dark silhouette;
  - tongues of flame (`TONGUES` a side) that split on its front, hug its flanks and stream away downstream, tapering
    to a point and wavering; brighter than the fire behind them, as a flame front is, and a narrower white-hot tongue
    inside the first;
  - a few embers shed off its flanks downstream (`EMBER*`).
- **One body per enemy:** an enemy two cones hold is drawn once. Its hottest cone sets its heat, colour and lit
  side, each further cone adds a little heat (`CROSSFIRE_HEAT`), and the Core's bonus counts if any cone holds it in
  its Core; so crossfire warms a rim without reaching the colours only upgrades reach.
- **Heat:** how hot it looks follows the fuel (`PolishFlame.heatOf`): 0 is the bare flame's fuel a second
  (`FLAME_POINTS`), 1 the fuel with every damage node (Hot Flame, White Flame, Hot Flame II). Each layer ramps from
  red-orange to yellow, then white, then blue-white at the core. A stream's heat is its own fuel a second; an enemy's
  rim takes the fuel that enemy is fed, so the Core (plus `CORE_HEAT`), Thermal Shock and Meltdown each make its rim
  hotter. The rim runs orange-red, yellow, white, then ice-blue, which only a full stream in the Core reaches; a
  Soulfire rim runs violet, lilac, white, then pale cyan. Soulfire keeps its violet ramps.
- Stylised as the board: filled shapes, strokes and gradients, no blur filter. Close-up in `flame.gif`'s bottom
  row.

### The Burst's stone

The thrown stone is drawn at half its size (`PolishShots.STONE_SIZE`, 0.5): the `CannonballDraw` it is sent with in
`TowerEffectFrameBuilder.stone`, its shadow with it. Its flight is unchanged.

### Answers to the review's questions (2026-11-18)

- **Do the birds fly the same every time?** No. Each bird's ring, height, speed (within a fifth either side), phase
  and wing beat come from the tower's seed, and its ring slowly breathes in and out; the birds of one tower drift
  against each other, so the flock's picture seldom repeats and no two towers fly alike. One bird alone does repeat
  its own circle.
- **The blue circle at the top of the Aerie** was the Aerie's ring, kept from the game's look. It did one thing: it
  brightened and glowed while a Release was ready. It is gone (the user prefers the spire without it); the Release
  cue moved to the spire's tip, which turns white and glows, as the other Aerie looks already showed it.
- **How is a crit known in advance (the white bullet)?** Only a bullet that carries `GUARANTEED_CRIT` knows it: the
  Fourth Shot's. Every other crit is rolled when the bullet lands, and shows in the trace after it.
- **Can an execution be known in advance?** Not for certain: a crit is rolled when the bullet lands (unless it is
  sure), the enemy may dodge, and its health may change while the bullet flies. So nothing is drawn before; the
  execution is drawn when it happens (round 2: the mark before it is dropped).

### Instructions for the implementing session

Implement the polish in the game. Aesthetics only: no rule, number, range or hit changes. The art is drawn and
approved: move it, do not redesign it. If something looks wrong against this doc, report it instead of redesigning.

**Read first, in full:** this doc, then the lab (`.claude/skills/run-jtd/td/ui/`: `PolishLooks`, `PolishLife`,
`PolishShots`, `PolishFlame`, `PolishSheets`), then `td/ui/CLAUDE.md` and `FEATURE-next-gen-tower-looks.md`
("Building a tower's look"). Render the GIFs with the command under "Current state" to see every animation; they are
not in the repository.

**Critical points:**

- A lab builder moves as it stands (step 1 below); a live painter moves as a pure function of clock or progress plus
  a seed, its strokes and sizes shares of `scale` (steps 3 to 5). Step 6 lists what each picture needs from the game:
  that, and only that, is new game-side state.
- The clocks: idle circling (sparks, ball, runes, nest, the birds' circling) on the wall clock; every moment (casing,
  steam, knell, Sundial, the Mortar's reload arc, Frenzy, the birds coming and going, the execution) on the tick
  clock, so it holds on pause. The flame is a moment too: its flicker, and the heat, tongues, wake and embers round an
  enemy in it, all run on the tick clock (`PolishFlame.paint`'s `seconds` is game time), so a paused flame
  holds still.
- The Burst's onager replaces Tier 0's look too, the one players see. Its Tiers I to III are drawn but unreachable
  in play; add the looks, not a way to reach them.
- The Mortar becomes a posed row with no head. Its rack is the kinds it can load (`MagazineView.chances` plus Nuke
  under Fusion), never the magazine; it changes only with an upgrade, so it belongs with the kept pictures. Count the
  kept pictures it adds.
- The flame paints every stream of a frame in one pass and needs per-enemy outlines and fuel; it is the costliest
  piece (shape unions and gradients every frame): measure it live with JFR (`jtd-paint-is-outside-the-harness`)
  on a crowded board (several Scorchers whose cones cross, a wave inside them). If it is overly expensive (it costs
  the EDT's paint a visible share of a frame, or frames drop), optimise it without changing the picture: for
  example cache each tongue shape per flicker step, skip the union when no two cones overlap, reuse the gradient
  paints per heat step, and cap the enemies drawn in the jet per stream to the nearest few. Measure again and report
  the before and after.
- The Aerie loses its ring; its Release cue moves to the tip. The Sniper's execution is drawn only from
  `lastExecution()`; nothing marks an enemy before.
- `TowerLook.WHITE` is a look only, unused on purpose (`TODO.md`).
- This request overrides three decisions in `FEATURE-next-gen-tower-looks.md` ("Interconnections"): fix every line
  in it, in `td/ui/CLAUDE.md`, `next-gen-art/README.md`, `README.md`'s tower table and the towers' Look sections that
  the change makes false, in the same commit. `FEATURE-next-gen-final-visual-review.md` is skipped.

**Phases, one commit each** (on main, staging your own files by name, `mvn verify` before each, no push):

1. Looks: the builders and their rows (Burst, Stormcaller, Hexer, Scorcher heads, Mortar, Sniper head, Aerie, Ruby,
   Shade, White, Obelisk), with the pose reads they need (knell, focus, Keen Eye, Frenzy shake, the Mortar's
   load). Render the game's sheets (`next-gen-art/README.md`) and compare them with `looks.png`, `heads.png`,
   `closeups.png`.
2. Idle life: sparks, swirl, rune wave, the nest at the top, the birds with `Presence`.
3. Moments: casing, steam, the Sundial, the Mortar's reload arc, Frenzy's embers.
4. The Sniper's shots: rounds, ricochets as their round, the Headhunter's laser, the execution.
5. The flame and the Burst's stone at half size.
6. Docs and verification: every check in "Verification after implementation", one live `run-jtd` pass at the end,
   `td.PerformanceHarness` both loadouts. Leave the lab as it is: it renders this doc's sheets.

### How to implement

1. Move a builder as it stands. Its measures, colours and pose reads are final; `Build`, `plinth` and the small
   helpers it calls exist in `TowerFoundations` under the same names. No look uses `stoneRing` any more: delete it
   once nothing calls it. `scaled`, `mirrored`, `ball`,
   `crystalShard`, `leg`, `turned`, `beam`, `Build.aroundColumn` and the constants marked NEW move with it. The
   game's Aerie ring and the Burst's stone ring and dark patch go.
2. A look whose body reads the pose stays `Row.posed`. The Mortar becomes a posed row with no head. The Scorcher
   stays as it is: only its heads change.
3. A new life is a `Life` constant and a case beside `BREATH`; its painter is the `PolishLife` method, which
   already takes board pixels and `scale`. Its seed is the tower's own (what `lifeSeconds` is offset by).
4. A moment is a tower effect whose progress the frame builder derives from a tick the tower recorded
   (`td/ui/CLAUDE.md`, "Timed visuals follow one idiom"); the `PolishLife` or `PolishShots` method paints it from that
   progress.
5. Every live painter is a pure function of its clock or progress and a seed: store nothing between frames.
6. What each new picture needs from the game, the art fixing only the picture:
   - Birds: two ticks the Aerie records, when its last fight ended and when its present one began, and whether it
     fights now (a missile of its own in flight), into `Presence.of`.
   - Rounds: a bullet's `Round` from its rounds and its crit (`FOURTH_SHOT`, `HOLLOW_POINT`, `SILVER_ROUND`, else a
     crit's trace `CRIT`, else `PLAIN`); a hop carries the round of the shot that made it, since its own rounds are
     cleared.
   - Steady Aim: the pose's stack count past three (`FOCUS_STACKS`), and a new cue for full stacks under Keen Eye
     (the lab's `keenFocus`).
   - Frenzy: a shake step, `(tick / FRENZY_SHAKE_TICKS) % 4`, reaching the head through its pose (four kept
     pictures a heading while Frenzy runs); the embers on the tick clock from the tower's seed.
   - Headhunter: the position of the enemy the Sniper aims at while it hunts it; the laser leaves from the lens
     (`0.09, -0.05` on the head, turned with it, at the head's height plus 0.08).
   - Executioner: no new rule. `SniperTower.lastExecution()` (its tick and place) is recorded only when
     `ExecutionerMechanic` executes, which is exactly the moment to draw: a crit that left the enemy under the line.
     Its place is the ground point; the cut also needs the body's height on screen, from the enemy's look (its
     `EnemyLooks.Form` height and size), carried in the record or read when it is made.
   - Mortar: the reload's progress (not snapped); `VOLLEY`; the kinds this reload loads, front first (the
     magazine's front shell, or its front three for a volley); the kinds on the rack: every kind with a share in
     `MagazineView.chances`, and Nuke under Fusion. The rack changes only with an upgrade, so it belongs with the
     look's kept pictures, not to every frame. A lobbed shell leaves from `mortarMouth`, the muzzle that no longer
     turns.
   - Flame: every stream of the frame together; a stream's fuel a second; for each enemy in a cone, its outline
     (its look's top shape at its size and facing), its centre, the nozzle, and the fuel it was fed this tick.
7. Out of scope, and needed before two looks can show in play: a way for a Burst to pass Tier 0 without a fork; a
   node, a missile and a name for the White tower.

### Verification after implementation

| Check | How |
|---|---|
| Each look matches its drawing | render the game's sheets (`next-gen-art/README.md`) and set `catalogue.png` and `heads.png` beside `looks.png` and `heads.png` here; every other changed sheet must be one this request explains |
| The lab and the game build the same parts | `TowerArtGame.dump` of the game's look against the same dump of the lab's `Built`, for every look, tier and pose on `heads.png` |
| The table rules hold | `TowerFoundationsTest`, `TowerPoseTest` |
| A cue reaches its picture | a `BoardRendererTest` per new pose read: a knell in, a recoiling pose out; Dragon's Breath in, the cue out; five stacks in, the focus out; Keen Eye full in, its cue out; Release ready in, the Aerie's tip white out; a Mortar's unlocked kinds in, their shells on the rack out |
| A ring's far half is hidden | at 7x, the Stormcaller's guard rings pass behind their coil |
| Idle life runs on the wall clock, moments on the tick clock | one live `run-jtd` pass: pause, and the sparks, the ball, the runes, the circling birds and the nest keep moving while a cartridge, steam, a knell, the Sundial, the Mortar's shell in the air, Frenzy's embers and the birds coming or leaving hold |
| Each animation matches | the live pass beside `strips.png` and the GIFs |
| The nest circles the top | at Tier III no missile is ever drawn over the spire from behind it |
| Birds only at a quiet tower | live: an Aerie that fights has no birds; quiet a few seconds, they come in one by one; a fight scatters them |
| The Mortar reloads as drawn | live, a Mortar with Barrage and a payload: three shells rise off the rack, each from its own kind's place, the front shell last, and its top shows in the muzzle until the shot; the rack shows every unlocked kind and never empties |
| An execution shows once | live, a Sniper with Executioner: the cut plays on an enemy a crit left under the line, and on no other kill; nothing marks an enemy before |
| The flame merges | live, two Scorchers whose cones cross: one flame where they cross, no darker overlap; an enemy in a cone shows as a silhouette with its hot edge toward the nozzle |
| A tall tower hides nothing it must not | an enemy on the path north of a Tier III Aerie still shows its bars and marks |
| Cost | `td.PerformanceHarness`, both loadouts; the sheets' `bench`; a live JFR of the painter on a board of Aeries, Stormcallers and Scorchers, since birds, sparks, the nest, the merged flame (shape unions every frame) and the silhouettes are painted live (`jtd-paint-is-outside-the-harness`) |
| Kept pictures stay few | count the poses a fight makes: the Mortar's reload and the knell add quarter steps, Frenzy four shake steps, Steady Aim at most six lens states |
| Docs | `src/main/java/td/ui/CLAUDE.md` (one idle light, what is painted live, the nest, the Mortar as a row with no head), `next-gen-art/README.md`, `README.md`'s tower table, each tower's Look section |

## Interconnections

- `FEATURE-next-gen-tower-looks.md` owns the art direction. This request overrides three of its decisions: an idle
  tower carries one small light (now also sparks, a ball, runes, birds, a turning nest); an identity keeps its
  tower's silhouette (Ruby, Shade and White each have their own); the nest does not run on an empty board.
- `FEATURE-next-gen-seeker-tower.md`, `-splash-tower.md`, `-cinder-tower.md`, `-pulse-tower.md`, `-mortar-tower.md`,
  `-sniper-tower.md`: their Look sections describe what this replaces. The Mortar's turret is gone.
- `FEATURE-next-gen-final-visual-review.md` is skipped; its checks on cost and paint order are in the table above.

## Constraints and open risks

- Towers are taller than the looks doc allowed for: a Tier III Aerie rises about a cell above its own, so it covers
  more of what is north of it. The Mortar's muzzle and its protruding shell reach above its cell.
- Birds, sparks, the ball, the nest, the merged flame and the silhouettes are live paint on every frame; painting is
  outside `td.PerformanceHarness`. Joining the cones is a shape union per layer per frame.
- At the board's own size a bird is a few pixels and a rune a dot; they are drawn to read as motion there, not as
  shapes.
- A mark on a wall is not foreshortened as its wall is (`RUNE_SQUASH`), or a rune would not read.
- `TowerLook.WHITE` is a look no tower wears until its node exists.
- The flattened orbit does not follow the board's projection, in which a level circle stays a circle.
- The shells on the Mortar are drawn smaller than in flight, on purpose; a larger shell (Big Bore) grows only in
  flight.

## Decisions made

- The art is lab builders and sheets, moved into the game unchanged; no image assets (the user, 2026-11-16).
- The Obelisk loses both its pyramidion and the rune on its top (the user, 2026-11-16).
- Shade follows Barad-dur, without the eye: stepped, spiked, two horns. Orthanc's smooth shaft was too close to
  the White tower at this size.
- The dragon's head is prisms at their own heights turned in plan, so it turns in space with the 64 heading
  pictures a head already has; no sprite sheet. The Scorcher's body stays (the user, 2026-11-18).
- The knell's shake rides the pose's recoil, so it costs four kept pictures and no new draw field.
- The orbit is flattened to 0.62 (the user, 2026-11-18). Its centre is the tower's top, so its far half stands above
  the tip and nothing has to be painted behind the tower.
- The White tower is a look kept for the future, unused on purpose (the user, 2026-11-18; `TODO.md`).
- The Sundial is the old wedge reworked into a lit band with a soft shadow in it; the stone's outline is dropped
  (the user, 2026-11-18).
- The Mortar's shells are the in-flight shell's art, drawn smaller than in flight so the rack fits and the barrel
  stays in proportion; the mortar stands in the middle of its cell; its rack shows every kind the tower can load,
  never the magazine (the user, 2026-11-18, second round).
- The plain Burst is an onager, not a pit, at every tier (the user, 2026-11-18, second round). It is turned to throw
  over the north-east, so its arm reads in the raised view; it still never turns.
- The Aerie has no ring; its Release cue is its tip (the user, 2026-11-18, second round).
- The Executioner is drawn only when it executes, never before (the user, 2026-11-18, second round).
- Beyond the request: Ruby, Shade and White each got a silhouette of their own; the Soulfire head carries a skull;
  the Obelisk's standing stones became rough stones; every Aerie look keeps a Release-ready cue; the Aerie's shards
  and the Stormcaller's insulators grew with their towers; the birds' rings breathe; the Mortar got a wooden rack
  and a glint once loaded; the Burst's onager got winches, braces, a basket and a heap of stones; the flame got a
  white-hot inner core from half heat; a loaded Fourth Shot glows larger on the
  rifle; the Silver Round leaves glitter.

## Open questions

None. The user approved all of the art on 2026-11-18.
