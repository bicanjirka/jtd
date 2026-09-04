package td.cell;

import java.awt.Graphics2D;

import td.tower.Tower;

/**
 * Interface pro herni policka
 * 
 * @author Juras
 *
 */
public interface Cell {
	
	/**
	 * Moznosti zvyrazneni herniho policka<br>
	 * <ul>
	 * 	<li>none - zadne</li>
	 * 	<li>select - oznaceni veze</li>
	 * 	<li>place - pokladani veze</li>
	 * </ul>
	 * 
	 * @author Juras
	 *
	 */
	public static enum highlightType {
		none,
		select,
		place;
	}
	
	/**
     * Vykresli zvyrazneni v zavislosti na jeho typu
     * @param g2 - grafika
     */
	public void paintEffect(Graphics2D g2);
	/**
     * Nastavi zvyrazneni policka
     * @param highlight - zvyrazneni
     */
	public void setHighlight(highlightType highlight);
	/**
     * Nastavi polomer kruznici znazornujici dostrel veze
     * @param range - dostrel veze
     */
	public void setHighlightRange(float range);
	/**
     * Nastavi jestli je policko po vuli staveni vezi
     */
	public void enable(boolean b);
	
	/**
     * Jestli se sa na policku stavet<br>
     * Neda pokud lezi na ceste a nebo obsahuje vez
     */
	public boolean buildable();
	/**
	 * Jestli policko obsahuje vez
	 */
	public boolean hasTower();
	/**
     * Postavi na policku vez
     * @param tower - vez co se ma postavit
     */
	public void setTower(Tower tower);
	/**
	 * Ucini policko prazdnym
	 */
	public void unSetTower();
	/**
     * Vrati vez, kterou obsahuje policko
     */
	public Tower getTower();
	
}
