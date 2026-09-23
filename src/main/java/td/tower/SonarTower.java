package td.tower;

import td.damage.Damage;
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

/**
 * "Sonar tower" - a sonar scan. A beam sweeps the full circle counterclockwise at a
 * constant rate, and every visible enemy in range is hit the moment the beam passes its
 * bearing. There is no cooldown and no fire rate: an enemy standing still is hit once per
 * revolution, and how fast the tower shoots is entirely a question of how fast it turns.
 * <p>
 * Targeting is by absolute bearing, not by position in the wave's enemy array, so the order
 * enemies are hit in follows where they actually are on the board. Two enemies at the same
 * bearing are both hit on the same tick - the beam is a ray, not a single target.
 * <p>
 * Because the beam jumps a fifth of a radian per tick, hits are decided against the whole
 * arc swept since the previous tick (see {@link SonarSweep#sweptThisTick}), never against
 * the beam's instantaneous angle - otherwise an enemy that the beam went past between two
 * ticks would never be shot at all.
 */
public final class SonarTower extends AbstractTower implements WaveStartListener {

    public static final int PRICE = 20;
    public static final int DAMAGE = 1600;
    public static final float RANGE = 5.2f;
    /**
     * Seconds per full revolution of the scan - this tower's headline stat, in place of a fire rate.
     */
    public static final float SECONDS_PER_REVOLUTION = 2f;

    /**
     * How long a hit stays drawn, so a sweep leaves a brief trail of what it just caught.
     */
    private static final int HIT_FLASH_TICKS = 8;

    private static final UpgradeNode BASE_RANGE = StandardBaseSlot.rangeNode(12);
    private static final UpgradeNode AWAKEN = StandardBaseSlot.awakenNode(20);

