package td.tower;

import td.damage.Damage;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.upgrade.ClusterCondition;
import td.tower.upgrade.KillCountCondition;
import td.tower.upgrade.UpgradePath;
import td.util.GameWorld;
import td.wave.WaveStartListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * "Sunshine tower" - a sonar scan. A beam sweeps the full circle counterclockwise at a
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
public final class TowerThree extends AbstractTower implements WaveStartListener {

    public static final int price = 20;
    public static final int damage = 1600;
    public static final float range = 5.2f;
    /** Seconds per full revolution of the scan - this tower's headline stat, in place of a fire rate. */
    public static final float secondsPerRevolution = 2f;

    /** How long a hit stays drawn, so a sweep leaves a brief trail of what it just caught. */
    private static final int HIT_FLASH_TICKS = 8;

    /** How much faster "Overcharged Array" makes the scan turn. */
    private static final float OVERCHARGED_SPEEDUP_FACTOR = 0.6f;
    /** Faster sweep and more range - a payoff for a deliberately grouped placement. */
    private static final UpgradePath OVERCHARGED_ARRAY = new UpgradePath(
            "Overcharged Array", 35, new TowerBuff(0f, 0.2f, 0f, 0f), new ClusterCondition(2));
    /** More damage per hit - earned by this tower's own proven kill record. */
    private static final UpgradePath MARKSMAN_BEAM = new UpgradePath(
            "Marksman Beam", 30, new TowerBuff(0.4f, 0f, 0f, 0f), new KillCountCondition(15));
    private static final List<UpgradePath> PATHS = List.of(OVERCHARGED_ARRAY, MARKSMAN_BEAM);

    private SonarSweep sweep = SonarSweep.perRevolution(secondsPerRevolution, TICKS_PER_SECOND);
    private float secondsPerRevolutionCurrent = secondsPerRevolution;
    private final List<SonarHit> recentHits = new ArrayList<>();

    public TowerThree(GameWorld context, int x, int y) {
        // No cooldown: this tower's cadence is its sweep rate, not a reload - see rateLine.
        super(TowerFactory.type.third, price, damage, range, 0, context, x, y);
        this.context.addWaveStartListener(this);
    }

    @Override
    public List<UpgradePath> availablePaths() {
        return PATHS;
    }

    /** Overcharged Array's turn-speed bump isn't a {@link TowerBuff} axis, so it's applied here instead. */
    @Override
    protected void onUpgradePathChosen(UpgradePath path) {
        if (path == OVERCHARGED_ARRAY) {
            this.secondsPerRevolutionCurrent = secondsPerRevolution * OVERCHARGED_SPEEDUP_FACTOR;
            this.sweep = SonarSweep.perRevolution(this.secondsPerRevolutionCurrent, TICKS_PER_SECOND);
        }
    }

    public void doTick(int gameTime) {
        this.sweep.advance();
        this.recentHits.removeIf(hit -> gameTime - hit.tick() >= HIT_FLASH_TICKS);

        List<EnemyMob> inRange = InRangeTargetQuery
                .ofType(this.centerX, this.centerY, this.rangeReal(), EnemyMob.type.Normal)
                .matching(this.context.getEnemyRegistry());

        for (EnemyMob enemy : inRange) {
            double bearing = TurretAim.angleTo(this.centerX, this.centerY, enemy.getX(), enemy.getY());
            if (this.sweep.sweptThisTick(bearing)) {
                this.dealDamage(enemy, Damage.physical(this.damageCurrent()));
                this.recentHits.add(new SonarHit((float) enemy.getX(), (float) enemy.getY(), gameTime));
            }
        }
    }

    /** The beam's heading for a render landing between two ticks; the turret head reads the same value. */
    public double sweepRadiansAt(double interpolationAlpha) {
        return this.sweep.radiansAt(interpolationAlpha);
    }

    /** Hits still worth drawing, oldest first. */
    public List<SonarHit> getRecentHits() {
        return Collections.unmodifiableList(this.recentHits);
    }

    /** How bright a hit should still be drawn, {@code 1} the tick it landed down to {@code 0}. */
    public float hitFade(SonarHit hit, int gameTime) {
        int age = gameTime - hit.tick();
        return Math.max(0f, 1f - (float) age / HIT_FLASH_TICKS);
    }

    /** Where the beam caught an enemy, and when - frozen at the hit position, not tracked afterwards. */
    public record SonarHit(float x, float y, int tick) {
    }

    @Override
    protected String rateLine(int coolDown) {
        return "Rotation: " + this.secondsPerRevolutionCurrent + "s/turn\n";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitTowerThree(this);
    }

    public String getInfoString() {
        return "Sunshine tower\n\n" +
                super.getInfoString() +
                "Sweeps a beam around itself, hitting everything it passes over";
    }

    public String getStatusString() {
        return "Sunshine tower\n\n" +
                super.getStatusString() +
                "Sweeps a beam around itself, hitting everything it passes over";
    }

    public void doCleanup() {
        super.doCleanup();
        this.context.removeWaveStartListener(this);
    }

    /**
     * Drops the trail of hit markers left over from the previous wave, which would otherwise
     * be drawn for a moment against enemies that no longer exist.
     */
    @Override
    public void waveStarted() {
        this.recentHits.clear();
    }
}
