package td.tower;

import td.util.GameWorld;

/**
 * Builds towers from their {@link Type}. The {@code createTower} switch has no {@code default}, so
 * a new constant without its class does not compile.
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

    /** The buildable towers, each with its price and placement key - the single source of both. */
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
        public final char placementKey;

        Type(int price, char placementKey) {
            this.price = price;
            this.placementKey = placementKey;
        }
    }

}
