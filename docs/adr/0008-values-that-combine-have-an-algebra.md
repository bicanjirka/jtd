# 0008. Values that combine have an algebra

- Status: Accepted
- Date: 2026-09-17 (recorded)

## Context
Combining values of one kind was hand-rolled arithmetic spread over several mutations. The
economy had separate add-score, receive and remove-life methods that fired up to two
notifications per event, and left score changes with none.

## Decision
Every place two values of one kind combine uses an operation, an identity and, where one exists,
an absorber (`docs/DESIGN.md` §7 has the principle). The specific choices:

- `EconomyDelta` turns a kill or a leak into one value, and `EconomyLedger.apply` fires exactly one
  notification per event.
- `StatModifiers` is the bonus algebra for towers and enemies alike (`plus`, `none()`, `scaledBy`).
  Each aura's emission is its own bundle, so auras of different strengths stack.
- `TargetQuery.and` has identity `all()` and absorber `none()`. `none()` never evaluates the other
  side.
- `Damage` clamps at zero on every path into its constructor, so a falloff or resistance can never
  heal. `plus` treats zero as the identity whatever its type, and refuses to merge two non-zero
  damages of different types. `cappedAt` keeps the type.
- `PathSmoothing.none()` is the identity for smoothing. A level without smoothing gets it, never a
  null.

## Consequences
- Combining needs no special cases, and the identities replace null checks.
- A new combinable value gets the same shape.

## Rejected
- *Mutating methods per event* (add score, remove a life): multiple notifications per event, and
  one change with none.
