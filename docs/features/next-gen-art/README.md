# Next-gen art sheets

The art for `FEATURE-next-gen.md`, in the raised view. The sheets are what to build; the decisions behind them are in
`../FEATURE-next-gen-tower-looks.md` ("Art direction", "Palette") and in each tower's Look section.

Every tower on a sheet is painted by the game: `td.ui.TowerFoundations` builds it and `td.ui.TowerSprites` paints it.
The sheets hold no tower drawing of their own, so a sheet cannot show a tower the game would draw differently.

| Sheet | Shows |
|---|---|
| `board.png` | All seven towers at Tiers 0 to III and every identity look on Iron Crossing, over the game's own frame, at 0.5x, 1x and 2.5x |
| `catalogue.png` | Every tower and identity at each tier: 3.5x, 1x and 0.5x, with its part count |
| `directions.png` | Frozen, not rendered again: the two choices as decided, raised view against plan view, growing footprint against constant |
| `heads.png` | Moving parts: idle, firing, reloading, every cue; then the tier unlock, a placed tower settling and idle life, frame by frame |
| `shots.png` | Shots and casts in flight |
| `marks.png` | Enemy marks, on the game's own enemies |
| `grounds.png` | The Obelisk's field patches and Sundial, today's payload grounds, the flare, Ice Wake |
| `icons.png` | Toolbar icons, the upgrade arrow, the Lit mark, the panel's tier thumbnails, the tier colours |
| `theme.png` | What the game itself paints in the same look: every enemy look, every ground zone, the towers at Tier I |

## Rendering them again

From the repository root, after `mvn -q compile` and with `target/runtime-classpath.txt` in place (run-jtd's
`SKILL.md`):

```bash
CP="target/classes;.claude/skills/run-jtd;$(cat target/runtime-classpath.txt)"
javac -cp "$CP" -d .claude/skills/run-jtd .claude/skills/run-jtd/td/ui/TowerArtGame.java .claude/skills/run-jtd/TowerArt*.java
java -Djava.awt.headless=true -cp "$CP" TowerArtSheets docs/features/next-gen-art            # every sheet
java -Djava.awt.headless=true -cp "$CP" TowerArtSheets docs/features/next-gen-art heads shots # some of them
java -Djava.awt.headless=true -cp "$CP" TowerArtSheets docs/features/next-gen-art bench       # the paint timing
```

A change to a tower's look is a change to its builder in `TowerFoundations`; render the sheets after it and read
`git diff --stat` on the PNGs: every sheet that changed must be one the change explains.

The lab (`TowerArt*.java`) lays the sheets out and still draws what the game does not have yet: shots and casts in
flight, the stream, the nest's missiles, marks, field patches. `td/ui/TowerArtGame.java` is its one way into the
game's package-private tables. Once a shot or a mark is in the game, its sheet calls the game for it and the lab's
drawing is deleted, as the towers' were.

The board, marks, grounds and theme sheets load Iron Crossing in a headless `GameEngine` and paint it through
`BoardRenderer.buildFrame` and `Java2DFrameRenderer.paint`.

## Where each look is defined

`td.ui.TowerFoundations` holds every tower as a list of parts in cell units (the cell's centre at 0,0, a cell 1
wide, north at -90 degrees). A look is a `TowerLook` row there: a builder from a tier and a `TowerPose` to a build.
A radius, an offset or a colour that a doc does not state is in the builder.

