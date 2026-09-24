# `td.economy`

`EconomyLedger` is written from two threads: buy/sell on the EDT, kill/leak on `game-loop`.

- `apply`, `doPay` **and `startEconomy`** assign inside `synchronized (this)` and fire
  `economyChanged` **outside** it, because listeners re-enter the ledger and touch Swing.
  `EconomyLedgerTest.concurrentEconomyEventsDoNotLoseUpdates` guards this.
- The listener list stays a `CopyOnWriteArrayList`. `economy` is `volatile`.
- Callers wanting more than one of credits/score/lives call `state()` once; separate getters
  are separate reads.
- `canPay` is advisory. `doPay` is the atomic check-and-charge; just call it and check the
  result.
- Every event is one `EconomyDelta` (`kill(bounty, score)`, `leak(penalty)`, ...) applied through
  `apply`, which fires exactly one notification. A new kind of event is a new `EconomyDelta`
  factory, never a new mutating method on the ledger.
