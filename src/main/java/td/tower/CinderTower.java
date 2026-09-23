package td.tower;

import td.damage.Damage;
import td.effect.Effect;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.InWedgeTargetQuery;
import td.tower.targeting.NearestSelector;
import td.tower.upgrade.DamageDealtCondition;
import td.tower.upgrade.UpgradeCondition;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * "Cinder tower" - a cooldown-gated flame cone, slowly reorienting toward the nearest enemy in
 * range. Each shot is a wave that travels outward from the tower over several ticks and fades as
 * it goes, burning an enemy only once the wave's own expanding front actually reaches it - the
 * same "hits everyone in the shape" spirit as {@link PulseTower}, just confined to a cone
 * instead of the whole range circle, and spread out over time instead of instantaneous. Like
 * every other non-{@link PulseTower} tower, it cannot target or hit an invisible enemy.
 * <p>
 * A wave's heading and half-width are captured once, at the moment it fires - it does not
 * retroactively change shape as the turret keeps tracking afterward, the same reasoning
 * {@code InWedgeTargetQuery} already applies to a static wedge's <em>current</em> heading, just
 * captured per-wave instead of read live.
 * <p>
 * Cinder never calls {@code dealDamage} directly - its entire attack is applying a burn once a
 * wave's front reaches an enemy. Damage still flows through this tower's own {@code dealDamage}
 * once the burn ticks (see {@code Effect}'s sink), so its damage/kill accounting stays accurate
 * without a second, parallel damage path.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)  // cooldown, advanced by doTick
public final class CinderTower extends AbstractTower {

    public static final int PRICE = 28;
    public static final int DAMAGE = 150;
    public static final float RANGE = 2.2f;
    /**
     * Ticks between shots before any fire-rate buff - a placeholder for the balance pass
     * {@code TODO.md} already tracks for Cinder's other numbers, not a tuned final value.
     */
    public static final int COOLDOWN_MAX = 20;
    /**
     * How many ticks a wave takes to travel from the tower out to its full range - configurable
     * here rather than inline, per this feature's own ask. A placeholder alongside
     * {@link #COOLDOWN_MAX}, not a tuned final value.
     */
    public static final int WAVE_TRAVEL_TICKS = 10;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.15;
    private static final double HALF_WIDTH_RADIANS_BASE = 0.35;
    private static final int BURN_DURATION_TICKS = 60;
    private static final double WIDE_NOZZLE_HALF_WIDTH_MULTIPLIER = 1.4;

    /**
     * More damage (and so more burn per tick, since burn's magnitude is this tower's own damageCurrent) - earned by proven output.
     */
    private static final UpgradeNode WHITE_FLAME = UpgradeNode.of("cinder.head.white_flame", UpgradeSlot.HEAD,
            "White Flame", 30)
            .withBuff(TowerBuff.damage(0.4f))
            .withRequires(UpgradeCondition.slotEmpty(UpgradeSlot.HEAD))
            .withGate(new DamageDealtCondition(15000));
    /**
     * A wider cone and more range - a straightforward money-gated specialization needing no track record.
     */
    private static final UpgradeNode WIDE_NOZZLE = UpgradeNode.of("cinder.head.wide_nozzle", UpgradeSlot.HEAD,
            "Wide Nozzle", 25)
            .withBuff(TowerBuff.range(0.3f))
            .withRequires(UpgradeCondition.slotEmpty(UpgradeSlot.HEAD))
            .withExtraEffect("+40% cone width");
    private static final UpgradeTree TREE = UpgradeTree.of(WHITE_FLAME, WIDE_NOZZLE);
    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private final List<FlameWave> inFlightWaves = new ArrayList<>();
    private volatile double halfWidthRadians = HALF_WIDTH_RADIANS_BASE;
    private int coolDown = 0;

