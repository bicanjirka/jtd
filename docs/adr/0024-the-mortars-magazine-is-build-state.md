# 0024. The Mortar's magazine is build state a save keeps

- Status: Accepted
- Date: 2026-11-15

## Context
The Mortar fires from a magazine: a short queue of shells drawn by lot, which the player sees and plans around
(a Heavy two shots away, a Nuke at the front). ADR 0021 makes every wave start with every tower ready and ends a
wave's fight state at its clear. A magazine drawn afresh at each wave would show the player shells they never get, and
one drawn from a reset would make the board a wave starts from depend on a draw the save does not hold.

## Decision
- **The magazine is build state.** No wave clear or send resets it; it carries from one wave into the next as the
  tower's upgrades do.
- **The save keeps it shell by shell** (one `magazine` line per Mortar), and a load puts it back as saved.
- **Only the game loop draws.** A firing draws the shells it used, and each tick drops the kinds the tower no longer
  owns and draws behind, from the shared random. Buying or selling a node on the EDT changes the weights, never the
  shells.

## Consequences
- A save still replays tick for tick (0021): the shells, and the random they were drawn from, are both in it.
- A loaded game shows the magazine the player left.
- A later tower that draws by lot (the Aerie's nest) decides the same question for itself; its state is build state
  only if the player sees and plans around it.

## Rejected
- *Reset the magazine at each wave to the opener.* The shells the player saw between waves would not be the ones fired.
- *Draw at purchase.* The draw would run on the EDT and take from the shared random off the game loop, so a replay would
  diverge with the player's timing.
