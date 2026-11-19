package td.tower;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.damage.DamageUnits;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.enemy.HitResult;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.proc.HitAdjustment;
import td.tower.proc.HitKind;
import td.tower.pulse.BurdenMechanic;
import td.tower.pulse.DeepShadowMechanic;
import td.tower.pulse.DeepTollMechanic;
import td.tower.pulse.GravityMechanic;
import td.tower.pulse.GravitySpec;
import td.tower.pulse.KnellMechanic;
import td.tower.pulse.PulseMechanic;
import td.tower.pulse.PulseSpec;
import td.tower.pulse.SonarSweep;
import td.tower.pulse.StoneLook;
import td.tower.pulse.SundialMechanic;
import td.tower.pulse.SundialSpec;
import td.tower.pulse.SwiftShadowMechanic;
import td.tower.pulse.UmbraMechanic;
import td.tower.pulse.WardMechanic;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.upgrade.MechanicCatalogue;
import td.tower.upgrade.OwnedMechanics;
import td.tower.upgrade.RangeNodes;
import td.tower.upgrade.UpgradeCluster;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.util.TickRate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import static td.tower.upgrade.MechanicCatalogue.entry;

/**
 * The Obelisk: a kill box. Every enemy in its field, hidden ones included, takes a little magic damage every tick and
 * the field's drag, whether or not a visible one is there to trigger it, and builds Toll while inside: each stack
 * deepens the drag and makes it take more damage from every source. Toll is the enemy's, so fields that overlap add to
 * the same stacks and the deeper drag holds. Its worth is how long it holds the enemies and how much harder every
 * tower hits them meanwhile.
 * <p>
 * What each owned node does lives in its data or in a {@link PulseMechanic}: the tower reads one spec from them every
 * tick.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class PulseTower extends AbstractTower {

    public static final int PRICE = 15;
    /** Damage a second to every enemy in the field. */
    public static final float DAMAGE_POINTS = 10f;
    public static final float RANGE = 1.5f;

    private static final float BASE_DRAG = 0.1f;
    private static final float DRAG_PER_TOLL = 0.04f;
    private static final int TOLL_PACE_TICKS = Math.round(TickRate.TICKS_PER_SECOND);
    /** How long what the field holds an enemy to lasts once it leaves. */
    private static final int LINGER_TICKS = Math.round(TickRate.TICKS_PER_SECOND / 2f);
    /** The stone rings on this beat. */
    private static final int KNELL_EVERY_TICKS = Math.round(TickRate.TICKS_PER_SECOND);
    /** The shadow's rhythm is paid on a clock of periods this long, so attack speed quickens it. */
    private static final int SHADOW_PERIOD_TICKS = Math.round(TickRate.TICKS_PER_SECOND);
    private static final float HUM_POINTS = 5f;

    private static final UpgradeNode WIDE_FIELD = UpgradeNode.of(RangeNodes.RANGE_ID, "Wide Field", 26)
            .withModifiers(TowerModifiers.range(0.5f));
    private static final UpgradeNode HUM = UpgradeNode.of("pulse.hum", "Hum", 14)
            .withModifiers(TowerModifiers.damage(HUM_POINTS));

    private static final UpgradeNode SUNDIAL = UpgradeNode.of("pulse.sundial", "Sundial", 35)
            .inTier(1);
    private static final UpgradeNode WARD = UpgradeNode.of("pulse.ward", "Ward", 1)
            .inTier(1);
    private static final UpgradeNode QUICK_TOLL = UpgradeNode.of("pulse.quick_toll", "Quick Toll", 9)
            .withModifiers(TowerModifiers.tollPace(-Math.round(0.25f * TickRate.TICKS_PER_SECOND)))
            .inTier(1);
    private static final UpgradeNode DEEP_PULL = UpgradeNode.of("pulse.deep_pull", "Deep Pull", 29)
            .withModifiers(TowerModifiers.dragPerToll(0.02f))
            .inTier(1);

    private static final UpgradeNode BURDEN = UpgradeNode.of("pulse.burden", "Burden", 62)
            .withModifiers(TowerModifiers.dragPerToll(0.03f))
            .inTier(2);
    private static final UpgradeNode GRAVITY = UpgradeNode.of("pulse.gravity", "Gravity", 62)
            .inTier(2);
    private static final UpgradeNode UMBRA = UpgradeNode.of("pulse.umbra", "Umbra", 62)
            .after(SUNDIAL)
            .inTier(2);
    private static final UpgradeNode HUM_2 = UpgradeNode.of("pulse.hum_2", "Hum II", 29)
            .withModifiers(TowerModifiers.damage(HUM_POINTS))
            .inTier(2);
    private static final UpgradeNode DEEP_SHADOW = UpgradeNode.of("pulse.deep_shadow", "Deep Shadow", 18)
            .after(SUNDIAL)
            .inTier(2);
    private static final UpgradeNode WIDE_FIELD_2 = UpgradeNode.of(RangeNodes.RANGE_2_ID, "Wide Field II", 52)
            .withModifiers(TowerModifiers.range(0.5f))
            .inTier(2);

    private static final UpgradeNode KNELL = UpgradeNode.of("pulse.knell", "Knell", 152)
            .inTier(3);
    private static final UpgradeNode DEEP_TOLL = UpgradeNode.of("pulse.deep_toll", "Deep Toll", 152)
            .inTier(3);
    private static final UpgradeNode DEEP_PULL_2 = UpgradeNode.of("pulse.deep_pull_2", "Deep Pull II", 102)
            .withModifiers(TowerModifiers.dragPerToll(0.02f))
            .inTier(3);
    private static final UpgradeNode SWIFT_SHADOW = UpgradeNode.of("pulse.swift_shadow", "Swift Shadow", 46)
            .after(SUNDIAL)
            .inTier(3);

    private static final UpgradeTree TREE = UpgradeTree.of(List.of(WIDE_FIELD, HUM))
            .with(SUNDIAL, WARD, QUICK_TOLL, DEEP_PULL,
                    BURDEN, GRAVITY, UMBRA, HUM_2, DEEP_SHADOW, WIDE_FIELD_2,
                    KNELL, DEEP_TOLL, DEEP_PULL_2, SWIFT_SHADOW)
            .withCluster(UpgradeCluster.of("The stone", BURDEN, GRAVITY, UMBRA))
            .withCluster(UpgradeCluster.of("Payoff", KNELL, DEEP_TOLL));

    private static final MechanicCatalogue<PulseMechanic> MECHANICS = MechanicCatalogue.of(
            entry(SUNDIAL.id(), SundialMechanic::new),
            entry(WARD.id(), WardMechanic::new),
            entry(BURDEN.id(), BurdenMechanic::new),
            entry(GRAVITY.id(), GravityMechanic::new),
            entry(UMBRA.id(), UmbraMechanic::new),
            entry(DEEP_SHADOW.id(), DeepShadowMechanic::new),
            entry(KNELL.id(), KnellMechanic::new),
            entry(DEEP_TOLL.id(), DeepTollMechanic::new),
            entry(SWIFT_SHADOW.id(), SwiftShadowMechanic::new));

    private final OwnedMechanics<PulseMechanic> mechanics = new OwnedMechanics<>(MECHANICS);
    /** How many ticks each enemy inside has stood in the field this visit, in the order they came in. */
    private final Map<EnemyMob, Integer> visits = new LinkedHashMap<>();
    private FireClock clock = new FireClock(0);
    private FireClock shadowClock = new FireClock(SHADOW_PERIOD_TICKS - 1);
    private SonarSweep shadow = SonarSweep.perRevolution(1, TickRate.TICKS_PER_SECOND);
    private int shadowLeft;
    private boolean fullTollInside;
    private OptionalInt lastKnell = OptionalInt.empty();

    public PulseTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.PULSE, new TowerBaseStats(DAMAGE_POINTS, RANGE, 0)
                .with(TowerStat.TOLL_PACE, TOLL_PACE_TICKS).with(TowerStat.DRAG_PER_TOLL, DRAG_PER_TOLL),
                context, x, y);
    }

    @Override
    protected OwnedMechanics<PulseMechanic> ownedMechanics() {
        return this.mechanics;
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    /** The field and the shadow, as the mechanics in {@code owned} make them. */
    private PulseSpec spec(List<PulseMechanic> owned) {
        PulseSpec spec = PulseSpec.from(this.viewpoint());
        for (PulseMechanic mechanic : owned) {
            spec = mechanic.refineSpec(spec);
        }
        return spec;
    }

    @Override
    protected void act(int gameTime) {
        PulseSpec spec = this.spec(this.mechanics.all());
        List<EnemyMob> inside = spec.reach().matching(this.context.enemies());
        this.visits.keySet().retainAll(inside);
        int shots = inside.isEmpty() ? 0 : this.clock.shotsDue();
        int pace = Math.max(1, Math.round(this.stats().value(TowerStat.TOLL_PACE)));
        boolean full = false;
        for (EnemyMob enemy : inside) {
            int ticksInside = this.visits.merge(enemy, 1, Integer::sum);
            enemy.holdStacks(EffectKind.TOLL, pace, spec.tollCap());
            this.holdInside(enemy, spec, ticksInside);
            full |= enemy.effectStacks(EffectKind.TOLL) >= PulseSpec.TOLL_CAP;
            HitAdjustment adjustment = this.hitAdjustment(enemy, HitKind.FIELD, 0f, this.stats());
            int perTick = Math.round(this.damageCurrent() / TickRate.TICKS_PER_SECOND * adjustment.damageFactor());
            for (int hit = 0; hit < shots && !enemy.isDead(); hit++) {
                this.dealPeriodicDamage(enemy, Damage.magic(perTick));
            }
        }
        this.fullTollInside = full;
        for (int hit = 0; hit < shots; hit++) {
            this.beginShot();
            this.clock.spend();
        }
        this.clock.advance(this.attackRate());
        if (full && spec.knell() > 0f && this.context.waves().tickOfWave(gameTime) % KNELL_EVERY_TICKS == 0) {
            this.knell(spec, inside, gameTime);
        }
        this.castShadow(spec.sundial(), inside);
    }

    /**
     * What holds while an enemy is inside: the drag Toll deepens, Ward's Silence, and Gravity's Daze once it has stood
     * a beat inside this visit. Each lasts a moment, refreshed every tick it stays.
     */
    private void holdInside(EnemyMob enemy, PulseSpec spec, int ticksInside) {
        float drag = Math.min(spec.dragCap(),
                BASE_DRAG + this.stats().value(TowerStat.DRAG_PER_TOLL) * enemy.effectStacks(EffectKind.TOLL));
        this.applyEffect(enemy, sink -> Effect.dragged(drag, LINGER_TICKS, sink));
        if (spec.silences()) {
            this.applyEffect(enemy, sink -> Effect.silenced(LINGER_TICKS, sink));
        }
        GravitySpec gravity = spec.gravity();
        if (gravity.isActive() && ticksInside == gravity.afterTicks()) {
            this.applyEffect(enemy, sink -> Effect.dazed(gravity.dazeTicks(), sink));
        }
    }

    /** One ring of the stone: every enemy in the field takes its hit. */
    private void knell(PulseSpec spec, List<EnemyMob> inside, int gameTime) {
        TowerStats shot = this.stats();
        Damage ring = Damage.magic(DamageUnits.ofPoints(spec.knell()));
        for (EnemyMob enemy : inside) {
            HitAdjustment adjustment = this.hitAdjustment(enemy, HitKind.FIELD, 0f, shot);
            boolean critical = this.dealDamage(enemy, ring.scaledBy(adjustment.damageFactor()), shot.attack());
            this.report(enemy, HitKind.FIELD, critical, enemy.isDead());
        }
        this.lastKnell = OptionalInt.of(gameTime);
    }

    /**
     * The Sundial: its rhythm counts on whatever is in reach, and a turn starts once it is due and an enemy is within
     * the shadow's reach, so the first comes at once when one walks in. While it turns, every visible enemy whose
     * bearing it swept this tick takes an aimed hit and a stack of Toll.
     */
    private void castShadow(SundialSpec sundial, List<EnemyMob> inside) {
        if (!sundial.isActive()) {
            this.shadowLeft = 0;
            return;
        }
        float reach = sundial.reachCells() * this.cellPixels();
        List<EnemyMob> within = InRangeTargetQuery.visible(this.centerX, this.centerY, reach)
                .matching(this.context.enemies());
        if (this.shadowLeft == 0 && this.shadowClock.isReady() && !within.isEmpty()) {
            this.shadowClock.spend((double) sundial.periodTicks() / SHADOW_PERIOD_TICKS);
            this.shadow = SonarSweep.perRevolution((double) sundial.turnTicks() / TickRate.TICKS_PER_SECOND,
                    TickRate.TICKS_PER_SECOND).startingAt(this.shadow.radiansAt(1.0));
            this.shadowLeft = sundial.turnTicks();
        }
        this.shadowClock.advance(this.attackRate());
        if (this.shadowLeft == 0) {
            return;
        }
        this.shadow.advance();
        this.shadowLeft--;
        TowerStats shot = this.stats();
        AttackProfile aimed = sundial.shadow() ? shot.attack().asShadow().asAimed() : shot.attack().asAimed();
        for (EnemyMob enemy : within) {
            if (!this.shadow.sweptThisTick(this.bearingTo(enemy))) {
                continue;
            }
            float factor = inside.contains(enemy) ? sundial.inFieldFactor() : 1f;
            HitAdjustment adjustment = this.hitAdjustment(enemy, HitKind.BEAM, 0f, shot);
            Damage hit = Damage.magic(DamageUnits.ofPoints(sundial.damagePoints() * factor))
                    .scaledBy(adjustment.damageFactor());
            HitResult result = this.landHit(enemy, hit, aimed);
            if (result.dodged()) {
                continue;
            }
            if (enemy.effectStacks(EffectKind.TOLL) < PulseSpec.TOLL_CAP) {
                this.applyEffect(enemy, _ -> Effect.toll(1));
            }
            this.report(enemy, HitKind.BEAM, result.critical(), enemy.isDead());
        }
    }

    @Override
    protected void endFight() {
        this.visits.clear();
        this.clock = new FireClock(0);
        this.shadowClock = new FireClock(SHADOW_PERIOD_TICKS - 1);
        this.shadowLeft = 0;
        this.fullTollInside = false;
        this.lastKnell = OptionalInt.empty();
    }

    /** Whether an enemy at full Toll stood in the field at the last tick. */
    public boolean holdsFullToll() {
        return this.fullTollInside;
    }

    /** How many ticks ago the stone last rang; a large number before the first. */
    public int ticksSinceKnell(int gameTime) {
        return this.lastKnell.stream().map(rang -> gameTime - rang).findFirst().orElse(Integer.MAX_VALUE);
    }

    /** Whether the shadow is turning now. */
    public boolean isCastingShadow() {
        return this.shadowLeft > 0;
    }

    /** The shadow's heading between the last two ticks, in radians. */
    public double shadowRadians(double interpolationAlpha) {
        return this.shadow.radiansAt(interpolationAlpha);
    }

    /** How far the shadow reaches, in board pixels; 0 without the Sundial. */
    public float shadowReach() {
        return this.spec(this.mechanics.all()).sundial().reachCells() * this.cellPixels();
    }

    /** What share of the shadow's turn is still to come; 0 while it is not turning. */
    public float shadowShareLeft() {
        int turn = this.spec(this.mechanics.all()).sundial().turnTicks();
        return turn == 0 ? 0f : (float) this.shadowLeft / turn;
    }

    /** Whether its field Silences what stands inside. */
    public boolean silences() {
        return this.spec(this.mechanics.all()).silences();
    }

    /** Which stone the identity pick made it. */
    public StoneLook stoneLook() {
        return this.spec(this.mechanics.all()).look();
    }

    @Override
    protected boolean dealsByField() {
        return true;
    }

    @Override
    protected DamageType damageType() {
        return DamageType.MAGIC;
    }

    @Override
    protected List<AppliedEffect> baseApplies() {
        String drag = TermNumbers.percent(BASE_DRAG) + ", deeper with every stack of ";
        return List.of(new AppliedEffect(EffectKind.TOLL, List.of(new AppliedEffect.Source(this.displayName(),
                        TermText.of("enemies inside build a stack, up to " + PulseSpec.TOLL_CAP + ": ")
                                .plus(TowerTerm.FRAILTY)))),
                new AppliedEffect(EffectKind.DRAGGED, List.of(new AppliedEffect.Source(this.displayName(),
                        TermText.of("enemies inside gain ").plus(EffectKind.DRAGGED).plus(" of " + drag)
                                .plus(EffectKind.TOLL)))));
    }

    @Override
    protected String targetingLabel() {
        return "all in range";
    }

    @Override
    protected String description() {
        return "Holds every enemy in its field: the longer one stays, the slower it goes and the harder every tower "
                + "hits it.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitPulseTower(this);
    }
}
