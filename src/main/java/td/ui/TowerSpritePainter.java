package td.ui;

import td.tower.Tower;
import td.tower.TowerFour;
import td.tower.TowerOne;
import td.tower.TowerThree;
import td.tower.TowerTwo;
import td.tower.TowerUpgrade;
import td.tower.TowerVisitor;
import td.util.Cache;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/**
 * Draws a tower's sprite (and, when selected, its range ring). The same
 * regardless of tower type, so every visit method delegates to the one
 * shared drawing routine - the {@link TowerVisitor} dispatch exists so this
 * class never needs an instanceof/cast to reach tower-specific state.
 */
public final class TowerSpritePainter implements TowerVisitor<Void> {

    private final Graphics2D g2;

    public TowerSpritePainter(Graphics2D g2) {
        this.g2 = g2;
    }

    private Void paintSprite(Tower tower) {
        if (tower.isSelected()) {
            float r = tower.getRangeReal();
            this.g2.setColor(Color.PINK);
            this.g2.draw(new Ellipse2D.Float(tower.getX() - r, tower.getY() - r, r * 2, r * 2));
        }
        BufferedImage img = Cache.getInstance().getBufImg(tower.getName());
        this.g2.drawImage(img, null, tower.getBoardX(), tower.getBoardY());
        return null;
    }

    public Void visitTowerOne(TowerOne tower) {
        return this.paintSprite(tower);
    }

    public Void visitTowerTwo(TowerTwo tower) {
        return this.paintSprite(tower);
    }

    public Void visitTowerThree(TowerThree tower) {
        return this.paintSprite(tower);
    }

    public Void visitTowerFour(TowerFour tower) {
        return this.paintSprite(tower);
    }

    public Void visitTowerUpgrade(TowerUpgrade tower) {
        return this.paintSprite(tower);
    }
}
