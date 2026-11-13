# Feature Request: Balance work that can be done before the next-gen towers

**Status: implemented** (2026-11-13).

## Summary

Enemies, bounties and waves are now held to one measured scale (`docs/BALANCE.md`). This request closes what that
pass left open: the roster has no enemy that magic barely touches while physical damage handles it as it handles a
Walker, the one insulated enemy is too fragile for its insulation to matter, and reinforcements a boss calls on a
timer pay for as long as it lives. None of it needs a next-gen tower to exist.

## Current state (what exists today)

- `td.balance.EnemyPowerReport` weighs every enemy at every rank against the Walker with a rig of four probes
  (single physical, single magic, area, burn), and `td.balance.WavePowerReport` weighs every round against what the
  level has paid by then, priced in the paper tools' reference tower. Neither uses a tower in code.
- Every ranked enemy's health and bounty are written rank by rank to a target weight; an enemy may weigh 1.75
  Walkers (more at Elite and Boss) unless it has a clear answer.
- Every level except The Labyrinth has its waves, starting credits and clear bonuses set so that its room follows
  one line, with three or four gates under it.
- The Relay is a fragile signal post with 3 to 12 points of insulation. Most of its weight is the Walkers it calls,
  so its body has less health than a Walker of its rank (a third of one at Veteran), and one Sniper hit kills it at
  every rank. It appears on Iron Crossing only.
- Magic resist is highest on the Magma (30 to 75), which is also slow, heavy and proof against crits and burn. No
  enemy is magic-resistant and otherwise ordinary.
- The Yeti appears on Iron Crossing and The Rift only, for no recorded reason.
- The Boss Knight's Squires, the Iron Colossus's Knights and the Keeper of Hours' Timekeeper are called on a timer
  and pay their bounty for as long as the caller lives. The Warden's Drones do the same.
- The Sand Reaver weighs 4.5 Walkers at Grunt and 3.1 at Elite with no answer that brings it under the limit.
- Unique bosses are left as authored. The Warden's round stands far under its level's line and the in-code players
  lose it; The Labyrinth's rounds pay while they run, so the wave tool's figures for it mean little.

## What this feature adds

- **Reinforcements on a timer pay nothing.** An enemy a caller sends again and again (Squires, the Colossus's
  Knights, the Keeper's Timekeeper) pays no bounty, so what a round pays is bounded. A spawn that comes once (a
  split, a call at a health threshold or on the first hit, a hatch) still pays. The Warden's Drones are the
  exception and keep paying.
- **A Relay whose insulation matters**: insulation in the range a Juggernaut's plating has, the Walkers it calls one
  rank below its own so that its body carries more of its weight, and its health refitted to the same weight, so a
  heavy hit is what kills it and small magic hits and burns barely touch it. When it calls stays as it is. It joins The Storm Ridge (a signal post in the storm, ahead of the Lightning's own insulation)
  and The Rift's main lane (help that arrives from nowhere).
- **A Magma that magic barely touches**: magic resist raised at every rank, health refitted to its weight, so its
  answer is physical damage.
- **The Yeti on more levels**: it keeps its armor and magic resist and joins The Storm Ridge and The Labyrinth.
- **The Templar**, a classic magic-resist enemy beside the Knight and the Juggernaut: high magic resist and no armor,
  at a Walker's pace, so physical damage handles it as it handles a Walker. From Veteran it raises a shield against
  magic once badly hurt; Elite and Boss carry it stronger. It is weighed at all five ranks, has its own body and
  look, and joins Iron Crossing's steel lane (its round with the Elite Magma under a commander becomes the gate
  against magic), The Rift's third lane and The Labyrinth's first round.

Out of scope: any tower, tower price or upgrade; the absolute difficulty of the game; a measure for bosses.

## Interconnections

- `FEATURE-next-gen.md` and the next-gen tower docs: nothing here changes their numbers. The Relay and the Templar
  give their damage-type and hit-size choices something to be right or wrong about.
- `FEATURE-balance-after-next-gen-towers.md`: the second pass, which needs the towers in code; it plays the levels
  this one changes, The Labyrinth and the Warden's round included.

## Constraints and open risks

- Enemy power is not additive, so an enemy tuned alone can still be wrong in the waves it is sent in. The wave tool
  is the check, except on The Labyrinth, which play proves.
- The rig stands in for a balanced defence. An enemy whose answer is a mechanic no probe has (silence, heal denial,
  a pull) reads as having no answer.
- A new enemy is weighed at all five ranks before it enters a wave, and its README row is written with it.
- Content gets no tests of its own; a new trait or ability does.
- The reference tower's file is rewritten by the paper tools; a change to the Sniper moves every `need`.

## Decisions made

- The Walker is the unit: 1.0 on every axis at every rank.
- The limit is 1.75 Walkers blind, 1.25 times that at Elite and 1.5 times at Boss, unless countered at 0.6 or less.
- Bounty is the Walker's at that rank times blind power, less what spawns pay. The Courier is overpaid on purpose.
- A level's room follows one line; gates sit about 15% under it; a clear bonus is at most 0.6 of its round's bounty
  and may differ from round to round.
- The Sand Reaver stands over the limit as the game's one brute.
- The Warden stays as authored; it is played by hand.
- The Labyrinth keeps its three long rounds; play proves them, not the wave tool.
- A new enemy enters a level first in a round that is not a gate, then comes back in or just before one.
- The Amber Hive and The Last Hour get neither the Relay nor the Templar: each is built on its own enemies.

## Open questions

None.
