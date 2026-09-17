# Feature Request: Enemy Spawn Types

**Status: proposed, not implemented.** Written after the architecture audit of 2026-09-17,
against the tree at that point. Every code fact cited below was verified against the source at
the time of writing; check the "Notes for a future session" section at the bottom before
trusting any line number.

## Summary

Today every spawn slot in a wave produces exactly one enemy, at the centre of the path, at its
definition's own size and speed. This feature makes **how a slot spawns** a first-class,
composable choice alongside **what it spawns**:

| Type       | What one slot produces                                                     |
|------------|----------------------------------------------------------------------------|
| **Normal** | one enemy on the path centre — today's behaviour, unchanged                |
| **Boss**   | one enemy at 200% size, 50% speed, double bounty                           |
| **Elite**  | one enemy at 150% size, more health, 1.5× bounty                           |
| **Swarm**  | *N* enemies at 50% size, randomly scattered off-path, bounty split exactly |
| **Line**   | *N* enemies spread evenly across the path's width, abreast                 |
| **Flank**  | two enemies hugging opposite edges of the path                             |
| **Column** | *N* enemies in tight single file, closer than *N* separate slots           |
| **Drip**   | *N* enemies stretched over *more* time than *N* separate slots             |

The unifying idea is that a `WaveSlot` stops meaning "one mob" and starts meaning "a group with
a shape". That single change is also what removes `EnemyMobEmpty`, so the spacer cleanup is not
a bundled chore — it is a prerequisite this feature pays for anyway. See **Part two**.

Those eight are not eight implementations. They are **presets over three independent
mechanisms** — per-mob multipliers, lateral offsets, and per-member spawn delay — which is what
makes adding the last five nearly free once the first three exist. See **Three mechanisms**.

This is deliberately *not* a second way to express what enemies already do. Traits and abilities (`td.enemy.Trait`,
`td.enemy.Ability`) own behaviour — resistance, hurt-speed, invisibility,
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
2. **`EnemyBodyDraw` already carries a per-mob `scale`** field. Drawing a mob at 50% or 200% needs *no renderer change
   at all*.
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

The slot's index still increments once per slot regardless of how many mobs it produced, so **spawn timing for existing
waves is bit-identical**.

`WaveSlot` does **not** grow a case per spawn type. `EnemySlot` gains a second component:

```java
record EnemySlot(EnemyDefinition definition, SpawnShape shape) implements WaveSlot {
}
```

`WaveSlot` stays the sealed pair it is today, so `WaveContent`'s three switches are untouched by
the spawn types themselves. A spawn type is a **value**, not a variant — the same call the
codebase already makes for `TowerBuff`, `Damage` and `TargetQuery` (CLAUDE.md §5 rules 3 and 4):
one immutable type with named static factories and an identity.

```java
SpawnShape.normal()        // the identity: one member, no offsets, no multipliers
SpawnShape.

boss()
SpawnShape.

elite()
SpawnShape.

swarm(int members)
SpawnShape.

line(int members)
SpawnShape.

flank()
SpawnShape.

column(int members)
SpawnShape.

drip(int members)
```

This was originally proposed as one sealed case per type. Adding the five extra types is what
exposed that as wrong: seven variants would have duplicated the lateral-offset logic across
three of them and the delay logic across two. Seven *values* over three mechanisms do not.

### Three mechanisms

Every spawn type is some combination of exactly three things. Nothing else is needed.

**1. Per-mob multipliers** — size, speed, health, bounty share.
Boss and Elite are only this. Swarm uses the size and bounty knobs. The multipliers must be **per-mob fields folded into
the existing recomputations**, not one-time writes — see Risk 2.

**2. Lateral offset** — a fixed perpendicular displacement from the path centre, held for the
mob's whole run so the formation follows the path around corners.
Swarm scatters randomly within the slot footprint; Line spaces evenly across it; Flank is two
members at opposite maximum offsets. Computed from `PathPose.facingRadians() + PI/2`, so no new
tangent maths is needed. Must be clamped to the board — see Risk 1.

**3. Per-member spawn delay** — a fractional slot-delay added to each member's countdown.
This is the mechanism Column and Drip share, differing only in magnitude:

