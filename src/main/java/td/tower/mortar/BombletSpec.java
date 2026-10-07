package td.tower.mortar;

/**
 * Bomblets a shell scatters along the enemy's path around, or ahead of, where it lands: each a small
 * blast of its own.
 *
 * @param count       how many
 * @param inLine      whether they run in a line ahead of the impact, rather than scattered around it
 * @param damageShare the share of the Mortar's damage each one hits for
 * @param radiusCells how wide each blast is
 */
public record BombletSpec(int count, boolean inLine, float damageShare, float radiusCells) {

    public static BombletSpec none() {
        return new BombletSpec(0, false, 0f, 0f);
    }

    public static BombletSpec of(int count, boolean inLine, float damageShare, float radiusCells) {
        return new BombletSpec(count, inLine, damageShare, radiusCells);
    }

    public boolean isActive() {
        return this.count > 0;
    }
}
