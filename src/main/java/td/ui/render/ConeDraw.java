package td.ui.render;

/**
 * One travelling flame wave from a {@code td.tower.CinderTower} shot: a wedge from
 * ({@code originX}, {@code originY}) in {@code headingRadians}, {@code halfWidthRadians} either
 * side, extending out to {@code maxRadius} - the same heading/half-width the wave's own
 * {@code InWedgeTargetQuery} decides hits against, captured once when it fired. Carries a 0..1
 * {@code progress} rather than a stored current radius/alpha, mirroring {@link EffectPulseDraw}'s
 * idiom: the backend grows the drawn wedge from {@code progress} and fades it out as it travels
 * farther, rather than this record tracking either value itself.
 */
public record ConeDraw(Palette palette, float originX, float originY, float headingRadians,
                       float maxRadius, float halfWidthRadians, float progress) implements TowerEffectDraw {
}
