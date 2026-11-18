# Feature Request: Next-gen Upgrade Tiers

**Status:** implemented 2026-11-14 (every tower moved onto tiers with its current nodes; the towers' own next-gen
trees follow in their docs). Draft 2026-11-10, reviewed with the whole bundle 2026-11-13. Part of `FEATURE-next-gen.md`; from the
handwritten notes of the visual request (in git history with `FEATURE-tower-visual-revamp.md`).

## Summary

Every tower's upgrade tree becomes a stack of tiers that climbs from the bottom. A tier opens through its gate; once
it is open, every upgrade in it costs only credits. The Attune → Awaken → Transcendent chain and the Core, Base,
Head and Special slots go. The goal is a more linear path through a tower and simpler choices.

## Current state (what exists today)

- A tree has four slots (`UpgradeSlot`): `CORE` (Attune → Awaken → Transcendent), `BASE` (the Range line), `HEAD` (two
  chains of levels I-IV and an extra line) and `SPECIAL` (sets of up to three). `StandardBaseSlot` builds Core and
  Base the same for every tower. `UpgradeTier` sets each step's price (a multiple of the list price), its XP and the
  Core step that opens it.
- Gates: XP from bounty (`ExperienceAwarder`), and conditions (`UpgradeCondition`): a purpose gate counting the
  tower's deeds once Attune is owned, a cluster of nearby towers, Transcendent.
- Choices are `ExclusiveChoice`: the head roots and level IV pairs are pick-one, special sets pick-two. A choice is
  permanent until the player downgrades.
- `buyPath` buys a node with everything it needs in one charge; `downgrade` removes a node and what depends on it,
  with a refund. Both work during a wave; only a finished level blocks them.
- The panel (`PanelUpgradeTree`, `UpgradeTreeLayout`) is a fixed 300 × 420 design: a rook, Core up its wall, Range
  along its foot, head rows by key, specials on a halo. Nodes get number keys 1-8 from their place (`keyOf`).
- The board shows per-slot pips and one chevron per slot with an affordable node.
- A save stores the ids of the nodes each tower owns; ADR 0016 keeps shipped ids forever for that.
- `TODO.md` ("The lab can't read a line past about 3.33x") measured that only 10-19 of about 160 lines per level beat
  building another tower.

## What this feature adds

- **Tiers, bottom up.** Tier 0 is open the moment the tower is built and needs no name: the place for the first basic
  upgrades. Each tier above has a gate. A tower may have any number of tiers and any number of upgrades in each.
- **Gates.** XP. A deed count (a "quest") or any other condition a gate can be today stays possible, but no tree
  uses one (open question 7). A locked tier shows how much is missing. A tier may also cost credits to unlock (open
  question 1).
- **Unlocking.** A free tier opens by itself once its gate is met, and the board animates it
  (`FEATURE-next-gen-tower-looks.md`, question 2). A paid tier waits for the player's click, and the tower's one up
  arrow shows while it can be paid.
- **Three kinds of upgrade in a tier:**
  - an **orphan**: no prerequisite and nothing after it;
  - a **cluster**: upgrades drawn together in one frame, bought or unlocked together; the player keeps one of them
    active and can switch to another for free between waves (the cluster stays paid; what came after the old
    member is sold back);
  - a **prerequisite and its follow-up**, in the same tier or the next.
