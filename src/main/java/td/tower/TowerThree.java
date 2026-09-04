package td.tower;

import td.enemy.EnemyMob;
import td.util.Context;
import td.util.WaveStartListener;

import java.awt.*;
import java.awt.geom.Line2D;

public class TowerThree extends AbstractTower implements WaveStartListener {

    public static final int price = 20;
    public static final int damage = 1600;
    public static final float range = 5.2f;

    private int fireAt = -1;
    private int[] enemyX;
    private int[] enemyY;
    private int coolDown = 0;
    private final int coolDownRecharge = 39;

    private final Stroke[] lineStrokes;
    private int[] lineSteps;

    public TowerThree(Context context, int x, int y) {
        super(TowerFactory.type.third, price, damage, range);
        this.name = "tower3";
        this.coolDownMax = 1;
        this.lineColor = Color.YELLOW;
        int coolDownHalf = this.coolDownRecharge / 2;
        this.lineStrokes = new Stroke[coolDownHalf];
        for (int i = 0; i < this.lineStrokes.length; i++) {
            this.lineStrokes[i] = new BasicStroke(3.0f * (float) i / this.coolDownRecharge, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL);
        }
        this.doInit(context, x, y);

        this.context.addWaveStartListener(this);
        this.waveStarted();
    }

    private int findEnemy(int preferedEnemyNr) {
        int distance2;
        int dx, dy;
        EnemyMob e;
        if (this.context.enemies != null) {
            for (int i = preferedEnemyNr + 1; i < this.context.enemies.length; i++) {
                e = this.context.enemies[i];
                if (e.validTarget(EnemyMob.type.Normal)) {
                    dx = e.getX() - this.centerX;
                    dy = e.getY() - this.centerY;
                    distance2 = dx * dx + dy * dy;
                    if (distance2 < this.rangeReal2) {
                        return i;
                    }
                }
            }
            if (preferedEnemyNr != -1) {
                this.coolDown = this.coolDownRecharge;
            }
        }
        return -1;
    }

    public void doTick(int gameTime) {

        if (this.coolDown > 0) {
            this.coolDown--;
        } else {
            int enemyNr = this.findEnemy(this.fireAt);
            this.fireAt = enemyNr;
            if (enemyNr >= 0) {
                EnemyMob enemy = this.context.enemies[enemyNr];
                this.enemyX[enemyNr] = enemy.getX();
                this.enemyY[enemyNr] = enemy.getY();
                enemy.doDamage(this.damageCurrent);
                this.lineSteps[enemyNr] = this.coolDownRecharge / 2;
                this.coolDown = this.coolDownMax;
            }
        }
        for (int i = 0; i < this.lineSteps.length; i++) {
            if (this.lineSteps[i] > 0) {
                this.lineSteps[i]--;
            }
        }
    }

    public void paintEffect(Graphics2D g2, int gameTime) {
        g2.setColor(this.lineColor);
        for (int i = 0; i < this.lineSteps.length; i++) {
            if (this.lineSteps[i] > 0) {
                g2.setStroke(this.lineStrokes[this.lineSteps[i]]);
                g2.draw(new Line2D.Float(this.centerX, this.centerY, this.enemyX[i], this.enemyY[i]));
            }
        }
    }

    public String getInfoString() {
        return "Sunshine tower\n\n" +
                super.getInfoString() +
                "Recharge: " + (this.coolDownRecharge + 1) / 20f + "s\n" +
                "Shoots all enemies in range, one by one. Once everyone damaged, needs time to recharge";
    }

    public String getStatusString() {
        return "Sunshine tower\n\n" +
                super.getStatusString() +
                "Recharge: " + (this.coolDownRecharge + 1) / 20f + "s\n" +
                "Shoots all enemies in range, one by one. Once everyone damaged, needs time to recharge";
    }

    public void doCleanup() {
        super.doCleanup();
        this.context.removeWaveStartListener(this);
    }

    @Override
    public void waveStarted() {
        if (this.context.enemies != null) {
            int length = this.context.enemies.length;
            this.lineSteps = new int[length];
            this.enemyX = new int[length];
            this.enemyY = new int[length];
        } else {
            this.lineSteps = new int[0];
            this.enemyX = new int[0];
            this.enemyY = new int[0];
        }
    }

}
