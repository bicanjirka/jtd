package td.tower;

import td.damage.Damage;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.OfTypeTargetQuery;
import td.tower.targeting.TargetQuery;
import td.tower.upgrade.DamageDealtCondition;
import td.tower.upgrade.UpgradeCondition;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.List;

/**
 * "Pulse tower" - short range, no cooldown, damages everything in range every tick,
 * ghosts included. It only fires when at least one non-ghost is in range, so its visible
 * pulse never gives away a ghost that is alone in range - but once something else triggers
 * it, that ghost takes the damage too.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)  // the fire flag, set and read within a tick
public final class PulseTower extends AbstractTower {

    public static final int PRICE = 25;
    public static final int DAMAGE = 200;
    public static final float RANGE = 1.5f;

    /**
     * More damage - earned by this tower having already proven itself against real targets.
     */
    private static final UpgradeNode OVERLOAD_CORE = UpgradeNode.of("pulse.head.overload_core", UpgradeSlot.HEAD,
            "Overload Core", 30)
            .withBuff(TowerBuff.damage(0.5f))
            .withRequires(UpgradeCondition.slotEmpty(UpgradeSlot.HEAD))
            .withGate(new DamageDealtCondition(15000));
    /**
     * More range - a straightforward money-gated specialization needing no track record.
     */
    private static final UpgradeNode EXPANDED_FIELD = UpgradeNode.of("pulse.head.expanded_field", UpgradeSlot.HEAD,
            "Expanded Field", 25)
            .withBuff(TowerBuff.range(0.3f))
            .withRequires(UpgradeCondition.slotEmpty(UpgradeSlot.HEAD));
    private static final UpgradeTree TREE = UpgradeTree.of(OVERLOAD_CORE, EXPANDED_FIELD);

    private boolean fire = false;

    public PulseTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.PULSE, PRICE, new TowerBaseStats(DAMAGE, RANGE, 0), context, x, y);
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    public void doTick(int gameTime) {
        TargetQuery inRange = InRangeTargetQuery.anyType(this.centerX, this.centerY, this.rangeReal());
        List<EnemyMob> enemies = inRange.matching(this.context.enemies());
        List<EnemyMob> ghosts = inRange.and(OfTypeTargetQuery.of(EnemyMob.Type.INVISIBLE)).matching(this.context.enemies());

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
        return "Pulse tower\n\n" +
                super.getInfoString() +
                "Hurts everyone in range";
    }

    public String getStatusString() {
        return "Pulse tower\n\n" +
                super.getStatusString() +
                "Hurts everyone in range";
    }

}
