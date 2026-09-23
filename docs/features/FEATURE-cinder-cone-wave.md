# Feature Request: Cinder Tower Cone Wave

**Status: implemented.** `CinderTower` now fires on a real cooldown, each shot is a travelling
`FlameWave` that burns an enemy once its expanding front reaches it, and
`TowerEffectFrameBuilder`/`Java2DFrameRenderer` draw one fading `ConeDraw` per in-flight wave,
exactly as decided below.

## Summary

Give the Cinder tower a real cooldown instead of firing every tick, and turn its cone from a
static, instantaneous wedge into a flame wave that visibly travels outward from the tower over
several frames, fading as it goes. The burn should land on an enemy only once the travelling
wave visually reaches it, not the instant the tower fires. This is a request the user has
already written down once, in their own words, in `TODO.md`'s hand-written notes section (line
17): *"change cinder tower to cast the flame differently - now it always casts the cone. I want
it to cast the cone only when hitting any enemy and the fire rate should be slower and the
'flame wave' should travel in a cone, slowly, as a fire that was spit. Make it configurable so
that it is easy to adjust color in the future. As a wave travel further, it is also less bright
... so that it is visually appealing. The effect is applied once the wave reaches the enemy."*
This document turns that note into the project's structured feature-request format.

## Current state (what exists today)

**`CinderTower` has no cooldown at all.** Its constructor passes a `coolDownMax` of `0`
straight to `super(...)` (`src/main/java/td/tower/CinderTower.java:60-61`), and its class doc
comment states the design outright: *"a continuous flame cone with no cooldown, slowly
reorienting toward the nearest enemy in range and burning everything currently caught in its
wedge, ghosts included"* (`CinderTower.java:18-21`). `doTick` (`CinderTower.java:78-92`) runs
unconditionally on every simulation tick:

1. It builds the in-range enemy list, picks the nearest one, and turns `turretAim` toward it by
   at most `MAX_TURN_RADIANS_PER_TICK` (`CinderTower.java:79-84`).
2. It re-queries `InWedgeTargetQuery(...).and(InRangeTargetQuery.anyType(...))` against the
   turret's *current* heading and `halfWidthRadians`, matching any enemy type — ghosts included,
   unlike every other single-target tower, which restricts to `EnemyMob.Type.NORMAL`
   (`CinderTower.java:86-88`).
3. For every enemy caught, it calls `enemy.applyEffect(Effect.burn(...))`
   (`CinderTower.java:89-91`) — which, per the class doc, *continually refreshes* the burn for
   as long as an enemy stays in the wedge, rather than applying it once per shot.

The gameplay effect is already correctly gated on catching something — an empty wedge is a
no-op that tick. **The visual cone is not gated on anything.** `TowerEffectFrameBuilder
.visitCinderTower` (`src/main/java/td/ui/TowerEffectFrameBuilder.java:144-149`) unconditionally
appends one `ConeDraw` per frame, at a fixed `CINDER_CONE_ALPHA` constant (`0.35f`,
`TowerEffectFrameBuilder.java:38`), reading only the turret's interpolated heading, its current
range and half-width — nothing about time, progress or whether anything was actually hit.
`ConeDraw` itself (`src/main/java/td/ui/render/ConeDraw.java:9-11`) is a flat record —
`palette`, `originX`, `originY`, `headingRadians`, `radius`, `halfWidthRadians`, `alpha` — with
no progress or duration field, and `Java2DFrameRenderer.paintCone`
(`src/main/java/td/ui/Java2DFrameRenderer.java:936-945`) draws it as one static, fully-extended
`Arc2D.PIE` wedge at that one alpha value. There is no animation: the cone is either fully drawn
(something is in range and roughly ahead) or absent, every single frame.