```
3 c           three slots      members at 0, 1, 2 slot-delays
column 3 c    one slot         members at 0, 0.3, 0.6   (tighter than separate slots)
drip 3 c      one slot         members at 0, 2, 4       (looser than separate slots)
swarm 3 c     one slot         members at 0, 0, 0       (simultaneous)
```

Column was originally described as a *longitudinal* position offset. That is wrong and the code
says so: `ArcLengthPath.poseAt` clamps its argument to `[0, totalLength]`, so trailing members
offset backwards from the path start would all pile up on the first waypoint. Delay is the
correct axis, and it makes Column and Drip one implementation.

**Consequence for the constructor:** `AbstractEnemyMob` currently takes `delay` as a *slot
index* and converts it with `DELAY_TICKS_PER_SLOT * delay / speed`. Fractional spacing needs it
to take a **tick count** that the slot has already computed. That conversion moves out of the
mob and into the spawn, which is where the slot's shape is known anyway.

### The eight shapes, by mechanism

| Shape      | Members | Multipliers                     | Lateral           | Delay              |
|------------|---------|---------------------------------|-------------------|--------------------|
| **Normal** | 1       | —                               | —                 | —                  |
| **Boss**   | 1       | 200% size, 50% speed, 2× bounty | —                 | —                  |
| **Elite**  | 1       | 150% size, +health, 1.5× bounty | —                 | —                  |
| **Swarm**  | *N*     | 50% size, bounty split          | random, seeded    | —                  |
| **Line**   | *N*     | —                               | even across width | —                  |
| **Flank**  | 2       | —                               | ± maximum         | —                  |
| **Column** | *N*     | —                               | —                 | tight (sub-slot)   |
| **Drip**   | *N*     | —                               | —                 | loose (super-slot) |

Every cell that is not "—" is one of the three mechanisms above. There is no eighth thing.

### Normal spawn

Unchanged, and it stays the default with no token: a bare `c` is a normal spawn.

### Swarm spawn

One slot, *N* members of the same definition, each:

- drawn at **50%** of its normal body scale;
- placed at a fixed **lateral (perpendicular) offset** from the path centre, scattered evenly
  within the slot's footprint, and holding that offset for its entire run so the formation
  follows the path around corners rather than smearing;
- worth an **exact share** of the definition's bounty — `price / N`, with the remainder handed
  to the first `price % N` members, so the swarm always sums to exactly one normal spawn's
  bounty and no credit is lost to rounding. See Decisions made.

Health is an open question (see below): the same `hp` per member makes a swarm of 3 three times
as durable as a normal spawn for the same bounty.

The scatter must be **stable across playthroughs**: the same level, wave and slot must produce
the same formation every time. That means a `RandomSource.seeded(...)` derived from level
identity + wave index + slot index — explicitly **not** `GameWorld.random()`, which is shared,
unseeded, and consumed by tower targeting, so drawing from it would make a formation depend on
how many towers happened to fire first.

### Boss spawn

One slot, one member, at **200%** body scale, **50%** speed, paying **2×** bounty. Mechanism 1
only — no offsets, no delay — which is why it is the first shape to build.

#### What each one is for

A spawn type earns its place by changing what the *player* has to do, not by looking different.
Each of these is a lever on a different weakness in a defence.

**Elite.** The middle gear between a normal spawn and a boss: one tough unit that
shows up mid-wave and has to be focused down. It exists so a wave author can raise pressure
without the ceremony of a boss, and without registering a parallel `xyzBig` definition on every
level that wants one. It asks the player: *do you own any single-target damage at all?* A
defence built entirely on `SplashTower` and `PulseTower` handles crowds beautifully and stalls
completely on one fat unit.

**Column.** A conga line — the same *N* enemies as `N c`, but packed far tighter than
one slot-delay apart. This is the splash lever. A tight column is the best thing that ever
happens to `SplashTower` and `MortarTower`, and the worst thing that happens to a defence of
single-target snipers, which have to chew through it one reload at a time. It is built from
per-member *delay*, not position — see Three mechanisms for why position cannot work.

