package td.tower;

import td.damage.Damage;
import td.damage.DamageType;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.FieldStrength;
import td.enemy.EnemyMob;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.proc.HitAdjustment;
import td.tower.proc.HitKind;
import td.tower.pulse.ArcDischargeMechanic;
import td.tower.pulse.EventHorizonMechanic;
import td.tower.pulse.FieldHolds;
import td.tower.pulse.FieldMode;
import td.tower.pulse.GravityWellMechanic;
import td.tower.pulse.HoldSpec;
import td.tower.pulse.LeyLine;
import td.tower.pulse.LeyLineMechanic;
import td.tower.pulse.LineSpec;
import td.tower.pulse.ModeMechanic;
import td.tower.pulse.ModeSpec;
import td.tower.pulse.PulseMechanic;
import td.tower.pulse.PulseSpec;
import td.tower.pulse.SightMechanic;
import td.tower.pulse.SightSpec;
import td.tower.pulse.SnareMechanic;
import td.tower.pulse.StasisMechanic;
import td.tower.pulse.TollMechanic;
import td.tower.pulse.TollSpec;
import td.tower.pulse.TollTracker;
import td.tower.pulse.TrueSightMechanic;
import td.tower.pulse.UndertowMechanic;
import td.tower.pulse.VisitSpec;
import td.tower.pulse.ZapSpec;
import td.tower.targeting.HighestRankSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.Viewpoint;
import td.tower.upgrade.BaseSlotMechanics;
import td.tower.upgrade.ExclusiveChoice;
import td.tower.upgrade.MechanicCatalogue;
import td.tower.upgrade.NodeEffects;
import td.tower.upgrade.OwnedMechanics;
import td.tower.upgrade.PurposeCondition;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.PathRuntime;
import td.util.ThreadConfined;
import td.wave.Path;
import td.wave.WaveStartListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The field: short range, no cooldown, a little magic damage to every enemy inside every tick, invisible
 * ones included, whether or not a visible one is there to trigger it, and a drag that slows them. It is a
 * kill box: its worth is how long it holds the enemies inside and what it does to them for the other
 * towers. Attuned, an enemy that stays inside builds Toll, which makes every effect the field holds on it
 * stronger. Fields that overlap interfere: each lands less damage.
 * <p>
 * What each owned node does lives in a {@link PulseMechanic}: the tower reads one spec from them every
 * tick.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class PulseTower extends AbstractTower {

    public static final int PRICE = 25;
    public static final float DAMAGE_POINTS = 1f;
    public static final float RANGE = 1.5f;

    /** How long a zap's bolt stays drawn. */
    private static final int ZAP_FLASH_TICKS = 8;
    private static final TermText FULL_TOLL_DEED = TermText.format("seconds at full {}", EffectKind.TOLL);
    private static final int FULL_TOLL_SECONDS_NEEDED = 30;
    /** What every other field overlapping this one takes off its damage, and the least it keeps. */
    private static final float INTERFERENCE_PER_FIELD = 0.25f;
    private static final float INTERFERENCE_FLOOR = 0.25f;

    private static final BaseSlotMechanics BASE_MECHANICS = BaseSlotMechanics.none()
            .withRangeThree(0.2f, NodeEffects.none().withModifiers(TowerModifiers.tollLinger(20)));

    private static final UpgradeNode UNDERTOW = UpgradeTier.HEAD_1.node("pulse.head.gravity.1", "Undertow", PRICE);
    private static final UpgradeNode SNARE = UpgradeTier.HEAD_2.node("pulse.head.gravity.2", "Snare", PRICE)
            .after(UNDERTOW);
    private static final UpgradeNode EVENT_HORIZON = UpgradeTier.HEAD_3.node("pulse.head.gravity.3", "Event Horizon",
            PRICE)
            .withGate(new PurposeCondition(FULL_TOLL_DEED, FULL_TOLL_SECONDS_NEEDED))
            .after(SNARE);
    private static final UpgradeNode GRAVITY_WELL = UpgradeTier.HEAD_4.node("pulse.head.gravity.4a", "Gravity Well",
            PRICE)
            .after(EVENT_HORIZON);
    private static final UpgradeNode STASIS = UpgradeTier.HEAD_4.node("pulse.head.gravity.4b", "Stasis", PRICE)
            .after(EVENT_HORIZON);
    private static final UpgradeNode SIGHT = UpgradeTier.HEAD_1.node("pulse.head.ward.1", "Sight", PRICE);
    private static final UpgradeNode NULL_FIELD = UpgradeTier.HEAD_2.node("pulse.head.ward.2", "Null Field", PRICE)
            .after(SIGHT);
    private static final UpgradeNode DEAD_ZONE = UpgradeTier.HEAD_3.node("pulse.head.ward.3", "Dead Zone", PRICE)
            .withGate(new PurposeCondition(FULL_TOLL_DEED, FULL_TOLL_SECONDS_NEEDED))
            .after(NULL_FIELD);
    private static final UpgradeNode TRUE_SIGHT = UpgradeTier.HEAD_4.node("pulse.head.ward.4a", "True Sight", PRICE)
            .after(DEAD_ZONE);
    private static final UpgradeNode PURGE = UpgradeTier.HEAD_4.node("pulse.head.ward.4b", "Purge", PRICE)
            .after(DEAD_ZONE);
    private static final UpgradeNode FRAILTY = UpgradeTier.EXTRA_1.node("pulse.extra.bite.1", "Frailty", PRICE);
    private static final UpgradeNode CORROSION = UpgradeTier.EXTRA_2.node("pulse.extra.bite.2", "Corrosion", PRICE)
            .after(FRAILTY);
    private static final UpgradeNode RESONANT_COILS = UpgradeTier.EXTRA_3.node("pulse.extra.bite.3", "Resonant Coils",
            PRICE)
            .withModifiers(TowerModifiers.tollRate(1f))
            .after(CORROSION);
    private static final UpgradeNode MELTDOWN = UpgradeTier.EXTRA_4.node("pulse.extra.bite.4", "Meltdown", PRICE)
            .withModifiers(TowerModifiers.tollCap(5))
            .after(RESONANT_COILS);
    private static final UpgradeNode ARC_DISCHARGE = UpgradeTier.SPECIAL.node("pulse.special.arc_discharge",
            "Arc Discharge", PRICE);
    private static final UpgradeNode SOUL_DRAIN = UpgradeTier.SPECIAL.node("pulse.special.soul_drain", "Soul Drain",
            PRICE);
    private static final UpgradeNode LEY_LINE = UpgradeTier.SPECIAL.node("pulse.special.ley_line", "Ley Line", PRICE);

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE, RANGE, BASE_MECHANICS, EVENT_HORIZON,
            DEAD_ZONE))
            .with(UNDERTOW, SNARE, EVENT_HORIZON, GRAVITY_WELL, STASIS, SIGHT, NULL_FIELD, DEAD_ZONE, TRUE_SIGHT, PURGE,
                    FRAILTY, CORROSION, RESONANT_COILS, MELTDOWN, ARC_DISCHARGE, SOUL_DRAIN, LEY_LINE)
            .withChoice(ExclusiveChoice.oneOf(UNDERTOW, SIGHT))
            .withChoice(ExclusiveChoice.oneOf(GRAVITY_WELL, STASIS))
            .withChoice(ExclusiveChoice.oneOf(TRUE_SIGHT, PURGE))
            .withChoice(ExclusiveChoice.specials(ARC_DISCHARGE, SOUL_DRAIN, LEY_LINE));

    private static final MechanicCatalogue<PulseMechanic> MECHANICS = MechanicCatalogue.<PulseMechanic>empty()
            .with(StandardBaseSlot.ATTUNE_ID, TollMechanic::new)
            .with(UNDERTOW.id(), UndertowMechanic::new)
            .with(SNARE.id(), SnareMechanic::new)
            .with(EVENT_HORIZON.id(), EventHorizonMechanic::new)
            .with(GRAVITY_WELL.id(), GravityWellMechanic::new)
            .with(STASIS.id(), StasisMechanic::new)
            .with(SIGHT.id(), SightMechanic::new)
            .with(NULL_FIELD.id(), () -> new ModeMechanic(FieldMode.SILENCE))
            .with(DEAD_ZONE.id(), () -> new ModeMechanic(FieldMode.DEAD_ZONE))
            .with(TRUE_SIGHT.id(), TrueSightMechanic::new)
            .with(PURGE.id(), () -> new ModeMechanic(FieldMode.PURGE))
            .with(FRAILTY.id(), () -> new ModeMechanic(FieldMode.FRAIL))
            .with(CORROSION.id(), () -> new ModeMechanic(FieldMode.CORROSION))
            .with(ARC_DISCHARGE.id(), ArcDischargeMechanic::new)
            .with(SOUL_DRAIN.id(), () -> new ModeMechanic(FieldMode.SOUL_DRAIN))
            .with(LEY_LINE.id(), LeyLineMechanic::new);

    private final OwnedMechanics<PulseMechanic> mechanics = new OwnedMechanics<>(MECHANICS);
    private final TollTracker tollTracker = new TollTracker();
    private final FieldHolds holds = new FieldHolds();
    private final FireClock clock = new FireClock(0);
    private boolean fire = false;
    private int highestToll;
    private float fieldRadius;
    private float interference = 1f;
    private List<Zap> zaps = List.of();
    private int lastZapTick = Integer.MIN_VALUE;
    private List<EnemyMob> previouslyInside = List.of();
    private List<EnemyMob> previouslyOnLines = List.of();
    private int deathsThisWave;
    private boolean watchingWaves;
    private final WaveStartListener waveListener = () -> this.deathsThisWave = 0;
    /** A pin the player asked for off the game loop, taken up at the next relink. */
    private final AtomicReference<Optional<PulseTower>> pinRequest = new AtomicReference<>(Optional.empty());
    private Optional<PulseTower> lineHost = Optional.empty();
    private Optional<PulseTower> pinnedHost = Optional.empty();
    private Optional<CastLine> castLine = Optional.empty();
    private List<PulseTower> castersHere = List.of();
    private boolean lineBusy;

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
    protected MechanicCatalogue<PulseMechanic> mechanicCatalogue() {
        return MECHANICS;
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        this.mechanics.add(node);
    }

    @Override
    protected void onUpgradesRemoved(List<UpgradeNode> removed) {
        this.mechanics.remove(removed);
    }

    /** Whom the field touches and how Toll builds, as the mechanics in {@code owned} make it and the deaths inside widen it. */
    private PulseSpec spec(List<PulseMechanic> owned) {
        PulseSpec spec = this.spec(owned, this.rangeReal());
        float growth = spec.field().growthAfter(this.deathsThisWave);
        return growth > 0f ? this.spec(owned, this.rangeReal() * (1f + growth)) : spec;
    }

    private PulseSpec spec(List<PulseMechanic> owned, float radius) {
        Viewpoint view = new Viewpoint(this.centerX, this.centerY, radius, this.context.getBoard().scale());
        PulseSpec spec = PulseSpec.from(view);
        for (PulseMechanic mechanic : owned) {
            spec = mechanic.refineSpec(spec);
        }
        return spec;
    }

    public void doTick(int gameTime) {
        PulseSpec spec = this.spec(this.mechanics.all());
        this.fieldRadius = spec.view().range();
        List<EnemyMob> inside = spec.reach().matching(this.context.enemies());
        this.fire = !inside.isEmpty();
        int shots = inside.isEmpty() ? 0 : this.clock.shotsDue();
        List<EnemyMob> wasInside = this.previouslyInside;
        this.countDeaths(spec, inside);
        boolean beat = gameTime % TICKS_PER_SECOND == 0;
        if (beat) {
            this.interference = this.interferenceNow();
            this.relink(spec);
        }
        List<EnemyMob> onLines = this.onLines(inside);
        this.lineBusy = this.castLine.isPresent() && onLines.stream().anyMatch(this.castLine.get()::holds);
        this.holds.keepOnly(onLines.isEmpty() ? inside : concat(inside, onLines));
        TollSpec toll = this.toll(spec);
        List<EnemyMob> earners = toll.isActive() ? this.tollTracker.tick(inside, toll.ticksPerStack()) : List.of();
        int highest = 0;
        for (EnemyMob enemy : inside) {
            boolean earned = earners.contains(enemy);
            boolean startsVisit = earned && !enemy.hasEffect(EffectKind.TOLL);
            this.addToll(enemy, toll, earned);
            if (startsVisit) {
                this.startVisit(enemy, spec.visit());
            }
            boolean fullToll = toll.isActive() && enemy.effectStacks(EffectKind.TOLL) >= toll.cap();
            this.still(enemy, spec.hold(), fullToll && (beat || earned), gameTime);
            this.holdInside(enemy, spec, beat, this.fromCentre(enemy), !wasInside.contains(enemy));
            highest = Math.max(highest, enemy.effectStacks(EffectKind.TOLL));
            HitAdjustment adjustment = this.hitAdjustment(enemy, HitKind.FIELD, 0f, this.stats());
            float factor = adjustment.damageFactor() * this.interference;
            for (int hit = 0; hit < shots && !enemy.isDead(); hit++) {
                this.dealPeriodicDamage(enemy, Damage.magic(Math.round(this.damageCurrent() * factor)));
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
        for (EnemyMob enemy : onLines) {
            this.holdInside(enemy, spec, beat, 1f, !this.previouslyOnLines.contains(enemy));
        }
        this.previouslyOnLines = onLines;
        if (spec.zap().isActive() && beat && !inside.isEmpty()) {
            this.zap(spec.zap(), spec, gameTime);
        }
    }

    private static List<EnemyMob> concat(List<EnemyMob> first, List<EnemyMob> second) {
        List<EnemyMob> both = new ArrayList<>(first);
        both.addAll(second);
        return both;
    }

    /**
     * Ley Line, once a second: the field this one casts to (its pinned host while that still stands within reach,
     * else the one whose beam crosses the path most), and the fields that cast to this one.
     */
    private void relink(PulseSpec spec) {
        List<PulseTower> others = this.context.towers().all().stream().map(tower -> tower.accept(AS_FIELD))
                .flatMap(Optional::stream).filter(other -> other != this).toList();
        LineSpec line = spec.line();
        float reach = line.reachCells() * this.context.getBoard().scale();
        float band = line.bandCells() * this.context.getBoard().scale();
        Optional<PulseTower> requested = this.pinRequest.getAndSet(Optional.empty());
        if (requested.isPresent()) {
            this.pinnedHost = this.pinnedHost.equals(requested) ? Optional.empty() : requested;
        }
        this.pinnedHost = this.pinnedHost.filter(others::contains).filter(host -> this.distanceTo(host) <= reach);
        if (!line.casts()) {
            this.lineHost = Optional.empty();
        } else if (this.pinnedHost.isPresent()) {
            this.lineHost = this.pinnedHost;
        } else {
            List<Path> paths = this.context.level().paths().stream().map(PathRuntime::path).toList();
            this.lineHost = others.stream().filter(other -> this.distanceTo(other) <= reach)
                    .map(other -> new Candidate(other, this.lineTo(other).scoredOn(paths, band)))
                    .filter(candidate -> candidate.scored().touchesAPath())
                    .min(Comparator.comparing(Candidate::scored, LeyLine.BETTER_FIRST))
                    .map(Candidate::host);
        }
        this.castLine = this.lineHost.map(host -> new CastLine(this.lineTo(host), band));
        this.castersHere = others.stream().filter(other -> other.lineHost.filter(host -> host == this).isPresent())
                .toList();
    }

    private LeyLine lineTo(PulseTower host) {
        return new LeyLine(this.centerX, this.centerY, host.centerX, host.centerY);
    }

    private float distanceTo(PulseTower other) {
        return (float) Math.hypot(other.centerX - this.centerX, other.centerY - this.centerY);
    }

    /** The enemies outside the field on a Ley Line this field casts or hosts, each once. */
    private List<EnemyMob> onLines(List<EnemyMob> inside) {
        if (this.castLine.isEmpty() && this.castersHere.isEmpty()) {
            return List.of();
        }
        List<CastLine> lines = new ArrayList<>();
        this.castLine.ifPresent(lines::add);
        this.castersHere.forEach(caster -> caster.castLine.ifPresent(lines::add));
        List<EnemyMob> on = new ArrayList<>();
        for (CastLine line : lines) {
            for (EnemyMob enemy : line.near(this.context)) {
                if (line.holds(enemy) && !inside.contains(enemy) && !on.contains(enemy)) {
                    on.add(enemy);
                }
            }
        }
        return on;
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

    /**
     * The zap: the visible enemy of the highest rank in the field, the furthest along of those, made the Priority of every
     * tower and Condemned a stack further, both held a while after the last zap.
     */
    private void zap(ZapSpec zap, PulseSpec spec, int gameTime) {
        Viewpoint view = spec.view();
        List<EnemyMob> seen = InRangeTargetQuery.visible(view.x(), view.y(), view.range())
                .matching(this.context.enemies());
        Optional<EnemyMob> struck = new HighestRankSelector().selectFrom(seen);
        if (struck.isEmpty()) {
            return;
        }
        EnemyMob target = struck.get();
        this.applyEffect(target, sink -> Effect.priority(zap.markTicks(), sink));
        this.applyEffect(target, sink -> Effect.condemned(1, zap.markTicks(), sink));
        this.zaps = List.of(new Zap(this.centerX, this.centerY, (float) target.getX(), (float) target.getY()));
        this.lastZapTick = gameTime;
    }

    /** An enemy's first Toll stack of a visit: it may be Dazed. */
    private void startVisit(EnemyMob enemy, VisitSpec visit) {
        if (visit.dazeTicks() > 0) {
            this.applyEffect(enemy, sink -> Effect.dazed(visit.dazeTicks(), sink));
        }
    }

    /**
     * Stasis: an enemy at full Toll, on the tick it gets there or once a second after, is Dazed and Stilled; while it
     * is both, it loses a Toll stack every so often. A Daze that doesn't take hold leaves it unstilled, to be tried
     * again.
     */
    private void still(EnemyMob enemy, HoldSpec hold, boolean tryNow, int gameTime) {
        if (!hold.stills()) {
            return;
        }
        if (tryNow && !enemy.hasEffect(EffectKind.STILLED)) {
            this.applyEffect(enemy, sink -> Effect.dazed(hold.stasisTicks(), sink));
            if (enemy.hasEffect(EffectKind.DAZED)) {
                this.applyEffect(enemy, sink -> Effect.stilled(hold.stasisTicks(), sink));
            }
        } else if (enemy.hasEffect(EffectKind.STILLED) && enemy.hasEffect(EffectKind.DAZED)
                && gameTime % hold.ticksPerToll() == 0) {
            enemy.loseStacks(EffectKind.TOLL, 1);
        }
    }

    /**
     * The rules that hold while an enemy is inside: each lasts a moment, refreshed every tick it stays, and those with
     * a number hold it at the strength its Toll gives. The drag also follows how near the centre it is, an enemy on a
     * Ley Line counting as at the rim. Purge strips an enemy only as it comes in; Soul Drain takes some spirit each
     * second.
     */
    private void holdInside(EnemyMob enemy, PulseSpec spec, boolean beat, float fromCentre, boolean entering) {
        ModeSpec modes = spec.modes();
        int strength = FieldStrength.at(enemy.effectStacks(EffectKind.TOLL));
        int drag = this.holds.toward(enemy, EffectKind.DRAGGED, spec.hold().dragAt(strength, fromCentre));
        this.applyEffect(enemy, sink -> Effect.dragged(drag, FieldMode.LINGER_TICKS, sink));
        if (modes.has(FieldMode.UNDERTOW)) {
            this.applyEffect(enemy, sink -> Effect.undertow(FieldMode.LINGER_TICKS, sink));
            this.applyEffect(enemy, sink -> Effect.anchored(FieldMode.LINGER_TICKS, sink));
        }
        if (modes.has(FieldMode.SILENCE)) {
            this.applyEffect(enemy, sink -> Effect.silenced(FieldMode.LINGER_TICKS, sink));
        }
        if (modes.has(FieldMode.DEAD_ZONE)) {
            this.applyEffect(enemy, sink -> Effect.deadZone(FieldMode.LINGER_TICKS, sink));
        }
        if (modes.has(FieldMode.CORROSION)) {
            int added = this.holds.toward(enemy, EffectKind.CORRODED, strength);
            this.applyEffect(enemy, sink -> Effect.corroded(added, FieldMode.LINGER_TICKS, sink));
        }
        if (modes.has(FieldMode.FRAIL)) {
            int added = this.holds.toward(enemy, EffectKind.FRAIL, strength);
            this.applyEffect(enemy, sink -> Effect.frail(added, FieldMode.LINGER_TICKS, sink));
        }
        if (entering && modes.has(FieldMode.PURGE)) {
            enemy.dispelBuffs();
            enemy.dispelRestoratives();
        }
        if (beat && modes.has(FieldMode.SOUL_DRAIN)) {
            this.applyEffect(enemy, sink -> Effect.sickened(FieldMode.SOUL_DRAIN_SPIRIT_PER_SECOND));
        }
        this.show(enemy, spec.sight());
    }

    /** Ward's sight: revealed while inside and a while after, and with True Sight not to be shrouded meanwhile. */
    private void show(EnemyMob enemy, SightSpec sight) {
        if (!sight.reveals()) {
            return;
        }
        if (sight.unveils()) {
            this.applyEffect(enemy, sink -> Effect.unveiled(sight.revealTicks(), sink));
        }
        this.reveal(enemy, sight.revealTicks());
    }

    /** How far from the centre {@code enemy} stands, as a share of the field's radius. */
    private float fromCentre(EnemyMob enemy) {
        double dx = enemy.getX() - this.centerX;
        double dy = enemy.getY() - this.centerY;
        return this.fieldRadius <= 0f ? 1f : (float) (Math.sqrt(dx * dx + dy * dy) / this.fieldRadius);
    }

    /** A stack for an enemy that has earned one; for one that holds some, the refresh that keeps them. */
    private void addToll(EnemyMob enemy, TollSpec toll, boolean earned) {
        if (!toll.isActive() || !(earned || enemy.hasEffect(EffectKind.TOLL))) {
            return;
        }
        int stacks = earned ? 1 : 0;
        this.applyEffect(enemy, sink -> Effect.toll(stacks, toll.fadeTicks(), sink).withStackCap(toll.cap()));
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
    }

    /** What the field's colour says about its head: Ward violet, Gravity blue. */
    public FieldLook getFieldLook() {
        PulseSpec spec = this.spec(this.mechanics.all());
        if (spec.sight().reveals()) {
            return FieldLook.WARD;
        }
        return spec.modes().has(FieldMode.UNDERTOW) ? FieldLook.GRAVITY : FieldLook.PLAIN;
    }

    /** Whether the field owns the bite line, drawn as a red rim. */
    public boolean bites() {
        return this.spec(this.mechanics.all()).modes().has(FieldMode.FRAIL);
    }

    /** Whether the field drags hardest at its centre, drawn darker there. */
    public boolean hasGravityWell() {
        return this.spec(this.mechanics.all()).hold().centreDrag() > 0;
    }

    /** The Ley Line this field casts, as of the last second; empty without one. */
    public Optional<LeyLine> getLeyLine() {
        return this.castLine.map(CastLine::line);
    }

    /** Whether an enemy stood on this field's Ley Line at the last tick. */
    public boolean isLeyLineBusy() {
        return this.lineBusy;
    }

    /** Whether the player chose this field's host rather than leaving it to the field. */
    public boolean isLeyLinePinned() {
        return this.pinnedHost.isPresent();
    }

    /**
     * Any thread: pins this field's Ley Line to {@code host} from the next second on, if it casts one and {@code host}
     * is another Obelisk within its reach; pinning the host it is pinned to unpins it. Tells whether it asked.
     */
    public boolean pinLeyLine(Tower host) {
        Optional<PulseTower> field = host.accept(AS_FIELD).filter(other -> other != this);
        LineSpec line = this.spec(this.mechanics.all()).line();
        if (field.isEmpty() || !line.casts()
                || this.distanceTo(field.get()) > line.reachCells() * this.context.getBoard().scale()) {
            return false;
        }
        this.pinRequest.set(field);
        return true;
    }

    /** Whether {@code tower} is an Obelisk that casts a Ley Line. */
    public static boolean castsLeyLine(Tower tower) {
        return tower.accept(AS_FIELD).filter(field -> field.spec(field.mechanics.all()).line().casts()).isPresent();
    }

    /** {@link #pinLeyLine} for {@code caster}, if it is an Obelisk; {@code false} for any other tower. */
    public static boolean pinLeyLine(Tower caster, Tower host) {
        return caster.accept(AS_FIELD).filter(field -> field.pinLeyLine(host)).isPresent();
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
    protected List<TowerBuffState> ownConditions() {
        List<TowerBuffState> states = new ArrayList<>();
        PulseSpec spec = this.spec(this.mechanics.all());
        float growth = spec.field().growthAfter(this.deathsThisWave);
        if (growth > 0f) {
            states.add(TowerBuffState.of(TowerTerm.EVENT_HORIZON, TermText.of(
                    TermNumbers.signedPercent(growth) + " field width until the wave ends"))
                    .withStacks(this.deathsThisWave));
        }
        if (this.interference < 1f) {
            states.add(TowerBuffState.of(TowerTerm.INTERFERENCE,
                    TermText.of(TermNumbers.signedPercent(this.interference - 1f) + " damage")));
        }
        if (this.castLine.isPresent()) {
            states.add(TowerBuffState.of(TowerTerm.LEY_LINE, TermText.of("casting to an Obelisk")));
        } else if (!this.castersHere.isEmpty()) {
            states.add(TowerBuffState.of(TowerTerm.LEY_LINE,
                    TermText.of("hosting " + this.castersHere.size() + " lines")));
        }
        this.mechanics.all().forEach(mechanic -> mechanic.condition().ifPresent(states::add));
        return states;
    }

    @Override
    protected List<AppliedEffect> baseApplies() {
        PulseSpec spec = this.spec(this.mechanics.all());
        return List.of(new AppliedEffect(EffectKind.DRAGGED, List.of(new AppliedEffect.Source(this.displayName(),
                TermText.of("enemies inside gain ").plus(EffectKind.DRAGGED)
                        .plus(" of " + spec.hold().dragAt(FieldStrength.FULL, 1f) + "%")))));
    }

    @Override
    protected String targetingLabel() {
        return "all in range";
    }

    private static String insideText(FieldMode mode) {
        return switch (mode) {
            case SILENCE -> "silenced";
            case DEAD_ZONE -> "no heals or shields";
            case CORROSION -> "armor and resist lowered";
            case UNDERTOW -> "anchored, drag chills";
            case FRAIL -> "take more damage";
            case SOUL_DRAIN -> "spirit drained";
            case PURGE -> "buffs stripped on entry";
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

    /** The Obelisk behind a tower, if it is one. */
    private static final TowerVisitor<Optional<PulseTower>> AS_FIELD = new TowerVisitor<>() {
        @Override
        public Optional<PulseTower> visitSniperTower(SniperTower tower) {
            return Optional.empty();
        }

        @Override
        public Optional<PulseTower> visitSplashTower(SplashTower tower) {
            return Optional.empty();
        }

        @Override
        public Optional<PulseTower> visitSonarTower(SonarTower tower) {
            return Optional.empty();
        }

        @Override
        public Optional<PulseTower> visitPulseTower(PulseTower tower) {
            return Optional.of(tower);
        }

        @Override
        public Optional<PulseTower> visitAuraTower(AuraTower tower) {
            return Optional.empty();
        }

        @Override
        public Optional<PulseTower> visitMortarTower(MortarTower tower) {
            return Optional.empty();
        }

        @Override
        public Optional<PulseTower> visitSeekerTower(SeekerTower tower) {
            return Optional.empty();
        }

        @Override
        public Optional<PulseTower> visitCinderTower(CinderTower tower) {
            return Optional.empty();
        }
    };

    /** A field the relink weighed as a host, with what its beam would do to the paths. */
    private record Candidate(PulseTower host, LeyLine.Scored scored) {
    }

    /** A cast Ley Line and how near it an enemy must be, in board pixels, to stand on it. */
    private record CastLine(LeyLine line, float band) {

        /** The enemies in the circle around the beam, of whom those within the band stand on it. */
        List<EnemyMob> near(GameWorld context) {
            float midX = (this.line.fromX() + this.line.toX()) / 2f;
            float midY = (this.line.fromY() + this.line.toY()) / 2f;
            float radius = (float) (this.line.length() / 2 + this.band);
            return InRangeTargetQuery.everyone(Math.round(midX), Math.round(midY), radius).matching(context.enemies());
        }

        boolean holds(EnemyMob enemy) {
            return this.line.distanceTo(enemy.getX(), enemy.getY()) <= this.band;
        }
    }

    /** What the field's colour says about its head. */
    public enum FieldLook {
        PLAIN,
        WARD,
        GRAVITY
    }

    /** One bolt of a zap, in board pixels. */
    public record Zap(float fromX, float fromY, float toX, float toY) {
    }
}
