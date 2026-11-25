---
name: tower-retune
description: Retune one jTD tower step by step (base, tier0, tier1, tier2, tier3) against the measured balance pipeline - base stats, per-tier stat growth, prices, node and ability parameters, node reworks - in the tower nerf, which keeps each tower's power ratio to the Sniper from before the nerf, prices nodes by their worth, and touches only the knobs the user names. Use at the start of every per-tower retune or rebalance task ("/tower-retune sniper base").
---

# Retune one tower, step by step

Args: `<tower> [step]`, step = `base | tier0 | tier1 | tier2 | tier3` (default: the first not yet retuned). Steps go in
that order. Each step ends in a stop and a report; the next starts when the user says so, in the same session or a
fresh one.

## The direction: towers are nerfed, enemies are fixed

The towers are too strong for the waves. The fix is in the towers alone: never change an enemy, a wave, a level or
an XP gate to make a tower fit. The curve the nerf aims for is a tower that starts much weaker and gets strong faster:
the bare tower and the early tiers end up much weaker than before the nerf, while the late tiers and the fully
upgraded tower stay about where they were. A tower with all its tiers is meant to be strong. The growth comes from
nodes that do more and from **per-tier stats**, which the tower gets free as each tier I, II and III opens.

- **The Sniper is nerfed by hand.** The user names its numbers; the skill builds them, measures them and reports
  each state against the baseline. It proposes Sniper numbers only when asked.
- **Every other tower keeps its ratio to the Sniper from the baseline**, at every state (see "What is judged").

## Each step: ask, rework, tune

1. **Read first:** the tower's class, its tree, its mechanics, its sub-package's `CLAUDE.md`, its `docs/CONTENT.md` row, its
   `StrategyBuilds` entries and its rows from one `TowerReport --tower <it>`.
2. **Ask for changes before knobs.** The user usually reshapes the tier first (moves, renames, new or replaced nodes,
   cluster changes) and only then names knobs. Ask: what changes in this tier's design, what is fixed, which knobs
   you may tune, which strategies the tier serves. In `tier1` to `tier3`, ask too for the tier's per-tier stats:
   which stats grow, which are open to tuning. Never guess a knob. When the user asks for alternatives to a
   node, give two or three, each with a one-line measured or "paper" estimate, and recommend one.
3. **Implement the asked changes, then measure, then tune** the named knobs. A mechanic the user did not ask for is
   never built: name it in one line with an estimate and wait.
4. **Stop:** the step's table, the suggestions, the open breaches, what the next step should fix. Wait.

The user answers in free text, often mid-step ("try X and report"). Each such answer is a new bench run on one
change; report its delta and wait again. Questions like "how much X makes up for Y" (crit for damage, price for
power) are answered with a bracket of two or three bench runs, never guessed: "break-even at about +40% crit".

## Fixed rules

