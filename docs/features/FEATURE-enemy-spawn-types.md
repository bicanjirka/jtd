# Feature Request: Enemy Spawn Types

**Status: proposed, not implemented.** Written after the architecture audit of 2026-09-17,
against the tree at that point. Every code fact cited below was verified against the source at
the time of writing; check the "Notes for a future session" section at the bottom before
trusting any line number.

## Summary

Today every spawn slot in a wave produces exactly one enemy, at the centre of the path, at its
definition's own size and speed. This feature makes **how a slot spawns** a first-class,
composable choice alongside **what it spawns**:

- **Normal** — one enemy on the path centre. Today's behaviour, unchanged.
- **Swarm** — one slot produces *N* enemies at 50% size, scattered off-path in a fixed
  formation they hold for their whole run, splitting one enemy's bounty between them.
- **Boss** — one enemy at 200% size and 50% speed, paying double bounty.

The unifying idea is that a `WaveSlot` stops meaning "one mob" and starts meaning "a group with
a shape". That single change is also what removes `EnemyMobEmpty`, so the spacer cleanup is not
a bundled chore — it is a prerequisite this feature pays for anyway. See **Part two**.

This is deliberately *not* a second way to express what enemies already do. Traits and abilities
(`td.enemy.Trait`, `td.enemy.Ability`) own behaviour — resistance, hurt-speed, invisibility,
spawning on death. A spawn type owns **formation, presentation and economics**: how many, where
they sit relative to the path, how big, how fast, and how the bounty divides. Where the two
could overlap, the trait model wins and the spawn type stays out. See **Boundary**.

## Current state (what exists today)

- `WaveScript.parse(tokens, catalog)` turns a token string into a `WaveContent`: an ordered
  `List<WaveSlot>` with repeat counts already flattened. `WaveSlot` is a **sealed interface
  permitting exactly `EnemySlot` and `EmptySlot`**.
- `Wave.spawn()` walks that sequence and calls `spawnSlot`, which returns **exactly one
  `EnemyMob` per slot**, always — an `EnemyMobEmpty` for the spacer, a `DefinedEnemyMob`
  otherwise. `delay` increments once per slot and is the mob's spawn countdown.
- `AbstractEnemyMob` converts that slot index into ticks:
  `delay = round(DELAY_TICKS_PER_SLOT * slotIndex / speed)`.
- Position comes from `updatePosition()`: `arcLengthPath.poseAt(distanceIntoLap)` yields a
  `PathPose(Vec2 position, double facingRadians)`, and the mob sits **exactly on** that point.
  There is no lateral offset anywhere in the model.
- Size comes from `DefinedEnemyMob.bodyScaleFor(archetype, boardScale, level)`, a static switch
  with no per-mob multiplier.
- Bounty is `int` end to end: `EnemyDefinition.price` → `AbstractEnemyMob.price` →
  `EconomyDelta.kill(int bounty)`.
- `WaveContent.enemyCount()` counts `EnemySlot`s. `GameWorld.startWave(w)` seeds the roster's
  alive count from it, and that count reaching zero is what declares the wave cleared.

Three things in that list are already in the feature's favour:

1. **`WaveSlot` is sealed**, so adding a case is a compile error at every site that must handle
   it — `Wave.spawnSlot` and `WaveContent`'s three counting methods.
2. **`EnemyBodyDraw` already carries a per-mob `scale`** field. Drawing a mob at 50% or 200% needs
   *no renderer change at all*.
3. **`RandomSource.seeded(long)` already exists** and `RandomSource` is a `@FunctionalInterface`,
   so deterministic per-level scatter needs no new randomness plumbing.

## What this feature adds

### The slot model change

One signature change carries the whole feature:

```java
// today — a slot is exactly one mob, always
private static EnemyMob spawnSlot(WaveSlot slot, GameWorld world, int delay, ...)

// proposed — a slot is a group, which may be empty or many
private static List<EnemyMob> spawnSlot(WaveSlot slot, GameWorld world, int delay, ...)
```

`delay` keeps incrementing once per slot regardless of how many mobs the slot produced, so
**spawn timing for existing waves is bit-identical**. Every member of a swarm shares its slot's
delay and therefore arrives together, which is what makes it a swarm rather than a queue.

