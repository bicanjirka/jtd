package td.enemy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A data-driven enemy type: a name/id (the wave-script token), base stats, a graphical
 * representation, and the composable {@link Trait}s and {@link Ability}s that give it its
 * behavior - replacing what today is a hardcoded method override on a {@code final} leaf
 * class. Built once, at {@link EnemyCatalog} registration time, not per-spawn; {@link Trait}s
 * and {@link Ability}s are shared across every mob spawned from this definition.
 * <p>
 * Carries its own {@code baseHealth}/{@code price}, scoped to whichever {@link Rank} this
 * particular definition represents within its {@link RankedEnemy} ladder - a rank-1 Circle has
 * the same health and bounty in every wave that spawns it. A wave-spawned enemy (via
 * {@code td.wave.Wave}) and an ability-spawned one (a {@code SpawnEnemiesAction}, resolved
 * through {@code DefinedEnemyMob}'s own ability execution) both read {@link #baseHealth()}/
 * {@link #price()} directly now - there is no separate "wave supplies the numbers" path left.
 *
 * @param id            the wave-script token this definition spawns under - built-ins use a
 *                      single letter (matching today's {@code c}/{@code s}/{@code t}/
 *                      {@code g}/{@code e}); a per-level custom or cloned definition uses an
 *                      ordinary, longer id. There is no separate syntax for the two - the wave
 *                      mini-language treats every token as a plain id lookup against whichever
 *                      {@link EnemyCatalog} is in scope.
 * @param displayName   shown as the first line of the in-game info text.
 * @param description   shown as the second line of the in-game info text.
 * @param baseHealth    this rank's own base health, before per-type {@code healthDivisor}
 *                      adjustment and a spawn shape's own multiplier.
 * @param price         this rank's own bounty per kill, and the score penalty if one leaks.
 * @param baseSpeed     pixels per tick before any active effect - {@code 0} means the mob never
 *                      advances along the path at all (the boss egg), which needs no separate
 *                      "stationary" flag: {@code distanceIntoLap} simply never accumulates.
 * @param healthDivisor {@code baseHealth} is divided by this before any other scaling -
 *                      generalizes Ghost's flat {@code /5}. {@code 1} for every definition that
 *                      doesn't need one.
 * @param mobType       which {@link EnemyMob.Type} this definition spawns as - what
 *                      type-filtering targeting queries (see {@code td.tower.targeting}) see,
 *                      independent of anything a {@link Trait} does.
 * @param traitSlots    always-on, no per-mob state of their own beyond what a {@link Trait}'s
 *                      own method parameters supply - see {@link #traits()} for the plain,
 *                      identity-free view most callers want, and {@link #withAdditionalTraits}
 *                      for how a later composition step replaces one by {@link TraitId}
 * @param abilitySlots  triggered behaviors - see {@link Ability}, and {@link #abilities()}/
 *                      {@link #withAdditionalAbilities} for the same identity-aware shape
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
     * The required shape every enemy has: no description, {@link EnemyMob.Type#NORMAL}, a
     * {@link FixedMovement} pace, no health divisor, no traits, no abilities. A definition that
     * needs any of those calls the matching {@code withX} copy below instead of this factory
     * growing another parameter - the same "with"-copy shape {@link td.wave.PathDefinition} and
     * {@code td.util.LoadedLevel} already use.
     */
    public static EnemyDefinition of(String id, String displayName, int baseHealth, int price, float baseSpeed,
            BodyArchetype archetype) {
        return new EnemyDefinition(id, displayName, "", baseHealth, price, baseSpeed, 1f,
                EnemyMob.Type.NORMAL, archetype, new FixedMovement(), List.of(), List.of());
    }

    /**
     * This definition's traits with their {@link TraitId} identity stripped - what every
     * runtime consumer (damage resistance, speed curves, target validity) actually needs.
     */
    public List<Trait> traits() {
        return this.traitSlots.stream().map(IdentifiedTrait::trait).toList();
    }

    /**
     * This definition's abilities with their {@link TraitId} identity stripped - what
     * {@code DefinedEnemyMob}'s ability evaluation and {@code EnemyCatalog}'s spawn-cycle check
     * actually need.
     */
    public List<Ability> abilities() {
        return this.abilitySlots.stream().map(IdentifiedAbility::ability).toList();
    }

    /**
     * A rank ladder step's ordinary shape: the next rank's own health and bounty, everything
     * else (traits included) unchanged from the rank before it - see {@link RankedEnemy}.
     */
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
     * Replaces every trait wholesale, each wrapped as an anonymous, non-replaceable
     * {@link IdentifiedTrait} - the ordinary authoring shape for a definition that isn't part of
     * a rank ladder. {@link #withAdditionalTraits} is the identity-aware alternative a rank step
     * (or a spawn shape's trait override) composes with instead.
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
     * Composes {@code additions} onto this definition's existing traits by {@link TraitId}: an
     * addition whose id matches an existing entry replaces it in place; every other addition is
     * appended. What a later rank step, and a spawn shape's trait override (e.g. {@code
     * armored}), both use to add or upgrade one trait without disturbing the rest.
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

    /**
     * The ability composition counterpart to {@link #withAdditionalTraits} - see its doc comment.
     */
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
