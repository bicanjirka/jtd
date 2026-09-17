# jTD — Tower Defense

A Swing tower-defense game. Java 26, Maven. Runtime deps: SLF4J + Logback. JUnit 5 + AssertJ
are test-scope only.

**This file holds constraints, not history.** [§10](#10-documentation-map) states the rule that
keeps it that way; [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) holds the *why* behind
everything below.

## Commands

```bash
mvn verify                # rules check + tests — the standing check before a commit
mvn test                  # tests only
mvn -q compile            # fast syntax/type check
mvn package               # build target/jTD.jar (main class: td.Main)
java -jar target/jTD.jar  # run the game

mvn test -Dtest=GameEngineTest#placingATowerOnABuildableCellChargesCreditsAndOccupiesTheCell
```

`mvn verify` runs `scripts/VerifyRules.java`, which fails the build on any greppable rule
below. Each such rule states its check. A rule the codebase has adopted but not yet finished
complying with is reported as PENDING with its remaining count rather than failing the build;
its outstanding work is a `TODO.md` entry.

## 1. Working in phases

When a task is planned as multiple phases, **commit after each phase completes**, not at the end.

## 2. The four boundaries

Structural. Breaking one is not a style disagreement.

**2.1 The engine is headless.** `GameEngine`, `GameWorld` and the domain packages own all game
state and input semantics. They never construct a window, touch `Graphics2D`, or require a
display.

> `grep -rn "javax\.swing\|java\.awt" src/main/java/td/{board,cell,damage,economy,effect,enemy,level,projectile,tower,util,wave}`

**2.2 New gameplay logic goes in `GameEngine`/`GameWorld`/the domain packages, never in
`TowerDefense`** — a rule placed there is a rule that cannot be tested. If a change needs
something from the UI, add a `GameHost` method rather than reaching for a Swing type.

**2.3 `GameHost` is the engine's only channel back to the UI.** Three methods today. Widen it
only when the UI genuinely needs a new callback, never speculatively.

**2.4 `td.ui.render` imports no `java.awt`, and `Java2DFrameRenderer` is the only class that
turns a `RenderFrame` into pixels.** `BoardRenderer.buildFrame` describes a frame as immutable,
AWT-free draw commands; a backend turns that into output. `AsciiBoardRenderer` is the second
backend and keeps the seam honest — if a change makes a frame impossible to describe without
AWT, the change is in the wrong place. `Panel*` components may import `java.awt` for layout and
their own previews; board content always goes through the pipeline.

> `grep -rn "import java\.awt" src/main/java/td/ui/render`

## 3. Threading

`GameLoop` runs the simulation on a daemon thread named `game-loop`. Rendering happens on the
Event Dispatch Thread. **Tick code never runs on the EDT.**

**The rule: the `game-loop` thread owns all mutable simulation state; the EDT reads immutable
snapshots, never live domain objects.** A concurrent collection makes the *collection* safe, not
its contents — `CopyOnWriteArrayList` publishes which enemies exist, not where they are.

- Never touch Swing from tick code. Route through a listener the UI observes.
- State crossing the two threads is published as an immutable value through a `volatile` field,
  or guarded. A plain mutable field read from the other thread is a bug — including a `double`
  (non-volatile 64-bit reads may tear, JLS 17.7).
- `EconomyLedger` computes new state inside `synchronized (this)` and fires `economyChanged`
  **outside** it. Never hold the lock while calling out — listeners re-enter it and touch Swing.
- `canPay` is advisory. `doPay` is the atomic check-and-charge and returns `false` if unaffordable.
  Never write `if (canPay(n)) { doPay(n); }`.
- Listener lists and the live tower/enemy/projectile lists are `CopyOnWriteArrayList` on purpose.
- When the loop falls behind it runs **one** tick and resyncs rather than bursting the backlog.
- `GameEngine.doTick` order is fixed: **enemies, then projectiles, then towers.**

> **Current gap:** the render path still reads live domain state from the EDT
> (`BoardRenderer.buildFrame`, `PanelTowerInfo.refreshSelected`) and `GameLoop.stop()` does not
> join its thread. Both are in `TODO.md`. Add no new cross-thread reads meanwhile.

## 4. Domain packages

`board` (scale and cell↔pixel math) · `cell` (squares, buildability) · `damage` · `economy`
(the credits/score/lives algebra + `EconomyLedger`) · `effect` (the shared timed status-effect
primitive — deliberately neutral, not under `tower` or `enemy`) · `enemy` · `level` · `projectile`
(depends on `enemy`/`damage`, never on `tower`) · `tower` (+ `targeting`, `buff`, `upgrade`) ·
`ui` (+ `render`, AWT-free) · `util` (`GameWorld`, `GameHost`) · `wave` (path geometry, smoothing,
wave composition).

**Depend on the narrowest thing that works.** A consumer needing only the enemy list takes
`EnemyRegistry`, not `GameWorld`.

## 5. Code style

**Value types** — data passed around and compared (`WaveDefinition`, `Point`, `EconomyDelta`,
`TowerBuff`, `Damage`, any new DTO-shaped class):

1. Immutable, every field `private final`. Prefer `record`.
2. Named static factories over public constructors, so the call site reads as a sentence:
   `Damage.physical(4)`, `EconomyDelta.kill(bounty)`.
3. Model "nothing" as a value, never `null`. Every abstraction gets an identity — `none()`,
   `all()`, `empty()`. Passing `null` to mean "any" is the bug this prevents.
4. Where two values of a kind combine, give the type an algebra: operation, combinator, identity,
   and an absorber if one exists. See `Damage`, `TowerBuff`, `TargetQuery`.

**Stateful engine/service classes** (`GameEngine`, `GameLoop`, `GameWorld`, `EconomyLedger`, the
rosters) are exempt from rule 1 by nature, and §3 overrides this section wherever they conflict.
Still applies:

5. Constructor injection only; dependencies arrive as final fields.
6. Small classes, one idea each. Small interfaces, one to five methods.
7. Strategy in the class name, role as the noun: `FurthestAlongPathSelector`, `ArcCornerSmoothing`.
8. Compose rather than branch. Filtering and selection are separate swappable pieces
   (`td.tower.targeting`); a new tower composes them instead of hand-rolling a scan.
9. Inherit only to model a closed set of variants, and make the leaves `final`.
10. Streams to transform, `reduce` to combine.
11. **No `instanceof` type-switching.** Branch on domain type through `EnemyMobVisitor`,
    `TowerVisitor`, `ProjectileVisitor`. A `switch` over a *sealed* type is fine — a new case is
    then a compile error, not a silent `default`.

> `grep -rn "instanceof" src/main/java --include=*.java` returns only comments.

**One scoped exception to rule 3:** `td.ui` frame builders return `null` for "no draw command",
18 sites in a per-frame hot path where `Optional` buys nothing. That is the only place `null`
models absence; engine and domain code returns `Optional`.

> `grep -rn "return null" src/main/java --include=*.java` matches nothing outside `td/ui`.
> (PENDING: 2 engine sites remain, see `TODO.md`.)

## 6. Conventions

- **No wildcard imports.** Not stylistic: the project hit a real `java.util.List` / `java.awt.List`
  collision that only compiled because an explicit import shadowed a wildcard.
  > `grep -rn "^import .*\.\*;" src/main/java src/test/java`
- **`this.` prefix on instance field access**, consistently.
- **Names:** types `UpperCamelCase`, constants `UPPER_SNAKE_CASE`, everything else
  `lowerCamelCase`. No lowercase type names, no lowercase `static final`. (PENDING: 3 lowercase
  enums remain, see `TODO.md`.)
- **Fields ordered:** constants, injected/final collaborators, mutable state. A value type has no
  third bucket.
- `@Serial` on `serialVersionUID` in Swing classes.
- **No inline `TODO` comments** — add a `TODO.md` entry instead; close a gap, delete its entry in
  the same commit.
  > `grep -rn "// *TODO" src/main/java`

## 7. Tests

JUnit 5 + AssertJ. `assertThat(...)`, never JUnit's bare assertions.

- Test classes and methods are **package-private**.
- **Method names are full sentences** describing behaviour and its consequence, not the method
  under test: `placingOnAPathCellIsRejectedAndCostsNothing`. No `test` prefix, no `should`, no
  underscores, no `@DisplayName`.
- **Hand-build fakes; no mocking framework.** Interfaces here are one to five methods, so a stub
  is a four-line anonymous class; capture an interaction in a local rather than verifying a mock.
  Don't introduce Mockito.
- **Headless and clock-free.** Drive the engine through its public API and call `doTick(t)` with
  explicit tick numbers. Never rely on the real loop's timing or open a window.
- Arrange / act / assert separated by blank lines — no `// given` comments, no `@Nested`.
- Test doubles live beside the tests they serve, named for their role: `FakeGameHost`,
  `RecordingGameHost`, `RecordingCell`.
- `GameEngineTest` is the integration surface, exercising the same entry points `TowerDefense`'s
  listeners call. New gameplay rules should be provable there.

## 8. UI: one look, and it is ours

The interface must be **clean, uniform and operating-system independent**. Standing requirement,
not a per-change preference.

- Every clickable control shares one visual style: `td.ui.HudButton` / `HudToggleButton`, never a
  bare Swing button styled by hand. A control must not look different because it happens to be a
  `JToggleButton`.
- Panel borders come from `td.ui.Hud`, so panels and controls read as one surface.
  > `grep -rn "createEtchedBorder" src/main/java`
- **Nothing inherits its appearance from the platform look-and-feel.** Controls paint themselves.
  Swing's default chrome differs across Windows, macOS and Metal, and its disabled-text colour has
  repeatedly rendered invisible against this game's black panels.
- Prefer a glyph character (`►`) over drawn artwork for a simple control.
- **Verify UI changes from a screenshot of the running game** (the `run-jtd` skill drives it and
  captures one). Look-and-feel defects are invisible in the code and in the tests.

## 9. Wave mini-language

`WaveScript.parse(tokens, catalog)` turns a space-separated token string into a `WaveContent` —
an ordered, `GameWorld`-free slot list with repeat counts flattened. `Wave`'s constructor then
does only the world-bound instantiation. A count applies to the token immediately following it
and resets to 1 afterward: `"3 s e 4 c"` = three Squares, one spacer, four Circles.

| Token | Enemy |
|-------|-------|
| `c` | Circle |
| `s` | Square |
| `t` | Triangle |
| `g` | Ghost |
| `e` | Empty — a spacer; counts toward spawn timing, not toward the enemy count |
| `warden1` | The Warden boss — the only id in its six-stage chain a wave spawns directly |

`e` is the one reserved token, recognized before any catalog lookup. Every other token resolves
against the `EnemyCatalog` identically whether it names a built-in or a per-level definition.
An unrecognized token is an authoring error and must fail the parse.

> **Current gap:** `parse` currently logs an unrecognized token at `WARN` and defaults it to a
> repeat count of 1, silently producing a wave the author did not write. In `TODO.md`.

## 10. Documentation map

| File | Role |
|---|---|
| **`CLAUDE.md`** | Always loaded. Constraints only. |
| **`docs/ARCHITECTURE.md`** | The why: rationale, history, rejected alternatives. Not auto-loaded. |
| **`docs/features/`** | Design docs for shipped features. Historical record. |
| **`src/main/java/td/<pkg>/CLAUDE.md`** | Per-package invariants, loaded when working there: lifecycle ordering, what must not be "simplified", the checklist for adding a type. Exists for `economy`, `enemy`, `tower`, `ui`, `wave`. |
| **`README.md`** | Human-facing: what the game is, build and run, controls, content tables. |
| **`TODO.md`** | Single source of truth for known gaps; each entry carries a **Where** and an **Approach**. |

**The rule governing this file.** Before adding a line, ask:

1. *Could a reviewer mechanically detect a violation?* If yes, make it a check in
   `scripts/VerifyRules.java` and state the check beside the rule, as above.
2. *Is it a constraint on future change, or something that used to be true?* History — what a
   refactor replaced, why a colour was chosen — belongs in `docs/ARCHITECTURE.md` or the commit
   message. Never here.

**Adding a feature should normally change no line of this file.** If it does, the feature
introduced a genuinely new invariant — exactly when this file should change. When a refactor
moves a class, changes a construction order, renames a collaborator or invalidates a "never do X"
note, update the affected `CLAUDE.md` — root or per-package — in the *same commit*. A stale
package doc is worse than none, precisely because it is loaded and believed.
