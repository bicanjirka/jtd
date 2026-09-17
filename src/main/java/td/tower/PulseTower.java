package td.tower;

import td.damage.Damage;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.OfTypeTargetQuery;
import td.tower.targeting.TargetQuery;
import td.tower.upgrade.DamageDealtCondition;
import td.tower.upgrade.UpgradeCondition;
import td.tower.upgrade.UpgradePath;
import td.util.GameWorld;

import java.util.List;

/**
 * "Stardust tower" - short range, no cooldown, damages everything in range every tick,
 * ghosts included. It only fires when at least one non-ghost is in range, so its visible
 * pulse never gives away a ghost that is alone in range - but once something else triggers
 * it, that ghost takes the damage too.
 */
public final class PulseTower extends AbstractTower {

    public static final int PRICE = 25;
    public static final int DAMAGE = 200;
    public static final float RANGE = 1.5f;

    /** More damage - earned by this tower having already proven itself against real targets. */
    private static final UpgradePath OVERLOAD_CORE = new UpgradePath(
            "Overload Core", 30, new TowerBuff(0.5f, 0f, 0f, 0f), new DamageDealtCondition(15000));
    /** More range - a straightforward money-gated specialization needing no track record. */
    private static final UpgradePath EXPANDED_FIELD = new UpgradePath(
            "Expanded Field", 25, new TowerBuff(0f, 0.3f, 0f, 0f), UpgradeCondition.always());
    private static final List<UpgradePath> PATHS = List.of(OVERLOAD_CORE, EXPANDED_FIELD);

    private boolean fire = false;

    public PulseTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.PULSE, PRICE, DAMAGE, RANGE, 0, context, x, y);
    }

    @Override
    public List<UpgradePath> availablePaths() {
        return PATHS;
    }

    public void doTick(int gameTime) {
        TargetQuery inRange = InRangeTargetQuery.anyType(this.centerX, this.centerY, this.rangeReal());
        List<EnemyMob> enemies = inRange.matching(this.context.enemies());
        List<EnemyMob> ghosts = inRange.and(OfTypeTargetQuery.of(EnemyMob.Type.Invisible)).matching(this.context.enemies());

        if (enemies.size() > ghosts.size()) {
            this.fire = true;
            for (EnemyMob enemy : enemies) {
                this.dealDamage(enemy, Damage.physical(this.damageCurrent()));
            }
        } else {
            this.fire = false;
        }
    }

    public boolean isFiring() {
        return this.fire;
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitPulseTower(this);
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
