# `td.level` — level definitions and the level catalog

Read the root `CLAUDE.md` first; this file only covers what is specific to this package.

`LevelDefinition` is a level's authored content: name, description, board size, its own starting
credits/lives, a `List<td.wave.PathDefinition>` - a level's one or more enemy paths, each
with its own corners, `PathSmoothing`, waves, `PathColor` and speed multiplier - and this
level's own enemy roster on top of the built-ins, split across two lists: `List<td.enemy
.EnemyDefinition> customEnemies` for a single-rank enemy (the common case) and
`List<td.enemy.RankedEnemy> customRankedEnemies` for one authored with a real rank ladder.
There is no separate `path`/`waves`/`smoothing` field for "the" path; even a single-lane level
is a one-entry `paths` list.

Both enemy lists default to empty via `LevelDefinition.of`, each grown by its own fluent
`withCustomEnemies`/`withCustomRankedEnemies` copy - see `BuiltInLevelCatalog`'s Reaver (Wild
Bezier Sweep's one level-authored enemy) for the ranked pattern: a level-authored enemy is
exactly as free to be a full `RankedEnemy` ladder as a built-in is, built the same way
(`RankedEnemy.startingAt(...).thenAt(...)`). `GameEngine.loadLevel` registers every entry of
both lists into that level's fresh `td.enemy.EnemyCatalog` right after the built-ins, before any
wave's tokens are parsed, so a wave-script token names a custom id exactly like it names a
built-in one - `td.wave.WaveScript` resolves every token against whichever catalog is in scope,
with no separate syntax for the two (see `td/wave/CLAUDE.md` and `td/enemy/CLAUDE.md`).

**Every path in a level must define the same number of waves.** A level's waves run as
synchronized rounds - starting round `N` spawns every path's wave `N` together, and the round
clears only once every path's enemies from it are gone (see `td/wave/CLAUDE.md`'s round model,
which is where the runtime side of this lives). `LevelDefinition`'s compact constructor checks
this and throws on a mismatch, the same way its own "at least one path" check already does -
both are plain illegal-argument errors, since this is a hand-authored-content mistake, not
loaded content (`GameStartupException` is for the latter).

`LevelDefinition.of(name, width, height, paths)` is the required shape every level has - no
description, $100 starting credits, 5 starting lives, the traditional defaults. A level that
needs any of those calls the matching fluent `withDescription`/`withStartingCredits`/
`withStartingLives` copy instead - see `BuiltInLevelCatalog.WILD_BEZIER_SWEEP` for the pattern
with a multi-path level. Two named factories cover authoring a level with exactly one path, both
built on top of `of`:

- `LevelDefinition.unsmoothed(name, description, width, height, path, waves, credits, lives)` -
  one path, no smoothing. The common case.
- `LevelDefinition.singlePath(..., smoothing)` - one path, an explicit `PathSmoothing`.

A path's color and speed multiplier are optional and default to white/`1x`, set via
`PathDefinition`'s fluent `withColor`/`withSpeed` rather than constructor parameters - see
`td/wave/CLAUDE.md` for why, and for the full runtime model (`PathRuntime`, per-mob path
binding, buildability unioning) this package's own `LevelDefinition` feeds into via
`GameEngine.loadLevel`.

`BuiltInLevelCatalog` is the only `LevelCatalog` implementation today - `levels()` just lists
three `LevelDefinition` constants. **Each level's own definition lives in its own package-private
class**, named for the level (`ClassicLoopLevel`, `ZigzagGauntletLevel`, `WildBezierSweepLevel`),
following `td.enemy.BuiltInEnemies`'s shape (a stateless holder, private constructor, package-
private `static final` fields) - so a change to one level's path, waves or numbers touches one
file instead of a shared one every level lives in. A level-authored enemy belongs beside the
level that authors it, not in the catalog: `WildBezierSweepLevel` also holds its Reaver
`RankedEnemy`, exercising two paths with distinct colors and one called-out faster lane. Content
(which enemies, what a wave says, a level's numbers) belongs in these classes and in
`README.md`'s level table, not in this file.
