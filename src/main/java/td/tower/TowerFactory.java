
package td.tower;

import td.util.Context;

/**
 * Trida na vyrobu vezi
 * @author Jirka
 *
 */
public class TowerFactory {
	/**
	 * enum obsahujici typy vezi a jejich cenu
	 * @author Jirka
	 *
	 */
	public static enum type {
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
	/**
	 * Vytvori vez podle zadaneho typu
	 * @param t - typ
	 * @param c - herni kontext
	 * @param x - souradnice X
	 * @param y - souradnice Y
	 * @return - vytvorena vez
	 */
	public static Tower createTower(type t, Context c, int x, int y) {
		switch (t) {
			case first: return new TowerOne(c, x, y);
			case second: return new TowerTwo(c, x, y);
			case third: return new TowerThree(c, x, y);
			case fourth: return new TowerFour(c, x, y);
			case upgrade: return new TowerUpgrade(c, x, y);
		}
		return null;
	}
	
}
