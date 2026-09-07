package td.tower;

import td.damage.Damage;
import td.enemy.EnemyMob;
import td.tower.targeting.FurthestAlongPathSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.util.Context;

import java.util.List;

public final class TowerOne extends AbstractTower {

    public static final int price = 10;
    public static final int damage = 4000;
    public static final float range = 3.8f;

    private int coolDown = 0;

    private EnemyMob currentTarget;

    public TowerOne(Context context, int x, int y) {
        super(TowerFactory.type.first, price, damage, range);
        this.coolDownMax = 39;
        this.name = "tower1";
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
        } else {
            this.currentTarget = this.findEnemy();
            if (this.currentTarget != null) {
                this.currentTarget.doDamage(Damage.of(this.damageCurrent));
                this.coolDown = this.coolDownMax;
            }
        }
    }

    public EnemyMob getCurrentTarget() {
        return this.currentTarget;
    }

    public float getCoolDownFraction() {
        return (float) this.coolDown / this.coolDownMax;
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitTowerOne(this);
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
