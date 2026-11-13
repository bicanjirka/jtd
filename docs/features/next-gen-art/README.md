# Next-gen art sheets

The art for `FEATURE-next-gen.md`, drawn on 2026-11-13 in the raised view. The sheets are what to build; the decisions behind them are in
`../FEATURE-next-gen-tower-looks.md` ("Art direction", "Palette") and in each tower's Look section.

| Sheet | Shows |
|---|---|
| `board.png` | All seven towers at Tiers 0 to III and every identity look on Iron Crossing, over the game's own frame, at 0.5x, 1x and 2.5x |
| `catalogue.png` | Every tower and identity at each tier: 3.5x, 1x and 0.5x, with its part count |
| `directions.png` | The two choices as decided: raised view B against plan view A, growing footprint B against constant B2 |
| `heads.png` | Moving parts: idle, firing, reloading, every buff cue, and the tier unlock frame by frame |
| `shots.png` | Shots and casts in flight |
| `marks.png` | Enemy marks, on the game's own enemies |
| `grounds.png` | The Obelisk's field patches and Sundial, today's payload grounds, the flare, Ice Wake |
| `icons.png` | Toolbar icons, the upgrade arrow, the Lit mark, the panel's tier thumbnails, the tier colours |
| `theme.png` | What the game itself paints in the same look: every enemy look, every ground zone, today's towers |

## Rendering them again

The sheets come from throwaway lab code beside `PreviewBoard`, not from the game. From the repository root, after
`mvn -q compile` and with `target/runtime-classpath.txt` in place (run-jtd's `SKILL.md`):

```bash
CP="target/classes;.claude/skills/run-jtd;$(cat target/runtime-classpath.txt)"
javac -cp "$CP" -d .claude/skills/run-jtd .claude/skills/run-jtd/TowerArt*.java
java -Djava.awt.headless=true -cp "$CP" TowerArtSheets docs/features/next-gen-art            # every sheet
java -Djava.awt.headless=true -cp "$CP" TowerArtSheets docs/features/next-gen-art heads shots # some of them
java -Djava.awt.headless=true -cp "$CP" TowerArtSheets docs/features/next-gen-art bench       # the paint timing
```

The board, marks, grounds and theme sheets load Iron Crossing in a headless `GameEngine` and paint it through
`BoardRenderer.buildFrame` and `Java2DFrameRenderer.paint`, then draw the new art on top with plain Java2D shapes.

## Where each look is defined

`TowerArt.java` holds every tower as a list of parts in cell units (the cell's centre at 0,0, a cell 1 wide, north
at -90 degrees). It is the measured source for the sheets: a radius, an offset or a colour that a doc does not state
is there.

| Look | In `TowerArt` | Notes |
|---|---|---|
| The shared plinth, footprint by tier | `plinth`, `GROW`, `STEP_H` | step 0.045 wide and 0.07 high; the top step paved in a dark tone of the tower's colour |
| Three tones of a colour | `Ramp.of` | shade 45% toward black, highlight 38% toward white |
| Plinth stone | `stone` | `#50535e` mixed 24% toward the tower's colour |
| Sniper and its rifle | `sniper`, `sniperHead` | cues: `long`, `silver`, `hollow`, `fourth`, `frenzy`, `headhunter`; `stacks` is Steady Aim |
| Burst | `thrower`, `stormcaller`, `hexer`, `stoneRing` | cue `full` is an ability waiting; `fire` is the flick, the spark, the bubble |
| Mortar, its barrel and shells | `mortar`, `mortarHead`, `shell` | cue `volley`; `shell` draws all seven kinds |
| Scorcher, its nozzle and stream | `scorcher`, `stream`, `tongue` | look `soulfire`; cues `stream`, `dragon`; stream looks `core`, `white`, `soul` |
| Aerie, the nest, a missile | `aerie`, `nest`, `missile` | looks `ruby`, `shade`; cue `release` |
| Obelisk and its rune | `obelisk`, `rune`, `runeColour` | looks `burden`, `gravity`, `umbra`; cue `full` is a full Toll inside |
| Beacon | `beacon` | `panes` are the Lit types' colours; cue `daylight` |
| Back-to-front order inside a foundation | `Build.around`, `Build.finish` | parts north of the centre before it, parts south of it last; one that straddles is cut in two |
| The raised view | `Dir.RAISED`, `paintPart` | a top lifted 0.42 of its height, the side swept below it |
| Upgrade arrow | `upgradeArrow` | |
| Tier colours | `TIERS` | |
| Marks, field patches, icons | `TowerArtSheets4` (`silence`, `toll`, `hex`, `crystal`, `shards`, `patch`, `icon`) | |
| Tracer, lobbed shots, bolts, curses, the Sundial | `TowerArtSheets3` (`tracer`, `lob`, `blast`, `lightning`, `curse`, `sundial`) | |

## How a part is painted

One part is one closed shape with a height and one colour in three tones (`TowerArt.paintPart`):

1. the shape in the shade tone, swept from the part's foot to its top;
2. the shape in the highlight tone at its top;
3. the same shape in the body tone, shifted 0.024 of a cell down and to the right and clipped to step 2, which leaves
   a highlight rim along the top-left edge.

In the raised view the top of a part of height `h` sits 0.42 x `h` of a cell straight up the screen from its foot,
and the shade tone fills the wall between them. Flat details (a deck, a pane, a rivet), glows and thin lines have no
height and no tones. The plan view (`Dir.PLAN`, not taken) shifts the top 0.075 x `h` up and to the left instead.

The game's own pieces follow the same light. An enemy body is a stack of parts too (`td.ui.EnemySprites`, shaped by
its `EnemyLooks.Form`), drawn once into a kept picture and placed pixel for pixel; today's towers and a mine use the
three tones through `Java2DFrameRenderer.paintStanding`. The next-gen foundations should be kept the same way as the
enemy pictures are.

## What the lab measured

200 Tier III towers on a 576 x 416 board, 26 parts a tower on average, painted part by part: 39 ms a frame raised
(32 ms in plan view). The same towers from one kept picture each: 0.7 ms. A foundation never changes between tier
unlocks, so its picture is rendered once from the parts (and again when the zoom changes) and the frame carries one
command for it.
