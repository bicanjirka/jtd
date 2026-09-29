package td.tower;

import td.damage.Damage;
import td.effect.Effect;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.RandomSelector;
import td.tower.targeting.TargetSelector;
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
import java.util.List;
import java.util.Optional;

/**
 * Picks a random visible enemy in range and damages everything within {@code spreadRadius} of it,
 * falling off with the square of the distance. The splash hits invisible enemies too.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class SplashTower extends AbstractTower {

    public static final int PRICE = 15;
    public static final int DAMAGE = 1600;
    public static final float RANGE = 3.2f;
    public static final float SPREAD_RADIUS_BASE = 1.75f;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.25;
    private static final float CHILL_AMOUNT = 0.5f;
    private static final int CHILL_DURATION_TICKS = 40;

    /** Blast radius multiplier from the first blast upgrade. */
    private static final float BLAST_ENGINEERING_SPREAD_MULTIPLIER = 1.3f;
    /** Scales the falloff term down, flattening damage across the blast. */
    private static final float FALLOFF_SOFTENING_FACTOR = 0.5f;
    /** Blasts per shot once Blast Engineering III is owned. */
    private static final int BLAST_ENGINEERING_BLAST_COUNT = 3;
    /** Rapid Battery III: a critical shot's blast is this much wider. */
    private static final float RAPID_BATTERY_CRIT_RADIUS_MULTIPLIER = 1.5f;
    /** Toxic Bloom's damage per tick, as a share of weapon damage, and how long it lasts. */
    private static final float POISON_DAMAGE_SHARE = 0.1f;
    private static final float POISON_SECONDS = 4f;
    /** Concussive Blast: a kill explodes for this share of weapon damage. */
    private static final float EXPLOSION_DAMAGE_SHARE = 0.5f;

    private static final UpgradeNode BASE_RANGE = StandardBaseSlot.rangeNode(9);
    private static final UpgradeNode AWAKEN = StandardBaseSlot.awakenNode(15);

    private static final UpgradeNode BLAST_ENGINEERING_1 = UpgradeNode.of("splash.head.blast_engineering.1",
            UpgradeSlot.HEAD, "Blast Engineering", 35)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD))
            .withGate(new DamageDealtCondition(10000))
            .withExtraEffect("+30% splash radius");
    private static final UpgradeNode BLAST_ENGINEERING_2 = UpgradeNode.of("splash.head.blast_engineering.2",
            UpgradeSlot.HEAD, "Blast Engineering II", 53)
            .withBuff(TowerBuff.damage(0.25f))
            .withRequires(UpgradeCondition.owns(BLAST_ENGINEERING_1.id()))
            .withGate(new DamageDealtCondition(20000))
            .withExtraEffect("flattens the falloff curve");
    private static final UpgradeNode BLAST_ENGINEERING_3 = UpgradeNode.of("splash.head.blast_engineering.3",
            UpgradeSlot.HEAD, "Blast Engineering III", 70)
            .withRequires(UpgradeCondition.owns(BLAST_ENGINEERING_2.id()))
            .withGate(new KillCountCondition(20))
            .withExtraEffect("fires 3 projectiles instead of 1");
    private static final UpgradeNode RAPID_BATTERY_1 = UpgradeNode.of("splash.head.rapid_battery.1", UpgradeSlot.HEAD,
            "Rapid Battery", 30)
            .withBuff(TowerBuff.fireRate(0.25f))
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD))
            .withGate(new KillCountCondition(8));
    private static final UpgradeNode RAPID_BATTERY_2 = UpgradeNode.of("splash.head.rapid_battery.2", UpgradeSlot.HEAD,
            "Rapid Battery II", 45)
            .withBuff(TowerBuff.damage(0.25f).withCritChance(0.15f))
            .withRequires(UpgradeCondition.owns(RAPID_BATTERY_1.id()))
            .withGate(new KillCountCondition(18));
    private static final UpgradeNode RAPID_BATTERY_3 = UpgradeNode.of("splash.head.rapid_battery.3", UpgradeSlot.HEAD,
            "Rapid Battery III", 60)
            .withRequires(UpgradeCondition.owns(RAPID_BATTERY_2.id()))
            .withGate(new DamageDealtCondition(25000))
            .withExtraEffect("crits splash 50% bigger");
    private static final UpgradeNode TOXIC_BLOOM = UpgradeNode.of("splash.special.toxic_bloom", UpgradeSlot.SPECIAL,
            "Toxic Bloom", 30)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new DamageDealtCondition(15000))
            .withExtraEffect("splash applies poison");
    private static final UpgradeNode CONCUSSIVE_BLAST = UpgradeNode.of("splash.special.concussive_blast",
            UpgradeSlot.SPECIAL, "Concussive Blast", 30)
            .withBuff(TowerBuff.fireRate(-0.5f))
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new KillCountCondition(15))
            .withExtraEffect("-50% fire rate, blast applies chill, killed enemies explode");
    private static final UpgradeNode OVERPRESSURE = UpgradeNode.of("splash.special.overpressure", UpgradeSlot.SPECIAL,
            "Overpressure", 30)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new KillCountCondition(25))
            .withExtraEffect("on crit, the next shot fires at every enemy in range");

    private static final UpgradeTree TREE = UpgradeTree.of(BASE_RANGE, AWAKEN, BLAST_ENGINEERING_1,
            BLAST_ENGINEERING_2, BLAST_ENGINEERING_3, RAPID_BATTERY_1, RAPID_BATTERY_2, RAPID_BATTERY_3, TOXIC_BLOOM,
            CONCUSSIVE_BLAST, OVERPRESSURE);

    private static final int COOLDOWN_MAX = 19;
    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private final TargetSelector targetSelector;
    private volatile float spreadRadius;
    private volatile float falloffSoftening = 1f;
    private volatile boolean concussiveBlast = false;
    private int coolDown = 0;
    private boolean overpressureArmed = false;
    private List<Blast> blasts = List.of();

    public SplashTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SPLASH, PRICE, new TowerBaseStats(DAMAGE, RANGE, COOLDOWN_MAX), context, x, y);
        this.spreadRadius = SPREAD_RADIUS_BASE * context.getBoard().scale();
        this.targetSelector = new RandomSelector(context.random());
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        if (node.equals(BLAST_ENGINEERING_1)) {
            this.spreadRadius *= BLAST_ENGINEERING_SPREAD_MULTIPLIER;
        } else if (node.equals(BLAST_ENGINEERING_2)) {
            this.falloffSoftening = FALLOFF_SOFTENING_FACTOR;
        } else if (node.equals(CONCUSSIVE_BLAST)) {
            this.concussiveBlast = true;
        }
    }

    private List<EnemyMob> findEnemiesInRangeVisible(int x, int y, float r) {
        return InRangeTargetQuery.visible(x, y, r).matching(this.context.enemies());
    }

    private List<EnemyMob> findEnemiesInRange(int x, int y, float r) {
        return InRangeTargetQuery.everyone(x, y, r).matching(this.context.enemies());
    }

    public void doTick(int gameTime) {
        if (this.coolDown > 0) {
            this.coolDown--;
        } else {
            List<EnemyMob> targets = this.pickTargets(this.findEnemiesInRangeVisible(this.centerX, this.centerY, this.rangeReal()));
            if (targets.isEmpty()) {
                this.blasts = List.of();
            } else {
                this.blasts = this.fire(targets);
                this.coolDown = this.coolDownCurrent();
            }
        }
        if (!this.blasts.isEmpty()) {
            EnemyMob aimed = this.blasts.getFirst().primary();
            this.turretAim.tick(TurretAim.angleTo(this.centerX, this.centerY, aimed.getX(), aimed.getY()));
        }
    }

    /**
     * Who this shot blasts: everyone visible in range after an armed Overpressure, else one random
     * enemy, or three distinct ones with Blast Engineering III.
     */
    private List<EnemyMob> pickTargets(List<EnemyMob> visible) {
        if (this.overpressureArmed && !visible.isEmpty()) {
            this.overpressureArmed = false;
            return visible;
        }
        int count = this.upgrades().owns(BLAST_ENGINEERING_3.id()) ? BLAST_ENGINEERING_BLAST_COUNT : 1;
        List<EnemyMob> remaining = new ArrayList<>(visible);
        List<EnemyMob> picked = new ArrayList<>();
        while (picked.size() < count) {
            Optional<EnemyMob> next = this.targetSelector.selectFrom(remaining);
            if (next.isEmpty()) {
                break;
            }
            picked.add(next.get());
            remaining.remove(next.get());
        }
        return picked;
    }

    /** One blast per target. A shot is critical if any blast's primary hit was; that arms Overpressure. */
    private List<Blast> fire(List<EnemyMob> targets) {
        List<Blast> fired = new ArrayList<>();
        boolean critical = false;
        for (EnemyMob primary : targets) {
            Blast blast = this.blast(primary);
            critical |= blast.critical();
            fired.add(blast);
        }
        if (critical && this.upgrades().owns(OVERPRESSURE.id())) {
            this.overpressureArmed = true;
        }
        return List.copyOf(fired);
    }

    /**
     * Hits the primary first, at full damage, then everything else within the blast. A critical
     * primary hit widens the blast (Rapid Battery III).
     */
    private Blast blast(EnemyMob primary) {
        int centerX = (int) primary.getX();
        int centerY = (int) primary.getY();
        boolean critical = this.hit(primary, centerX, centerY, this.spreadRadius);
        float radius = critical && this.upgrades().owns(RAPID_BATTERY_3.id())
                ? this.spreadRadius * RAPID_BATTERY_CRIT_RADIUS_MULTIPLIER
                : this.spreadRadius;
        List<EnemyMob> caught = this.findEnemiesInRange(centerX, centerY, radius);
        for (EnemyMob enemy : caught) {
            if (enemy != primary) {
                this.hit(enemy, centerX, centerY, radius);
            }
        }
        return new Blast(primary, caught, new Blast.Area(centerX, centerY, radius), critical);
    }

    /** One enemy's share of a blast: falloff damage, then the effects the upgrades add. */
    private boolean hit(EnemyMob enemy, int centerX, int centerY, float radius) {
        int dx = centerX - (int) enemy.getX();
        int dy = centerY - (int) enemy.getY();
        int r2 = dx * dx + dy * dy;
        int damage = Math.round(this.damageCurrent() * (1 - (r2 * this.falloffSoftening) / (radius * radius)));
        boolean critical = this.dealDamage(enemy, Damage.physical(damage));
        if (this.concussiveBlast) {
            enemy.applyEffect(Effect.chill(CHILL_AMOUNT, CHILL_DURATION_TICKS, d -> this.dealDamage(enemy, d)));
        }
        if (this.upgrades().owns(TOXIC_BLOOM.id())) {
            Damage perTick = Damage.magic(Math.round(this.damageCurrent() * POISON_DAMAGE_SHARE));
            enemy.applyEffect(Effect.poison(perTick, Math.round(POISON_SECONDS * TICKS_PER_SECOND), d -> this.dealDamage(enemy, d)));
        }
        return critical;
    }

    /** Concussive Blast: an enemy this tower kills explodes onto whatever stands within one blast radius. */
    @Override
    protected void onKill(EnemyMob killed) {
        if (!this.concussiveBlast) {
            return;
        }
        Damage explosion = Damage.physical(Math.round(this.damageCurrent() * EXPLOSION_DAMAGE_SHARE));
        for (EnemyMob nearby : this.findEnemiesInRange((int) killed.getX(), (int) killed.getY(), this.spreadRadius)) {
            this.dealDamage(nearby, explosion);
        }
    }

    /** The first blast's target, which the turret follows; {@code null} when the last look found none. */
    public EnemyMob getPrimaryTarget() {
        return this.blasts.isEmpty() ? null : this.blasts.getFirst().primary();
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    /** The blasts of the latest shot, in the order they were fired. */
    public List<Blast> getBlasts() {
        return this.blasts;
    }

    public float getSpreadRadius() {
        return this.spreadRadius;
    }

    public float getCoolDownFraction() {
        return (float) this.coolDown / this.coolDownCurrent();
    }

    public boolean isSplashVisible() {
        return this.coolDown >= this.coolDownCurrent();
    }

    @Override
    protected List<TowerStatLine> ownStats() {
        return List.of(new TowerStatLine(TowerStat.SPLASH_RADIUS, SPREAD_RADIUS_BASE,
                this.spreadRadius / this.context.getBoard().scale()));
    }

    @Override
    protected List<BehaviourLine> behaviours() {
        boolean triple = this.upgrades().owns(BLAST_ENGINEERING_3.id());
        List<BehaviourLine> lines = new ArrayList<>();
        lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Targets", triple ? "3 random" : "random"));
        if (this.concussiveBlast) {
            lines.add(new BehaviourLine(BehaviourMarker.CHILL, "Chills",
                    BehaviourLine.percent(CHILL_AMOUNT) + ", " + BehaviourLine.seconds(CHILL_DURATION_TICKS)));
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Kills", "explode"));
        }
        if (this.upgrades().owns(TOXIC_BLOOM.id())) {
            lines.add(new BehaviourLine(BehaviourMarker.POISON, "Poisons", BehaviourLine.seconds(Math.round(POISON_SECONDS * TICKS_PER_SECOND))));
        }
        if (this.upgrades().owns(OVERPRESSURE.id())) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Crit arms", "blast on all"));
        }
        return lines;
    }

    @Override
    protected String description() {
        return "Damage falls off away from the blast's centre.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitSplashTower(this);
    }

    /**
     * One blast of a shot: the enemy it centred on, everything it caught, and where and how wide it
     * was.
     *
     * @param critical whether the primary hit was a critical hit
     */
    public record Blast(EnemyMob primary, List<EnemyMob> caught, Area area, boolean critical) {

        /** The disc the blast covered, in board pixels. */
        public record Area(int centerX, int centerY, float radius) {
        }
    }
}
