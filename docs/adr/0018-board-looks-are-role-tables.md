# 0018. Board looks are tables per role, not switches in the painter

- Status: Accepted, not yet implemented
- Date: 2026-10-28

## Context
`Palette` is one flat enum of draw roles: enemy bodies, tower bodies, markers, terms, interface
colours. `Java2DFrameRenderer` maps roles to colours in one long switch, and to shapes in several
more. In the last 400 commits, every commit that changed `Palette` also changed the renderer. The
shape switches handle only their own slice of the enum, so they can't be exhaustive: a new body
without a shape compiles and throws at paint time.

## Decision
- `Palette` splits into one AWT-free role type per kind of thing drawn (enemy look, tower look,
  marker look, …) in `td.ui.render`.
- A table per role in `td.ui` holds each role's colour and shapes (for an enemy: colour, body
  shape, fade). It is built once and checked complete when it loads.
- The painter looks a role up in its table and paints. The colour switch and the per-role shape
  switches go.
- Implemented one role at a time, starting with the tower visual revamp.

## Consequences
- A new body, tower, trait or marker is one row, and the compiler or the table check forces every
  case.
- The painter changes only when painting changes, not for each new piece of content.
- Draw records carry a role type instead of `Palette`, so the frame builders and the ASCII backend
  change with each step.

## Rejected
- *Colours on the `Palette` constants.* It would put look data into the frame model, and it
  wouldn't fix the shape switches.
- *A paint test only* (paint every role headless so a missing case fails `verify`). Cheaper, but it
  keeps two places per new look and moves the failure to the test, not to the compiler.
