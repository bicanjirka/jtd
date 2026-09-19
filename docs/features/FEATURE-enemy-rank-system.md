# Feature Request: Enemy Rank System

**Status: implemented.** Landed in four phases: (1) the `TraitId`/`IdentifiedTrait`/
`IdentifiedAbility` identity mechanism, (2) `Rank` replacing `int level` mechanically
throughout the engine, (3) `RankedEnemy`, the wave-script rank grammar, `armored`, and content
(all four basic built-ins are now full five-rank ladders; `SIMPLE`'s own ladder demonstrates a
rank adding an identified trait and a later rank replacing it with a stronger one), and (4) the
on-board/wave-preview rank badges. See the root `CLAUDE.md`, `td/wave/CLAUDE.md` and
`td/enemy/CLAUDE.md` for the resulting invariants; this document is kept for the history below.

## Summary

Replace the single numeric `level` difficulty knob with a named, visually-badged **rank**
system, and move enemy stats (health, bounty) off the wave and onto the enemy definition
itself, scoped per rank. Today a wave says "everything in this wave has 450 hp, is worth 3
credits, and is level 2." Under this feature, a rank-1 Circle is *always* the same health and
bounty, in every level; a wave says "this wave defaults to rank 2," and any slot can name a
different rank for just that spawn.

Rank also absorbs one of `SpawnShape`'s two non-formation shapes outright: `boss` stops being
a wave-script spawn-shape keyword and becomes the top tier of the rank ladder itself, since it
never did anything formation-shaped (no offset, no multi-member spacing) - it was always a
toughness dial on one enemy wearing a formation keyword's costume. `elite` survives, renamed to
**`armored`**, now that "Elite" the rank name and `armored` the spawn-shape keyword no longer
collide - but its job changes: rather than a stat multiplier, it becomes this feature's own
demonstration that a spawn shape can attach or override a spawned enemy's traits/abilities, the
same identity-based mechanism rank-authoring itself needs. And a kill's score value stops
mirroring its bounty 1:1: a higher-rank kill is worth disproportionately more score than an
equivalent-bounty low-rank kill.

## Current state (what exists today)

- `WaveDefinition` (`td/wave/WaveDefinition.java`) carries `hp`, `price` and `level` once per
  wave — every enemy type mixed into that wave shares all three, regardless of what kind of
  enemy it is.
- `EnemyDefinition` (`td/enemy/EnemyDefinition.java`) already has its own `baseHealth`/`price`
  fields, but its own doc comment says a normal, wave-spawned enemy **ignores them completely**
  — they're only read by an *ability*-spawned enemy (the Warden boss's egg-hatch chain via
  `SpawnEnemiesAction`), which has no wave slot of its own to inherit stats from. This is the
  reverse of what this feature wants to be true everywhere.
- `level` (an `int`) flows from `WaveDefinition` through `Wave` and `SpawnParameters` into
  `DefinedEnemyMob`, where it is read in exactly four places:
  - `DefinedEnemyMob.bodyScaleFor` — grows Square/Triangle/Ghost's sprite as level rises,
    capped at level 6 (`DefinedEnemyMob.java:67-76`).
  - `PercentResistTrait.onHit` — Square's damage resistance **decreases** by
    `perLevelReduction × level` (`PercentResistTrait.java:14`); higher level makes Square
    *less* tanky, on the theory that the wave's own `hp` is what should carry difficulty.
  - `HurtSpeedTrait.speedFactor` — Triangle's low-health speed spike scales up with level
    (`HurtSpeedTrait.java:14-15`).
  - `AbstractEnemyMob.fadeDurationTicks` — `3 × level + 6` (`AbstractEnemyMob.java:363`).
  
  Circle has no level-scaled trait at all; Ghost only gets the body-scale effect.
- The wave mini-language already has the exact grammar shape the requester's `"rank1 swarm 4
  c"` example follows: a keyword sits *before* the token it modifies, and a spawn-type keyword
  (`boss`, `swarm`, `line`, `flank`, `column`, `drip`) shapes the one slot count/id after it —
  see `td/wave/CLAUDE.md`'s spawn-shape keyword table. `WaveScript.RESERVED_TOKENS`
  (`WaveScript.java:45`) is currently the spacer `e` plus those seven spawn-type keywords;
  `EnemyCatalog.register` already refuses to register a definition id that collides with one.
