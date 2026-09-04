package td.tower;

import td.util.Context;

public class TowerFactory {
    public static Tower createTower(type t, Context c, int x, int y) {
        return switch (t) {
            case first -> new TowerOne(c, x, y);
            case second -> new TowerTwo(c, x, y);
            case third -> new TowerThree(c, x, y);
            case fourth -> new TowerFour(c, x, y);
            case upgrade -> new TowerUpgrade(c, x, y);
        };
    }

    public enum type {
        first(TowerOne.price),
        second(TowerTwo.price),
        third(TowerThree.price),
        fourth(TowerFour.price),
        upgrade(TowerUpgrade.price);

        public final int price;

        type(int price) {
            this.price = price;
        }
    }

}
