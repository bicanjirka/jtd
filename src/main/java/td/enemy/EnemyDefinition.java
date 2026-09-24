package td.enemy;

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
 * @param mobType       the type targeting queries filter on
 * @param traitSlots    always-on traits, identified so {@link #withAdditionalTraits} can replace
 * one by id
 * @param abilitySlots  triggered abilities, identified the same way
 */
public record EnemyDefinition(
        String id,
        String displayName,
        String description,
        int baseHealth,
        int price,
        float baseSpeed,
        float healthDivisor,
        EnemyMob.Type mobType,
        BodyArchetype archetype,
        MovementBehavior movement,
        List<IdentifiedTrait> traitSlots,
        List<IdentifiedAbility> abilitySlots) {

    public EnemyDefinition {
        traitSlots = List.copyOf(traitSlots);
        abilitySlots = List.copyOf(abilitySlots);
    }

    /**
     * The required shape: no description, normal type, fixed movement, no divisor, traits or
     * abilities. Add the rest with the {@code withX} copies.
     */
    public static EnemyDefinition of(String id, String displayName, int baseHealth, int price, float baseSpeed,
            BodyArchetype archetype) {
        return new EnemyDefinition(id, displayName, "", baseHealth, price, baseSpeed, 1f,
                EnemyMob.Type.NORMAL, archetype, new FixedMovement(), List.of(), List.of());
    }

    /** Traits without their ids. */
    public List<Trait> traits() {
        return this.traitSlots.stream().map(IdentifiedTrait::trait).toList();
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

    /** A rank step's usual change: new health and bounty, everything else kept. */
    public EnemyDefinition withHealthAndPrice(int baseHealth, int price) {
        return new EnemyDefinition(this.id, this.displayName, this.description, baseHealth, price,
                this.baseSpeed, this.healthDivisor, this.mobType, this.archetype, this.movement, this.traitSlots,
                this.abilitySlots);
    }

    public EnemyDefinition withDescription(String description) {
        return new EnemyDefinition(this.id, this.displayName, description, this.baseHealth, this.price,
                this.baseSpeed, this.healthDivisor, this.mobType, this.archetype, this.movement, this.traitSlots,
                this.abilitySlots);
    }

    public EnemyDefinition withHealthDivisor(float healthDivisor) {
        return new EnemyDefinition(this.id, this.displayName, this.description, this.baseHealth, this.price,
                this.baseSpeed, healthDivisor, this.mobType, this.archetype, this.movement, this.traitSlots,
                this.abilitySlots);
    }

    public EnemyDefinition withMobType(EnemyMob.Type mobType) {
        return new EnemyDefinition(this.id, this.displayName, this.description, this.baseHealth, this.price,
                this.baseSpeed, this.healthDivisor, mobType, this.archetype, this.movement, this.traitSlots,
                this.abilitySlots);
    }

    public EnemyDefinition withMovement(MovementBehavior movement) {
        return new EnemyDefinition(this.id, this.displayName, this.description, this.baseHealth, this.price,
                this.baseSpeed, this.healthDivisor, this.mobType, this.archetype, movement, this.traitSlots,
                this.abilitySlots);
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
                this.baseSpeed, this.healthDivisor, this.mobType, this.archetype, this.movement, traitSlots,
                this.abilitySlots);
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
                this.baseSpeed, this.healthDivisor, this.mobType, this.archetype, this.movement, this.traitSlots,
                abilitySlots);
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