- **No built-in level actually uses the `boss` or `elite` spawn-shape keyword.** Verified by
  grepping every wave string in `BuiltInLevelCatalog.java`: zero matches for either token. Even
  the Warden fight doesn't use `boss warden1` - it spawns bare `warden1` and gets its toughness
  from three hand-authored definitions (`warden1`/`warden2`/`warden3`) swapped by an
  ability-driven death transition instead. Removing both keywords from `SpawnShape` costs
  nothing against shipped content.
- `Trait` (`td/enemy/Trait.java`) and `Ability` (`td/enemy/Ability.java`) are both plain,
  identity-free types today — `Ability` is `record Ability(AbilityTrigger, AbilityAction)`, and
  `EnemyDefinition.traits()`/`.abilities()` are just `List.copyOf(...)`. Nothing today lets two
  trait instances be recognized as "the same trait, a stronger version" — a second instance
  added later is simply a second, additive list entry.
- `EnemyDefinition` is already built the way `CLAUDE.md` §5 requires for a wide value type: a
  narrow required-shape factory (`EnemyDefinition.of(...)`) plus fluent `withX` copies
  (`withDescription`, `withHealthDivisor`, `withMobType`, `withMovement`, `withTraits`,
  `withAbilities`).
- **Score**, for context since it's asked about below: `EconomyDelta.kill(bounty)` adds the
  same amount to both `credits` and `score`; `leak(penalty)` subtracts from `score` as well as
  costing a life (`EconomyDelta.java:32-41`). Nothing in the codebase reads
  `EconomyLedger.getScore()` except the HUD's score label (`PanelGameConsole.java:116`) and two
  `LOG.info` lines at game over/win (`TowerDefense.java:483,495`) — no persistence, no
  leaderboard, no win condition depends on it.
- The wave-preview strip (`PanelEnemy`/`PathWaveRow`) shows a numeric `"lvl N"` `JLabel` per
  wave row today, plus each enemy type's preview sprite and spawn count — no per-enemy badge of
  any kind exists.
- The board renderer already has a precedent for a small ring drawn around a body to signal
  "this one is special": `Java2DFrameRenderer.paintUpgradeAccent`, fed by
  `TowerSpriteDraw.accent()`, draws a tower's chosen upgrade path as a thin two-color ring
  outside its shape. There's also an existing chevron shape primitive,
  `td.ui.render.PathMarkerShape.CHEVRON`, currently used for path-direction markers.

## What this feature adds

- **`level` (int) is replaced everywhere by a named `Rank`.** No numeric level survives, inside
  the engine or in front of the player.
- **`baseHealth` and bounty move fully onto `EnemyDefinition`, scoped per rank.** Any rank-1
  Circle has the same starting health and the same bounty in every level that spawns it.
  `WaveDefinition` stops carrying `hp`/`price`.
- **A wave declares a default rank for its slots.** Any individual slot can override it inline
  — the requester's own example, `"rank1 swarm 4 c"`, spawns four rank-1 Circles even if the
  wave's own default is rank 2 — following the same keyword-before-token pattern the
  spawn-shape grammar already established.
- **An enemy kind declares which ranks it supports, and each rank is authored as a change from
  the previous rank, not from scratch.** The requester's own pseudocode illustrates the intent
  (see below) — the actual pattern is for planning to design, not mandated by this request.
- **If a wave or slot asks for a rank an enemy kind doesn't define, that enemy's own highest
  defined rank is used instead.** This is never an authoring error — an enemy that only
  supports ranks 1-3 spawns at rank 3 for a rank-5 wave slot, silently and by design.
- **Every enemy displays its rank as a visual badge — not a number — in both the wave-preview
  panel and on the board.** The badge's visual language should reflect the naming direction
  chosen (see Open questions) — army rank insignia, if that's the direction.
- **A trait or ability may optionally carry an identity.** A later rank's definition step can
  then *replace* an earlier rank's same-identified trait/ability (a stronger Shield overwriting
  a weaker one) instead of stacking a second instance alongside it. Leaving the identity
  unspecified means "always a new, additive entry," matching today's behavior exactly, and
  should probably generate something unique under the hood so an unidentified trait never
  accidentally collides with another unidentified one.
- **`SpawnShape.boss()` and its `boss` wave-script keyword are removed.** The Boss rank tier
  takes over its job - a Boss-rank enemy is bigger and tougher because its own definition says
  so at that rank, not because a generic formation-layer multiplier was bolted on at spawn
  time. This is a real behavior change, not just a rename - see Constraints for what it costs.
