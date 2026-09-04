package td.util;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class Cache {

    private static final Cache INSTANCE = new Cache();
    private final Map<String, BufferedImage> cImage;

    private Cache() {

        this.cImage = new HashMap<>();

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
            JOptionPane.showMessageDialog(null, "Could not load images", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    public static Cache getInstance() {
        return INSTANCE;
    }

    public void clearCache() {
        this.cImage.clear();
    }

    public boolean hasBufImg(String name) {
        return this.cImage.containsKey(name);
    }

    public void putBufImg(String name, BufferedImage img) {
        this.cImage.put(name, img);
    }

    public BufferedImage getBufImg(String name) {
        return this.cImage.get(name);
    }

}
