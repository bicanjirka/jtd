# Feature Request: Tower Progression

**Status: not started.** Feature 1 of 7 in the tower rework (order in `TODO.md`). It replaces
the never-implemented `FEATURE-tower-upgrades-iteration-2.md` and carries over every answer that
request left open.

## Summary

The second generation of tower upgrades. A tower climbs a three-step base chain (Attune ->
Awaken -> Transcendent), its head chains reach level III and then fork into two overpowered
options (IV-A | IV-B), every tower gains a non-exclusive extra head node, and Transcendent opens
a second special. The panel finally says which purchases are exclusive before the player pays, and
prices follow one rule.

This feature builds the mechanism and the price rules, and moves today's content into the new
shape. What gates each node (XP and purpose gates) is feature 2. Each tower's new content (its
Attune passive, level III and IV, extra node, new specials) arrives with its own feature, 3 to 7.

## Current state (what exists today)

- **Three slots** per tower, `base` / `head` / `special` (`FEATURE-tower-upgrade-trees.md`,
  implemented). `base` holds Range and Awaken, both price only (`StandardBaseSlot`). Awaken opens
  the head roots and the special roots.
- **Head:** two chains that exclude each other. Most stop at level II; Sonar's Twin Array and both
  Splash chains reach III. Nothing has a level IV or a third, non-exclusive head line.
- **Special:** one per tower, ever. Sniper, Sonar and Splash offer three; the others one.
- **Panel:** up to three offered nodes per slot, numbered across the slots for the number-key
  shortcuts. Every button already says why it can't be bought (its gate's progress). Nothing says
  a choice is exclusive: the player learns after paying. On the board, one row of pips per slot
  and a glow while the tower owns a special.
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
  owned, not a separate button. It needs an owned special and a chain's level III, plus its price
  (and XP, feature 2). It has no stat bonus of its own. It is **offered as soon as Awaken is
  owned and says what's missing** on its button ("needs a special and a level III head"), the way
  every button already says why it can't be bought: it is the goal, and hiding it hides the game's
  ceiling. Its look: a slow halo ring on the base and a gold pip.
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
  level I is owned, and offers that chain's set; until then the special slot says "choose a chain
  first". Slot 2 picks from the same three. The pick is free: no written pairings, since a tower's
  specials share one trigger (the Sniper's are all crits) and combine by themselves.
- **Aura caps at Awaken**: no Transcendent, no Range III, no level IV, one special. Its chains and
  extra node stop at III.
- The panel still shows at most three offered nodes per slot. A fresh head slot offers the two
  chain roots and extra I: exactly three.

### Today's content in the new shape

- Every existing node keeps its id and effect until its tower's feature reworks it. Attune is
  inserted before Awaken on every tower.
- Transcendent can be reached once a tower's tree has a level III head (the Sonar's and the
  Splash's do today). Until a tower's feature adds level IV and the extra node, Transcendent opens
  only its second special and Range III there.
- No placeholder nodes: a slot shows only content that exists.

### The exclusive-choice mark

- Nodes that exclude each other are drawn as one linked set: a bracket joining them and a "1 of 2"
  / "1 of 3" label. The same mark serves every exclusive choice: the two chain roots, a tower's
  specials (in both slots) and IV-A / IV-B. A node outside such a set carries no mark, so no mark
  means "safe to buy". The extra head node is never in a set.
- While a set is on offer, the panel names what a pick locks out (the other chain's name), not
  just that something will be.
- Once a choice is made, the owned node's row says what it was chosen over, with a lock glyph
  ("chosen over Marksman's Eye"). The panel doesn't keep listing the locked alternatives.
- Panel only; the tower's pips on the board stay as they are. Drawn by the panel in the HUD look,
  readable without colour (glyph and label).

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
  Final prices come from the balance pass (feature 7), which measures effects and support as well
  as damage.

### Out of scope

- A third special slot, or anything beyond Transcendent.
- Refunding, respeccing or swapping a bought special.
- Player-chosen target priority, including click-to-focus. Each tower aims by a built-in strategy
  that fits its job; an upgrade may change it.
- The buy-once, per-type global upgrade (`TODO.md`).
- Gates (feature 2): until it lands, the new nodes are price only and today's kill and damage gates
  stay on today's nodes.

## Interconnections

- **Replaces `FEATURE-tower-upgrades-iteration-2.md`**, deleted when this request was written.
  Its open questions are answered under Decisions.
- **Feature 2** sets every node's gate (XP, purpose and layout) and Transcendent's XP.
- **`FEATURE-tower-upgrade-trees.md`, `FEATURE-tower-specialization-abilities.md`**
  (implemented) say "one special" and "Awaken unlocks head and special". Their status lines should
  say what this feature changed.
- **Features 3 to 7** fill each tower's Attune passive, Range III perk, level III and IV, extra
  node and specials. Feature 7's balance pass sets the final prices.
- `td/tower/CLAUDE.md`'s "Upgrade tree" section describes the slots and becomes false when this
  lands.

## Constraints and open risks

- **IV-A / IV-B are a new kind of exclusivity**: two siblings under one prerequisite, each gone
  once the other is bought. Today exclusivity exists only between the roots of a slot.
- **A head node that coexists with everything is new.** Today every head root is offered only
  while the slot is empty. The head pips count owned nodes, so the extra node changes what "head
  level" means on the board.
- **Per-copy prices and selling.** The sell value must stay coherent with what was paid, and
  selling then rebuying must not dodge the rising price.
- **UI room:** the exclusive-choice mark and the waiting Transcendent must fit the panel at a
  glance, in the HUD look, with no platform controls. Node names must stay short enough to fit a
  panel row.
- **Tests churn.** Tests that walk today's trees meet Attune before Awaken. Prefer moving them to
  shared fixtures over hand-editing each (standing preference: grow value types with `withX`,
  route setup through `td.fixtures`).
- Standing requirements: the engine stays headless, board content goes through the render
  pipeline, the UI looks the same on every OS.

## Decisions made

- The base chain is **Attune -> Awaken -> Transcendent**. "Fortify" was a working name and no
  longer fits: the step grants the tower's signature passive, not armour.
- Transcendent is Awaken's second level, needs a special and a chain level III (and XP), and is
  offered with what's missing rather than hidden. It opens head IV, extra IV, Range III and special
  slot 2. Its look is the halo ring and gold pip; recolouring every tower's shot was dropped (eight
  towers' shot drawings for a cosmetic).
- Range steps are +15%, +10%, +10%; Range III needs Transcendent. (The tower sections of the
  brainstorm wrote "Range II +15%"; the decided steps win.)
- The extra head node doesn't branch, its IV needs Transcendent, and its III doesn't count toward
  Transcendent.
- The second special is a free pick from the tower's own (or its chain's) three. No written
  pairings, no generic pairing bonus.
- Single-special towers get their second and third specials in their own features.
- After an exclusive choice, the bought node's row names what it beat; the panel doesn't keep
  drawing locked alternatives (that needs rows for nodes that are no longer offered).
- The mechanism is proven in tests here and on the Sniper, the first tower to get full content,
  in feature 3.
- Tower display names don't change here; renames happen once, in feature 7.

## Open questions

- **Sell value under per-copy prices.** Refund what was actually paid, or the current list price?
