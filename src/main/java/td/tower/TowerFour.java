package td.tower;

import td.enemy.EnemyMob;
import td.tower.targeting.InRangeTargetQuery;
import td.util.Context;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.List;

public final class TowerFour extends AbstractTower {

    public static final int price = 25;
    public static final int damage = 200;
    public static final float range = 1.5f;

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

    private List<EnemyMob> findEnemiesInRange(int x, int y, float r) {
        List<EnemyMob> matches = InRangeTargetQuery.anyType(x, y, r).matching(this.context);
        this.ghosts = (int) matches.stream().filter(e -> e.validTarget(EnemyMob.type.Invisible)).count();
        return matches;
    }

    public void doTick(int gameTime) {
        List<EnemyMob> enemies = this.findEnemiesInRange(this.centerX, this.centerY, this.rangeReal);
        if (enemies.size() > this.ghosts) {
            this.fire = true;
            for (EnemyMob enemy : enemies) {
                enemy.doDamage(this.damageCurrent);
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
