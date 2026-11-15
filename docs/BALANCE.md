# Balancing enemies, bounties and waves

How the game's enemies, what they pay, the waves they come in and the towers that meet them are held to one scale,
and how to check that they still are after a change. The enemy and wave tools use no tower, so they keep working
while the towers are being redone; the tower bench measures the towers in code.

```bash
mvn -q compile exec:java -Dexec.mainClass=td.balance.EnemyPowerReport   # every enemy at every rank; seconds
mvn -q compile exec:java -Dexec.mainClass=td.balance.WavePowerReport    # every round of every level; two minutes
mvn -q compile exec:java -Dexec.mainClass=td.balance.TowerBench         # every build of every tower; a minute
mvn -q compile exec:java -Dexec.mainClass=td.balance.TowerReport        # prices and verdicts from the bench's sheets; seconds
mvn -q compile exec:java -Dexec.mainClass=td.balance.TowerPriceWriter   # writes the prices the report calls off into the towers' sources
```

`EnemyPowerReport` takes `--kinds golem,yeti`, `--ranks grunt,elite` and `--fit golem=1.7,yeti=1.7`;
`WavePowerReport` takes `--levels rift,hive`; `TowerBench` takes `--towers sniper,burst`, `--scenarios stream,group`
and `--quick`; `TowerReport` takes nothing and reads what the bench stored. All store their rows in
`target/balance/`.

## The pipeline

Balance is settled in a fixed order, each stage reading what the one before it stored in `target/balance/`:
enemies, then waves, then towers. A stage stores a tab-separated sheet for the next stage and an HTML page for a
person (`ReportPage` writes both).

