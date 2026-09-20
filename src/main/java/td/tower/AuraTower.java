package td.tower;

import td.tower.buff.TowerBuff;
import td.util.GameWorld;

import java.util.List;

/**
 * "Aura tower" - passive. Never attacks; instead it contributes a {@link TowerBuff} to every
 * non-aura tower whose centre falls within its range, and several stack additively. It
 * listens for towers being built and removed so a tower placed after it still picks the buff
 * up, and it unregisters its clients in {@link #doCleanup()} so selling it takes the buff away.
 */
public final class AuraTower extends AbstractTower {

    public static final int PRICE = 20;
    public static final int DAMAGE = 0;
    public static final float RANGE = 1.5f;
    public static final float DEFAULT_POWER = 0.2f;

    private final float power;

    public AuraTower(GameWorld context, int x, int y) {
        this(context, x, y, DEFAULT_POWER);
    }

    /**
     * Lets an aura tower contribute a buff stronger or weaker than the default, so two
     * aura towers can stack unequal amounts via TowerBuff's additive combine.
     */
    public AuraTower(GameWorld context, int x, int y, float power) {
        super(TowerFactory.Type.AURA, PRICE, DAMAGE, RANGE, 0, 0f, context, x, y);
        this.power = power;
    }

    @Override
    protected boolean isPassive() {
        return true;
    }

    public TowerBuff buff() {
        return TowerBuff.amplifying(this.power);
    }

    /**
     * Whether {@code other} is a tower this one amplifies: any non-Aura tower whose centre
     * falls inside this aura's range. An aura never buffs another aura, and never itself.
     */
    private boolean buffs(Tower other) {
        if (other == this || other.getType() == TowerFactory.Type.AURA) {
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

    public void doTick(int gameTime) {
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
