package td.ui;

import td.ui.render.BoardDraw;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A board visual with its place in the paint order: its height and its foot, the screen row of the ground it stands
 * on. What lies flat keeps the order it was built in, so a mark never flickers over its neighbour.
 */
record PlacedDraw(DrawHeight height, float foot, BoardDraw draw) {

    private static final Comparator<PlacedDraw> ORDER = (a, b) -> {
        int byHeight = a.height.compareTo(b.height);
        return byHeight != 0 ? byHeight : Float.compare(a.foot, b.foot);
    };

    static PlacedDraw of(BoardDraw draw, float foot) {
        DrawHeight height = DrawHeight.of(draw);
        return new PlacedDraw(height, height == DrawHeight.GROUND ? 0f : foot, draw);
    }

    /** {@code placed} in paint order; a stable sort, so ties keep the order they were built in. */
    static List<BoardDraw> inOrder(List<PlacedDraw> placed) {
        placed.sort(ORDER);
        List<BoardDraw> draws = new ArrayList<>(placed.size());
        for (PlacedDraw draw : placed) {
            draws.add(draw.draw);
        }
        return draws;
    }
}
