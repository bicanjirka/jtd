# Feature Request: Tower Upgrade Visual Revamp

**Status:** implemented, 2026-10-24 (plan: `docs/plans/tower-upgrade-visual-revamp.md`). Refined in a grilling session; the design was accepted from the mockup
canvas (https://claude.ai/artifact/CTqU66VFJauPieb1CGes2S): Sniper and Aura at rest, a blocked
multi-step hover, a two-step path and the downgrade confirmation, all at 1:1.

## Summary

The Upgrades panel shows a tower's whole upgrade tree at once, like a talent tree, instead of a list
of the few nodes on offer. Lines run left to right over a faint chess-rook silhouette: the Base slot
at its foundation, the Head slot at its crenellations, the specials on a halo above it, and the
Attune -> Awaken -> Transcendent chain, now a slot of its own called **Core**, as a spine up its
wall. Every node shows whether it is owned, buyable, waiting for XP, a deed or money, sealed until a
Core step, or locked out. A click buys every step up to the node clicked; a right-click downgrades,
for a 75% refund. The panel never scrolls.

## Current state (what exists today)

- **The panel.** `PanelUpgradeTree` swaps with `PanelWaveInfo` in the lower sidebar cell while a
  tower is selected (`PanelGameConsole`; the sidebar is 300 px wide, the window 1100x740). Above it
  `PanelTowerInfo` stretches to fill the rest. The panel shows an XP bar, then per `UpgradeSlot` a
  header and at most three numbered `HudButton`s for the *offered* nodes only: a node that isn't
  offered is absent (`UpgradeTree.offered`). An exclusive choice is a `ChoiceBracket` plus "1 of N".
  Buttons carry 14 px icons from `TermLooks.faceOfLine`.
- **The tree model** (`td.tower.upgrade`). `UpgradeTree` is a list of `UpgradeNode`s plus
  `ExclusiveChoice`s. A node has an id, a slot (`BASE`, `HEAD`, `SPECIAL`), a price, `NodeEffects`,
  `requires` (prerequisites: which nodes must be owned) and a `gate` plus XP (checked at purchase),
  and an `UpgradeTier` that gives its level in its line. `UpgradeTree.firstOf`/`artNodeOf` find a
  node's line; `UpgradeTier.openedBy` says which chain step opens each tier.
- **The shared shape.** `StandardBaseSlot` builds every tower's `BASE` slot: the Range line and
  the chain Attune -> Awaken -> Transcendent. Attune opens head levels I-II, Awaken head III, Range II
  and a first special, Transcendent (gated on a special and a head III) head IV, Range III and a
  second special. Sizes differ: Sniper and Splash have 26 nodes with 6 specials (pick 2), five towers
  23 with 3 specials (pick 2), the Aura 16: no Transcendent, no level IV, 3 specials, pick 1.
- **Number keys.** `UpgradeTree.keyOf` fixes a key per role for every tower (1 the chain, 2 Range,
  3 the head line that does not branch, 4/5 the two exclusive head branches, 6-8 specials); the
  constructor rejects a node no key buys. `TowerDefense` routes digits to
  `GameEngine.buyUpgradeForSelected`.
- **Buying is one node at a time.** `AbstractTower.buyUpgrade` checks `offers` and `gateMet`, pays
  with `doPay`, runs `onUpgradeBought`, publishes stats and recomputes every tower. Multi-step
  buying exists only in tools: the `UpgradePaths.buy` test fixture buys a node with everything its
  `requires` needs, recursively and across slots, and `UpgradeTree.pathTo` lists that path for the
  balance tools (`CompetentPlayer`, `TowerLab`, `PurchaseHarness`, `BuildHarness`).
- **Nothing is ever removed.** `UpgradeState` (owned nodes in purchase order) and `OwnedMechanics`
  only grow; every tower's `onUpgradeBought` adds mechanics, and towers hold runtime state for some
  of them (charges, counters, a nest). `ExclusiveChoice` assumes a pick is for good. The README says
  "Every purchase is permanent for that tower".
- **Refunds.** `SellRefund.SHARE` is 0.75; `getSellPrice` refunds three quarters of the tower and
  every upgrade bought for it. Selling has no confirmation.
- **Confirmations.** The only one, abandoning a level (`TowerDefense.confirmAbandonLevel`), is a
  platform `JOptionPane`, outside the HUD look.
- **Hover.** Tooltips are `TooltipPanel`s owned by `TooltipLayer`; `TooltipLayer.watchSpots` serves
  one component with several hover targets. Hover never changes the Info panel.
- **Board marks.** `TowerSpriteFrameBuilder.slotPaletteFor` colours the slots (base green, head
  gold, special violet); `Java2DFrameRenderer.paintSlotPips` draws one pip per owned node grouped by
  slot, Transcendent's as a gold diamond keyed on the base palette, plus per-slot ready chevrons.
- **Saves** store a tower's owned node ids in purchase order (`TowerSave`).

## What this feature adds

### Where and how big

- The tree replaces the current Upgrades panel in the same cell, swapped with the wave preview. The
  Info panel above keeps whatever height is left (the mockup leaves it about 236 px).
- **No scrolling, ever.** Every tree fits the panel whole. Nodes are as large as the space allows
  (the mockup uses 32 px, specials 28 px), and may be smaller if that is what it takes to fit; where
  there is room, bigger. The panel never trades a visible tree for bigger icons.
- Line names appear only in hovers; nodes show the game's pictograms.

### The Core slot

- Attune, Awaken and Transcendent leave `base` and become a fourth slot, **Core**, coloured sky blue
  (about `#7FD4FF`). Base keeps the Range line.
- Each Core step gets its own pictogram (the mockup's stand-ins: a ring, an open eye, a crown), drawn
  large on the spine and small in the badges of the nodes it seals.
- What each step opens, and every price, XP and gate, stay as they are.

### The rook

- A faint chess-rook silhouette behind the tree: Base at its foundation, Head at its crenellations,
  the specials on a halo above, and Core as a spine up its left wall, Attune at the foundation,
  Awaken at the heart, Transcendent at the crown, linked into the halo.
- Each part takes a faint tint of its slot's colour once the tower owns a node there. The halo is
  only a ghost outline until Awaken opens it.

### The layout comes from the tree

- Positions are derived from data every tree already has: slot -> place on the rook, line -> row
  (in declaration order), level -> column, an exclusive pair of level IVs -> a fork. No tower has
  layout code or layout data of its own; a new tower of the same shape just works.
- Smaller trees sit on the same grid and leave gaps, so a node of a kind is in the same place on
  every tower and the panel's height doesn't change between towers.
- A tree whose shape doesn't fit the grid (too many lines in a slot, a level past IV, too many
  specials) is rejected when it loads, naming what doesn't fit, the way a node no key buys is today.

### Node looks

| Look | Meaning |
|---|---|
| Filled in its slot colour | owned |
| Glowing, price under it | buyable now |
| Glowing, red price | only money missing |
| Dimmed, progress ring around it | XP or the node's gate (a deed, towers nearby) unmet; the ring shows XP first, then the gate |
| Dashed outline | needs the node before it in its line |
| Grey, sky-blue badge with a Core glyph | sealed until that Core step is owned (wins over the ring) |
| Hatched ash, broken links | locked out by an exclusive pick |

- Prices show only under the next node of each line and under open specials; every other price is in
  the hover.
- The XP bar stays, slimmed to one line (XP, a thin bar, the next XP gate).
- Number keys stay and keep buying one step. A small key digit sits on each line's next node (none on
  a sealed node, which the key can't buy). Specials show no digit.
- Hovering a Core node lights every node it opens, in every band.

### Exclusive choices

- The members of an either-or (the two head roots, each branch's two level IVs) meet at a diamond.
- Hovering a member, or any node whose path runs through one, marks with red cracks every node the
  purchase would lock out, and the hover names them. After the purchase the losers turn to ash and
  their links break; losing a head root ashes that whole branch.
- The specials' halo has no budget mark: their own looks carry it (open, sealed until Transcendent,
  ash once every pick is used). The hover explains the picks ("uses your 1st of 2; the 2nd opens with
  Transcendent").

### Buying several steps

- Hovering any node highlights its path (the nodes and links still to buy, in white) and a plain
  left click buys it. A single next step is a path of one, and only its link lights.
- The path reaches across slots, like the `UpgradePaths` fixture: Core steps and earlier nodes of the
  line. Clicking a sealed node therefore works and buys its Core steps too. A path never picks a
  special or an exclusive member off the clicked node's own line.
- **All or nothing.** If any step's XP or gate is unmet (Transcendent's special included), or the cash
  doesn't cover the total, nothing is bought. The hover shows this before the click: the first
  blocked step is marked, or the shortfall named.
- The hover popup gains a path section: each step and its price, the total, any blocker, any lock-out.
  While hovering, the panel's footer swaps from "Click: buy · Right-click: downgrade" to the path's
  step count and total ("2 steps · $48").

### Downgrading

- Right-clicking an owned node downgrades it **and every owned node that depends on it**, cascading.
  Right-clicking Attune removes most of the tree.
- Only prerequisites cascade; gates are checked at purchase only. Removal is decided by replaying the
  remaining nodes in purchase order and dropping those whose prerequisites now fail, so with two
  specials owned and Transcendent removed, the later special goes and the first stays.
- An exclusive choice counts only what is owned now: downgrading a pick reopens its rivals, and
  downgrading a special frees a pick.
- The refund is 75% of what the removed nodes cost (the sell share). Downgrading then selling pays
  what selling would have.
- Allowed at any time, mid-wave included, until the level ends, like selling.
- Hovering an owned node outlines in red what a right-click would remove; the footer and the popup
  read "Right-click: downgrade 3 · +$87".
- A right-click opens a HUD-styled confirmation beside the node: the nodes going with their prices,
  the refund, what reopens, a red-text "Downgrade ( +$X )" button styled like Sell, and Cancel. Esc,
  a click elsewhere or selecting another tower cancels. The game keeps running; the list and refund
  are worked out again when confirmed.
- What a removed node's mechanic has already put into the world (a zone on the ground, an effect on
  an enemy) runs out its time, as when a tower is sold. What the tower itself holds for it (charges,
  counters, a primed shot) is cleared at once. XP is never refunded or lost.

