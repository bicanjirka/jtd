# Feature Request: Balance Verification

**Status: draft.** Not sequenced against other requests; it is enabling infrastructure, and every
`TODO.md` entry that says "unbalanced placeholders" would be tuned with it.

## Summary

Check a change to any number in the game (an enemy, a tower, an upgrade node, a level) by playing
the game headlessly the way a player would, and report whether the game is still the game we want:
a level that is neither easy nor hard, where *where* you put towers matters, where upgrades matter,
and where the enemies in a wave force you to build differently. Today a balance change is judged
by one number, lives lost, from runs that are nothing like a real game. That number cannot see most
of what makes tower defence worth playing, and it has already led us to change numbers on noise.

It starts by reviewing the whole set of testing and balancing tools we have, because the problem is
larger than one metric: it is also what we can *observe* in a run, what we can *set up* without
replaying a level, and which standard game-design and game-testing practices we are missing.

## Current state (what exists today)

**Tooling.** Four headless tools exist and none is a test: `td.BalanceHarness` (one fixed loadout on
hand-picked cells, one seed), `td.PurchaseHarness` and `td.BuildHarness` (compare purchases and
builds), and `td.PerformanceHarness` (a budget check, out of scope here). `HarnessBoard` ranks
buildable cells by how much path lies within 3.5 cells of them and the harnesses fill the best cells
first. `FEATURE-playtesting-and-balance-tooling.md` shipped the first of these and says it makes
tuning cheap; it never said what to measure.

**The wider toolset** (what a person or an agent has to work with today):

| Layer | What exists |
|---|---|
| Rules | Unit tests, with `GameEngineTest` as the integration surface; headless, driven by `doTick`. |
| Manual testing | The hidden dev panel (`Ctrl`+`Shift`+`D`): set credits and lives, free build, infinite lives, upgrades that ignore their gates, spawn any wave script with a live preview and a choice of rank and path, skip or jump to a wave, restart, step one tick, kill or clear the board, grant XP to a tower, show the cell grid. The keys `n`, `x` and `c`. Game speed buttons. It helped manual testing a great deal, which is evidence that the right setup tool is worth more than a better report. |
| Looking | The tower and enemy inspectors, and the headless `PreviewBoard` and `PreviewEnemy` renders. |
| Driving the live window | The `run-jtd` skill's `Driver`: clicks, hover, text and state output, screenshots. |
| Balance runs | The four harnesses above. |
| Logs | `GameEngine` logs each round start and the outcome. |

**What can be observed in a run today.** What the code exposes without changes: lives, credits and
score through `EconomyListener`; a wave starting; per tower the damage dealt, the kills, XP and
a count of its purpose deeds; per enemy its active effects and health while it lives. A walk ending is
reported with its bounty, its entry order and whether it passed near a point. It is not reported
with *how* it ended (killed or leaked), the enemy's kind or rank, its health, or where or when. I
found nothing that records, over a run: when and where enemies die or leak; how long enemies spend
inside a tower's reach; how much of a tower's time is idle or its damage wasted; who damaged
whom; how long effects last; credits earned against credits spent or sitting unspent; what
the player (or an agent) decided and when; or any per-wave summary. Several measures a balance
question needs are therefore not observable at all, however many runs are made.

**What they measure.** Lives lost, damage dealt and kills. Everything but `BalanceHarness` also
grants 10,000,000 credits and 1,000,000 lives, so that a purchase's effect is not hidden by
running out of money or losing. That is a useful way to isolate power and a poor picture of a real game:

- The built-in levels start a player with 50 to 100 credits and 3 to 5 lives, over 8 to 18 rounds.
  Income is only kill bounty. A wave starts only after the last one is cleared, and the player's
  one pacing choice is when to call the next. None of that is exercised by the harnesses.
- A report of "36 lives lost" describes a level a real player loses at the fourth leak (Twisted
  Hourglass has 4 lives); the harness keeps counting. The number is a degree of losing, not a result a
  player ever sees.

**Placement.** Towers go on the cells nearest the most path. That ignores range, the cone a
Scorcher faces, bends and junctions, a Mortar's dead zone, Aura neighbours, the clusters some
upgrade gates require, and what a *player* does: reads the wave preview, builds where the next
wave will pass, builds a second tower to cover the first one's gap.

**What it got wrong, in practice** (the Scorcher and Sniper passes, October 2026):

