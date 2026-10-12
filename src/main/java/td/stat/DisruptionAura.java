package td.stat;

/**
 * What an enemy does to every tower within {@code radius} pixels of it: lowers attack speed by a
 * fraction and range by cells, added to the tower's other buffs.
 */
public record DisruptionAura(float radius, float attackSpeedPenalty, float rangePenaltyCells) {

    public float radius2() {
        return this.radius * this.radius;
    }
}
