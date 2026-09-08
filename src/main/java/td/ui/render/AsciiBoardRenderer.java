package td.ui.render;

import java.util.Arrays;

/**
 * A second, deliberately minimal backend for {@link RenderFrame}: a compact
 * ASCII grid of the board's enemies and towers, with no display required.
 * Proves the describe/draw split is a real seam rather than a theoretical
 * one, and doubles as a headless troubleshooting aid - see its use at DEBUG
 * in {@code TowerDefense.doGameTick()}. Tower effects (beams/splash/pulse)
 * and cell highlights are intentionally omitted; this is a board-state
 * snapshot, not a full render.
 */
public final class AsciiBoardRenderer {

    private static final char EMPTY = '.';
    private static final char UNKNOWN = '?';

    public String render(RenderFrame frame) {
        int scale = frame.scale();
        int width = (frame.maxX() + 1) / scale;
        int height = (frame.maxY() + 1) / scale;
        char[][] grid = new char[height][width];
        for (char[] row : grid) {
            Arrays.fill(row, EMPTY);
        }

        for (TowerSpriteDraw sprite : frame.towerSprites()) {
            place(grid, sprite.boardX() / scale, sprite.boardY() / scale, towerChar(sprite.palette()));
        }
        for (EnemyDraw enemy : frame.enemies()) {
            switch (enemy) {
                case EnemyBodyDraw body ->
                        place(grid, cellOf(body.x(), scale), cellOf(body.y(), scale), enemyChar(body.palette(), false));
                case EnemyFadeDraw fade ->
                        place(grid, cellOf(fade.x(), scale), cellOf(fade.y(), scale), enemyChar(fade.palette(), true));
            }
        }

        StringBuilder sb = new StringBuilder();
        for (char[] row : grid) {
            sb.append(row).append('\n');
        }
        return sb.toString();
    }

    private static int cellOf(float pixel, int scale) {
        return (int) (pixel / scale);
    }

    private static void place(char[][] grid, int x, int y, char c) {
        if (y >= 0 && y < grid.length && x >= 0 && x < grid[0].length) {
            grid[y][x] = c;
        }
    }

    private static char towerChar(Palette palette) {
        return switch (palette) {
            case TOWER_ONE_BODY -> '1';
            case TOWER_TWO_BODY -> '2';
            case TOWER_THREE_BODY -> '3';
            case TOWER_FOUR_BODY -> '4';
            case TOWER_UPGRADE_BODY -> 'U';
            default -> UNKNOWN;
        };
    }

    private static char enemyChar(Palette palette, boolean fading) {
        char c = switch (palette) {
            case ENEMY_CIRCLE -> 'c';
            case ENEMY_GHOST -> 'g';
            case ENEMY_SQUARE -> 's';
            case ENEMY_TRIANGLE -> 't';
            default -> UNKNOWN;
        };
        return fading ? Character.toUpperCase(c) : c;
    }
}
