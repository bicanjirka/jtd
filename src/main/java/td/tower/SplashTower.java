package td.tower;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.damage.DamageUnits;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;
import td.enemy.HitResult;
import td.stat.StatModifiers;
import td.tower.ability.AbilityUnits;
import td.tower.proc.HitAdjustment;
import td.tower.proc.HitKind;
import td.tower.splash.AgonyMechanic;
import td.tower.splash.ArcMechanic;
import td.tower.splash.ArcSpec;
import td.tower.splash.BlightCloudMechanic;
import td.tower.splash.BlightHex;
import td.tower.splash.CastActions;
import td.tower.splash.ChainLightningMechanic;
import td.tower.splash.ChokingBlightMechanic;
import td.tower.splash.ConductorMechanic;
import td.tower.splash.ContagionMechanic;
import td.tower.splash.DeepDoomMechanic;
import td.tower.splash.DoomHex;
import td.tower.splash.FeedbackMechanic;
import td.tower.splash.Hex;
import td.tower.splash.HexCaster;
import td.tower.splash.HexHand;
import td.tower.splash.HexJump;
import td.tower.splash.HexKeeper;
import td.tower.splash.HexMechanic;
import td.tower.splash.HexSpec;
import td.tower.splash.HighVoltageMechanic;
import td.tower.splash.InversionHex;
import td.tower.splash.Passive;
import td.tower.splash.PassivePayout;
import td.tower.splash.ShortCircuitMechanic;
import td.tower.splash.SoulHarvestMechanic;
import td.tower.splash.SoulbrandMechanic;
import td.tower.splash.SplashMechanic;
import td.tower.splash.SplashSpec;
import td.tower.splash.StaticMechanic;
import td.tower.splash.TagLedger;
import td.tower.splash.ThunderclapMechanic;
import td.tower.splash.ThunderstrikeMechanic;
import td.tower.splash.TwinHexMechanic;
import td.tower.splash.WildArcMechanic;
import td.tower.targeting.HighestHealthSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.NearestSelector;
import td.tower.targeting.PreferringSelector;
import td.tower.targeting.RandomSelector;
import td.tower.targeting.TargetSelector;
import td.tower.targeting.Viewpoint;
import td.tower.upgrade.MechanicCatalogue;
import td.tower.upgrade.OwnedMechanics;
import td.tower.upgrade.RangeNodes;
import td.tower.upgrade.UpgradeCluster;
import td.tower.upgrade.UpgradeCondition;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.wave.Vec2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static td.tower.upgrade.MechanicCatalogue.entry;

