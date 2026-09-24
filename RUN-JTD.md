# Pending `run-jtd` checks (enemy stats)

Deferred visual and balance checks from implementing `docs/plans/enemy-stats.md`, to run in one
batch. Each entry says what to do and what should be true.

## 1. Phase 1 - balance sanity after the stat core (`td.BalanceHarness`)

- Run `td.BalanceHarness` on one built-in level (e.g. Curly Path) with a mixed loadout.
- Expect: numbers in a sensible range, not equal to before. Armored Elite/Boss now apply armor
  before plating, and a shield multiplies with armor, so they take slightly different damage.

## 2. Phase 2 - crit rolled by the target (`td.BalanceHarness` + board)

- Run `td.BalanceHarness` on the same level and loadout as entry 1.
- Expect: Sniper still crits at about its crit chance; Cinder-burning targets crit about twice as
  often; Armored and Frenzied Boss never show a crit spark.
- On the board: a Sniper crit still draws its crit beam at the moment it fires.