- **`SpawnShape.elite()` survives, renamed to `armored`, and its job changes completely.** It
  drops its stat multiplier entirely and becomes this feature's proof that a spawn shape can
  attach or override a spawned enemy's traits/abilities - concretely, giving the spawned enemy
  an armor/defensive trait it wouldn't otherwise have, via the same optional trait-identity
  mechanism rank-authoring needs (see below). No size, health or bounty change survives from
  today's Elite. `SpawnShape` therefore keeps seven shapes: Normal, Swarm, Line, Flank, Column,
  Drip, and Armored - six about formation, one deliberately about trait composition instead.
- **A kill's score value is decoupled from its bounty and scales with the killed enemy's
  rank.** A Grunt-rank kill and a Boss-rank kill no longer earn the same score-per-credit
  ratio; score becomes a genuine difficulty-adjusted "how well did you defend" number instead
  of a shadow of the credit counter. The exact weighting curve is planning's to design.

The requester's illustrative pseudocode, given only to convey the shape of the idea — the
actual builder/registration pattern, and how it fits `EnemyDefinition`'s existing `withX`-copy
discipline, is planning's to design:

```java
EnemyFactory.newEnemy("c", Map.of(
    Rank.RANK_1, e -> EnemyDefinition.withHealth(100).withTrait(new Shield(...)),
    Rank.RANK_2, e -> e.withHealth(200)
));
```

Rank 1 defines the Circle from scratch: 100 health, a Shield trait. Rank 2 starts from rank 1's
own result (the `e` passed in) and only changes health to 200 — it still carries rank 1's
Shield, since nothing removed it. A rank 2 that instead wants a *stronger* Shield (same trait,
better numbers) is the case that needs trait identity: without it, rank 2 would carry *two*
Shields rather than one upgraded one.

**Likely out of scope for a first version** (my read of the request — please confirm or
correct): a rank auto-derived from wave/round index rather than hand-authored per wave; a
per-mob random rank roll within one spawn; any in-game UI for editing ranks at runtime.

## Interconnections

- **Narrows `FEATURE-enemy-spawn-types.md`'s `SpawnShape` boundary for `boss`, deliberately -
  but not for `elite`/`armored`.** That feature already self-identified Boss and Elite as the
  odd ones out among its eight shapes - both use only "mechanism 1" (per-mob multipliers,
  Elite adding "mechanism 2's" permanent damage reduction), with none of the lateral-offset or
  delay-spacing machinery every other shape needs. `boss` is fully absorbed - the Boss rank
  tier takes over its job entirely. `elite`, renamed `armored`, is kept on purpose: its
  multiplier-only mechanism is replaced (not supplemented - `armored` is trait-only, no
  multiplier survives) by trait/ability composition, which is a *new* fourth-ish mechanism for
  `SpawnShape`, not a formation one. This also resolves a tension that feature's own doc
  flagged and explicitly declined to fix -
  it rejected "spawn type as a property of the definition" because a spawn type has to stay a
  wave-authoring choice, independent of the enemy's identity. Rank already has that exact shape
  (a wave declares a default, a slot can override it), and `armored` keeps it too, so nothing
  from that rejection is reintroduced by either move.
- **`SpawnShape.boss()`/`elite()` currently compute their bounty multipliers (2×/1.5×) and
  size percentages against *the wave's* `baseHealth`/`basePrice`** (`Wave.spawnShaped`). Once
  those numbers move onto the enemy/rank instead, there is no wave-level number left to
  multiply for `boss` - its rank's numbers have to be authored directly (via the
  rank-builds-on-rank mechanism). `armored` sidesteps this entirely by carrying no multiplier
  at all - it is purely about the attached trait, so it never needs a wave- or rank-level
  number to multiply against.
- **The trait/ability identity mechanism (Constraints, below) now has two required call
  sites, not one.** It was already needed for a higher rank to replace an earlier rank's same
  trait. `armored` needing to attach (and, on an already-armored enemy, potentially replace) a
  defensive trait at the spawn-shape layer means the same mechanism has to work cleanly from
  *both* directions - planning should design it generally, not as something shaped narrowly
  around the rank-authoring pseudocode alone.
- The per-slot rank override uses the rank names themselves (`grunt`/`soldier`/`veteran`/
  `elite`/`boss`) as new entries in `WaveScript.RESERVED_TOKENS`, alongside the renamed
  `armored` keyword - replacing only the one `boss` entry removed from the spawn-shape side.
  `EnemyCatalog.register`'s existing collision guard covers this either way.
