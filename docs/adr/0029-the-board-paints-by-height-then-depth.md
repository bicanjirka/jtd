# 0029. The board paints by height, then depth

- Status: Accepted
- Date: 2026-11-27

## Context
The painter walked `RenderFrame`'s lists one kind after another: enemies, towers, enemy overlays, tower effects,
projectiles. Inside a kind the order was the build order, so a tower effect followed tower placement. A spell blast's
ground ring could cover a nuke's cloud, flat marks covered towers, enemies and bars, an enemy's ice floated over the
tower in front of it, and the flames on a burning ground lay under every enemy.

## Decision
- Every board visual over the ground is a `BoardDraw` with a `DrawHeight` (ground, standing, low air, signal, high
  air) and a foot, the screen row of the ground it stands on. `DrawHeight.of` names each kind's height in one
  exhaustive switch.
- The frame build sorts them once, stably, by height then foot, into `RenderFrame.scene`; the painter walks it.
  What lies on the ground keeps build order.
- What belongs to an enemy or a tower takes its foot, so it is painted with it. Something with a place of its own (a
  blast, a shot, a flame on a burning ground) takes that place.
- A visual sits at one height whole, except a shell's blast: what it lays on the ground (`BlastGroundDraw`: scorch,
  shock ring, splats) lies under what stands, what it throws up is in the air (a nuke's cloud in the high air).
- The floor, path marks, the cell highlight and zone patches stay ahead of the scene, as before.

## Consequences
- A new visual needs a case in `DrawHeight.of`, which the compiler asks for.
- Each draw costs a `PlacedDraw` and a place in one sort per frame.
- Towers and enemies interleave by depth, so a tower no longer always covers an enemy; bars and marks stay readable
  because signals are a height of their own.

## Rejected
- *One list per height on `RenderFrame`*: the painter would still need the depth inside each, and a new height would
  widen the record.
- *A free depth number per visual*: every builder would invent numbers; a few named heights are what the art needs.
- *Splitting every visual that touches the ground*: only a blast lays a wide mark on the ground while it throws
  things up; the rest read right at one height.
