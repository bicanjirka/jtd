---
name: tower-retune
description: Retune one jTD tower step by step (base, tier0, tier1, tier2, tier3) against the measured balance pipeline - base stats, prices, node and ability parameters, node reworks - under the user's fixed price menu and the knobs they name. Use at the start of every per-tower retune or rebalance task ("/tower-retune sniper base").
---

# Retune one tower, step by step

Args: `<tower> [step]`, step = `base | tier0 | tier1 | tier2 | tier3` (default: the first not yet retuned). Steps go in
that order. Each step ends in a stop and a report; the next starts when the user says so, in the same session or a
fresh one.

## Each step: ask, rework, tune

1. **Read first:** the tower's class, its tree, its mechanics, its sub-package's `CLAUDE.md`, its `docs/CONTENT.md` row, its
   `StrategyBuilds` entries and its rows from one `TowerReport --tower <it>`.
2. **Ask for changes before knobs.** The user usually reshapes the tier first (moves, renames, new or replaced nodes,
   cluster changes) and only then names knobs. Ask: what changes in this tier's design, what is fixed, which knobs
   you may tune, which strategies the tier serves. Never guess a knob. When the user asks for alternatives to a
   node, give two or three, each with a one-line measured or "paper" estimate, and recommend one.
3. **Implement the asked changes, then measure, then tune** the named knobs. A mechanic the user did not ask for is
   never built: name it in one line with an estimate and wait.
4. **Stop:** the step's table, the suggestions, the open breaches, what the next step should fix. Wait.

The user answers in free text, often mid-step ("try X and report"). Each such answer is a new bench run on one
change; report its delta and wait again. Questions like "how much X makes up for Y" (crit for damage, price for
power) are answered with a bracket of two or three bench runs, never guessed: "break-even at about +40% crit".

## Fixed rules

