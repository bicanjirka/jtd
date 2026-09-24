# `td.level`

- `LevelDefinition.of(name, width, height, paths)` plus `withDescription`/`withStartingCredits`/
  `withStartingLives`/`withCustomEnemies`/`withCustomRankedEnemies`. `unsmoothed(...)` and
  `singlePath(...)` are the one-path shortcuts. Even a single-lane level is a one-entry
  `paths` list.
- The compact constructor requires at least one path and an equal wave count on every path. It
  throws a plain illegal-argument error: this is authored code, not loaded content.
- Per-level enemies are registered into that level's fresh `EnemyCatalog` before its waves are
  parsed, so wave tokens name them like built-ins.
- Each built-in level lives in its own package-private holder class (`CurlyPathLevel`, ...),
  together with any enemy only that level uses. `BuiltInLevelCatalog.levels()` just lists them.
  Level content belongs there and in `README.md`, not here.
