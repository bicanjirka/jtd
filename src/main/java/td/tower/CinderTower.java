package td.tower;

import td.damage.Damage;
import td.damage.DamageType;
import td.damage.DamageUnits;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.stat.StatPart;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.cinder.Caught;
import td.tower.cinder.CinderMechanic;
import td.tower.cinder.CinderSpec;
import td.tower.cinder.CoreMechanic;
import td.tower.cinder.DragonsBreathMechanic;
import td.tower.cinder.FlameLook;
import td.tower.cinder.Flash;
import td.tower.cinder.FlashoverMechanic;
import td.tower.cinder.KindlingMechanic;
import td.tower.cinder.MeltdownMechanic;
import td.tower.cinder.SoulfireMechanic;
import td.tower.cinder.StreamLook;
import td.tower.cinder.ThermalShockMechanic;
import td.tower.cinder.WhiteFlameMechanic;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.InWedgeTargetQuery;
import td.tower.targeting.NearestSelector;
import td.tower.targeting.PreferringSelector;
import td.tower.targeting.TargetSelector;
import td.tower.upgrade.MechanicCatalogue;
import td.tower.upgrade.OwnedMechanics;
import td.tower.upgrade.RangeNodes;
import td.tower.upgrade.UpgradeCluster;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;

import static td.tower.upgrade.MechanicCatalogue.entry;

