# 0027. The presentation times its moments

- Status: Accepted
- Date: 2026-11-25

## Context
Towers kept what they had just done (a Mortar's blast mark, a Seeker's shatter, a Splash blast or bolt, a Beacon's
reveal) for exactly as long as it was drawn, each with a constant of its own in the domain that the frame builder then
read back. A look change (a longer shatter, a slower fade) meant editing gameplay code, and the lengths didn't share
the board's motion vocabulary (`MomentLength`, `Easing`).

## Decision
- A tower keeps a moment for one shared window, `MomentMemory.TICKS`, long enough for the longest moment the board
  draws. Nothing in the game reads a kept moment.
- How long each moment is drawn is the frame builder's, as a `MomentLength` step; it draws a kept moment while its
  progress is under 1.

## Consequences
- Retiming a moment touches only `td.ui`.
- A tower holds a few more past moments than it draws; the lists stay short, since the window is seconds.

## Rejected
- *The frame builder remembering moments itself*: it builds every frame from scratch on the game-loop thread, and
  stored animation state there breaks the "draw records carry progress" idiom.
- *Each tower keeping its own window*: the duplication this replaces.
