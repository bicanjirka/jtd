# `td.economy` — credits, score and lives

Read the root `CLAUDE.md` first. This package is small, but it is the one place in the
codebase where a plausible-looking simplification causes a real concurrency bug, so it gets
its own note.

## Shape

- `EconomyState` — credits/score/lives as one immutable snapshot.
- `EconomyDelta` — what a single game event changes, with `none()` as the identity and
  `plus` as the combinator. `kill(bounty)` and `leak(penalty)` are the two named events.
- `EconomyLedger` — owns the current state and the listener list.
- `EconomyListener` — the one-method notification interface.

## The rule that matters

**The ledger is written from two threads.** Buying and selling a tower happen on the Event
Dispatch Thread; a kill or a leak happens on the `game-loop` thread. So:

- `apply` and `doPay` compute the new state inside `synchronized (this)` and fire
  `economyChanged` **outside** the lock. Listeners re-enter the ledger and touch Swing —
  notifying while holding the lock is how this deadlocks.
- The listener list is a `CopyOnWriteArrayList` on purpose. Do not "optimize" it to
  `ArrayList`.
- `economy` is `volatile` so the unsynchronized readers (`getCredits`, `getScore`,
  `getLives`, `canPay`) see a consistent published snapshot rather than a torn one.

`canPay` is advisory only — it is a read with no lock held. The atomic check-and-charge is
`doPay`, which returns `false` if the player cannot afford it. Never write
`if (canPay(n)) { doPay(n); }`; just call `doPay` and check its result.

## Why deltas instead of separate mutations

`apply(EconomyDelta)` fires **exactly one** `economyChanged` per game event. The
`addScore`/`doReceive`/`removeLife` trio it replaced fired up to two notifications for a
single kill and none at all for a score change. If you add a new kind of economy event, add
a named factory to `EconomyDelta` and route it through `apply` — do not add a second
mutating method to the ledger.
