package td.tower.mortar;

import java.util.ArrayList;
import java.util.List;

/**
 * Which shell carries what. Every third shell is a special one: with one special owned that is the
 * third of each three; with two, the first and second of each three are the two specials, in the
 * order they were bought, and the third is plain. A nuke, when owned, is every {@code nukeEvery}th
 * shell, never carries a special, and the special pattern runs on the shells in between.
 *
 * @param specials  the special shell types owned, in the order they were bought
 * @param nukeEvery which shell is a nuke, {@code 0} for none
 */
public record ShellPlan(List<ShellType> specials, int nukeEvery) {

    private static final int CYCLE = 3;

    public ShellPlan {
        specials = List.copyOf(specials);
    }

    public static ShellPlan none() {
        return new ShellPlan(List.of(), 0);
    }

    public ShellPlan withSpecial(ShellType type) {
        List<ShellType> next = new ArrayList<>(this.specials);
        next.add(type);
        return new ShellPlan(next, this.nukeEvery);
    }

    public ShellPlan withNukeEvery(int nukeEvery) {
        return new ShellPlan(this.specials, nukeEvery);
    }

    /** The type of the shell that is number {@code shellNumber} in the Mortar's firing, counting from 1. */
    public ShellType typeOf(int shellNumber) {
        if (this.nukeEvery > 0 && shellNumber % this.nukeEvery == 0) {
            return ShellType.NUKE;
        }
        int number = this.nukeEvery > 0 ? shellNumber - shellNumber / this.nukeEvery : shellNumber;
        int position = (number - 1) % CYCLE;
        if (this.specials.size() == 1) {
            return position == CYCLE - 1 ? this.specials.getFirst() : ShellType.PLAIN;
        }
        return position < this.specials.size() ? this.specials.get(position) : ShellType.PLAIN;
    }
}
