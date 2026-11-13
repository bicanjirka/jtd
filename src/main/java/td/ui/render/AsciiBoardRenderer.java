package td.ui.render;

import java.util.Arrays;

/**
 * A minimal text backend for {@link RenderFrame}: enemies and towers as an ASCII grid. Used for
 * debug logging, and proves the frame model is backend-independent.
 */
public final class AsciiBoardRenderer {

    private static final char EMPTY = '.';
    private static final char UNKNOWN = '?';

    private static int cellOf(float pixel, int scale) {
        return (int) (pixel / scale);
    }

    private static void place(char[][] grid, int x, int y, char c) {
        if (y >= 0 && y < grid.length && x >= 0 && x < grid[0].length) {
            grid[y][x] = c;
        }
    }

    private static char towerChar(TowerLook look) {
        return switch (look) {
            case SNIPER -> '1';
            case SPLASH, STORMCALLER, HEXER -> '2';
            case PULSE -> '4';
            case AURA -> 'A';
            case MORTAR -> 'M';
            case SEEKER -> 'K';
            case CINDER -> 'N';
        };
    }

    private static char enemyChar(EnemyLook look, boolean fading) {
        char c = switch (look) {
            case CIRCLE -> 'c';
            case GHOST -> 'g';
            case SQUARE -> 's';
            case TRIANGLE -> 't';
            default -> UNKNOWN;
        };
        return fading ? Character.toUpperCase(c) : c;
    }

    public String render(RenderFrame frame) {
        int scale = frame.scale();
        int width = (frame.maxX() + 1) / scale;
        int height = (frame.maxY() + 1) / scale;
        char[][] grid = new char[height][width];
        for (char[] row : grid) {
            Arrays.fill(row, EMPTY);
        }

        for (TowerSpriteDraw sprite : frame.towerSprites()) {
            place(grid, sprite.boardX() / scale, sprite.boardY() / scale, towerChar(sprite.look()));
        }
        for (EnemyDraw enemy : frame.enemies()) {
            switch (enemy) {
                case EnemyBodyDraw body ->
                        place(grid, cellOf(body.x(), scale), cellOf(body.y(), scale), enemyChar(body.look(), false));
                case EnemyFadeDraw fade ->
                        place(grid, cellOf(fade.x(), scale), cellOf(fade.y(), scale), enemyChar(fade.look(), true));
                case EnemyEmissionDraw ignored -> {
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (char[] row : grid) {
            sb.append(row).append('\n');
        }
        return sb.toString();
    }
}
