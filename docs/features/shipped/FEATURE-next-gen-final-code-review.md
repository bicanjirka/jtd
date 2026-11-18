# Feature Request: The final code review, once the next-gen towers are in code

**Status:** implemented 2026-11-18 (498d2b9f..). What each finding became is under "Outcome".

## Summary

Each tower's rework was written while its own mechanics were still moving, so each carries some noise: constructs
that work but read badly, state passed through a field, one idea written three times. Once all seven towers are
reworked, one review reads them side by side, fixes the noise that repeats across towers and writes the rule for what
it finds, so the next tower doesn't copy it.

## Current state (what exists today)

- Only the Sniper has been scanned (2026-11-14). The findings are below; every other tower waits for the review.
- The first rule it produced is in the root `CLAUDE.md`: a catalogue is built by one factory call listing its entries,
  never `empty()` and a chain of `with`. Seven towers and `OwnedMechanicsTest` still write the chain
  (`MechanicCatalogue.<X>empty().with(...)`), each with a type witness.

### The Sniper's findings

| Smell | Where | Why it is noise |
|---|---|---|
| A chain of copying `with` calls from `empty()`, with a type witness | `SniperTower.MECHANICS`, and the same in the other six towers | The rule in `CLAUDE.md`; `MechanicCatalogue.with` copies the whole map per entry |
| State passed through a field: `landing` is set in `land` and read in `ricochet`, which the mechanics reach through `ShotActions` | `SniperTower` | The order of two calls is the only thing that makes it right |
| `tickNow` stored to be read by `flightFrom`, `traceOf` and `Actions.execute` | `SniperTower` | A tick is a parameter, kept as state |
| Two methods named `land` (a bullet; a shot, a target and stats) | `SniperTower` | One name, two meanings |
| `Actions.ricochet` and `SniperTower.ricochet` share a name and the first only calls the second | `SniperTower` | A pass-through |
| An execution is a billion points of physical damage dealt as a periodic hit so that it can't crit | `SniperTower.Actions.execute`, `EXECUTION_DAMAGE` | A trick standing in for "kill"; the comment says it "takes what health is left", which a billion does not |
| One literal, `new HitScaling.PerFocus(TowerTerm.STEADY_AIM, 0)`, written twice, and one node shape (counted shot that primes a status) written three times | `STEADY_AIM`, `KEEN_EYE`; `FOURTH_SHOT`, `SILVER_ROUND`, `HOLLOW_POINT` | A name or a small helper per repeat |
| `FRENZY` is the payload and `FRENZY_NODE` the node | `SniperTower` | A name made up to dodge a clash, not to say what it is |
| `new Vec2(x.getX(), x.getY())` written four times; `(int)` casts to ask a query | `SniperTower` | A helper that answers where an enemy is |
| `findEnemy` ends in `orElse(null)` and `currentTarget` is a nullable field | `SniperTower` | Absence is `Optional` in new code; `getCurrentTarget()` stays as it is |
| `spec()` builds a record per ready tick and per label read, then reassigns a local after `isPresent()`/`get()` | `SniperTower` | `map` and a field-free pipeline say the same in two lines |
| `SniperSpec` holds an `Optional` field, and has `aim` and `chosenAim` for one thing; `view()` has no caller | `SniperSpec` | Two accessors, one dead |
| `ShotContext` has one component, `target` | `td.tower.sniper` | A wrapper made for a second field that hasn't come |
| Every `SniperShot.withX` repeats the six components | `SniperShot` | Allowed by the record rule, but five copies of the same constructor call |
| `Bullet`, `Hop`, `Flight`, `ShotTrace` and `Execution` are nested in the 530-line tower, and the renderer names them as `SniperTower.X` | `SniperTower` | They belong to the sniper package; the tower file is half records |
| `ownConditions` builds the Steady Aim line by filtering hit bonuses with `focusCounter().filter(equals).isPresent()` | `SniperTower` | The describing belongs where the bonus is |
| `clock.spend(rate / (rate + bonus))` has no comment on why | `SniperTower.fire` | A non-obvious *why*, unstated |
| `landArrived` removes the first of a list while its `landsAt` is due | `SniperTower` | It holds only while bullets land in the order they leave; nothing says so |
| `lastShot` isn't reset in `endFight` while `lastExecution` is | `SniperTower` | The fight-state rule resets what a fight changes; check whether a trace outlives its wave |
| `OwnedMechanics` keeps two parallel lists (`from`, `mechanics`) and `renew` skips repeats with `node != last` | `td.tower.upgrade` | A list of pairs is one list; the identity test is a hack |

## What this feature adds

A review, and the fixes it finds. In this order:

1. **Read every tower for the same smells.** Start from the Sniper's table: each row is a question to ask of the
   other six, and of each tower's package. The ones that repeat are one fix, not seven.
