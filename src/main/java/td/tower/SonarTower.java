package td.tower;

import td.damage.Damage;
import td.damage.DamageType;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.upgrade.ClusterCondition;
import td.tower.upgrade.DamageDealtCondition;
import td.tower.upgrade.KillCountCondition;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeCondition;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.wave.WaveStartListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A beam sweeps the full circle and hits every visible enemy in range as it passes its bearing. No
 * cooldown: fire rate is rotation speed, and enemies at the same bearing are all hit on the same
 * tick.
 */
public final class SonarTower extends AbstractTower implements WaveStartListener {

    public static final int PRICE = 20;
    public static final int DAMAGE = 1600;
    public static final float RANGE = 5.2f;
    /** Seconds per revolution; this tower's fire rate. */
    public static final float SECONDS_PER_REVOLUTION = 2f;

    /** How long a hit stays drawn. */
    private static final int HIT_FLASH_TICKS = 8;

    private static final UpgradeNode BASE_RANGE = StandardBaseSlot.rangeNode(12);
    private static final UpgradeNode AWAKEN = StandardBaseSlot.awakenNode(20);

    private static final UpgradeNode TWIN_ARRAY_1 = UpgradeNode.of("sonar.head.twin_array.1", UpgradeSlot.HEAD,
            "Twin Array", 35)
            .withBuff(TowerBuff.damage(0.25f))
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD))
            .withGate(new DamageDealtCondition(10000));
    private static final UpgradeNode TWIN_ARRAY_2 = UpgradeNode.of("sonar.head.twin_array.2", UpgradeSlot.HEAD,
            "Twin Array II", 53)
            .withBuff(TowerBuff.damage(0.25f).withCritChance(0.1f))
            .withRequires(UpgradeCondition.owns(TWIN_ARRAY_1.id()))
            .withGate(new DamageDealtCondition(20000));
    /** Not implemented yet (TODO.md). */
    private static final UpgradeNode TWIN_ARRAY_3 = UpgradeNode.of("sonar.head.twin_array.3", UpgradeSlot.HEAD,
            "Twin Array III", 70)
            .withRequires(UpgradeCondition.owns(TWIN_ARRAY_2.id()))
            .withGate(new KillCountCondition(25))
            .withExtraEffect("a second turret, facing the opposite direction");
    private static final UpgradeNode LONG_REACH_1 = UpgradeNode.of("sonar.head.long_reach.1", UpgradeSlot.HEAD,
            "Long Reach", 30)
            .withBuff(TowerBuff.critChance(0.15f))
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD))
            .withGate(new KillCountCondition(10));
    /** Not implemented yet (TODO.md). */
    private static final UpgradeNode LONG_REACH_2 = UpgradeNode.of("sonar.head.long_reach.2", UpgradeSlot.HEAD,
            "Long Reach II", 45)
            .withRequires(UpgradeCondition.owns(LONG_REACH_1.id()))
            .withGate(new DamageDealtCondition(20000))
            .withExtraEffect("damage scales up to +100% at max range");
    /** Not implemented yet (TODO.md). */
    private static final UpgradeNode WIDE_BAND = UpgradeNode.of("sonar.special.wide_band", UpgradeSlot.SPECIAL,
            "Wide Band", 40)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new ClusterCondition(2))
            .withExtraEffect("each revolution briefly reveals invisible enemies to every tower");
    /** Not implemented yet (TODO.md). */
    private static final UpgradeNode MARK_ON_SWEEP = UpgradeNode.of("sonar.special.mark_on_sweep", UpgradeSlot.SPECIAL,
            "Mark on Sweep", 40)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new KillCountCondition(15))
            .withExtraEffect("a beam hit marks its target; the next hit on it is a guaranteed crit");
    private static final UpgradeNode PIERCING_TONE = UpgradeNode.of("sonar.special.piercing_tone", UpgradeSlot.SPECIAL,
            "Piercing Tone", 40)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new DamageDealtCondition(20000))
            .withExtraEffect("bonus magic damage against physically armored/shielded enemies, up to +50%");
    /** Piercing Tone's bonus is the target's physical reduction, as a share of this hit, capped here. */
    private static final float PIERCING_TONE_MAX_BONUS = 0.5f;

    private static final UpgradeTree TREE = UpgradeTree.of(BASE_RANGE, AWAKEN, TWIN_ARRAY_1, TWIN_ARRAY_2,
            TWIN_ARRAY_3, LONG_REACH_1, LONG_REACH_2, WIDE_BAND, MARK_ON_SWEEP, PIERCING_TONE);

    private final List<SonarHit> recentHits = new ArrayList<>();
    private volatile SonarSweep sweep = SonarSweep.perRevolution(SECONDS_PER_REVOLUTION, TICKS_PER_SECOND);

    public SonarTower(GameWorld context, int x, int y) {
        // No cooldown: the cadence is the sweep rate.
        super(TowerFactory.Type.SONAR, PRICE, new TowerBaseStats(DAMAGE, RANGE, 0), context, x, y);
        this.context.waves().addListener(this);
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    public void doTick(int gameTime) {
        this.sweep.advance();
        this.recentHits.removeIf(hit -> gameTime - hit.tick() >= HIT_FLASH_TICKS);

        List<EnemyMob> inRange = InRangeTargetQuery
                .visible(this.centerX, this.centerY, this.rangeReal())
                .matching(this.context.enemies());

        for (EnemyMob enemy : inRange) {
            double bearing = TurretAim.angleTo(this.centerX, this.centerY, enemy.getX(), enemy.getY());
            if (this.sweep.sweptThisTick(bearing)) {
                this.dealDamage(enemy, Damage.physical(this.damageCurrent()));
                this.piercingTone(enemy);
                this.recentHits.add(new SonarHit((float) enemy.getX(), (float) enemy.getY(), gameTime));
            }
        }
    }

    /** Adds magic damage in proportion to how much of a physical hit the target shrugs off. */
    private void piercingTone(EnemyMob enemy) {
        if (!this.upgrades().owns(PIERCING_TONE.id()) || enemy.isDead()) {
            return;
        }
        float bonus = Math.min(PIERCING_TONE_MAX_BONUS, enemy.reductionAgainst(DamageType.PHYSICAL));
        if (bonus > 0f) {
            this.dealDamage(enemy, Damage.magic(Math.round(this.damageCurrent() * bonus)));
        }
    }

    /** The beam's heading between two ticks; the turret head uses it too. */
    public double sweepRadiansAt(double interpolationAlpha) {
        return this.sweep.radiansAt(interpolationAlpha);
    }

    /** Hits still worth drawing, oldest first. */
    public List<SonarHit> getRecentHits() {
        return Collections.unmodifiableList(this.recentHits);
    }

    /** From {@code 1} the tick a hit landed down to {@code 0}. */
    public float hitFade(SonarHit hit, int gameTime) {
        int age = gameTime - hit.tick();
        return Math.max(0f, 1f - (float) age / HIT_FLASH_TICKS);
    }

    @Override
    protected Optional<TowerStatLine> cadence() {
        return Optional.of(TowerStatLine.fixed(TowerStat.ROTATION, SECONDS_PER_REVOLUTION));
    }

    @Override
    protected List<BehaviourLine> behaviours() {
        return List.of(new BehaviourLine(BehaviourMarker.TARGETING, "Hits", "all it sweeps"));
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitSonarTower(this);
    }

    public void doCleanup() {
        super.doCleanup();
        this.context.waves().removeListener(this);
    }

    /** Drops the previous wave's hit markers. */
    @Override
    public void waveStarted() {
        this.recentHits.clear();
    }

    /** Where and when the beam caught an enemy. */
    public record SonarHit(float x, float y, int tick) {
    }
}
