# Review: testing and balancing tools

**Status:** written with the first version of `FEATURE-balance-verification.md`, 2026-10-13. It is
part 0 of that feature: what the tools could and could not tell before, what they tell now, and what
is still missing, ranked. The feature doc carries the findings about the game itself.

## The seven questions, and what answers each

| Question | Before | Now (`td.balance.BalanceReport`) | Still weak |
|---|---|---|---|
| Is the level neither easy nor hard? | Lives lost of a fixed loadout, unlimited credits | Win rate and lives lost with real credits and lives, for a naive and a competent player; the **margin**: the highest enemy health a player still holds, seed by seed | The verdict leans on one player model; the strong player shows how far off it is |
| Does positioning matter? | Nothing | Competent player on its chosen cells against the same player on random cells, paired by seed; the lab's best cells per tower type, and how much they overlap | Random cells are only one kind of careless |
| Do upgrades matter? | Lives saved per 100 credits, unlimited credits (`PurchaseHarness`, `BuildHarness`) | Competent player with and without upgrades, paired; which nodes it buys; every node's factor in a lab duel | By phase of the level: not yet |
| Do enemy types force variability? | Nothing | The counter matrix: every tower against every kind at every rank the level sends, from lab duels; leaks per play by kind | No per-kind scenario played by the player yet |
| Is any option dead or dominant? | Nothing | `--variety`: each tower alone and each tower banned, by margin; damage share and tower counts; nodes far from the rest in the lab | Nodes dead or dominant across all players: only for the competent one |
| Is the answer trustworthy? | One seed, or a mean with no interval | Every number with its 95% interval; Wilson intervals for win rates; differences paired by seed; a test that a seeded play repeats exactly | Few seeds make wide intervals; 20 is the default |
| Did this change move any of the above? | Diff two text files by eye | `--save NAME` then `--compare NAME`: headline numbers every time, everything else only when it moved beyond the seeds' noise, lab numbers when they moved at all; and the same play: the saved competent actions played back on the changed game, with their own margin | Only the competent player's actions are kept for the same play |

## The toolset, layer by layer

| Layer | What exists | Verdict |
|---|---|---|
| Rules | `GameEngineTest` and unit tests, headless, `doTick` driven | Unchanged; the right place for rules |
| Manual testing | Dev panel, `n`/`x`/`c` keys, game speed | Still the fastest way to *see* a situation; the headless tools do not replace it |
| Looking | Inspectors, `PreviewBoard`/`PreviewEnemy` renders | Unchanged |
| Driving the live window | `run-jtd` Driver | Unchanged; only for Swing wiring |
| Balance, old | `BalanceHarness` (fixed loadout, one seed), `PurchaseHarness`, `BuildHarness` (unlimited credits and lives) | Superseded for verdicts; `BuildHarness` still answers "this build against that one" at equal spend, which the report does not |
| Balance, new | `td.balance`: the players, the lab, the report, compare, HTML | See below |
| Performance | `PerformanceHarness` | Out of scope, unchanged |

What `td.balance` is made of, each small enough to read on its own:

- `LevelPlay` plays a level as a person does: real credits and lives, bounty the only income, the player
  acting before each round and every second during one, the next round called after a 3-second pause.
  Lives never run out, so one play gives both the verdict (the leak that would have lost) and the degree
  (every leak after it). `Rules` makes enemies tougher or credits scarcer for a margin.
- `RunRecorder` watches the play through the engine's public state only.
- Players: `NaivePlayer` (random affordable tower, random cell that reaches a path, never upgrades);
  `CompetentPlayer` (reads the next rounds, estimates for each coming enemy how likely the army is to kill
  it, buys the most enemy health saved per credit, with styles for random cells, no upgrades, one tower
  type, one tower banned); `StrongPlayer` (rehearses each round under a dozen ways of spending and plays
  the best); `ScriptedPlayer` (plays back an action log).
- `TowerLab`, the theory-crafting table, measured instead of computed: probe rounds rank every cell for
  every tower; duels measure every tower against every kind at every rank and every upgrade line.
- `Margin`, `Estimate`, `LevelRun`, `ResultTable`, `BalanceReport`, `HtmlReport`.

## Observability

What a play now reports, with no change to the engine: every enemy's end (kind, rank, full health,
bounty, killed or leaked, where, how far along its path); per round the credits idle at its start,
bounty earned, credits spent, lives lost, enemy health that walked in, leaks, how close the furthest
enemy came, and damage per tower type; every action with its round and offset; every tower at the end
with its damage, kills, XP and nodes.

The one engine change is `PlaytestRules.setEnemyHealthPercent`, a playtest rule like
`setUpgradeGatesIgnored`, applied where every mob is built; the dev panel can expose it later.

Still not observable, ranked by what it would unblock:

1. **Damage wasted**: overkill, and damage into shields. Says whether a tower is strong or just first in
   line. Needs the hit result per tower, which `AbstractTower.dealDamage` already has.
2. **Time an enemy spends in each tower's reach**: separates "badly placed" from "weak". Can be computed
   in the recorder from positions and ranges, at a per-tick cost only the tool pays.
3. **Effect uptime** (how long a freeze, burn or Vulnerable held): says whether a control or debuff node
   works. Readable from `ActiveEffects` per tick, also tool-side.
4. **Who damaged whom**: the damage split per enemy kind per tower type. A tally keyed by attack origin.

## Setup: reaching a situation without replaying everything

- An **action log** is a scenario: a list of `round offset action` lines (`3 -1 build SNIPER 4 10`,
  `7 140 upgrade 4 10 base.attune`). `ScriptedPlayer` plays one back exactly against the same game and
  seed, and still plays it on a changed game (an action waits until it is affordable).
