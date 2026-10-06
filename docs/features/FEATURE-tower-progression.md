# Feature Request: Tower Progression

**Status: not started.** Feature 1 of 6 in the tower rework (order in `TODO.md`). It replaces
the never-implemented `FEATURE-tower-upgrades-iteration-2.md` and carries over every answer that
request left open.

## Summary

The second generation of tower upgrades. A tower climbs a three-step base chain (Attune ->
Awaken -> Transcendent), its head chains reach level III and then fork into two overpowered
options (IV-A | IV-B), every tower gains a non-exclusive extra head node, and Transcendent opens
a second special. Every performance gate (kills, damage dealt) is replaced by one progress number,
**XP**, that a tower earns by doing its job, so support towers stop starving and damage gates stop
being free. The panel finally says which purchases are exclusive before the player pays.

This feature builds the mechanism, XP and the price rules, and moves today's content into the new
shape. Each tower's new content (its Attune passive, level III and IV, extra node, new specials)
arrives with its own feature, 2 to 6.

## Current state (what exists today)

- **Three slots** per tower, `base` / `head` / `special` (`FEATURE-tower-upgrade-trees.md`,
  implemented). `base` holds Range and Awaken, both price only (`StandardBaseSlot`). Awaken opens
  the head roots and the special roots.
- **Head:** two chains that exclude each other. Most stop at level II; Sonar's Twin Array and both
  Splash chains reach III. Nothing has a level IV or a third, non-exclusive head line.
- **Special:** one per tower, ever. Sniper, Sonar and Splash offer three; the others one.
- **Gates:** `KillCountCondition`, `DamageDealtCondition`, `ClusterCondition` (N of the 8
  surrounding cells hold towers) and `always()`, combined with and/or. Measured against play,
  damage gates (100-300) clear in one or two enemy passes while kill gates (8-25) take waves, and
  four of the Seeker's five nodes gate on kills on the tower that kills least.
- **Only the killing blow counts.** `dealDamage` credits a kill to the tower that landed it.
  Nothing records which other towers damaged the enemy.
- **Panel:** up to three offered nodes per slot, numbered across the slots for the number-key
  shortcuts. Nothing says a choice is exclusive: the player learns after paying. On the board, one
  row of pips per slot and a glow while the tower owns a special.
- **Prices:** every copy of a tower costs the same. Upgrade prices follow a placeholder rule
  (`TODO.md`, "Upgrade-tree node numbers are unbalanced placeholders").

## What this feature adds

### The tree shape

```
BASE     Range I  ->  Range II (needs Awaken)  ->  Range III (needs Transcendent)
         Attune  ->  Awaken  ->  Transcendent
HEAD     chain A   I -> II -> III -> IV-A | IV-B  ┐ one chain per tower; the other locks out
         chain B   I -> II -> III -> IV-A | IV-B  ┘
         extra     I -> II -> III -> IV             exclusive with nothing
SPECIAL  slot 1 (needs Awaken)      slot 2 (needs Transcendent)

Attune opens head I and II (both chains and the extra node).
Awaken opens head III and special slot 1.
Transcendent opens head IV, extra IV, Range III and special slot 2.
```

- **Attune** (the "Fortify" of earlier docs) is the new base root, price only. It is where each
  tower's **signature passive** lives (the Sniper's Steady Aim, the Pulse's Toll). Until a tower's
  own feature gives it one, its Attune is a plain step that only unlocks the head.
- **Awaken** needs Attune.
- **Transcendent** is Awaken's second level: it takes Awaken's place in the panel once Awaken is
  owned, not a separate button. It needs an owned special, a chain's level III and XP (1500 to
  start with), plus its price. It has no stat bonus of its own. It is shown **locked, with what's
  missing** ("needs a special, a level III head, XP 1500"), because it is the goal and hiding it
  hides the game's ceiling. Its look: a slow halo ring on the base, a gold pip, and the tower's
  shot drawn in a lighter, whiter colour.
- **Range** has three steps: +15%, then +10%, then +10%. Range II needs Range I and Awaken; Range
  III needs Range II and Transcendent. A tower's Range III may carry a perk of its own (content).
- **Head chains** run I -> II -> III, then branch into **IV-A and IV-B**: both need Transcendent,
  and buying one locks the other out for good. The tower still picks one chain. A chain's level I
  decides how the tower is used; IV-A and IV-B are two ways to make that use overpowered.
- **The extra head node**: one per tower, levels I to IV, exclusive with nothing. It coexists with
  either chain, or with none. Levels I and II need Attune, III needs Awaken, IV needs
  Transcendent. It doesn't branch, and its level III doesn't count toward Transcendent. A level
  whose content reads the chosen chain (the Splash's do) also needs a chain's level I.
- **Specials.** A tower offers either one shared set of three specials, or **one set of three
  per chain** (Sniper, Splash). With sets per chain, Awaken offers specials only once a chain's
  level I is owned, and offers that chain's set; the panel says why none is offered before that
  ("choose a chain first"). Slot 2 picks from the same three. The pick is free: no written
  pairings, since a tower's specials share one trigger (the Sniper's are all crits) and combine by
  themselves.
