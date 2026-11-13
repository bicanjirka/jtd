---
name: jtd-balance-tools
description: Run jTD's headless tuning harnesses - BalanceReport (balance per level), WaveCheckReport (one level wave by wave), PurchaseHarness (purchases and fair prices), BuildHarness (builds of one tower) - and weigh next-gen towers and upgrade nodes on paper before they exist in code (tree_calc.py against a stored reference, node-math.md, node_sim.py, area_sim.py). Use when judging or tuning numbers of towers, enemies, levels, waves or upgrade nodes.
---

Weighing a next-gen tower before code: `tree_calc.py` reads the tree from the tower's doc and its numbers from
`trees/<tower>.py`, reads every named build tier by tier against `reference.json` (the Sniper), prices every node
by its worth and finds pairs worth more together. How it counts, and how to model a new tower, is in `node-math.md`
("The tree calculator"), with the assumptions, the cluster rule, the formulas, `node_sim.py` for shot sequences and
`area_sim.py` for blasts, grounds, magazines and bars.

```bash
python .claude/skills/jtd-balance-tools/tree_calc.py tiers sniper                  # each named build per tier, every scenario, against the reference, then the floor and ceiling; also check, save-reference
python .claude/skills/jtd-balance-tools/tree_calc.py tiers sniper --set frenzy=0.5 # the same with one of the model's numbers changed for this run
python .claude/skills/jtd-balance-tools/tree_calc.py prices mortar                 # each node's fair price, each cluster even, situational or one right answer
python .claude/skills/jtd-balance-tools/tree_calc.py combos burst                  # pairs of nodes worth more together (OP?), strategies per dollar side by side
```

```bash
mvn -q compile exec:java -Dexec.mainClass=td.balance.BalanceReport -Dexec.args="--save before"   # balance per level; then --compare before; also --seeds N --levels 0,2 --strong --variety --sweep lives|bounty|clear|pause|spacing --html
mvn -q compile exec:java -Dexec.mainClass=td.balance.WaveCheckReport -Dexec.args="--level 0 --checks 6:single,12:control+sustain --strip 17"   # one level wave by wave: strong margin, pressure, role bans, strip; --parts strong,margin,bounty,bans,strip; --save-before 17 then --from 17 replays only the late waves
mvn -q compile exec:java -Dexec.mainClass=td.balance.EnemyPowerReport -Dexec.args="--kinds golem,yeti --ranks grunt,elite"   # every enemy at every rank against the Walker of its rank, with no tower: each answer alone, blind, countered, the bounty its power asks for; no args for all of it (seconds), also target/balance/enemy-power.tsv
mvn -q compile exec:java -Dexec.mainClass=td.balance.WavePowerReport -Dexec.args="--levels rift,hive"   # every round of every level with no tower: its strength in Walkers, what its kills and its clear bonus pay, the credits banked before it against what the paper reference tower's cheapest defence that holds it costs (room), room against the level's own trend (even), and the part of a defence it asks for; no args for all levels (2 min), also target/balance/wave-power.tsv
mvn -q compile exec:java -Dexec.mainClass=td.PurchaseHarness -Dexec.args="0,1,2 12 all 4"   # purchases compared, with each line's fair price and a table of fair prices per tier: levels, seeds, tower or all, copies of each tower in the army
mvn -q compile exec:java -Dexec.mainClass=td.BuildHarness -Dexec.args="2 40 cinder 4 2 1500 1"   # builds of one tower compared: level, seeds, tower, copies, background copies, budget, upgraded copies
```
