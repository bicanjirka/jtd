# jTD — a Swing tower defense game

A desktop tower-defense game written in plain Java and Swing. Enemies walk a fixed path
across a grid board; you spend credits placing towers on the buildable cells beside it and
try to kill everything before it laps the path and costs you a life.

All of the artwork is vector — every enemy, tower, beam and path marker is a
`java.awt.Shape` built and painted in code. There are no image assets in the project.

## Requirements

- **JDK 25** (the build targets release 25)
- **Maven 3.9+**

## Build and run

```bash
mvn package               # build target/jTD.jar
java -jar target/jTD.jar  # run the game
```

The jar is shaded, so it bundles its SLF4J/Logback dependencies and needs no extra classpath.

Other useful commands:

```bash
mvn verify        # rules check + test suite — the standing check before a commit
mvn test          # run the test suite
mvn -q compile    # fast syntax/type check

# run a single test
mvn test -Dtest=GameEngineTest#placingATowerOnABuildableCellChargesCreditsAndOccupiesTheCell
```

## Playing

The game opens on a level-select screen. Pick a level to start it; each level brings its own
board size, path, wave list, starting credits and starting lives.

Buy a tower from the toolbar on the right (or with `q`–`t`), then click a buildable
cell to place it. Clicking a placed tower selects it and shows its stats and its own upgrade
tree — three slots, `base`/`head`/`special`, each offering a small number of nodes to buy.
Start each wave yourself when you are ready — waves do not auto-advance.

### Controls

| Key                             | Action                                                                                               |
|---------------------------------|------------------------------------------------------------------------------------------------------|
| `q` `w` `e` `r` `t` `y` `u` `i` | Select tower 1–8 for placement                                                                       |
| `Esc`                           | Cancel tower placement                                                                               |
| `1`–`9`                         | With a tower selected, buy the correspondingly-numbered upgrade node it currently offers             |
| `s`                             | Start the next wave                                                                                  |
| `p`                             | Pause / unpause                                                                                      |
| `f`                             | Cycle tick speed (normal → fast → super fast → normal)                                               |
| `m`                             | Back to the level-select menu (asks to confirm mid-level)                                            |
| `Ctrl`+`Shift`+`D`              | Open or close the dev panel (playtesting controls; its **Help** button lists its own keys)           |

Mouse: move to preview placement, click to place or to select a placed tower, or click an
upgrade-tree button to buy that node. Click a moving enemy (when not placing a tower) to inspect
it: the side panel shows its live health, stats, effects and traits, updating even while paused,
and keeps a "Killed" or "Leaked" line after it's gone until your next click. Hovering an enemy in
the wave preview shows the same stat block for that enemy type.

The dev panel ships in the jar, hidden behind `Ctrl`+`Shift`+`D` rather than a build flag, to
make manual playtesting cheap. It sets credits and lives, keeps them topped up, spawns any wave
script on any path with a live preview, skips, jumps to or restarts waves, steps one tick, kills
or clears the board, grants XP to or resets the selected tower, lets upgrades ignore their gates,
and draws the cell grid with the path's cells shaded and the pointed-at cell's coordinates. Every
control has an `Alt` shortcut, and while the panel is open the bare keys `n` (skip wave), `x`
(spawn the next catalog enemy) and `c` (grant credits) work too. There's also
`td.BalanceHarness`, a headless batch simulation: it drives a level to completion with a fixed
tower loadout and no human input, then prints lives lost, ticks-to-clear per wave, and each
tower's kills/damage dealt.

```bash
mvn -q package -DskipTests
mvn -q dependency:build-classpath -Dmdep.outputFile=target/runtime-classpath.txt
java -cp "target/classes;$(cat target/runtime-classpath.txt)" td.BalanceHarness
```

Run with no arguments it plays one built-in loadout against Curly Path; edit `main()` to try
a different loadout or level, since v1 has no config format for either.

### Towers

