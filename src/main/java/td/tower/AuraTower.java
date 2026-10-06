package td.tower;

import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.upgrade.ClusterCondition;
import td.tower.upgrade.ExclusiveChoice;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.List;

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

    /** Ticks between periodic passes over enemies in range. */
    private static final int WITHERING_FIELD_TICK_INTERVAL = 20;

    private static final UpgradeNode AMPLIFYING_CORE_1 = UpgradeTier.HEAD_1.node("aura.head.amplifying_core.1",
            "Amplifying Core", PRICE)
            .withGate(new ClusterCondition(2))
            .withExtraEffect("+50% buff strength");
    private static final UpgradeNode AMPLIFYING_CORE_2 = UpgradeTier.HEAD_2.node("aura.head.amplifying_core.2",
            "Amplifying Core II", PRICE)
            .after(AMPLIFYING_CORE_1)
            .withGate(new ClusterCondition(3))
            .withExtraEffect("+50% more buff strength, and the aura now also grants a fire-rate bonus");
    private static final UpgradeNode RESONANCE_FIELD_1 = UpgradeTier.HEAD_1.node("aura.head.resonance_field.1",
            "Resonance Field", PRICE)
            .withBuff(TowerBuff.range(0.3f))
            .withGate(new ClusterCondition(2));
    private static final UpgradeNode RESONANCE_FIELD_2 = UpgradeTier.HEAD_2.node("aura.head.resonance_field.2",
            "Resonance Field II", PRICE)
            .withBuff(TowerBuff.range(0.25f))
            .after(RESONANCE_FIELD_1)
            .withGate(new ClusterCondition(3))
            .withExtraEffect("the aura no longer refuses to buff other Aura towers");
    private static final UpgradeNode WITHERING_FIELD = UpgradeTier.SPECIAL.node("aura.special.withering_field",
            "Withering Field", PRICE)
            .withGate(new ClusterCondition(2))
            .withExtraEffect("every few ticks, every enemy inside the aura's range gains 1 Vulnerable stack (cap 3)");

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE))
            .with(AMPLIFYING_CORE_1, AMPLIFYING_CORE_2, RESONANCE_FIELD_1, RESONANCE_FIELD_2, WITHERING_FIELD)
            .withChoice(ExclusiveChoice.oneOf(AMPLIFYING_CORE_1, RESONANCE_FIELD_1));

    private volatile float power;
    private volatile boolean grantsFireRate = false;
    private volatile boolean buffsAuras = false;
    private int tickCounter = 0;

    public AuraTower(GameWorld context, int x, int y) {
        this(context, x, y, DEFAULT_POWER);
    }

    /** An aura with a non-default buff strength. */
    public AuraTower(GameWorld context, int x, int y, float power) {
        super(TowerFactory.Type.AURA, PRICE, new TowerBaseStats(DAMAGE_POINTS, RANGE, 0), context, x, y);
        this.power = power;
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        if (node.equals(AMPLIFYING_CORE_1) || node.equals(AMPLIFYING_CORE_2)) {
            this.power *= 1.5f;
            if (node.equals(AMPLIFYING_CORE_2)) {
                this.grantsFireRate = true;
            }
        } else if (node.equals(RESONANCE_FIELD_2)) {
            this.buffsAuras = true;
        }
    }

    @Override
    protected boolean isPassive() {
        return true;
    }

    public TowerBuff buff() {
        TowerBuff base = TowerBuff.amplifying(this.power);
        return this.grantsFireRate ? base.withFireRate(this.power) : base;
    }

    /**
     * Another tower within range; auras only once the upgrade allowing it is owned. Never itself.
     */
    private boolean buffs(Tower other) {
        if (other == this) {
            return false;
        }
        if (!this.buffsAuras && other.getType() == TowerFactory.Type.AURA) {
            return false;
        }
        int dx = this.centerX - other.getX();
        int dy = this.centerY - other.getY();
        return (dx * dx + dy * dy) < this.rangeReal2();
    }

    @Override
    public TowerBuff buffFor(Tower other) {
        return this.buffs(other) ? this.buff() : TowerBuff.none();
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
                this.applyVulnerable(enemy, 1);
            }
        }
    }

    @Override
    protected List<BehaviourLine> behaviours() {
        String bonus = "+" + BehaviourLine.percent(this.power);
        List<BehaviourLine> lines = new ArrayList<>();
        lines.add(new BehaviourLine(BehaviourMarker.BUFF, "Nearby damage", bonus));
        lines.add(new BehaviourLine(BehaviourMarker.BUFF, "Nearby range", bonus));
        if (this.grantsFireRate) {
            lines.add(new BehaviourLine(BehaviourMarker.BUFF, "Nearby fire rate", bonus));
        }
        if (this.upgrades().owns(WITHERING_FIELD.id())) {
            lines.add(new BehaviourLine(BehaviourMarker.VULNERABLE, "Enemies inside", "vulnerable"));
        }
        if (this.isPlaced()) {
            lines.add(new BehaviourLine(BehaviourMarker.BUFF, "Buffing", this.buffedTowers().size() + " towers"));
        }
        return lines;
    }

    @Override
    protected String description() {
        return "Never attacks. Several auras stack.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitAuraTower(this);
    }
}
