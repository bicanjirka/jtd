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
public final class TowerAura extends AbstractTower {

    public static final int PRICE = 20;
    public static final int DAMAGE = 0;
    public static final float RANGE = 1.5f;
    public static final float DEFAULT_POWER = 0.2f;

    private final float power;

    public TowerAura(GameWorld context, int x, int y) {
        this(context, x, y, DEFAULT_POWER);
    }

    /**
     * Lets an aura tower contribute a buff stronger or weaker than the default, so two
     * aura towers can stack unequal amounts via TowerBuff's additive combine.
     */
    public TowerAura(GameWorld context, int x, int y, float power) {
        super(TowerFactory.Type.aura, PRICE, DAMAGE, RANGE, 0, context, x, y);
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
        if (other == this || other.getType() == TowerFactory.Type.aura) {
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

    /** How many towers this aura is currently amplifying - counted, not tracked. */
    private long buffedTowerCount() {
        return this.context.towers().all().stream().filter(this::buffs).count();
    }

    public void doTick(int gameTime) {
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitTowerAura(this);
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
                "Affects towers: " + this.buffedTowerCount();
    }
}
