# Feature Request: The second balance pass, once the next-gen towers are in code

## Summary

The first balance pass set the shape of every level against a tower that exists only on paper. Once the next-gen
towers are built, the same levels have to be played by them: this pass sets how hard the game is, checks every
tower against the floor and the ceiling in play, and retunes what the paper could not see.

## Current state (what exists today)

- `docs/BALANCE.md` describes the method. Enemies are weighed against the Walker; waves are priced in the reference
  tower stored in `.claude/skills/jtd-balance-tools/reference.json` (the Sniper), with the bare Burst as the floor
  at about 1.2 times its cost.
- Each level's room (credits banked before a round over what the reference defence costs) follows one line: about
  1.3 in the first round to 1.0 in the last, 1.1 to 0.7 on The Last Hour. Gates sit about 15% under it.
- No absolute difficulty was chosen. The line was picked so that levels are even with each other and within
  themselves, nothing more.
- The towers in code today are stronger than the reference. On the retuned waves the competent in-code player wins
  every play of Iron Crossing (holding about 200% of the enemy health), The Storm Ridge (about 160%) and The Last
  Hour (about 130%) with no round close to a leak, and loses every play of The Amber Hive in its last round, the
  Warden's. Two seeds each; The Rift and The Labyrinth were not played.
- The reference tower's tiers add less damage per credit than another bare tower does, so on paper a defence grows
  by building wide first. Whether the built towers behave that way is not known.
- `BalanceReport`, `WaveCheckReport`, `PurchaseHarness` and `BuildHarness` play levels with the towers in code; they
  were not used for tuning in the first pass, since the towers they play are being replaced.
- Clear bonuses differ by round and were set to close the gap the reference tower leaves before a gate.
- Lives were not measured: the rig counts any leak as a loss.

## What this feature adds

- **A chosen difficulty**: what a competent player should hold, how often they should win, and how that differs
  from the first level to the last, stated as targets the in-code reports can check.
- **Every level played by the built towers** and its waves, starting credits and clear bonuses retuned to those
  targets, keeping each level's rounds, lanes, theme, boss and gates.
- **Every tower checked in play against the floor and the ceiling**: no tower below the bare Burst, none above the
  Sniper on one target, and any breach reported.
- **Each gate checked for what it asks**: that the answer the gate was written for is the one that passes it, and
  that at least two builds pass every gate.
- **The room line re-read**: what room on paper a level that plays well turns out to have, so the paper tools can be
  used alone for the next level.
- **Lives set per level** from how many leaks a level should forgive.
- **Bosses tuned in the rounds they lead.**

Out of scope: new towers, new enemies, new levels.

## Interconnections

- `FEATURE-next-gen.md` and every next-gen tower doc: this pass starts when their towers are in code and does not
  change their designs; a tower that breaches the floor or the ceiling goes back to its own doc.
- `shipped/FEATURE-balance-before-next-gen-towers.md`: the roster this pass tunes, final: the Templar, the insulated
  Relay, reinforcements on a timer that pay nothing. The Warden and The Labyrinth are proved in play.
- `TODO.md`, "Waves and clear bonuses are tuned on paper, not in play": this request closes it.

## Constraints and open risks

- The in-code players are provisional: they buy what the lab says is worth buying, and the lab has known blind
  spots (`TODO.md`, "What the balance tools cannot see yet"). A level they find easy may be one they play badly.
- A tower stronger than the reference makes every round look affordable; a tower weaker than the floor makes the
  early rounds a wall. Either shows first in the rooms of rounds 1 to 3.
- Retuning by the built towers must not undo the enemy scale: enemies stay weighed against the Walker, and a wave
  that needs fixing is fixed in the wave.
- A clear bonus large enough to matter makes one leak compound. The 0.6 cap is a guess.

## Decisions made

- Shape before level: the first pass's line and gates are kept as the starting point, and the difficulty is moved
  by moving the whole line.
- The reference tower stays the unit the paper tools price in.
- Waves keep their rounds, lanes, themes and bosses.

## Open questions

- What should a competent player hold, and win how often, on the first level and on the last?
- Should a level be winnable by one tower type alone, or is needing two the rule?
- How many leaks should a level forgive, and should that differ by level?
- Is the clear bonus meant to reward a clean round, or to pay for the next gate? It does the second today.
- Should later levels start richer, or only ask for more?
