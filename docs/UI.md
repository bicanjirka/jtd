# jTD UI guide

This guide is for the moment you choose how something looks: above all, a colour for a new tower, enemy, effect,
level or control. It says what colours mean in this game, what is already taken, and how to pick. Components and layout
join it when they need a written choice.

The code holds the live values: `Hud`, `Java2DFrameRenderer.colorFor`, `EnemyLooks` and each level's `PathColor` and
`BoardGround`. A table here is for deciding. When a table and the code disagree on a value, the code is right and the
table gets fixed.

## 1. The idea

**The interface is the instrument; the level and the things on it are the colour.** Chrome is quiet and belongs to no
level, so it sits beside all of them. Almost every colour on screen therefore means a particular tower, enemy, effect or
level.

## 2. Three kinds of colour

Every colour is exactly one of these. Decide which before choosing a value.

| Kind | Means | Examples | Rule |
|---|---|---|---|
| **Chrome** | Nothing: it structures the screen | Text, borders, fills, popups | Only the ramp in section 4. Never add a hue, never a new grey by hand |
| **Signal** | One fixed fact, everywhere | A problem, a damage type, an upgrade slot | Spent rarely. One colour, one meaning, on every surface |
| **Identity** | One particular thing | A tower, an enemy, an effect, a level's lanes | The thing wears it everywhere it appears. It says who, never how good |

## 3. Rules

1. **Loudness follows information density.** The level select may be loud, the HUD stays medium, and the board is the
   quietest ground of all, because the board is where the player reads the game. A new decoration goes on the loudest
   surface it can.
2. **A problem has a colour; good news needs none.** Can't afford, an unmet gate and a value getting worse are rose. A
   met gate or an improvement is plain text with its ✓ or →.
3. **Colour is never the only signal.** Shape, glyph or position carries the meaning first. Several sets of colours here
   merge for colour-blind players (section 6), so a design that needs colour alone is wrong, not unlucky.
4. **No pure primaries.** A new colour never pairs a channel at 0 with a channel at 255. This is what reads as a 1990s web
   page. Section 5 lists the ones that remain.
5. **Readable first.** See section 7's floors. A colour that fails as text keeps its value for borders and shapes, and
   its text uses a lighter tint of the same hue.
6. **Reuse before adding.** A new thing related to an existing one takes that one's colour: a Sniper's shot is the
   Sniper's. A new colour is the last resort.
7. **A level's colours come from the level.** Its lanes and its floor, never a colour chosen for the menu.

## 4. Chrome and signals

Contrast is against the background. Each grey reads as well as the green-tinted one it replaces did on black, except main
text, which had more contrast than it needs (19.4:1).

| Chrome | Value | Contrast | Used for |
|---|---|---|---|
| Background | `#0D0E10` | | Window, panels, cards |
| Raised | `#171717` | 1.1:1 | Tooltips, popups |
| Hover fill | `#29292A` | 1.3:1 | A control under the pointer |
| Selected fill | `#39393A` | 1.7:1 | A chosen toggle |
| Pressed fill | `#424243` | 1.9:1 | A control held down |
| Border, disabled | `#3E3E3F` | 1.8:1 | |
| Border | `#676769` | 3.4:1 | Every panel and control outline |
| Chip border | `#8D8D90` | 5.8:1 | Key chips |
| Border, hover | `#9E9EA1` | 7.2:1 | |
| Text, disabled | `#717173` | 4.0:1 | Exempt from the text floors |
| Text, gloss | `#8D8D90` | 5.8:1 | Fine print beside a value |
| Text, dim | `#A9A9AC` | 8.3:1 | Captions, stat names |
| Text, prose | `#ACACAF` | 8.5:1 | Sentences |
| Text | `#EAEAEF` | 16.1:1 | Everything read first, values included |

A painter's own states (the upgrade tree's nodes, a text field's fill, the Info pane's bars) sit between these steps and stay
private to that painter. They are greys too; a new control uses the ramp.

| Signal | Value | Contrast | Means |
|---|---|---|---|
| Problem | `#FF6B6B` | 7.0:1 | Can't pay, or something is wrong. Unmet gates use its neighbour `#FF7878` |
| Physical damage | `#E69646` | 8.1:1 | Physical, on every surface |
| Magic damage | `#5ABEFF` | 9.4:1 | Magic, on every surface |
| Core slot | `#7FD4FF` | 11.7:1 | Upgrades in the Core slot |
| Base slot | `#E1E1E6` | 14.8:1 | Upgrades in the Base slot. Deliberately close to text: the foundation |
| Head slot | `#FFC83C` | 12.5:1 | Upgrades in the Head slot |
| Special slot | `#C864FF` | 6.2:1 | Upgrades in the Special slot. A border and shape colour, not for small text |

## 5. Identities

**Towers.** "Nearest" is the closest other tower by CIEDE2000 distance (section 7).

