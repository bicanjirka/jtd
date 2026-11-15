package td.ui.render;

import java.util.List;

/**
 * A thin dashed rune line in Silence's colour just inside the outline round every field that silences, so overlapping
 * fields read as one warded stretch. {@code unit} is a cell, for its stroke.
 */
public record WardOutlineDraw(List<ZoneArcDraw> edge, float unit) implements TowerEffectDraw {

    public WardOutlineDraw {
        edge = List.copyOf(edge);
    }
}
