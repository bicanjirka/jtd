import td.board.BoardGeometry;
import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.enemy.SpawnParameters;
import td.ui.EnemyFrameBuilder;
import td.ui.Java2DFrameRenderer;
import td.util.GameHost;
import td.util.GameWorld;
import td.wave.PathNormal;
import td.wave.Vec2;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Headless, single-shot preview of one enemy body (shape, palette and rank badge) as a PNG -
 * no JFrame, no level, no wave, no Robot/screenshot. For "does this look like a skull" checks,
 * this is a few hundred milliseconds against already-built classes instead of a full
 * Driver.java run (build window, load level, spawn, screenshot, teardown).
 * <p>
 * It reuses exactly the recipe {@link td.ui.PanelEnemy} already uses to preview a wave's enemies
 * inside the real game: a throwaway {@link GameWorld} with a {@link GameHost#noOp()} host and a
 * one-point {@link PathNormal}, one {@link DefinedEnemyMob} built directly (no level/catalog
 * plumbing beyond {@link EnemyCatalog#builtIn()}), and {@link Java2DFrameRenderer#paintEnemies}
 * - the same call the board itself paints enemies through, so this can never drift from the real
 * in-game look. It only covers body + rank badge, matching what {@code PanelEnemy} shows - not
 * status-effect markers or overlay rings, which need a live simulation tick to have anything to
 * show and are exactly what a full {@code Driver.java} run is for instead.
 * <p>
 * Only reaches {@link EnemyCatalog#builtIn()}'s ids - a level's own custom/cloned enemies
 * (see {@code td/level/CLAUDE.md}) aren't registered anywhere this tool can see without loading
 * that level, so previewing one of those still needs the full {@code Driver.java} + {@code spawn}
 * path.
 */
public class PreviewEnemy {

    private static final int MARGIN_CELLS = 4;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: PreviewEnemy <id> [rank] [outputPath] [scale]");
            System.out.println("  rank defaults to GRUNT, outputPath to shots/preview_<id>_<rank>.png, scale (px/cell) to 64");
            return;
        }
        String id = args[0];
        Rank rank = args.length > 1 ? Rank.valueOf(args[1].toUpperCase()) : Rank.GRUNT;
        int scale = args.length > 3 ? Integer.parseInt(args[3]) : 64;
        String outputPath = args.length > 2
                ? args[2]
                : ".claude/skills/run-jtd/shots/preview_" + id + "_" + rank.name().toLowerCase() + ".png";

        EnemyCatalog catalog = EnemyCatalog.builtIn();
        EnemyDefinition definition = catalog.get(id, rank);

        GameWorld world = new GameWorld(GameHost.noOp());
        world.setBoard(BoardGeometry.of(scale, 0, 0));
        int size = scale * MARGIN_CELLS;
        world.setPath(new PathNormal(List.of(new Vec2(size / 2f, size / 2f))));

        SpawnParameters spawnParameters = SpawnParameters.atSlot(0, definition.baseSpeed(), definition.baseHealth(), definition.price());
        EnemyMob mob = new DefinedEnemyMob(definition, world, spawnParameters, rank);
        mob.doTick(0);

        EnemyFrameBuilder frameBuilder = new EnemyFrameBuilder(0, 1.0);
        mob.accept(frameBuilder);

        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, size, size);
        new Java2DFrameRenderer().paintEnemies(g2, frameBuilder.build());
        g2.dispose();

        File outFile = new File(outputPath);
        File parent = outFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        ImageIO.write(image, "png", outFile);
        System.out.println("OK wrote " + outputPath + " (" + id + " at rank " + rank + ", " + size + "x" + size + ")");
    }
}
