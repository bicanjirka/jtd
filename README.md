# jTD: a Swing tower defense game

A desktop tower-defense game written in plain Java and Swing. Enemies walk a fixed path
across a grid board; you spend credits placing towers on the buildable cells beside it and
try to kill everything before it laps the path and costs you a life.

All of the artwork is vector: every enemy, tower, beam and path marker is a
`java.awt.Shape` built and painted in code. There are no image assets in the project.

![A level in play](docs/screenshots/game.png)

## Why it exists

jTD began as a hobby project in my first year at university, when I was learning Java. Today it
is how I learn agentic coding with Claude Code, in a way that is fun and rewarding.

## Tech stack

- **Java 25**, Swing and Java2D, with no game framework
- **Maven 3.9+**, shaded into one runnable jar
- **JUnit 5.12** and **AssertJ 3.27**, with hand-written fakes and no mocking library
- **SLF4J 2.0** with **Logback 1.5**
- **Spotless** (formatting) and **Checkstyle 14.1** (style and package boundaries), both run by
  `mvn verify`; **PIT** for mutation testing, run on demand

## Build, run and test

Needs JDK 25 and Maven 3.9+.

```bash
mvn package               # build target/jTD.jar
java -jar target/jTD.jar  # run the game
mvn verify                # formatting, Checkstyle and the whole test suite

# run a single test
mvn test -Dtest=GameEngineTest#placingATowerOnABuildableCellChargesCreditsAndOccupiesTheCell
```

The jar is shaded, so it bundles its SLF4J/Logback dependencies and needs no extra classpath.

## Design notes

- **The engine is headless.** `GameEngine` and the domain packages own all game state and input
  semantics and never touch Swing. The game, the tests and the balance tools drive the same engine;
  `GameHost` is its only channel to the UI ([ADR 0002](docs/adr/0002-headless-engine.md)).
- **Two threads, and only immutable snapshots cross between them.** The `game-loop` thread runs the
  simulation and builds each frame. The Swing thread only paints the finished frame. Related fields
  cross as one immutable snapshot behind one `volatile`, and a click that changes game state is a
  request the next tick picks up, never a write into engine state
  ([0007](docs/adr/0007-owner-publishes-snapshots.md), [0019](docs/adr/0019-edt-commands-are-requests.md)).
- **Frames are described, then painted.** `BoardRenderer` turns a tick into AWT-free draw records,
  and `Java2DFrameRenderer` alone turns them into pixels. The same frame can also be printed as
  text for debug logs ([0003](docs/adr/0003-frames-are-described-then-painted.md)).
- **Runs reproduce.** A tick always runs in the same order (enemies, projectiles, zones, towers),
  randomness comes from an injected seeded source, and whatever the simulation iterates keeps a
  fixed order. A save between waves holds enough to replay the next wave tick for tick
  ([0021](docs/adr/0021-a-wave-starts-from-its-save.md)).
- **Balance is measured, not guessed.** `td.balance.BalanceReport` plays every level headlessly
  with simulated players over many seeds and reports each margin with its 95% interval
  ([0010](docs/adr/0010-balance-is-measured-by-players.md)).
- **A performance budget with a harness that checks it.** `td.PerformanceHarness` fills the level
  that has the most buildable cells with towers. It exits with an error when the 99th-percentile tick
  or frame build takes longer than 1 ms, or when a frame build allocates more than 512 KB.
- **Decisions are written down.** [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) is the overview,
  [`docs/adr/`](docs/adr/README.md) holds the decision records, and Checkstyle enforces the rules
  that can be checked mechanically.

## How it is built

[Claude Code](https://claude.com/claude-code) writes the code and commits every change, so nearly
every commit is co-authored with it. I steer it and review its design decisions, aiming for clean
code and a clean architecture. The `CLAUDE.md` files hold the constraints it works under.

## Licence

[Apache License 2.0](LICENSE).

## Playing

The game opens on a level-select screen. Pick a level to start it; each level brings its own
board size, path, wave list, starting credits and starting lives.

Buy a tower from the toolbar (or with its key, `q`–`u`), then click a buildable
cell to place it; hovering a toolbar button opens a tooltip of the tower, and the side panel shows
it in full while you place it. The click that builds a tower selects it, as does clicking a placed
tower later: the side panel shows its stats and its own upgrade
tree — four tiers, drawn whole as rows climbing from the bottom.
Start each wave yourself when you are ready — waves do not auto-advance. A level may pay bonus credits, scored
alike, for each round cleared with no enemy leaked: the Waves panel marks a round's bounty and bonus (`24+5`), and the
cash icon flashes when the bonus pays. Hover that mark, or a round's skull in the Wave popup, for what each one means.

### Controls

Every button shows its key on a chip in its top-left corner.

| Key                             | Action                                                                                               |
|---------------------------------|------------------------------------------------------------------------------------------------------|
| `q` `w` `e` `r` `t` `y` `u`     | Select tower 1–7 for placement                                                                       |
| `Esc`                           | Cancel tower placement; cancel a question                                                            |
| `1`–`6`                         | Buy an upgrade in the selected tower's highest open tier, left to right; a cluster takes one key, each fork side its own |
| `Space` or `P`                  | Pause; while paused or between waves, play on at the last speed and send the next wave               |
| `d` `f` `g`                     | Normal, fast, super fast speed; each also sends the next wave if it is waiting                       |
| `Delete`                        | Sell the selected tower                                                                              |
| `m`                             | Back to the level-select menu (asks to confirm mid-level)                                            |
| `z`                             | Restart the last wave sent, back to how it was sent (asks to confirm)                                |
| `Enter`                         | Confirm a question                                                                                   |
| `1`–`9` on the menu             | Start that level                                                                                     |
| `Ctrl`+`Shift`+`D`              | Open or close the dev panel (playtesting controls; hold `Alt` to see their keys)                     |

Mouse: move to preview placement, click to place or to select a placed tower. In the Upgrades panel,
hovering a node shows what it does and what a click would do; a click buys it with every node it
needs first, in one payment or not at all, and a right-click on an owned node downgrades it (after a
confirmation, unless all of it refunds in full) with every node that depends on it. Between waves, a click on a
cluster's other member switches to it. Click a moving enemy, invisible ones too (when not placing a
tower), to inspect it: the side panel shows its live health, stats, effects and traits, updating even while paused,
and keeps a "Killed" or "Leaked" line after it's gone until your next click. Hovering an enemy in
the wave preview opens a tooltip of the stats that enemy type spawns with.

The dev panel ships in the jar, hidden behind `Ctrl`+`Shift`+`D` rather than a build flag, to
make manual playtesting cheap. It sets credits and lives, keeps them topped up, spawns any wave
script on any path with a live preview (enemies spawned between waves hold the next wave back until
they are gone), skips, jumps to or restarts waves, steps one tick, shows
the live speed in ticks per second and nudges it by 1 or 5 (0 pauses), kills
or clears the board, saves the game between waves and loads it back (one save per level, in `saves/`), grants XP to or resets the selected tower, lets upgrades ignore their gates,
and draws the cell grid with the path's cells shaded and the pointed-at cell's coordinates. Every
control has an `Alt` shortcut.