- **Lab levels** are any level with its rounds replaced by one round of probes or of one kind: built in
  code from the level itself, so a new level gets them for free.
- **Rules** change enemy health and starting credits per play.

Missing: loading a log or a scenario file in the dev panel; a level's state mid-round as a file (the
engine has no snapshot, and replaying to a round takes well under a second, so the log is enough).

## Trust

- A seeded play repeats exactly, and a log replays to the same record: `LevelPlayTest` holds both.
- The engine and the player draw from separate seeded sources, so a choice never shifts what the game
  rolls.
- Every report number carries a 95% interval. Players are compared seed by seed (ratio of margins, with an
  interval from the per-seed log ratios), which takes out the luck the seeds share.
- The lab repeats exactly, so any change in it is the change.
- At 20 seeds a margin is known to about ±5% for the competent player and ±10% to ±25% for the naive one.

What the numbers rest on, and how far they hold:

- **The players are models.** The strong player measures the competent one: it holds x1.02 the
  competent margin on Curly Path, x1.21 on Zigzag Path and x1.16 on Twisted Hourglass, where the
  competent player's opening is wrong. A verdict is about the game only as far as it holds for both.
  One sweep of the competent player's kill estimate (how soft its line between leaking and dying is)
  raised its margins by up to x1.12; tuning it against the strong player is a loop the tools support.
- **The lab's probes never die**, so a probe round measures where a tower stands, not what it does to
  enemies that die, turn invisible when hit or split. Those are what duels are for. Duels give every kind
  the same health, so a flat mechanic (a shield of fixed points, flat damage reduction) weighs more in a
  duel than in a real round.
- **The competent player values a tower by what it lands**; what it does for the others it knows only for
  the Beacon. A slow or a debuff that makes others hit harder is invisible to it, and so is overkill.

## Standard practices, assessed

Each with what it would unblock here, what it costs, and a recommendation (⭐ = recommended now).

| Practice | Status | What it unblocks | Cost | Recommendation |
|---|---|---|---|---|
| Telemetry and event logs | Built, tool-side (`RunRecorder`) | Questions nobody thought of: the per-enemy ends and per-round numbers are kept in the record | Done; widening it is per-question | ⭐ keep; add the four measures above as questions need them |
| Deterministic replay | Built for headless play (action log, `ScriptedPlayer`), and used by `--compare` to play the saved actions back on a changed game | Reproducing a result; telling what a change does from how a player adapts to it | Done headless; in the live game it needs `TODO.md`'s command queue | ⭐ keep; leave the live game alone |
| Heat maps | Built in the HTML report | Where enemies die, where the player builds, each tower's best cells | Done | ⭐ keep; add leaks and damage-landed maps when asked |
| Timelines per wave | Built per round, not per tick | The difficulty curve: closest call, leaks, enemy health per round | Per-tick health on the board is a recorder change | Later |
| Theory-crafting tables | Built, measured (`TowerLab`) | Every tower by cell, kind and node before any play | Done | ⭐ keep; it caught Absolute Zero |
| Economy modelling | Partly (idle credits, bounty per round) | Whether credits pile up or run dry | Bounty against price curve: a table from the catalog | Later |
| Parameter sweeps | Partly (enemy health, starting credits via `Rules`) | How fast an outcome moves with a number | Any other number needs an override mechanism; content is Java constants | Later, and only for level knobs; a tower number is changed in code and checked with `--compare` |
| Regression baselines | Built (`--save`, `--compare`) | Noticing that an unrelated change moved balance | A committed baseline goes stale with every balance change | ⭐ keep baselines in `target/`, not in the repository (open question 4 A) |
| Content coverage reports | Partly (nodes bought, towers built, kinds leaked) | Which content no play touches | Counting effects triggered needs uptime (above) | Later |
| Invariant and fuzz runs | Built as a test: `RandomPlayInvariantTest` plays every level with random builds, upgrades and sells | Every round clears, credits never go negative, every enemy that walks in ends once, a sold tower leaves its cell | Done; more invariants as rules appear | ⭐ keep in `mvn verify` |
| Scenario files | Partly (action logs) | Anyone loading the same situation | A file format for logs exists; the dev panel loader does not | Later, with file-based levels |
| Debug overlays | Not built | Seeing ranges, targeting lines, pool values on the live board | UI work in `td.ui` | Drop for this feature; it is a dev-panel feature of its own |
| Time control | Exists (pause, step, speeds); no rewind | Going back to a moment | Rewind needs snapshots or replay; replay exists headless | Drop |
| Recorded human sessions | Not built | Human playtests in the same checks | Needs the command queue to record live input | Later, after the command queue |

## Gaps, ranked by what they unblock

1. **The competent player's opening.** It loses 3 of Twisted Hourglass's 4 lives in round 1 on every seed;
   the strong player opens with Snipers and loses none. Its kill estimate gives up on a tough enemy it
   cannot certainly kill. Tuning it against the strong player is a loop the tools now support. A second
   lead: its own build, played back unchanged, holds more than it does re-deciding against tougher
   enemies (400% against 351% on Curly Path, 292% against 235% on Zigzag Path).
2. **What a tower does for others.** Slows, Vulnerable, Priority and the like are worth nothing to the
   competent player. A lab duel with a partner tower beside the measured one would price them.
3. **Upgrades by phase**, and dead or dominant nodes across players (`FEATURE-balance-verification.md`,
   later item 2).
4. **Same play for every player**, not only the competent one, if a change ever moves the naive player's
   verdict in a way the adaptive comparison cannot explain.
5. **The old harnesses.** `BalanceHarness` is superseded; `PurchaseHarness` mostly so. Retiring them is one
   less thing to keep working; it is the user's call.
