package td.tower;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.enemy.EnemyWalk;
import td.tower.buff.TowerBuff;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.upgrade.BaseSlotPerks;
import td.tower.upgrade.BuffedTowersCondition;
import td.tower.upgrade.ExclusiveChoice;
import td.tower.upgrade.NeighbourOfTypeCondition;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Never attacks; contributes a {@link TowerBuff} to other towers whose centre is within its range.
 * Several auras stack additively.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class AuraTower extends AbstractTower {

    public static final int PRICE = 20;
    public static final float DAMAGE_POINTS = 0f;
    public static final float RANGE = 1.5f;
    public static final float DEFAULT_POWER = 0.2f;

    /** How much of a jammer's effect a tower in range is spared. */
    private static final float DISRUPTION_SHIELD = 0.5f;
    /** What each Amplifying Core level adds to the buff, and the fire rate the second level grants. */
    private static final float CORE_STEP = 0.1f;
    private static final float CORE_FIRE_RATE = 0.1f;

    /** What each other tower type in reach adds to a buffed tower's XP, and the most it adds without Broadcast. */
    private static final float KINSHIP_XP_PER_TYPE = 0.05f;
    private static final int KINSHIP_XP_TYPE_CAP = 4;
    /** What Kinship adds to the buff's strength per other tower type on the Amplifying Core chain, capped alike. */
    private static final float KINSHIP_STRENGTH_PER_TYPE = 0.05f;
    private static final float TUTELAGE_XP = 0.1f;
    private static final float APPRENTICE_XP = 1f;

    /** What the aura must buff before its third head level is offered; Keen Edge also needs an aura beside it. */
    private static final int HEAD_THREE_BUFFED_TOWERS = 4;
    private static final float KEEN_EDGE_CRIT_DAMAGE = 0.5f;

    /** Ticks between periodic passes over enemies in range. */
    private static final int WITHERING_FIELD_TICK_INTERVAL = 20;

    private static final UpgradeNode AMPLIFYING_CORE_1 = UpgradeTier.HEAD_1.node("aura.head.amplifying_core.1",
            "Amplifying Core", PRICE)
            .withExtraEffect("+10% buff strength");
    private static final UpgradeNode AMPLIFYING_CORE_2 = UpgradeTier.HEAD_2.node("aura.head.amplifying_core.2",
            "Amplifying Core II", PRICE)
            .after(AMPLIFYING_CORE_1)
            .withExtraEffect("+10% more buff strength, and the aura also grants +10% fire rate");
    private static final UpgradeNode KEEN_EDGE = UpgradeTier.HEAD_3.node("aura.head.amplifying_core.3", "Keen Edge",
            PRICE)
            .withGate(new BuffedTowersCondition(HEAD_THREE_BUFFED_TOWERS)
                    .and(new NeighbourOfTypeCondition(TowerFactory.Type.AURA)))
            .withExtraEffect("buffed towers deal +50% crit damage")
            .after(AMPLIFYING_CORE_2);
    private static final UpgradeNode BROADCAST_1 = UpgradeTier.HEAD_1.node("aura.head.broadcast.1",
            "Broadcast", PRICE)
            .withBuff(TowerBuff.range(0.3f));
    private static final UpgradeNode TUTELAGE_1 = UpgradeTier.EXTRA_1.node("aura.extra.tutelage.1", "Tutelage", PRICE)
            .withExtraEffect("buffed towers earn +10% XP, on top of Kinship");
    private static final UpgradeNode SHARED_LESSONS = UpgradeTier.EXTRA_2.node("aura.extra.tutelage.2",
            "Shared Lessons", PRICE)
            .withExtraEffect("buffed towers share what they see: each earns the XP of every enemy any of them reached")
            .after(TUTELAGE_1);
    private static final UpgradeNode APPRENTICE = UpgradeTier.EXTRA_3.node("aura.extra.tutelage.3", "Apprentice",
            PRICE)
            .withExtraEffect("the buffed tower with the least XP earns double XP from every source")
            .after(SHARED_LESSONS);
    private static final UpgradeNode WITHERING_FIELD = UpgradeTier.SPECIAL.node("aura.special.withering_field",
            "Withering Field", PRICE)
            .withExtraEffect("every few ticks, every enemy inside the aura's range gains 1 Vulnerable stack (cap 3)");

    private static final BaseSlotPerks BASE_PERKS = BaseSlotPerks.none().withAttune("Kinship: buffed towers earn "
            + "+5% XP for each other tower type in range, up to +20%");

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE, BASE_PERKS))
            .with(AMPLIFYING_CORE_1, AMPLIFYING_CORE_2, KEEN_EDGE, BROADCAST_1, TUTELAGE_1, SHARED_LESSONS, APPRENTICE,
                    WITHERING_FIELD)
            .withChoice(ExclusiveChoice.oneOf(AMPLIFYING_CORE_1, BROADCAST_1));

    private volatile float power;
    private volatile boolean grantsFireRate = false;
    private int tickCounter = 0;

    public AuraTower(GameWorld context, int x, int y) {
        this(context, x, y, DEFAULT_POWER);
    }

    /** An aura with a non-default buff strength. */
    public AuraTower(GameWorld context, int x, int y, float power) {
        super(TowerFactory.Type.AURA, new TowerBaseStats(DAMAGE_POINTS, RANGE, 0), context, x, y);
        this.power = power;
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        if (node.equals(AMPLIFYING_CORE_1) || node.equals(AMPLIFYING_CORE_2)) {
            this.power += CORE_STEP;
            if (node.equals(AMPLIFYING_CORE_2)) {
                this.grantsFireRate = true;
            }
        }
    }

    @Override
    protected boolean isPassive() {
        return true;
    }

    public TowerBuff buff() {
        return this.buffWithStrength(this.power);
    }

    private TowerBuff buffWithStrength(float strength) {
        TowerBuff base = TowerBuff.amplifying(strength).withDisruptionShield(DISRUPTION_SHIELD);
        if (this.owns(KEEN_EDGE)) {
            base = base.withCritDamage(KEEN_EDGE_CRIT_DAMAGE);
        }
        return this.grantsFireRate ? base.withFireRate(CORE_FIRE_RATE) : base;
    }

    /**
     * Another tower within range. Never itself, and never another aura.
     */
    private boolean buffs(Tower other) {
        if (other == this) {
            return false;
        }
        if (other.getType() == TowerFactory.Type.AURA) {
            return false;
        }
        int dx = this.centerX - other.getX();
        int dy = this.centerY - other.getY();
        return (dx * dx + dy * dy) < this.rangeReal2();
    }

    /** An aura has no reach on the path: it earns through the towers it buffs. */
    @Override
    public boolean reached(EnemyWalk walk) {
        return false;
    }

    @Override
    public TowerBuff buffFor(Tower other) {
        if (!this.buffs(other)) {
            return TowerBuff.none();
        }
        return this.buffWithStrength(this.power + this.kinshipStrengthFor(other));
    }

    private boolean owns(UpgradeNode node) {
        return this.upgrades().owns(node.id());
    }

    private boolean isAttuned() {
        return this.upgrades().owns(StandardBaseSlot.ATTUNE_ID);
    }

    /** The other tower types Kinship counts for {@code other}: not its own, not an aura, within reach. */
    private int kinshipTypesFor(Tower other) {
        Set<TowerFactory.Type> types = this.typesInReach();
        types.remove(other.getType());
        return types.size();
    }

    /** Kinship's reach is the aura's range, or with Broadcast one cell further. */
    private Set<TowerFactory.Type> typesInReach() {
        Set<TowerFactory.Type> types = EnumSet.noneOf(TowerFactory.Type.class);
        double reach = this.rangeReal() + (this.owns(BROADCAST_1) ? this.context.getBoard().scale() : 0);
        for (Tower tower : this.context.towers().all()) {
            double dx = this.centerX - tower.getX();
            double dy = this.centerY - tower.getY();
            if (tower != this && tower.getType() != TowerFactory.Type.AURA && dx * dx + dy * dy < reach * reach) {
                types.add(tower.getType());
            }
        }
        return types;
    }

    /** On the Amplifying Core chain Kinship also strengthens the buff, up to the same four types. */
    private float kinshipStrengthFor(Tower other) {
        if (!this.isAttuned() || !this.owns(AMPLIFYING_CORE_1)) {
            return 0f;
        }
        return KINSHIP_STRENGTH_PER_TYPE * Math.min(KINSHIP_XP_TYPE_CAP, this.kinshipTypesFor(other));
    }

    /** Kinship, Tutelage and the Apprentice mark, added together; nothing for a tower this aura does not buff. */
    @Override
    public float xpBonusFor(Tower earner) {
        if (!this.buffs(earner)) {
            return 0f;
        }
        float bonus = 0f;
        if (this.isAttuned()) {
            int types = this.kinshipTypesFor(earner);
            bonus += KINSHIP_XP_PER_TYPE * (this.owns(BROADCAST_1) ? types : Math.min(KINSHIP_XP_TYPE_CAP, types));
        }
        if (this.owns(TUTELAGE_1)) {
            bonus += TUTELAGE_XP;
        }
        if (this.owns(APPRENTICE) && earner == this.apprentice()) {
            bonus += APPRENTICE_XP;
        }
        return bonus;
    }

    @Override
    public boolean sharesXp(Tower recipient, Tower earner) {
        return this.owns(SHARED_LESSONS) && this.buffs(recipient) && this.buffs(earner);
    }

    /** The buffed tower with the least XP, the earliest built on a tie; null when this aura buffs none. */
    public Tower apprentice() {
        Tower least = null;
        for (Tower tower : this.buffedTowers()) {
            if (least == null || tower.experience().xp() < least.experience().xp()) {
                least = tower;
            }
        }
        return least;
    }

    /** The towers this aura buffs, derived on each call rather than tracked. */
    public List<Tower> buffedTowers() {
        return this.context.towers().all().stream().filter(this::buffs).toList();
    }

    /** Withering Field: every interval, each enemy inside the aura, hidden or not, gains a vulnerability stack. */
    public void doTick(int gameTime) {
        this.tickCounter++;
        if (this.tickCounter < WITHERING_FIELD_TICK_INTERVAL) {
            return;
        }
        this.tickCounter = 0;
        if (this.upgrades().owns(WITHERING_FIELD.id())) {
            for (EnemyMob enemy : InRangeTargetQuery.everyone(this.centerX, this.centerY, this.rangeReal())
                    .matching(this.context.enemies())) {
                this.applyStacks(enemy, EffectKind.VULNERABLE, 1);
            }
        }
    }

    @Override
    protected List<BehaviourLine> behaviours() {
        String bonus = "+" + BehaviourLine.percent(this.power);
        List<BehaviourLine> lines = new ArrayList<>();
        lines.add(new BehaviourLine(BehaviourMarker.BUFF, "Nearby damage", bonus));
        lines.add(new BehaviourLine(BehaviourMarker.BUFF, "Nearby range", bonus));
        lines.add(new BehaviourLine(BehaviourMarker.BUFF, "Nearby disruption", "-" + BehaviourLine.percent(DISRUPTION_SHIELD)));
        if (this.grantsFireRate) {
            lines.add(new BehaviourLine(BehaviourMarker.BUFF, "Nearby fire rate", "+" + BehaviourLine.percent(CORE_FIRE_RATE)));
        }
        if (this.upgrades().owns(WITHERING_FIELD.id())) {
            lines.add(new BehaviourLine(BehaviourMarker.VULNERABLE, "Enemies inside", "vulnerable"));
        }
        if (this.isPlaced()) {
            lines.add(new BehaviourLine(BehaviourMarker.BUFF, "Buffing", this.buffedTowers().size() + " towers"));
            if (this.isAttuned()) {
                lines.add(new BehaviourLine(BehaviourMarker.BUFF, "Kinship", this.typesInReach().size() + " tower types"));
            }
        }
        return lines;
    }

    @Override
    protected String description() {
        return "Never attacks. Several auras stack, but an aura never buffs another aura.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitAuraTower(this);
    }
}
