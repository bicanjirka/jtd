package td.damage;

/**
 * Who an attack or an effect came from, by identity only: two origins are the same source only if they are
 * the same object. Each tower holds one for its whole life, as do each enemy ability on each enemy and each zone an
 * enemy leaves, so their heals and shields stack per source.
 */
public final class AttackOrigin {

    private static final AttackOrigin NONE = new AttackOrigin();

    private AttackOrigin() {
    }

    /** No attacker in particular. */
    public static AttackOrigin none() {
        return NONE;
    }

    /** A new attacker, unlike every other. */
    public static AttackOrigin fresh() {
        return new AttackOrigin();
    }
}
