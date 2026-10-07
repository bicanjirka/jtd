package td.tower;

import td.damage.Damage;
import td.damage.DamageType;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.pulse.PulsePerk;
import td.tower.pulse.PulseSpec;
import td.tower.pulse.TollPerk;
import td.tower.pulse.TollSpec;
import td.tower.pulse.TollTracker;
import td.tower.targeting.Viewpoint;
import td.tower.upgrade.BaseSlotPerks;
import td.tower.upgrade.ExclusiveChoice;
import td.tower.upgrade.OwnedPerks;
import td.tower.upgrade.PerkCatalogue;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.List;

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

    private static final BaseSlotPerks BASE_PERKS = BaseSlotPerks.none()
            .withAttune("Toll: each second an enemy stays inside adds a stack (up to 5), fading 1s after it leaves; "
                    + "each stack: +10% field damage, and every debuff on it wears off 10% slower");

    private static final UpgradeNode OVERCHARGED_COILS_1 = UpgradeTier.HEAD_1.node("pulse.head.overcharged_coils.1",
            "Overcharged Coils", PRICE)
            .withBuff(TowerBuff.damage(0.3f));
    private static final UpgradeNode OVERCHARGED_COILS_2 = UpgradeTier.HEAD_2.node("pulse.head.overcharged_coils.2",
            "Overcharged Coils II", PRICE)
            .withBuff(TowerBuff.damage(0.25f).withCritChance(0.1f))
            .after(OVERCHARGED_COILS_1);
    private static final UpgradeNode RESONANT_FIELD_1 = UpgradeTier.HEAD_1.node("pulse.head.resonant_field.1",
            "Resonant Field", PRICE)
            .withBuff(TowerBuff.range(0.2f));
    private static final UpgradeNode RESONANT_FIELD_2 = UpgradeTier.HEAD_2.node("pulse.head.resonant_field.2",
            "Resonant Field II", PRICE)
            .withBuff(TowerBuff.range(0.15f))
            .after(RESONANT_FIELD_1)
            .withExtraEffect("any invisible enemy it hits is revealed to every tower for 2s");
    private static final UpgradeNode WARDING_FIELD = UpgradeTier.SPECIAL.node("pulse.special.warding_field",
            "Warding Field", PRICE)
            .withExtraEffect("each tick, everything hit has a 10% chance to gain 1 Vulnerable stack (cap 3)");

    /** Chance per tick that each enemy hit gains a vulnerability stack. */
    private static final double WARDING_FIELD_CHANCE = 0.1;
    private static final float REVEAL_SECONDS = 2f;

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE, BASE_PERKS))
            .with(OVERCHARGED_COILS_1, OVERCHARGED_COILS_2, RESONANT_FIELD_1, RESONANT_FIELD_2, WARDING_FIELD)
            .withChoice(ExclusiveChoice.oneOf(OVERCHARGED_COILS_1, RESONANT_FIELD_1));

    private static final PerkCatalogue<PulsePerk> PERKS = PerkCatalogue.<PulsePerk>empty()
            .with(StandardBaseSlot.ATTUNE_ID, TollPerk::new);

    private final OwnedPerks<PulsePerk> perks = new OwnedPerks<>(PERKS);
    private final TollTracker tollTracker = new TollTracker();
    private boolean fire = false;
    private int highestToll;

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
        List<EnemyMob> earners = spec.toll().isActive()
                ? this.tollTracker.tick(inside, spec.toll().ticksPerStack()) : List.of();
        int highest = 0;
        for (EnemyMob enemy : inside) {
            this.addToll(enemy, spec.toll(), earners.contains(enemy));
            int stacks = enemy.effectStacks(EffectKind.TOLL);
            highest = Math.max(highest, stacks);
            float factor = 1f + TOLL_DAMAGE_PER_STACK * stacks;
            this.dealPeriodicDamage(enemy, Damage.magic(Math.round(this.damageCurrent() * factor)));
            this.applyUpgradeEffects(enemy);
        }
        this.highestToll = highest;
        if (spec.toll().isActive() && highest >= spec.toll().cap()) {
            this.countDeedOfSecond();
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

    /** Warding Field's chance of a stack, and Resonant Field II's reveal of a hidden enemy it hits. */
    private void applyUpgradeEffects(EnemyMob enemy) {
        if (this.upgrades().owns(WARDING_FIELD.id()) && this.context.random().nextDouble() < WARDING_FIELD_CHANCE) {
            this.applyStacks(enemy, EffectKind.VULNERABLE, 1);
        }
        if (this.upgrades().owns(RESONANT_FIELD_2.id()) && enemy.isHidden()) {
            this.reveal(enemy, Math.round(REVEAL_SECONDS * TICKS_PER_SECOND));
        }
    }

    public boolean isFiring() {
        return this.fire;
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
        if (this.upgrades().owns(WARDING_FIELD.id())) {
            lines.add(new BehaviourLine(BehaviourMarker.VULNERABLE, "Each tick", BehaviourLine.percent((float) WARDING_FIELD_CHANCE) + " vulnerable"));
        }
        if (this.upgrades().owns(RESONANT_FIELD_2.id())) {
            lines.add(new BehaviourLine(BehaviourMarker.REVEAL, "Reveals hidden", BehaviourLine.seconds(Math.round(REVEAL_SECONDS * TICKS_PER_SECOND))));
        }
        return lines;
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitPulseTower(this);
    }
}
