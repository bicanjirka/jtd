# FIXME — cheap follow-ups from the test-churn cleanup

This file is a session-handoff artifact, distinct from `TODO.md` (which tracks known design/
balance gaps with a stable "Where"/"Approach" shape). It exists so the *next* session doesn't
have to re-run the research that produced it: every item below already has exact file/line
references and a concrete fix, gathered by two research subagents plus manual verification
during the session that finished the three-phase test-churn plan (commits `c287aea`, `b71c6f6`,
`c4fac3a`, `a034f2e` — see `docs/ARCHITECTURE.md` §12 for the full rationale/numbers behind that
plan). Delete an item's section here once it's fixed, the same way `TODO.md` asks entries to be
deleted in the commit that closes them. This file itself can be deleted once every item below is
either fixed or explicitly declined.

Ordered by priority. Do the "Worth doing" items in order; the "Nice-to-have" item only if
touching those files anyway; skip everything in "Investigated and rejected" unless a stated
condition changes — re-researching those would be pure token waste, the answer is already in.

---

## 1. Worth doing

### 1.1 `BoardRendererTest.java` was missed by the Phase 2 fixture migration

**Confidence: high. Risk: low. Effort: ~5 minutes.**

Every other engine-level test file was migrated onto `td.fixtures` in commit `b71c6f6`, but
`src/test/java/td/ui/BoardRendererTest.java` was not in that commit's touched-file list and
still hand-builds its engine/level the old way.

Current code (`src/test/java/td/ui/BoardRendererTest.java:34-43`):

```java
private static GameEngine newEngine() {
    GameEngine engine = new GameEngine(new RecordingGameHost());
    engine.loadLevel(LevelDefinition.unsmoothed("Test Level", "", 5, 5,
            List.of(new Point(0, 2), new Point(4, 2)), List.of(), 100, 5));
    return engine;
}

private static BoardRenderer rendererFor(GameEngine engine, GameWorld context) {
    return new BoardRenderer(context);
}
```

Fix — mirror the pattern every migrated file uses (e.g. `src/test/java/td/GameEngineTest.java`'s
`FakeGameHost.newBoundEngine()` + `LevelFixtures.levelWith(...)`, or, since this file doesn't need
`FakeGameHost`'s host-bound-to-engine wiring, the simpler `WorldFixtures`/`LevelFixtures` pair
other non-`GameEngineTest` files use). Concretely:

```java
private static GameEngine newEngine() {
    GameEngine engine = new GameEngine(new RecordingGameHost());
    engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
    return engine;
}
```

Note `LevelFixtures.levelWith(waves, credits)` builds a 5x5 level on `LevelFixtures.STRAIGHT_PATH`
(`(0,2)`→`(4,2)`) at 5 starting lives — check the rest of the file's tests don't depend on the
level being named exactly `"Test Level"` (`LevelFixtures.levelWith` names it `"Test Level"` too,
so this should be a no-op rename) or on board dimensions other than 5x5 before assuming a
drop-in replacement; if any test asserts on the level's name/description string, that still
matches. After the swap: delete the now-unused `td.level.LevelDefinition` and `td.wave.Point`
imports if nothing else in the file uses them (check with `grep -n "LevelDefinition\.\|Point("`
first), add `import td.fixtures.LevelFixtures;`.

