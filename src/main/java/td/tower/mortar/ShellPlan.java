package td.tower.mortar;

import java.util.ArrayList;
import java.util.List;

/**
 * Which shell carries what: every third shell is a special one. With one special owned that is the
 * third of each three; with two, the first and second of each three are the two specials, in the
 * order they were bought, and the third is plain.
 *
 * @param specials the special shell types owned, in the order they were bought
 */
public record ShellPlan(List<ShellType> specials) {

    private static final int CYCLE = 3;

    public ShellPlan {
        specials = List.copyOf(specials);
    }

    public static ShellPlan none() {
        return new ShellPlan(List.of());
    }

    public ShellPlan withSpecial(ShellType type) {
        List<ShellType> next = new ArrayList<>(this.specials);
        next.add(type);
        return new ShellPlan(next);
    }

    /** The type of the shell that is number {@code shellNumber} in the Mortar's firing, counting from 1. */
    public ShellType typeOf(int shellNumber) {
        int position = (shellNumber - 1) % CYCLE;
        if (this.specials.size() == 1) {
            return position == CYCLE - 1 ? this.specials.getFirst() : ShellType.PLAIN;
        }
        return position < this.specials.size() ? this.specials.get(position) : ShellType.PLAIN;
    }
}
