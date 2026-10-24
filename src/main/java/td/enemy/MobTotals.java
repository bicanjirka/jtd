package td.enemy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * What an observed enemy took while it was observed, added up from its events.
 *
 * @param landed   units that reached health, by source and damage type ({@code Sniper@7,4 physical}), in the
 *                 order each first landed
 * @param absorbed units its shields took
 * @param overkill units of the killing hit that health could not take
 */
public record MobTotals(Map<String, Integer> landed, int absorbed, int dodges, int overkill) {

    private static final MobTotals NONE = new MobTotals(Map.of(), 0, 0, 0);

    public MobTotals {
        landed = Collections.unmodifiableMap(new LinkedHashMap<>(landed));
    }

    public static MobTotals none() {
        return NONE;
    }

    /** These totals with {@code event} counted; an event that takes nothing leaves them as they are. */
    public MobTotals plus(MobEvent event) {
        return switch (event) {
            case MobEvent.Hit hit -> {
                Map<String, Integer> more = new LinkedHashMap<>(this.landed);
                String source = hit.cause().from() + " " + hit.outcome().landed().type().name().toLowerCase(Locale.ROOT);
                more.merge(source, hit.outcome().landed().amount(), Integer::sum);
                yield new MobTotals(more, this.absorbed + hit.outcome().absorbed(), this.dodges, this.overkill);
            }
            case MobEvent.Dodged ignored -> new MobTotals(this.landed, this.absorbed, this.dodges + 1, this.overkill);
            case MobEvent.Killed killed -> new MobTotals(this.landed, this.absorbed, this.dodges,
                    this.overkill + killed.overkill());
            default -> this;
        };
    }
}
