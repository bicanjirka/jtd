package td.ui.render;

/**
 * The missiles banked in a Seeker's nest, orbiting it: {@code count} small missiles spread evenly on
 * a circle of {@code orbitRadius} around the tower, the first at {@code phaseRadians}.
 */
public record NestDraw(Palette palette, float centerX, float centerY, float orbitRadius, int count,
                       double phaseRadians) implements TowerEffectDraw {
}
