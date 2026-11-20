package td.tower.mortar;

import td.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The shells a Mortar has ready, front first: the front one is the one loading. Firing takes shells off the front and
 * draws new ones at the back; a shell of a kind the tower no longer owns leaves, and the ones behind move up.
 */
public record Magazine(List<ShellKind> shells) {

    public static final int SIZE = 10;

    private static final Magazine OPENER = new Magazine(List.of(ShellKind.NORMAL, ShellKind.NORMAL, ShellKind.HEAVY,
            ShellKind.NORMAL, ShellKind.NORMAL, ShellKind.NORMAL, ShellKind.NORMAL, ShellKind.HEAVY, ShellKind.NORMAL,
            ShellKind.NORMAL));

    public Magazine {
        shells = List.copyOf(shells);
    }

    /** What a new Mortar has loaded, the same for every one. */
    public static Magazine opener() {
        return OPENER;
    }

    /** A magazine drawn full from {@code spec}, front to back, as one that has fired long enough holds. */
    public static Magazine drawn(MagazineSpec spec, RandomSource random) {
        return filled(new ArrayList<>(), spec, random);
    }

    /** The front shell, the one loading. */
    public ShellKind front() {
        return this.shells.getFirst();
    }

    /** The {@code count} front shells, as many as it holds. */
    public List<ShellKind> front(int count) {
        return this.shells.subList(0, Math.min(count, this.shells.size()));
    }

    /** Its shells with the {@code count} front ones fired and the back filled again. */
    public Magazine fired(int count, MagazineSpec spec, RandomSource random) {
        List<ShellKind> left = new ArrayList<>(this.shells.subList(Math.min(count, this.shells.size()),
                this.shells.size()));
        return filled(left, spec, random);
    }

    /** Its shells with those {@code spec} no longer owns taken out and the back filled again; itself when nothing left. */
    public Magazine keptTo(MagazineSpec spec, RandomSource random) {
        if (this.shells.size() == SIZE && this.shells.stream().allMatch(spec::owns)) {
            return this;
        }
        List<ShellKind> left = new ArrayList<>(this.shells.stream().filter(spec::owns).toList());
        return filled(left, spec, random);
    }

    /** {@code left} drawn full at the back: a Heavy behind a Heavy fuses with it into a Nuke when Heavies fuse. */
    private static Magazine filled(List<ShellKind> left, MagazineSpec spec, RandomSource random) {
        while (left.size() < SIZE) {
            Optional<ShellKind> inFront = left.isEmpty() ? Optional.empty() : Optional.of(left.getLast());
            ShellKind drawn = spec.draw(inFront, random);
            if (spec.fuses() && drawn == ShellKind.HEAVY && inFront.filter(kind -> kind == ShellKind.HEAVY).isPresent()) {
                left.set(left.size() - 1, ShellKind.NUKE);
            } else {
                left.add(drawn);
            }
        }
        return new Magazine(left);
    }
}