**Line.** A wall advancing side by side, evenly spaced across the path's width,
deterministic rather than scattered. Where a column tests splash, a line tests **coverage**:
`CinderTower`'s cone and `SonarTower`'s sweep catch a whole line at once, while a single-target
tower picks off one member per reload. It is also the shape that stresses the board-edge clamp
hardest, because a line is deliberately as wide as it is allowed to be — see Risk 1.

**Drip.** The exact inverse of a swarm. A swarm compresses *N* members into one moment
in space; a drip stretches them over *more* time than *N* separate slots would take. It
denies the player the satisfying one-splash clear and tests sustained damage instead of burst,
which is a genuinely different question to ask of a defence. It is also a pacing tool: a drip
at the tail of a wave keeps light pressure on exactly while the player is spending their bounty
and rebuilding.

**Flank.** Two members hugging opposite edges of the path. Towers in this game sit on cells *beside* the path, so a
tower's range circle always covers one side of the path better than the
other — a flank is what makes that placement asymmetry cost something. Mechanically it is a
two-member line at maximum offset, so it is free once Line exists.

**Escort / Retinue — the one deliberately left out.** A leader with bodyguards holding station
around it. This is the one that does not fit the model, and the reason is worth recording: every other type computes a
member's
position as `pathPose(distance) + fixedOffset`. A retinue computes it as
`leaderPosition + offset`, so a member needs a live reference to another mob, and the design has
to answer what happens when the leader dies first — scatter, hold formation, or revert to
normal. That is a new movement mode and the first inter-mob dependency in the enemy model, not
a parameter. **Do not design V1 to accommodate it**; a composite of boss + swarm with a coupling
is better built once, deliberately, than half-anticipated everywhere.

#### The boundary against `MovementBehavior`

One shape that looks like a spawn type is not one: a **zigzag or weaving formation**, where a
member's lateral offset varies as it advances. A spawn type sets a member's offset *once*; an
offset that changes over time is movement, and `td.enemy.MovementBehavior` already owns that
axis (`FixedMovement`, `RotorMovement`, `PulseMovement`, `PathDirectionalMovement`). The test:
**if the formation is still the same shape one minute later, it is a spawn type; if it breathes,
it is a `MovementBehavior`.**

Similarly, "nothing for a while, then everything at once" needs no new type — that is spacers
followed by a swarm, and the grammar already expresses it.

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
   token, and `WaveScript` already recognises it independently before any catalog lookup. **Recommendation: drop it from
   `EnemyFactory` entirely.**

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

### B. Spawn-type keyword modifying the next token ← **recommended**

```
c                one normal spawn
3 c              three normal spawns, three slots
boss warden1     ONE slot: a boss
3 boss warden1   THREE boss slots
swarm 3 c        ONE slot: a swarm of three members
3 swarm 4 c      three swarm slots, four members each
```

Reads as a sentence, needs no new punctuation, and extends by adding a word.

**The counting rule stays literally what it is today** — *a count applies to the token
immediately following it* — and that is what makes the two positions unambiguous:

| Count sits before…                             | It means                                            |
|------------------------------------------------|-----------------------------------------------------|
| an enemy id (`3 c`)                            | repeat the **slot** three times — today's behaviour |
| a spawn-type token (`3 swarm 4 c`)             | repeat the whole **shaped slot** three times        |
| an id *after* a spawn-type token (`swarm 4 c`) | how many **members** that one slot holds            |

A count is required after `swarm`, `line`, `column` and `drip`, and rejected after `boss`,
`elite` and `flank`, whose member count is fixed by the shape. Both failures are authoring
errors and fail the parse with a `GameStartupException`, like any unrecognised token.

The cost is that the reserved set grows from one token to eight:

```
e  boss  elite  swarm  line  flank  column  drip
```

This does **not** break §9's rule so much as extend its existing precedent — `e` is already
recognised before catalog lookup — but at eight tokens the honest form of the rule has to be
written down: *"a small, closed set of reserved tokens is recognised before catalog lookup;
every other token resolves against the catalog identically, whether built-in or per-level."*
`EnemyCatalog.register` must **reject an id colliding with a reserved token** with a
`GameStartupException`, rather than letting a level silently shadow the grammar. At one reserved
token that guard was optional; at eight it is not.

