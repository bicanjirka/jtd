package td.tower;

import td.damage.Damage;
import td.damage.DamageType;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.FieldStrength;
import td.enemy.EnemyMob;
import td.enemy.ShieldListener;
import td.tower.proc.HitAdjustment;
import td.tower.proc.HitKind;
import td.tower.pulse.ArcDischargePerk;
import td.tower.pulse.EventHorizonPerk;
import td.tower.pulse.FieldMode;
import td.tower.pulse.LongerRevealPerk;
import td.tower.pulse.ModePerk;
import td.tower.pulse.ModeSpec;
import td.tower.pulse.PulsePerk;
import td.tower.pulse.PulseSpec;
import td.tower.pulse.RevealOnEntryPerk;
import td.tower.pulse.TeslaCoilPerk;
import td.tower.pulse.TollPerk;
import td.tower.pulse.TollSpec;
import td.tower.pulse.TollTracker;
import td.tower.pulse.TrueSightPerk;
import td.tower.pulse.VisitSpec;
import td.tower.pulse.ZapSpec;
import td.tower.targeting.HighestHealthSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.NearestSelector;
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
import td.wave.WaveStartListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The field: short range, no cooldown, a little magic damage to every enemy inside every tick, invisible
 * ones included, whether or not a visible one is there to trigger it. Its worth is what it does to the
 * enemies inside for the other towers: attuned, an enemy that stays inside builds Toll, which makes every
 * effect the field holds on it stronger. Fields that overlap interfere: each lands less damage.
 * <p>
 * What each owned node does lives in a {@link PulsePerk}: the tower reads one spec from them every
 * tick.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class PulseTower extends AbstractTower {

    public static final int PRICE = 25;
    public static final float DAMAGE_POINTS = 1f;
    public static final float RANGE = 1.5f;

    /** How long a zap's bolt stays drawn. */
    private static final int ZAP_FLASH_TICKS = 8;
    private static final String FULL_TOLL_DEED = "Seconds at full Toll";
    private static final int FULL_TOLL_SECONDS_NEEDED = 30;
    /** What every other field overlapping this one takes off its damage, and the least it keeps. */
    private static final float INTERFERENCE_PER_FIELD = 0.25f;
    private static final float INTERFERENCE_FLOOR = 0.25f;

    private static final BaseSlotPerks BASE_PERKS = BaseSlotPerks.none()
            .withRangeThree(0.2f, NodeEffects.none().withModifiers(TowerModifiers.tollLinger(20)));

    private static final UpgradeNode OVERCHARGED_COILS_1 = UpgradeTier.HEAD_1.node("pulse.head.overcharged_coils.1",
            "Resonant Coils", PRICE)
            .withModifiers(TowerModifiers.tollRate(1f));
    private static final UpgradeNode OVERCHARGED_COILS_2 = UpgradeTier.HEAD_2.node("pulse.head.overcharged_coils.2",
            "Resonant Coils II", PRICE)
            .after(OVERCHARGED_COILS_1);
    private static final UpgradeNode ARC_DISCHARGE = UpgradeTier.HEAD_3.node("pulse.head.overcharged_coils.3",
            "Arc Discharge", PRICE)
            .withGate(new PurposeCondition(FULL_TOLL_DEED, FULL_TOLL_SECONDS_NEEDED))
            .after(OVERCHARGED_COILS_2);
    private static final UpgradeNode MELTDOWN = UpgradeTier.HEAD_4.node("pulse.head.overcharged_coils.4a", "Meltdown",
            PRICE)
            .withModifiers(TowerModifiers.tollCap(5))
            .after(ARC_DISCHARGE);
    private static final UpgradeNode TESLA_COIL = UpgradeTier.HEAD_4.node("pulse.head.overcharged_coils.4b",
            "Tesla Coil", PRICE)
            .after(ARC_DISCHARGE);
    private static final UpgradeNode PHASE_FIELD_1 = UpgradeTier.HEAD_1.node("pulse.head.phase_field.1",
            "Phase Field", PRICE)
            .withModifiers(TowerModifiers.range(0.3f).plus(TowerModifiers.tollLinger(20)));
    private static final UpgradeNode PHASE_FIELD_2 = UpgradeTier.HEAD_2.node("pulse.head.phase_field.2",
            "Phase Field II", PRICE)
            .withModifiers(TowerModifiers.range(0.225f))
            .after(PHASE_FIELD_1);
    private static final UpgradeNode NULL_FIELD = UpgradeTier.HEAD_3.node("pulse.head.phase_field.3", "Null Field",
            PRICE)
            .withGate(new PurposeCondition(FULL_TOLL_DEED, FULL_TOLL_SECONDS_NEEDED))
            .after(PHASE_FIELD_2);
    private static final UpgradeNode TRUE_SIGHT = UpgradeTier.HEAD_4.node("pulse.head.phase_field.4a", "True Sight",
            PRICE)
            .after(NULL_FIELD);
    private static final UpgradeNode DEAD_ZONE = UpgradeTier.HEAD_4.node("pulse.head.phase_field.4b", "Dead Zone",
            PRICE)
            .after(NULL_FIELD);
    private static final UpgradeNode CORROSION = UpgradeTier.EXTRA_1.node("pulse.extra.field_shaping.1", "Corrosion",
            PRICE);
    private static final UpgradeNode MIRROR_FIELD = UpgradeTier.EXTRA_2.node("pulse.extra.field_shaping.2",
            "Mirror Field", PRICE)
            .after(CORROSION);
    private static final UpgradeNode UNDERTOW = UpgradeTier.EXTRA_3.node("pulse.extra.field_shaping.3", "Undertow",
            PRICE)
            .after(MIRROR_FIELD);
    private static final UpgradeNode EVENT_HORIZON = UpgradeTier.EXTRA_4.node("pulse.extra.field_shaping.4",
            "Event Horizon", PRICE)
            .after(UNDERTOW);
    private static final UpgradeNode RATTLE_FIELD = UpgradeTier.SPECIAL.node("pulse.special.rattle_field",
            "Rattle Field", PRICE);
    private static final UpgradeNode SOUL_DRAIN = UpgradeTier.SPECIAL.node("pulse.special.soul_drain", "Soul Drain",
            PRICE);
    private static final UpgradeNode KILL_ZONE = UpgradeTier.SPECIAL.node("pulse.special.kill_zone", "Kill Zone",
            PRICE);


    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE, RANGE, BASE_PERKS, ARC_DISCHARGE,
            NULL_FIELD))
            .with(OVERCHARGED_COILS_1, OVERCHARGED_COILS_2, ARC_DISCHARGE, MELTDOWN, TESLA_COIL, PHASE_FIELD_1,
                    PHASE_FIELD_2, NULL_FIELD, TRUE_SIGHT, DEAD_ZONE, CORROSION, MIRROR_FIELD, UNDERTOW,
                    EVENT_HORIZON, RATTLE_FIELD, SOUL_DRAIN, KILL_ZONE)
            .withChoice(ExclusiveChoice.oneOf(OVERCHARGED_COILS_1, PHASE_FIELD_1))
            .withChoice(ExclusiveChoice.oneOf(MELTDOWN, TESLA_COIL))
            .withChoice(ExclusiveChoice.oneOf(TRUE_SIGHT, DEAD_ZONE))
            .withChoice(ExclusiveChoice.specials(RATTLE_FIELD, SOUL_DRAIN, KILL_ZONE));

    private static final PerkCatalogue<PulsePerk> PERKS = PerkCatalogue.<PulsePerk>empty()
            .with(StandardBaseSlot.ATTUNE_ID, TollPerk::new)
            .with(OVERCHARGED_COILS_2.id(), () -> new ModePerk(FieldMode.AMPLIFY))
            .with(ARC_DISCHARGE.id(), ArcDischargePerk::new)
            .with(TESLA_COIL.id(), TeslaCoilPerk::new)
            .with(PHASE_FIELD_1.id(), RevealOnEntryPerk::new)
            .with(PHASE_FIELD_2.id(), LongerRevealPerk::new)
            .with(NULL_FIELD.id(), () -> new ModePerk(FieldMode.SILENCE))
            .with(TRUE_SIGHT.id(), TrueSightPerk::new)
            .with(DEAD_ZONE.id(), () -> new ModePerk(FieldMode.DEAD_ZONE))
            .with(CORROSION.id(), () -> new ModePerk(FieldMode.CORROSION))
            .with(MIRROR_FIELD.id(), () -> new ModePerk(FieldMode.MIRROR))
            .with(UNDERTOW.id(), () -> new ModePerk(FieldMode.UNDERTOW))
            .with(EVENT_HORIZON.id(), EventHorizonPerk::new)
            .with(RATTLE_FIELD.id(), () -> new ModePerk(FieldMode.RATTLE))
            .with(SOUL_DRAIN.id(), () -> new ModePerk(FieldMode.SOUL_DRAIN))
            .with(KILL_ZONE.id(), () -> new ModePerk(FieldMode.KILL_ZONE));

    private final OwnedPerks<PulsePerk> perks = new OwnedPerks<>(PERKS);
    private final TollTracker tollTracker = new TollTracker();
    private final FireClock clock = new FireClock(0);
    private boolean fire = false;
    private int highestToll;
    private float fieldRadius;
    private float interference = 1f;
    private List<Zap> zaps = List.of();
    private int lastZapTick = Integer.MIN_VALUE;
    private List<EnemyMob> previouslyInside = List.of();
    private int deathsThisWave;
    private boolean watchingWaves;
    private final WaveStartListener waveListener = () -> this.deathsThisWave = 0;
    private boolean watchingShields;
    private boolean mirroring;
    private final ShieldListener shieldListener = this::mirrorShield;

    public PulseTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.PULSE, new TowerBaseStats(DAMAGE_POINTS, RANGE, 0)
                .with(TowerStat.TOLL_CAP, 5f).with(TowerStat.TOLL_RATE, 1f).with(TowerStat.TOLL_LINGER, 20f),
                context, x, y);
    }

    private TollSpec toll(PulseSpec spec) {
        return spec.tolls()
                ? new TollSpec(Math.round(this.stats().value(TowerStat.TOLL_CAP)), this.stats().value(TowerStat.TOLL_RATE),
                        Math.round(this.stats().value(TowerStat.TOLL_LINGER)))
                : TollSpec.none();
    }

    @Override
    protected PerkCatalogue<PulsePerk> perkCatalogue() {
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

    /** Whom the field touches and how Toll builds, as the perks in {@code owned} make it and the deaths inside widen it. */
    private PulseSpec spec(List<PulsePerk> owned) {
        PulseSpec spec = this.spec(owned, this.rangeReal());
        float growth = spec.field().growthAfter(this.deathsThisWave);
        return growth > 0f ? this.spec(owned, this.rangeReal() * (1f + growth)) : spec;
    }

    private PulseSpec spec(List<PulsePerk> owned, float radius) {
        Viewpoint view = new Viewpoint(this.centerX, this.centerY, radius, this.context.getBoard().scale());
        PulseSpec spec = PulseSpec.from(view);
        for (PulsePerk perk : owned) {
            spec = perk.refineSpec(spec);
        }
        return spec;
    }

    public void doTick(int gameTime) {
        PulseSpec spec = this.spec(this.perks.all());
        this.fieldRadius = spec.view().range();
        List<EnemyMob> inside = spec.reach().matching(this.context.enemies());
        this.fire = !inside.isEmpty();
        int shots = inside.isEmpty() ? 0 : this.clock.shotsDue();
        this.countDeaths(spec, inside);
        this.watchShields(spec);
        boolean beat = gameTime % TICKS_PER_SECOND == 0;
        if (beat) {
            this.interference = this.interferenceNow();
        }
        TollSpec toll = this.toll(spec);
        List<EnemyMob> earners = toll.isActive() ? this.tollTracker.tick(inside, toll.ticksPerStack()) : List.of();
        int highest = 0;
        for (EnemyMob enemy : inside) {
            boolean startsVisit = earners.contains(enemy) && !enemy.hasEffect(EffectKind.TOLL);
            this.addToll(enemy, toll, earners.contains(enemy));
            if (startsVisit) {
                this.startVisit(enemy, spec.visit());
            }
            int stacks = enemy.effectStacks(EffectKind.TOLL);
            this.holdInside(enemy, spec, beat, toll.isActive() && stacks >= toll.cap());
            highest = Math.max(highest, stacks);
            HitAdjustment adjustment = this.hitAdjustment(enemy, HitKind.FIELD, 0f, this.stats());
            float factor = adjustment.damageFactor() * this.interference;
            for (int hit = 0; hit < shots && !enemy.isDead(); hit++) {
                this.dealPeriodicDamage(enemy, Damage.magic(Math.round(this.damageCurrent() * factor)));
                this.rattle(enemy, spec);
            }
        }
        for (int hit = 0; hit < shots; hit++) {
            this.beginShot();
            this.clock.spend();
        }
        this.clock.advance(this.attackRate());
        this.highestToll = highest;
        if (toll.isActive() && highest >= toll.cap()) {
            this.countDeedOfSecond();
        }
        if (spec.zap().isActive() && beat && !inside.isEmpty()) {
            this.zap(spec.zap(), spec, gameTime);
        }
    }

    /** What is left of the field's damage beside the other fields that overlap it. */
    private float interferenceNow() {
        long overlapping = this.context.towers().all().stream()
                .filter(other -> other != this && this.overlaps(other, other.accept(FIELD_RADIUS)))
                .count();
        return Math.max(INTERFERENCE_FLOOR, 1f - INTERFERENCE_PER_FIELD * overlapping);
    }

    private boolean overlaps(Tower other, float otherRadius) {
        float reach = this.fieldRadius + otherRadius;
        float dx = other.getX() - this.centerX;
        float dy = other.getY() - this.centerY;
        return otherRadius > 0f && dx * dx + dy * dy < reach * reach;
    }

    /**
     * Event Horizon: every enemy that was in the field last tick and has died since widens the
     * field until the next wave starts.
     */
    private void countDeaths(PulseSpec spec, List<EnemyMob> inside) {
        if (spec.field().perDeathGrowth() > 0f) {
            if (!this.watchingWaves) {
                this.context.waves().addListener(this.waveListener);
                this.watchingWaves = true;
            }
            this.deathsThisWave += (int) this.previouslyInside.stream().filter(EnemyMob::isDead).count();
        }
        this.previouslyInside = inside;
    }

    /** Mirror Field: listens for shields taking damage, from the first tick that has the rule. */
    private void watchShields(PulseSpec spec) {
        if (spec.modes().has(FieldMode.MIRROR) && !this.watchingShields) {
            this.context.enemies().addShieldListener(this.shieldListener);
            this.watchingShields = true;
        }
    }

    /**
     * Mirror Field: what a shield takes of any hit on an enemy in the field comes back as damage no
     * shield takes a share of. What it deals itself is never mirrored again.
     */
    private void mirrorShield(EnemyMob enemy, Damage absorbed) {
        if (this.mirroring || enemy.isDead() || !this.previouslyInside.contains(enemy)) {
            return;
        }
        this.mirroring = true;
        try {
            this.dealDamage(enemy, Damage.magic(absorbed.amount()), this.stats().attack().asPeriodic().withIgnoredShields());
        } finally {
            this.mirroring = false;
        }
    }

    /**
     * The zap: the healthiest visible enemy in the field, made the Priority of every tower, and with a
     * chain the nearest visible enemies not yet struck, each within reach of the last, even outside the
     * field. Every one it strikes is Exposed, and may be Dazed.
     */
    private void zap(ZapSpec zap, PulseSpec spec, int gameTime) {
        Viewpoint view = spec.view();
        List<EnemyMob> seen = InRangeTargetQuery.visible((int) view.x(), (int) view.y(), view.range())
                .matching(this.context.enemies());
        Optional<EnemyMob> first = new HighestHealthSelector().selectFrom(seen);
        if (first.isEmpty()) {
            return;
        }
        Set<EnemyMob> struck = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Zap> bolts = new ArrayList<>();
        float fromX = this.centerX;
        float fromY = this.centerY;
        EnemyMob target = first.get();
        for (int hop = 0; hop <= zap.chains() && target != null; hop++) {
            bolts.add(new Zap(fromX, fromY, (float) target.getX(), (float) target.getY()));
            if (hop == 0) {
                this.applyEffect(target, sink -> Effect.priority(zap.markTicks(), sink));
            }
            this.strikeWithZap(target, zap);
            struck.add(target);
            fromX = (float) target.getX();
            fromY = (float) target.getY();
            target = hop < zap.chains() ? this.nextInChain(target, zap, struck) : null;
        }
        this.zaps = List.copyOf(bolts);
        this.lastZapTick = gameTime;
    }

    private EnemyMob nextInChain(EnemyMob from, ZapSpec zap, Set<EnemyMob> struck) {
        float reach = zap.chainCells() * this.context.getBoard().scale();
        List<EnemyMob> beside = InRangeTargetQuery.visible((int) from.getX(), (int) from.getY(), reach)
                .matching(this.context.enemies());
        beside.removeIf(struck::contains);
        return new NearestSelector(from.getX(), from.getY()).selectFrom(beside).orElse(null);
    }

    private void strikeWithZap(EnemyMob target, ZapSpec zap) {
        this.applyEffect(target, sink -> Effect.exposed(zap.markTicks(), sink));
        if (zap.dazeSeconds() > 0f) {
            int ticks = Math.round(zap.dazeSeconds() * TICKS_PER_SECOND);
            this.applyEffect(target, sink -> Effect.dazed(ticks, sink));
        }
    }

    /** An enemy's first Toll stack of a visit: it is revealed, and may be Dazed. */
    private void startVisit(EnemyMob enemy, VisitSpec visit) {
        if (visit.revealTicks() > 0) {
            this.reveal(enemy, visit.revealTicks());
        }
        if (visit.dazeTicks() > 0) {
            this.applyEffect(enemy, sink -> Effect.dazed(visit.dazeTicks(), sink));
        }
    }

    /**
     * The rules that hold while an enemy is inside: each lasts a moment, refreshed every tick it
     * stays, and those with a number hold it at the strength its Toll gives. Soul Drain also takes
     * some spirit each second, and at none left, heals and shields too.
     */
    private void holdInside(EnemyMob enemy, PulseSpec spec, boolean beat, boolean fullToll) {
        ModeSpec modes = spec.modes();
        int strength = FieldStrength.at(enemy.effectStacks(EffectKind.TOLL));
        if (modes.has(FieldMode.SILENCE)) {
            this.applyEffect(enemy, sink -> Effect.silenced(FieldMode.LINGER_TICKS, sink));
        }
        if (modes.has(FieldMode.DEAD_ZONE)
                || modes.has(FieldMode.SOUL_DRAIN) && enemy.spirit() <= FieldMode.SOUL_DRAIN_SPIRIT_FLOOR) {
            this.applyEffect(enemy, sink -> Effect.deadZone(FieldMode.LINGER_TICKS, sink));
        }
        if (modes.has(FieldMode.CORROSION)) {
            int added = topUp(enemy, EffectKind.CORRODED, strength);
            this.applyEffect(enemy, sink -> Effect.corroded(added, FieldMode.LINGER_TICKS, sink));
        }
        if (modes.has(FieldMode.UNDERTOW)) {
            int added = topUp(enemy, EffectKind.UNDERTOW, strength);
            this.applyEffect(enemy, sink -> Effect.undertow(added, FieldMode.LINGER_TICKS, sink));
            this.applyEffect(enemy, sink -> Effect.anchored(FieldMode.LINGER_TICKS, sink));
        }
        if (modes.has(FieldMode.KILL_ZONE)) {
            int added = topUp(enemy, EffectKind.KILL_ZONE, strength);
            this.applyEffect(enemy, sink -> Effect.killZone(added, FieldMode.LINGER_TICKS, sink));
        }
        if (modes.has(FieldMode.AMPLIFY) && fullToll) {
            int added = topUp(enemy, EffectKind.VULNERABLE, FieldMode.AMPLIFY_STACKS);
            this.applyEffect(enemy, sink -> Effect.vulnerable(added, FieldMode.LINGER_TICKS, sink));
        }
        if (beat && modes.has(FieldMode.SOUL_DRAIN)) {
            this.applyEffect(enemy, sink -> Effect.sickened(FieldMode.SOUL_DRAIN_SPIRIT_PER_SECOND));
        }
    }

    /** What to add to {@code enemy}'s {@code kind} to bring it to {@code strength}: nothing, a refresh, if it holds as much. */
    private static int topUp(EnemyMob enemy, EffectKind kind, int strength) {
        return Math.max(0, strength - enemy.effectStacks(kind));
    }

    /** A stack for an enemy that has earned one; for one that holds some, the refresh that keeps them. */
    private void addToll(EnemyMob enemy, TollSpec toll, boolean earned) {
        if (!toll.isActive() || !(earned || enemy.hasEffect(EffectKind.TOLL))) {
            return;
        }
        int stacks = earned ? 1 : 0;
        this.applyEffect(enemy, sink -> Effect.toll(stacks, toll.fadeTicks(), sink).withStackCap(toll.cap()));
    }

    /** Rattle Field: now and then a stack of Sundered or of Exposed, whichever the coin says. */
    private void rattle(EnemyMob enemy, PulseSpec spec) {
        if (!spec.modes().has(FieldMode.RATTLE) || this.context.random().nextDouble() >= FieldMode.RATTLE_CHANCE) {
            return;
        }
        if (this.context.random().nextIndex(2) == 0) {
            this.applyStacks(enemy, EffectKind.SUNDERED, 1);
        } else {
            this.applyEffect(enemy, sink -> Effect.exposed(Math.round(FieldMode.RATTLE_EXPOSED_SECONDS * TICKS_PER_SECOND), sink));
        }
    }

    public boolean isFiring() {
        return this.fire;
    }

    /** The bolts of the latest zap, from the tower to its first target and on along its chain. */
    public List<Zap> getZaps() {
        return this.zaps;
    }

    /** How many ticks ago it last zapped; a large number before the first. */
    public int ticksSinceZap(int gameTime) {
        return this.lastZapTick == Integer.MIN_VALUE ? Integer.MAX_VALUE : gameTime - this.lastZapTick;
    }

    /** How many ticks a zap's bolt stays drawn. */
    public int zapFlashTicks() {
        return ZAP_FLASH_TICKS;
    }

    @Override
    public void doCleanup() {
        super.doCleanup();
        this.context.waves().removeListener(this.waveListener);
        this.context.enemies().removeShieldListener(this.shieldListener);
    }

    /** What the field's colour says about its rules: Null Field violet, Undertow blue, Corrosion green. */
    public FieldLook getFieldLook() {
        PulseSpec spec = this.spec(this.perks.all());
        if (spec.modes().has(FieldMode.SILENCE)) {
            return FieldLook.NULL;
        }
        if (spec.modes().has(FieldMode.UNDERTOW)) {
            return FieldLook.UNDERTOW;
        }
        return spec.modes().has(FieldMode.CORROSION) ? FieldLook.CORROSION : FieldLook.PLAIN;
    }

    /** How far the field reached at the last tick, in board pixels: its range, widened by the deaths inside. */
    public float getFieldRadius() {
        return this.fieldRadius;
    }

    /** The most Toll stacks any enemy in the field held at the last tick. */
    public int getHighestToll() {
        return this.highestToll;
    }

    @Override
    protected DamageType damageType() {
        return DamageType.MAGIC;
    }

    @Override
    protected List<BehaviourLine> behaviours() {
        PulseSpec spec = this.spec(this.perks.all());
        List<BehaviourLine> lines = new ArrayList<>();
        lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Hits", "all in range"));
        TollSpec toll = this.toll(spec);
        if (toll.isActive()) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Toll", "up to " + toll.cap() + ", effects +10% each"));
        }
        if (spec.visit().revealTicks() > 0) {
            lines.add(new BehaviourLine(BehaviourMarker.REVEAL, "Reveals on Toll",
                    BehaviourLine.seconds(spec.visit().revealTicks())));
        }
        if (spec.visit().dazeTicks() > 0) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Dazes on Toll",
                    BehaviourLine.seconds(spec.visit().dazeTicks())));
        }
        for (FieldMode mode : FieldMode.values()) {
            if (spec.modes().has(mode)) {
                lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Inside", insideText(mode)));
            }
        }
        if (spec.field().perDeathGrowth() > 0f) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Field widened",
                    "+" + Math.round(100 * spec.field().growthAfter(this.deathsThisWave)) + "%"));
        }
        if (this.interference < 1f) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Interference",
                    "-" + Math.round(100 * (1f - this.interference)) + "% damage"));
        }
        this.perks.all().forEach(perk -> perk.behaviour().ifPresent(lines::add));
        return lines;
    }

    private static String insideText(FieldMode mode) {
        return switch (mode) {
            case SILENCE -> "silenced";
            case DEAD_ZONE -> "no heals or shields";
            case CORROSION -> "armor lowered";
            case MIRROR -> "shields reflect";
            case UNDERTOW -> "chilled, anchored";
            case KILL_ZONE -> "take more damage";
            case AMPLIFY -> "Vulnerable at full Toll";
            case SOUL_DRAIN -> "spirit drained";
            case RATTLE -> "sundered or exposed";
        };
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitPulseTower(this);
    }

    /** How far a tower's field reaches, in board pixels; {@code 0} for a tower with no field. */
    private static final TowerVisitor<Float> FIELD_RADIUS = new TowerVisitor<>() {
        @Override
        public Float visitSniperTower(SniperTower tower) {
            return 0f;
        }

        @Override
        public Float visitSplashTower(SplashTower tower) {
            return 0f;
        }

        @Override
        public Float visitSonarTower(SonarTower tower) {
            return 0f;
        }

        @Override
        public Float visitPulseTower(PulseTower tower) {
            return tower.fieldRadius;
        }

        @Override
        public Float visitAuraTower(AuraTower tower) {
            return 0f;
        }

        @Override
        public Float visitMortarTower(MortarTower tower) {
            return 0f;
        }

        @Override
        public Float visitSeekerTower(SeekerTower tower) {
            return 0f;
        }

        @Override
        public Float visitCinderTower(CinderTower tower) {
            return 0f;
        }
    };

    /** What the field's colour says about its rules. */
    public enum FieldLook {
        PLAIN,
        NULL,
        UNDERTOW,
        CORROSION
    }

    /** One bolt of a zap, in board pixels. */
    public record Zap(float fromX, float fromY, float toX, float toY) {
    }
}
