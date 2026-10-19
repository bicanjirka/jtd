package td.ui.render;

/**
 * An Obelisk's Ley Line, from its field's centre to the field it links to, in the caster's tint. {@code pinned} draws
 * it dashed; {@code alpha} (0..1) brightens it while an enemy stands on it.
 */
public record LeyLineDraw(Palette palette, float fromX, float fromY, float toX, float toY, boolean pinned, float alpha)
        implements TowerEffectDraw {
}
