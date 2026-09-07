package td.tower;

import td.damage.Damage;
import td.enemy.EnemyMob;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.OfTypeTargetQuery;
import td.tower.targeting.TargetQuery;
import td.util.Context;

import java.util.List;

public final class TowerFour extends AbstractTower {

    public static final int price = 25;
    public static final int damage = 200;
    public static final float range = 1.5f;

    private boolean fire = false;

    public TowerFour(Context context, int x, int y) {
        super(TowerFactory.type.fourth, price, damage, range);
        this.name = "tower4";
        this.doInit(context, x, y);
    }

    public void doTick(int gameTime) {
        TargetQuery inRange = InRangeTargetQuery.anyType(this.centerX, this.centerY, this.rangeReal);
        List<EnemyMob> enemies = inRange.matching(this.context);
        List<EnemyMob> ghosts = inRange.and(OfTypeTargetQuery.of(EnemyMob.type.Invisible)).matching(this.context);

        if (enemies.size() > ghosts.size()) {
            this.fire = true;
            for (EnemyMob enemy : enemies) {
                enemy.doDamage(Damage.of(this.damageCurrent));
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
