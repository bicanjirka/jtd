package td.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Cache {

    private static final Logger LOG = LoggerFactory.getLogger(Cache.class);
    private static final Cache INSTANCE = new Cache();
    private final Map<String, BufferedImage> cImage;

    private Cache() {

        this.cImage = new HashMap<>();

        this.putBufImg("tower1", this.loadImage("/td/images/tower1b.png"));
        this.putBufImg("tower1_ico", this.loadImage("/td/images/tower1b_ico.png"));
        this.putBufImg("tower2", this.loadImage("/td/images/tower2a.png"));
        this.putBufImg("tower2_ico", this.loadImage("/td/images/tower2a_ico.png"));
        this.putBufImg("tower3", this.loadImage("/td/images/tower3a.png"));
        this.putBufImg("tower3_ico", this.loadImage("/td/images/tower3a_ico.png"));
        this.putBufImg("tower4", this.loadImage("/td/images/tower4a.png"));
        this.putBufImg("tower4_ico", this.loadImage("/td/images/tower4a_ico.png"));
        this.putBufImg("upg", this.loadImage("/td/images/jingjang.png"));
        this.putBufImg("upg_ico", this.loadImage("/td/images/jingjang_ico.png"));

        LOG.info("Cache loaded {} images", this.cImage.size());
    }

    private BufferedImage loadImage(String resourcePath) {
        try {
            BufferedImage img = ImageIO.read(getClass().getResourceAsStream(resourcePath));
            if (img == null) {
                throw new IOException("Resource not found or not a recognized image: " + resourcePath);
            }
            return img;
        } catch (IOException e) {
            LOG.error("Could not load image resource {}", resourcePath, e);
            throw new GameStartupException("Could not load image resource " + resourcePath, e);
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
