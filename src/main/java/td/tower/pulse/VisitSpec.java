package td.tower.pulse;

/**
 * What happens to an enemy when it gains its first Toll stack of a visit: a visit starts then and
 * ends when its Toll has faded.
 *
 * @param dazeTicks how long it is Dazed; {@code 0} for not at all
 */
public record VisitSpec(int dazeTicks) {

    /** Nothing happens. */
    public static VisitSpec none() {
        return new VisitSpec(0);
    }

    public VisitSpec dazing(int dazeTicks) {
        return new VisitSpec(dazeTicks);
    }
}
