package td.enemy;

import td.board.BoardGeometry;
import td.util.ThreadConfined;
import td.wave.ArcLengthPath;
import td.wave.Path;
import td.wave.PathPose;
import td.wave.Vec2;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A mob's position along its path: arc-length distance, so curves and diagonals move at the same
 * pace as straight legs, plus a fixed formation offset.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
final class PathMotion {

    /**
     * Scale of {@link #progression()}: fine enough that mobs a fraction of a percent apart still
     * compare distinctly.
     */
    private static final int PROGRESSION_SCALE = 1_000_000;

    private final Supplier<BoardGeometry> board;
    /** Empty for a path with fewer than two points; the mob then holds at {@link #stationaryPosition}. */
    private final Optional<ArcLengthPath> arcLengthPath;
    private final Vec2 stationaryPosition;
    /**
     * Formation offset in world space, fixed at spawn from the path's facing there. Not re-rotated
     * with the path's tangent each tick: that would warp a member's speed on curves and make it
     * jump at sharp corners.
     */
    private final double offsetX;
    private final double offsetY;

    private double distanceIntoLap = 0;
    private double x;
    private double y;
    private double prevX;
    private double prevY;
    private double facingRadians = 0;

    /** @param localOffset relative to the spawn-point facing: {@code x} forward, {@code y} lateral */
    PathMotion(Path path, Vec2 localOffset, Supplier<BoardGeometry> board) {
        this.board = board;
        this.arcLengthPath = ArcLengthPath.of(path);
        List<Vec2> pathPoints = path.points();
        this.stationaryPosition = pathPoints.isEmpty() ? new Vec2(0, 0) : pathPoints.getFirst();
        double spawnFacing = this.arcLengthPath.map(arcPath -> arcPath.poseAt(0).facingRadians()).orElse(0.0);
        double cos = Math.cos(spawnFacing);
        double sin = Math.sin(spawnFacing);
        this.offsetX = localOffset.x() * cos - localOffset.y() * sin;
        this.offsetY = localOffset.x() * sin + localOffset.y() * cos;
        this.jumpTo(0);
    }

    double x() {
        return this.x;
    }

    double y() {
        return this.y;
    }

    /** Position as of the previous advance, the interpolation source. */
    double prevX() {
        return this.prevX;
    }

    double prevY() {
        return this.prevY;
    }

    /** The path's exact facing at this position, not derived from movement. */
    double facingRadians() {
        return this.facingRadians;
    }

    double distanceIntoLap() {
        return this.distanceIntoLap;
    }

    /**
     * Distance into the lap as a fraction of this path's own length, so mobs on paths of
     * different lengths rank by how close they are to leaking.
     */
    int progression() {
        return this.arcLengthPath
                .map(path -> (int) Math.round(this.distanceIntoLap / path.totalLength() * PROGRESSION_SCALE))
                .orElse(0);
    }

    boolean isOnBoard() {
        BoardGeometry geometry = this.board.get();
        return this.x >= 0 && this.x <= geometry.maxX() && this.y >= 0 && this.y <= geometry.maxY();
    }

    /** Places this mob anywhere along the path with no interpolation from where it was. */
    void jumpTo(double distanceIntoLap) {
        this.distanceIntoLap = distanceIntoLap;
        this.updatePosition();
        this.prevX = this.x;
        this.prevY = this.y;
    }

    /**
     * Moves {@code distance} along the path.
     *
     * @return true if that reaches the path's end; the position then stays where it was
     */
    boolean advance(double distance) {
        this.prevX = this.x;
        this.prevY = this.y;
        if (this.arcLengthPath.isPresent()) {
            this.distanceIntoLap += distance;
            if (this.distanceIntoLap >= this.arcLengthPath.get().totalLength()) {
                return true;
            }
        }
        this.updatePosition();
        return false;
    }

    /**
     * The offset is clamped to the board only while the path point is on the board: clamping keeps
     * a formation member targetable, and skipping it off-board lets enemies walk in from and out to
     * off-screen.
     */
    private void updatePosition() {
        if (this.arcLengthPath.isPresent()) {
            PathPose pose = this.arcLengthPath.get().poseAt(this.distanceIntoLap);
            BoardGeometry geometry = this.board.get();
            this.x = clampOntoBoard(pose.position().x(), this.offsetX, geometry.maxX());
            this.y = clampOntoBoard(pose.position().y(), this.offsetY, geometry.maxY());
            this.facingRadians = pose.facingRadians();
        } else {
            this.x = this.stationaryPosition.x() + this.offsetX;
            this.y = this.stationaryPosition.y() + this.offsetY;
        }
    }

    private static double clampOntoBoard(double pathPosition, double offset, int max) {
        if (pathPosition < 0 || pathPosition > max) {
            return pathPosition + offset;
        }
        return Math.max(0, Math.min(pathPosition + offset, max));
    }
}
