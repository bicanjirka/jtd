# Feature Request: Enemy Traits, Abilities and Effects

**Status: implemented.** `EnemyDefinition`/`EnemyCatalog` replaced `EnemyFactory.Enemy`; all
five original enemies migrated onto the trait/ability model; the Warden/egg boss chain shipped
in `BuiltInEnemies`. One point below is now stale: "not in scope: any trait or ability keyed
off a critical hit" was true when this document's v1 shipped, but `FEATURE-critical-damage.md`
later closed that gap — `CriticalImmunityTrait` went onto `ARMORED` and
`OnCriticalHitTakenTrigger` onto the Warden. Balance numbers remain placeholders, tracked in
`TODO.md`'s "Enemy traits/abilities numbers are unbalanced placeholders" entry. Per-level
authoring, the other point this document called out as deferred, has since landed too:
`LevelDefinition.customEnemies()` (a fluent `withCustomEnemies` copy) is registered into that
level's own `EnemyCatalog` by `GameEngine.loadLevel`; Wild Bezier Sweep's Reaver is the first
level-authored enemy to use it.

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
    - `EnemyMobGhost` sets `type = EnemyMob.type.Invisible`, a two-value closed enum (`Normal`/`Invisible`) that every
      targeting query filters on.
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
  last hit, having just taken a critical hit, dying, or a fixed delay after spawning) and,
  when triggered, either apply an **effect** to the enemy itself and/or to enemies around
  it, or **spawn one or more new enemies** — periodically on a fixed interval, once after a
  delay, at a health percentage threshold, or on death. Examples from the request: temporary
  invisibility after taking damage, a speed boost below 50% health (a generalization of
  `EnemyMobTriangle`'s hardcoded curve), an on-death shield aura cast on nearby enemies,
  gaining a shield after surviving a critical hit, and — added during scoping — an enemy
  that periodically spawns reinforcements, periodically re-shields itself, and on death
  spawns a stationary "egg" that, if not killed in time, hatches back into a weaker version
  of the same enemy — repeating until the egg itself is finally the one that dies (see the
  Warden in V1 Scope).
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
  value/record describing what changed, for how long, and how it renders — that abilities (this feature), on-hit effects
  (feature 2), and tower auras (feature 3) all produce, and
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
  dispatches through visitors over a *closed* domain hierarchy (`EnemyMobVisitor`/`TowerVisitor`) into a *sealed*
  `RenderFrame` draw-command hierarchy.
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
  parser's "unrecognized token → warn and default to repeat-count 1" fallback (`WaveScript.parse`, see
  `td/wave/CLAUDE.md`'s Gotchas) needs re-examining once tokens can
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
- **Spawning is a new kind of ability action, not just a new trigger.** Every ability so far
  in this document *modifies* an existing mob (itself or a neighbor); spawning instead *adds* one to `EnemyRoster`
  mid-tick, from code running on the `game-loop` thread inside
  `doTick`/`doDamage` — the same thread and the same call stack a wave's own spawn logic
  already runs on, so no new synchronization is needed, but the roster's alive-count/
  win-condition bookkeeping has to treat a spawned mob exactly like a wave-declared one from
  the instant it's added, including when it's added in the same tick another mob dies (the
  Warden's on-death egg spawn). Left unguarded, an ability that can itself spawn a
  definition carrying the same spawn ability creates an unbounded chain — see Risks and
  costs.
- **A spawn that replaces its own mob (the Warden's egg hatching back into a weaker Warden)
  is not a death.** `EnemyRoster` today has exactly two removal paths — `remove()` (an
  actual kill: notifies `GameHost.enemyDied`, awards bounty/score) and `clear()` (level
  teardown: notifies nothing) — documented in `td/enemy/CLAUDE.md`. Neither fits an egg that
  survives its timer and turns into a boss: it wasn't killed (no bounty, doesn't count
  toward a kill-count upgrade gate) and it isn't teardown (the game is still very much in
  progress). This needs a third path — see V1 Scope's `consumesSelf`.
- **Standing still needs no new movement mechanic.** `MovementBehavior` (see V1 Scope)
  governs facing/rotation only; a mob that doesn't advance along the path at all is simply
  one whose `EnemyDefinition` gives it a base speed of `0` — `AbstractEnemyMob`'s existing
  `distanceIntoLap` accumulator already handles a zero increment correctly, so the egg needs
  no special-cased "stationary" flag.

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
  for its corner icon (s) — what happens at 3+ active effects — or the sprite becomes
  unreadable exactly when the player most needs to read it (a heavily-buffed enemy in a
  packed wave).
- **Losing a compile-time safety net.** As above: today, forgetting to wire up a new
  enemy's art is impossible to ship. A data-driven registry trades that for a runtime check,
  which is weaker unless the validation is genuinely exhaustive and genuinely fires before
  a level can be played.
- **A spawn ability can create a runaway chain.** Nothing stops a definition's spawn
  ability from naming a definition that itself has a spawn ability, which could in
  principle balloon `EnemyRoster` well past what a wave was authored to contain, on the
  `game-loop` thread, uncapped — or, if two definitions named each other, loop forever. See
  Decisions made / V1 Scope for the v1 guard (the spawn graph must be acyclic) and for why
  the Warden/egg encounter, despite spawning repeatedly, stays safe under that guard: it's
  authored as a finite, strictly linear chain of distinct definitions, not an actual cycle,
  and every hop in it *replaces* one live mob with another (`consumesSelf`) rather than
  adding to the roster, so `EnemyRoster`'s size never grows regardless of how many stages
  the fight goes through.

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
  work on any of the three requests begins (see Interconnections, above). **Done**: it
  shipped as `td.effect.Effect`/`EffectKind`/`ActiveEffects`/`DamageSink` during feature 2 (damage types), and is reused
  as-is rather than redesigned — see V1 Scope, below, for the
  additions it still needs (shield/invisibility kinds).
- All five existing enemies will be migrated onto the new trait/ability model rather than
  kept as legacy classes alongside it (see Risks and costs, above).
- The full registry/builder/per-level-authoring scope is confirmed as justified: substantial
  further content (more levels, more enemies) is planned, which is what makes this
  generality worth building now rather than deferring it.
- Sequencing: this feature ships **third**, after tower upgrades and damage types/on-hit
  effects (see Priority, above).
- **Enemy art is a closed set of parameterized body archetypes**, not fully open
  per-definition art. Mirrors the towers' existing convention (8 flat, single-`Shape`
  bodies) rather than departing from `td/ui/CLAUDE.md`'s "renderer owns every shape/colour
  choice" rule. See V1 Scope for how this actually simplifies the rendering dispatch versus
  what the Architectural implications section above anticipated.