Verify: `mvn verify` must stay at the pre-change test count (548 as of `a034f2e`, may have grown
if other work landed between then and now — check via `mvn test 2>&1 | grep "Tests run:"` on the
tree *before* this change and confirm the count doesn't drop after).

### 1.2 `TODO.md` cites classes and methods that no longer exist

**Confidence: high (all verified directly against current source, not just subagent-reported).
Risk: none (doc-only). Effort: ~15-20 minutes for a careful sweep.**

`TODO.md` predates several renames/refactors and was never swept afterward. Every stale
reference found so far, with the corrected current name/location:

| `TODO.md` line(s) | Stale reference | Current reality |
|---|---|---|
| 80 (`### Ghost health doesn't scale with level`) | `EnemyMobGhost.doInit()` | No such class. Ghost is now `BuiltInEnemies.GHOST`, a data-driven `EnemyDefinition` built via `EnemyDefinition.of("g", "Ghost mob", 100, 4, 1.28f, BodyArchetype.GHOST).withMobType(EnemyMob.Type.INVISIBLE).withHealthDivisor(5f)` (`src/main/java/td/enemy/BuiltInEnemies.java`). The flat, non-level-scaled divisor is applied in `DefinedEnemyMob.withDividedHealth(SpawnParameters, float)` (`src/main/java/td/enemy/DefinedEnemyMob.java:61-64`), called from the constructor at line 47. **The gap itself is still real** — `healthDivisor` is a flat constant (`5f`) with no level term, while body size (`DefinedEnemyMob.bodyScaleFor`, same file, line 67) does scale with `level`. Only the "Where" needs correcting, not the substance. |
| 89, 92 (`### Wave-entry spawn delay is a hardcoded constant`) | `AbstractEnemyMob.doInit()` | No such method exists anywhere — `AbstractEnemyMob`'s own doc comment (`src/main/java/td/enemy/AbstractEnemyMob.java:32`) explicitly notes the two-phase constructor/`doInit` pattern was replaced by a single constructor. The `22.4f` magic constant now lives as `SpawnParameters.DELAY_TICKS_PER_SLOT` (`src/main/java/td/enemy/SpawnParameters.java:27`), a `private static final float`, consumed inside `SpawnParameters.atSlot(...)`/`.of(...)`. **The gap itself is still real** — it's still a single hardcoded constant with no per-wave override. Correct the "Where" to `SpawnParameters.DELAY_TICKS_PER_SLOT` and `SpawnParameters.of(...)`. |
| 116, 121 | `TowerOne`/`TowerTwo`/`TowerThree`/`TowerFour` | Renamed to `SniperTower`/`SplashTower`/`SonarTower`/`PulseTower` (`src/main/java/td/tower/`). The `UpgradePath` constants these lines refer to are `SniperTower.VETERAN`/`.OVERCLOCK`, `SplashTower.SIEGE`/`.CLUSTER_CHARGE`, `SonarTower.OVERCHARGED_ARRAY`/`.MARKSMAN_BEAM`, `PulseTower.OVERLOAD_CORE`/`.EXPANDED_FIELD` — verify exact constant names with `grep -n "static final UpgradePath" src/main/java/td/tower/{Sniper,Splash,Sonar,Pulse}Tower.java` before editing. |
| 130, 135-136 | `TowerMortar`, `TowerSeeker`, `TowerCinder` | Renamed to `MortarTower`, `SeekerTower`, `CinderTower` (`src/main/java/td/tower/`). |
| 237 | `TowerOne.findEnemy()`, "`TowerTwo`'s find methods" | No `findEnemy()` method exists on any tower today. Targeting is composed per-tower inside `doTick` via `td.tower.targeting` pieces (e.g. `SniperTower.doTick`, `src/main/java/td/tower/SniperTower.java:62`, builds candidates through `InRangeTargetQuery.ofType(...)` then a `TargetSelector`) — see the root `CLAUDE.md` §4/`td/tower/CLAUDE.md`'s "Targeting" section for the current model. This whole entry ("Rotating tower sprites") should be reworded around that composed-targeting shape rather than a per-tower `findEnemy()` method — there is no single method to add a "just picked a new target" hook to anymore; it would need to go wherever each tower's `doTick` currently calls its selector. |
| 231-232, 235-236 | `AbstractEnemyMobDirectional`/`AbstractEnemyMobRotor` (as subclasses), `AbstractEnemyMobRotor.getFacingRadians()`/`AbstractEnemyMobDirectional.getFacingRadians()` | Movement is now composed via `MovementBehavior` implementations (`FixedMovement`, `PulseMovement`, `PathDirectionalMovement`, `RotorMovement` — `src/main/java/td/enemy/`), not mob subclasses. The current facing logic is `DefinedEnemyMob.getFacingRadians()` (`src/main/java/td/enemy/DefinedEnemyMob.java:87-94`), a `switch` over `this.definition.movement()`'s sealed type. The "Rotating tower sprites" approach paragraph should point here instead, and note there is no per-tower equivalent to reuse from (see the 237 entry above — towers have no comparable composed-movement model yet, so this entry's "reuse the facing-angle approach" premise needs re-examining, not just a renamed pointer). |

