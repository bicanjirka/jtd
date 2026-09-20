# Feature Request: Visual effects for every effect, ability, trait and aura

## Summary

A player currently cannot see most of what the simulation is doing to an enemy. Every effect,
ability, trait and aura in the game should have an unambiguous on-board visual, and every
*transition* between states (gaining a shield, going invisible, becoming visible again, casting a
radius ability, being healed) should be visible too - not just the steady-state condition.

## Current state (what exists today)

- The five existing `EffectKind`s (slow, burn, freeze, shield, invisible) each get exactly one
  generic coloured dot in `EnemyFrameBuilder`'s marker row, capped at three visible with an
  overflow glyph beyond that. No transition between having and not having an effect is visible.
- No aura is visible on the board. The Ghost's Elite/Boss-rank shroud ability and the Warden's
  call-to-arms both apply an effect to everything within a radius, with no indication of where
  that radius ends.
- Traits (`PercentResistTrait`, `FlatResistTrait`, `CriticalImmunityTrait`, `HurtSpeedTrait`) have
  no visual of their own at all. Some are implied by a mob's body archetype (Armored is a square),
  but a trait added by rank (Simple's Elite-rank resistance) or by the `armored` spawn shape is
  invisible regardless of which mob carries it.
- A tower buffed by the Aura tower shows nothing. The Aura tower itself pulses its own rings; the
  towers it amplifies are unmarked.
- There is no healing mechanic anywhere in the game. `EffectKind` has no `HEAL` value, and
  `td.damage.Damage`'s compact constructor clamps every amount at zero specifically so a
  "healing hit" cannot exist.
- `docs/features/FEATURE-enemy-traits-and-effects.md`'s product review notes already set this
  bar for the original five effect kinds: "no trait or ability ships without an unambiguous
  on-board visual telling the player it's active." That bar was met only by the marker row, and
  this request extends it to abilities, traits, auras, and every transition between states.

## What this feature adds

- A visible transition for every effect kind gaining or losing: at minimum, something happens on
  the mob's sprite at the moment it starts or stops being slowed, burning, frozen, shielded,
  invisible, or (once it exists) healing.
- A visible indicator for any ability that projects a radius effect onto nearby allies (the
  Ghost's shroud, the Warden's call-to-arms, and the new healing aura below), showing where that
  radius actually reaches.
- A visible indicator for a self-cast ability (a shield cast on oneself) and for an ability-driven
  spawn (an egg hatching, a reinforcement, a death-split) - all silent today.
- A persistent visual for every trait a mob carries, including ones a mob's body archetype
  already hints at.
- A visible indicator on a tower currently being amplified by an Aura tower.
- **Healing becomes a real mechanic**, not only a visual - there is nothing to visualize
  otherwise. A new support enemy projects a periodic healing aura onto nearby allies.
- Everything stays in the existing visual language: thin vector rings, brief flashes,
  translucency. No new art style, no particle effects, no floating text/numbers (this codebase
  has no text-rendering primitive at all, and adding one is out of scope for this request).

## Interconnections

- Builds directly on the recent Ghost rework, which made invisibility ability/effect-driven
  (`OnFirstDamageTakenTrigger` + `InvisibleTemplate`) rather than a hardcoded mob type - this
  request's transition visuals are what that rework was missing.
- Shares the enemy rank system's existing rank badge as precedent: a small, closed set of visual
  roles, keyed off a `Palette` enum, drawn by one frame builder - not a per-enemy special case.
- Extends `docs/features/FEATURE-enemy-traits-and-effects.md`'s effect-marker-row plan rather than
  replacing it; the marker row's existing three-visible-plus-overflow cap is reused, not redesigned.

## Constraints and open risks

- **Nothing in this codebase pushes a UI event from the engine.** The engine is headless and
  `td.ui` holds no gameplay logic; every existing transient visual (a death fade, a critical-hit
  spark) works by the *entity* recording a plain fact (a tick) and the frame builder deriving a
  progress from it. Any new visual needs to fit that same shape rather than introducing a
  push-based notification.
- **There is no text-rendering primitive anywhere in the render pipeline.** Every draw command is
  a coloured `Shape`. A "+N" count, a floating heal number, or similar is out of scope here for
  the same reason it's out of scope for the marker row's own existing overflow badge.
- **Visual crowding.** A mob can plausibly carry several effect markers, a rank badge, a trait
  indicator and an aura ring at once. The existing three-marker cap exists for exactly this
  reason; new persistent visuals need their own legibility discipline, not an unbounded sprite.
- **Standing UI requirement**: every change here must stay OS-independent, self-painted (no
  platform look-and-feel dependency), and verified from an actual screenshot of the running game,
  per this project's UI conventions - this is a look-and-feel change, and tests alone won't prove
  it reads correctly.
- **Performance is unmeasured.** The frame build already constructs every cell/enemy/tower/
  projectile as records 60 times a second; nothing here should claim a performance budget without
  a number behind it (see `TODO.md`'s existing note on this), but new per-frame allocation should
  still be gated on actual state so an idle board adds nothing.

## Decisions made

- Healing ships as a genuine new mechanic (a `HEAL` effect kind and a support enemy), not a
  visual-only stand-in, because there would otherwise be nothing real to visualize.
- Every trait gets a persistent visual, even one whose body archetype already implies it - no
  trait is exempted for being "already obvious."
- The tower-buff-indicator gap is in scope for this request, alongside the enemy-side work.
- Visual style stays subtle and consistent with the existing thin-vector-ring/brief-flash
  language - no bolder or more dramatic treatment.

## Open questions

None outstanding - the four decisions above resolved every open product question raised during
planning.

---

*Implemented across seven phases: healing as a mechanic, the Mender enemy, per-effect transition
capture and the cloaked body, a shield bubble and support-aura ring, gain/loss pulses with
ability-cast and spawn-burst capture, trait markers, and the tower-buff indicator. See
`src/main/java/td/effect/CLAUDE.md`, `src/main/java/td/ui/CLAUDE.md` and
`src/main/java/td/enemy/CLAUDE.md` for the resulting invariants.*
