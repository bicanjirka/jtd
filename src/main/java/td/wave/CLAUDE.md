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

- `script := w<n>? item*`; an item is a slot or a **group**: items in brackets (`3 elite armored ( … )`). Brackets
  need no space beside them, and groups nest. Before a group stand only a count, a rank and affixes, in that order: the
  count repeats the group, and the rank and affixes are the defaults of everything inside.
- `w<number>` sets the ticks one slot of spacing is worth to an enemy of speed 1, default
  `SpawnParameters.DEFAULT_DELAY_TICKS_PER_SLOT`, and carries on `WaveContent`. It is positive, stands only first in the
  script, since it times the whole wave, and no enemy id may look like it.
- **Rank keywords** `grunt soldier veteran elite boss` go before the enemy, shape or group they rank; the innermost one
  wins. A level wave's slot that no rank reaches is Grunt. A unique enemy is Boss whatever its group or the default
  says; any rank keyword but `boss` right before it is an error.
- **Shape keywords** go before the enemy: `flank` (2 members at opposite sides), `swarm n` (smaller and weaker the more members, the swarm more health and bounty in all,
  scattered), `line n` (spread across), `column n` / `drip n` (spaced tighter / looser than one
  slot).
- **Affix words** (`td.enemy.Affix`) go after the rank and shape, right before the enemy or the group, and add
  to the open groups'. The grammar applies them to the slot's definition when it builds the slot, measured
  against the slot's rank and one member's health. At most `WaveScript.MAX_AFFIXES` a slot, groups'
  included; none twice, none that `Affix.excludes` another, none on a unique enemy, none whose
  `problemWith` objects, and none that `actsOnAllies` on a swarm.
- A shape says how a slot spawns, never what the enemy is: that is an affix.
- A slot takes at most two counts: one first, which repeats the whole slot (`3 knight e 4 walker`,
  `3 elite swarm 4 walker`), and one right after a shape keyword, its member count. A group takes the first only. A
  count anywhere else fails, so no enemy id may be all digits.
- Groups expand while parsing: `WaveContent` is a flat slot list, and nothing after the parser knows of a group. Nested
  counts multiply, so a wave holds at most `WaveScript.MAX_SLOTS` slots, checked before a repeat is built.
- Errors (`GameStartupException`): an unknown token, a spacing that isn't the first word, a bracket never closed or
  closing none, a group with no slot, a shape on a group, a rank after a shape, a rank right before `e`, two ranks in a
  row, a count out of place or left over, a missing or forbidden member count, a repeat count over
  `WaveScript.MAX_REPEAT`, more slots than `WaveScript.MAX_SLOTS`. Blank tokens are skipped. An error
  marks the offending token in the script and ends with `WaveScriptGrammar.RULES`, and a level's load adds its
  level, path and wave.
- `WaveScriptGrammar` parses: each rule takes an immutable cursor and returns what it
  read with the cursor after it, only the open groups are carried between rules (a stack, so reading a whole script and
  reading its start for suggestions share them), and a broken rule throws its checked `ScriptFault`. A spawn-type keyword is one
  `ShapeKeyword` row, a rank one `RankKeyword` row.
- `WaveScriptSuggestions` asks `WaveScriptGrammar.next` what the same rules accept after the typed tokens; it never
  reads the script itself. A new keyword is its row plus the rule that reads it, and `next` answers for it.
- Reserved tokens are matched before catalog lookup, and `EnemyCatalog.register` rejects ids that
  collide with them. Built-in and per-level ids resolve identically.
- A new enemy id gets a row in `docs/WAVE-SCRIPTS.md`'s "Enemy ids" table.

## Composition rules

- A slot enters once the enemy before it has walked one slot of spacing (`Wave`), so spawn order is script order;
  a spacer is one more slot that enemy walks. Never time a slot from its index alone: a slow enemy would enter after
  the faster ones scripted behind it.
- `WaveContent.enemyCount()` counts **members**, not slots (it seeds the alive count). Spacers
  keep their slot for timing but aren't counted.
- Health comes from the slot's resolved `EnemyDefinition` × shape multipliers; bounty and size from
  the shape's times the definition's affixes'. A slot's bounty is rounded once, over all its members.
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
