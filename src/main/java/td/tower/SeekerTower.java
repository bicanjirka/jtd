package td.tower;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.projectile.MissileProjectile;
import td.projectile.ProjectileStats;
import td.tower.buff.TowerBuff;
import td.tower.seeker.AbsoluteZeroPerk;
import td.tower.seeker.ArcanePayload;
import td.tower.seeker.ArcaneWarheadPerk;
import td.tower.seeker.BrittlePerk;
import td.tower.seeker.BroodPerk;
import td.tower.seeker.DeepFreezePerk;
import td.tower.seeker.EmpPayload;
import td.tower.seeker.FrostbitePerk;
import td.tower.seeker.FrozenLedger;
import td.tower.seeker.FullRackPerk;
import td.tower.seeker.HuntersMarkPerk;
import td.tower.seeker.Impact;
import td.tower.seeker.NestGrowthPerk;
import td.tower.seeker.NestPerk;
import td.tower.seeker.NullifierPerk;
import td.tower.seeker.OverTheHorizonPerk;
import td.tower.seeker.Payload;
import td.tower.seeker.PayloadLoad;
import td.tower.seeker.PayloadPerk;
import td.tower.seeker.RearmPerk;
import td.tower.seeker.SalvoPlanner;
import td.tower.seeker.SecondMissilePerk;
import td.tower.seeker.SeekerActions;
import td.tower.seeker.SeekerNest;
import td.tower.seeker.SeekerPerk;
import td.tower.seeker.SeekerSpec;
import td.tower.seeker.ShatterPerk;
import td.tower.seeker.ShatterburstPerk;
import td.tower.seeker.TracerPayload;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.PreferringSelector;
import td.tower.targeting.Viewpoint;
import td.tower.upgrade.BaseSlotPerks;
import td.tower.upgrade.ExclusiveChoice;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    /** Share of weapon damage a shattered enemy's neighbours take. */
    private static final float SHATTER_DAMAGE_SHARE = 0.5f;
    private static final float SHATTER_RADIUS_CELLS = 1.5f;

    private static final String FREEZE_DEED = "Freezes";
    private static final int FREEZES_NEEDED = 25;

    private static final BaseSlotPerks BASE_PERKS = BaseSlotPerks.none()
            .withAttune("Nest: the cooldown loads a missile into the nest (up to 3) instead of firing; with a "
                    + "target in range, stored missiles launch 4 ticks apart")
            .withRangeThree(0.1f, "Over the Horizon: may fire at Revealed or Marked enemies within 1.5x its range");

    private static final UpgradeNode TWIN_WARHEAD_1 = UpgradeTier.HEAD_1.node("seeker.head.twin_warhead.1",
            "Twin Warhead", PRICE)
            .withBuff(TowerBuff.fireRate(0.3f))
            .withExtraEffect("the nest holds one more missile");
    private static final UpgradeNode TWIN_WARHEAD_2 = UpgradeTier.HEAD_2.node("seeker.head.twin_warhead.2",
            "Twin Warhead II", PRICE)
            .withExtraEffect("two missiles a shot, the second at the next target")
            .after(TWIN_WARHEAD_1);
    private static final UpgradeNode BROOD = UpgradeTier.HEAD_3.node("seeker.head.twin_warhead.3", "Brood", PRICE)
            .withGate(new PurposeCondition(FREEZE_DEED, FREEZES_NEEDED))
            .withExtraEffect("the nest holds 6; a salvo spreads across different targets")
            .after(TWIN_WARHEAD_2);
    private static final UpgradeNode SHATTERBURST = UpgradeTier.HEAD_4.node("seeker.head.twin_warhead.4a",
            "Shatterburst", PRICE)
            .withExtraEffect("a missile that hits a frozen enemy bursts for 35% magic damage around it; every enemy "
                    + "the burst hits is Silenced 2s and gains Unraveled")
            .after(BROOD);
    private static final UpgradeNode REARM = UpgradeTier.HEAD_4.node("seeker.head.twin_warhead.4b", "Rearm", PRICE)
            .withExtraEffect("a missile that freezes its target launches a new missile from the tower at once; a "
                    + "rearmed missile's freeze rearms nothing")
            .after(BROOD);
    private static final UpgradeNode DEEP_FREEZE_1 = UpgradeTier.HEAD_1.node("seeker.head.deep_freeze.1",
            "Deep Freeze", PRICE)
            .withExtraEffect("+50% freeze time");
    private static final UpgradeNode DEEP_FREEZE_2 = UpgradeTier.HEAD_2.node("seeker.head.deep_freeze.2",
            "Deep Freeze II", PRICE)
            .withBuff(TowerBuff.damage(0.3f).withCritChance(0.1f))
            .withExtraEffect("a frozen enemy this tower kills shatters for 50% of its damage around it")
            .after(DEEP_FREEZE_1);
    private static final UpgradeNode BRITTLE = UpgradeTier.HEAD_3.node("seeker.head.deep_freeze.3", "Brittle", PRICE)
            .withGate(new PurposeCondition(FREEZE_DEED, FREEZES_NEEDED))
            .withExtraEffect("frozen enemies take +30% physical damage")
            .after(DEEP_FREEZE_2);
    private static final UpgradeNode ABSOLUTE_ZERO = UpgradeTier.HEAD_4.node("seeker.head.deep_freeze.4a",
            "Absolute Zero", PRICE)
            .withExtraEffect("the missile freezes everything within 1 cell of the impact; shatters deal x2; an "
                    + "enemy this tower froze shatters when its freeze ends")
            .after(BRITTLE);
    private static final UpgradeNode FROSTBITE = UpgradeTier.HEAD_4.node("seeker.head.deep_freeze.4b", "Frostbite",
            PRICE)
            .withExtraEffect("this tower's hits on a frozen enemy are guaranteed crits, and so are its hits on an "
                    + "enemy whose freezes are diminished by repeated freezing")
            .after(BRITTLE);
    private static final UpgradeNode ARCANE_PAYLOAD = UpgradeTier.EXTRA_1.node("seeker.extra.mixed_payloads.1",
            "Arcane Payload", PRICE)
            .withExtraEffect("every 3rd missile Unravels its target instead of freezing it");
    private static final UpgradeNode EMP_PAYLOAD = UpgradeTier.EXTRA_2.node("seeker.extra.mixed_payloads.2",
            "EMP Payload", PRICE)
            .withExtraEffect("the payload missiles also carry EMP in turn: it strips shields and heals and Silences "
                    + "2s")
            .after(ARCANE_PAYLOAD);
    private static final UpgradeNode TRACER_PAYLOAD = UpgradeTier.EXTRA_3.node("seeker.extra.mixed_payloads.3",
            "Tracer Payload", PRICE)
            .withExtraEffect("the payload missiles also carry Tracer in turn: it reveals and Marks 4s")
            .after(EMP_PAYLOAD);
    private static final UpgradeNode FULL_RACK = UpgradeTier.EXTRA_4.node("seeker.extra.mixed_payloads.4",
            "Full Rack", PRICE)
            .withExtraEffect("every missile carries a payload, cycling Cryo, Arcane, EMP, Tracer, each 25% stronger")
            .after(TRACER_PAYLOAD);
    private static final UpgradeNode ARCANE_WARHEAD = UpgradeTier.SPECIAL.node("seeker.special.arcane_warhead",
            "Arcane Warhead", PRICE)
            .withExtraEffect("every impact applies Unraveled, 2 stacks if the target was frozen or chilled");
    private static final UpgradeNode NULLIFIER = UpgradeTier.SPECIAL.node("seeker.special.nullifier", "Nullifier",
            PRICE)
            .withExtraEffect("every impact strips the target's shield and heal; +50% damage against a shielded "
                    + "enemy");
    private static final UpgradeNode HUNTERS_MARK = UpgradeTier.SPECIAL.node("seeker.special.hunters_mark",
            "Hunter's Mark", PRICE)
            .withExtraEffect("each consecutive hit on the same target +20%, up to +100%; aims at the highest rank");

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE, BASE_PERKS, BROOD, BRITTLE))
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
            .with(TWIN_WARHEAD_1.id(), () -> new NestGrowthPerk(1))
            .with(TWIN_WARHEAD_2.id(), SecondMissilePerk::new)
            .with(BROOD.id(), BroodPerk::new)
            .with(SHATTERBURST.id(), ShatterburstPerk::new)
            .with(REARM.id(), RearmPerk::new)
            .with(DEEP_FREEZE_1.id(), DeepFreezePerk::new)
            .with(DEEP_FREEZE_2.id(), ShatterPerk::new)
            .with(BRITTLE.id(), BrittlePerk::new)
            .with(ABSOLUTE_ZERO.id(), AbsoluteZeroPerk::new)
            .with(FROSTBITE.id(), FrostbitePerk::new)
            .with(ARCANE_PAYLOAD.id(), () -> new PayloadPerk(new ArcanePayload()))
            .with(EMP_PAYLOAD.id(), () -> new PayloadPerk(new EmpPayload()))
            .with(TRACER_PAYLOAD.id(), () -> new PayloadPerk(new TracerPayload()))
            .with(FULL_RACK.id(), FullRackPerk::new)
            .with(ARCANE_WARHEAD.id(), ArcaneWarheadPerk::new)
            .with(NULLIFIER.id(), NullifierPerk::new)
            .with(HUNTERS_MARK.id(), HuntersMarkPerk::new);

    private static final int COOLDOWN_MAX = 45;

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private final OwnedPerks<SeekerPerk> perks = new OwnedPerks<>(PERKS);
    private final SeekerNest nest = new SeekerNest();
    private final ProjectileStats missile = ProjectileStats.of(MISSILE_SPEED);
    private final SeekerActions actions = new Actions();
    private final FrozenLedger frozen = new FrozenLedger();
    private int missilesLaunched;
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

    /**
     * One launch, as the spec shapes it: a missile at the pick among the enemies in reach, and a
     * second at the next one when it sends two. A spread salvo avoids whom it has already fired at.
     * Whether anything was launched; it is not when nothing is in reach.
     */
    private boolean launchShot(SeekerSpec spec) {
        List<EnemyMob> inReach = spec.reach().matching(this.context.enemies());
        Set<EnemyMob> avoided = new HashSet<>(spec.spreadsSalvo() ? this.nest.salvoTargets() : Set.of());
        for (int i = 0; i < spec.missilesPerShot(); i++) {
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
            }
            this.context.projectiles().add(this.newMissile(target.get(), false));
            if (spec.nest().isActive()) {
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
        if (this.launchShot(spec)) {
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
        this.nest.tick(spec.nest());
        if (this.nest.readyToLaunch() && this.launchShot(spec)) {
            this.nest.launch(spec.nest());
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
        float factor = 1f;
        for (SeekerPerk perk : this.perks.all()) {
            factor *= perk.damageFactor(target);
        }
        this.strike(target, Damage.magic(Math.round(this.damageCurrent() * factor)), spec);
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
        Impact impact = new Impact(target, suffering, frozenNow && !wasFrozen, rearmed);
        for (SeekerPerk perk : this.perks.all()) {
            perk.react(impact, this.actions);
        }
    }

    /**
     * A hit of {@code damage}. With Frostbite, a hit on a frozen enemy, or one whose freezes are
     * diminished, is a guaranteed crit.
     */
    private boolean strike(EnemyMob enemy, Damage damage, SeekerSpec spec) {
        AttackProfile attack = this.stats().attack();
        if (spec.frostbite() && (enemy.hasEffect(EffectKind.FREEZE) || enemy.freezeDiminished())) {
            attack = attack.withGuaranteedCrit();
        }
        return this.dealDamage(enemy, damage, attack);
    }

    private int freezeTicks(SeekerSpec spec) {
        return Math.round(FREEZE_DURATION_TICKS_BASE * spec.freeze().durationFactor());
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
        int ticks = Math.round(this.freezeTicks(spec) * strength);
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
        this.burst(shattered, SHATTER_DAMAGE_SHARE * spec.shatter().multiplier(), SHATTER_RADIUS_CELLS, spec);
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
        return this.freezeTicks(this.spec(this.perks.all()));
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
        lines.add(new BehaviourLine(BehaviourMarker.FREEZE, "Freezes", BehaviourLine.seconds(this.freezeTicks(spec))));
        if (spec.freeze().areaCells() > 0f) {
            lines.add(new BehaviourLine(BehaviourMarker.FREEZE, "Freezes around", "1 cell"));
        }
        if (spec.freeze().brittle()) {
            lines.add(new BehaviourLine(BehaviourMarker.FREEZE, "Frozen take", "+30% physical"));
        }
        if (spec.shatter().onKill()) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Shatters",
                    spec.shatter().onThaw() ? "on kill and thaw" : "on kill"));
        }
        if (spec.frostbite()) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Always crits", "frozen enemies"));
        }
        lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Targets", spec.aim().label()));
        if (spec.nest().isActive()) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Nest", this.nest.stored() + "/" + spec.nest().capacity()));
        }
        if (!spec.payloads().owned().isEmpty() || spec.payloads().everyMissile()) {
            String carried = spec.payloads().everyMissile() ? "cryo, arcane, emp, tracer" : spec.payloads().owned()
                    .stream().map(Payload::label).collect(Collectors.joining(", "));
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, spec.payloads().everyMissile() ? "Every missile"
                    : "Every 3rd missile", carried));
        }
        if (this.upgrades().owns(ARCANE_WARHEAD.id())) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Impact applies", "unraveled, x2 if frozen or chilled"));
        }
        if (this.upgrades().owns(NULLIFIER.id())) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Impact strips", "shields and heals"));
        }
        if (this.upgrades().owns(HUNTERS_MARK.id())) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Consecutive hits", "+20% each, up to +100%"));
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
