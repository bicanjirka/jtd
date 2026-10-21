package td.tower;

import td.damage.Damage;
import td.damage.DamageType;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.enemy.HitResult;
import td.term.TermText;
import td.tower.proc.HitAdjustment;
import td.tower.proc.HitBonus;
import td.tower.proc.HitKind;
import td.tower.proc.HitScaling;
import td.tower.proc.Proc;
import td.tower.proc.ProcCondition;
import td.tower.proc.ProcPayload;
import td.tower.sonar.BeamSpec;
import td.tower.sonar.CommandPingPerk;
import td.tower.sonar.CritRefreshPerk;
import td.tower.sonar.DeepScanPerk;
import td.tower.sonar.FarPingPerk;
import td.tower.sonar.FaultLinePerk;
import td.tower.sonar.MarkOnSweepPerk;
import td.tower.sonar.PhasedArrayPerk;
import td.tower.sonar.PingPerk;
import td.tower.sonar.PingRule;
import td.tower.sonar.PingTracker;
import td.tower.sonar.ResonantCrackPerk;
import td.tower.sonar.Revolution;
import td.tower.sonar.SonarActions;
import td.tower.sonar.SonarBeam;
import td.tower.sonar.SonarPerk;
import td.tower.sonar.SonarSpec;
import td.tower.sonar.SonarStrike;
import td.tower.sonar.SpinningBeam;
import td.tower.sonar.StrikeContext;
import td.tower.sonar.StrikeResult;
import td.tower.sonar.TwinBeamPerk;
import td.tower.sonar.UltrasoundPerk;
import td.tower.sonar.WideBandPerk;
import td.tower.targeting.BeyondRadiusTargetQuery;
import td.tower.targeting.HighestHealthSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.PreferringSelector;
import td.tower.targeting.TargetSelector;
import td.tower.targeting.Viewpoint;
import td.tower.upgrade.BaseSlotPerks;
import td.tower.upgrade.ClusterCondition;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Function;

