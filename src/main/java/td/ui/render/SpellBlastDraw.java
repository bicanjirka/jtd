package td.ui.render;

/**
 * A spellcasting Burst's blast at {@code x, y}: full damage within {@code core} pixels, the edge share out to
 * {@code edge}. {@code progress} runs 0..1 over the impact, {@code scale} is a cell in pixels, {@code seed} varies it.
 */
public record SpellBlastDraw(SpellBlastLook look, float x, float y, float core, float edge, float progress,
                             int scale, int seed) implements TowerEffectDraw {
}
