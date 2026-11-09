# `td.wave` (+ `smoothing`)

## Geometry

- `Point` = integer cell coordinates (level authoring, `PathCoverage` results). `Vec2` = pixel
  coordinates, the only thing a `Path` stores. Convert only in `PathBuilder`/`PathCoverage`.
- Corners → `PathBuilder.build` → `PathSmoothing` → `Path`. Consecutive corners may be any
  distance and angle apart (one straight leg).
- `ArcLengthPath` is the single answer to "where is distance *d*"; enemy movement and the path
  markers both use it. `of` returns empty for a path under 2 points or of zero length. `poseAt`
  clamps rather than extrapolates.
- Buildability comes from the final geometry (`PathCoverage.unbuildableCells` supersamples),
  not the authored cells. It takes scale/width/height, not the cell array. Multi-path levels
  use the union.
- Smoothing: `PathSmoothing.none()` is the identity, never `null`. `cornerPull` is a fraction of
  the shorter leg, capped at 0.5; degenerate corners stay unrounded. A new strategy extends
  `AbstractCornerSmoothing` and supplies only one corner's curve.

## Wave script

Space-separated tokens, parsed by `WaveScript.parse(script, catalog)` (or with a fallback rank, for
the dev panel) into a
`WaveContent` without needing `GameWorld`. `Wave.spawn()` builds the mobs when the wave starts,
never in the constructor: that keeps a level load a single `LoadedLevel` publication. Each
`spawn()` call builds a fresh set, so calling it twice puts two copies on the board. For
counts, use `enemyCount()`/`enemySet()`, which need no spawn.

- An optional **head**, ended by `:`, holds the wave's defaults in slot order: `w<number>`, a
  rank, then affixes (`w30 soldier armored: 3 walker`). The first `:` ends it; a token ending in `:` reads as the word and
  the `:`. `w<number>` sets the ticks one slot of spacing is worth, default
  `SpawnParameters.DEFAULT_DELAY_TICKS_PER_SLOT`, and carries on `WaveContent`; it is positive, stands
  only in a head, and no enemy id may look like it. A level wave whose head names no rank is Grunt:
  the head is the only place a wave's rank is written.
- **Rank keywords** `grunt soldier veteran elite boss` go before the enemy or shape they rank and
  override the head's rank for that slot. A unique enemy is Boss whatever the default; any
  rank keyword but `boss` before it is an error.
- **Shape keywords** go before the enemy: `flank` (2 members at opposite sides), `swarm n` (half size, health/bounty split,
  scattered), `line n` (spread across), `column n` / `drip n` (spaced tighter / looser than one
  slot).
- **Affix words** (`td.enemy.Affix`) go after the rank and shape, right before the enemy, and add
  to the head's. The grammar applies them to the slot's definition when it builds the slot, measured
  against the slot's rank and one member's health. At most `WaveScript.MAX_AFFIXES` a slot, head
  included; none twice, none that `Affix.excludes` another, none on a unique enemy, none whose
  `problemWith` objects.
- A shape says how a slot spawns, never what the enemy is: that is an affix.
- A count applies to the next token: `3 knight e 4 walker`. Placed before a rank/shape keyword, it repeats
  the whole slot (`3 elite swarm 4 walker`); placed right after a shape keyword, it is the member
  count.
- Errors (`GameStartupException`): an unknown token, anything but a spacing and a rank in a head,
  a second `:`, a spacing outside a head, a rank after a shape, a rank right before
  `e`, two ranks in a row, a missing or forbidden member count, a repeat count over
  `WaveScript.MAX_REPEAT`. Blank tokens are skipped. An error
  marks the offending token in the script, and a level's load adds its level, path and wave.
- `WaveScriptGrammar` parses: each rule takes an immutable cursor and returns what it
  read with the cursor after it. A spawn-type keyword is one `ShapeKeyword` row.
- Reserved tokens are matched before catalog lookup, and `EnemyCatalog.register` rejects ids that
  collide with them. Built-in and per-level ids resolve identically.
- A new enemy id gets a row in `README.md`'s "Wave script ids" table.

## Composition rules

- `WaveContent.enemyCount()` counts **members**, not slots (it seeds the alive count). Spacers
  keep their slot for timing but aren't counted.
- Health and bounty come from the slot's resolved `EnemyDefinition` × shape multipliers, size from
  the shape's times the definition's affixes'.
- Formation scatter uses `RandomSource.seeded(scatterSeed * 31 + slotIndex)`, never
  `GameWorld.random()`. `scatterSeed` comes from the level name and wave index, so formations
  reproduce.
- `Wave`/`SpawnSpread` give offsets relative to spawn facing and never read the path's facing;
  the mob fixes the world vector.
- Read the wave index and count together via `GameEngine.waveProgress()`.

## Multiple paths

- A level has 1+ `PathDefinition`s (`of`/`smoothed` + `withColor`/`withSpeed`), and every path
  has the same wave count (enforced). Round *N* spawns every path's wave *N* together
  (`LoadedLevel.wavesAt`), and it clears when the summed alive count reaches zero.
- A mob binds to its path by `SpawnParameters.pathIndex`, resolved once in its constructor. A path's
  speed multiplier folds into `SpawnParameters.speedMultiplier()`; no new mechanism.
- A round's clear bonus is the sum of its waves' `WaveDefinition.clearBonus`, one per path
  (`LoadedLevel.clearBonusAt`); the engine pays it only when no enemy leaked during the round.
- `EnemyMob.getProgression()` is a fraction of the mob's own path length, so "furthest along"
  compares correctly across paths.
- `PathColor` is level content (RGB, no AWT), not a `Palette` role.

`WaveAnnouncer`/`WaveStartListener` is the wave-start hub (`CopyOnWriteArrayList`, fired on
`game-loop`). Where the game is in its waves (the count started, whether one is out, its start tick) is one
`WaveClock` behind one `volatile`; a reader needing two of them reads `clock()` once.
