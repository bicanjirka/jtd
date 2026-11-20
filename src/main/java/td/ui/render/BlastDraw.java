package td.ui.render;

/**
 * A shell going off at {@code x, y}, {@code progress} from 0 as it lands to 1 as its drawing ends: what it throws up
 * stands on the board, what spreads lies on the ground. {@code core} and {@code edge} are its blast's radii and
 * {@code unit} a cell, in pixels; {@code seed} keeps one blast from looking like the next.
 */
public record BlastDraw(BlastLook look, float x, float y, float core, float edge, float progress, float unit,
                        int seed) implements TowerEffectDraw {
}
