import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import td.economy.EconomyDelta;
import td.tower.TowerFactory;
import td.ui.Java2DFrameRenderer;
import td.ui.render.EnemyBodyDraw;
import td.ui.render.EnemyDraw;
import td.ui.render.EnemyLook;
import td.zone.ZoneKind;

/** The game's own pieces beside the new towers: every enemy look, every ground zone, today's towers. Lab code. */
final class TowerArtSheets5 {

    private TowerArtSheets5() {
    }

    static void run(Set<String> only) throws Exception {
        if (only.isEmpty() || only.contains("theme")) {
            theme();
        }
    }

    static void theme() throws Exception {
        double zoom = 3;
        EnemyLook[] looks = EnemyLook.values();
        int columns = 12;
        int rows = (looks.length + columns - 1) / columns;
        int cellW = 34;
        int cellH = 40;
        BufferedImage enemies = TowerArtSheets.image((int) (columns * cellW * zoom), (int) (rows * cellH * zoom), TowerArtSheets.FLOOR);
        Graphics2D g = TowerArtSheets.pen(enemies);
        List<EnemyDraw> draws = new ArrayList<>();
        for (int i = 0; i < looks.length; i++) {
            float x = (i % columns) * cellW + cellW / 2f;
            float y = (i / columns) * cellH + 15f;
            draws.add(new EnemyBodyDraw(looks[i], x, y, i % 2 == 0 ? 0 : Math.toRadians(140), 9f, i == looks.length - 1 ? 0.6f : 0f));
        }
        g.scale(zoom, zoom);
        new Java2DFrameRenderer().paintEnemies(g, draws, List.of());
        for (int i = 0; i < looks.length; i++) {
            TowerArtSheets.text(g, looks[i].name().toLowerCase().replace('_', ' '), (i % columns) * cellW + 1, (i / columns) * cellH + 36,
                    TowerArtSheets.GLOSS, 4, false);
        }
        g.dispose();

        TowerArtSheets2.level("walker", "knight", "walker:elite", "knight:elite", "walker");
        ZoneKind[] kinds = ZoneKind.values();
        for (int i = 0; i < kinds.length; i++) {
            TowerArtSheets4.zone(kinds[i], 1.5 + (i % 5) * 1.5, 3.0 + (i / 5) * 1.6, 0.65);
        }
        var world = TowerArtSheets2.engine.getGameWorld();
        world.economy().apply(EconomyDelta.credits(5000));
        TowerFactory.Type[] types = TowerFactory.Type.values();
        for (int i = 0; i < types.length; i++) {
            TowerArtSheets2.engine.startPlacing(types[i], 0f);
            TowerArtSheets2.engine.mouseClicked((4 + i % 4) * 32 + 16, (8 + i / 4) * 32 + 16);
        }
        TowerArtSheets2.tick(64);
        BufferedImage board = TowerArtSheets2.ground(zoom);
        BufferedImage zones = TowerArtSheets2.crop(board, zoom, 0.5, 0.4, 9.0, 5.6);
        BufferedImage towers = TowerArtSheets2.crop(board, zoom, 3.5, 7.5, 8.5, 10.5);
        BufferedImage img = TowerArtSheets.image(Math.max(enemies.getWidth(), zones.getWidth() + towers.getWidth() + 20) + 40,
                enemies.getHeight() + zones.getHeight() + 110, TowerArtSheets.PAPER);
        Graphics2D p = TowerArtSheets.pen(img);
        TowerArtSheets.title(p, "The game's own pieces in the raised theme (3x): every enemy look, the ground zones, today's towers", 20, 26);
        TowerArtSheets.label(p, "Enemy looks, facing east and south-west by turns; the last one is cloaked", 20, 46);
        p.drawImage(enemies, 20, 54, null);
        int y = 54 + enemies.getHeight() + 30;
        TowerArtSheets.label(p, "Ground zones in ZoneKind order, enemies walking the path above them", 20, y - 6);
        p.drawImage(zones, 20, y, null);
        TowerArtSheets.label(p, "Today's towers, until the next-gen looks replace them", 40 + zones.getWidth(), y - 6);
        p.drawImage(towers, 40 + zones.getWidth(), y, null);
        p.dispose();
        TowerArtSheets.save(img, "theme.png");
    }
}