**Reconsidered at eight tokens, and kept.** A sigil prefix (`@swarm 3 c`) would make collision
impossible rather than merely detected. It was rejected because the grammar's whole character is
bare and terse — `3 s e 4 c` — and one rejecting check in `register` buys the same safety
without the noise. If the reserved set ever needs to grow much past eight, revisit this.

### C. Bracket grouping — `{3 c}`

Explicit about the "one slot" semantics, which is the real distinction. Rejected for V1: it
needs a nesting-aware parser for a benefit option B gets from a keyword, and it says *grouping*
without saying *which kind* — a swarm and a column are both "one slot of three".

### D. Structured per-slot authoring — waves as records rather than a token string

Most expressive, and where this ends up if spawn types ever need per-slot parameters (`swarm(3, spread=0.8)`). Rejected
for V1 as premature: it discards the mini-language's real
virtue, which is that a whole wave is legible on one line.

### E. Spawn type as a property of the definition

Rejected outright. The same enemy must be usable both normally and as a swarm, and a swarm of
Armored mobs is a wave-authoring decision, not an enemy's identity.

## Architectural implications

| Area          | Change                                                                                                                                                                                                                                                                                                |
|---------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `td.wave`     | New value type `SpawnShape` with named factories. `EnemySlot` gains a `shape` component; `WaveSlot` stays the sealed pair it already is. `spawnSlot` returns `List<EnemyMob>` and computes each member's tick delay. `WaveScript` gains seven reserved spawn-type tokens and the two count positions. |
| `WaveContent` | `enemyCount()`, `enemyCount(definition)` and `enemySet()` must count **members, not slots**. See Risks — this gates wave completion.                                                                                                                                                                  |
| `td.enemy`    | `AbstractEnemyMob` gains a lateral offset applied in `updatePosition`, and takes its spawn countdown as **ticks** rather than a slot index. `DefinedEnemyMob` gains body-scale and speed multipliers — the speed one folded into the trait recomputation, **not** a one-time `setSpeed` (see Risks).  |
| `td.economy`  | Bounty becomes fractional, or the split is distributed exactly. See Open questions.                                                                                                                                                                                                                   |
| `td.ui`       | **No renderer change.** `EnemyBodyDraw` already carries per-mob `scale`, and x/y are absolute, so off-path members and resized bodies draw correctly as-is.                                                                                                                                           |
| `td.util`     | A level-stable `RandomSource` for formation scatter, separate from `GameWorld.random()`.                                                                                                                                                                                                              |

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
A constant lateral offset means the inner and outer members of a line traverse different real
distances around a curve while sharing one `distanceIntoLap`. On the built-in levels' tighter
corners the inner member can cross the path centre. Acceptable visually at swarm scale, but it
should be a deliberate decision, not a discovery.

**5. A column cannot be built from a negative path distance.**
`ArcLengthPath.poseAt` clamps its argument into `[0, totalLength]`. A trailing column member
offset backwards from the path start does not appear behind the leader — it lands on the first
waypoint, on top of everything else that has not moved yet. Column must be built from delay.

**6. Fractional spacing needs the delay to leave the mob.**
`AbstractEnemyMob` takes `delay` as a slot index and converts it internally. Column and Drip both
need sub- and super-slot spacing, so the conversion has to happen in the spawn, where the shape
is known, and the constructor has to take ticks. This is a small signature change with a wide
blast radius — every construction site and several tests pass a slot index today.

**7. The scatter RNG must not be the gameplay RNG.**
Covered above; repeated here because it is easy to "just use `context.random()`" and get scatter
that differs between runs of the same level.

## Decisions made

**Swarm bounty divides exactly, by distributing the remainder — bounty stays `int`.**
The original request asked for bounty to become a `float`. It would work, but `price / N` then
rounds per kill, and a swarm of 3 worth 10 pays 3+3+3 = 9 — quietly losing a credit, with the
loss growing as swarms get larger. Instead the slot splits the bounty exactly at spawn time: the
first `price % N` members are worth one credit more than the rest, so the members always sum to
the definition's own `price`.

