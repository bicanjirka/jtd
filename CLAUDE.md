# jTD — Tower Defense

A Swing tower-defense game. Java 26, Maven. Runtime deps: SLF4J + Logback. JUnit 5 + AssertJ
are test-scope only.

**This file holds constraints, not history.** [§10](#10-documentation-map) states the rule that
keeps it that way; [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) holds the *why* behind
everything below.

## Commands

```bash
mvn verify                # formatting + rules check + tests — the standing check before a commit
mvn test                  # tests only
mvn -q compile            # fast syntax/type check
mvn package               # build target/jTD.jar (main class: td.Main)
java -jar target/jTD.jar  # run the game
mvn spotless:apply        # fix formatting/import-order violations `mvn verify` reports

mvn test -Dtest=GameEngineTest#placingATowerOnABuildableCellChargesCreditsAndOccupiesTheCell
```

`mvn verify` runs `scripts/VerifyRules.java`, which fails the build on any mechanically
checkable rule below. **A rule that has a check cites it by name**, as a blockquote under the
rule — `> \`no-instanceof\`` — never by restating the check's own regex, which is a second copy
of the logic and drifts from it. `checks-are-documented` verifies the citations both ways: every
check exists in this file, and every name cited here is a real check. **A rule that can be checked must be**
— if a reviewer could detect a violation by reading, a program can, and a rule left as prose
is a rule that drifts: the checks for enum constants and for field ownership were both added
after the tree had been violating them for months with the build reporting OK.

## 1. Working in phases

When a task is planned as multiple phases, **commit after each phase completes**, not at the end.

## 2. The four boundaries

Structural. Breaking one is not a style disagreement.

**2.1 The engine is headless.** `GameEngine`, `GameWorld` and the domain packages own all game
state and input semantics. They never construct a window, touch `Graphics2D`, or require a
display.

> `engine-is-headless`

**2.2 New gameplay logic goes in `GameEngine`/`GameWorld`/the domain packages, never in
`TowerDefense`** — a rule placed there is a rule that cannot be tested. If a change needs
something from the UI, add a `GameHost` method rather than reaching for a Swing type.

**2.3 `GameHost` is the engine's only channel back to the UI.** Widen it only when the UI
genuinely needs a new callback, never speculatively. Its `noOp()` is the real substitute for a
display-less world, so nothing passes `null` and hopes.

**2.4 `td.ui.render` imports no `java.awt`, and `Java2DFrameRenderer` is the only class that
turns a `RenderFrame` into pixels.** `BoardRenderer.buildFrame` describes a frame as immutable,
AWT-free draw commands; a backend turns that into output. If a change makes a frame impossible
to describe without AWT, the change is in the wrong place. `Panel*` components may import
`java.awt` for layout and their own previews; board content always goes through the pipeline.

> `render-has-no-awt`

## 3. Threading

`GameLoop` runs the simulation *and* the frame build on a daemon thread named `game-loop`;
only the painting of an already-built frame happens on the Event Dispatch Thread. **Tick code
never runs on the EDT.**

**The rule: the thread that owns mutable state publishes it; the other thread reads only what
was published.** A concurrent collection makes the *collection* safe, not its contents, and not
the operation either — `clear()` then `addAll()` shows a reader an empty list in between.

**Every class holding mutable state names its owner.** A non-final, non-volatile instance field
obliges its class to carry `td.util.ThreadConfined`, so "which thread owns this?" is answered
once per class instead of left to whoever reads the field next. A field with no honest answer
is not confined and belongs behind one of the two mechanisms below.

> `fields-declare-their-owner` in `scripts/VerifyRules.java`.

State crosses by one of exactly two mechanisms, chosen on whether its fields are correlated:

1. **Correlated fields cross as one immutable snapshot**, built on the owning thread and
   published through a single `volatile`. The board is a `RenderFrame`
   (`TowerDefense.buildAndPublishFrame`); a level is a `LoadedLevel` (`GameWorld.installLevel`);
   a tower's buffed damage/range/cooldown are a `TowerStats`.
2. **Independent scalars may be `volatile` fields** — a kill count, a cell highlight. Not a
   cheaper substitute: `volatile` fixes tearing, not a half-updated object (this tick's `x`
   with last tick's `y`). Use it only where a one-pulse-stale read of one value is correct
   alone. Why, and how to tell them apart: `docs/ARCHITECTURE.md`.

**A correlated set also needs a correlated *read*.** Handing out its parts through separate
accessors undoes the snapshot — that is two reads of the volatile, and a caller wanting both
gets a pair that never coexisted. Give the set one accessor and use it: `GameWorld.level()`,
`GameEngine.waveProgress()`, `EconomyLedger.state()`.

Both directions. Cells are EDT-owned and read by the loop's frame build.

- **Swing is built, shown and mutated on the EDT**, `TowerDefense`'s constructor included —
  `Main` goes through `invokeAndWait`. Never touch Swing from tick code; route through a
  listener the UI observes, or drive it from the EDT render pulse.
- `td.util.Threads` asserts the two that matter (`assertEventDispatchThread`,
  `assertNotEventDispatchThread`), so a violation fails where it happens.
- A plain mutable field read from the other thread is a bug, `double` and `long` especially (non-volatile 64-bit reads
  may tear, JLS 17.7).
- Publish only finished objects: fill a local, then assign the field.
- `GameLoop.stop()` joins **without bound**, so it must not run on the EDT — level teardown
  goes through `TowerDefense.stopLoopThen`, which stops the loop on a lifecycle thread and
  posts the EDT work back. Idempotency is not thread-safety.
- `EconomyLedger` computes inside `synchronized (this)` and fires listeners **outside** it.
- `canPay` is advisory; `doPay` is the atomic check-and-charge. Never
  `if (canPay(n)) { doPay(n); }`.
- Listener and live-entity lists are `CopyOnWriteArrayList` on purpose.
- Falling behind runs **one** tick and resyncs, never a burst.
- `GameEngine.doTick` order is fixed: **enemies, then projectiles, then towers.**
- The tick rate lives once, in `TickRate`. Don't restate 50ms or 20/s anywhere else.

## 4. Domain packages

`board` (scale and cell↔pixel math) · `cell` (squares, buildability) · `damage` · `economy`
(the credits/score/lives algebra + `EconomyLedger`) · `effect` (the shared timed status-effect
primitive — deliberately neutral, not under `tower` or `enemy`) · `enemy` · `level` · `projectile`
(depends on `enemy`/`damage`, never on `tower`) · `tower` (+ `targeting`, `buff`, `upgrade`) ·
`ui` (+ `render`, AWT-free) · `util` (`GameWorld`, `GameHost`) · `wave` (path geometry, smoothing,
wave composition).

**Depend on the narrowest thing that works.** `GameWorld` hands out its collaborators rather
than wrapping them — `world.economy().doPay(n)`, `world.towers().all()` — so a call site names
the capability it uses instead of looking like it uses "the world". A consumer that needs only
one of them takes that type directly (`td.tower.targeting` and `td.projectile` take
`EnemyRegistry`), and a class constructed against `GameWorld` should be reaching for two or
three accessors, not ten. The converse is also a rule: a consumer that genuinely uses most of
the world takes `GameWorld` itself rather than a list of six slices — `BoardRenderer` draws
every part of the board, so naming six collaborators would say less than naming one.

## 5. Code style

**Value types** — data passed around and compared (`WaveDefinition`, `Point`, `EconomyDelta`,
`TowerBuff`, `Damage`, any new DTO-shaped class):

1. Immutable, every field `private final`. Prefer `record`.
2. Named static factories over public constructors, so the call site reads as a sentence:
   `Damage.physical(4)`, `EconomyDelta.kill(bounty)`.
3. Model "nothing" as a value, never `null`. Every abstraction gets an identity — `none()`,
   `all()`, `empty()`. Passing `null` to mean "any" is the bug this prevents. **This binds the
   whole API, not just the `return null` statement the grep can see**: a nullable field handed
   out by a getter, and an `orElse(null)`, are the same rule broken less visibly. Absence in a
   return type is `Optional`; absence in a value is that type's own identity.
4. Where two values of a kind combine, give the type an algebra: operation, combinator, identity,
   and an absorber if one exists. See `Damage`, `TowerBuff`, `TargetQuery`.

**Randomness is injected, never static.** Take a `td.util.RandomSource`; `GameWorld.random()`
is where a tower gets one. `Math.random()` is unseeded and global, so a run cannot be
reproduced — which is what `td.BalanceHarness` needs.

> `no-static-random`

**Stateful engine/service classes** (`GameEngine`, `GameLoop`, `GameWorld`, `EconomyLedger`, the
rosters) are exempt from rule 1 by nature, and §3 overrides this section wherever they conflict.
Still applies:

5. Constructor injection only; dependencies arrive as final fields.
6. Small classes, one idea each. Small interfaces, one to five methods.
7. Strategy in the class name, role as the noun: `FurthestAlongPathSelector`, `ArcCornerSmoothing`.
8. Compose rather than branch. Filtering and selection are separate swappable pieces (`td.tower.targeting`); a new tower
   composes them instead of hand-rolling a scan.
9. Inherit only to model a closed set of variants, and make the leaves `final`.
   9b. **A base class's mutable state is `private`**, reached by a leaf through an accessor. A
   `protected` mutable field means the base can hold no invariant a subclass cannot break.
   9c. **One constructor, no second init step.** Everything an object is born with is assigned by
   its constructor and is `final`, so the ordering is a compile error rather than a convention a
   leaf has to remember. `AbstractTower` and `AbstractEnemyMob` are both built this way.
10. Streams to transform, `reduce` to combine.
11. **No `instanceof` type-switching.** Branch on domain type through `EnemyMobVisitor`,
    `TowerVisitor`, `ProjectileVisitor`. A `switch` over a *sealed* type is fine — a new case is
    then a compile error, not a silent `default`.

> `no-instanceof`

**One scoped exception to rule 3:** `td.ui` frame builders return `null` for "no draw command".
That is the only place `null` models absence; engine and domain code returns `Optional`.

> `no-null-return-in-engine`

## 6. Conventions

- **No wildcard imports.** Not stylistic: the project hit a real `java.util.List` / `java.awt.List`
  collision that only compiled because an explicit import shadowed a wildcard.
  > `no-wildcard-imports`
- **`this.` prefix on instance field access**, consistently.
- **Names:** types `UpperCamelCase`, constants `UPPER_SNAKE_CASE`, everything else
  `lowerCamelCase`. **An enum constant is a constant**, so `Type.SNIPER`, not `Type.first`.
  `serialVersionUID` is the one exemption — the JVM fixes that name.
  > `no-lowercase-type-names`, `no-lowercase-constants` and `enum-constants-upper-snake` in
  > `scripts/VerifyRules.java`. The third exists because the second greps for `static final`,
  > which an enum constant is written without — it reported OK for a long time over fourteen
  > lowercase enum constants.
- **A type's name puts the distinguishing part first and the category noun last**, and names
  what a thing *does* rather than what it looks like: `SniperTower`, not TowerSniper and not
  TowerOne. (Counter-examples go unbackticked on purpose — `docs-name-real-types` requires every
  backticked type name to be a real file, which is what makes the rest of this file checkable.)
  Judgement, not a grep.
- **Fields ordered:** constants, injected/final collaborators, mutable state. A value type has no
  third bucket.
- **Never expose an internal mutable structure.** Return the queries callers actually make —
  `cellAt(x, y)`, `width()`, `forEach` — not the array or collection behind them. An array is
  the worst case: unlike a collection it cannot even be wrapped in an unmodifiable view, so
  every caller gets write access and the owner can hold no invariant. Returning a *fresh
  snapshot* (`EnemyRegistry.getEnemies()`) is fine and is not this. Judgement, not a grep.
- **Error-handling bounds.** Catch `Exception`, never `Throwable` or `Error` — an `Error`
  means the JVM is in trouble and a tidy log line hides it. Never catch an unchecked exception
  as control flow (`NullPointerException`, `ClassCastException`, `IndexOutOfBoundsException`):
  test the condition instead. Catching `NumberFormatException` around a parse is fine — that is
  converting a failure into a domain error, which is the one legitimate shape. A caught
  exception is logged or rethrown, never silently dropped. Content that cannot be loaded throws
  `td.util.GameStartupException`, which `Main` handles once.
  > `no-broad-catch`, `no-unchecked-catch`
- `@Serial` on `serialVersionUID` in Swing classes.
- **No inline `TODO` comments** — add a `TODO.md` entry instead; close a gap, delete its entry in
  the same commit.
  > `no-inline-todo`

## 7. Tests

JUnit 5 + AssertJ. `assertThat(...)`, never JUnit's bare assertions.

> `no-raw-junit-assert`

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
  `RecordingGameHost`, `FakeEnemyMob`, `RecordingEnemyMob`. Shared setup used across packages -
  building a world, a board, a test level - lives in `td.fixtures` instead, and is public.
- `GameEngineTest` is the integration surface, exercising the same entry points `TowerDefense`'s
  listeners call. New gameplay rules should be provable there.

## 8. UI: one look, and it is ours

The interface must be **clean, uniform and operating-system independent**. Standing requirement,
not a per-change preference.

- Every clickable control shares one visual style: `td.ui.HudButton` / `HudToggleButton`, never a
  bare Swing button styled by hand. A control must not look different because it happens to be a
  `JToggleButton`.
- Panel borders come from `td.ui.Hud`, so panels and controls read as one surface.
  > `no-etched-border`
- **Nothing inherits its appearance from the platform look-and-feel.** Controls paint themselves.
  Swing's default chrome differs across Windows, macOS and Metal, and its disabled-text colour has
  repeatedly rendered invisible against this game's black panels.
- Prefer a glyph character (`►`) over drawn artwork for a simple control.
- **Verify UI changes from a screenshot of the running game** (the `run-jtd` skill drives it and
  captures one). Look-and-feel defects are invisible in the code and in the tests.

## 9. Wave mini-language

`WaveScript.parse(tokens, catalog)` turns a space-separated token string into a `WaveContent` —
an ordered, `GameWorld`-free slot list with repeat counts flattened. `Wave` holds that content
and does the world-bound instantiation in `spawn()`, called when the wave starts — never in its
constructor, which is what lets a level install in one write (§3, `LoadedLevel`).

- A count applies to the token immediately following it and resets to 1 afterward:
  `"3 s e 4 c"` = three Squares, one spacer, four Circles. The spacer is a token like any
  other, so a count works on it too — `"4 e"` is four spacers, exactly like `"e e e e"` — there
  is no separate rule for it. Before a spawn-type keyword (below) a count repeats the whole
  shaped slot; immediately after one it sets that slot's member count instead — the same rule,
  applied to whichever kind of token follows.
- A small, closed set of tokens — the `e` spacer plus seven spawn-type keywords naming a
  `SpawnShape` (`WaveSlot`'s companion value describing how many members a slot spawns, and how)
  — is recognized before any catalog lookup. Every other token resolves against the
  `EnemyCatalog` identically whether it names a built-in or a per-level definition; there is no
  separate syntax for the two. `EnemyCatalog.register` rejects an id colliding with a reserved
  token, so a level can never silently shadow the grammar.
- An unrecognized token is an authoring error and **fails the parse** with a
  `GameStartupException`. Whitespace is not a token: blank entries are skipped.

**The list of ids lives in `td/wave/CLAUDE.md`, not here** — it grows with content rather than
with the grammar, and this file changes only when an invariant does.

## 10. Documentation map

| File                                   | Role                                                                                                                                                                |
|----------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`CLAUDE.md`**                        | Always loaded. Constraints only.                                                                                                                                    |
| **`docs/ARCHITECTURE.md`**             | The why: rationale, history, rejected alternatives. Not auto-loaded.                                                                                                |
| **`docs/features/`**                   | One design doc per feature, written as a request before implementation and kept afterwards. A doc here may describe something not yet built; it says so at the top. |
| **`src/main/java/td/<pkg>/CLAUDE.md`** | Per-package invariants and per-type checklists, loaded when working there. `economy`, `enemy`, `level`, `tower`, `ui`, `wave`.                                       |
| **`README.md`**                        | Human-facing: build, run, controls, content tables.                                                                                                                 |
| **`TODO.md`**                          | Known gaps; each entry carries a **Where** and an **Approach**.                                                                                                     |

**The rule governing this file.** Before adding a line, ask:

1. *Could a reviewer detect a violation mechanically?* If yes, make it a check in
   `scripts/VerifyRules.java` and state the check beside the rule. If it needs judgement, say
   so, and don't fake a check.
2. *Is it a constraint, or something that used to be true?* History and rationale belong in
   `docs/ARCHITECTURE.md` or the commit message.
3. *Does it name content rather than structure?* A list of enemies, towers or levels grows
   with the game and belongs in the owning package's doc or `README.md`.
4. *Does it enumerate something the code already enumerates?* A package list, a method count, a
   set of class names — that is a copy of the code, not a constraint on it, and it has to be
   edited every time the code it copies changes. This is what actually drove this file's churn:
   it was the second most-modified file in the repository, and the edits were rarely rule
   changes. Name the type that holds the enumeration and stop.

Every backticked type name in a `CLAUDE.md` must be a real source file — the drift that
survived three manual reviews was a doc citing classes deleted long before.

> `docs-name-real-types` in `scripts/VerifyRules.java`. `docs/ARCHITECTURE.md` is exempt: its
> job is history, so naming a class that no longer exists is correct there.

**The number to watch is unverified rules, not commits touching this file.** A commit count
cannot tell a rule change from a correction from a narrative paragraph, and it punishes paying
down debt. `mvn verify` prints the inventory — how many rules this file states, how many are
enforced, how many are explicitly judgement. A rule that only exists as prose is one that can be
wrong, can drift, and invites a paragraph of explanation beside it; moving one into
`scripts/VerifyRules.java` removes all three at once.

**Adding a feature should normally change no line of this file** — if it does, the feature
introduced a genuinely new invariant, which is exactly when it should. A refactor that moves a
class, changes a construction order or invalidates a "never do X" note updates the affected
`CLAUDE.md`, root or per-package, in the *same commit*. A stale package doc is worse than none,
precisely because it is loaded and believed.
