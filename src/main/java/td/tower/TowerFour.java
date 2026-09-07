package td.tower;

import td.enemy.EnemyMob;
import td.tower.targeting.InRangeTargetQuery;
import td.util.Context;

import java.util.List;

public final class TowerFour extends AbstractTower {

    public static final int price = 25;
    public static final int damage = 200;
    public static final float range = 1.5f;

    private boolean fire = false;
    private int ghosts = 0;

    public TowerFour(Context context, int x, int y) {
        super(TowerFactory.type.fourth, price, damage, range);
        this.name = "tower4";
        this.doInit(context, x, y);
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

    public boolean isFiring() {
        return this.fire;
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitTowerFour(this);
    }

    public String getStatusString() {
        return "Stardust tower\n\n" +
                super.getStatusString() +
                "Hurts everyone in range";
    }

}
