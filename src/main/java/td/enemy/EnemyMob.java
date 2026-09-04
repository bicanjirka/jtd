package td.enemy;

import java.awt.Graphics2D;

import td.util.Context;

/**
 * Interface pro nepratele
 * @author Jirka
 *
 */
public interface EnemyMob {
	public static enum type {
		Normal,
		Flying,
		Invisible;
	}
	
	/**
	 * Provede s nepritelem vse, co se ma behem jednoho herniho tiknuti
	 * stat, napriklad ho posune po herni plose
	 * @param gameTime - herni cas
	 */
	public void doTick(int gameTime);
	/**
	 * Vykresli nepritele
	 * @param g2 - grafika komponenty
	 * @param gameTime - herni cas
	 */
    public void paint(Graphics2D g2, int gameTime);
    /**
     * Aktualni souradnice X nepritele na herni plose
     * @return - x
     */
	public int getX();
	/**
     * Aktualni souradnice Y nepritele na herni plose
     * @return - y
     */
    public int getY();
    public int getProgression();
//    public boolean visible();
    /**
     * Jestli je nepritel validni target, tzn. zameritelny vezema
     * @return - validni
     */
    public boolean validTarget();
    /**
     * Jestli je nepritel validni target daneho typu, tzn. zameritelny vezema
     * @param
     * @return - validni
     */
    public boolean validTarget(type type);
    /**
     * Jestli je nepritel validni target a je jednim z danych typu, tzn. zameritelny vezema
     * @param
     * @return - validni
     */
    public boolean validTarget(type type0, type type1);
    /**
     * Kolik ma nepritel zivotu
     * @return - hp
     */
    public long getHealth();
    /**
     * Odebere nepriteli patricny pocet zivotu
     * @param damage - zraneni
     */
    public void doDamage(int damage);
    /**
     * Zjisti aktualni rychlost
     * @return - rychlost
     */
    public int getSpeed();
    /**
     * Informace o nepriteli
     * @return - info
     */
    public String getInfoString();
    /**
     * Vytvori kopii nepritele (klon)
     * @param context - herni kontext
     * @param delay - zpozdeni
     * @param health - zivoty
     * @param price - hodnota
     * @return - klon
     */
    public EnemyMob create(Context context, int delay, int health, int price, int level);
}