    /**
     * More damage per hit.
     */
    private static final UpgradeNode TWIN_ARRAY_1 = UpgradeNode.of("sonar.head.twin_array.1", UpgradeSlot.HEAD,
            "Twin Array", 35)
            .withBuff(TowerBuff.damage(0.25f))
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD))
            .withGate(new DamageDealtCondition(10000));
    /**
     * More damage still, plus some crit chance.
     */
    private static final UpgradeNode TWIN_ARRAY_2 = UpgradeNode.of("sonar.head.twin_array.2", UpgradeSlot.HEAD,
            "Twin Array II", 53)
            .withBuff(TowerBuff.damage(0.25f).withCritChance(0.1f))
            .withRequires(UpgradeCondition.owns(TWIN_ARRAY_1.id()))
            .withGate(new DamageDealtCondition(20000));
    /**
     * A second turret facing the opposite direction, once the second-turret render/aim
     * primitive exists - see TODO.md.
     */
    private static final UpgradeNode TWIN_ARRAY_3 = UpgradeNode.of("sonar.head.twin_array.3", UpgradeSlot.HEAD,
            "Twin Array III", 70)
            .withRequires(UpgradeCondition.owns(TWIN_ARRAY_2.id()))
            .withGate(new KillCountCondition(25))
            .withExtraEffect("a second turret, facing the opposite direction");
    /**
     * More crit chance.
     */
    private static final UpgradeNode LONG_REACH_1 = UpgradeNode.of("sonar.head.long_reach.1", UpgradeSlot.HEAD,
            "Long Reach", 30)
            .withBuff(TowerBuff.critChance(0.15f))
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD))
            .withGate(new KillCountCondition(10));
    /**
     * Damage scales up to +100% at max range, once the distance-scaling-damage primitive
     * exists - see TODO.md.
     */
    private static final UpgradeNode LONG_REACH_2 = UpgradeNode.of("sonar.head.long_reach.2", UpgradeSlot.HEAD,
            "Long Reach II", 45)
            .withRequires(UpgradeCondition.owns(LONG_REACH_1.id()))
            .withGate(new DamageDealtCondition(20000))
            .withExtraEffect("damage scales up to +100% at max range");
    /**
     * Each revolution briefly reveals invisible enemies to every tower, once the
     * reveal-to-every-tower primitive exists - see TODO.md.
     */
    private static final UpgradeNode WIDE_BAND = UpgradeNode.of("sonar.special.wide_band", UpgradeSlot.SPECIAL,
            "Wide Band", 40)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new ClusterCondition(2))
            .withExtraEffect("each revolution briefly reveals invisible enemies to every tower");
    /**
     * A beam hit marks its target for a guaranteed crit on the next hit, once the
     * guaranteed-crit primitive exists - see TODO.md.
     */
    private static final UpgradeNode MARK_ON_SWEEP = UpgradeNode.of("sonar.special.mark_on_sweep", UpgradeSlot.SPECIAL,
            "Mark on Sweep", 40)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new KillCountCondition(15))
            .withExtraEffect("a beam hit marks its target; the next hit on it is a guaranteed crit");
    /**
     * Bonus magic damage against physically armored/shielded enemies, once the
     * resistance-aware-damage-scaling primitive exists - see TODO.md.
     */
    private static final UpgradeNode PIERCING_TONE = UpgradeNode.of("sonar.special.piercing_tone", UpgradeSlot.SPECIAL,
            "Piercing Tone", 40)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new DamageDealtCondition(20000))
            .withExtraEffect("bonus magic damage against physically armored/shielded enemies");

    private static final UpgradeTree TREE = UpgradeTree.of(BASE_RANGE, AWAKEN, TWIN_ARRAY_1, TWIN_ARRAY_2,
            TWIN_ARRAY_3, LONG_REACH_1, LONG_REACH_2, WIDE_BAND, MARK_ON_SWEEP, PIERCING_TONE);

    private final List<SonarHit> recentHits = new ArrayList<>();
    private volatile SonarSweep sweep = SonarSweep.perRevolution(SECONDS_PER_REVOLUTION, TICKS_PER_SECOND);

    public SonarTower(GameWorld context, int x, int y) {
        // No cooldown: this tower's cadence is its sweep rate, not a reload - see rateLine.
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
                .ofType(this.centerX, this.centerY, this.rangeReal(), EnemyMob.Type.NORMAL)
                .matching(this.context.enemies());

        for (EnemyMob enemy : inRange) {
            double bearing = TurretAim.angleTo(this.centerX, this.centerY, enemy.getX(), enemy.getY());
            if (this.sweep.sweptThisTick(bearing)) {
                this.dealDamage(enemy, Damage.physical(this.damageCurrent()));
                this.recentHits.add(new SonarHit((float) enemy.getX(), (float) enemy.getY(), gameTime));
            }
        }
    }

    /**
     * The beam's heading for a render landing between two ticks; the turret head reads the same value.
     */
    public double sweepRadiansAt(double interpolationAlpha) {
        return this.sweep.radiansAt(interpolationAlpha);
    }

    /**
     * Hits still worth drawing, oldest first.
     */
    public List<SonarHit> getRecentHits() {
        return Collections.unmodifiableList(this.recentHits);
    }

    /**
     * How bright a hit should still be drawn, {@code 1} the tick it landed down to {@code 0}.
     */
    public float hitFade(SonarHit hit, int gameTime) {
        int age = gameTime - hit.tick();
        return Math.max(0f, 1f - (float) age / HIT_FLASH_TICKS);
    }

    @Override
    protected String rateLine(int coolDown) {
        return "Rotation: " + SECONDS_PER_REVOLUTION + "s/turn\n";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitSonarTower(this);
    }

    public String getInfoString() {
        return "Sonar tower\n\n" +
                super.getInfoString() +
                "Sweeps a beam around itself, hitting everything it passes over";
    }

    public String getStatusString() {
        return "Sonar tower\n\n" +
                super.getStatusString() +
                "Sweeps a beam around itself, hitting everything it passes over";
    }

    public void doCleanup() {
        super.doCleanup();
        this.context.waves().removeListener(this);
    }

    /**
     * Drops the trail of hit markers left over from the previous wave, which would otherwise
     * be drawn for a moment against enemies that no longer exist.
     */
    @Override
    public void waveStarted() {
        this.recentHits.clear();
    }

    /**
     * Where the beam caught an enemy, and when - frozen at the hit position, not tracked afterwards.
     */
    public record SonarHit(float x, float y, int tick) {
    }
}
