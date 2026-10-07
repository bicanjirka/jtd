package td.damage;

/**
 * Who an attack came from, by identity only: two origins are the same attacker only if they are
 * the same object. Each tower holds one for its whole life.
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