- `AbstractEnemyMob.fadeDurationTicks()` (`3 × level + 6`) and the body-scale cap "at level 6"
  are real, existing consumers of the numeric value that need a replacement once rank isn't a
  plain int — not hypothetical, already-shipped behavior.
- `td/enemy/CLAUDE.md`'s existing rule that "a `Trait` instance is shared across every mob
  built from the same `EnemyDefinition`... don't bake a level into a `Trait` at construction
  time" is largely about to become moot: if a rank's numbers are baked into the definition at
  authoring time (per the pseudocode above) rather than read live via `TraitContext.level()`,
  a rank's traits can genuinely be different concrete instances per rank. `TraitContext` as it
  exists today is likely obsolete under this model — planning should treat that as expected,
  not as a compatibility constraint to preserve.

## Constraints and open risks

- **Trait/ability identity is a real, unsolved design problem — not a rename.** Traits and
  abilities are currently anonymous, order-independent list entries with no notion of "this one
  plays the same role as that one." "Replace the weaker shield with the stronger one" requires
  deciding what identity means (a string id field? a marker/role interface? keying by concrete
  Java type, which would forbid two distinct shield-like traits ever coexisting on one
  definition?), how a replacement composes with "this rank is built from the previous rank's
  result," and what happens if two unrelated enemy kinds happen to reuse the same id. This is
  explicitly flagged as open rather than answered here — it's exactly the kind of question a
  planning phase has the room to explore properly.
- **`EnemyDefinition` is already a wide value type** (12 components) governed by `CLAUDE.md`
  §5's "grows by fluent `withX` copy, never a widening constructor/factory" rule. A per-rank,
  definition-building-on-definition pattern (the requester's `Map<Rank, ...>` idea being one
  candidate) has to live inside that existing discipline, not around it.
- **Every wave-spawned enemy currently ignores its own definition's `baseHealth`/`price`** —
  only the Warden's ability-spawned egg chain reads them today (see Current state). This
  feature fully reverses which path is "normal": the egg-hatch spawn path needs re-examining
  under the new model too, not just the everyday wave-spawn path.
- **`SpawnShape`'s remaining multipliers (`swarm`'s exact bounty-share split, etc.) currently
  operate on a wave's `baseHealth`/`basePrice`.** Once those numbers live on the enemy/rank,
  every remaining shape needs its arithmetic re-derived against a per-mob base.
- **Removing `boss` from `SpawnShape` removes a universal, free lever - and keeping `armored`
  means the game ends up with one axis that's free and one that isn't.** Today `boss c` makes
  *any* registered enemy id bigger/slower/worth more, with zero authoring per enemy kind - the
  multiplier is generic. Under Rank, an enemy only spawns at Boss tier if someone explicitly
  authored that enemy's Boss-rank definition; a wave/slot asking for a rank it doesn't have
  silently gets that enemy's own highest defined rank instead. Meanwhile `armored` stays a
  free, generic, any-enemy lever exactly like it is today - toughness via rank costs authoring
  per enemy kind, toughness via `armored` doesn't. That asymmetry appears to be the deliberate
  point of keeping `armored` around (a showcase of trait composition, not a rank-authoring
  shortcut), but it's worth naming so it isn't read as an inconsistency later.
- **Two of the five badges have no existing render vocabulary; two do.** A chevron already
  exists as a render primitive (`PathMarkerShape.CHEVRON`), so Soldier's one stripe and
  Veteran's two are cheap. A star shape also already exists, just not for this purpose -
  `Java2DFrameRenderer`'s tower bodies already include a star shape (`td/ui/CLAUDE.md`'s "every
  tower body is a single closed Shape (triangle, circle, spiral, star, pulsar)"), so Elite's
  gold star likely reuses that geometry rather than inventing new geometry. Boss's silver skull
  has no precedent anywhere in the existing vector-art vocabulary and is a genuinely new shape
  to design - everything drawn today is geometric primitives (circle, square, triangle, spiral,
  star, pulsar); a skull is the first representational glyph.
- **"Use the enemy's own highest defined rank" needs an explicit ordering**, not just a set of
  names — once ranks are named rather than integers, "highest" has to be defined by the rank
  type itself.
- Standing project requirements this still has to satisfy: the engine stays headless (a rank
  badge is a `td.ui.render` frame-builder concern, never domain logic touching AWT — root
  `CLAUDE.md` §2.4); no `null`, no `instanceof` — a closed, ordered `Rank` type (an `enum` or a
  sealed hierarchy) fits naturally and keeps "highest supported rank" a compile-checked
  question rather than a runtime one; `EnemyDefinition`'s wide-value-type rule above; and
  `docs/features/`, `td/wave/CLAUDE.md`, `td/enemy/CLAUDE.md` and `td/ui/CLAUDE.md` all need
  updating in the same change as the code, since this rewrites invariants each of them
  currently documents.

