package td.enemy;

/** An {@link Ability} with the {@link TraitId} it is composed by. */
public record IdentifiedAbility(TraitId id, Ability ability) {

    public static IdentifiedAbility anonymous(Ability ability) {
        return new IdentifiedAbility(TraitId.anonymous(), ability);
    }

    /** What {@code affix} adds, beside every ability the enemy has of its own. */
    public static IdentifiedAbility of(Affix affix, Ability ability) {
        return new IdentifiedAbility(TraitId.of(affix), ability);
    }

    public static IdentifiedAbility named(String name, Ability ability) {
        return new IdentifiedAbility(TraitId.named(name), ability);
    }
}
