package td.tower;

import td.damage.Damage;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.upgrade.ExclusiveChoice;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.List;

/**
 * Short range, no cooldown: damages everything in range every tick, invisible enemies included.
 * Fires only while a visible enemy is in range, so it never reveals an invisible enemy alone in
 * range; an upgrade removes that restraint.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class PulseTower extends AbstractTower {

    public static final int PRICE = 25;
    public static final float DAMAGE_POINTS = 2f;
    public static final float RANGE = 1.5f;

    private static final UpgradeNode OVERCHARGED_COILS_1 = UpgradeTier.HEAD_1.node("pulse.head.overcharged_coils.1",
            "Overcharged Coils", PRICE)
            .withBuff(TowerBuff.damage(0.3f));
    private static final UpgradeNode OVERCHARGED_COILS_2 = UpgradeTier.HEAD_2.node("pulse.head.overcharged_coils.2",
            "Overcharged Coils II", PRICE)
            .withBuff(TowerBuff.damage(0.25f).withCritChance(0.1f))
            .after(OVERCHARGED_COILS_1);
    private static final UpgradeNode RESONANT_FIELD_1 = UpgradeTier.HEAD_1.node("pulse.head.resonant_field.1",
            "Resonant Field", PRICE)
            .withBuff(TowerBuff.range(0.2f))
            .withExtraEffect("damages invisible enemies every tick, cover no longer required");
    private static final UpgradeNode RESONANT_FIELD_2 = UpgradeTier.HEAD_2.node("pulse.head.resonant_field.2",
            "Resonant Field II", PRICE)
            .withBuff(TowerBuff.range(0.15f))
            .after(RESONANT_FIELD_1)
            .withExtraEffect("any invisible enemy it hits is revealed to every tower for 2s");
    private static final UpgradeNode WARDING_FIELD = UpgradeTier.SPECIAL.node("pulse.special.warding_field",
            "Warding Field", PRICE)
            .withExtraEffect("each tick, everything hit has a 10% chance to gain 1 Vulnerable stack (cap 3)");

    /** Chance per tick that each enemy hit gains a vulnerability stack. */
    private static final double WARDING_FIELD_CHANCE = 0.1;
    private static final float REVEAL_SECONDS = 2f;

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE))
            .with(OVERCHARGED_COILS_1, OVERCHARGED_COILS_2, RESONANT_FIELD_1, RESONANT_FIELD_2, WARDING_FIELD)
            .withChoice(ExclusiveChoice.oneOf(OVERCHARGED_COILS_1, RESONANT_FIELD_1));

    private volatile boolean resonantField = false;
    private boolean fire = false;

    public PulseTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.PULSE, new TowerBaseStats(DAMAGE_POINTS, RANGE, 0), context, x, y);
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
                this.dealPeriodicDamage(enemy, Damage.physical(this.damageCurrent()));
                this.applyUpgradeEffects(enemy);
            }
        } else {
            this.fire = false;
        }
    }

    /** Warding Field's chance of a stack, and Resonant Field II's reveal of a hidden enemy it hits. */
    private void applyUpgradeEffects(EnemyMob enemy) {
        if (this.upgrades().owns(WARDING_FIELD.id()) && this.context.random().nextDouble() < WARDING_FIELD_CHANCE) {
            this.applyVulnerable(enemy, 1);
        }
        if (this.upgrades().owns(RESONANT_FIELD_2.id()) && enemy.isHidden()) {
            this.reveal(enemy, Math.round(REVEAL_SECONDS * TICKS_PER_SECOND));
        }
    }

    public boolean isFiring() {
        return this.fire;
    }

    @Override
    protected List<BehaviourLine> behaviours() {
        List<BehaviourLine> lines = new ArrayList<>();
        lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Hits", "all in range"));
        if (this.upgrades().owns(WARDING_FIELD.id())) {
            lines.add(new BehaviourLine(BehaviourMarker.VULNERABLE, "Each tick", BehaviourLine.percent((float) WARDING_FIELD_CHANCE) + " vulnerable"));
        }
        if (this.upgrades().owns(RESONANT_FIELD_2.id())) {
            lines.add(new BehaviourLine(BehaviourMarker.REVEAL, "Reveals hidden", BehaviourLine.seconds(Math.round(REVEAL_SECONDS * TICKS_PER_SECOND))));
        }
        return lines;
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitPulseTower(this);
    }
}
