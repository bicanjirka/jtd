package td.ui;

import td.enemy.EnemyMob;
import td.tower.TowerFour;
import td.tower.TowerOne;
import td.tower.TowerThree;
import td.tower.TowerTwo;
import td.tower.TowerUpgrade;
import td.tower.TowerVisitor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

/**
 * Draws each tower's transient targeting effect (beam, splash, pulse). One
 * visit method per concrete tower type, since - unlike the sprite - these
 * genuinely differ by tower.
 */
public final class TowerEffectPainter implements TowerVisitor<Void> {

    private final Graphics2D g2;

    public TowerEffectPainter(Graphics2D g2) {
        this.g2 = g2;
    }

    private static Stroke beamStroke(float coolDownFraction) {
        return new BasicStroke(3.0f * coolDownFraction, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL);
    }

    public Void visitTowerOne(TowerOne tower) {
        EnemyMob target = tower.getCurrentTarget();
        if (target != null) {
            Stroke defaultStroke = this.g2.getStroke();
            this.g2.setColor(Color.GREEN);
            this.g2.setStroke(beamStroke(tower.getCoolDownFraction()));
            this.g2.draw(new Line2D.Float(tower.getX(), tower.getY(), target.getX(), target.getY()));
            this.g2.setStroke(defaultStroke);
        }
        return null;
    }

    public Void visitTowerTwo(TowerTwo tower) {
        EnemyMob target = tower.getPrimaryTarget();
        if (target != null) {
            Stroke defaultStroke = this.g2.getStroke();
            Color lineColor = Color.RED;
            this.g2.setColor(lineColor);
            this.g2.setStroke(beamStroke(tower.getCoolDownFraction()));
            this.g2.draw(new Line2D.Float(tower.getX(), tower.getY(), target.getX(), target.getY()));
            this.g2.setColor(new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 80));
            for (EnemyMob splashTarget : tower.getSplashTargets()) {
                this.g2.draw(new Line2D.Float(target.getX(), target.getY(), splashTarget.getX(), splashTarget.getY()));
            }
            this.g2.setStroke(defaultStroke);
        }
        if (tower.isSplashVisible()) {
            float r = tower.getSpreadRadius();
            this.g2.setColor(new Color(Color.RED.getRed(), Color.RED.getGreen(), Color.RED.getBlue(), 80));
            this.g2.fill(new Ellipse2D.Float(tower.getSplashCenterX() - r, tower.getSplashCenterY() - r, r * 2, r * 2));
        }
        return null;
    }

    public Void visitTowerThree(TowerThree tower) {
        this.g2.setColor(Color.YELLOW);
        int[] lineSteps = tower.getLineSteps();
        int[] enemyX = tower.getEnemyX();
        int[] enemyY = tower.getEnemyY();
        float maxSteps = tower.getCoolDownRecharge() / 2f;
        for (int i = 0; i < lineSteps.length; i++) {
            if (lineSteps[i] > 0) {
                this.g2.setStroke(beamStroke(lineSteps[i] / maxSteps));
                this.g2.draw(new Line2D.Float(tower.getX(), tower.getY(), enemyX[i], enemyY[i]));
            }
        }
        return null;
    }

    public Void visitTowerFour(TowerFour tower) {
        if (tower.isFiring()) {
            float r = tower.getRangeReal();
            this.g2.setColor(new Color(Color.ORANGE.getRed(), Color.ORANGE.getGreen(), Color.ORANGE.getBlue(), 80));
            this.g2.fill(new Ellipse2D.Float(tower.getX() - r, tower.getY() - r, r * 2, r * 2));
        }
        return null;
    }

    public Void visitTowerUpgrade(TowerUpgrade tower) {
        return null;
    }
}
