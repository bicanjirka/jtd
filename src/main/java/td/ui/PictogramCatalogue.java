package td.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/** Every pictogram by key, parsed once from the four tables; "=key" entries reuse an earlier one. */
final class PictogramCatalogue {

    private static final Map<String, Pictogram> BY_KEY = build();

    private PictogramCatalogue() {
    }

    static Optional<Pictogram> of(String key) {
        return Optional.ofNullable(BY_KEY.get(key));
    }

    static Set<String> keys() {
        return BY_KEY.keySet();
    }

    private static Map<String, Pictogram> build() {
        Map<String, Pictogram> byKey = new HashMap<>();
        Stream.of(EffectPictograms.SPECS, TowerTermPictograms.SPECS, EnemyTermPictograms.SPECS, PerkPictograms.SPECS,
                StatusPictograms.SPECS)
                .flatMap(List::stream)
                .forEach(entry -> byKey.put(entry.getKey(), Pictogram.parse(entry.getValue(),
                        key -> Objects.requireNonNull(byKey.get(key), () -> "No earlier pictogram " + key))));
        return Map.copyOf(byKey);
    }
}
