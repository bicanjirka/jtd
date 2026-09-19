package td.enemy;

/**
 * Pairs an {@link Ability} with the {@link TraitId} identity mechanism
 * {@link EnemyDefinition#withAdditionalAbilities} composes by - see {@link TraitId}'s own doc
 * comment for why identity lives here rather than on {@link Ability} itself.
 */
public record IdentifiedAbility(TraitId id, Ability ability) {

    public static IdentifiedAbility anonymous(Ability ability) {
        return new IdentifiedAbility(TraitId.anonymous(), ability);
    }

    public static IdentifiedAbility named(String name, Ability ability) {
        return new IdentifiedAbility(TraitId.named(name), ability);
    }
}
