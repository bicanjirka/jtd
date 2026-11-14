import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Renders the next-gen art sheets (docs/features/next-gen-art) from {@link TowerArt}. Throwaway lab code.
 * {@code java TowerArtSheets <outDir> [sheet...]}
 */
public final class TowerArtSheets {

    static final Color FLOOR = new Color(0x08090c);
    static final Color PAPER = new Color(0x0d0e10);
    static final Color TEXT = new Color(0xeaeaef);
    static final Color DIM = new Color(0xa9a9ac);
    static final Color GLOSS = new Color(0x77777a);
    static final String[] TIER_NAMES = {"Tier 0", "Tier I", "Tier II", "Tier III"};

    static File out;

    private TowerArtSheets() {
    }

    public static void main(String[] args) throws Exception {
        out = new File(args[0]);
        out.mkdirs();
        java.util.Set<String> only = new java.util.HashSet<>(java.util.List.of(args).subList(1, args.length));
        if (only.isEmpty() || only.contains("catalogue")) {
            catalogue();
        }
        if (only.contains("bench")) {
            bench();
            return;
        }
        TowerArtSheets2.run(only);
    }

    /** How long the game takes to paint a board of Tier III towers from its kept pictures, still and with its lights. */
    static void bench() {
        td.ui.render.TowerLook[] looks = td.ui.render.TowerLook.values();
        int towers = 200;
        BufferedImage board = image(576, 416, FLOOR);
        Graphics2D g = pen(board);
        long still = 0;
        long live = 0;
        for (int round = 0; round < 60; round++) {
            long t0 = System.nanoTime();
            for (int i = 0; i < towers; i++) {
                int at = i;
                cell(g, 16 + (i % 18) * 32, 16 + (i / 18) * 32, 32, () -> td.ui.TowerArtGame.paint(g, looks[at % looks.length], 3,
                        td.ui.render.TowerPose.idle(), at * 37));
            }
            long t1 = System.nanoTime();
            for (int i = 0; i < towers; i++) {
                int at = i;
                double seconds = round / 60.0 + i;
                cell(g, 16 + (i % 18) * 32, 16 + (i / 18) * 32, 32, () -> td.ui.TowerArtGame.paintLive(g, looks[at % looks.length], 3, 3,
                        td.ui.render.TowerPose.idle(), at * 37, 1, 1, seconds));
            }
            long t2 = System.nanoTime();
            if (round >= 20) {
                still += t1 - t0;
                live += t2 - t1;
            }
        }
        g.dispose();
        System.out.printf("%d Tier III towers through the game's painter: still %.2f ms a frame, with idle lights %.2f ms a frame%n",
                towers, still / 40 / 1e6, live / 40 / 1e6);
        java.util.List<td.ui.render.EnemyDraw> bodies = new java.util.ArrayList<>();
        td.ui.render.EnemyLook[] enemies = td.ui.render.EnemyLook.values();
        for (int i = 0; i < 300; i++) {
            bodies.add(new td.ui.render.EnemyBodyDraw(enemies[i % enemies.length], 16 + (i % 18) * 32, 16 + (i / 18) * 24, i * 0.7, 9f, 0f));
        }
        BufferedImage ground = image(576, 416, FLOOR);
        Graphics2D eg = pen(ground);
        td.ui.Java2DFrameRenderer renderer = new td.ui.Java2DFrameRenderer();
        long spent = 0;
        for (int round = 0; round < 60; round++) {
            long t0 = System.nanoTime();
            renderer.paintEnemies(eg, bodies, java.util.List.of());
            if (round >= 20) {
                spent += System.nanoTime() - t0;
            }
        }
        eg.dispose();
        System.out.printf("300 standing enemy bodies through the game's renderer: %.2f ms a frame%n", spent / 40 / 1e6);
    }

    // ---- helpers ---------------------------------------------------------------------------------------------

    static BufferedImage image(int w, int h, Color bg) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(bg);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    static Graphics2D pen(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }

    static void text(Graphics2D g, String s, double x, double y, Color c, int size, boolean bold) {
        g.setFont(new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, size));
        g.setColor(c);
        g.drawString(s, (float) x, (float) y);
    }

    static void label(Graphics2D g, String s, double x, double y) {
        text(g, s, x, y, DIM, 11, false);
    }

    static void title(Graphics2D g, String s, double x, double y) {
        text(g, s, x, y, TEXT, 15, true);
    }

    static void save(BufferedImage img, String name) throws Exception {
        ImageIO.write(img, "png", new File(out, name));
        System.out.println("wrote " + name + " " + img.getWidth() + "x" + img.getHeight());
    }

    /** A dark floor tile behind one drawing, so a sheet shows the art on the board's own ground. */
    static void tile(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(FLOOR);
        g.fill(new java.awt.geom.Rectangle2D.Double(x, y, w, h));
        g.setColor(new Color(0x16181d));
        g.setStroke(new java.awt.BasicStroke(1f));
        g.draw(new java.awt.geom.Rectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1));
    }

    /** Runs {@code art} in cell units around a centre. */
    static void cell(Graphics2D g, double cx, double cy, double px, Runnable art) {
        AffineTransform save = g.getTransform();
        g.translate(cx, cy);
        g.scale(px, px);
        art.run();
        g.setTransform(save);
    }

    /** A tower with what always stands around it: the Aerie's nest. */
    static void tower(Graphics2D g, String id, int tier, TowerArt.State s, double cx, double cy, double px) {
        TowerArt.Build b = TowerArt.build(id, tier, s);
        boolean nest = id.equals("aerie") || id.equals("ruby") || id.equals("shade");
        int count = s.has("swarm") ? 7 : s.stacks > 0 ? s.stacks : tier <= 1 ? 1 : tier;
        String kind = id.equals("aerie") ? "frost" : id;
        double lift = TowerArt.LIFT * b.nest() * px;
        if (nest) {
            cell(g, cx, cy + lift, px, () -> TowerArt.nest(g, count, -60, b.footprint() + 0.06, kind, s.has("release"), -1));
        }
        TowerArt.paint(g, b, cx, cy, px);
        if (nest) {
            cell(g, cx, cy + lift, px, () -> TowerArt.nest(g, count, -60, b.footprint() + 0.06, kind, s.has("release"), 1));
        }
    }

    static final String[][] ROWS = {
        {"sniper", "Sniper"}, {"burst", "Burst: stone thrower, then Stormcaller"}, {"hexer", "Burst: Hexer"},
        {"mortar", "Mortar"}, {"scorcher", "Scorcher"}, {"soulfire", "Scorcher: Soulfire"}, {"aerie", "Aerie"},
        {"ruby", "Aerie: Ruby"}, {"shade", "Aerie: Shade"}, {"obelisk", "Obelisk"}, {"beacon", "Beacon"},
    };

    static boolean exists(String id, int tier) {
        return switch (id) {
            case "hexer" -> tier >= 1;
            case "ruby", "shade" -> tier >= 2;
            default -> true;
        };
    }

    static void catalogue() throws Exception {
        int big = 112;
        int colW = big + 32 + 16 + 44;
        int rowH = big + 34;
        BufferedImage img = image(250 + colW * 4, 60 + rowH * ROWS.length, PAPER);
        Graphics2D g = pen(img);
        title(g, "Every tower at Tiers 0 to III, raised view: 2.5x, then the board's 1x (32 px cell) and 0.5x (16 px cell), each with the cell above it", 16, 26);
        for (int t = 0; t < 4; t++) {
            text(g, TIER_NAMES[t], 250 + t * colW, 48, DIM, 12, true);
        }
        for (int row = 0; row < ROWS.length; row++) {
            String id = ROWS[row][0];
            double y = 60 + row * rowH;
            text(g, ROWS[row][1], 16, y + 20, TEXT, 12, true);
            for (int t = 0; t < 4; t++) {
                if (!exists(id, t)) {
                    continue;
                }
                double x = 250 + t * colW;
                tile(g, x, y, big, big);
                tower(g, id, t, TowerArt.State.idle(), x + big / 2.0, y + big * 0.70, big * 0.70);
                tile(g, x + big + 8, y, 32, 48);
                tower(g, id, t, TowerArt.State.idle(), x + big + 24, y + 32, 32);
                tile(g, x + big + 8, y + 56, 16, 24);
                tower(g, id, t, TowerArt.State.idle(), x + big + 16, y + 72, 16);
                label(g, TowerArt.build(id, t, TowerArt.State.idle()).count() + " parts", x + big + 8, y + 96);
            }
        }
        g.dispose();
        save(img, "catalogue.png");
    }
}
