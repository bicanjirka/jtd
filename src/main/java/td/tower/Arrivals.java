package td.tower;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/** What a tower has in flight that arrives now. */
final class Arrivals {

    private Arrivals() {
    }

    /**
     * Takes out of {@code flying}, in its order, everything that lands by {@code now}: a shot fired later may land
     * sooner when its flight is shorter.
     */
    static <T> List<T> takeDue(List<T> flying, ToIntFunction<T> landsAt, int now) {
        if (flying.isEmpty()) {
            return List.of();
        }
        List<T> due = new ArrayList<>();
        flying.removeIf(item -> {
            boolean arrived = landsAt.applyAsInt(item) <= now;
            if (arrived) {
                due.add(item);
            }
            return arrived;
        });
        return due;
    }
}
