package td.ui.render;

import java.util.List;

/**
 * The missiles an Aerie holds, one for one, spread evenly on a ring of {@code ringRadius} round
 * ({@code centerX}, {@code centerY}), the ring already lifted to the height it circles at; the first at
 * {@code phaseRadians}, each {@code missileLength} pixels long. While {@code releaseReady}, the ring's path shows and
 * every missile wears a halo.
 */
public record NestDraw(float centerX, float centerY, float ringRadius, List<LoadedMissile> missiles,
                       double phaseRadians, float missileLength, boolean releaseReady) implements TowerEffectDraw {

    public NestDraw {
        missiles = List.copyOf(missiles);
    }
}