**The codebase already has the pattern this feature needs, just applied to a different draw
kind.** `EffectPulseDraw` (`src/main/java/td/ui/render/EffectPulseDraw.java:10-11`) carries a
target `radius` and a `0..1 progress` rather than a stored current radius/alpha; its backend,
`Java2DFrameRenderer.paintEffectPulse` (`Java2DFrameRenderer.java:783-795`), grows or shrinks
the drawn ring from `progress` (depending on `PulseDirection`) and fades it by
`(1f - progress)` at the same time. `td/ui/CLAUDE.md` documents this as *"one shape for every
one-shot transition ... never a stored current radius/alpha"* — exactly the shape a travelling,
fading cone needs, just applied to a wedge instead of a ring.

**A "fire now, resolve later" mechanic already exists, but only inside `td.projectile`, and
only for a single point target.** `MissileProjectile` (`src/main/java/td/projectile
/MissileProjectile.java`) is constructed with a `TargetImpact` — a callback the firing tower
binds to its own `dealDamage` (`src/main/java/td/projectile/TargetImpact.java:6-8`) — and calls
`impact.onImpact(target)` only once it physically reaches that one target
(`MissileProjectile.java:55-59`). `CannonballProjectile` follows the same shape via
`PointImpact`. Every attack tower except Cinder that deals damage does so either instantly
(`SniperTower`, `SplashTower`, `SonarTower`, `PulseTower`) or through exactly this
fire-then-travel-then-arrive `td.projectile` model (`MortarTower`, `SeekerTower`). **Cinder
today is neither** — it is instant-hit, continuous, and untimed, which is what this feature
changes.

**`CinderTower` doesn't actually override `rateLine`, despite its own comment saying to.**
`td/tower/CLAUDE.md` states the convention: *"a leaf with no reload cadence ... passes `0` for
`coolDownMax` and overrides `rateLine(int)` to describe its cadence some other way"* — `SonarTower`
does this (`SonarTower.java:128`). `CinderTower.java:59`'s comment says *"No cooldown ... see
rateLine"*, but no override exists. Its `getInfoString()`/`getStatusString()`
(`CinderTower.java:106-118`) call `super.getInfoString()`/`super.getStatusString()`, which — per
`AbstractTower.java:357-367` — unconditionally compute `this.rateLine(this.coolDownMax)`
(`AbstractTower.java:353-355`: `"Fire rate: " + TICKS_PER_SECOND / (coolDown + 1) + "/s\n"`).
With `coolDownMax = 0` this prints a real, misleadingly-huge fire-rate number immediately
followed by Cinder's own hand-appended `"No cooldown"` line. This is a pre-existing, unrelated
small wart, not something this feature is required to fix — but a real `coolDownMax` would
naturally make `rateLine`'s printed number correct again, and the now-false `"No cooldown"`
text (`CinderTower.java:109, 117`) would need removing either way once a cooldown exists.

**A second, unrelated Cinder change may land around the same time.** `TODO.md`'s notes also
record (line 18) that Cinder currently aims at invisible enemies by mistake and should be
restricted to visible ones like every other tower — a separate bug, out of scope here (see
Interconnections).

## What this feature adds

- **Cinder fires on a real cooldown**, like every other attack tower, instead of every tick.
- **Each shot is a visible gout of flame that travels outward from the tower over multiple
  frames**, rather than appearing as an instantaneous, fully-extended wedge the moment something
  is in range.
- **The travelling wave grows more transparent the further it has travelled**, so it reads as
  flame dissipating rather than as a static area-of-effect marker.
- **The burn lands on a given enemy only once the visible wave reaches that enemy's position**,
  not the instant the tower fires. This is the core behavior change: today, hit-detection and
  the visual are the same continuous thing, every tick; this feature makes firing a discrete
  event with a travel delay before its effect resolves.
- **Cinder keeps hitting every enemy type in its wedge, ghosts included** — this feature does
  not narrow targeting scope; it changes only when a caught enemy's burn is applied and how
  often the tower fires.
- **Color and the alpha-over-distance falloff are configurable via named constants**, not
  inline magic numbers, so a later pass can retune "how fast it fades" or "what color it is"
  without hunting through the drawing code.
