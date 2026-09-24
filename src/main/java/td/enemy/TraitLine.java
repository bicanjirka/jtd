package td.enemy;

/**
 * One trait slot for the player, short enough for one row: {@code "Resist all"} and
 * {@code "-50%"}, with the marker the board draws for it.
 *
 * @param value empty when the label says everything
 */
public record TraitLine(TraitMarker marker, String label, String value) {

    public static TraitLine of(TraitMarker marker, String label) {
        return new TraitLine(marker, label, "");
    }
}
