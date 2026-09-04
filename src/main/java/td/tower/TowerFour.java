package td.tower;

import td.enemy.EnemyMob;
import td.util.Context;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

public class TowerFour extends AbstractTower {

    public static int price = 25;
    public static int damage = 200;
    public static float range = 1.5f;

    private final Color transColor;
    private boolean fire = false;
    private Shape spread;
    private int ghosts = 0;

    public TowerFour(Context context, int x, int y) {
        super(TowerFactory.type.fourth, price, damage, range);
        this.name = "tower4";
        this.lineColor = Color.ORANGE;
        this.transColor = new Color(this.lineColor.getRed(), this.lineColor.getGreen(), this.lineColor.getBlue(), 80);
        this.doInit(context, x, y);
        this.spread = new Ellipse2D.Float(this.centerX - this.rangeReal, this.centerY - this.rangeReal, this.rangeReal * 2, this.rangeReal * 2);
    }

    private EnemyMob[] findEnemiesInRange(int x, int y, float r) {
        List<EnemyMob> tempEnemies = new ArrayList<EnemyMob>();
        EnemyMob e;
        float r2 = r * r;
        int dx, dy, d2;
        if (this.context.enemies != null) {
            for (int i = 0; i < this.context.enemies.length; i++) {
                e = this.context.enemies[i];
                if (e.validTarget()) {
                    dx = e.getX() - x;
                    dy = e.getY() - y;
                    d2 = dx * dx + dy * dy;
                    if (d2 < r2) {
                        tempEnemies.add(e);
                        if (e.validTarget(EnemyMob.type.Invisible)) {
                            this.ghosts++;
                        }
                    }
                }
            }
        }
        EnemyMob[] retVal = new EnemyMob[tempEnemies.size()];
        retVal = tempEnemies.toArray(retVal);
        return retVal;
    }

    public void doTick(int gameTime) {
        this.ghosts = 0;
        EnemyMob[] enemies = this.findEnemiesInRange(this.centerX, this.centerY, this.rangeReal);
        if (enemies.length > this.ghosts) {
            this.fire = true;
            for (int i = 0; i < enemies.length; i++) {
                enemies[i].doDamage(this.damageCurrent);
            }
        } else {
            this.fire = false;
        }
    }

    public void paintEffect(Graphics2D g2, int gameTime) {
        if (this.fire) {
            g2.setColor(this.transColor);
            g2.fill(this.spread);
        }
    }

    protected void calcDamageRange() {
        super.calcDamageRange();
        this.spread = new Ellipse2D.Float(this.centerX - this.rangeReal, this.centerY - this.rangeReal, this.rangeReal * 2, this.rangeReal * 2);
    }

    public String getStatusString() {
        return "Stardust tower\n\n" +
                super.getStatusString() +
                "Hurts everyone in range";
    }

}
