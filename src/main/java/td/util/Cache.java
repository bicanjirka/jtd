package td.util;

import java.awt.image.BufferedImage;
import java.util.Hashtable;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

/**
 * Trida obsahujici nactene obrazky<br>
 * Je to singleton
 * @author Juras
 *
 */
public class Cache {
	
	private static final Cache INSTANCE = new Cache();
	private Hashtable<String, BufferedImage> cImage;
	
	private Cache() {
		
		this.cImage = new Hashtable<String, BufferedImage>();
		
		try {
			BufferedImage img;
			
			img = ImageIO.read(getClass().getResourceAsStream("/td/images/tower1b.png"));
			this.putBufImg("tower1", img);
			
			img = ImageIO.read(getClass().getResourceAsStream("/td/images/tower1b_ico.png"));
			this.putBufImg("tower1_ico", img);
			
			img = ImageIO.read(getClass().getResourceAsStream("/td/images/tower2a.png"));
			this.putBufImg("tower2", img);
			
			img = ImageIO.read(getClass().getResourceAsStream("/td/images/tower2a_ico.png"));
			this.putBufImg("tower2_ico", img);
			
			img = ImageIO.read(getClass().getResourceAsStream("/td/images/tower3a.png"));
			this.putBufImg("tower3", img);
			
			img = ImageIO.read(getClass().getResourceAsStream("/td/images/tower3a_ico.png"));
			this.putBufImg("tower3_ico", img);
			
			img = ImageIO.read(getClass().getResourceAsStream("/td/images/tower4a.png"));
			this.putBufImg("tower4", img);
			
			img = ImageIO.read(getClass().getResourceAsStream("/td/images/tower4a_ico.png"));
			this.putBufImg("tower4_ico", img);
			
			img = ImageIO.read(getClass().getResourceAsStream("/td/images/jingjang.png"));
			this.putBufImg("upg", img);
			
			img = ImageIO.read(getClass().getResourceAsStream("/td/images/jingjang_ico.png"));
			this.putBufImg("upg_ico", img);
			
			img = ImageIO.read(getClass().getResourceAsStream("/td/images/playground_fractal.png"));
			this.putBufImg("bg", img);
		
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Could not load images", "Error", 0);
			System.exit(0);
		}
	}
	/**
	 * Vraci statickou instanci tridy
	 * @return - instance stridy
	 */
	public static Cache getInstance() {
		return INSTANCE;
	}
	/**
	 * Vymaze veskery obsah tridy
	 */
	public void clearCache() {
		this.cImage.clear();
	}
	/**
	 * Zjisti, jestli se ve tride nachazi obrazek
	 * k prislusnemu jmenu
	 * @param name - jmeno
	 * @return - nalez jmena
	 */
	public boolean hasBufImg(String name) {
		return this.cImage.containsKey(name);
	}
	/**
	 * Umisti obrazek do tridy k prislusnemu jmenu
	 * @param name - jmeno
	 * @param img - obrazek
	 */
	public void putBufImg(String name, BufferedImage img) {
        this.cImage.put(name, img);
    }
	/**
	 * Vytahne ze tridy obrazek podle prislusneho jmena
	 * @param name - jmeno
	 * @return - obrazek
	 */
    public BufferedImage getBufImg(String name) {
        return this.cImage.get(name);
    }
	
}