`td.balance.BalanceReport` checks balance headlessly. It plays every built-in level with the level's real
credits and lives, over many seeds, with a naive player (random towers on random cells), a competent one
(it reads the coming rounds and buys what it estimates saves the most enemy health per credit) and that
competent player on random cells and without upgrades. Per level it reports whether the level is too easy
or too hard, whether positioning and upgrades matter, where the pressure comes round by round, where the
credits go, which towers carry the defence and what leaks, which effects, enemy abilities and upgrade
nodes no play ever triggers, every number with its 95% interval. The
**margin** is the highest enemy health, as a percentage of the authored one, a player still wins at. A lab
measures each tower on every cell, against every enemy kind at every rank a level sends, and with each
upgrade line, and what it does for a tower beside it, before any play.

```bash
mvn -q compile exec:java -Dexec.mainClass=td.balance.BalanceReport -Dexec.args="--save before"
# change a number, then:
mvn -q compile exec:java -Dexec.mainClass=td.balance.BalanceReport -Dexec.args="--compare before"
```

`--compare` plays the same seeds again and lists what moved beyond the seeds' noise, then plays the saved
competent actions back unchanged, so what the change does shows apart from how the player adapts to it. `--seeds N` (20 by
default) and `--levels 0,2` narrow a run; `--strong` adds a player that rehearses each round before
playing it, `--variety` measures each tower alone and each tower banned, `--sweep lives|bounty|clear|pause|spacing`
shows how the competent player's margin moves with one of the level's knobs, and `--html` writes
`target/balance/report.html` with charts and board heat maps. A run takes one to two minutes; the extras
add several each.

Two older harnesses remain. `td.PurchaseHarness` compares purchases: on each level asked for, with an army
of one tower of every type, it reports the lives saved per 100 credits by another copy of a tower and by each
upgrade node, over many seeded runs (`mvn -q compile exec:java -Dexec.mainClass=td.PurchaseHarness
-Dexec.args="0,1,2 12 all 4"`: levels, seeds, a tower or all of them, and how many of each tower the base army has; a bigger
army prices "another copy" at what a real build pays). It also gives each line's fair price, the price at which
it saves as many lives per credit as another tower, and closes with what a node at each tier should cost as a
percent of its tower's list price. `td.BuildHarness` compares builds of one tower
instead: copies of it bought the same way among a fixed background of the others, for every node on its own
and for every last head node with a special, either at equal copies or at an equal budget with one upgraded
copy and plain ones for the rest (`-Dexec.args="2 40 cinder 4 2 1500 1"`: level, seeds, tower, copies,
background copies, budget, upgraded copies).


### Towers

A tower's info pane shows Range, then how it deals damage: a hit tower its Physical or Magic damage (/hit), Fire
rate (/s) and DPS (/target: damage times fire rate times the average crit, before the target's defences; for the Scorcher, the burn
its waves keep going on one target), the Obelisk its damage a second, a placed tower its Wave DPS (what it really
dealt this wave, over the span from its first damage to its last, once that span reaches a second); then every other stat off its default
(Crit damage whenever it can crit, Effect duration whenever it applies an effect), and what a Beacon gives.
A changed value has an arrow for which way it moved, and is red only when that is worse; hovering a row says what
the stat does and what made it. An upgrade's hover shows each change and what the row it moves would become
(`Fire rate +25% 1.3 → 1.6 /s`).

| Tower  | Price | Behaviour                                                                                                                                           |
|--------|-------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| Sniper | 15    | Single target, hits whichever enemy in range is furthest along the path when its tracer arrives, a moment after it fires; each shot adds a stack of Steady Aim, whichever enemy it hits, so the longer it keeps firing, the faster it fires and the likelier it crits; a stack fades 3 s after its last shot, one at a time. Its tree has no fork: every 4th shot sure to crit, loaded with a Silver Round or a Hollow Point; a crit that starts a Frenzy, ricochets or sunders; an Executioner for the wounded or a Headhunter for elites and bosses |
| Burst  | 15    | Throws a stone at a random target in range; it lands where the target stood and bursts, full physical damage in its core and half at its edge. Forks into the Stormcaller (Static and Sparks: Arc, Thunderclap or Thunderstrike) or the Hexer (Soulbrand and Souls: Doom, Blight, Inversion) |
| Mortar | 20    | Lobs a slow, unguided shell at whichever enemy in range is furthest along the path, never one within 1.5 cells of it; it lands where the target stood, full physical damage in its core and half at its edge, and cracks the plating of everything it reaches. It fires from a magazine of ten shells, drawn by lot: Normal and, now and then, a Heavy that hits twice as hard. Its tree adds a Payload (Napalm, Tar or Cryo shells that leave burning, tarred or frozen ground), Flares that reveal, a Heavy that Dazes or makes Bleed, a Volley (a Barrage at one target or Carpet Bombing back along the path), and Fusion (two Heavies in a row become a Nuke that leaves Fallout) or Rich Mix (never the same kind twice) |
| Scorcher | 28    | Turns slowly toward the nearest visible enemy and, once one is in the cone, fires a flame wave that burns everything it reaches, invisible enemies included. A burn never crits but an ignition can, which starts the pool stronger. Attuned, each wave on an enemy it is already burning raises that burn. Forks into hot, short White Flame with Soulfire, or the wide Wide Nozzle that leaves burning ground; a fuel line makes burning enemies heal less, take more and scorch faster |
| Hive   | 30    | Fires a slow homing missile at the fastest enemy in range, which it keeps through invisibility and re-aims at each tick; deals magic damage and freezes whichever mob it actually hits. Attuned, it banks missiles in a nest, between waves too, and launches them as a salvo |
| Obelisk | 25    | Support, a kill box. Short range; a field that drags everything inside 15% and deals a little magic damage to it every tick, ghosts included and with no visible enemy needed, the damage a quarter less for every other field overlapping it. Attuned, an enemy that stays inside builds Toll, which makes every effect the field holds on it stronger. One head holds enemies longer (Gravity: a deeper drag, Dazes, a wider field, a well or Stasis), the other denies them (Ward: reveal, silence, worthless heals, True Sight or Purge); an extra line makes them take more damage and builds Toll faster. A special Condemns the strongest enemy inside, bosses first, for every tower to turn on; drains spirit; or links two Obelisks with a Ley Line. Reveals nothing until Ward is bought |
| Beacon | 20    | Passive: gives the towers in its range Damage and Range +20% and Jam taken x0.5; it never buffs another Beacon, and several stack. Attuned, the towers it buffs earn more XP for each other tower type in its range (Kinship). One head strengthens the buff, then adds Fire rate and Crit damage; the other widens its reach, then raises Buff duration and Effect duration. An extra line mentors XP, and a special makes it wither enemies, buff only its best tower, or rally the towers when an enemy reaches a spot on the path |

The shop and the board call the towers Sniper, Burst, Mortar, Scorcher, Hive, Obelisk and Beacon
(keys `q` `w` `e` `r` `t` `y` `u`, in that order). A Burst that forks is called a Stormcaller or a Hexer.

