package td.tower;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.projectile.MissileProjectile;
import td.projectile.ProjectileStats;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.proc.HitAdjustment;
import td.tower.proc.HitBonus;
import td.tower.proc.HitCondition;
import td.tower.proc.HitKind;
import td.tower.proc.HitScaling;
import td.tower.proc.Proc;
import td.tower.proc.ProcCondition;
import td.tower.proc.ProcPayload;
import td.tower.seeker.AbsoluteZeroPerk;
import td.tower.seeker.ArcanePayload;
import td.tower.seeker.ArcaneWarheadPerk;
import td.tower.seeker.BrittlePerk;
import td.tower.seeker.BroodPerk;
import td.tower.seeker.EmpPayload;
import td.tower.seeker.FrostbitePerk;
import td.tower.seeker.FrozenLedger;
import td.tower.seeker.FullRackPerk;
import td.tower.seeker.Impact;
import td.tower.seeker.NestPerk;
import td.tower.seeker.NestSpec;
import td.tower.seeker.OverTheHorizonPerk;
import td.tower.seeker.PayloadLoad;
import td.tower.seeker.PayloadPerk;
import td.tower.seeker.PayloadPlan;
import td.tower.seeker.RearmPerk;
import td.tower.seeker.SalvoPlanner;
import td.tower.seeker.SeekerActions;
import td.tower.seeker.SeekerAim;
import td.tower.seeker.SeekerNest;
import td.tower.seeker.SeekerPerk;
import td.tower.seeker.SeekerSpec;
import td.tower.seeker.ShatterPerk;
import td.tower.seeker.ShatterburstPerk;
import td.tower.seeker.TargetStreak;
import td.tower.seeker.TracerPayload;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.PreferringSelector;
import td.tower.targeting.TargetPriority;
import td.tower.targeting.Viewpoint;
import td.tower.upgrade.BaseSlotPerks;
import td.tower.upgrade.ExclusiveChoice;
import td.tower.upgrade.NodeEffects;
import td.tower.upgrade.OwnedPerks;
import td.tower.upgrade.PerkCatalogue;
import td.tower.upgrade.PurposeCondition;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
    /** How far a shatter reaches. */
    private static final float SHATTER_RADIUS_CELLS = 1.5f;

    private static final TermText FREEZE_DEED = TermText.format("times {} landed", EffectKind.FREEZE);
    private static final int FREEZES_NEEDED = 25;

    private static final BaseSlotPerks BASE_PERKS = BaseSlotPerks.none().withRangeThree(0.1f, NodeEffects.none());

    private static final UpgradeNode TWIN_WARHEAD_1 = UpgradeTier.HEAD_1.node("seeker.head.twin_warhead.1",
            "Twin Warhead", PRICE)
            .withModifiers(TowerModifiers.attackSpeed(0.3f).plus(TowerModifiers.nestCapacity(1)));
    private static final UpgradeNode TWIN_WARHEAD_2 = UpgradeTier.HEAD_2.node("seeker.head.twin_warhead.2",
            "Twin Warhead II", PRICE)
            .withModifiers(TowerModifiers.missilesPerLaunch(1))
            .after(TWIN_WARHEAD_1);
    private static final UpgradeNode BROOD = UpgradeTier.HEAD_3.node("seeker.head.twin_warhead.3", "Brood", PRICE)
            .withGate(new PurposeCondition(FREEZE_DEED, FREEZES_NEEDED))
            .withModifiers(TowerModifiers.nestCapacity(2))
            .after(TWIN_WARHEAD_2);
    private static final UpgradeNode SHATTERBURST = UpgradeTier.HEAD_4.node("seeker.head.twin_warhead.4a",
            "Shatterburst", PRICE)
            .after(BROOD);
    private static final UpgradeNode REARM = UpgradeTier.HEAD_4.node("seeker.head.twin_warhead.4b", "Rearm", PRICE)
            .after(BROOD);
    private static final UpgradeNode DEEP_FREEZE_1 = UpgradeTier.HEAD_1.node("seeker.head.deep_freeze.1",
            "Deep Freeze", PRICE)
            .withModifiers(TowerModifiers.freezeDuration(15));
    private static final UpgradeNode DEEP_FREEZE_2 = UpgradeTier.HEAD_2.node("seeker.head.deep_freeze.2",
            "Deep Freeze II", PRICE)
            .withModifiers(TowerModifiers.damage(12f).plus(TowerModifiers.critChance(0.1f)))
            .after(DEEP_FREEZE_1);
    private static final UpgradeNode BRITTLE = UpgradeTier.HEAD_3.node("seeker.head.deep_freeze.3", "Brittle", PRICE)
            .withGate(new PurposeCondition(FREEZE_DEED, FREEZES_NEEDED))
            .after(DEEP_FREEZE_2);
    private static final UpgradeNode ABSOLUTE_ZERO = UpgradeTier.HEAD_4.node("seeker.head.deep_freeze.4a",
            "Absolute Zero", PRICE)
            .after(BRITTLE);
    private static final UpgradeNode FROSTBITE = UpgradeTier.HEAD_4.node("seeker.head.deep_freeze.4b", "Frostbite",
            PRICE)
            .after(BRITTLE);
    private static final UpgradeNode ARCANE_PAYLOAD = UpgradeTier.EXTRA_1.node("seeker.extra.mixed_payloads.1",
            "Arcane Payload", PRICE);
    private static final UpgradeNode EMP_PAYLOAD = UpgradeTier.EXTRA_2.node("seeker.extra.mixed_payloads.2",
            "EMP Payload", PRICE)
            .after(ARCANE_PAYLOAD);
    private static final UpgradeNode TRACER_PAYLOAD = UpgradeTier.EXTRA_3.node("seeker.extra.mixed_payloads.3",
            "Tracer Payload", PRICE)
            .after(EMP_PAYLOAD);
    private static final UpgradeNode FULL_RACK = UpgradeTier.EXTRA_4.node("seeker.extra.mixed_payloads.4",
            "Full Rack", PRICE)
            .after(TRACER_PAYLOAD);
    private static final UpgradeNode ARCANE_WARHEAD = UpgradeTier.SPECIAL.node("seeker.special.arcane_warhead",
            "Arcane Warhead", PRICE);
    private static final UpgradeNode NULLIFIER = UpgradeTier.SPECIAL.node("seeker.special.nullifier", "Nullifier",
            PRICE)
            .withHitBonus(HitBonus.when(new HitCondition.TargetHasAnyOf(Set.of(EffectKind.SHIELD)),
                    HitAdjustment.damage(0.5f)))
            .withProc(Proc.onHit(new ProcPayload.Dispel()).when(new ProcCondition.HitKindIn(Set.of(HitKind.MISSILE))));
    private static final UpgradeNode HUNTERS_MARK = UpgradeTier.SPECIAL.node("seeker.special.hunters_mark",
            "Hunter's Mark", PRICE)
            .withTargeting(TargetPriority.HIGHEST_RANK)
            .withHitBonus(HitBonus.per(new HitScaling.PerFocus(TowerTerm.FOCUS, 5), HitAdjustment.damage(0.2f)));

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE, RANGE, BASE_PERKS, BROOD, BRITTLE))
            .with(TWIN_WARHEAD_1, TWIN_WARHEAD_2, BROOD, SHATTERBURST, REARM, DEEP_FREEZE_1, DEEP_FREEZE_2, BRITTLE,
                    ABSOLUTE_ZERO, FROSTBITE, ARCANE_PAYLOAD, EMP_PAYLOAD, TRACER_PAYLOAD, FULL_RACK,
                    ARCANE_WARHEAD, NULLIFIER, HUNTERS_MARK)
            .withChoice(ExclusiveChoice.oneOf(TWIN_WARHEAD_1, DEEP_FREEZE_1))
            .withChoice(ExclusiveChoice.oneOf(SHATTERBURST, REARM))
            .withChoice(ExclusiveChoice.oneOf(ABSOLUTE_ZERO, FROSTBITE))
            .withChoice(ExclusiveChoice.specials(ARCANE_WARHEAD, NULLIFIER, HUNTERS_MARK));

    private static final PerkCatalogue<SeekerPerk> PERKS = PerkCatalogue.<SeekerPerk>empty()
            .with(StandardBaseSlot.ATTUNE_ID, NestPerk::new)
            .with(StandardBaseSlot.RANGE_3_ID, OverTheHorizonPerk::new)
            .with(BROOD.id(), BroodPerk::new)
            .with(SHATTERBURST.id(), ShatterburstPerk::new)
            .with(REARM.id(), RearmPerk::new)
            .with(DEEP_FREEZE_2.id(), ShatterPerk::new)
            .with(BRITTLE.id(), BrittlePerk::new)
            .with(ABSOLUTE_ZERO.id(), AbsoluteZeroPerk::new)
            .with(FROSTBITE.id(), FrostbitePerk::new)
            .with(ARCANE_PAYLOAD.id(), () -> new PayloadPerk(new ArcanePayload()))
            .with(EMP_PAYLOAD.id(), () -> new PayloadPerk(new EmpPayload()))
            .with(TRACER_PAYLOAD.id(), () -> new PayloadPerk(new TracerPayload()))
            .with(FULL_RACK.id(), FullRackPerk::new)
            .with(ARCANE_WARHEAD.id(), ArcaneWarheadPerk::new);

    private static final int COOLDOWN_MAX = 45;

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private final OwnedPerks<SeekerPerk> perks = new OwnedPerks<>(PERKS);
    private final TargetStreak streak = new TargetStreak();
    private final SeekerNest nest = new SeekerNest();
    private final ProjectileStats missile = ProjectileStats.of(MISSILE_SPEED);
    private final SeekerActions actions = new Actions();
    private final FrozenLedger frozen = new FrozenLedger();
    private final FireClock clock = new FireClock(COOLDOWN_MAX);
    private int missilesLaunched;
    private EnemyMob currentTarget;

    public SeekerTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SEEKER, new TowerBaseStats(DAMAGE_POINTS, RANGE, COOLDOWN_MAX)
                .with(TowerStat.FREEZE_DURATION, 30f).with(TowerStat.NEST_CAPACITY, 3f)
                .with(TowerStat.MISSILES_PER_LAUNCH, 1f), context, x, y);
    }

    @Override
    protected PerkCatalogue<SeekerPerk> perkCatalogue() {
        return PERKS;
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        this.perks.add(node);
    }

    /** Whom it may fire at, how it picks and what its nest holds, as the perks in {@code owned} make it. */
    private SeekerSpec spec(List<SeekerPerk> owned) {
        Viewpoint view = new Viewpoint(this.centerX, this.centerY, this.rangeReal(), this.context.getBoard().scale());
        SeekerSpec spec = SeekerSpec.from(view);
        Optional<TargetPriority> priority = this.upgrades().targeting();
        if (priority.isPresent()) {
            spec = spec.withAim(new SeekerAim(priority.get().selector(view.cellSize()), priority.get().label()));
        }
        for (SeekerPerk perk : owned) {
            spec = perk.refineSpec(spec);
        }
        return spec;
    }

    private EnemyMob findTarget(SeekerSpec spec) {
        return PreferringSelector.priority(spec.aim().selector())
                .selectFrom(spec.reach().matching(this.context.enemies())).orElse(null);
    }

    /**
     * One launch, as the spec shapes it: a missile at the pick among the enemies in reach, and a
     * second at the next one when it sends two. A spread salvo avoids whom it has already fired at.
     * Whether anything was launched; it is not when nothing is in reach.
     */
    private boolean launchShot(SeekerSpec spec) {
        List<EnemyMob> inReach = spec.reach().matching(this.context.enemies());
        Set<EnemyMob> avoided = new HashSet<>(spec.spreadsSalvo() ? this.nest.salvoTargets() : Set.of());
        int missiles = Math.round(this.stats().value(TowerStat.MISSILES_PER_LAUNCH));
        for (int i = 0; i < missiles; i++) {
            Optional<EnemyMob> target = SalvoPlanner.pick(PreferringSelector.priority(spec.aim().selector()), inReach,
                    avoided);
            if (target.isEmpty()) {
                if (i == 0) {
                    this.currentTarget = null;
                }
                return i > 0;
            }
            if (i == 0) {
                this.currentTarget = target.get();
                this.beginShot();
            }
            this.context.projectiles().add(this.newMissile(target.get(), false));
            if (this.nest(spec).isActive()) {
                this.nest.recordTarget(target.get());
            }
            avoided.add(target.get());
        }
        return true;
    }

    public void doTick(int gameTime) {
        SeekerSpec spec = this.spec(this.perks.all());
        if (spec.shatter().onThaw()) {
            this.frozen.settle().forEach(thawed -> this.shatter(thawed, spec));
        }
        if (this.nest(spec).isActive()) {
            this.tickNest(spec);
        } else {
            this.tickDirect(spec);
        }
        this.clock.advance(this.attackRate());
        if (this.currentTarget != null) {
            this.turretAim.tick(TurretAim.angleTo(this.centerX, this.centerY, this.currentTarget.getX(), this.currentTarget.getY()));
        }
    }

    /** No nest: a missile leaves as the cooldown ends, if there is anything to fire at. */
    private void tickDirect(SeekerSpec spec) {
        if (this.clock.isReady() && this.launchShot(spec)) {
            this.clock.spend();
        }
    }

    /**
     * The cooldown loads a missile whenever there is room, with or without a target; a stored
     * missile launches at the fastest enemy in reach once the gap since the last has passed.
     */
    private void tickNest(SeekerSpec spec) {
        NestSpec nest = this.nest(spec);
        if (this.clock.isReady() && this.nest.hasRoom(nest)) {
            this.nest.load();
            this.clock.spend();
        }
        this.nest.tick(nest);
        if (this.nest.readyToLaunch() && this.launchShot(spec)) {
            this.nest.launch(nest);
        }
    }

    /** A missile at {@code target}, carrying what the plan says its number carries; a rearmed one's freeze rearms nothing. */
    private MissileProjectile newMissile(EnemyMob target, boolean rearmed) {
        this.missilesLaunched++;
        Optional<PayloadLoad> load = this.spec(this.perks.all()).payloads().loadFor(this.missilesLaunched);
        ProjectileStats stats = load.map(carried -> this.missile.withLook(carried.payload().look())).orElse(this.missile);
        return new MissileProjectile(this.centerX, this.centerY, target, this.context.enemies(), stats,
                hit -> this.onImpact(hit, rearmed, load));
    }

    /**
     * A missile lands: the hit, scaled by what the perks make of it, then what it carries (a plain
     * missile freezes), then the perks' reactions.
     */
    private void onImpact(EnemyMob target, boolean rearmed, Optional<PayloadLoad> load) {
        SeekerSpec spec = this.spec(this.perks.all());
        Set<EffectKind> before = target.activeEffectKinds();
        boolean wasFrozen = before.contains(EffectKind.FREEZE);
        Impact.Before suffering = wasFrozen ? Impact.Before.FROZEN
                : before.contains(EffectKind.CHILL) ? Impact.Before.CHILLED : Impact.Before.NOTHING;
        HitAdjustment adjustment = this.hitAdjustment(target, HitKind.MISSILE, 0f, this.stats(), this.streak.on(target),
                false);
        float factor = adjustment.damageFactor();
        for (SeekerPerk perk : this.perks.all()) {
            factor *= perk.damageFactor(target);
        }
        boolean critical = this.strike(target, Damage.magic(Math.round(this.damageCurrent() * factor)), spec, adjustment);
        boolean freezes = load.isEmpty() || load.get().payload().freezes();
        if (load.isEmpty()) {
            this.freezeAround(target, 1f, spec);
        } else {
            load.get().payload().deliver(target, load.get().strength(), this.actions);
        }
        boolean frozenNow = freezes && target.hasEffect(EffectKind.FREEZE);
        if (frozenNow) {
            this.countDeedOfAttack();
        }
        this.streak.landed(target);
        this.report(target, HitKind.MISSILE, critical, target.isDead());
        Impact impact = new Impact(target, suffering, frozenNow && !wasFrozen, rearmed);
        for (SeekerPerk perk : this.perks.all()) {
            perk.react(impact, this.actions);
        }
    }

    /**
     * A hit of {@code damage}. With Frostbite, a hit on a frozen enemy, or one whose freezes are
     * diminished, is a guaranteed crit.
     */
    private void strike(EnemyMob enemy, Damage damage, SeekerSpec spec) {
        this.strike(enemy, damage, spec, HitAdjustment.none());
    }

    private boolean strike(EnemyMob enemy, Damage damage, SeekerSpec spec, HitAdjustment adjustment) {
        AttackProfile attack = adjustment.applyTo(this.stats().attack());
        if (spec.frostbite() && (enemy.hasEffect(EffectKind.FREEZE) || enemy.freezeDiminished())) {
            attack = attack.withGuaranteedCrit();
        }
        return this.dealDamage(enemy, damage, attack);
    }

    private int freezeTicks() {
        return Math.round(this.stats().value(TowerStat.FREEZE_DURATION));
    }

    private NestSpec nest(SeekerSpec spec) {
        return spec.launchGapTicks() > 0
                ? NestSpec.of(Math.round(this.stats().value(TowerStat.NEST_CAPACITY)), spec.launchGapTicks())
                : NestSpec.none();
    }

    /** Freezes the enemy a missile reached, and with Absolute Zero everything within a cell of it. */
    private void freezeAround(EnemyMob target, float strength, SeekerSpec spec) {
        this.freeze(target, strength, spec);
        if (spec.freeze().areaCells() > 0f) {
            float radius = spec.freeze().areaCells() * this.context.getBoard().scale();
            InRangeTargetQuery.everyone((int) target.getX(), (int) target.getY(), radius)
                    .matching(this.context.enemies()).stream()
                    .filter(enemy -> enemy != target)
                    .forEach(enemy -> this.freeze(enemy, strength, spec));
        }
    }

    /**
     * Freezes {@code enemy} for {@code strength} times the freeze time, and with Brittle leaves it
     * brittle for as long; with a thaw shatter, watches it.
     */
    private void freeze(EnemyMob enemy, float strength, SeekerSpec spec) {
        int ticks = Math.round(this.freezeTicks() * strength);
        this.applyEffect(enemy, sink -> Effect.freeze(ticks, sink));
        if (spec.freeze().brittle()) {
            this.applyEffect(enemy, sink -> Effect.brittle(ticks, sink));
        }
        if (spec.shatter().onThaw() && enemy.hasEffect(EffectKind.FREEZE)) {
            this.frozen.record(enemy);
        }
    }

    /** Hits every other enemy within {@code radiusCells} of {@code center} for {@code share} of the damage, as magic. */
    private List<EnemyMob> burst(EnemyMob center, float share, float radiusCells, SeekerSpec spec) {
        float radius = radiusCells * this.context.getBoard().scale();
        Damage damage = Damage.magic(Math.round(this.damageCurrent() * share));
        List<EnemyMob> hit = InRangeTargetQuery.everyone((int) center.getX(), (int) center.getY(), radius)
                .matching(this.context.enemies()).stream()
                .filter(enemy -> enemy != center)
                .toList();
        hit.forEach(enemy -> this.strike(enemy, damage, spec));
        return hit;
    }

    /** A frozen enemy shatters: whatever stands near it takes a share of the Seeker's damage. */
    private void shatter(EnemyMob shattered, SeekerSpec spec) {
        this.burst(shattered, spec.shatter().share(), SHATTER_RADIUS_CELLS, spec);
    }

    /** Deep Freeze II: a frozen enemy this tower kills shatters. */
    @Override
    protected void onKill(EnemyMob killed) {
        SeekerSpec spec = this.spec(this.perks.all());
        if (spec.shatter().onKill() && killed.hasEffect(EffectKind.FREEZE)) {
            this.shatter(killed, spec);
        }
    }

    public EnemyMob getCurrentTarget() {
        return this.currentTarget;
    }

    int getFreezeDurationTicks() {
        return this.freezeTicks();
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
        return List.of(TowerStatLine.fixed(TowerStatRow.PROJECTILE_SPEED, cellsPerSecond),
                TowerStatLine.fixed(TowerStatRow.PROJECTILE_SIZE, this.missile.size()));
    }

    @Override
    protected List<TowerBuffState> ownConditions() {
        List<TowerBuffState> states = new ArrayList<>();
        if (this.streak.current() > 0) {
            states.add(TowerBuffState.of(TowerTerm.FOCUS, TermText.of("hits on one target build it"))
                    .withStacks(this.streak.current()));
        }
        SeekerSpec spec = this.spec(this.perks.all());
        NestSpec nest = this.nest(spec);
        if (nest.isActive() && this.nest.stored() > 0) {
            states.add(TowerBuffState.of(TowerTerm.NEST, TermText.of(this.nest.stored() + " of " + nest.capacity()
                    + " missiles stored")).withStacks(this.nest.stored()));
        }
        spec.payloads().loadFor(this.missilesLaunched + 1).ifPresent(load -> states.add(
                TowerBuffState.of(load.payload().term(), load.payload().effect())));
        this.perks.all().forEach(perk -> perk.condition().ifPresent(states::add));
        return states;
    }

    @Override
    protected List<NodeProgress> ownProgress() {
        if (this.spec(this.perks.all()).payloads().everyMissile()) {
            return List.of();
        }
        return List.of(ARCANE_PAYLOAD, EMP_PAYLOAD, TRACER_PAYLOAD).stream()
                .flatMap(node -> this.countingNode(node.id(), this.missilesLaunched % PayloadPlan.EVERY,
                        PayloadPlan.EVERY).stream())
                .toList();
    }

    @Override
    protected List<AppliedEffect> baseApplies() {
        return List.of(new AppliedEffect(EffectKind.FREEZE, List.of(new AppliedEffect.Source(this.displayName(),
                TermText.of("each missile applies ").plus(EffectKind.FREEZE)
                        .plus(" for " + TermNumbers.seconds((this.freezeTicks()) / TickRate.TICKS_PER_SECOND))))));
    }

    @Override
    protected String targetingLabel() {
        return this.spec(this.perks.all()).aim().label();
    }

    @Override
    protected String description() {
        return "Fires a homing missile.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitSeekerTower(this);
    }

    /** What the perks may make this tower do. */
    private final class Actions implements SeekerActions {

        @Override
        public List<EnemyMob> burst(EnemyMob center, float share, float radiusCells) {
            return SeekerTower.this.burst(center, share, radiusCells, SeekerTower.this.spec(SeekerTower.this.perks.all()));
        }

        @Override
        public void freeze(EnemyMob target, float strength) {
            SeekerTower.this.freezeAround(target, strength, SeekerTower.this.spec(SeekerTower.this.perks.all()));
        }

        @Override
        public void dispel(EnemyMob target) {
            target.dispelRestoratives();
        }

        @Override
        public void spot(EnemyMob target, int ticks) {
            SeekerTower.this.reveal(target, ticks);
            SeekerTower.this.applyEffect(target, sink -> Effect.marked(ticks, sink));
        }

        @Override
        public void silence(EnemyMob target, int ticks) {
            SeekerTower.this.applyEffect(target, sink -> Effect.silenced(ticks, sink));
        }

        @Override
        public void applyStacks(EnemyMob target, EffectKind kind, int stacks) {
            SeekerTower.this.applyStacks(target, kind, stacks);
        }

        @Override
        public void rearm() {
            EnemyMob target = SeekerTower.this.findTarget(SeekerTower.this.spec(SeekerTower.this.perks.all()));
            if (target != null) {
                SeekerTower.this.context.projectiles().add(SeekerTower.this.newMissile(target, true));
            }
        }
    }
}
