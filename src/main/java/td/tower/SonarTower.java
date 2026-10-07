package td.tower;

import td.damage.Damage;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.sonar.BeamSpec;
import td.tower.sonar.CommandPingPerk;
import td.tower.sonar.CritRefreshPerk;
import td.tower.sonar.DeepScanPerk;
import td.tower.sonar.FarDamagePerk;
import td.tower.sonar.FarPingPerk;
import td.tower.sonar.FaultLinePerk;
import td.tower.sonar.HarmonicsPerk;
import td.tower.sonar.HorizonPerk;
import td.tower.sonar.MarkOnSweepPerk;
import td.tower.sonar.PhasedArrayPerk;
import td.tower.sonar.PingPerk;
import td.tower.sonar.PingTracker;
import td.tower.sonar.PureTonePerk;
import td.tower.sonar.ResonantCrackPerk;
import td.tower.sonar.Revolution;
import td.tower.sonar.ShatterTonePerk;
import td.tower.sonar.SonarActions;
import td.tower.sonar.SonarBeam;
import td.tower.sonar.SonarPerk;
import td.tower.sonar.SonarSpec;
import td.tower.sonar.SonarStrike;
import td.tower.sonar.SpinUpPerk;
import td.tower.sonar.SpinningBeam;
import td.tower.sonar.StrikeContext;
import td.tower.sonar.StrikeResult;
import td.tower.sonar.TwinBeamPerk;
import td.tower.sonar.TwoPingsPerk;
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
    private static final String PING_DEED = "Pings";
    private static final int PING_DEEDS_NEEDED = 25;

    private static final BaseSlotPerks BASE_PERKS = BaseSlotPerks.none()
            .withAttune("Ping: each revolution Exposes the healthiest enemy the beam passes, until the next pass")
            .withRangeThree(0.1f, "Deep Scan: each revolution reveals invisible enemies in the outer quarter of its "
                    + "range for 1s");

    private static final UpgradeNode RAPID_ARRAY_1 = UpgradeTier.HEAD_1.node("sonar.head.rapid_array.1",
            "Rapid Array", PRICE)
            .withBuff(TowerBuff.damage(0.25f))
            .withExtraEffect("Ping Exposes the two healthiest enemies it passes");
    private static final UpgradeNode RAPID_ARRAY_2 = UpgradeTier.HEAD_2.node("sonar.head.rapid_array.2",
            "Rapid Array II", PRICE)
            .withBuff(TowerBuff.damage(0.25f).withCritChance(0.1f))
            .withExtraEffect("a beam crit refreshes Exposed on its target")
            .after(RAPID_ARRAY_1);
    private static final UpgradeNode SPIN_UP = UpgradeTier.HEAD_3.node("sonar.head.rapid_array.3", "Spin-Up", PRICE)
            .withGate(new PurposeCondition(PING_DEED, PING_DEEDS_NEEDED))
            .withExtraEffect("a revolution every 2s instead of 3s")
            .after(RAPID_ARRAY_2);
    private static final UpgradeNode TWIN_BEAM = UpgradeTier.HEAD_4.node("sonar.head.rapid_array.4a", "Twin Beam",
            PRICE)
            .withExtraEffect("a second beam, opposite, at 70% damage")
            .after(SPIN_UP);
    private static final UpgradeNode PHASED_ARRAY = UpgradeTier.HEAD_4.node("sonar.head.rapid_array.4b",
            "Phased Array", PRICE)
            .withExtraEffect("stops spinning and holds the beam on the enemy with most health, hitting everything "
                    + "on it three times a revolution")
            .after(SPIN_UP);
    private static final UpgradeNode LONG_REACH_1 = UpgradeTier.HEAD_1.node("sonar.head.long_reach.1",
            "Long Reach", PRICE)
            .withBuff(TowerBuff.range(0.2f))
            .withExtraEffect("Ping picks the healthiest enemy past half range, Exposed for two passes");
    private static final UpgradeNode LONG_REACH_2 = UpgradeTier.HEAD_2.node("sonar.head.long_reach.2",
            "Long Reach II", PRICE)
            .withBuff(TowerBuff.critChance(0.1f))
            .withExtraEffect("damage up to +100% at max range")
            .after(LONG_REACH_1);
    private static final UpgradeNode RESONANT_CRACK = UpgradeTier.HEAD_3.node("sonar.head.long_reach.3",
            "Resonant Crack", PRICE)
            .withGate(new PurposeCondition(PING_DEED, PING_DEEDS_NEEDED))
            .withExtraEffect("each beam hit takes 10 resilience, down to -50, back 10 a second")
            .after(LONG_REACH_2);
    private static final UpgradeNode HORIZON = UpgradeTier.HEAD_4.node("sonar.head.long_reach.4a", "Horizon", PRICE)
            .withBuff(TowerBuff.range(0.4f))
            .withExtraEffect("far bonus up to +150%, but no damage within 1.5 cells")
            .after(RESONANT_CRACK);
    private static final UpgradeNode FAULT_LINE = UpgradeTier.HEAD_4.node("sonar.head.long_reach.4b", "Fault Line",
            PRICE)
            .withExtraEffect("Resonant Crack's loss doesn't recover while Exposed, and falls to -100")
            .after(RESONANT_CRACK);
    private static final UpgradeNode ULTRASOUND = UpgradeTier.EXTRA_1.node("sonar.extra.frequency.1", "Ultrasound",
            PRICE)
            .withExtraEffect("20% of each hit is added as magic, up to 50% against armored or shielded enemies");
    private static final UpgradeNode HARMONICS = UpgradeTier.EXTRA_2.node("sonar.extra.frequency.2", "Harmonics",
            PRICE)
            .withExtraEffect("hits apply Resonating, +8% magic damage taken, stacks x3")
            .after(ULTRASOUND);
    private static final UpgradeNode PURE_TONE = UpgradeTier.EXTRA_3.node("sonar.extra.frequency.3", "Pure Tone",
            PRICE)
            .withExtraEffect("the beam deals magic instead of physical, +15% magic penetration")
            .after(HARMONICS);
    private static final UpgradeNode SHATTER_TONE = UpgradeTier.EXTRA_4.node("sonar.extra.frequency.4",
            "Shatter Tone", PRICE)
            .withExtraEffect("a hit on a shielded enemy breaks a quarter of the shield")
            .after(PURE_TONE);
    private static final UpgradeNode MARK_ON_SWEEP = UpgradeTier.SPECIAL.node("sonar.special.mark_on_sweep",
            "Mark on Sweep", PRICE)
            .withExtraEffect("marks every enemy the beam hits: the next hit on it from any tower is a guaranteed "
                    + "crit");
    private static final UpgradeNode WIDE_BAND = UpgradeTier.SPECIAL.node("sonar.special.wide_band", "Wide Band", PRICE)
            .withGate(new ClusterCondition(2))
            .withExtraEffect("the beam reveals invisible enemies it passes to every tower, Exposed for 1s");
    private static final UpgradeNode COMMAND_PING = UpgradeTier.SPECIAL.node("sonar.special.command_ping",
            "Command Ping", PRICE)
            .withExtraEffect("the pinged enemy is the Priority until the next pass: +15% damage taken, and towers "
                    + "that pick one target pick it");

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE, BASE_PERKS, SPIN_UP,
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
            .with(RAPID_ARRAY_1.id(), TwoPingsPerk::new)
            .with(RAPID_ARRAY_2.id(), CritRefreshPerk::new)
            .with(SPIN_UP.id(), SpinUpPerk::new)
            .with(TWIN_BEAM.id(), TwinBeamPerk::new)
            .with(PHASED_ARRAY.id(), PhasedArrayPerk::new)
            .with(LONG_REACH_1.id(), FarPingPerk::new)
            .with(LONG_REACH_2.id(), () -> new FarDamagePerk(1f))
            .with(RESONANT_CRACK.id(), ResonantCrackPerk::new)
            .with(HORIZON.id(), HorizonPerk::new)
            .with(FAULT_LINE.id(), FaultLinePerk::new)
            .with(ULTRASOUND.id(), UltrasoundPerk::new)
            .with(HARMONICS.id(), HarmonicsPerk::new)
            .with(PURE_TONE.id(), PureTonePerk::new)
            .with(SHATTER_TONE.id(), ShatterTonePerk::new)
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

    public SonarTower(GameWorld context, int x, int y) {
        // No cooldown: the cadence is the sweep rate.
        super(TowerFactory.Type.SONAR, new TowerBaseStats(DAMAGE_POINTS, RANGE, 0), context, x, y);
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
        return spec;
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
            this.completeRevolution(spec, owned);
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
     * rolls a crit or spends a mark.
     */
    private void strike(EnemyMob enemy, float share, SonarSpec spec, List<SonarPerk> owned, int gameTime) {
        StrikeContext context = new StrikeContext(enemy, this.rangeShareOf(enemy));
        SonarStrike strike = SonarStrike.of(this.stats().attack(), share);
        for (SonarPerk perk : owned) {
            strike = perk.shape(strike, context);
        }
        float amount = this.damageCurrent() * strike.damageFactor() * (1f + strike.farBonus() * context.rangeShare());
        boolean critical = this.dealDamage(enemy, Damage.of(strike.type(), Math.round(amount)), strike.attack());
        if (strike.magicShare() > 0f && !enemy.isDead()) {
            float riding = amount * strike.magicShare() * this.potencyOfHit(enemy, critical);
            this.dealPeriodicDamage(enemy, Damage.magic(Math.round(riding)));
        }
        this.pings.pass(enemy);
        StrikeResult result = new StrikeResult(enemy, critical, spec);
        for (SonarPerk perk : owned) {
            perk.react(result, this.actions);
        }
        this.recentHits.add(new SonarHit((float) enemy.getX(), (float) enemy.getY(), gameTime));
    }

    private float rangeShareOf(EnemyMob enemy) {
        double distance = Math.hypot(enemy.getX() - this.centerX, enemy.getY() - this.centerY);
        return (float) Math.min(1.0, distance / this.rangeReal());
    }

    /** The ping Exposes the healthiest enemies passed, which is the Sonar's deed, then the perks react. */
    private void completeRevolution(SonarSpec spec, List<SonarPerk> owned) {
        List<EnemyMob> pinged = this.pings.pick(spec.ping(), spec.view());
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

    /** Whether a second beam sweeps half a turn opposite the first. */
    public boolean hasTwinBeam() {
        return this.beamSpec.hasTwinBeam();
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
        return Optional.of(new TowerStatLine(TowerStat.ROTATION, SECONDS_PER_REVOLUTION, seconds));
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
        if (spec.ping().isActive()) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Pings", spec.ping().count() == 1
                    ? "the healthiest" : spec.ping().count() + " healthiest"));
        }
        if (this.upgrades().owns(WIDE_BAND.id())) {
            lines.add(new BehaviourLine(BehaviourMarker.REVEAL, "Reveals", "hidden it passes"));
        }
        if (this.upgrades().owns(MARK_ON_SWEEP.id())) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Marks", "next hit crits"));
        }
        if (this.upgrades().owns(COMMAND_PING.id())) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Priority", "the pinged enemy"));
        }
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
        public void applyStacks(EnemyMob target, EffectKind kind, int stacks) {
            SonarTower.this.applyStacks(target, kind, stacks);
        }

        @Override
        public void breakShield(EnemyMob target, float fraction) {
            target.breakShield(fraction);
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

    /** Where and when the beam caught an enemy. */
    public record SonarHit(float x, float y, int tick) {
    }
}
