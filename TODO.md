# Notes written by hand, to be processed later - write nicer detailed TODO entries from it

I would like to add feature ideas to this todo as well to have all possible work idead at one place.

# Known gaps and future work

Extracted from inline `TODO` comments (and one unmarked-but-real gap) found throughout the codebase during a
documentation cleanup pass. Each item below replaces the original comment; the source no longer carries these notes, so
this file is the single place to look for outstanding design/feature gaps.

## Feature request implementation order

The tower rework, in this order (each needs the ones before it):

1. `FEATURE-tower-progression.md`: Attune -> Awaken -> Transcendent, level IV, the extra head
   node, two special slots, the exclusive-choice mark, price rules.
2. `FEATURE-xp-and-purpose-gates.md`: XP from bounty and one purpose gate per tower replace kill
   and damage gates.
3. `FEATURE-sniper-and-sonar.md`: crit per tower, the hit / periodic rule, the first debuffs and
   the effect rules every tower follows.
4. `FEATURE-splash-stormcaller-and-hexer.md`: Dazed and the hex pool.
5. `FEATURE-pulse-and-seeker.md`: Silenced, Anchored, Unraveled, Brittle, the nest.
6. `FEATURE-ground-zones-mortar-and-cinder.md`: ground zones and the fire-and-ice rules.
7. `FEATURE-aura-and-balance-pass.md`: the Aura, harness pricing, display names, the README.

Enemies come after, from `docs/ideas/enemies-brainstorm.md`.

