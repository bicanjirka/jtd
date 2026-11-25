package td.tower.mortar;

/**
 * A shell in the order the Mortar sends it: one the magazine holds, or a Normal a volley sneaks between two it loads,
 * which no draw put there.
 *
 * @param fromMagazine whether the magazine holds it; false for a sneaked Normal
 */
public record LinedUpShell(HeldShell shell, boolean fromMagazine) {

    public static LinedUpShell held(HeldShell shell) {
        return new LinedUpShell(shell, true);
    }

    public static LinedUpShell sneaked(HeldShell shell) {
        return new LinedUpShell(shell, false);
    }

    public LinedUpShell withShell(HeldShell next) {
        return new LinedUpShell(next, this.fromMagazine);
    }
}
