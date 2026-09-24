# Notes written by hand, to be processed later - write nicer detailed TODO entries from it

I would like to add feature ideas to this todo as well to have all possible work idead at one place.

# Known gaps and future work

Extracted from inline `TODO` comments (and one unmarked-but-real gap) found throughout the codebase during a
documentation cleanup pass. Each item below replaces the original comment; the source no longer carries these notes, so
this file is the single place to look for outstanding design/feature gaps.

## Feature request implementation order

Every feature request has shipped (see each doc's own status line). `FEATURE-tower-
specialization-abilities.md` is the one partial exception: its [F]-tagged content is built, its
[S]-tagged stub nodes wait on the primitives listed under "Tower specialization primitives"
below.

Implemented, for reference: `FEATURE-enemy-spawn-types.md`, `FEATURE-multiple-enemy-paths.md`,
`FEATURE-playtesting-and-balance-tooling.md`, `FEATURE-enemy-rank-system.md`,
`FEATURE-effect-visuals.md`, `FEATURE-tower-upgrades.md`, `FEATURE-critical-damage.md`,
`FEATURE-damage-types-and-projectiles.md`, `FEATURE-enemy-traits-and-effects.md`,
`FEATURE-sniper-crit-beam.md`, `FEATURE-wave-preview-cleanup.md`, `FEATURE-freeze-visual.md`,
`FEATURE-effect-diminishing-returns.md`, `FEATURE-cinder-cone-wave.md`,
`FEATURE-tower-upgrade-trees.md`.

## Architecture and correctness

Findings from the architecture audits of 2026-09-17, highest-severity first. The threading
group and the external audit's critical/high/moderate findings have landed; what remains is
listed below.

### No performance budget, because nothing has been measured

`CLAUDE.md` states no allocation or frame-time budget, and deliberately does not invent one: a
number picked without measuring is worse than none. What is known is only shape, not size - the
render pulse builds a whole `RenderFrame` (every cell, enemy, tower, projectile and path marker
as records) sixty times a second on the `game-loop` thread, and `EnemyRegistry.getEnemies()`
copies its array on every call, several times per frame plus once per tower per tick.

- **Where:** `td.TowerDefense.buildAndPublishFrame`, `td.ui.BoardRenderer.buildFrame`,
  `td.enemy.EnemyRoster.getEnemies`.
- **Approach:** measure first - p99 frame-build time and allocation per frame on the largest
  built-in level with a full board of towers, which `td.BalanceHarness` is already the right
  harness for. Set a budget from the measurement, then state it in `CLAUDE.md` with the command
  that checks it. Do not state a budget before there is a number behind it.

### A command queue would make the simulation a true single writer

Not a defect - an option, recorded with the condition that would make it worth taking.

Every mutation originating on the EDT (placing or selling a tower, choosing an upgrade path,
loading a level, requesting a wave) currently reaches the simulation by one of three ad-hoc
routes: a volatile flag `doTick` consumes (`requestNextWave`), direct mutation under a stopped
loop (`loadLevel`), or direct mutation while the loop runs (tower placement). Each is
individually safe, but there are three of them, and the third is why `AbstractTower`,
`CellNormal` and `GameWorld` all carry publication rules of their own.

The second route is now genuinely guaranteed rather than best-effort: `GameLoop.stop()` joins
without bound and `TowerDefense.stopLoopThen` runs it off the EDT, so `loadLevel` provably has
no tick in flight. The third is the one a queue would still replace.

A single `ConcurrentLinkedQueue<Runnable>` drained at the top of `doTick` would replace all
three: the simulation thread becomes the only writer, most of the volatile markers become
unnecessary, and every mutation lands on a deterministic tick boundary.

- **Where:** `td.GameEngine.doTick`, `td.TowerPlacement`, `td.TowerDefense`'s listeners.
- **Why not yet:** buying a tower deducts credits and reports success synchronously, and the
  UI uses that answer immediately. Through a queue, click feedback waits for the next tick -
  up to 50ms at normal speed, and unbounded while paused, where the queue never drains. That
  needs optimistic UI or a second synchronous path for purchases, which puts two mechanisms
  back.
- **The trigger:** deterministic replay. A recorded command log is what would let
  `td.BalanceHarness` produce runs that are comparable to each other and reproducible across
  machines. If that becomes a goal, this stops being indirection and starts being the feature
    - do it then, and not before.

### `Tower` is a twenty-three-method interface

`CLAUDE.md`'s code style asks for interfaces of one to five methods. `Tower` has twenty-three:
identity, position, rendering, selection, economy, damage accounting, upgrade paths and buff
contribution, all on one type. It is what every consumer depends on, so every consumer depends
on all of it — `PanelTowerInfo` wants the upgrade and accounting half, `BoardRenderer` wants
position and the visitor, `AuraTower` wants position and type.

- **Where:** `td.tower.Tower`, and its consumers in `td.ui` and `td.tower`.
- **Approach:** split along the lines the consumers already use rather than inventing a
  taxonomy — most likely a small `TowerView` (position, type, range, visitor) that rendering
  and targeting take, leaving the mutating lifecycle on `Tower`. Do it when a consumer is
  actually hurt by the width, not as a tidying exercise: a wide interface with one
  implementation hierarchy costs far less than a wrong split.

### `EnemyMob`/`Tower`/`Projectile` use Visitor, not a sealed type + switch

Sealing would drop the `accept()` methods and the visitor interfaces, but `permits` cannot name
test classes, so the hand-written `EnemyMob` and `AbstractTower` fakes would stop compiling.

- **Where:** `EnemyMobVisitor`, `TowerVisitor`, `ProjectileVisitor` and their `td.ui` frame
  builders.
- **Approach:** only if the fakes go away first (tests built on real mobs/towers from
  `td.fixtures`). `Projectile` has no fakes but alone would leave two dispatch styles.

## Gameplay / balance

### Ghost health doesn't scale with level

`BuiltInEnemies.GHOST` (`EnemyDefinition.of(...).withHealthDivisor(5f)`) always divides incoming
health by a flat `5`, unlike its body size, which already scales with `level` via
`DefinedEnemyMob.bodyScaleFor`'s `(level < 6) ? (7 - level) : 2` curve (the same one `SQUARE`/
`TRIANGLE` use). Health reduction should probably scale the same way so ghosts stay balanced at
higher levels.

- **Where:** `BuiltInEnemies.GHOST`'s `withHealthDivisor(5f)`; the division itself is applied in
  `DefinedEnemyMob.withDividedHealth(SpawnParameters, float)`, called from the constructor before
  `level` is otherwise available to it.
- **Approach:** replace the flat `5f` with a level-scaled value, mirroring `bodyScaleFor`'s
  `(level < 6) ? (7 - level) : 2` curve (or a deliberately different one, if `5` was chosen for a
  reason that's no longer documented — worth play-testing either way). `healthDivisor` is a
  fixed field on the `EnemyDefinition` today, not computed per-spawn, so this needs
  `withDividedHealth` (or its caller) to take `level` as well, not just a differently-shaped
  constant.

## Movement / pathing

### Wave-entry spawn delay is a hardcoded constant

`SpawnParameters.DELAY_TICKS_PER_SLOT` (a `private static final float`, consumed inside
`SpawnParameters.atSlot(...)`/`.of(...)`) converts an enemy's slot position within a wave to a
tick-count countdown via `Math.round(DELAY_TICKS_PER_SLOT * slotPosition / speed)`. The `22.4f`
value is a magic constant with no way to override it per-wave.

- **Where:** `td.enemy.SpawnParameters`
- **Approach:** add a `delay`-scaling field to `WaveDefinition` (or `Wave`) that defaults to `22.4f`, and extend the
  wave
  mini-language (see `WaveScript.parse`'s spawn-shape/spacer token grammar — `td/wave/CLAUDE.md`) with a token — e.g. a
  `w<number>` prefix — that
  lets a wave definition override the spacing between spawns before listing enemies.

## Levels

### Only a Java-code level catalog exists

`LevelCatalog` is the abstraction levels are meant to be sourced through, but `BuiltInLevelCatalog` (levels defined as
Java code, e.g. `LevelDefinition` constants built from a hand-authored `List<Point>` of corners) is the only
implementation. There is no way to add or edit a level without a code change and a rebuild.

- **Where:** `td.level.LevelCatalog`, `td.level.BuiltInLevelCatalog`
- **Approach:** add a `FileLevelCatalog implements LevelCatalog` that parses level files (format TBD — JSON is the
  obvious choice given `LevelDefinition`'s shape) from a resources or config directory into `LevelDefinition`s. A
  malformed level file should throw `td.util.GameStartupException`, which `Main` already catches as the one
  fatal-startup boundary, rather than adding a second error-handling path.

## Tower features

### A global, buy-once upgrade for a whole tower type doesn't exist

`FEATURE-tower-upgrades.md` named a second kind of upgrade alongside the four per-instance ones
that shipped: bought once, it retroactively applies to every tower of a type already on the
field and to every one built afterward. Deferred because nothing today reaches back into an
already-built tower to change its base behavior, and nothing persists a per-type flag across a
tower purchase — both real, new mechanisms, not a variation on the per-instance model.

- **Where:** a new field on `GameWorld` (or wherever per-level persistent state would live),
  consulted by `TowerFactory` at construction and by something that re-walks `TowerRoster.all()`
  at purchase time.
- **Approach:** see `FEATURE-tower-upgrades.md`'s "Architectural implications" section for the
  concrete blockers (level-teardown/reload correctness in particular — a purchased global
  upgrade must not leak into the next level or the next run, the same discipline
  `TowerRoster.clear()`/`EnemyRoster.clear()` already follow). Settle where that persistent state
  lives before writing the retroactive-apply logic.

### Bounty for any kill inside an aura, not just the aura tower's own kills

Also named and deferred in `FEATURE-tower-upgrades.md`: an aura that pays out bonus bounty for
*any* kill scored inside its radius, not only the aura-owning tower's own. `EconomyDelta.kill`
is computed once, inside the dying enemy's own kill path, with no notion of which tower killed
it or which auras cover that location — a new coupling between `td.enemy` and `td.tower` that
the codebase has deliberately avoided so far (towers query enemies, never the reverse).

- **Where:** `AbstractEnemyMob`'s kill/death path (`td.enemy`), `EconomyDelta.kill` (`td.economy`).
- **Approach:** see `FEATURE-tower-upgrades.md`'s "Architectural implications" and "Risks and
  costs" sections. Either pass the killing tower's position outward from the death path so an
  aura-owning tower can be consulted, or add a new aura-query step in the kill path — both are a
  real, new cross-package coupling and should be designed deliberately, not bolted on.

### Upgrade-tree node numbers are unbalanced placeholders

Every tower's `base`/`head`/`special` `UpgradeNode`s (see `docs/features/FEATURE-tower-specialization-abilities.md`
and `docs/features/FEATURE-tower-upgrade-trees.md`) carry real prices and stat bonuses, but none of them have been
played against actual waves — the numbers follow a mechanical placeholder rule (Base Range ≈ +15% range at ~0.6×
tower price; Awaken ≈ 1× tower price; a `head` chain's level 1 ≈ its superseded v1 path's own price, level 2 ≈ 1.5×,
level 3 ≈ 2×; a `special` root ≈ 2× tower price; an unquantified "+X" in the source doc became +25%, an unquantified
crit bonus +10%) rather than a tuned one. The `ClusterCondition`/`DamageDealtCondition`/`KillCountCondition`
thresholds are similarly unverified guesses at what a reasonable mid-level of investment looks like.

- **Where:** the `private static final UpgradeNode` constants in every leaf under `td.tower`
  (`SniperTower`/`SplashTower`/`SonarTower`/`PulseTower`/`MortarTower`/`SeekerTower`/`CinderTower`/`AuraTower`).
- **Approach:** play each of the built-in levels with every node bought at least once, and adjust price/stat-bonus/
  condition-threshold values until each node feels like a meaningful, roughly-comparable-in-power choice rather than
  a strictly-better-or-worse one. No code or architecture change needed — every number here is already a named
  constant, not embedded in logic. `td.BalanceHarness` and the `n`/`x`/`c` debug keybindings (see `docs/ARCHITECTURE.md` section 9) now make this cheap to actually do.

### New tower numbers are unbalanced placeholders

`MortarTower`, `SeekerTower` and `CinderTower`'s price, damage, range, cooldown, splash radius, and slow/freeze/burn
magnitudes and durations were chosen to be plausible, not tuned - the same situation the upgrade-tree node numbers
above were in before their own balance pass. `CinderTower.COOLDOWN_MAX` and `CinderTower.WAVE_TRAVEL_TICKS` (added
with the cooldown-gated travelling-wave firing model) join this same bucket.

- **Where:** the `public static final` constants and effect-duration fields in `MortarTower`, `SeekerTower`,
  `CinderTower`.
- **Approach:** play each of the built-in levels with all three new towers, and adjust values until each feels like
  a meaningful, roughly-comparable-in-power choice next to the existing four attack towers. No code or architecture
  change needed - every number here is already a named constant, not embedded in logic. `td.BalanceHarness` and the
  `n`/`x`/`c` debug keybindings (see `docs/ARCHITECTURE.md` section 9) now make this cheap
  to actually do.
- **Evidence gathered, not yet acted on:** a one-tower-vs-one-captive-target comparison (all 7 attack towers, same
  position/level/2000-tick budget, single very-tanky enemy so none of them run out of target) found raw damage-per-
  credit-spent of `first` 16000, `second` 6827, `third` 4000, `cinder` 4270, `fourth` 3520, `mortar` 2393, `seeker`
  1697 - `mortar`/`seeker` are the two weakest of all seven, including both pre-existing splash/AoE towers, even
  though this single-target setup already under-counts `mortar`'s splash and `cinder`'s ghost-hitting value and
  over-counts nothing in their favor. Some of that gap is `mortar`'s unguided shell missing a moving target rather
  than raw output, which this comparison can't separate out - worth a real multi-enemy/formation test before
  concluding `mortar`'s price or damage needs to move, not just this single-target number on its own.
- **Formation follow-up, also gathered, also not yet acted on:** ran the multi-enemy test the entry above called
  for - all 7 attack towers, same solo position/board, but this time a single wave of many ordinary (not tanky)
  Circles spaced by the wave mini-language's default (no-spacer) spawn delay, which packs them into a dense moving
  column rather than a lone captive target. Two tick-scale runs at the same density (40 Circles, one straight
  corridor, one tower defending alone, no other help - a deliberately harsh solo-defense scenario, harsher than any
  real level's multi-tower setup): at moderate hp (800, comparable to `BuiltInLevelCatalog`'s own mid-game waves)
  nobody could solo-clear the column in 5000 ticks, but raw damage dealt reordered the field completely - `third`
  81680, `fourth` 70400, `second` 57290, `first` 35200, `mortar` 30094, `cinder` 29641, `seeker` 2931 (dmg-per-
  credit, not comparable in magnitude to the captive-target numbers above since the scenario differs, only in
  relative order). `mortar` climbed from tied-worst to solidly mid-pack once splash actually had neighbors to hit,
  confirming the captive-target test under-sold it as suspected. `seeker` got dramatically *worse*, not better -
  roughly 10-30x behind every other tower, including `first`, its closest single-target-only relative. At a lower
  hp (150) where a solo tower can plausibly clear the whole column, `second`/`third`/`fourth`/`mortar`/`cinder` all
  fully cleared 40 Circles (`third`/`fourth` fastest at ~2400 ticks, needing the fewest leaks along the way);
  `first` only managed 7/40 kills and never cleared; `seeker` killed *zero* and never cleared, even when the same
  test was re-run against a much smaller column of just 10 - ruling out "the group was too big" as the explanation.
  The likely cause isn't `seeker`'s reliability (it never misses) but its raw throughput: at damage/cooldown =
  1800/60 = 30 per tick, it is the lowest-DPS attack tower in the game, well below even `first`'s 4000/39 ≈ 103 per
  tick, and unlike `mortar`, `second`, `third`, `fourth` or `cinder` it has no splash/sweep/continuous-AoE
  multiplier to make up the gap once more than one target needs killing per unit time - every one of the other six
  towers has *some* way to hit more than one enemy per action; `seeker` alone does not. (`seeker`'s own freeze
  effect reordering the "furthest along path" ranking after every hit - each just-hit target instantly falls out of
  the lead once frozen, so the tower never gets to land a second shot on an already-damaged target - is a plausible
  compounding factor worth a follow-up ablation, but the plain DPS gap above is already sufficient to explain the
  result on its own.) Net read: `mortar`'s numbers may not need to move much, since real gameplay routinely bunches
  same-type enemies (`"10 c"`, dense runs inside `"s t s c g c t c s g t c s g c t s g t c"`, etc.) where its splash
  already earns its keep; `seeker` looks like the tower that actually needs attention, either a straightforward
  damage/cooldown buff or - possibly a better fit given it's explicitly the guaranteed-hit, never-misses tower - a
  narrower intended role (e.g. a single tough priority target, boss-adjacent) rather than a general crowd-clear
  price point.
- **`seeker`'s damage/cooldown buffed, its role deliberately left alone:** decided to take the straightforward-buff
  direction above, not the reroll - `SeekerTower.damage` 1800 -> 2600 and its `coolDownMax` 60 -> 45 (dmg/tick
  30 -> ~58), keeping guaranteed-hit reliability and freeze CC as its reason to exist rather than adding any
  splash/multi-target mechanic. Re-ran the same formation test against the buffed numbers: dmg-per-credit in the
  moderate-hp (800) 40-Circle scenario roughly doubled (2931 -> 5720), and it went from killing nothing at all
  against a 10-Circle column to landing one confirmed kill - a real improvement, but it's still the clear last place
  of all seven (`cinder`, the next-lowest, is still ~5x ahead, versus ~17x before the buff), because a pure
  single-target tower's dmg-per-credit in a *packed-formation* stress test specifically will never match one with
  any splash/sweep/continuous-AoE mechanic no matter how far its own numbers move - that gap is structural to the
  scenario, not a sign the buff was sized wrong. The formation test also confirmed the freeze-reordering mechanic
  flagged above as "plausible, unconfirmed": doubling `seeker`'s output didn't proportionally raise its kill count,
  because each hit still knocks its target out of the "furthest along path" lead it needs to be re-selected and
  finished off - only a targeting-behavior change (out of scope here; the ask was numbers only) would fix that, so
  a future pass could reconsider it if `seeker` still feels weak after this buff lands in real play.

## Tower specialization primitives

`docs/features/FEATURE-tower-specialization-abilities.md` tags eleven consumers across the tower
catalogue **[S]** - the node itself is real (id, price, gate, description, selectable in the UI)
but its behavioral hook is a documented no-op until the primitive below lands, per that document's
own two-document build order. Each entry closes once its primitive is implemented and every node
listed is wired to real behavior in the same commit.

### `EffectKind.VULNERABLE` doesn't exist

The largest of the eleven: a stacking (cap 3) damage-amplifying status effect. Six nodes wait on
it: Marked Round (`SniperTower`), Warding Field (`PulseTower`), Cursed Shrapnel (`MortarTower`),
Homing Curse (`SeekerTower`), Hexflame (`CinderTower`), Withering Field (`AuraTower`).

- **Where:** `td.effect` (`EffectKind`, `Effect`'s static factories, `ActiveEffects.magnitude`),
  per `td/effect/CLAUDE.md`'s "Adding a kind" checklist.
- **Approach:** see `FEATURE-tower-specialization-abilities.md`'s "New primitives" #1 and its own
  open questions (stack shape, per-stack magnitude, composition order with `SHIELD`/`Trait`
  resistance) - none of those are resolved yet.

### Partial or full armor/shield bypass on a hit doesn't exist

`AbstractEnemyMob.doDamage`'s `absorb`/`applyShield` steps are always fully applied or not applied
at all; nothing can skip part of either. Two nodes wait on it: Sniper's Marksman's Eye II (50%
ignore) and Momentum (100% ignore, "Fifth Shot"'s sibling special).

- **Where:** `td.enemy.AbstractEnemyMob.doDamage`.
- **Approach:** see `FEATURE-tower-specialization-abilities.md`'s "New primitives" #2.

### A guaranteed-crit trigger with a per-node crit multiplier doesn't exist

`Damage.asCritical()` applies one fixed, project-wide multiplier; nothing can force a crit outside
the normal roll or override that multiplier per node. Three nodes wait on it: Sniper's Fifth Shot
(every 5th shot, 250%) and Momentum (crit-then-boosted-next-shot, 500%), and Sonar's Mark on Sweep
(mark-then-guaranteed-crit).

- **Where:** `td.tower.AbstractTower.rollCritical`, `td.damage.Damage.asCritical`.
- **Approach:** see `FEATURE-tower-specialization-abilities.md`'s "New primitives" #3 and open
  question #7 (revise the fixed multiplier into a per-node override, or treat these nodes' own
  percentages as flavor text for the existing fixed one - not resolved).

### A timed, non-stacking self-buff pulse triggered by a kill doesn't exist

Momentum's "+100% fire rate for 5s, does not stack" - every upgrade-node buff today is permanent
from the moment it's bought, not one with its own expiry layered on top.

- **Where:** `td.tower.AbstractTower` (or a new small per-tower timed-buff holder).
- **Approach:** see `FEATURE-tower-specialization-abilities.md`'s "New primitives" #4.

### An on-kill secondary trigger, aware of the kill's own status effects, doesn't exist

Two nodes wait on it: Seeker's Deep Freeze II (shatter-on-kill splash, only if the kill was
frozen) and Splash's Concussive Blast (a killed enemy explodes).

- **Where:** `td.tower.AbstractTower.dealDamage` or `td.enemy.AbstractEnemyMob`'s kill path.
- **Approach:** see `FEATURE-tower-specialization-abilities.md`'s "New primitives" #5.

### "Reveal an invisible enemy to every tower" doesn't exist

Distinct from Pulse's existing "already hit because something else triggered it" behavior - this
is an active reveal other towers can then target off of. Two nodes wait on it: Pulse's Resonant
Field II and Sonar's Wide Band.

- **Where:** `td.enemy.EnemyMob`/`ActiveEffects` (a reveal needs to be visible to every tower's
  own targeting query, not just this one's).
- **Approach:** see `FEATURE-tower-specialization-abilities.md`'s "New primitives" #6.

### Crit-triggered behavior override, beyond bonus damage, doesn't exist

Two nodes wait on it: Splash's Overpressure (a crit fires at every enemy in range instead of the
one target) and Rapid Battery III (a crit's splash is 50% bigger).

- **Where:** `td.tower.SplashTower`, once `AbstractTower.rollCritical`/`dealDamage` can report a
  hit's crit outcome back to the caller before the caller's own splash loop finishes (it already
  can via `dealDamage`'s boolean return - what's missing is a place to act on it mid-loop).
- **Approach:** see `FEATURE-tower-specialization-abilities.md`'s "New primitives" #7.

### Distance-scaling damage doesn't exist

Sonar's Long Reach II: damage scales up to +100% at max range. `TowerBuff` has no axis for
"strength depends on this shot's own distance from the tower."

- **Where:** `td.tower.SonarTower.doTick`.
- **Approach:** see `FEATURE-tower-specialization-abilities.md`'s "New primitives" #8.

### A second, independently-aimed turret head doesn't exist

Sonar's Twin Array III - primarily a render/aim concern (a second `TurretAim` instance, a second
`TurretHeadDraw`), not damage math.

- **Where:** `td.tower.SonarTower`, `td.ui.TowerSpriteFrameBuilder.visitSonarTower`.
- **Approach:** see `FEATURE-tower-specialization-abilities.md`'s "New primitives" #9.

### A toxic-DoT `EffectKind`, distinct from burn, doesn't exist

Splash's Toxic Bloom - its own decay curve, separate from `BURN`'s fuel-pool model.

- **Where:** `td.effect`, per `td/effect/CLAUDE.md`'s "Adding a kind" checklist.
- **Approach:** see `FEATURE-tower-specialization-abilities.md`'s "New primitives" #10.

### Resistance-aware damage scaling doesn't exist

Sonar's Piercing Tone: bonus magic damage against physically armored/shielded enemies, scaling
with how much resistance they carry, up to a cap.

- **Where:** `td.tower.SonarTower.doTick`, reading the target's own `Trait`/`SHIELD` state.
- **Approach:** see `FEATURE-tower-specialization-abilities.md`'s "New primitives" #11.

### A shrapnel damage ring past the main splash doesn't exist

Not one of `FEATURE-tower-specialization-abilities.md`'s own eleven primitives - a gap this
feature's own implementation pass found in Mortar's `head` chain. Fragmentation Rounds (25% weapon
damage in a wider ring past the main splash) and Fragmentation Rounds II (the same shrapnel also
applies this tower's slow, at half duration) both wait on it; the second literally cannot exist
without the first.

- **Where:** `td.tower.MortarTower.onImpact`.
- **Approach:** add a second, wider `InRangeTargetQuery` ring past the existing splash radius,
  dealing 25% of `damageCurrent()` with no falloff, gated on Fragmentation Rounds being owned;
  Fragmentation Rounds II then applies `Effect.slow` (the same call `onImpact` already makes for
  the main splash) at half `slowDurationTicks` to whatever the ring catches.

## Damage types

### Acid is not implemented as a second damage-over-time effect

The original damage-types request named acid alongside burn as a second damage-over-time effect; v1 shipped only
slow, burn and freeze (see `docs/features/FEATURE-damage-types-and-projectiles.md`'s Decisions and V1 Scope), deferring
acid rather
than dropping it.

- **Where:** `td.effect` (`EffectKind`, `Effect`) has no `ACID` case; nothing produces one.
- **Approach:** first settle the feature doc's open question — is acid meant to be mechanically distinct from burn
  (different scaling, a different interaction with a future armor/shield trait) or primarily a different visual on
  the same damage-over-time mechanism? Only then add an `EffectKind.ACID` case and an `Effect.acid(...)` factory,
  mirroring `Effect.burn(...)`.

### A typed shield has no concrete user yet

`ShieldTemplate`/`Effect`/`ActiveEffects.applyShield` can all restrict a shield to one `DamageType`
(`ShieldTemplate.physicalOnly`/`.magicOnly`), mirroring the same restriction `PercentResistTrait`/
`FlatResistTrait` already carry - but no built-in ability actually authors one. This has precedent:
`td/effect/CLAUDE.md` already records that `SLOW`/`BURN`/`FREEZE` ship with no `EffectTemplate`
authoring them either.

- **Where:** `td.effect.ShieldTemplate.physicalOnly`/`.magicOnly`, `ActiveEffects.applyShield`.
- **Approach:** give a concrete enemy ability a typed shield so it is play-verified, not just
  unit-tested - the Warden's `reshield`/`callToArms` (`BuiltInEnemies.WARDEN_STANDING_ABILITIES`)
  are the obvious carriers.

### Rotating tower sprites

Towers don't rotate their sprite image to visually face their current target (enemy mobs already
do this — see below). This was a speculative "nice to have," not a committed design.

- **Where:** `td.ui.TowerSpriteFrameBuilder`, `td.ui.render.TowerSpriteDraw`
- **Approach:** if pursued, note the facing-angle precedent on the enemy side no longer looks the
  way this entry originally described: mob movement is composed via `MovementBehavior`
  implementations (`FixedMovement`/`PulseMovement`/`PathDirectionalMovement`/`RotorMovement`,
  `td.enemy`), and the current facing logic is `DefinedEnemyMob.getFacingRadians()` — a `switch`
  over `this.definition.movement()`'s sealed type. Towers have no equivalent composed-movement
  model, so there's no direct mechanism to reuse from there; a tower's facing would instead need
  to be derived from its own chosen target, per tower. There's also no `findEnemy()` method to
  hook today — targeting is composed per-tower inside `doTick` via `td.tower.targeting` pieces
  (e.g. `SniperTower.doTick` builds candidates through `InRangeTargetQuery.ofType(...)` then a
  `TargetSelector`; see `td/tower/CLAUDE.md`'s "Targeting" section) — so
  this needs new per-tower "facing" state updated wherever each tower's `doTick` calls its
  selector, exposed as a getter, then threaded through as a new `facingRadians` field on
  `TowerSpriteDraw` (currently absent — `EnemyBodyDraw`/`EnemyFadeDraw` already carry one) and
  applied as a rotation in `Java2DFrameRenderer.paintTowerSprite()` alongside rotated sprite art
  for each tower.

## Enemy features

### The effect-marker overflow indicator has no count

`EnemyFrameBuilder`'s marker row caps at 3 visible status-effect icons; a 4th+ simultaneous
effect collapses into one generic `Palette.STATUS_MARKER_OVERFLOW` marker rather than the "+N"
badge `docs/features/FEATURE-enemy-traits-and-effects.md`'s V1 Scope originally called for. The render frame
model has no text-drawing primitive today (every draw command is a coloured `Shape`), so
showing an actual number would be new render infrastructure, not a tweak to this one marker.

- **Where:** `td.ui.EnemyFrameBuilder.markers()`, `td.ui.render.Palette.STATUS_MARKER_OVERFLOW`.
- **Approach:** add a small text-drawing `RenderFrame` primitive (a `Palette` role isn't enough
  on its own - it needs the string/number itself, so a new sealed draw record carrying the
  count) and a `Java2DFrameRenderer.drawString` case for it, then have the overflow marker
  carry `activeCount - MAX_VISIBLE_MARKERS` instead of being a fixed, countless glyph.

### Enemy traits/abilities numbers are unbalanced placeholders

Every number introduced by the data-driven enemy model - `PercentResistTrait`/
`HurtSpeedTrait`'s migrated Square/Triangle factors, and the Warden/egg chain's health, price,
ability intervals, shield percentages/radii and `EGG_HATCH_DELAY_TICKS` - was chosen to be
plausible, not tuned, the same situation the tower-upgrade and new-tower-numbers entries above
were in before their own balance passes. **One exception:** the Warden's `FlatResistTrait`
value has been tuned (15 -> `BuiltInEnemies.WARDEN_FLAT_RESIST` = 200) against the actual
per-hit/per-tick damage scale every attack tower deals (150-4000, see the head-to-head data in
the new-tower-numbers entry above) - 15 was negligible against any of them (0.375%-10% of a
single hit), making the Warden's armor mechanically inert regardless of which tower fought it.

- **Where:** `BuiltInEnemies` (all trait/ability constants), `PercentResistTrait`,
  `HurtSpeedTrait`, `FlatResistTrait`.
- **Approach:** play Curly Path through to the Warden encounter (and the other two levels,
  once they get their own late-game content) repeatedly, adjusting values until the chain and
  the migrated traits feel meaningfully tuned rather than placeholder guesses - no code or
  architecture change needed, every number here is already a named constant. `td.BalanceHarness`
  and the `n`/`x`/`c` debug keybindings (see `docs/ARCHITECTURE.md` section 9) now make this cheap to actually do - `x` specifically can spawn the Warden chain's
  stages on demand without playing to wave 18 first.

### Critical-damage numbers are unbalanced placeholders

`FEATURE-critical-damage.md` shipped `Damage.CRITICAL_MULTIPLIER` (1.5x), `SniperTower.VETERAN`'s
crit-chance bonus (15%), the Warden's new on-crit-survived shield (30% for 100 ticks), and (per
the feature doc's Addendum) `AbstractTower.BURN_CRIT_CHANCE_MULTIPLIER` (2x) as illustrative
placeholders, the same situation every other feature's first-pass numbers were in before their
own balance passes.

- **Where:** `td.damage.Damage.CRITICAL_MULTIPLIER`, `SniperTower.VETERAN`'s `TowerBuff`,
  `BuiltInEnemies.WARDEN_STANDING_ABILITIES`'s new `OnCriticalHitTakenTrigger` ability,
  `td.tower.AbstractTower.BURN_CRIT_CHANCE_MULTIPLIER`.
- **Approach:** tune via actual play (or `td.BalanceHarness`) once the other placeholder-number
  entries in this file get their own pass - no code or architecture change needed, every number
  here is already a named constant or a `TowerBuff` literal.