**Suggested approach for whoever picks this up:** re-read each flagged entry's "Where"/"Approach"
in full (not just the table above, which only excerpts the stale token), confirm the gap it
describes is still real against current source (two of six confirmed still-real above; the
other four are tower/method renames where the substance is probably unaffected but wasn't
re-verified line-by-line — do that before editing, in case the underlying feature also changed
shape, not just its name), then fix the "Where" in place. Don't do a mechanical find-replace of
old names to new names blindly — at least one entry (237, the `findEnemy()` one) needs an actual
rewrite of its "Approach" paragraph, not just a renamed pointer, because the mechanism it
describes reusing no longer exists in that shape.

This sweep is exactly the kind of drift `CLAUDE.md` §10's `docs-name-real-types` check already
prevents for `CLAUDE.md` files — `TODO.md` has no equivalent mechanical check today (it isn't
in `scripts/VerifyRules.java`'s `docFiles()` scan), so this is manual-only. Worth considering,
separately, whether `TODO.md` should be added to that check's scope — the `` ` ``-backtick type
names in it (`EnemyMobGhost`, `TowerOne`, etc.) are exactly the pattern that check already
catches in `CLAUDE.md` files.

### 1.3 `td/enemy/CLAUDE.md` overstated which render switches are compiler-enforced — **fixed**

**Confidence: high (verified directly against source). Status: already fixed, in the same
commit that added this entry — kept here as a record, since this is exactly the kind of
behavioral doc-drift `docs-name-real-types` does *not* catch (it only verifies backticked type
names exist, not claims about what the code does).**

The "Adding a new enemy" checklist's step 3 claimed `Java2DFrameRenderer.enemyShape`,
`.colorFor`, and the fade switch in `.paintEnemyFade` were all **not** compiler-enforced
("fall back to a runtime exception / silently skip"). Verified against current source
(`src/main/java/td/ui/Java2DFrameRenderer.java`):

| Method | Switches over | Has `default`? | Actually compiler-enforced? |
|---|---|---|---|
| `EnemyFrameBuilder.paletteFor` | `BodyArchetype` | No | Yes — doc was correct |
| `Java2DFrameRenderer.enemyShape` | `Palette` | Yes, `throw new IllegalStateException` | No — doc was correct |
| `Java2DFrameRenderer.colorFor` | `Palette`, exhaustively covering all 33 constants | **No** | **Yes — doc was wrong** |
| `Java2DFrameRenderer.paintEnemyFade`'s inner switch | `Palette` | Yes, `throw new IllegalStateException` | No — doc was correct |

`colorFor` has no `default` case and exhaustively covers every `Palette` constant, so adding a
new one without a `colorFor` case fails the *build*, not a runtime scan — the doc had it grouped
with the two genuinely-runtime-only switches. Fixed by moving `colorFor` into step 2 (the
compiler-enforced pair) and leaving only `enemyShape` and the fade switch in step 3, with the
`default -> throw` mechanism named explicitly instead of the vaguer "falls back to a runtime
exception / silently skip" phrasing.

---

## 2. Nice-to-have — only if touching these files anyway, not a dedicated pass

### 2.1 Tower upgrade-path `TowerBuff` literals could read as fluent copies

**Confidence: medium (real readability win, zero churn risk either way). Risk: none — purely
cosmetic, doesn't change behavior. Effort: ~10 minutes for all 14, or do them one at a time
opportunistically.**

All 14 `UpgradePath` constants across the 7 tower leaf classes build their `TowerBuff` bonus
positionally. 13 of the 14 use the narrower 4-arg legacy constructor (fine, not a churn risk,
`no-wide-value-literals-in-tests` doesn't apply to `src/main` anyway); **one uses the full 5-arg
canonical form**: `src/main/java/td/tower/SniperTower.java:36`:

```java
"Veteran", 30, new TowerBuff(0.3f, 0.1f, 0f, 0.25f, 0.15f), new KillCountCondition(10));
```

→

```java
"Veteran", 30,
TowerBuff.none().withDamage(0.3f).withRange(0.1f).withBounty(0.25f).withCritChance(0.15f),
new KillCountCondition(10));
```

(This exact buff shape/values are already converted this way in the test that describes it,
`src/test/java/td/tower/upgrade/UpgradePathTest.java` — see commit `c4fac3a` — so the fluent form
is already proven correct for these exact numbers.)

The other 13, for reference if doing all of them in one pass (all currently 4-arg, i.e. already
using the narrower constructor — converting these is pure style, not fixing a wide-literal
problem):

| File:line | Current | Fluent equivalent |
|---|---|---|
| `SniperTower.java:41` | `new TowerBuff(-0.2f, 0f, 0.4f, 0f)` | `TowerBuff.none().withDamage(-0.2f).withFireRate(0.4f)` |
| `SplashTower.java:43` | `new TowerBuff(0.35f, 0f, 0f, 0f)` | `TowerBuff.none().withDamage(0.35f)` |
| `SplashTower.java:48` | `new TowerBuff(0.2f, 0.2f, 0f, 0f)` | `TowerBuff.none().withDamage(0.2f).withRange(0.2f)` |
| `SeekerTower.java:49` | `new TowerBuff(0f, 0f, 0.35f, 0f)` | `TowerBuff.none().withFireRate(0.35f)` |
| `SeekerTower.java:54` | `new TowerBuff(0.3f, 0f, 0f, 0f)` | `TowerBuff.none().withDamage(0.3f)` |
| `PulseTower.java:34` | `new TowerBuff(0.5f, 0f, 0f, 0f)` | `TowerBuff.none().withDamage(0.5f)` |
| `PulseTower.java:39` | `new TowerBuff(0f, 0.3f, 0f, 0f)` | `TowerBuff.none().withRange(0.3f)` |
| `SonarTower.java:55` | `new TowerBuff(0f, 0.2f, 0f, 0f)` | `TowerBuff.none().withRange(0.2f)` |
| `SonarTower.java:60` | `new TowerBuff(0.4f, 0f, 0f, 0f)` | `TowerBuff.none().withDamage(0.4f)` |
| `MortarTower.java:48` | `new TowerBuff(0.4f, 0f, 0f, 0f)` | `TowerBuff.none().withDamage(0.4f)` |
| `MortarTower.java:53` | `new TowerBuff(0f, 0.25f, 0f, 0f)` | `TowerBuff.none().withRange(0.25f)` |
| `CinderTower.java:48` | `new TowerBuff(0.4f, 0f, 0f, 0f)` | `TowerBuff.none().withDamage(0.4f)` |
| `CinderTower.java:53` | `new TowerBuff(0f, 0.3f, 0f, 0f)` | `TowerBuff.none().withRange(0.3f)` |

If doing this, run `mvn verify` after — `spotless:apply` may reflow the fluent chains'
line-wrapping differently than shown above; let it, don't hand-format against the formatter.

---

## 3. Investigated and rejected — do not re-research, re-open only if the stated condition changes

All of these were checked by two research subagents plus manual verification in the session
that produced this file. Re-running this research would duplicate ~370k+374k tokens of
subagent work for the same answer.

- **No wide/telescoping constructors exist outside records.** Checked `AbstractTower` (7 params,
  but only 7 fixed leaf-class call sites, not a scattered-call-site risk), `AbstractEnemyMob`/
  `DefinedEnemyMob` (already narrowed to `SpawnParameters`/`EnemyDefinition` + 2-3 scalars),
  `GameEngine`/`TowerRoster`/`EnemyRoster`/`GameWorld` (1-3 constructor-injected params each).
  Reopen only if a new class is added with 4+ constructor params built from many scattered call
  sites — the record-only scope of `wide-values-have-a-narrow-entry-point` was a deliberate,
  verified choice, not an oversight.
- **Exhaustive switches over sealed types** (`Java2DFrameRenderer`, `EnemyFrameBuilder`,
  `TowerSpriteFrameBuilder`) look repetitive but are the project's own documented,
  compiler-enforced extension point (`td/enemy/CLAUDE.md`'s "Adding a new enemy" checklist).
  Don't collapse into a dispatch table — that trades a compile error for a runtime lookup miss.
- **No magic-number duplication outside tests** — checked for a `SCALE=32`-style repeated
  semantic constant in `src/main`; the few literal `32`s found (`PanelLevelSelect` border
  padding, `PanelTowerSelector.ICON_SIZE`, `PanelEnemy.scale`) are unrelated UI values that
  happen to share a number, not the same quantity duplicated.
- **`EnemyFixtures.spawn(world, mobs...)` retrofit** at ~28 `context.enemies().setEnemies(new
  EnemyMob[]{...})` call sites across 9 tower/upgrade test files. Real duplication, but trades
  one one-liner for a barely-shorter one and isn't what breaks when something upstream changes
  shape. Reopen only if `EnemyMob[]` array construction itself becomes a churn source (e.g. if
  `setEnemies` ever changes signature).
- **Render-pipeline assertions on `frame.towerSprites()`, `sprite.palette()`, `draw.x()/y()`**
  in `BoardRendererTest`/`EnemyFrameBuilderTest` — appropriate, not accidental coupling; these
  tests exist specifically to prove `RenderFrame`'s output shape. No repeated "find the
  CellDraw/EnemyDraw at (x,y)" hand-rolled search pattern was found across these tests to
  extract into a helper.
- **`WaveDefinition`'s ~26 inline literals in `GameEngineTest`** — legitimate per-test content
  (health/price tuned against that specific test's own assertions), the same way
  `BuiltInLevelCatalog`'s wave lists are authored game content, not test-setup boilerplate.
  Forcing these into a shared fixture would hide the exact numbers each test's assertions
  depend on.
- **`GameEngineTest`'s size** (~550 lines, 30+ flat `@Test` methods, no `@Nested` per CLAUDE.md
  §7) — `CLAUDE.md` §7 explicitly frames it as "the integration surface." A split by concern
  (multi-path tests, tower-placement tests, wave/economy tests) was considered and rejected:
  organizational churn with no reduction in future edit cost, and no `@Nested` escape hatch
  exists under this project's test-style rules anyway.
- **No further copy-pasted test doubles or helpers remain** beyond what Phase 2 already
  migrated — a fresh grep for repeated private helper signatures across `src/test` (post-Phase-2,
  pre-`BoardRendererTest`-fix) turned up nothing new.
- **Test naming/arrange-act-assert convention** — spot-checked (not exhaustive)
  `GameEngineTest`, `BoardRendererTest`, `EnemyCatalogTest`; full-sentence names, no `test`/
  `should` prefixes, consistent blank-line AAA separation throughout. No violations found.
- **`Tower`'s 23-method interface and a possible EDT command-queue** are already tracked in
  `TODO.md`'s "Architecture and correctness" section with their own stated trigger conditions
  ("when a consumer is actually hurt by the width," "if deterministic replay becomes a goal").
  Not re-proposed here as new findings — if revisiting, the question is whether the stated
  trigger has now been met, not whether the idea is sound (it already was, per that entry).