Which member gets the extra credit must be decided at spawn and stored per mob, not computed at
death — otherwise kill order changes the payout. This keeps `EnemyDefinition.price`,
`AbstractEnemyMob.price`, `EconomyDelta.kill(int)`, `EconomyState` and the HUD entirely
untouched; the whole change is arithmetic inside the swarm slot's spawn.

## Open questions

1. **Swarm health.** Does each member keep the wave's `hp`, making a swarm of 3 three times the
   total health for the same bounty? Or is health divided like bounty? Dividing is the more
   defensible default; the request did not say.
2. **Swarm member cap.** Is `swarm 20 c` legal? A cap keeps one slot from dwarfing a whole wave
   and bounds the per-tick cost the spacer removal just recovered.
3. **Does a boss scale with `healthDivisor`?** `EnemyDefinition.healthDivisor` already exists for
   per-definition toughness; a boss multiplier interacts with it and the precedence must be
   stated.
4. **Preview panel.** `PanelEnemy` shows one sprite per distinct definition with a count. Should
   a swarm show 3 small sprites, or one with a "×3" badge? Purely presentational, but it is the
   only place the player learns what is coming.

## V1 Scope

**In:** the `List<EnemyMob>` slot model; `EnemyMobEmpty` removed; the `SpawnShape` value type
and all eight shapes — Normal, Boss, Elite, Swarm, Line, Flank, Column, Drip; option B grammar
with seven reserved spawn-type tokens and the collision guard in `EnemyCatalog.register`; the
three mechanisms (multipliers, lateral offset with board clamping, per-member tick delay);
level-stable scatter; exact bounty splitting.

**Out:** Escort/Retinue, which needs a movement mode the other seven do not; per-slot parameter
tuning (`spread`, `jitter`, explicit spacing) — the shapes ship with fixed magnitudes and a
balance pass can make them configurable later; structured wave authoring; any new enemy
definitions; balance itself.

The eight are in together because they are three mechanisms, not eight implementations. Shipping
Swarm without Line and Flank would mean building the lateral-offset mechanism and then using one
third of it.

## Phased implementation order

Each phase ends green and is committed on its own (CLAUDE.md §1).

1. **Slot model + spacer removal.** `spawnSlot` returns `List<EnemyMob>`; `EmptySlot` returns
   empty; delete `EnemyMobEmpty` and its visitor method and factory case. No new spawn types, no
   grammar change. Existing waves must be provably unchanged: spawn timing is identical because
   `delay` still increments per slot.
2. **Member counting.** `WaveContent`'s three counting methods count members. Still a no-op for
   existing content, since every slot has one member — but it is the line that makes phase 4
   safe, and it wants its own test.
3. **`SpawnShape` + mechanism 1 (multipliers): Boss and Elite.** The value type with its
   `normal()` identity, the size/speed/health/bounty multipliers folded into
   `DefinedEnemyMob`'s existing recomputations, and the `boss`/`elite` tokens. No offsets and no
   delay work, so this proves the grammar, the shape value and the multipliers in isolation.
4. **Mechanism 3 (per-member delay): Column and Drip.** Move the slot-index-to-ticks conversion
   out of `AbstractEnemyMob` and into the spawn, then add the two spacing presets. Done before
   the offsets because it touches a constructor signature and is better landed on its own.
5. **Mechanism 2 (lateral offset): Swarm, Line and Flank.** The offset in `updatePosition` with
   board clamping, the level-stable seeded scatter for Swarm, the even spread for Line, the
   two-member maximum for Flank, and exact bounty splitting.
6. **Docs.** `td/wave/CLAUDE.md` gets the grammar table and the count-overloading rule;
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
  `EconomyDelta.kill(int)`. Health, by contrast, is already stored in hundredths (`HEALTH_UNITS_PER_POINT`) — that is
  the precedent to copy if credits ever need fractions.
- Unrelated bug found while researching this, worth fixing separately: `EnemyRoster.remove()`
  does not remove anything — it decrements a count and fires a callback, because dead mobs stay
  in the list for the death fade. Consequently `getEnemies().length` is the wave's slot count,
  not the alive count, and `BalanceHarness`'s
  `getEnemies().length == 0` exit condition can never fire, so the harness always runs to its
  tick budget. `remove()` should be named `reportDeath()`.