An attack-speed bonus adds to the attacks a second, so +100% is twice as fast; bonuses add up, and a Jamming enemy
takes 30% away and shrinks range by 0.6 cells. The rate need not be a whole number of ticks: a tower remembers
what is left over, so +10% on the Obelisk, which hits every tick, gives 22 hits in 20 ticks and a second hit in
the tick the fractions add up. No tower runs more than ten times faster than its
base, or slower than a quarter of it.

The price is the first copy's: each copy of a tower already on the board makes the next one cost
25% more than the one before it (compounding: the 10th copy costs about 7.5 times the first), and the
shop shows the current price. Selling refunds three quarters of what the tower
and its upgrades cost.

Every tower, the Aura included, has its own upgrade tree of four tiers, shown in place of the
wave preview once the tower is selected. A tier opens with XP (Tier I at 60, Tier II at 120, Tier III at 200), and
Tier I also asks a one-off unlock payment; every tower an enemy reached earns its bounty as XP when its walk ends,
killed or leaked (an Aura earns with the towers it buffs). Once a tier is open its upgrades cost only credits. In a
tier an upgrade stands alone, follows one it needs (in the same tier or the one below), or belongs to a cluster:
one framed set of which a tower keeps one active, switched for free between waves, which sells back what came
after the old one. Attune, in Tier 0, gives each tower its own mechanic. The Burst picks Stormcaller or Hexer at Tier
I's gate, which makes it a Stormcaller or a Hexer; picking the other side later works only between waves and sells
the old side's upgrades. A downgrade takes a node back with everything that needs it, refunds three quarters of what
they cost (selling afterwards pays what selling would have) and reopens the choices they made. Upgrade
prices are multiples of the tower's price. A tower stands on one plinth step for each tier it has open, and its
foundation grows wider and richer with each; when a tier opens, the new foundation spreads over the old with a ring
in the tier's colour. A green arrow in a tower's top-left corner says it can buy an upgrade or a tier's unlock now. The Upgrades panel's XP bar ticks where the next tier opens. The Beacon draws a ring on the
tower it mentors and (while it is selected) the path spot its Rally watches.

```mermaid
flowchart BT
    t0[Tier 0<br/>Attune, Range, the tower's own] -->|XP 60 and the unlock| t1[Tier I]
    t1 -->|XP 120| t2[Tier II]
    t2 -->|XP 200| t3[Tier III]
```

### Enemies

An enemy is made of four kinds of thing, and each belongs to exactly one:

