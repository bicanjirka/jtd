package td.stat;

import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.List;

/**
 * Where enemies disrupt towers this tick. Enemies add their zone during the enemies phase, after it
 * is cleared; towers sample it in the towers phase. Neither side knows the other.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class DisruptionField {

    private final List<Zone> zones = new ArrayList<>();

    public void clear() {
        this.zones.clear();
    }

    public void add(double x, double y, DisruptionAura aura) {
        this.zones.add(new Zone(x, y, aura));
    }

    /** Every zone reaching {@code (x, y)}, summed; {@link DisruptionPenalty#none()} if none does. */
    public DisruptionPenalty penaltyAt(double x, double y) {
        DisruptionPenalty total = DisruptionPenalty.none();
        for (Zone zone : this.zones) {
            double dx = zone.x() - x;
            double dy = zone.y() - y;
            if (dx * dx + dy * dy <= zone.aura().radius2()) {
                total = total.plus(new DisruptionPenalty(zone.aura().fireRatePenalty(), zone.aura().rangePenalty()));
            }
        }
        return total;
    }

    private record Zone(double x, double y, DisruptionAura aura) {
    }
}
