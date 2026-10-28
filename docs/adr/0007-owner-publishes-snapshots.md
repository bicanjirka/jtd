# 0007. The owning thread publishes; correlated state crosses as one snapshot

- Status: Accepted
- Date: 2026-09-17

## Context
The EDT once walked live domain objects while painting: it read enemy positions, health and death
flags, and tower damage and kill counts, all plain fields written on the game loop. The docs
claimed thread-safety from three mitigations, each correct and together insufficient:
- copy-on-write lists publish the *list*, not the objects in it;
- one lock in the economy covers only the economy;
- `invokeLater` publishes the *request* to repaint, not the state the repaint reads.

Without a happens-before edge those reads may see stale or, for 64-bit fields, torn values. That
is mostly harmless on x86 and wrong elsewhere, and the game must behave the same everywhere.

The first fix then got one case wrong. A tower's damage, range and cooldown were made separate
`volatile`s: each read was fresh, but the set wasn't coherent, so a tick could fire with the new
damage and the old cooldown.

## Decision
- Whichever thread owns a piece of mutable state publishes it; the other reads only what was
  published. Ownership runs both ways: the game loop owns the simulation and builds each frame,
  while the EDT owns the cells and recalculates tower stats.
- Fields that must agree with each other cross as **one immutable snapshot behind one `volatile`**,
  read through one accessor: `RenderFrame`, `TowerStats`, `LoadedLevel`, the economy state. Only an
  independent scalar (a kill count, the damage dealt) is a bare `volatile`.
- The test that decides between the two: must the fields agree with one another?
- Other parts of the same model:
  - `GameLoop.stop()` joins its thread before returning, so a level is never swapped out from
    under a tick.
  - Falling behind runs one tick and resyncs, never a burst.
  - Finished projectiles are collected and removed after the iteration: a copy-on-write list's
    iterator can't remove, and the exception would vanish into the loop's per-tick failure
    handling instead of failing at its call site.
  - `EconomyLedger` fires its listeners outside its lock, because they re-enter it.
  - `GameLoop.start()` is idempotent and restartable: a second call while running is a logged
    no-op, and a call after `stop()` resets the accumulators, the interpolation alpha, the tick
    counter and the circuit breaker, because returning to the menu stops the loop and the next level
    starts the same instance. `animationSeconds` alone stays monotonic, since it is wall-clock
    cosmetics, not level state. `run()` pins itself to the thread `start()` launched, so a stale run
    can't mistake the new run's flag for its own.
  - Tick speed is a plain multiplier.
- No tick tag in the logging MDC: it is thread-local, so most lines showed it empty. Code on the
  loop that cares logs the tick explicitly.

## Consequences
- A concurrent collection makes the collection safe, not its contents. The question is always "who
  owns this, and what do the other threads get instead?"
- Several separate `volatile`s that must agree are a defect. The 2026-10 review found two (the
  round state in `GameEngine`, the hover highlight on cells), both since fixed.

## Rejected
- *A dusting of `volatile`* on whatever is read across threads: it doesn't make correlated fields
  coherent.
- *Locks around domain objects*: they would put lock order into gameplay code.