/**
 * The patient spotter: a beam sweeps the full circle and hits every enemy in range as it passes its
 * bearing, and each revolution pings the biggest threat. No cooldown: fire rate is rotation speed,
 * and enemies at the same bearing are all hit on the same tick.
 * <p>
 * What each owned node does lives in a {@link SonarPerk}: the tower runs every hit and every
 * revolution through them.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class SonarTower extends AbstractTower {

    public static final int PRICE = 20;
    public static final float DAMAGE_POINTS = 16f;
    public static final float RANGE = 4.5f;
    /** Seconds per revolution before Spin-Up; this tower's fire rate. */
    public static final float SECONDS_PER_REVOLUTION = 3f;

    /** How long a hit stays drawn. */
    private static final int HIT_FLASH_TICKS = 8;
    /** How long the ring a revolution sends out stays drawn. */
    private static final int PING_RING_TICKS = 12;
    private static final TermText PING_DEED = TermText.format("times {} fired", TowerTerm.PING);
    private static final int PING_DEEDS_NEEDED = 25;

    private static final BaseSlotPerks BASE_PERKS = BaseSlotPerks.none().withRangeThree(0.1f, NodeEffects.none());

    private static final UpgradeNode RAPID_ARRAY_1 = UpgradeTier.HEAD_1.node("sonar.head.rapid_array.1",
            "Rapid Array", PRICE)
            .withModifiers(TowerModifiers.damage(4f).plus(TowerModifiers.pingCount(1)));
    private static final UpgradeNode RAPID_ARRAY_2 = UpgradeTier.HEAD_2.node("sonar.head.rapid_array.2",
            "Rapid Array II", PRICE)
            .withModifiers(TowerModifiers.damage(4f).plus(TowerModifiers.critChance(0.1f)))
            .after(RAPID_ARRAY_1);
    private static final UpgradeNode SPIN_UP = UpgradeTier.HEAD_3.node("sonar.head.rapid_array.3", "Spin-Up", PRICE)
            .withGate(new PurposeCondition(PING_DEED, PING_DEEDS_NEEDED))
            .withModifiers(TowerModifiers.attackSpeed(0.5f))
            .after(RAPID_ARRAY_2);
    private static final UpgradeNode TWIN_BEAM = UpgradeTier.HEAD_4.node("sonar.head.rapid_array.4a", "Twin Beam",
            PRICE)
            .after(SPIN_UP);
    private static final UpgradeNode PHASED_ARRAY = UpgradeTier.HEAD_4.node("sonar.head.rapid_array.4b",
            "Phased Array", PRICE)
            .after(SPIN_UP);
    private static final UpgradeNode LONG_REACH_1 = UpgradeTier.HEAD_1.node("sonar.head.long_reach.1",
            "Long Reach", PRICE)
            .withModifiers(TowerModifiers.range(0.9f));
    private static final UpgradeNode LONG_REACH_2 = UpgradeTier.HEAD_2.node("sonar.head.long_reach.2",
            "Long Reach II", PRICE)
            .withModifiers(TowerModifiers.critChance(0.1f))
            .withHitBonus(HitBonus.per(new HitScaling.PerRangeShare(), HitAdjustment.damage(1f)))
            .after(LONG_REACH_1);
    private static final UpgradeNode RESONANT_CRACK = UpgradeTier.HEAD_3.node("sonar.head.long_reach.3",
            "Resonant Crack", PRICE)
            .withGate(new PurposeCondition(PING_DEED, PING_DEEDS_NEEDED))
            .after(LONG_REACH_2);
    private static final UpgradeNode HORIZON = UpgradeTier.HEAD_4.node("sonar.head.long_reach.4a", "Horizon", PRICE)
            .withModifiers(TowerModifiers.range(1.8f).plus(TowerModifiers.deadZone(1.5f)))
            .withHitBonus(HitBonus.per(new HitScaling.PerRangeShare(), HitAdjustment.damage(0.5f)))
            .after(RESONANT_CRACK);
    private static final UpgradeNode FAULT_LINE = UpgradeTier.HEAD_4.node("sonar.head.long_reach.4b", "Fault Line",
            PRICE)
            .after(RESONANT_CRACK);
    private static final UpgradeNode ULTRASOUND = UpgradeTier.EXTRA_1.node("sonar.extra.frequency.1", "Ultrasound",
            PRICE);
    private static final UpgradeNode HARMONICS = UpgradeTier.EXTRA_2.node("sonar.extra.frequency.2", "Harmonics",
            PRICE)
            .withProc(Proc.onHit(new ProcPayload.ApplyStacks(EffectKind.RESONATING, 1))
                    .when(new ProcCondition.HitKindIn(Set.of(HitKind.BEAM))))
            .after(ULTRASOUND);
    private static final UpgradeNode PURE_TONE = UpgradeTier.EXTRA_3.node("sonar.extra.frequency.3", "Pure Tone",
            PRICE)
            .withModifiers(TowerModifiers.magicPenetration(0.15f))
            .withCapability(TowerCapability.MAGIC_DAMAGE)
            .after(HARMONICS);
    private static final UpgradeNode SHATTER_TONE = UpgradeTier.EXTRA_4.node("sonar.extra.frequency.4",
            "Shatter Tone", PRICE)
            .withProc(Proc.onHit(new ProcPayload.BreakShield(0.25f))
                    .when(new ProcCondition.TargetHasAnyOf(Set.of(EffectKind.SHIELD))))
            .after(PURE_TONE);
    private static final UpgradeNode MARK_ON_SWEEP = UpgradeTier.SPECIAL.node("sonar.special.mark_on_sweep",
            "Mark on Sweep", PRICE);
    private static final UpgradeNode WIDE_BAND = UpgradeTier.SPECIAL.node("sonar.special.wide_band", "Wide Band", PRICE)
            .withGate(new ClusterCondition(2));
    private static final UpgradeNode COMMAND_PING = UpgradeTier.SPECIAL.node("sonar.special.command_ping",
            "Command Ping", PRICE);

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE, RANGE, BASE_PERKS, SPIN_UP,
            RESONANT_CRACK))
            .with(RAPID_ARRAY_1, RAPID_ARRAY_2, SPIN_UP, TWIN_BEAM, PHASED_ARRAY, LONG_REACH_1, LONG_REACH_2,
                    RESONANT_CRACK, HORIZON, FAULT_LINE, ULTRASOUND, HARMONICS, PURE_TONE, SHATTER_TONE,
                    MARK_ON_SWEEP, WIDE_BAND, COMMAND_PING)
            .withChoice(ExclusiveChoice.oneOf(RAPID_ARRAY_1, LONG_REACH_1))
            .withChoice(ExclusiveChoice.oneOf(TWIN_BEAM, PHASED_ARRAY))
            .withChoice(ExclusiveChoice.oneOf(HORIZON, FAULT_LINE))
            .withChoice(ExclusiveChoice.specials(MARK_ON_SWEEP, WIDE_BAND, COMMAND_PING));

    private static final PerkCatalogue<SonarPerk> PERKS = PerkCatalogue.<SonarPerk>empty()
            .with(StandardBaseSlot.ATTUNE_ID, PingPerk::new)
            .with(StandardBaseSlot.RANGE_3_ID, DeepScanPerk::new)
            .with(RAPID_ARRAY_2.id(), CritRefreshPerk::new)
            .with(TWIN_BEAM.id(), TwinBeamPerk::new)
            .with(PHASED_ARRAY.id(), PhasedArrayPerk::new)
            .with(LONG_REACH_1.id(), FarPingPerk::new)
            .with(RESONANT_CRACK.id(), ResonantCrackPerk::new)
            .with(FAULT_LINE.id(), FaultLinePerk::new)
            .with(ULTRASOUND.id(), UltrasoundPerk::new)
            .with(MARK_ON_SWEEP.id(), MarkOnSweepPerk::new)
            .with(WIDE_BAND.id(), WideBandPerk::new)
            .with(COMMAND_PING.id(), CommandPingPerk::new);

    private static final BeamSpec BASE_BEAM = BeamSpec.spinning(SECONDS_PER_REVOLUTION);
    private static final TargetSelector PHASED_FOCUS = PreferringSelector.priority(new HighestHealthSelector());

    private final OwnedPerks<SonarPerk> perks = new OwnedPerks<>(PERKS);
    private final SonarActions actions = new Actions();
    private final PingTracker pings = new PingTracker();
    private final List<SonarHit> recentHits = new ArrayList<>();
    private SonarBeam beam = SpinningBeam.of(BASE_BEAM);
    private BeamSpec beamSpec = BASE_BEAM;
    private List<EnemyMob> pinged = List.of();
    private int lastRevolutionAt = Integer.MIN_VALUE;

    public SonarTower(GameWorld context, int x, int y) {
        // No cooldown: the cadence is the sweep rate.
        super(TowerFactory.Type.SONAR, new TowerBaseStats(DAMAGE_POINTS, RANGE, 0)
                .with(TowerStat.PING_COUNT, 1f).with(TowerStat.DEAD_ZONE, 0f), context, x, y);
    }

    @Override
    protected PerkCatalogue<SonarPerk> perkCatalogue() {
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

    /** How it scans, as the perks in {@code owned} make it. */
    private SonarSpec spec(List<SonarPerk> owned) {
        Viewpoint view = new Viewpoint(this.centerX, this.centerY, this.rangeReal(), this.context.getBoard().scale());
        SonarSpec spec = SonarSpec.from(view, BASE_BEAM);
        for (SonarPerk perk : owned) {
            spec = perk.refineSpec(spec);
        }
        float deadZone = this.stats().value(TowerStat.DEAD_ZONE);
        if (deadZone > 0f) {
            spec = spec.withReach(spec.reach().withDeadZone(deadZone * view.cellSize()));
        }
        BeamSpec beam = spec.beam();
        return spec.withBeam(beam.withSecondsPerRevolution((float) (beam.secondsPerRevolution() / this.attackRate())));
    }

    private PingRule ping(SonarSpec spec) {
        return spec.ping().isActive()
                ? spec.ping().withCount(Math.round(this.stats().value(TowerStat.PING_COUNT)))
                : spec.ping();
    }

    public void doTick(int gameTime) {
        List<SonarPerk> owned = this.perks.all();
        SonarSpec spec = this.spec(owned);
        this.beamSpec = spec.beam();
        this.beam = this.beam.reshaped(spec.beam());
        List<EnemyMob> inReach = spec.reach().matching(this.context.enemies());
        this.beam.advance(spec.beam().phased() ? this.focusBearing(inReach) : OptionalDouble.empty());
        this.recentHits.removeIf(hit -> gameTime - hit.tick() >= HIT_FLASH_TICKS);

        for (EnemyMob enemy : inReach) {
            float share = this.beam.strikeShare(this.bearingOf(enemy));
            if (share > 0f) {
                this.strike(enemy, share, spec, owned, gameTime);
            }
        }
        if (this.beam.completedRevolution()) {
            this.beginShot();
            this.completeRevolution(spec, owned, gameTime);
        }
    }

    private double bearingOf(EnemyMob enemy) {
        return TurretAim.angleTo(this.centerX, this.centerY, enemy.getX(), enemy.getY());
    }

    /** Where a held beam points: at the enemy with the most health, the Priority first. */
    private OptionalDouble focusBearing(List<EnemyMob> inReach) {
        return PHASED_FOCUS.selectFrom(inReach)
                .map(enemy -> OptionalDouble.of(this.bearingOf(enemy)))
                .orElse(OptionalDouble.empty());
    }

    /**
     * One beam hit, shaped and reacted to by the perks. The magic Ultrasound adds rides on the hit:
     * it is as much stronger as the hit's crit made it, and is never a hit of its own, so it never
     * rolls a crit or spends a mark. The hit is aimed at the enemy, so it may dodge it; the beam still
     * saw a dodging enemy, so it still counts for the ping and the perks still hear of it.
     */
    private void strike(EnemyMob enemy, float share, SonarSpec spec, List<SonarPerk> owned, int gameTime) {
        StrikeContext context = new StrikeContext(enemy, this.rangeShareOf(enemy));
        HitAdjustment adjustment = this.hitAdjustment(enemy, HitKind.BEAM, context.rangeShare(), this.stats());
        SonarStrike strike = SonarStrike.of(adjustment.applyTo(this.stats().attack()), share);
        if (this.stats().has(TowerCapability.MAGIC_DAMAGE)) {
            strike = strike.withType(DamageType.MAGIC);
        }
        for (SonarPerk perk : owned) {
            strike = perk.shape(strike, context);
        }
        float amount = this.damageCurrent() * strike.damageFactor() * adjustment.damageFactor();
        HitResult hit = this.landHit(enemy, Damage.of(strike.type(), Math.round(amount)), strike.attack().asAimed());
        boolean critical = hit.critical();
        if (strike.magicShare() > 0f && !hit.dodged() && !enemy.isDead()) {
            float riding = amount * strike.magicShare() * this.potencyOfHit(enemy, critical);
            this.dealPeriodicDamage(enemy, Damage.magic(Math.round(riding)));
        }
        this.pings.pass(enemy);
        StrikeResult result = new StrikeResult(enemy, critical, hit.dodged(), spec);
        for (SonarPerk perk : owned) {
            perk.react(result, this.actions);
        }
        if (hit.dodged()) {
            this.reportDodge(enemy, HitKind.BEAM);
        } else {
            this.report(enemy, HitKind.BEAM, critical, enemy.isDead());
        }
        this.recentHits.add(new SonarHit((float) enemy.getX(), (float) enemy.getY(), gameTime, strike.type()));
    }

    private float rangeShareOf(EnemyMob enemy) {
        double distance = Math.hypot(enemy.getX() - this.centerX, enemy.getY() - this.centerY);
        return (float) Math.min(1.0, distance / this.rangeReal());
    }

    /** The ping Exposes the healthiest enemies passed, which is the Sonar's deed, then the perks react. */
    private void completeRevolution(SonarSpec spec, List<SonarPerk> owned, int gameTime) {
        List<EnemyMob> pinged = this.pings.pick(this.ping(spec), spec.view());
        this.pinged = pinged;
        this.lastRevolutionAt = gameTime;
        for (EnemyMob enemy : pinged) {
            this.applyEffect(enemy, sink -> Effect.exposed(spec.pingTicks(), sink));
        }
        if (!pinged.isEmpty()) {
            this.countDeedOfAttack();
        }
        Revolution revolution = new Revolution(pinged, spec);
        for (SonarPerk perk : owned) {
            perk.onRevolution(revolution, this.actions);
        }
    }

    /** The damage share of a second beam half a turn opposite the first; {@code 0} for none. */
    public float twinBeamShare() {
        return this.beamSpec.twinShare();
    }

    /** Whether the beam has stopped spinning and holds on one enemy. */
    public boolean holdsBeam() {
        return this.beamSpec.phased();
    }

    /** The enemies the last revolution pinged, healthiest first; some may have died since. */
    public List<EnemyMob> pinged() {
        return this.pinged;
    }

    /** How far through its ring the last revolution's ping is, from 0 to 1; {@code -1} once it has faded. */
    public float pingRingProgress(int gameTime) {
        long age = (long) gameTime - this.lastRevolutionAt;
        return age < PING_RING_TICKS ? age / (float) PING_RING_TICKS : -1f;
    }

    /** The beam's heading between two ticks; the turret head uses it too. */
    public double sweepRadiansAt(double interpolationAlpha) {
        return this.beam.headingAt(interpolationAlpha);
    }

    /** Hits still worth drawing, oldest first. */
    public List<SonarHit> getRecentHits() {
        return List.copyOf(this.recentHits);
    }

    /** From {@code 1} the tick a hit landed down to {@code 0}. */
    public float hitFade(SonarHit hit, int gameTime) {
        int age = gameTime - hit.tick();
        return Math.max(0f, 1f - (float) age / HIT_FLASH_TICKS);
    }

    @Override
    protected Optional<TowerStatLine> cadence() {
        float seconds = this.spec(this.perks.all()).beam().secondsPerRevolution();
        return Optional.of(new TowerStatLine(TowerStatRow.ROTATION, SECONDS_PER_REVOLUTION, seconds));
    }

    @Override
    protected String targetingLabel() {
        return this.spec(this.perks.all()).beam().phased() ? "most health" : "all it sweeps";
    }

    @Override
    protected List<BehaviourLine> behaviours() {
        SonarSpec spec = this.spec(this.perks.all());
        List<BehaviourLine> lines = new ArrayList<>();
        lines.add(spec.beam().phased()
                ? new BehaviourLine(BehaviourMarker.TARGETING, "Holds on", "most health")
                : new BehaviourLine(BehaviourMarker.TARGETING, "Hits", "all it sweeps"));
        if (spec.beam().hasTwinBeam()) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Beams", "2"));
        }
        PingRule ping = this.ping(spec);
        if (ping.isActive()) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Pings", ping.count() == 1
                    ? "the healthiest" : ping.count() + " healthiest"));
        }
        this.perks.all().forEach(perk -> perk.behaviour().ifPresent(lines::add));
        return lines;
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitSonarTower(this);
    }

    /** What the perks may make this tower do. */
    private final class Actions implements SonarActions {

        @Override
        public void apply(EnemyMob target, Function<DamageSink, Effect> effect) {
            SonarTower.this.applyEffect(target, effect);
        }

        @Override
        public void revealHiddenBeyond(float rangeShare, int ticks) {
            SonarTower self = SonarTower.this;
            float range = self.rangeReal();
            InRangeTargetQuery.everyone(self.centerX, self.centerY, range)
                    .and(new BeyondRadiusTargetQuery(self.centerX, self.centerY, rangeShare * range))
                    .matching(self.context.enemies()).stream()
                    .filter(EnemyMob::isHidden)
                    .forEach(enemy -> self.reveal(enemy, ticks));
        }
    }

    /** Where and when the beam caught an enemy, and with what damage. */
    public record SonarHit(float x, float y, int tick, DamageType type) {
    }
}
