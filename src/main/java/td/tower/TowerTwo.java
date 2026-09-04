package td.tower;

import td.enemy.EnemyMob;
import td.util.Context;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

public class TowerTwo extends AbstractTower {

    public static int price = 15;
    public static int damage = 1600;
    public static float range = 3.2f;
    public static float spreadRadiusBase = 1.75f;

    private final float spreadRadius;
    private int coolDown = 0;

    private Color transLineColor;
    private Color transShapeColor;
    private final Stroke[] lineStrokes;
    private Stroke lineStroke;
    private Shape spread;

    private EnemyMob enemy;
    private EnemyMob[] currentTargets;


    public TowerTwo(Context context, int x, int y) {
        super(TowerFactory.type.second, price, damage, range);
        this.name = "tower2";
        this.coolDownMax = 19;
        this.lineColor = Color.RED;
        this.transLineColor = new Color(this.lineColor.getRed(), this.lineColor.getGreen(), this.lineColor.getBlue(), 160);
        this.transLineColor = new Color(this.lineColor.getRed(), this.lineColor.getGreen(), this.lineColor.getBlue(), 80);
        this.lineStrokes = new Stroke[this.coolDownMax];
        for (int i = 0; i < this.coolDownMax; i++) {
            this.lineStrokes[i] = new BasicStroke(3.0f * (float) i / this.coolDownMax, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL);
        }
        this.lineStroke = this.lineStrokes[this.coolDownMax - 1];
        this.spreadRadius = spreadRadiusBase * context.scale;
        this.doInit(context, x, y);
    }

    private EnemyMob[] findEnemiesInRangeVisible(int x, int y, float r) {
        List<EnemyMob> tempEnemies = new ArrayList<EnemyMob>();
        float r2 = r * r;
        int dx, dy, d2;
        if (this.context.enemies != null) {
            for (int i = 0; i < this.context.enemies.length; i++) {
                EnemyMob e = this.context.enemies[i];
                if (e.validTarget(EnemyMob.type.Normal)) {
                    dx = e.getX() - x;
                    dy = e.getY() - y;
                    d2 = dx * dx + dy * dy;
                    if (d2 < r2) {
                        tempEnemies.add(e);
                    }
                }
            }
        }
        EnemyMob[] retVal = new EnemyMob[tempEnemies.size()];
        retVal = tempEnemies.toArray(retVal);
        return retVal;
    }

    private EnemyMob[] findEnemiesInRange(int x, int y, float r) {
        List<EnemyMob> tempEnemies = new ArrayList<EnemyMob>();
        float r2 = r * r;
        int dx, dy, d2;
        if (this.context.enemies != null) {
            for (int i = 0; i < this.context.enemies.length; i++) {
                EnemyMob e = this.context.enemies[i];
                if (e.validTarget()) {
                    dx = e.getX() - x;
                    dy = e.getY() - y;
                    d2 = dx * dx + dy * dy;
                    if (d2 < r2) {
                        tempEnemies.add(e);
                    }
                }
            }
        }
        EnemyMob[] retVal = new EnemyMob[tempEnemies.size()];
        retVal = tempEnemies.toArray(retVal);
        return retVal;
    }

    public void doTick(int gameTime) {
        if (this.coolDown > 0) {
            this.coolDown--;
            this.lineStroke = this.lineStrokes[this.coolDown];
        } else {
            EnemyMob[] enemies = this.findEnemiesInRangeVisible(this.centerX, this.centerY, this.rangeReal);

            if (enemies.length > 0) {
                this.enemy = enemies[(int) (Math.random() * enemies.length)];
                int ex = this.enemy.getX();
                int ey = this.enemy.getY();
                int dx, dy, r2;
                int damage;

                EnemyMob enemy2;
                this.currentTargets = this.findEnemiesInRange(ex, ey, this.spreadRadius);

                for (int i = 0; i < this.currentTargets.length; i++) {
                    enemy2 = this.currentTargets[i];
                    dx = ex - enemy2.getX();
                    dy = ey - enemy2.getY();
                    r2 = dx * dx + dy * dy;
                    damage = Math.round(this.damageCurrent * (1 - r2 / (this.spreadRadius * this.spreadRadius)));
                    enemy2.doDamage(damage);
                }

                this.coolDown = this.coolDownMax;
                this.spread = new Ellipse2D.Float(ex - this.spreadRadius, ey - this.spreadRadius, spreadRadius * 2, spreadRadius * 2);
            } else {
                this.enemy = null;
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
            g2.setColor(this.transLineColor);
            for (int i = 0; i < this.currentTargets.length; i++) {
                g2.draw(new Line2D.Float(ex, ey, currentTargets[i].getX(), currentTargets[i].getY()));
            }
            g2.setStroke(defaultStroke);
        }
        if (this.coolDown > this.coolDownMax - 1) {
            g2.setColor(this.transShapeColor);
            g2.fill(this.spread);
        }
    }

    public String getInfoString() {
        return "Circle tower\n\n" +
                super.getInfoString() +
                "Splash radius " + spreadRadiusBase + "\n" +
                "Targets random";
    }

    public String getStatusString() {
        return "Circle tower\n\n" +
                super.getStatusString() +
                "Splash radius " + spreadRadiusBase + "\n" +
                "Targets random";
    }

}
