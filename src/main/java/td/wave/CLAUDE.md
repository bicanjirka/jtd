# `td.wave` — path geometry and wave composition

Read the root `CLAUDE.md` first; this file only covers what is specific to this package and
its `smoothing` subpackage.

The package holds two loosely related things that share a name only by history: the **geometry** enemies walk, and the
**composition** of a wave.

## Geometry

**`Point` and `Vec2` are not interchangeable, and the split is load-bearing.** `Point` is
integer *cell* coordinates and appears only in level authoring (`LevelDefinition.path`) and
in `PathCoverage`'s result. `Vec2` is continuous *pixel* coordinates and is the only thing a
`Path` ever stores. If you find yourself converting between them outside `PathBuilder` or
`PathCoverage`, something is in the wrong layer.

The pipeline, in order:

```
List<Point>  --PathBuilder-->  pixel centers  --PathSmoothing-->  Path (List<Vec2>)
                                                                    |
                        ArcLengthPath.of(path) <---------------------+---> PathCoverage
                        (movement + marker overlay)                        (buildability)
```

`PathBuilder.build` is the *only* place a level's corner list becomes a `Path`. There is no
per-cell expansion step: consecutive corners may sit at any distance and any angle, and are
joined by one straight leg.

**`ArcLengthPath` is the single shared answer to "where is distance `d` along this path".**
Both `AbstractEnemyMob`'s movement and `td.ui.PathMarkerFrameBuilder`'s overlay go through
it, which is what keeps them at a consistent pace on curved and diagonal geometry. It is
`Optional`-returning — a path with fewer than two points or zero length has nothing to
measure, and callers handle that rather than being handed a broken instance.

`poseAt` clamps out-of-range distances to the nearest endpoint rather than extrapolating.

**Buildability is computed from the final geometry, not from the authored cell list.**
`PathCoverage.unbuildableCells` supersamples each cell in the path's bounding box and marks
it unbuildable when enough of its area falls inside a one-cell-wide corridor. For an
unsmoothed axis-aligned path this reproduces the naive "mark the listed cells" result
exactly (`PathCoverageTest` proves it for the built-in levels); for a smoothed path it
correctly follows the curve. It takes plain `scale`/`width`/`height` rather than a
`Cell[][]` on purpose, which is what keeps it and its tests independent of the cell machinery.

### Smoothing (`td.wave.smoothing`)

`PathSmoothing` is a per-level strategy. `none()` is the identity — a level that wants no
smoothing gets this, not a `null` field. `AbstractCornerSmoothing` is the shared template
both real strategies build on; a subclass supplies only the curve for one corner.

`cornerPull` is a *fraction* of the shorter adjacent leg, capped at 0.5, so two nearby
corners' pullbacks can never cross. Degenerate corners (already straight, or a near-total
reversal) are passed through unrounded rather than forced into a curve the construction
cannot handle.

`ArcCornerSmoothing` is an exact circular fillet, tangent to both legs.
`QuadraticBezierSmoothing` is cheaper and not exactly tangent, but visually similar — it is
what the two curved built-in levels use.

## Wave composition

**The token table.** These are the ids `EnemyCatalog.builtIn()` registers, and this is the
canonical list - the root `CLAUDE.md` documents the grammar but deliberately not the content,
so adding an enemy does not touch the always-loaded file. Add a row here and to `README.md`'s
enemy table when you register a new id.

| Token     | Enemy                                                                               |
|-----------|-------------------------------------------------------------------------------------|
| `c`       | Simple — no special behaviour (drawn as a circle)                                   |
| `s`       | Armored — takes reduced damage (drawn as a square)                                  |
| `t`       | Frenzied — speeds up as it is hurt; Boss also calls a brood of spawnlings (drawn as a triangle) |
| `g`       | Ghost — vanishes to single-target towers the first time it's hit; Elite/Boss also shroud nearby allies (drawn as a tinted circle) |
| `m`       | Mender — deals no damage; periodically heals nearby allies (drawn as a cross)       |
| `e`       | Empty - the reserved spacer; counts toward spawn timing, not toward the enemy count |
| `warden1` | The Warden boss - the only id in its six-stage chain a wave spawns directly         |

**The rank keyword table.** A rank keyword goes *before the token it ranks* - an enemy id
(`elite c`) or, when both are present, before the spawn-shape keyword that follows it
(`elite swarm 4 c`) - always in that order, rank first. It sets that one slot's `td.enemy.Rank`,
overriding the wave's own default (`WaveDefinition.rank()`) for that slot only.

| Keyword    | `Rank`             |
|------------|--------------------|
| `grunt`    | `Rank.GRUNT`       |
| `soldier`  | `Rank.SOLDIER`     |
| `veteran`  | `Rank.VETERAN`     |
| `elite`    | `Rank.ELITE`       |
| `boss`     | `Rank.BOSS`        |