- **Prices are a menu, not a result:** tower 15 or 20 (25 only for a strong, stated reason); every tier0 node 5;
  per node, tier I 10 or 15, tier II 15, 20 or 25, tier III 20 to 50 in steps of 5, never more. A tier may mix
  prices; its stronger nodes take the higher ones (a cluster's members share one). A later tier costs more because a
  player expects it to, not only because its nodes are worth more. A price off the menu is only a concern you raise.
  Never `TowerPriceWriter` and never TowerReport's "suggested" price: pick from the menu, then tune the parameters to
  fit the price. Copy surcharge (x1.25) and sell refund (0.75) stay fixed.
- **XP gates stay as they are.** The user set them and doesn't retune them per tower. TowerReport's Rounds tables are
  information; mention a `DOMINANT` or `weak` round only as an open item. Touch a gate (`UpgradeTree.withTiers`) only
  when the user asks, then state it as "XP n = round r" from one `WavePowerReport` run.
- **No implicit passive.** A tower whose bare self does something beyond its basic attack (on-hit effect, trait,
  aura, hidden bonus) moves it into a tier0 node in the `base` step (a mechanics change).
- **The Sniper is the baseline.** Later towers are tuned to its retuned rows; it is not tuned to them. A tower not yet
  retuned is a stale rival: compare against it, say that the verdict rests on it, never tune to it. Say which rivals
  are retuned in every verdict against "the others". A final cross-tower pass after the last tower settles
  "equal to each other".
- Memories that hold: bare Burst (base tree) is every tower's damage floor, Sniper the single-target ceiling, Aerie
  the top single-target vs armor/plating; report any breach (open: the Aerie above the Sniper on single and boss,
  for the Aerie's step). No tower allows for the Beacon; its combos are tuned in the Beacon's nodes. It holds nothing
  alone, so it is judged on the `+` (beside-partners) columns only, and TowerReport prints it "not judged"
  (`TowerSheet.isJudged`): its step first makes the report judge it on those columns (tooling, commit 1).
  The Prism node must apply Vulnerable, the Burst's signature debuff (its Jolt and Agony apply it); set it in the
  Beacon's step.

## What is judged, at which states

Measure every state at its **total spent**: tower price plus every node bought.
1. **Bare** at its price: a valid tower on its own.
2. **Bare + all of tier0** (price + 5 per node): every tower equal in weighted power per credit, each leading in its
   own scenarios.
3. **Each tier whole**, each cluster member, and **each named strategy** (only its nodes) at its own total.
4. **Fully upgraded:** equal to the other towers overall; the strongest in one scenario is weak in another.

Targets:
- No tower dominates and none sits at the bottom, at any state, in weighted power per credit (MIX weights).
- Strategies lead in the scenarios the user assigned to each; none is the go-to (TowerReport's "Strategies").
- Members of one cluster are as even as possible, each ahead somewhere ("Clusters and forks").
- Later tiers give stronger nodes, but price climbs more steeply than power:
  `price(Tn)/price(Tn-1) > worth(Tn)/worth(Tn-1) > 1`, per node, from TowerReport's worth.

## Tools (real game, real code)

```bash
mvn -q compile exec:java -Dexec.mainClass=td.balance.EnemyPowerReport                       # only if target/balance/enemy-power.tsv is gone
mvn -q compile exec:java -Dexec.mainClass=td.balance.TowerBench -Dexec.args="--towers <t>"   # re-measures only this tower; --quick to print only
mvn -q exec:java -Dexec.mainClass=td.balance.TowerReport -Dexec.args="--tower <t>"           # that tower's rows of every table; stores all towers
```
Run the report after every bench run; `--tower` replaces any filter script. `docs/BALANCE.md` explains every column.
Weighted power per credit is the Stages table's `value` and `vs rivals`; a stage is a tier bought whole by the build
that pays best. Change one knob per bench run, so each delta has one cause.

- **Strategies** are one entry each in `StrategyBuilds` (tower, name, the node ids it owns). The user may give them as
  node lists or screenshots of the Upgrades panel; write them down, and ask only for what the picture leaves open.
  The bench measures them as `strategy:<name>` and the Strategies table judges them against each other.
- **Read a node in the right table.** A node that acts only on top of others (on crit, on kill, a debuff, against a
  rank) shows near nothing in the Nodes table's `alone` and is often `UNMEASURED`: judge it by "Clusters and forks"
  (the whole tree with that member) and by the strategy that owns it.
- **What the bench cannot see:** its partners are bare and never crit, so a per-source share or a stack several
  copies of the tower build together (cross-fire on a boss) is unmeasured; so are corners and overlap, mixed waves,
  buying order and economy pace. Say so, reason it out on paper, and don't tune for it. A missing enemy behaviour that
  a node is for (enemies that dodge, before the `elusive` scenario) is a new bench scenario: tooling, commit 1, and a
  `MIX` weight.
- For gates only, when the user asks: a full `WavePowerReport` run (two minutes). Never `PlayCheck` (twenty minutes,
  unfinished waves), `BalanceReport`, the `jtd-balance-tools` skill or the old harnesses (PurchaseHarness,
  BuildHarness): they are stale. Labyrinth is not a yardstick: there the towers are built during the wave.
- A prototype that is cheap goes in one scratch worktree for the whole session, with `target/balance/*.tsv` copied
  in. Windows may refuse to delete it: `git worktree remove --force`, then `git worktree prune`.

## A new, renamed or moved node

Each of these fails `mvn verify` or misleads a player when forgotten:
- its `MechanicCatalogue` entry, when it has a mechanic;
- its `PerkPictograms` entry (`=effect.x` reuses a term's face) - `PictogramCatalogueTest`;
- its place in `UpgradeTreePlans` (a cluster takes one place) - `UpgradeTreeLayoutTest`;
- a changed id raises `GameSaveText.FORMAT` once per unpushed series (check against `origin/main` first); ids follow
  names, no legacy ids;
- its text: no number the code beside it holds that a tuning edit can make false; the sub-package's `CLAUDE.md` and
  the `docs/CONTENT.md` row when behaviour changes;
- tests of other towers that bought it by name (move them to another tower's node of the same kind).

## Output

- Each suggestion is one line: `Class.FIELD old -> new  | scenario deltas, power/credit before -> after`. No prose.
- The step's states as one compact table (state, cost, key scenarios, power/credit, vs rivals and whether those
  are retuned).
- Answer a design question ("does Unravel fit this tower?") in a few lines with a recommendation, then offer the run.

## Live check, once per tower

After the tower's last step, one look at its Upgrades panel: `td.ui.PreviewUpgrades` (headless, `run-jtd`) for the
layout of every tier and each changed node's sheet; the `Driver` only for what that can't show. Never per step.

The shop button's mark (`TowerIcons`) is drawn apart from the board art and may still show a tower's earlier design:
check it against the tower on the board in its retune and redraw it in the table's language (a flat silhouette, the
moving part lighter, a seam). The Burst's and the Mortar's are redrawn; the rest still show their old designs.

## Names and glyphs, once per tower

After the tower's last step, review every node's name and glyph against the tower's look on the board and its lore,
then suggest changes and wait. Judge from pictures, not path strings: `PreviewIcons` (each face at 40 and 20 px) and
`td.ui.PreviewUpgrades` (the panel and every node's sheet), both from `run-jtd`. Look for:
- a name that fights the look or lore, or that another term already uses (enemy abilities in `AbilityTerm`, other
  towers' nodes and `TowerTerm`s): one name for two things misleads the player;
- one face on two nodes of the tree (a fork node reusing its passive's face, a tier 0 and a tier III node alike);
- a face borrowed from an effect the node doesn't apply, or from another tower's node;
- two faces that read the same at 20 px (a cut-out bolt reads as a crack), or a face that reads as something else.

A rename follows "A new, renamed or moved node" above, renames code with the IDE's `rename_refactoring`, and updates
the tower's `StrategyBuilds` names and `docs/CONTENT.md`, never `jtd-balance-tools`' `trees/` model, which mirrors the
shipped design doc and breaks on a name the doc lacks; then
`TowerBench --towers <it>` and `TowerReport --tower <it>` re-store the tables under the new names. One commit.

## Commits

- On main, stage own files by name (other sessions share the checkout), no push. `mvn -q verify && git commit ...`
  in one chain, so a failure stops it. A failure in a test the change doesn't touch: rerun it alone before calling it
  flaky; another session building in the same `target` is the usual cause. Never `git stash`: the stash is shared.
- Edit with the Edit tool or a script that keeps CRLF; after `sed -i`, run `mvn -q spotless:apply`.
- Each step ends as **exactly two commits**: **1)** mechanics, balance tooling and other complex code (with their
  tests); **2)** parameters, prices and `StrategyBuilds` entries only. A step with no mechanics change is the second
  alone. Tooling every tower uses (a new bench scenario) may be its own commit.
- A later change that belongs to an earlier step goes in as `git commit --fixup <that commit>` and is folded with
  `GIT_SEQUENCE_EDITOR="cp <todo>" git rebase -i <base>`, the todo listing `pick`, `fixup` and
  `exec git commit -q --amend -F <msg>` where the change makes a message false (the user prefers reworded messages).
  Only over this tower's unpushed commits. A rename folded into an early commit conflicts in every later one that
  touched those lines; resolve each by script. Before: `git tag retune-backup`. After: `git diff retune-backup` is
  empty, every commit compiles (`git rev-list` + `mvn -q compile`), `mvn verify` passes, delete the tag.
- A content change adds no test. A test it breaks is rewritten on a fixture or deleted, never given a new expected
  number. Update `docs/CONTENT.md`'s tower table and any `docs/BALANCE.md` or `CLAUDE.md` line the step makes false.
