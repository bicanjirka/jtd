package td.tower;

import td.damage.Damage;
import td.damage.DamageType;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.projectile.MissileProjectile;
import td.projectile.ProjectileStats;
import td.tower.buff.TowerBuff;
import td.tower.seeker.NestPerk;
import td.tower.seeker.SeekerNest;
import td.tower.seeker.SeekerPerk;
import td.tower.seeker.SeekerSpec;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.PreferringSelector;
import td.tower.targeting.Viewpoint;
import td.tower.upgrade.BaseSlotPerks;
import td.tower.upgrade.ExclusiveChoice;
import td.tower.upgrade.OwnedPerks;
import td.tower.upgrade.PerkCatalogue;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The hunter: a slow homing missile at the fastest visible enemy in range, which it never loses
 * once fired. On impact it deals magic damage and freezes whichever enemy it reached. A frozen
 * enemy has stopped, so the next missile hunts the next runner.
 * <p>
 * Attuned, the cooldown loads a missile into a nest instead of firing, and stored missiles launch
 * as a salvo whenever a target is in range; the nest fills between waves too.
 * <p>
 * What each owned node does lives in a {@link SeekerPerk}: the tower reads one spec from them
 * whenever it looks for a target.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class SeekerTower extends AbstractTower {

    public static final int PRICE = 30;
    public static final float DAMAGE_POINTS = 40f;
    public static final float RANGE = 4.5f;
    /** Pixels a tick: slow enough to see a missile hunt. */
    public static final float MISSILE_SPEED = 8f;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.3;
    private static final int FREEZE_DURATION_TICKS_BASE = 30;
    private static final float DEEP_FREEZE_DURATION_MULTIPLIER = 1.75f;
    /** Share of weapon damage a shattered enemy's neighbours take. */
    private static final float SHATTER_DAMAGE_SHARE = 0.5f;
    private static final float SHATTER_RADIUS_CELLS = 1.5f;

    private static final BaseSlotPerks BASE_PERKS = BaseSlotPerks.none()
            .withAttune("Nest: the cooldown loads a missile into the nest (up to 3) instead of firing; with a "
                    + "target in range, stored missiles launch 4 ticks apart");

    private static final UpgradeNode TWIN_WARHEAD_1 = UpgradeTier.HEAD_1.node("seeker.head.twin_warhead.1",
            "Twin Warhead", PRICE)
            .withBuff(TowerBuff.fireRate(0.3f));
    private static final UpgradeNode TWIN_WARHEAD_2 = UpgradeTier.HEAD_2.node("seeker.head.twin_warhead.2",
            "Twin Warhead II", PRICE)
            .withBuff(TowerBuff.fireRate(0.25f))
            .after(TWIN_WARHEAD_1)
            .withExtraEffect("fires two independently-retargeting missiles instead of one");
    private static final UpgradeNode DEEP_FREEZE_1 = UpgradeTier.HEAD_1.node("seeker.head.deep_freeze.1",
            "Deep Freeze", PRICE)
            .withBuff(TowerBuff.damage(0.3f));
    private static final UpgradeNode DEEP_FREEZE_2 = UpgradeTier.HEAD_2.node("seeker.head.deep_freeze.2",
            "Deep Freeze II", PRICE)
            .withBuff(TowerBuff.damage(0.25f))
            .after(DEEP_FREEZE_1)
            .withExtraEffect("+75% freeze duration, killing a frozen enemy shatters it for 50% weapon damage splash");
    private static final UpgradeNode HOMING_CURSE = UpgradeTier.SPECIAL.node("seeker.special.homing_curse",
            "Homing Curse", PRICE)
            .withExtraEffect("impact applies 1 Vulnerable stack, or 2 if the target was already frozen or chilled");

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE, BASE_PERKS))
            .with(TWIN_WARHEAD_1, TWIN_WARHEAD_2, DEEP_FREEZE_1, DEEP_FREEZE_2, HOMING_CURSE)
            .withChoice(ExclusiveChoice.oneOf(TWIN_WARHEAD_1, DEEP_FREEZE_1));

    private static final PerkCatalogue<SeekerPerk> PERKS = PerkCatalogue.<SeekerPerk>empty()
            .with(StandardBaseSlot.ATTUNE_ID, NestPerk::new);

    private static final int COOLDOWN_MAX = 45;

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private final OwnedPerks<SeekerPerk> perks = new OwnedPerks<>(PERKS);
    private final SeekerNest nest = new SeekerNest();
    private final ProjectileStats missile = ProjectileStats.of(MISSILE_SPEED);
    private volatile int freezeDurationTicks = FREEZE_DURATION_TICKS_BASE;
    private volatile boolean twinMissiles = false;
    private int coolDown = 0;
    private EnemyMob currentTarget;

    public SeekerTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SEEKER, new TowerBaseStats(DAMAGE_POINTS, RANGE, COOLDOWN_MAX), context, x, y);
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        this.perks.add(node);
        if (node.equals(DEEP_FREEZE_2)) {
            this.freezeDurationTicks = Math.round(this.freezeDurationTicks * DEEP_FREEZE_DURATION_MULTIPLIER);
        } else if (node.equals(TWIN_WARHEAD_2)) {
            this.twinMissiles = true;
        }
    }

    /** Whom it may fire at, how it picks and what its nest holds, as the perks in {@code owned} make it. */
    private SeekerSpec spec(List<SeekerPerk> owned) {
        Viewpoint view = new Viewpoint(this.centerX, this.centerY, this.rangeReal(), this.context.getBoard().scale());
        SeekerSpec spec = SeekerSpec.from(view);
        for (SeekerPerk perk : owned) {
            spec = perk.refineSpec(spec);
        }
        return spec;
    }

    private EnemyMob findTarget(SeekerSpec spec) {
        return PreferringSelector.priority(spec.aim().selector())
                .selectFrom(spec.reach().matching(this.context.enemies())).orElse(null);
    }

    public void doTick(int gameTime) {
        SeekerSpec spec = this.spec(this.perks.all());
        if (spec.nest().isActive()) {
            this.tickNest(spec);
        } else {
            this.tickDirect(spec);
        }
        if (this.currentTarget != null) {
            this.turretAim.tick(TurretAim.angleTo(this.centerX, this.centerY, this.currentTarget.getX(), this.currentTarget.getY()));
        }
    }

    /** No nest: a missile leaves as the cooldown ends, if there is anything to fire at. */
    private void tickDirect(SeekerSpec spec) {
        if (this.coolDown > 0) {
            this.coolDown--;
            return;
        }
        this.currentTarget = this.findTarget(spec);
        if (this.currentTarget != null) {
            this.launch(this.currentTarget);
            this.coolDown = this.coolDownCurrent();
        }
    }

    /**
     * The cooldown loads a missile whenever there is room, with or without a target; a stored
     * missile launches at the fastest enemy in reach once the gap since the last has passed.
     */
    private void tickNest(SeekerSpec spec) {
        if (this.coolDown > 0) {
            this.coolDown--;
        } else if (this.nest.hasRoom(spec.nest())) {
            this.nest.load();
            this.coolDown = this.coolDownCurrent();
        }
        this.nest.tick();
        if (!this.nest.readyToLaunch()) {
            return;
        }
        EnemyMob target = this.findTarget(spec);
        if (target != null) {
            this.currentTarget = target;
            this.nest.launch(spec.nest());
            this.launch(target);
        }
    }

    /** Launches one missile, or two once Twin Warhead II is owned; each retargets on its own. */
    private void launch(EnemyMob target) {
        this.context.projectiles().add(this.newMissile(target));
        if (this.twinMissiles) {
            this.context.projectiles().add(this.newMissile(target));
        }
    }

    private MissileProjectile newMissile(EnemyMob target) {
        return new MissileProjectile(this.centerX, this.centerY, target, this.context.enemies(), this.missile,
                this::onImpact);
    }

    private void onImpact(EnemyMob target) {
        Set<EffectKind> before = target.activeEffectKinds();
        boolean controlled = before.contains(EffectKind.FREEZE) || before.contains(EffectKind.CHILL);
        this.dealDamage(target, Damage.magic(this.damageCurrent()));
        this.applyEffect(target, sink -> Effect.freeze(this.freezeDurationTicks, sink));
        if (target.hasEffect(EffectKind.FREEZE)) {
            this.countDeedOfAttack();
        }
        if (this.upgrades().owns(HOMING_CURSE.id())) {
            this.applyStacks(target, EffectKind.VULNERABLE, controlled ? 2 : 1);
        }
    }

    /** Deep Freeze II: a frozen enemy this tower kills shatters, hurting whatever stands near it. */
    @Override
    protected void onKill(EnemyMob killed) {
        if (!this.upgrades().owns(DEEP_FREEZE_2.id()) || !killed.activeEffectKinds().contains(EffectKind.FREEZE)) {
            return;
        }
        float radius = SHATTER_RADIUS_CELLS * this.context.getBoard().scale();
        Damage shatter = Damage.magic(Math.round(this.damageCurrent() * SHATTER_DAMAGE_SHARE));
        for (EnemyMob nearby : InRangeTargetQuery.everyone((int) killed.getX(), (int) killed.getY(), radius)
                .matching(this.context.enemies())) {
            this.dealDamage(nearby, shatter);
        }
    }

    public EnemyMob getCurrentTarget() {
        return this.currentTarget;
    }

    int getFreezeDurationTicks() {
        return this.freezeDurationTicks;
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    /** How many missiles the nest holds now; none without Attune. */
    public int getNestStored() {
        return this.nest.stored();
    }

    @Override
    protected DamageType damageType() {
        return DamageType.MAGIC;
    }

    @Override
    protected List<TowerStatLine> ownStats() {
        float cellsPerSecond = this.missile.speed() * TICKS_PER_SECOND / this.context.getBoard().scale();
        return List.of(TowerStatLine.fixed(TowerStat.PROJECTILE_SPEED, cellsPerSecond),
                TowerStatLine.fixed(TowerStat.PROJECTILE_SIZE, this.missile.size()));
    }

    @Override
    protected List<BehaviourLine> behaviours() {
        SeekerSpec spec = this.spec(this.perks.all());
        List<BehaviourLine> lines = new ArrayList<>();
        lines.add(new BehaviourLine(BehaviourMarker.FREEZE, "Freezes", BehaviourLine.seconds(this.freezeDurationTicks)));
        lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Targets", spec.aim().label()));
        if (spec.nest().isActive()) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Nest", this.nest.stored() + "/" + spec.nest().capacity()));
        }
        if (this.upgrades().owns(HOMING_CURSE.id())) {
            lines.add(new BehaviourLine(BehaviourMarker.VULNERABLE, "Impact applies", "1 vulnerable, 2 if chilled"));
        }
        return lines;
    }

    @Override
    protected String description() {
        return "Fires a homing missile.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitSeekerTower(this);
    }
}
