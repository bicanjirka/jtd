# 0017. The domain packages form one cycle through `GameWorld`; only boundaries that hold are enforced

- Status: Accepted
- Date: 2026-10-28

## Context
The 2026-10 design review found nine packages in one dependency cycle: `cell`, `effect`, `enemy`,
`projectile`, `stat`, `tower`, `util`, `wave`, `zone`. Three things close it:
- `td.util` holds both thread primitives and the world, which owns every roster.
- `td.tower.targeting` is shared by projectiles and zones.
- `td.wave` holds both path geometry and wave scripts.

Root `CLAUDE.md` said "`projectile` never depends on `tower`", which was false through targeting.
Checkstyle checked only the AWT/Swing ban, through a package list that had already missed two
newer packages (`save`, `term`).

## Decision
- No package moves. Targeting stays under `tower`, and the rule is reworded: "`projectile` uses
  only `tower.targeting` from `tower`". It isn't enforced.
- The actors-and-world cycle is accepted. Breaking it needs narrow contexts, which 0004 rejects.
- Checkstyle enforces the boundaries that hold today:
  - `damage` imports nothing in `td` but `util`;
  - `stat` imports only `damage` and `util`;
  - `effect` imports neither `tower` nor `enemy`;
  - AWT and Swing are banned for all of `td` except `ui`, `Main` and `TowerDefense`, so a new
    package is covered without editing a list.
- Splitting `td.util` into primitives and the world, and splitting path geometry out of `td.wave`,
  stay in `TODO.md`. Each is worth doing only together with a full layer map.

## Consequences
- The layers in `docs/DESIGN.md` §2 describe intent. Only the listed boundaries are guaranteed.
- A new boundary worth keeping is added to `checkstyle-imports.xml`, not to prose.

## Rejected
- *Moving `td.tower.targeting` to `td.targeting`.* It touches dozens of files for a rule whose
  purpose already holds, since projectiles use only pure targeting filters.
- *A full layer map now.* It needs the moves above, and it buys guards, not cheaper changes.
