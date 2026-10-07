package td.ui.render;

/** One puff of a missile's smoke trail: where it was, how wide the puff has spread, and how faint. */
public record SmokeDraw(Palette palette, float x, float y, float radius, float alpha) implements ProjectileDraw {
}
