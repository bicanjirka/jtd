package td.enemy;

/**
 * One step of {@link HitResolution}'s formula as a hit went through it, for a trace.
 *
 * @param kind      which step
 * @param stat      the enemy's number the step reads: a crit or damage-taken factor, armor or magic resist,
 *                  plating or insulation in units
 * @param effective what the attacker's penetration left of {@code stat}; {@code stat} itself where nothing
 *                  penetrates it
 * @param after     the damage in units after this step, before rounding
 */
public record HitStep(Kind kind, float stat, float effective, float after) {

    public enum Kind {
        CRIT, MITIGATION, FLAT_REDUCTION, DAMAGE_TAKEN, PERIODIC_TAKEN
    }

    public static HitStep crit(float factor, float after) {
        return new HitStep(Kind.CRIT, factor, factor, after);
    }

    public static HitStep mitigation(float stat, float penetrated, float after) {
        return new HitStep(Kind.MITIGATION, stat, penetrated, after);
    }

    public static HitStep flatReduction(float stat, float penetrated, float after) {
        return new HitStep(Kind.FLAT_REDUCTION, stat, penetrated, after);
    }

    public static HitStep damageTaken(float factor, float after) {
        return new HitStep(Kind.DAMAGE_TAKEN, factor, factor, after);
    }

    public static HitStep periodicTaken(float factor, float after) {
        return new HitStep(Kind.PERIODIC_TAKEN, factor, factor, after);
    }
}
