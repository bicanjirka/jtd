# Plan: Stat-driven buffs, debuffs and upgrades

**Status:** planned, 2026-10-12. Not started.
**Follow-up:** `docs/features/FEATURE-agility-and-dodge.md` is implemented after this plan, on top of
it. Do not start it here.

This plan replaces an earlier one written for an older codebase (kept on the local branch
`stat-driven-buffs`; never merge that branch). Its product decisions carry over, revised where the
current content needed it (section 2).

## How to use this plan

The plan is written to be executed mechanically. Read sections 1 to 5 once, then do the phases in
order.

- One phase is one commit, with the commit subject the phase gives. `mvn verify` is green before
  every commit. The docs a phase names change in the same commit.
- Do exactly what a step says. The numbers given are the numbers to use. A number marked
  *starting value* is still the number to use; a balance pass tunes it later, not you.
- Names in this plan are final. Do not rename them, and do not add types, methods or stats the plan
  does not name.
- The usual way through a step is: make the stated change, run `mvn -q compile`, and fix every
  compile error the way the step's mapping table says. The compiler finds the call sites; the plan
  tells you what each becomes.
- Keep every existing test's behaviour sentence true. When a phase deliberately changes a rule, it
  lists the tests that change: update their numbers, and rename a test whose sentence became false.
  Never delete a test to get green unless the phase says the tested thing is removed.
- If a step cannot be done as written (a file moved, a method doesn't exist, a number doesn't
  match), stop and ask. Do not invent a replacement mechanism.
- The project rules in the root `CLAUDE.md` still apply: `this.` on every field, a record with 5+
  components gets a narrow factory and `withX` copies, no `instanceof` on domain types, hand-written
  fakes, behaviour-sentence test names, AssertJ, no comments that restate code.
- Run `td.PerformanceHarness` after every phase from 2 on; every one of them touches per-tick or
  per-hit code. It exits non-zero when over budget: fix the allocation, don't raise the budget.

## 1. Why: the assessment

The enemy side already works the way this plan wants. An enemy is data (`EnemyDefinition`). Traits
change it only through `StatModifiers`, one `StatSheet` resolves every stat with one formula, and
`HitResolution` is the one hit formula. Behaviour reads resolved numbers.

The tower side and the effects don't:

1. **Towers have no stat sheet.** "Change a number" is done five different ways: `TowerBuff` (12
   fixed fields with four different combine rules), 30 per-tower `*Spec` records refined by 123 perk
   classes in purchase order (each `withX` invents its own rule: scale, replace, max, first-wins),
   per-shot conditional bonuses written as perk code, towers computing bonuses from enemy stacks,
   and two timed self-buff mechanisms. In Structured Design terms: stamp coupling through wide
   records, and control coupling through spec flags read by `if` in 400 to 800-line tower classes.
2. **Effect kinds are a closed enum with logical, not functional, cohesion.** A kind's meaning is
   spread over 13 classification methods in `EffectKind`, four per-kind branches in `ActiveEffects`,
   `Effect`'s factories, `AbstractTower.applyStacks`, `EffectInteractions` and the UI. Its numbers
   are constants inside `ActiveEffects`. Rime, Ash, Tarred, Undertow and Brittle are conditions in
   `ActiveEffects`, although the package rule puts cross-kind rules in `EffectInteractions`.
3. **`AuraTower` is the one real node-identity hotspot:** about twenty `owns(...)` checks, and
   `power`/`grantsFireRate` mutated in `onUpgradeBought`, so derived state is stored.
4. **Relative numbers everywhere.** Upgrades are "+30% damage", armor is authored as "the fraction
   of a hit kept", shields are "a share of every hit". The player can't compare them, and stacking
   rules differ per kind.

## 2. Decisions

Product decisions made with the user. They are not open for change during implementation.

- **D1. Aura stacking stays additive.** No diminishing returns (section 13).
- **D2. Fire rate becomes attack speed, and attack speed adds.** `attacks per second = base rate ×
  (1 + Σ attack speed)`, the sum held to `[-0.75, 9]`. Authored node numbers are kept literally, so
  "+25%" now means exactly +25% attacks per second. The one exception is Dragon's Breath, whose
  `2/3` meant "three times as fast": it becomes `+2.0` (D14).
- **D3. Pulse and Sonar keep their designs.** Pulse's fractional clock (`FireClock`) and Sonar's
  sweep already scale with the rate; only the rate's formula changes.
- **D4. Absolute modifiers where the author knows the base.**
  - Flat for content authored for one tower or one enemy: upgrade nodes, enemy traits, enemy
    abilities.
  - Flat for an effect that lands on many units when the stat has one shared scale: armor, magic
    resist, plating, resilience, spirit, and damage taken (D11).
  - A percentage only for a cross-unit effect on a stat whose scale differs widely per unit: an
    aura's damage and range bonus. Attack speed, crit chance and penetration are fractions by
    nature and stay fractions.
  - Armor and magic resist show as points in the UI ("Armor 25").
- **D5. Resilience owns crit chance taken, both ways.** A hit's crit chance and crit bonus are both
  scaled by `1 - resilience / 100`, so resilience −100 doubles both and +100 is crit-immune.
  `CRIT_CHANCE_TAKEN` is removed. Exposed (and Revealed, which exposes) becomes "−50 resilience".
  Consequence, accepted: burns' Scorched stacks, Fractured, Resonant Crack and Fault Line now also
  make crits likelier, and an Exposed crit-immune enemy (resilience 100 − 50) can be critted at half
  chance.
- **D6. Heals are visibly significant.** Mender, Warden, Frenzied boss and spawnling heals are raised
  to the phase 4 values.
- **D7. Shields are absorb pools (World of Warcraft style).** A shield is a number of points that
  absorbs damage after armor and plating and then depletes. Reapplying keeps the larger pool and the
  longer time. Absorbed damage counts toward the tower's damage dealt, not toward the kill.
- **D8. Agility and dodge** get a feature request now and are implemented after this plan.
- **D9. Nothing an aura receives changes what it gives.** An aura never buffs another aura (already
  true), so one recompute pass in any order is correct.
- **D10. No scripting language.** Stats, statuses, procs and hit bonuses are data with closed,
  sealed vocabularies. A mechanic with its own math stays a small, named Java class (a perk).
- **D11. Damage taken adds.** Vulnerable, Priority, Kill Zone, Resonating, Brittle, Pyromancer's Mark
  and Heat add percentage points to the damage-taken stats instead of multiplying: three Vulnerable
  and a Priority are +60%, not ×1.52×1.15.
- **D12. Speed changes stay relative.** Every slow (chill, poison, tar, undertow, anchored) and every
  speed-up (hurt speed) stays a share of speed, shown as a percentage. No soft floor is needed.
- **D13. Statuses: the same id refreshes, different ids stack.** A tower status granted again
  restarts its clock and charges; two different statuses add up.
- **D14. A number becomes a stat by a rule.** A number is a `TowerStat` when the tower has it from
  the start (its base attack) or it belongs to the tower's Attune mechanic, *and* some node changes
  it. Every other number belongs to the mechanic a node adds, and lives in that mechanic's spec, set
  by its perk. The rule keeps the stat list bounded and puts each mechanic's data in one class.
- **D15. Effect kinds own their numbers.** Every Vulnerable is the same Vulnerable whoever applies
  it: the per-stack modifiers live on the `EffectKind`.
- **D16. Purchase order matters only for targeting.** Today's rule stays: the first owned node that
  sets a target priority decides it. Everything else combines with the stat algebra, in any order.

## 3. Target architecture

### 3.1 Vocabulary

| Term | Meaning | Type |
|---|---|---|
| Stat key | One number on a sheet, with a default and a clamp | an enum implementing `td.stat.StatKey`: `EnemyStat`, `TowerStat` |
| Base | The authored value of each stat | `BaseStats<S>` |
| Modifier | A change to one stat: flat, percent, multiply, set-to (and, from phase 6, a cap at a share of base) | `StatModifier` |
| Modifier bundle | Modifiers for many stats, with `plus`, `none()`, `scaledBy`, `times` | `StatModifiers<S>` |
| Resolved stats | The immutable result of base + bundle | `ResolvedStats<S>` (towers); `StatSheet` (enemies, cached and allocation-free) |
| Node effects | What an upgrade node does: modifiers, emission, hit bonuses, procs, targeting, capabilities, note | `td.tower.upgrade.NodeEffects` |
| Emission | What an aura gives the towers in its range | `Tower.buffFor(Tower)` returning `StatModifiers<TowerStat>` |
| Perk | A tower mechanic a node adds; refines the tower's spec | per tower, e.g. `CinderPerk` |
| Status | A timed or per-shot tower buff | `TowerStatus`, held by `ActiveTowerStatuses` |
| Capability | A yes/no a node or status grants | `TowerCapability` |
| Hit bonus | A conditional change to one hit (damage, crit, the wait after it) | `td.tower.proc.HitBonus` |
| Proc | Trigger + conditions + payload: the Warcraft III on-attack ability | `td.tower.proc.Proc` |

### 3.2 One resolve formula

Every stat on both sides resolves the way `StatModifier.applyTo` already does:

```
if any set-to value:   value = min(set-to values)
else:                  value = (base + Σflat) × (1 + Σpercent) × Πmultiply
                       (enemies only: + restorative flats × spirit factor × healing received)
                       (phase 6: then held to at most base × min(cap shares))
clamp to the stat's [min, max]
```

### 3.3 The layers of a tower's stats

```
base (TowerBaseStats)
 + owned upgrade nodes          (UpgradeState.totalModifiers)
 + active statuses              (ActiveTowerStatuses: timed and per-shot buffs, Rally)
 = the tower's own layer
 + received emissions           (Σ other.buffFor(this); an Aura never receives one)
 + disruption                   (Jammer zones: attack speed and range in cells, × DISRUPTION_TAKEN)
 = resolved TowerStats          -> what behaviour reads
```

An aura's emission is computed from its own stats (`AURA_POWER`) and its owned nodes, never from what
it receives (D9).

### 3.4 Data flow (Constantine's transform analysis)

```
afferent (inputs)                       central transform           efferent (consumers)
------------------------------------    -----------------------     -------------------------------------
base stats, upgrade nodes, statuses,    stat formula ->             targeting reach, fire clock, damage,
emissions, disruption, traits,          TowerStats / StatSheet      blast radius, counts, effect lengths,
effect kinds                            (one immutable snapshot)    the hit formula
```

