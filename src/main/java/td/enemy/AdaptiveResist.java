package td.enemy;

import td.damage.DamageMix;
import td.damage.DamageType;
import td.damage.DamageUnits;
import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;
import td.term.TermNumbers;
import td.term.TermText;

import java.util.Locale;
import java.util.Optional;

/**
 * A defence chosen once per mob at spawn from how far the level's damage mix leans toward the type it
 * resists: {@code evenPoints} at an even split or before any damage, {@code fullPoints} when every
 * landed point was that type, linear in between. A trait, not a base stat, because what it gives
 * depends on how the level has been played.
 *
 * @param defence    what it grows: armor that cuts a share of a hit, or a fixed loss per hit
 * @param resisted   the type it resists; empty to resist whichever type dominates the mix
 * @param evenPoints the points at no lean
 * @param fullPoints the points at a one-sided mix
 */
public record AdaptiveResist(Defence defence, Optional<DamageType> resisted, float evenPoints, float fullPoints)
        implements TraitTemplate {

    /** Punishes a one-type defence and does nothing against an even one. */
    public static AdaptiveResist againstDominant(float fullArmor) {
        return new AdaptiveResist(Defence.ARMOR, Optional.empty(), 0f, fullArmor);
    }

    /** Always resists {@code type}, harder the more of the landed damage was {@code type}. */
    public static AdaptiveResist against(DamageType type, float evenArmor, float fullArmor) {
        return new AdaptiveResist(Defence.ARMOR, Optional.of(type), evenArmor, fullArmor);
    }

    /** Takes a fixed amount off every hit of {@code type}, more the more of the landed damage was {@code type}. */
    public static AdaptiveResist perHitAgainst(DamageType type, float evenPoints, float fullPoints) {
        return new AdaptiveResist(Defence.PER_HIT, Optional.of(type), evenPoints, fullPoints);
    }

    @Override
    public TraitEntry entry() {
        String most = TermNumbers.signed(Math.round(Math.max(this.evenPoints, this.fullPoints)));
        TermText text = this.resisted
                .map(type -> TermText.of("settles on up to " + most + " " + this.defence.label(type)
                        + ", more the more of this level's damage was " + type.name().toLowerCase(Locale.ROOT)))
                .orElseGet(() -> TermText.of("settles on up to " + most + " " + this.defence.label(DamageType.PHYSICAL)
                        + " or " + this.defence.label(DamageType.MAGIC) + " against whichever damage type this level"
                        + " has dealt most"));
        return TraitEntry.of(TraitTerm.ADAPTATION, text);
    }

    @Override
    public Optional<Trait> resolvedFor(DamageMix mix) {
        DamageType type = this.resisted.orElseGet(mix::dominant);
        float points = this.evenPoints + mix.lean(type) * (this.fullPoints - this.evenPoints);
        if (points <= 0f) {
            return Optional.empty();
        }
        return Optional.of(new Adapted(points, this.defence, type));
    }

    /** What an adaptive resist grows: the stat it raises against a type, and in what units. */
    public enum Defence {
        ARMOR {
            @Override
            EnemyStat stat(DamageType type) {
                return EnemyStat.mitigationFor(type);
            }

            @Override
            float statValue(float points) {
                return points;
            }
        },
        PER_HIT {
            @Override
            EnemyStat stat(DamageType type) {
                return EnemyStat.flatReductionFor(type);
            }

            @Override
            float statValue(float points) {
                return DamageUnits.ofPoints(points);
            }
        };

        abstract EnemyStat stat(DamageType type);

        /** {@code points} as the stat holds them. */
        abstract float statValue(float points);

        String label(DamageType type) {
            return this.stat(type).title().toLowerCase(Locale.ROOT);
        }
    }

    /** What one mob's adaptive resist settled on: {@code points} of its defence against {@code type}. */
    public record Adapted(float points, Defence defence, DamageType type) implements Trait {

        @Override
        public StatModifiers<EnemyStat> modifiers(TraitContext context) {
            return StatModifiers.of(this.defence.stat(this.type), StatModifier.flat(this.defence.statValue(this.points)));
        }

        @Override
        public TraitEntry entry() {
            return TraitEntry.of(TraitTerm.ADAPTATION, TermText.of("settled on "
                    + TermNumbers.signed(Math.round(this.points)) + " " + this.defence.label(this.type)
                    + " against this level's damage"));
        }
    }
}
