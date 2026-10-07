package td.tower;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.splash.ArcPerk;
import td.tower.splash.ArcPlanner;
import td.tower.splash.ArcSpec;
import td.tower.splash.ArcStrike;
import td.tower.splash.BlastRadiusPerk;
import td.tower.splash.BlastSpec;
import td.tower.splash.ChainLightningPerk;
import td.tower.splash.ConductorPerk;
import td.tower.splash.FireControlPerk;
import td.tower.splash.LightningRodPerk;
import td.tower.splash.MasteryPerk;
import td.tower.splash.OverloadPerk;
import td.tower.splash.PotencyPerk;
import td.tower.splash.SaturationRule;
import td.tower.splash.ShapedChargePerk;
import td.tower.splash.ShotContext;
import td.tower.splash.ShotResult;
import td.tower.splash.SplashActions;
import td.tower.splash.SplashPerk;
import td.tower.splash.SplashShot;
import td.tower.splash.SplashSpec;
import td.tower.splash.StaticChargePerk;
import td.tower.splash.ThunderclapPerk;
import td.tower.splash.ThunderstrikePerk;
import td.tower.splash.WideChargePerk;
import td.tower.targeting.HighestHealthSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.MostNeighboursSelector;
import td.tower.targeting.PreferringSelector;
import td.tower.targeting.RandomSelector;
import td.tower.targeting.TargetSelector;
import td.tower.targeting.Viewpoint;
import td.tower.upgrade.BaseSlotPerks;
import td.tower.upgrade.ExclusiveChoice;
import td.tower.upgrade.OwnedPerks;
import td.tower.upgrade.PerkCatalogue;
import td.tower.upgrade.PurposeCondition;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeCondition;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Cheap, instant, never misses: it picks a visible enemy in range and blasts everything around it,
 * hidden enemies included, the damage falling off from the centre. Forked, the blast carries a
 * payload: lightning that arcs on past it (the Stormcaller) or curses (the Hexer).
 * <p>
 * What each owned node does lives in a {@link SplashPerk}: the tower reads one spec from them for
 * every shot.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class SplashTower extends AbstractTower {

    public static final int PRICE = 15;
    public static final float DAMAGE_POINTS = 14f;
    public static final float RANGE = 3.2f;
    public static final float BLAST_RADIUS_CELLS = 1.5f;
    /** Each Saturation stack on an enemy makes this tower's blast hit it this much harder. */
    public static final float SATURATION_DAMAGE_PER_STACK = 0.05f;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.25;
    /** One shot a second. */
    private static final int COOLDOWN_MAX = 19;
    /** Saturation fades this long after the last blast that caught the enemy. */
    private static final float SATURATION_SECONDS = 1.5f;
    private static final String SATURATED_BLAST_DEED = "Saturated blasts";
    /** How long a Thunderclap or a Thunderstrike Dazes. */
    private static final float DAZE_SECONDS = 0.5f;
    private static final float THUNDERCLAP_SHARE = 0.5f;
    private static final float THUNDERSTRIKE_FACTOR = 4f;
    /** How far above its target a Thunderstrike's bolt is drawn from. */
    private static final float THUNDERSTRIKE_HEIGHT_CELLS = 2f;
    private static final float STATIC_CHARGE_SECONDS = 3f;
    private static final TargetSelector THUNDERSTRIKE_AIM = PreferringSelector.priority(new HighestHealthSelector());
    private static final int SATURATED_BLASTS_NEEDED = 45;

    private static final BaseSlotPerks BASE_PERKS = BaseSlotPerks.none()
            .withAttune("Fire Control: aims at the enemy with most neighbours in the blast; Saturation: +5% blast "
                    + "damage a stack, 3 stacks")
            .withRangeThree(0.1f, "blast radius +10%");

    private static final UpgradeNode ARC = UpgradeTier.HEAD_1.node("splash.head.arc.1", "Arc", PRICE)
            .withExtraEffect("the blast carries past its edge: 2 jumps at 50% as magic, further from Saturated "
                    + "enemies");
    private static final UpgradeNode CONDUCTOR = UpgradeTier.HEAD_2.node("splash.head.arc.2", "Conductor", PRICE)
            .withBuff(TowerBuff.critChance(0.1f))
            .withExtraEffect("arcs can crit, +1 jump")
            .after(ARC);
    private static final UpgradeNode OVERLOAD = UpgradeTier.HEAD_3.node("splash.head.arc.3", "Overload", PRICE)
            .withGate(new PurposeCondition(SATURATED_BLAST_DEED, SATURATED_BLASTS_NEEDED))
            .withExtraEffect("an arc crit Dazes its target 0.5s; arcs into a fully Saturated enemy +10% crit")
            .after(CONDUCTOR);
    private static final UpgradeNode CHAIN_LIGHTNING = UpgradeTier.HEAD_4.node("splash.head.arc.4a",
            "Chain Lightning", PRICE)
            .withExtraEffect("up to 6 jumps at the blast's full damage; the 3rd jump forks")
            .after(OVERLOAD);
    private static final UpgradeNode LIGHTNING_ROD = UpgradeTier.HEAD_4.node("splash.head.arc.4b", "Lightning Rod",
            PRICE)
            .withExtraEffect("an arc with nowhere to go returns to the primary at full strength, up to 3 times; arcs "
                    + "on a Dazed enemy deal +50% crit damage")
            .after(OVERLOAD);
    private static final UpgradeNode HEX = UpgradeTier.HEAD_1.node("splash.head.hex.1", "Hex", PRICE)
            .withExtraEffect("casts Hex of Doom");
    private static final UpgradeCondition A_CHAIN = UpgradeCondition.owns(ARC.id()).or(UpgradeCondition.owns(HEX.id()));
    private static final UpgradeNode WIDE_CHARGE = UpgradeTier.EXTRA_1.node("splash.extra.blast_engineering.1",
            "Wide Charge", PRICE)
            .withExtraEffect("+25% blast radius; the blast's edge deals 25% instead of nothing");
    private static final UpgradeNode SHAPED_CHARGE = UpgradeTier.EXTRA_2.node("splash.extra.blast_engineering.2",
            "Shaped Charge", PRICE)
            .withExtraEffect("enemies in the inner half gain 2 Saturation instead of 1; Saturation caps at 4")
            .after(WIDE_CHARGE);
    private static final UpgradeNode POTENCY = UpgradeTier.EXTRA_3.node("splash.extra.blast_engineering.3",
            "Potency", PRICE)
            .withRequires(A_CHAIN)
            .withExtraEffect("Stormcaller: +1 jump, arcs deal 65%")
            .after(SHAPED_CHARGE);
    private static final UpgradeNode MASTERY = UpgradeTier.EXTRA_4.node("splash.extra.blast_engineering.4",
            "Mastery", PRICE)
            .withExtraEffect("Stormcaller: arcs +10% crit, Overload's Daze lasts 1s")
            .after(POTENCY);

    private static final UpgradeNode THUNDERCLAP = UpgradeTier.SPECIAL.node("splash.special.thunderclap",
            "Thunderclap", PRICE)
            .withExtraEffect("after a crit, the next shot also arcs into every enemy in range at 50%, Dazing each one "
                    + "it crits")
            .after(ARC);
    private static final UpgradeNode STATIC_CHARGE = UpgradeTier.SPECIAL.node("splash.special.static_charge",
            "Static Charge", PRICE)
            .withExtraEffect("arcs leave enemies Charged for 3s: another tower's next hit discharges it for +30% of "
                    + "that hit as magic, credited to this tower; a crit discharges at double")
            .after(ARC);
    private static final UpgradeNode THUNDERSTRIKE = UpgradeTier.SPECIAL.node("splash.special.thunderstrike",
            "Thunderstrike", PRICE)
            .withExtraEffect("every 6th shot calls lightning onto the healthiest enemy in range: 4x the blast as magic "
                    + "and Dazed 0.5s; the shot's arcs start there at full damage; counts as a crit")
            .after(ARC);

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE, BASE_PERKS, OVERLOAD))
            .with(ARC, CONDUCTOR, OVERLOAD, CHAIN_LIGHTNING, LIGHTNING_ROD, HEX, WIDE_CHARGE, SHAPED_CHARGE, POTENCY,
                    MASTERY, THUNDERCLAP, STATIC_CHARGE, THUNDERSTRIKE)
            .withChoice(ExclusiveChoice.oneOf(ARC, HEX))
            .withChoice(ExclusiveChoice.oneOf(CHAIN_LIGHTNING, LIGHTNING_ROD))
            .withChoice(ExclusiveChoice.specials(THUNDERCLAP, STATIC_CHARGE, THUNDERSTRIKE));

    private static final PerkCatalogue<SplashPerk> PERKS = PerkCatalogue.<SplashPerk>empty()
            .with(StandardBaseSlot.ATTUNE_ID, FireControlPerk::new)
            .with(StandardBaseSlot.RANGE_3_ID, () -> new BlastRadiusPerk(0.1f))
            .with(ARC.id(), ArcPerk::new)
            .with(CONDUCTOR.id(), ConductorPerk::new)
            .with(OVERLOAD.id(), OverloadPerk::new)
            .with(CHAIN_LIGHTNING.id(), ChainLightningPerk::new)
            .with(LIGHTNING_ROD.id(), LightningRodPerk::new)
            .with(WIDE_CHARGE.id(), WideChargePerk::new)
            .with(SHAPED_CHARGE.id(), ShapedChargePerk::new)
            .with(POTENCY.id(), PotencyPerk::new)
            .with(MASTERY.id(), MasteryPerk::new)
            .with(THUNDERCLAP.id(), ThunderclapPerk::new)
            .with(STATIC_CHARGE.id(), StaticChargePerk::new)
            .with(THUNDERSTRIKE.id(), ThunderstrikePerk::new);

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private final OwnedPerks<SplashPerk> perks = new OwnedPerks<>(PERKS);
    private final TargetSelector randomAim;
    private final SplashActions actions = new Actions();
    private int coolDown = 0;
    private List<Blast> blasts = List.of();
    private List<ArcTrace> arcs = List.of();
    private int lastShotTick;
    private int currentTick;
    private int shotsFired;

    public SplashTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SPLASH, new TowerBaseStats(DAMAGE_POINTS, RANGE, COOLDOWN_MAX), context, x, y);
        this.randomAim = PreferringSelector.priority(new RandomSelector(context.random()));
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        this.perks.add(node);
    }

    /** What its blast is like and what it carries, as the perks in {@code owned} make it. */
    private SplashSpec spec(List<SplashPerk> owned) {
        Viewpoint view = new Viewpoint(this.centerX, this.centerY, this.rangeReal(), this.context.getBoard().scale());
        SplashSpec spec = SplashSpec.from(view);
        for (SplashPerk perk : owned) {
            spec = perk.refineSpec(spec);
        }
        return spec;
    }

    private float blastRadius(BlastSpec blast) {
        return BLAST_RADIUS_CELLS * this.context.getBoard().scale() * blast.distanceScale();
    }

    public void doTick(int gameTime) {
        this.currentTick = gameTime;
        if (this.coolDown > 0) {
            this.coolDown--;
        } else {
            List<SplashPerk> owned = this.perks.all();
            SplashSpec spec = this.spec(owned);
            Optional<EnemyMob> primary = this.aimFor(spec).selectFrom(spec.reach().matching(this.context.enemies()));
            if (primary.isEmpty()) {
                this.blasts = List.of();
            } else {
                this.fire(primary.get(), spec, owned);
                this.coolDown = this.coolDownCurrent();
            }
        }
        if (!this.blasts.isEmpty()) {
            EnemyMob aimed = this.blasts.getFirst().primary();
            this.turretAim.tick(TurretAim.angleTo(this.centerX, this.centerY, aimed.getX(), aimed.getY()));
        }
    }

    private TargetSelector aimFor(SplashSpec spec) {
        if (!spec.blast().aimsAtCrowds()) {
            return this.randomAim;
        }
        return PreferringSelector.priority(new MostNeighboursSelector(this.blastRadius(spec.blast())));
    }

    /**
     * One shot, as the perks shape it: a Thunderstrike first if one is due, then the blast, the arcs
     * it carries, and a Thunderclap's discharge if one is armed. The perks then react to how it
     * landed.
     */
    private void fire(EnemyMob primary, SplashSpec spec, List<SplashPerk> owned) {
        this.lastShotTick = this.currentTick;
        this.shotsFired++;
        SplashShot shot = SplashShot.plain();
        ShotContext context = new ShotContext(this.shotsFired);
        for (SplashPerk perk : owned) {
            shot = perk.shape(shot, context);
        }
        if (spec.blast().saturation().isActive() && primary.hasEffect(EffectKind.SATURATED)) {
            this.countDeedOfAttack();
        }
        List<ArcTrace> traces = new ArrayList<>();
        Optional<EnemyMob> struck = shot.thunderstrike() ? this.thunderstrike(spec, traces) : Optional.empty();
        Blast blast = this.blast(primary, spec.blast());
        this.blasts = List.of(blast);
        List<EnemyMob> arced = new ArrayList<>();
        boolean critical = struck.isPresent();
        if (spec.arcs().active()) {
            EnemyMob start = struck.orElseGet(() -> this.outermost(blast));
            ArcSpec arcs = struck.isPresent() ? spec.arcs().withShareAtLeast(1f) : spec.arcs();
            critical |= this.arc(blast, start, spec.withArcs(arcs), traces, arced);
        }
        if (shot.thunderclap()) {
            this.discharge(spec, traces, arced);
        }
        this.arcs = List.copyOf(traces);
        ShotResult result = new ShotResult(critical, arced);
        for (SplashPerk perk : owned) {
            perk.react(result, this.actions);
        }
    }

    /**
     * Hits the primary first, then everything else within the blast, each for its share of the
     * damage at its distance, harder the more Saturated it is; then Saturates them all.
     */
    private Blast blast(EnemyMob primary, BlastSpec spec) {
        int x = (int) primary.getX();
        int y = (int) primary.getY();
        float radius = this.blastRadius(spec);
        List<EnemyMob> caught = new ArrayList<>(InRangeTargetQuery.everyone(x, y, radius).matching(this.context.enemies()));
        caught.remove(primary);
        caught.addFirst(primary);
        SaturationRule saturation = spec.saturation();
        boolean critical = false;
        for (EnemyMob enemy : caught) {
            float distanceShare = (float) (Math.hypot(enemy.getX() - x, enemy.getY() - y) / radius);
            float saturated = saturation.isActive()
                    ? 1f + SATURATION_DAMAGE_PER_STACK * enemy.effectStacks(EffectKind.SATURATED) : 1f;
            int amount = Math.round(this.damageCurrent() * spec.damageShareAt(distanceShare) * saturated);
            boolean crit = this.dealDamage(enemy, Damage.physical(amount));
            critical |= enemy == primary && crit;
            if (saturation.isActive() && !enemy.isDead()) {
                int stacks = saturation.stacksAt(distanceShare);
                int ticks = Math.round(SATURATION_SECONDS * TICKS_PER_SECOND);
                this.applyEffect(enemy, sink -> Effect.saturated(stacks, ticks, sink).withStackCap(saturation.cap()));
            }
        }
        return new Blast(primary, List.copyOf(caught), new Blast.Area(x, y, radius), critical);
    }

    /** The enemy the blast caught furthest from its centre, where its arcs start. */
    private EnemyMob outermost(Blast blast) {
        return blast.caught().stream()
                .max(Comparator.comparingDouble(enemy -> Math.hypot(enemy.getX() - blast.area().centerX(),
                        enemy.getY() - blast.area().centerY())))
                .orElse(blast.primary());
    }

    /**
     * The arcs the blast carries, from {@code start}; each lands as a magic hit. Records where they
     * ran and whom they struck, and tells whether one crit.
     */
    private boolean arc(Blast blast, EnemyMob start, SplashSpec spec, List<ArcTrace> traces, List<EnemyMob> arced) {
        ArcSpec arcs = spec.arcs();
        float cellReach = this.context.getBoard().scale() * spec.blast().distanceScale();
        float bound = blast.area().radius()
                + arcs.jumps() * ArcSpec.reachCells(spec.blast().saturation().cap()) * cellReach;
        List<EnemyMob> candidates = InRangeTargetQuery.everyone(blast.area().centerX(), blast.area().centerY(), bound)
                .matching(this.context.enemies());
        Set<EnemyMob> struck = Collections.newSetFromMap(new IdentityHashMap<>());
        struck.addAll(blast.caught());
        List<ArcStrike> strikes = ArcPlanner.plan(blast.primary(), start, candidates, struck, arcs,
                from -> ArcSpec.reachCells(from.effectStacks(EffectKind.SATURATED)) * cellReach);
        boolean critical = false;
        for (ArcStrike strike : strikes) {
            traces.add(new ArcTrace((float) strike.from().getX(), (float) strike.from().getY(),
                    (float) strike.to().getX(), (float) strike.to().getY()));
            critical |= this.strikeWithArc(strike.to(), strike.share(), spec, arcs.daze());
            arced.add(strike.to());
        }
        return critical;
    }

    /** Thunderclap: an arc from the tower into every enemy in range; each one it crits is Dazed. */
    private void discharge(SplashSpec spec, List<ArcTrace> traces, List<EnemyMob> arced) {
        for (EnemyMob enemy : spec.reach().matching(this.context.enemies())) {
            traces.add(new ArcTrace(this.centerX, this.centerY, (float) enemy.getX(), (float) enemy.getY()));
            this.strikeWithArc(enemy, THUNDERCLAP_SHARE, spec, DAZE_SECONDS);
            arced.add(enemy);
        }
    }

    /**
     * Thunderstrike: lightning from above onto the healthiest enemy in range, the Priority first,
     * for several times the blast as magic, and a Daze. The enemy it struck, if any.
     */
    private Optional<EnemyMob> thunderstrike(SplashSpec spec, List<ArcTrace> traces) {
        Optional<EnemyMob> target = THUNDERSTRIKE_AIM.selectFrom(spec.reach().matching(this.context.enemies()));
        target.ifPresent(enemy -> {
            float x = (float) enemy.getX();
            float y = (float) enemy.getY();
            traces.add(new ArcTrace(x, y - THUNDERSTRIKE_HEIGHT_CELLS * this.context.getBoard().scale(), x, y));
            this.dealDamage(enemy, Damage.magic(Math.round(this.damageCurrent() * THUNDERSTRIKE_FACTOR)));
            if (!enemy.isDead()) {
                int ticks = Math.round(DAZE_SECONDS * TICKS_PER_SECOND);
                this.applyEffect(enemy, sink -> Effect.dazed(ticks, sink));
            }
        });
        return target;
    }

    /** One arc's hit on {@code target}: magic, with the arc spec's crit rules; a crit Dazes for {@code dazeSeconds}. */
    private boolean strikeWithArc(EnemyMob target, float share, SplashSpec spec, float dazeSeconds) {
        ArcSpec arcs = spec.arcs();
        AttackProfile attack = this.stats().attack();
        if (!arcs.canCrit()) {
            attack = attack.withCritChance(0f);
        } else {
            SaturationRule saturation = spec.blast().saturation();
            boolean fullySaturated = saturation.isActive()
                    && target.effectStacks(EffectKind.SATURATED) >= saturation.cap();
            float bonus = arcs.critBonus() + (fullySaturated ? arcs.saturatedCritBonus() : 0f);
            attack = attack.withCritChance(Math.min(1f, attack.critChance() + bonus));
        }
        if (target.isStopped()) {
            attack = attack.withCritDamageBonus(arcs.stoppedCritDamage());
        }
        boolean critical = this.dealDamage(target, Damage.magic(Math.round(this.damageCurrent() * share)), attack);
        if (critical && dazeSeconds > 0f && !target.isDead()) {
            int ticks = Math.round(dazeSeconds * TICKS_PER_SECOND);
            this.applyEffect(target, sink -> Effect.dazed(ticks, sink));
        }
        return critical;
    }

    /** What the fork has made it: the Stormcaller on Arc, the Hexer on Hex. */
    public Fork fork() {
        if (this.upgrades().owns(ARC.id())) {
            return Fork.STORMCALLER;
        }
        return this.upgrades().owns(HEX.id()) ? Fork.HEXER : Fork.NONE;
    }

    @Override
    public String displayName() {
        return switch (this.fork()) {
            case NONE -> super.displayName();
            case STORMCALLER -> "Stormcaller";
            case HEXER -> "Hexer";
        };
    }

    /** The blast's target, which the turret follows; {@code null} when the last look found none. */
    public EnemyMob getPrimaryTarget() {
        return this.blasts.isEmpty() ? null : this.blasts.getFirst().primary();
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    /** The blasts of the latest shot. */
    public List<Blast> getBlasts() {
        return this.blasts;
    }

    /** The arcs of the latest shot, a Thunderstrike's bolt among them, in the order they struck. */
    public List<ArcTrace> getArcs() {
        return this.arcs;
    }

    /** How many ticks ago it last fired. */
    public int ticksSinceShot(int gameTime) {
        return gameTime - this.lastShotTick;
    }

    public float getCoolDownFraction() {
        return (float) this.coolDown / this.coolDownCurrent();
    }

    @Override
    protected List<TowerStatLine> ownStats() {
        BlastSpec blast = this.spec(this.perks.all()).blast();
        return List.of(new TowerStatLine(TowerStat.SPLASH_RADIUS, BLAST_RADIUS_CELLS,
                BLAST_RADIUS_CELLS * blast.distanceScale()));
    }

    @Override
    protected List<BehaviourLine> behaviours() {
        SplashSpec spec = this.spec(this.perks.all());
        List<BehaviourLine> lines = new ArrayList<>();
        lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Targets",
                spec.blast().aimsAtCrowds() ? "most crowded" : "random"));
        if (spec.blast().saturation().isActive()) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Saturates", "up to x" + spec.blast().saturation().cap()));
        }
        if (spec.arcs().active()) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Arcs", spec.arcs().jumps() + " jumps"));
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

    /** What the perks may make this tower do. */
    private final class Actions implements SplashActions {

        @Override
        public void charge(EnemyMob target) {
            SplashTower.this.charge(target, Math.round(STATIC_CHARGE_SECONDS * TICKS_PER_SECOND));
        }
    }

    /** What the fork has made the Splash. */
    public enum Fork {
        NONE,
        STORMCALLER,
        HEXER
    }

    /**
     * One blast: the enemy it centred on, everything it caught (the primary first), and where and
     * how wide it was.
     *
     * @param critical whether the primary hit was a critical hit
     */
    public record Blast(EnemyMob primary, List<EnemyMob> caught, Area area, boolean critical) {

        /** The disc the blast covered, in board pixels. */
        public record Area(int centerX, int centerY, float radius) {
        }
    }

    /** Where one arc ran, in board pixels. */
    public record ArcTrace(float fromX, float fromY, float toX, float toY) {
    }
}