Tower code never asks which node is owned (the base chain's Attune, Awaken and Transcendent aside).
It asks for a number (`stats().value(TowerStat.AREA_RADIUS)`), a capability
(`stats().has(TowerCapability.MAGIC_DAMAGE)`), its spec (what its perks switched on), or the hit
bonuses for a hit.

### 3.5 Threading

- Tower stats stay one immutable `TowerStats` behind one `volatile` (`AbstractTower.stats`). Writers
  stay as today: the EDT on purchase, placement and sale; the game-loop thread in `beginTick`,
  `beginShot` and when a status is granted. A race between them is corrected on the next tick.
- `ActiveTowerStatuses` is game-loop confined. It publishes its folded modifiers through one volatile
  record that `publishStats` reads, so the EDT never reads the live status list.
- `ResolvedStats`, `StatModifiers`, `NodeEffects`, `TowerStatus`, `Proc` and `HitBonus` are immutable.

### 3.6 What stays code

Mechanics whose math is their identity stay named classes and are switched on by perks: the Sonar
sweep and beams, the Cinder wave and its pools' Stoke, shell and missile flight, every fork's
mechanic (arcs, hexes, mines, nukes, bomblets, salvos, payloads, the nest, the zap, field modes),
the burn and poison fuel pools and their damage apportionment, chill's linear decay, freeze
diminishing returns, Steady Aim's focus, Hunter's Mark's streak. They read stats; they are never
switched by node identity.

### 3.7 Rejected alternatives

- **A trigger/condition/action scripting layer.** This is how Warcraft III maps became spaghetti.
- **Iterating aura buffs to a fixed point.** D9 makes it unnecessary.
- **Every perk number as a stat.** D14 keeps mechanic parameters with their mechanic.
- **Making `StatSheet`/`StatView` generic.** The enemy sheet is on the per-hit hot path and
  allocation-free; only the shared pieces become generic.
- **Moving tower stat recomputation onto the game-loop thread only.** Purchases would show one tick
  late and every test that buys and asserts would churn.

## 4. Phase overview

| Phase | Commit subject | Behaviour change |
|---|---|---|
| 1 | Make the stat core generic over its stat key | none |
| 2 | Replace TowerBuff with a tower stat sheet and additive attack speed | attack speed adds; Jammer range in cells; a disrupted aura updates the towers it buffs at once |
| 3 | Turn tower numbers into stats, author upgrades in absolute values and make the Aura data | upgrades are flat, so an aura amplifies them a little more; a few multiplicative combos now add |
| 4 | Author enemy stats in absolute values, let resilience own crits and strengthen heals | armor in points; negative resilience and Exposed raise crit chance; damage taken adds; Cracked is flat; heals much stronger |
| 5 | Make shields absorb damage from a depleting pool | shields absorb points |
| 6 | Describe effect kinds as data and give towers statuses | Frenzy refreshes and stacks with Momentum's haste; Soulfire shows no countdown; earned stacks cap at 100 |
| 7 | Move generic upgrade effects into procs, hit bonuses, targeting and capabilities | conditional damage bonuses add instead of multiplying |

## 5. Baseline

Before phase 1, run `mvn -q compile exec:java -Dexec.mainClass=td.BalanceHarness` on every built-in
level with its default loadout (see the harness's own usage line in `BalanceHarness.main`) and save
the output in the session scratchpad, not the repo. Also run
`td.PurchaseHarness` (`-Dexec.args="2 12 sniper"`, then the same for `mortar`, `cinder` and `aura`)
and save that. Each phase's "Done when" compares against these.

---

## Phase 1: Make the stat core generic over its stat key

**Goal:** the pieces both sides share become generic. No behaviour change.

### Steps

1. Add `td/stat/StatKey.java`:

   ```java
   /** A number on a stat sheet: the base it has unless content authors another, and the range it is clamped to. */
   public interface StatKey {
       float defaultBase();

       float clamp(float value);
   }
   ```

   `EnemyStat implements StatKey`; add `@Override` to its existing `defaultBase()` and `clamp()`.

2. Make `StatModifiers` generic: `public final class StatModifiers<S extends Enum<S> & StatKey>`.
   - Fields: `private final List<S> stats;` (ascending ordinal) and `private final StatModifier[] modifiers;`
     (parallel to `stats`).
   - One shared empty instance:
     `@SuppressWarnings({"rawtypes", "unchecked"}) private static final StatModifiers NONE = new StatModifiers(List.of(), new StatModifier[0]);`
     and `@SuppressWarnings("unchecked") public static <S extends Enum<S> & StatKey> StatModifiers<S> none()`
     returning it.
   - Keep `of(S, StatModifier)`, `and(S, StatModifier)`, `plus(StatModifiers<S>)`, `get(S)`,
     `applyTo(StatAccumulator<S>)`, `equals`, `hashCode`, `toString`. Merging uses a
     `TreeMap<S, StatModifier>` (enums sort by ordinal) in place of the `EnumMap`.
   - Add `public List<S> stats()` (returns `this.stats`, which is already unmodifiable) and
     `public boolean isNone()` (no stats).
   - `get` and `applyTo` loop over the index, so they allocate nothing.
3. Make `StatAccumulator<S extends Enum<S> & StatKey>` and `StatContributor<S ...>` generic: every
   `EnemyStat` parameter becomes `S`. `StatSheet`'s inner accumulator implements
   `StatAccumulator<EnemyStat>`, and `StatSheet`'s constructor takes `StatContributor<EnemyStat>`.
   `StatSheet` and `StatView` otherwise stay `EnemyStat`-only.
4. Make `BaseStats<S extends Enum<S> & StatKey>` generic.
   - `public static <S ...> BaseStats<S> defaults(Class<S> type)` builds the default array from
     `type.getEnumConstants()`. Delete the no-argument `defaults()`.
   - Store `float[] values` and a parallel `boolean[] set`. `with(S, float)` copies both and marks the
     stat set. Keep `value(S)`. Add `public boolean isSet(S stat)`.
   - `equals`/`hashCode` cover both arrays.
5. Add `td/stat/ResolvedStats.java`:

   ```java
   /** Every stat of one key type resolved once, immutable; the snapshot form of a stat sheet. */
   public final class ResolvedStats<S extends Enum<S> & StatKey> {
       private final float[] values;

       private ResolvedStats(float[] values) {
           this.values = values;
       }

       public static <S extends Enum<S> & StatKey> ResolvedStats<S> resolve(BaseStats<S> base, StatModifiers<S> modifiers,
                                                                          Class<S> type) {
           S[] keys = type.getEnumConstants();
           float[] values = new float[keys.length];
           for (S key : keys) {
               values[key.ordinal()] = key.clamp(modifiers.get(key).applyTo(base.value(key)));
           }
           return new ResolvedStats<>(values);
       }

       public float value(S stat) {
           return this.values[stat.ordinal()];
       }
   }
   ```

6. Fix the compile errors: `StatModifiers` → `StatModifiers<EnemyStat>`, `StatAccumulator` →
   `StatAccumulator<EnemyStat>`, `BaseStats.defaults()` → `BaseStats.defaults(EnemyStat.class)`, in
   `td.enemy`, `td.effect`, `td.ui` and tests.

### Tests

- New `td/stat/ResolvedStatsTest`:
  - `resolvesLikeTheEnemySheetForTheSameModifiers`: one bundle with a flat, a percent, a multiply and
    a set value on `EnemyStat`s; resolve it with `ResolvedStats` and with a `StatSheet` whose
    contributor applies the same bundle; every stat is equal.
  - `aSetValueWinsAndIsClamped`.
  - `aStatNoModifierTouchesKeepsItsDefault`.
- New `td/stat/BaseStatsTest`: `onlyAuthoredStatsAreSet`.
- New `td/stat/StatModifiersTest`: `plusMergesStatByStat`, `noneIsTheIdentityOnBothSides`.
- `StatModifierTest` and `StatSheetTest` compile with the type argument and keep passing unchanged.

### Docs

`td/stat/CLAUDE.md`, first bullet becomes:

```markdown
- Neutral ground for stats: depends only on `td.damage` (and `td.util` annotations), so `effect`,
  `enemy` and `tower` can all use it without knowing each other. The core is generic over a
  `StatKey` enum (`EnemyStat`, `TowerStat`): `StatModifier(s)`, `BaseStats`, `StatAccumulator`.
  `StatSheet`/`StatView` stay enemy-only, cached and allocation-free; `ResolvedStats` is the
  immutable form for snapshots.
```

### Done when

`mvn verify` is green and the `BalanceHarness` output is identical to the baseline.

---

## Phase 2: Replace TowerBuff with a tower stat sheet and additive attack speed

**Goal:** towers resolve stats with the shared formula; attack speed adds (D2); disruption becomes
ordinary modifiers; an aura's emission is a `StatModifiers` bundle; a change to any tower's own stats
refreshes everyone's received emissions in the same tick.

### 2.1 Rename the display enum

Rename `td.tower.TowerStat` (the info-panel rows: `RANGE, PHYSICAL_DAMAGE, MAGIC_DAMAGE, FIRE_RATE,
ROTATION, CRIT_CHANCE, SPLASH_RADIUS, PROJECTILE_SPEED, PROJECTILE_SIZE`) to `td.tower.TowerStatRow`
across main and test code, file name included. `TowerStatLine` holds a `TowerStatRow`.
`TowerSheetText.statRow` switches on `TowerStatRow`. Constants are unchanged.

### 2.2 The new key enum

Add `td/tower/TowerStat.java`:

```java
/** One number on a tower's stat sheet, with its default, its clamp, and how a bonus to it reads. */
public enum TowerStat implements StatKey {
    ...constants...;

    private final float defaultBase;
    private final float min;
    private final float max;
    private final Unit unit;
    private final String label;
    private final boolean universal;

    TowerStat(float defaultBase, float min, float max, Unit unit, String label, boolean universal) { ... }

    public float defaultBase() / clamp(float) / Unit unit() / String label()
    /** Whether any tower has this stat, so an aura or a status may change it on any tower. */
    public boolean universal()
}
```

Phase 2 constants (all universal):

| Constant | Default | Min | Max | Unit | Label |
|---|---|---|---|---|---|
| `DAMAGE` | 0 | 0 | MAX | `DAMAGE` | Damage |
| `RANGE` | 0 | 0.5 | MAX | `CELLS` | Range |
| `ATTACK_SPEED` | 0 | −0.75 | 9 | `PERCENT` | Attack speed |
| `CRIT_CHANCE` | 0 | 0 | 1 | `PERCENT` | Crit chance |
| `CRIT_MULTIPLIER` | `AttackProfile.DEFAULT_CRIT_MULTIPLIER` | 1 | MAX | `PERCENT` | Crit damage |
| `ARMOR_PENETRATION` | 0 | 0 | 1 | `PERCENT` | Armor penetration |
| `ARMOR_PENETRATION_FLAT` | 0 | 0 | MAX | `POINTS` | Armor ignored |
| `MAGIC_PENETRATION` | 0 | 0 | 1 | `PERCENT` | Magic penetration |
| `PLATING_PENETRATION` | 0 | 0 | 1 | `PERCENT` | Plating penetration |
| `BOUNTY` | 0 | 0 | MAX | `PERCENT` | Bounty |
| `EFFECT_DURATION` | 1 | 0 | MAX | `PERCENT` | Effect duration |
| `BUFF_DURATION` | 1 | 0 | MAX | `PERCENT` | Buff duration |
| `DISRUPTION_TAKEN` | 1 | 0 | 1 | `PERCENT` | Disruption taken |
| `WITHER_INTERVAL` | 0 | 0 | MAX | `TICKS` | Hits apply Vulnerable every |

`MAX` is `Float.MAX_VALUE`. `DAMAGE` is in `DamageUnits` (hundredths), like today's
`TowerBaseStats.damage`. `WITHER_INTERVAL` is only ever set (`setTo`), so two sources keep the
shorter interval and 0 means never, exactly like `TowerBuff.combine` today.

`TowerStat.Unit` is a nested public enum that formats bonus text. Do not import `td.ui` into
`td.tower`; the unit formats itself:

```java
public enum Unit {
    DAMAGE, CELLS, PERCENT, POINTS, TICKS, COUNT, RADIANS, MULTIPLE, DECIMAL;

    /** A flat change, signed: "+6", "+0.6", "+25%", "+30", "+1.5 s", "+2", "+12°", "+x2", "+0.5". */
    public String signed(float delta) {
        return (delta < 0f ? "-" : "+") + this.plain(Math.abs(delta));
    }

    /** A value as it reads on its own: "6", "0.6", "25%", "30", "1.5 s", "2", "12°", "x2", "0.5". */
    public String plain(float value) {
        return switch (this) {
            case DAMAGE -> number(DamageUnits.inPoints(Math.round(value)));
            case CELLS, DECIMAL -> number(value);
            case PERCENT -> Math.round(value * 100) + "%";
            case POINTS, COUNT -> Integer.toString(Math.round(value));
            case TICKS -> number(value / TickRate.TICKS_PER_SECOND) + " s";
            case RADIANS -> Math.round(Math.toDegrees(value)) + "°";
            case MULTIPLE -> "x" + number(value);
        };
    }

    private static String number(float value) {
        return BigDecimal.valueOf(Math.round(value * 100) / 100.0).stripTrailingZeros().toPlainString();
    }
}
```

### 2.3 Content factories

Add `td/tower/TowerModifiers.java`, a final class with a private constructor and static factories
returning `StatModifiers<TowerStat>`. Combine them with `plus`.

| Factory | Modifier |
|---|---|
| `damagePercent(float fraction)` | `DAMAGE` percent |
| `rangePercent(float fraction)` | `RANGE` percent |
| `attackSpeed(float fraction)` | `ATTACK_SPEED` flat |
| `critChance(float fraction)` | `CRIT_CHANCE` flat |
| `critMultiplier(float bonus)` | `CRIT_MULTIPLIER` flat |
| `armorPenetration(float fraction)` | `ARMOR_PENETRATION` flat |
| `armorPenetrationFlat(float points)` | `ARMOR_PENETRATION_FLAT` flat |
| `magicPenetration(float fraction)` | `MAGIC_PENETRATION` flat |
| `platingPenetration(float fraction)` | `PLATING_PENETRATION` flat |
| `bounty(float fraction)` | `BOUNTY` flat |
| `effectDuration(float fraction)` | `EFFECT_DURATION` flat |
| `buffDuration(float fraction)` | `BUFF_DURATION` flat |
| `disruptionTaken(float factor)` | `DISRUPTION_TAKEN` multiply |
| `witherEvery(int ticks)` | `WITHER_INTERVAL` set to `ticks` |

Add to `StatModifiers`:

```java
/** Every flat and percent amount {@code factor} times as big; multipliers and set values stay. */
public StatModifiers<S> scaledBy(float factor)

/** The flat amount on {@code stat}, 0 when this bundle leaves it alone. */
public float flat(S stat)
```

### 2.4 `TowerBaseStats`

Rewrite as `public record TowerBaseStats(BaseStats<TowerStat> stats, int coolDownMax)`:

- Keep the constructor `TowerBaseStats(float damagePoints, float range, int coolDownMax)`: it sets
  `DAMAGE` (units, through `DamageUnits.ofPoints`) and `RANGE` on `BaseStats.defaults(TowerStat.class)`.
- `withCritChance(float)` and `withCritMultiplier(float)` set `CRIT_CHANCE` / `CRIT_MULTIPLIER`.
- Add `public TowerBaseStats with(TowerStat stat, float value)` and `public float value(TowerStat stat)`.
- Keep the accessors `damage()` (int units), `range()`, `critChanceBase()`, `critMultiplier()`,
  computed from `stats`.

### 2.5 `TowerStats`

Rewrite as:

```java
public record TowerStats(ResolvedStats<TowerStat> values, float rangeReal, float rangeReal2, AttackProfile attack,
                         DisruptionPenalty disruption, float reachReal, StatModifiers<TowerStat> received) {

    public static TowerStats of(TowerBaseStats base, StatModifiers<TowerStat> own, StatModifiers<TowerStat> received,
                                DisruptionPenalty disruption, int scale)

    public float value(TowerStat stat)
    public int damage()              // Math.round(value(DAMAGE))
    public float range()             // value(RANGE), in cells
    public double attackRate()       // 1.0 + value(ATTACK_SPEED); 1 is the base rate
    public float critChance()        // value(CRIT_CHANCE)
    public float bountyBonus()       // value(BOUNTY)
    public float effectLength()      // value(EFFECT_DURATION)
    public float timedBuffLength()   // value(BUFF_DURATION)
    public int witherTicks()         // Math.round(value(WITHER_INTERVAL))
}
```

`of` does, in order:

1. `undisrupted = ResolvedStats.resolve(base.stats(), own.plus(received), TowerStat.class)`.
2. `taken = undisrupted.value(DISRUPTION_TAKEN)`.
3. `values = ResolvedStats.resolve(base.stats(), own.plus(received)
   .plus(TowerModifiers.attackSpeed(-disruption.attackSpeed() * taken))
   .plus(TowerModifiers.range(-disruption.rangeCells() * taken)), TowerStat.class)`.
   (`TowerModifiers.range(cells)`, a flat `RANGE` modifier, is added in this phase for this use.)
4. `rangeReal = values.value(RANGE) * scale`; `rangeReal2` its square;
   `reachReal = undisrupted.value(RANGE) * scale`.
5. `attack = AttackProfile.critChance(value(CRIT_CHANCE)).withCritMultiplier(value(CRIT_MULTIPLIER))
   .withArmorPenetration(value(ARMOR_PENETRATION), value(ARMOR_PENETRATION_FLAT))
   .withMagicPenetration(value(MAGIC_PENETRATION), 0f).withPlatingPenetration(value(PLATING_PENETRATION))`.

### 2.6 Disruption

- `DisruptionAura(float radius, float attackSpeedPenalty, float rangePenaltyCells)` (rename the last
  two components). The Jammer becomes `new DisruptionAura(100f, 0.3f, 0.6f)`.
- `DisruptionPenalty(float attackSpeed, float rangeCells)` (rename both). Keep `none`, `plus`,
  `isNone`. Update `DisruptionField.penaltyAt`.

### 2.7 `AbstractTower`

- Remove every `TowerBuff` use and the `td.tower.buff` import.
- `private volatile TowerBuff timedBuff` becomes
  `private volatile StatModifiers<TowerStat> timedModifiers = StatModifiers.none();`.
- The constructor's initial stats: `TowerStats.of(base, StatModifiers.none(), StatModifiers.none(),
  DisruptionPenalty.none(), scale)`.
- `publishStats(UpgradeState upgrades)`:

  ```java
  StatModifiers<TowerStat> received = this.context.towers().all().stream()
          .map(t -> t.buffFor(this))
          .reduce(StatModifiers.none(), StatModifiers::plus);
  StatModifiers<TowerStat> own = upgrades.totalModifiers().plus(this.timedModifiers);
  this.stats = TowerStats.of(this.baseStats, own, received, this.disruption, this.context.getBoard().scale());
  ```

- Rename `fireRateCurrent()` to `attackRate()`, returning `this.stats.attackRate()`. Every
  `this.clock.advance(this.fireRateCurrent())` becomes `this.clock.advance(this.attackRate())`.
- `dealDamage`: the bounty bonus is `this.stats.bountyBonus()`.
- `wither` reads `this.stats.witherTicks()` (unchanged name on the new record).
- `applyEffect`/`lengthened` read `this.stats.effectLength()`; `timedBuffLength()` reads
  `this.stats.timedBuffLength()` (unchanged names).
- `receiveTimedBuff(TowerBuff, int)` becomes `receiveTimedModifiers(StatModifiers<TowerStat>, int)`
  and `grantTimedBuff` becomes `grantTimedModifiers`, same rules.
- `beginTick(int gameTime)` now **returns `boolean`**: whether this tower republished its stats
  (timed modifiers ended, or the disruption changed). The expiry check is
  `!this.timedModifiers.isNone() && gameTime >= this.timedBuffEndsAt`.
- `cadence()`: base `TICKS_PER_SECOND / (this.coolDownMax + 1)`, current base × `attackRate()`.
- `buffFor(Tower other)` returns `StatModifiers<TowerStat>`, `StatModifiers.none()` by default.
- `inspect()` counts auras with `!other.buffFor(this).isNone()`.

### 2.8 `Tower`, roster and engine

- `Tower`: `StatModifiers<TowerStat> buffFor(Tower other)`; `void receiveTimedModifiers(StatModifiers<TowerStat> modifiers,
  int durationTicks)`; `boolean beginTick(int gameTime)` with its Javadoc saying it returns whether
  the stats were republished.
- `GameEngine`, towers phase (today lines ~260-262):

  ```java
  List<Tower> towers = this.gameWorld.towers().all();
  boolean changed = false;
  for (Tower tower : towers) {
      changed |= tower.beginTick(time);
  }
  if (changed) {
      towers.forEach(Tower::recalculateStats);
  }
  for (Tower tower : towers) {
      tower.doTick(time);
  }
  ```

  Every tower's turn now starts after every tower's stats are settled, so a disrupted aura stops
  buffing a tower it no longer reaches in the same tick.
- `ExperienceAwarder` and `BuffedTowersCondition`: `!tower.buffFor(x).equals(TowerBuff.none())`
  becomes `!tower.buffFor(x).isNone()`.

### 2.9 Upgrade nodes

Add `td/tower/upgrade/NodeEffects.java`:

```java
/** What an upgrade node does when bought, as data, and the one row of words for what data can't say. */
public record NodeEffects(StatModifiers<TowerStat> modifiers, String note) {

    private static final NodeEffects NONE = new NodeEffects(StatModifiers.none(), "");

    public static NodeEffects none()
    /** This, plus {@code more}: modifiers add up. */
    public NodeEffects withModifiers(StatModifiers<TowerStat> more)
    public NodeEffects withNote(String note)
    /** Every row this node shows in the Upgrades panel: one per stat changed, then the note. */
    public List<UpgradeBonus> bonuses()
}
```

`bonuses()` builds, for each stat in `modifiers.stats()` (ordinal order), one `UpgradeBonus(label,
value)`:

- `label` is `stat.label()`;
- `value` is, when the modifier has a set value, `stat.unit().plain(setTo)`; otherwise the non-empty
  parts joined with `", "`: the flat as `stat.unit().signed(flat)`, the percent as a signed whole
  percent (`"+30%"`), and the multiply as a signed whole percent of `multiply - 1` (`"-50%"`).

Then, when `note` is not empty, one row with the note's first letter upper-cased and an empty value
(today's extra-effect row).

`UpgradeNode` becomes `record UpgradeNode(String id, UpgradeSlot slot, String displayName, int price,
NodeEffects effects, UpgradeCondition requires, UpgradeCondition gate, int xp)`:

- `of(...)` uses `NodeEffects.none()`.
- `withBuff(TowerBuff)` becomes `withModifiers(StatModifiers<TowerStat>)` (adds through
  `effects.withModifiers`).
- `withExtraEffect(String)` becomes `withNote(String)`; replace every call in main and test code.
- Add `withEffects(NodeEffects effects)` (replaces the whole value).
- `bonuses()` returns `this.effects.bonuses()`. Delete `statBonus()`, `extraEffect()` and
  `addIfNonZero`.

`UpgradeState.totalBuff()` becomes `totalModifiers()`: the `plus`-fold of every owned node's
`effects().modifiers()`.

### 2.10 Content conversion (percent semantics kept this phase)

| Today | Phase 2 |
|---|---|
| `TowerBuff.damage(f)` | `TowerModifiers.damagePercent(f)` |
| `TowerBuff.range(f)` | `TowerModifiers.rangePercent(f)` |
| `TowerBuff.fireRate(f)` | `TowerModifiers.attackSpeed(f)`, literal (D2) |
| Cinder Dragon's Breath `withFireRate(2f / 3f)` | `attackSpeed(2f)` (D2 exception: it stays three times as fast) |
| `TowerBuff.critChance(f)` | `TowerModifiers.critChance(f)` |
| `a.withX(b)` chains | `a.plus(TowerModifiers.x(b))` |
| `StandardBaseSlot` `TowerBuff.range(RANGE_BONUS)` etc. | `TowerModifiers.rangePercent(...)` |

### 2.11 Tower leaves

- **Aura.** `buff()`/`buffWithStrength(float strength)` return `StatModifiers<TowerStat>`:

  ```java
  StatModifiers<TowerStat> buff = TowerModifiers.damagePercent(strength).plus(TowerModifiers.rangePercent(strength))
          .plus(TowerModifiers.disruptionTaken(DISRUPTION_LEFT));
  if (broadcasting) {
      buff = buff.plus(TowerModifiers.disruptionTaken(BROADCAST_DISRUPTION_LEFT)).plus(TowerModifiers.buffDuration(1f));
  }
  if (this.owns(CONDUIT)) { buff = buff.plus(TowerModifiers.effectDuration(CONDUIT_EFFECT_LENGTH)); }
  if (this.owns(KEEN_EDGE)) { buff = buff.plus(TowerModifiers.critMultiplier(KEEN_EDGE_CRIT_DAMAGE)); }
  if (this.owns(WITHERING_FIELD)) { buff = buff.plus(TowerModifiers.witherEvery(WITHERING_FIELD_TICKS)); }
  return this.grantsFireRate ? buff.plus(TowerModifiers.attackSpeed(CORE_FIRE_RATE)) : buff;
  ```

  Rename `DISRUPTION_SHIELD = 0.5f` to `DISRUPTION_LEFT = 0.5f` and replace
  `BROADCAST_DISRUPTION_SHIELD = 0.75f` with `BROADCAST_DISRUPTION_LEFT = 0.5f` (one aura's half
  times Broadcast II's half leaves the same quarter as today). `buffFor` keeps
  `scaledBy(CHOSEN_STRENGTH)`, now `StatModifiers.scaledBy`, which leaves the disruption factor and
  the wither interval alone, as `TowerBuff.scaledBy` did. Rally calls
  `buffed.receiveTimedModifiers(TowerModifiers.attackSpeed(RALLY_FIRE_RATE), RALLY_TICKS)`. Its
  `behaviours()` "Nearby fire rate" row becomes "Nearby attack speed". The `owns` checks stay until
  phase 3.
- **Cinder.** `fireRateFromAuras()` returns `Math.max(0f, this.stats().received().flat(TowerStat.ATTACK_SPEED))`.
- **Sniper.** `SniperShot.fireRateBonus` becomes `attackSpeedBonus`; `withFireRateBonus(b)` becomes
  `withAttackSpeedBonus(b)`, which adds. `SteadyTempoPerk` passes `0.25f`. `SniperTempo`:
  `DOUBLE_SPEED = 0.5f` becomes `HASTE = 1f`, `takeFireRateBonus` becomes `takeAttackSpeedBonus`.
  `fire()` ends with:

  ```java
  double rate = this.attackRate();
  double bonus = shot.attackSpeedBonus() + this.tempo.takeAttackSpeedBonus(this.currentTick);
  this.clock.spend(rate / (rate + bonus));
  ```

  (A shot's wait is one period at `rate + bonus` instead of `rate`.)
- **Sonar.** `spec()` divides seconds per revolution by `this.attackRate()`.
- **Every other tower:** only the renames above.

Delete the `td.tower.buff` package (`TowerBuff`, `TowerBuffTest`). `td.fixtures.FakeTower.buffFor`
returns `StatModifiers<TowerStat>`.

### 2.12 Balance changes this phase makes (expected, not bugs)

| Case | Rate before | Rate after |
|---|---|---|
| One +25% node (Marksman's Eye II) | ×1.33 | ×1.25 |
| Seeker Twin Warhead +30% | ×1.43 | ×1.30 |
| Cinder Wide Nozzle II +20% | ×1.25 | ×1.20 |
| Mortar Heavy Shell −20% | ×0.83 | ×0.80 |
| Aura Amplifying Core II +10% | ×1.11 | ×1.10 |
| Rally +30% | ×1.43 | ×1.30 |
| Frenzy, Momentum burst ("twice as fast") | ×2 | ×2 alone; adds with other bonuses |
| Steady Tempo (Focused Optics) | wait ×0.75 | wait ×0.80 |
| Jammer | rate ×0.77, range −20% | rate ×0.70, range −0.6 cells |
| Several bonuses together | multiply | add |

### Tests

- New `td/tower/TowerStatsTest`:
  - `attackSpeedBonusesAdd`;
  - `attackSpeedStaysBetweenAQuarterAndTenTimesTheBaseRate`;
  - `disruptionLowersRangeByCellsAndAttackSpeedByWhatTheAurasLeave`;
  - `disruptionTakenMultipliesAcrossAuras` (ported from `TowerBuffTest.disruptionShieldsMultiply…`);
  - `twoWitheringSourcesKeepTheShorterInterval` (ported from `combineKeepsTheShorterWitherInterval`);
  - `rangeNeverDropsBelowHalfACell`;
  - `anAuraBuffAndADisruptionPenaltyAddUp` and `reachForXpKeepsTheBuffedRangeAndIgnoresTheDisruption`
    (ported).
- New `td/tower/TowerModifiersTest`: `percentFactoriesArePercentAndTheRestAreFlat`,
  `witherEverySetsTheInterval`.
- `StatModifiersTest`: `scaledByMultipliesAmountsButNotFactorsOrSetValues` (ported from
  `scaledByMultipliesEveryAmountButNotTheShieldOrTheInterval`).
- New `GameEngineTest` tests: `aDisruptedAuraStopsBuffingATowerItNoLongerReachesInTheSameTick` and
  `aJammerSlowsAPulsesHitsByThirtyPercent`.
- Changed:
  - `FireRateTest.aTenPercentBuffedPulseHitsTwentyTwoPointTwoTimesPerTwentyTicks` →
    `aTenPercentFasterPulseHitsTwentyTwoTimesPerTwentyTicks` (rate 1.1); recompute
    `aBuffedPulseHitsTwiceInATickOnceTheRememberedFractionsAddUp` and `aJammedPulseHitsLessOften` with
    the new rate; rename `aSonarSpinsFasterByTheFireRateBuff` → `aSonarSpinsFasterByItsAttackSpeed`.
  - `AbstractTowerTest.aBoughtNodesFireRateBonusRaisesTheFireRate` →
    `aBoughtNodesAttackSpeedRaisesTheRate`, using `TowerModifiers.attackSpeed`.
  - `AuraTowerTest`: rename `amplifyingCoreIiAlsoGrantsAFireRateBonus` →
    `amplifyingCoreIiAlsoGrantsAnAttackSpeedBonus`,
    `amplifyingCoreAddsATenthToTheBuffPerLevelAndTheSecondAddsTenPercentFireRate` →
    `…TenPercentAttackSpeed`, and `anEnemyOnTheRallySpotGivesBuffedTowersMoreFireRate…` →
    `…MoreAttackSpeed…`; update their expected rates.
  - Sniper tests that assert a wait after Frenzy, Momentum or Steady Tempo: recompute with
    `rate / (rate + bonus)`.
  - `UpgradeNodeTest`, `UpgradeSheetTextTest`, `TowerSheetTextTest`: "Fire rate +25%" bonus rows
    become "Attack speed +25%". The info-panel row stays "Fire rate" (it shows attacks per second).
  - Every other `TowerBuff` use in tests moves to `TowerModifiers` by the table in 2.10.

### Docs

- Root `CLAUDE.md`:
  - Packages line: `tower` (+ `targeting`, `upgrade`, `sniper`, `sonar`, `splash`): drop `buff`.
  - Code style, algebra examples: `(Damage, StatModifiers, TargetQuery)`.
- `td/tower/CLAUDE.md`: replace the whole "Stats and buffs" section with:

  ```markdown
  ## Stats and buffs

  - A tower's stats are `TowerStat` keys resolved by the shared `td.stat` formula into one immutable
    `TowerStats` (correlated fields, read by the tick thread). Read them through `stats()` or the
    conveniences (`damageCurrent()`, `rangeReal()`, `rangeReal2()`, `attackRate()`).
  - Layers: base, owned nodes (`UpgradeState.totalModifiers`), timed modifiers, then received
    emissions (`buffFor`), then disruption × `DISRUPTION_TAKEN`. An aura never receives an emission.
  - What a tower receives is **derived, never stored**: a fold of every tower's `buffFor(this)`.
    `recalculateStats()` runs on every tower whenever the roster or any upgrade changes, and in the
    towers phase whenever any tower's `beginTick` republished; `TowerRoster.clear()` needn't.
  - Author bonuses with `TowerModifiers`. Attack speed is a fraction that adds:
    `rate = 1 + Σ`, held to `[-0.75, 9]`. Every tower's cadence is its `FireClock` advanced by
    `attackRate()`; the Sonar divides its seconds per revolution by it.
  - Enemy disruption reaches a tower only through `GameWorld.disruptions()`, sampled at its centre in
    `beginTick`, and folds in as negative attack speed and range in cells, times `DISRUPTION_TAKEN`.
  - A kill this tower makes runs `onKill(EnemyMob)` (the mob still carries the effects it died
    under); a kill made from inside the hook triggers nothing. A temporary self-buff goes through
    `grantTimedModifiers` and ends in `beginTick`; it never stacks.
  - How long a tower's effects and timed buffs last is a stat (`EFFECT_DURATION`, `BUFF_DURATION`).
    Every effect goes through `applyEffect`, which scales it; what a tower leaves on the ground takes
    its lifetime through `lengthened(ticks)`.
  - `dealDamage` sends the hit with `TowerStats.attack()`; a special shot passes a one-off profile.
    Damage that ticks goes through `dealPeriodicDamage` and never crits. The hit that starts a pool
    scales it by `potencyOfHit`.
  ```

  Also in that file: the `FireClock` bullet's example becomes "a 10% faster Pulse hits 22 times in 20
  ticks"; "(fire-rate bonuses …)" wording anywhere becomes attack speed; the Bellows sentence says it
  reads the attack speed in what the tower receives.
- `docs/ARCHITECTURE.md` §7: replace the `td.tower.buff.TowerBuff` bullet with: "**`td.stat.StatModifiers`**
  is the bonus algebra on both sides: `plus`, identity `none()`, and `scaledBy`. A tower's bonuses are
  `StatModifiers<TowerStat>` bundles; each aura's emission is its own bundle, so auras of different
  strengths stack."
- `README.md`: in the towers table, a bonus called "fire rate" becomes "attack speed".

### Done when

`mvn verify` is green, `PerformanceHarness` passes, and `BalanceHarness` differs from the baseline
only where 2.12 predicts.

---

## Phase 3: Turn tower numbers into stats, author upgrades in absolute values and make the Aura data

**Goal:**
- Every number a node changes is a `TowerStat` (D14), authored flat (D4).
- Perks that only changed such a number are deleted; specs lose those fields.
- The Aura follows the same perk/spec pattern as every other tower: no `owns(...)`, no mutated
  fields; nodes add to what it emits.

### 3.1 New stats

Add to `TowerStat`, all `universal = false`:

| Constant | Default | Min | Max | Unit | Label |
|---|---|---|---|---|---|
| `DEAD_ZONE` | 0 | 0 | MAX | `CELLS` | Dead zone |
| `AREA_RADIUS` | 0 | 0 | MAX | `CELLS` | Blast radius |
| `BLAST_EDGE` | 0 | 0 | 1 | `PERCENT` | Blast edge damage |
| `SATURATION_CAP` | 0 | 0 | MAX | `COUNT` | Saturation cap |
| `SATURATION_INNER_STACKS` | 0 | 0 | MAX | `COUNT` | Saturation in the inner half |
| `PROJECTILE_SPEED` | 1 | 0.1 | MAX | `PERCENT` | Shell speed |
| `PROJECTILE_SIZE` | 1 | 0.1 | MAX | `PERCENT` | Shell size |
| `BRACKET_DAMAGE_STEP` | 0 | 0 | MAX | `PERCENT` | Bracketing step |
| `CONE_WIDTH` | 0 | 0 | 2π | `RADIANS` | Cone width |
| `WAVE_SPEED` | 1 | 0.1 | MAX | `PERCENT` | Wave speed |
| `BURN_DURATION` | 0 | 1 | MAX | `TICKS` | Burn duration |
| `STOKE_STEP` | 0 | 0 | MAX | `PERCENT` | Stoke step |
| `STOKE_GRACE` | 0 | 0 | MAX | `TICKS` | Stoke kept after leaving |
| `BURN_POOL_CAP` | 0 | 1 | MAX | `MULTIPLE` | Burn pool cap |
| `BURN_MARK_RATE` | 1 | 1 | MAX | `PERCENT` | Scorched rate |
| `TOLL_CAP` | 0 | 0 | MAX | `COUNT` | Toll cap |
| `TOLL_RATE` | 0 | 0 | MAX | `DECIMAL` | Toll a second |
| `TOLL_LINGER` | 0 | 0 | MAX | `TICKS` | Toll lingers |
| `FREEZE_DURATION` | 0 | 0 | MAX | `TICKS` | Freeze duration |
| `NEST_CAPACITY` | 0 | 0 | MAX | `COUNT` | Nest |
| `MISSILES_PER_LAUNCH` | 1 | 1 | MAX | `COUNT` | Missiles a shot |
| `PING_COUNT` | 0 | 0 | MAX | `COUNT` | Pings |
| `AIM_STACKS` | 0 | 0 | MAX | `COUNT` | Steady Aim stacks |
| `AURA_POWER` | 0 | 0 | MAX | `PERCENT` | Buff strength |

Count stats are read with `Math.round(value)`. `CONE_WIDTH` is the cone's full width; the Cinder's
half width is half of it.

Add flat factories to `TowerModifiers`: `damage(float points)` (through `DamageUnits.ofPoints`),
`range(float cells)` (added in phase 2), `deadZone(float cells)`, `areaRadius(float cells)`,
`blastEdge(float fraction)`, `saturationCap(int)`, `saturationInnerStacks(int)`,
`projectileSpeed(float)`, `projectileSize(float)`, `bracketDamageStep(float)`, `coneWidth(float radians)`,
`waveSpeed(float)`, `burnDuration(int ticks)`, `stokeStep(float)`, `stokeGrace(int ticks)`,
`burnPoolCap(float)`, `burnMarkRate(float)`, `tollCap(int)`, `tollRate(float)`, `tollLinger(int ticks)`,
`freezeDuration(int ticks)`, `nestCapacity(int)`, `missilesPerLaunch(int)`, `pingCount(int)`,
`aimStacks(int)`, `auraPower(float)`.

### 3.2 Node effects gain an emission

`NodeEffects` becomes `(StatModifiers<TowerStat> modifiers, StatModifiers<TowerStat> emission, String note)`
with `withEmission(StatModifiers<TowerStat> more)` (adds). `bonuses()` lists, after the modifier rows,
one row per emitted stat with the label `"Nearby " + label with its first letter lower-cased` and the
same value rules. `UpgradeNode` gains `withEmission(...)` delegating. `UpgradeState` gains
`totalEmission()`, the `plus`-fold of every owned node's emission.

### 3.3 The base slot

`BaseSlotPerks` becomes `record BaseSlotPerks(NodeEffects attune, NodeEffects awaken, float rangeThreeBonus,
NodeEffects rangeThree)`:

- `none()` → `(NodeEffects.none(), NodeEffects.none(), StandardBaseSlot.RANGE_STEP_BONUS, NodeEffects.none())`.
- Keep `withAttune(String note)`, `withAwaken(String note)` and `withRangeThree(float bonus, String note)`,
  now storing the note in the matching `NodeEffects`.
- Add `withAttune(NodeEffects)`, `withAwaken(NodeEffects)`, `withRangeThree(float bonus, NodeEffects rangeThree)`.

`StandardBaseSlot.nodes` takes the tower's base range:
`nodes(int listPrice, float baseRange, UpgradeNode... levelThrees)` and
`nodes(int listPrice, float baseRange, BaseSlotPerks perks, UpgradeNode... levelThrees)`.

- Range I: `withModifiers(TowerModifiers.range(RANGE_BONUS * baseRange))`.
- Range II: `range(RANGE_STEP_BONUS * baseRange)`.
- Attune: `withEffects(perks.attune().withNote(text))`, where `text` is built from
  `perks.attune().note()` exactly as today's attune text.
- Awaken: the same with `perks.awaken()`.
- Range III: `withEffects(perks.rangeThree().withModifiers(TowerModifiers.range(perks.rangeThreeBonus() * baseRange)))`.

Every tower passes its own `RANGE` constant. Each range node alone is unchanged.

### 3.4 Conversions, tower by tower

Notation: `+X` on a stat means `TowerModifiers.<stat factory>(X)` on the node. A deleted perk is
removed from its tower's `PerkCatalogue` and its file is deleted. "Note: …" gives the node's note
after this phase; "Note: none" means `withNote` is removed, because the derived rows now say it.
Nodes not listed keep their effects and notes.

#### Sniper (base: damage 40, range 4.0)

Base stats: `.with(TowerStat.DEAD_ZONE, 0f).with(TowerStat.AIM_STACKS, 1f)`.

| Node | Effects | Perks | Note |
|---|---|---|---|
| Range III | `withRangeThree(0.35f, NodeEffects.none().withModifiers(deadZone(2f)))` | delete `OverwatchPerk` | none |
| Focused Optics II | `damage(16)` | — | unchanged |
| Focused Optics III | `damage(12)` | — | unchanged |
| Marksman's Eye | `aimStacks(2)` | delete `SteadyAimStacksPerk` | none |
| Marksman's Eye II | `attackSpeed(0.25)`, `armorPenetrationFlat(30)` | delete `ArmorPiercePerk` | none |
| Unbroken Aim | `aimStacks(2)` | `UnbrokenAimPerk` keeps only `thatSurvivesAKill()` | "Steady Aim survives a kill; only retargeting a living enemy resets it" |

- `AimRules` becomes `record AimRules(boolean survivesKill)`; `attuned()` returns
  `new AimRules(false)`; delete `withStackCap`.
- `SniperTower.fire` locks with `Math.round(this.stats().value(TowerStat.AIM_STACKS))` as the cap.
- `SniperTower.spec(owned)`: after the perk fold, when `DEAD_ZONE > 0`,
  `spec = spec.withReach(spec.reach().withDeadZone(deadZone * view.cellSize()))`.

#### Splash (base: damage 14, range 3.2, blast 1.5 cells)

Base stats: `AREA_RADIUS 1.5` (`BLAST_RADIUS_CELLS`), `BLAST_EDGE 0`, `SATURATION_CAP 3`,
`SATURATION_INNER_STACKS 1`.

| Node | Effects | Perks | Note |
|---|---|---|---|
| Range III | `withRangeThree(0.1f, NodeEffects.none().withModifiers(areaRadius(0.15f)).withNote("Mines: every 4th blast leaves a mine on the path for 10s, up to 3, that the next enemy to step on it sets off as one blast"))` | delete `BlastRadiusPerk`; `MinesPerk` stays | as given |
| Conductor | `critChance(0.1)` | — | unchanged |
| Wide Charge | `areaRadius(0.375)`, `blastEdge(0.25)` | delete `WideChargePerk` | none |
| Shaped Charge | `saturationInnerStacks(1)`, `saturationCap(1)` | delete `ShapedChargePerk` | none |

- `BlastSpec` becomes `record BlastSpec(boolean aimsAtCrowds, boolean saturates)` with `none()`,
  `aimingAtCrowds()`, `saturating()`. Delete `radiusBonus`, `edgeFloor`, `saturation`,
  `withRadiusBonus`, `withEdgeFloor`, `withSaturation`, `distanceScale()` and the falloff method;
  `FireControlPerk` returns `spec.withBlast(spec.blast().aimingAtCrowds().saturating())`.
- `SaturationRule` keeps `(perHit, innerPerHit, cap)`, `none()`, `isActive()`, `stacksAt`. Delete
  `attuned()`, `withInnerPerHit`, `withCap`.
- `SplashTower` gains:

  ```java
  /** How far every blast distance reaches compared with the base blast. */
  private float distanceScale() {
      return this.stats().value(TowerStat.AREA_RADIUS) / BLAST_RADIUS_CELLS;
  }

  private SaturationRule saturation(SplashSpec spec) {
      return spec.blast().saturates()
              ? new SaturationRule(1, Math.round(this.stats().value(TowerStat.SATURATION_INNER_STACKS)),
                      Math.round(this.stats().value(TowerStat.SATURATION_CAP)))
              : SaturationRule.none();
  }

  /** The share of damage a blast deals at {@code share} of its radius out, floored by its edge. */
  private float falloff(float share) {
      return Math.max(this.stats().value(TowerStat.BLAST_EDGE), 1f - share * share);
  }
  ```

  Replace every `spec.blast().distanceScale()` with `this.distanceScale()`, every
  `spec.blast().saturation()`/`spec.saturation()` with `this.saturation(spec)`, and the old falloff
  call with `this.falloff(share)`. `ownStats()` shows base `BLAST_RADIUS_CELLS` and current
  `AREA_RADIUS`.

#### Mortar (base: damage 32, range 4.5, blast 1.75 cells)

Base stats: `AREA_RADIUS 1.75` (`SPLASH_RADIUS_BASE`), `PROJECTILE_SPEED 1`, `PROJECTILE_SIZE 1`,
`DEAD_ZONE 1.5`, `BRACKET_DAMAGE_STEP 0.1`.

| Node | Effects | Perks | Note |
|---|---|---|---|
| Range III | `withRangeThree(0.3f, NodeEffects.none().withModifiers(deadZone(1f)))` | delete `LongBatteryPerk` | none |
| Siege Rounds | `damage(9.6)`, `bracketDamageStep(0.05)` | delete `BracketDamagePerk` | none |
| Siege Rounds II | `damage(8)`, `critChance(0.1)`, `areaRadius(0.4375)` | delete `WiderBlastPerk` | none |
| Heavy Shell | `damage(16)`, `attackSpeed(-0.2)`, `projectileSpeed(-0.25)`, `projectileSize(0.3)` | `HeavyShellPerk` keeps only the daze | "enemies within 0.5 cells of the impact are Dazed 0.5s" |
| Bunker Buster | `areaRadius(-0.525)` | `BunkerBusterPerk` keeps only the centre | "the enemy at the centre of the blast takes x3 damage and is Sundered" |
| Rifled Barrel | `projectileSpeed(0.4)`, `projectileSize(-0.3)` | delete `RifledBarrelPerk` | none |
| Airburst | `areaRadius(0.4375)` | `AirburstPerk` keeps only `withFlatCore()` | "full damage across the inner half of the blast" |

- `MortarSpec` loses `blastScale`, `speedScale`, `sizeScale` and their `with…ScaledBy` methods;
  `MortarSpec.from(view)` no longer puts a dead zone on the reach; delete `BASE_DEAD_ZONE_CELLS`.
- `BracketSpec` loses `damageStep` and `withDamageStep`; `BracketingPerk` passes
  `BracketSpec.of(RADIUS_CELLS, MAX_STEPS, STEP)` (the radius step).
- `MortarTower`:
  - `spec(owned)`: after the perk fold, `spec.withReach(spec.reach().withDeadZone(DEAD_ZONE * cellSize))`.
  - `blastRadius(spec, stepScale)` uses `this.stats().value(AREA_RADIUS) * scale * stepScale`.
  - `aimPoint` and `shellStats` use `SHELL_SPEED * PROJECTILE_SPEED`, size `× PROJECTILE_SIZE`, and
    `withStreak(PROJECTILE_SPEED > 1f)`.
  - `onImpact` and `behaviours()` use `this.stats().value(BRACKET_DAMAGE_STEP)` where they read
    `spec.bracket().damageStep()`.
  - `ownStats()` shows base `SPLASH_RADIUS_BASE` and current `AREA_RADIUS`.

#### Pulse (base: damage 2, range 1.5)

Base stats: `TOLL_CAP 5`, `TOLL_RATE 1`, `TOLL_LINGER 20`.

| Node | Effects | Perks | Note |
|---|---|---|---|
| Range III | `withRangeThree(0.2f, NodeEffects.none().withModifiers(tollLinger(20)))` | delete `LingeringTollPerk` | none |
| Overcharged Coils | `damage(0.6)`, `tollRate(1)` | delete `FasterTollPerk` | none |
| Overcharged Coils II | `damage(0.5)` | `FullTollBonusPerk` stays | unchanged |
| Arc Discharge | `critChance(0.1)` | — | unchanged |
| Meltdown | `tollCap(5)` | `MeltdownPerk` keeps only the zap's crit bonus and Toll raising | "the zap gets +10% crit, and Toll raises the zap's damage too" |
| Phase Field | `range(0.3)`, `tollLinger(20)` | `LingeringTollPerk` deleted | none |
| Phase Field II | `range(0.225)` | — | unchanged |

- `PulseSpec`'s `TollSpec toll` becomes `boolean tolls`; `TollPerk` returns `spec.tolling()`.
- `TollSpec` keeps `(cap, stacksPerSecond, fadeTicks)`, `none()`, `isActive()`, `ticksPerStack()`;
  delete `attuned()`, `withCap`, `buildingFasterBy`, `lingeringLonger` and the two constants.
- `PulseTower` gains `private TollSpec toll(PulseSpec spec)`: `spec.tolls() ? new TollSpec(round(TOLL_CAP),
  TOLL_RATE, round(TOLL_LINGER)) : TollSpec.none()`, and every `spec.toll()` becomes `this.toll(spec)`.

#### Seeker (base: damage 40, range 4.5)

Base stats: `FREEZE_DURATION 30` (`FREEZE_DURATION_TICKS_BASE`), `NEST_CAPACITY 3`,
`MISSILES_PER_LAUNCH 1`.

| Node | Effects | Perks | Note |
|---|---|---|---|
| Twin Warhead | `attackSpeed(0.3)`, `nestCapacity(1)` | delete `NestGrowthPerk` | none |
| Twin Warhead II | `missilesPerLaunch(1)` | delete `SecondMissilePerk` | "the second missile goes to the next target" |
| Brood | `nestCapacity(2)` | `BroodPerk` keeps only `withSpreadSalvo()` | "a salvo spreads across different targets" |
| Deep Freeze | `freezeDuration(15)` | delete `DeepFreezePerk` | none |
| Deep Freeze II | `damage(12)`, `critChance(0.1)` | `ShatterPerk` stays | unchanged |

- `SeekerSpec`'s `NestSpec nest` becomes `int launchGapTicks` (0 = no nest) and `missilesPerShot`
  is deleted. `NestPerk` sets the gap to `LAUNCH_GAP_TICKS` (4); delete its `CAPACITY`.
- `NestSpec` keeps `(capacity, launchGapTicks)`, `none()`, `of`, `isActive()`; delete `withCapacity`
  and `holdingMore`.
- `FreezeSpec` loses `durationFactor` and `lastingLonger`.
- `SeekerTower` gains `private NestSpec nest(SeekerSpec spec)`: `spec.launchGapTicks() > 0 ?
  NestSpec.of(round(NEST_CAPACITY), spec.launchGapTicks()) : NestSpec.none()`; every `spec.nest()`
  becomes `this.nest(spec)`; the launch loop runs `round(MISSILES_PER_LAUNCH)` times; the freeze
  length is `round(FREEZE_DURATION)` (times a payload's strength where it is today).

#### Sonar (base: damage 16, range 4.5)

Base stats: `PING_COUNT 1`, `DEAD_ZONE 0`.

| Node | Effects | Perks | Note |
|---|---|---|---|
| Rapid Array | `damage(4)`, `pingCount(1)` | delete `TwoPingsPerk` | none |
| Rapid Array II | `damage(4)`, `critChance(0.1)` | — | unchanged |
| Spin-Up | `attackSpeed(0.5)` | delete `SpinUpPerk` | none (3 s / 1.5 = 2 s) |
| Long Reach | `range(0.9)` | — | unchanged |
| Horizon | `range(1.8)`, `deadZone(1.5)` | `HorizonPerk` keeps only the far bonus | "far bonus up to +150%" |
| Pure Tone | `magicPenetration(0.15)` | `PureTonePerk` keeps only the magic type | "the beam deals magic instead of physical" |

- `PingRule.count` stays the on/off of Ping (`PingPerk` sets 1). `SonarTower` gains
  `private PingRule ping(SonarSpec spec)`: `spec.ping().isActive() ?
  spec.ping().withCount(round(PING_COUNT)) : spec.ping()`; every `spec.ping()` becomes `this.ping(spec)`.
- `SonarTower.spec(owned)`: after the perk fold, apply `DEAD_ZONE` to the reach as the Sniper does.

#### Cinder (base: damage 1.5, range 2.2)

Base stats: `CONE_WIDTH 0.7` (twice `HALF_WIDTH_RADIANS_BASE`), `WAVE_SPEED 1`, `BURN_DURATION 60`,
`STOKE_STEP 0.1`, `STOKE_GRACE 0`, `BURN_POOL_CAP 2`, `BURN_MARK_RATE 1`.

| Node | Effects | Perks | Note |
|---|---|---|---|
| Range III | `withRangeThree(0.2f, NodeEffects.none().withModifiers(waveSpeed(1f)))` | delete `LongNozzlePerk` | none |
| White Flame | `damage(0.45)`, `stokeStep(0.05)` | delete `WhiteFlamePerk` | none |
| White Flame II | `damage(0.75)`, `critChance(0.1)`, `burnDuration(-15)` | delete `ShortBurnPerk` | none |
| Flashpoint | `critChance(0.2)` | — | unchanged |
| Combustion | `burnPoolCap(2)` | `CombustionPerk` keeps only the burst | "a pool that reaches its cap bursts for half of it onto enemies within 1 cell" |
| Wide Nozzle | `range(0.55)`, `coneWidth(0.21)`, `stokeGrace(40)` | delete `WideNozzlePerk` | none |
| Wide Nozzle II | `range(0.44)`, `attackSpeed(0.2)`, `coneWidth(0.182)` | delete `NozzleWidthPerk` | none |
| Inferno Ring | `range(-0.55)` | — | unchanged |
| Dragon's Breath | `range(0.66)`, `attackSpeed(2)`, `damage(-0.9)` | — | unchanged |
| Kindling | `burnMarkRate(1)` | delete `KindlingPerk` | none |

- `CinderSpec` loses `coneScale`, `waveSpeed`, `burnScale` and their `with…ScaledBy` methods.
- `StokeSpec` becomes `record StokeSpec(boolean active, int maxSteps, boolean alwaysFull)` with
  `none()`, `of(int maxSteps)`, `full()`; `StokePerk` passes `StokeSpec.of(MAX_STEPS)` and loses `STEP`.
- `CinderTower`:
  - `halfWidthRadians(spec)`: `this.stats().value(CONE_WIDTH) / 2 * bellows` (Inferno Ring still π).
  - `burnDuration(spec)`: `Math.max(1, Math.round(this.stats().value(BURN_DURATION)))`.
  - `fire`: `travelTicks = Math.max(1, Math.round(WAVE_TRAVEL_TICKS / this.stats().value(WAVE_SPEED)))`.
  - Stoke's step and grace read `STOKE_STEP` and `STOKE_GRACE`.
  - Add `private PoolTuning tuning(CinderSpec spec)`: `spec.tuning().withCapFactor(stats().value(BURN_POOL_CAP))
    .withStackRate(Math.round(stats().value(BURN_MARK_RATE)))`; every `spec.tuning()` becomes
    `this.tuning(spec)`. `CombustionPerk` no longer calls `withCapFactor`; `KindlingPerk` is gone.
  - Delete `HALF_WIDTH_RADIANS_BASE` and `BURN_DURATION_TICKS_BASE` after moving their values into
    the base stats.

#### Aura (range 1.5)

Create package `td.tower.aura` with:

```java
/** How Kinship counts the other tower types near an aura. 0 for a cap means none. */
public record KinshipRule(boolean active, float xpPerType, int xpTypeCap, float extraReachCells, float strengthPerType,
                          int strengthTypeCap) {
    public static KinshipRule none()       // (false, 0, 0, 0, 0, 0)
    public static KinshipRule attuned()    // (true, 0.05f, 4, 0f, 0f, 4)
    public KinshipRule reachingFurther(float cells)   // extraReachCells + cells, xpTypeCap 0
    public KinshipRule strengthening(float perType)   // strengthPerType = perType
}

/** What an aura does beyond its emission: Kinship, the XP mentoring, and how it picks whom it buffs. */
public record AuraSpec(KinshipRule kinship, float tutelageXp, boolean sharesLessons, float apprenticeXp, boolean chosen,
                       boolean rally) {
    public static AuraSpec none()
    public AuraSpec withKinship(KinshipRule kinship)
    public AuraSpec withTutelageXp(float xp)
    public AuraSpec sharingLessons()
    public AuraSpec withApprenticeXp(float xp)
    public AuraSpec choosing()
    public AuraSpec rallying()
}

/** What one owned node does to the Aura; nothing by default. */
public interface AuraPerk {
    default AuraSpec refineSpec(AuraSpec spec) { return spec; }
}
```

Perks (one class each, `final`, a constant for each number):

| Perk | `refineSpec` |
|---|---|
| `KinshipPerk` | `spec.withKinship(KinshipRule.attuned())` |
| `FarKinshipPerk` | `spec.withKinship(spec.kinship().reachingFurther(1f))` |
| `KinshipStrengthPerk` | `spec.withKinship(spec.kinship().strengthening(0.05f))` |
| `TutelagePerk` | `spec.withTutelageXp(0.1f)` |
| `SharedLessonsPerk` | `spec.sharingLessons()` |
| `ApprenticePerk` | `spec.withApprenticeXp(1f)` |
| `ChosenPerk` | `spec.choosing()` |
| `RallyPerk` | `spec.rallying()` |

`AuraTower`:

- Base stats: `new TowerBaseStats(DAMAGE_POINTS, RANGE, 0).with(TowerStat.AURA_POWER, power)`.
- `PERKS = PerkCatalogue.<AuraPerk>empty()` with `ATTUNE_ID → KinshipPerk`, `BROADCAST_1 →
  FarKinshipPerk`, `AMPLIFYING_CORE_1 → KinshipStrengthPerk`, `TUTELAGE_1 → TutelagePerk`,
  `SHARED_LESSONS → SharedLessonsPerk`, `APPRENTICE → ApprenticePerk`, `CHOSEN → ChosenPerk`,
  `RALLY → RallyPerk`; `OwnedPerks<AuraPerk> perks`; `onUpgradeBought` calls `this.perks.add(node)`;
  `private AuraSpec spec()` folds them over `AuraSpec.none()`.
- Nodes:

  | Node | Effects | Note |
  |---|---|---|
  | Amplifying Core | `auraPower(0.1)` | "Kinship also strengthens the buff: +5% per other tower type, up to 4" |
  | Amplifying Core II | `auraPower(0.1)`; emission `attackSpeed(0.1)` | none |
  | Keen Edge | emission `critMultiplier(0.5)` (gate unchanged) | none |
  | Broadcast | `range(0.45)` | unchanged |
  | Broadcast II | emission `buffDuration(1).plus(disruptionTaken(0.5))` | none |
  | Conduit | emission `effectDuration(0.3)` (gate unchanged) | none |
  | Withering Field | emission `witherEvery(200)` | none |
  | Tutelage, Shared Lessons, Apprentice, Chosen, Rally | — | unchanged |

- Emission:

  ```java
  @Override
  public StatModifiers<TowerStat> buffFor(Tower other) {
      if (!this.buffs(other)) {
          return StatModifiers.none();
      }
      AuraSpec spec = this.spec();
      float strength = this.stats().value(TowerStat.AURA_POWER) + this.kinshipStrengthFor(other, spec);
      StatModifiers<TowerStat> emission = TowerModifiers.damagePercent(strength)
              .plus(TowerModifiers.rangePercent(strength))
              .plus(TowerModifiers.disruptionTaken(DISRUPTION_LEFT))
              .plus(this.upgrades().totalEmission());
      return spec.chosen() ? emission.scaledBy(CHOSEN_STRENGTH) : emission;
  }
  ```

  `buff()` returns the same for a tower with no Kinship strength.
- Replace every `owns(...)`/`isAttuned()` with the spec: Kinship needs `kinship().active()`; its reach
  adds `extraReachCells × scale`; the XP cap is `xpTypeCap` (0 = none); the strength is
  `strengthPerType × min(strengthTypeCap, types)`; Tutelage adds `tutelageXp`; the Apprentice adds
  `apprenticeXp` to the apprentice; Shared Lessons needs `sharesLessons()`; Chosen and Rally read
  `chosen()` / `rally()`. `grantLook()` is CHOSEN when `chosen()`, REACH when `extraReachCells > 0`,
  otherwise PLAIN. `mentorsApprentice()` is `apprenticeXp() > 0`.
- Delete `power`, `grantsFireRate`, the private `owns`/`isAttuned`, and the constants now living in
  nodes or perks (`CORE_STEP`, `CORE_FIRE_RATE`, `KINSHIP_*`, `TUTELAGE_XP`, `APPRENTICE_XP`,
  `KEEN_EDGE_CRIT_DAMAGE`, `BROADCAST_DISRUPTION_LEFT`, `CONDUIT_EFFECT_LENGTH`, `WITHERING_FIELD_TICKS`).
- `behaviours()` rows read `AURA_POWER`, the spec, and `this.upgrades().totalEmission()` (the
  attack-speed row shows when its `flat(ATTACK_SPEED) > 0`; the Withering row when
  `WITHER_INTERVAL` is set in it).

### 3.5 Catalogue check

Add `td/tower/TowerStatCatalogueTest` (package `td.tower`, so it can call a new package-private
`TowerBaseStats baseStats()` on `AbstractTower`). Build every `TowerFactory.Type` the way
`UpgradeTreeCatalogueTest` does, and for every node of its tree:

- `everyNodeChangesOnlyUniversalStatsOrStatsItsTowerSets`: every stat in `effects().modifiers()` is
  `universal()` or `baseStats().stats().isSet(stat)`;
- `everyEmittedStatIsUniversal`;
- `everyNodeSaysWhatItDoes`: `bonuses()` is not empty.

So every tower's base must `with(...)` every non-universal stat it reads, even at its default value.

### Tests

- Existing getter tests keep passing unchanged: Splash radius, Mortar splash radius, Seeker freeze
  length, Cinder half width, Sonar rotation.
- New, in the tower tests:
  - `MortarTowerTest.aFlatDamageNodeIsAmplifiedByANearbyAura`: Siege Rounds next to a default aura
    is `(32 + 9.6) × 1.2` points.
  - `SplashTowerTest.wideChargeAndRangeThreeWidenTheBlastByTheirCells`: 1.5 + 0.15 + 0.375.
  - `SeekerTowerTest.broodHoldsSixMissiles`.
  - `AuraTowerTest.anAurasEmissionNeverIncludesABlastRadius` (documents D9 and the stat split).
- `UpgradeNodeTest`: `bonuses` read `Damage +16`, `Range +0.6`, `Burn duration -0.75 s`,
  `Cone width +12°`, `Missiles a shot +1`, `Nearby crit damage +50%`.
- Delete the `*PerkTest` methods of deleted perks; move what they asserted to tower-level tests of
  the stat (for example `SniperTowerTest.marksmansEyeLetsSteadyAimStackToThree`).
- `AuraTowerTest`: every test keeps its sentence; `amplifyingCoreIncreasesTheBuffANeighbourReceives`
  and the Kinship tests read the power through the emission.

### Docs

- Root `CLAUDE.md` packages line: `tower` (+ `targeting`, `upgrade`, `aura`, `sniper`, `sonar`, `splash`).
- `td/tower/CLAUDE.md`:
  - Splash bullet: "`SplashTower` falloff is `1 - (d/radius)²` from the primary, floored by
    `BLAST_EDGE`. The blast radius is `AREA_RADIUS`; auras never emit it. Every distance the payload
    uses grows with it (`distanceScale()`). The blast lands and Saturates first, then the payload runs."
  - Mortar: "Its dead zone is `DEAD_ZONE` on the `Reach`, which range never moves."
  - Aura bullets: "The Aura gives `buffFor`: damage and range at `AURA_POWER` (plus Kinship), half the
    disruption, and every owned node's emission; Chosen scales it all ×3. Its mechanics (Kinship, the
    XP mentoring, Chosen, Rally) are `AuraPerk`s over an `AuraSpec`."
  - Upgrade tree: replace "A bonus outside `TowerBuff`'s axes goes in `onUpgradeBought`…" and "A node
    whose behaviour is more than a `TowerBuff` becomes a perk…" with:

    ```markdown
    - A node is data: `withModifiers` (its own stats, flat, through `TowerModifiers`), `withEmission`
      (what an aura gives), `withNote` (only for what a perk does). A number a node changes is a
      `TowerStat` when the tower has it from the start or it belongs to the Attune mechanic; any
      other number belongs to the mechanic a node adds, in that mechanic's spec, set by its perk.
      Never add a field a node mutates. `TowerStatCatalogueTest` rejects a node that changes a stat
      its tower doesn't set.
    - A node that adds a mechanic brings a perk, in the tower's own package. A static
      `PerkCatalogue` says which perks each node brings, and the tower's `OwnedPerks` builds its own.
      Perks run in purchase order through `refineSpec` (and the tower's per-hit hooks); they are pure
      (no tower, no `GameWorld`).
    ```

  - "Adding a tower", step 1: "passing a `TowerBaseStats` that sets every non-universal stat the
    tower reads".
  - `StandardBaseSlot.nodes` now takes the base range; say so in the upgrade-tree bullet that names it.

### Done when

- `mvn verify` is green and `PerformanceHarness` passes.
- `PurchaseHarness` for the Sniper, Mortar, Cinder and Aura: every single node gives the same numbers
  as phase 2, except combos that multiplied before (two blast-radius nodes, two shell-speed nodes,
  Wide Nozzle I+II's cone), which now add.
- `BalanceHarness`: towers next to auras deal slightly more damage; everything else within noise.

---

## Phase 4: Author enemy stats in absolute values, let resilience own crits and strengthen heals

**Goal:** D4, D5, D6, D11 and D12 on the enemy side.

### 4.1 Armor in points

- Rename `PercentResistTrait` to `ArmorTrait(float armor, Optional<DamageType> restrictedTo)` (file and
  test too: `ArmorTraitTest`).
  - Factories: `ArmorTrait.both(float armor)`, `physicalOnly(float armor)`, `magicOnly(float armor)`.
    Delete the one-argument constructor and `armorKeeping`.
  - `modifiers` adds `StatModifier.flat(armor)` to `ARMOR`, `MAGIC_RESIST`, or both.
  - `describe()`: label `"Armor"`, `"Magic resist"` or `"Armor, magic resist"`; value `"+25"`.
  - Keep `damageName(DamageType)` here (`FlatResistTrait` uses it).
- `AdaptiveResist(Optional<DamageType> resisted, float evenArmor, float fullArmor)`:
  - `againstDominant(float fullArmor)` (even 0) and `against(DamageType type, float evenArmor, float fullArmor)`;
  - `resolvedFor` gives `armor = even + lean × (full − even)` as an `ArmorTrait`, empty when `armor <= 0`;
  - `describe()`: `"Adaptive armor"` / `"Adaptive magic resist"` / `"Adaptive resist"`, `"up to +67"`.
- Content (`armor = 100 × (1 − kept) / kept`, rounded):

  | Where | Today (kept) | Now (armor) |
  |---|---|---|
  | `ArmoredEnemy` GRUNT, and its single-rank variant | 0.8 | `both(25)` |
  | `ArmoredEnemy` SOLDIER | 0.75 | `both(33)` |
  | `ArmoredEnemy` VETERAN | 0.7 | `both(43)` |
  | `ArmoredEnemy` ELITE | 0.6 | `both(67)` |
  | `ArmoredEnemy` BOSS | 0.5 | `both(100)` |
  | `WardenEnemy` final egg | 0.8 | `both(25)` |
  | `WardenEnemy` second egg | 0.5 | `both(100)` |
  | `WardenEnemy` first egg | 0.2 | `both(400)` |
  | `SimpleEnemy`, `MenderEnemy` ELITE adaptive | `againstDominant(0.6)` | `againstDominant(67)` |
  | `SpawnShape` ARMORED | `against(PHYSICAL, 0.8, 0.2)` | `against(PHYSICAL, 25, 400)`; rename the constants `ARMORED_EVEN_ARMOR`, `ARMORED_FULL_ARMOR` |

- `EnemyStatText`: `ARMOR` → `("Armor", integer)`, `MAGIC_RESIST` → `("Magic resist", integer)`, the
  merged pair → `"Armor, magic resist"`. Delete `mitigationText`.

### 4.2 Resilience owns crits (D5)

- `HitResolution.rollsCrit`: `chance = attacker.critChance() * (1f - resilience / 100f)`. Drop the
  `CRIT_CHANCE_TAKEN` factor and the `Math.min(1f, …)`. Keep "draws only when the chance is above
  zero" and "none at resilience 100".
- Delete `EnemyStat.CRIT_CHANCE_TAKEN` and its `EnemyStatText` case.
- `ActiveEffects`: `EXPOSED_CRIT_CHANCE_TAKEN = 2f` becomes `EXPOSED_RESILIENCE = 50f`; the "exposed
  or revealed" line becomes `accumulator.addFlat(EnemyStat.RESILIENCE, -EXPOSED_RESILIENCE)` (still
  once, however many apply).
- `EnemyStat.RESILIENCE` Javadoc: "Each point removes 1% of an attacker's crit chance and crit bonus;
  below zero each point adds 1% to both; 100 is crit-immune."
- `EnemyStatText.resilienceText`: negative → `("Resilience", "+N% crits")`, positive →
  `("Resilience", "-N% crits")`, 100 → `"Crit immune"`.

### 4.3 Damage taken adds (D11)

In `ActiveEffects.contributeTo`, every damage-taken change becomes a flat add of percentage points:

| Kind / flag | Today | Now |
|---|---|---|
| `VULNERABLE` | multiply `1 + 0.15 × stacks` both types | `addFlat` `0.15 × stacks` both types |
| `PRIORITY` | multiply 1.15 both | `addFlat` 0.15 both |
| `KILL_ZONE` | multiply 1.25 both | `addFlat` 0.25 both |
| `RESONATING` | multiply `1 + 0.08 × stacks` magic | `addFlat` `0.08 × stacks` magic |
| `BRITTLE` (while frozen) | multiply 1.3 physical | `addFlat` 0.3 physical |
| burning, marks for magic | multiply 1.15 magic | `addFlat` 0.15 magic |
| burning, heats | multiply 1.1 periodic | `addFlat` 0.1 periodic |

Rename `multiplyDamageTaken` to `addDamageTaken`; the constants become the added amounts
(`PRIORITY_DAMAGE_TAKEN = 0.15f`, `KILL_ZONE_DAMAGE_TAKEN = 0.25f`, `BRITTLE_PHYSICAL_DAMAGE_TAKEN = 0.3f`,
`MARKED_MAGIC_DAMAGE_TAKEN = 0.15f`, `HEATED_PERIODIC_DAMAGE_TAKEN = 0.1f`).

### 4.4 Plating is flat

`CRACKED` no longer halves plating: it adds `-DamageUnits.ofPoints(CRACKED_PLATING_POINTS)` to both
platings, with `CRACKED_PLATING_POINTS = 5f` (*starting value*; plating already clamps at 0).

### 4.5 Speed stays relative (D12)

No change to chill, poison, tar, undertow, anchored or hurt speed. Their rows keep showing percentages.

### 4.6 Heals (D6)

Values in points per tick (×20 for HP/s); all are *starting values*.

| Ability | Today | Now | Target |
|---|---|---|---|
| Mender ally heal, GRUNT | 0.02, 40 t | 0.125, 40 t | 5%/s of a same-rank Simple mob |
| Mender ally heal, SOLDIER | inherits GRUNT | `heal(0.25f, 40)` | same |
| Mender ally heal, VETERAN | inherits GRUNT | `heal(0.5f, 40)` | same |
| Mender ally heal, ELITE | 0.02, 80 t | 1.0, 80 t | same |
| Mender ally heal, BOSS | 0.04, 80 t | 2.0, 80 t | same |
| Mender self-heal, VETERAN | 0.01 | 0.35 | 3%/s of its own health |
| Mender self-heal, ELITE | inherits VETERAN | `selfHeal(0.7f)` | same |
| Mender self-heal, BOSS | 0.02 | 1.4 | same |
| Warden neglect heal (each stage) | 0.04, 40 t | `health * 0.0015f`, 100 t (12 / 7.5 / 4.5) | 15% of the stage's health over 5 s |
| Frenzied boss threshold heal | 0.02, 200 t | 1.0, 200 t | about 20% over 10 s |
| Frenzy Spawnling death heal (allies) | 0.04, 40 t | 0.5, 40 t | 20 HP per ally |

- `MenderEnemy`: SOLDIER and VETERAN steps add `withAdditionalAbilities(List.of(heal(…)))` (the named
  `"heal"` replaces the inherited one); ELITE adds `selfHeal(0.7f)`.
- `WardenEnemy.warden(...)` already has the stage `health`: the neglect heal becomes
  `new HealTemplate(health * 0.0015f, 100)`.

### Tests

- `ArmorTraitTest` (renamed): `twentyFiveArmorKeepsFourFifthsOfAHit`,
  `aPhysicalOnlyArmorLeavesMagicUntouched`.
- `AdaptiveResistTest`: numbers in armor.
- `HitResolutionTest`:
  - `negativeResilienceNeverRaisesTheCritChanceButRaisesTheCritBonus` →
    `negativeResilienceRaisesBothTheCritChanceAndTheCritBonus` (−50 turns a 20% chance into 30%);
  - `halfResilienceHalvesBothTheCritChanceAndTheCritBonus` stays.
- `AbstractTowerTest.aBurningTargetDoesNotRaiseTheCritChanceTakenFromAnyTower`: Scorched stacks now
  raise crit chance. Rewrite it to assert `chance × (1 + stacks / 100)` and rename it
  `scorchedStacksMakeCritsSlightlyLikelier`.
- `ActiveEffectsTest`:
  - `vulnerableAndPriorityAddTheirDamageTakenRatherThanMultiply` (3 Vulnerable + Priority = 1.60);
  - `anExposedEnemyLosesFiftyResilienceOnceEvenWhenAlsoRevealed`;
  - `crackedTakesFivePlatingAndNeverBelowZero`;
  - rewrite any assertion of ×1.15/×1.25/×1.3 damage taken as an added amount.
- `MenderHealingTest`, `WardenChainTest`, `FrenziedBossSpawnTest`: update healed amounts; add
  `MenderHealingTest.aMenderVisiblyRefillsAnAlly` (an ally at half health, same rank as the Mender,
  regains at least 20% of its health within 5 s).
- `EnemyStatTextTest` (or the test covering it): `"Armor 25"`, `"Armor, magic resist 67"`, the
  resilience texts.
- New `GameEngineTest.anExposedEnemyIsCritMoreOften`.

### Docs

- `td/enemy/CLAUDE.md`: `PercentResistTrait (armor or magic resist, authored as the fraction kept)` →
  `ArmorTrait (armor or magic resist points)`; the `doDamage` bullet: "it rolls against resilience,
  which scales crit chance and crit bonus both ways".
- `td/effect/CLAUDE.md`:
  - the "Effects change a mob only through `contributeTo`" bullet: vulnerable, priority, kill zone,
    resonating and brittle **add** to damage taken; exposed or revealed take 50 resilience, once;
  - `CRACKED` takes 5 plating points.
- `td/stat/CLAUDE.md`: no change.
- `README.md`: the Armored and spawn-shape Armored rows ("+25 armor", "from +25 rising to +400
  armor"); the Vulnerable row ("+15% damage taken each, added"); the Cracked row ("-5 plating");
  the crit paragraph: "A guaranteed crit lands whenever the enemy's resilience is below 100; at 100
  nothing crits. Negative resilience makes crits likelier and harder; Exposed and Revealed take 50
  resilience. Armor and magic resist never go below 0."
- `TODO.md`: under "Enemy traits/abilities numbers are unbalanced placeholders" add a bullet for the
  phase 4 heal values, and under "Effect and specialization numbers are unbalanced placeholders" one
  for Cracked's 5 plating.

### Done when

- `mvn verify` is green and `PerformanceHarness` passes.
- `BalanceHarness`: no level that was won with the reference loadout is lost by more than a few
  lives, and none becomes trivially won. Write the headline numbers in the commit body.

---

## Phase 5: Make shields absorb damage from a depleting pool

**Goal:** D7.

### Steps

1. **`ShieldTemplate(float points, int durationTicks, Optional<DamageType> restrictedTo)`**, with
   `physicalOnly(points, ticks)` and `magicOnly(points, ticks)`. `toEffect` passes
   `DamageUnits.ofPoints(points)`.
2. **`Effect`:** rename `shieldPercent` to `shieldRemaining` (float, units) and add `shieldMax` (float,
   units) right after it. `Effect.shield(float units, int durationTicks, DamageSink sink)` sets both.
   `withShieldPercent` becomes `withShieldRemaining`. Add `Effect withShieldScaledBy(float factor)`
   (scales both). Every other factory passes `0f, 0f`.
3. **`EnemyStat`:** delete `PHYSICAL_SHIELDING`, `MAGIC_SHIELDING` and `shieldingFor`. Add
   `HEALING_RECEIVED(1f, 0f, Float.MAX_VALUE)`: the share of heals and shields the enemy receives.
4. **`StatSheet`:** resolve `SPIRIT` first (as today), then `HEALING_RECEIVED` with no restorative part,
   then every other stat with restorative flats times `spiritFactor × healingReceived`. Delete
   `restoreReduction` from `StatAccumulator` and `StatSheet` (no main code uses it once shields are
   pools).
5. **`ActiveEffects`:**
   - `magnitude` for `SHIELD` is `shieldRemaining()`, so a reapplication keeps the larger pool and the
     longer time (`strongerOf`, unchanged).
   - New:

     ```java
     /** Takes what an active SHIELD can absorb of {@code amount} units of {@code type}; a pool that runs dry goes. */
     public int absorb(DamageType type, int amount)
     ```

     A restricted shield absorbs only its type. `absorbed = Math.min(amount, (int) remaining)`; when
     less than one unit remains, remove the effect.
   - `weakenShield(fraction)` takes that fraction of what remains.
   - `contributeTo`: `SHIELD` contributes nothing. Cauterize (`burning.cauterizes()`) now adds
     `-CAUTERIZED_LOSS` to `HEALING_RECEIVED` (`CAUTERIZED_LOSS = 0.5f`, replacing `CAUTERIZED_SHARE`);
     `HEAL` contributes `restoreFlat(REGENERATION, healPerTick)` with no `received` factor.
   - Add `public ShieldGauge shieldGauge()`: `record ShieldGauge(float remaining, float max)` in
     `td.effect`, in points, with `none()`.
6. **`HitResolution`:**
   - `resolve(Damage incoming, AttackProfile attacker, StatView stats, RandomSource random)` returns the
     mitigated hit (crit, penetration, mitigation, plating, damage taken, periodic taken), **uncapped**
     and with no shield step. Delete `Outcome`, `resolveWithShield` and the `health` parameters. Keep
     a convenience `resolve(Damage incoming, StatView stats)` for tests.
   - `reductionAgainst(type, stats)` = `1 - mitigationMultiplier(mitigation) × damageTaken` (no shielding).
7. **New `td/enemy/HitResult.java`:** `record HitResult(Damage landed, int absorbed)` with `none()`,
   `int credited()` (`landed.amount() + absorbed`) and `boolean critical()` (`landed.critical()`).
8. **`HitReceiver`:** add `HitResult takeHit(Damage damage, AttackProfile attacker)`; `doDamage(damage,
   attacker)` becomes a default method returning `this.takeHit(damage, attacker).landed()`, so the
   existing `doDamage` call sites stay. Delete `shieldingFor`. Implement `takeHit` in
   `DefinedEnemyMob` (today's `doDamage` body) and in `td.fixtures.FakeEnemyMob`.
9. **`DefinedEnemyMob.land`:**

   ```java
   Damage mitigated = HitResolution.resolve(damage, this.spendMark(attacker), this.stats, this.gameWorld.random());
   boolean bypasses = attacker.ignoresShields() || (mitigated.critical() && attacker.critsPierceShields());
   int absorbed = bypasses ? 0 : this.activeEffects.absorb(mitigated.type(), mitigated.amount());
   if (absorbed > 0) {
       this.gameWorld.enemies().reportShielded(this, Damage.of(mitigated.type(), absorbed));
   }
   Damage landed = new Damage(mitigated.amount() - absorbed, mitigated.type(), mitigated.critical())
           .cappedAt(this.health);
   // health, damageTaken, damageTally (landed + absorbed), moments, death: as today
   return new HitResult(landed, absorbed);
   ```

   The damage-taken moment marks when `landed + absorbed > 0`. Invalidate the stat sheet as today.
10. **`DefinedEnemyMob.applyEffect`:** a `SHIELD` is scaled once, when applied, by
    `Math.max(0f, 1f + spirit / 100f) × HEALING_RECEIVED` (`effect.withShieldScaledBy(factor)`), before
    `applyResisted`. Under Inversion, `strikeInsteadOfShielding` deals
    `Damage.magic(Math.round(shield.shieldRemaining()))` (the authored pool) instead of a share of full
    health.
11. **`AbstractTower.dealDamage`:** `HitResult result = enemy.takeHit(damage, attacker.withOrigin(this.origin))`;
    `damageDealt += result.credited()`; the kill and the return value use `result`.
12. **Sonar Ultrasound:** `float protection = context.target().hasEffect(EffectKind.SHIELD) ? MAX_SHARE
    : context.target().reductionAgainst(DamageType.PHYSICAL);` (a shield is no longer a reduction).
13. **Content (points; *starting values*):**

    | Ability | Today | Now |
    |---|---|---|
    | Warden periodic self-shield (magic only) | 50%, 100 t | `magicOnly(health * 0.1f, 100)`: 800 / 500 / 300 |
    | Warden half-health shield on allies in 150 px | 30%, 150 t | `new ShieldTemplate(60f, 150)` |
    | Warden shield on critical hit taken | 30%, 30 t | `new ShieldTemplate(health * 0.03f, 30)`: 240 / 150 / 90 |
    | Shieldbearer ally shield (physical only) | 50%, 60 t, every rank | GRUNT 20, SOLDIER 40, VETERAN 80, ELITE 160, BOSS 320, 60 t |
    | Reaver panic shield | 25%, 100 t | 10% of the rank's health: 50 / 100 / 200 / 400 / 800, 100 t |

    The Shieldbearer and the Reaver need a named ability (`"shield"`, `"panicShield"`) re-authored at
    each rank step with `withAdditionalAbilities`, like the Mender's heal.
14. **UI:**
    - `EnemyInspection.EffectState` gains a fifth component `ShieldGauge shield`. Give it a narrow
      factory `of(EffectKind kind, OptionalInt remainingTicks)` and `withStacks`, `withLevel`,
      `withShield` copies; `EnemyInspection` fills the shield from `mob.shieldGauge()` (add
      `shieldGauge()` to `DefinedEnemyMob`, delegating to `ActiveEffects`).
    - `EnemyStatText.effectRow`: for `SHIELD`, the category text gains
      `" " + round(remaining) + "/" + round(max)` (points) before the time.
    - Delete `isShielding` and the `*_SHIELDING` text cases; add
      `HEALING_RECEIVED -> new StatText("Heals received", SheetNumbers.percent(value))`.
    - `EnemyFrameBuilder` shield bubble alpha: `SHIELD_BUBBLE_ALPHA × (0.3f + 0.7f × fraction)`, where
      `fraction` is `remaining / max` (add `shieldFraction()` next to `shieldGauge()` on
      `DefinedEnemyMob`, and use whichever type `EnemyFrameBuilder` already reads
      `activeEffectKinds()` from).
15. **Mirror Field** needs no change: `ShieldListener` still hears what a shield took of each hit.

### Tests

- `ActiveEffectsTest`: `aShieldAbsorbsUntilItsPoolRunsOut`, `aReappliedShieldKeepsTheLargerPoolAndTheLongerTime`,
  `aPhysicalShieldLetsMagicThrough`, `shatterToneTakesAQuarterOfWhatRemains`.
- `DefinedEnemyMobTest`: `armorReducesAHitBeforeTheShieldAbsorbsIt`, `spiritScalesAShieldWhenItIsApplied`,
  `aBrokenShieldLetsTheRestOfTheHitThrough`, `aCritFromAnAttackerThatPiercesShieldsGoesStraightToHealth`
  (moved from `HitResolutionTest`), `aCauterizedEnemyReceivesHalfAShield`.
- `HitResolutionTest`: delete `shieldingTakesItsShareOfAHitAfterDamageTaken`,
  `theShieldsShareOfAHitIsReportedAndAnAttackThatIgnoresShieldsTakesNoneOfIt` (re-assert the report
  and the ignore rule in `DefinedEnemyMobTest`) and `aShieldCountsTowardsWhatAnEnemyShrugsOff`;
  `damageTakenMultipliesLastAndTheLandedHitIsCappedAtHealth` → `damageTakenMultipliesLast` (the cap
  is now the mob's).
- `AbstractTowerTest`: `damageAbsorbedByAShieldCountsTowardDamageDealtButNotAKill`.
- `InversionTest`: a shield arrives as one hit of its points.
- `WardenChainTest.theWardensPeriodicShieldOnlyTurnsAwayMagic` and
  `theWardenGainsAShieldAfterSurvivingACriticalHit`: assert pools; `PulseTowerTest` Mirror Field: the
  reflected amount is what the pool absorbed.

### Docs

- `td/effect/CLAUDE.md`, the `contributeTo` bullet: remove "shield … on damage taken". Add: "A shield
  is an absorb pool (`ActiveEffects.absorb`), applied after armor and plating, scaled once when
  applied by spirit and `HEALING_RECEIVED`; reapplying keeps the larger pool. Cauterize lowers
  `HEALING_RECEIVED`." Remove `ShieldTemplate`'s percent wording.
- `td/enemy/CLAUDE.md`: the `doDamage` bullet becomes: "`takeHit(damage, attacker)` lands a hit through
  `HitResolution` (crit, penetration, mitigation, plating, damage taken), then a shield pool absorbs
  what it can, then the cap at health; it returns a `HitResult` (health lost, absorbed). Callers
  credit `credited()`. `doDamage` is its `landed()`." The `reportShielded` bullet: "from the pool".
- `td/stat/CLAUDE.md`: the formula bullet says restorative flats scale by spirit and
  `HEALING_RECEIVED`; delete the reduction wording.
- `README.md`: the Shield row: "Absorbs damage up to its points, after armor and plating, then
  breaks"; the Inverted row: "shields as one hit of their points".
- `TODO.md`: add the shield values to "Enemy traits/abilities numbers are unbalanced placeholders".

### Done when

`mvn verify` is green; `PerformanceHarness` passes (the hit path allocates only the `HitResult`);
`BalanceHarness` is sensible on the Warden level.

---

## Phase 6: Describe effect kinds as data and give towers statuses

**Goal:**
- Adding a stat debuff is one `EffectKind` row; `contributeTo` has no per-kind switch for stat kinds.
- Every cross-kind rule is a row in `EffectInteractions`.
- Burn-pool flags become modifiers the pool carries.
- Towers get statuses (D13) that replace `grantTimedModifiers` and `SniperTempo`.

### 6.1 `StatModifier` and `StatModifiers`

- `StatModifier` gains a fifth component `capShareOfBase` (identity `Float.POSITIVE_INFINITY`; `plus`
  keeps the minimum) and the factory `capAtShareOfBase(float share)`. Update `none()`, `flat`,
  `percent`, `times`, `setTo` and `plus`. `applyTo(base)` ends with `Math.min(value, base × capShareOfBase)`
  when the cap is set and no set value is. `StatSheet`'s accumulator `add` feeds the cap into the
  existing `capShare` array.
- `StatModifiers.times(int n)`: flats and percents ×n, multipliers to the power n, set values and caps
  unchanged; `n <= 0` gives `none()`.

### 6.2 `EffectKind` carries its rule

Give `EffectKind` constructor data and nested enums:

```java
public enum Stacking { REFRESH, SHARED_CLOCK, EARNED, LEVEL, POOL }
public enum Pacing { SPIRIT, OWN }
public enum Tag { STOPS, BURNING, SPREADS_WITH_CURSE, SHARED_BY_SYMPATHY, FIELD }

EffectKind(EffectCategory category, Optional<EnemyStat> resistedBy, Stacking stacking, int stackCap, Pacing pacing,
           StatModifiers<EnemyStat> perStack, Set<Tag> tags)
```

Pass `Optional.empty()` / `Optional.of(...)`, `StatModifiers.none()`, `EnumSet.noneOf(Tag.class)` /
`EnumSet.of(...)` inline (an enum constant cannot read the enum's own static fields).

| Kind | Category | Resisted by | Stacking | Cap | Pacing | Per stack | Tags |
|---|---|---|---|---|---|---|---|
| CHILL | SOFT_CC | CHILL_RESIST | LEVEL | 1 | SPIRIT | — | SHARED_BY_SYMPATHY |
| BURN | DAMAGE_OVER_TIME | BURN_RESIST | POOL | 1 | OWN | — | BURNING |
| FREEZE | HARD_CC | FREEZE_RESIST | REFRESH | 1 | OWN | MOVE_SPEED set 0 | STOPS |
| SHIELD | RESTORATIVE | – | REFRESH | 1 | OWN | — | – |
| INVISIBLE | STEALTH | – | REFRESH | 1 | OWN | STEALTH set 1 | – |
| HEAL | RESTORATIVE | – | REFRESH | 1 | OWN | — | – |
| VULNERABLE | DEBUFF | – | SHARED_CLOCK | 3 | SPIRIT | PHYSICAL_ and MAGIC_DAMAGE_TAKEN +0.15 | SPREADS_WITH_CURSE, SHARED_BY_SYMPATHY |
| REVEALED | STEALTH | – | REFRESH | 1 | SPIRIT | STEALTH set 0 | – |
| POISON | DAMAGE_OVER_TIME | – | POOL | 1 | OWN | — | SPREADS_WITH_CURSE |
| SCORCHED | DEBUFF | – | EARNED | 100 | OWN | RESILIENCE −1 | SPREADS_WITH_CURSE |
| SICKENED | DEBUFF | – | EARNED | 100 | OWN | SPIRIT −1 | SPREADS_WITH_CURSE |
| SUNDERED | DEBUFF | – | SHARED_CLOCK | 10 | SPIRIT | ARMOR −5 | SPREADS_WITH_CURSE, SHARED_BY_SYMPATHY |
| EXPOSED | SPOTTED | – | REFRESH | 1 | SPIRIT | RESILIENCE −50 | SPREADS_WITH_CURSE, SHARED_BY_SYMPATHY |
| MARKED | SPOTTED | – | REFRESH | 1 | SPIRIT | — | – |
| PRIORITY | SPOTTED | – | REFRESH | 1 | SPIRIT | both DAMAGE_TAKEN +0.15 | – |
| RESONATING | DEBUFF | – | SHARED_CLOCK | 3 | SPIRIT | MAGIC_DAMAGE_TAKEN +0.08 | SPREADS_WITH_CURSE |
| FRACTURED | DEBUFF | – | EARNED | 5 | OWN | RESILIENCE −10 | SPREADS_WITH_CURSE |
| DAZED | HARD_CC | – | REFRESH | 1 | OWN | MOVE_SPEED set 0 | STOPS |
| SATURATED | DEBUFF | – | SHARED_CLOCK | 3 | OWN | — | – |
| CHARGED | SPOTTED | – | REFRESH | 1 | SPIRIT | — | – |
| DOOM, BLIGHT, CONTAGION, RIME, ASH, INVERSION, SYMPATHY, RECKONING | HEX | – | REFRESH | 1 | SPIRIT | — | – |
| SILENCED | SOFT_CC | – | REFRESH | 1 | OWN | — | FIELD |
| ANCHORED | SOFT_CC | – | REFRESH | 1 | OWN | MOVE_SPEED cap at 0.75 of base | FIELD |
| UNRAVELED | DEBUFF | – | SHARED_CLOCK | 5 | SPIRIT | MAGIC_RESIST −10 | SPREADS_WITH_CURSE, SHARED_BY_SYMPATHY |
| BRITTLE | DEBUFF | – | REFRESH | 1 | SPIRIT | PHYSICAL_DAMAGE_TAKEN +0.3 | – |
| TOLL | DEBUFF | – | SHARED_CLOCK | 5 | OWN | — | – |
| CORRODED | DEBUFF | – | REFRESH | 1 | OWN | ARMOR −30 | FIELD |
| UNDERTOW | SOFT_CC | – | REFRESH | 1 | OWN | MOVE_SPEED ×0.75 | FIELD |
| DEAD_ZONE | DEBUFF | – | REFRESH | 1 | OWN | — | FIELD |
| FALLOUT | DEBUFF | – | REFRESH | 1 | OWN | — | – |
| KILL_ZONE | DEBUFF | – | REFRESH | 1 | OWN | both DAMAGE_TAKEN +0.25 | FIELD |
| CRACKED | DEBUFF | – | REFRESH | 1 | SPIRIT | both PLATING −`DamageUnits.ofPoints(5)` | – |
| TARRED | DEBUFF | – | REFRESH | 1 | OWN | MOVE_SPEED ×0.6 | – |
| BLEEDING | DAMAGE_OVER_TIME | – | REFRESH | 1 | OWN | — | – |
| SOULFIRE | DAMAGE_OVER_TIME | – | POOL | 1 | OWN | — | BURNING |

Every "+"/"−" is a flat modifier; "set" is `setTo`; "×" is `times`. The table must reproduce today's
`category()`, `resistedBy()`, `maxStacks()`, `isPacedBySpirit()`, `spreadsWithCurse()`,
`sharedBySympathy()`, `isFieldEffect()`, `stopsEnemy()` and `isBurning()` exactly; if a row disagrees
with today's switch for a kind, today's switch wins and you report the difference.

Replace the methods:

| Method | Becomes |
|---|---|
| `category()`, `resistedBy()` | getters |
| `maxStacks()` | `stackCap()` for `SHARED_CLOCK` kinds, 0 otherwise (keep the name `maxStacks`) |
| `isPacedBySpirit()` | `pacing == SPIRIT` |
| `stopsEnemy()`, `isBurning()`, `spreadsWithCurse()`, `sharedBySympathy()`, `isFieldEffect()` | `tags.contains(...)` |
| `isFuelPool()` | `stacking == POOL` |
| `isStackDebuff()` | `stacking == EARNED` |
| `isDecaying()` | `stacking` is `LEVEL`, `POOL` or `EARNED` |
| `debuffsEarned()` | moves to `EffectInteractions.earnedBy(kind)` (6.3) |

Add `public StatModifiers<EnemyStat> modifiersAt(int stacks)`: the per-stack bundle `times(stacks)`,
precomputed in the constructor for `0..stackCap` into a `List` so `contributeTo` allocates nothing.

`isDecaying()` now includes `SOULFIRE`, so a Soulfire row shows no countdown, like Burn (intended).
Earned `SCORCHED`/`SICKENED` stacks now cap at 100, where a pool's own earning was uncapped (intended:
the stat bottoms out at −100 anyway).

### 6.3 `EffectInteractions` holds every cross-kind rule

Add these tables (each an `EnumMap`/`EnumSet` built in the static block) and accessors; loop over the
maps rather than streaming, since `apply`/`tick` call them per effect:

| Table | Rows | Accessor |
|---|---|---|
| `REQUIRES` | BRITTLE → FREEZE | `static boolean contributes(EffectKind kind, Set<EffectKind> active)` |
| `IMPLIES` | REVEALED → EXPOSED | `static boolean isActiveOrImplied(EffectKind kind, Set<EffectKind> active)` |
| `EARNS` | BURN → [SCORCHED]; POISON → [SICKENED]; SOULFIRE → [SCORCHED, SICKENED] | `static List<EffectKind> earnedBy(EffectKind pool)` |
| `CHILL_EQUIVALENT` | UNDERTOW → 0.25 | used by `freezeLengthFactor` |
| `FREEZE_CHILL_WEIGHT` | RIME → 2 (default 1) | `static float freezeLengthFactor(Set<EffectKind> active, float chillLevel)` = `1 + weight × (chillLevel + Σ equivalents)` |
| `FREEZE_EXTRA_TICKS` | TARRED → 20 | `static int freezeExtraTicks(Set<EffectKind> active)` |
| `FIRE_START_FACTOR` | TARRED → 2 | `static float fireStartFactor(Set<EffectKind> active)` (a burning kind that starts fresh) |
| `FULL_FREEZE_BURST` | RIME | `static float freezeBurstShare(Set<EffectKind> active, float tuned)` = 1 when any is active, else `max(0.5f, tuned)` |
| `CHILL_TAKEN` | ASH → 0.25 | `static float chillTaken(Set<EffectKind> active)` (default 1) |
| `POOL_FACTOR` | ASH → 2 | `static int poolFactor(Set<EffectKind> active)` (default 1) |

`ActiveEffects` loses every `containsKey(RIME/ASH/TARRED/UNDERTOW/BRITTLE)` condition and the
constants `ASH_POOL_FACTOR`, `ASH_CHILL_TAKEN`, `RIME_CHILL_FACTOR`, `UNDERTOW_CHILL` (for the
freeze), `TAR_BURN_FACTOR`, `TAR_FREEZE_EXTRA_TICKS`, `FREEZE_BURST_SHARE`; it calls the accessors
instead. `isExposed()` becomes `EffectInteractions.isActiveOrImplied(EXPOSED, this.active.keySet())`.

### 6.4 Generic `contributeTo`

```java
public void contributeTo(StatAccumulator<EnemyStat> accumulator) {
    Set<EffectKind> kinds = this.active.keySet();
    this.burningTuning().carried().applyTo(accumulator);
    for (Map.Entry<EffectKind, Effect> entry : this.active.entrySet()) {
        EffectKind kind = entry.getKey();
        Effect effect = entry.getValue();
        if (!EffectInteractions.contributes(kind, kinds)) {
            continue;
        }
        switch (kind.stacking()) {
            case SHARED_CLOCK, EARNED -> kind.modifiersAt(effect.stacks()).applyTo(accumulator);
            case REFRESH -> kind.modifiersAt(1).applyTo(accumulator);
            case LEVEL -> accumulator.multiply(EnemyStat.MOVE_SPEED, 1f - chillLevel(effect));
            case POOL -> this.contributePool(kind, effect, accumulator);
        }
        if (kind == EffectKind.HEAL) {
            accumulator.restoreFlat(EnemyStat.REGENERATION, effect.healPerTick());
        }
    }
    if (!this.active.containsKey(EffectKind.EXPOSED) && EffectInteractions.isActiveOrImplied(EffectKind.EXPOSED, kinds)) {
        EffectKind.EXPOSED.modifiersAt(1).applyTo(accumulator);
    }
}
```

`contributePool` keeps today's two pool rules: a burning pool above `BURN_REVEAL_FUEL` sets `STEALTH`
to 0, and `POISON` slows by `POISON_MAX_SLOW × share`. `modifiersAt(stacks)` clamps `stacks` to
`0..stackCap`.

### 6.5 Burn pools carry modifiers

`PoolTuning(float capFactor, int stackRate, float freezeBurstShare, StatModifiers<EnemyStat> carried)`:

- `standard()` has `StatModifiers.none()` carried; add `withCarried(StatModifiers<EnemyStat> more)`
  (adds); delete `cauterizing()`, `heating()`, `marking()` and the three booleans.
- `strongerWith(other)`: the three numbers as today (max); `carried` keeps, per stat, the modifier whose
  flat has the larger absolute value (a private static helper in `PoolTuning`).
- Cinder perks: `CauterizePerk` → `withCarried(StatModifiers.of(HEALING_RECEIVED, flat(-0.5f)))`,
  `HeatPerk` → `PERIODIC_DAMAGE_TAKEN` +0.1, `PyromancersMarkPerk` → `MAGIC_DAMAGE_TAKEN` +0.15.
- `ActiveEffects` deletes `CAUTERIZED_LOSS`, `HEATED_PERIODIC_DAMAGE_TAKEN`, `MARKED_MAGIC_DAMAGE_TAKEN`
  and their branches (6.4 applies `carried`).

### 6.6 Tower statuses (D13)

New `td/tower/TowerStatus.java`:

```java
/** A buff a tower holds for a time or for its next shots; granting the same id again restarts it. */
public record TowerStatus(String id, StatModifiers<TowerStat> modifiers, int durationTicks, int shots) {
    /** Lasts {@code durationTicks}; spent by no shot. */
    public static TowerStatus timed(String id, StatModifiers<TowerStat> modifiers, int durationTicks)
    /** Lasts until {@code shots} shots have begun with it. */
    public static TowerStatus nextShots(String id, int shots, StatModifiers<TowerStat> modifiers)
}
```

New `td/tower/ActiveTowerStatuses.java` (`@ThreadConfined(GAME_LOOP)`, package-private):

- `void grant(TowerStatus status, int nowTick, float lengthFactor)`: the same id replaces the old one;
  a timed status ends at `nowTick + round(durationTicks × lengthFactor)`, a shot status holds
  `round(shots × lengthFactor)` shots.
- `boolean expire(int nowTick)`: drops timed statuses past their end; whether anything changed.
- `boolean spendShot()`: one shot from every shot status, dropping those at 0; whether anything changed.
- `StatModifiers<TowerStat> modifiers()`: the `plus`-fold of the active statuses.

`AbstractTower`:

- Replace `timedModifiers`/`timedBuffEndsAt` with `private final ActiveTowerStatuses statuses = new
  ActiveTowerStatuses();` and `private volatile StatModifiers<TowerStat> statusModifiers = StatModifiers.none();`.
- `publishStats`: `own = upgrades.totalModifiers().plus(this.statusModifiers)`.
- `protected final void grantStatus(TowerStatus status)`: grant at `currentTick` with
  `stats.timedBuffLength()`, refresh `statusModifiers`, republish.
- `Tower.receiveTimedModifiers` becomes `void receiveStatus(TowerStatus status)` (calls `grantStatus`).
- `beginTick`: `if (this.statuses.expire(gameTime))` refresh, republish, and return `true`.
- New `protected final TowerStats beginShot()`: `TowerStats shot = this.stats;` then
  `if (this.statuses.spendShot())` refresh and republish; return `shot`. A tower calls it once per
  attack, before the attack, and uses the returned stats for that attack's damage and attack profile.
- Delete `grantTimedModifiers`/`receiveTimedModifiers`.

Conversions:

| Today | Now |
|---|---|
| `SniperTempo.startFrenzy` (via `ShotActions.startFrenzy`) | `grantStatus(TowerStatus.nextShots("sniper.frenzy", 3, TowerModifiers.attackSpeed(1f)))` |
| `SniperTempo.startBurst` (Momentum kill) | `grantStatus(TowerStatus.timed("sniper.momentum.haste", TowerModifiers.attackSpeed(1f), 100))` |
| Rally's `receiveTimedModifiers(attackSpeed(0.3), RALLY_TICKS)` | `receiveStatus(TowerStatus.timed("aura.rally", TowerModifiers.attackSpeed(RALLY_FIRE_RATE), RALLY_TICKS))` |

- Delete `SniperTempo` and its test. `SniperTower.fire` calls `TowerStats shot = this.beginShot()`
  first and builds `SniperShot.of(shot.attack())`; the wait is
  `this.clock.spend(rate / (rate + sniperShot.attackSpeedBonus()))` with `rate = this.attackRate()`.
- The status is in the stats of the waits that follow the grant; a Frenzy granted after shot N is
  spent by the starts of shots N+1, N+2 and N+3, so the three waits after N, N+1 and N+2 are fast.
- Every other attacking tower calls `this.beginShot()` once per attack: Splash per shot, Mortar per
  shell fired (each shell of a salvo), Seeker per launch, Cinder per wave, Pulse per pulse, Sonar
  once per completed revolution. They ignore the returned stats in this plan.

Behaviour changes (intended, D13): a crit during a Frenzy restarts it at 3; a Frenzy and Momentum's
haste now stack (+200%).

### Tests

- `EffectKindTest` (new): `everyKindReproducesItsOldClassification` — a table-driven test of the old
  switch results for all 42 kinds (write the expected sets from today's code before deleting it);
  `modifiersAtScaleWithStacksUpToTheCap`.
- `ActiveEffectsTest`: `aPlainStatDebuffNeedsOnlyItsKindRow` (for each `SHARED_CLOCK`/`EARNED` kind,
  `contributeTo` at 2 stacks equals `modifiersAt(2)`); `brittleCountsOnlyWhileFrozen`;
  `aRevealedEnemyCountsAsExposedOnce`; the Rime, Ash, Tar and Undertow tests keep passing unchanged.
- `EffectInteractionsTest` (new): one test per new table row.
- `StatModifierTest`: `aCapHoldsTheValueToAShareOfItsBase`; `StatModifiersTest`: `timesRepeatsAFlatAndPowersAMultiplier`.
- `ActiveTowerStatusesTest` (new): `aSameIdGrantRestartsTheClock`, `aShotStatusIsSpentOneShotAtATime`,
  `aTimedStatusExpires`, `twoDifferentStatusesStack`.
- `SniperTowerTest`: Frenzy and Momentum tests updated for D13; add
  `aCritDuringFrenzyRestartsIt` and `frenzyAndMomentumHasteStack`.
- `AuraTowerTest` Rally tests keep their sentences.

### Docs

- `td/effect/CLAUDE.md`:
  - replace "Adding a kind" with: "1. An `EffectKind` row: category, resisting stat, stacking, cap,
    pacing, per-stack modifiers, tags. 2. A named `Effect` factory if towers or abilities apply it.
    3. An `EffectTemplate` if abilities author it. 4. The palette cases the compiler forces. A plain
    stat debuff needs no other code; a rule between kinds is a row in `EffectInteractions`."
  - the `contributeTo` bullet: "Effects change a mob only through `contributeTo`, which applies each
    kind's `modifiersAt(stacks)`, the chill level and the pools' rules, and what burning pools carry."
  - the Rime/Ash, Tarred, Brittle and Undertow sentences point to `EffectInteractions`' tables.
  - `PoolTuning` bullet: Cauterize, Heat and the magic mark are modifiers the pool carries.
- `td/tower/CLAUDE.md`: the self-buff bullet becomes: "A temporary or next-shot self-buff is a
  `TowerStatus` granted with `grantStatus` (another tower's with `receiveStatus`): the same id
  restarts, different ids stack. Every attack starts with `beginShot()`, which spends shot charges and
  returns the stats for that attack."

### Done when

`mvn verify` is green; `PerformanceHarness` passes (`contributeTo` allocates nothing); `BalanceHarness`
is within noise of phase 5 apart from the Sniper's Frenzy.

---

## Phase 7: Move generic upgrade effects into procs, hit bonuses, targeting and capabilities

**Goal:** an upgrade that applies a generic effect, grants a status, changes one hit conditionally,
picks a target or flips a capability is data on its node. Tower code no longer checks which node it
owns, the base chain aside.

### 7.1 Capabilities and targeting

- `td/tower/TowerCapability.java`: `GUARANTEED_CRIT`, `MAGIC_DAMAGE`, `CRITS_PIERCE_SHIELDS`.
- `TowerStatus` gains a fifth component `Set<TowerCapability> capabilities` (empty in both factories)
  and `withCapability(TowerCapability)`. `ActiveTowerStatuses.capabilities()` is their union.
- `td/tower/targeting/TargetPriority.java`:

  ```java
  public enum TargetPriority {
      HIGHEST_RANK("highest rank"), MOST_HEALTH("most health"), MOST_NEIGHBOURS("most neighbours"),
      STOPPED_FIRST("frozen or dazed first");

      private static final float NEIGHBOUR_RADIUS_CELLS = 1.5f;

      public TargetSelector selector(float cellSize)   // HighestRankSelector; HighestHealthSelector;
                                                        // new MostNeighboursSelector(NEIGHBOUR_RADIUS_CELLS * cellSize);
                                                        // PreferringSelector.stoppedFirst(new HighestHealthSelector())
      public String label()
  }
  ```

- `NodeEffects` gains `Set<TowerCapability> capabilities`, `Optional<TargetPriority> targeting`,
  `List<HitBonus> hitBonuses` and `List<Proc> procs`, with `withCapability`, `withTargeting`,
  `withHitBonus`, `withProc` (each adds). `bonuses()` lists, after the emission rows: one row per
  capability (`"Crits pierce shields"`, `"Deals magic damage"`, `"Guaranteed crit"`, empty value), a
  `("Targets", label)` row, then `HitBonus.describe()` and `Proc.describe()` rows, then the note.
  `UpgradeNode` gains the matching `with…` delegates.
- `UpgradeState` gains `capabilities()` (union), `targeting()` (the first owned node that sets one,
  D16), `hitBonuses()` and `procs()` (concatenated in purchase order).
- `TowerStats` gains a final component `Set<TowerCapability> capabilities` (an unmodifiable copy) and
  `public boolean has(TowerCapability capability)`; `TowerStats.of` takes the set as a last parameter;
  `publishStats` passes `upgrades.capabilities()` ∪ `statuses.capabilities()` (published through the
  same volatile as the status modifiers: make it one `record StatusLayer(StatModifiers<TowerStat> modifiers,
  Set<TowerCapability> capabilities)`). `of` adds `withGuaranteedCrit()` to the attack profile for
  `GUARANTEED_CRIT` and `withCritsPierceShields()` for `CRITS_PIERCE_SHIELDS`.
- `AbstractTower.damageType()` returns `MAGIC` when `stats().has(MAGIC_DAMAGE)`, else its current
  answer.

### 7.2 Hit bonuses

Package `td.tower.proc`:

```java
/** What a tower knows about a hit it is about to land. */
public record HitContext(EnemyMob target, float rangeShare, int aimStacks, boolean freshTarget) {
    public static HitContext of(EnemyMob target, float rangeShare)   // aimStacks 0, freshTarget false
}

public sealed interface HitCondition {
    record Always() implements HitCondition {}
    record FreshTarget() implements HitCondition {}
    record TargetRankAtLeast(Rank rank) implements HitCondition {}
    record TargetHealthBelow(float share) implements HitCondition {}
    record TargetStopped() implements HitCondition {}
    record TargetHasAnyOf(Set<EffectKind> kinds) implements HitCondition {}
    record BeyondRangeShare(float share) implements HitCondition {}
    record AimStacksAtLeast(int stacks) implements HitCondition {}
    record All(List<HitCondition> conditions) implements HitCondition {}

    boolean holds(HitContext context);   // a default method switching over the sealed records
    String describe();                   // "Vs Elite and Boss", "First shot at a target", ...
}

public enum HitScaling { ONCE, PER_AIM_STACK, PER_RANGE_SHARE }

/** What a bonus adds to a hit: damage as a fraction, crit chance and crit damage, and attack speed for the wait after it. */
public record HitAdjustment(float damage, float critChance, float critMultiplier, float attackSpeed) {
    public static HitAdjustment none(); damage(float); critChance(float); critMultiplier(float); attackSpeed(float)
    public HitAdjustment plus(HitAdjustment other)
    public HitAdjustment times(float factor)
}

public record HitBonus(HitCondition when, HitScaling scaling, HitAdjustment adjustment) {
    public static HitBonus when(HitCondition condition, HitAdjustment adjustment)   // ONCE
    public static HitBonus perAimStack(HitAdjustment adjustment)                     // Always
    public static HitBonus perRangeShare(HitAdjustment adjustment)                   // Always
    /** The adjustment this bonus gives the hit, none when its condition fails. */
    public HitAdjustment adjustmentFor(HitContext context)   // ONCE: 1, PER_AIM_STACK: aimStacks, PER_RANGE_SHARE: rangeShare
    public UpgradeBonus describe()                           // label from condition and scaling, value from the adjustment
}
```

`HitCondition.TargetRankAtLeast` compares `target.getRank().compareTo(rank) >= 0`;
`TargetHealthBelow` uses `getHealthFraction()`; `TargetStopped` uses `isStopped()`; `TargetHasAnyOf`
uses `hasEffect`.

`AbstractTower`:

- `protected List<HitBonus> baseHitBonuses()`, empty by default.
- `protected final HitAdjustment hitAdjustment(HitContext context)`: the `plus`-fold of
  `adjustmentFor(context)` over `baseHitBonuses()` then `upgrades().hitBonuses()`.

A tower applies an adjustment to its hit: damage × `(1 + damage())`, crit chance + `critChance()`
(kept within `[0, 1]`), crit multiplier + `critMultiplier()` (`withCritDamageBonus`), and, for the
Sniper, `attackSpeed()` as the shot's `attackSpeedBonus`. Bonuses add (D11's rule extended): today
Headhunter ×1.4 and the Executioner's ×1.5 on a wounded boss multiply to ×2.1; now they give ×1.9.

### 7.3 Procs

Package `td.tower.proc`:

```java
public enum HitKind { DIRECT, PIERCE, BOUNCE, BLAST, ARC, BEAM, MISSILE, ZAP }

/** How one landed attack went, as the tower settled it. */
public record AttackOutcome(EnemyMob target, HitKind kind, boolean critical, boolean killed) {}

public sealed interface ProcTrigger {
    record OnHit() implements ProcTrigger {}
    record OnCrit() implements ProcTrigger {}
    record OnKill() implements ProcTrigger {}
    record EveryNthShot(int n) implements ProcTrigger {}
}

public sealed interface ProcCondition {
    record HitKindIn(Set<HitKind> kinds) implements ProcCondition {}
    record TargetHasAnyOf(Set<EffectKind> kinds) implements ProcCondition {}
}

public sealed interface ProcPayload {
    record ApplyStacks(EffectKind kind, int stacks) implements ProcPayload {}
    record Charge(int ticks) implements ProcPayload {}
    record GrantStatus(TowerStatus status) implements ProcPayload {}
    record Dispel() implements ProcPayload {}
    record BreakShield(float share) implements ProcPayload {}
}

public record Proc(ProcTrigger trigger, List<ProcCondition> conditions, ProcPayload payload) {
    public static Proc onHit(ProcPayload payload); onCrit(...); onKill(...); everyNthShot(int n, ProcPayload payload)
    public Proc when(ProcCondition condition)   // adds one
    public UpgradeBonus describe()
}
```

`Proc.describe()`: the label is the trigger (`"On hit"`, `"On crit"`, `"On kill"`, `"Every 3rd shot"`)
followed by its conditions (`", vs shielded"`, `", arcs"`); the value is the payload: `"Vulnerable
+1"`, `"Charged 3 s"`, `"Dispel"`, `"Shield -25%"`, and for a status `"next shot: Damage x5, Armor
penetration +100%, Plating penetration +100%"`, `"next 3 shots: Attack speed +100%"` or `"5 s: Attack
speed +100%"` (modifier text from `NodeEffects`' row rules; capabilities by name).

`AbstractTower` runs them:

- `protected List<Proc> baseProcs()`, empty by default; `procs()` is `baseProcs()` then
  `upgrades().procs()`.
- `protected final void report(AttackOutcome outcome)`: for every proc whose trigger is `OnHit`, or
  `OnCrit` with `outcome.critical()`, or `OnKill` with `outcome.killed()`, and whose conditions all
  hold, run its payload. `ApplyStacks`, `Charge`, `Dispel` and `BreakShield` skip a dead target.
  Payloads: `applyStacks(target, kind, stacks)`, `charge(target, ticks)`, `grantStatus(status)`,
  `target.dispelRestoratives()`, `target.breakShield(share)`.
- `beginShot()` (phase 6) now first counts the shot (`private int shotsBegun`, read through
  `protected final int shotNumber()`), then runs every `EveryNthShot` proc with `shotsBegun % n == 0`
  (its status lands in this shot's stats), then takes the stats and spends charges as before.

A tower reports once per landed attack, **after** its own perks settled it, for the target the attack
was aimed at: Sniper (`DIRECT`, after `settle`, so an execution counts as a crit), Sonar (`BEAM`, per
strike result), Seeker (`MISSILE`, per impact), Splash arcs (`ARC`, per arced enemy). Other towers
don't report in this plan.

### 7.4 Conversions

| Tower | Node | Data | Deleted |
|---|---|---|---|
| Sniper | Attune | `BaseSlotPerks.withAttune(NodeEffects.none().withHitBonus(perAimStack(critChance(0.1))).withNote("Steady Aim: +10% crit chance on every shot after the first at one target"))` | `SteadyAimPerk` |
| Sniper | Focused Optics | `when(FreshTarget, critChance(0.5))`, `when(AimStacksAtLeast(1), attackSpeed(0.25))` | `QuickScopePerk`, `SteadyTempoPerk` |
| Sniper | Focused Optics II | `onCrit(GrantStatus(nextShots("sniper.frenzy", 3, attackSpeed(1))))` | `FrenzyPerk`, `ShotActions.startFrenzy` |
| Sniper | Executioner | `when(All(TargetRankAtLeast(BOSS), TargetHealthBelow(0.25)), damage(0.5))`; `ExecutionerPerk` keeps only `settle` | — |
| Sniper | Marksman's Eye III | capability `CRITS_PIERCE_SHIELDS` | `CleanShotPerk` |
| Sniper | Sunder Rounds | `onCrit(ApplyStacks(SUNDERED, 1)).when(HitKindIn(DIRECT))` | `SunderRoundsPerk` |
| Sniper | Tradecraft II | `when(BeyondRangeShare(2/3f), damage(0.25))` | `LongShotPerk` |
| Sniper | Silver Rounds | `everyNthShot(3, GrantStatus(nextShots("sniper.silver", 1, magicPenetration(0.25)).withCapability(MAGIC_DAMAGE)))` | `SilverRoundsPerk` |
| Sniper | Momentum | targeting `HIGHEST_RANK`; `onCrit(GrantStatus(nextShots("sniper.momentum.charge", 1, damageTimes(5).plus(armorPenetration(1)).plus(platingPenetration(1))))).when(HitKindIn(DIRECT))`; `onKill(GrantStatus(timed("sniper.momentum.haste", attackSpeed(1), 100))).when(HitKindIn(DIRECT))` | `MomentumPerk`, `ShotActions.startBurst` |
| Sniper | Ricochet | targeting `MOST_NEIGHBOURS`; `RicochetPerk` keeps the bounce | — |
| Sniper | Headhunter | targeting `HIGHEST_RANK`; `when(TargetRankAtLeast(ELITE), damage(0.4))` | `HeadhunterPerk` |
| Sniper | Hollow Point | targeting `MOST_HEALTH`; `onCrit(ApplyStacks(VULNERABLE, 1)).when(HitKindIn(DIRECT))` | `HollowPointPerk` |
| Sniper | Fifth Shot | targeting `MOST_HEALTH`; `everyNthShot(5, GrantStatus(nextShots("sniper.fifth", 1, critMultiplier(0.5)).withCapability(GUARANTEED_CRIT)))` | `FifthShotPerk` |
| Sniper | Shatter Shot | targeting `STOPPED_FIRST`; `when(TargetStopped, critMultiplier(0.5))` | `ShatterShotPerk` |
| Sonar | Long Reach II | `perRangeShare(damage(1))` | `FarDamagePerk` |
| Sonar | Horizon | `perRangeShare(damage(0.5))` | `HorizonPerk` |
| Sonar | Harmonics | `onHit(ApplyStacks(RESONATING, 1)).when(HitKindIn(BEAM))` | `HarmonicsPerk` |
| Sonar | Pure Tone | capability `MAGIC_DAMAGE` | `PureTonePerk` |
| Sonar | Shatter Tone | `onHit(BreakShield(0.25)).when(TargetHasAnyOf(SHIELD))` | `ShatterTonePerk` |
| Seeker | Nullifier | `when(TargetHasAnyOf(SHIELD), damage(0.5))`; `onHit(Dispel).when(HitKindIn(MISSILE))` | `NullifierPerk` |
| Seeker | Hunter's Mark | targeting `HIGHEST_RANK`; `HuntersMarkPerk` keeps the streak | — |
| Splash | Static Charge | `onHit(Charge(60)).when(HitKindIn(ARC))` | `StaticChargePerk`, `SplashActions` |

`TowerModifiers.damageTimes(float factor)` is a `StatModifier.times` on `DAMAGE` (add it).

Tower changes:

- **Sniper.** `spec()` puts `upgrades().targeting()` on the spec before the perks
  (`spec.aimingAt(new SniperAim(priority.selector(cellSize), priority.label()))`); delete the aim
  from `RicochetPerk` etc. `fire`: `TowerStats shot = this.beginShot()`; the shot number is
  `this.shotNumber()` (delete the Sniper's own counter); the shot type is `MAGIC` when
  `shot.has(MAGIC_DAMAGE)`; apply `this.hitAdjustment(new HitContext(target, rangeShare, lock.stacks(),
  lock.fresh()))` to the `SniperShot` before the remaining perks shape it; after `settle`, call
  `this.report(new AttackOutcome(target, HitKind.DIRECT, result.critical(), result.killed()))`. Remove
  from `ShotActions` every method no perk still calls.
- **Sonar.** Each strike's damage multiplies by `1 + hitAdjustment(HitContext.of(target, rangeShare)).damage()`
  in place of `(1 + strike.farBonus() × rangeShare)`; delete `SonarStrike.farBonus`. The strike type is
  `MAGIC` when `stats().has(MAGIC_DAMAGE)`. After each strike, `report(... HitKind.BEAM ...)`. Remove
  the unused `SonarActions` methods.
- **Seeker.** The impact damage multiplies by `1 + hitAdjustment(HitContext.of(target, 0f)).damage()`
  before the remaining `damageFactor` perks; after each impact, `report(... HitKind.MISSILE ...)`;
  `spec()` takes `upgrades().targeting()` for its aim. Fix `HuntersMarkPerk` so the query doesn't
  mutate: `damageFactor(target)` returns `1 + min(MAX_BONUS, BONUS_PER_HIT × hits)` when `target ==
  marked`, else 1; `react(impact)` does `hits++` when the impact's target is `marked`, otherwise
  `marked = target; hits = 1`.
- **Splash.** After each arc lands, `report(new AttackOutcome(enemy, HitKind.ARC, crit, enemy.isDead()))`.
  `fork()` reads the spec (`arcs().active()` → STORMCALLER, `hexes().isActive()` → HEXER) instead of
  `owns`.
- **Info rows.** Each perk interface that has a node-specific info row gets
  `default Optional<BehaviourLine> behaviour() { return Optional.empty(); }`; the tower appends every
  owned perk's row in `behaviours()`. Move into their perks: Seeker's Arcane Warhead and Hunter's Mark
  rows, Sonar's Wide Band, Mark on Sweep and Command Ping rows. `AbstractTower.inspect()` appends one
  `BehaviourLine` per owned proc and hit bonus from its `describe()`, marker `VULNERABLE` for a
  Vulnerable payload and `TARGETING` otherwise. Delete the Sniper's Hollow Point row and every
  remaining `owns(X.id())` in tower code.

Notes: a node whose effects are now all data loses its note (the derived rows say it); a node that
still brings a perk keeps a note saying only what the perk does.

### 7.5 Catalogue check

Add to `TowerStatCatalogueTest`:

- `noTowerAsksWhichNodeItOwns`: read every file under `src/main/java/td/tower/` (recursively); every
  line containing `upgrades().owns(` or `upgrades.owns(` also contains `StandardBaseSlot.`.
- `onlyNodesThatBringAPerkCarryANote` is **not** added (the perk catalogues are private); instead
  `everyNodeSaysWhatItDoes` (phase 3) keeps guarding empty nodes.

### Tests

- `td/tower/proc/ProcTest` with `FakeTower` (give it a way to set base procs and hit bonuses):
  `anOnHitProcRunsOncePerReportedHit`, `anOnCritProcRunsOnlyOnACrit`,
  `anOnKillProcRunsOnlyOnTheKill`, `aConditionThatFailsStopsTheProc`,
  `everyNthShotGrantsBeforeTheNthShotsStatsAreTaken`, `aPayloadSkipsADeadTarget`.
- `td/tower/proc/HitBonusTest`: `aConditionalBonusAppliesOnlyWhenItHolds`, `bonusesAdd`,
  `perAimStackScalesWithTheStacks`, `perRangeShareScalesWithDistance`.
- Every existing tower test for a converted node stays green **unchanged**, except where 7.2's
  additive rule changes a combined number (Headhunter with the Executioner's boss bonus): update that
  number. If any other converted-node test fails, the conversion is wrong: fix the conversion, not the
  test.
- Delete the `*PerkTest` methods of deleted perks; their tower-level tests cover the behaviour.
- `UpgradeNodeTest`: proc and hit-bonus rows for Momentum, Headhunter and Static Charge read as 7.3
  and 7.2 say.

### Docs

- Root `CLAUDE.md` packages line: `tower` (+ `targeting`, `upgrade`, `proc`, `aura`, `sniper`, `sonar`,
  `splash`).
- `td/tower/CLAUDE.md`:
  - Add under "Upgrade tree": "An upgrade that applies a generic effect, grants a status, changes a
    hit conditionally, picks a target or flips a capability is data on its node: `withProc`,
    `withHitBonus`, `withTargeting` (the first owned node that sets one decides), `withCapability`.
    A tower reports each landed attack with `report(AttackOutcome)` after its perks settled it, and
    applies `hitAdjustment(HitContext)` to each hit; hit bonuses add. Never check `owns(...)` in tower
    code except for the base chain; `TowerStatCatalogueTest` scans for it."
  - Remove the Sniper/Seeker aim sentences that say a perk picks the aim.
- `docs/ARCHITECTURE.md`: add section 13, "Stat-driven buffs" (and its line in the contents):

  ```markdown
  ## 13. Stat-driven buffs

  Enemies were already data: traits and effects change stats, and behaviour reads the resolved
  numbers. Towers were not: a fixed `TowerBuff`, per-tower specs refined by perk classes in purchase
  order, per-shot bonuses written as code, and an Aura that stored what it derived. Effects kept their
  numbers in `ActiveEffects` and their rules in a dozen switches.

  Both sides now share one stat formula. A tower's number is a `TowerStat` when the tower has it from
  the start or it belongs to its Attune mechanic, and a node changes it flat; any other number belongs
  to the mechanic a node adds, in that mechanic's spec. Generic upgrade effects are data: procs, hit
  bonuses, statuses, targeting and capabilities. An effect kind is one row: its stacking, pacing,
  per-stack modifiers and tags; every rule between kinds is a row of `EffectInteractions`.

  Two rules keep it simple: nothing an aura receives changes what it gives (auras never buff auras, so
  one recompute in any order is right), and there is no scripting language (every vocabulary is a
  sealed type; a mechanic with its own math stays a named class).

  Rejected: iterating aura-on-aura buffs to a fixed point; making every perk number a stat; a generic
  `StatSheet` on the per-hit path; data-defined towers (worth doing later, not needed for this).
  ```

- `README.md`: unchanged, unless a tower row names a number that changed.

### Done when

- `mvn verify` is green and `PerformanceHarness` passes.
- `BalanceHarness` is within noise of phase 6 apart from the additive hit bonuses.
- Then do the live pass (section 8).

---

## 8. Verification

Per phase: `mvn verify` before the commit; `PerformanceHarness` from phase 2 on; `BalanceHarness`
against the baseline as each "Done when" says, with the headline numbers (lives lost per level) in
the commit body.

Live checks, one `run-jtd` pass after phase 7. Each names what no test can assert:

1. **Upgrade panel layout.** Hover nodes on every tower. The derived rows ("Damage +16", "Cone width
   +12°", "Nearby crit damage +50%", the Momentum and Headhunter rows) each fit one line and the
   panel doesn't overflow.
2. **Tower info wiring.** A Pulse and a Sonar next to an Aura with Amplifying Core II: the Fire rate
   and Rotation rows show base → buffed with the better-trend colour. Debug-spawn a Jammer beside
   them: the rows drop and "Jammed" appears, and an aura beside the Jammer stops buffing a tower its
   shrunk range no longer reaches.
3. **Enemy inspector wiring.** An Armored mob reads "Armor 25". A Warden's shield row counts its
   points down and depletes live, including while paused.
4. **Shield and heal look over time.** The Warden's shield bubble fades as it depletes; a Mender's
   hurt allies visibly brighten as they heal. A `PreviewEnemy` render shows one frame, not a change
   over time.

## 9. Critical files

- `td/stat/{StatKey,StatModifier,StatModifiers,StatAccumulator,StatContributor,BaseStats,ResolvedStats,StatSheet,EnemyStat,DisruptionAura,DisruptionPenalty,DisruptionField}.java`
- `td/tower/{AbstractTower,Tower,TowerStat,TowerStatRow,TowerStats,TowerBaseStats,TowerModifiers,TowerStatus,ActiveTowerStatuses,TowerCapability,FireClock,TowerRoster,ExperienceAwarder}.java`
- every `td/tower/*Tower.java` leaf, and the perks and specs named in phases 3 and 7
- `td/tower/aura/*`, `td/tower/proc/*`, `td/tower/targeting/TargetPriority.java`
- `td/tower/upgrade/{NodeEffects,UpgradeNode,UpgradeState,StandardBaseSlot,BaseSlotPerks,BuffedTowersCondition}.java`
- `td/effect/{Effect,EffectKind,ActiveEffects,EffectInteractions,PoolTuning,ShieldTemplate,ShieldGauge}.java`
- `td/enemy/{ArmorTrait,AdaptiveResist,HitResolution,HitResult,HitReceiver,DefinedEnemyMob,EnemyInspection,ArmoredEnemy,WardenEnemy,MenderEnemy,FrenziedEnemy,ShieldbearerEnemy}.java`,
  `td/level/TwistedHourglassLevel.java`, `td/wave/SpawnShape.java`
- `td/ui/{EnemyStatText,EnemyFrameBuilder,TowerSheetText}.java`, `td/GameEngine.java`
- tests: `td/fixtures/{FakeTower,FakeEnemyMob}.java`, `td/tower/TowerStatCatalogueTest.java`
- docs: root `CLAUDE.md`, `td/{stat,tower,effect,enemy}/CLAUDE.md`, `docs/ARCHITECTURE.md`,
  `README.md`, `TODO.md`

## 10. Glossary for the implementer

- **Flat / percent / multiply / set-to / cap:** the parts of a `StatModifier`. Content uses flat unless
  D4 says percent.
- **Universal stat:** one every tower has; an aura or status may change it on any tower.
- **Own layer:** a tower's stats without received emissions; what an aura emits from.
- **Shot stats:** the `TowerStats` `beginShot()` returns; use it for that attack.
- **Perk:** a mechanic a node adds; it refines the tower's spec.
- **Proc / hit bonus / status / capability:** see 3.1.

## 11. Open questions

1. **Starting values.** Heal, shield and Cracked numbers are *starting values* (phases 4 and 5). They
   get a balance pass after phase 7, recorded in `TODO.md`'s placeholder entries.
2. **Crit-immune enemies and Exposed.** D5 lets an Exposed crit-immune enemy be critted at half
   chance. If playtesting shows bosses melting to Sonar pings, the fix is a product decision (for
   example crit immunity as a set value); don't change it inside this plan.

## 12. Deferred (recorded, not in this plan)

- **Narrowing `Effect`** into a sealed `EffectDynamics` per family (flag, stacks, chill level, fuel
  pool, heal, shield pool, bleed): removes the stamp coupling of the 18-component record. Worth doing
  after phase 6 settles.
- **Perks that describe themselves:** move each mechanic node's note into its perk class, so a
  mechanic's numbers and words live in one file.
- **Aura diminishing returns (Factorio beacon style).** If it comes back, use the strongest-first
  variant: sort auras by strength and weight the k-th by √k − √(k−1), per stat, emissions only.
- **Data-defined towers (`TowerDefinition`)**, like `EnemyDefinition`, with a shared attack loop.
- **Agility and dodge:** `docs/features/FEATURE-agility-and-dodge.md`.
