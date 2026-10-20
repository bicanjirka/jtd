package td.enemy;

import td.damage.DamageMix;
import td.damage.DamageUnits;
import td.stat.BaseStats;
import td.stat.DisruptionAura;
import td.stat.EnemyStat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * A data-driven enemy type at one {@link Rank}: id, base stats, body, and the {@link Trait}s and
 * {@link Ability}s that give it behaviour. Built once at registration and shared by every mob
 * spawned from it.
 *
 * @param id            the wave-script token it spawns under
 * @param displayName   first line of the in-game info text
 * @param description   second line of the in-game info text
 * @param baseHealth    this rank's health, before {@code healthDivisor} and spawn-shape scaling
 * @param price         bounty per kill, and the score lost if it leaks
 * @param baseSpeed     pixels per tick; {@code 0} never moves
 * @param healthDivisor divides {@code baseHealth} before any other scaling; {@code 1} for none
 * @param traitSlots    always-on traits, resolved per mob at spawn, identified so
 * {@link #withAdditionalTraits} can replace one by id
 * @param abilitySlots  triggered abilities, identified the same way
 * @param baseStats     authored stat bases; speed comes from {@code baseSpeed}, not from here
 * @param disruption    what it does to towers near it, if anything
 * @param livesOnLeak   the lives it takes if it reaches the end of the path, {@link #ALL_LIVES} for every one left
 */
public record EnemyDefinition(
        String id,
        String displayName,
        String description,
        int baseHealth,
        int price,
        float baseSpeed,
        float healthDivisor,
        BodyArchetype archetype,
        MovementBehavior movement,
        List<IdentifiedTrait> traitSlots,
        List<IdentifiedAbility> abilitySlots,
        BaseStats<EnemyStat> baseStats,
        Optional<DisruptionAura> disruption,
        int livesOnLeak) {

    /**
     * A {@code livesOnLeak} that ends the level: more than any level starts with, yet finite, so a play given lives
     * that cannot run out plays on past it.
     */
    public static final int ALL_LIVES = 1000;

    public EnemyDefinition {
        traitSlots = List.copyOf(traitSlots);
        abilitySlots = List.copyOf(abilitySlots);
    }

    /**
     * The required shape: no description, fixed movement, no divisor, traits or
     * abilities. Add the rest with the {@code withX} copies.
     */
    public static EnemyDefinition of(String id, String displayName, int baseHealth, int price, float baseSpeed,
            BodyArchetype archetype) {
        return new EnemyDefinition(id, displayName, "", baseHealth, price, baseSpeed, 1f,
                archetype, new FixedMovement(), List.of(), List.of(), BaseStats.defaults(EnemyStat.class), Optional.empty(),
                1);
    }

    /** The traits one mob spawned against {@code mix} carries, each slot resolved once. */
    public List<Trait> traitsFor(DamageMix mix) {
        return this.traitSlots.stream().flatMap(slot -> slot.template().resolvedFor(mix).stream()).toList();
    }

    /** Abilities without their ids. */
    public List<Ability> abilities() {
        return this.abilitySlots.stream().map(IdentifiedAbility::ability).toList();
    }

    /**
     * The largest radius any ability casts at, with the kind it applies; empty if nothing is cast
     * at a radius.
     */
    public Optional<SupportAura> supportAura() {
        SupportAura largest = null;
        for (Ability ability : this.abilities()) {
            Optional<SupportAura> candidate = supportAuraFor(ability.action());
            if (candidate.isPresent() && (largest == null || candidate.get().radius() > largest.radius())) {
                largest = candidate.get();
            }
        }
        return Optional.ofNullable(largest);
    }

    private static Optional<SupportAura> supportAuraFor(AbilityAction action) {
        return switch (action) {
            case ApplyEffectAction apply -> switch (apply.target()) {
                case RadiusTarget radiusTarget -> Optional.of(new SupportAura(apply.template().kind(), radiusTarget.radius()));
                case SelfTarget ignored -> Optional.empty();
            };
            case SpawnEnemiesAction ignored -> Optional.empty();
        };
    }

    /**
     * Authors every base stat at once, replacing what an earlier step set; each parameter means what its
     * named method says, plating and insulation in points.
     */
    public EnemyDefinition withStats(float armor, float magicResist, float plating, float insulation,
            float resilience, float agility, float spirit) {
        return this.withArmor(armor)
                .withMagicResist(magicResist)
                .withPlating(plating)
                .withInsulation(insulation)
                .withResilience(resilience)
                .withAgility(agility)
                .withSpirit(spirit);
    }

    /** Physical mitigation in points: a physical hit is multiplied by {@code 100 / (100 + armor)}. */
    public EnemyDefinition withArmor(float points) {
        return this.withBase(EnemyStat.ARMOR, points);
    }

    /** Magic mitigation in points: a magic hit is multiplied by {@code 100 / (100 + resist)}. */
    public EnemyDefinition withMagicResist(float points) {
        return this.withBase(EnemyStat.MAGIC_RESIST, points);
    }

    /** Points every physical hit loses after mitigation, never below zero. */
    public EnemyDefinition withPlating(float points) {
        return this.withBase(EnemyStat.PLATING, DamageUnits.ofPoints(points));
    }

    /** Points every magic hit loses after mitigation, never below zero. */
    public EnemyDefinition withInsulation(float points) {
        return this.withBase(EnemyStat.INSULATION, DamageUnits.ofPoints(points));
    }

    /**
     * Each point removes 1% of an attacker's crit chance and crit bonus; 100 is crit-immune, and below zero
     * crits land harder.
     */
    public EnemyDefinition withResilience(float points) {
        return this.withBase(EnemyStat.RESILIENCE, points);
    }

    /** An aimed hit misses with chance {@code agility / (agility + 100)}, after the attacker's accuracy. */
    public EnemyDefinition withAgility(float points) {
        return this.withBase(EnemyStat.AGILITY, points);
    }

    /**
     * Scales the heals and shields it receives by {@code 1 + spirit / 100} and paces its debuffs; below zero
     * both work against it.
     */
    public EnemyDefinition withSpirit(float points) {
        return this.withBase(EnemyStat.SPIRIT, points);
    }

    /** Authors the base of {@code stat} before any trait or effect modifies it; a later step replaces it. */
    private EnemyDefinition withBase(EnemyStat stat, float base) {
        return new EnemyDefinition(this.id, this.displayName, this.description, this.baseHealth, this.price,
                this.baseSpeed, this.healthDivisor, this.archetype, this.movement, this.traitSlots,
                this.abilitySlots, this.baseStats.with(stat, base), this.disruption, this.livesOnLeak);
    }

    /** Weakens every tower within the aura's radius while this enemy is alive on the board. */
    public EnemyDefinition withDisruption(DisruptionAura aura) {
        return new EnemyDefinition(this.id, this.displayName, this.description, this.baseHealth, this.price,
                this.baseSpeed, this.healthDivisor, this.archetype, this.movement, this.traitSlots,
                this.abilitySlots, this.baseStats, Optional.of(aura), this.livesOnLeak);
    }

    /** A rank step's usual change: new health and bounty, everything else kept. */
    /** Takes {@code lives} if it reaches the end of the path; {@link #ALL_LIVES} ends the level. */
    public EnemyDefinition withLivesOnLeak(int lives) {
        return new EnemyDefinition(this.id, this.displayName, this.description, this.baseHealth, this.price,
                this.baseSpeed, this.healthDivisor, this.archetype, this.movement, this.traitSlots,
                this.abilitySlots, this.baseStats, this.disruption, lives);
    }

    public EnemyDefinition withHealthAndPrice(int baseHealth, int price) {
        return new EnemyDefinition(this.id, this.displayName, this.description, baseHealth, price,
                this.baseSpeed, this.healthDivisor, this.archetype, this.movement, this.traitSlots,
                this.abilitySlots, this.baseStats, this.disruption, this.livesOnLeak);
    }

    public EnemyDefinition withDescription(String description) {
        return new EnemyDefinition(this.id, this.displayName, description, this.baseHealth, this.price,
                this.baseSpeed, this.healthDivisor, this.archetype, this.movement, this.traitSlots,
                this.abilitySlots, this.baseStats, this.disruption, this.livesOnLeak);
    }

    public EnemyDefinition withHealthDivisor(float healthDivisor) {
        return new EnemyDefinition(this.id, this.displayName, this.description, this.baseHealth, this.price,
                this.baseSpeed, healthDivisor, this.archetype, this.movement, this.traitSlots,
                this.abilitySlots, this.baseStats, this.disruption, this.livesOnLeak);
    }

    public EnemyDefinition withMovement(MovementBehavior movement) {
        return new EnemyDefinition(this.id, this.displayName, this.description, this.baseHealth, this.price,
                this.baseSpeed, this.healthDivisor, this.archetype, movement, this.traitSlots,
                this.abilitySlots, this.baseStats, this.disruption, this.livesOnLeak);
    }

    /**
     * Replaces every trait, each anonymous. Use {@link #withAdditionalTraits} to add or replace by
     * id.
     */
    public EnemyDefinition withTraits(List<Trait> traits) {
        return withIdentifiedTraits(traits.stream().map(IdentifiedTrait::anonymous).toList());
    }

    public EnemyDefinition withIdentifiedTraits(List<IdentifiedTrait> traitSlots) {
        return new EnemyDefinition(this.id, this.displayName, this.description, this.baseHealth, this.price,
                this.baseSpeed, this.healthDivisor, this.archetype, this.movement, traitSlots,
                this.abilitySlots, this.baseStats, this.disruption, this.livesOnLeak);
    }

    /**
     * Adds traits by id: one whose id matches an existing trait replaces it in place, the rest are
     * appended.
     */
    public EnemyDefinition withAdditionalTraits(List<IdentifiedTrait> additions) {
        return withIdentifiedTraits(compose(this.traitSlots, additions, IdentifiedTrait::id));
    }

    public EnemyDefinition withAbilities(List<Ability> abilities) {
        return withIdentifiedAbilities(abilities.stream().map(IdentifiedAbility::anonymous).toList());
    }

    public EnemyDefinition withIdentifiedAbilities(List<IdentifiedAbility> abilitySlots) {
        return new EnemyDefinition(this.id, this.displayName, this.description, this.baseHealth, this.price,
                this.baseSpeed, this.healthDivisor, this.archetype, this.movement, this.traitSlots,
                abilitySlots, this.baseStats, this.disruption, this.livesOnLeak);
    }

    /** Adds abilities by id, like {@link #withAdditionalTraits}. */
    public EnemyDefinition withAdditionalAbilities(List<IdentifiedAbility> additions) {
        return withIdentifiedAbilities(compose(this.abilitySlots, additions, IdentifiedAbility::id));
    }

    private static <T> List<T> compose(List<T> existing, List<T> additions, Function<T, TraitId> idOf) {
        List<T> result = new ArrayList<>(existing);
        for (T addition : additions) {
            TraitId id = idOf.apply(addition);
            int index = indexOfId(result, id, idOf);
            if (index >= 0) {
                result.set(index, addition);
            } else {
                result.add(addition);
            }
        }
        return List.copyOf(result);
    }

    private static <T> int indexOfId(List<T> items, TraitId id, Function<T, TraitId> idOf) {
        for (int i = 0; i < items.size(); i++) {
            if (idOf.apply(items.get(i)).equals(id)) {
                return i;
            }
        }
        return -1;
    }
}