- **Tower price:** 15 or 20 (25 only for a strong, stated reason).
- **A node's price follows its power.** Every node, tier 0 included, costs a multiple of 5 picked to fit its
  measured worth (TowerReport's `worth`), at one rate of credits per worth for every tier and every tower. A tier
  sets no price, there is no cap, and no factor raises a later tier: a later node costs more only because it does
  more. The members of a cluster and the sides of a fork share the price of the dearest. TowerReport's
  "suggested" price is exactly that and is where a price starts; set it by hand after the parameters are settled, and
  stay one step of 5 off it only for a stated reason. Never `TowerPriceWriter`. Copy surcharge (x1.25) and
  sell refund (0.75) stay fixed.
- **Absolute stats.** Damage, range, area, durations, stacks, armor penetration and per-tier growth are flat
  amounts (`TowerModifiers.damage(8f)`, `range(0.5f)`), never percentages of base. These stay relative: attack
  speed (`+x` of the rate), crit chance and crit multiplier (already points on one scale for every tower), and debuff
  factors (jam, Vulnerable). A damage or range percent left in a tree (`damagePercent`, `rangePercent`, `*Times`)
  is converted to a flat amount in that tower's retune. Any other move between relative and absolute gets discussed
  with the user first.
- **Per-tier stats** are a tower's own flat bonuses, one set for each tier I, II and III, authored as the tier's
  bonus (`UpgradeTier.gated(xp, 0).withBonus(...)` in the tower's `withTiers`). The tree turns each into the tier's
  **stat node** (`tier.N`, named "Tier N", drawn with its numeral): it carries any number of stats with any values,
  has no price, and unlocks by itself when the tier opens by XP, so it counts in every state at and above that
  tier and is never bought, sold, refunded or saved. It replaces the tier's existing stat nodes: in that tier's step
  the tier's plain stat nodes leave the tree and their effect moves into the tier's bonus, so the node count stays
  about the same. The user names which stats grow and by how much. In the same step, place the stat node first in
  the tier's bottom row of `UpgradeTreePlans` (`node("tier.N", 0f)`), shifting that row right; until a tower's tier
  is retuned its plan leaves the node out and the panel doesn't show it.
- **XP gates stay as they are.** The user set them and doesn't retune them per tower. TowerReport's Rounds tables are
  information; mention a `DOMINANT` or `weak` round only as an open item. Touch a gate (`UpgradeTree.withTiers`) only
  when the user asks, then state it as "XP n = round r" from one `WavePowerReport` run.
- **No implicit passive.** A tower whose bare self does something beyond its basic attack (on-hit effect, trait,
  aura, hidden bonus) moves it into a tier0 node in the `base` step (a mechanics change).
- **The Sniper is the yardstick.** Later towers are tuned to its retuned rows; it is not tuned to them. A tower not
  yet retuned is a stale rival: stronger than intended in its early states. Its rows tell you nothing about the
  nerfed scale, so compare only to the Sniper and to towers already retuned, and say which those are. A final
  cross-tower pass after the last tower settles any ratio that drifted.
- Memories that hold: bare Burst (base tree) is every tower's damage floor, Sniper the single-target ceiling, Aerie
  the top single-target vs armor/plating; report any breach (open: the Aerie above the Sniper on single and boss,
  for the Aerie's step). No tower allows for the Beacon; its combos are tuned in the Beacon's nodes. It holds nothing
  alone, so it is judged on the `+` (beside-partners) columns only, and TowerReport prints it "not judged"
  (`TowerSheet.isJudged`): its step first makes the report judge it on those columns (tooling, commit 1).
  The Prism node must apply Vulnerable, the Burst's signature debuff (its Jolt and Agony apply it); set it in the
  Beacon's step.

## What is judged, at which states

Measure every state at its **total spent**: tower price plus every node bought. The per-tier stats of every tier the
state reaches are in, free.
1. **Bare** at its price: a valid tower on its own.
2. **Bare + all of tier0**, each leading in its own scenarios.
3. **Each tier whole**, each cluster member, and **each named strategy** (only its nodes) at its own total.
4. **Fully upgraded:** the strongest in one scenario is weak in another.

**The baseline** is the full set of tables from before the nerf, stored in `.claude/skills/tower-retune/baseline/`
(`tower-stages.tsv`, `tower-prices.tsv`, `towers.tsv`, `tower-scenarios.tsv`). If that folder is missing, the
Sniper's first step builds it before any change: a full `TowerBench` (every tower) and `TowerReport` on the commit
before the nerf, copy the tables in, and commit them alone. Never re-measure it after a change.

Targets:
- **Ratio to the Sniper, kept.** For each state, `value(tower) / value(Sniper)` (Stages table, weighted power per
  credit) is what it was in the baseline, within the bench's noise. Put the power ratio beside it. A baseline ratio
  that was a breach (`DOMINANT`, `weak`) is not copied silently: name it and ask whether to keep it.
- **The curve:** each state's power against the baseline, as a "vs before" share. The early states are expected to
  drop a lot and the late ones to land near 1. A late state far under 1 is a finding.
- Strategies lead in the scenarios the user assigned to each; none is the go-to (TowerReport's "Strategies").
- Members of one cluster are as even as possible, each ahead somewhere ("Clusters and forks").
- Later tiers give stronger nodes (worth climbs from tier to tier); their prices follow the worth, not the tier.

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
- **Per-tier stats are new code.** The step that first sets them (the Sniper's, unless they already exist) builds
  the shared mechanic in its commit 1: the stats on the tier (beside `UpgradeTier`'s gate), applied when the tier
  opens, shown on the tier's band in the Upgrades panel (a figure the player decides on, so it gets a test), and
  counted by `TowerBench` in every state at or above the tier, and by `TowerRounds` once the XP opens it. Check
  `docs/DESIGN.md` before choosing its shape.
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

- Each suggestion is one line: `Class.FIELD old -> new  | scenario deltas, power/credit before -> after, ratio to
  Sniper before -> after`. No prose.
- The step's states as one compact table: state, cost, key scenarios, power/credit, power vs before (baseline),
  ratio to the Sniper now and in the baseline. For the Sniper, the last two columns are the curve against its
  baseline and the retuned rivals.
- Answer a design question ("does Unravel fit this tower?") in a few lines with a recommendation, then offer the run.

## Live check, once per tower

After the tower's last step, one look at its Upgrades panel: `td.ui.PreviewUpgrades` (headless, `run-jtd`) for the
layout of every tier and each changed node's sheet; the `Driver` only for what that can't show. Never per step.

The shop button's mark (`TowerIcons`) is drawn apart from the board art and may still show a tower's earlier design:
check it against the tower on the board in its retune and redraw it in the table's language (a flat silhouette, the
moving part lighter, a seam). The Burst's and the Mortar's are redrawn; the rest still show their old designs.

## Info texts, once per tower

After the tower's last step, review everything the player reads about it, in pictures (`td.ui.PreviewUpgrades`,
`run-jtd` shots): the tower info panel, the shop button's buy hover popup and every upgrade node's hover sheet. Then
suggest changes and wait.
- **A node's text describes it accurately and names its key terms first.** The hover sheet lists the terms a text
  mentions in their order of appearance, not by importance, and shows only the first few: a text that opens with
  its key terms puts the right popups on screen. Rewrite a text to lead with them rather than reorder the sheet.
- **The tower info panel shows the tower's own stats only** (damage, rate, range, crit, ...). A stat that belongs to
  what it fires - a shell's blast radius, a projectile's speed or lifetime - moves to that shell's or projectile's
  term popup, which the panel links.
- The buy hover popup says what the bare tower does after the retune (a passive moved to tier0 is gone from it).

The fixes are wording and the panel's rows: commit 2 of the last step, or one commit of their own when they move a
stat between the panel and a popup (code).

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
  tests); **2)** parameters, per-tier stat values, prices and `StrategyBuilds` entries only. A step with no mechanics change is the second
  alone. Tooling every tower uses (a new bench scenario) may be its own commit.
- A later change that belongs to an earlier step goes in as `git commit --fixup <that commit>` and is folded with
  `GIT_SEQUENCE_EDITOR="cp <todo>" git rebase -i <base>`, the todo listing `pick`, `fixup` and
  `exec git commit -q --amend -F <msg>` where the change makes a message false (the user prefers reworded messages).
  Only over this tower's unpushed commits. A rename folded into an early commit conflicts in every later one that
  touched those lines; resolve each by script. Before: `git tag retune-backup`. After: `git diff retune-backup` is
  empty, every commit compiles (`git rev-list` + `mvn -q compile`), `mvn verify` passes, delete the tag.
- A content change adds no test. A test it breaks is rewritten on a fixture or deleted, never given a new expected
  number. Update `docs/CONTENT.md`'s tower table and any `docs/BALANCE.md` or `CLAUDE.md` line the step makes false.
