package td.ui.render;

/** What stands on one disc of a zone patch, painted among the enemies and towers by where it stands. */
public record ZoneDetailDraw(Palette palette, ZoneDraw zone) implements BoardDraw {
}