/**
 * The cheap area tower: it throws a stone at a random visible enemy in range, at where the enemy stands, and the stone
 * bursts where it lands, at full damage in its core and less at its edge, hidden enemies included. Forked, it earns a
 * currency by fighting and spends it on abilities: Sparks for the Stormcaller's lightning, Souls for the Hexer's hexes.
 * <p>
 * What each owned node does lives in its data or in a {@link SplashMechanic}: the tower reads one spec from them and
 * lets the ones that bring an ability cast it.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class SplashTower extends AbstractTower {

    public static final int PRICE = 15;
    public static final float DAMAGE_POINTS = 14f;
    public static final float RANGE = 2.5f;
    public static final float CORE_CELLS = 0.75f;
    public static final float EDGE_CELLS = 1.25f;
    /** What the edge of a blast deals, a share of its core's damage. */
    public static final float EDGE_SHARE = 0.5f;

    /** A throw a second. */
    private static final int COOLDOWN_MAX = 19;
    /** How far a stone flies over the ground in a tick, in cells: a walking enemy is still in the core when it lands. */
    private static final float STONE_CELLS_PER_TICK = 0.4f;
    private static final float WISP_CELLS_PER_TICK = 0.3f;
    private static final float CURSE_CELLS_PER_TICK = 0.35f;
    private static final int MIN_FLIGHT_TICKS = 2;
    /** The pause between two jumps of an Arc, so each reads on its own. */
    private static final int HOP_TICKS = 4;
    /** How long a landed blast, a jump's bolt or a cast stays to be drawn. */
    private static final int TRACE_TICKS = 8;
    /** How far above its target a Thunderstrike's bolt starts, in cells. */
    private static final float THUNDERSTRIKE_HEIGHT_CELLS = 2f;
    private static final TargetSelector THUNDERSTRIKE_AIM = PreferringSelector.priority(new HighestHealthSelector());

    private static final Hex DOOM = new DoomHex();
    private static final Hex BLIGHT = new BlightHex();
    private static final Hex INVERSION = new InversionHex();
    private static final List<Hex> HEXES = List.of(DOOM, BLIGHT, INVERSION);

    private static final UpgradeNode LONG_THROW = UpgradeNode.of(RangeNodes.RANGE_ID, "Long Throw", 8)
            .withModifiers(TowerModifiers.range(0.7f));
    private static final UpgradeNode WIDE_CHARGE = UpgradeNode.of("splash.wide_charge", "Wide Charge", 7)
            .withModifiers(TowerModifiers.areaRadius(0.25f).plus(TowerModifiers.blastEdge(0.5f)));
    private static final UpgradeNode FLINT_STONES = UpgradeNode.of("splash.flint_stones", "Flint Stones", 1)
            .withModifiers(TowerModifiers.armorPenetrationFlat(10f));

    private static final UpgradeNode STORMCALLER = UpgradeNode.of("splash.stormcaller", "Stormcaller", 2)
            .inTier(1);
    private static final UpgradeNode HEXER = UpgradeNode.of("splash.hexer", "Hexer", 2)
            .inTier(1);
    private static final UpgradeNode ARC = UpgradeNode.of("splash.arc", "Arc", 2)
            .after(STORMCALLER)
            .inTier(1);
    private static final UpgradeNode HEX_OF_DOOM = UpgradeNode.of("splash.hex_of_doom", "Hex of Doom", 5)
            .after(HEXER)
            .inTier(1);
    private static final UpgradeNode HEX_OF_BLIGHT = UpgradeNode.of("splash.hex_of_blight", "Hex of Blight", 5)
            .after(HEXER)
            .inTier(1);
    private static final UpgradeCondition A_HEX = UpgradeCondition.owns(HEX_OF_DOOM.id())
            .or(UpgradeCondition.owns(HEX_OF_BLIGHT.id()));

    private static final UpgradeNode DEEP_DOOM = UpgradeNode.of("splash.deep_doom", "Deep Doom", 3)
            .after(HEX_OF_DOOM)
            .inTier(2);
    private static final UpgradeNode BLIGHT_CLOUD = UpgradeNode.of("splash.blight_cloud", "Blight Cloud", 16)
            .after(HEX_OF_BLIGHT)
            .inTier(2);
    private static final UpgradeNode AGONY = UpgradeNode.of("splash.agony", "Agony", 2)
            .withRequires(A_HEX)
            .inTier(2);
    private static final UpgradeNode CONTAGION = UpgradeNode.of("splash.contagion", "Contagion", 1)
            .withRequires(A_HEX)
            .inTier(2);
    private static final UpgradeNode CHAIN_LIGHTNING = UpgradeNode.of("splash.chain_lightning", "Chain Lightning", 3)
            .after(ARC)
            .inTier(2);
    private static final UpgradeNode WILD_ARC = UpgradeNode.of("splash.wild_arc", "Wild Arc", 3)
            .after(ARC)
            .inTier(2);
    private static final UpgradeNode CONDUCTOR = UpgradeNode.of("splash.conductor", "Conductor", 1)
            .after(ARC)
            .inTier(2);
    private static final UpgradeNode SHORT_CIRCUIT = UpgradeNode.of("splash.short_circuit", "Short Circuit", 1)
            .after(ARC)
            .inTier(2);
    private static final UpgradeNode HEAVY_STONES = UpgradeNode.of("splash.heavy_stones", "Heavy Stones", 28)
            .withModifiers(TowerModifiers.damage(7f))
            .inTier(2);
    private static final UpgradeNode LONG_THROW_2 = UpgradeNode.of(RangeNodes.RANGE_2_ID, "Long Throw II", 19)
            .withModifiers(TowerModifiers.range(1f))
            .inTier(2);

    private static final UpgradeNode SOUL_HARVEST = UpgradeNode.of("splash.soul_harvest", "Soul Harvest", 26)
            .after(HEXER)
            .inTier(3);
    private static final UpgradeNode HEX_OF_INVERSION = UpgradeNode.of("splash.hex_of_inversion", "Hex of Inversion", 1)
            .after(HEXER)
            .inTier(3);
    private static final UpgradeNode CHOKING_BLIGHT = UpgradeNode.of("splash.choking_blight", "Choking Blight", 1)
            .after(HEX_OF_BLIGHT)
            .inTier(3);
    private static final UpgradeNode TWIN_HEX = UpgradeNode.of("splash.twin_hex", "Twin Hex", 20)
            .withRequires(A_HEX)
            .inTier(3);
    private static final UpgradeNode THUNDERCLAP = UpgradeNode.of("splash.thunderclap", "Thunderclap", 10)
            .after(STORMCALLER)
            .inTier(3);
    private static final UpgradeNode THUNDERSTRIKE = UpgradeNode.of("splash.thunderstrike", "Thunderstrike", 10)
            .after(STORMCALLER)
            .inTier(3);
    private static final UpgradeNode HIGH_VOLTAGE = UpgradeNode.of("splash.high_voltage", "High Voltage", 7)
            .after(ARC)
            .inTier(3);
    private static final UpgradeNode FEEDBACK = UpgradeNode.of("splash.feedback", "Feedback", 5)
            .after(CONDUCTOR)
            .inTier(3);
    private static final UpgradeNode QUICK_HANDS = UpgradeNode.of("splash.quick_hands", "Quick Hands", 19)
            .withModifiers(TowerModifiers.attackSpeed(1f / 0.85f - 1f))
            .inTier(3);
    private static final UpgradeNode PIERCING_STONES = UpgradeNode.of("splash.piercing_stones", "Piercing Stones", 14)
            .withModifiers(TowerModifiers.armorPenetrationFlat(40f))
            .inTier(3);

    private static final UpgradeTree TREE = UpgradeTree.of(List.of(LONG_THROW, WIDE_CHARGE, FLINT_STONES))
            .with(STORMCALLER, HEXER, ARC, HEX_OF_DOOM, HEX_OF_BLIGHT,
                    DEEP_DOOM, BLIGHT_CLOUD, AGONY, CONTAGION, CHAIN_LIGHTNING, WILD_ARC, CONDUCTOR, SHORT_CIRCUIT,
                    HEAVY_STONES, LONG_THROW_2,
                    SOUL_HARVEST, HEX_OF_INVERSION, CHOKING_BLIGHT, TWIN_HEX, THUNDERCLAP, THUNDERSTRIKE, HIGH_VOLTAGE,
                    FEEDBACK, QUICK_HANDS, PIERCING_STONES)
            .withFork(UpgradeCluster.of("Stormcaller or Hexer", STORMCALLER, HEXER))
            .withCluster(UpgradeCluster.of("Hex", HEX_OF_DOOM, HEX_OF_BLIGHT))
            .withCluster(UpgradeCluster.of("Arc's path", CHAIN_LIGHTNING, WILD_ARC))
            .withCluster(UpgradeCluster.of("Storm", THUNDERCLAP, THUNDERSTRIKE));

    private static final MechanicCatalogue<SplashMechanic> MECHANICS = MechanicCatalogue.of(
            entry(STORMCALLER.id(), StaticMechanic::new),
            entry(HEXER.id(), SoulbrandMechanic::new),
            entry(ARC.id(), ArcMechanic::new),
            entry(HEX_OF_DOOM.id(), () -> new HexMechanic(DOOM, 2)),
            entry(HEX_OF_BLIGHT.id(), () -> new HexMechanic(BLIGHT, 2)),
            entry(DEEP_DOOM.id(), DeepDoomMechanic::new),
            entry(BLIGHT_CLOUD.id(), BlightCloudMechanic::new),
            entry(AGONY.id(), AgonyMechanic::new),
            entry(CONTAGION.id(), ContagionMechanic::new),
            entry(CHAIN_LIGHTNING.id(), ChainLightningMechanic::new),
            entry(WILD_ARC.id(), WildArcMechanic::new),
            entry(CONDUCTOR.id(), ConductorMechanic::new),
            entry(SHORT_CIRCUIT.id(), ShortCircuitMechanic::new),
            entry(SOUL_HARVEST.id(), SoulHarvestMechanic::new),
            entry(HEX_OF_INVERSION.id(), () -> new HexMechanic(INVERSION, 3)),
            entry(CHOKING_BLIGHT.id(), ChokingBlightMechanic::new),
            entry(TWIN_HEX.id(), TwinHexMechanic::new),
            entry(THUNDERCLAP.id(), ThunderclapMechanic::new),
            entry(THUNDERSTRIKE.id(), ThunderstrikeMechanic::new),
            entry(HIGH_VOLTAGE.id(), HighVoltageMechanic::new),
            entry(FEEDBACK.id(), FeedbackMechanic::new));

    private final OwnedMechanics<SplashMechanic> mechanics = new OwnedMechanics<>(MECHANICS);
    private final TargetSelector randomAim;
    private final Hexing hexing = new Hexing();
    private FireClock clock = new FireClock(COOLDOWN_MAX);
    private TagLedger tags = new TagLedger();
    private HexKeeper hexKeeper = new HexKeeper(this.hexing);
    private final List<Stone> stones = new ArrayList<>();
    private final List<PendingHop> hops = new ArrayList<>();
    private final List<PendingCurse> curses = new ArrayList<>();
    private final List<PendingWisp> wisps = new ArrayList<>();
    private final List<Blast> blasts = new ArrayList<>();
    private final List<Bolt> bolts = new ArrayList<>();
    private int tickNow;

    public SplashTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SPLASH, new TowerBaseStats(DAMAGE_POINTS, RANGE, COOLDOWN_MAX)
                .with(TowerStat.AREA_RADIUS, CORE_CELLS).with(TowerStat.BLAST_EDGE, EDGE_CELLS)
                .with(TowerStat.ARMOR_PENETRATION_FLAT, 0f), context, x, y);
        this.randomAim = PreferringSelector.priority(new RandomSelector(context.random()));
    }

    @Override
    protected OwnedMechanics<SplashMechanic> ownedMechanics() {
        return this.mechanics;
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    /** What its fork, Arc and hexes are like, as the mechanics in {@code owned} make them. */
    private SplashSpec spec(List<SplashMechanic> owned) {
        Viewpoint view = new Viewpoint(this.centerX, this.centerY, this.rangeReal(), this.cell());
        SplashSpec spec = SplashSpec.from(view);
        for (SplashMechanic mechanic : owned) {
            spec = mechanic.refineSpec(spec);
        }
        return spec;
    }

    private int cell() {
        return this.context.getBoard().scale();
    }

    public void doTick(int gameTime) {
        this.tickNow = gameTime;
        List<SplashMechanic> owned = this.mechanics.all();
        SplashSpec spec = this.spec(owned);
        this.forgetOldTraces();
        this.settlePassive(spec);
        if (!this.hexKeeper.isEmpty()) {
            this.hexKeeper.doTick(spec.hexes());
        }
        this.landArrived(spec);
        boolean ready = this.clock.isReady();
        boolean casting = owned.stream().anyMatch(m -> m.ability().filter(c -> c.isReady()).isPresent());
        if (ready || casting) {
            List<EnemyMob> inReach = spec.reach().matching(this.context.enemies());
            if (!inReach.isEmpty()) {
                Casting actions = new Casting(spec, inReach);
                this.castReadyAbilities(owned, mechanic -> mechanic.cast(actions));
                if (ready) {
                    this.randomAim.selectFrom(inReach).ifPresent(this::throwAt);
                }
            }
        }
        this.clock.advance(this.attackRate());
    }

    @Override
    protected void endFight() {
        this.clock = new FireClock(COOLDOWN_MAX);
        this.tags = new TagLedger();
        this.hexKeeper = new HexKeeper(this.hexing);
        this.stones.clear();
        this.hops.clear();
        this.curses.clear();
        this.wisps.clear();
        this.blasts.clear();
        this.bolts.clear();
    }

    private void forgetOldTraces() {
        this.blasts.removeIf(blast -> this.tickNow - blast.tick() > TRACE_TICKS);
        this.bolts.removeIf(bolt -> this.tickNow - bolt.tick() > TRACE_TICKS);
    }

    /** Throws a stone at where {@code target} stands: it flies with the stats it was thrown with, charged or not. */
    private void throwAt(EnemyMob target) {
        boolean charged = this.holdsStatus(TowerTerm.ARC);
        TowerStats stats = this.beginShot();
        Vec2 from = new Vec2(this.centerX, this.centerY);
        Vec2 to = new Vec2(target.getX(), target.getY());
        this.stones.add(new Stone(to, stats, charged, new Flight(this.tickNow,
                this.tickNow + this.flightTicks(from, to, STONE_CELLS_PER_TICK))));
        this.clock.spend();
    }

    private int flightTicks(Vec2 from, Vec2 to, float cellsPerTick) {
        double distance = Math.hypot(to.x() - from.x(), to.y() - from.y());
        return Math.max(MIN_FLIGHT_TICKS, (int) Math.ceil(distance / (cellsPerTick * this.cell())));
    }

    /** Lands every stone, jump, curse and soul or spark whose flight ends now. */
    private void landArrived(SplashSpec spec) {
        while (!this.stones.isEmpty() && this.stones.getFirst().flight().landsAt() <= this.tickNow) {
            this.land(this.stones.removeFirst(), spec);
        }
        List<PendingHop> arrivedHops = takeArrived(this.hops, hop -> hop.hop().flight().landsAt());
        arrivedHops.forEach(hop -> this.landHop(hop, spec));
        List<PendingCurse> arrivedCurses = takeArrived(this.curses, curse -> curse.curse().flight().landsAt());
        arrivedCurses.forEach(curse -> this.landCurse(curse, spec));
        List<PendingWisp> arrivedWisps = takeArrived(this.wisps, wisp -> wisp.wisp().flight().landsAt());
        arrivedWisps.forEach(wisp -> this.earnAbilities(wisp.pays()));
    }

    private <T> List<T> takeArrived(List<T> flying, java.util.function.ToIntFunction<T> landsAt) {
        if (flying.isEmpty()) {
            return List.of();
        }
        List<T> arrived = new ArrayList<>();
        flying.removeIf(item -> {
            if (landsAt.applyAsInt(item) <= this.tickNow) {
                arrived.add(item);
                return true;
            }
            return false;
        });
        return arrived;
    }

    /**
     * A stone lands with the stats it was thrown with: everything within its edge takes a hit, the core at full damage;
     * what it damaged carries the passive's stack, and what it dealt earns the currency. A charged stone starts its Arc
     * from there.
     */
    private void land(Stone stone, SplashSpec spec) {
        TowerStats stats = stone.stats();
        float core = stats.value(TowerStat.AREA_RADIUS) * this.cell();
        float edge = Math.max(core, stats.value(TowerStat.BLAST_EDGE) * this.cell());
        Vec2 at = stone.to();
        List<EnemyMob> caught = InRangeTargetQuery.everyone((int) at.x(), (int) at.y(), edge)
                .matching(this.context.enemies());
        float core2 = core * core;
        long credited = 0L;
        for (EnemyMob enemy : caught) {
            double dx = enemy.getX() - at.x();
            double dy = enemy.getY() - at.y();
            float share = dx * dx + dy * dy <= core2 ? 1f : EDGE_SHARE;
            HitAdjustment adjustment = this.hitAdjustment(enemy, HitKind.BLAST, 0f, stats);
            int amount = Math.round(stats.damage() * share * adjustment.damageFactor());
            HitResult hit = this.landHit(enemy, Damage.physical(amount), adjustment.applyTo(stats.attack()));
            if (hit.dodged()) {
                continue;
            }
            this.report(enemy, HitKind.BLAST, hit.critical(), enemy.isDead());
            credited += hit.credited();
            this.tag(enemy, hit, spec);
        }
        if (spec.passive() != Passive.NONE && credited > 0L) {
            this.earnAbilities(AbilityUnits.forDamage((int) Math.min(Integer.MAX_VALUE, credited), spec.damagePerUnit()));
        }
        this.blasts.add(new Blast(at, core, edge, this.tickNow));
        if (stone.charged()) {
            ArcRun run = new ArcRun(stats, spec.arc());
            this.nextJump(at, null, run);
        }
    }

    /** {@code enemy} carries the passive's stack once a hit reached its health. */
    private void tag(EnemyMob enemy, HitResult hit, SplashSpec spec) {
        if (spec.passive() != Passive.NONE && hit.landed().amount() > 0) {
            this.tags.tag(enemy);
        }
    }

    /** Sends the Arc on from {@code from} to the nearest enemy it may strike, if it has a jump left. */
    private void nextJump(Vec2 from, EnemyMob current, ArcRun run) {
        if (run.jumpsLeft() <= 0) {
            return;
        }
        List<EnemyMob> near = InRangeTargetQuery.everyone((int) from.x(), (int) from.y(),
                ArcSpec.REACH_CELLS * this.cell()).matching(this.context.enemies()).stream()
                .filter(enemy -> enemy != current && enemy.validTarget()
                        && (run.spec().revisits() || !run.struck(enemy)))
                .toList();
        Optional<EnemyMob> next = new NearestSelector(from.x(), from.y()).selectFrom(near);
        if (next.isEmpty() && run.spec().revisits() && current != null && !current.isDead()) {
            next = Optional.of(current);
        }
        next.ifPresent(to -> this.hops.add(new PendingHop(new Hop(from, to,
                new Flight(this.tickNow, this.tickNow + HOP_TICKS)), run)));
    }

    /** One jump of an Arc lands as magic, unable to crit unless the Arc can, then the Arc jumps on. */
    private void landHop(PendingHop pending, SplashSpec spec) {
        Hop hop = pending.hop();
        EnemyMob target = hop.to();
        ArcRun run = pending.run();
        if (target.isDead()) {
            return;
        }
        TowerStats stats = run.stats();
        ArcSpec arc = run.spec();
        HitAdjustment adjustment = this.hitAdjustment(target, HitKind.ARC, 0f, stats);
        int amount = Math.round(stats.damage() * arc.share() * adjustment.damageFactor()
                * arc.factorAgainst(target.reductionAgainst(DamageType.PHYSICAL)));
        AttackProfile attack = adjustment.applyTo(stats.attack()).withCritChance(arc.critChance());
        Vec2 at = new Vec2(target.getX(), target.getY());
        HitResult hit = this.landHit(target, Damage.magic(amount), attack);
        run.spend(target);
        if (!hit.dodged()) {
            this.report(target, HitKind.ARC, hit.critical(), target.isDead());
            this.tag(target, hit, spec);
            if (hit.critical()) {
                this.earnAbilities(arc.critRefund());
            }
            this.bolts.add(new Bolt(hop.from(), at, this.tickNow, BoltKind.ARC));
        }
        this.nextJump(at, target, run);
    }

    /** Static discharges what it tagged once badly hurt; Soulbrand bursts what it tagged once killed. */
    private void settlePassive(SplashSpec spec) {
        if (this.tags.isEmpty()) {
            return;
        }
        switch (spec.passive()) {
            case STATIC -> this.tags.take(enemy -> enemy.isDead()
                            || enemy.getHealthFraction() <= StaticMechanic.DISCHARGE_AT).stream()
                    .filter(enemy -> !enemy.isDead())
                    .forEach(this::discharge);
            case SOULBRAND -> this.tags.take(EnemyMob::isDead).stream()
                    .filter(EnemyMob::wasKilled)
                    .forEach(this::burstSoul);
            case NONE -> this.tags.take(enemy -> true);
        }
    }

    /** Static: this tower's stack discharges into the enemy alone, and a spark runs home with Sparks. */
    private void discharge(EnemyMob enemy) {
        this.dealDamage(enemy, Damage.magic(DamageUnits.ofPoints(StaticMechanic.DISCHARGE_POINTS)),
                this.stats().attack().withoutCrits());
        this.sendHome(enemy, Passive.STATIC);
    }

    /** Soulbrand: this tower's stack bursts around where the enemy died, tagging nothing, and its soul flies home. */
    private void burstSoul(EnemyMob dead) {
        InRangeTargetQuery.everyone((int) dead.getX(), (int) dead.getY(), SoulbrandMechanic.BURST_CELLS * this.cell())
                .matching(this.context.enemies()).stream()
                .filter(enemy -> enemy != dead && !enemy.isDead())
                .forEach(enemy -> this.dealDamage(enemy, Damage.magic(DamageUnits.ofPoints(SoulbrandMechanic.BURST_POINTS)),
                        this.stats().attack().withoutCrits()));
        this.sendHome(dead, Passive.SOULBRAND);
    }

    private void sendHome(EnemyMob enemy, Passive passive) {
        Vec2 from = new Vec2(enemy.getX(), enemy.getY());
        Vec2 home = new Vec2(this.centerX, this.centerY);
        this.wisps.add(new PendingWisp(new Wisp(from, passive, new Flight(this.tickNow,
                this.tickNow + this.flightTicks(from, home, WISP_CELLS_PER_TICK))),
                PassivePayout.forMaxHealth(enemy.getMaxHealthPoints())));
    }

    /** Sends {@code hex} from {@code from} to {@code target}; it takes hold when it arrives. */
    private void sendCurse(Hex hex, Vec2 from, EnemyMob target, Dose dose) {
        Vec2 to = new Vec2(target.getX(), target.getY());
        this.curses.add(new PendingCurse(new Curse(hex.kind(), from, target, new Flight(this.tickNow,
                this.tickNow + this.flightTicks(from, to, CURSE_CELLS_PER_TICK))), hex, this.stats(), dose));
    }

    /** A curse that arrives takes hold, on the enemies around it too with Blight Cloud; one whose enemy died is lost. */
    private void landCurse(PendingCurse pending, SplashSpec spec) {
        EnemyMob target = pending.curse().target();
        if (target.isDead()) {
            return;
        }
        HexSpec hexes = spec.hexes();
        this.lay(pending, target, hexes);
        if (hexes.cloud() && pending.hex().kind() == EffectKind.BLIGHT) {
            InRangeTargetQuery.everyone((int) target.getX(), (int) target.getY(), HexSpec.CLOUD_CELLS * this.cell())
                    .matching(this.context.enemies()).stream()
                    .filter(enemy -> enemy != target && !enemy.isDead())
                    .forEach(enemy -> this.lay(pending, enemy, hexes));
        }
    }

    private void lay(PendingCurse pending, EnemyMob target, HexSpec hexes) {
        Hex hex = pending.hex();
        int ticks = pending.dose().ticks();
        hex.lay(target, ticks, pending.dose().grownSlow(), new Laying(pending.stats()));
        if (!target.hasEffect(hex.kind())) {
            return;
        }
        this.hexKeeper.record(target, hex.kind(), ticks);
        if (hexes.agony()) {
            this.applyEffect(target, sink -> Effect.vulnerable(1, ticks, sink));
        }
        if (hexes.choking() && hex.kind() == EffectKind.BLIGHT) {
            this.applyEffect(target, sink -> Effect.silenced(ticks, sink));
        }
    }

    /** What the fork has made it: the Stormcaller on Static, the Hexer on Soulbrand. */
    public Fork fork() {
        return switch (this.spec(this.mechanics.all()).passive()) {
            case NONE -> Fork.NONE;
            case STATIC -> Fork.STORMCALLER;
            case SOULBRAND -> Fork.HEXER;
        };
    }

    /** The side of the fork {@code node} belongs to: none for a node either side may buy. */
    public static Fork forkOf(UpgradeNode node) {
        List<UpgradeNode> path = TREE.pathTo(node);
        if (path.contains(STORMCALLER)) {
            return Fork.STORMCALLER;
        }
        return path.contains(HEXER) ? Fork.HEXER : Fork.NONE;
    }

    @Override
    public String displayName() {
        return switch (this.fork()) {
            case NONE -> super.displayName();
            case STORMCALLER -> "Stormcaller";
            case HEXER -> "Hexer";
        };
    }

    /** The stones in flight, oldest first. */
    public List<Stone> stonesInFlight() {
        return List.copyOf(this.stones);
    }

    /** The curses on their way. */
    public List<Curse> cursesInFlight() {
        return this.curses.stream().map(PendingCurse::curse).toList();
    }

    /** The souls and sparks flying home. */
    public List<Wisp> wispsInFlight() {
        return this.wisps.stream().map(PendingWisp::wisp).toList();
    }

    /** The blasts that landed in the last few ticks. */
    public List<Blast> recentBlasts() {
        return List.copyOf(this.blasts);
    }

    /** The bolts that struck in the last few ticks: the Arc's jumps and the casts. */
    public List<Bolt> recentBolts() {
        return List.copyOf(this.bolts);
    }

    public float getCoolDownFraction() {
        return this.clock.remaining();
    }

    /** Whether an ability it owns is full and waits for a target. */
    public boolean holdsAFullAbility() {
        return this.abilities().stream().anyMatch(state -> state.bar().isReady());
    }

    @Override
    protected String targetingLabel() {
        return "random";
    }

    @Override
    protected String description() {
        return "A stone lands where its target stood when it was thrown, and bursts there.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitSplashTower(this);
    }

    /** What the abilities may make the tower do, on the enemies in range this tick. */
    private final class Casting implements CastActions {

        private final SplashSpec spec;
        private final List<EnemyMob> inReach;

        private Casting(SplashSpec spec, List<EnemyMob> inReach) {
            this.spec = spec;
            this.inReach = inReach;
        }

        @Override
        public boolean primeArc() {
            SplashTower.this.grantStatus(TowerStatus.primedShot(TowerTerm.ARC, StatModifiers.none()));
            return true;
        }

        @Override
        public boolean thunderclap(float share) {
            TowerStats stats = SplashTower.this.stats();
            Vec2 from = new Vec2(SplashTower.this.centerX, SplashTower.this.centerY);
            for (EnemyMob enemy : this.inReach) {
                SplashTower.this.strike(enemy, from, Math.round(stats.damage() * share), stats, BoltKind.THUNDERCLAP);
            }
            return true;
        }

        @Override
        public boolean thunderstrike(float factor) {
            Optional<EnemyMob> target = THUNDERSTRIKE_AIM.selectFrom(this.inReach);
            target.ifPresent(enemy -> {
                TowerStats stats = SplashTower.this.stats();
                Vec2 above = new Vec2(enemy.getX(), enemy.getY() - THUNDERSTRIKE_HEIGHT_CELLS * SplashTower.this.cell());
                SplashTower.this.strike(enemy, above, Math.round(stats.damage() * factor), stats, BoltKind.THUNDERSTRIKE);
            });
            return target.isPresent();
        }

        @Override
        public boolean hex(Hex hex) {
            List<EnemyMob> left = new ArrayList<>(this.inReach);
            List<EnemyMob> cursed = new ArrayList<>();
            float cell = SplashTower.this.cell();
            for (int i = 0; i < this.spec.hexes().cursesPerCast(); i++) {
                Optional<EnemyMob> target = hex.target(left, cell);
                if (target.isEmpty()) {
                    break;
                }
                cursed.add(target.get());
                left.remove(target.get());
            }
            Vec2 from = new Vec2(SplashTower.this.centerX, SplashTower.this.centerY);
            int ticks = Math.round(hex.seconds() * TICKS_PER_SECOND);
            cursed.forEach(target -> SplashTower.this.sendCurse(hex, from, target, new Dose(ticks, 0f)));
            return !cursed.isEmpty();
        }
    }

    /** A cast's bolt: magic that never crits, and tags nothing. */
    private void strike(EnemyMob enemy, Vec2 from, int amount, TowerStats stats, BoltKind kind) {
        HitResult hit = this.landHit(enemy, Damage.magic(amount), stats.attack().withoutCrits());
        if (!hit.dodged()) {
            this.report(enemy, HitKind.ARC, false, enemy.isDead());
        }
        this.bolts.add(new Bolt(from, new Vec2(enemy.getX(), enemy.getY()), this.tickNow, kind));
    }

    /** What a hex puts on an enemy as it lands, at the strength of the tower when it was cast. */
    private final class Laying implements HexHand {

        private final TowerStats stats;

        private Laying(TowerStats stats) {
            this.stats = stats;
        }

        @Override
        public void curse(EnemyMob target, EffectKind kind, int ticks) {
            SplashTower.this.applyEffect(target, sink -> Effect.hex(kind, ticks, sink));
        }

        @Override
        public void blight(EnemyMob target, int ticks, float slowPerTick, float start) {
            float held = target.activeEffects().stream().filter(effect -> effect.kind() == EffectKind.BLIGHT)
                    .map(Effect::grownSlow).findFirst().orElse(0f);
            SplashTower.this.applyEffect(target, sink -> Effect.blight(ticks, slowPerTick, start, held, sink));
        }

        @Override
        public void poison(EnemyMob target, float baseShare, int ticks) {
            Damage perTick = Damage.magic(Math.round(this.stats.damage() * baseShare));
            SplashTower.this.applyEffect(target, sink -> Effect.poison(perTick, ticks, sink));
        }
    }

    /** What its hexes may make it do as they pay out. */
    private final class Hexing implements HexCaster {

        @Override
        public void dealPeriodicDamage(EnemyMob target, Damage damage) {
            SplashTower.this.dealPeriodicDamage(target, damage);
        }

        @Override
        public float distance(float cells) {
            return cells * SplashTower.this.cell();
        }

        @Override
        public EnemyRegistry enemies() {
            return SplashTower.this.context.enemies();
        }

        @Override
        public void jump(HexJump jump) {
            HEXES.stream().filter(hex -> hex.kind() == jump.hex().kind()).findFirst().ifPresent(hex ->
                    SplashTower.this.sendCurse(hex, jump.from(), jump.to(), new Dose(jump.hex().castTicks(), jump.grownSlow())));
        }
    }

    /** One Arc on its way: what it was thrown with, how many jumps it has left and whom it has hit. */
    private static final class ArcRun {

        private final TowerStats stats;
        private final ArcSpec spec;
        /** Membership only: who the Arc has hit. */
        private final Set<EnemyMob> struck = Collections.newSetFromMap(new IdentityHashMap<>());
        private int jumpsLeft;

        private ArcRun(TowerStats stats, ArcSpec spec) {
            this.stats = stats;
            this.spec = spec;
            this.jumpsLeft = spec.jumps();
        }

        TowerStats stats() {
            return this.stats;
        }

        ArcSpec spec() {
            return this.spec;
        }

        int jumpsLeft() {
            return this.jumpsLeft;
        }

        boolean struck(EnemyMob enemy) {
            return this.struck.contains(enemy);
        }

        void spend(EnemyMob hit) {
            this.struck.add(hit);
            this.jumpsLeft--;
        }
    }

    private record PendingHop(Hop hop, ArcRun run) {
    }

    private record PendingCurse(Curse curse, Hex hex, TowerStats stats, Dose dose) {
    }

    /** How long a hex lasts as cast, and the slow it starts from. */
    private record Dose(int ticks, float grownSlow) {
    }

    private record PendingWisp(Wisp wisp, AbilityUnits pays) {
    }

    /** What the fork has made the Burst. */
    public enum Fork {
        NONE,
        STORMCALLER,
        HEXER
    }

    /** What struck: a jump of the Arc, or a cast. */
    public enum BoltKind {
        ARC,
        THUNDERCLAP,
        THUNDERSTRIKE
    }

    /**
     * When something flies.
     *
     * @param firedAt the tick it left
     * @param landsAt the tick it arrives
     */
    public record Flight(int firedAt, int landsAt) {
    }

    /**
     * A stone on its way: unguided, from the tower to where its target stood, with the stats it was thrown with.
     *
     * @param charged whether it carries an Arc
     */
    public record Stone(Vec2 to, TowerStats stats, boolean charged, Flight flight) {
    }

    /**
     * A stone that burst.
     *
     * @param core    the radius it dealt full damage within, in pixels
     * @param edge    the radius its weaker edge reached, in pixels
     * @param tick when it landed
     */
    public record Blast(Vec2 at, float core, float edge, int tick) {
    }

    /** A jump of an Arc on its way, from where it is to the enemy it strikes. */
    public record Hop(Vec2 from, EnemyMob to, Flight flight) {
    }

    /** A bolt that struck. */
    public record Bolt(Vec2 from, Vec2 to, int tick, BoltKind kind) {
    }

    /** A hex on its way to the enemy it curses. */
    public record Curse(EffectKind kind, Vec2 from, EnemyMob target, Flight flight) {
    }

    /**
     * A soul or a spark flying home with what its passive pays.
     *
     * @param passive Soulbrand for a soul, Static for a spark
     */
    public record Wisp(Vec2 from, Passive passive, Flight flight) {
    }
}