| Stage | Tool | Stores | Page |
| --- | --- | --- | --- |
| 1 Enemies | `EnemyPowerReport` (a full run, no `--kinds` or `--ranks`) | `enemy-power.tsv`: one row per enemy and rank, its power on every axis as a multiple of the Walker; `enemy-baseline.tsv`: what the Walker of each rank takes, which turns those multiples into damage a second | `enemies.html` |
| 2 Waves | `WavePowerReport` | `wave-power.tsv` | |
| 3 Towers, measured | `TowerBench` (reads `enemy-power.tsv`; with `--towers` it replaces only those towers' rows) | `towers.tsv`: one row per build of each tower, what it holds of each scenario in Walkers; `tower-pairs.tsv`: every two towers side by side; `tower-scenarios.tsv`: the scenarios and what a point of health is worth in each | `tower-bench.html` |
| 3 Towers, judged | `TowerReport` (reads the bench's three sheets; rerun it after every bench run) | `tower-prices.tsv`: one row per tower and node, its price, worth, fair and suggested price and verdict; `tower-stages.tsv`: each tower bought whole to each tier, its cost, power, worth and value | `towers.html` |

## The probe rig

Enemies are measured against a rig, not a tower. The rig lands hits on every enemy of a board through the same calls a
tower's hit makes, so armor, plating, insulation, dodging, hiding, shields, heals, spawns and immunities answer as
they do in play, and how fast an enemy walks is how long the rig has. It has four probes:

| Probe | What it lands | What it stands for |
| --- | --- | --- |
| shot | one heavy physical hit on the enemy furthest along, sometimes a crit | a tower that picks one target |
| bolt | the same hit as magic | single-target magic |
| blast | a small physical hit on everything near the enemy furthest along | area damage |
| burn | a magic burn on everything | damage over time |

It can also chill everything, freeze now and then, and keep every enemy revealed. What enemies do to towers counts:
the rig samples each tower-weakening aura and field on the cells a defence would stand on and lands only the share
towers standing there would still land.

The measure is always a threshold: the weakest rig that lets nothing through, found by bisection over four seeds.

## An enemy's power

Every figure is a multiple of the Walker of the same rank, which is 1.0 on every axis. Enemies walk in groups (six
at Grunt, Soldier and Veteran, three at Elite, one at Boss), and the rig's hits grow with rank as an upgraded
defence's do.

- **shot, bolt, blast, burn**: what the enemy takes of each probe alone.
- **seen**: the shot while something keeps the enemy revealed.
- **blind**: what it takes of a balanced mix, a quarter of what the Walker takes of each probe, with a chill. This is
  the enemy's power when the player has not prepared for it. An enemy whose strength is what it does for the enemies
  around it (an aura, a shove) is weighed among Walkers instead, as if it alone made the whole difference to them.
- **countered**: the least of its axes: what it takes of its best answer.
- **unheld**: how much less a chill and a freeze hold it back.
- **oneType**: for an enemy that adapts to the damage a level deals, how much more a defence of one damage type takes.

Enemy power is not additive: a slow enemy costs little beside fast ones, and a healer is worth what it heals. So a
wave is never the sum of its enemies; waves are measured whole (below).

### The rule

An enemy may weigh up to **1.75 Walkers** blind at Grunt, Soldier and Veteran, **1.25 times that at Elite and 1.5
times at Boss**. It may weigh more only if it has a clear answer: countered at 0.6 of its blind power or less, and
countered under the limit. The report's verdict column says `ok`, `ok: has an answer` or `BREACH`. A Boss and a
unique enemy are marked `estimate`: one enemy alone on a test lane says little about a boss fight.

### What each rank is for

| Rank | Intent | Carries |
| --- | --- | --- |
| Grunt, Soldier | the classic enemy | one piece of identity (the Knight's armor, the Golem's shield, the Relay's alarm) |
| Veteran | special, not overpowering | the second piece (the Golem splits, the Magma's last stand, the Salamander's hide) |
| Elite | a miniboss | everything, stronger; weighs a quarter more against the Walker |
| Boss | massive | everything at its strongest; half as much again; only estimated |

### Health and bounty

The Walker sets the ladder: 75, 138, 458, 1175 and 2400 health, so an Elite is worth more than two Veterans and a
Boss two Elites, and it pays 2, 3, 5, 8 and 11. Its bounty grows with the square root of its health, so a higher
rank asks for more damage per credit.

Every other enemy's health is written rank by rank (`RankGrowth.stepped`) so that it weighs its target number of
Walkers at every rank: `--fit kind=target` solves the health. Targets in use: Runner 1.2, Courier 1.3, Knight,
Ghost, Salamander and Skirmisher 1.5, Relay and Templar 1.6, Mender, Magma, Golem and Yeti 1.7, Juggernaut 2.8 (its answer is
magic).

**Bounty = the Walker's bounty at that rank × blind power − what its spawns pay**, rounded (`paying`; the report's
`fair` column). Within a rank it is linear in power; up the ranks it is sublinear because the Walker's is. The
Courier is the deliberate exception: it pays far more than it weighs, as a prize for catching it. A bounty never
falls as rank rises, so the Relay, whose calls pay more of its share the higher it stands, holds its Soldier bounty. Reinforcements a
caller sends on a timer pay nothing, so what a round pays is bounded; the Warden's Drones still pay.

### Where the table stands

Regenerate it with the first command above; these are the lines that are not `ok`.

| Enemy | Blind | Why it stands |
| --- | --- | --- |
| Sand Reaver | 4.5 at Grunt, 3.1 at Elite | the game's one brute, allowed over the limit |
| Juggernaut, Soldier | 2.6, countered 1.65 | its answer (magic) is clear at every other rank; a margin of the measure |
| Glass Sentinel, Raincloud, Hornet | 1.77 to 1.83 at one or two ranks (Hornet 2.20 at Elite) | within a few hundredths of the limit |
| Warden | about 22 | unique boss, left as authored and played by hand |
| Minotaur | about 7 | unique boss, left as authored |

Each damage type has a classic that blunts it and is ordinary to the other: the Juggernaut's plating against
physical, the Relay's insulation and the Templar's magic resist against magic. Plating and insulation take a flat
loss from every hit, so a heavy hit goes through them; magic resist takes a share, so only the other type does.

## A wave's strength and a level's economy

`WavePowerReport` plays each round of each level alone, on the level's own board, lane by lane.

- **walkers**: the round's strength: the single-shot damage that holds it, times the seconds it is on the board,
  counted in Walkers of the lowest rank.
- **bounty, bonus**: what its kills pay and what clearing it with no leak pays.
- **bank**: the starting credits and everything earlier rounds paid.
- **need**: what the cheapest defence that holds the round costs when it is built of the paper tools' reference
  tower (`.claude/skills/jtd-balance-tools/reference.json`, the Sniper: the single-target ceiling). The defence is
  filled cell by cell, best cell first, bare towers before tiers. A tower is credited with the share of the lane it
  reaches while a group walks past, and with the whole time a stream keeps coming. **floor** is the same for the
  weakest tower.
- **room**: bank over need. Above 1 the reference defence is affordable.
- **even**: room over the line the level's own rooms follow from round to round. 1 is a round exactly as tight as
  its place in the level asks; 0.8 is a gate; 1.3 is a breather.
- **asks for**: the part of a defence the round leans on more than Walkers do: how much more of the mix it takes
  once one kind of damage is removed, or how much less once enemies are revealed or held.

Need is a yardstick, not a forecast. The reference tower's tiers add less damage per credit than a new tower does,
and the towers in code today are stronger than it. Read how room moves through a level, not where it stands.

### The targets

- Room follows one line through a level: about **1.3 in the first round down to 1.0 in the last** (The Last Hour,
  the hardest level: 1.1 down to 0.7).
- A **gate** sits about 15% under the line; every level has three or four, one of them its last round.
- No other round is more than about 30% off the line.
- A **clear bonus** differs from round to round. It is paid where kills alone leave the next round short, most
  often on the round before a gate, and is never more than 0.6 of that round's bounty, so a leak costs a player
  something but does not sink them.

### The gates

| Level | Gates (round: what it checks) |
| --- | --- |
| Iron Crossing | 6: the first Veteran on the long lane · 10: armor and plating · 13: an Elite Magma under a commander, with Templars: magic resist · 15: the Colossus |
| The Storm Ridge | 6: the first jamming Raincloud · 8: a mending cloud over a column · 10: an Elite cloud and Gale-pushed Knights · 12: the twins |
| The Amber Hive | 5: a Veteran larva rush · 9: swarms round an Elite Brood Nurse · 12: Elite Hive Guards and Elite drones · 16: the Warden |
| The Rift | 5: the third lane opens · 9: jamming Ghosts behind a Juggernaut · 12: all three lanes at once |
| The Last Hour | 4: Soldiers on three lanes · 8: Timekeepers on every lane · 11: an Elite on every lane · 12: the Keeper |

The Labyrinth is three long rounds that pay while they run; the report counts a round's pay only once it is over, so
its rooms there mean little and its waves are proved in play, not by the report. Its middle round is headed Elite: that is what
Boss weighed before the rank ladder was steepened. For the same reason the Warden calls two Elite Drones at a time, not
three, and its round's escort is Elite.

The Warden's round stands far under its level's line (room about 0.3), and the players in code lose it every time.
It is the one round the method flags and does not fix: the Warden stays as authored and is played by hand.

### Tuning a wave

Room does not move with how many enemies a wave has, since more enemies pay more. It moves with:

- **rank**: a higher rank has more health per credit;
- **how much arrives at once**: a group needs more than a stream of the same enemies;
- **the lane**: a short lane is expensive to hold;
- **the clear bonus and the starting credits**, which add credits and no strength.

Change a wave, run the report for that level, read `even`.

## What a tower holds

`TowerBench` measures towers the way the rig measures enemies, turned round: the defence is fixed and the wave is
searched. One build of one tower stands beside the middle of the straight test lane, in the real engine, with its
upgrade gates waived. A scenario's wave has a fixed shape, and the health of its enemies is searched for the most the
build lets nothing through of, over five seeds. So everything a tower does counts for what it does: a slow for the
time it buys, a kill for what it triggers, a miss for the leak it causes.

- **Capacity** is that health in **Walkers**: the health of a Grunt Walker, times how many enemies the wave has,
  times how much tougher than a Walker of its rank the enemy stage found the enemy per point of health
  (`blind` × the Walker's health ÷ its own). A defence of plain mixed damage therefore holds about as many Walkers
  in every scenario, and a tower's row shows what it is an answer to and what it is not.
- A wave's enemies have from half the health searched to half as much again, so they do not all take the same
  number of hits and a small gain in a tower shows as a small gain in capacity.
- **0** means the build alone lets something through whatever its health: too few shots for a column, hits that
  bounce off plating, shells that miss.

| Scenario | Wave | Shows |
| --- | --- | --- |
| single | one Veteran Walker | damage on one target over one pass |
| stream | eight Veteran Walkers, five seconds apart | sustained single-target damage |
| group | twelve Soldier Walkers in a tight column | area damage |
| boss | one Boss Walker | one target with a boss's defences |
| armored, plated, resist, hidden, fast | a stream of Veteran Knights, Juggernauts, Templars, Ghosts, Runners | each defence and trick |

Every scenario is fought twice: alone, and beside two fixed partners, a bare Sniper and a bare Burst (the column
ending in `+`, counting what all three hold; `tower-scenarios.tsv` has what the partners hold by themselves). The
second is where a tower that only helps others shows, and where a tower that holds nothing alone shows what it adds.

The builds are read off each tower's tree, so a new tier, node, cluster or tower needs no change to the tool:

| Role | Build | Set against (`versus`) |
| --- | --- | --- |
| `bare` | the tower as bought | |
| `line` | one node on the nodes it needs | the same without it (`before`, or `bare`) |
| `full` | every node up to a tier; each cluster gives its first member, a fork one family per side, and each other cluster member a family of its own | |
| `without` | a whole tier less one node, in the tier the node sits in | that tier whole |
| `without-top` | the whole tree less one node | the whole tree |

A node's worth alone is its `line` over what that is set against; its worth in a build is the `full` build over the
`without` one. `tower-pairs.tsv` sets every two towers side by side, bare and bought whole, against what each holds
alone on the same cells: a synergy above 1 is help, below 1 is two towers in each other's way.

## What a tower is worth

`TowerReport` turns the bench's sheets into prices and verdicts. It measures nothing itself, so it runs in seconds
and is rerun after every bench run. `towers.html` opens with every verdict that breaks a rule; the console prints
the same list.

**Power and worth.** A build's **power** is the Walkers of a mixed wave it adds to the bench's two partners: for
each scenario, what the team holds less what the partners hold without it, weighted by the mix in `TowerSheet`
(plain enemies one half, the defences the other). It is read beside the partners because a tower alone holds
nothing of a wave it lets one enemy through. The **going rate** is the power a credit buys of a bare tower, the
mean over the judged towers. A build's **worth** is its power at that rate, in credits, and its **value** is worth
over cost: 1 is as good a buy as the average bare tower.

**Tower prices.** A tower's fair price is its bare worth. A price within a fifth of that is right.

**Node prices.** What a node does is the given; its price follows. Nodes multiply each other, so what a build
loses without a node grows with the build, and those losses add up to more than the tree. A node's **worth** is
therefore a share, the mean of two:

- its share of what its tier adds to the tiers below it, split among the tier's nodes by what the whole tier loses
  without each: what the node gives when it is bought;
- its share of what the whole tree adds to the bare tower, split the same way among all its nodes: what it gives in
  the end, which is where a node bought early that multiplies everything after it gets its due.

A tower's node worths add up to its tree, and a node others need is not credited with them. Then:

- **fair price** = worth � the tier's **premium**, 1.25 to the power of tier + 1: the copy surcharge of the copy of
  the tower a player would be buying instead by then. An upgrade takes no cell and no surcharge, so it costs more
  than the same power as bare towers, and more the later its tier. A whole tree comes to about half its cost in
  worth.
- No node costs less than its tier's **floor**, a tenth of the tower's price for each tier up from the tower.
- The members of a cluster and the sides of a fork share the dearest measured member's price: a tower that owns
  one switches to another for nothing, so a cheaper member would be a way round the dearer one's price.
- A price within a quarter of the suggestion, or two credits of it, is right.
- A node that moves no build's power by a fortieth is **unmeasured**: a utility the bench's scenarios do not show
  (a reveal with nothing hidden left to find, a counter to an ability no scenario enemy has). It is listed and
  keeps the price it has, unless its cluster gives it one. A node that adds to one build and takes from another is
  unmeasured too, says so, and is priced at its tier's floor.
- A node every build holds less with is a **breach**, as is one the whole tree holds a tenth more without.

`TowerPriceWriter` writes every tower and node price the report calls off into the tower's source, by the node's
name, and leaves the ones it calls right, so writing, measuring and judging again settles in two rounds instead of
chasing the bench's noise.

**The pace of tiers.** Prices follow what nodes do, so the pace the game wants of its tiers is a check on the nodes,
not a rule the prices are bent to. The Tiers table adds up each tier of a tower's best whole tree at the suggested
prices, in tower prices: the first tier should come to less than the tower, the second to about the tower (0.6 to
1.6), and each later tier to more than the one before. It also sets each tier against the same tier of the other
towers. A tier off the pace, or more than twice or under half its rivals', is one whose nodes do too little or too
much for where they sit: that is what to change a node's numbers for.

**Verdicts.**

| Table | Asks | Breaks when |
| --- | --- | --- |
| Stages | what a credit buys of each tower bought whole to each tier, by its best build, against the median of the judged towers at that tier | `DOMINANT`: it pays 1.25 times its rivals, or no rival holds more for a credit against 60% of a mixed wave. `WARN weak`: it pays under 0.8 |
| Roles | the rules in `TowerReport.RULES`, at every tier, by what a credit holds alone | `BREACH`, naming each rival on the wrong side and by how much |
| Profiles | what a credit holds alone in each scenario over the median tower | no verdict: a row warm everywhere is a tower with no weakness |
| Tiers | each tier's cost at the suggested prices against the pace and the same tier of the other towers | `WARN`, saying which |
| Nodes | the prices above | `WARN price`: off its suggestion. `DOMINANT node`: the whole tree loses over a third of its power without it |
| Clusters and forks | each member in the whole tree bought with it | `DOMINANT pick`: a member a credit of which buys 15% more than any other, and that is behind in no scenario. `WARN weaker pick`: under 0.85 of the best |
| Synergies | the bench's pairs, by the geometric mean over the paired scenarios | `WARN strong pair` from 2, `WARN clash` to 0.7 |

Roles compare what a **credit** holds, not what a tower holds: towers bought to the same tier cost up to three times
each other, so the top tower of a role is the one a credit spent on holds the most there. The rules now:

- the Sniper is the top single-target tower (single, stream, boss);
- against armor and plating the Aerie is the top single-target tower (armored, plated);
- the Burst before its fork is the weakest area tower, below the Mortar and the Scorcher on a group, and about
  equal to the Obelisk there;
- no tower is weaker on one target than the Burst before its fork.

A tower whose tree is not final is measured and listed but **not judged** (`TowerSheet.NOT_JUDGED`): no rate,
median or rule counts it. Taking it off that list is all it needs to be judged.

**Changing a number.** Change what a tower or a node does, run `TowerBench --towers <it>`, `TowerReport`, then
`TowerPriceWriter`, and the bench and the report once more: the bench stores each build's cost, so the report
judges the new prices only after it. A price change moves no power, only cost, and with it value, the roles and the
price verdicts; a change to what a node does moves its worth, and its neighbours' shares with it. A new tier or
tower is priced the same way, with no change to the tools.

## What this does not settle

- **How hard the game is.** The targets fix the shape of each level, not its difficulty; that waits for the
  next-gen towers (`features/FEATURE-balance-after-next-gen-towers.md`).
- **Bosses.** Their rows are estimates, and their numbers are as authored.
- **Lives.** A leak is a loss to the rig; how many leaks a level forgives is not measured.
- **Whether upgrades are worth buying in play.** The premium is set by rule; whether a player with few cells left
  buys a node over a tower at those prices is for the play check to show.
