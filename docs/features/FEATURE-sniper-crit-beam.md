# Feature Request: Sniper Crit Beam Color

**Status: implemented.** `AbstractTower.dealDamage` now returns whether the hit landed
critical, `SniperTower` records it (`wasLastShotCritical()`), and
`TowerEffectFrameBuilder.visitSniperTower` colors the beam `Palette.CRIT_SPARK` on a crit and
`Palette.TOWER_SNIPER_BEAM` otherwise, exactly as sketched below.

## Summary

When `SniperTower` lands a critical hit, its beam should read as a stronger hit: drawn in the
same white the critical-hit spark already uses (`Palette.CRIT_SPARK`), instead of the beam's
plain green (`Palette.TOWER_SNIPER_BEAM`). A non-crit hit keeps the beam's current color. Purely
visual — no change to damage, crit chance, or any other tower's rendering.

## Current state (what exists today)

- **The crit roll already happens, but its result never leaves `AbstractTower`.**
  `AbstractTower.dealDamage(EnemyMob, Damage)` (`src/main/java/td/tower/AbstractTower.java:227-245`)
  is `protected void`. Line 232 computes `Damage landed = enemy.doDamage(this.rollCritical(enemy,
  damage))` — `rollCritical` (lines 260-272) is where the chance/multiplier roll actually happens,
  via `Damage.asCritical()`. `landed.critical()` is available on that same local right there, but
  the method only ever reads `landed.amount()` (line 234) before returning nothing. The boolean is
  computed and then discarded every single hit.
- **`Damage` already carries the flag needed.** `Damage.critical()` (`src/main/java/td/damage/Damage.java`)
  is set by `asCritical()` and preserved through `scaledBy`/`cappedAt`, so `landed.critical()` at
  `dealDamage` line 232 is the authoritative, already-resist/cap-adjusted answer to "was this hit a
  crit" — nothing needs to be recomputed, only carried one step further out.
- **`SniperTower.doTick`** (`src/main/java/td/tower/SniperTower.java:71-86`) calls
  `this.dealDamage(this.currentTarget, Damage.physical(this.damageCurrent()))` at line 77 and
  ignores the (currently nonexistent) result. `SniperTower` has no field recording whether its last
  shot crit.
- **`TowerEffectFrameBuilder.visitSniperTower`** (`src/main/java/td/ui/TowerEffectFrameBuilder.java:67-74`)
  draws the beam unconditionally via `new BeamDraw(Palette.TOWER_SNIPER_BEAM, ...)`. It has no
  access to crit state at all — `SniperTower` exposes none.
- **Colors**: `Palette.TOWER_SNIPER_BEAM → Color.GREEN`, `Palette.CRIT_SPARK → Color.WHITE`
  (`src/main/java/td/ui/Java2DFrameRenderer.java:427,442`).
- **`AbstractTower.dealDamage` has seven other call sites**, none of which use its return value
  today (`SplashTower:114`, `SonarTower:99`, `PulseTower:61`, `MortarTower:125`, `SeekerTower:108`,
  `CinderTower:90` via an `Effect` sink lambda, plus `SniperTower:77` itself). No subclass
  overrides `dealDamage`. Widening its return type from `void` to `boolean` is a no-op at every
  existing call site — Java allows a caller to ignore a return value, and a lambda body that is a
  single method call (`CinderTower`'s `d -> this.dealDamage(enemy, d)`) compiles as a statement
  regardless of that method's return type.
- **Threading: both the write and the read already happen on the same thread.** `GameLoop`
  (`src/main/java/td/GameLoop.java`) drives two callbacks, `onTick` and `onRender`, and its class
  doc states plainly: "Both callbacks run on the `game-loop` thread, never on the Event Dispatch
  Thread." `SniperTower.doTick` (where the crit would be recorded) runs inside `onTick`;
  `BoardRenderer.buildFrame` — which constructs `TowerEffectFrameBuilder` and calls
  `visitSniperTower` (`src/main/java/td/ui/BoardRenderer.java:43,60`) — runs inside `onRender`.
  Per `td/ui/CLAUDE.md`'s render pipeline section, `buildFrame` runs on `game-loop`, not the EDT.
  So the write (tick) and the read (frame build) are the *same* thread, sequentially — this is
  unlike `MortarTower.splashRadius`/`SplashTower.spreadRadius`, whose `volatile` fields are written
  on the EDT (a player choosing an upgrade path, via `chooseUpgradePath`) and read on `game-loop`
  during a tick — a genuine cross-thread case. `SniperTower` already carries
  `@ThreadConfined(GAME_LOOP)` covering `coolDown`/`currentTarget`, which are likewise written and
  read only from `onTick`/`onRender`, both on that one thread.

## What this feature adds

- `SniperTower`'s beam is drawn in `Palette.CRIT_SPARK`'s color for the duration it is displaying
  the shot that just landed as a crit, and in `Palette.TOWER_SNIPER_BEAM`'s color otherwise —
  matching today's existing rule for *when* a beam is drawn at all (whenever `currentTarget` is
  non-null), just recoloring it.
- No other tower's beam, splash line, or any other effect draw changes.
- No change to crit chance, the crit multiplier, or any other part of the crit mechanic itself —
  this only exposes an already-computed fact to the renderer.

## Interconnections

