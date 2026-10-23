package td;

/**
 * What the dev panel's selection row works on: nothing, the selected tower or the selected enemy.
 * Built on the game-loop thread, which owns both selections, and read by the EDT.
 *
 * @param kind        what is selected
 * @param description one short line naming it
 */
public record DevSelection(Kind kind, String description) {

    public enum Kind {
        NONE, TOWER, ENEMY
    }

    public static DevSelection none() {
        return new DevSelection(Kind.NONE, "");
    }

    public static DevSelection tower(String description) {
        return new DevSelection(Kind.TOWER, description);
    }

    public static DevSelection enemy(String description) {
        return new DevSelection(Kind.ENEMY, description);
    }
}
