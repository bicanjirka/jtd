package td;

/**
 * What the dev panel's selection row works on: nothing, the selected tower or the selected enemy.
 * Built on the game-loop thread, which owns both selections, and read by the EDT.
 *
 * @param kind        what is selected
 * @param description its name and, for an enemy, its id first, so a cut-off name keeps it: the board
 *                    already shows the rest
 * @param observed    whether the selected enemy is observed
 */
public record DevSelection(Kind kind, String description, boolean observed) {

    public enum Kind {
        NONE, TOWER, ENEMY
    }

    public static DevSelection none() {
        return new DevSelection(Kind.NONE, "", false);
    }

    public static DevSelection tower(String description) {
        return new DevSelection(Kind.TOWER, description, false);
    }

    public static DevSelection enemy(String description, boolean observed) {
        return new DevSelection(Kind.ENEMY, description, observed);
    }
}