- **Aura caps at Awaken**: no Transcendent, no Range III, no level IV, one special. Its chains and
  extra node stop at III.
- The panel still shows at most three offered nodes per slot. A fresh head slot offers the two
  chain roots and extra I: exactly three.

### Today's content in the new shape

- Every existing node keeps its id and effect until its tower's feature reworks it. Attune is
  inserted before Awaken on every tower.
- Transcendent appears on a tower once its tree can reach it (a level III head exists today on
  Sonar and Splash). Until a tower's feature adds level IV and the extra node, Transcendent opens
  only its second special and Range III there.
- No placeholder nodes: a slot shows only content that exists.

### The exclusive-choice mark

- Nodes that exclude each other are drawn as one linked set: a bracket joining them and a "1 of 2"
  / "1 of 3" label. The same mark serves every exclusive choice: the two chain roots, a tower's
  specials (in both slots) and IV-A / IV-B. A node outside such a set carries no mark, so no mark
  means "safe to buy". The extra head node is never in a set.
- While a set is on offer, the panel names what a pick locks out (the other chain's name), not
  just that something will be.
- Once a choice is made, the panel shows what was picked and a lock glyph on the alternatives,
  instead of silently dropping them.
- Panel only; the tower's pips on the board stay as they are. Drawn by the panel in the HUD look,
  readable without colour (glyph and label).

### XP: one progress number

- **Every tower has XP.** It is never spent and never lost while the tower stands, and it only
  climbs. Selling loses it (see Sacrifice). It is not carried between levels.
- **Sources:**

  | Source | XP | Notes |
  |---|---|---|
  | a landed hit | 1 | misses earn nothing, so fire rate alone doesn't farm it |
  | a job event | 3 | per tower, the table below |
  | an assist | 5 | the tower damaged the enemy in the last 3 s, or has a DoT on it, when it dies |
  | a kill | 10 | |
  | presence | 2 | every tower whose range covers a death, whoever killed it |

- **Value weighting:** kill and assist XP scale with the dead enemy's rank: Grunt x1, Soldier
  x1.5, Veteran x2, Elite x4, Boss x8. Killing the Warden is an event; killing a swarm is not.
- **Per-tower hit scaling**, so towers that touch many enemies a second don't flood: the Pulse
  earns hit XP per second an enemy spends inside (not per tick), the Cinder per wave (not per burn
  pulse), the Splash per enemy caught, at most 5 a blast.
- **Job events** (3 XP each) follow each tower's purpose. This feature wires the ones whose
  mechanics exist today (a crit, a freeze, a reveal, an ignition); each tower's feature completes
  its row as its content lands.

  | Tower | Job events |
  |---|---|
  | Sniper | a crit; reaching full Steady Aim; an execution |
  | Splash | an arc jump; a hex cast; a hex passed on by Contagion |
  | Sonar | a reveal; an enemy Exposed by Ping; a Mark another tower spends |
  | Pulse | a Toll stack; a silence; a reveal |
  | Aura | none of its own: it earns 20% of the XP its buffed towers earn |
  | Mortar | each enemy beyond the first in one blast; an enemy Cracked; a zone pulse that lands |
  | Seeker | a freeze; a shield stripped; a salvo of 3 or more |
  | Cinder | an ignition; 5 Scorched stacks earned; a burning enemy revealed |

- **XP replaces the kill and damage-dealt gates** (and the purpose gates earlier designs asked
  for). A gated node reads "XP 300". **Layout gates stay**, because they are about the board, not
  progress: the neighbour count (`near-N`), and "next to an Aura" / "N different tower types
  adjacent" when content asks for them. Base nodes (Range, Attune, Awaken) stay price only.
- **Thresholds per level, the same on every tower** (starting points; the balance pass tunes the
  per-tower weights so a tower doing its job reaches head III around the same wave, whatever its
  job):

  | Node | XP |
  |---|---|
  | head I / II / III / IV | 0 (price only) / 100 / 300 / 800 |
  | extra node I / II / III / IV | 50 / 150 / 400 / 1000 |
  | every special | 300 |
  | Transcendent | 1500 |

- **Shown as a bar** in the tower panel with a tick at the next gate ("XP 120 / 300 to Railgun"),
  and the total in the tower's info rows.
- **Ranks you can see:** pips on the tower at XP milestones (Recruit, Seasoned, Expert, Hero),
  and a brief glow ring at each rank-up. No text on the board. Each rank gives +3% damage.
- **A rank perk at Expert**, one per tower, that changes play a little. Each reads its tower's
  passive, so each lands with its tower's feature: Sniper's Steady Aim starts at 1 stack;
  Splash's Saturation caps one higher; Sonar's Ping lasts a pass longer; Pulse's Toll starts at
  1; Mortar's Bracketing starts at 1 step; Seeker's nest +1; Cinder's Stoke starts at 1; Aura's
  Kinship counts its own type.
- **Catch-up:** a tower built after wave 5 starts with a quarter of the average XP of the towers
  of its type on the board. **Sacrifice:** selling a tower passes half its XP, split evenly, to
  the towers in the 8 cells around it.

### Price rules

- **Each extra copy of a tower type costs more**: +15% for every copy already on the board. The
  shop shows the current price. Upgrading becomes the efficient path for a fourth Sniper, and
  mixed defences pay off.
- **Upgrade prices start from a formula** of the tower's list price P (not the per-copy price):

  | Node | Price | Node | Price |
  |---|---|---|---|
  | Range I | 0.6P | head I | 1P |
  | Attune | 0.8P | head II | 1.5P |
  | Awaken | 1P | head III | 2.5P |
  | Range II | 1.2P | head IV | 4P |
  | Range III | 2P | extra I / II / III / IV | 0.8P / 1.2P / 2P / 3P |
  | Transcendent | 4P | every special, the second too | 4P |

  Specials cost more than head III on purpose: the most powerful slot should not be the cheapest.
  All of a tower's specials cost the same, the second one too; Transcendent carries the weight.
  Final prices come from the balance pass (feature 6), which measures effects and support as well
  as damage.

### Out of scope

- A third special slot, or anything beyond Transcendent.
- Refunding, respeccing or swapping a bought special.
- XP carried between levels (meta-progression).
- Player-chosen target priority, including click-to-focus. Each tower aims by a built-in strategy
  that fits its job; an upgrade may change it.
- The buy-once, per-type global upgrade (`TODO.md`).

## Interconnections

- **Replaces `FEATURE-tower-upgrades-iteration-2.md`**, deleted with this request's creation. Its
  open questions are answered under Decisions.
- **`FEATURE-tower-upgrade-trees.md`, `FEATURE-tower-specialization-abilities.md`** (implemented)
  say "one special", "Awaken unlocks head and special" and list kill and damage gates. Their
  status lines should say what this feature changed.
- **Features 2 to 6** fill each tower's Attune passive, Range III perk, level III and IV, extra
  node, specials, job events and rank perk. Feature 6's balance pass sets the final prices, XP
  weights and rank milestones.
- `td/tower/CLAUDE.md`'s "Upgrade tree" section describes slots and gate kinds, and becomes false
  when this lands.

## Constraints and open risks

- **IV-A / IV-B are a new kind of exclusivity**: two siblings under one prerequisite, each gone
  once the other is bought. Today exclusivity exists only between the roots of a slot.
- **A head node that coexists with everything is new.** Today every head root is offered only
  while the slot is empty. The head pips count owned nodes, so the extra node changes what "head
  level" means on the board.
- **Assists need a record** of which towers damaged an enemy recently, including DoTs that credit
  their applier through `DamageSink`. Hits, kills and effect applications are frequent: XP must
  stay inside the per-tick performance budget with every buildable cell a tower.
- **XP balance risks:** towers that touch many enemies a second flooding (the per-tower hit
  scaling answers it), support towers starving (job events, presence, Aura's share). Tuned in
  feature 6.
- **Per-copy prices and selling.** The sell value must stay coherent with what was paid, and
  selling then rebuying must not dodge the rising price.
- **Tests churn.** Many existing tests build nodes with kill or damage gates. Prefer moving them
  to shared fixtures over hand-editing each (standing preference: grow value types with `withX`,
  route setup through `td.fixtures`).
- **UI room:** the XP bar, the exclusive-choice mark and the locked Transcendent must fit the
  panel at a glance, in the HUD look, with no platform controls.
- Standing requirements: the engine stays headless (XP, gates and prices are engine state),
  board content goes through the render pipeline, UI looks the same on every OS.

## Decisions made

- The base chain is **Attune -> Awaken -> Transcendent**. "Fortify" was a working name and no
  longer fits: the step grants the tower's signature passive, not armour.
- Transcendent is Awaken's second level, gated on a special, a chain level III and XP, shown
  locked with what's missing. It opens head IV, extra IV, Range III and special slot 2.
- Range steps are +15%, +10%, +10%; Range III needs Transcendent.
- The extra head node doesn't branch, its IV needs Transcendent, and its III doesn't count toward
  Transcendent.
- The second special is a free pick from the tower's own (or its chain's) three. No written
  pairings, no generic pairing bonus.
- Single-special towers get their second and third specials in their own features.
- XP replaces kill, damage and purpose gates; layout gates stay. Everything in this request's XP
  section is decided; the numbers are starting points.
- Tower ranks are named Recruit, Seasoned, Expert, Hero: the enemy ranks already use Veteran and
  Elite, and the inspector shows both.
- The mechanism is proven in tests here and on the Sniper, the first tower to get full content,
  in feature 2.
- Tower display names don't change here; renames happen once, in feature 6.

## Open questions

- **Rank milestones.** XP for Seasoned, Expert and Hero (starting points 300 / 800 / 2000), and
  whether +3% damage per rank survives the balance pass.
- **Sell value under per-copy prices.** Refund what was actually paid, or the current list price?
- **Presence XP** for a tower that already got assist or kill XP from the same death: both, or
  the larger?