- **Out of scope for this request:** the two existing upgrade paths' balance (`WHITE_FLAME`,
  `DamageDealtCondition`-gated; `WIDE_NOZZLE`, money-gated — `CinderTower.java:47-54`) is not
  being redesigned here, only whatever adjustment falls out of moving from a continuous refresh
  to a discrete per-shot hit (see Constraints and open risks). No new upgrade path is being
  added.

## Interconnections

- **The single biggest architectural question this request raises: does the "fire now, effect
  lands later" bookkeeping for one Cinder shot belong in `td.projectile`, or stay local to
  `CinderTower`?** This is the first tower outside `td.projectile` to need this shape of
  mechanic at all — every other instant-hit tower (`SniperTower`/`SplashTower`/`SonarTower`/
  `PulseTower`) resolves synchronously, and every delayed-hit tower already goes through
  `td.projectile`. The root `CLAUDE.md` §4 already names `td.projectile` as the package that
  "depends on `enemy`/`damage`, never on `tower`" and models exactly "something fired now that
  deals its effect on arrival later." But `Projectile`'s current shape
  (`src/main/java/td/projectile/Projectile.java`) is built around a single travelling *point*
  with one eventual impact (`getX/getY`, `isFinished`, a `TargetImpact`/`PointImpact` called
  once) — a cone is a widening wedge that can reach, and needs to burn, several enemies at
  different distances as its front sweeps past them, not one point arriving at one target. Fitting
  it into `td.projectile` as-is would mean either widening `Projectile`'s contract for an
  area-effect wavefront (nothing today has more than one impact point) or adding a sibling
  abstraction beside it; keeping it local avoids touching that package at all but duplicates,
  inside `CinderTower`, a "fired now, resolves on arrival" bookkeeping shape the project already
  has one home for. **This document intentionally does not settle it** — see Constraints and
  open risks, and Open questions, below.
- **Depends on nothing new in `td.effect`.** `Effect.burn(...)` is unchanged; only *when* it
  gets applied to a given enemy changes, not its own shape.
- **The existing "a burning target doubles every tower's crit chance" rule is unaffected.**
  `AbstractTower.rollCritical` reads `enemy.activeEffectKinds().contains(EffectKind.BURN)`
  (documented in `td/tower/CLAUDE.md`) — this still works exactly the same once an enemy is
  actually burning; only the moment burning starts moves later.
- **Orthogonal to the concurrent invisible-enemies bugfix.** The `TODO.md`-recorded fix (Cinder
  should not aim at invisible enemies) only narrows the candidate list `NearestSelector`/
  `InRangeTargetQuery` draws `turretAim`'s target from — it doesn't touch firing cadence, the
  wedge's hit resolution, or rendering, so it should compose cleanly with this feature
  regardless of which lands first, and `CinderTower.java` may show unrelated changes from it
  landing around the same time as this document.
- **Reuses, rather than duplicates, `EffectPulseDraw`'s progress-based animation pattern** — see
  Current state, above. This is not a new visual language for the render pipeline, just a second
  application of one that already exists.

## Constraints and open risks

