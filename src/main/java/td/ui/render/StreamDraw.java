package td.ui.render;

import java.util.List;

/**
 * A flame stream from a muzzle at {@code originX, originY} along {@code headingRadians}: nested tongues, the outermost
 * first, each a flame with ragged tips. {@code flicker} picks where the tips fall, so the flame changes only when it
 * does.
 */
public record StreamDraw(float originX, float originY, float headingRadians, List<Tongue> tongues, int flicker)
        implements TowerEffectDraw {

    public StreamDraw {
        tongues = List.copyOf(tongues);
    }

    /** One flame of the stream: {@code reach} pixels long, {@code halfWidthRadians} either side, with {@code tips}. */
    public record Tongue(Palette palette, float alpha, float reach, float halfWidthRadians, int tips) {
    }
}
