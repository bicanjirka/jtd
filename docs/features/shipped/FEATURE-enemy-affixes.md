# Feature Request: Enemy affixes

## Summary

Enemy affixes, inspired by Diablo 3 and 4: adjectives a level author puts on an enemy in a wave
script ("Hulking Taunting Walker"), each giving it one generic, readable twist on top of what its
type and rank already make it. They give level authors variety without new enemy types and ask the
player new questions: which enemy dies first, where on the path, and to which tower. Every enemy
name becomes a noun so the adjectives read naturally, and today's `plated` spawn shape becomes the
first affix.

## Current state (what exists today)

- **`plated` is already an affix in disguise.** It is a spawn-shape keyword (`ShapeKeyword.PLATED` →
  `SpawnShape.armored()`), but unlike every other shape it changes what the enemy is, not how the
  slot spawns: it adds an adaptive physical-only armor (`AdaptiveResist`, +25 rising to +400 as the
  level's landed damage leans physical) through `SpawnShape.traitOverride`. No other shape uses
  `traitOverride`. Its name is also wrong: it gives *armor*, while *Plating* is a separate stat
  (flat reduction per physical hit). Iron Crossing is the only level that uses it.
- **Wave script.** `[w<n>] ` then slots of `[count] [rank] [shape [members]] id`. A wave's default rank is
  not in the script: it is the `Rank` argument of `WaveDefinition(enemies, rank)`, written in every
  level's Java (145 `WaveDefinition`s). The dev panel's spawn box takes a script.
- **Names.** Five enemies are adjective + "mob": Simple mob (`simple`), Armored mob (`armored`),
  Fast mob (`fast`), Ghost mob (`ghost`), Mender mob (`mender`). The rest are already nouns. Several
  level enemies are described as "on the Simple's ladder" / "a Fast mob cast in bronze" in `README.md`.
  `EnemyCatalog.register` rejects an id that collides with a reserved wave-script token, so the
  id `armored` and an `armored` keyword cannot both exist.
- **Ranks already deepen identity.** "Every enemy gains something of its identity from Elite on."
  A unique enemy (the boss chains) is Boss whatever the wave asks, and a non-`boss` rank before it
  fails the load.
- **Bounty follows health.** `SpawnShape.bountyShares`: "the slot pays for the health it sends".
  The purchase and build harnesses reason in fair prices built on that.
- **Every mechanic the ten affixes need exists**, except Exalted's doubling and Taunting's flag:
  adaptive armor, agility, regeneration (and its negative, crumbling), a shield at a health threshold
  (the Armored mob's Brace), radius effects while alive and on death (Mender, Shieldbearer, Mender's
  death blast), Sprint, the stat sheet's damage-taken stats, size/health/speed multipliers (shapes).
- **Targeting preference exists.** Every single-target tower wraps its selector in
  `PreferringSelector.priority`: an enemy under the `PRIORITY` effect wins while in range. Area
  towers don't pick, and invisibility is filtered before any preference. `PRIORITY` is a player-side
  debuff (Sonar's Command Ping, Pulse's Arc Discharge) that also adds 15% damage taken.
- **Spawned enemies** (Golemites, reinforcements, Raindrops, broodlings) are built by
  `AbilitySpawnFactory` at the caster's rank; nothing else of the caster carries over.
- **Board marks.** A rank badge (chevrons, star, skull) sits upright above an enemy; effects without a
  picture show as dots under it; radius-targeted abilities draw a support-aura ring
  (`EnemyDefinition.supportAura()`). The wave preview (`PathWaveRow`) shows upcoming enemies with a
  tooltip each. `docs/UI.md`: colour is never the only signal, and the board is the quietest surface.

## What this feature adds

### Nouns and adjectives

Every enemy's display name is a noun; affixes are adjectives in front of it, in the order written
("Elite Hulking Taunting Walker"). The five "mob" enemies get new noun names, every enemy id is made
consistent with its name, and every level script, `README.md` row and description that names them
follows.

### The ten affixes

| Affix | What it does | Scales with | What answers it |
|---|---|---|---|
| **Hulking** | Much more health, but slower and bigger | its health | Time in range; splash finds it more easily |
| **Armored** | Today's `plated`, unchanged: adaptive physical-only armor on top of its own | the level's damage mix | Magic |
| **Elusive** | Dodges aimed shots | rank | Area damage, damage over time, accuracy, Mark |
| **Exalted** | Every stat it has off its default, doubled (not speed, not health) | its own sheet | Depends on the enemy, by design |
| **Regenerating** | Restores health over time | its health | Dead Zone, Inversion, Sicken, burst |
| **Shielded** | A shield once below half health | its health | Shield strip, damage over time, shield-piercing crits |
| **Withered** | Faster, but loses health on its own | its health | Slows: a slowed one dies on the way |
| **Commanding** | While it lives, allies near it take less damage | rank | Kill it first |
| **Vengeful** | When it dies, allies near it Sprint | rank | Kill it last, or at the back |
| **Taunting** | Single-target towers in range must aim at it | - | Area damage; the player's Priority |

Hulking and Withered are trades (a strength and a weakness); the rest are pure. "Scales with" says
what an affix's number is measured against, so the same affix means as much on a Mite as on a Boss:
a share of the enemy's own health, a value per rank, or (Armored) what it adapts to.

### Wave script

- **Per slot:** affixes go after rank and shape, right before the id, as English orders adjectives:
  `3 elite swarm 4 hulking withered mite`. A count before the rank repeats the whole slot, as today.
- **Script head:** a wave's defaults move into the script, ended by `:`. Order: `w<n>`, rank,
  affixes, the same order as a slot: `w30 soldier armored: 8 walker 2 runner`. Every part is
  optional, and a script without defaults has no head.
- A slot's rank replaces the head's rank; a slot's affixes add to the head's.
- The `Rank` argument of `WaveDefinition` goes; the head is the only place a wave's default rank is
  written. A wave with no rank in its head is Grunt.
- `plated` is no longer a shape keyword.

### Seeing them

- The info pane names the enemy with its affixes and explains each in one line, in terms the player
  already knows.
- The wave preview shows them before the wave starts, since the player answers an affix between
  waves, not during one.
- The board marks an affixed enemy (see Decisions made). Commanding and Vengeful get the
  support-aura ring the game already draws for radius abilities.

### Out of scope for v1

Splitting, Mending, Jamming, Enraged (faster when hurt), Escorted, Timeless (rewinds itself),
Volatile (shoves allies on death), Emboldened (tougher further along the path), Coldblooded (freeze
wears off fast), Greedy (a leak also costs credits), Plated (as plating), immunities. One line each
under `TODO.md`'s "Feature ideas".

## Interconnections

- `FEATURE-tower-visual-revamp.md` is in flight (uncommitted edits). It is about towers, not
  enemies, but both touch the board's marks and `docs/UI.md`'s colour budget.
- Iron Crossing's waves use `plated armored`; they become the Armored affix on the renamed Armored
  mob, and should play the same.

## Constraints and open risks

- **Affixes must not duplicate rank abilities.** Rank deepens what an enemy is; an affix adds
  something generic any enemy can carry.
- **No extra bounty breaks "the slot pays for the health it sends"** for Hulking (more health, same
  pay). Affixes are a difficulty knob, and their cost lands in balance tuning, not the economy. The
  fair-price harnesses and `BalanceReport` must stay meaningful on affixed waves.
- **Exalted on a stat with a ceiling.** Resilience 100 already means crit immune; tenacity, agility
  and resists have curves. Doubling past a ceiling does nothing more, and doubling a negative stat
  makes the enemy weaker.
- **Taunting vs Priority.** `PRIORITY` cannot simply be put on the taunter: it is a timed debuff
  that tenacity shortens, it adds damage taken, and its dot means a player-side mark.
- **Board noise.** A `swarm 12` of affixed enemies multiplies the marks; the board is the quietest
  surface in `docs/UI.md`, and colour may never be the only signal.
- **Test rules.** Affix mechanics are tested on fixture definitions; built-in affix numbers are
  content, which `BalanceReport` judges. A new reserved token must not collide with any enemy id.
- **Renames reach far:** every enemy id changes where it doesn't match its name, so level scripts, `README.md` tables and descriptions, tests that name the ids,
  the dev panel, `td.balance` (the tower lab names enemy ids).
- **Performance budget:** a radius affix (Commanding) on many enemies runs per tick; it must stay
  inside the tick budget on the zones loadout.
- Headless engine, `GameHost` as the only channel, and the threading rules apply as usual.

## Decisions made

- **Authored only.** No random affixes, so balance runs reproduce exactly.
- **No extra bounty** for any affix, Hulking included.
- **Spawns don't inherit** affixes: what an affixed enemy spawns or splits into is plain.
- **The rename is part of this feature**, not a separate one before it.
- **Ten affixes in v1**, the list above.
- **A load fails on** an affix that does nothing (Exalted on an enemy with no stats off default),
  or Regenerating together with Withered.
- **Taunting:** the player's Priority beats a taunt. A tower with both in range aims at the
  Prioritised enemy, which gives Command Ping an answer to taunts.
- **The script head** carries `w<n>`, the default rank and wave-wide affixes, ended by `:`, in slot
  order (`w30 soldier armored:`), and replaces the `Rank` argument of `WaveDefinition`.
- **New nouns:** Simple mob → **Walker**, Armored mob → **Knight**, Fast mob → **Runner**, Ghost mob →
  **Ghost**, Mender mob → **Mender**.
- **Ids are consistent:** an enemy's id is its display name in lower camel case, without "The"
  (`walker`, `knight`, `runner`, `juggernaut`, `salamander`, `skirmisher`, `shieldbearer`,
  `raindrop`, `glassSentinel`, `keeperOfHours`). Spawnlings follow the same rule, and a spawnling
  whose name uses an affix's word (Armored Reinforcement) is renamed so it doesn't read as affixed.
- **Exalted** doubles every stat off its default except speed and health.
- **The board mark** is small glyph pips beside the rank badge, one per affix, in one colour meaning
  "affixed"; checked on real enemies with `PreviewEnemy` before it ships.
- **Every head ends with `:`**, `w<n>` alone included.
- **Any rank, at most two affixes per slot** (head and slot counted together), **none on a unique
  enemy**: an affix on one fails the load, as a wrong rank does now.
- **Names:** adjectives read naturally; no `-ed`/`-ing` rule. An affix never reuses an existing
  term's word (Frenzy, Brittle, Frail, Hasten, Sprint, Mark and Priority are taken).

## Open questions

None.
