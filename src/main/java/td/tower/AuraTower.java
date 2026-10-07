package td.tower;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.enemy.EnemyWalk;
import td.tower.buff.TowerBuff;
import td.tower.targeting.InRangeTargetQuery;
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

    /** How much of a jammer's effect a tower in range is spared. */
    private static final float DISRUPTION_SHIELD = 0.5f;
    /** What each Amplifying Core level adds to the buff, and the fire rate the second level grants. */
    private static final float CORE_STEP = 0.1f;
    private static final float CORE_FIRE_RATE = 0.1f;

    /** Ticks between periodic passes over enemies in range. */
    private static final int WITHERING_FIELD_TICK_INTERVAL = 20;

    private static final UpgradeNode AMPLIFYING_CORE_1 = UpgradeTier.HEAD_1.node("aura.head.amplifying_core.1",
            "Amplifying Core", PRICE)
            .withExtraEffect("+10% buff strength");
    private static final UpgradeNode AMPLIFYING_CORE_2 = UpgradeTier.HEAD_2.node("aura.head.amplifying_core.2",
            "Amplifying Core II", PRICE)
            .after(AMPLIFYING_CORE_1)
            .withExtraEffect("+10% more buff strength, and the aura also grants +10% fire rate");
    private static final UpgradeNode BROADCAST_1 = UpgradeTier.HEAD_1.node("aura.head.broadcast.1",
            "Broadcast", PRICE)
            .withBuff(TowerBuff.range(0.3f));
    private static final UpgradeNode WITHERING_FIELD = UpgradeTier.SPECIAL.node("aura.special.withering_field",
            "Withering Field", PRICE)
            .withExtraEffect("every few ticks, every enemy inside the aura's range gains 1 Vulnerable stack (cap 3)");

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE))
            .with(AMPLIFYING_CORE_1, AMPLIFYING_CORE_2, BROADCAST_1, WITHERING_FIELD)
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
        TowerBuff base = TowerBuff.amplifying(this.power).withDisruptionShield(DISRUPTION_SHIELD);
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