2. **Fix a repeat once, in the shared type**, and move each tower onto it. A smell found in only one tower is fixed in
   that tower.
3. **Turn a lesson into a rule.** Where a smell would come back, the rule goes in `CLAUDE.md` (a constraint, never a
   story), and into Checkstyle where a tool can see it. Where it is a judgment, it goes in `docs/DESIGN.md`.
4. **Delete what the review leaves unused**: an accessor, a record component or a constant nothing reads.
   Among them the looks built ahead of their towers: a `TowerCue` or `LoadedShell` nothing sends, a `TowerLook` no
   tower wears (except `WHITE` and its eagles, kept on purpose), and helpers kept only for the flat towers.
5. **Check the budget and the tests**: `mvn verify`, and `td.PerformanceHarness` if per-tick code moved. A fix is a
   refactor; no expected value changes.

Out of scope: balance, looks and any change a player can see.

## Outcome

| Finding | What became of it |
|---|---|
| Copying `with` chain | The last one (the Sniper's, two tests) is one `of(entry(...))`; `MechanicCatalogue.with` is gone |
| `landing` through a field; `Actions.ricochet` a pass-through | The Sniper makes one `Actions` per landed bullet, which holds it and hops it on |
| `tickNow` kept as state | Four towers kept it; `AbstractTower.doTick` notes it once and calls `act`, `tickNow()` reads it. Projectile hooks are passed their tick, since they run before the towers act (the Mortar's `+ 1`, the Aerie's freeze a tick early). A rule in `td/tower/CLAUDE.md` |
| Two `land` methods | `land(Bullet)` and `strike(Bullet)` |
| A billion points as an execution | Kept: a kill has to go through `dealDamage` to be credited and counted. The constant says why it is large |
| One literal twice, one node shape three times | `perSteadyAimStack`, `everyFourthShotIs` |
| `FRENZY` / `FRENZY_NODE` | `GRANTS_FRENZY` / `FRENZY` |
| `new Vec2(x.getX(), x.getY())` | Fifteen times in five classes: `EnemyTarget.position()` |
| `orElse(null)` and a nullable target | The Sniper's and the Mortar's target are `Optional`; `getCurrentTarget()` stays |
| `spec()`, `SniperSpec`'s `Optional`, `aim`/`chosenAim`, dead `view()` | `SniperSpec(reach, aim)`, built with `map` |
| `ShotContext` | Gone: a mechanic shapes a shot for an enemy |
| `SniperShot.withX` copies | Kept, as the record rule allows |
| Records nested in the tower | The Sniper's, the Burst's and the Scorcher's drawn records live in their packages; a rule in `td/tower/CLAUDE.md` |
| Steady Aim's line filtered by `focusCounter` | One `growsWithSteadyAim`, also behind `keenAtFullFocus` |
| `spend` without a why | A comment |
| `landArrived` pops the head | `Arrivals.takeDue`, shared with the Burst, takes whatever is due; a rule |
| `lastShot` not reset in `endFight` | Kept: both traces fade by their own tick, and a reset would change what is drawn |
| `OwnedMechanics`' parallel lists | One list of pairs; `renew` takes the distinct nodes |
| Looks built ahead | Every `TowerCue` and `LoadedShell` is sent; only `TowerLook.WHITE` goes unworn, on purpose. Dead accessors removed: `glares`, `isHolding`, `buildsDeepToll` |

## Interconnections

- `FEATURE-balance-after-next-gen-towers.md` runs at the same point.
- `docs/DESIGN.md` and `CLAUDE.md` hold the rules the review reads against and extends.
- Known gaps the review finds that it won't fix go in `TODO.md` with **Where** and **Approach**, never in this list.

## Constraints and open risks

- Fixes that touch `MechanicCatalogue`, `OwnedMechanics` or a tower's tree change what seven towers import; a tower
  mid-rework collides with them, which is why this waits for the last tower.
- A catalogue's entries are looked up, never iterated, so its map need not keep an order; `OwnedMechanics` is where
  purchase order lives and it must keep it.
- Several smells above are the same shape in the other towers; counting them first decides whether a shared type is
  worth it or a smaller rule is.

## Decisions made

- The review runs once, after the last tower's rework, and starts from the Sniper's list.
- A rule is written only for a smell that repeated; a one-off is fixed and not legislated.

## Open questions

- ~~A snapshot type per tower for its visible-state getters?~~ They stay queries: the frame is built on the game-loop
  thread that changes them, so no reader needs them correlated across threads.
- ~~A `Vec2`-returning accessor for an enemy's position?~~ Yes: `EnemyTarget.position()`.
- ~~"Kill outright" as a call on the damage path?~~ No: the kill must be credited and counted as any other, which
  `dealDamage` does.
