package td.ui;

import td.enemy.EnemyMobCircle;
import td.enemy.EnemyMobEmpty;
import td.enemy.EnemyMobGhost;
import td.enemy.EnemyMobSquare;
import td.enemy.EnemyMobTriangle;
import td.enemy.EnemyMobVisitor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

/**
 * Draws each enemy's body and death-fade animation. One visit method per
 * concrete enemy type, since body shape/color genuinely differ by kind.
 */
public final class EnemyPainter implements EnemyMobVisitor<Void> {

    private final Graphics2D g2;
    private final int gameTime;

    public EnemyPainter(Graphics2D g2, int gameTime) {
        this.g2 = g2;
        this.gameTime = gameTime;
    }

    private static Color withAlpha(Color base, int alpha) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.max(alpha, 0));
    }

    private static Color healthColor(Color base, float healthFraction) {
        return withAlpha(base, Math.round(healthFraction * 255));
    }

    private static Shape circleShape(float scale) {
        return new Ellipse2D.Float(-scale, -scale, scale * 2, scale * 2);
    }

    private static Shape triangleShape(float scale, boolean up) {
        int u = up ? 1 : -1;
        double point = (Math.sqrt(3) * scale) / 2;
        GeneralPath p = new GeneralPath();
        p.moveTo(0.0f, -scale * u);
        p.lineTo(-point * u, scale / 2 * u);
        p.lineTo(point * u, scale / 2 * u);
        p.closePath();
        return p;
    }

    public Void visitCircle(EnemyMobCircle mob) {
        Color color = Color.CYAN;
        float scale = mob.getBodyScale();
        AffineTransform saveXform = this.g2.getTransform();
        this.g2.translate(mob.getX(), mob.getY());
        this.g2.setColor(color);

        if (mob.isDead()) {
            if (!mob.isFadeComplete(this.gameTime)) {
                int age = mob.ticksSinceDeath(this.gameTime);
                this.g2.setColor(withAlpha(color, mob.fadeAlpha(age)));
                this.g2.draw(circleShape(scale + age));
            }
        } else if (!mob.isInactive()) {
            Shape body = circleShape(scale);
            this.g2.draw(body);
            this.g2.setColor(healthColor(color, mob.getHealthFraction()));
            this.g2.fill(body);
        }
        this.g2.setTransform(saveXform);
        return null;
    }

    public Void visitGhost(EnemyMobGhost mob) {
        Color color = Color.LIGHT_GRAY;
        float scale = mob.getBodyScale();
        AffineTransform saveXform = this.g2.getTransform();
        this.g2.translate(mob.getX(), mob.getY());
        this.g2.setColor(color);

        if (mob.isDead()) {
            if (!mob.isFadeComplete(this.gameTime)) {
                int age = mob.ticksSinceDeath(this.gameTime);
                this.g2.setColor(withAlpha(color, mob.fadeAlpha(age)));
                this.g2.draw(circleShape(scale + age));
            }
        } else if (!mob.isInactive()) {
            Shape body = circleShape(scale);
            this.g2.draw(body);
            this.g2.setColor(healthColor(color, mob.getHealthFraction()));
            this.g2.fill(body);
        }
        this.g2.setTransform(saveXform);
        return null;
    }

    public Void visitSquare(EnemyMobSquare mob) {
        Color color = Color.PINK;
        float scale = mob.getBodyScale();
        AffineTransform saveXform = this.g2.getTransform();
        this.g2.translate(mob.getX(), mob.getY());
        this.g2.rotate(mob.getFacingRadians());
        this.g2.setColor(color);

        if (mob.isDead()) {
            if (!mob.isFadeComplete(this.gameTime)) {
                int age = mob.ticksSinceDeath(this.gameTime);
                this.g2.setColor(withAlpha(color, mob.fadeAlpha(age)));
                int growth = age * 2;
                this.g2.draw(new Rectangle2D.Float(-(scale + growth), -scale, (scale + growth) * 2, scale * 2));
                this.g2.draw(new Rectangle2D.Float(-scale, -(scale + growth), scale * 2, (scale + growth) * 2));
            }
        } else if (!mob.isInactive()) {
            Shape body = new Rectangle2D.Float(-scale, -scale, scale * 2, scale * 2);
            this.g2.draw(body);
            this.g2.setColor(healthColor(color, mob.getHealthFraction()));
            this.g2.fill(body);
        }
        this.g2.setTransform(saveXform);
        return null;
    }

    public Void visitTriangle(EnemyMobTriangle mob) {
        Color color = Color.YELLOW;
        float scale = mob.getBodyScale();
        AffineTransform saveXform = this.g2.getTransform();
        this.g2.translate(mob.getX(), mob.getY());
        this.g2.rotate(mob.getFacingRadians());
        this.g2.setColor(color);

        if (mob.isDead()) {
            if (!mob.isFadeComplete(this.gameTime)) {
                int age = mob.ticksSinceDeath(this.gameTime);
                this.g2.setColor(withAlpha(color, mob.fadeAlpha(age)));
                int growth = age * 2;
                this.g2.draw(triangleShape(scale + growth, false));
                this.g2.draw(triangleShape(scale + growth, true));
            }
        } else if (!mob.isInactive()) {
            Shape body = triangleShape(scale, true);
            this.g2.draw(body);
            this.g2.setColor(healthColor(color, mob.getHealthFraction()));
            this.g2.fill(body);
        }
        this.g2.setTransform(saveXform);
        return null;
    }

    public Void visitEmpty(EnemyMobEmpty mob) {
        return null;
    }
}
