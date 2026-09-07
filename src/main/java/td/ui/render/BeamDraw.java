package td.ui.render;

public record BeamDraw(Palette palette, float fromX, float fromY, float toX, float toY,
                        float strokeWidth) implements TowerEffectDraw {
}
