package td.tower;

import td.damage.Damage;
import td.enemy.EnemyMob;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.OfTypeTargetQuery;
import td.tower.targeting.TargetQuery;
import td.util.GameWorld;

import java.util.List;

/**
 * "Stardust tower" - short range, no cooldown, damages everything in range every tick,
 * ghosts included. It only fires when at least one non-ghost is in range, so its visible
 * pulse never gives away a ghost that is alone in range - but once something else triggers
 * it, that ghost takes the damage too.
 */
public final class TowerFour extends AbstractTower {

    public static final int price = 25;
    public static final int damage = 200;
    public static final float range = 1.5f;

    private boolean fire = false;

    public TowerFour(GameWorld context, int x, int y) {
        super(TowerFactory.type.fourth, price, damage, range);
        this.doInit(context, x, y);
    }

    public void doTick(int gameTime) {
        TargetQuery inRange = InRangeTargetQuery.anyType(this.centerX, this.centerY, this.rangeReal);
        List<EnemyMob> enemies = inRange.matching(this.context.getEnemyRegistry());
        List<EnemyMob> ghosts = inRange.and(OfTypeTargetQuery.of(EnemyMob.type.Invisible)).matching(this.context.getEnemyRegistry());

        if (enemies.size() > ghosts.size()) {
            this.fire = true;
            for (EnemyMob enemy : enemies) {
                this.dealDamage(enemy, Damage.of(this.damageCurrent));
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

    public String getInfoString() {
        return "Stardust tower\n\n" +
                super.getInfoString() +
                "Hurts everyone in range";
    }

    public String getStatusString() {
        return "Stardust tower\n\n" +
                super.getStatusString() +
                "Hurts everyone in range";
    }

}
