package td.tower.pulse;

import td.effect.DragStrength;
import td.effect.FieldStrength;

/**
 * How the field holds the enemies inside: how hard it drags them, and whether it stills them at full Toll.
 *
 * @param dragAtFull      the drag at full field strength, in points of speed out of a hundred; the base drag alone
 *                        never grows with Toll
 * @param centreDrag      the drag at the field's very centre, falling to the edge's at the rim; {@code 0} for none
 * @param stasisTicks     how long an enemy reaching full Toll is Dazed and Stilled; {@code 0} for never
 * @param ticksPerToll    how often a Stilled enemy loses a Toll stack while it stays Dazed
 */
public record HoldSpec(int dragAtFull, int centreDrag, int stasisTicks, int ticksPerToll) {

    /** The base field: a flat drag, no centre, no stasis. */
    public static HoldSpec base() {
        return new HoldSpec(DragStrength.BASE, 0, 0, 1);
    }

    /** Drags {@code atFull} at full field strength, growing with Toll. */
    public HoldSpec deepeningTo(int atFull) {
        return new HoldSpec(Math.max(this.dragAtFull, atFull), this.centreDrag, this.stasisTicks, this.ticksPerToll);
    }

    /** Drags {@code atCentre} at the field's centre. */
    public HoldSpec wellOf(int atCentre) {
        return new HoldSpec(this.dragAtFull, atCentre, this.stasisTicks, this.ticksPerToll);
    }

    /** Dazes and stills an enemy at full Toll for {@code ticks}, taking a Toll stack every {@code ticksPerToll}. */
    public HoldSpec stilling(int ticks, int ticksPerToll) {
        return new HoldSpec(this.dragAtFull, this.centreDrag, ticks, ticksPerToll);
    }

    public boolean stills() {
        return this.stasisTicks > 0;
    }

    /**
     * The drag on an enemy held at {@code strength} stacks ({@link FieldStrength}), {@code fromCentre} of the way from
     * the centre to the rim (0 to 1).
     */
    public int dragAt(int strength, float fromCentre) {
        int edge = this.dragAtFull > DragStrength.BASE ? DragStrength.at(this.dragAtFull, strength) : DragStrength.BASE;
        if (this.centreDrag <= edge) {
            return edge;
        }
        float inward = 1f - Math.clamp(fromCentre, 0f, 1f);
        return Math.min(DragStrength.MOST, Math.round(edge + (this.centreDrag - edge) * inward));
    }
}
