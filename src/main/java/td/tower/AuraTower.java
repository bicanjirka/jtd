package td.tower;

import td.tower.buff.TowerBuff;
import td.tower.upgrade.ClusterCondition;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeCondition;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.List;

/**
 * "Aura tower" - passive. Never attacks; instead it contributes a {@link TowerBuff} to every
 * non-aura tower whose centre falls within its range, and several stack additively. It
 * listens for towers being built and removed so a tower placed after it still picks the buff
 * up, and it unregisters its clients in {@link #doCleanup()} so selling it takes the buff away.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class AuraTower extends AbstractTower {

    public static final int PRICE = 20;
    public static final int DAMAGE = 0;
    public static final float RANGE = 1.5f;
    public static final float DEFAULT_POWER = 0.2f;

    /**
     * How many ticks between periodic "everything currently in range" passes - what
     * {@code Withering Field} (see TODO.md) will eventually drive; the counting itself is real,
     * its consequence is not yet.
     */
    private static final int WITHERING_FIELD_TICK_INTERVAL = 20;

    private static final UpgradeNode BASE_RANGE = StandardBaseSlot.rangeNode(12);
    private static final UpgradeNode AWAKEN = StandardBaseSlot.awakenNode(20);

    /**
     * More buff strength - a payoff for a deliberately grouped placement.
     */
    private static final UpgradeNode AMPLIFYING_CORE_1 = UpgradeNode.of("aura.head.amplifying_core.1",
            UpgradeSlot.HEAD, "Amplifying Core", 20)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD))
            .withGate(new ClusterCondition(2))
            .withExtraEffect("+50% buff strength");
    /**
     * More buff strength still, and the aura now also grants a fire-rate bonus.
     */
    private static final UpgradeNode AMPLIFYING_CORE_2 = UpgradeNode.of("aura.head.amplifying_core.2",
            UpgradeSlot.HEAD, "Amplifying Core II", 30)
            .withRequires(UpgradeCondition.owns(AMPLIFYING_CORE_1.id()))
            .withGate(new ClusterCondition(3))
            .withExtraEffect("+50% more buff strength, and the aura now also grants a fire-rate bonus");
    /**
     * More range.
     */
    private static final UpgradeNode RESONANCE_FIELD_1 = UpgradeNode.of("aura.head.resonance_field.1",
            UpgradeSlot.HEAD, "Resonance Field", 20)
            .withBuff(TowerBuff.range(0.3f))
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD))
            .withGate(new ClusterCondition(2));
    /**
     * More range still, and the aura no longer refuses to buff other Aura towers.
     */
    private static final UpgradeNode RESONANCE_FIELD_2 = UpgradeNode.of("aura.head.resonance_field.2",
            UpgradeSlot.HEAD, "Resonance Field II", 30)
            .withBuff(TowerBuff.range(0.25f))
            .withRequires(UpgradeCondition.owns(RESONANCE_FIELD_1.id()))
            .withGate(new ClusterCondition(3))
            .withExtraEffect("the aura no longer refuses to buff other Aura towers");
    /**
     * Every enemy standing inside the aura's range periodically gains a Vulnerable stack, once
     * that primitive exists - see TODO.md.
     */
    private static final UpgradeNode WITHERING_FIELD = UpgradeNode.of("aura.special.withering_field",
            UpgradeSlot.SPECIAL, "Withering Field", 40)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new ClusterCondition(2))
            .withExtraEffect("every few ticks, every enemy inside the aura's range gains 1 Vulnerable stack (cap 3)");

    private static final UpgradeTree TREE = UpgradeTree.of(BASE_RANGE, AWAKEN, AMPLIFYING_CORE_1, AMPLIFYING_CORE_2,
            RESONANCE_FIELD_1, RESONANCE_FIELD_2, WITHERING_FIELD);

    private volatile float power;
    private volatile boolean grantsFireRate = false;
    private volatile boolean buffsAuras = false;
    private int tickCounter = 0;

    public AuraTower(GameWorld context, int x, int y) {
        this(context, x, y, DEFAULT_POWER);
    }

    /**
     * Lets an aura tower contribute a buff stronger or weaker than the default, so two
     * aura towers can stack unequal amounts via TowerBuff's additive combine.
     */
    public AuraTower(GameWorld context, int x, int y, float power) {
        super(TowerFactory.Type.AURA, PRICE, new TowerBaseStats(DAMAGE, RANGE, 0), context, x, y);
        this.power = power;
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    /**
     * Bonuses that aren't a {@link TowerBuff} axis are applied here instead.
     */
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
     * Whether {@code other} is a tower this one amplifies: any non-Aura tower whose centre
     * falls inside this aura's range, or any tower at all once Resonance Field II is owned.
     * An aura never buffs itself.
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

    /**
     * The towers this aura is currently amplifying, as a fresh snapshot - derived the same way
     * {@link #buffFor} is, never stored. A stored bidirectional buff graph (this class's own
     * {@code clients} set, paired with a {@code List<AuraTower>} on every tower) was deliberately
     * deleted once already - two structures that must agree is a bug factory - so this stays a
     * query, not new tracked state, even though it now feeds a render line as well as the status
     * text below.
     */
    public List<Tower> buffedTowers() {
        return this.context.towers().all().stream().filter(this::buffs).toList();
    }

    /**
     * Counts ticks toward {@code Withering Field}'s periodic pass (see TODO.md) - the counting
     * plumbing is real; what a completed interval does once bought is still a no-op.
     */
    public void doTick(int gameTime) {
        this.tickCounter++;
        if (this.tickCounter >= WITHERING_FIELD_TICK_INTERVAL) {
            this.tickCounter = 0;
        }
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitAuraTower(this);
    }

    public String getInfoString() {
        return "Aura tower\n\n" +
                super.getInfoString() +
                "Increases damage and range of nearby towers by " + (this.power * 100) + "%";
    }

    public String getStatusString() {
        return "Aura tower\n\n" +
                super.getStatusString() +
                "Increases damage and range of nearby towers by " + (this.power * 100) + "%\n\n" +
                "Affects towers: " + this.buffedTowers().size();
    }
}