- **Stats** are the numbers every enemy has, zero or not: Move speed (cells/s), Armor and Magic resist (a hit
  keeps `100 / (100 + armor)` of itself), Plating and Insulation (dmg every physical or magic hit loses; Flat
  penetration ignores them), Resilience (against crits; 100 is crit-immune, a negative value makes crits stronger), Agility
  (aimed shots miss; a negative value means no dodges and more crits), Physical, Magic and Damage over time taken (%), Spirit (what its regeneration restores and its shields hold),
  Regeneration (hp/s; at a negative value it crumbles, losing health on its own) and Tenacity (how long its debuffs last: `100 / (100 + t)` of their time at a positive
  value, `1 - t / 100` at a negative one; it cuts a pool's damage with its time). An immunity is a trait of its own: that effect never takes hold.
  The info pane lists the ones off their default, each with its unit and, where the number alone says little, what
  it does (`Armor 30 -23% dmg`); hovering a row explains it and, live, what made it. Nothing is drawn on the board
  for them. Under a selected enemy's health bar a strip shows what its shields hold, tinted where a shield takes only
  physical or only magic damage; hovering the Shield or Heal icon lists each source's share and how long it has.
- **Traits** are what an enemy has that most don't, always on: a resistance or immunity to an effect,
  regeneration, a stat that follows its state (faster or harder to hit when hurt) or adapts to how the
  level is played.
- **Abilities** are what it does at a moment: a trigger (a timer, a threshold, a hit, a dodge, its death)
  and an action (put an effect on itself or its allies, or spawn enemies).
- **Effects** are timed statuses on an enemy, from a tower or an ability. The main ones are drawn on the enemy; others show as a small dot under it.

From Elite every enemy carries some of each stat, small where it is off its identity; the exceptions are
plating and insulation (only the enemies built around them), agility on the heaviest (Juggernaut, Wardens, eggs) and resilience
on the Wardens, whose shield answers crits.

| Enemy      | Looks like    | Behaviour                                                                                                                                                                                                                                                                                                         |
|------------|---------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Walker     | circle        | Plain mob, no special ability, and no stats up to Veteran. From Elite, adaptive armor and a little of every stat; at Boss, +30 resilience |
| Knight     | square        | +25 armor and magic resist, rising with rank, plating from 2 to 10; immune to crits. From Veteran, +33 tenacity; from Elite, thick plating, a physical shield once below half health, and ward mines laid along its path; at Boss rank, calls three Squires every three seconds, who pay no bounty |
| Runner     | triangle      | Speeds up as it loses health; -20 agility, so crits land more often. From Veteran, once badly hurt, it throws down haste ground that lasts until the wave is over; from Elite it also sprints once below half health; at Boss rank, no haste ground, crit immune, calls three spawnlings once below half health and heals itself when left alone |
| Ghost      | sheet with eyes, floating | +20 armor. Turns invisible to single-target towers for ~10s the first time it's hit (~15s from Veteran; area damage still reaches it); at Elite and Boss rank, also permanently shrouds every other ally near it; at Boss rank it fades out for 3s of every 10 |
| Mender     | cross         | Deals no damage of its own; periodically restores health to every other ally near it; +20 spirit, rising to 60 at Boss. From Veteran it heals itself when left alone and, when it dies, heals and shields every ally near it; from Elite, adaptive armor; at Boss rank, the death blast is a heal alone. |
| Relay      | disc with four antennae, spinning | A signal post with heavy insulation (15 points, rising to 40 at Boss), so a burn or any other small magic hit does next to nothing to it. The first time it is hurt it calls a Walker a rank below its own (a Grunt at Grunt); from Veteran it calls another once below half health; from Elite, two and one, and at Boss two each time. Kill it in one blow, or fight what it calls. |
| Juggernaut | square with an inset plate | Slow, with plating that blunts a huge flat chunk of every physical hit; magic, Cracked and Sundered get past it. No magic resist below Boss, and +43 tenacity. From Veteran, when it dies it blinds the towers within 2 cells (4 from Elite) until the wave is over; from Elite, crit immune; at Boss rank, a large magic shield once below half health. |
| Mite       | circle        | Tiny and quick, with 10 agility rising 5 a rank to 25 at Elite and 35 at Boss; meant for a `swarm 12 mite`. at Boss rank it bursts into three broodlings when it dies. |
| Courier    | triangle      | Very fast and fragile, with a high bounty, and nimble (25 agility, 35 from Veteran, 50 at Boss) enough to dodge an aimed shot now and then. From Elite it sprints when first hit; at Boss rank, after every dodge too. |
| Salamander | lizard, wriggling as it walks | Immune to burn, so a Cinder-only defence does nothing; Soulfire still burns it. Regenerates 1 health a second (a trait), doubling at Veteran, Elite and Boss. From Veteran its hide is adaptive armor (up to 25, then 50 and 67); from Elite, a burst of healing once below 30% health. |
| Yeti       | shaggy ball with eyes, lumbering | Immune to freeze and chill; Dazed still stops it. +15 armor and magic resist. From Elite its fur blunts 2 points of every hit and it heals once below 40% health; at Boss rank, +50 resilience. |
| Magma      | jagged boulder with a glowing core | Slow, pulsing and immune to burn. +50 magic resist, rising with rank to 110 at Boss, 30 spirit, 10 agility, 70 resilience (85 from Elite, 95 at Boss), so crits rarely land. From Soldier, once below 65% health it gives every other enemy within 3 cells +30 magic resist for 15 s (+50 from Veteran). From Veteran, below 5% it turns almost proof against magic and crits for 10 s, and it leaves pools of molten rock behind it that heal whatever stands in them, until it is too hurt to keep up (wider and stronger with rank). |
| Golem      | hexagon       | Slow, swaying and heavily armored: +35 armor, +40 resilience, so crits rarely land. Once below 50% health it hardens into a shield that holds until broken (40 points, then 80, 160, 300 and 600 at Boss). From Veteran, when it dies it splits into two smaller Golemites with its defences and little health. From Elite, 50 armor, 60 resilience, +20 magic resist and 2 plating (65, 80, 35 and 4 at Boss). |
| Skirmisher | diamond       | Nimble: 40 agility, rising to 80 with rank, so it dodges some aimed shots; +20 resilience. From Soldier it sprints off for 1.5 s after every dodge; from Elite it gets harder to hit the more it is hurt; at Boss rank it also vanishes for a moment after every dodge. |
| Templar    | ivory heater shield with a violet diamond, swaying | +70 magic resist, rising with rank to 200 at Boss, and no armor, so magic barely touches it while physical damage handles it as it handles a Walker. From Veteran, once below half health, it raises a shield against magic; from Elite, a little armor, insulation and resilience, and a stronger shield. |
| Empty      | —             | Not a real enemy — a spacer that opens a timing gap inside a wave                                                                                                                                                                                                                                                 |
| The Warden | large spiked crest with a gold gem | A boss: armor and magic resist, and plating, all lower with each hatching; periodically calls two Elite Drones, hardens for every ally near it (Hivemind), shields itself and nearby allies, and shields itself again whenever it survives a crit, so its escort has to be cleared before it can be worn down. On death it leaves behind a stationary egg that hatches back into a weaker Warden if not destroyed in time — the fight only ends once an egg is defeated before it hatches. If any stage escapes, the level is lost, however many lives are left. The final round of The Amber Hive — a level-authored enemy. |
| Sandling   | small grain of sand, path-facing | Quick (1.5 cells/s) and a little nimble, and it crumbles: it loses health on its own (negative regeneration), faster with rank, so it lasts about as long as it takes to cross the board. Slow it and it never arrives; a heal it is given holds it together. Appears on The Last Hour — a level-authored enemy, like the Drone. |
| Timekeeper | brass clock face, spinning | Every seven seconds it turns the health of every other enemy within about 3 cells back to what it was two to four seconds before (Rewind), further with rank. A rewind is a heal: it gives back what Spirit makes it worth, nothing under Dead Zone, Inversion turns it into damage, and a freeze, a daze or a silence stops the cast. Appears on The Last Hour. |
| Glass Sentinel | glass crystal | Slow, with 8 plating and 8 insulation that blunt every small hit of either type (up to 22 at Boss) and -60 resilience, so crits shatter it: it asks for big hits, crits, Cracked or flat penetration. Appears on The Last Hour. |
| Sand Reaver | sand-coloured barbed arrowhead, path-facing | Speeds up as it's hurt, shrugs off a flat point of every hit, crit immune from Elite, shields itself once badly wounded, and falls apart into two Sandlings on death. Appears on The Last Hour. |
| The Keeper of Hours | glass hourglass with falling sand, swaying | A boss: every six seconds it turns its health back to what it was three seconds before (Rewind), every ten it calls a Veteran Timekeeper that turns it back further and pays no bounty, once below half it calls six Sandlings, and once nearly dead it turns the glass one last time, eight seconds back. Time runs slow around it: it jams the towers near it. Stop it casting (freeze, Daze, Silence), make heals worth nothing (Dead Zone), Invert it, or outpace it. If it escapes, the level is lost. The last round of The Last Hour. |
| Drone      | striped bee, buzzing | A Walker with Hivemind: +4 armor and magic resist for every other enemy within 1.5 cells, up to five, so a pack is far tougher than its members alone; area damage breaks it up. On the Walker's ladder. Appears on The Amber Hive — a level-authored enemy, like the Reaver. |
| Hive Guard | amber beetle, striding | A Walker with 2.8 times the health at half the speed, a point of plating and a stronger Hivemind (+9 a neighbour, up to four). On the Walker's ladder. Appears on The Amber Hive. |
| Brood Nurse | cream honeycomb cell with an amber cross | A Mender in the hive's colours, on the Mender's ladder; from Veteran it also wards every other ally near it with a shield that stops any damage. Appears on The Amber Hive. |
| Larva      | pale grub, wriggling | A soft, slow Walker that spins itself into a Cocoon where it stands after nine seconds, unless it is killed first. On the Walker's ladder. Appears on The Amber Hive. |
| Cocoon     | silk-wound ovoid | Stationary, armored and plated but soft to crits (-40 resilience); it hatches into a Hornet after six seconds unless it is broken open. Appears on The Amber Hive. |
| Hornet     | orange hornet with black bands, path-facing | Quick (2 cells/s), nimble and faster as it is hurt, with -20 resilience; from Veteran it sprints after every dodge. What a Cocoon hatches into, and sent on its own in later rounds. Appears on The Amber Hive. |
| Raincloud  | grey cloud with falling rain, pulsing | Slow, +15 magic resist; bursts into Raindrops when it dies, more with rank (2 to 8). From Elite it leaves rain behind it that heals the enemies who stand in it. Appears on The Storm Ridge — a level-authored enemy. |
| Raindrop   | tiny blue drop, path-facing | Quick (2 cells/s) and a little nimble; what a Raincloud bursts into, and sent in swarms of its own. Appears on The Storm Ridge. |
| Gale       | three-bladed whirlwind, spinning | Every four seconds it shoves every other enemy within 1.5 cells 1.2 cells further along its path (Gust), further and wider with rank, never past the end; 15 agility. Kill it first, or hold the whole lane. Appears on The Storm Ridge. |
| The Thunder | wide storm cloud with a lightning bolt, rumbling | A boss, the slow half of the storm's twins: armor, plating and resilience that blunt physical hits, +100 tenacity, immune to Daze, and a rumble that jams the towers near it; once badly hurt it blinds the towers near it until the wave is over (Thunderclap). Bond: if the Lightning falls and the Thunder still stands six seconds later, it calls the Lightning back, reborn - quicker, harder to hit and worth no bounty. If any twin escapes, the level is lost. The last round of The Storm Ridge. |
| The Lightning | white-hot lightning bolt, path-facing | A boss, the quick half of the twins: magic resist and insulation that blunt magic hits, agility, +100 tenacity, immune to Daze, faster as it is hurt and a sprint after every dodge. Its Bond calls the Thunder back, reborn - louder and more heavily plated - if the Thunder falls first and stays down six seconds. Bring both down close together. |
| The Iron Colossus | riveted octagon with a furnace slit, swaying | A boss in an iron shell: armor and heavy plating blunt physical hits, crit immune, +100 tenacity, immune to Daze. It lays ward mines that shield the escorts behind it and calls two Knight escorts from time to time, who pay no bounty. Once badly hurt it reforges into the Molten Core. If it or the Core escapes, the level is lost. The final wave of Iron Crossing — a level-authored enemy. |
| The Molten Core | blazing star with a white-hot heart, pulsing | What the Colossus melts down into: no armor or plating but high magic resist and a casing that blunts magic hits, immune to burn, freeze and Daze, and faster the more it is hurt. It leaves pools behind it that heal the escorts who stand in them, and bursts into four Embers when it dies. |
| Ember      | tiny flame | A quick, nimble, burn-immune shard of the Molten Core; each one that gets through costs a life. |
| Mirage     | two overlapping diamonds | A Skirmisher that leaves a Mirror Image behind every time it dodges: a fragile copy that pays no bounty and grows tougher with the Mirage's rank. Appears on The Rift — a level-authored enemy. |
| Herald     | swept golden arrow, path-facing | A Courier that lays haste ground until the wave is over the moment it is first hurt, speeding everything behind it. Appears on The Rift. |
| Eclipse    | violet crescent, spinning | Every few seconds hides every other ally near it from aimed shots, longer with rank; never itself. Appears on The Rift. |
| The Monolith | standing stone with a glowing rune | A lone, very slow boss: crit immune, immune to freeze, chill and Daze, armor that hardens against the damage type that hurts it most, regeneration, a heal whenever it is left alone, a stone-skin shield at two thirds and at one third of its health, and a last stand once nearly dead. If it escapes, the level is lost. Walks alone in one round of The Rift. |
| Bronze Bull | horned bull's head, striding | A Runner cast in bronze: half its health, a little plating and immune to burn, on the Runner's ladder and abilities. Appears on The Labyrinth — a level-authored enemy. |
| Talos      | bronze giant with a vein of ichor, swaying | A boss: the bronze guardian. A body that crits cut deep (-60 resilience) inside a huge shield that holds until broken, recast again and again; shooting through it is hopeless, so it asks for a tower that strips, pierces, blocks or inverts shields. If it escapes, the level is lost. Round 2 of The Labyrinth. |
| The Minotaur | large horned bull's head, striding | A boss: thick hide (armor, plating, resilience), +100 tenacity, a bellow that jams the towers near it, a charge every ten seconds, faster the more it bleeds, and a bloodlust once badly hurt that heals it and sends it running. If it escapes, the level is lost. The last round of The Labyrinth. |

### Rank

Every enemy spawns at one of five named ranks — **Grunt, Soldier, Veteran, Elite, Boss** — shown
as a badge on its body rather than as a number. A wave's script can name the rank its slots default to in its head
(`soldier: 8 walker`), and is Grunt when it names none; any slot can name a different rank inline, before the enemy id (`elite walker`) or before a spawn
shape (`elite swarm 4 walker`). The Walker sets the ladder: its health climbs a curve that bends upward to 32 times the Grunt's, so each
rank adds more than the one before, an Elite is worth more than two Veterans and a Boss two Elites; its
bounty grows only with the square root of its health, so a tougher rank asks for more damage per credit (a
Walker has 75, 138, 458, 1175 and 2400 health and pays 2, 3, 5, 8 and 11). Every other enemy's health is
written rank by rank, so that it weighs about the same number of Walkers of its rank at every rank up to
Veteran, a quarter more at Elite and half as much again at Boss, and its bounty is that weight times the
Walker's, less what its spawns pay; `td.balance.EnemyPowerReport` measures the weight (`docs/BALANCE.md`). A kill's score is weighted by
rank too — a Boss-rank kill is worth disproportionately more than a Grunt-rank one of the same
bounty. Every enemy defines all five ranks, and gains traits or abilities as it climbs, except the
spawnlings, which are one rank each, and the Warden chain, which is unique: every stage and egg is a
Boss whatever rank its wave asks for (a non-`boss` rank keyword before it fails the level load), and
the reinforcements a Warden calls come two at a time as Elites, every five seconds. Elite and Boss Walkers and Menders carry adaptive
armor: it resists whichever damage type (physical or magic) has landed most this level, from not
at all at an even split up to full strength against a one-type defence.

### Status effects

Towers and enemy abilities put timed effects on enemies. A burning enemy burns, a frozen one is inside ice and a
shielded one is inside a bubble. Effects with no picture show as one small dot per group under the enemy: a debuff, a mark
that sets it up for another tower, or a buff. Selecting an enemy shows each effect as an icon whose tooltip says what it does.

| Effect | Group | Side | What it does |
|---|---|---|---|
| Chill | Control | Debuff | Slows by what its source gives; only the strongest slow counts, and speed comes back over its last 0.5 s. |
| Burn | Affliction | Debuff | Damage over time that fades; a top-up adds less the fuller it is, up to x2 its strongest application; it adds a stack of Scorch every 0.5 s while it lasts, and every tower can see and aim at it. |
| Freeze | Control | Debuff | Stops it where it stands and keeps its abilities from firing; Agility -100, so no dodges and more crits on it; time under it shortens the next one, and walking wins that back, more slowly at higher rank. |
| Shield | Boon | Buff | Takes a hit before armor does, until its amount is spent or it runs out; more Spirit makes it hold more, shields from different sources add up, and damage over time goes past it. |
| Invisibility | Boon | Buff | Towers can't aim at it, though damage on an area still reaches it. |
| Heal | Boon | Buff | Adds to its Regeneration while it lasts; heals from different sources add up. |
| Vulnerable | Weakness | Debuff | Each stack: Physical damage taken and Magic damage taken +15%, up to 3 stacks. |
| Reveal | Mark | Debuff | Every tower can see and aim at it, and it can't gain Invisibility. |
| Poison | Affliction | Debuff | Damage over time that fades; it adds a stack of Sicken every 0.5 s while it lasts. |
| Scorch | Weakness | Debuff | Each stack: Resilience -1, up to 100; once nothing adds more, it loses a stack every 1 s. |
| Sicken | Weakness | Debuff | Each stack: Spirit -1, up to 100; once nothing adds more, it loses a stack every 1 s. |
| Sunder | Weakness | Debuff | Each stack: Armor -5, up to 10 stacks. |
| Expose | Mark | Debuff | Agility and Resilience -50: more and stronger crits on it. |
| Mark | Mark | Debuff | The next hit on it is a crit, which spends this. |
| Priority | Mark | Debuff | Physical damage taken and Magic damage taken +15%, and a tower that picks one target picks it. |
| Daze | Control | Debuff | Stops it and keeps its abilities from firing, like Freeze; Agility -100, so no dodges and more crits on it. |
| Charge | Mark | Debuff | The next hit on it from another tower lands 30% of that hit again as magic, twice that on a crit, for the tower that applied it. |
| Doom | Hex | Debuff | When it ends, it takes a share of all the damage it took while it lasted. |
| Blight | Hex | Debuff | It has Poison for as long as this lasts, and a slow that grows to its strongest as it ends. |
| Inversion | Hex | Debuff | Its Regeneration deals it damage instead of restoring health, and a Shield it receives strikes it instead. |
| Reckoning | Hex | Debuff | When it dies, each Doom from the same tower near it releases at once and starts again. |
| Silence | Control | Debuff | Its abilities don't fire; abilities on death and auras still do. |
| Anchor | Control | Debuff | Holds Move speed to at most 75% of its base, whatever raises it, and it comes back over its last 0.5 s. |
| Unravel | Weakness | Debuff | Each stack: Magic resist -10, up to 5 stacks. |
| Brittle | Weakness | Debuff | Physical damage taken +30%. |
| Toll | Weakness | Debuff | Time spent in an Obelisk's field, a stack a second; it sets how strongly the field holds it, and goes soon after it leaves. |
| Corrode | Weakness | Debuff | Each stack: Armor and Magic resist -3, up to 15 stacks. |
| Drag | Control | Debuff | Slows by 1% a stack, up to 50 stacks; only the strongest slow counts, and speed comes back over its last 0.5 s. |
| Condemn | Weakness | Debuff | Each stack: Physical damage taken and Magic damage taken +5%, up to 4 stacks. |
| Stasis | Weakness | Debuff | While it also has Daze, the field holding it takes its Toll away. |
| Dead Zone | Weakness | Debuff | Spirit -100, so Heal and Shield are worth nothing. |
| Frail | Weakness | Debuff | Each stack: Physical damage taken and Magic damage taken +2%, up to 15 stacks. |
| Crack | Weakness | Debuff | Plating -5 and Insulation -5. |
| Deep Crack | Weakness | Debuff | Plating -10 and Insulation -5. |
| Tar | Weakness | Debuff | Slows by 40%; only the strongest slow counts, and speed comes back over its last 0.5 s. |
| Bleed | Affliction | Debuff | Physical damage for every cell it travels, ignoring Armor and Plating; standing still, it takes none. |
| Soulfire | Affliction | Debuff | Damage over time in a pool of its own beside Burn and Poison; it adds a stack of Scorch and Sicken every 0.5 s while it lasts, and every tower can see and aim at it. |
| Sprint | Boon | Buff | Move speed +60%. |
| Hasten | Boon | Buff | Move speed x2; Freeze and Daze still stop it. |
| Resolve | Boon | Buff | Each stack: Physical damage taken and Magic damage taken -1%, as many stacks as the ally that gives it has to give. |

#### Tower terms

Buffs, debuffs and mechanics that towers carry or leave on enemies.

| Term | Side | What it does |
|---|---|---|
| Steady Aim | Buff | Each shot adds a stack, kept across targets and kills; 3 s after the last shot a stack fades, then the next. |
| Focus | Buff | Hits on one target build it; it starts over on a new target. |
| Frenzy | Buff | Faster attacks for the next few shots; a second one starts them over. |
| Haste | Buff | Faster attacks for a while. |
| Fourth Shot | Buff | A shot that is sure to crit. |
| Hollow Point | Buff | A shot that hits harder. |
| Silver Round | Buff | A shot of magic damage that ignores some magic resist. |
| Magazine | Buff | The shells it has ready, in the order it fires them; a new one at the back draws its kind by lot, each kind as likely as its weight among the kinds it owns. |
| Heavy | Buff | A shell that hits harder than a Normal one in the same blast, and flies slower. |
| Nuke | Buff | Two Heavies fused: their damage together in a wider blast that leaves fallout applying Dead Zone, Sicken and Poison. |
| Payload | Buff | A special shell: half a Normal one's damage, and ground it leaves where it lands. |
| Napalm | Buff | A payload whose blast is magic and leaves flames that apply Burn. |
| Tar Shell | Buff | A payload that leaves tar applying Tar and Poison. |
| Cryo | Buff | A payload that leaves frost applying Chill, and Freeze to an enemy that stays. |
| Flare | Buff | A shell that leaves a flare lasting until the wave ends; every enemy that enters it gains Reveal. |
| Barrage | Buff | The front shells loaded together and fired one after another at one target. |
| Carpet Bombing | Buff | The front shells loaded together and laid along the path, from the target back toward where the enemies come from. |
| Cryo Missile | Buff | A missile that applies Freeze. |
| Arcane Missile | Buff | A missile that applies Unravel instead of Freeze. |
| EMP Missile | Buff | A missile that removes Shield and Heal and applies Silence. |
| Tracer Missile | Buff | A missile that applies Reveal and Mark. |
| Event Horizon | Buff | Each death inside the field widens it until the wave ends. |
| Rally | Buff | Faster attacks for a while, from a Beacon's rally spot. |
| Aura | Buff | A Beacon in range raises its damage and range, and takes some of the jam enemies put on it. |
| Ley Line | Buff | A beam between two Obelisks; an enemy on it gets both fields' rules. |
| Nest | Buff | Missiles stored to launch one after another when a target comes. |
| Jammed | Debuff | An enemy nearby slows its attacks and shortens its range. |
| Interference | Debuff | Other Obelisk fields overlapping this one take some of its damage away. |
| Stoke | Debuff | Each wave on an enemy with this tower's Burn raises that Burn a step; the steps go when it ends. |
| Kinship | Buff | Towers a Beacon buffs earn more XP for each other tower type in range. |
| Shatter | Buff | An enemy with Freeze breaks apart and hurts the enemies around it. |
| Arc | Buff | Lightning a primed stone carries from where it lands, jumping from enemy to enemy as magic. |
| Hex | Buff | A curse a Hexer casts with its Souls; each kind does its own thing. |
| Static | Buff | Every enemy its stones or their lightning damage carries a stack, one per Stormcaller; badly hurt, the enemy discharges into itself for each stack and pays each Stormcaller that tagged it in its currency. |
| Soulbrand | Buff | Every enemy its stones damage carries a stack, one per Hexer; when it dies it bursts for each stack, and pays each Hexer that tagged it in its currency. |
| Spark | Buff | The Stormcaller's currency: its stones earn it by the damage they deal, its passive by the enemies that discharge; all of it goes into each of its abilities at once. |
| Soul | Buff | The Hexer's currency: its stones earn it by the damage they deal, its passive by the enemies that die; all of it goes into each of its abilities at once. |
| Thunderclap | Buff | A discharge into every enemy in range, as magic. |
| Thunderstrike | Buff | Lightning onto the healthiest enemy in range, as magic. |

Stacks wear off one per second once nothing adds more, and every other debuff runs out on its own clock, whatever the enemy's spirit. Effects don't change one another, with two exceptions: a pool earns its marks while it lasts, and a Reveal keeps Invisibility out.

### Ground zones

A tower can leave a patch of ground that keeps working after the shot that made it. A zone pulses
twice a second on every enemy inside (hidden ones too), credited to the tower that made it, and the
pulse never crits. Two zones of one kind never stack: an enemy standing in both takes one pulse.
Zones combine only through what they leave on the enemy: only the strongest slow counts, and a burn keeps
burning through a freeze.

| Zone           | Made by                    | What it does to enemies inside |
|----------------|----------------------------|--------------------------------|
| Burning ground | Mortar's Napalm            | Burns them |
| Tar            | Mortar's Tar               | Poisons them and leaves them Tarred (slowed 40%) |
| Frost ground   | Mortar's Cryo              | Chills them, and freezes one that stays 2 s |
| Fallout        | Mortar's Nuke              | Drains their spirit, poisons them and leaves them in a Dead Zone |
| Flare          | Mortar's Flare             | Reveals them for 3 s; it lasts until the wave is over, and a Mortar keeps only its newest three |
| Healing ground | Mender, from Soldier       | Heals them; it lasts 5 s |
| Haste ground   | Runner, from Veteran       | Hastens them (twice the speed) for a second after each pulse; it lasts until the wave is over |
| Ward mine      | Knight, from Elite         | Armed a second after it is laid; the first enemy to step on it gets a Shield, and it is gone; it lasts 8 s |
| Blinding field | Juggernaut, from Veteran   | Works on towers, not enemies: every tower inside misses 75% of its hits, whatever hits them again; fields never stack, the strongest counts; it lasts until the wave is over |

Only a hit can crit: a shot, a blast, a beam pass. Damage over time (a burn, a poison, a field)
never does, but a burn or poison started by a crit starts that much stronger. Each tower has its
own Crit damage (150% unless an upgrade raises it). A guaranteed crit lands whenever the enemy's
resilience is below 100; at 100 nothing crits. Negative resilience makes crits hit harder, and negative agility multiplies their chance (-50 turns 20% into 30%);
Exposed takes 50 agility and 50 resilience. Armor and magic resist never go below 0.

An enemy with agility can dodge a hit aimed at it alone: the Sniper's shot and its Ricochet bounces.
The chance is `agility / (agility + 100)` (25 dodges 20%, 100 dodges 50%),
after the tower's accuracy, which cancels agility point for point. A dodged hit deals nothing, can't
crit and sets off nothing a hit would. Area damage,
damage over time, arcs and missiles are never dodged, and neither is a
guaranteed crit, so a Marked enemy can't dodge. A frozen or Dazed enemy can't dodge at all.

### Spawn shapes

A wave's script can shape how a slot spawns, not just what it spawns — write the shape's
keyword before the enemy id (`flank walker`), or before a member count and the enemy id for
the shapes that take one (`swarm 4 walker`):

| Shape   | Keyword  | What one slot produces                                                |
|---------|----------|-------------------------------------------------------------------------|
| Normal  | (none)   | One enemy on the path centre                                            |
| Swarm   | `swarm`  | *N* smaller, weaker enemies scattered off-path: each has 4/(*N*+3) of the health and bounty (half at five), so a bigger swarm is more of both in all, and shrinks with it, never under half size |
| Line    | `line`   | *N* enemies spread evenly across the path's width, abreast              |
| Flank   | `flank`  | Two enemies hugging opposite edges of the path                          |
| Column  | `column` | *N* enemies in a tight single file, closer together than *N* separate spawns |
| Drip    | `drip`   | *N* enemies stretched over more time than *N* separate spawns           |

A wave's script can also open with a head that sets `w<number>` (`w30: 8 walker`, or `w30 soldier:
8 walker` with a rank), how many ticks one slot of spacing is worth. The default is 22.4; a bigger number spreads the wave out, a smaller one
packs it tighter.

### Affixes

An affix is an adjective a wave script writes right before the enemy id (`3 elite swarm 4 armored
mite`), or in the head for every slot of the wave (`soldier armored: 8 walker`). Each gives the enemy
one twist on top of its type and rank, and its name reads in front of the enemy's ("Armored Knight").
A slot carries at most two, the head's included, and a boss chain takes none. An affix that works on the allies
near it (Commanding, Vengeful, Warding, Mending, Jamming) never goes on a swarm, whose every member would cast it. No affix changes the
bounty. An affix that measures health measures what one member of the slot is sent with, so a swarm
member's share is its own; Hulking counts first, so the others measure its extra health.

| Affix   | Keyword   | What it does                                                                     |
|---------|-----------|----------------------------------------------------------------------------------|
| Hulking | `hulking` | x2.5 health but 75% of its speed, and a bigger body |
| Exalted | `exalted` | Stronger in every stat it has off its default (not speed): a strength twice as far from it, a weakness half as far; a load fails if it has none |
| Armored | `armored` | Armor against physical damage on top of its own: +25, rising to +400 as the level's landed damage leans physical; never with Plated |
| Elusive | `elusive` | Dodges aimed shots: +15 agility at Grunt, rising to +40 at Boss |
| Regenerating | `regenerating` | Restores 2% of its health every second |
| Shielded | `shielded` | A shield worth 30% of its health once below half of it |
| Withered | `withered` | 130% of its speed, but loses 2% of its health every second, so what holds it back kills it; never with Regenerating |
| Commanding | `commanding` | While it lives, the allies near it have Resolve: 10% less damage taken at Grunt, up to 30% at Boss; it fades soon after the Commander falls |
| Vengeful | `vengeful` | As it dies, the allies near it Sprint: 1.5 s at Grunt, up to 4 s at Boss |
| Warding | `warding` | Every 5 s, the allies near it get a Shield against physical hits only: 40 points at Grunt, up to 640 at Boss; magic goes straight through |
| Mending | `mending` | While it lives, the allies near it Heal, more with rank; less than a Mender heals |
| Jamming | `jamming` | Jams the towers near it: they fire 30% slower and see 0.6 cells less far at Grunt, up to 50% and a cell at Boss |
| Taunting | `taunting` | Every tower that picks one target aims at it while it is in range; an enemy carrying Priority still comes first |
| Coldblooded | `coldblooded` | A Freeze on it lasts half as long; never with Unstoppable |
| Steadfast | `steadfast` | A Daze on it lasts a third as long |
| Fireproof | `fireproof` | Immune to Burn, so it never builds Scorch from one; Soulfire still burns it |
| Unstoppable | `unstoppable` | Immune to every slow: Chill, Tar, Drag and Anchor; a Freeze or a Daze still stops it; never with Coldblooded |
| Plated | `plated` | Plating on top of its own, a fixed loss on every physical hit: +5, rising to +30 as the level's landed damage leans physical; never with Armored |

### Wave script ids

A wave script names enemies by these ids; the full grammar is in `src/main/java/td/wave/CLAUDE.md`.

| Token     | Enemy                                                    |
|-----------|----------------------------------------------------------|
| `walker`  | Walker                                                   |
| `knight`  | Knight (reduced damage)                                  |
| `runner`  | Runner (faster when hurt; Boss spawns a brood)           |
| `ghost`   | Ghost (turns invisible when first hit; Elite+ shrouds)   |
| `mender`  | Mender (heals allies)                                    |
| `relay`   | Relay (calls Walkers when hurt)                          |
| `e`       | spacer: takes a spawn slot, spawns nothing               |
| `warden`  | the Warden boss chain (The Amber Hive only)              |
| `drone`, `hiveGuard`, `broodNurse`, `larva`, `cocoon`, `hornet` | The Amber Hive's own enemies (The Amber Hive only) |
| `raincloud`, `raindrop`, `gale`, `thunder`, `lightning` | The Storm Ridge's own enemies (The Storm Ridge only) |
| `sandling`, `timekeeper`, `glassSentinel`, `sandReaver`, `keeperOfHours` | The Last Hour's own enemies (The Last Hour only) |
| `ironColossus` | the Iron Colossus boss (Iron Crossing only)         |
| `mirage`, `herald`, `eclipse`, `monolith` | The Rift's own enemies (The Rift only) |
| `bronzeBull`, `talos`, `minotaur` | The Labyrinth's own enemies (The Labyrinth only) |
| `juggernaut` | Juggernaut (slow, huge plating)                       |
| `mite`    | Mite (tiny, one hit; send in a `swarm`)                  |
| `courier` | Courier (very fast, fragile, high bounty)                |
| `salamander` | Salamander (burn-immune)                              |
| `yeti`    | Yeti (immune to freeze and chill)                        |
| `magma`   | Magma (slow, burn-proof; its trail heals)                |
| `golem`   | Golem (slow, heavily armored; shields itself when hurt)  |
| `skirmisher` | Skirmisher (nimble: dodges aimed shots)               |
| `templar` | Templar (magic barely touches it; physical does)         |

### Levels

Six levels ship with the game, each on a ground of its own: **The Amber Hive** (a 20×13 comb whose single honey
lane loops once at the entrance, sweeps around a wide bend and hooks inward to leave over the top edge, 16 waves: a
brood that is only strong together - Drones and Hive Guards harden for every ally beside them, so area damage that
breaks a pack up comes first; Larvae spin Cocoons that hatch into Hornets if they are let walk; Brood Nurses heal and
ward - and the Warden at the end, who calls Drones to its side), **The Labyrinth** (a 9×9 board where one lane
winds through every cell but the 3×3 at its heart, so nine towers are all there is: three rounds - a long one that
pays for the defence, a round of Bosses around Talos, whose shield needs a tower's answer to shields, and the
Minotaur), **The Last Hour** (a tight, 9×15 portrait board over
wind-rippled sand where three Bezier lanes - dusk, glass and sand - twist through a pinched waist so the layout reads
as an hourglass, glass and sand crossing at its middle, 12 waves per lane: Sandlings crumble on their own, so what
holds them back is worth more than what hits them; Timekeepers turn their allies' health back; Glass Sentinels
shrug off small hits and shatter under crits; and at the end the Keeper of Hours, who turns back what was done to it
unless it is stopped from casting, kept from healing or Inverted), **The Storm Ridge** (a
16×11 board in the rain where a long blue road hooks back on itself and a short yellow lane cuts across it twice, 12
waves per lane, 4 lives: Gales shove whatever walks beside them further along, Rainclouds burst into Raindrops, and
at the end the storm's twins, the Thunder down the road and the Lightning down the lane, who have to fall close
together), **The Rift** (a 17×15 board: lanes
that start and end at a tear in the middle of the board, one of them silent for the first rounds, 12 waves per lane, 3
lives, and one round in which a single enemy walks alone) and **Iron Crossing** (an 18×13
board, the hardest: a long steel lane winds Golems, Juggernauts, Magma and their support back and forth
while a short rush lane of Couriers, Skirmishers, Ghosts and Mite swarms knifes straight through it
three times, 15 waves per lane, 3 lives, and the Iron Colossus at the end: break its shell with magic, then catch the
Molten Core inside with physical damage before it runs). A level can define more than one enemy path, each with its own color, waves
and pace; every built-in level but The Amber Hive and The Labyrinth does. A lane may start or end inside the board, run faster or
slower than the others (`PathDefinition.withSpeed`) and send nothing in a round (a wave of just `e`). A level's
paths all run the same number of waves as synchronized rounds - starting a round spawns every
path's wave for it together. A level can also register its own enemy on top of the ones above,
scoped to that level only; The Last Hour's Sandling, Timekeeper, Glass Sentinel, Sand Reaver and Keeper and The Amber Hive's Drone, Hive Guard, Brood Nurse, Larva, Cocoon, Hornet
and Warden chain and The Storm Ridge's Raincloud, Raindrop, Gale and twins and Iron Crossing's Colossus, Molten Core and Embers and The Rift's Mirage, Herald, Eclipse and Monolith and The Labyrinth's Bronze Bull, Talos and Minotaur (see the Enemies table)
are the built-in ones.

Levels are defined as Java constants in `td.level.BuiltInLevelCatalog`, so adding one today
means a code change and a rebuild. See `TODO.md` for the planned file-based catalog.

## Logging

Each run writes its own log file under `logs/jTD-<timestamp>.log`, alongside console output.
The default level is `INFO`; per-tick and per-render detail is at `DEBUG` and is off by
default — raise the root level in `src/main/resources/logback.xml` for a deep-dive session.
Old run logs are pruned on startup. Tests use `src/test/resources/logback-test.xml` and stay
quiet.

## Project layout

```
src/main/java/td/
  GameEngine, GameLoop, TowerDefense   entry points and the loop
  board/    a level's pixel scale and cell↔pixel math
  cell/     board squares and buildability
  damage/   the damage value type
  economy/  credits, score and lives
  effect/   timed status effects
  enemy/    enemy definitions, traits, abilities and the live mob
  level/    level definitions and the level catalog
  projectile/ in-flight shells and missiles
  stat/     the enemy stat sheet
  tower/    the tower hierarchy, targeting, buffs and upgrade trees
  ui/       Swing presentation, render commands and the Java2D backend
  util/     GameWorld (the composition root) and GameHost
  wave/     path geometry, smoothing and wave composition
```

The one structural rule worth knowing before changing anything: **the game engine is
headless.** `GameEngine` and the domain packages own all game state and never touch Swing;
`TowerDefense` and `td.ui` own all presentation. That is what lets the whole simulation be
driven and asserted from tests with no display.

## Documentation

- **`CLAUDE.md`** — the constraints: the headless/Swing boundary, the threading rule, code
  style, conventions, test conventions and the wave mini-language. Read this before making
  non-trivial changes. It deliberately holds rules only, no rationale.
- **`docs/DESIGN.md`** — how to choose a design here: decomposition, coupling and cohesion,
  object design and SOLID as this codebase applies them, the functional core, and a quick guide.
- **`docs/adr/`** — one architecture decision record per structural decision: what was decided,
  why, and what was rejected.
- **`docs/ARCHITECTURE.md`** — an overview of the system as it is now: the two sides, a tick, the
  threads, the world, a frame, levels, stats and the tooling, linking to the records.
- **`docs/features/`** — design documents for features that have shipped, kept as a record.
- **`src/main/java/td/<package>/CLAUDE.md`** — per-package notes for the packages whose
  internals have invariants worth stating up front.
- **`TODO.md`** — the single source of truth for known gaps and future work. Inline `TODO`
  comments are deliberately not used; add an entry here instead.
- **`docs/releases/`** — one frozen snapshot per released version: every feature, its numbers
  and the technical shape of the game at that tag.

## Version

`pom.xml`'s `<version>` is the single source of truth. It is filtered into
`src/main/resources/version.properties` at build time and read back at startup.

Cutting a release: write `docs/releases/v<version>.md`, commit it, tag that commit
`v<version>`, then raise the pom version in the next commit, so everything after the tag builds
as the next version.
