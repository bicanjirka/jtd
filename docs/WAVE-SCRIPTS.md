# Wave scripts

What a wave script can write: spawn shapes, brackets, spacing, affixes and enemy ids. The full grammar is in
[`src/main/java/td/wave/CLAUDE.md`](../src/main/java/td/wave/CLAUDE.md); ranks are in
[`GAMEPLAY.md`](GAMEPLAY.md#rank), the enemies themselves in [`CONTENT.md`](CONTENT.md#enemies).

## Spawn shapes

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

## Brackets

Brackets group slots (`3 (3 walker e 2 line 2 knight 2 e)`). What stands before the bracket applies to the group:

| Before `(` | What it does to the group |
|------------|----------------------------|
| A count | Sends the whole group that many times in a row. Nothing is added between the repeats, so end the group with `e` for a gap |
| A rank | Every slot inside takes it, unless it or an inner bracket names its own |
| Affixes | Every slot inside carries them, besides its own |

They combine in that order (`3 elite regenerating (3 walker e 2 knight)`). A spawn shape can't go on a bracket.
Brackets nest (`veteran (4 walker e 3 elite (knight 2 walker))`), and a bracket round the whole script gives the
wave its rank or affixes (`soldier armored (8 walker)`). A wave holds at most 1000 slots.

## Spacing

A script's first word can be `w<number>` (`w30 8 walker`, or `w30 soldier (8 walker)` with a rank): how many ticks
one slot of spacing is worth to an enemy of speed 1. Each enemy enters once
the one before it has walked one slot, so a wave enters in script order and its neighbours start the same distance
apart whatever their speeds; an `e` is one more slot the enemy before it walks. The default is 22.4; a bigger number
spreads the wave out, a smaller one packs it tighter.

## Affixes

An affix is an adjective a wave script writes right before the enemy id (`3 elite swarm 4 armored
mite`), or before a bracket for every slot in it (`soldier armored (8 walker)`). Each gives the enemy
one twist on top of its type and rank, and its name reads in front of the enemy's ("Armored Knight").
A slot carries at most two, its brackets' included, and a boss chain takes none. An affix that works on the allies
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
| Poor | `poor` | Pays half its bounty, rounded once over the whole slot; never with Rich |
| Rich | `rich` | Pays double its bounty; never with Poor |

## Enemy ids

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
| `mite`    | Mite (tiny; send in a `swarm`; Veteran+ broods on death) |
| `courier` | Courier (very fast, fragile, high bounty)                |
| `salamander` | Salamander (burn-immune)                              |
| `yeti`    | Yeti (immune to freeze and chill)                        |
| `magma`   | Magma (slow, burn-proof; its trail heals)                |
| `golem`   | Golem (slow, heavily armored; shields itself when hurt)  |
| `skirmisher` | Skirmisher (nimble: dodges aimed shots)               |
| `templar` | Templar (magic barely touches it; physical does)         |
