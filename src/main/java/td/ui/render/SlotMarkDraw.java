package td.ui.render;

/**
 * One upgrade tier's mark on a tower: {@code level} pips for owned nodes, and {@code ready} when an
 * affordable node of it is offered. A tower has one per tier, bottom first.
 */
public record SlotMarkDraw(Palette palette, int level, boolean ready) {
}