**The spawn-shape keyword table.** A keyword goes *before* the token it shapes - `armored
warden1`, `swarm 4 c`. `SpawnShape.normal()` (no keyword) is the identity every plain token
already gets. `armored` absorbed the old `boss` shape's slot in this table when `boss` became a
rank keyword instead (the Boss rank tier took over its job); `elite`, the old shape keyword, was
renamed `armored` once `elite` the rank name needed the string for itself.

| Keyword    | `SpawnShape` factory | Members        | Mechanism(s)                                      |
|------------|-----------------------|----------------|-----------------------------------------------------|
| `armored`  | `armored()`            | 1 (fixed)      | attaches/replaces a physical-only defensive trait, no multipliers |
| `swarm`    | `swarm(n)`             | *n* (required) | 50% size, health/bounty split, circular scatter      |
| `line`     | `line(n)`              | *n* (required) | evenly spread offset, no multipliers                 |
| `flank`    | `flank()`              | 2 (fixed)      | offset at opposite maximums, no multipliers          |
| `column`   | `column(n)`            | *n* (required) | delay spacing tighter than one slot apart            |
| `drip`     | `drip(n)`              | *n* (required) | delay spacing looser than one slot apart             |

A count immediately **before** a rank or spawn-type keyword repeats the whole ranked-and/or-
shaped slot (`3 elite swarm 4 c` is three Elite-ranked swarm-of-4 slots) - whichever of the two
keywords comes first in a slot is the one the count attaches to, since a rank keyword (when
present) always precedes a spawn-type one, never the reverse. A count immediately **after** a
spawn-type keyword sets that slot's member count instead (`swarm 4 c` is one slot of four) - the
same "a count applies to the token immediately following it" rule as any other token, just read
against whichever kind of token follows. A count is required after `swarm`/`line`/`column`/
`drip` and rejected after `armored`/`flank`, whose member count the shape itself fixes; both are
parse errors, like any other malformed token. A rank token cannot immediately follow a spawn-type
token (rank always comes first), cannot immediately precede the `e` spacer, and two rank tokens
cannot appear back to back - all three are parse errors.

Parsing and instantiation are deliberately separate:

