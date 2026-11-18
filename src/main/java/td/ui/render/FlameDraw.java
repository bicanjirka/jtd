package td.ui.render;

import java.util.List;

/**
 * Every stream of the frame, painted in one pass so cones that cross merge into one flame, and every enemy caught in
 * one; {@code cellPixels} is a cell's size on the board, {@code seconds} the tick clock its tongues waver on.
 */
public record FlameDraw(List<Stream> streams, List<Caught> caught, float cellPixels, float seconds)
        implements TowerEffectDraw {

    public FlameDraw {
        streams = List.copyOf(streams);
        caught = List.copyOf(caught);
    }

    /**
     * One stream from its nozzle at ({@code x}, {@code y}) along {@code headingRadians}, {@code reach} long and
     * {@code halfWidthRadians} wide either side; {@code heat} 0..1 from its fuel, {@code coreHalfWidthRadians} its
     * Core's half-width (0 without one), in Soulfire's colours while {@code soulfire}; {@code flicker} picks its edge.
     */
    public record Stream(float x, float y, float headingRadians, float reach, float halfWidthRadians, float heat,
                         float coreHalfWidthRadians, boolean soulfire, int flicker) {
    }

    /**
     * An enemy of {@code look} standing in a stream at ({@code x}, {@code y}), its body {@code bodyScale} big and
     * facing {@code facingRadians}, as hot as {@code heat} from what it is fed, facing the nozzle at ({@code nozzleX},
     * {@code nozzleY}).
     */
    public record Caught(EnemyLook look, float x, float y, float bodyScale, float facingRadians, float heat,
                         float nozzleX, float nozzleY, boolean soulfire) {
    }
}