| Tower  | Price | Behaviour                                                                                                                                           |
|--------|-------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| Sniper | 15    | Single target, hits whichever enemy in range is furthest along the path; the longer it stays on one enemy, the likelier it crits (crits x2.0) |
| Splash | 15    | Instant blast on a random target in range, physical damage falling off with distance; attuned, it aims at the most crowded group and Saturates what it hits. Forks into the Stormcaller (lightning arcs on past the blast) or the Hexer (every 4th shot casts a hex) |
| Sonar  | 20    | Long range; a beam sweeps around it once every 3s, hitting everything it passes; once attuned, each sweep Exposes the healthiest enemy it passed |
| Pulse  | 25    | Short range; a field that deals magic damage to everything in range every tick, ghosts included and with no visible enemy needed. Attuned, an enemy that stays inside builds Toll: it takes more from the field, and every debuff on it wears off slower |
| Aura   | 20    | Passive — boosts the damage and range of nearby towers; several stack                                                                               |
| Mortar | 30    | Lobs a slow, unguided shell at whichever enemy in range is furthest along the path, never one within 1.5 cells of it; the blast does physical damage with falloff and cracks the plating of everything it reaches. Attuned, a shell landing near the last one hits harder and wider. One head adds heavier shells and nukes, the other shrapnel and bombs along the path, and a ballistics line speeds the shell up, leads the target, widens the blast and fires salvos. Its specials turn every third shell into a Napalm, Tar or Cryo shell that leaves burning, tarred or frozen ground |
| Seeker | 30    | Fires a slow homing missile at the fastest enemy in range, which it keeps through invisibility and re-aims at each tick; deals magic damage and freezes whichever mob it actually hits. Attuned, it banks missiles in a nest, between waves too, and launches them as a salvo |
| Cinder | 28    | Turns slowly toward the nearest visible enemy and, once one is in the cone, fires a flame wave that burns everything it reaches, invisible enemies included. A burn never crits but an ignition can, which starts the pool stronger. Attuned, each wave on an enemy it is already burning raises that burn. Forks into hot, short White Flame with Soulfire, or the wide Wide Nozzle that leaves burning ground; a fuel line makes burning enemies heal less, take more and scorch faster |

The price is the first copy's: each copy of a tower already on the board makes the next one cost
15% more, and the shop shows the current price. Selling refunds three quarters of what the tower
and its upgrades cost.

Every tower, the Aura included, has its own upgrade tree of three slots, shown in place of the
wave preview once the tower is selected. `base` holds three Range steps and the chain Attune ->
Awaken -> Transcendent: Attune opens the first two levels of `head`, Awaken opens level III and
a special, and Transcendent (once a special and a level III head are owned) opens Range III and a
second special. `head` offers two chains that exclude each other; `special` offers one to three
specializations, of which a tower keeps one, or two once Transcendent. Past the first steps a
node also needs XP: every tower an enemy reached earns its bounty as XP when its walk ends,
killed or leaked (an Aura earns with the towers it buffs), and a few nodes need towers built
nearby. Every purchase is permanent for that tower. Upgrade prices are multiples of the tower's
price. Pips on the board show each slot's
level, and a tower with a special glows. Silver diamonds beside a tower show its rank, earned
with XP: Seasoned at 50, Expert at 150, Hero at 300. The Upgrades panel's XP bar ticks where the
next node waits. The Aura tower draws a faint line to every tower it's
currently amplifying.

### Enemies