- `WaveScript.parse(tokens, defaultRank, catalog)` turns a token string into a `WaveContent`
  against a wave's default `td.enemy.Rank` and a given `td.enemy.EnemyCatalog`. No `GameWorld`
  involved, so it is trivially testable. Every non-reserved token is looked up the same way
  regardless of whether it names a built-in or a per-level custom/cloned definition — there is
  no separate syntax for the two, only whether the id happens to be registered in the catalog
  passed in. Resolving an id against `defaultRank` (or a slot's own rank-token override) already
  applies `RankedEnemy`'s fallback rule (`td/enemy/CLAUDE.md`), so a slot's `EnemyDefinition` and
  effective `Rank` are both fully resolved by the time `WaveContent` exists - `Wave.spawn()`
  needs no further rank logic. `WaveScript.RESERVED_TOKENS` — the spacer, six spawn-type
  keywords and five rank keywords — are recognized before any catalog lookup; `EnemyCatalog
  .register` rejects an id that collides with one. A token this can't recognize as one of those,
  a registered id, or an integer repeat count **fails the parse** with a `GameStartupException`
  — a wave the author did not write is content corruption, and recovering from it silently
  produced a level that was subtly not the authored one. Blank tokens are whitespace rather than
  content and are skipped, which is what lets `"".split(" ")` and any run of spaces parse
  cleanly.
- `WaveContent` is the parsed result: one `WaveSlot` per spawn slot, in order, repeat counts
  already flattened. `WaveSlot` is a closed pair - `EnemySlot(EnemyDefinition, Rank, SpawnShape)`
  for a real enemy, `EmptySlot()` for the spacer, which keeps its slot (it counts toward spawn
  *timing*) but is excluded from `enemyCount()`/`enemySet()`. `enemyCount()`/
  `enemyCount(definition)` count **members, not slots**: a slot whose `SpawnShape` holds 4
  members counts as 4, not 1 - this is what `GameWorld.startWave` seeds a wave's alive count
  from, so it gates when a wave is declared cleared. `WaveContent.rankFor(definition)` is the
  preview panel's way to ask "what rank did the slot spawning this resolve to" without spawning
  anything.
- `Wave` holds that content and does the world-bound instantiation in `spawn()`, **not in its
  constructor** — `shape.members()` `DefinedEnemyMob`s for an `EnemySlot` (zero for a plain
  `EnemySlot` is impossible; `SpawnShape.normal()`'s one member is the floor), nothing at all
  for an `EmptySlot`. A member's slot position is this slot's index plus its member index scaled
  by `shape.delaySpacingSlots()` (zero except for Column/Drip), and its formation offset comes
  from `shape.spread()` (a `SpawnSpread`) as a `Vec2` relative to the mob's own spawn-facing
  direction (`x` forward, `y` lateral) - `td.enemy.AbstractEnemyMob` is what turns that into a
  fixed world-space vector, once, at construction; `Wave`/`SpawnSpread` never touch the path's
  facing at all. `spawn()` is a factory: each call builds a fresh set bound to the path
  installed at that moment, so calling it twice puts two copies of the wave on the board.
  `enemyCount()`/`enemySet()` come from the content and need no spawn, which is what lets the
  preview panel describe a wave before it runs.
- **A member's health and bounty come from its own slot's resolved `EnemyDefinition`, not from
  `Wave` itself.** `Wave` carries no health/price of its own any more - `spawnShaped` reads
  `definition.baseHealth()`/`.price()` straight off the slot, applies the shape's own
  `healthMultiplier()`/`bountyShares()` on top, and - if `shape.traitOverride()` is present
  (`armored`'s case) - composes it onto the definition via `EnemyDefinition
  .withAdditionalTraits` before building any mob from it, so every member of a shaped slot
  spawns from the same (possibly trait-augmented) definition.
- **A `Wave` carries its own `scatterSeed`**, one `long` fixed at construction and reused by
  every `spawn()` call. `GameEngine.loadLevel` derives it from the level's name and the wave's
  index, so the same level and wave scatter identically on every run and every machine.
  Each slot draws from `RandomSource.seeded(scatterSeed * 31 + slotIndex)`, **not**
  `GameWorld.random()` — that generator is shared with tower targeting, so a formation's shape
  would otherwise depend on how many towers happened to fire first before the wave spawned.
  A leaked swarm member still costs exactly one life, the same as any other mob reaching the
  path's end — a swarm of 3 leaking all three costs 3 lives for one spawn's worth of bounty and
  health, which is a deliberate consequence of keeping the leak penalty per-mob rather than
  scaling it by the shape.
- **Deferring the spawn is what makes a level load one atomic publication.** An `EnemyMob` binds
  to `GameWorld`'s installed path when it is built, so spawning in the constructor forced
  `GameEngine.loadLevel` to install the path before building the waves — two writes where
  `LoadedLevel` needs one. See `td.util.LoadedLevel`.
- `WaveProgress` is how a caller reads the wave index and the wave count together. They are
  correlated (the index counts into the list the count measures), so `GameEngine.waveProgress()`
  builds one from a single `LoadedLevel` snapshot; combining `getCurrentWaveIndex()` with
  `getWaveCount()` is two reads and can straddle a level change.

### Grammar

`WaveScript.parse(tokens, defaultRank, catalog)` turns a space-separated token string into a
`WaveContent` — an ordered, `GameWorld`-free slot list with repeat counts flattened, each slot's
`EnemyDefinition` and `td.enemy.Rank` already resolved. `Wave` holds that content and does the
world-bound instantiation in `spawn()`, called when the wave starts — never in its constructor,
which is what lets a level install in one write (root `CLAUDE.md` §3, `LoadedLevel`).

- **Every enemy spawns at one of five named `Rank`s — Grunt, Soldier, Veteran, Elite, Boss — never
  a plain number, anywhere a wave author or the player can see it.** A wave declares its slots'
  default rank; a slot's own leading rank keyword overrides it. A rank an enemy kind doesn't
  define is not an authoring error — it silently resolves to that enemy's own highest defined
  rank instead (`td.enemy.RankedEnemy`). Health, bounty and a kill's score all scale with rank,
  authored per enemy rather than read off a wave-wide number.
- A count applies to the token immediately following it and resets to 1 afterward:
  `"3 s e 4 c"` = three Squares, one spacer, four Circles. The spacer is a token like any
  other, so a count works on it too — `"4 e"` is four spacers, exactly like `"e e e e"` — there
  is no separate rule for it. Before a rank or spawn-type keyword (below) a count repeats the
  whole ranked-and/or-shaped slot; immediately after a spawn-type keyword it sets that slot's
  member count instead — the same rule, applied to whichever kind of token follows.
- A small, closed set of tokens — the `e` spacer, six spawn-type keywords naming a `SpawnShape`
  (`WaveSlot`'s companion value describing how many members a slot spawns, and how) and five
  rank keywords naming a `Rank` — is recognized before any catalog lookup. Every other token
  resolves against the `EnemyCatalog` identically whether it names a built-in or a per-level
  definition; there is no separate syntax for the two. `EnemyCatalog.register` rejects an id
  colliding with a reserved token, so a level can never silently shadow the grammar.
- An unrecognized token is an authoring error and **fails the parse** with a
  `GameStartupException`. Whitespace is not a token: blank entries are skipped.

`GameEngine.loadLevel` builds one `EnemyCatalog.builtIn()` per level load, registers that level's
own `LevelDefinition.customEnemies()` into it, and passes the result to every wave's `parse` call -
see `td/level/CLAUDE.md` and `td/enemy/CLAUDE.md`.

## Multiple paths and rounds

A `td.level.LevelDefinition` owns a `List<PathDefinition>` (minimum one), not one path and one
wave list. Each `PathDefinition` is a lane's own corners, `PathSmoothing`, waves, `PathColor`
and speed multiplier - `PathDefinition.of`/`.smoothed` are the required shape (corners, waves);
`.withColor`/`.withSpeed` are optional fluent copies defaulting to white and `1x`, the same
"with"-copy shape `td.util.LoadedLevel` itself already uses, rather than constructor parameters
every path would otherwise have to name.

**Every path in a level must define the same number of waves - `LevelDefinition`'s compact
constructor enforces it.** A level's waves run as *synchronized rounds*: starting round `N`
spawns every path's wave `N` together, via `LoadedLevel.wavesAt(N)` (one `Wave` per path, in
path order), and the round is cleared only once every path's enemies from it are gone. This
reuses `GameEngine`'s single wave counter and ready-gate unchanged - `EnemyRoster` already
tracks one alive count shared across whatever it was handed, so seeding it from the *sum* of
every path's `Wave.enemyCount()` for the round (`GameWorld.startWave(List<Wave>)`) is what makes
"the round is cleared" wait for every lane automatically, the same mechanism that already made a
multi-member shaped slot count correctly before multiple paths existed at all.

Independent per-path progression - path A on round 6 while path B is still on round 3, each with
its own ready-gate - was considered and rejected: it would need a second per-path gate and a
redesigned wave-info HUD, and it drops the very invariant (`LevelDefinition`'s same-round-count
check) that makes "start the next round" still mean one button, one counter.

**An enemy mob binds to its own path by index, not to "the" path.** `SpawnParameters.pathIndex`
(defaulting to `0`) is resolved once, in `AbstractEnemyMob`'s constructor, against
`GameWorld.level().pathAt(index)` - not cached anywhere else. `Wave` carries its own `pathIndex`
and `speedMultiplier` (that path's `speedMultiplier()` already composed with its own
`WaveDefinition.speedMultiplier()`, resolved once by `GameEngine.loadLevel`); `spawnShaped`
multiplies that into `SpawnShape.speedMultiplier()` before handing it to `SpawnParameters` -
**this needs no new mechanism**, since `SpawnParameters.speedMultiplier()` is already stored as
`DefinedEnemyMob.shapeSpeedMultiplier` and reused by every `doDamage` speed recompute, the exact
thing that already keeps a `SpawnShape.boss()`'s slow from being wiped by the first hit it takes.
An ability-driven spawn (`SpawnEnemiesAction`, the Warden's egg) passes its parent's own
`getPathIndex()` rather than defaulting to path 0, so a hatch stays on the path its parent walked
- see `td/enemy/CLAUDE.md`.

**`EnemyMob.getProgression()` is a fraction of a mob's own path length, not raw pixels**,
specifically because `FurthestAlongPathSelector` compares it across candidates that can now be
on different paths of different total lengths - comparing raw `distanceIntoLap` would rank
whichever path happens to be longer as "further along" regardless of which mob is actually
closer to leaking.

Buildability still means exactly what it always meant - no tower on any path - just unioned:
`GameEngine.markUnbuildableCells` calls `PathCoverage.unbuildableCells` once per path and
disables the union of every returned cell. `PathCoverage` itself is unchanged; it has never
known how many paths exist.

`PathColor` (plain 0-255 RGB, no `java.awt` dependency) is real per-path *content*, not a
`Palette` role - `Palette` names the game's own fixed design language (`td/ui/CLAUDE.md`), while
a path's color is level-authored data the same way an enemy's health or a wave's price is. It
reaches the renderer as a field on `td.ui.render.PathMarkerDraw`, combined at paint time with the
`PathMarkerBrightness` (`STATIC`/`MOVING`) role the two marker layers have always had - `Palette`
itself no longer has path-marker cases.

## Wave start broadcast

`WaveAnnouncer`/`WaveStartListener` are just the "a wave started" hub, kept separate from
the economy and the rosters despite once living bundled with them in `GameWorld`.
`SonarTower` is the only subscriber, dropping the hit markers its scan left on the previous
wave's enemies. Its listener list is `CopyOnWriteArrayList` because it
is fired from the `game-loop` thread — see the root `CLAUDE.md` §3 (Threading).
