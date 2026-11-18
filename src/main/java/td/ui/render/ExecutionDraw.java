package td.ui.render;

/**
 * An execution, {@code progress} 0..1 after it killed the enemy standing at ({@code x}, {@code groundY}), its point on
 * the ground, with its body's centre {@code bodyLift} pixels above it; {@code seed} scatters its shards.
 */
public record ExecutionDraw(float x, float groundY, float bodyLift, float cellPixels, float progress, int seed)
        implements TowerEffectDraw {
}