| Enemy      | Looks like    | Behaviour                                                                                                                                                                                                                                                                                                         |
|------------|---------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Simple     | circle        | Plain mob, no special ability                                                                                                                                                                                                                                                                                     |
| Armored    | square        | Absorbs part of every hit; immune to critical hits                                                                                                                                                                                                                                                                |
| Frenzied   | triangle      | Speeds up as it loses health                                                                                                                                                                                                                                                                                      |
| Ghost      | tinted circle | Turns invisible to single-target towers for ~10s the first time it's hit (area damage still reaches it); at Elite and Boss rank, also permanently shrouds every other ally near it                                                                                                                              |
| Mender     | cross         | Deals no damage of its own; periodically restores health to every other ally near it                                                                                                                                                                                                                             |
| Jammer     | square        | Jams every tower near it: they fire more slowly and see less far while it's in range (drawn as a pink ring; affected towers show a pink marker). Not in any level's waves yet.                                                                                                                                  |
| Empty      | —             | Not a real enemy — a spacer that opens a timing gap inside a wave                                                                                                                                                                                                                                                 |
| The Warden | large spiked crest | A boss: armored, periodically summons reinforcements and shields itself and nearby allies, and shields itself again whenever it survives a critical hit. On death it leaves behind a stationary egg that hatches back into a weaker Warden if not destroyed in time — the fight only ends once an egg is defeated before it hatches. Appears as the final wave of Curly Path. |
| Reaver     | triangle, path-facing | Speeds up as it's hurt, shrugs off a flat amount of every hit, shields itself once badly wounded, and splits into two Simple mobs on death. Appears in Twisted Hourglass's first wave — a level-authored enemy, registered only for that level rather than globally (see `td.level.LevelDefinition.customEnemies`). |

### Rank

Every enemy spawns at one of five named ranks — **Grunt, Soldier, Veteran, Elite, Boss** — shown
as a badge on its body rather than as a number. A wave declares the rank its slots default to;
any slot can name a different rank inline, before the enemy id (`elite c`) or before a spawn
shape (`elite swarm 4 c`). Health and bounty both rise with rank, and a kill's score is weighted
by it too — a Boss-rank kill is worth disproportionately more than a Grunt-rank one of the same
bounty. Not every enemy defines all five ranks; asking for one an enemy doesn't have silently
uses its own highest defined rank instead. Elite and Boss Simple and Mender enemies carry adaptive
armor: it resists whichever damage type (physical or magic) has landed most this level, from not
at all at an even split up to full strength against a one-type defence.

### Status effects

Towers and enemy abilities put timed effects on enemies. An enemy shows one marker per kind of
effect it carries (control, damage over time, debuff, spotted, ...), with a count when that kind
holds more than one effect; selecting it lists every effect with what kind it is and the time left.