- A change that doubled a target's burn damage (confirmed at x2.0 in a pool simulation) moved lives
  lost by about nothing on four towers, while damage dealt rose 25% to 70%. Lives lost was blind
  to it; damage dealt is a share of a fixed pool of enemy health that other towers lose when this one
  kills first, so it cannot say what a tower is worth either.
- At 12 seeds the standard error on lives lost was about 2 lives, and early rankings were inside
  that. Forty seeds helped. No report said which differences were noise.
- Whether an upgrade beat another copy of the tower flipped with the budget and with the level.
  Momentum at x5 and at x3 gave the same best builds. Five of the Sniper's six specials, and the
  Scorcher's whole fuel line, show no effect in any scenario the harnesses can build.
- Which of those results mean "this node is dead" and which mean "this harness cannot see what the
  node is for" is not knowable from the current output.

**What helps.** The engine is headless and seeded (`RandomSource`), a level plays in about a third of
a second, and the game already gives a player a wave preview and a say in when the next wave starts.

## What this feature adds

Seen from the person balancing the game, not from the code.

0. **A review of the whole toolset, before anything is built.** A written assessment of every
   testing and balancing tool we have, against what balancing needs. It asks, tool by tool and
   metric by metric:
   - *Coverage.* For each thing we want to know (the seven questions below, and each variable that
     can change), which tool answers it today, and which question has no tool.
   - *Observability.* Can each measure be read from a run at all? Where it cannot, what the run
     would have to report, and what that costs the headless engine's boundaries.
   - *Setup.* How cheaply can a person or an agent reach a given situation (a wave, an army, a
     level state, an enemy mix) without replaying everything before it? The dev panel solved this
     for a person at the keyboard. The review asks what the equivalent is for a headless run and
     for an agent.
   - *Trust.* Is each tool's output reproducible, and does it say how sure it is?
   - *Gaps.* What the review finds missing, ranked by how much balancing it would unblock and what
     it costs to add.

   The review also looks outward. Game design and game testing have standard practices that we
   may not use, or may not know we lack. The review should assess each one below for fit and say
   which are worth adding. The list is what has been raised so far, not a decision:
   - *Telemetry and event logs*: a record of what happened in a run (spawns, hits, kills, leaks,
     buys, sells, effect starts and ends), to answer questions nobody thought of in advance.
   - *Deterministic replay*: a run recorded as its inputs and played back exactly, to reproduce a
     result, compare two versions of the game on the same play, and attach a failing run to a bug
     report. (`TODO.md` already records the command queue as the thing that would make this
     possible.)
   - *Heat maps*: where enemies die, where they leak, where damage lands, and which cells towers
     are placed on, drawn on the level.
   - *Timelines per wave*: enemy health on the board over time, damage per second, time to clear,
     and the resulting difficulty curve across the level.
   - *Theory-crafting tables*: time to kill for every tower against every enemy, damage per
     credit, effective health per enemy, computed from the numbers without a run, to find what is
     wrong before playing.
   - *Economy modelling*: sources and sinks of credits, how much is idle at each wave, and how
     the bounty curve compares with the price curve.
   - *Parameter sweeps and sensitivity analysis*: vary one number across a range and see which
     outcome moves, how fast, and where it stops mattering.
   - *Regression baselines*: a stored reference result per level, so that an unrelated change
     that shifts balance is noticed.
   - *Content coverage reports*: which enemies were met, which nodes bought, which effects
     triggered, across a body of runs.
   - *Invariant and fuzz runs*: random legal play for many runs, with the rules checked all the
     way (no negative credits, no stuck waves, no tower that outlives its sale).
   - *Scenario files*: a situation described in a file (this level, this wave, these towers and
     upgrades, these credits) that anyone can load, in the dev panel and in a headless run.
   - *Debug overlays*: ranges, targeting lines, pool values and stack counts drawn on the board.
   - *Time control*: pause, single-step (exists), slow motion, and going back.
   - *Recorded human sessions*: a person's play kept as a replay with a few questions after it,
     so that human playtests feed the same checks.

   The review is part of this feature and comes first, because it decides what the rest needs.

1. **A player that plays the whole level.** It starts with the level's real credits and lives,
   earns only what a player earns, builds where a thoughtful player would (where a tower has the
   biggest positive effect for what is coming), chooses between a new tower and an upgrade
   with the credits it has, reads the upcoming wave, and calls the next one. It comes in
   skill levels, so that "positioning matters" is something the tool can show: a naive player and
   a competent one on the same level should end up clearly apart.

