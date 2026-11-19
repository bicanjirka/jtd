package td.tower.seeker;

import td.tower.DrawnShares;

import java.util.List;
import java.util.Map;

/**
 * An Aerie's nest as its pane shows it: the missiles it holds, the next to leave first, and the share of the new
 * missiles each kind it can draw comes to, in the kinds' order.
 */
public record NestView(List<MissileKind> missiles, Map<MissileKind, Float> chances) {

    public NestView {
        missiles = List.copyOf(missiles);
        chances = DrawnShares.of(MissileKind.class, chances);
    }
}