| Effect     | Kind             | What it does                                                                                       |
|------------|------------------|----------------------------------------------------------------------------------------------------|
| Chilled    | soft CC          | Slows in proportion to its level, which adds up (never past 80%) and fades linearly. Every slowing tower chills. It also cuts the damage the enemy takes from burning |
| Freeze     | hard CC          | Stops the enemy. Every enemy diminishes it: within 10 s of the last one, each fresh freeze lasts half as long as the one before, and a fourth doesn't take hold. Freezing a chilled enemy uses up the chill and lasts longer by its level; freezing a burning one puts the burn out and lands half of what was left at once |
| Dazed      | hard CC          | Stops the enemy and its abilities like a freeze, but keeps its burn and its chill. It has its own diminishing returns: within 10 s of the last one, each fresh daze lasts 20% less than the one before, and a sixth doesn't take hold |
| Silenced   | soft CC          | Its abilities don't fire: no heals, shields, summons, shrouds or vanishing. Its death abilities still fire, its aura keeps running and it keeps walking |
| Anchored   | soft CC          | Can't be sped up: its speed is held to three quarters of its base, hurt-speed included |
| Undertow   | soft CC          | A quarter slow that doesn't fade while the enemy is in a Pulse's field, and that counts as a chill for a freeze |
| Burning    | damage over time | Damage that decays as it burns; several towers add to one pool and each is credited its share. It earns a Scorched stack when it starts and another every half second while it lasts. An invisible enemy is visible while its pool is strong |
| Soulfire   | damage over time | A third pool of its own beside burning and poisoning, so it stacks with both. It burns enemies that are immune to fire, and earns Scorched and Sickened both |
| Poisoned   | damage over time | Its own decaying pool, so it stacks with burning. It slows in proportion to the pool (at most 30%, on top of chill) and earns Sickened stacks the same way |
| Scorched   | debuff           | Burning's lasting mark: each stack lowers resilience by 1 (down to -100; below 0 crits hit harder but no likelier). It outlasts the fire, then loses a stack per second |
| Sickened   | debuff           | Poison's lasting mark: each stack lowers spirit by 1 (down to -100), so heals and shields shrink and vanish at the floor. Loses a stack per second |
| Vulnerable | debuff           | Up to 3 stacks, +15% damage taken each, from any tower; one shared 4 s clock any application refreshes |
| Sundered   | debuff           | Up to 10 stacks, -5 armor each (armor never goes below 0); one shared 5 s clock                    |
| Resonating | debuff           | Up to 3 stacks, +8% magic damage taken each; one shared 4 s clock                                  |
| Fractured  | debuff           | Up to 5 stacks, -10 resilience each, losing a stack a second. A Sonar with Fault Line lets it fall to 10 stacks (-100) and holds it while the enemy is Exposed |
| Saturated  | debuff           | Up to 3 stacks (a Splash upgrade allows 4), each making a Splash's blast hit it 5% harder; one shared 1.5 s clock any blast that catches it refreshes |
| Unraveled  | debuff           | Up to 5 stacks, -10 magic resist each (never below 0); one shared 5 s clock |
| Brittle    | debuff           | While the enemy is frozen it takes +30% physical damage |
| Toll       | debuff           | Up to 5 stacks (10 with Meltdown) a Pulse's field builds on an enemy that stays inside, one a second: each makes the field hit it 10% harder and every other debuff's timer, and every stack debuff, wear off 10% slower. It fades a second after the enemy leaves |
| Corroded   | debuff           | -30 armor (never below 0) while the enemy is in a Pulse's field |
| Dead zone  | debuff           | No heal or shield takes hold while the enemy is in a Pulse's field; what it already has stays |
| Kill zone  | debuff           | +25% damage taken from every source while the enemy is in a Pulse's field |
| Cracked    | debuff           | Plating is halved; one shared 5 s clock any application refreshes |
| Bleeding   | damage over time | Physical damage for every cell the enemy travels: the faster it runs the more it bleeds, and a stopped enemy bleeds nothing |
| Tarred     | debuff           | Slowed by 40%. A burn it catches starts at double the pool, and a freeze it suffers lasts a second longer |
| Exposed    | spotted          | Crit chance taken doubles. A revealed enemy counts as Exposed                                       |
| Marked     | spotted          | The next hit from any tower is a guaranteed crit, and spends the mark. Damage that ticks never spends it, and it waits on a crit-immune enemy |
| Priority   | spotted          | +15% damage taken from every tower, and every tower that picks one target picks it while it is in range |
| Charged    | spotted          | The next hit from another tower than the one that charged it discharges it: +30% of that hit again as magic (x2 on a crit), credited to the charging tower |
| Doomed     | hex              | When it ends, the enemy takes 30% of all the damage it took meanwhile as one magic hit, credited to the Hexer. Lasts 4 s, +1 s per Saturation stack |
| Blighted   | hex              | Poisons the enemy for as long as it lasts |
| Contagious | hex              | When the enemy dies, its hexes and debuffs jump to the 2 nearest unhexed enemies nearby with the time they had left; a jumped hex jumps once more at most |
| Rimed      | hex              | Chills when cast. Freezing the enemy buys twice the chill's extra freeze time and lands all of its burn at once instead of half. Replaces Ashen |
| Ashen      | hex              | Its burn and poison hold twice as much and earn Scorched and Sickened twice as fast; it can't be frozen and takes a quarter of any chill. Replaces Rimed |
| Inverted   | hex              | Heals it receives are dealt to it as magic damage over their length, shields as one hit of their share of its full health, both credited to the Hexer; it can't turn invisible |
| Sympathetic | hex             | Once a second, its Vulnerable, Sundered, Exposed and chill are copied, up to what it has, to the Hexer's other hexed enemies within 2 cells |
| Reckoned   | hex              | When the enemy dies, every Doom of that Hexer within 2 cells pays out at once and starts again |
| Shield     | restorative      | Absorbs a share of every hit                                                                       |
| Heal       | restorative      | Restores health every tick                                                                         |
| Invisible  | stealth          | Towers cannot target the enemy; area damage still reaches it                                       |
| Revealed   | stealth          | Towers can target the enemy again, even through invisibility; turning invisible again ends it      |

