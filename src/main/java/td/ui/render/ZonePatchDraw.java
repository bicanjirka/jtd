package td.ui.render;

import java.util.List;

/**
 * Every zone of one kind as one patch of ground, drawn under everything that stands on it. Zones of a kind never stack
 * in play, so their overlap has one even fill and {@code edge} holds only the outline around them all.
 */
public record ZonePatchDraw(Palette palette, List<ZoneDraw> discs, List<ZoneArcDraw> edge) {
}
