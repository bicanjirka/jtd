package td.ui.render;

/**
 * One upgrade slot's mark on a tower: {@code level} pips for owned nodes, and {@code ready} when an
 * affordable, ungated node is offered. A tower has three, in slot order.
 */
public record SlotMarkDraw(Palette palette, int level, boolean ready) {
}
