package td.enemy;

import td.damage.DamageType;
import td.term.Term;
import td.term.TermText;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * An adjective a wave script puts before an enemy, each one generic twist on top of what its type and rank make
 * it. A row changes the definition once, when the script is parsed; what it adds is anonymous, so it stacks with
 * the enemy's own traits and abilities and never replaces a rank step.
 */
public enum Affix implements Term {
    ARMORED("Armored") {
        @Override
        EnemyDefinition applyTo(EnemyDefinition definition, AffixContext context) {
            return definition.withAdditionalTraits(List.of(IdentifiedTrait.anonymous(PHYSICAL_ARMOR)));
        }

        @Override
        public TermText describe() {
            return PHYSICAL_ARMOR.entry().text();
        }
    };

    private static final AdaptiveResist PHYSICAL_ARMOR = AdaptiveResist.against(DamageType.PHYSICAL, 25f, 400f);

    private final String title;

    Affix(String title) {
        this.title = title;
    }

    /** The affix written as {@code word}, if any. */
    public static Optional<Affix> of(String word) {
        return Stream.of(values()).filter(affix -> affix.word().equals(word)).findFirst();
    }

    /** How a wave script writes it. */
    public String word() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String key() {
        return "affix." + this.word();
    }

    @Override
    public String title() {
        return this.title;
    }

    /** What it scales the enemy's body by. */
    public float sizeMultiplier() {
        return 1f;
    }

    /** Whether one slot may not carry both, because together they undo each other. */
    public boolean excludes(Affix other) {
        return false;
    }

    /** Why it cannot go on {@code definition}, if it cannot. */
    public Optional<String> problemWith(EnemyDefinition definition) {
        return Optional.empty();
    }

    /** {@code definition} with this affix's change; {@link EnemyDefinition#withAffixes} records it. */
    abstract EnemyDefinition applyTo(EnemyDefinition definition, AffixContext context);
}