### Also in scope

- The board gets a Core pip group in sky blue and a Core ready chevron; Transcendent's pip stays a
  diamond.
- The abandon-level confirmation moves to the new HUD popup, ending the last platform dialog.

### Out of scope

- A confirmation for selling.
- Previewing stat changes in the Info panel on hover.
- Downgrading in the headless balance players, and multi-step buying from number keys.

## Interconnections

- **Supersedes the panel of `FEATURE-tower-upgrade-trees.md`** (header lines plus three buttons per
  slot) and that document's three-slot model: Core becomes a fourth slot. `FEATURE-tower-progression.md`
  owns the chain's rules, which stay as they are; only its slot changes.
- **`FEATURE-tower-visual-revamp.md` (draft) comes next**, and covers the towers themselves; this
  feature covers only the upgrade panel and the upgrade model behind it. The revamp removes slot pips,
  per-slot chevrons and the halos in favour of one "upgrade available" arrow, and makes Attune, Awaken
  and Transcendent change the tower's shape, so the Core pips and chevron this feature adds are
  short-lived: the revamp removes them with the rest. Its Core-steps-change-the-shape idea fits Core
  being its own slot.
- **`FEATURE-hover-popups.md`** owns the tooltip system the node hovers and the confirmation build
  on.
- **The README** content (three slots, "every purchase is permanent", the chain under `base`) changes
  with this feature.
