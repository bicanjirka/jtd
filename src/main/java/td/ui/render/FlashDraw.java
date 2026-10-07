package td.ui.render;

/** A filled disc that fades: the white flash of a nuke. */
public record FlashDraw(Palette palette, float centerX, float centerY, float radius,
                        float alpha) implements TowerEffectDraw {
}