- Depends entirely on `docs/features/FEATURE-critical-damage.md`, which is already implemented —
  this request adds no new crit mechanics, only plumbs an existing result one step further.
- Same visual family as that document's own crit-spark marker (`CritSparkDraw`,
  `EnemyFrameBuilder`) and as `docs/features/FEATURE-effect-visuals.md`'s standing bar ("no
  trait or ability ships without an unambiguous on-board visual telling the player it's active") —
  this is the same idea applied to the tower side of a crit rather than the enemy side.

## Constraints and open risks

- **Reusing `Palette.CRIT_SPARK` for a beam couples two currently-independent visuals.**
  `Java2DFrameRenderer.colorFor`'s `CRIT_SPARK` case today governs only the enemy-side spark
  marker; making the beam use the same enum constant means a future change to the spark's color
  changes the beam's color too, silently. The ask is explicit that they should be the same color,
  so this is the requested behavior, not a defect — flagged so planning can decide whether that
  coupling is acceptable or whether a second `Palette` role with the same color value is worth the
  extra enum case instead.
- **Standing UI requirement**: this is a look-and-feel change and must be verified from an actual
  screenshot of the running game (root `CLAUDE.md` §8, `td/ui/CLAUDE.md`), not just from tests —
  landing a real crit in a `run-jtd` session is a genuine grind (per `FEATURE-critical-damage.md`'s
  own Open Questions, a live crit didn't land in the time available during that feature's own
  verification pass), so verifying this may need a seeded/forced-crit debug path or patience.
- **`dealDamage`'s widened return type is a small public-surface change to a method every attack
  tower calls.** It stays behaviorally invisible to every existing caller (see Current state), but
  a reviewer should confirm no future call site starts relying on the old void contract in a way
  that would make a boolean return awkward.

## Decisions made

- The crit beam reuses `Palette.CRIT_SPARK` directly, as asked, rather than introducing a new
  `Palette` role — see the coupling risk above, left for planning to revisit if it matters.
- Scope is `SniperTower` only. No other tower gains crit-colored effects in this request.
- The non-crit beam's existing color (`Palette.TOWER_SNIPER_BEAM`, green) is unchanged.

## Open questions (resolved)

- Sniper-only for now, as scoped. Extending the same treatment to other single-target beam
  towers is left for a future request.
- A seeded-`RandomSource` unit-test case was added instead of relying on `run-jtd` alone:
  `AbstractTowerTest.dealDamageReturnsWhetherTheHitWasCriticalMatchingTheDamageItLanded` and
  `SniperTowerTest.doTickRecordsWhetherTheShotThatJustFiredWasACriticalHit`.

## Shape of the solution

This is a single, narrow mechanism — sketched here since there is really only one real design
question (how the flag gets from the roll to the render), and it is now answered by the threading
fact above.

- **`AbstractTower.dealDamage` widens from `protected void` to `protected boolean`**, returning
  `landed.critical()` — the value already sitting in the local at line 232, just now returned
  instead of dropped. The `if (this.removed) { return; }` guard at line 228-230 returns `false`
  (nothing happened, so nothing was critical). No other line of the method changes.
- **`SniperTower` gains one new field**, e.g. `private boolean lastShotCritical;`, alongside
  `coolDown`/`currentTarget`, under the class's existing `@ThreadConfined(value =
  ThreadConfined.Owner.GAME_LOOP)` — plain, **not `volatile`**, because (per Current state's
  threading finding) both the write (`doTick`, inside `onTick`) and the read (`visitSniperTower`,
  inside `onRender`) happen on the same `game-loop` thread; `volatile` is the fix for a
  cross-thread read, which this isn't. This is a deliberate departure from the
  `MortarTower.splashRadius`/`SplashTower.spreadRadius` precedent the original ask pointed at,
  because those two fields really are cross-thread (EDT write via an upgrade path, `game-loop`
  read via a tick) and this one is not — worth planning double-checking this reasoning rather than
  copying the `volatile` pattern by default.
- **`SniperTower.doTick`** (line 77) becomes `this.lastShotCritical =
  this.dealDamage(this.currentTarget, Damage.physical(this.damageCurrent()));`. A getter, e.g.
  `wasLastShotCritical()` (no `get` prefix on a boolean accessor, matching this codebase's existing
  `isDead()`/`isFiring()`/`isSplashVisible()` naming), exposes it to the render side. The flag is
  only ever read while `currentTarget != null` (the same guard `visitSniperTower` already has for
  drawing a beam at all), so there is no meaningful "stale" state to reason about between shots.
- **`TowerEffectFrameBuilder.visitSniperTower`** picks the beam's `Palette` role from that getter
  instead of hardcoding `Palette.TOWER_SNIPER_BEAM`:
  `tower.wasLastShotCritical() ? Palette.CRIT_SPARK : Palette.TOWER_SNIPER_BEAM`, passed into the
  existing `BeamDraw` construction. Nothing else in that method changes.
- No changes needed to `Damage`, `TowerBuff`, `TowerStats`, `RenderFrame`, or
  `Java2DFrameRenderer` — both `Palette` colors this reuses already exist and are already mapped.

Small enough to land as one phase/commit: the `dealDamage` signature change, the new field and
getter, and the render-side branch, verified together via `run-jtd`.

---

*Implemented. `run-jtd` visual verification (a forced/seeded crit showing a white beam) is
covered as part of this implementation's end-of-plan verification pass.*
