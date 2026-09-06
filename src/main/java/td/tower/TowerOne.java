package td.tower;

import td.enemy.EnemyMob;
import td.tower.targeting.FurthestAlongPathSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.util.Context;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.util.List;

public final class TowerOne extends AbstractTower {

    public static final int price = 10;
    public static final int damage = 4000;
    public static final float range = 3.8f;

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
        List<EnemyMob> inRange = InRangeTargetQuery.ofType(this.centerX, this.centerY, this.rangeReal, EnemyMob.type.Normal)
                .matching(this.context);
        return new FurthestAlongPathSelector().selectFrom(inRange).orElse(null);
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
