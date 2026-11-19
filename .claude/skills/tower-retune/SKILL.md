---
name: tower-retune
description: Retune one jTD tower in one step (base, tier0, tier1, tier2 or tier3) against the measured balance pipeline - base stats, prices, node and ability parameters, XP gates - under the user's fixed price menu and the knobs they name. Use at the start of every per-tower retune or rebalance task ("/tower-retune sniper base").
---

# Retune one tower, one step

Args: `<tower> <step>`, step = `base | tier0 | tier1 | tier2 | tier3`. Towers go base -> tier0 -> tier1 -> tier2 -> tier3,
one step per session. Each session starts fresh from this skill.

## Before any measurement: ask

The user names, per step, **what is fixed**, **which knobs you may tune**, and **which nodes form which build
strategies** (and in which scenarios each strategy must be strong). Without them, ask; never guess a knob. Also ask
whatever else blocks the step. Before you ask, read the tower's class, its tree, its mechanics, its sub-package's
`CLAUDE.md` and its `README.md` row.

## Fixed rules

- **Prices are a menu, not a result:** tower 15 or 20 (25 only for a strong, stated reason); every tier0 node 5;
  per node, tier I 10 or 15, tier II 20 or 25, tier III 30 or 35. A tier may mix both; its stronger nodes take the
  higher price (a cluster's members share one). A price off the menu is only a concern you raise, when it would
  simplify the design or the balance; the menu stays preferred. Never `TowerPriceWriter` and never TowerReport's
  "suggested" price: pick from the menu, then tune the parameters to fit the price. Copy surcharge (x1.25) and sell
  refund (0.75) stay fixed.
- **No implicit passive.** A tower whose bare self does something beyond its basic attack (on-hit effect, trait,
  aura, hidden bonus) moves it into a tier0 node in the `base` step: a mechanics change (commit 1, below). If a node id
  changes, raise the save format; ids follow names, no legacy ids.
- **XP gates are yours to set, per tower** (`UpgradeTree.withTiers`; `UpgradeTier.standard()` is only the default).
  XP is the bounty of every enemy whose walk passed within the tower's reach, hit or not (`ExperienceAwarder`), so
  every tower beside a lane earns about that lane's whole bounty: a gate is in effect a round. Read the rounds off
  WavePowerReport's `xp` and `opens` columns on Iron Crossing. Each tower gets its own XP curve (its gate per tier),
  picked by how its power grows: a tier whose power per credit jumps opens later, so that at every round the tower's
  best affordable build stays in the band of the others'. State each gate as "XP n = round r". The Beacon earns through the towers it lights (plus Kinship,
  Mentor and Breakthrough bonuses), so its pace is its partners'. Gates are provisional until the levels are final.
- Memories that hold: bare Burst (base tree) is every tower's damage floor, Sniper the single-target ceiling, Aerie
  the top single-target vs armor/plating; report any breach. No tower allows for the Beacon; its combos are tuned in
  the Beacon's nodes. It holds nothing alone, so it is judged on the `+` (beside-partners) columns only, and
  TowerReport prints it "not judged" (`TowerSheet.isJudged`): its step first makes the report judge it on those
  columns (tooling, commit 1).

## What is judged, at which states

Measure every state at its **total spent**: tower price plus every node bought.
1. **Bare** at its price: a valid tower on its own, no upgrades.
2. **Bare + all of tier0** (price + 5 per node): every tower equal in weighted power per credit, each leading in its own
   scenarios.
3. **Each tier whole**, and **each named strategy build** (only its nodes, up to the step's tier) at its own total.
4. **Fully upgraded:** equal to the other towers overall; the strongest in one scenario is weak in another.

Targets:
- No tower dominates and none sits at the bottom, at any state, in weighted power per credit (TowerReport, MIX weights).
- Strategies are equally strong in the scenarios the user assigned to each. No go-to build for every case.
- Nodes of one cluster are as even as possible (TowerReport's "Clusters and forks").
- Later tiers give stronger nodes, but price climbs more steeply than power:
  `price(Tn)/price(Tn-1) > worth(Tn)/worth(Tn-1) > 1`, per node, from TowerReport's worth.
- Towers not yet retuned are stale rivals. Compare against them, but flag a verdict that rests on one. A final
  cross-tower pass after the last tower settles the "equal to each other" rule.

## Tools (real game, real code)

```bash
mvn -q compile exec:java -Dexec.mainClass=td.balance.EnemyPowerReport                       # only if target/balance/enemy-power.tsv is gone
mvn -q compile exec:java -Dexec.mainClass=td.balance.TowerBench -Dexec.args="--towers <t>"   # re-measures only this tower; --quick to print only
mvn -q compile exec:java -Dexec.mainClass=td.balance.TowerReport                            # prices, worth, stages, roles, clusters, synergies
```
Open `target/balance/towers.html` and the `.tsv` sheets beside it; `docs/BALANCE.md` explains every column. Weighted
power per credit is `tower-stages.tsv`'s `value` and `vs rivals`. Its stages are each tier bought whole by the build
that pays best. The bench measures only the builds it reads off the tree (`TowerBench.buildsOf`: bare, line, full per
fork side and cluster member, without). A strategy the user names that is none of them gets added as a bench build
first (tooling, commit 1). Change one knob per bench run, so each delta has one cause. For gates only, a full
`WavePowerReport` run (two minutes; `--levels` prints but stores nothing). No `PlayCheck`: it takes twenty minutes and
judges unfinished waves. Never `BalanceReport`, the `jtd-balance-tools` Python tools or the old
harnesses (PurchaseHarness, BuildHarness); that skill is stale. Labyrinth is not a yardstick: there the towers are built
during the wave.

The bench shows a tower on a straight lane against single-kind waves; levels are not final. So say what the bench
cannot settle (corners and overlap, mixed waves, buying order, economy pace) instead of tuning for it.

## Output

- Each suggestion is one line: `Class.FIELD old -> new  | scenario deltas, power/credit before -> after`. No prose.
  Show the step's states as one compact table (state, cost, key scenarios, power/credit, rank among towers).
- A **mechanics change** (new code, new behaviour, anything more than a number) is never done unasked. Name it in one
  line with a measured estimate (% capacity per bench scenario, power/credit before -> after; prototype it in a
  scratch worktree when cheap, copying `target/balance/*.tsv` in, else mark it "paper"), then wait for the user's decision. When the user asks for a
  mechanic, implement it first, then tune.
- Stop after the step: report the table, the open breaches and what the next step should fix. Wait.

## Commits

- On main, stage own files by name, no push. `mvn verify` before every commit; `td.PerformanceHarness` after
  per-tick or per-frame code. No feature request, no plan doc.
- Commit as you go, then fold the step into **exactly two commits** (`git commit --fixup` + `git rebase --autosquash`,
  only over this step's unpushed commits): **1)** mechanics, balance tooling and other complex code (with their tests),
  **2)** parameters, prices and XP gates only. A step with no mechanics change is the second commit alone.
- A content change adds no test. A test it breaks is rewritten on a fixture or deleted, never given a new expected
  number. Update `README.md`'s tower table and any `docs/BALANCE.md` or `CLAUDE.md` line the step makes false.