    public CinderTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.CINDER, PRICE, new TowerBaseStats(DAMAGE, RANGE, COOLDOWN_MAX), context, x, y);
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    /**
     * Wide Nozzle's wider cone isn't a {@link TowerBuff} axis, so it's applied here instead.
     */
    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        if (node.equals(WIDE_NOZZLE)) {
            this.halfWidthRadians *= WIDE_NOZZLE_HALF_WIDTH_MULTIPLIER;
        }
    }

    public void doTick(int gameTime) {
        List<EnemyMob> inRange = InRangeTargetQuery.ofType(this.centerX, this.centerY, this.rangeReal(), EnemyMob.Type.NORMAL)
                .matching(this.context.enemies());
        // No target: hold the last heading rather than snapping back to a neutral angle - see
        // TurretAim's class doc on skipping tick() while idle.
        new NearestSelector(this.centerX, this.centerY).selectFrom(inRange)
                .ifPresent(nearest -> this.turretAim.tick(TurretAim.angleTo(this.centerX, this.centerY, nearest.getX(), nearest.getY())));

        if (this.coolDown > 0) {
            this.coolDown--;
        } else if (!inRange.isEmpty()) {
            this.inFlightWaves.add(new FlameWave(this.turretAim.currentRadians(), this.halfWidthRadians, gameTime));
            this.coolDown = this.coolDownCurrent();
        }

        this.advanceWaves(gameTime);
    }

    /**
     * Advances every in-flight wave by one tick, burning whichever enemies its expanding front
     * has just reached for the first time - each wave hits a given enemy at most once, no matter
     * how long that enemy lingers inside the band, tracked by the wave's own hit set. A wave that
     * has fully travelled its range is dropped.
     */
    private void advanceWaves(int gameTime) {
        Iterator<FlameWave> waves = this.inFlightWaves.iterator();
        while (waves.hasNext()) {
            FlameWave wave = waves.next();
            float travelled = Math.min(1f, (float) (gameTime - wave.firedAtTick) / WAVE_TRAVEL_TICKS);
            float currentRadius = travelled * this.rangeReal();
            List<EnemyMob> caught = new InWedgeTargetQuery(this.centerX, this.centerY, wave.headingRadians, wave.halfWidthRadians)
                    .and(InRangeTargetQuery.ofType(this.centerX, this.centerY, currentRadius, EnemyMob.Type.NORMAL))
                    .matching(this.context.enemies());
            for (EnemyMob enemy : caught) {
                if (wave.alreadyHit.add(enemy)) {
                    enemy.applyEffect(Effect.burn(Damage.magic(this.damageCurrent()), BURN_DURATION_TICKS, d -> this.dealDamage(enemy, d)));
                }
            }
            if (travelled >= 1f) {
                waves.remove();
            }
        }
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    public double getHalfWidthRadians() {
        return this.halfWidthRadians;
    }

    /**
     * Waves still travelling outward, oldest first - what {@code td.ui.TowerEffectFrameBuilder}
     * draws one {@code ConeDraw} per, each at its own progress.
     */
    public List<FlameWave> getInFlightWaves() {
        return Collections.unmodifiableList(this.inFlightWaves);
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitCinderTower(this);
    }

    public String getInfoString() {
        return "Cinder tower\n\n" +
                super.getInfoString() +
                "Burns everything in a cone";
    }

    public String getStatusString() {
        return "Cinder tower\n\n" +
                super.getStatusString() +
                "Burns everything in a cone";
    }

    /**
     * One shot in flight: the heading and half-width it was fired at (captured once, so a wave
     * doesn't retroactively change shape as the turret keeps tracking), when it fired, and which
     * enemies its advancing front has already burned.
     */
    public static final class FlameWave {
        private final double headingRadians;
        private final double halfWidthRadians;
        private final int firedAtTick;
        private final Set<EnemyMob> alreadyHit = new HashSet<>();

        private FlameWave(double headingRadians, double halfWidthRadians, int firedAtTick) {
            this.headingRadians = headingRadians;
            this.halfWidthRadians = halfWidthRadians;
            this.firedAtTick = firedAtTick;
        }

        public double headingRadians() {
            return this.headingRadians;
        }

        public double halfWidthRadians() {
            return this.halfWidthRadians;
        }

        public int firedAtTick() {
            return this.firedAtTick;
        }
    }
}