- **The wave-script token syntax needs no new grammar.** `WaveScript.parse` already splits
  on whitespace and calls `EnemyFactory.isEnemy(String)`/`identifyEnemy(String)` on whole
  tokens, not single characters — `c`/`s`/`t`/`g`/`e` are simply the ids the five built-ins
  happen to be pre-registered under, not a length limit the engine enforces. A per-level
  custom or cloned enemy uses an ordinary multi-character id (e.g. `tankySquare`) and is
  otherwise indistinguishable, at the token-parsing level, from a built-in. There is
  deliberately no bracket or prefix syntax to mark a "special" enemy id — built-in and
  per-level ids share one namespace and one lookup path. See V1 Scope for the one real
  consequence of this: `WaveScript.parse` needs a catalog argument instead of the static,
  globally-fixed `EnemyFactory` it calls today.
- **No engine-enforced cap on traits/abilities per definition or on effects stacking on a
  live mob.** Balance is a content-authoring discipline, not an engine limit. The **UI icon
  row is capped** instead — see V1 Scope.
- **The v1 payoff use case for abilities is a boss named the Warden** — see V1 Scope for its
  concrete design.
- **Abilities gain a second action kind: spawning new enemies**, alongside applying an
  effect — triggered periodically (a fixed interval while the mob is alive), once (after a
  fixed delay from spawning), at a health-percentage threshold, or on death. This was added
  during scoping specifically so the Warden's kit can include reinforcement-summoning and an
  on-death "boss egg" hatch-back mechanic, not just effect application.
