package td.tower.mortar;

/**
 * The Bunker Buster: the enemy at the centre of the blast, within {@code radiusCells} of where the
 * shell lands, takes {@code damageFactor} times as much and loses {@code sunderStacks} of armor.
 */
public record CentreSpec(float damageFactor, int sunderStacks, float radiusCells) {

    public static CentreSpec none() {
        return new CentreSpec(1f, 0, 0f);
    }

    public static CentreSpec of(float damageFactor, int sunderStacks, float radiusCells) {
        return new CentreSpec(damageFactor, sunderStacks, radiusCells);
    }

    public boolean isActive() {
        return this.radiusCells > 0f;
    }
}