## Decisions made

- Rank fully replaces level — no numeric level survives anywhere, including internally.
- Rank is authored per-enemy-kind, progressively: each supported rank builds on the previous
  rank's result, not as an independent flat table of per-rank definitions.
- A wave declares a default rank; any slot may override it inline, in the spawn-shape-keyword
  grammar style.
- A rank an enemy kind doesn't define is not an authoring error — the enemy's own highest
  defined rank is used silently.
- `baseHealth` and bounty (`price`) move from `WaveDefinition` onto `EnemyDefinition`, scoped
  per rank.
- Rank is always shown as a visual badge, in both the wave-preview panel and on the board —
  never as a plain number, anywhere the player can see it.
- Traits/abilities may optionally carry an identity so a higher rank can replace a specific
  earlier one instead of stacking; an unidentified one is always additive. The identity
  mechanism's actual shape is deferred to planning (see Constraints).
- **The rank ladder is Grunt → Soldier → Veteran → Elite → Boss** (5 tiers, final). No numeric
  level or generic "rank1"/"rank2" name survives in front of the player - these five words are
  the rank system.
- **Each rank's badge is fixed:** Grunt carries no badge at all (the unranked default); Soldier
  gets one army-style chevron; Veteran gets two; Elite gets a gold star; Boss gets a bright
  silver skull. See Constraints for which of these need new render vocabulary.
- `SpawnShape.boss()` and its `boss` wave-script keyword are removed; Boss becomes the top
  tier of the rank ladder instead. No built-in level content is affected - nothing uses the
  `boss` keyword today (see Current state).
- `SpawnShape.elite()` is **kept**, renamed to `armored`, and repurposed as **trait-only**: it
  attaches or overrides a defensive trait on the spawned enemy and does nothing else - no size,
  health or bounty multiplier survives from today's Elite. It exists to demonstrate that a
  spawn shape can modify a spawned enemy's traits/abilities via the same identity-based
  mechanism rank-authoring needs, and never double-dips against an enemy's own rank-authored
  numbers.
- **The per-slot rank override uses the rank names themselves as reserved tokens** -
  `grunt`/`soldier`/`veteran`/`elite`/`boss` prefix a slot directly (e.g. `elite swarm 4 c`),
  not a generic `rank1`..`rank5` scheme. These join `armored` and the five formation keywords
  in `WaveScript.RESERVED_TOKENS`.
- **Not every enemy kind has to define all five ranks.** An enemy that only defines up through
  Veteran is legal; a wave or slot asking for Elite/Boss on it silently gets its Veteran
  definition instead, via the already-decided highest-defined-rank fallback. Nothing is
  enforced at registration time beyond that.
- Score is rank-weighted rather than a 1:1 mirror of bounty: a higher-rank kill is worth
  disproportionately more score than an equivalent-bounty lower-rank kill. The exact curve is
  planning's to design.
- **No persisted high score, for this iteration.** Not deferred as "maybe" - a plain no for
  now. A future request can revisit it if wanted.

## Open questions

None remain. The one item left for planning - whether a count placed *before* a rank-name token
(`3 elite swarm 4 c`) repeats the whole ranked-and-shaped slot - was resolved yes, symmetrically
with a count before a spawn-shape keyword: whichever of the two keywords comes first in a slot is
the one a leading count attaches to, since a rank keyword (when present) always precedes a
spawn-type one. See `td/wave/CLAUDE.md`'s wave-composition section.

## Deferred scope

Still out of scope, as this request originally called out - no future request exists for these
yet:

- A rank auto-derived from the wave/round index rather than hand-authored per wave.
- A per-mob random rank roll within one spawn.
- Any in-game UI for editing ranks at runtime.

Also not done, and worth naming since it wasn't explicitly deferred up front: `ARMORED`/
`FRENZIED`/`GHOST`/`WARDEN`'s traits (`PercentResistTrait`/`HurtSpeedTrait`/`FlatResistTrait`)
stay a single concrete value across all five of `SIMPLE`'s sibling ladders' own ranks - only
`SIMPLE` demonstrates a trait actually changing (being added, then replaced) rank to rank. Giving
the other three basics their own rank-scaled trait tuning is real content work for a later pass,
not a mechanism gap - the identity-replace machinery already supports it.
