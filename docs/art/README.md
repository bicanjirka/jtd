# Art sheets

The living sheets of the board's art: every tower, enemy, mark and ground as the game paints it today, in the raised
view. What the look must keep (palette, light, values, saturation, motion) is in `docs/UI.md`, "The board's look".
The sheets under `docs/features/shipped/` are the record of what each feature shipped and are never rendered again.

Every tower on a sheet is painted by the game: `td.ui.TowerFoundations` builds it and `td.ui.TowerSprites` paints it.
The sheets hold no tower drawing of their own, so a sheet cannot show a tower the game would draw differently.

| Sheet | Shows |
|---|---|
| `board.png` | All seven towers at Tiers 0 to III and every identity look on Iron Crossing, over the game's own frame, at 0.5x, 1x and 2.5x |
| `catalogue.png` | Every tower and identity at each tier: 3.5x, 1x and 0.5x, with its part count |
| `heads.png` | Moving parts: idle, firing, reloading, every cue; then the tier unlock, a placed tower settling and idle life, frame by frame |
| `shots.png` | Shots and casts in flight |
| `marks.png` | Enemy marks, on the game's own enemies |
| `grounds.png` | The Obelisk's field patches and Sundial, the payload grounds, the flare, Ice Wake |
| `icons.png` | Toolbar icons, the upgrade arrow, the Lit mark, the panel's tier thumbnails, the tier colours |
| `theme.png` | What the game itself paints in the same look: every enemy look, every ground zone, the towers at Tier I |

Motion is judged from a recording, not a sheet: `PreviewBoard`'s `record` (run-jtd `SKILL.md`).

## Rendering them again

From the repository root, after `mvn -q compile` and with `target/runtime-classpath.txt` in place (run-jtd's
`SKILL.md`):

```bash
CP="target/classes;.claude/skills/run-jtd;$(cat target/runtime-classpath.txt)"
javac -cp "$CP" -d .claude/skills/run-jtd .claude/skills/run-jtd/td/ui/TowerArtGame.java .claude/skills/run-jtd/td/ui/LookChecks.java .claude/skills/run-jtd/TowerArt*.java
java -Djava.awt.headless=true -cp "$CP" TowerArtSheets docs/art            # every sheet
java -Djava.awt.headless=true -cp "$CP" TowerArtSheets docs/art heads shots # some of them
java -Djava.awt.headless=true -cp "$CP" TowerArtSheets docs/art looks       # the look checks only
```

Every run first prints the look checks (`docs/UI.md` section 8: readability under each level's light, the saturation band, sibling distance). Each sheet written prints the share of its pixels that changed from the one it replaced and writes a diff image
(changed pixels bright over the dimmed new sheet) to `target/art-diff/`. Every sheet that changed must be one the
change explains; read the diff images, not `git diff --stat`.
