package td.tower;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.pulse.ArcDischargePerk;
import td.tower.pulse.EventHorizonPerk;
import td.tower.pulse.FasterTollPerk;
import td.tower.pulse.FieldMode;
import td.tower.pulse.FullTollBonusPerk;
import td.tower.pulse.LingeringTollPerk;
import td.tower.pulse.MeltdownPerk;
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
 * The field: short range, no cooldown, magic damage to every enemy inside every tick, invisible
 * ones included, whether or not a visible one is there to trigger it. Attuned, an enemy that stays
 * inside builds Toll, which makes the field hit it harder and every debuff on it wear off slower.
 * <p>
 * What each owned node does lives in a {@link PulsePerk}: the tower reads one spec from them every
 * tick.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class PulseTower extends AbstractTower {

    public static final int PRICE = 25;
    public static final float DAMAGE_POINTS = 2f;
    public static final float RANGE = 1.5f;
    /** Each Toll stack makes the field hit its enemy this much harder. */
    public static final float TOLL_DAMAGE_PER_STACK = 0.1f;

    /** How long a zap's bolt stays drawn. */
    private static final int ZAP_FLASH_TICKS = 8;
    private static final String FULL_TOLL_DEED = "Seconds at full Toll";
    private static final int FULL_TOLL_SECONDS_NEEDED = 30;

    private static final BaseSlotPerks BASE_PERKS = BaseSlotPerks.none()
            .withAttune("Toll: each second an enemy stays inside adds a stack (up to 5), fading 1s after it leaves; "
                    + "each stack: +10% field damage, and every debuff on it wears off 10% slower")
            .withRangeThree(0.2f, "Wide Field: Toll lasts 1s longer after an enemy leaves");

    private static final UpgradeNode OVERCHARGED_COILS_1 = UpgradeTier.HEAD_1.node("pulse.head.overcharged_coils.1",
            "Overcharged Coils", PRICE)
            .withBuff(TowerBuff.damage(0.3f))
            .withExtraEffect("Toll builds twice as fast");
    private static final UpgradeNode OVERCHARGED_COILS_2 = UpgradeTier.HEAD_2.node("pulse.head.overcharged_coils.2",
            "Overcharged Coils II", PRICE)
            .withBuff(TowerBuff.damage(0.25f))
            .withExtraEffect("an enemy at full Toll takes +25% from the field")
            .after(OVERCHARGED_COILS_1);
    private static final UpgradeNode ARC_DISCHARGE = UpgradeTier.HEAD_3.node("pulse.head.overcharged_coils.3",
            "Arc Discharge", PRICE)
            .withBuff(TowerBuff.critChance(0.1f))
            .withGate(new PurposeCondition(FULL_TOLL_DEED, FULL_TOLL_SECONDS_NEEDED))
            .withExtraEffect("once a second a zap hits the healthiest enemy inside for 15x the tick damage, magic, "
                    + "and can crit")
            .after(OVERCHARGED_COILS_2);
    private static final UpgradeNode MELTDOWN = UpgradeTier.HEAD_4.node("pulse.head.overcharged_coils.4a", "Meltdown",
            PRICE)
            .withExtraEffect("Toll stacks to 10; the zap gets +10% crit, and Toll raises the zap's damage too")
            .after(ARC_DISCHARGE);
    private static final UpgradeNode TESLA_COIL = UpgradeTier.HEAD_4.node("pulse.head.overcharged_coils.4b",
            "Tesla Coil", PRICE)
            .withExtraEffect("the zap chains to 3 more enemies within 1.5 cells, even outside the field; every "
                    + "enemy zapped is Dazed 0.25s")
            .after(ARC_DISCHARGE);
    private static final UpgradeNode PHASE_FIELD_1 = UpgradeTier.HEAD_1.node("pulse.head.phase_field.1",
            "Phase Field", PRICE)
            .withBuff(TowerBuff.range(0.2f))
            .withExtraEffect("Toll stays 2s after an enemy leaves");
    private static final UpgradeNode PHASE_FIELD_2 = UpgradeTier.HEAD_2.node("pulse.head.phase_field.2",
            "Phase Field II", PRICE)
            .withBuff(TowerBuff.range(0.15f))
            .withExtraEffect("an enemy is revealed to every tower for 2s when it gains its first Toll stack, once "
                    + "per visit")
            .after(PHASE_FIELD_1);
    private static final UpgradeNode NULL_FIELD = UpgradeTier.HEAD_3.node("pulse.head.phase_field.3", "Null Field",
            PRICE)
            .withGate(new PurposeCondition(FULL_TOLL_DEED, FULL_TOLL_SECONDS_NEEDED))
            .withExtraEffect("enemies inside are Silenced")
            .after(PHASE_FIELD_2);
    private static final UpgradeNode TRUE_SIGHT = UpgradeTier.HEAD_4.node("pulse.head.phase_field.4a", "True Sight",
            PRICE)
            .withExtraEffect("the visit's reveal lasts 4s and Dazes the enemy for 2s")
            .after(NULL_FIELD);
    private static final UpgradeNode DEAD_ZONE = UpgradeTier.HEAD_4.node("pulse.head.phase_field.4b", "Dead Zone",
            PRICE)
            .withExtraEffect("nothing inside can be healed or shielded")
            .after(NULL_FIELD);
    private static final UpgradeNode CORROSION = UpgradeTier.EXTRA_1.node("pulse.extra.field_shaping.1", "Corrosion",
            PRICE)
            .withExtraEffect("-30 armor while inside, armor stopping at 0");
    private static final UpgradeNode MIRROR_FIELD = UpgradeTier.EXTRA_2.node("pulse.extra.field_shaping.2",
            "Mirror Field", PRICE)
            .withExtraEffect("what a shield absorbs of the field's hits is dealt back to its enemy as magic")
            .after(CORROSION);
    private static final UpgradeNode UNDERTOW = UpgradeTier.EXTRA_3.node("pulse.extra.field_shaping.3", "Undertow",
            PRICE)
            .withExtraEffect("inside, enemies are chilled 25% and the chill doesn't fade while they stay, it counts "
                    + "for freezes, and they are Anchored")
            .after(MIRROR_FIELD);
    private static final UpgradeNode EVENT_HORIZON = UpgradeTier.EXTRA_4.node("pulse.extra.field_shaping.4",
            "Event Horizon", PRICE)
            .withExtraEffect("each death inside adds +5% field damage until the wave ends, up to +100%")
            .after(UNDERTOW);
    private static final UpgradeNode RATTLE_FIELD = UpgradeTier.SPECIAL.node("pulse.special.rattle_field",
            "Rattle Field", PRICE)
            .withExtraEffect("each tick, a 5% chance to add a stack of Sundered or Exposed: about one a second");
    private static final UpgradeNode SOUL_DRAIN = UpgradeTier.SPECIAL.node("pulse.special.soul_drain", "Soul Drain",
            PRICE)
            .withExtraEffect("each second inside costs 5 spirit, and the field deals +1% damage per point of spirit "
                    + "below zero; at -100 double damage and no heals or shields");
    private static final UpgradeNode KILL_ZONE = UpgradeTier.SPECIAL.node("pulse.special.kill_zone", "Kill Zone",
            PRICE)
            .withExtraEffect("enemies inside take +25% damage from every source");

    /** Chance per tick that Rattle Field adds a stack, and one in this many being Exposed rather than Sundered. */
    private static final double RATTLE_CHANCE = 0.05;
    private static final float EXPOSED_SECONDS = 4f;
    /** Soul Drain's cost each second, and the spirit at which it has taken everything. */
    private static final int SOUL_DRAIN_SPIRIT_PER_SECOND = 5;
    private static final float SPIRIT_FLOOR = -100f;
    /** Each point of spirit below zero makes the field hit this much harder under Soul Drain. */
    private static final float SOUL_DRAIN_DAMAGE_PER_POINT = 0.01f;
    /** Event Horizon stops adding at this much. */
    private static final float MAX_DEATH_BONUS = 1f;
    /** A rule that holds inside the field lasts this long after the last tick an enemy was in it. */
    private static final int INSIDE_TICKS = 3;

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE, BASE_PERKS, ARC_DISCHARGE,
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
            .with(StandardBaseSlot.RANGE_3_ID, () -> new LingeringTollPerk(1f))
            .with(OVERCHARGED_COILS_1.id(), FasterTollPerk::new)
            .with(OVERCHARGED_COILS_2.id(), FullTollBonusPerk::new)
            .with(ARC_DISCHARGE.id(), ArcDischargePerk::new)
            .with(MELTDOWN.id(), MeltdownPerk::new)
            .with(TESLA_COIL.id(), TeslaCoilPerk::new)
            .with(PHASE_FIELD_1.id(), () -> new LingeringTollPerk(1f))
            .with(PHASE_FIELD_2.id(), RevealOnEntryPerk::new)
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
    private boolean fire = false;
    private int highestToll;
    private List<Zap> zaps = List.of();
    private int lastZapTick = Integer.MIN_VALUE;
    private List<EnemyMob> previouslyInside = List.of();
    private int deathsThisWave;
    private boolean watchingWaves;
    private final WaveStartListener waveListener = () -> this.deathsThisWave = 0;

    public PulseTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.PULSE, new TowerBaseStats(DAMAGE_POINTS, RANGE, 0), context, x, y);
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        this.perks.add(node);
    }

    /** Whom the field touches and how Toll builds, as the perks in {@code owned} make it. */
    private PulseSpec spec(List<PulsePerk> owned) {
        Viewpoint view = new Viewpoint(this.centerX, this.centerY, this.rangeReal(), this.context.getBoard().scale());
        PulseSpec spec = PulseSpec.from(view);
        for (PulsePerk perk : owned) {
            spec = perk.refineSpec(spec);
        }
        return spec;
    }

    public void doTick(int gameTime) {
        PulseSpec spec = this.spec(this.perks.all());
        List<EnemyMob> inside = spec.reach().matching(this.context.enemies());
        this.fire = !inside.isEmpty();
        this.countDeaths(spec, inside);
        boolean beat = gameTime % TICKS_PER_SECOND == 0;
        List<EnemyMob> earners = spec.toll().isActive()
                ? this.tollTracker.tick(inside, spec.toll().ticksPerStack()) : List.of();
        int highest = 0;
        for (EnemyMob enemy : inside) {
            boolean startsVisit = earners.contains(enemy) && !enemy.hasEffect(EffectKind.TOLL);
            this.addToll(enemy, spec.toll(), earners.contains(enemy));
            if (startsVisit) {
                this.startVisit(enemy, spec.visit());
            }
            this.holdInside(enemy, spec, beat);
            int stacks = enemy.effectStacks(EffectKind.TOLL);
            highest = Math.max(highest, stacks);
            float factor = 1f + TOLL_DAMAGE_PER_STACK * stacks
                    + (stacks >= spec.toll().cap() ? spec.field().fullTollBonus() : 0f)
                    + Math.min(MAX_DEATH_BONUS, spec.field().perDeathBonus() * this.deathsThisWave)
                    + (spec.modes().has(FieldMode.SOUL_DRAIN) ? SOUL_DRAIN_DAMAGE_PER_POINT * Math.max(0f, -enemy.spirit()) : 0f);
            this.hitWithField(enemy, Math.round(this.damageCurrent() * factor), spec);
            this.rattle(enemy, spec);
        }
        this.highestToll = highest;
        if (spec.toll().isActive() && highest >= spec.toll().cap()) {
            this.countDeedOfSecond();
        }
        if (spec.zap().isActive() && beat && !inside.isEmpty()) {
            this.zap(spec.zap(), inside, gameTime);
        }
    }

    /**
     * Event Horizon: every enemy that was in the field last tick and has died since adds to the
     * field until the next wave starts.
     */
    private void countDeaths(PulseSpec spec, List<EnemyMob> inside) {
        if (spec.field().perDeathBonus() > 0f) {
            if (!this.watchingWaves) {
                this.context.waves().addListener(this.waveListener);
                this.watchingWaves = true;
            }
            this.deathsThisWave += (int) this.previouslyInside.stream().filter(EnemyMob::isDead).count();
        }
        this.previouslyInside = inside;
    }

    /** One field tick: a periodic magic hit, and with Mirror Field what a shield took of it dealt back. */
    private void hitWithField(EnemyMob enemy, int amount, PulseSpec spec) {
        float absorbed = spec.modes().has(FieldMode.MIRROR) ? enemy.shieldingFor(DamageType.MAGIC) : 0f;
        this.dealPeriodicDamage(enemy, Damage.magic(amount));
        if (absorbed > 0f) {
            this.dealPeriodicDamage(enemy, Damage.magic(Math.round(amount * absorbed)));
        }
    }

    /**
     * The zap: the healthiest enemy in the field, and with a chain the nearest enemies not yet
     * struck, each within reach of the last, even outside the field. Each is hit as a magic hit
     * that can crit, and Dazed.
     */
    private void zap(ZapSpec zap, List<EnemyMob> inside, int gameTime) {
        Optional<EnemyMob> first = new HighestHealthSelector().selectFrom(inside);
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
        List<EnemyMob> beside = InRangeTargetQuery.everyone((int) from.getX(), (int) from.getY(), reach)
                .matching(this.context.enemies());
        beside.removeIf(struck::contains);
        return new NearestSelector(from.getX(), from.getY()).selectFrom(beside).orElse(null);
    }

    private void strikeWithZap(EnemyMob target, ZapSpec zap) {
        float tollRaise = zap.tollRaises() ? TOLL_DAMAGE_PER_STACK * target.effectStacks(EffectKind.TOLL) : 0f;
        Damage damage = Damage.magic(Math.round(this.damageCurrent() * zap.damageFactor() * (1f + tollRaise)));
        AttackProfile attack = this.stats().attack();
        attack = attack.withCritChance(Math.min(1f, attack.critChance() + zap.critBonus()));
        this.dealDamage(target, damage, attack);
        if (zap.dazeSeconds() > 0f && !target.isDead()) {
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
     * stays. Soul Drain also takes some spirit each second, and at none left, heals and shields too.
     */
    private void holdInside(EnemyMob enemy, PulseSpec spec, boolean beat) {
        ModeSpec modes = spec.modes();
        if (modes.has(FieldMode.SILENCE)) {
            this.applyEffect(enemy, sink -> Effect.silenced(INSIDE_TICKS, sink));
        }
        if (modes.has(FieldMode.DEAD_ZONE)
                || modes.has(FieldMode.SOUL_DRAIN) && enemy.spirit() <= SPIRIT_FLOOR) {
            this.applyEffect(enemy, sink -> Effect.deadZone(INSIDE_TICKS, sink));
        }
        if (modes.has(FieldMode.CORROSION)) {
            this.applyEffect(enemy, sink -> Effect.corroded(INSIDE_TICKS, sink));
        }
        if (modes.has(FieldMode.UNDERTOW)) {
            this.applyEffect(enemy, sink -> Effect.undertow(INSIDE_TICKS, sink));
            this.applyEffect(enemy, sink -> Effect.anchored(INSIDE_TICKS, sink));
        }
        if (modes.has(FieldMode.KILL_ZONE)) {
            this.applyEffect(enemy, sink -> Effect.killZone(INSIDE_TICKS, sink));
        }
        if (beat && modes.has(FieldMode.SOUL_DRAIN)) {
            this.applyEffect(enemy, sink -> Effect.sickened(SOUL_DRAIN_SPIRIT_PER_SECOND));
        }
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
        if (!spec.modes().has(FieldMode.RATTLE) || this.context.random().nextDouble() >= RATTLE_CHANCE) {
            return;
        }
        if (this.context.random().nextIndex(2) == 0) {
            this.applyStacks(enemy, EffectKind.SUNDERED, 1);
        } else {
            this.applyEffect(enemy, sink -> Effect.exposed(Math.round(EXPOSED_SECONDS * TICKS_PER_SECOND), sink));
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
        if (spec.toll().isActive()) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Toll", "up to " + spec.toll().cap() + ", +10% each"));
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
        if (spec.field().perDeathBonus() > 0f) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Deaths this wave", String.valueOf(this.deathsThisWave)));
        }
        return lines;
    }

    private static String insideText(FieldMode mode) {
        return switch (mode) {
            case SILENCE -> "silenced";
            case DEAD_ZONE -> "no heals or shields";
            case CORROSION -> "-30 armor";
            case MIRROR -> "shields reflect";
            case UNDERTOW -> "chilled 25%, anchored";
            case KILL_ZONE -> "+25% damage taken";
            case SOUL_DRAIN -> "spirit drained";
            case RATTLE -> "sundered or exposed";
        };
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitPulseTower(this);
    }

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