- **`TODO.md`'s command-queue entry** lists EDT-originated mutations; a downgrade is a new one of the
  same kind as a purchase.

## Constraints and open risks

- **Fit is a hard requirement.** The full Sniper tree in the mockup takes about 420 px of a column
  whose real usable height hasn't been measured in the live game; the Info panel keeps the rest.
  Nodes may shrink to fit; how small is still readable, and whether the size adapts to the window or
  is fixed, is for planning (see Open questions).
- **Downgrading is new ground for the model.** Owned nodes, owned mechanics and exclusive choices
  only ever grow today; every tower's `onUpgradeBought` assumes it is called once per node, forward.
  Removing nodes must take back stats, capabilities, targeting, hit bonuses, procs, emission and
  per-tower runtime state, and every neighbour's buffs, consistently.
- **Paths have to be checked before paying.** Gates and prerequisites read the live tower, and a
  later step's condition depends on the earlier steps being owned. "All or nothing" means a path is
  judged as if bought, before any credit moves, and paid as one atomic charge.
- **Reopening choices changes a rule tests and tools may assume** (a spent choice stays spent).
- **Generic layout vs. key mapping.** `UpgradeTree.keyOf` already encodes the shared shape (chain,
  Range, extra line, two branches, specials); the layout rule and the fit check will encode the same
  shape again. They must not drift apart.