- **A wave's "already hit this enemy" semantics aren't specified by the ask and need
  deciding.** Today, an enemy that lingers in the wedge gets its burn continually refreshed,
  tick after tick, for as long as it stays caught (`CinderTower.java`'s class doc, "continually
  refreshing"). Once a shot becomes a single travelling wave, does that wave apply burn to an
  enemy exactly once (the instant its front reaches that enemy), or can the same wave re-hit an
  enemy that remains within its expanding band for several consecutive ticks as the front
  passes through it? The two read very differently in play and neither is dictated by the
  request text.
- **A real `coolDownMax` changes Cinder's effective DPS math against its existing two upgrade
  paths.** `WHITE_FLAME` (`CinderTower.java:47-48`) buys more damage, which today compounds by
  continually refreshing burn on anything that stays in the wedge; `WIDE_NOZZLE`
  (`CinderTower.java:52-53`) buys a wider cone. Both were priced and thresholded
  (`DamageDealtCondition(15000)`, money-only) against the old always-firing model. `TODO.md`
  already carries an "Upgrade-path numbers are unbalanced placeholders" entry and a "New tower
  numbers are unbalanced placeholders" entry for exactly this kind of not-yet-tuned constant —
  this feature's cooldown value belongs in that same bucket: a placeholder `coolDownMax` naming
  the mechanism, not a tuned final number, to be balanced later the same way those entries
  describe.
- **The render/domain split still holds, and is easier here than it might look.** Per root
  `CLAUDE.md` §3, `GameLoop` runs both simulation ticks and frame-building on the same
  `game-loop` thread — `td/ui/CLAUDE.md` confirms `buildFrame` runs there, not on the EDT. A
  travelling-wave's in-flight state is read and written entirely on that one thread either way
  (inside `CinderTower.doTick` and inside `TowerEffectFrameBuilder.visitCinderTower`), so this
  doesn't introduce a new cross-thread publication case beyond what `RenderFrame` already
  handles as a whole — worth confirming during planning, not assuming, since a wrong assumption
  here is exactly the kind of bug root `CLAUDE.md` §3 exists to prevent.
- **The now-false `"No cooldown"` text and the never-overridden `rateLine` (see Current state)
  need addressing once a real cooldown exists**, even though neither is this feature's original
  ask — leaving them would make the tower's own info panel contradict its new behavior.
- **UI uniformity and visual verification still apply.** Root `CLAUDE.md` §8 requires the HUD to
  read as one surface and asks that gameplay/visual changes be verified from an actual
  screenshot via the `run-jtd` skill, not from code or tests alone — a travelling, fading cone is
  exactly the kind of change that can look wrong only in motion.
- **Headless, clock-free provability.** Root `CLAUDE.md` §7 expects `GameEngineTest` to exercise
  new gameplay rules by driving `doTick` with explicit tick numbers — a cooldown gate and a
  delayed-hit resolution both need to be provable that way, without relying on real time or a
  running render loop.

## Decisions made

- Cinder gets a real cooldown; the exact `coolDownMax` value is a placeholder for a later
  balance pass, not decided here (see Constraints, above).
- The flame's colour and its alpha-over-distance falloff must be adjustable through named
  constants/fields, not inline literals.
- Burn application moves from "continuously, every tick an enemy is caught" to "discretely, when
  a travelling wave visually reaches an enemy" — the *whether* of this change is decided; the
  *exact re-hit semantics while an enemy lingers* is not (see Open questions).
- Whether the underlying delayed-hit bookkeeping lives in `td.projectile` or stays local to
  `CinderTower` is explicitly **not** decided here — see Interconnections and Open questions.

## Open questions (resolved)

1. **The in-flight wave bookkeeping stays local to `CinderTower`** (`CinderTower.FlameWave`),
   not `td.projectile` — widening `Projectile`'s single-point-impact contract for one tower's
   one-off widening-wedge shape was judged the wrong direction; `CinderTower` already owned the
   equivalent wedge/turret-aim bookkeeping shape, so this is one more field, not a new pattern.
2. **A wave resolves against each enemy exactly once**, tracked by the wave's own per-wave hit
   set (`FlameWave.alreadyHit`) — not a continuous refresh while an enemy lingers inside the
   expanding band.
3. **Cinder's cooldown plays by the same buffable fire-rate axis every other cooldown tower
   uses** (`coolDownCurrent()`) — no special-casing.
4. **`WHITE_FLAME`/`WIDE_NOZZLE` stay mechanically as they are** — out of scope for this
   request's own delivery; `WHITE_FLAME`'s damage bonus still scales each wave's burn intensity,
   `WIDE_NOZZLE` still widens `halfWidthRadians`, both via the same `onUpgradePathChosen` hook as
   before. Retuning either against the new cooldown-gated cadence is left to a later balance pass.

---

*Implemented. `run-jtd` visual verification (watching a wave actually travel and fade in real
play) is covered as part of this implementation's end-of-plan verification pass.*
