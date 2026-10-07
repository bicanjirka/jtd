package td.tower;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.PoolTuning;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.cinder.BellowsPerk;
import td.tower.cinder.CauterizePerk;
import td.tower.cinder.CinderPerk;
import td.tower.cinder.CinderSpec;
import td.tower.cinder.CombustionPerk;
import td.tower.cinder.CombustionSpec;
import td.tower.cinder.DragonsBreathPerk;
import td.tower.cinder.EverburnPerk;
import td.tower.cinder.FlameLook;
import td.tower.cinder.FlashpointPerk;
import td.tower.cinder.HeatPerk;
import td.tower.cinder.InfernoRingPerk;
import td.tower.cinder.KindlingPerk;
import td.tower.cinder.LingeringFlamesPerk;
import td.tower.cinder.LongNozzlePerk;
import td.tower.cinder.NozzleWidthPerk;
import td.tower.cinder.PyromancersMarkPerk;
import td.tower.cinder.SearingFlamePerk;
import td.tower.cinder.ShortBurnPerk;
import td.tower.cinder.SoulfirePerk;
import td.tower.cinder.StokePerk;
import td.tower.cinder.ThermalShockPerk;
import td.tower.cinder.WhiteFlamePerk;
import td.tower.cinder.WideNozzlePerk;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.InWedgeTargetQuery;
import td.tower.targeting.NearestSelector;
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
import td.zone.Zone;
import td.zone.ZoneKind;
import td.zone.ZoneOwner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The Pyre: a cooldown-gated flame cone that turns slowly toward the nearest enemy and fires only once
 * an enemy is inside the cone. Each shot is a wave travelling outward over several ticks, setting an
 * enemy alight once when its front reaches it. Never aims at an invisible enemy, but burns one caught
 * in a wave.
 * <p>
 * A wave's heading and width are fixed when it fires. The tower never deals direct damage: its burns
 * credit it through {@code dealPeriodicDamage}, and a burn it starts never crits, though an ignition
 * can, which starts the pool stronger. What each owned node does lives in a {@link CinderPerk}.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class CinderTower extends AbstractTower {

    public static final int PRICE = 28;
    public static final float DAMAGE_POINTS = 1.5f;
    public static final float RANGE = 2.2f;
    /** Ticks between shots before buffs. */
    public static final int COOLDOWN_MAX = 20;
    /** Ticks a wave takes to reach full range. */
    public static final int WAVE_TRAVEL_TICKS = 10;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.15;
    private static final double HALF_WIDTH_RADIANS_BASE = 0.35;
    private static final int BURN_DURATION_TICKS_BASE = 60;
    /** Searing Flame makes an enemy Vulnerable at most this often, in ticks. */
    private static final int SEARING_INTERVAL_TICKS = 20;
    private static final float THERMAL_SHOCK_RADIUS_CELLS = 1.5f;
    private static final float THERMAL_SHOCK_CHILL = 0.4f;
    private static final int THERMAL_SHOCK_CHILL_TICKS = 40;
    /** A pool this close to its cap counts as full. */
    private static final float CAP_REACHED_SHARE = 0.97f;
    /** A capped pool must fall under this share of its cap before it can burst again. */
    private static final float CAP_RESET_SHARE = 0.5f;

    private static final String STOKE_DEED = "Stoking waves";
    private static final int STOKES_NEEDED = 45;

    private static final BaseSlotPerks BASE_PERKS = BaseSlotPerks.none()
            .withAttune("Stoke: a wave on an enemy already burning from this tower raises its burn 10%, up to 3 "
                    + "times; the stacks go when the burn ends")
            .withAwaken("Bellows: an Aura's fire-rate buff on this tower also widens its cone 10% per +10%")
            .withRangeThree(0.2f, "Long Nozzle: the wave travels twice as fast");

    private static final UpgradeNode WHITE_FLAME_1 = UpgradeTier.HEAD_1.node("cinder.head.white_flame.1",
            "White Flame", PRICE)
            .withBuff(TowerBuff.damage(0.3f))
            .withExtraEffect("Stoke steps +15%");
    private static final UpgradeNode WHITE_FLAME_2 = UpgradeTier.HEAD_2.node("cinder.head.white_flame.2",
            "White Flame II", PRICE)
            .withBuff(TowerBuff.damage(0.5f).withCritChance(0.1f))
            .withExtraEffect("-25% burn duration; the crit chance is for ignitions")
            .after(WHITE_FLAME_1);
    private static final UpgradeNode SOULFIRE = UpgradeTier.HEAD_3.node("cinder.head.white_flame.3", "Soulfire",
            PRICE)
            .withGate(new PurposeCondition(STOKE_DEED, STOKES_NEEDED))
            .withExtraEffect("every other wave is Soulfire: a blue burn in its own pool that stacks with ordinary "
                    + "burn, earns Sickened as well as Scorched and burns the burn-immune; Stoke applies to both")
            .after(WHITE_FLAME_2);
    private static final UpgradeNode FLASHPOINT = UpgradeTier.HEAD_4.node("cinder.head.white_flame.4a",
            "Flashpoint", PRICE)
            .withBuff(TowerBuff.critChance(0.2f))
            .withExtraEffect("a critical ignition also adds 3 Scorched")
            .after(SOULFIRE);
    private static final UpgradeNode COMBUSTION = UpgradeTier.HEAD_4.node("cinder.head.white_flame.4b",
            "Combustion", PRICE)
            .withExtraEffect("the pool cap doubles; a pool that reaches its cap bursts for half of it onto enemies "
                    + "within 1 cell")
            .after(SOULFIRE);

    private static final UpgradeNode WIDE_NOZZLE_1 = UpgradeTier.HEAD_1.node("cinder.head.wide_nozzle.1",
            "Wide Nozzle", PRICE)
            .withBuff(TowerBuff.range(0.25f))
            .withExtraEffect("+30% cone width; an enemy that leaves the cone keeps its Stoke for 2s");
    private static final UpgradeNode WIDE_NOZZLE_2 = UpgradeTier.HEAD_2.node("cinder.head.wide_nozzle.2",
            "Wide Nozzle II", PRICE)
            .withBuff(TowerBuff.range(0.2f).withFireRate(0.2f))
            .withExtraEffect("+20% cone width")
            .after(WIDE_NOZZLE_1);
    private static final UpgradeNode LINGERING_FLAMES = UpgradeTier.HEAD_3.node("cinder.head.wide_nozzle.3",
            "Lingering Flames", PRICE)
            .withGate(new PurposeCondition(STOKE_DEED, STOKES_NEEDED))
            .withExtraEffect("each wave leaves a patch of burning ground where its target stands, for 2s")
            .after(WIDE_NOZZLE_2);
    private static final UpgradeNode INFERNO_RING = UpgradeTier.HEAD_4.node("cinder.head.wide_nozzle.4a",
            "Inferno Ring", PRICE)
            .withBuff(TowerBuff.range(-0.25f))
            .withExtraEffect("the cone becomes a full ring")
            .after(LINGERING_FLAMES);
    private static final UpgradeNode DRAGONS_BREATH = UpgradeTier.HEAD_4.node("cinder.head.wide_nozzle.4b",
            "Dragon's Breath", PRICE)
            .withBuff(TowerBuff.range(0.3f).withFireRate(2f / 3f).withDamage(-0.6f))
            .withExtraEffect("a continuous stream that keeps every pool topped up and Stoke always full")
            .after(LINGERING_FLAMES);

    private static final UpgradeNode KINDLING = UpgradeTier.EXTRA_1.node("cinder.extra.fuel.1", "Kindling", PRICE)
            .withExtraEffect("burning enemies earn Scorched twice as fast");
    private static final UpgradeNode CAUTERIZE = UpgradeTier.EXTRA_2.node("cinder.extra.fuel.2", "Cauterize", PRICE)
            .withExtraEffect("burning enemies receive 50% less healing and shielding")
            .after(KINDLING);
    private static final UpgradeNode HEAT = UpgradeTier.EXTRA_3.node("cinder.extra.fuel.3", "Heat", PRICE)
            .withExtraEffect("burning enemies take +10% damage over time from every source")
            .after(CAUTERIZE);
    private static final UpgradeNode EVERBURN = UpgradeTier.EXTRA_4.node("cinder.extra.fuel.4", "Everburn", PRICE)
            .withExtraEffect("a pool never decays below 25% while its enemy is in this tower's range")
            .after(HEAT);

    private static final UpgradeNode SEARING_FLAME = UpgradeTier.SPECIAL.node("cinder.special.searing_flame",
            "Searing Flame", PRICE)
            .withExtraEffect("an ignition grants Vulnerable, and so does every wave that hits a burning enemy, at "
                    + "most one stack a second");
    private static final UpgradeNode THERMAL_SHOCK = UpgradeTier.SPECIAL.node("cinder.special.thermal_shock",
            "Thermal Shock", PRICE)
            .withExtraEffect("when a burning enemy is frozen by anything, its remaining pool detonates at 150% "
                    + "instead of 50% and chills its neighbours");
    private static final UpgradeNode PYROMANCERS_MARK = UpgradeTier.SPECIAL.node("cinder.special.pyromancers_mark",
            "Pyromancer's Mark", PRICE)
            .withExtraEffect("burning enemies take +15% magic damage from every source");

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE, BASE_PERKS, SOULFIRE,
                    LINGERING_FLAMES))
            .with(WHITE_FLAME_1, WHITE_FLAME_2, SOULFIRE, FLASHPOINT, COMBUSTION,
                    WIDE_NOZZLE_1, WIDE_NOZZLE_2, LINGERING_FLAMES, INFERNO_RING, DRAGONS_BREATH,
                    KINDLING, CAUTERIZE, HEAT, EVERBURN,
                    SEARING_FLAME, THERMAL_SHOCK, PYROMANCERS_MARK)
            .withChoice(ExclusiveChoice.oneOf(WHITE_FLAME_1, WIDE_NOZZLE_1))
            .withChoice(ExclusiveChoice.oneOf(FLASHPOINT, COMBUSTION))
            .withChoice(ExclusiveChoice.oneOf(INFERNO_RING, DRAGONS_BREATH))
            .withChoice(ExclusiveChoice.specials(SEARING_FLAME, THERMAL_SHOCK, PYROMANCERS_MARK));

    private static final PerkCatalogue<CinderPerk> PERKS = PerkCatalogue.<CinderPerk>empty()
            .with(StandardBaseSlot.ATTUNE_ID, StokePerk::new)
            .with(StandardBaseSlot.AWAKEN_ID, BellowsPerk::new)
            .with(StandardBaseSlot.RANGE_3_ID, LongNozzlePerk::new)
            .with(WHITE_FLAME_1.id(), WhiteFlamePerk::new)
            .with(WHITE_FLAME_2.id(), ShortBurnPerk::new)
            .with(SOULFIRE.id(), SoulfirePerk::new)
            .with(FLASHPOINT.id(), FlashpointPerk::new)
            .with(COMBUSTION.id(), CombustionPerk::new)
            .with(WIDE_NOZZLE_1.id(), WideNozzlePerk::new)
            .with(WIDE_NOZZLE_2.id(), NozzleWidthPerk::new)
            .with(LINGERING_FLAMES.id(), LingeringFlamesPerk::new)
            .with(INFERNO_RING.id(), InfernoRingPerk::new)
            .with(DRAGONS_BREATH.id(), DragonsBreathPerk::new)
            .with(KINDLING.id(), KindlingPerk::new)
            .with(CAUTERIZE.id(), CauterizePerk::new)
            .with(HEAT.id(), HeatPerk::new)
            .with(EVERBURN.id(), EverburnPerk::new)
            .with(SEARING_FLAME.id(), SearingFlamePerk::new)
            .with(THERMAL_SHOCK.id(), ThermalShockPerk::new)
            .with(PYROMANCERS_MARK.id(), PyromancersMarkPerk::new);

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private final OwnedPerks<CinderPerk> perks = new OwnedPerks<>(PERKS);
    private final List<FlameWave> inFlightWaves = new ArrayList<>();
    /** The enemies this tower has set alight and still tracks, by identity. */
    private final Map<EnemyMob, Burning> burning = new IdentityHashMap<>();
    private final ZoneOwner zoneOwner = this::applyEffect;
    private final FireClock clock = new FireClock(COOLDOWN_MAX);
    private int wavesFired;
    private int tickNow;

    public CinderTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.CINDER, new TowerBaseStats(DAMAGE_POINTS, RANGE, COOLDOWN_MAX), context, x, y);
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        this.perks.add(node);
    }

    /** How the cone and the burn are shaped, as the perks in {@code owned} make them. */
    private CinderSpec spec(List<CinderPerk> owned) {
        Viewpoint view = new Viewpoint(this.centerX, this.centerY, this.rangeReal(), this.context.getBoard().scale());
        CinderSpec spec = CinderSpec.from(view);
        for (CinderPerk perk : owned) {
            spec = perk.refineSpec(spec);
        }
        return spec;
    }

    /** What every Aura beside this tower adds to its fire rate. */
    private float fireRateFromAuras() {
        float bonus = 0f;
        for (Tower other : this.context.towers().all()) {
            if (other != this) {
                bonus += other.buffFor(this).fireRateBonus();
            }
        }
        return Math.max(0f, bonus);
    }

    private double halfWidthRadians(CinderSpec spec) {
        if (spec.ring()) {
            return Math.PI;
        }
        double bellows = spec.bellows() ? 1.0 + this.fireRateFromAuras() : 1.0;
        return HALF_WIDTH_RADIANS_BASE * spec.coneScale() * bellows;
    }

    private int burnDuration(CinderSpec spec) {
        return Math.max(1, Math.round(BURN_DURATION_TICKS_BASE * spec.burnScale()));
    }

    public void doTick(int gameTime) {
        this.tickNow = gameTime;
        CinderSpec spec = this.spec(this.perks.all());
        List<EnemyMob> inRange = spec.reach().matching(this.context.enemies());
        Optional<EnemyMob> nearest = PreferringSelector.priority(new NearestSelector(this.centerX, this.centerY))
                .selectFrom(inRange);
        nearest.ifPresent(enemy -> this.turretAim.tick(TurretAim.angleTo(this.centerX, this.centerY, enemy.getX(),
                enemy.getY())));

        if (this.clock.isReady() && this.hasEnemyAhead(spec)) {
            this.fire(spec, nearest.orElse(null), gameTime);
            this.clock.spend();
        }
        this.clock.advance(this.fireRateCurrent());

        this.advanceWaves(spec, gameTime);
        this.tendBurning(spec);
    }

    /** Whether a wave fired at the turret's current heading would reach at least one enemy. */
    private boolean hasEnemyAhead(CinderSpec spec) {
        if (spec.ring()) {
            return !spec.reach().matching(this.context.enemies()).isEmpty();
        }
        return !new InWedgeTargetQuery(this.centerX, this.centerY, this.turretAim.currentRadians(),
                this.halfWidthRadians(spec))
                .and(InRangeTargetQuery.visible(this.centerX, this.centerY, this.rangeReal()))
                .matching(this.context.enemies())
                .isEmpty();
    }

    private void fire(CinderSpec spec, EnemyMob target, int gameTime) {
        this.wavesFired++;
        boolean soul = spec.soulfire() && this.wavesFired % 2 == 0;
        int travelTicks = Math.max(1, Math.round(WAVE_TRAVEL_TICKS / spec.waveSpeed()));
        this.inFlightWaves.add(new FlameWave(this.turretAim.currentRadians(), this.halfWidthRadians(spec), gameTime,
                travelTicks, soul, spec.look()));
        if (spec.linger().isActive() && target != null) {
            int scale = this.context.getBoard().scale();
            this.context.zones().add(new Zone(ZoneKind.BURNING_GROUND, target.getX(), target.getY(),
                    spec.linger().radiusCells() * scale, spec.linger().ticks(), this.damageCurrent(),
                    this.zoneOwner));
        }
    }

    /**
     * Advances every wave, setting alight the enemies its front reaches for the first time; each wave
     * reaches an enemy at most once. Drops waves that reached full range.
     */
    private void advanceWaves(CinderSpec spec, int gameTime) {
        Iterator<FlameWave> waves = this.inFlightWaves.iterator();
        while (waves.hasNext()) {
            FlameWave wave = waves.next();
            float travelled = Math.min(1f, (float) (gameTime - wave.firedAtTick) / wave.travelTicks);
            float currentRadius = travelled * this.rangeReal();
            List<EnemyMob> caught = new InWedgeTargetQuery(this.centerX, this.centerY, wave.headingRadians,
                    wave.halfWidthRadians)
                    .and(InRangeTargetQuery.everyone(this.centerX, this.centerY, currentRadius))
                    .matching(this.context.enemies());
            boolean stoked = false;
            for (EnemyMob enemy : caught) {
                if (wave.alreadyHit.add(enemy)) {
                    stoked |= this.ignite(enemy, wave, spec, gameTime);
                }
            }
            if (stoked) {
                this.countDeedOfAttack();
            }
            if (travelled >= 1f) {
                waves.remove();
            }
        }
    }

    /** One enemy a wave reaches: Stoke, the crit, the pool, and what the specials add. Whether it was stoked. */
    private boolean ignite(EnemyMob enemy, FlameWave wave, CinderSpec spec, int gameTime) {
        Burning entry = this.burning.get(enemy);
        boolean stoked = entry != null && spec.stoke().active();
        boolean alreadyBurning = entry != null;
        if (entry == null) {
            entry = new Burning(gameTime);
            this.burning.put(enemy, entry);
        }
        if (spec.stoke().active()) {
            entry.steps = spec.stoke().alwaysFull() ? spec.stoke().maxSteps()
                    : Math.min(spec.stoke().maxSteps(), entry.steps + (stoked ? 1 : 0));
        }
        AttackProfile attack = this.stats().attack();
        boolean critical = enemy.rollsCrit(attack);
        float potency = (1f + (spec.stoke().active() ? spec.stoke().step() * entry.steps : 0f))
                * (critical ? enemy.critFactorFor(attack) : 1f);
        Damage level = Damage.magic(Math.round(this.damageCurrent() * potency));
        int duration = this.burnDuration(spec);
        PoolTuning tuning = spec.tuning();
        if (wave.soul) {
            this.applyEffect(enemy, sink -> Effect.soulfire(level, duration, sink).withTuning(tuning));
        } else {
            this.applyEffect(enemy, sink -> Effect.burn(level, duration, sink).withTuning(tuning));
        }
        entry.lastBurningTick = gameTime;
        if (critical && spec.critScorch() > 0) {
            this.applyEffect(enemy, sink -> Effect.scorched(spec.critScorch()));
        }
        if (spec.searing() && (!alreadyBurning || gameTime - entry.lastVulnerableTick >= SEARING_INTERVAL_TICKS)) {
            this.applyStacks(enemy, EffectKind.VULNERABLE, 1);
            entry.lastVulnerableTick = gameTime;
        }
        if (spec.combustion().isActive()) {
            this.combust(enemy, entry, spec);
        }
        return stoked;
    }

    /** A pool that has reached its cap bursts once onto the enemies near it, and may burst again after it falls. */
    private void combust(EnemyMob enemy, Burning entry, CinderSpec spec) {
        for (Effect pool : enemy.activeEffects()) {
            if (!pool.kind().isBurning()) {
                continue;
            }
            float cap = pool.tuning().capFactor() * pool.peakL0();
            if (!entry.capped && pool.fuelLevel() >= cap * CAP_REACHED_SHARE) {
                entry.capped = true;
                this.burstOnto(enemy, Math.round(pool.remainingPoolDamage() * spec.combustion().share()),
                        spec.combustion().radiusCells());
            } else if (entry.capped && pool.fuelLevel() < cap * CAP_RESET_SHARE) {
                entry.capped = false;
            }
        }
    }

    private void burstOnto(EnemyMob centre, int damage, float radiusCells) {
        float radius = radiusCells * this.context.getBoard().scale();
        for (EnemyMob other : InRangeTargetQuery.everyone((int) centre.getX(), (int) centre.getY(), radius)
                .matching(this.context.enemies())) {
            if (other != centre) {
                this.dealPeriodicDamage(other, Damage.magic(damage));
            }
        }
    }

    /**
     * Keeps track of the enemies it burns: forgets the ones whose fire has been out too long, tops up
     * pools Everburn holds, and lets Thermal Shock chill the neighbours of one that froze while it burned.
     */
    private void tendBurning(CinderSpec spec) {
        Iterator<Map.Entry<EnemyMob, Burning>> entries = this.burning.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<EnemyMob, Burning> tracked = entries.next();
            EnemyMob enemy = tracked.getKey();
            Burning entry = tracked.getValue();
            boolean onFire = enemy.hasEffect(EffectKind.BURN) || enemy.hasEffect(EffectKind.SOULFIRE);
            if (enemy.isDead()) {
                entries.remove();
                continue;
            }
            if (onFire) {
                entry.lastBurningTick = this.tickNow;
                entry.wasBurning = true;
                if (spec.everburnFloor() > 0f) {
                    this.holdPools(enemy, spec);
                }
                continue;
            }
            if (entry.wasBurning && spec.thermalShock() && enemy.hasEffect(EffectKind.FREEZE)) {
                this.chillAround(enemy);
            }
            entry.wasBurning = false;
            if (this.tickNow - entry.lastBurningTick > spec.stoke().graceTicks()) {
                entries.remove();
            }
        }
    }

    /** Everburn: a pool in this tower's range is held up to its floor with a top-up that marks nothing. */
    private void holdPools(EnemyMob enemy, CinderSpec spec) {
        double dx = enemy.getX() - this.centerX;
        double dy = enemy.getY() - this.centerY;
        if (dx * dx + dy * dy > this.rangeReal2()) {
            return;
        }
        for (Effect pool : enemy.activeEffects()) {
            if (!pool.kind().isBurning()) {
                continue;
            }
            float floor = spec.everburnFloor() * pool.peakL0();
            float cap = pool.tuning().capFactor() * pool.peakL0();
            if (pool.fuelLevel() < floor && pool.fuelLevel() < cap) {
                int topUp = Math.round((floor - pool.fuelLevel()) / (1f - pool.fuelLevel() / cap));
                int duration = pool.authoredDurationTicks();
                PoolTuning tuning = pool.tuning();
                EffectKind kind = pool.kind();
                this.applyEffect(enemy, sink -> kind == EffectKind.SOULFIRE
                        ? Effect.soulfire(Damage.magic(topUp), duration, sink).withTuning(tuning)
                        : Effect.burn(Damage.magic(topUp), duration, sink).withTuning(tuning));
            }
        }
    }

    private void chillAround(EnemyMob frozen) {
        float radius = THERMAL_SHOCK_RADIUS_CELLS * this.context.getBoard().scale();
        for (EnemyMob other : InRangeTargetQuery.everyone((int) frozen.getX(), (int) frozen.getY(), radius)
                .matching(this.context.enemies())) {
            if (other != frozen) {
                this.applyEffect(other, sink -> Effect.chill(THERMAL_SHOCK_CHILL, THERMAL_SHOCK_CHILL_TICKS, sink));
            }
        }
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    public double getHalfWidthRadians() {
        return this.halfWidthRadians(this.spec(this.perks.all()));
    }

    /** Waves still travelling, oldest first. */
    public List<FlameWave> getInFlightWaves() {
        return Collections.unmodifiableList(this.inFlightWaves);
    }

    /** How many steps of Stoke {@code enemy} carries now; none when it is not being stoked. */
    int stokeStepsOf(EnemyMob enemy) {
        Burning entry = this.burning.get(enemy);
        return entry == null ? 0 : entry.steps;
    }

    @Override
    protected DamageType damageType() {
        return DamageType.MAGIC;
    }

    @Override
    protected List<BehaviourLine> behaviours() {
        CinderSpec spec = this.spec(this.perks.all());
        List<BehaviourLine> lines = new ArrayList<>();
        lines.add(new BehaviourLine(BehaviourMarker.BURN, "Burns", BehaviourLine.seconds(this.burnDuration(spec))));
        lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Aims at", "nearest"));
        if (spec.stoke().active()) {
            lines.add(new BehaviourLine(BehaviourMarker.BURN, "Stoke",
                    "+" + Math.round(spec.stoke().step() * 100) + "% a wave, x" + spec.stoke().maxSteps()));
        }
        if (spec.bellows()) {
            lines.add(new BehaviourLine(BehaviourMarker.BUFF, "Bellows", "fire rate widens it"));
        }
        if (spec.ring()) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Wave", "a full ring"));
        }
        if (spec.soulfire()) {
            lines.add(new BehaviourLine(BehaviourMarker.BURN, "Every other wave", "soulfire"));
        }
        if (spec.linger().isActive()) {
            lines.add(new BehaviourLine(BehaviourMarker.BURN, "Each wave leaves", "burning ground"));
        }
        if (spec.searing()) {
            lines.add(new BehaviourLine(BehaviourMarker.VULNERABLE, "Burns apply", "vulnerable"));
        }
        if (spec.thermalShock()) {
            lines.add(new BehaviourLine(BehaviourMarker.FREEZE, "A frozen burn", "150%, chills nearby"));
        }
        if (spec.critScorch() > 0) {
            lines.add(new BehaviourLine(BehaviourMarker.BURN, "A crit ignition", "+" + spec.critScorch() + " scorched"));
        }
        CombustionSpec combustion = spec.combustion();
        if (combustion.isActive()) {
            lines.add(new BehaviourLine(BehaviourMarker.BURN, "A full pool", "bursts for " + BehaviourLine.percent(combustion.share())));
        }
        PoolTuning tuning = spec.tuning();
        if (tuning.stackRate() > 1) {
            lines.add(new BehaviourLine(BehaviourMarker.BURN, "Scorches", "x" + tuning.stackRate() + " as fast"));
        }
        if (tuning.cauterizes()) {
            lines.add(new BehaviourLine(BehaviourMarker.BURN, "Burning enemies", "heal 50% less"));
        }
        if (tuning.heats()) {
            lines.add(new BehaviourLine(BehaviourMarker.BURN, "Burning enemies", "+10% over time"));
        }
        if (tuning.marksForMagic()) {
            lines.add(new BehaviourLine(BehaviourMarker.BURN, "Burning enemies", "+15% magic"));
        }
        if (spec.everburnFloor() > 0f) {
            lines.add(new BehaviourLine(BehaviourMarker.BURN, "A pool in range", "holds at 25%"));
        }
        return lines;
    }

    @Override
    protected String description() {
        return "Fires a cone of flame that sets everything in it alight.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitCinderTower(this);
    }

    /** What the Cinder remembers of an enemy it has set alight. */
    private static final class Burning {
        private int steps;
        private int lastBurningTick;
        private int lastVulnerableTick = Integer.MIN_VALUE / 2;
        private boolean wasBurning;
        private boolean capped;

        private Burning(int tick) {
            this.lastBurningTick = tick;
        }
    }

    /**
     * One shot in flight: its fixed heading and width, fire time, how long it takes to reach full
     * range, whether it is Soulfire, its look, and the enemies it has already burned.
     */
    public static final class FlameWave {
        private final double headingRadians;
        private final double halfWidthRadians;
        private final int firedAtTick;
        private final int travelTicks;
        private final boolean soul;
        private final FlameLook look;
        private final Set<EnemyMob> alreadyHit = new HashSet<>();

        private FlameWave(double headingRadians, double halfWidthRadians, int firedAtTick, int travelTicks,
                          boolean soul, FlameLook look) {
            this.headingRadians = headingRadians;
            this.halfWidthRadians = halfWidthRadians;
            this.firedAtTick = firedAtTick;
            this.travelTicks = travelTicks;
            this.soul = soul;
            this.look = look;
        }

        public double headingRadians() {
            return this.headingRadians;
        }

        public double halfWidthRadians() {
            return this.halfWidthRadians;
        }

        public int firedAtTick() {
            return this.firedAtTick;
        }

        /** Ticks this wave takes to reach full range. */
        public int travelTicks() {
            return this.travelTicks;
        }

        /** Whether this wave burns as Soulfire. */
        public boolean isSoulfire() {
            return this.soul;
        }

        public FlameLook look() {
            return this.look;
        }
    }
}
