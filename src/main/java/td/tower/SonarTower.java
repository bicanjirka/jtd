package td.tower;

import td.damage.AttackProfile;
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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private static final UpgradeNode LONG_REACH_2 = UpgradeNode.of("sonar.head.long_reach.2", UpgradeSlot.HEAD,
            "Long Reach II", 45)
            .withRequires(UpgradeCondition.owns(LONG_REACH_1.id()))
            .withGate(new DamageDealtCondition(20000))
            .withExtraEffect("damage scales up to +100% at max range");
    private static final UpgradeNode WIDE_BAND = UpgradeNode.of("sonar.special.wide_band", UpgradeSlot.SPECIAL,
            "Wide Band", 40)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new ClusterCondition(2))
            .withExtraEffect("each revolution briefly reveals invisible enemies to every tower");
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
    /** Wide Band's reveal lasts this long from the beam's pass. */
    private static final float WIDE_BAND_REVEAL_SECONDS = 3f;
    /** Long Reach II adds up to this much damage at the edge of the range. */
    private static final float LONG_REACH_MAX_BONUS = 1f;

    private static final UpgradeTree TREE = UpgradeTree.of(BASE_RANGE, AWAKEN, TWIN_ARRAY_1, TWIN_ARRAY_2,
            TWIN_ARRAY_3, LONG_REACH_1, LONG_REACH_2, WIDE_BAND, MARK_ON_SWEEP, PIERCING_TONE);

    private final List<SonarHit> recentHits = new ArrayList<>();
    private final Set<EnemyMob> marked = Collections.newSetFromMap(new IdentityHashMap<>());
    private volatile boolean twinBeam = false;
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

    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        if (node.equals(TWIN_ARRAY_3)) {
            this.twinBeam = true;
        }
    }

    public void doTick(int gameTime) {
        this.sweep.advance();
        this.recentHits.removeIf(hit -> gameTime - hit.tick() >= HIT_FLASH_TICKS);
        this.marked.removeIf(EnemyMob::isDead);

        // Wide Band lets the beam reach hidden enemies, and reveals them as it does.
        boolean wideBand = this.upgrades().owns(WIDE_BAND.id());
        List<EnemyMob> inRange = (wideBand
                ? InRangeTargetQuery.everyone(this.centerX, this.centerY, this.rangeReal())
                : InRangeTargetQuery.visible(this.centerX, this.centerY, this.rangeReal()))
                .matching(this.context.enemies());

        for (EnemyMob enemy : inRange) {
            double bearing = TurretAim.angleTo(this.centerX, this.centerY, enemy.getX(), enemy.getY());
            if (this.sweptByABeam(bearing)) {
                this.strike(enemy, gameTime);
            }
        }
    }

    /** Twin Array III adds a second beam half a turn behind the first. */
    private boolean sweptByABeam(double bearing) {
        return this.sweep.sweptThisTick(bearing) || (this.twinBeam && this.sweep.sweptThisTick(bearing + Math.PI));
    }

    /**
     * One beam hit: reveals a hidden enemy (Wide Band), sends a marked one's hit as a guaranteed
     * crit that spends the mark, and marks an unmarked one (Mark on Sweep).
     */
    private void strike(EnemyMob enemy, int gameTime) {
        if (enemy.isHidden()) {
            this.reveal(enemy, Math.round(WIDE_BAND_REVEAL_SECONDS * TICKS_PER_SECOND));
        }
        boolean wasMarked = this.marked.remove(enemy);
        AttackProfile attack = wasMarked ? this.stats().attack().withCritChance(1f) : this.stats().attack();
        this.dealDamage(enemy, Damage.physical(this.distanceScaled(enemy)), attack);
        this.piercingTone(enemy);
        if (!wasMarked && !enemy.isDead() && this.upgrades().owns(MARK_ON_SWEEP.id())) {
            this.marked.add(enemy);
        }
        this.recentHits.add(new SonarHit((float) enemy.getX(), (float) enemy.getY(), gameTime));
    }

    /** Long Reach II: the hit grows linearly with the enemy's distance, up to double at the edge. */
    private int distanceScaled(EnemyMob enemy) {
        if (!this.upgrades().owns(LONG_REACH_2.id())) {
            return this.damageCurrent();
        }
        double distance = Math.hypot(enemy.getX() - this.centerX, enemy.getY() - this.centerY);
        float share = (float) Math.min(1.0, distance / this.rangeReal());
        return Math.round(this.damageCurrent() * (1f + LONG_REACH_MAX_BONUS * share));
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

    /** Whether a second beam sweeps half a turn opposite the first. */
    public boolean hasTwinBeam() {
        return this.twinBeam;
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
        List<BehaviourLine> lines = new ArrayList<>();
        lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Hits", "all it sweeps"));
        if (this.twinBeam) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Beams", "2"));
        }
        if (this.upgrades().owns(WIDE_BAND.id())) {
            lines.add(new BehaviourLine(BehaviourMarker.REVEAL, "Reveals hidden", BehaviourLine.seconds(Math.round(WIDE_BAND_REVEAL_SECONDS * TICKS_PER_SECOND))));
        }
        if (this.upgrades().owns(MARK_ON_SWEEP.id())) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Marks", "next hit crits"));
        }
        if (this.upgrades().owns(LONG_REACH_2.id())) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Far hits", "up to +100%"));
        }
        return lines;
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitSonarTower(this);
    }

    public void doCleanup() {
        super.doCleanup();
        this.context.waves().removeListener(this);
    }

    /** Drops the previous wave's hit markers and marks. */
    @Override
    public void waveStarted() {
        this.recentHits.clear();
        this.marked.clear();
    }

    /** Where and when the beam caught an enemy. */
    public record SonarHit(float x, float y, int tick) {
    }
}
