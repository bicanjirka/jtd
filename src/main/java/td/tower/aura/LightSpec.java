package td.tower.aura;

/**
 * What the Beacon's light does beyond the bare light, as its owned mechanics make it. Each {@code withX} is a copy.
 *
 * @param spotlight which enemy the beam holds; {@link Spotlight#NONE} for no beam
 * @param lesson    how the Lit towers learn
 * @param edge      what the light's edge does to the enemies inside
 * @param prism     whether every enemy inside carries the mark of each Lit tower type
 */
public record LightSpec(Spotlight spotlight, Lesson lesson, Edge edge, boolean prism) {

    /** The bare light. */
    public static LightSpec bare() {
        return new LightSpec(Spotlight.NONE, Lesson.NONE, Edge.NONE, false);
    }

    public LightSpec withSpotlight(Spotlight next) {
        return new LightSpec(next, this.lesson, this.edge, this.prism);
    }

    public LightSpec withLesson(Lesson next) {
        return new LightSpec(this.spotlight, next, this.edge, this.prism);
    }

    public LightSpec withEdge(Edge next) {
        return new LightSpec(this.spotlight, this.lesson, next, this.prism);
    }

    public LightSpec withPrism() {
        return new LightSpec(this.spotlight, this.lesson, this.edge, true);
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
