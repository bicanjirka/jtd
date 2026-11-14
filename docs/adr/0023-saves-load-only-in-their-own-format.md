# 0023. A save loads only in its own format

- Status: Accepted
- Date: 2026-11-14
- Supersedes: 0016

## Context
ADR 0016 fixed every node id once it shipped, so a save would keep loading after a tree was
rearranged. Replacing the upgrade slots with tiers removed nodes (Awaken on most towers,
Transcendent, Range III on the Aura), added the tiers' unlocks and changed what a node needs. An old
save names nodes that are gone, owns nodes in an order the new tree would not allow, and carries a
deeds column. Loading it would rebuild a tower other than the one saved and only name the gaps.

## Decision
A save's first line names its text format (`format 2`). A save of another format is refused with
that reason, never read as far as it goes. A change that moves, removes or re-gates nodes raises
the format; a renamed display name, a price or a stat does not. Ids are still kept where a node
survives, since a format change costs every player their saves.

## Consequences
- Upgrading the game can lose a saved game; the refusal says why.
- A tree change asks one question before it ships: does an old save still rebuild the same tower?
  If not, it raises the format.

## Rejected
- *A migration per format*: code to keep forever, for a hobby game whose saves last a session or two.
- *Keeping 0016 and loading what still matches*: the tower that comes back is not the one saved.
