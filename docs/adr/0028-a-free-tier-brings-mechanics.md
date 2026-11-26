# 0028. A free tier's node brings mechanics, granted by the XP it holds

- Status: Accepted
- Date: 2026-11-27

## Context
A tier's own node (`tier.N`) carried stats and procs, held from the XP that opens the tier, never bought or saved. A
mechanic only came with a bought node: `OwnedMechanics.add` at purchase. Each tower's signature debuff is to come from a
free tier, and some of those debuffs are mechanics with their own rules (a spell's currency, a shell's share), not procs.
Mechanics carry state, so they cannot be derived afresh on each read the way stats and procs are.

## Decision
- A tier says it brings mechanics (`UpgradeTier.withMechanics()`), and the tower's `MechanicCatalogue` lists them under
  the tier's node id (`UpgradeTree.tierNodeId(n)`), as for any node. The hover's words come from the same catalogue.
- `OwnedMechanics` holds two lists: the granted (earned tiers' nodes, lowest first) and the bought (purchase order).
  `grant` sets the first, keeping the instances of a node still earned; `renew` renews both. Readers see granted first,
  the order `heldNodes` gives procs.
- The tower syncs the granted list from `tiersEarned` whenever that count moves: in `earnXp`, in `restore`, and in
  `beginTick`, which catches a paid unlock bought on the EDT. A paid tier's unlock brings its mechanics this way too,
  never at purchase, so it acts when its stats do.
- Nothing is saved: a loaded game's XP grants the mechanics again.

## Consequences
- A tier node may be made of a mechanic alone; the tree makes it a node because the tier says so.
- A catalogue entry under a tier id whose tier does not say `withMechanics()` (and gives nothing else) brings nothing.

## Rejected
- *Reconciling all mechanics from `heldNodes` on every change*: the EDT's purchase and the game loop's XP would each
  rebuild the list from a view of the other's state that may be stale, and drop a node just bought.
- *A tier node for every tier, empty or not*: nodes that say nothing on the panel.
- *Buying the tier node automatically when the XP arrives*: it would be owned, so sold and saved.