- **A fork gate.** A tower whose identity is a fork (the Burst's Stormcaller or Hexer) picks its side at a tier's
  gate, in the space between that tier and the one below. Meeting the gate opens the pick; the tier and every tier
  above show only the chosen side's upgrades, plus the ones both sides share. Hovering a side before picking shows its
  tree, dimmed, so the pick is never blind. Picking the other side later works only between waves, by clicking it in
  the gate, and sells back the old side's upgrades.
- **Two columns above a fork gate.** From the fork's tier up, each tier has a narrow column on the left for what
  both sides share, at most 2 upgrades, and a column on the right for the chosen side, at most a cluster of two
  and two more upgrades. Before the pick, the side's column is empty.
- **A tier is one row.** A tier fits in one row of the panel, about 6 upgrades. A tree that needs more spreads them
  over more tiers.
- **Stat and ability upgrades look different.** An upgrade that adds an ability or a skill is drawn differently from
  one that changes a number or improves something the tower already has: by shape as well as colour.
- **Colour.** A locked tier is grey; an open tier takes its own colour, chosen per tower to fit it. Fewer colours on
  links and frames than today. Glyphs may stay the same as today's or close to them.
- **The panel** reads bottom to top. Where every upgrade sits is worked out from the tree, never configured per tower.
  The whole tree can be hovered from the start, as today. A tier whose every node is bought folds into one thin
  row. Number keys 1-6 buy in the highest open tier, left to right; a cluster's key buys it and takes its first
  member; lower tiers are bought with the mouse.
- **The panel's look is reviewed with it.** If the new tree stands taller than today's panel, its graphics,
  margins and overall look and feel are reviewed as a whole, not patched row by row (the user, 2026-11-13).
- **One choice for a whole wave.** Between waves (from a wave's clear to the next send, where a save is made) the
  player may buy, sell, downgrade, switch a cluster pick and pick the other side of a fork. While a wave runs they
  may buy towers and upgrades (a cluster's first pick is a purchase) and sell or destroy a whole tower, but not
  switch a pick or downgrade. The panel keeps what is blocked visible and says
  why on hover ("Changes between waves").
- **Every current tree moves over roughly**, so the whole game runs on tiers from this feature on: seven trees, as
  the Radar leaves the game before this feature (`FEATURE-next-gen.md`). Each tower's real next-gen tree comes later,
  in its own doc.

### How a node reads

Today each node reads like a legendary perk: it adds something everywhere, and the many effects make a confusing tree
instead of a combinator. A next-gen tree builds from small, plain pieces the player combines:

1. **One node, one change**: a number, or one trigger and one result. Its tooltip says it in one line. An identity
   pick is the one exception: it may carry the two or three numbers that make the tower another kind (open
   question 10).
2. **Few triggers.** A tower uses a handful, such as a crit, its special shot and a kill, never "a crit or a dodge".
3. **A small set of names per tower**, about 8 (per side, for a forked tower). Its doc lists them; a node uses one
   or adds one. The review board counts them.
4. **Few debuffs.** One or two enemy debuffs per tower (per side), from its set.
5. **A cluster is a real alternative**: the same trigger with different results, or the same slot with different
   contents. Its frame carries a name that says which ("On crit"). Its members are even, or each wins in its own
   situation (a boss or the rest, one enemy or a crowd), never one right answer.
6. **Paths combine; they don't fork**, unless a tower's identity is the fork (the Burst's Stormcaller or Hexer).
7. **After names what a node needs.** A node comes after a cluster member when it belongs to that pick (Deep Doom
   after Doom), and after a node or a whole cluster it does nothing without (Thick Mix after Payload, a hex's
   follow-ups after Hex): the chain is a road the player commits to. A node that only works better beside another
   has none, and neither has a second step of the same number ("Long Barrel II"). On a forked tower, After also
   names the side; the side's column shows that, so the panel draws no line for it.

## Interconnections

- `FEATURE-next-gen-tower-looks.md`: a tower's look follows its tier, and the tier unlock is a moment on the board.
- `FEATURE-next-gen-tower-abilities.md`: ability upgrades are bought here.
- Each `FEATURE-next-gen-<type>-tower.md` holds its tower's tier tables.
- ADR 0011 (saves hold only between rounds), ADR 0016 (node ids are permanent).
- `FEATURE-key-hints-and-hud-unification.md` (shipped) gave nodes their number keys.

## Constraints and open risks

- The panel's height: a tall tree may not fit the 420-unit panel. The review board draws today's height as a line.
- Purpose gates count deeds only once Attune is owned, and Attune goes. No next-gen tree uses one, so they go
  with it unless a tree asks for one (open question 7).
- Rules that name the Core chain go with it: `td/tower/CLAUDE.md` allows `owns(...)` in tower code only for the Core
  chain, and Checkstyle enforces it.
- `TowerStatCatalogueTest` and every tower's tree tests are written against slots and levels: keep the churn small.
- The board's per-slot pips and chevrons read slots; `FEATURE-next-gen-tower-looks.md` replaces them.
- Tier-switching rules meet ADR 0021 (every wave starts from its save) and ADR 0019 (an EDT command is a request the
  next tick consumes).

## Decisions made

- Tiers replace the slots and the Attune → Awaken → Transcendent chain.
- Tier 0 is open from the start and unnamed.
- Once a tier is open, everything in it costs only credits.
- A cluster is bought once; one member is active; switching costs nothing and refunds nothing for the member
  itself. Only what came after the old member is sold back, at the sell price: a refund for the member would pay
  the player for every switch.
- Picks and downgrades change only between waves; buying and selling a whole tower go on during
  a wave. A pick the player can undo for free at any moment is no choice: the player who pauses to swap
  Thunderclap for Thunderstrike before each boss would win, and play would turn into micromanagement. A tower's
  kit then changes only at a wave's edge, where ADR 0021 resets its fight state anyway, so no ability bar,
  primed shot or magazine has to follow a switch mid-fight. Selling and rebuilding stays possible for an
  emergency, but costs the sell share and the tower's XP, so it is no way around the rule. A node bought by
  mistake during a wave stays until the wave ends.
- A node that comes after a cluster member the player didn't pick is hidden, not greyed out, and the panel shows
  that a hidden branch is there.
- A fork that is a tower's identity is a fork gate, not a cluster inside a tier. With both sides drawn in one tree,
  the Burst's panel was unreadable: twice the nodes, wrapped rows and crossing links, with the most important choice
  drawn as one node among many. A switch sells back the old side at the sell price, since a dormant side the panel
  no longer shows would be bought power the player can't see.
- A tier is one row; above a fork gate, a shared column and a side column. Of the layouts tried on the review
  board (shared nodes in the row, in a line of their own, in a column of their own), the columns were the only one
  without wrapped rows, and the lowest.
- Stat and ability upgrades look different.
- Nodes follow "How a node reads": one change each, few triggers, few names, few debuffs.
- All seven trees move onto tiers in this feature, roughly. No two systems side by side.
- Gates are XP (open question 7); a tier's unlock price decides whether it opens by itself (open question 1).
- Saves don't load across versions; ADR 0016 is superseded by a new ADR.

## Open questions

⭐ marks the preferred option.

1. ✅ **Unlocking a tier.** Both, by one rule: a tier with an unlock price waits for the player's click, one without
   opens by itself and the board animates it. Every draft prices Tier I ($20, a placeholder), so the prices are tuning
   for the balance pass (`FEATURE-next-gen.md`, "Answered by the user"), not a choice of system.
2. ✅ **When a cluster pick can change.** Only between waves.
3. ✅ **When a downgrade can happen.** Only between waves.
4. ✅ **A follow-up of an unchosen cluster member.** Whatever is easiest to build (the user, 2026-11-13), and only
   between waves, as every sale and downgrade is. The easiest is today's `downgrade`: switching a pick downgrades the
   old member, which takes what depends on it with it and refunds that at the sell price, then picks the new one for
   free. The cluster itself stays paid: the old member is not refunded (final review).
   A fork gate's switch is the same downgrade of the old side. One path for selling, downgrading and switching. In the
   drafts it reaches only the Hexer's Deep Doom, Blight Cloud and Choking Blight.
5. ✅ **Number keys.** Keys 1-6 buy in the highest open tier, left to right (a tier is one row of about six), and a
   cluster's key buys it and takes its first member; lower tiers are bought with the mouse (the user, 2026-11-13).
6. ✅ **A tree taller than the panel.** Fold a tier whose every node is bought (or every cluster picked) into one
   thin row, and scroll only if that is not enough. If the review board shows the new tree over today's 420-unit
   height, the panel's graphics, margins and overall look are reviewed with it (the user, 2026-11-13). Not yet
   measured on the review board: the tiers plan starts there.
7. ✅ **Gate kinds.** XP only: all seven trees gate on XP, the Mortar and the Obelisk dropped their purpose gates, and
   the Beacon's doc settles it. A deed gate (a "quest") stays possible for a later tree.
8. ✅ **The fork's columns.** A side's column holds a cluster of two and two more nodes: the Stormcaller's Tier II
   (Chain Lightning or Wild Arc, Conductor, Short Circuit) and Tier III (Thunderclap or Thunderstrike, High Voltage,
   Feedback) are the widest rows in the drafts, and the layout is worked out from the tree, so the column is as wide
   as they need (final review). No Stormcaller node moves.
9. ✅ **The tier's look in the panel.** Each tier's row shows, at its left, a small picture of the tower's foundation
   at that tier, grey while locked: the player sees what the tower grows into (final review, autonomously). The art
   pass drew the thumbnails, with every tier colour: `next-gen-art/icons.png`.
10. ✅ **Identity picks and "one node, one change".** Soulfire (burns the immune, Flame +10%, half a magic resist),
    Dragon's Breath (length, reload, Flame), Ruby (×2 damage, holds the freeze-immune), Shade (Shadow damage, Silence
    after the thaw), Burden (deeper drag, a higher cap) and Lance (range for width) each carry two or three changes.
    Accepted as the rule's one exception: a pick that changes what the tower is may carry the two or three numbers
    that make it so, as long as its tooltip still says it in one line (final review, autonomously).
