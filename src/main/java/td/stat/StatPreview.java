package td.stat;

/**
 * What a row would become: its value now and with a change.
 *
 * @param stat the row both values read in
 */
public record StatPreview(StatTerm stat, float before, float after) {
}
