package td.ui.render;

import java.util.Optional;

/**
 * A tower's body sprite, plus its selection range ring when {@code selected}.
 * {@code accent}, when present, marks a tower that has permanently chosen an upgrade path
 * (see {@code td.tower.upgrade}) - one of exactly two roles
 * ({@code TOWER_UPGRADE_PATH_A}/{@code _B}), shared across every tower type rather than one
 * role per tower per path, so the ring reads as one consistent "which slot did this tower
 * pick" language instead of eight colours to memorize.
 */
public record TowerSpriteDraw(Palette palette, int boardX, int boardY, boolean selected,
                              float centerX, float centerY, float rangeReal, Optional<Palette> accent) {
}
