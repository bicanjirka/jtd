package td.tower;

import td.damage.Damage;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.upgrade.DamageDealtCondition;
import td.tower.upgrade.KillCountCondition;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeCondition;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.List;

/**
 * Short range, no cooldown: damages everything in range every tick, invisible enemies included.
 * Fires only while a visible enemy is in range, so it never reveals an invisible enemy alone in
 * range; an upgrade removes that restraint.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class PulseTower extends AbstractTower {

    public static final int PRICE = 25;
    public static final int DAMAGE = 200;
    public static final float RANGE = 1.5f;

    private static final UpgradeNode BASE_RANGE = StandardBaseSlot.rangeNode(15);
    private static final UpgradeNode AWAKEN = StandardBaseSlot.awakenNode(25);

    private static final UpgradeNode OVERCHARGED_COILS_1 = UpgradeNode.of("pulse.head.overcharged_coils.1",
            UpgradeSlot.HEAD, "Overcharged Coils", 30)
            .withBuff(TowerBuff.damage(0.3f))
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD))
            .withGate(new DamageDealtCondition(10000));
    private static final UpgradeNode OVERCHARGED_COILS_2 = UpgradeNode.of("pulse.head.overcharged_coils.2",
            UpgradeSlot.HEAD, "Overcharged Coils II", 45)
            .withBuff(TowerBuff.damage(0.25f).withCritChance(0.1f))
            .withRequires(UpgradeCondition.owns(OVERCHARGED_COILS_1.id()))
            .withGate(new KillCountCondition(20));
    private static final UpgradeNode RESONANT_FIELD_1 = UpgradeNode.of("pulse.head.resonant_field.1",
            UpgradeSlot.HEAD, "Resonant Field", 25)
            .withBuff(TowerBuff.range(0.2f))
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD))
            .withGate(new KillCountCondition(10))
            .withExtraEffect("damages invisible enemies every tick, cover no longer required");
    /** Its reveal effect is not implemented yet (TODO.md). */
    private static final UpgradeNode RESONANT_FIELD_2 = UpgradeNode.of("pulse.head.resonant_field.2",
            UpgradeSlot.HEAD, "Resonant Field II", 38)
            .withBuff(TowerBuff.range(0.15f))
            .withRequires(UpgradeCondition.owns(RESONANT_FIELD_1.id()))
            .withGate(new DamageDealtCondition(20000))
            .withExtraEffect("any invisible enemy it hits is revealed to every tower for 2s");
    /** Not implemented yet (TODO.md). */
    private static final UpgradeNode WARDING_FIELD = UpgradeNode.of("pulse.special.warding_field", UpgradeSlot.SPECIAL,
            "Warding Field", 50)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new KillCountCondition(20))
            .withExtraEffect("each tick, everything hit has a 10% chance to gain 1 Vulnerable stack (cap 3)");

    private static final UpgradeTree TREE = UpgradeTree.of(BASE_RANGE, AWAKEN, OVERCHARGED_COILS_1,
            OVERCHARGED_COILS_2, RESONANT_FIELD_1, RESONANT_FIELD_2, WARDING_FIELD);

    private volatile boolean resonantField = false;
    private boolean fire = false;

    public PulseTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.PULSE, PRICE, new TowerBaseStats(DAMAGE, RANGE, 0), context, x, y);
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        if (node.equals(RESONANT_FIELD_1)) {
            this.resonantField = true;
        }
    }

    public void doTick(int gameTime) {
        List<EnemyMob> enemies = InRangeTargetQuery.everyone(this.centerX, this.centerY, this.rangeReal())
                .matching(this.context.enemies());
        boolean triggered = this.resonantField
                ? !enemies.isEmpty()
                : enemies.stream().anyMatch(EnemyMob::canBeTargeted);
        if (triggered) {
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

    @Override
    protected List<BehaviourLine> behaviours() {
        return List.of(new BehaviourLine(BehaviourMarker.TARGETING, "Hits", "all in range"));
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitPulseTower(this);
    }
}
