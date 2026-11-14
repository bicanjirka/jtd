import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Set;
import td.GameEngine;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.level.BuiltInLevelCatalog;
import td.ui.BoardRenderer;
import td.ui.Java2DFrameRenderer;
import td.ui.render.RenderFrame;
import td.util.GameHost;
import td.util.GameWorld;
import td.util.TickRate;

/** The board sheets: the art on a real level's ground, through the game's own renderer. Throwaway lab code. */
final class TowerArtSheets2 {

    static final int LEVEL = 5;
    static final int CELL = 32;
    static final String[] COLUMNS = {"sniper", "burst", "mortar", "scorcher", "aerie", "obelisk", "beacon"};

    static GameEngine engine;
    static int gameTime;

    private TowerArtSheets2() {
    }

    static void run(Set<String> only) throws Exception {
        if (only.isEmpty() || only.contains("board")) {
            board();
        }
        TowerArtSheets3.run(only);
    }

    static void level(String... enemies) {
        engine = new GameEngine(GameHost.noOp());
        engine.loadLevel(new BuiltInLevelCatalog().levels().get(LEVEL));
        gameTime = 0;
        GameWorld world = engine.getGameWorld();
        EnemyCatalog catalog = world.getEnemyCatalog();
        for (String token : enemies) {
            String[] parts = token.split(":");
            Rank rank = parts.length > 1 ? Rank.valueOf(parts[1].toUpperCase()) : Rank.GRUNT;
            EnemyDefinition definition = catalog.get(parts[0], rank);
            world.enemies().add(catalog.spawn(parts[0], world, 0, definition.baseHealth(), definition.price(), rank));
            tick(26);
        }
    }

    static void tick(int count) {
        for (int i = 0; i < count; i++) {
            gameTime++;
            engine.doTick(gameTime);
        }
    }

    static EnemyMob[] enemies() {
        return engine.getGameWorld().enemies().getEnemies();
    }

    /** The level as the game paints it, {@code zoom} times the board's own scale. */
    static BufferedImage ground(double zoom) {
        GameWorld world = engine.getGameWorld();
        BoardRenderer renderer = new BoardRenderer(world, engine::cellHighlight);
        RenderFrame frame = renderer.buildFrame(gameTime, 1.0, gameTime / TickRate.TICKS_PER_SECOND);
        BufferedImage img = TowerArtSheets.image((int) Math.round(frame.maxX() * zoom),
                (int) Math.round(frame.maxY() * zoom), TowerArtSheets.FLOOR);
        Graphics2D g = TowerArtSheets.pen(img);
        g.scale(zoom, zoom);
        new Java2DFrameRenderer().paint(g, frame);
        g.dispose();
        return img;
    }

    static double centre(int cell, double zoom) {
        return (cell + 0.5) * CELL * zoom;
    }

    /** Every tower at every tier in the level's free block, and the identity looks in the block below it. */
    static BufferedImage boardWithTowers(double zoom) {
        BufferedImage img = ground(zoom);
        Graphics2D g = TowerArtSheets.pen(img);
        double px = CELL * zoom;
        for (int tier = 0; tier < 4; tier++) {
            for (int c = 0; c < COLUMNS.length; c++) {
                TowerArt.State s = TowerArt.State.idle().aim(-20 - c * 37 + tier * 61);
                TowerArtSheets.tower(g, COLUMNS[c], tier, s, centre(1 + c, zoom), centre(2 + tier, zoom), px);
            }
        }
        String[][] identities = {{"hexer", "hexer", "hexer", "soulfire", "soulfire"},
            {"ruby", "ruby", "shade", "shade", "soulfire"}};
        int[][] tiers = {{1, 2, 3, 1, 2}, {2, 3, 2, 3, 3}};
        for (int row = 0; row < 2; row++) {
            for (int c = 0; c < 5; c++) {
                TowerArtSheets.tower(g, identities[row][c], tiers[row][c], TowerArt.State.idle().aim(200 + c * 40),
                        centre(4 + c, zoom), centre(8 + row, zoom), px);
            }
        }
        TowerArtSheets.cell(g, centre(3, zoom) - 0.34 * px, centre(3, zoom) - 0.34 * px, px, () -> TowerArt.upgradeArrow(g));
        TowerArtSheets.cell(g, centre(6, zoom) - 0.34 * px, centre(4, zoom) - 0.34 * px, px, () -> TowerArt.upgradeArrow(g));
        g.dispose();
        return img;
    }

    static BufferedImage crop(BufferedImage img, double zoom, double x0, double y0, double x1, double y1) {
        int x = (int) (x0 * CELL * zoom);
        int y = (int) (y0 * CELL * zoom);
        return img.getSubimage(x, y, (int) ((x1 - x0) * CELL * zoom), (int) ((y1 - y0) * CELL * zoom));
    }

    static void board() throws Exception {
        level("walker", "knight", "walker", "walker:elite", "walker", "knight", "walker");
        tick(140);
        BufferedImage half = boardWithTowers(0.5);
        BufferedImage one = boardWithTowers(1.0);
        BufferedImage big = crop(boardWithTowers(2.5), 2.5, 0.5, 1.5, 8.5, 6.5);
        BufferedImage ids = crop(boardWithTowers(2.5), 2.5, 3.5, 7.5, 9.5, 10.5);
        int w = 40 + big.getWidth() + 20 + ids.getWidth();
        BufferedImage img = TowerArtSheets.image(Math.max(w, 40 + one.getWidth() + half.getWidth() + 40),
                70 + one.getHeight() + 50 + big.getHeight() + 30, TowerArtSheets.PAPER);
        Graphics2D g = TowerArtSheets.pen(img);
        TowerArtSheets.title(g, "The seven towers on Iron Crossing, painted over the game's own frame", 20, 26);
        TowerArtSheets.label(g, "Board scale 1x (32 px cells). Columns: Sniper, Burst (Stormcaller from Tier I), Mortar, Scorcher, "
                + "Aerie, Obelisk, Beacon. Rows: Tiers 0 to III.", 20, 46);
        g.drawImage(one, 20, 56, null);
        TowerArtSheets.label(g, "Smallest zoom, 0.5x (16 px cells)", 40 + one.getWidth(), 76);
        g.drawImage(half, 40 + one.getWidth(), 84, null);
        int y = 56 + one.getHeight() + 34;
        TowerArtSheets.label(g, "2.5x: the tier block", 20, y - 8);
        g.drawImage(big, 20, y, null);
        TowerArtSheets.label(g, "2.5x: Hexer I-III, Soulfire I-II; Ruby II-III, Shade II-III, Soulfire III", 40 + big.getWidth(), y - 8);
        g.drawImage(ids, 40 + big.getWidth(), y, null);
        g.dispose();
        TowerArtSheets.save(img, "board.png");
    }
}
