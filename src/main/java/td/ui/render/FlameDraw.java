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
     * One stream along {@code headingRadians} from its tower's centre at ({@code x}, {@code y}), shown from its nozzle,
     * {@code inner} out, to {@code reach}, {@code halfWidthRadians} wide either side; {@code heat} 0..1 from its fuel.
     * {@code middleHalfWidthRadians} and {@code coreHalfWidthRadians} are the half-widths of the bands its nodes give
     * it (0 for none) and {@code coreHeat} how hot its core burns; it is Soulfire or Dragon's Breath by those flags,
     * and {@code flicker} picks its tips.
     */
    public record Stream(float x, float y, float headingRadians, float inner, float reach, float halfWidthRadians,
                         float heat, float middleHalfWidthRadians, float coreHalfWidthRadians, float coreHeat,
                         boolean soulfire, boolean dragon, int flicker) {
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
