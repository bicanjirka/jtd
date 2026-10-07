package td.tower;

import td.damage.Damage;
import td.effect.Effect;
import td.enemy.EnemyMob;
import td.projectile.CannonballProjectile;
import td.projectile.ProjectileStats;
import td.tower.buff.TowerBuff;
import td.tower.mortar.BracketDamagePerk;
import td.tower.mortar.BracketTracker;
import td.tower.mortar.BracketingPerk;
import td.tower.mortar.HeavyShellPerk;
import td.tower.mortar.LongBatteryPerk;
import td.tower.mortar.MortarPerk;
import td.tower.mortar.MortarSpec;
import td.tower.mortar.WiderBlastPerk;
import td.tower.targeting.FurthestAlongPathSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.PreferringSelector;
import td.tower.targeting.Viewpoint;
import td.tower.upgrade.BaseSlotPerks;
import td.tower.upgrade.OwnedPerks;
import td.tower.upgrade.PerkCatalogue;
import td.tower.upgrade.PurposeCondition;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Artillery: lobs a slow, unguided shell at the visible enemy furthest along the path, beyond a
 * dead zone around it. The shell flies to where the enemy was when fired, so a fast enemy dodges it.
 * The blast deals physical damage with distance falloff and cracks the plating of everything it
 * reaches.
 * <p>
 * Attuned, a shell that lands near the last one hits harder and wider (Bracketing). What each owned
 * node does lives in a {@link MortarPerk}: the tower reads one spec from them whenever it looks for a
 * target or lands a shell.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class MortarTower extends AbstractTower {

    public static final int PRICE = 30;
    public static final float DAMAGE_POINTS = 32f;
    public static final float RANGE = 4.5f;
    public static final float SPLASH_RADIUS_BASE = 1.75f;
    /** Pixels a tick: slow enough to watch a shell fly and for a fast enemy to dodge it. */
    public static final float SHELL_SPEED = 8f;

    private static final int COOLDOWN_MAX = 70;
    private static final double MAX_TURN_RADIANS_PER_TICK = 0.3;
    private static final int CRACKED_TICKS = 100;
    /** A shell's drawn size grows with the square root of how much harder than base it hits. */
    private static final float SHELL_SIZE_PER_DAMAGE_ROOT = 1f;
    private static final float MAX_SHELL_SIZE = 2.5f;

    private static final String BRACKET_DEED = "Bracketed shells";
    private static final int BRACKETED_SHELLS_NEEDED = 15;

    private static final BaseSlotPerks BASE_PERKS = BaseSlotPerks.none()
            .withAttune("Bracketing: a shell landing within 1.5 cells of the last gets +10% damage and radius, "
                    + "up to 3 steps; a shell elsewhere resets it")
            .withRangeThree(0.3f, "Long Battery: the dead zone grows to 2.5 cells");

    private static final UpgradeNode SIEGE_ROUNDS_1 = UpgradeTier.HEAD_1.node("mortar.head.siege_rounds.1",
            "Siege Rounds", PRICE)
            .withBuff(TowerBuff.damage(0.3f))
            .withExtraEffect("each Bracketing step gives +15% instead of +10%");
    private static final UpgradeNode SIEGE_ROUNDS_2 = UpgradeTier.HEAD_2.node("mortar.head.siege_rounds.2",
            "Siege Rounds II", PRICE)
            .withBuff(TowerBuff.damage(0.25f).withCritChance(0.1f))
            .withExtraEffect("+25% blast radius")
            .after(SIEGE_ROUNDS_1);
    private static final UpgradeNode HEAVY_SHELL = UpgradeTier.HEAD_3.node("mortar.head.siege_rounds.3",
            "Heavy Shell", PRICE)
            .withBuff(TowerBuff.damage(0.5f).withFireRate(-0.2f))
            .withGate(new PurposeCondition(BRACKET_DEED, BRACKETED_SHELLS_NEEDED))
            .withExtraEffect("a bigger, slower shell; enemies within 0.5 cells of the impact are Dazed 0.5s")
            .after(SIEGE_ROUNDS_2);

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE, BASE_PERKS, HEAVY_SHELL))
            .with(SIEGE_ROUNDS_1, SIEGE_ROUNDS_2, HEAVY_SHELL);

    private static final PerkCatalogue<MortarPerk> PERKS = PerkCatalogue.<MortarPerk>empty()
            .with(StandardBaseSlot.ATTUNE_ID, BracketingPerk::new)
            .with(StandardBaseSlot.RANGE_3_ID, LongBatteryPerk::new)
            .with(SIEGE_ROUNDS_1.id(), BracketDamagePerk::new)
            .with(SIEGE_ROUNDS_2.id(), WiderBlastPerk::new)
            .with(HEAVY_SHELL.id(), HeavyShellPerk::new);

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private final OwnedPerks<MortarPerk> perks = new OwnedPerks<>(PERKS);
    private final BracketTracker bracketing = new BracketTracker();
    private int coolDown = 0;
    private EnemyMob currentTarget;

    public MortarTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.MORTAR, new TowerBaseStats(DAMAGE_POINTS, RANGE, COOLDOWN_MAX), context, x, y);
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        this.perks.add(node);
    }

    /** Whom it may shell and how its shells fly and land, as the perks in {@code owned} make it. */
    private MortarSpec spec(List<MortarPerk> owned) {
        Viewpoint view = new Viewpoint(this.centerX, this.centerY, this.rangeReal(), this.context.getBoard().scale());
        MortarSpec spec = MortarSpec.from(view);
        for (MortarPerk perk : owned) {
            spec = perk.refineSpec(spec);
        }
        return spec;
    }

    private EnemyMob findTarget(MortarSpec spec) {
        List<EnemyMob> inReach = spec.reach().matching(this.context.enemies());
        return PreferringSelector.priority(new FurthestAlongPathSelector()).selectFrom(inReach).orElse(null);
    }

    public void doTick(int gameTime) {
        MortarSpec spec = this.spec(this.perks.all());
        if (this.coolDown > 0) {
            this.coolDown--;
        } else {
            this.currentTarget = this.findTarget(spec);
            if (this.currentTarget != null) {
                this.fireAt(this.currentTarget, spec);
                this.coolDown = this.coolDownCurrent();
            }
        }
        if (this.currentTarget != null) {
            this.turretAim.tick(TurretAim.angleTo(this.centerX, this.centerY, this.currentTarget.getX(), this.currentTarget.getY()));
        }
    }

    private void fireAt(EnemyMob target, MortarSpec spec) {
        this.context.projectiles().add(new CannonballProjectile(this.centerX, this.centerY, target.getX(),
                target.getY(), this.shellStats(spec), this::onImpact));
    }

    /** The shell as it flies and is drawn: its speed and size are stats, and its size follows how hard it hits. */
    private ProjectileStats shellStats(MortarSpec spec) {
        float size = Math.min(MAX_SHELL_SIZE, SHELL_SIZE_PER_DAMAGE_ROOT
                * (float) Math.sqrt((double) this.damageCurrent() / this.damageBase));
        return ProjectileStats.of(SHELL_SPEED * spec.speedScale()).withSize(size * spec.sizeScale());
    }

    private float blastRadius(MortarSpec spec, float stepScale) {
        return SPLASH_RADIUS_BASE * this.context.getBoard().scale() * spec.blastScale() * stepScale;
    }

    private void onImpact(double x, double y) {
        MortarSpec spec = this.spec(this.perks.all());
        int scale = this.context.getBoard().scale();
        int step = this.bracketing.land(x, y, spec.bracket(), scale);
        if (step > 0) {
            this.countDeedOfAttack();
        }
        float radius = this.blastRadius(spec, 1f + step * spec.bracket().radiusStep());
        float damage = this.damageCurrent() * (1f + step * spec.bracket().damageStep());
        List<EnemyMob> hit = InRangeTargetQuery.everyone((int) Math.round(x), (int) Math.round(y), radius)
                .matching(this.context.enemies());
        for (EnemyMob enemy : hit) {
            double dx = x - enemy.getX();
            double dy = y - enemy.getY();
            float falloff = 1f - (float) ((dx * dx + dy * dy) / ((double) radius * radius));
            this.dealDamage(enemy, Damage.physical(Math.round(damage * falloff)));
            this.applyEffect(enemy, sink -> Effect.cracked(CRACKED_TICKS, sink));
        }
        if (spec.daze().isActive()) {
            float dazeRadius = spec.daze().radiusCells() * scale;
            for (EnemyMob enemy : hit) {
                double dx = x - enemy.getX();
                double dy = y - enemy.getY();
                if (dx * dx + dy * dy <= (double) dazeRadius * dazeRadius) {
                    this.applyEffect(enemy, sink -> Effect.dazed(spec.daze().ticks(), sink));
                }
            }
        }
    }

    public EnemyMob getCurrentTarget() {
        return this.currentTarget;
    }

    /** The blast's radius in pixels before Bracketing. */
    float getSplashRadius() {
        return this.blastRadius(this.spec(this.perks.all()), 1f);
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    /** The last landing and how tight its bracket is, for the ranging marker; empty when not attuned. */
    public Optional<BracketTracker.Marker> getRangingMarker() {
        return this.bracketing.marker();
    }

    @Override
    protected List<TowerStatLine> ownStats() {
        MortarSpec spec = this.spec(this.perks.all());
        ProjectileStats shell = this.shellStats(spec);
        float cellsPerSecond = shell.speed() * TICKS_PER_SECOND / this.context.getBoard().scale();
        return List.of(new TowerStatLine(TowerStat.SPLASH_RADIUS, SPLASH_RADIUS_BASE, SPLASH_RADIUS_BASE * spec.blastScale()),
                TowerStatLine.fixed(TowerStat.PROJECTILE_SPEED, cellsPerSecond),
                TowerStatLine.fixed(TowerStat.PROJECTILE_SIZE, shell.size()));
    }

    @Override
    protected List<BehaviourLine> behaviours() {
        MortarSpec spec = this.spec(this.perks.all());
        List<BehaviourLine> lines = new ArrayList<>();
        lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Targets", "first"));
        lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Dead zone",
                Math.round(spec.reach().deadZone() / this.context.getBoard().scale() * 10f) / 10f + " cells"));
        lines.add(new BehaviourLine(BehaviourMarker.CRACKED, "Cracks plating", BehaviourLine.seconds(CRACKED_TICKS)));
        if (spec.bracket().active()) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Bracketing",
                    "+" + Math.round(spec.bracket().damageStep() * 100) + "% damage, +"
                            + Math.round(spec.bracket().radiusStep() * 100) + "% radius a step"));
        }
        if (spec.daze().isActive()) {
            lines.add(new BehaviourLine(BehaviourMarker.DAZE, "Impact dazes",
                    BehaviourLine.seconds(spec.daze().ticks())));
        }
        return lines;
    }

    @Override
    protected String description() {
        return "Lobs a slow, unguided shell.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitMortarTower(this);
    }
}
