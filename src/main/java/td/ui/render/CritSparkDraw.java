package td.ui.render;

/**
 * A brief, fading burst at the point a critical hit landed - not tied to any one enemy shape,
 * and not a persistent {@link StatusMarkerDraw}: a crit is a one-shot event, not an ongoing
 * status. Shaped like {@link EnemyFadeDraw} rather than {@code StatusMarkerDraw} for that
 * reason - a timed animation with a fade fraction, positioned by whoever builds it (see
 * {@code EnemyFrameBuilder.buildCritSparks}).
 *
 * @param fadeProgress {@code 0} the tick it landed, {@code 1} fully faded out
 */
public record CritSparkDraw(Palette palette, float x, float y, float scale, float fadeProgress) {
}
