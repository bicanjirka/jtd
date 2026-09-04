
package td.tower;

import td.tower.Tower;

import java.awt.Graphics2D;

/**
 * Interface vsech vezi
 * @author Jirka
 *
 */
public interface Tower {
	/**
	 * Zjisti typ veze
	 * @return
	 */
	public TowerFactory.type getType();
	/**
	 * Provede na vezi tiknuti
	 * @param gameTime - herni cas
	 */
	public void doTick(int gameTime);
	/**
	 * Vykresli vez
	 * @param g2 - grafika komponenty kam se ma vykreslit
	 * @param gameTime - herni cas
	 */
    public void paint(Graphics2D g2, int gameTime);
    /**
     * Vykresli efekt strelby
     * @param g2 - grafika komponenty
     * @param gameTime - herni cas
     */
    public void paintEffect(Graphics2D g2, int gameTime);
    /**
     * Oznaci vez
     * @param selected - oznacena
     */
    public void setSelected(boolean selected);
    /**
     * Informace o typu veze (cena, damage, ...)
     * @return - info
     */
	public String getInfoString();
	/**
     * Informace o konkretni vezi
     * @return - info
     */
	public String getStatusString();
	/**
	 * Cena, za kolik se vez prodava
	 * @return - cena
	 */
	public int getSellPrice();
	/**
	 * Dostrel veze v jednotkach sirky policka
	 * @return - dostrel
	 */
	public float getRange();
	/**
	 * Dostrel veze v px
	 * @return - dostrel
	 */
	public float getRangeReal();
	/**
	 * @return - souradnice X veze
	 */
    public int getX();
    /**
     * @return - souradnice Y veze
     */
    public int getY();
    /**
     * Zjisti jmeno veze
     * @return - jmeno
     */
    public String getName();
    public void registerTower(Tower t);
    public void unregisterTower(Tower t);
    public void doCleanup();
	
}