/**
 * The Scorcher: a flamethrower that streams, then reloads. While it streams, every enemy in its cone takes Flame
 * every tick as fuel for its own burn, hidden ones included; the burn knows nothing of the tower and deals all its
 * fuel. It aims at the nearest visible enemy and keeps it the whole stream while it lives and stays in range. It never
 * deals a hit: its burns credit it as periodic damage, and nothing it pours crits.
 * <p>
 * What each owned node does lives in its data or in a {@link CinderMechanic}: the tower reads one spec from them every
 * tick.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class CinderTower extends AbstractTower {

    public static final int PRICE = 20;
    /** Fuel a second, in points. */
    public static final float FLAME_POINTS = 60f;
    public static final float RANGE = 2.2f;

    private static final float CONE_DEGREES = 60f;
    private static final int STREAM_TICKS = Math.round(TickRate.TICKS_PER_SECOND);
    private static final int RELOAD_TICKS = Math.round(2 * TickRate.TICKS_PER_SECOND);
    /** The reload is paid on the fire clock in periods of this many ticks, so attack speed shortens it alone. */
    private static final int RELOAD_PERIOD_TICKS = Math.round(TickRate.TICKS_PER_SECOND);
    private static final int SEAR_STACKS = 5;
    /** The stream's Singe outlasts its last touch by this, so an enemy at the cone's edge keeps it as the head turns. */
    private static final int SINGE_LINGER_TICKS = Math.round(TickRate.TICKS_PER_SECOND / 2f);
    private static final double MAX_TURN_RADIANS_PER_TICK = 0.3;

    private static final UpgradeNode HOT_FLAME = UpgradeNode.of("cinder.hot_flame", "Hot Flame", 13)
            .withModifiers(TowerModifiers.damage(15f));
    private static final UpgradeNode LONG_STREAM = UpgradeNode.of("cinder.long_stream", "Long Stream", 17)
            .withModifiers(TowerModifiers.stream(Math.round(0.5f * TickRate.TICKS_PER_SECOND)));
    private static final UpgradeNode WIDE_NOZZLE = UpgradeNode.of("cinder.wide_nozzle", "Wide Nozzle", 6)
            .withModifiers(TowerModifiers.coneTimes(1.5f));
    private static final UpgradeNode LONG_NOZZLE = UpgradeNode.of(RangeNodes.RANGE_ID, "Long Nozzle", 7)
            .withModifiers(TowerModifiers.range(0.5f));

    private static final UpgradeNode CORE = UpgradeNode.of("cinder.core", "Core", 38)
            .inTier(1);
    private static final UpgradeNode LANCE = UpgradeNode.of(RangeNodes.RANGE_2_ID, "Lance", 9)
            .withModifiers(TowerModifiers.range(0.65f).plus(TowerModifiers.coneTimes(0.6f)))
            .inTier(1);
    private static final UpgradeNode FAN = UpgradeNode.of("cinder.fan", "Fan", 9)
            .withModifiers(TowerModifiers.coneTimes(1.4f))
            .inTier(1);
    private static final UpgradeNode KINDLING = UpgradeNode.of("cinder.kindling", "Kindling", 1)
            .inTier(1);

    private static final UpgradeNode SOULFIRE = UpgradeNode.of("cinder.soulfire", "Soulfire", 23)
            .withModifiers(TowerModifiers.damagePercent(0.1f).plus(TowerModifiers.magicPenetration(0.5f)))
            .inTier(2);
    private static final UpgradeNode DRAGONS_BREATH = UpgradeNode.of("cinder.dragons_breath", "Dragon's Breath", 23)
            .withModifiers(TowerModifiers.streamTimes(2f).plus(TowerModifiers.damageTimes(0.65f))
                    .plus(TowerModifiers.reload(-Math.round(TickRate.TICKS_PER_SECOND))))
            .inTier(2);
    private static final UpgradeNode WHITE_FLAME = UpgradeNode.of("cinder.white_flame", "White Flame", 59)
            .withModifiers(TowerModifiers.damage(30f))
            .inTier(2);
    private static final UpgradeNode BELLOWS = UpgradeNode.of("cinder.bellows", "Bellows", 32)
            .withModifiers(TowerModifiers.reload(-Math.round(0.5f * TickRate.TICKS_PER_SECOND)))
            .inTier(2);

    private static final UpgradeNode FLASHOVER = UpgradeNode.of("cinder.flashover", "Flashover", 66)
            .inTier(3);
    private static final UpgradeNode MELTDOWN = UpgradeNode.of("cinder.meltdown", "Meltdown", 66)
            .inTier(3);
    private static final UpgradeNode THERMAL_SHOCK = UpgradeNode.of("cinder.thermal_shock", "Thermal Shock", 66)
            .inTier(3);
    private static final UpgradeNode HOT_FLAME_2 = UpgradeNode.of("cinder.hot_flame_2", "Hot Flame II", 43)
            .withModifiers(TowerModifiers.damage(15f))
            .inTier(3);
    private static final UpgradeNode LONG_STREAM_2 = UpgradeNode.of("cinder.long_stream_2", "Long Stream II", 107)
            .withModifiers(TowerModifiers.stream(Math.round(TickRate.TICKS_PER_SECOND)))
            .inTier(3);

    private static final UpgradeTree TREE = UpgradeTree.of(List.of(HOT_FLAME, LONG_STREAM, WIDE_NOZZLE, LONG_NOZZLE))
            .with(CORE, LANCE, FAN, KINDLING,
                    SOULFIRE, DRAGONS_BREATH, WHITE_FLAME, BELLOWS,
                    FLASHOVER, MELTDOWN, THERMAL_SHOCK, HOT_FLAME_2, LONG_STREAM_2)
            .withCluster(UpgradeCluster.of("Nozzle", LANCE, FAN))
            .withCluster(UpgradeCluster.of("Flame", SOULFIRE, DRAGONS_BREATH))
            .withCluster(UpgradeCluster.of("Burning", FLASHOVER, MELTDOWN, THERMAL_SHOCK));

    private static final MechanicCatalogue<CinderMechanic> MECHANICS = MechanicCatalogue.of(
            entry(CORE.id(), CoreMechanic::new),
            entry(KINDLING.id(), KindlingMechanic::new),
            entry(SOULFIRE.id(), SoulfireMechanic::new),
            entry(DRAGONS_BREATH.id(), DragonsBreathMechanic::new),
            entry(WHITE_FLAME.id(), WhiteFlameMechanic::new),
            entry(FLASHOVER.id(), FlashoverMechanic::new),
            entry(MELTDOWN.id(), MeltdownMechanic::new),
            entry(THERMAL_SHOCK.id(), ThermalShockMechanic::new));

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK, this.restHeading());
    private final OwnedMechanics<CinderMechanic> mechanics = new OwnedMechanics<>(MECHANICS);
    private final TargetSelector nearest = PreferringSelector.priority(new NearestSelector(this.centerX, this.centerY));
    /** The enemies this stream has touched, which Sear has already scorched; it only answers membership. */
    private final Set<EnemyMob> touched = new HashSet<>();
    /** Each hot enemy that flashed over this tick and an enemy it reached, in pairs, for the look. */
    private final List<Flash> flashes = new ArrayList<>();
    /** Each enemy the cone fed this tick and what it was fed, for the look. */
    private final List<Caught> caught = new ArrayList<>();
    private FireClock clock = new FireClock(RELOAD_PERIOD_TICKS - 1);
    private TowerStats streamStats = this.stats();
    private int streamLeft;
    private OptionalInt streamEndedAt = OptionalInt.empty();
    private Optional<EnemyMob> target = Optional.empty();

    public CinderTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.CINDER, new TowerBaseStats(FLAME_POINTS, RANGE, 0)
                .with(TowerStat.CONE_WIDTH, (float) Math.toRadians(CONE_DEGREES))
                .with(TowerStat.STREAM, STREAM_TICKS).with(TowerStat.RELOAD, RELOAD_TICKS), context, x, y);
    }

    @Override
    protected OwnedMechanics<CinderMechanic> ownedMechanics() {
        return this.mechanics;
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    /** Whom it aims at and what it pours, as the mechanics in {@code owned} make it. */
    private CinderSpec spec(List<CinderMechanic> owned) {
        CinderSpec spec = CinderSpec.from(this.viewpoint(), SEAR_STACKS);
        for (CinderMechanic mechanic : owned) {
            spec = mechanic.refineSpec(spec);
        }
        return spec;
    }

    @Override
    protected void act(int gameTime) {
        CinderSpec spec = this.spec(this.mechanics.all());
        List<EnemyMob> inRange = spec.reach().matching(this.context.enemies());
        this.flashes.clear();
        this.caught.clear();
        if (this.streamLeft == 0 && this.clock.isReady() && !inRange.isEmpty()) {
            this.streamStats = this.beginShot();
            this.streamLeft = Math.max(1, Math.round(this.streamStats.value(TowerStat.STREAM)));
            this.touched.clear();
            this.target = Optional.empty();
        }
        if (this.streamLeft > 0 && this.target.filter(inRange::contains).isEmpty()) {
            this.target = this.nearest.selectFrom(inRange);
        }
        this.turnTurret(this.turretAim, this.streamLeft > 0 ? this.target : this.nearest.selectFrom(inRange));
        if (this.streamLeft > 0) {
            this.pour(spec);
            this.streamLeft--;
            if (this.streamLeft == 0) {
                this.streamEndedAt = OptionalInt.of(gameTime);
                this.clock.spend(this.streamStats.value(TowerStat.RELOAD) / RELOAD_PERIOD_TICKS);
                this.target = Optional.empty();
            }
        } else {
            this.clock.advance(this.attackRate());
        }
    }

    /**
     * One tick of the stream: Flame and this Scorcher's share of Singe into every enemy in the cone, more Flame in the
     * Core, Sear on the first touch of each enemy, then a hot burn's Flashover onto the enemies beside it.
     */
    private void pour(CinderSpec spec) {
        double heading = this.turretAim.currentRadians();
        double halfWidth = this.halfWidthRadians();
        float perTick = this.streamStats.damage() / TickRate.TICKS_PER_SECOND;
        float hot = DamageUnits.ofPoints(FlashoverMechanic.HOT_FUEL_POINTS);
        List<EnemyMob> hotOnes = new ArrayList<>();
        for (EnemyMob enemy : new InWedgeTargetQuery(this.centerX, this.centerY, heading, halfWidth)
                .and(InRangeTargetQuery.everyone(this.centerX, this.centerY, this.rangeReal()))
                .matching(this.context.enemies())) {
            boolean inCore = spec.core().isActive() && this.offAxis(enemy, heading) <= halfWidth * spec.core().share();
            float fed = this.feed(enemy, perTick * (inCore ? spec.core().factor() : 1f), spec);
            this.caught.add(new Caught(enemy, DamageUnits.inPoints(fed) * TickRate.TICKS_PER_SECOND, inCore));
            this.applyEffect(enemy, sink -> Effect.singed(SINGE_LINGER_TICKS, sink));
            if (this.touched.add(enemy)) {
                this.applyEffect(enemy, sink -> Effect.scorched(spec.searStacks()));
            }
            if (spec.flashover() && fuelOf(enemy, spec.fire()) >= hot) {
                hotOnes.add(enemy);
            }
        }
        float radius = FlashoverMechanic.REACH_CELLS * this.cellPixels();
        for (EnemyMob source : hotOnes) {
            for (EnemyMob beside : InRangeTargetQuery.around(source, radius).matching(this.context.enemies())) {
                this.feed(beside, perTick * FlashoverMechanic.SHARE, spec);
                this.flashes.add(new Flash(source.position(), beside.position()));
            }
        }
    }

    /** {@code fuel} damage units into {@code enemy}'s pool, as Thermal Shock and Meltdown make it; what it poured. */
    private float feed(EnemyMob enemy, float fuel, CinderSpec spec) {
        boolean held = enemy.hasEffect(EffectKind.CHILL) || enemy.hasEffect(EffectKind.FREEZE)
                || enemy.hasEffect(EffectKind.DAZED);
        float amount = fuel * (held ? spec.thermalShock() : 1f) * (1f + spec.meltdown() * enemy.armorAndPlating());
        Damage poured = Damage.magic(Math.round(amount));
        if (poured.amount() <= 0) {
            return 0f;
        }
        this.applyEffect(enemy, sink -> spec.soulfire() ? Effect.soulfire(poured, sink) : Effect.burn(poured, sink));
        return poured.amount();
    }

    private static float fuelOf(EnemyMob enemy, EffectKind fire) {
        return enemy.activeEffect(fire).map(Effect::fuelLevel).orElse(0f);
    }

    /** How far {@code enemy}'s bearing is from {@code heading}, in radians. */
    private double offAxis(EnemyMob enemy, double heading) {
        double off = Math.abs(this.bearingTo(enemy) - heading) % (2 * Math.PI);
        return off > Math.PI ? 2 * Math.PI - off : off;
    }

    @Override
    protected void endFight() {
        this.clock = new FireClock(RELOAD_PERIOD_TICKS - 1);
        this.streamLeft = 0;
        this.target = Optional.empty();
        this.touched.clear();
        this.flashes.clear();
        this.caught.clear();
    }

    @Override
    protected void takeRestPose() {
        this.turretAim.rest();
    }

    /** Flame, then what one stream gives an enemy; how long it streams and reloads are rows of their own. */
    @Override
    protected List<TowerStatLine> attackRows(TowerStats stats, Function<TowerStat, List<StatPart>> parts) {
        TowerStatLine flame = this.derivedRow(TowerStatRow.FLAME, TowerStat.DAMAGE, stats, parts,
                at -> DamageUnits.inPoints(at.damage()));
        return List.of(flame, dpsLine(TowerStatRow.STREAM_FUEL, streamFuel(this.authoredStats()), streamFuel(stats)));
    }

    /** What a whole stream at {@code stats} gives one enemy outside the Core, in points. */
    private static float streamFuel(TowerStats stats) {
        return DamageUnits.inPoints(stats.damage()) * stats.value(TowerStat.STREAM) / TickRate.TICKS_PER_SECOND;
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    public double halfWidthRadians() {
        return this.stats().value(TowerStat.CONE_WIDTH) / 2;
    }

    /** Whether it is streaming now. */
    public boolean isStreaming() {
        return this.streamLeft > 0;
    }

    /** The fuel its stream pours a second, in points, outside the Core. */
    public float fuelPerSecond() {
        return DamageUnits.inPoints(this.streamStats.damage());
    }

    /** Each enemy the cone fed this tick, in the order it fed them. */
    public List<Caught> caught() {
        return List.copyOf(this.caught);
    }

    /** The ticks since its last stream ended, while they are within {@code window}. */
    public OptionalInt ticksSinceStreamEnded(int gameTime, int window) {
        return within(this.streamEndedAt, gameTime, window);
    }

    /** How much of the reload is left, from 1 as a stream ends down to 0 once it may stream again. */
    public float reloadLeft() {
        return this.isStreaming() ? 0f
                : this.clock.remaining(this.streamStats.value(TowerStat.RELOAD) / RELOAD_PERIOD_TICKS);
    }

    /** What its stream looks like now, as its nodes shape it. */
    public StreamLook streamLook() {
        CinderSpec spec = this.spec(this.mechanics.all());
        return new StreamLook(spec.soulfire(), spec.look() == FlameLook.WHITE, spec.dragonsBreath(),
                spec.core().isActive() ? (float) this.halfWidthRadians() * spec.core().share() : 0f);
    }

    /** This tick's Flashovers, each from a hot enemy to one beside it. */
    public List<Flash> flashes() {
        return List.copyOf(this.flashes);
    }

    @Override
    protected DamageType damageType() {
        return DamageType.MAGIC;
    }

    @Override
    protected List<AppliedEffect> baseApplies() {
        return List.of(new AppliedEffect(EffectKind.BURN, List.of(new AppliedEffect.Source(this.displayName(),
                        TermText.of("its stream pours ").plus(TowerTerm.FLAME).plus(" into ").plus(EffectKind.BURN)))),
                new AppliedEffect(EffectKind.SCORCHED, List.of(new AppliedEffect.Source(this.displayName(),
                        TermText.of("Sear: the first touch of each stream gives " + SEAR_STACKS + " stacks")))),
                new AppliedEffect(EffectKind.SINGED, List.of(new AppliedEffect.Source(this.displayName(),
                        TermText.of("its stream gives its own share while it touches, and "
                                + TermNumbers.seconds(SINGE_LINGER_TICKS / TickRate.TICKS_PER_SECOND) + " after")))));
    }

    @Override
    protected String targetingLabel() {
        return "nearest";
    }

    @Override
    protected String description() {
        return "Streams flame into everything in its cone, then reloads; what it pours goes on hurting.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitCinderTower(this);
    }
}
