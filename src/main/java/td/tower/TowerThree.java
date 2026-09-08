package td.tower;

import td.damage.Damage;
import td.enemy.EnemyMob;
import td.tower.targeting.InRangeAfterIndexQuery;
import td.util.Context;
import td.util.WaveStartListener;

import java.util.OptionalInt;

public final class TowerThree extends AbstractTower implements WaveStartListener {

    public static final int price = 20;
    public static final int damage = 1600;
    public static final float range = 5.2f;

    private int fireAt = -1;
    private int[] enemyX;
    private int[] enemyY;
    private int coolDown = 0;
    private final int coolDownRecharge = 39;

    private int[] lineSteps;

    public TowerThree(Context context, int x, int y) {
        super(TowerFactory.type.third, price, damage, range);
        this.name = "tower3";
        this.coolDownMax = 1;
        this.doInit(context, x, y);

        this.context.addWaveStartListener(this);
        this.waveStarted();
    }

    private int findEnemy(int preferedEnemyNr) {
        OptionalInt found = new InRangeAfterIndexQuery(this.centerX, this.centerY, this.rangeReal, EnemyMob.type.Normal)
                .nextIndexAfter(this.context, preferedEnemyNr);
        if (found.isEmpty() && preferedEnemyNr != -1) {
            this.coolDown = this.coolDownRecharge;
        }
        return found.orElse(-1);
    }

    public void doTick(int gameTime) {

        if (this.coolDown > 0) {
            this.coolDown--;
        } else {
            int enemyNr = this.findEnemy(this.fireAt);
            this.fireAt = enemyNr;
            if (enemyNr >= 0) {
                EnemyMob enemy = this.context.getEnemies()[enemyNr];
                this.enemyX[enemyNr] = (int) enemy.getX();
                this.enemyY[enemyNr] = (int) enemy.getY();
                this.dealDamage(enemy, Damage.of(this.damageCurrent));
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

    public int[] getEnemyX() {
        return this.enemyX;
    }

    public int[] getEnemyY() {
        return this.enemyY;
    }

    public int[] getLineSteps() {
        return this.lineSteps;
    }

    public int getCoolDownRecharge() {
        return this.coolDownRecharge;
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitTowerThree(this);
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
        int length = this.context.getEnemies().length;
        this.lineSteps = new int[length];
        this.enemyX = new int[length];
        this.enemyY = new int[length];
    }

}
