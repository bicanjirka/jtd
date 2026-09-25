package td.tower;

import td.damage.Damage;
import td.damage.DamageType;
import td.effect.Effect;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.InWedgeTargetQuery;
import td.tower.targeting.NearestSelector;
import td.tower.upgrade.DamageDealtCondition;
import td.tower.upgrade.KillCountCondition;
import td.tower.upgrade.StandardBaseSlot;
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
 * A cooldown-gated flame cone that turns slowly toward the nearest enemy. Each shot is a wave
 * travelling outward over several ticks, burning an enemy once when its front reaches it. Cannot
 * hit invisible enemies.
 * <p>
 * A wave's heading and width are fixed when it fires. The tower never deals direct damage: its
 * burns credit it through {@code dealDamage}.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class CinderTower extends AbstractTower {

    public static final int PRICE = 28;
    public static final int DAMAGE = 150;
    public static final float RANGE = 2.2f;
    /** Ticks between shots before buffs. */
    public static final int COOLDOWN_MAX = 20;
    /** Ticks a wave takes to reach full range. */
    public static final int WAVE_TRAVEL_TICKS = 10;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.15;
    private static final double HALF_WIDTH_RADIANS_BASE = 0.35;
    private static final int BURN_DURATION_TICKS_BASE = 60;
    private static final float WHITE_FLAME_BURN_DURATION_MULTIPLIER = 1.5f;
    private static final double WIDE_NOZZLE_HALF_WIDTH_MULTIPLIER_1 = 1.3;
    private static final double WIDE_NOZZLE_HALF_WIDTH_MULTIPLIER_2 = 1.2;

    private static final UpgradeNode BASE_RANGE = StandardBaseSlot.rangeNode(17);
    private static final UpgradeNode AWAKEN = StandardBaseSlot.awakenNode(28);

    private static final UpgradeNode WHITE_FLAME_1 = UpgradeNode.of("cinder.head.white_flame.1", UpgradeSlot.HEAD,
            "White Flame", 30)
            .withBuff(TowerBuff.damage(0.3f))
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD))
            .withGate(new DamageDealtCondition(15000));
    private static final UpgradeNode WHITE_FLAME_2 = UpgradeNode.of("cinder.head.white_flame.2", UpgradeSlot.HEAD,
            "White Flame II", 45)
            .withBuff(TowerBuff.damage(0.25f))
            .withRequires(UpgradeCondition.owns(WHITE_FLAME_1.id()))
            .withGate(new DamageDealtCondition(30000))
            .withExtraEffect("+50% burn duration");
    private static final UpgradeNode WIDE_NOZZLE_1 = UpgradeNode.of("cinder.head.wide_nozzle.1", UpgradeSlot.HEAD,
            "Wide Nozzle", 25)
            .withBuff(TowerBuff.range(0.25f))
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD))
            .withGate(new KillCountCondition(10))
            .withExtraEffect("+30% cone width");
    private static final UpgradeNode WIDE_NOZZLE_2 = UpgradeNode.of("cinder.head.wide_nozzle.2", UpgradeSlot.HEAD,
            "Wide Nozzle II", 38)
            .withBuff(TowerBuff.range(0.2f).withFireRate(0.2f))
            .withRequires(UpgradeCondition.owns(WIDE_NOZZLE_1.id()))
            .withGate(new DamageDealtCondition(25000))
            .withExtraEffect("+20% cone width, -20% cooldown");
    /** Not implemented yet (TODO.md). */
    private static final UpgradeNode HEXFLAME = UpgradeNode.of("cinder.special.hexflame", UpgradeSlot.SPECIAL,
            "Hexflame", 56)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new KillCountCondition(20))
            .withExtraEffect("each wave that newly ignites an enemy also grants 1 Vulnerable stack (cap 3)");

    private static final UpgradeTree TREE = UpgradeTree.of(BASE_RANGE, AWAKEN, WHITE_FLAME_1, WHITE_FLAME_2,
            WIDE_NOZZLE_1, WIDE_NOZZLE_2, HEXFLAME);

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private final List<FlameWave> inFlightWaves = new ArrayList<>();
    private volatile double halfWidthRadians = HALF_WIDTH_RADIANS_BASE;
    private volatile int burnDurationTicks = BURN_DURATION_TICKS_BASE;
    private int coolDown = 0;

    public CinderTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.CINDER, PRICE, new TowerBaseStats(DAMAGE, RANGE, COOLDOWN_MAX), context, x, y);
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        if (node.equals(WHITE_FLAME_2)) {
            this.burnDurationTicks = Math.round(this.burnDurationTicks * WHITE_FLAME_BURN_DURATION_MULTIPLIER);
        } else if (node.equals(WIDE_NOZZLE_1)) {
            this.halfWidthRadians *= WIDE_NOZZLE_HALF_WIDTH_MULTIPLIER_1;
        } else if (node.equals(WIDE_NOZZLE_2)) {
            this.halfWidthRadians *= WIDE_NOZZLE_HALF_WIDTH_MULTIPLIER_2;
        }
    }

    public void doTick(int gameTime) {
        List<EnemyMob> inRange = InRangeTargetQuery.ofType(this.centerX, this.centerY, this.rangeReal(), EnemyMob.Type.NORMAL)
                .matching(this.context.enemies());
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
     * Advances every wave, burning enemies its front reaches for the first time; each wave hits an
     * enemy at most once. Drops waves that reached full range.
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
                    enemy.applyEffect(Effect.burn(Damage.magic(this.damageCurrent()), this.burnDurationTicks, d -> this.dealDamage(enemy, d)));
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

    /** Waves still travelling, oldest first. */
    public List<FlameWave> getInFlightWaves() {
        return Collections.unmodifiableList(this.inFlightWaves);
    }

    @Override
    protected DamageType damageType() {
        return DamageType.MAGIC;
    }

    @Override
    protected List<BehaviourLine> behaviours() {
        return List.of(new BehaviourLine(BehaviourMarker.BURN, "Burns", BehaviourLine.seconds(this.burnDurationTicks)),
                new BehaviourLine(BehaviourMarker.TARGETING, "Aims at", "nearest"));
    }

    @Override
    protected String description() {
        return "Fires a cone of flame that sets everything in it alight.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitCinderTower(this);
    }

    /**
     * One shot in flight: its fixed heading and width, fire time, and the enemies it has already
     * burned.
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
