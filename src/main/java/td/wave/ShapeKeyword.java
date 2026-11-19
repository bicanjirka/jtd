package td.wave;

import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;

import java.util.Optional;
import java.util.function.IntFunction;
import java.util.stream.Stream;

/**
 * A wave script's spawn-type keyword: the shape it gives its slot, and whether it takes a member count. Each is a term
 * the script editor names and explains.
 */
public enum ShapeKeyword implements Term {
    FLANK("flank", false, members -> SpawnShape.flank()),
    SWARM("swarm", true, SpawnShape::swarm),
    LINE("line", true, SpawnShape::line),
    COLUMN("column", true, SpawnShape::column),
    DRIP("drip", true, SpawnShape::drip);

    private final String token;
    private final boolean takesMemberCount;
    private final IntFunction<SpawnShape> shape;

    ShapeKeyword(String token, boolean takesMemberCount, IntFunction<SpawnShape> shape) {
        this.token = token;
        this.takesMemberCount = takesMemberCount;
        this.shape = shape;
    }

    static Optional<ShapeKeyword> of(String token) {
        return Stream.of(values()).filter(keyword -> keyword.token.equals(token)).findFirst();
    }

    String token() {
        return this.token;
    }

    /** Whether it needs a member count; the others forbid one. */
    boolean takesMemberCount() {
        return this.takesMemberCount;
    }

    @Override
    public String key() {
        return "shape." + this.token;
    }

    @Override
    public String title() {
        return TermNumbers.titleCase(this);
    }

    @Override
    public TermText describe() {
        return TermText.of(switch (this) {
            case FLANK -> "sends two at once, one at each side of the path";
            case SWARM -> "sends its count at once, scattered, each smaller and weaker the more there are, though "
                    + "the whole swarm has more health and bounty than one enemy";
            case LINE -> "sends its count at once, spread evenly across the path";
            case COLUMN, DRIP -> "sends its count one after another, "
                    + TermNumbers.number((float) this.shape(2).delaySpacingSlots()) + " slots apart";
        });
    }

    /** The shape for {@code members}, which a keyword with a fixed member count ignores. */
    SpawnShape shape(int members) {
        return this.shape.apply(members);
    }
}
