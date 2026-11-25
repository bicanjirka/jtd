package td.tower;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageUnits;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;
import td.enemy.HitResult;
import td.stat.StatModifiers;
import td.tower.ability.AbilityCharge;
import td.tower.ability.AbilityUnits;
import td.tower.proc.HitAdjustment;
import td.tower.proc.HitKind;
import td.tower.splash.AgonyMechanic;
import td.tower.splash.ArcMechanic;
import td.tower.splash.ArcSpec;
import td.tower.splash.BacklashHex;
import td.tower.splash.Blast;
import td.tower.splash.BlightCloudMechanic;
import td.tower.splash.BlightHex;
import td.tower.splash.Bolt;
import td.tower.splash.BoltKind;
import td.tower.splash.CastActions;
import td.tower.splash.ChainLightningMechanic;
import td.tower.splash.ChokingHexesMechanic;
import td.tower.splash.ConductorMechanic;
import td.tower.splash.ContagionMechanic;
import td.tower.splash.Curse;
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
import td.tower.splash.JoltMechanic;
import td.tower.splash.Passive;
import td.tower.splash.PassivePayout;
import td.tower.splash.ReckoningMechanic;
import td.tower.splash.ShockwaveMechanic;
import td.tower.splash.SoulHarvestMechanic;
import td.tower.splash.SoulbrandMechanic;
import td.tower.splash.SplashMechanic;
import td.tower.splash.SplashSpec;
import td.tower.splash.StaticMechanic;
import td.tower.splash.Stone;
import td.tower.splash.TagLedger;
import td.tower.splash.ThunderstrikeMechanic;
import td.tower.splash.TwinHexMechanic;
import td.tower.splash.WildArcMechanic;
import td.tower.splash.Wisp;
import td.tower.targeting.HighestHealthSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.NearestSelector;
import td.tower.targeting.PreferringSelector;
import td.tower.targeting.RandomSelector;
import td.tower.targeting.SoonestOutSelector;
import td.tower.targeting.TargetSelector;
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
    public static final float DAMAGE_POINTS = 15f;
    public static final float RANGE = 2.5f;
    public static final float CORE_CELLS = 0.75f;
    public static final float EDGE_CELLS = 1.25f;
    /** What the edge of a blast deals, a share of its core's damage. */
    public static final float EDGE_SHARE = 0.5f;

    /** A throw a second. */
    private static final int COOLDOWN_MAX = 19;
    /** How far a stone flies over the ground in a tick, in cells: a walking enemy is still in the core when it lands. */
    private static final float STONE_CELLS_PER_TICK = 0.2f;
    private static final float WISP_CELLS_PER_TICK = 0.3f;
    private static final float CURSE_CELLS_PER_TICK = 0.35f;
    private static final int MIN_FLIGHT_TICKS = 2;
    /** The pause between two jumps of an Arc, so each reads on its own. */
    private static final int HOP_TICKS = 4;
    /** How far above its target a Thunderstrike's bolt starts, in cells. */
    private static final float THUNDERSTRIKE_HEIGHT_CELLS = 2f;
    private static final TargetSelector THUNDERSTRIKE_AIM = PreferringSelector.priority(new HighestHealthSelector());

    private static final Hex DOOM = new DoomHex();
    private static final Hex BLIGHT = new BlightHex();
    private static final Hex BACKLASH = new BacklashHex();
    private static final List<Hex> HEXES = List.of(DOOM, BLIGHT, BACKLASH);

    private static final UpgradeNode LONG_THROW = UpgradeNode.of(RangeNodes.RANGE_ID, "Long Throw", 5)
            .withModifiers(TowerModifiers.range(0.7f));
    private static final UpgradeNode WIDE_CHARGE = UpgradeNode.of("splash.wide_charge", "Wide Charge", 5)
            .withModifiers(TowerModifiers.areaRadius(0.25f).plus(TowerModifiers.blastEdge(0.5f)));
    private static final UpgradeNode FLINT_STONES = UpgradeNode.of("splash.flint_stones", "Flint Stones", 5)
            .withModifiers(TowerModifiers.armorPenetrationFlat(30f));

    private static final UpgradeNode STORMCALLER = UpgradeNode.of("splash.stormcaller", "Stormcaller", 10)
            .inTier(1);
    private static final UpgradeNode HEXER = UpgradeNode.of("splash.hexer", "Hexer", 10)
            .inTier(1);
    private static final UpgradeNode ARC = UpgradeNode.of("splash.arc", "Arc", 10)
            .after(STORMCALLER)
            .inTier(1);
    private static final UpgradeNode HEX_OF_DOOM = UpgradeNode.of("splash.hex_of_doom", "Hex of Doom", 10)
            .after(HEXER)
            .inTier(1);
    private static final UpgradeNode HEX_OF_BLIGHT = UpgradeNode.of("splash.hex_of_blight", "Hex of Blight", 10)
            .after(HEXER)
            .inTier(1);
    private static final UpgradeCondition A_HEX = UpgradeCondition.owns(HEX_OF_DOOM.id())
            .or(UpgradeCondition.owns(HEX_OF_BLIGHT.id()));

    private static final UpgradeNode RECKONING = UpgradeNode.of("splash.reckoning", "Reckoning", 15)
            .after(HEX_OF_DOOM)
            .withOwnArt()
            .inTier(2);
    private static final UpgradeNode BLIGHT_CLOUD = UpgradeNode.of("splash.blight_cloud", "Blight Cloud", 20)
            .after(HEX_OF_BLIGHT)
            .withOwnArt()
            .inTier(2);
    private static final UpgradeNode AGONY = UpgradeNode.of("splash.agony", "Agony", 15)
            .withRequires(A_HEX)
            .withOwnArt()
            .inTier(2);
    private static final UpgradeNode CONTAGION = UpgradeNode.of("splash.contagion", "Contagion", 15)
            .withRequires(A_HEX)
            .withOwnArt()
            .inTier(2);
    private static final UpgradeNode CHAIN_LIGHTNING = UpgradeNode.of("splash.chain_lightning", "Chain Lightning", 15)
            .after(ARC)
            .inTier(2);
    private static final UpgradeNode WILD_ARC = UpgradeNode.of("splash.wild_arc", "Wild Arc", 15)
            .after(ARC)
            .inTier(2);
    private static final UpgradeNode CONDUCTOR = UpgradeNode.of("splash.conductor", "Conductor", 15)
            .after(ARC)
            .withOwnArt()
            .inTier(2);
    private static final UpgradeNode JOLT = UpgradeNode.of("splash.jolt", "Jolt", 15)
            .after(ARC)
            .withOwnArt()
            .inTier(2);
    private static final UpgradeNode HEAVY_STONES = UpgradeNode.of("splash.heavy_stones", "Heavy Stones", 25)
            .withModifiers(TowerModifiers.damage(7f))
            .inTier(2);
    private static final UpgradeNode LONG_THROW_2 = UpgradeNode.of(RangeNodes.RANGE_2_ID, "Long Throw II", 15)
            .withModifiers(TowerModifiers.range(1f))
            .inTier(2);

    private static final UpgradeNode SOUL_HARVEST = UpgradeNode.of("splash.soul_harvest", "Soul Harvest", 20)
            .after(HEXER)
            .withOwnArt()
            .inTier(3);
    private static final UpgradeNode HEX_OF_BACKLASH = UpgradeNode.of("splash.hex_of_backlash", "Hex of Backlash", 30)
            .after(HEXER)
            .withOwnArt()
            .inTier(3);
    private static final UpgradeNode CHOKING_HEXES = UpgradeNode.of("splash.choking_hexes", "Choking Hexes", 20)
            .after(HEXER)
            .withOwnArt()
            .inTier(3);
    private static final UpgradeNode TWIN_HEX = UpgradeNode.of("splash.twin_hex", "Twin Hex", 20)
            .withRequires(A_HEX)
            .withOwnArt()
            .inTier(3);
    private static final UpgradeNode SHOCKWAVE = UpgradeNode.of("splash.shockwave", "Shockwave", 25)
            .after(STORMCALLER)
            .inTier(3);
    private static final UpgradeNode THUNDERSTRIKE = UpgradeNode.of("splash.thunderstrike", "Thunderstrike", 25)
            .after(STORMCALLER)
            .inTier(3);
    private static final UpgradeNode HIGH_VOLTAGE = UpgradeNode.of("splash.high_voltage", "High Voltage", 20)
            .after(ARC)
            .withOwnArt()
            .inTier(3);
    private static final UpgradeNode FEEDBACK = UpgradeNode.of("splash.feedback", "Feedback", 20)
            .after(CONDUCTOR)
            .withOwnArt()
            .inTier(3);
    private static final UpgradeNode QUICK_HANDS = UpgradeNode.of("splash.quick_hands", "Quick Hands", 30)
            .withModifiers(TowerModifiers.attackSpeed(1f / 0.8f - 1f))
            .inTier(3);
    private static final UpgradeNode PIERCING_STONES = UpgradeNode.of("splash.piercing_stones", "Piercing Stones", 20)
            .withModifiers(TowerModifiers.armorPenetrationFlat(100f).plus(TowerModifiers.damage(3f)))
            .inTier(3);

    private static final UpgradeTree TREE = UpgradeTree.of(List.of(LONG_THROW, WIDE_CHARGE, FLINT_STONES))
            .with(STORMCALLER, HEXER, ARC, HEX_OF_DOOM, HEX_OF_BLIGHT,
                    RECKONING, BLIGHT_CLOUD, AGONY, CONTAGION, CHAIN_LIGHTNING, WILD_ARC, CONDUCTOR, JOLT,
                    HEAVY_STONES, LONG_THROW_2,
                    SOUL_HARVEST, HEX_OF_BACKLASH, CHOKING_HEXES, TWIN_HEX, SHOCKWAVE, THUNDERSTRIKE, HIGH_VOLTAGE,
                    FEEDBACK, QUICK_HANDS, PIERCING_STONES)
            .withFork(UpgradeCluster.of("Stormcaller or Hexer", STORMCALLER, HEXER))
            .withCluster(UpgradeCluster.of("Hex", HEX_OF_DOOM, HEX_OF_BLIGHT))
            .withCluster(UpgradeCluster.of("Arc's path", CHAIN_LIGHTNING, WILD_ARC))
            .withCluster(UpgradeCluster.of("Storm", SHOCKWAVE, THUNDERSTRIKE));

    private static final MechanicCatalogue<SplashMechanic> MECHANICS = MechanicCatalogue.of(
            entry(STORMCALLER.id(), StaticMechanic::new),
            entry(HEXER.id(), SoulbrandMechanic::new),
            entry(ARC.id(), ArcMechanic::new),
            entry(HEX_OF_DOOM.id(), () -> new HexMechanic(DOOM, 2)),
            entry(HEX_OF_BLIGHT.id(), () -> new HexMechanic(BLIGHT, 2)),
            entry(RECKONING.id(), ReckoningMechanic::new),
            entry(BLIGHT_CLOUD.id(), BlightCloudMechanic::new),
            entry(AGONY.id(), AgonyMechanic::new),
            entry(CONTAGION.id(), ContagionMechanic::new),
            entry(CHAIN_LIGHTNING.id(), ChainLightningMechanic::new),
            entry(WILD_ARC.id(), WildArcMechanic::new),
            entry(CONDUCTOR.id(), ConductorMechanic::new),
            entry(JOLT.id(), JoltMechanic::new),
            entry(SOUL_HARVEST.id(), SoulHarvestMechanic::new),
            entry(HEX_OF_BACKLASH.id(), () -> new HexMechanic(BACKLASH, 3)),
            entry(CHOKING_HEXES.id(), ChokingHexesMechanic::new),
            entry(TWIN_HEX.id(), TwinHexMechanic::new),
            entry(SHOCKWAVE.id(), ShockwaveMechanic::new),
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
        SplashSpec spec = SplashSpec.from(this.viewpoint());
        for (SplashMechanic mechanic : owned) {
            spec = mechanic.refineSpec(spec);
        }
        return spec;
    }

    private Vec2 center() {
        return new Vec2(this.centerX, this.centerY);
    }

    @Override
    protected void act(int gameTime) {
        List<SplashMechanic> owned = this.mechanics.all();
        SplashSpec spec = this.spec(owned);
        this.forgetOldTraces();
        this.settlePassive(spec);
        this.hexKeeper.doTick(spec.hexes());
        this.forkClouds(spec);
        this.landArrived(spec);
        boolean ready = this.clock.isReady();
        boolean casting = owned.stream().flatMap(mechanic -> mechanic.ability().stream()).anyMatch(AbilityCharge::isReady);
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
        this.blasts.removeIf(blast -> MomentMemory.forgets(blast.tick(), this.tickNow()));
        this.bolts.removeIf(bolt -> MomentMemory.forgets(bolt.tick(), this.tickNow()));
    }

    /** Throws a stone at where {@code target} stands: it flies with the stats it was thrown with, charged or not. */
    private void throwAt(EnemyMob target) {
        boolean charged = this.holdsStatus(TowerTerm.ARC);
        TowerStats stats = this.beginShot();
        Vec2 to = target.position();
        this.stones.add(new Stone(to, stats, charged, this.flightOver(this.center(), to, STONE_CELLS_PER_TICK)));
        this.clock.spend();
    }

    /** A flight from {@code from} to {@code to} at {@code cellsPerTick}, starting now. */
    private ShotFlight flightOver(Vec2 from, Vec2 to, float cellsPerTick) {
        return ShotFlight.across(this.tickNow(), Math.hypot(to.x() - from.x(), to.y() - from.y()),
                cellsPerTick * this.cellPixels(), MIN_FLIGHT_TICKS);
    }

    /** Lands every stone, jump, curse and soul or spark whose flight ends now. */
    private void landArrived(SplashSpec spec) {
        int now = this.tickNow();
        Arrivals.takeDue(this.stones, stone -> stone.flight().landsAt(), now).forEach(stone -> this.land(stone, spec));
        Arrivals.takeDue(this.hops, hop -> hop.hop().flight().landsAt(), now).forEach(hop -> this.landHop(hop, spec));
        Arrivals.takeDue(this.curses, curse -> curse.curse().flight().landsAt(), now)
                .forEach(curse -> this.landCurse(curse, spec));
        Arrivals.takeDue(this.wisps, wisp -> wisp.wisp().flight().landsAt(), now)
                .forEach(wisp -> this.earnAbilities(wisp.pays()));
    }

    /**
     * A stone lands with the stats it was thrown with: everything within its edge takes a hit, the core at full damage;
     * what it damaged carries the passive's stack, and what it dealt earns the currency. A charged stone starts its Arc
     * from there.
     */
    private void land(Stone stone, SplashSpec spec) {
        TowerStats stats = stone.stats();
        float core = stats.value(TowerStat.AREA_RADIUS) * this.cellPixels();
        float edge = Math.max(core, stats.value(TowerStat.BLAST_EDGE) * this.cellPixels());
        Vec2 at = stone.to();
        List<EnemyMob> caught = InRangeTargetQuery.everyone(at, edge).matching(this.context.enemies());
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
        this.blasts.add(new Blast(at, core, edge, this.tickNow()));
        if (stone.charged()) {
            ArcRun run = new ArcRun(stats, spec.arc());
            this.jumpOn(at, Optional.empty(), run);
        }
    }

    /** {@code enemy} carries the passive's stack once a hit reached its health. */
    private void tag(EnemyMob enemy, HitResult hit, SplashSpec spec) {
        if (spec.passive() != Passive.NONE && hit.landed().amount() > 0) {
            this.tags.tag(enemy);
        }
    }

    /**
     * Sends the Arc on from {@code from}, off {@code current}, to the nearest enemy it may strike, if it has a jump
     * left; one that may revisit stays on {@code current} when no other is near.
     */
    private void jumpOn(Vec2 from, Optional<EnemyMob> current, ArcRun run) {
        if (run.jumpsLeft() <= 0) {
            return;
        }
        List<EnemyMob> near = InRangeTargetQuery.everyone(from, ArcSpec.REACH_CELLS * this.cellPixels())
                .matching(this.context.enemies()).stream()
                .filter(enemy -> current.stream().noneMatch(off -> off == enemy) && enemy.validTarget()
                        && (run.spec().revisits() || !run.struck(enemy)))
                .toList();
        Optional<EnemyMob> next = new NearestSelector(from.x(), from.y()).selectFrom(near)
                .or(() -> current.filter(stay -> run.spec().revisits() && !stay.isDead()));
        next.ifPresent(to -> this.hops.add(new PendingHop(new Hop(from, to, ShotFlight.lasting(this.tickNow(), HOP_TICKS)),
                run)));
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
        int amount = DamageUnits.ofPoints(arc.points() * adjustment.damageFactor());
        AttackProfile attack = adjustment.applyTo(stats.attack()).withCritChance(arc.critChance());
        attack = attack.withCritMultiplier(Math.max(attack.critMultiplier(), arc.critMultiplier()));
        Vec2 at = target.position();
        HitResult hit = this.landHit(target, Damage.magic(amount), attack);
        run.spend(target);
        if (!hit.dodged()) {
            this.report(target, HitKind.ARC, hit.critical(), target.isDead());
            this.tag(target, hit, spec);
            if (hit.critical()) {
                this.earnAbilities(arc.critRefund());
            }
            this.bolts.add(Bolt.jump(hop.from(), target, this.tickNow(), hit.critical()));
            if (arc.jolt()) {
                this.applyEffect(target, sink -> Effect.vulnerable(1,
                        Math.round(ArcSpec.JOLT_SECONDS * TICKS_PER_SECOND), sink));
            }
        }
        this.jumpOn(at, Optional.of(target), run);
    }

    /** Static discharges what it tagged once badly hurt; Soulbrand bursts what it tagged once killed. */
    private void settlePassive(SplashSpec spec) {
        if (this.tags.isEmpty()) {
            return;
        }
        switch (spec.passive()) {
            case STATIC -> this.tags.settleStatic(this::discharge);
            case SOULBRAND -> this.tags.settleSoulbrand(this::burstSoul);
            case NONE -> this.tags.clear();
        }
    }

    /** Static: this tower's stack discharges into the enemy alone, and a spark runs home with Sparks. */
    private void discharge(EnemyMob enemy) {
        this.dealDamage(enemy, StaticMechanic.discharge(), this.stats().attack().withoutCrits());
        this.sendHome(enemy, Passive.STATIC);
    }

    /** Soulbrand: this tower's stack bursts around where the enemy died, tagging nothing, and its soul flies home. */
    private void burstSoul(EnemyMob dead) {
        InRangeTargetQuery.around(dead, SoulbrandMechanic.BURST_CELLS * this.cellPixels())
                .matching(this.context.enemies()).stream()
                .filter(enemy -> !enemy.isDead())
                .forEach(enemy -> this.dealDamage(enemy, SoulbrandMechanic.burst(), this.stats().attack().withoutCrits()));
        this.sendHome(dead, Passive.SOULBRAND);
    }

    private void sendHome(EnemyMob enemy, Passive passive) {
        Vec2 from = enemy.position();
        this.wisps.add(new PendingWisp(new Wisp(from, passive, this.flightOver(from, this.center(), WISP_CELLS_PER_TICK)),
                PassivePayout.forMaxHealth(enemy.getMaxHealthPoints())));
    }

    /** Sends {@code hex} from {@code from} to {@code target}; it takes hold when it arrives. */
    private void sendCurse(Hex hex, Vec2 from, EnemyMob target, Dose dose) {
        Curse curse = new Curse(hex.kind(), from, target, this.flightOver(from, target.position(), CURSE_CELLS_PER_TICK));
        PendingCurse pending = PendingCurse.of(curse, hex, dose);
        this.curses.add(hex.kind() == EffectKind.BLIGHT
                ? pending.withForkAt(Math.max(curse.flight().firedAt(), curse.flight().landsAt()
                        - (int) Math.ceil(HexSpec.CLOUD_FORK_CELLS / CURSE_CELLS_PER_TICK)))
                : pending);
    }

    /**
     * With Blight Cloud, a Blight curse that is {@code CLOUD_FORK_CELLS} short of its enemy forks: it picks the enemies
     * around its target that would leave soonest, and a curse of its own leaves the fork point for each, while it flies
     * on to its target.
     */
    private void forkClouds(SplashSpec spec) {
        if (!spec.hexes().cloud()) {
            return;
        }
        int now = this.tickNow();
        for (int i = 0; i < this.curses.size(); i++) {
            PendingCurse pending = this.curses.get(i);
            if (pending.forkAt() > now) {
                continue;
            }
            this.curses.set(i, pending.unforking());
            EnemyMob target = pending.curse().target();
            if (target.isDead()) {
                continue;
            }
            ShotFlight flight = pending.curse().flight();
            double flown = Math.min(1d, (double) (now - flight.firedAt()) / Math.max(1, flight.landsAt() - flight.firedAt()));
            Vec2 from = pending.curse().from();
            Vec2 at = new Vec2(from.x() + (target.getX() - from.x()) * flown, from.y() + (target.getY() - from.y()) * flown);
            List<EnemyMob> around = new ArrayList<>(InRangeTargetQuery.around(target, HexSpec.CLOUD_CELLS * this.cellPixels())
                    .matching(this.context.enemies()).stream()
                    .filter(enemy -> enemy != target && !enemy.isDead())
                    .toList());
            for (int taken = 0; taken < HexSpec.CLOUD_TARGETS; taken++) {
                Optional<EnemyMob> next = new SoonestOutSelector().selectFrom(around);
                next.ifPresent(enemy -> this.curses.add(PendingCurse.of(new Curse(pending.curse().kind(), at, enemy,
                        this.flightOver(at, enemy.position(), CURSE_CELLS_PER_TICK)), pending.hex(), pending.dose())));
                next.ifPresent(around::remove);
            }
        }
    }

    /** A curse that arrives takes hold; one whose enemy died is lost. */
    private void landCurse(PendingCurse pending, SplashSpec spec) {
        EnemyMob target = pending.curse().target();
        if (target.isDead()) {
            return;
        }
        HexSpec hexes = spec.hexes();
        this.lay(pending, target, hexes);
    }

    private void lay(PendingCurse pending, EnemyMob target, HexSpec hexes) {
        Hex hex = pending.hex();
        int ticks = pending.dose().ticks();
        hex.lay(target, ticks, pending.dose().grownSlow(), new Laying());
        if (!target.hasEffect(hex.kind())) {
            return;
        }
        if (pending.dose().jumped()) {
            this.hexKeeper.recordJump(target, hex.kind(), ticks, pending.dose().stored());
        } else {
            this.hexKeeper.record(target, hex.kind(), ticks);
        }
        if (hexes.agony()) {
            this.applyEffect(target, sink -> Effect.vulnerable(1, ticks, sink));
        }
        if (hexes.choking()) {
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
        public boolean shockwave(float points) {
            TowerStats stats = SplashTower.this.stats();
            Vec2 from = SplashTower.this.center();
            for (EnemyMob enemy : this.inReach) {
                SplashTower.this.strike(enemy, from, DamageUnits.ofPoints(points), stats, BoltKind.SHOCKWAVE);
            }
            return true;
        }

        @Override
        public boolean thunderstrike(float points, float dazeSeconds) {
            return THUNDERSTRIKE_AIM.selectFrom(this.inReach).map(enemy -> {
                TowerStats stats = SplashTower.this.stats();
                Vec2 above = new Vec2(enemy.getX(), enemy.getY() - THUNDERSTRIKE_HEIGHT_CELLS * SplashTower.this.cellPixels());
                SplashTower.this.strike(enemy, above, DamageUnits.ofPoints(points), stats, BoltKind.THUNDERSTRIKE);
                if (!enemy.isDead()) {
                    SplashTower.this.applyEffect(enemy,
                            sink -> Effect.dazed(Math.round(dazeSeconds * TICKS_PER_SECOND), sink));
                }
                return true;
            }).orElse(false);
        }

        @Override
        public boolean hex(Hex hex) {
            List<EnemyMob> left = new ArrayList<>(this.inReach);
            List<EnemyMob> cursed = new ArrayList<>();
            for (int i = 0; i < this.spec.hexes().cursesPerCast(); i++) {
                Optional<EnemyMob> target = hex.target(left);
                if (target.isEmpty()) {
                    break;
                }
                cursed.add(target.get());
                left.remove(target.get());
            }
            Vec2 from = SplashTower.this.center();
            int ticks = Math.round(hex.seconds() * TICKS_PER_SECOND);
            cursed.forEach(target -> SplashTower.this.sendCurse(hex, from, target, Dose.cast(ticks)));
            return !cursed.isEmpty();
        }
    }

    /** A cast's bolt: magic that never crits, and tags nothing. */
    private void strike(EnemyMob enemy, Vec2 from, int amount, TowerStats stats, BoltKind kind) {
        HitResult hit = this.landHit(enemy, Damage.magic(amount), stats.attack().withoutCrits());
        if (!hit.dodged()) {
            this.report(enemy, HitKind.ARC, false, enemy.isDead());
        }
        Bolt bolt = Bolt.cast(from, enemy, this.tickNow(), kind);
        this.bolts.add(hit.dodged() ? bolt.missed() : bolt);
    }

    /** What a hex puts on an enemy as it lands. */
    private final class Laying implements HexHand {

        @Override
        public void curse(EnemyMob target, EffectKind kind, int ticks) {
            SplashTower.this.applyEffect(target, sink -> Effect.hex(kind, ticks, sink));
        }

        @Override
        public void blight(EnemyMob target, int ticks, float slowPerTick, float start) {
            float held = target.activeEffect(EffectKind.BLIGHT).map(Effect::grownSlow).orElse(0f);
            SplashTower.this.applyEffect(target, sink -> Effect.blight(ticks, slowPerTick, start, held, sink));
        }

        @Override
        public void poison(EnemyMob target, float points) {
            Damage fuel = Damage.magic(DamageUnits.ofPoints(points));
            SplashTower.this.applyEffect(target, sink -> Effect.poison(fuel, sink));
        }
    }

    /** What its hexes may make it do as they pay out. */
    private final class Hexing implements HexCaster {

        @Override
        public void dealShadowHit(EnemyMob target, Damage damage) {
            SplashTower.this.landHit(target, damage, SplashTower.this.stats().attack().withoutCrits().asShadow());
        }

        @Override
        public float distance(float cells) {
            return cells * SplashTower.this.cellPixels();
        }

        @Override
        public EnemyRegistry enemies() {
            return SplashTower.this.context.enemies();
        }

        @Override
        public void jump(HexJump jump) {
            HEXES.stream().filter(hex -> hex.kind() == jump.hex().kind()).findFirst().ifPresent(hex ->
                    SplashTower.this.sendCurse(hex, jump.from(), jump.to(), Dose.jumped(jump.hex().castTicks(), jump.grownSlow(), jump.hex().stored())));
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

    /** A jump of an Arc on its way, from where it is to the enemy it strikes. */
    private record Hop(Vec2 from, EnemyMob to, ShotFlight flight) {
    }

    private record PendingHop(Hop hop, ArcRun run) {
    }

    /** A curse on its way, and the tick it forks on, for a Blight that a cloud may fork. */
    private record PendingCurse(Curse curse, Hex hex, Dose dose, int forkAt) {

        static final int NEVER = Integer.MAX_VALUE;

        static PendingCurse of(Curse curse, Hex hex, Dose dose) {
            return new PendingCurse(curse, hex, dose, NEVER);
        }

        PendingCurse withForkAt(int tick) {
            return new PendingCurse(this.curse, this.hex, this.dose, tick);
        }

        PendingCurse unforking() {
            return this.withForkAt(NEVER);
        }
    }

    /**
     * How long a hex lasts as cast, the slow it starts from, and for one that jumped on from a dead carrier the damage
     * it recorded there.
     */
    private record Dose(int ticks, float grownSlow, boolean jumped, long stored) {

        static Dose cast(int ticks) {
            return new Dose(ticks, 0f, false, 0);
        }

        static Dose jumped(int ticks, float grownSlow, long stored) {
            return new Dose(ticks, grownSlow, true, stored);
        }
    }

    private record PendingWisp(Wisp wisp, AbilityUnits pays) {
    }

    /** What the fork has made the Burst. */
    public enum Fork {
        NONE,
        STORMCALLER,
        HEXER
    }
}
