# Review: testing and balancing tools

**Status:** written with the first version of `FEATURE-balance-verification.md`, 2026-10-13. It is
part 0 of that feature: what the tools could and could not tell before, what they tell now, and what
is still missing, ranked. The feature doc carries the findings about the game itself.

## The seven questions, and what answers each

| Question | Before | Now (`td.balance.BalanceReport`) | Still weak |
|---|---|---|---|
| Is the level neither easy nor hard? | Lives lost of a fixed loadout, unlimited credits | Win rate and lives lost with real credits and lives, for a naive and a competent player; the **margin**: the highest enemy health a player still holds, seed by seed | The verdict leans on one player model; the strong player shows how far off it is |
| Does positioning matter? | Nothing | Competent player on its chosen cells against the same player on random cells, paired by seed; the lab's best cells per tower type, and how much they overlap | Random cells are only one kind of careless |
| Do upgrades matter? | Lives saved per 100 credits, unlimited credits (`PurchaseHarness`, `BuildHarness`) | Competent player with and without upgrades, paired; which nodes it buys; every node's factor in a lab duel; the share of spending on upgrades in each third of the level, and the nodes the strong player picks far more or less often | Nodes no play buys are listed, not judged dead: a player model that never wants them and a node nobody should want look the same |
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

- **The players are models.** The strong player measures the competent one: after the competent
  player's constants were swept against it (the most it believes one node multiplies a tower by, now
  x1.5, and how much better a purchase must be before it saves for it, now x2), it holds x1.07 the
  competent margin on Curly Path, x1.00 on Zigzag Path and x1.12 on Twisted Hourglass, 12 seeds. Before
  the sweep the competent player chased lab outliers among the nodes and lost Zigzag Path's last round,
  where building only would have held. A verdict is about the game only as far as it holds for both.
- **The lab's probes never die**, so a probe round measures where a tower stands, not what it does to
  enemies that die, turn invisible when hit or split. Those are what duels are for. Duels give every kind
  the same health, so a flat mechanic (a shield of fixed points, flat damage reduction) weighs more in a
  duel than in a real round.
- **What a tower does for the others is measured on one partner.** The lab puts a Sniper beside each tower
  in a probe round and reports the share more it lands, bare and per node (Radar's Mark on Sweep +70-90%,
  the Mortar's Cryo Shells and Tar, the Hive's Deep Freeze and the Scorcher's Searing Flame +8-17%). The
  competent player counts half of it, the strongest of each type per tower: counting all of it held less
  on Zigzag Path and Twisted Hourglass, and half held as much as ignoring it, so on the built-in levels
  support does not decide a margin. Overkill is still invisible to it.

## Standard practices, assessed

Each with what it would unblock here, what it costs, and the decision (⭐ = kept or adopted on this branch;
`FEATURE-balance-verification.md`, question 9).

| Practice | Status | What it unblocks | Cost | Recommendation |
|---|---|---|---|---|
| Telemetry and event logs | Built, tool-side (`RunRecorder`) | Questions nobody thought of: the per-enemy ends and per-round numbers are kept in the record | Done; widening it is per-question | ⭐ keep; add the four measures above as questions need them |
| Deterministic replay | Built for headless play (action log, `ScriptedPlayer`), and used by `--compare` to play the saved actions back on a changed game | Reproducing a result; telling what a change does from how a player adapts to it | Done headless; in the live game it needs `TODO.md`'s command queue | ⭐ keep; leave the live game alone |
| Heat maps | Built in the HTML report | Where enemies die, where the player builds, each tower's best cells | Done | ⭐ keep; add leaks and damage-landed maps when asked |
| Timelines per wave | Built: per round, and every 20 ticks (enemy health on the board, the furthest enemy, credits) | The difficulty curve: closest call, leaks, enemy health per round and how much of it stacks up at once | Done | ⭐ adopted: drawn per level in the HTML, the per-round peak on the board in the text |
| Theory-crafting tables | Built, measured (`TowerLab`) | Every tower by cell, kind and node before any play | Done | ⭐ keep; it caught Absolute Zero |
| Economy modelling | Built: per round, credits idle, bounty earned, spent on towers and on upgrades, and the price of what the competent player would buy next; credits over time in the timeline | Whether credits pile up or run dry, and the rounds where the next purchase costs more than two rounds of bounty | Done | ⭐ adopted |
| Parameter sweeps | Built for the level's knobs: enemy health, starting credits, lives, bounty, spawn spacing (`Rules`) and the pause between rounds (pacing); `--sweep KNOB` shows the competent player's margin and wins at five values | How fast an outcome moves with a number | Done; a tower or enemy number is changed in code and checked with `--compare` | ⭐ adopted for level knobs |
| Regression baselines | Built (`--save`, `--compare`) | Noticing that an unrelated change moved balance | A committed baseline goes stale with every balance change | ⭐ keep baselines in `target/`, not in the repository (open question 4 A) |
| Content coverage reports | Built: the recorder counts every effect's share of enemy time and how often it takes hold, and every enemy ability used; the report lists what no play of any player triggered | Which content no play touches | Done | ⭐ adopted, with effect uptime |
| Invariant and fuzz runs | Built as a test: `RandomPlayInvariantTest` plays every level with random builds, upgrades and sells | Every round clears, credits never go negative, every enemy that walks in ends once, a sold tower leaves its cell | Done; more invariants as rules appear | ⭐ keep in `mvn verify` |
| Scenario files | Partly (action logs) | Anyone loading the same situation | A file format for logs exists; the dev panel loader does not | Later, with file-based levels |
| Debug overlays | Not built | Seeing ranges, targeting lines, pool values on the live board | UI work in `td.ui` | Dropped |
| Time control | Exists (pause, step, speeds); no rewind | Going back to a moment | Rewind needs snapshots or replay; replay exists headless | Drop |
| Recorded human sessions | Not built | Human playtests in the same checks | Needs the command queue to record live input | Later, after the command queue |

## Gaps, ranked by what they unblock

1. ✅ **The competent player's opening.** Swept against the strong player (see above). Twisted
   Hourglass's round 1 still costs 3 of its 4 lives, but now for the strong player too, so it is the
   level's, not the player's. Its own build played back now holds what it does re-deciding, within the
   seeds' noise.
2. ✅ **What a tower does for others.** Priced by a partner Sniper in a probe round and counted by the
   competent player at half (see above).
3. ✅ **Upgrades by phase**, and dead or dominant nodes across players: the share spent on upgrades per
   third of the level, node pick gaps between the strong and competent players, and the nodes no play buys
   (under COVERAGE).
4. **Same play for every player**, not only the competent one, if a change ever moves the naive player's
   verdict in a way the adaptive comparison cannot explain.
5. **The old harnesses.** `BalanceHarness` is superseded and is retired on this branch. `PurchaseHarness` and
   `BuildHarness` stay: they compare one purchase or build against another at equal spend, which the report
   does not.