### Normal spawn

Unchanged, and it stays the default with no token: a bare `c` is a normal spawn.

### Swarm spawn

One slot, *N* members of the same definition, each:

- drawn at **50%** of its normal body scale;
- placed at a fixed **lateral (perpendicular) offset** from the path centre, scattered evenly
  within the slot's footprint, and holding that offset for its entire run so the formation
  follows the path around corners rather than smearing;
- worth **`price / N`** of the definition's bounty, so a swarm of 3 pays the same total as one
  normal spawn of the same enemy.

Health is an open question (see below): the same `hp` per member makes a swarm of 3 three times
as durable as a normal spawn for the same bounty.

The scatter must be **stable across playthroughs**: the same level, wave and slot must produce
the same formation every time. That means a `RandomSource.seeded(...)` derived from level
identity + wave index + slot index — explicitly **not** `GameWorld.random()`, which is shared,
unseeded, and consumed by tower targeting, so drawing from it would make a formation depend on
how many towers happened to fire first.

### Boss spawn

One slot, one member, at **200%** body scale, **50%** speed, paying **2×** bounty. Mechanically
the cheapest of the three: it is the normal path with three multipliers.

### Additional spawn types worth considering

From the tower-defense genre generally, these are the ones that are genuinely *slot-shaped* —
formation, presentation or economics — rather than behaviour a trait already covers. Roughly
ordered by value-per-effort once swarm and boss exist:

| Type | Shape | Cost once swarm exists |
|---|---|---|
| **Elite / Champion** | 1 member, ~150% size, more health, ~1.5× bounty | Trivial — the boss knobs at different magnitudes |
| **Column / Lockstep** | *N* members single-file at sub-slot spacing, tighter than *N* separate slots | Trivial — a *longitudinal* offset instead of a lateral one |
| **Rank / Line abreast** | *N* members spread perpendicular to the path, evenly, no randomness | Trivial — swarm with a deterministic offset pattern |
| **Trickle / Drip** | *N* members from one slot, stretched over *more* time than *N* slots | Small — a per-member delay increment inside the slot |
| **Flank** | 2 members on fixed opposite offsets, hugging the path edges | Small — a two-member rank |
| **Escort / Retinue** | 1 large leader with *N* small members holding station around it | **Large** — the retinue tracks the leader's live position rather than its own offset, which is a new movement mode. V2 at the earliest. |

**Explicitly not spawn types**, because the existing model already owns them, and duplicating
them would give the game two ways to say one thing:

- *Split on death* — `Ability` + `SpawnEnemiesAction` (the Warden's egg chain).
- *Cloaked / stealth* — `EnemyMob.Type.INVISIBLE` and the Ghost definition.
- *Armored / shielded* — `PercentResistTrait`, `FlatResistTrait`.
- *Healer, summoner, buffer* — `Ability` with the appropriate trigger.
- *Fast / slow variants* — `EnemyDefinition.baseSpeed`, or a per-level `cloneAndAdjust`.

A spawn type should be rejected if it can be expressed as "a definition with different numbers".
Boss is the borderline case and earns its place because it also changes *size* and *bounty*, and
because authoring `boss warden1` is meaningfully better than registering a parallel
`warden1Big` definition per level.

## Part two: removing the spacer mob

`EnemyMobEmpty` is an inert `EnemyMob` that never ticks, is never a valid target, and draws
nothing. Its only function is to occupy one index so later slots spawn later — and that index
increment lives in `Wave.spawnEnemies`, not in the object.

Measured against the built-in content: **36 waves, 296 spawn slots, 68 of them spacers (23%)**.
The worst single wave is `"c e c e c e c e c"`, 4 inert objects out of 9. Every one of them is
walked by `GameEngine.doTick` each tick, dispatched through the frame builder ~60×/second, and
filtered out by every tower's targeting query every tick.

With `spawnSlot` returning a list, the removal is the `EmptySlot` case returning `List.of()`.

**Deleted:** `EnemyMobEmpty`; `EnemyMobVisitor.visitEmpty`; `EnemyFrameBuilder.visitEmpty`;
`EnemyFactory`'s `"e"` special case; `EnemyFactoryTest.getEnemyBuildsARealEmptyMobForTheSpacerToken`;
`BoardRendererTest.anEmptyEnemyYieldsNoDraw`; the `isEnemy("e")` assertion.

**Kept, unchanged:** `EmptySlot`, `WaveSlot`'s sealed pair, `WaveScript`'s reserved `e` token,
`WaveContent` excluding empties from counts, and every wave string in every level. The feature
ends up living entirely in the parse layer, which is where it belonged.

**Two judgement calls it forces:**

1. `EnemyMobVisitor` drops to one method (`visitDefined`). A one-method visitor over a single
   implementation is vestigial, but collapsing it further means merging
   `EnemyMob`/`AbstractEnemyMob`/`DefinedEnemyMob`, and the test doubles `FakeEnemyMob` and
   `RecordingEnemyMob` implement `EnemyMob`. **Recommendation: keep the one-method visitor**,
   with a comment saying why, and revisit only if a second non-data-driven mob ever appears.
2. `EnemyFactory.getEnemy("e", ...)` stops working. `e` was never an enemy — it is a parse
   token, and `WaveScript` already recognises it independently before any catalog lookup.
   **Recommendation: drop it from `EnemyFactory` entirely.**

## Configuration: how a spawn type is authored

This was the explicitly open part of the request. Five approaches were considered.

The constraint that matters is CLAUDE.md §9: *"`e` is the one reserved token — the spacer —
recognized before any catalog lookup. Every other token resolves against the `EnemyCatalog`
identically whether it names a built-in or a per-level definition; there is no separate syntax
for the two."* Any proposal that makes some ids parse differently from others breaks that.

### A. Sigils — `3*c` swarm, `!warden1` boss

Terse and fits the one-line format. Rejected: the distinction between `3 c` (three slots) and
`3*c` (one slot of three) is one character wide and carries the entire semantic difference, in a
language whose authors are reading a wall of single letters.

### B. Spawn-type keyword modifying the next token — `swarm 3 c`, `boss warden1` ← **recommended**

```
c              one normal spawn
3 c            three normal spawns, three slots
swarm 3 c      ONE slot: a swarm of three
boss warden1   ONE slot: a boss
```

Reads as a sentence, needs no new punctuation, and extends to every future type by adding a
word. It reuses the existing "a count applies to the token immediately following it" rule, with
the spawn type changing what the count *means* — from "how many slots" to "how many members of
this slot". That overloading must be stated explicitly in `td/wave/CLAUDE.md`, because it is the
one genuinely surprising thing about the grammar.

The cost is that `swarm` and `boss` become reserved words. This does **not** break §9's rule so
much as extend its existing precedent: `e` is already recognised before catalog lookup. The
honest form of the rule becomes *"a small, closed set of reserved tokens is recognised before
catalog lookup; every other token resolves against the catalog identically."* To keep that
safe, `EnemyCatalog.register` should **reject an id that collides with a reserved token** with a
`GameStartupException`, rather than letting a level silently shadow the grammar.

### C. Bracket grouping — `{3 c}`

Explicit about the "one slot" semantics, which is the real distinction. Rejected for V1: it
needs a nesting-aware parser for a benefit option B gets from a keyword, and it says *grouping*
without saying *which kind* — a swarm and a column are both "one slot of three".

### D. Structured per-slot authoring — waves as records rather than a token string

Most expressive, and where this ends up if spawn types ever need per-slot parameters
(`swarm(3, spread=0.8)`). Rejected for V1 as premature: it discards the mini-language's real
virtue, which is that a whole wave is legible on one line.

### E. Spawn type as a property of the definition

Rejected outright. The same enemy must be usable both normally and as a swarm, and a swarm of
Armored mobs is a wave-authoring decision, not an enemy's identity.

## Architectural implications

| Area | Change |
|---|---|
| `td.wave` | `WaveSlot` gains `SwarmSlot(definition, count)` and `BossSlot(definition)`. `spawnSlot` returns `List<EnemyMob>`. `WaveScript` gains reserved spawn-type tokens. |
| `WaveContent` | `enemyCount()`, `enemyCount(definition)` and `enemySet()` must count **members, not slots**. See Risks — this gates wave completion. |
| `td.enemy` | `AbstractEnemyMob` gains a lateral (and, for column, longitudinal) offset applied in `updatePosition`. `DefinedEnemyMob` gains a body-scale multiplier. A per-mob speed multiplier — **not** a one-time `setSpeed` (see Risks). |
| `td.economy` | Bounty becomes fractional, or the split is distributed exactly. See Open questions. |
| `td.ui` | **No renderer change.** `EnemyBodyDraw` already carries per-mob `scale`, and x/y are absolute, so off-path members and resized bodies draw correctly as-is. |
| `td.util` | A level-stable `RandomSource` for formation scatter, separate from `GameWorld.random()`. |

## Risks and traps found during research

These are the ones that would silently produce a wrong game rather than a compile error.

**1. An off-path member can leave the board and become untargetable.**
`AbstractEnemyMob.doTick` sets `validTarget = x >= 0 && x <= board.maxX() && y >= 0 && y <= board.maxY()`.
A lateral offset near the board edge pushes a swarm member outside that box, and it silently
stops being a legal target for every tower while still walking to the exit and leaking a life.
The scatter must be clamped to the board, and ideally to a margin inside it.

**2. A boss's speed multiplier will be wiped by the first hit.**
`DefinedEnemyMob.doDamage` recomputes intrinsic speed as
`setSpeed(definition.baseSpeed() * productOfTraitFactors)` on every hit. A boss whose 50% speed
was applied once at construction reverts to full speed the moment anything shoots it. The
multiplier must be a per-mob field folded **into** that recomputation, not a one-time call.

**3. Wave completion breaks if member counts are wrong.**
`GameWorld.startWave` seeds the roster's alive count from `WaveContent.enemyCount()`, and the
wave is declared cleared when that reaches zero. If a swarm slot counts as 1 while spawning 3,
the wave clears two kills early — or never clears, if the counting errs the other way. This is
the single highest-risk line in the feature.

**4. Formation on tight corners.**
A constant lateral offset means the inner and outer members of a rank traverse different real
distances around a curve while sharing one `distanceIntoLap`. On the built-in levels' tighter
corners the inner member can cross the path centre. Acceptable visually at swarm scale, but it
should be a deliberate decision, not a discovery.

**5. The scatter RNG must not be the gameplay RNG.**
Covered above; repeated here because it is easy to "just use `context.random()`" and get scatter
that differs between runs of the same level.

## Open questions

1. **Swarm health.** Does each member keep the wave's `hp`, making a swarm of 3 three times the
   total health for the same bounty? Or is health divided like bounty? Dividing is the more
   defensible default; the request did not say.
2. **Bounty representation.** The request asks for bounty to become a `float`. That works, but
   `price / N` then rounds per kill and a swarm of 3 worth 10 pays 3+3+3 = 9, quietly losing a
   credit. Two alternatives, both exact: distribute the remainder deterministically (the first
   `price % N` members pay one extra), or store credits in hundredths the way health already
   uses `HEALTH_UNITS_PER_POINT`. **Recommendation: the remainder split**, because it needs no
   change to `EconomyState`, `EconomyDelta` or the HUD at all.
3. **Swarm member cap.** Is `swarm 20 c` legal? A cap keeps one slot from dwarfing a whole wave
   and bounds the per-tick cost the spacer removal just recovered.
4. **Does a boss scale with `healthDivisor`?** `EnemyDefinition.healthDivisor` already exists for
   per-definition toughness; a boss multiplier interacts with it and the precedence must be
   stated.
5. **Preview panel.** `PanelEnemy` shows one sprite per distinct definition with a count. Should
   a swarm show 3 small sprites, or one with a "×3" badge? Purely presentational, but it is the
   only place the player learns what is coming.

## V1 Scope

**In:** the `List<EnemyMob>` slot model; `EnemyMobEmpty` removed; Normal, Swarm and Boss; option
B grammar with `swarm` and `boss` reserved; lateral offset with board clamping; per-mob size and
speed multipliers; level-stable scatter; exact bounty splitting.

**Out:** Escort/Retinue; per-slot parameters (`spread`, `jitter`); structured wave authoring;
any new enemy definitions; balance. Elite, Column, Rank, Trickle and Flank are out of V1 but
should be *cheap to add afterwards* — if the V1 design makes any of them expensive, the design
is wrong.

## Phased implementation order

Each phase ends green and is committed on its own (CLAUDE.md §1).

1. **Slot model + spacer removal.** `spawnSlot` returns `List<EnemyMob>`; `EmptySlot` returns
   empty; delete `EnemyMobEmpty` and its visitor method and factory case. No new spawn types, no
   grammar change. Existing waves must be provably unchanged: spawn timing is identical because
   `delay` still increments per slot.
2. **Member counting.** `WaveContent`'s three counting methods count members. Still a no-op for
   existing content, since every slot has one member — but it is the line that makes phase 4
   safe, and it wants its own test.
3. **Boss.** Size and speed multipliers on `DefinedEnemyMob`, folded into the trait speed
   recomputation. `boss` token. No offset work needed, so this proves the grammar and the
   multipliers independently of the formation model.
4. **Swarm.** Lateral offset in `updatePosition` with board clamping; level-stable scatter;
   bounty splitting; `swarm` token.
5. **Docs.** `td/wave/CLAUDE.md` gets the grammar table and the count-overloading rule;
   `td/enemy/CLAUDE.md` gets the offset and multiplier invariants; `README.md` gets a spawn-type
   table. Per CLAUDE.md, a genuinely new invariant is exactly when the root file may change —
   the reserved-token rule in §9 is one.

## Notes for a future session

Verified facts, so they need not be re-derived. Line numbers were accurate at the time of
writing and should be re-checked, but the *claims* are the durable part.

- `td.wave.WaveSlot` is `sealed permits EnemySlot, EmptySlot`. Adding a case produces a compile
  error in `Wave.spawnSlot` and in `WaveContent`'s three switches — use that, do not add a
  `default`.
- `Wave.spawnEnemies` increments `delay` once per slot; `AbstractEnemyMob`'s constructor turns
  it into ticks via `DELAY_TICKS_PER_SLOT * delay / speed`. Keeping the increment per *slot* is
  what preserves existing wave timing.
- `AbstractEnemyMob.updatePosition()` is the *only* place x/y are written, and it reads
  `arcLengthPath.poseAt(distanceIntoLap)`. `PathPose` carries `facingRadians`, so a perpendicular
  offset is `facingRadians + PI/2` — no separate tangent calculation needed.
- `EnemyBodyDraw(palette, x, y, facingRadians, scale, healthFraction)` already has per-mob
  `scale`. **Do not add a size field to the render pipeline**; it is already there.
- `RandomSource` is a `@FunctionalInterface` with `nextDouble()`, plus `seeded(long)` and
  `shared()`. `GameWorld.random()` returns `shared()` unless the harness seeds it.
- `AbstractEnemyMob` has zero non-final, non-volatile fields that cross threads, and carries
  `@ThreadConfined(GAME_LOOP)`. Any new per-mob field is game-loop-owned and needs no
  publication — but `scripts/VerifyRules.java`'s `fields-declare-their-owner` will require the
  annotation on any *new* class holding mutable state.
- The economy is integral end to end: `EnemyDefinition.price` → `AbstractEnemyMob.price` →
  `EconomyDelta.kill(int)`. Health, by contrast, is already stored in hundredths
  (`HEALTH_UNITS_PER_POINT`) — that is the precedent to copy if credits ever need fractions.
- Unrelated bug found while researching this, worth fixing separately: `EnemyRoster.remove()`
  does not remove anything — it decrements a count and fires a callback, because dead mobs stay
  in the list for the death fade. Consequently `getEnemies().length` is the wave's slot count,
  not the alive count, and `BalanceHarness`'s
  `getEnemies().length == 0` exit condition can never fire, so the harness always runs to its
  tick budget. `remove()` should be named `reportDeath()`.