Every earlier feature request has shipped (see each doc's own status line) except the draft
`FEATURE-effect-interactions.md`, whose scope has landed in part (vulnerable, poison, revealed,
freeze-burn, universal freeze diminishing returns) and is closed out in its own status line.

Implemented, for reference: `FEATURE-enemy-spawn-types.md`, `FEATURE-multiple-enemy-paths.md`,
`FEATURE-playtesting-and-balance-tooling.md`, `FEATURE-enemy-rank-system.md`,
`FEATURE-effect-visuals.md`, `FEATURE-tower-upgrades.md`, `FEATURE-critical-damage.md`,
`FEATURE-damage-types-and-projectiles.md`, `FEATURE-enemy-traits-and-effects.md`,
`FEATURE-wave-preview-cleanup.md`, `FEATURE-effect-diminishing-returns.md`,
`FEATURE-tower-upgrade-trees.md`.

## Architecture and correctness

Findings from the architecture audits of 2026-09-17, highest-severity first. The threading
group and the external audit's critical/high/moderate findings have landed; what remains is
listed below.

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

- **Where:** `DefinedEnemyMob`'s kill/death path (`td.enemy`), `EconomyDelta.kill` (`td.economy`).
- **Approach:** see `FEATURE-tower-upgrades.md`'s "Architectural implications" and "Risks and
  costs" sections. Either pass the killing tower's position outward from the death path so an
  aura-owning tower can be consulted, or add a new aura-query step in the kill path — both are a
  real, new cross-package coupling and should be designed deliberately, not bolted on.

### Upgrade-tree node numbers are unbalanced placeholders

Every tower's `UpgradeNode`s carry real prices and stat bonuses, but none of them have been played
against actual waves. Prices start from `UpgradeTier`'s multiples of the tower's list price (the table in
`docs/features/FEATURE-tower-progression.md`); the stat bonuses still follow the placeholder rule of the
first trees (an unquantified "+X" in the source doc became +25%, an unquantified crit bonus +10%). The
`ClusterCondition`/`DamageDealtCondition`/`KillCountCondition` thresholds are unverified guesses at what a
reasonable mid-level of investment looks like. Feature 7's balance pass sets the final numbers.

- **Where:** `UpgradeTier`'s price multiples, and the `private static final UpgradeNode` constants in every
  leaf under `td.tower` (`SniperTower`/`SplashTower`/`SonarTower`/`PulseTower`/`MortarTower`/`SeekerTower`/
  `CinderTower`/`AuraTower`).
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

### Effect and specialization numbers are unbalanced placeholders

Every number the effect-interactions and specialization work introduced was chosen to be plausible,
not tuned: vulnerable (+15% per stack, cap 3, 4 s), the chill cap (80%), how far a chill cuts a burn (half
at the cap), poison's damage (10% of weapon damage per tick, 4 s), its slow (up to 30%) and the half-second
stack interval of burning and poisoned, revealed (3 s from Wide Band, 2 s from Resonant Field II),
Momentum's cooldown cut and 5 s, the shatter and explosion share and radius, shrapnel's ring width and
share, Warding Field's 10% chance, Withering Field's interval, Long Reach II's scaling, and Splash's three
blasts. Burning's resilience loss (-1 per stack) and poisoned's spirit loss (-1 per stack) grow without
limit up to their stat floors of -100. Scorched and Sickened lose a stack per second at neutral spirit
(one per 20 ticks, `ActiveEffects.STACK_DECAY_INTERVAL_TICKS`), so steady burning nets one stack per
second and needs about 100 s to reach the floor; at -100 spirit they never wear off.

**Not as designed - to iterate on:** stack decay currently runs all the time, including while a burn or
poison is still earning stacks. A burning enemy earns 2 stacks per second and loses 1, so the net gain is 1
per second. That is a side effect of the first implementation, not the intended design, and the behaviour
wanted here is still to be settled: for example decay paused while the pool is active, decay only once it has
ended, or a different rate. Sickened lowering spirit, which slows the decay of its own stacks and of Scorched,
interacts with whichever is chosen.

- **Where:** the named constants in `td.effect.ActiveEffects`, `AbstractTower` and each tower.
- **Approach:** tune through play or `td.BalanceHarness` with the other placeholder entries; decide
  whether a long burn's resilience loss (crit damage up to double at the floor) is too strong.

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

`ShieldTemplate`/`Effect`/`ActiveEffects.contributeTo` can all restrict a shield to one `DamageType`
(`ShieldTemplate.physicalOnly`/`.magicOnly`), mirroring the same restriction `PercentResistTrait`/
`FlatResistTrait` already carry - but no built-in ability actually authors one. This has precedent:
`td/effect/CLAUDE.md` already records that `SLOW`/`BURN`/`FREEZE` ship with no `EffectTemplate`
authoring them either.

- **Where:** `td.effect.ShieldTemplate.physicalOnly`/`.magicOnly`, `ActiveEffects.contributeTo`.
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
  (e.g. `SniperTower.doTick` builds candidates through `InRangeTargetQuery.visible(...)` then a
  `TargetSelector`; see `td/tower/CLAUDE.md`'s "Targeting" section) — so
  this needs new per-tower "facing" state updated wherever each tower's `doTick` calls its
  selector, exposed as a getter, then threaded through as a new `facingRadians` field on
  `TowerSpriteDraw` (currently absent — `EnemyBodyDraw`/`EnemyFadeDraw` already carry one) and
  applied as a rotation in `Java2DFrameRenderer.paintTowerSprite()` alongside rotated sprite art
  for each tower.

## Enemy features

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

`FEATURE-critical-damage.md` shipped the default crit multiplier (1.5x, now
`AttackProfile.DEFAULT_CRIT_MULTIPLIER`), `SniperTower.VETERAN`'s crit-chance bonus (15%), the
Warden's new on-crit-survived shield (30% for 100 ticks) as illustrative
placeholders, the same situation every other feature's first-pass numbers were in before their
own balance passes.

- **Where:** `td.damage.AttackProfile.DEFAULT_CRIT_MULTIPLIER`, `SniperTower.VETERAN`'s
  `TowerBuff`, `BuiltInEnemies.WARDEN_STANDING_ABILITIES`'s new `OnCriticalHitTakenTrigger`
  ability.
- **Approach:** tune via actual play (or `td.BalanceHarness`) once the other placeholder-number
  entries in this file get their own pass - no code or architecture change needed, every number
  here is already a named constant or a `TowerBuff` literal.

### The Warden's description promises a reinforcement its ability doesn't call

Every Warden stage's description says it "calls an extra reinforcement if left unattacked too
long", but the ability behind that trigger heals the Warden instead.

- **Where:** `BuiltInEnemies.WARDEN_ABILITY_BLURB` and the `TimeSinceLastHitTrigger` entry in
  `WARDEN_STANDING_ABILITIES`.
- **Approach:** decide which behaviour is intended, then change either the ability's action or
  the description so they agree.

## UI

### A hatched egg leaves its stale stats in the inspector

Selecting a Warden egg and letting it hatch clears the selection ring, but the panel keeps the
egg's last stats with no status line, unlike a kill (`Killed`) or a leak (`Leaked`).

- **Where:** `td.enemy.EnemyInspection.Fate`, the hatch path in `SpawnEnemiesAction`.
- **Approach:** give a mob replaced by its own spawn a fate (e.g. `Hatched`) so the inspector
  says why it stopped updating.
