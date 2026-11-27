# Feature Request: Burst Backlash and spell visuals

Status: implemented.

## Summary

The Burst's Hexer and Stormcaller forks read as spellcasters: what they throw, what they curse with, how their lightning
strikes and how their blasts land each carry the fork's lore. The Hex of Inversion becomes the Hex of Backlash, and a
heal under it strikes the enemy whole, as a shield already does.

## Current state (what exists today)

- Every fork throws the same lobbed stone from the arm; a charged stone is blue, a plain one cannonball grey and hard to
  see on most grounds.
- A curse flies as a thin streak of one colour for every hex; over a hexed enemy each hex is a thin outline rune
  (hourglass, drop, inverted triangle) in one rune colour.
- A bolt (an Arc jump, a Shockwave, a Thunderstrike) is a thin zigzag, the Thunderstrike's from a little above its
  enemy; nothing shows on the enemy struck.
- A blast is a flash and two rings, tinted by fork.
- Under Inversion a shield strikes as one hit, but a heal adds to regeneration, which is dealt as damage tick by tick.
- Doom records the damage that landed on health, hit by hit (`HexLedger`), not health at its start against its end;
  regeneration does not lower it.
- Mortar shells already draw glowing tails (`ShellTrail`, `BlastArt.trail`).

## What this feature adds

Mechanics:

- **Hex of Backlash** (was Inversion): the same hex, renamed. A heal it receives strikes it at once as one magic hit of
  all the heal would give; a heal it already carried when the hex landed strikes at once with what it had left. A
  source renewing its heal before it ran out strikes only what the renewal adds. Its own regeneration stays turned into
  damage tick by tick.

Visuals:

- The plain Burst's stone is easier to see.
- The Stormcaller and the Hexer no longer throw stones from the arm: the Stormcaller casts a ball lightning from its
  tesla coil, the Hexer a ball of magic from its oracle ball, each with a tail, on an almost flat arc. Same speed, same
  landing, same hits.
- A curse flies as a hex projectile: bigger, with its hex's rune as its head, in its hex's own colour.
- The runes over a hexed enemy say what the hex does at a glance (a skull for Doom, a poison drop for Blight, a broken
  heart for Backlash), keep a magic-rune feel, are animated and wear their hex's colour.
- A Stormcaller ability that strikes an enemy shocks it as in cartoons: a black silhouette with a blue electric rim; a
  crit and every Thunderstrike also show the skeleton inside.
- The Thunderstrike falls from the top of the board as a big lightning bolt, standing in 3D like the towers and blasts.
- The Stormcaller's and the Hexer's blasts land with a brief impact of their own lore, which still shows where the full
  damage core ends and where the half-damage edge ends. It is as brief and as quiet as a Mortar's Heavy shell: a short
  flash, a thin ring running out to the core, then one running on to the edge, and nothing filled. The Stormcaller's
  rings crackle and a few arcs of lightning leap out of the landing; the Hexer's pentagram is summoned line by line,
  each line fading a frame or two after the next one starts.

Out of scope: any change to Doom's record, to what the hexes pick or to any number.

## Decisions made

- Name: Hex of Backlash; the node id follows the name (`splash.hex_of_backlash`) and the save format is raised.
- Regeneration under Backlash stays turned tick by tick; only heals and shields strike whole.
- Doom is left as it is: its record already counts the real damage taken.
- The skeleton shows on crits and on every Thunderstrike; other ability hits show the silhouette and rim only.
