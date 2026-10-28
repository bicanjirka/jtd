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

Space-separated tokens, parsed by `WaveScript.parse(tokens, defaultRank, catalog)` into a
`WaveContent` without needing `GameWorld`. `Wave.spawn()` builds the mobs when the wave starts,
never in the constructor: that keeps a level load a single `LoadedLevel` publication. Each
`spawn()` call builds a fresh set, so calling it twice puts two copies on the board. For
counts, use `enemyCount()`/`enemySet()`, which need no spawn.

| Token     | Enemy                                                    |
|-----------|----------------------------------------------------------|
| `simple`  | Simple                                                   |
| `armored` | Armored (reduced damage)                                 |
| `fast`    | Fast (faster when hurt; Boss spawns a brood)         |
| `ghost`   | Ghost (turns invisible when first hit; Elite+ shrouds)   |
| `mender`  | Mender (heals allies)                                    |
| `jammer`  | Jammer (weakens nearby towers; in no wave yet)           |
| `e`       | spacer: takes a spawn slot, spawns nothing               |
| `warden`  | the Warden boss chain                                    |
| `titan`  | Juggernaut (slow, huge plating)                       |
| `mite`    | Mite (tiny, one hit; send in a `swarm`)                  |
| `courier` | Courier (very fast, fragile, high bounty)                |
| `shield` | Shieldbearer (physical-only shield for allies)      |
| `lizard` | Salamander (burn-immune)                              |
| `yeti`    | Yeti (freeze-immune, half chill)                         |
| `dodger` | Skirmisher (nimble: dodges aimed shots)               |

- A leading `w<number>` (`w30 3 c`) sets the ticks one slot of spacing is worth, default
  `SpawnParameters.DEFAULT_DELAY_TICKS_PER_SLOT`; it carries on `WaveContent`. It must be the first token and
  positive, and no enemy id may look like it.
- **Rank keywords** `grunt soldier veteran elite boss` go before the enemy or shape they rank and
  override the wave's default rank for that slot. A unique enemy is Boss whatever the default; any
  rank keyword but `boss` before it is an error.
- **Shape keywords** go before the enemy: `plated` (1 member, stacks a physical-only adaptive
  resist on any armor it has), `flank` (2 members at opposite sides), `swarm n` (half size, health/bounty split,
  scattered), `line n` (spread across), `column n` / `drip n` (spaced tighter / looser than one
  slot).
- A count applies to the next token: `3 s e 4 c`. Placed before a rank/shape keyword, it repeats
  the whole slot (`3 elite swarm 4 c`); placed right after a shape keyword, it is the member
  count.
- Errors (`GameStartupException`): an unknown token, a rank after a shape, a rank right before
  `e`, two ranks in a row, a missing or forbidden member count. Blank tokens are skipped. An error
  marks the offending token in the script, and a level's load adds its level, path and wave.
- `WaveScriptGrammar` parses: each rule takes an immutable cursor and returns what it
  read with the cursor after it. A spawn-type keyword is one `ShapeKeyword` row.
- Reserved tokens are matched before catalog lookup, and `EnemyCatalog.register` rejects ids that
  collide with them. Built-in and per-level ids resolve identically.
- A new enemy id gets a row in this table and in `README.md`.

## Composition rules

- `WaveContent.enemyCount()` counts **members**, not slots (it seeds the alive count). Spacers
  keep their slot for timing but aren't counted.
- Health and bounty come from the slot's resolved `EnemyDefinition` × shape multipliers. A shape's
  trait override is composed onto the definition before any mob is built.
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
- A mob binds to its path by `SpawnParameters.pathIndex`, resolved once in its constructor. Path
  and wave speed multipliers fold into `SpawnParameters.speedMultiplier()`; no new mechanism.
- `EnemyMob.getProgression()` is a fraction of the mob's own path length, so "furthest along"
  compares correctly across paths.
- `PathColor` is level content (RGB, no AWT), not a `Palette` role.

`WaveAnnouncer`/`WaveStartListener` is the wave-start hub (`CopyOnWriteArrayList`, fired on
`game-loop`).
