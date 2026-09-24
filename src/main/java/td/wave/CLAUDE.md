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
| `c`       | Simple                                                   |
| `s`       | Armored (reduced damage)                                 |
| `t`       | Frenzied (faster when hurt; Boss spawns a brood)         |
| `g`       | Ghost (turns invisible when first hit; Elite+ shrouds)   |
| `m`       | Mender (heals allies)                                    |
| `e`       | spacer: takes a spawn slot, spawns nothing               |
| `warden1` | the Warden boss chain                                    |

- **Rank keywords** `grunt soldier veteran elite boss` go before the enemy or shape they rank and
  override the wave's default rank for that slot.
- **Shape keywords** go before the enemy: `armored` (1 member, stacks a physical-only adaptive
  resist on any armor it has), `flank` (2 members at opposite sides), `swarm n` (half size, health/bounty split,
  scattered), `line n` (spread across), `column n` / `drip n` (spaced tighter / looser than one
  slot).
- A count applies to the next token: `3 s e 4 c`. Placed before a rank/shape keyword, it repeats
  the whole slot (`3 elite swarm 4 c`); placed right after a shape keyword, it is the member
  count.
- Errors (`GameStartupException`): an unknown token, a rank after a shape, a rank right before
  `e`, two ranks in a row, a missing or forbidden member count. Blank tokens are skipped.
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