2. **An answer to seven questions**, per level, stated in plain terms and with how sure the tool is:

   | Question | What a good answer looks like |
   |---|---|
   | Is the level neither easy nor hard? | A competent player usually holds it but is pushed; a naive player loses; a strong one wins with room. The pressure builds across waves, has walls and breathers, and the close calls are survivable. |
   | Does positioning matter? | Smart placement beats naive placement by a wide margin on the same credits. The best cells differ by tower type, and more than one layout wins. |
   | Do upgrades matter? | At equal credits there is a phase where upgrading beats building another copy and a phase where it does not. No node is bought by nobody, and none is bought by everybody. |
   | Do enemy types force variability? | Every kind of enemy has a counter. An army of one tower type fails somewhere. The best build differs between levels and between waves. |
   | Is any option dead or dominant? | Every tower, node and special appears in some winning plan. None appears in all of them. |
   | Is the answer trustworthy? | Differences smaller than the noise are reported as no difference. |
   | Did this change move any of the above? | See below. |

3. **A check for one change.** Given a changed enemy, tower, upgrade node, constant or level, say which of the
   answers above moved, by how much, whether that is more than noise, and which moved out of its target
   band. It runs the same seeds before and after, so a difference belongs to the change.
   A change to a level's path, credits or lives is checked the same way as a change to a tower.

4. **Focused scenarios beside whole-level plays.** Some questions cannot be asked of a whole level:
   what a node is worth against armored enemies, healers, swarms, fast or agile enemies, invisible
   enemies, burn-immune or crit-immune ones, shielded bosses. A short scenario built from one kind of
   enemy, played by the same player, gives each tower and node a *profile* (strong against these,
   nothing against those) in place of one score. "This node is useless" then means useless in every scenario.

5. **A lab mode that stays honest.** Isolating a node's raw power with unlimited credits and lives remains
   useful. It must say it is the lab, and its numbers must never be read as how the level plays.

### First version

Not everything at once. The first version is:

- the review of the toolset, written up, with the gaps ranked and the cheap ones that unblock the
  rest fixed first (what a run must report, how a situation is set up);
- the player, at two skill levels (naive and competent), on the three built-in levels with real
  credits and lives;
- the difficulty answer and the positioning answer, with uncertainty;
- the check for one change to a tower, an upgrade node or an enemy's numbers.

### Later, in this order

1. The enemy-type scenarios and the variability answer.
2. The upgrades-versus-copies answer by phase, and the dead-or-dominant answer.
3. Synergy: what a node is worth in different armies, so that a node that only works beside another is
   known as that, not scored as weak.
4. Level changes and new levels (this meets the file-based level catalogue in `TODO.md`).
5. A strong skill level.
6. Whichever tools from the review are worth adding after the first version.

### Out of scope

Automatically searching for better numbers; a learned player; measuring fun beyond the proxies
above (a person playing the game stays the last word); anything in the player-facing jar or UI;
the performance budget.

## Interconnections

- **The dev panel and the `run-jtd` skill** (documented in `README.md` and the skill): the review
  starts from what they do well. Any headless equivalent should not make them worse.
- **The command queue and deterministic replay entry in `TODO.md`**: replay is one of the
  practices the review assesses, and that entry says what it would take.
- **`FEATURE-playtesting-and-balance-tooling.md`** (implemented): this builds on its tools and
  its idea of a headless run. `BalanceHarness`'s fixed loadout is what the player replaces.
- **`FEATURE-aura-and-balance-pass.md`**: its target (a node worth 0.7x to 1x of a copy, its watch
  list) is a target this feature is meant to make checkable.
- **`TODO.md`'s "unbalanced placeholders" entries** (upgrade nodes, Mortar and Cinder, effects and
  specializations, enemy traits, critical damage): this is how they get closed; none is closed by this
  document.
- **File-based levels** (`TODO.md`): a level added without a rebuild must be checkable without code.
- **`docs/ideas/enemies-brainstorm.md`**: every new enemy there needs a scenario that shows what it
  forces.

## Constraints and open risks

- **The player is a model.** If it favours a tower's shape, the tool will find that tower strong.
  When a tower changes (a longer cone, a new dead zone) the placement that suits it changes too,
  so a player written for today's towers can mismeasure tomorrow's. Numbers tuned only to one
  player's style may not suit a person who sells and rebuilds, calls waves early, or builds around a
  combo.
