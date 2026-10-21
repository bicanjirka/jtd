package td.ui.render;

/** The selected tower's sheet, and where that tower stands, so a frame built before a selection change shows nothing. */
public record TowerSheet(int x, int y, InfoSheet sheet) {
}
