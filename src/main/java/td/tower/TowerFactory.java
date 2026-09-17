package td.tower;

import td.util.GameWorld;

/**
 * Constructs towers from their {@link Type}. That enum is the closed set of buildable towers
 * and the single source of each one's price - the toolbar, the affordability check and the
 * tower itself all read it, so a price lives in exactly one place. The {@code createTower}
 * switch has no {@code default}, so adding a constant without wiring up its class is a
 * compile error rather than a silent gap.
 */
public class TowerFactory {
    public static Tower createTower(Type t, GameWorld c, int x, int y) {
        return switch (t) {
            case SNIPER -> new SniperTower(c, x, y);
            case SPLASH -> new SplashTower(c, x, y);
            case SONAR -> new SonarTower(c, x, y);
            case PULSE -> new PulseTower(c, x, y);
            case AURA -> new AuraTower(c, x, y);
            case MORTAR -> new MortarTower(c, x, y);
            case SEEKER -> new SeekerTower(c, x, y);
            case CINDER -> new CinderTower(c, x, y);
        };
    }

    /**
     * The closed set of buildable towers, each carrying its own price and placement shortcut.
     * The key lives here rather than in a parallel array indexed by {@code ordinal()}, where
     * reordering this enum would silently rebind the keyboard.
     */
    public enum Type {
        SNIPER(SniperTower.PRICE, 'q'),
        SPLASH(SplashTower.PRICE, 'w'),
        SONAR(SonarTower.PRICE, 'e'),
        PULSE(PulseTower.PRICE, 'r'),
        AURA(AuraTower.PRICE, 't'),
        MORTAR(MortarTower.PRICE, 'y'),
        SEEKER(SeekerTower.PRICE, 'u'),
        CINDER(CinderTower.PRICE, 'i');

        public final int price;
        /**
         * The keyboard shortcut that starts placing this tower.
         */
        public final char placementKey;

        Type(int price, char placementKey) {
            this.price = price;
            this.placementKey = placementKey;
        }
    }

}