Stacks wear off at a pace spirit sets: neutral spirit loses one per second, more spirit is faster, and at -100 they never wear off, so a fully sickened enemy stays sickened. Every other debuff's timer runs at `max(0.25, 1 + spirit / 100)` of normal speed (freeze keeps its own diminishing returns). A frozen enemy cannot burn, and freezing an enemy puts out its burn, landing half of what was left at once, but keeps its Scorched stacks. Hexes bend this: Rimed and Ashen change what a freeze does, and Inverted keeps an enemy from turning invisible. Nothing else cancels anything.

### Ground zones

A tower can leave a patch of ground that keeps working after the shot that made it. A zone pulses
twice a second on every enemy inside (hidden ones too), credited to the tower that made it, and the
pulse never crits. Two zones of one kind never stack: an enemy standing in both takes one pulse.
Zones combine only through what they leave on the enemy, so tar then fire burns at double and a
freeze in frost bursts a burn.

| Zone           | Made by                    | What it does to enemies inside |
|----------------|----------------------------|--------------------------------|
| Burning ground | Mortar's Napalm            | Burns them |
| Tar            | Mortar's Tar               | Poisons them and leaves them Tarred (slowed 40%; a burn starts at double, a freeze lasts a second longer) |
| Frost ground   | Mortar's Cryo Shells       | Chills them, and freezes one that stays 2 s |
| Fallout        | Mortar's Tactical Nuke     | Drains their spirit and keeps heals and shields from taking hold |

Only a hit can crit: a shot, a blast, a beam pass. Damage that ticks (a burn, a poison, a field)
never does, but a burn or poison started by a crit starts that much stronger. Each tower has its
own crit multiplier (x1.5, the Sniper x2.0). A guaranteed crit lands whenever the enemy's
resilience is below 100; at 100 nothing crits. Armor and magic resist never go below 0.

### Spawn shapes

A wave's script can shape how a slot spawns, not just what it spawns — write the shape's
keyword before the enemy id (`armored warden1`), or before a member count and the enemy id for
the shapes that take one (`swarm 4 c`):

| Shape   | Keyword  | What one slot produces                                                |
|---------|----------|-------------------------------------------------------------------------|
| Normal  | (none)   | One enemy on the path centre                                            |
| Armored | `armored`| One enemy with extra physical-only armor, on top of any armor it already has: it blocks 20% of physical damage, rising to 80% as the level's landed damage leans fully physical — no size, speed, health or bounty change |
| Swarm   | `swarm`  | *N* enemies at 50% size, scattered off-path, sharing one spawn's bounty and health |
| Line    | `line`   | *N* enemies spread evenly across the path's width, abreast              |
| Flank   | `flank`  | Two enemies hugging opposite edges of the path                          |
| Column  | `column` | *N* enemies in a tight single file, closer together than *N* separate spawns |
| Drip    | `drip`   | *N* enemies stretched over more time than *N* separate spawns           |

### Levels

Three levels ship with the game: **Curly Path** (a 20×13 board whose single lane spirals
through two tight loops before unwinding into a zigzag, 18 waves), **Zigzag Path** (a smaller,
16×11 board where a fuchsia lane hooks back on itself and a lime lane cuts across it twice, 8
waves per lane, only 3 lives) and **Twisted Hourglass** (a tight, 9×14 portrait board where three
Bezier lanes - crimson, teal and amber - twist through a pinched waist so the layout reads as an
hourglass, 10 waves per lane). A level can define more than one enemy path, each with its own color, waves
and pace; Zigzag Path and Twisted Hourglass are the two built-in levels that do. A level's
paths all run the same number of waves as synchronized rounds - starting a round spawns every
path's wave for it together. A level can also register its own enemy on top of the ones above,
scoped to that level only; Twisted Hourglass's Reaver (see the Enemies table) is the one
built-in level that does.

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
- **`docs/ARCHITECTURE.md`** — the *why* behind those constraints: how the render pipeline is
  layered, why the art is vector, how levels load and unload, what the threading rule is
  protecting against, and the design history.
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
