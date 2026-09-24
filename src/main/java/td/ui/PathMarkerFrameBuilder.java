package td.ui;

import td.ui.render.PathMarkerBrightness;
import td.ui.render.PathMarkerDraw;
import td.ui.render.PathMarkerShape;
import td.wave.ArcLengthPath;
import td.wave.Path;
import td.wave.PathColor;
import td.wave.PathPose;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Describes the path as two layers of {@link PathMarkerDraw}s: a static trail and a moving
 * indicator, both computed from {@link Path}'s real pixel-space points via the shared
 * {@link ArcLengthPath} rather than baked into any art asset - so the visual always matches
 * whatever path a level actually defines, diagonal or curved segments included. Enemy movement
 * (see {@code AbstractEnemyMob}) is built on the same {@link ArcLengthPath}, so both move at a
 * consistent real-world pace along the same geometry.
 * <p>
 * Both layers run through the same arc-length placement code, driven only by {@link MarkerStyle}
 * below - swapping either layer's symbol, spacing, size, or brightness role is a one-line change
 * to one of the two constants, and every marker carries a facing angle regardless of shape, so a
 * static dot can become a direction-sensitive symbol without touching this class further.
 */
final class PathMarkerFrameBuilder {

    // Half a baseline enemy's pace at normal speed; deliberately not derived from simulation
    // constants.
    private static final float MOVING_CELLS_PER_SECOND = 0.4f;

    private static final MarkerStyle STATIC =
            MarkerStyle.of(PathMarkerShape.DOT, PathMarkerBrightness.STATIC, 0.5f, 0.06f);
    private static final MarkerStyle MOVING =
            MarkerStyle.of(PathMarkerShape.CHEVRON, PathMarkerBrightness.MOVING, 1.5f, 0.12f);

    private PathMarkerFrameBuilder() {
    }

    static List<PathMarkerDraw> build(Path path, PathColor color, int scale, double animationSeconds) {
        Optional<ArcLengthPath> arcLengthPath = ArcLengthPath.of(path);
        if (arcLengthPath.isEmpty()) {
            return List.of();
        }
        ArcLengthPath polyline = arcLengthPath.get();
        List<PathMarkerDraw> draws = new ArrayList<>();
        addLayer(draws, polyline, color, scale, STATIC, 0.0);
        addLayer(draws, polyline, color, scale, MOVING, animationSeconds * MOVING_CELLS_PER_SECOND * scale);
        return draws;
    }

    private static void addLayer(List<PathMarkerDraw> draws, ArcLengthPath polyline, PathColor color, int scale, MarkerStyle style, double offset) {
        float targetSpacing = style.spacingCells() * scale;
        int count = Math.max(1, (int) Math.round(polyline.totalLength() / targetSpacing));
        // Total length over count keeps spacing uniform with no seam at the wrap.
        double spacing = polyline.totalLength() / (double) count;
        float size = style.sizeCells() * scale;
        for (int i = 0; i < count; i++) {
            PathPose pose = polyline.poseAt(mod(i * spacing + offset, polyline.totalLength()));
            draws.add(new PathMarkerDraw(style.shape(), style.brightness(), color, (float) pose.position().x(),
                    (float) pose.position().y(), pose.facingRadians(), size));
        }
    }

    private static double mod(double value, double modulus) {
        double remainder = value % modulus;
        return remainder < 0 ? remainder + modulus : remainder;
    }

    /**
     * One layer's symbol, spacing, size, and brightness role - see the STATIC/MOVING constants
     * above. The actual color is a per-path value passed into {@link #build}, not part of this
     * style.
     */
    private record MarkerStyle(PathMarkerShape shape, PathMarkerBrightness brightness, float spacingCells, float sizeCells) {
        static MarkerStyle of(PathMarkerShape shape, PathMarkerBrightness brightness, float spacingCells, float sizeCells) {
            return new MarkerStyle(shape, brightness, spacingCells, sizeCells);
        }
    }
}
