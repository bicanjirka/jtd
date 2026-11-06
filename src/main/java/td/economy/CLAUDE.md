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
- Every event is one `EconomyDelta` (`kill(bounty, score)` with rank-weighted score for real
  kills, `kill(bounty)` for 1:1, `leak(penalty)`) applied through `apply`, which fires exactly
  one notification. A new kind of event is a new `EconomyDelta`
  factory, never a new mutating method on the ledger.
- Free build is a mode, not an event, so its switch is a method here. Game and UI code asks `canPay`, `shortOf` and
  `refundOf`, which answer for it, and never compares a price to `credits()`. The balance tools' players never turn it on.
