package td.enemy;

import td.damage.DamageType;
import td.effect.EffectKind;
import td.effect.ShieldTemplate;
import td.stat.EnemyStat;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * An adjective a wave script puts before an enemy, each one generic twist on top of what its type and rank make
 * it. A row changes the definition once, when the script is parsed; what it adds is anonymous, so it stacks with
 * the enemy's own traits and abilities and never replaces a rank step. Rows apply in the order declared here:
 * Hulking first, so a share of health measures the hulking health, and Exalted next, so it doubles only the
 * enemy's own sheet.
 */
public enum Affix implements Term {
    HULKING("Hulking") {
        @Override
        EnemyDefinition applyTo(EnemyDefinition definition, AffixContext context) {
            return definition
                    .withHealthAndPrice(Math.round(definition.baseHealth() * HULKING_HEALTH), definition.price())
                    .withBaseSpeed(definition.baseSpeed() * HULKING_SPEED);
        }

        @Override
        public float sizeMultiplier() {
            return HULKING_SIZE;
        }

        @Override
        public TermText describe() {
            return TermText.of("has " + TermNumbers.times(HULKING_HEALTH) + " health but moves at "
                    + TermNumbers.percent(HULKING_SPEED) + " of its speed, and is bigger");
        }
    },
    EXALTED("Exalted") {
        @Override
        EnemyDefinition applyTo(EnemyDefinition definition, AffixContext context) {
            return exaltedStats().reduce(definition, (exalted, stat) -> exalted.withBase(stat,
                    stat.defaultBase() + 2f * (definition.baseStats().value(stat) - stat.defaultBase())), (a, b) -> b);
        }

        @Override
        public Optional<String> problemWith(EnemyDefinition definition) {
            return exaltedStats().anyMatch(stat -> definition.baseStats().value(stat) != stat.defaultBase())
                    ? Optional.empty()
                    : Optional.of("it has no stat off its default to double");
        }

        @Override
        public TermText describe() {
            return TermText.of("is twice as far from the usual in every stat it has, but not in its speed");
        }
    },
    ARMORED("Armored") {
        @Override
        EnemyDefinition applyTo(EnemyDefinition definition, AffixContext context) {
            return definition.withAdditionalTraits(List.of(IdentifiedTrait.anonymous(PHYSICAL_ARMOR)));
        }

        @Override
        public TermText describe() {
            return PHYSICAL_ARMOR.entry().text();
        }
    },
    ELUSIVE("Elusive") {
        @Override
        EnemyDefinition applyTo(EnemyDefinition definition, AffixContext context) {
            return definition.withAgility(definition.baseStats().value(EnemyStat.AGILITY) + elusiveAgility(context.rank()));
        }

        @Override
        public TermText describe() {
            return TermText.of("dodges aimed shots: " + TermNumbers.signed(elusiveAgility(Rank.GRUNT))
                    + " agility at Grunt, up to " + TermNumbers.signed(elusiveAgility(Rank.BOSS)) + " at Boss");
        }
    },
    REGENERATING("Regenerating") {
        @Override
        EnemyDefinition applyTo(EnemyDefinition definition, AffixContext context) {
            return definition.withAdditionalTraits(List.of(IdentifiedTrait.anonymous(
                    new RegenerationTrait(context.memberHealthPoints(definition) * REGENERATING_SHARE))));
        }

        @Override
        public boolean excludes(Affix other) {
            return other == WITHERED;
        }

        @Override
        public TermText describe() {
            return TermText.of("restores " + TermNumbers.percent(REGENERATING_SHARE) + " of its health every second");
        }
    },
    SHIELDED("Shielded") {
        @Override
        EnemyDefinition applyTo(EnemyDefinition definition, AffixContext context) {
            return definition.withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(AbilityTerm.PANIC,
                    new HealthThresholdTrigger(SHIELDED_BELOW),
                    new ApplyEffectAction(ShieldTemplate.untilBroken(context.memberHealthPoints(definition) * SHIELDED_SHARE),
                            new SelfTarget())))));
        }

        @Override
        public TermText describe() {
            return TermText.format("throws up a {} worth " + TermNumbers.percent(SHIELDED_SHARE)
                    + " of its health once below " + TermNumbers.percent(SHIELDED_BELOW) + " of it", EffectKind.SHIELD);
        }
    },
    WITHERED("Withered") {
        @Override
        EnemyDefinition applyTo(EnemyDefinition definition, AffixContext context) {
            return definition.withBaseSpeed(definition.baseSpeed() * WITHERED_SPEED)
                    .withAdditionalTraits(List.of(IdentifiedTrait.anonymous(
                            new CrumbleTrait(context.memberHealthPoints(definition) * WITHERED_SHARE))));
        }

        @Override
        public boolean excludes(Affix other) {
            return other == REGENERATING;
        }

        @Override
        public TermText describe() {
            return TermText.of("moves at " + TermNumbers.percent(WITHERED_SPEED) + " of its speed but loses "
                    + TermNumbers.percent(WITHERED_SHARE) + " of its health every second, so what holds it back kills it");
        }
    };

    private static final float HULKING_HEALTH = 2.5f;
    private static final float HULKING_SPEED = 0.75f;
    private static final float HULKING_SIZE = 1.4f;
    private static final AdaptiveResist PHYSICAL_ARMOR = AdaptiveResist.against(DamageType.PHYSICAL, 25f, 400f);
    private static final float REGENERATING_SHARE = 0.02f;
    private static final float SHIELDED_BELOW = 0.5f;
    private static final float SHIELDED_SHARE = 0.3f;
    private static final float WITHERED_SPEED = 1.3f;
    private static final float WITHERED_SHARE = 0.02f;

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

    private static float elusiveAgility(Rank rank) {
        return switch (rank) {
            case GRUNT -> 15f;
            case SOLDIER -> 20f;
            case VETERAN -> 25f;
            case ELITE -> 30f;
            case BOSS -> 40f;
        };
    }

    /** Every stat but speed: health is no stat, and a doubled speed would be a different affix. */
    private static Stream<EnemyStat> exaltedStats() {
        return Stream.of(EnemyStat.values()).filter(stat -> stat != EnemyStat.MOVE_SPEED);
    }
}
