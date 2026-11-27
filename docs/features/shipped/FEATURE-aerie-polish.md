# Feature Request: Aerie polish

Status: implemented.

## Summary

The Aerie's visuals and mechanics settle before its balance run: its nest fills in a fixed order instead of by lot, a
Release matches the nest, Long Flight lengthens the missiles' hunt instead of the tower's range, its stronger ice shows
and acts over a weaker one, Shade stops regeneration, and an Unravel node makes it open enemies to magic.

## Current state (what exists today)

- A new missile is drawn by lot: with Ruby or Shade owned, one in three (one in two with Rich Brood) is the second kind,
  each draw a roll of the shared random. The Info pane shows the nest as a strip of slots and the kinds' shares in
  percent, like the Mortar's magazine.
- A Release (Swarm or Hatching) sends seven missiles, whatever the nest's size (3, 5 or 7). A Hatching's missiles fill
  the nest past its size, and the nest loads nothing until it is back under its size.
- Long Flight is the shared range node (`base.range`, +1 range); Long Flight II (`base.range.2`) adds another.
- A missile whose enemy died or was frozen looks 2 cells round itself for an unfrozen enemy, stays on its pick when
  there is none, and flies home when its pick is gone too.
- A freeze is not renewed while it holds, so a Ruby or Shade missile on an enemy already frozen in frost changes
  nothing: the ice stays blue and does what frost does.
- Missiles leave from the tower's cell centre, below the ring they circle on.
- The Shade missile has a look of its own, unlike Frost and Ruby, which share a shape.
- No regeneration stop exists apart from Radiation's own rule. Unravel (-5 magic resist a stack, up to 20) is an
  effect no tower applies.
- A node may cost 0 and the Aerie's hits already carry the tower's Accuracy stat.

## What this feature adds

- **Fixed order.** Every missile the tower makes (a load, a hatch, a Swarm's) takes the next place in one sequence:
  one in three is every third (Frost, Frost, second), one in two every second. The sequence starts over with each
  wave, so every wave starts with the same nest. The nest circles in that order. Player wording stays "one in three"
  and "one in two".
- **Nest as buffs.** The slot strip and shares leave the Info pane; each kind the nest holds is a condition with its
  count (Frost x2, Ruby x1), with or without a second kind.
- **Release follows the nest.** A Swarm or a Hatching sends as many missiles as the nest holds.
- **Release on top.** Released missiles are extra to the nest's size: they launch first, and the nest keeps loading
  its own beneath them.
- **Long Flight** (tier 0) no longer adds range: a missile in flight looks one cell further round itself for a new
  enemy, so it flies home less. It leaves the shared range id. **High Perch** (was Long Flight II, tier 2) adds a cell
  of range and a cell to that look. No ring is drawn for it (the look is round the missile, not the tower). The
  Mortar's minimum-range ring is drawn thin.
- **Stronger ice wins.** Ruby over Shade over Frost: a stronger missile on an enemy the Aerie already holds frozen
  turns the ice to its own, in colour and in what it does (Ruby holds the freeze-immune, Shade
  stops regeneration), until the freeze ends as it would have. It is no new freeze for the Release or
  Feeding.
- **Shade's look** is Frost's and Ruby's shape in its own grey, on the board and in its glyph.
- **Wither** (new effect): regeneration held at zero, so a heal over time gives nothing; a Shade hit applies it for the
  freeze's duration, which tenacity shortens and the freeze meter does not.
- **Rime** (tier II's perk, held once its XP opens the tier, never bought): every freeze the Aerie lands adds a
  stack of Unravel.
- **Launch from the ring.** A missile is drawn leaving the centre of the ring it circled on, descending to its flight.

## Decisions made

- Missile reach is a look-around radius round the missile, not a ring round the tower (so no ring is drawn).
- The stronger ice takes over in colour and effect.
- Unravel belongs to the Aerie, on every freeze, as a node.
- Accuracy: a free node needs no new code; the balancing agent adds it to the tree.
- Numbers (Rime's stacks and duration, Wither's) are placeholders for the balance run.

## Open questions

None.
