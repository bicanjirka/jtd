package td.tower;

import td.enemy.EnemyMob;
import td.util.Context;

import java.awt.*;
import java.awt.geom.Line2D;

/**
 * Vez co strili jen do jednoho nepritele, targetuje vzdy prvniho
 *
 * @author Jirka
 *
 */
public class TowerOne extends AbstractTower {

    public static int price = 10;
    public static int damage = 4000;
    public static float range = 3.8f;

    private int coolDown = 0;

    private final Stroke[] lineStrokes;
    private Stroke lineStroke;

    private EnemyMob enemy;

    public TowerOne(Context context, int x, int y) {
        super(TowerFactory.type.first, price, damage, range);
        this.coolDownMax = 39;
        this.name = "tower1";
        this.lineColor = Color.GREEN;
        this.lineStrokes = new Stroke[this.coolDownMax];
        for (int i = 0; i < this.coolDownMax; i++) {
            this.lineStrokes[i] = new BasicStroke(3.0f * (float) i / (this.coolDownMax), BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL);
        }
        this.lineStroke = this.lineStrokes[this.coolDownMax - 1];
        this.doInit(context, x, y);
    }

    private EnemyMob findEnemy() {
        int distance2;
        int dx, dy;
        EnemyMob e, e2 = null;
        int progression = 0;

        if (this.context.enemies != null) {
            for (int i = 0; i < this.context.enemies.length; i++) {
                e = this.context.enemies[i];
                if (e.validTarget(EnemyMob.type.Normal)) {
                    dx = e.getX() - this.centerX;
                    dy = e.getY() - this.centerY;
                    distance2 = dx * dx + dy * dy;
                    if (distance2 < this.rangeReal2) {
                        if (e.getProgression() > progression) {
                            e2 = e;
                            progression = e2.getProgression();
                        }
                    }
                }
            }
            //if (progression != 0) return e2;
        }
        return e2;
    }

    public void doTick(int gameTime) {
        if (this.coolDown > 0) {
            this.coolDown--;
            this.lineStroke = this.lineStrokes[this.coolDown];

        } else {
            this.enemy = this.findEnemy();
            if (this.enemy != null) {
                this.enemy.doDamage(this.damageCurrent);
                this.coolDown = this.coolDownMax;
            }
        }
    }

    public void paintEffect(Graphics2D g2, int gameTime) {
        Stroke defaultStroke = g2.getStroke();

        if (this.enemy != null) {
            g2.setColor(this.lineColor);
            g2.setStroke(this.lineStroke);
            int ex = this.enemy.getX();
            int ey = this.enemy.getY();
            g2.draw(new Line2D.Float(this.centerX, this.centerY, ex, ey));
        }
        g2.setStroke(defaultStroke);
    }

    public String getInfoString() {
        return "Triangle tower\n\n" +
                super.getInfoString() +
                "Targets first one";
    }

    public String getStatusString() {
        return "Triangle tower\n\n" +
                super.getStatusString() +
                "Targets first one";
    }

}