- **On death, the Warden spawns a stationary boss egg instead of a swarm.** The egg does not
  move (see Architectural implications); if it isn't killed within a fixed time limit (8
  seconds, placeholder), it hatches into a weaker Warden, whose own death spawns another
  egg, and so on — the fight ends only once a player kills an egg before its timer expires.
  This replaces the earlier on-death "shield nearby enemies"/swarm ideas from the first
  scoping pass.
- **v1's spawn-graph validation rule is "acyclic," not "no chaining at all."** A definition's
  spawn ability may name a definition that itself has a spawn ability — the Warden/egg
  encounter requires exactly this — but `EnemyCatalog` registration walks the graph of
  `SpawnEnemiesAction` references and rejects (via `GameStartupException`) any definition
  that, directly or transitively, could spawn itself. This supersedes the first scoping
  pass's blunter "no definition may name a definition with its own spawn ability" rule,
  which would have forbidden the Warden/egg chain outright.
- **The Warden/egg encounter is authored as a finite, explicit chain of stages**, not an
  open-ended procedural weakening — each stage is its own `EnemyDefinition` pair (a Warden
  variant and its egg), consistent with this codebase's preference for named, authored
  content over runtime stat formulas (see the tower-upgrade and new-tower `TODO.md` entries,
  which tune authored constants rather than a formula). The last stage's egg carries no
  spawn ability, so the encounter is guaranteed to terminate in a fixed number of
  hatch-backs. Stage count (a placeholder of 3) and how much weaker each stage is are
  balance-pass numbers, same as everything else in this doc.
