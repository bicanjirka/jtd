# 0004. `GameWorld` hands out collaborators; narrowing stops where it would be ritual

- Status: Accepted
- Date: 2026-09-17 (recorded); re-tested 2026-10-28

## Context
`GameWorld` was once a god object, `Context`, owning several unrelated jobs. The first split into
collaborators (board geometry, economy ledger, enemy, tower and projectile rosters, the wave
announcer) then routed all of them back through dozens of forwarding methods on the world. That
moved the coupling instead of removing it: no call site showed which part of the world it used.

Measured then, `td.tower` used eleven distinct world methods and `td.enemy` eight, not the
"three or four" an earlier note assumed. Re-measured during the 2026-10 design review: `td.tower`
still uses eleven, with zero to seven per file.

## Decision
- `GameWorld` hands its collaborators out (`economy()`, `enemies()`, `towers()`, …) instead of
  wrapping them, so a call site reads `world.economy().doPay(n)`.
- A consumer that needs one collaborator takes that type and never sees `GameWorld` (targeting and
  `BoardRenderer` take an `EnemyRegistry`).
- Narrowing stops where every implementation of a strategy would carry arguments it never uses for
  the sake of one of them. `UpgradeCondition.isSatisfied(Tower, GameWorld)` keeps the whole world
  on purpose.
- The one method that coordinates two collaborators, `startWave`, does so because order is its
  job: it seeds the alive count before announcing the start, so no listener sees a stale count.

## Consequences
- The dependency is legible at every use, and it can be counted.
- The actor packages depend on the world and the world on their rosters: one cycle by design
  (0017).

## Rejected
- *A narrow context interface per package.* Each would be about as wide as the world itself, which
  is what the measurement showed then and again in the re-test.
- *Forwarding methods on the world.* They hide who uses what.
