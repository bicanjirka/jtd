package td.tower.aura;

/**
 * What the Beacon's light does, as its owned mechanics make it. Each {@code withX} is a copy.
 *
 * @param spotlight which enemy the beam holds; {@link Spotlight#NONE} for no beam
 * @param lesson    how the Lit towers learn
 * @param edge      what the light's edge does to the enemies inside
 * @param prism     whether every enemy inside carries the mark of each Lit tower type
 * @param jamTaken  the share of the jam and the blinding a Lit tower still takes; 1 when the light lights nothing
 * @param reveals   whether every enemy inside is Revealed
 * @param litsSelf  whether the Beacon is Lit by its own light
 * @param xpFloor   the XP the light lifts every Lit tower to; 0 for none
 */
public record LightSpec(Spotlight spotlight, Lesson lesson, Edge edge, boolean prism, float jamTaken, boolean reveals,
                        boolean litsSelf, int xpFloor) {

    /** The bare light: it lights nothing and reveals nothing. */
    public static LightSpec bare() {
        return new LightSpec(Spotlight.NONE, Lesson.NONE, Edge.NONE, false, 1f, false, false, 0);
    }

    public LightSpec withSpotlight(Spotlight next) {
        return new LightSpec(next, this.lesson, this.edge, this.prism, this.jamTaken, this.reveals, this.litsSelf,
                this.xpFloor);
    }

    public LightSpec withLesson(Lesson next) {
        return new LightSpec(this.spotlight, next, this.edge, this.prism, this.jamTaken, this.reveals, this.litsSelf,
                this.xpFloor);
    }

    public LightSpec withEdge(Edge next) {
        return new LightSpec(this.spotlight, this.lesson, next, this.prism, this.jamTaken, this.reveals, this.litsSelf,
                this.xpFloor);
    }

    public LightSpec withPrism() {
        return new LightSpec(this.spotlight, this.lesson, this.edge, true, this.jamTaken, this.reveals, this.litsSelf,
                this.xpFloor);
    }

    /** The light lets only {@code factor} of what it lets through now reach a Lit tower. */
    public LightSpec withJamTakenTimes(float factor) {
        return new LightSpec(this.spotlight, this.lesson, this.edge, this.prism, this.jamTaken * factor, this.reveals,
                this.litsSelf, this.xpFloor);
    }

    public LightSpec withReveal() {
        return new LightSpec(this.spotlight, this.lesson, this.edge, this.prism, this.jamTaken, true, this.litsSelf,
                this.xpFloor);
    }

    public LightSpec withLitSelf() {
        return new LightSpec(this.spotlight, this.lesson, this.edge, this.prism, this.jamTaken, this.reveals, true,
                this.xpFloor);
    }

    public LightSpec withXpFloor(int next) {
        return new LightSpec(this.spotlight, this.lesson, this.edge, this.prism, this.jamTaken, this.reveals,
                this.litsSelf, Math.max(this.xpFloor, next));
    }

    /** How the Lit towers learn: the Tier I Lessons pick. */
    public enum Lesson {
        NONE,
        KINSHIP,
        MENTOR,
        BREAKTHROUGH
    }

    /** What the light's edge does: the Tier II pick. */
    public enum Edge {
        NONE,
        DAYLIGHT,
        GLARE
    }
}