- **The Warden gains two more abilities so v1 content exercises every `AbilityTrigger` kind
  and both `AbilityAction` shapes**, not just the three this scoping pass had reached before
  a product-review pass flagged the gap: a **health-threshold-crossed** ability — at 50%
  health (placeholder), casts a shield effect on every other enemy currently in a radius
  around it, a "call to arms" moment — and a **time-since-last-hit** ability — if untouched
  by damage for a fixed window (placeholder), immediately spawns one bonus reinforcement on
  top of its regular periodic one, an "ignored too long" punish. Together with the three
  abilities already designed, every enemy stays on a single boss rather than adding new
  content, matching the discipline the tower-upgrades doc already holds itself to ("each
  condition is used... so the feature exercises all four gates in actual content rather than
  leaving one theoretical") — see Proposed content and Shape of the solution for the full,
  now five-ability kit.

## Open questions

None blocking — see V1 Scope's Phased implementation order for what's deferred and why.
Balance numbers (trait magnitudes, ability thresholds, the Warden's exact stats) are
deliberately left as placeholders for a post-implementation play-test pass, the same way
the tower-upgrades and new-tower-numbers `TODO.md` entries already are for the other two
features — see `FEATURE-playtesting-and-balance-tooling.md` for a proposed (not yet
scoped) piece of future work aimed at making that kind of pass cheap across all three
features, including the two already-shipped ones, instead of manual replay-and-eyeball.

## V1 Scope

With the decisions above settled, this section pins down what a first version actually
contains: concrete content, the shape of the solution, and a phased implementation order.

### Boundary

- All five existing enemies (Circle, Square, Triangle, Ghost, Empty) migrate onto the new
  model; their current behavior is the regression bar (existing `GameEngineTest` and
  friends must keep passing unchanged in intent, even though the implementation underneath
  each one changes).
- One new boss encounter, the **Warden** (and its boss-egg chain), is added as this
  version's proof that the ability system supports more than a straight port of the
  existing five (see Proposed content).
- Closed set of body archetypes for rendering — reusing the towers' shape vocabulary (triangle, circle, spiral, star,
  diamond, kite, plus whatever subset of that list a given
  archetype needs) rather than inventing a parallel one, plus one new archetype for the
  egg's own rounded shape — and a closed, small set of movement behaviors governing *facing*
  only (fixed, path-directional, rotor-spin, pulse), mirroring
  `AbstractEnemyMobDirectional`/`AbstractEnemyMobRotor`'s existing two. Whether a mob *moves at all* is separate and
  needs no behavior of its own — a base speed of `0` is
  already a legal value (see Architectural implications), which is how the egg stands
  still.
- Traits and abilities operate only on effects already in `td.effect`'s vocabulary, plus
  two new `EffectKind`s this version adds: a damage-absorbing shield (flat or percent,
  depletes before health) and invisibility (a mob is not a valid target while it holds this
  kind of effect). **Not in scope**: any trait or ability keyed off a critical hit (the
  doc's "gain a shield after surviving a critical hit" example) — critical damage (`TODO.md`) doesn't exist yet, so that
  trigger has nothing to observe. It's deferred the
  same way feature 3 (tower upgrades) deferred enemy-affecting status effects until feature
  2 shipped — see that doc's V1 Scope for the precedent.
- **Not in scope**: a file-based `LevelCatalog` (a separate, already-tracked `TODO.md` gap);
  more than one boss concept; a UI editor/authoring tool for traits and abilities (they're
  still authored as Java-code `EnemyDefinition` constants, the same way `LevelDefinition`s
  are today via `BuiltInLevelCatalog`); an actual cycle in the spawn graph (a definition
  that, directly or transitively, could spawn itself) — rejected at catalog-registration
  time, per Decisions made. A finite chain (a definition spawning another that spawns
  another, and so on, terminating) is in scope and is exactly what the Warden/egg encounter
  needs.
- Spawning is in scope as a second `Ability` action kind (alongside applying an effect),
  with four trigger kinds: periodic, once (after a delay), health-percentage threshold, and
  on-death. Spawned enemies are added to `EnemyRoster` exactly like a wave's own spawns and
  are counted identically for alive-count and win-condition purposes — no special-casing for
  "did this mob come from a wave or from an ability." The one exception is a
  self-replacing spawn (`consumesSelf`, see Shape of the solution): the replaced mob is
  removed without being treated as a kill — no bounty, no score, no kill-count credit,
  mirroring how `EnemyRoster.clear()` already removes mobs without calling
  `GameHost.enemyDied` for level teardown.

### Proposed content (illustrative — magnitudes are placeholders for a later balance pass)

| Enemy            | Model                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
|------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Circle           | No traits, no abilities — the "nothing modifies this enemy" case is now an empty trait/ability list on its `EnemyDefinition`, not the absence of a Java override.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| Square           | One trait: percent damage-absorb shield, replacing `EnemyMobSquare.absorb()`'s hand-rolled fraction.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| Triangle         | One trait: speed scales with missing health, replacing `EnemyMobTriangle.doDamage()`'s hardcoded curve — expressed as a formula the trait evaluates on-tick against current health fraction, feeding `AbstractEnemyMob`'s existing intrinsic/effective speed split.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| Ghost            | One trait: permanently not a valid target (today's `type = Invisible`), reusing the same "not a valid target" check `AbstractEnemyMob`/targeting already have, just sourced from a trait instead of a hardcoded field.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| **Warden** (new) | One trait, shared by every stage: **armor shield** — a flat amount absorbed per hit, depleting before health drops, distinct in kind from Square's percent shield so the feature demonstrates more than one shield flavor. Every stage's Warden carries the same five-ability kit: (1) **periodic** — spawns a reinforcement enemy (an existing basic type, e.g. a Circle) on a fixed interval while alive; (2) **periodic** — re-casts a shield effect on itself, layering a timed top-up on top of its permanent armor trait so the boss cycles through tougher "phase" windows rather than one flat difficulty; (3) **health-threshold-crossed** (50%, placeholder) — casts a shield effect on every *other* enemy within a radius, a one-time "call to arms" moment mid-fight; (4) **time-since-last-hit** — if untouched by damage for a fixed window (placeholder), immediately spawns one bonus reinforcement on top of ability (1)'s regular one, punishing a player who ignores it; (5) **on death** — spawns its stage's boss egg, replacing the earlier "swarm" idea — a stationary, non-moving enemy with one ability of its own, **once** (hatch on timeout): if not killed within a fixed window (8 seconds, placeholder), it spawns the *next* (weaker) stage's Warden and is itself removed via `consumesSelf`, not killed. The final stage's egg has no hatch ability — it must simply be killed. See the Warden/egg chain note below, and Shape of the solution for `consumesSelf`. Between the trait and its five abilities, the Warden exercises a trait, all five v1 `AbilityTrigger` kinds, both `AbilityAction` shapes (apply-effect on self, apply-effect on others-in-radius, and spawn — both consuming and non-consuming), and a multi-hop (but acyclic) spawn chain — no v1 model surface ships unexercised by real content. |

**The Warden/egg chain** (illustrative, 3 stages — see Decisions made): `Warden` (full strength) → dies → `WardenEgg1` →
hatches on timeout → `Warden2` (weaker) → dies → `WardenEgg2` → hatches on timeout → `Warden3` (weakest) → dies →
`WardenEgg3` (terminal — no hatch ability, must be killed to end the encounter). Six distinct `EnemyDefinition`s in
total; only the egg killed *before* its timer expires ends the fight without another Warden appearing.

### Shape of the solution

- **`EnemyDefinition`** (new, `td.enemy`, a `record` per this codebase's value-type rule):
  id (the wave-script token), display name, base health, price, a `BodyArchetype`, a
  `MovementBehavior`, a `List<Trait>`, a `List<Ability>`. Built once at registration time,
  not per-spawn.
- **`EnemyCatalog`** (new, `td.enemy`) replaces `EnemyFactory` as the source of buildable
  enemy types, naming it after the existing `LevelCatalog`/`BuiltInLevelCatalog` precedent
  rather than reusing the word "Registry" — `td.enemy.EnemyRegistry` already names the *live per-wave roster* read
  interface, and overloading the name would be exactly the kind
  of collision the Power-tower/tower-upgrade rename avoided in feature 3. The five built-ins (plus the Warden/egg
  chain's 6 definitions) are pre-registered globally; a level can register new `EnemyDefinition`s
  or clone-and-adjust an existing one (e.g. `EnemyCatalog.clone(String baseId, String newId,
  UnaryOperator<EnemyDefinition> adjust)`), scoped to that level only.
- **One concrete `EnemyMob` class replaces the five leaf classes.** Since behavior now comes
  from a definition's trait/ability list rather than from which Java class was instantiated,
  there's no longer a closed set of *classes* to dispatch over — only a closed set of
  `BodyArchetype`/`MovementBehavior` *values*, which a plain exhaustive `switch` (no
  `default`, same discipline as `EnemyFactory.Enemy.create` today) handles without a
  visitor. **This resolves the Architectural implications section's rendering-dispatch
  tension more cleanly than either option it anticipated**: `EnemyMobVisitor`'s 5-method
  interface is retired rather than widened, because after migration there's only one
  concrete class left to visit. `td.ui.EnemyFrameBuilder` switches on `mob.definition()
  .bodyArchetype()` directly; the compile-time safety net moves from "one visitor method
  per Java class" to "one switch case per archetype" — arguably tighter, since archetypes
  are inherently bounded by what the renderer knows how to draw, while `EnemyDefinition`s
  are inherently open by design.
- **`Trait`** (new, `td.enemy`, a narrow interface with default no-op methods): `onHit
  (Damage incoming) -> Damage` (generalizing `absorb`), `isValidTarget() -> boolean`
  (generalizing the `Normal`/`Invisible` closed enum Ghost hardcodes today), `speedFactor
  (float healthFraction) -> float` (generalizing Triangle's curve). A mob's effective value
  for each folds every trait's contribution the same way `getSpeed()` already folds
  `ActiveEffects.speedMultiplier()` on top of the intrinsic value — traits and effects are
  two independent multiplier sources feeding the same read path, not a new branch.
- **`Ability`** (new, `td.enemy`) pairs an `AbilityTrigger` with an `AbilityAction`, both *closed* for v1 (not an open
  predicate/effect pair) to keep evaluation cheap on the
  `game-loop` thread per the Performance note above:
    - `AbilityTrigger`: health-threshold-crossed, on-death, time-since-last-hit, **periodic**
      (a fixed-interval cooldown while the mob is alive), and **once**, which now carries a
      delay-in-ticks parameter (fires exactly once, that many ticks after the mob spawned).
      The last two are new, added to support spawning. `once` needs no explicit "cancel if the
      mob dies first" case — `doTick` already never runs again for a dead mob (see
      `td/enemy/CLAUDE.md`), so a delayed ability simply never fires if its mob is killed
      before the delay elapses — this is exactly the boss egg's "if not defeated within 8
      seconds" rule, for free. All five kinds are exercised by v1 content (the Warden) rather
      than three of the five, as an earlier pass through this scoping had it: `health-
    threshold-crossed` needs edge-triggered evaluation (fire once when health crosses the
      boundary, not once per tick while below it, tracked via a small per-mob "already fired"
      flag reset only if the mob's effective max health ever changes, which it doesn't in
      v1), and `time-since-last-hit` needs a new per-mob tick counter reset in `doDamage` on
      every hit landed and read on `doTick` — both small, cheap pieces of state, not
      per-tick lookups.
    - `AbilityAction`: a small sealed hierarchy with two implementations —
      `ApplyEffectAction` (apply an `Effect` to itself, or to every enemy within a radius, via
      the `EnemyRegistry` it's already constructed with) and `SpawnEnemiesAction` (an
      `EnemyDefinition` id plus a count, constructed through the same `EnemyCatalog` the mob
      itself came from and added to `EnemyRoster` at the mob's own position, plus a
      `consumesSelf` flag). Pattern-matching over this sealed pair in the one place that
      executes an ability is the same narrow, compiler-checked exception `Java2DFrameRenderer`
      already has for `RenderFrame`'s sealed draw-command hierarchy (root `CLAUDE.md`'s
      no-`instanceof` rule) — not a general license to branch on `EnemyMob`/`Trait` types,
      which stays off-limits.
    - **`consumesSelf`** (new field on `SpawnEnemiesAction`): when true, the ability removes
      the triggering mob from `EnemyRoster` in the same step it spawns the replacement,
      through a new third roster-removal path alongside `remove()`/`clear()` (see
      Architectural implications) that, like `clear()`, does **not** call
      `GameHost.enemyDied` — a hatch is a transformation, not a kill, so it awards no bounty,
      no score, and no kill-count-gate credit. The boss egg's hatch-on-timeout ability is the
      only v1 use of `consumesSelf`; the Warden's own on-death spawn doesn't need it, since a
      death already removes the mob through the existing path.
    - `EnemyCatalog` registration walks the directed graph formed by every `EnemyDefinition`'s
      `SpawnEnemiesAction` references and rejects (via `GameStartupException`) any definition
      that, directly or transitively, could spawn itself — a standard cycle check (depth-first,
      tracking the definitions currently on the walk's stack), not a blanket "no chaining"
      rule. A finite, strictly linear chain like the Warden's 6-stage sequence passes; a true
      cycle (two definitions each naming the other) does not. This supersedes the first
      scoping pass's stricter one-hop-only guard.
- **`EffectKind` grows two v1 cases**: a shield kind (flat or percent, carried as new fields
  on `Effect` alongside the existing `speedMultiplier`/`damagePerTick`, consumed by `onHit`
  before health drops) and an invisibility kind. `ActiveEffects.magnitude`'s exhaustive
  switch (no `default`) means the compiler flags every call site that needs a new case —
  the same safety net `Damage.DamageType` and `EffectKind` already rely on.
- **`WaveScript.parse` gains a catalog argument.** Today it's a pure function over
  `EnemyFactory`'s static, globally-fixed table, which is exactly what makes it trivially
  testable per `td/wave/CLAUDE.md`. A per-level custom id only resolves against that level's
  `EnemyCatalog`, so `parse` takes one as a parameter instead of reaching for a static
  table — it stays just as pure and testable, only with the catalog constructed inline in
  each test instead of assumed globally. `GameEngine.loadLevel` builds the level's
  `EnemyCatalog` (global definitions plus the level's own registrations/clones) before
  parsing any of its waves.
- **`LevelDefinition` gains a field** for its enemy roster (definitions registered or cloned
  for that level only) — every construction site (`BuiltInLevelCatalog`, test fixtures)
  needs updating, the same mechanical fan-out the `smoothing`/`startingLives` fields caused
  when they were added.
- **Validation moves from compile-time to level-load-time.** An `EnemyDefinition` missing a
  required archetype/movement mapping, or a wave token that resolves to nothing in the
  level's catalog, throws `td.util.GameStartupException` — the project's one existing fatal-
  startup boundary — rather than failing silently or mid-game.
- **Effect icon UI**: a small closed set of icon glyphs, one per `EffectKind` (shield,
  invisibility, slow, freeze, burn), painted in a row in a sprite corner. The row caps at 3
  visible icons; a 4th+ simultaneous effect collapses into a `+N` badge rather than growing
  the row — keeps the "no trait or ability ships without an unambiguous on-board visual"
  requirement (Product review notes, above) legible even on a heavily-buffed enemy in a
  packed wave, without an unbounded layout.

### Phased implementation order

Per this project's standing "commit after each phase" convention:

1. **Core data model, headless**: `EnemyDefinition`, `EnemyCatalog` (global registration +
   per-level clone/register, including the chained-spawn validation), `Trait`/`Ability`/
   `AbilityTrigger`/`AbilityAction` (`ApplyEffectAction`/`SpawnEnemiesAction`), the two new
   `EffectKind` cases and `Effect` fields. No enemy migrated yet, no UI — provable entirely
   against constructed definitions, `ActiveEffects`, and a fake/headless `EnemyRoster`.
2. **Migrate the five built-ins onto the model**: one concrete `EnemyMob` class replacing
   the five leaves; `EnemyMobVisitor` retired; `EnemyFrameBuilder`/`Java2DFrameRenderer`
   switch on `BodyArchetype`/`MovementBehavior` instead of the visitor. Existing
   `GameEngineTest` behavior is the regression bar — no gameplay change should be visible.
3. **Wave-script and level-authoring plumbing**: `WaveScript.parse` takes an `EnemyCatalog`;
   `LevelDefinition` gains its enemy-roster field; `GameEngine.loadLevel` builds the
   per-level catalog before parsing waves; `GameStartupException` validation at load time.
4. **The Warden**: the full 6-definition Warden/egg chain — armor-shield trait, all five
   abilities on every Warden stage (the two periodic ones, health-threshold-crossed,
   time-since-last-hit, and on-death), `consumesSelf` on the egg's hatch ability, and the
   terminal egg's lack of one — plus its own archetype/art and the egg's new archetype. The
   first content that only exists through this new model, proving the registry, both
   `AbilityAction` shapes (self and radius-others `ApplyEffectAction`, consuming and
   non-consuming `SpawnEnemiesAction`), `consumesSelf`, the multi-hop acyclic-chain
   validation, and all five `AbilityTrigger` kinds end to end — no part of the v1 model ships
   unexercised.
5. **Effect icon UI**: the icon row, the 3-icon-plus-overflow-badge cap, wired to every
   `EffectKind` including the two new ones. Verified visually via the `run-jtd` skill, per
   this project's UI requirement.
6. **Documentation and balance pass**: update `td/enemy/CLAUDE.md`'s "Adding a new enemy
   type" checklist for the new model (it currently describes editing `EnemyFactory.Enemy`,
   `EnemyMobVisitor`, and the leaf-class checklist, all retired by this feature), update the
   root `CLAUDE.md`'s wave mini-language section and `README.md`'s enemy table, then play
   each built-in level to tune the Warden/egg chain's placeholder numbers (stage count,
   per-stage stats, the 8-second hatch timer) and the migrated traits' placeholder magnitudes
   and add a `TODO.md` entry for that balance work, matching the pattern the other two
   features' number-tuning gaps already follow.
