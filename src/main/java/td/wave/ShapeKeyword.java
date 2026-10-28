package td.wave;

import java.util.Optional;
import java.util.function.IntFunction;
import java.util.stream.Stream;

/** A wave script's spawn-type keyword: the shape it gives its slot, and whether it takes a member count. */
enum ShapeKeyword {
    PLATED("plated", false, members -> SpawnShape.armored()),
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

    /** The shape for {@code members}, which a keyword with a fixed member count ignores. */
    SpawnShape shape(int members) {
        return this.shape.apply(members);
    }
}
