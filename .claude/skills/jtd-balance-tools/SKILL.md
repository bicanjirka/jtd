---
name: jtd-balance-tools
description: Run jTD's headless tuning harnesses - BalanceReport (balance per level), WaveCheckReport (one level wave by wave), PurchaseHarness (purchases and fair prices), BuildHarness (builds of one tower). Use when judging or tuning numbers of towers, enemies, levels or waves.
---

```bash
mvn -q compile exec:java -Dexec.mainClass=td.balance.BalanceReport -Dexec.args="--save before"   # balance per level; then --compare before; also --seeds N --levels 0,2 --strong --variety --sweep lives|bounty|clear|pause|spacing --html
mvn -q compile exec:java -Dexec.mainClass=td.balance.WaveCheckReport -Dexec.args="--level 0 --checks 6:single,12:control+sustain --strip 17"   # one level wave by wave: strong margin, pressure, role bans, strip; --parts strong,margin,bounty,bans,strip; --save-before 17 then --from 17 replays only the late waves
mvn -q compile exec:java -Dexec.mainClass=td.PurchaseHarness -Dexec.args="0,1,2 12 all 4"   # purchases compared, with each line's fair price and a table of fair prices per tier: levels, seeds, tower or all, copies of each tower in the army
mvn -q compile exec:java -Dexec.mainClass=td.BuildHarness -Dexec.args="2 40 cinder 4 2 1500 1"   # builds of one tower compared: level, seeds, tower, copies, background copies, budget, upgraded copies
```
