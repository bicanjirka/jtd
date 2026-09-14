# Feature Request: Enemy Traits, Abilities and Effects

**Priority: Phase 3 — after tower upgrades and damage types.** This is the most
foundational-feeling of the three requests but also the most speculative if the game
stayed small — its full scope (registry, builder, per-level authoring and cloning) is
justified by a confirmed roadmap of substantially more content ahead (more levels, more
enemies); see Product review notes, below. It also has a genuine dependency on feature 2's
damage-type tag and burning effect for two of its named traits, which is a second reason it
sequences last.

## Summary

Decouple an enemy's *identity* from its *behavior*. Today, "what makes a Square a Square"
(damage resistance) and "what makes a Triangle a Triangle" (speeds up as it's hurt) is a
hardcoded method override on a `final` Java class. This feature replaces that with a
data-driven model: an enemy is built from a name/id, base stats, a graphical
representation, and a set of composable **traits** (passive) and **abilities** (active,
triggered) — so that adding a new enemy, or a per-level variant of an existing one, does
not require writing a new Java class. It also introduces **effects**, the runtime state a
trait or ability applies to a mob (a shield, a speed boost, temporary invisibility), with a
small UI marker (e.g. a corner icon) so the player can see what's affecting an enemy.

## Current state (what exists today)

- `EnemyFactory.Enemy` (`td.enemy.EnemyFactory`) is a closed enum — `Circle`, `Square`,
  `Triangle`, `Ghost`, `Empty` — each wired to exactly one wave-script letter (`c`, `s`,
  `t`, `g`, `e`) and one `final` leaf class. Its `create` switch has **no `default`**,
  deliberately: adding a constant without wiring up its class is a compile error, not a
  silent gap.
- Each leaf class hand-implements its one behavioral twist directly in Java:
  - `EnemyMobSquare.absorb()` scales incoming `Damage` by a flat, level-scaled fraction.
  - `EnemyMobTriangle.doDamage()` recomputes `speed` from current health fraction every hit.
  - `EnemyMobGhost` sets `type = EnemyMob.type.Invisible`, a two-value closed enum
    (`Normal`/`Invisible`) that every targeting query filters on.
  - `EnemyMobCircle` has no override at all — the "no traits" case today is simply the
    absence of any override, not an explicit empty trait list.
- `AbstractEnemyMob.absorb(Damage)` is the *only* extension point a subclass has for
  reacting to an incoming hit. There is no hook for "on death", "on health threshold
  crossed", "on critical hit taken", nor any concept of a time-limited effect at all.
- Rendering dispatches over the closed enemy set via `EnemyMobVisitor` (5 methods, one per
  leaf class), consumed by `td.ui.EnemyFrameBuilder` and painted by
  `Java2DFrameRenderer`/`Palette`. Adding a 6th enemy today means touching the enum, the
  visitor, the frame builder and the renderer — a fixed, compiler-enforced checklist
  documented in `td/enemy/CLAUDE.md`.
- `LevelDefinition` carries only `waves` (see `td.wave.WaveDefinition`/`WaveScript`); it has
  no concept of enemy definitions at all. Every level draws from the same fixed, global set
  of five enemy types — there is no per-level enemy roster, override, or variant.
- The wave mini-language (`WaveScript.parse`) maps a **single character** token 1:1 to one
  `EnemyFactory.Enemy` constant. It has no notion of an arbitrary string id.

## What this feature adds

- **An enemy registry/builder**, replacing the fixed `EnemyFactory.Enemy` enum as the
  source of buildable enemy types. Some enemy definitions are pre-registered globally (the
  current five, at minimum, for backward compatibility); a level can register new
  definitions of its own, and can clone-and-adjust an existing definition (e.g., "a Square
  with double the usual resistance for this one level") without touching Java code or
  affecting the global definition.
- **Each enemy definition carries**: a name/id (the wave-script token), a level, health, a
  graphical sprite representation, a graphical *behavior* (rotating like
  `AbstractEnemyMobRotor`, path-directional like `AbstractEnemyMobDirectional`, pulsing, or
  new kinds), a list of **traits**, and a list of **abilities**.
- **Traits are passive**, always-on modifiers of an enemy's properties or of incoming
  interactions: a shield absorbing a percentage of damage (generalizing
  `EnemyMobSquare.absorb`), a shield absorbing a flat amount, an armor-style shield that
  depletes before the health bar, a magic-damage shield (see the damage-types feature
  below), critical-hit immunity, burning-effect immunity, and so on.
- **Abilities are active**: triggered by a precondition (a health threshold, time since
  last hit, having just taken a critical hit, dying) and, when triggered, apply an
  **effect** to the enemy itself and/or to enemies around it. Examples from the request:
  temporary invisibility after taking damage, a speed boost below 50% health (a
  generalization of `EnemyMobTriangle`'s hardcoded curve), an on-death shield aura cast on
  nearby enemies, and gaining a shield after surviving a critical hit.
- **Effects are the runtime, time-boxed (or permanent-until-removed) result** of a trait or
  ability firing — the thing actually attached to a live `EnemyMob` at any moment, read
  back by other traits, by targeting, and by rendering.
- **UI**: an active effect gets a small visual marker (the request specifically mentions a
  shield icon in a sprite corner), so a player can tell at a glance that a mob is currently
  shielded, sped up, or invisible.

## Interconnections with the other two feature requests

This is the foundational piece the other two lean on, not an isolated system:

- **Damage types** (feature 2) is a direct dependency for two traits named explicitly in
  this request: a *magic* shield only makes sense once damage carries a physical/magic tag,
  and *burning-effect immunity* only makes sense once burning exists as a damage-over-time
  effect. Those two traits cannot be meaningfully implemented before feature 2 lands (or at
  least before its damage-type taxonomy is settled) — see that document.
- **Tower upgrades** (feature 3) explicitly wants upgraded towers to *apply* enemy-affecting
  effects (slow, burn, a damage-taken-increase aura). That is the same "apply an effect to a
  mob" operation an enemy's own ability performs internally (e.g., the on-death shield
  aura). If each feature invents its own status-effect representation independently, the
  codebase ends up with two or three parallel, subtly incompatible effect systems (one
  enemy-internal, one tower-inflicted) that both need their own UI marker, their own
  duration/stacking rules, and their own render support. **Decided: "effect" is one shared
  primitive**, designed up front before any of these three features is implemented — a
  value/record describing what changed, for how long, and how it renders — that abilities
  (this feature), on-hit effects (feature 2), and tower auras (feature 3) all produce, and
  that a mob's effect list simply accumulates regardless of source. This shared design pass
  is a prerequisite piece of work that should land before feature-specific work on any of
  the three requests starts.

## Architectural implications

- **This is in direct tension with the codebase's "closed set, no `default`" doctrine.**
  `EnemyFactory.Enemy`'s exhaustive enum and `EnemyMobVisitor`'s fixed 5-method interface
  are deliberate: they turn "I added an enemy but forgot to draw it" into a compile error.
  A registry of data-defined enemies is by nature an *open* set, so that compile-time
  safety net has to be replaced with something else — most plausibly, validation at level-
  load time (an `EnemyDefinition` missing required art or a graphical-behavior mapping
  throws `td.util.GameStartupException`, the project's existing one fatal-startup boundary)
  rather than a runtime silent skip.
- **Rendering needs a new dispatch shape.** `td.ui.render` is deliberately AWT-free and
  dispatches through visitors over a *closed* domain hierarchy
  (`EnemyMobVisitor`/`TowerVisitor`) into a *sealed* `RenderFrame` draw-command hierarchy.
  A data-defined enemy can't be visited by a fixed 5-method interface; the render side needs
  either (a) a small, closed set of generic *body archetypes* (circle/square/triangle/
  spiral/star, matching the existing single-`Shape` tower convention) that a definition
  picks from and parameterizes, keeping the sealed/visitor discipline intact, or (b) a more
  open per-definition rendering strategy, which would be a real departure from this
  codebase's current "renderer owns every shape/colour choice" rule (`td/ui/CLAUDE.md`).
  Effect icons (the shield-in-corner ask) need the same decision: a small closed set of
  icon *kinds* rendered generically works within the existing discipline; an arbitrary
  per-trait icon does not.
- **The wave mini-language needs to grow.** `WaveScript` today maps exactly one character
  to one global `EnemyFactory.Enemy`. Per-level custom/cloned enemies need either
  multi-character ids in the token grammar or a different addressing scheme, and the
  parser's "unrecognized token → warn and default to repeat-count 1" fallback
  (`WaveScript.parse`, see `td/wave/CLAUDE.md`'s Gotchas) needs re-examining once tokens can
  legitimately be longer than one character.
- **`LevelDefinition` needs a new field** for its enemy roster (built-ins used as-is,
  built-ins overridden, and net-new definitions), which is itself a modest but real change
  to a `record` — everywhere a `LevelDefinition` is constructed (`BuiltInLevelCatalog`, any
  test fixture) needs updating.
- **Traits/abilities need a defined evaluation order and hook set.** Today `absorb` is the
  only hook, called synchronously inside `doDamage`. A general trait system needs to define,
  and keep cheap, at least: on-hit (before/after resistance), on-death, on-tick (health-
  threshold and cooldown-based ability checks), and an ordering rule for when two traits
  both want to modify the same hit.
- **Performance**: `doTick`/`doDamage` run on the dedicated `game-loop` thread for every
  live enemy, potentially several times a tick while fast-forwarding (see the root
  `CLAUDE.md`'s Threading model). A trait/ability list walked per enemy per tick needs to
  stay allocation-light and avoid reflection/lookup-by-string in the hot path — the registry
  lookup belongs at enemy-construction time, not per-tick.

## Risks and costs

- **Large surface area**: touches `EnemyFactory`, `EnemyMobVisitor`, every enemy render
  frame builder, `WaveScript`, `LevelDefinition`, `GameEngine.loadLevel`, and — since all
  five existing enemies are to be migrated onto the new model rather than left as legacy
  code (see Decided, below) — every existing enemy-behavior test (`GameEngineTest` and
  friends) needs re-verifying against the new implementation, not just the new code paths.
- **Decided: migrate all five existing enemies onto the new trait/ability model**, rather
  than leaving them as hardcoded legacy classes running alongside new data-driven ones.
  This avoids a permanent two-model codebase, at the cost of more upfront work: each of
  `EnemyMobSquare`'s resistance, `EnemyMobTriangle`'s speed curve, and `EnemyMobGhost`'s
  invisibility needs to be re-expressed as a trait (or, for invisibility, possibly a
  targeting-relevant property the trait system exposes) that reproduces its current
  behavior exactly, with the existing tests as the regression bar.
- **Balance and test-matrix growth.** Traits × abilities × per-level overrides is
  combinatorial in a way five fixed classes never were. This needs either a much smaller
  curated set of traits/abilities to launch with, or a deliberate testing strategy (e.g.
  property-style tests over trait combinations) rather than one integration test per enemy.
- **UI crowding.** An enemy with several simultaneous effects needs a defined layout rule
  for its corner icon(s) — what happens at 3+ active effects — or the sprite becomes
  unreadable exactly when the player most needs to read it (a heavily-buffed enemy in a
  packed wave).
- **Losing a compile-time safety net.** As above: today, forgetting to wire up a new
  enemy's art is impossible to ship. A data-driven registry trades that for a runtime check,
  which is weaker unless the validation is genuinely exhaustive and genuinely fires before
  a level can be played.

## Product review notes

A senior-product-owner pass over this request, with a genre lens, surfaced points worth
recording:

- **This feature's full scope (an open-ended registry/builder with per-level authoring and
  cloning) is more generality than five enemies and four levels justify on their own.** It
  sits in real tension with this project's own stated coding philosophy ("don't design for
  hypothetical future requirements," root `CLAUDE.md`). That tension is resolved by a
  confirmed roadmap of substantially more content ahead — see Decisions made — which is
  exactly the condition under which this kind of investment pays for itself. If that
  roadmap changes, this feature's scope should be revisited down to a much smaller
  "traits are data, on the existing five enemies" cut.
- **The doc's own examples never name a payoff use case for abilities specifically.**
  Nothing here proposes a boss or wave-champion enemy — the natural proving ground for a
  multi-ability enemy (a rare, late-wave enemy with two or three abilities is the kind of
  moment that justifies this system existing, the way Kingdom Rush's and Bloons' bosses
  justify theirs). Recommendation: name at least one boss concept as part of scoping this
  feature's first version, so the ability system ships with a concrete thing it's for.
- **Legibility is the real risk, more than the architecture.** TD as a genre lives on the
  player being able to read the board and understand why a tower isn't working. As traits,
  immunities, resistances and effects stack, the existing "small UI marker" plan (a corner
  icon) needs to be treated as a hard requirement, not a nice-to-have: no trait or ability
  ships without an unambiguous on-board visual telling the player it's active.

## Decisions made

- The shared "effect" primitive is to be designed once, up front, before feature-specific
  work on any of the three requests begins (see Interconnections, above).
- All five existing enemies will be migrated onto the new trait/ability model rather than
  kept as legacy classes alongside it (see Risks and costs, above).
- The full registry/builder/per-level-authoring scope is confirmed as justified: substantial
  further content (more levels, more enemies) is planned, which is what makes this
  generality worth building now rather than deferring it.
- Sequencing: this feature ships **third**, after tower upgrades and damage types/on-hit
  effects (see Priority, above).

## Open questions

1. Should this document's ability-produced effects, feature 2's on-hit effects, and
   feature 3's tower auras literally share one Java type, or just a common design pattern
   (parallel types with the same shape)?
2. How open does enemy *art* need to be — a small closed set of parameterized body
   archetypes (keeps the current sealed/visitor rendering discipline), or fully custom
   per-definition art (a bigger, more open-ended rendering change)?
3. What's the wave-script token syntax for a per-level custom/cloned enemy id?
4. Is there a bound on how many traits/abilities one enemy can carry, or how many effects
   can stack on one mob at once — both for balance and for the UI icon layout?
5. What's at least one concrete boss/wave-champion concept this ability system is being
   built to support?
