# 0016. Upgrade node ids are permanent

- Status: Accepted
- Date: 2026-10-25

## Context
A save records each tower's upgrades as the ids of the nodes it owns, in purchase order, and
loading rebuys them by id (0011). A renamed id brings a tower back without that upgrade, and
without any later node that needs it. Loading names what it couldn't rebuild, but the save is
spoiled all the same.

## Decision
A node's id is fixed once it ships. Its player-facing name, its slot and the tree's layout can all
change around it. The upgrade visual revamp moved Attune, Awaken and Transcendent into a new
`CORE` slot, and they kept their `base.*` ids.

## Consequences
- Some ids read slightly wrong (`base.*` in the `CORE` slot). Players never see them.

## Rejected
- *New ids with an alias table or a save migration*: code to keep forever, for no gain a player can
  see.