- **Moving the chain out of `BASE`** touches the key mapping, slot colours, slot header text, the
  Transcendent crown pip (keyed on the base palette today), the README, and tests that name
  `UpgradeSlot.BASE` for chain nodes (about five test files). Node ids are plain strings, so saves
  keep loading whichever ids are chosen.
- **Performance.** `offered()` already runs for every tower on every frame build; the panel refreshes
  from the EDT render pulse. Path, lock-out and downgrade previews are per-hover work on the EDT and
  must stay out of tick and frame-build code, within the frame budget.
- **Standing rules:** the engine stays headless, so buying a path and downgrading are engine
  operations the panel only calls; every control paints itself in the HUD look (the confirmation
  included); the tests prove mechanics on fixture trees, never a built-in tower's nodes; and
  `td/ui/CLAUDE.md`'s line "An upgrade button paints its perk icon beside its row text" becomes
  false with the panel it describes.

## Decisions made

1. The tree lives in today's Upgrades cell; the Info panel shrinks above it.
2. The panel never scrolls. Nodes may shrink to fit; bigger where the space allows.
3. Attune, Awaken and Transcendent move to a new fourth slot, **Core**, sky blue, drawn as a spine
   up the rook from foundation to crown. Rejected: a base row (consistent but taller), inferring the
   spine from the data (implicit), putting the chain in the `SPECIAL` slot (muddles the picks).
4. Specials sit on a halo above the rook, not in its heart.
5. Positions are derived from slot, line, level and exclusive choices; no per-tower layout. Smaller
   trees keep the same grid with gaps; a tree that doesn't fit is rejected at load.
6. The rook's parts tint faintly in their slot colour once the slot owns a node; the halo is a ghost
   until Awaken.
7. Node looks as in the table above: Core-sealed nodes are grey with a Core-glyph badge, distinct from
   "not yet" nodes, which keep their colour with a ring (XP, gate) or a red price (money).
8. Each Core step gets its own pictogram; hovering a Core node lights what it opens.
9. Prices only under each line's next node and open specials; the slim XP bar stays; number keys stay,
   with a digit on each line's next node.
10. Either-or members meet at a diamond; hovering previews lock-outs with red cracks; losers become ash.
    No budget mark on the halo.
11. A plain click buys the whole path, across slots, all or nothing; the popup and footer show steps
    and total.
12. Right-click downgrades the node and its dependents (prerequisites only), with a HUD confirmation,
    at a 75% refund, at any time, and reopens exclusive choices. Hovering an owned node previews it.
13. Leftovers of a downgraded mechanic: the world's run out, the tower's are cleared.
14. The abandon-level dialog moves to the new HUD popup. Selling stays one click.

## Open questions

1. **Minimum node size and adaptivity.** What is the smallest node that still reads (24 px?), and does
   the size adapt to the window and Info panel at runtime or get fixed at design time from the
   largest tree?
2. **Measured fit.** The real height the sidebar gives the panel, measured in the live game, decides
   whether 32 px holds for Sniper and Splash.
3. **Core ids and names in text.** Keep the `base.attune`-style ids, or rename to `core.*`? Saves load
   either way only if old ids still resolve.
4. **Specials and number keys.** Keys 6-8 cover three specials; Sniper and Splash have six. Whether
   their specials keep keys, and how a key digit would show on the halo, is unsettled (the mockup
   shows none).
5. **Pictograms for the Core steps** (the mockup's ring, eye and crown are stand-ins) and their look at
   badge size, about 10 px.