| Tower | Value | On the lightest floor | Nearest | Note |
|---|---|---|---|---|
| Sniper | `#00FF00` | 14.3:1 | Sonar, 23.4 | Pure primary. See the warning below |
| Burst | `#FF0000` | 4.9:1 | Cinder, 9.1 | Pure primary, and the closest pair of all |
| Stormcaller | `#5096FF` | 6.7:1 | Seeker, 10.2 | |
| Hexer | `#AA50E6` | 4.7:1 | Stormcaller, 28.5 | |
| Sonar | `#FFFF00` | 18.2:1 | Pulse, 16.6 | Pure primary |
| Pulse | `#FFC800` | 12.6:1 | Sonar, 16.6 | Pure primary |
| Aura | `#FFFFFF` | 19.6:1 | Seeker, 29.9 | |
| Mortar | `#8B5A2B` | 3.4:1 | Cinder, 25.1 | Barely above the 3:1 floor in section 7 |
| Seeker | `#50B4FF` | 8.7:1 | Stormcaller, 10.2 | |
| Cinder | `#FF5A1E` | 6.3:1 | Burst, 9.1 | |

**Softening a tower is not free.** The obvious Sniper green, `#48D860`, sits 3.1 from the healing zone `#5ADC78`, while
today's pure green is 12.6 from it. Softening the primaries needs each new value checked against everything else in its
hue (section 6).

**Levels.** Each level has one card colour, from its own lanes (`withCardColor`, the first lane's by default), and its floor.
Text on the card uses the text tint, which `TextTint` computes: the lane colour lightened only as far as 7:1.
In play the tint is the level's voice, in four small places and no others: the game title, the panel titles, the selected
speed button's border and the XP bar. Every panel stays Graphite.

| Level | Card lane | Lane colour | Text tint | Floor |
|---|---|---|---|---|
| The Amber Hive | Honey | `#F0AF28` | same (10.0:1) | `#100B04` |
| The Storm Ridge | Thunder | `#5F73EB` | `#8796F0` (7.0:1) | `#06090E` |
| The Last Hour | Glass | `#8CD7E1` | same (11.9:1) | `#070C0E` |
| Iron Crossing | Rush | `#EB5F2D` | `#EF7C53` (7.1:1) | `#08090C` |
| The Rift | Violet | `#A05AE6` | `#B985EC` (7.0:1) | `#07040C` |
| The Labyrinth | Stone | `#C8AA78` | same (8.7:1) | `#0D0B06` |

## 6. What each hue already means

Look here first when a new thing needs an identity. Hue sectors are OKLCH. "Crowded" means a new colour there will be
hard to tell from its neighbours, and "open" means it has room.

| Hue | Reads as | Already means | Room |
|---|---|---|---|
| 0–30 | Red, rose | **Problem**, lives, Burst, mines, marked | Crowded, and reserved: red says something is wrong |
| 30–60 | Orange | Cinder, burn, ash, damage over time | Busy |
| 60–90 | Amber, gold, brown | Pulse, Mortar, **Head slot**, **physical damage**, score, ranks, many enemies | **The most crowded** |
| 90–120 | Yellow | Sonar, daze, shields | Busy |
| 120–150 | Green, lime | Sniper, healing, regeneration, poison, blight | Busy, and split between tower and heal |
| 150–180 | Mint | Mender, haste | **Open** |
| 180–210 | Teal | A few enemies, The Last Hour's glass lane | **Open: the most room in the game** |
| 210–240 | Sky | **Core slot**, frost, freeze | Some room, but it reads as cold |
| 240–270 | Blue | Stormcaller, Seeker, **magic damage**, control | Crowded, and reads as magic |
| 270–300 | Periwinkle | Chill, silence, several enemies, The Storm Ridge's lanes | Some room, no tower yet |
| 300–330 | Violet | Hexer, **Special slot**, doom, weakness, The Rift's lanes | Busy |
| 330–360 | Pink | Disruption, hexes, vulnerable | Some room, but it reads as hostile |

**Known compromises:** thunder and violet merge for colour-blind players (distance 0.9 to 2.3), so level cards rely on
their name and lane shape. Honey and stone share a hue and differ by strength (13.4). Most towers have a neighbour under
10 for some colour-blind player, which is why towers differ by shape first.

## 7. Choosing a colour

1. **Is it chrome?** Use the ramp. Stop.
2. **Is it a problem?** Use Problem. Stop.
3. **Is it good news?** Use plain text with a glyph. Stop.
4. **Does it belong to something that has a colour?** Use that thing's colour. Stop.
5. **It needs an identity of its own.** Pick from the open hues in section 6, then check it:

| Check | Floor | Why |
|---|---|---|
| Small text | at least 7:1 on the background | What today's values read at |
| Fine print beside a value | at least 5.5:1 | Secondary, and always beside something brighter |
| A shape on the board | at least 3:1 on the lightest floor, `#100B04` | It has to read in play. The darkest floors only help |
| Distance from its nearest sibling | at least 10 (CIEDE2000) | Under that, two towers or two effects look like one at a glance |
| Primaries | no channel at 0 paired with one at 255 | Rule 4 |

Then look at it in the game, beside the things it will appear with: `PreviewBoard` for the board, one `run-jtd` screenshot
for a panel. Numbers rule colours out; they don't choose one.