| Look | In `TowerFoundations` | Pose it reads |
|---|---|---|
| The shared plinth, footprint by tier | `FOOTPRINT`, `PLINTH_STEP`, `STEP_HEIGHT`, `footprint(tier)` | none |
| Sniper and its rifle | `sniper` (`Row.aiming`) | `LONG_BARREL`, `SILVER_ROUND`, `HOLLOW_POINT`, `FOURTH_SHOT`, `FRENZY`, `HEADHUNTER`, `KEEN_FOCUS`; `stacks` is Steady Aim, the scope's focus; `beat` is Frenzy's shake; recoil, flash |
| Burst | `thrower` (the onager), `stormcaller`, `hexer` | `CHARGED` is an ability waiting; `fired()` and `reloading` drive the arm, the spark, the bubble |
| Mortar, its barrel and shells | `siegeMortar` (`Row.posed`, no head), `shell(LoadedShell, r)` | `VOLLEY`, `LONG_BARREL`; `load` is what this reload carries up, `stock` the rack; recoil, reload |
| Scorcher and its nozzle | `scorcher` (`CINDER`, `SOULFIRE`; a dragon's head or a soul cage for its head) | `STREAMING`, `DRAGONS_BREATH`, `LONG_BARREL`; reload cools the nozzle |
| Aerie | `aerie`, `ruby`, `shade`, `white` (`SEEKER`, `RUBY`, `SHADE`, `WHITE`) | `RELEASE_READY` whitens the tip; `Foundation.nest` is the top the missiles circle |
| Obelisk, a menhir, and its runes | `obelisk` (`PULSE`, `BURDEN`, `GRAVITY`, `UMBRA`) | `CHARGED` is a full Toll inside; `reloading` is the knell's throw |
| Beacon | `beacon` (`AURA`) | `panes` are the Lit types' looks; `DAYLIGHT` |
| A living light on a still tower | `Build.light(Life, ...)` | painted live by `TowerSprites`, never part of a kept picture |
| Back-to-front order inside a foundation | `Foundation.parts`, `head`, `front` | parts south of the head are painted over it |
| The raised view, the three paint steps | `TowerSprites.paintParts` | |
| Colours | `td.ui.TowerLooks` | |
| The flame, a nest's missile, the upgrade arrow | lab: `TowerArt` (`stream`, `tongue`, `nest`, `missile`, `upgradeArrow`); the flame through the game's `FlameArt` | |
| Marks, field patches, icons | lab: `TowerArtSheets4` (`silence`, `toll`, `hex`, `crystal`, `shards`, `patch`, `icon`) | |
| Tracer, lobbed shots, bolts, curses, the Sundial | lab: `TowerArtSheets3` (`tracer`, `lob`, `blast`, `lightning`, `curse`, `sundial`) | |

## How a part is painted

One part is one closed shape with a height and one colour in three tones (`TowerSprites.paintParts`):

1. the shape in the shade tone, swept from the part's foot to its top;
2. the shape in the highlight tone at its top;
3. the same shape in the body tone, shifted 0.024 of a cell down and to the right and clipped to step 2, which leaves
   a highlight rim along the top-left edge.

In the raised view the top of a part of height `h` sits 0.42 x `h` of a cell straight up the screen from its foot,
and the shade tone fills the wall between them. Flat details (a deck, a pane, a rivet), glows and thin lines have no
height and no tones.

The game's own pieces follow the same light. An enemy body is a stack of parts too (`td.ui.EnemySprites`, shaped by
its `EnemyLooks.Form`), drawn once into a kept picture and placed pixel for pixel; a mine uses the
three tones through `Java2DFrameRenderer.paintStanding`. A tower's foundation, its head at each heading step and its
front parts are kept as pictures by `td.ui.TowerSprites`, as the enemy bodies are.

## What the lab measured

200 Tier III towers on a 576 x 416 board, 26 parts a tower on average, painted part by part: 39 ms a frame. The
same towers through the game's painter, from kept pictures (`bench`): 1.5 ms a frame still, 2.9 ms with their idle
lights. A picture is rendered once for a look, tier, pose and zoom, and the frame carries one command for the tower.

Before the lab's own tower builders were deleted, 1284 builds (12 looks, 4 tiers, aims, cues, shells, panes, runes)
were compared part for part, lab against game. All matched but 8 Scorcher builds partway through a reload, where the
game snaps the reload to a quarter before mixing the nozzle tip's colour.