- **Real lives are few.** With 3 to 5 lives a run is close to win or lose. A verdict needs many runs, and a
  degree of winning or losing needs a second, continuous measure.
- **Positioning by search costs runs.** Trying candidate cells multiplies the work for every run, and
  the work has to stay cheap enough to use after each change.
- **Observability costs the engine.** To report more from a run, the engine must expose more. That
  has to respect the boundaries: the engine stays headless, the UI is reached only through
  `GameHost`, and a tick still finishes inside its budget with the reporting on (the performance
  budget in the root `CLAUDE.md`).
- **More tools is more to keep working.** Every tool added is code that has to follow every rename
  and rule change. The review has to weigh what a tool unblocks against that, and prefer standard
  tooling.
- **Fun is not measurable.** The questions above are proxies. A person plays the level before it ships.
- **Not a test.** Tests have 5 s each and 60 s per fork; this is an on-demand tool like the other
  harnesses, and its report has to be short enough to read.
- **Determinism.** Randomness comes from an injected source; a run must repeat exactly from its seed.
- **Standing rules.** The engine stays headless and free of Swing; no Mockito in anything under test;
  new gameplay rules stay out of the tool.
- **Maintenance.** This is a hobby project: standard tooling over custom scripts, and a small number of
  files that stay understandable.

## Decisions made

- A change is judged on what makes the game good, not on one outcome: a level that is not easy and not hard, that is
  fun, where positioning matters (the strategy is the point), where upgrades matter, and where the enemy
  types force different builds.
- Lives lost alone is not the measure, and damage dealt alone is not either.
- The runs are headless, and the towers are placed the way a player would place them.
- Any variable can be changed and checked: enemies, towers, levels.
- The whole toolset is reviewed first: coverage, whether the measures are observable, and the
  gaps, including standard game-design and game-testing practices we may be missing. The dev panel
  is the example of a tool that paid for itself.
- Not everything at once; the first version is the one above.

## Open questions

Each has two options and a preferred one (⭐).

1. **What are "not easy, not hard" in numbers?**
   - A ⭐: a competent player wins 70% to 90% of seeds and loses 30% to 70% of its lives; a naive player wins
     under 20%. Simple to read; coarse with 3 to 5 lives.
   - B: express it in credits, as the least credit total that holds the level for 90% of seeds, and
     require it to sit within a band around what a competent player earns. Finer; harder to explain.
2. **How does the player place towers?**
   - A ⭐: rules about the path, range and what is coming, with an occasional search over candidate cells to audit
     that the rules are near the best.
   - B: search always: try the candidate cells and keep the best. Follows tower changes without rewriting
     rules; costs many more runs.
3. **How many skill levels?**
   - A ⭐: naive and competent first, strong later (as above).
   - B: three from the start.
4. **Where does the "before" come from?**
   - A ⭐: re-run both sides on the same seeds each time. Slower; always consistent.
   - B: a stored result per level in the repository. Fast and reviewable; it goes stale.
5. **Is a result with real lives enough?**
   - A ⭐: report the verdict with real lives and the degree with unlimited lives, side by side.
   - B: the verdict only.
6. **Where does it run?**
   - A ⭐: on demand and before a balance commit, like the other harnesses.
   - B: also as part of `mvn verify`. Needs the whole thing to fit in the test budget.
7. **Which fun proxies do we want?** The table's rows are the candidates (rising pressure, close calls that
   are survivable, several winning layouts, no dead or dominant option, a decision every few waves).
   Which are required, and which are wanted later?
8. **How far does the review go?**
   - A ⭐: tools and metrics only: an assessment document with ranked gaps, and fixes only for the
     gaps that block the first version.
   - B: also build a first version of every tool the review recommends.
9. **Who chooses among the standard practices?**
   - A ⭐: you pick from the list, with a line each on what it would unblock for us; the ones left
     are dropped.
   - B: the review decides on its own which to adopt.
10. **What does a naive player do?** Random legal cells, cells chosen by distance from the entrance, or
   a player that builds only one tower type? They answer different questions.

---

*After planning and implementation, update this document rather than deleting it: mark it implemented, prune
resolved open questions, and either promote deferred scope to a new request or note it is still wanted for a
later version.*
