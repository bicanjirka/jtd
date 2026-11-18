package td.ui;

import td.ui.PolishLife.Flock;
import td.ui.PolishLooks.Built;
import td.ui.PolishLooks.Look;
import td.ui.TowerFoundations.Part;
import td.ui.render.LoadedMissile;
import td.ui.render.LoadedShell;
import td.ui.render.TowerCue;
import td.ui.render.TowerLook;
import td.ui.render.TowerPose;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Renders the tower visual polish: stills as PNG sheets and every animation as a GIF, from {@link PolishLooks} and
 * {@link PolishLife}. {@code java td.ui.PolishSheets <outDir> [name ...]}; no name renders everything.
 */
public final class PolishSheets {

    private static final Color BACK = new Color(0x0b0c10);
    private static final Color FLOOR = new Color(0x13151a);
    private static final Color GRID = new Color(0x23262e);
    private static final Color PATH = new Color(0x2b2e3a);
    private static final Color TEXT = new Color(0xe8eaf0);
    private static final Color DIM = new Color(0x8e95a3);
    private static final Font LABEL = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private static final Font TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    private static final int FPS = 20;

    private PolishSheets() {
    }

    @FunctionalInterface
    interface Frame {
        void paint(Graphics2D g, double seconds);
    }

    public static void main(String[] args) throws IOException {
        File out = new File(args.length > 0 ? args[0] : "docs/features/shipped/tower-polish-art");
        out.mkdirs();
        Set<String> only = new HashSet<>(Arrays.asList(args).subList(Math.min(1, args.length), args.length));
        if (wanted(only, "looks")) {
            looks(new File(out, "looks.png"));
        }
        if (wanted(only, "lineup")) {
            lineup(new File(out, "lineup.png"));
        }
        if (wanted(only, "heads")) {
            heads(new File(out, "heads.png"));
        }
        if (wanted(only, "idle-stormcaller")) {
            idleRow(new File(out, "idle-stormcaller.gif"), "Stormcaller: tesla sparks (idle, wall clock)",
                    new Look[] {Look.STORMCALLER, Look.STORMCALLER, Look.STORMCALLER}, new int[] {1, 2, 3}, 5);
        }
        if (wanted(only, "idle-hexer")) {
            idleRow(new File(out, "idle-hexer.gif"), "Hexer: the oracle ball's inner magic (idle, wall clock)",
                    new Look[] {Look.HEXER, Look.HEXER, Look.HEXER}, new int[] {1, 2, 3}, 5);
        }
        if (wanted(only, "idle-aerie")) {
            idleRow(new File(out, "idle-aerie.gif"), "Aerie, Ruby, Shade, White at Tier III: birds and the nest (idle)",
                    new Look[] {Look.SEEKER, Look.RUBY, Look.SHADE, Look.WHITE}, new int[] {3, 3, 3, 3}, 7);
        }
        if (wanted(only, "idle-aerie-tiers")) {
            idleRow(new File(out, "idle-aerie-tiers.gif"), "Aerie at Tiers 0 to III: birds and the nest (idle)",
                    new Look[] {Look.SEEKER, Look.SEEKER, Look.SEEKER, Look.SEEKER}, new int[] {0, 1, 2, 3}, 6);
        }
        if (wanted(only, "idle-obelisk")) {
            idleRow(new File(out, "idle-obelisk.gif"), "Obelisk: plain Tiers 0 to III, then Burden, Gravity, Umbra",
                    new Look[] {Look.PULSE, Look.PULSE, Look.PULSE, Look.PULSE, Look.BURDEN, Look.GRAVITY, Look.UMBRA},
                    new int[] {0, 1, 2, 3, 3, 3, 3}, 6);
        }
        if (wanted(only, "orbit-options")) {
            orbitOptions(new File(out, "orbit-options.gif"));
        }
        if (wanted(only, "moment-knell")) {
            knell(new File(out, "moment-knell.gif"));
        }
        if (wanted(only, "moment-sundial")) {
            sundial(new File(out, "moment-sundial.gif"));
        }
        if (wanted(only, "moment-sniper")) {
            sniper(new File(out, "moment-sniper.gif"));
        }
        if (wanted(only, "moment-mortar")) {
            mortar(new File(out, "moment-mortar.gif"));
        }
        if (wanted(only, "moment-scorcher")) {
            scorcher(new File(out, "moment-scorcher.gif"));
        }
        if (wanted(only, "dragon-turn")) {
            dragonTurn(new File(out, "dragon-turn.gif"));
        }
        if (wanted(only, "sniper-shots")) {
            sniperShots(new File(out, "sniper-shots.gif"));
        }
        if (wanted(only, "sniper-states")) {
            sniperStates(new File(out, "sniper-states.gif"));
        }
        if (wanted(only, "flock")) {
            flock(new File(out, "flock.gif"));
        }
        if (wanted(only, "flame")) {
            flame(new File(out, "flame.gif"));
        }
        if (wanted(only, "stones")) {
            stones(new File(out, "stones.png"));
        }
        if (wanted(only, "closeups")) {
            closeups(new File(out, "closeups.png"));
        }
        if (wanted(only, "strips")) {
            strips(new File(out, "strips.png"));
        }
        if (wanted(only, "tops")) {
            tops();
        }
    }

    private static boolean wanted(Set<String> only, String name) {
        return only.isEmpty() || only.contains(name);
    }

    // ---- painting one tower ------------------------------------------------------------------------------------------

    private static Graphics2D pen(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }

    /** The cell at {@code cx, cy} and {@code above} cells north of it, as the board's floor. */
    private static void floor(Graphics2D g, double cx, double cy, double scale, int above) {
        for (int i = 0; i <= above; i++) {
            Rectangle2D cell = new Rectangle2D.Double(cx - scale / 2, cy - scale / 2 - i * scale, scale, scale);
            g.setColor(FLOOR);
            g.fill(cell);
            g.setColor(GRID);
            g.setStroke(new BasicStroke(1f));
            g.draw(cell);
        }
    }

    /** A lab tower standing at {@code cx, cy}, {@code scale} pixels to a cell, its head turned to {@code heading}. */
    static void tower(Graphics2D g, Built f, double cx, double cy, double scale, double heading) {
        double foot = f.footprint() * scale;
        g.setColor(new Color(0, 0, 0, 150));
        g.fill(new Ellipse2D.Double(cx + foot * 0.14 - foot * 1.06, cy + foot * 0.16 - foot * 1.02, foot * 2.12,
                foot * 2.04));
        Graphics2D p = (Graphics2D) g.create();
        p.translate(cx, cy);
        p.scale(scale, scale);
        TowerSprites.paintParts(p, f.parts(), scale);
        if (!f.head().isEmpty()) {
            AffineTransform turn = AffineTransform.getRotateInstance(heading);
            List<Part> head = new ArrayList<>();
            for (Part part : f.head()) {
                head.add(part.withShape(turn.createTransformedShape(part.shape())));
            }
            TowerSprites.paintParts(p, head, scale);
        }
        TowerSprites.paintParts(p, f.front(), scale);
        p.dispose();
    }

    /** The tower with everything that lives on it while it idles: lights, the nest's missiles, its birds. */
    static void alive(Graphics2D g, Look look, int tier, TowerPose pose, double cx, double cy, double scale,
                      double heading, double seconds, int seed, double squash) {
        alive(g, look, tier, pose, cx, cy, scale, heading, seconds, seed, squash, PolishLife.Presence.CIRCLING);
    }

    static void alive(Graphics2D g, Look look, int tier, TowerPose pose, double cx, double cy, double scale,
                      double heading, double seconds, int seed, double squash, PolishLife.Presence flock) {
        Built f = PolishLooks.of(look, tier, pose);
        tower(g, f, cx, cy, scale, heading);
        PolishLife.paintLives(g, f, cx, cy, scale, heading, seconds, seed);
        Flock birds = flockOf(look);
        if (birds != null) {
            List<LoadedMissile> missiles = missilesOf(look, tier);
            PolishLife.nest(g, missiles, cx, cy - PolishLooks.LIFT * f.nest() * scale, scale, squash,
                    seconds * PolishLife.NEST_TURN + seed, pose.has(TowerCue.RELEASE_READY));
            PolishLife.birds(g, birds, tier, cx, cy - PolishLooks.LIFT * f.top() * scale, scale, squash, seconds, seed,
                    flock);
        }
    }

    private static Flock flockOf(Look look) {
        return switch (look) {
            case SEEKER -> Flock.TERNS;
            case RUBY -> Flock.EMBERS;
            case SHADE -> Flock.CROWS;
            case WHITE -> Flock.EAGLES;
            default -> null;
        };
    }

    private static List<LoadedMissile> missilesOf(Look look, int tier) {
        LoadedMissile own = look == Look.RUBY ? LoadedMissile.RUBY : look == Look.SHADE ? LoadedMissile.SHADE
                : LoadedMissile.FROST;
        List<LoadedMissile> held = new ArrayList<>();
        for (int i = 0; i < 2 + tier / 2; i++) {
            held.add(i == 1 ? own : LoadedMissile.FROST);
        }
        return held;
    }

    private static void text(Graphics2D g, String s, double x, double y, Color c, Font f) {
        g.setFont(f);
        g.setColor(c);
        g.drawString(s, (float) x, (float) y);
    }

    private static void save(BufferedImage image, File file) throws IOException {
        ImageIO.write(image, "png", file);
        System.out.println("wrote " + file);
    }

    // ---- the game's own look of a tower, for "now" ---------------------------------------------------------------------

    private static void game(Graphics2D g, TowerLook look, int tier, TowerPose pose, double cx, double cy, double scale,
                             double aimDegrees) {
        Graphics2D p = (Graphics2D) g.create();
        p.translate(cx, cy);
        p.scale(scale, scale);
        TowerArtGame.paint(p, look, tier, pose, aimDegrees);
        p.dispose();
    }

    private static TowerLook gameLook(Look look) {
        return look == Look.WHITE ? null : TowerLook.valueOf(look.name());
    }

    private static TowerPose poseFor(Look look) {
        return look == Look.CINDER ? TowerPose.idle().with(TowerCue.DRAGONS_BREATH) : TowerPose.idle();
    }

    // ---- looks.png: now against new, every tier ------------------------------------------------------------------------

    private static void looks(File file) throws IOException {
        Look[] rows = {Look.SPLASH, Look.STORMCALLER, Look.HEXER, Look.CINDER, Look.SOULFIRE, Look.MORTAR, Look.SEEKER,
                Look.RUBY, Look.SHADE, Look.WHITE, Look.PULSE, Look.BURDEN, Look.GRAVITY, Look.UMBRA};
        String[] names = {"Burst (no fork)", "Stormcaller", "Hexer", "Scorcher + Dragon's Breath", "Soulfire",
                "Mortar", "Aerie", "Aerie: Ruby", "Aerie: Shade", "Aerie: White (new)", "Obelisk", "Obelisk: Burden",
                "Obelisk: Gravity", "Obelisk: Umbra"};
        int big = 96;
        int tile = 150;
        int rowHeight = 250;
        int left = 190;
        int group = tile * 2 + 56 + 56 + 24;
        BufferedImage image = new BufferedImage(left + group * 4 + 10, 60 + rowHeight * rows.length,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = pen(image);
        g.setColor(BACK);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        text(g, "Tower visual polish: each tier as it is NOW and NEW, at 3x; then now and new at the board's 1x (32 px"
                + " cell) and 0.5x. Each with the cells north of it.", 14, 22, TEXT, TITLE);
        for (int t = 0; t < 4; t++) {
            text(g, "Tier " + "0 I II III".split(" ")[t] + ":   now", left + t * group + 6, 46, DIM, LABEL);
            text(g, "new", left + t * group + tile + 6, 46, TEXT, LABEL);
        }
        for (int r = 0; r < rows.length; r++) {
            Look look = rows[r];
            int top = 56 + r * rowHeight;
            text(g, names[r], 14, top + 24, TEXT, TITLE);
            TowerPose pose = poseFor(look);
            for (int t = 0; t < 4; t++) {
                boolean exists = !(look == Look.STORMCALLER || look == Look.HEXER) || t >= 1;
                boolean picked = !(look == Look.RUBY || look == Look.SHADE || look == Look.WHITE
                        || look == Look.SOULFIRE || look == Look.CINDER || look == Look.BURDEN || look == Look.GRAVITY
                        || look == Look.UMBRA) || t >= 2;
                if (!exists || !picked) {
                    continue;
                }
                int x = left + t * group;
                double cy = top + rowHeight - 62;
                double aim = Math.toRadians(-35);
                TowerLook now = gameLook(look);
                boolean hasNow = now != null && !(look == Look.SPLASH && t > 0);
                floor(g, x + tile / 2.0, cy, big, 1);
                if (hasNow) {
                    game(g, now, t, pose, x + tile / 2.0, cy, big, -35);
                }
                floor(g, x + tile + tile / 2.0, cy, big, 1);
                Built f = PolishLooks.of(look, t, pose);
                tower(g, f, x + tile + tile / 2.0, cy, big, aim);
                text(g, String.format("%d parts, top %.2f", f.parts().size() + f.head().size() + f.front().size(),
                        f.top()), x + tile + 8, top + rowHeight - 4, DIM, LABEL);
                double sx = x + tile * 2 + 28;
                floor(g, sx, cy - 20, 32, 1);
                floor(g, sx + 56, cy - 20, 32, 1);
                floor(g, sx + 8, cy + 56 - 20, 16, 1);
                floor(g, sx + 56 + 8, cy + 56 - 20, 16, 1);
                if (hasNow) {
                    game(g, now, t, pose, sx, cy - 20, 32, -35);
                    game(g, now, t, pose, sx + 8, cy + 36, 16, -35);
                }
                tower(g, f, sx + 56, cy - 20, 32, aim);
                tower(g, f, sx + 64, cy + 36, 16, aim);
            }
        }
        g.dispose();
        save(image, file);
    }

    // ---- lineup.png: every tower side by side, for the heights ---------------------------------------------------------

    private static final Object[] LINE = {TowerLook.SNIPER, Look.SPLASH, Look.STORMCALLER, Look.HEXER, Look.MORTAR,
            Look.CINDER, Look.SOULFIRE, Look.SEEKER, Look.RUBY, Look.SHADE, Look.WHITE, Look.PULSE, TowerLook.AURA};
    private static final String[] LINE_NAMES = {"Sniper", "Burst", "Stormcaller", "Hexer", "Mortar", "Dragon",
            "Soulfire", "Aerie", "Ruby", "Shade", "White", "Obelisk", "Beacon"};

    private static void lineup(File file) throws IOException {
        int big = 80;
        int step = 104;
        int rowHeight = 220;
        BufferedImage image = new BufferedImage(60 + step * LINE.length, 50 + rowHeight * 4 + 130,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = pen(image);
        g.setColor(BACK);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        text(g, "Every tower as it will stand, Tiers 0 to III at 2.5x, then Tier III at the board's own 1x. The Aerie"
                + " and its identities are the tallest at every tier.", 14, 22, TEXT, TITLE);
        for (int t = 0; t < 4; t++) {
            double cy = 50 + t * rowHeight + rowHeight - 56;
            text(g, "T" + t, 14, cy, DIM, TITLE);
            for (int i = 0; i < LINE.length; i++) {
                double cx = 60 + i * step + step / 2.0;
                floor(g, cx, cy, big, 1);
                lineTower(g, LINE[i], t, cx, cy, big);
                if (t == 3) {
                    text(g, LINE_NAMES[i], cx - 34, cy + 56, TEXT, LABEL);
                }
            }
        }
        double cy = 50 + 4 * rowHeight + 80;
        for (int i = 0; i < LINE.length; i++) {
            double cx = 60 + i * 40 + 20;
            floor(g, cx, cy, 32, 1);
        }
        for (int i = 0; i < LINE.length; i++) {
            lineTower(g, LINE[i], 3, 60 + i * 40 + 20, cy, 32);
        }
        for (int i = 0; i < LINE.length; i++) {
            double cx = 700 + i * 20 + 10;
            floor(g, cx, cy, 16, 1);
        }
        for (int i = 0; i < LINE.length; i++) {
            lineTower(g, LINE[i], 3, 700 + i * 20 + 10, cy, 16);
        }
        g.dispose();
        save(image, file);
    }

    private static void lineTower(Graphics2D g, Object which, int tier, double cx, double cy, double scale) {
        if (which instanceof TowerLook look) {
            game(g, look, tier, TowerPose.idle(), cx, cy, scale, -35);
        } else {
            Look look = (Look) which;
            if ((look == Look.STORMCALLER || look == Look.HEXER) && tier == 0) {
                return;
            }
            tower(g, PolishLooks.of(look, tier, poseFor(look)), cx, cy, scale, Math.toRadians(-35));
        }
    }

    private static void tops() {
        for (int t = 0; t < 4; t++) {
            StringBuilder line = new StringBuilder("T" + t);
            for (int i = 0; i < LINE.length; i++) {
                double top = LINE[i] instanceof TowerLook look ? TowerFoundations.of(look, t).top()
                        : PolishLooks.of((Look) LINE[i], t, TowerPose.idle()).top();
                line.append(String.format("  %s %.2f", LINE_NAMES[i], top));
            }
            System.out.println(line);
        }
    }

    // ---- heads.png: heads at their headings, and every pose a builder reads ---------------------------------------------

    private static void heads(File file) throws IOException {
        int big = 96;
        int tile = 150;
        int rowHeight = 190;
        BufferedImage image = new BufferedImage(40 + tile * 9, 50 + rowHeight * 9, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = pen(image);
        g.setColor(BACK);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        text(g, "Heads and poses at 3x. A head is prisms at their own heights, turned in plan: it turns in space.", 14,
                22, TEXT, TITLE);
        TowerPose dragon = TowerPose.idle().with(TowerCue.DRAGONS_BREATH);
        int row = 0;
        for (int i = 0; i < 16; i++) {
            if (i == 8) {
                row++;
            }
            headTile(g, Look.CINDER, 2, dragon, i * 22.5, 40 + (i % 8) * tile, 50 + row * rowHeight, big,
                    "Dragon " + (int) (i * 22.5) + " deg" + (i == 0 ? " (east)" : ""));
        }
        row++;
        String[] dragonPose = {"streaming", "streaming", "streaming", "just stopped", "cooling 1/2", "Long Barrel",
                "Tier III", "Tier III streaming"};
        TowerPose[] dragonPoses = {dragon.with(TowerCue.STREAMING), dragon.with(TowerCue.STREAMING),
                dragon.with(TowerCue.STREAMING), dragon.refilling(0f), dragon.refilling(0.5f),
                dragon.with(TowerCue.LONG_BARREL), dragon, dragon.with(TowerCue.STREAMING)};
        double[] dragonAim = {0, 135, -70, 20, 20, 20, 160, 160};
        for (int i = 0; i < 8; i++) {
            headTile(g, Look.CINDER, i >= 6 ? 3 : 2, dragonPoses[i], dragonAim[i], 40 + i * tile, 50 + row * rowHeight,
                    big, "Dragon: " + dragonPose[i]);
        }
        row++;
        for (int i = 0; i < 8; i++) {
            TowerPose pose = i == 5 ? TowerPose.idle().with(TowerCue.STREAMING) : i == 6 ? TowerPose.idle().refilling(0f)
                    : i == 7 ? TowerPose.idle().with(TowerCue.LONG_BARREL) : TowerPose.idle();
            String label = i == 5 ? "streaming" : i == 6 ? "just stopped" : i == 7 ? "Long Barrel"
                    : (int) (i * 72.0) + " deg";
            headTile(g, Look.SOULFIRE, i < 3 ? 2 : 3, pose, i < 5 ? i * 72.0 : 25, 40 + i * tile,
                    50 + row * rowHeight, big, "Soulfire: " + label);
        }
        row++;
        float[] reload = {0f, 0.25f, 0.5f, 0.75f, 1f};
        String[] reloadName = {"fired: bore smokes", "1/4: (a shell in the air)", "1/2: (a shell in the air)",
                "3/4: loaded, top shows", "ready: glint"};
        for (int i = 0; i < 5; i++) {
            TowerPose pose = i == 0 ? TowerPose.idle().reloading(0.13f) : TowerPose.idle().refilling(reload[i]);
            headTile(g, Look.MORTAR, 2, pose, -30, 40 + i * tile, 50 + row * rowHeight, big, "Mortar " + reloadName[i]);
        }
        headTile(g, Look.MORTAR, 2, TowerPose.idle().fired(), -30, 40 + 5 * tile, 50 + row * rowHeight, big,
                "Mortar: the shot");
        headTile(g, Look.MORTAR, 3, TowerPose.idle().with(TowerCue.VOLLEY), 150, 40 + 6 * tile, 50 + row * rowHeight,
                big, "Volley, loaded");
        headTile(g, Look.MORTAR, 3, TowerPose.idle().refilling(0.5f), 60, 40 + 7 * tile, 50 + row * rowHeight, big,
                "1/2 (nothing turns)");
        row++;
        LoadedShell[] shells = LoadedShell.values();
        for (int i = 0; i < shells.length; i++) {
            headTile(g, Look.MORTAR, 1, TowerPose.idle().withLoad(List.of(shells[i])), -30, 40 + i * tile,
                    50 + row * rowHeight, big, "Loaded: " + shells[i]);
        }
        row++;
        String[] thrown = {"loaded", "thrown", "reloading"};
        for (int i = 0; i < 9; i++) {
            TowerPose pose = i % 3 == 0 ? TowerPose.idle() : i % 3 == 1 ? TowerPose.idle().fired()
                    : TowerPose.idle().refilling(0.25f);
            int tier = new int[] {0, 1, 3}[i / 3];
            headTile(g, Look.SPLASH, tier, pose, 0, 40 + i * tile, 50 + row * rowHeight, big,
                    "Burst T" + tier + ": " + thrown[i % 3]);
        }
        row++;
        Look[] cued = {Look.STORMCALLER, Look.STORMCALLER, Look.HEXER, Look.HEXER, Look.SEEKER, Look.RUBY, Look.SHADE,
                Look.WHITE, Look.PULSE};
        TowerPose[] cuePose = {TowerPose.idle().with(TowerCue.CHARGED), TowerPose.idle().fired(),
                TowerPose.idle().with(TowerCue.CHARGED), TowerPose.idle().fired(),
                TowerPose.idle().with(TowerCue.RELEASE_READY), TowerPose.idle().with(TowerCue.RELEASE_READY),
                TowerPose.idle().with(TowerCue.RELEASE_READY), TowerPose.idle().with(TowerCue.RELEASE_READY),
                TowerPose.idle().with(TowerCue.CHARGED)};
        String[] cueName = {"Storm: ability full", "Storm: thrown", "Hexer: ability full", "Hexer: cast",
                "Aerie: Release ready", "Ruby: Release ready", "Shade: Release ready", "White: Release ready",
                "Obelisk: full Toll"};
        for (int i = 0; i < 9; i++) {
            headTile(g, cued[i], 3, cuePose[i], 0, 40 + i * tile, 50 + row * rowHeight - 40, big * 0.8, cueName[i]);
        }
        row++;
        float[] recoil = {1f, 0.75f, 0.5f, 0.25f, 0f};
        for (int i = 0; i < 5; i++) {
            TowerPose base = TowerPose.idle().fired();
            TowerPose pose = new TowerPose(base.cues(), 0, 0, base.load(), base.stock(), base.panes(), recoil[i], i < 2,
                    1f - recoil[i]);
            headTile(g, Look.PULSE, 3, pose, 0, 40 + i * tile, 50 + row * rowHeight, big * 0.8,
                    "Knell: recoil " + recoil[i]);
        }
        g.dispose();
        save(image, file);
    }

    private static void closeups(File file) throws IOException {
        int big = 220;
        int tile = 300;
        BufferedImage image = new BufferedImage(20 + tile * 6, 40 + 330 * 4 + 520, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = pen(image);
        g.setColor(BACK);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        text(g, "Close-ups at about 7x", 14, 22, TEXT, TITLE);
        TowerPose dragon = TowerPose.idle().with(TowerCue.DRAGONS_BREATH);
        Object[][] tiles = {
                {Look.CINDER, 2, dragon, 0.0, "Dragon, east"}, {Look.CINDER, 2, dragon, 50.0, "Dragon, south-east"},
                {Look.CINDER, 2, dragon, 120.0, "Dragon, south-west"}, {Look.CINDER, 2, dragon, -90.0, "Dragon, north"},
                {Look.CINDER, 3, dragon.with(TowerCue.STREAMING), -30.0, "Dragon streaming"},
                {Look.CINDER, 3, dragon, 200.0, "Dragon, west"},
                {Look.SOULFIRE, 2, TowerPose.idle(), -30.0, "Soulfire"},
                {Look.SOULFIRE, 2, TowerPose.idle(), 130.0, "Soulfire"},
                {Look.SOULFIRE, 3, TowerPose.idle().with(TowerCue.STREAMING), 40.0, "Soulfire streaming"},
                {Look.MORTAR, 2, TowerPose.idle().refilling(0.25f), -30.0, "Mortar 1/4"},
                {Look.MORTAR, 2, TowerPose.idle().refilling(0.5f), -30.0, "Mortar 1/2"},
                {Look.MORTAR, 2, TowerPose.idle(), -30.0, "Mortar ready"},
                {Look.SPLASH, 0, TowerPose.idle(), 0.0, "Burst T0"}, {Look.SPLASH, 2, TowerPose.idle(), 0.0, "Burst T2"},
                {Look.SPLASH, 3, TowerPose.idle(), 0.0, "Burst T3"},
                {Look.SPLASH, 3, TowerPose.idle().fired(), 0.0, "Burst T3 thrown"},
                {Look.HEXER, 1, TowerPose.idle(), 0.0, "Hexer T1"}, {Look.PULSE, 1, TowerPose.idle(), 0.0, "Obelisk T1"},
                {Look.STORMCALLER, 3, TowerPose.idle(), 0.0, "Stormcaller T3"},
                {Look.HEXER, 3, TowerPose.idle(), 0.0, "Hexer T3"}, {Look.SHADE, 3, TowerPose.idle(), 0.0, "Shade T3"},
                {Look.WHITE, 3, TowerPose.idle(), 0.0, "White T3"}, {Look.RUBY, 3, TowerPose.idle(), 0.0, "Ruby T3"},
                {Look.BURDEN, 3, TowerPose.idle(), 0.0, "Obelisk T3 Burden"}};
        for (int i = 0; i < tiles.length; i++) {
            int row = i / 6;
            double cx = 20 + (i % 6) * tile + tile / 2.0;
            double cy = 40 + row * 330 + (row == 3 ? 680 : 210);
            floor(g, cx, cy, big, row == 3 ? 2 : 0);
            tower(g, PolishLooks.of((Look) tiles[i][0], (Integer) tiles[i][1], (TowerPose) tiles[i][2]), cx, cy, big,
                    Math.toRadians((Double) tiles[i][3]));
            text(g, (String) tiles[i][4], cx - 130, cy + 130, DIM, LABEL);
        }
        g.dispose();
        save(image, file);
    }

    /** Still references of every animation: eight moments of each in a row, at 3x. */
    private static void strips(File file) throws IOException {
        int tile = 190;
        int rowHeight = 300;
        String[] names = {"Stormcaller sparks (idle), every 0.1 s", "Hexer ball (idle), every 0.6 s",
                "Aerie birds and nest (idle), every 0.7 s", "Ruby", "Shade", "White",
                "Obelisk runes (idle), every 0.5 s", "Knell (tick clock), every 0.06 s",
                "Sniper cartridge (tick clock), every 0.06 s", "Scorcher steam (tick clock), every 0.12 s",
                "Aerie flock comes in to a quiet tower (tick clock), every 0.7 s from 1.5 s quiet",
                "Aerie flock scatters as the tower fights (tick clock), every 0.2 s",
                "Mortar single reload (tick clock), every 0.3 s of a 2.4 s reload",
                "Mortar volley reload: Normal, Napalm, then Heavy (fired first) goes in last, every 0.3 s",
                "Sniper Frenzy: shake and embers (tick clock), every 0.1 s"};
        BufferedImage image = new BufferedImage(20 + tile * 8, 40 + rowHeight * names.length,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = pen(image);
        g.setColor(BACK);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        for (int row = 0; row < names.length; row++) {
            text(g, names[row], 14, 30 + row * rowHeight, TEXT, TITLE);
            for (int i = 0; i < 8; i++) {
                double cx = 20 + i * tile + tile / 2.0;
                double cy = 40 + row * rowHeight + 215;
                floor(g, cx, cy, 96, 1);
                Shape clip = g.getClip();
                g.clip(new Rectangle2D.Double(cx - tile / 2.0, cy - 250, tile, 300));
                switch (row) {
                    case 0 -> alive(g, Look.STORMCALLER, 3, TowerPose.idle(), cx, cy, 96, 0, 0.35 + i * 0.1, 11, 1);
                    case 1 -> alive(g, Look.HEXER, 3, TowerPose.idle(), cx, cy, 96, 0, i * 0.6, 11, 1);
                    case 2 -> alive(g, Look.SEEKER, 3, TowerPose.idle(), cx, cy, 96, 0, i * 0.7, 11,
                            PolishLife.ORBIT_SQUASH);
                    case 3 -> alive(g, Look.RUBY, 3, TowerPose.idle(), cx, cy, 96, 0, i * 0.7, 16,
                            PolishLife.ORBIT_SQUASH);
                    case 4 -> alive(g, Look.SHADE, 3, TowerPose.idle(), cx, cy, 96, 0, i * 0.7, 21,
                            PolishLife.ORBIT_SQUASH);
                    case 5 -> alive(g, Look.WHITE, 3, TowerPose.idle(), cx, cy, 96, 0, i * 0.7, 26,
                            PolishLife.ORBIT_SQUASH);
                    case 6 -> alive(g, Look.GRAVITY, 3, TowerPose.idle(), cx, cy, 96, 0, i * 0.5, 3, 1);
                    case 7 -> {
                        knellRing(g, cx, cy, 96, i * 0.06);
                        tower(g, PolishLooks.of(Look.PULSE, 3, knellPose(i * 0.06)), cx, cy, 96, 0);
                    }
                    case 8 -> {
                        Built f = PolishLooks.of(Look.SNIPER, 2, TowerPose.idle());
                        tower(g, f, cx, cy, 96, Math.toRadians(-35));
                        PolishLife.casing(g, cx, cy, 96, Math.toRadians(-35), f.top() + 0.05,
                                i * 0.06 / PolishLife.CASING_SECONDS);
                    }
                    case 10 -> alive(g, Look.SHADE, 3, TowerPose.idle(), cx, cy, 96, 0, i * 0.7, 21,
                            PolishLife.ORBIT_SQUASH, PolishLife.Presence.of(1.5 + i * 0.7, false, 0));
                    case 11 -> alive(g, Look.SHADE, 3, TowerPose.idle(), cx, cy, 96, 0, 3 + i * 0.2, 21,
                            PolishLife.ORBIT_SQUASH, PolishLife.Presence.of(9, true, i * 0.2));
                    case 12 -> mortarAt(g, 2, false, MORTAR_STOCKS.get(2), List.of(LoadedShell.TAR), 0.05 + i * 0.3,
                            2.4, cx, cy, 96);
                    case 13 -> mortarAt(g, 3, true, PolishLooks.MORTAR_STOCK,
                            List.of(LoadedShell.HEAVY, LoadedShell.NAPALM, LoadedShell.NORMAL), 0.05 + i * 0.3, 2.4,
                            cx, cy, 96);
                    case 14 -> {
                        Built f = PolishLooks.sniper(2, TowerPose.idle().with(TowerCue.FRENZY), i, false);
                        tower(g, f, cx, cy, 96, Math.toRadians(-20));
                        PolishShots.frenzy(g, cx, cy - PolishLooks.LIFT * (f.top() + 0.06) * 96, 96, i * 0.1, 7);
                    }
                    default -> {
                        Built f = PolishLooks.of(Look.CINDER, 2,
                                TowerPose.idle().with(TowerCue.DRAGONS_BREATH).refilling(0.25f));
                        double aim = Math.toRadians(-25);
                        tower(g, f, cx, cy, 96, aim);
                        PolishLife.steam(g, cx + Math.cos(aim) * f.muzzle() * 96,
                                cy + (Math.sin(aim) * f.muzzle() - PolishLooks.LIFT * f.muzzleHeight()) * 96, 96,
                                i * 0.12 / PolishLife.STEAM_SECONDS, 5);
                    }
                }
                g.setClip(clip);
            }
        }
        g.dispose();
        save(image, file);
    }

    private static void headTile(Graphics2D g, Look look, int tier, TowerPose pose, double aimDegrees, double x,
                                 double y, double scale, String label) {
        double cx = x + 75;
        double cy = y + 125;
        floor(g, cx, cy, scale, 0);
        tower(g, PolishLooks.of(look, tier, pose), cx, cy, scale, Math.toRadians(aimDegrees));
        text(g, label, x + 4, y + 184, DIM, LABEL);
    }

    // ---- GIFs --------------------------------------------------------------------------------------------------------

    private static void gif(File file, int width, int height, double seconds, Frame frame) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersBySuffix("gif").next();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(file)) {
            file.delete();
        }
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(file)) {
            writer.setOutput(stream);
            writer.prepareWriteSequence(null);
            int frames = (int) Math.round(seconds * FPS);
            for (int i = 0; i < frames; i++) {
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = pen(image);
                g.setColor(BACK);
                g.fillRect(0, 0, width, height);
                frame.paint(g, (double) i / FPS);
                g.dispose();
                IIOMetadata meta = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image),
                        null);
                String format = meta.getNativeMetadataFormatName();
                IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(format);
                IIOMetadataNode control = child(root, "GraphicControlExtension");
                control.setAttribute("disposalMethod", "none");
                control.setAttribute("userInputFlag", "FALSE");
                control.setAttribute("transparentColorFlag", "FALSE");
                control.setAttribute("delayTime", Integer.toString(100 / FPS));
                control.setAttribute("transparentColorIndex", "0");
                IIOMetadataNode apps = child(root, "ApplicationExtensions");
                IIOMetadataNode app = new IIOMetadataNode("ApplicationExtension");
                app.setAttribute("applicationID", "NETSCAPE");
                app.setAttribute("authenticationCode", "2.0");
                app.setUserObject(new byte[] {1, 0, 0});
                apps.appendChild(app);
                meta.setFromTree(format, root);
                writer.writeToSequence(new IIOImage(image, null, meta), null);
            }
            writer.endWriteSequence();
        }
        writer.dispose();
        System.out.println("wrote " + file);
    }

    private static IIOMetadataNode child(IIOMetadataNode root, String name) {
        for (int i = 0; i < root.getLength(); i++) {
            if (root.item(i).getNodeName().equalsIgnoreCase(name)) {
                return (IIOMetadataNode) root.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(name);
        root.appendChild(node);
        return node;
    }

    /** A row of idle towers at 3x over the same row at the board's 1x. */
    private static void idleRow(File file, String title, Look[] looks, int[] tiers, double seconds)
            throws IOException {
        int big = 96;
        int step = 190;
        int width = 30 + step * looks.length;
        gif(file, width, 420, seconds, (g, t) -> {
            text(g, title, 12, 20, TEXT, TITLE);
            for (int i = 0; i < looks.length; i++) {
                floor(g, 30 + i * step + step / 2.0, 250, big, 1);
            }
            for (int i = 0; i < looks.length; i++) {
                alive(g, looks[i], tiers[i], TowerPose.idle(), 30 + i * step + step / 2.0, 250, big, 0, t, 11 + i * 5,
                        PolishLife.ORBIT_SQUASH);
            }
            text(g, "the board's own size (32 px cell):", 12, 330, DIM, LABEL);
            for (int i = 0; i < looks.length; i++) {
                floor(g, 60 + i * 48, 390, 32, 1);
            }
            for (int i = 0; i < looks.length; i++) {
                alive(g, looks[i], tiers[i], TowerPose.idle(), 60 + i * 48, 390, 32, 0, t, 11 + i * 5,
                        PolishLife.ORBIT_SQUASH);
            }
        });
    }

    private static void orbitOptions(File file) throws IOException {
        double[] squash = {1.0, 0.62, 0.42};
        String[] names = {"A: level circle (squash 1.0)", "B: flattened to 0.62", "C: flattened to 0.42"};
        gif(file, 30 + 230 * 3, 420, 7, (g, t) -> {
            text(g, "The nest's ring and the birds round the Aerie's top: how flat the circle is drawn", 12, 20, TEXT,
                    TITLE);
            for (int i = 0; i < 3; i++) {
                double cx = 30 + i * 230 + 115;
                floor(g, cx, 250, 96, 1);
                alive(g, Look.SEEKER, 3, TowerPose.idle(), cx, 250, 96, 0, t, 21, squash[i]);
                text(g, names[i], cx - 90, 318, TEXT, LABEL);
                floor(g, cx, 390, 32, 1);
                alive(g, Look.SEEKER, 3, TowerPose.idle(), cx, 390, 32, 0, t, 21, squash[i]);
            }
        });
    }

    private static TowerPose knellPose(double sinceKnell) {
        double done = Math.clamp(sinceKnell / 0.4, 0, 1);
        float back = (float) ((1 - done) * (1 - done));
        TowerPose pose = new TowerPose(Set.of(TowerCue.CHARGED), 0, 0, List.of(), List.of(), List.of(), back, done < 0.3,
                (float) done);
        return pose.snapped(4);
    }

    private static void knell(File file) throws IOException {
        gif(file, 620, 420, 4, (g, t) -> {
            text(g, "Knell (tick clock): the stone rings once a second: thrown side to side, runes white, a ring", 12,
                    20, TEXT, TITLE);
            double since = t % 1.0;
            Look[] looks = {Look.PULSE, Look.BURDEN, Look.UMBRA};
            for (int i = 0; i < 3; i++) {
                double cx = 110 + i * 200;
                floor(g, cx, 250, 96, 1);
                knellRing(g, cx, 250, 96, since);
                tower(g, PolishLooks.of(looks[i], 3, knellPose(since)), cx, 250, 96, 0);
                floor(g, 60 + i * 48, 390, 32, 1);
            }
            for (int i = 0; i < 3; i++) {
                knellRing(g, 60 + i * 48, 390, 32, since);
                tower(g, PolishLooks.of(looks[i], 3, knellPose(since)), 60 + i * 48, 390, 32, 0);
            }
        });
    }

    /** Stands in for the knell's ring the game already paints. */
    private static void knellRing(Graphics2D g, double cx, double cy, double scale, double since) {
        double p = since / 0.5;
        if (p >= 1) {
            return;
        }
        double r = scale * (0.3 + 0.9 * p);
        g.setColor(new Color(0xc6, 0xc0, 0xe1, (int) (150 * (1 - p))));
        g.setStroke(new BasicStroke((float) (scale * 0.04)));
        g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    private static void sundial(File file) throws IOException {
        int scale = 48;
        gif(file, 20 + scale * 9 * 2 + 20, 60 + scale * 9, 6, (g, t) -> {
            text(g, "The Sundial (tick clock), one turn every 3 s out to 4 cells. Left: the wedge as it is now. Right:"
                    + " the stone's shadow in a band of sun-lit ground.", 12, 20, TEXT, TITLE);
            for (int side = 0; side < 2; side++) {
                double ox = 20 + side * (scale * 9 + 20);
                double oy = 40;
                for (int x = 0; x < 9; x++) {
                    for (int y = 0; y < 9; y++) {
                        boolean path = y == 2 || x == 6 && y > 2;
                        g.setColor(path ? PATH : FLOOR);
                        g.fill(new Rectangle2D.Double(ox + x * scale, oy + y * scale, scale, scale));
                        g.setColor(GRID);
                        g.setStroke(new BasicStroke(1f));
                        g.draw(new Rectangle2D.Double(ox + x * scale, oy + y * scale, scale, scale));
                    }
                }
                double cx = ox + 4.5 * scale;
                double cy = oy + 4.5 * scale;
                double turn = t % 3.0 / 3.0;
                double heading = turn * 2 * Math.PI - Math.PI / 2;
                double alpha = Math.min(1, Math.min(turn, 1 - turn) / 0.08);
                Color veil = TowerLooks.color(TowerLook.PULSE);
                if (side == 0) {
                    AffineTransform save = g.getTransform();
                    g.translate(cx, cy);
                    g.rotate(heading);
                    Path2D.Double wedge = new Path2D.Double();
                    wedge.moveTo(0, -0.07 * scale);
                    wedge.lineTo(4 * scale, -0.26 * scale);
                    wedge.lineTo(4 * scale, 0.26 * scale);
                    wedge.lineTo(0, 0.07 * scale);
                    wedge.closePath();
                    g.setPaint(new java.awt.LinearGradientPaint(0f, 0f, 4f * scale, 0f, new float[] {0f, 1f},
                            new Color[] {new Color(veil.getRed(), veil.getGreen(), veil.getBlue(), (int) (102 * alpha)),
                                    new Color(veil.getRed(), veil.getGreen(), veil.getBlue(), (int) (10 * alpha))}));
                    g.fill(wedge);
                    g.setTransform(save);
                    game(g, TowerLook.PULSE, 2, TowerPose.idle(), cx, cy, scale, 0);
                } else {
                    PolishLife.sundial(g, cx, cy, scale, heading, 4.0 * scale, alpha, false, veil);
                    alive(g, Look.PULSE, 2, TowerPose.idle(), cx, cy, scale, 0, t, 3, 1);
                }
            }
        });
    }

    private static void sniper(File file) throws IOException {
        gif(file, 560, 380, 4.8, (g, t) -> {
            text(g, "Sniper (tick clock): a spent cartridge flies from the breech after every shot", 12, 20, TEXT,
                    TITLE);
            int shot = (int) Math.floor(t / 1.2);
            double since = t - shot * 1.2;
            double aim = Math.toRadians(new double[] {-35, 40, 160, -120}[shot % 4]);
            TowerPose pose = since < 1.0 ? TowerPose.idle().reloading((float) (since / 1.0)).snapped(4)
                    : TowerPose.idle();
            Built f = PolishLooks.of(Look.SNIPER, 2, pose);
            floor(g, 200, 240, 128, 1);
            tower(g, f, 200, 240, 128, aim);
            PolishLife.casing(g, 200, 240, 128, aim, f.top() + 0.05, since / PolishLife.CASING_SECONDS);
            floor(g, 440, 200, 32, 1);
            tower(g, f, 440, 200, 32, aim);
            PolishLife.casing(g, 440, 200, 32, aim, f.top() + 0.05, since / PolishLife.CASING_SECONDS);
            floor(g, 440, 300, 64, 1);
            tower(g, f, 440, 300, 64, aim);
            PolishLife.casing(g, 440, 300, 64, aim, f.top() + 0.05, since / PolishLife.CASING_SECONDS);
        });
    }

    /** The racks the sheets show, Tiers 0 to III: what a tower that far up the tree might load. */
    private static final List<List<LoadedShell>> MORTAR_STOCKS = List.of(
            List.of(LoadedShell.NORMAL, LoadedShell.HEAVY),
            List.of(LoadedShell.NORMAL, LoadedShell.HEAVY, LoadedShell.NAPALM),
            List.of(LoadedShell.NORMAL, LoadedShell.HEAVY, LoadedShell.TAR, LoadedShell.FLARE),
            PolishLooks.MORTAR_STOCK);

    /**
     * One lab Mortar {@code since} seconds after a shot with a reload of {@code reload} seconds, its arc live:
     * {@code stock} on its rack, loading {@code load} (front first).
     */
    private static void mortarAt(Graphics2D g, int tier, boolean volley, List<LoadedShell> stock,
                                 List<LoadedShell> load, double since, double reload, double cx, double cy,
                                 double scale) {
        double done = Math.min(1, since / reload);
        TowerPose pose = (done < 1 ? TowerPose.idle().reloading((float) done).snapped(4) : TowerPose.idle())
                .withLoad(List.of(load.get(0))).with(TowerCue.VOLLEY, volley);
        tower(g, Built.of(PolishLooks.siegeMortar(tier, pose, stock, load)), cx, cy, scale, 0);
        PolishLife.mortarLoad(g, cx, cy, scale, tier, pose, done, volley, stock, load);
    }

    /**
     * The siege mortar's reload, a shot every 3 s: Tiers 0 to III loading one shell, each a rack of what it can load,
     * then a volley loading three, the shell fired first going in last; under them the board's own size. Left of
     * all, the Mortar as it is now.
     */
    private static void mortar(File file) throws IOException {
        int step = 190;
        gif(file, 40 + step * 6, 470, 12, (g, t) -> {
            text(g, "Mortar (tick clock): a stationary siege mortar; a shot every 3 s, the reload lifts a shell off the"
                    + " rack into the muzzle. Now, Tiers 0-III, a volley (3 shells, the first to fire goes in last)", 12,
                    20, TEXT, TITLE);
            double since = t % 3.0;
            int shot = (int) Math.floor(t / 3.0);
            LoadedShell kindNow = shot % 2 == 0 ? LoadedShell.NORMAL : LoadedShell.NAPALM;
            TowerPose now = (since < 2.4 ? TowerPose.idle().reloading((float) (since / 2.4)).snapped(4)
                    : TowerPose.idle()).withLoad(List.of(kindNow));
            floor(g, 30 + step / 2.0, 260, 112, 1);
            game(g, TowerLook.MORTAR, 2, now, 30 + step / 2.0, 260, 112, -30);
            text(g, "now", 30 + step / 2.0 - 12, 340, DIM, LABEL);
            for (int i = 0; i < 5; i++) {
                double cx = 30 + step * (i + 1) + step / 2.0;
                int tier = Math.min(3, i);
                List<LoadedShell> stock = MORTAR_STOCKS.get(tier);
                List<LoadedShell> load = i == 4
                        ? List.of(stock.get(shot % stock.size()), LoadedShell.NAPALM, LoadedShell.HEAVY)
                        : List.of(stock.get(shot % stock.size()));
                floor(g, cx, 260, 112, 1);
                mortarAt(g, tier, i == 4, stock, load, since, 2.4, cx, 260, 112);
                text(g, i == 4 ? "T3 volley" : "T" + i, cx - 20, 340, TEXT, LABEL);
                floor(g, cx, 420, 32, 1);
                mortarAt(g, tier, i == 4, stock, load, since, 2.4, cx, 420, 32);
            }
        });
    }

    private static void scorcher(File file) throws IOException {
        gif(file, 700, 380, 6, (g, t) -> {
            text(g, "Scorcher (tick clock): steam the moment the reload starts. Dragon's Breath, Soulfire, plain", 12,
                    20, TEXT, TITLE);
            double cycle = t % 3.0;
            boolean streaming = cycle < 1.2;
            double since = cycle - 1.2;
            Look[] looks = {Look.CINDER, Look.SOULFIRE, Look.CINDER};
            for (int i = 0; i < 3; i++) {
                TowerPose pose = i == 0 ? TowerPose.idle().with(TowerCue.DRAGONS_BREATH) : TowerPose.idle();
                pose = streaming ? pose.with(TowerCue.STREAMING)
                        : pose.refilling((float) Math.min(1, since / 1.5)).snapped(4);
                double aim = Math.toRadians(-25 + i * 15 + (t >= 3 ? 150 : 0));
                for (int size = 0; size < 2; size++) {
                    double scale = size == 0 ? 112 : 32;
                    double cx = size == 0 ? 120 + i * 210 : 250 + i * 60;
                    double cy = size == 0 ? 230 : 340;
                    Built f = PolishLooks.of(looks[i], 2, pose);
                    floor(g, cx, cy, scale, 0);
                    tower(g, f, cx, cy, scale, aim);
                    PolishLife.paintLives(g, f, cx, cy, scale, aim, t, 5 + i);
                    if (!streaming) {
                        PolishLife.steam(g, cx + Math.cos(aim) * f.muzzle() * scale,
                                cy + (Math.sin(aim) * f.muzzle() - PolishLooks.LIFT * f.muzzleHeight()) * scale, scale,
                                since / PolishLife.STEAM_SECONDS, 5 + i);
                    }
                }
            }
        });
    }

    /** A stand-in enemy: a disc with its own shadow, at {@code x, y}, {@code r} pixels across. */
    private static void dummy(Graphics2D g, double x, double y, double r, Color body) {
        g.setColor(new Color(0, 0, 0, 120));
        g.fill(new Ellipse2D.Double(x - r * 0.9, y - r * 0.2, r * 1.8, r * 0.9));
        g.setColor(body.darker());
        g.fill(new Ellipse2D.Double(x - r, y - r * 1.25, r * 2, r * 2));
        g.setColor(body);
        g.fill(new Ellipse2D.Double(x - r * 0.92, y - r * 1.35, r * 1.84, r * 1.84));
    }

    /** Where a lab Sniper's bullet leaves: as TowerSprites.muzzle. */
    private static double[] muzzle(Built f, double cx, double cy, double scale, double aim) {
        return new double[] {cx + Math.cos(aim) * f.muzzle() * scale,
                cy + (Math.sin(aim) * f.muzzle() - PolishLooks.LIFT * f.muzzleHeight()) * scale};
    }

    /**
     * Every round a Sniper fires, a row each: the bullet flies, lands, its trace fades, and it hops on to a second
     * enemy as a ricochet drawn the same way. Left at 3x, right at the board's 1x.
     */
    private static void sniperShots(File file) throws IOException {
        PolishShots.Round[] rounds = PolishShots.Round.values();
        String[] names = {"plain shot", "crit (trace after it lands)", "Fourth Shot (sure crit)", "Hollow Point",
                "Silver Round"};
        TowerPose[] poses = {TowerPose.idle(), TowerPose.idle(), TowerPose.idle().with(TowerCue.FOURTH_SHOT),
                TowerPose.idle().with(TowerCue.HOLLOW_POINT), TowerPose.idle().with(TowerCue.SILVER_ROUND)};
        int rowHeight = 150;
        gif(file, 1000, 40 + rowHeight * rounds.length, 2.0, (g, t) -> {
            text(g, "Sniper rounds (tick clock): the bullet, its fading trace, and a ricochet drawn as its bullet."
                    + " Left 3x, right the board's 1x", 12, 20, TEXT, TITLE);
            double cycle = t % 1.0;
            for (int r = 0; r < rounds.length; r++) {
                double y0 = 40 + r * rowHeight;
                for (int size = 0; size < 2; size++) {
                    double scale = size == 0 ? 96 : 32;
                    double cx = size == 0 ? 80 : 720;
                    double cy = y0 + (size == 0 ? 110 : 90);
                    double ex = cx + scale * (size == 0 ? 4.6 : 4.2);
                    double ey = cy - scale * 0.35;
                    double hx = ex + scale * 1.0;
                    double hy = ey + scale * 0.55;
                    g.setColor(FLOOR);
                    g.fill(new Rectangle2D.Double(cx - scale / 2, y0 + 26, ex - cx + scale * 1.6, rowHeight - 30));
                    double aim = Math.atan2(ey - cy, ex - cx);
                    TowerPose pose = cycle < 0.06 ? poses[r] : cycle < 0.6
                            ? TowerPose.idle().reloading((float) ((cycle - 0.06) / 0.54)).snapped(4) : poses[r];
                    Built f = PolishLooks.sniper(2, pose, 0, false);
                    dummy(g, ex, ey, scale * 0.14, new Color(0x8a7f74));
                    dummy(g, hx, hy, scale * 0.12, new Color(0x7f8a74));
                    tower(g, f, cx, cy, scale, aim);
                    double[] m = muzzle(PolishLooks.sniper(2, poses[r], 0, false), cx, cy, scale, aim);
                    double flown = cycle / 0.22;
                    double ty = ey - scale * 0.12;
                    PolishShots.Round shown = r == 1 && flown < 1 ? PolishShots.Round.PLAIN : rounds[r];
                    if (flown < 1) {
                        PolishShots.shot(g, shown, m[0], m[1], ex, ty, flown, 1, scale, t);
                    } else if (flown < 1.25) {
                        PolishShots.shot(g, rounds[r], m[0], m[1], ex, ty, 1, 1 - (flown - 1) / 0.25, scale, t);
                    }
                    double hop = (cycle - 0.22) / 0.25;
                    if (hop > 0 && hop < 1.2) {
                        PolishShots.ricochet(g, rounds[r], ex, ty, hx, hy - scale * 0.1, 0.25, Math.min(1, hop), scale,
                                t);
                    }
                    if (size == 0) {
                        text(g, names[r], cx - 40, y0 + 40, TEXT, LABEL);
                    }
                }
            }
        });
    }

    /**
     * What a Sniper shows while it fights: Steady Aim stacks on the lens, focused from five, the golden eye at full
     * stacks under Keen Eye; Frenzy shaking the head with embers rising; the Headhunter's laser on an Elite; the
     * Executioner's mark, possible and sure.
     */
    private static void sniperStates(File file) throws IOException {
        int tile = 170;
        gif(file, 20 + tile * 7, 560, 3.0, (g, t) -> {
            text(g, "Sniper states at 3x (top) and the board's 1x (under each): Steady Aim stacks, focus, Keen Eye",
                    12, 20, TEXT, TITLE);
            int[] stacks = {0, 1, 2, 3, 4, 6, 5};
            String[] names = {"0 stacks", "1", "2", "3", "4", "5+: focused", "full + Keen Eye"};
            for (int i = 0; i < 7; i++) {
                double cx = 20 + i * tile + tile / 2.0;
                floor(g, cx, 170, 96, 0);
                TowerPose pose = TowerPose.idle().withStacks(stacks[i]);
                Built f = PolishLooks.sniper(2, pose, 0, i == 6);
                tower(g, f, cx, 170, 96, Math.toRadians(-35));
                PolishLife.paintLives(g, f, cx, 170, 96, Math.toRadians(-35), t, 3);
                floor(g, cx, 250, 32, 0);
                tower(g, f, cx, 250, 32, Math.toRadians(-35));
                PolishLife.paintLives(g, f, cx, 250, 32, Math.toRadians(-35), t, 3);
                text(g, names[i], cx - 60, 290, DIM, LABEL);
            }
            text(g, "Frenzy (shake + embers, tick clock)", 12, 330, TEXT, TITLE);
            int step = (int) Math.floor(t * 20 / PolishLooks.FRENZY_SHAKE_TICKS);
            for (int k = 0; k < 2; k++) {
                double scale = k == 0 ? 96 : 32;
                double cx = k == 0 ? 90 : 200;
                floor(g, cx, 470, scale, 0);
                Built f = PolishLooks.sniper(2, TowerPose.idle().with(TowerCue.FRENZY).withStacks(2), step, false);
                tower(g, f, cx, 470, scale, Math.toRadians(-20));
                PolishShots.frenzy(g, cx, 470 - PolishLooks.LIFT * (f.top() + 0.06) * scale, scale, t, 7);
            }
            text(g, "Headhunter: laser on an Elite or a Boss (wall clock)", 300, 330, TEXT, TITLE);
            double hx = 330;
            double hy = 470;
            double ex = hx + 96 * 3.2;
            double ey = hy - 96 * 0.9 + 12 * Math.sin(t * 1.4);
            floor(g, hx, hy, 96, 0);
            dummy(g, ex, ey, 96 * 0.2, new Color(0xc9a24a));
            double aim = Math.atan2(ey - hy, ex - hx);
            Built hf = PolishLooks.sniper(2, TowerPose.idle().with(TowerCue.HEADHUNTER).withStacks(1), 0, false);
            tower(g, hf, hx, hy, 96, aim);
            double lensX = 0.09;
            double lensY = -0.05;
            double lx = hx + (Math.cos(aim) * lensX - Math.sin(aim) * lensY) * 96;
            double ly = hy + (Math.sin(aim) * lensX + Math.cos(aim) * lensY) * 96
                    - PolishLooks.LIFT * (hf.top() + 0.08) * 96;
            PolishShots.laser(g, lx, ly, ex, ey - 96 * 0.2, 96, t);
            text(g, "Executioner: a crit leaves it under the line and it is executed (now | new)", 760, 330, TEXT,
                    TITLE);
            double cycle = t % 1.6;
            for (int k = 0; k < 4; k++) {
                boolean fresh = k % 2 == 1;
                double scale = k < 2 ? 96 : 32;
                double vx = k < 2 ? 900 + k * 200 : 940 + (k - 2) * 60;
                double vy = k < 2 ? 450 : 540;
                double from = vx - scale * 1.4;
                double rise = scale * 0.16;
                if (cycle < 0.3) {
                    dummy(g, vx, vy, scale * 0.16, new Color(0x8a7f74));
                    PolishShots.shot(g, PolishShots.Round.CRIT, from, vy - rise - scale * 0.3, vx, vy - rise,
                            cycle / 0.3, 1, scale, t);
                } else {
                    double progress = (cycle - 0.3) / PolishShots.EXECUTION_SECONDS;
                    if (fresh) {
                        PolishShots.execution(g, vx, vy + scale * 0.04, scale * 0.12, scale, progress, 11);
                    } else {
                        PolishShots.executionNow(g, vx, vy - rise, scale, (cycle - 0.3) / 0.33);
                    }
                }
            }
        });
    }

    /**
     * The flock's whole cycle on the tick clock, for every Aerie look: quiet, the birds come in one after another,
     * circle, then the tower fights (a red mark) and they scatter; then quiet again. Wall clock for their circling.
     */
    private static void flock(File file) throws IOException {
        Look[] looks = {Look.SEEKER, Look.RUBY, Look.SHADE, Look.WHITE};
        gif(file, 30 + 190 * 4, 420, 12, (g, t) -> {
            double cycle = t % 12;
            boolean fighting = cycle >= 8.5;
            PolishLife.Presence flock = fighting ? PolishLife.Presence.of(8.5, true, cycle - 8.5)
                    : PolishLife.Presence.of(cycle, false, 0);
            text(g, "Birds only at a quiet tower: they come in after 1.5 s of quiet and scatter when it fights. Now: "
                    + (fighting ? "FIGHTING" : String.format("quiet %.1f s", cycle)), 12, 20, fighting
                    ? new Color(0xff7060) : TEXT, TITLE);
            for (int i = 0; i < looks.length; i++) {
                double cx = 30 + i * 190 + 95;
                floor(g, cx, 250, 96, 1);
                alive(g, looks[i], 3, TowerPose.idle(), cx, 250, 96, 0, t, 11 + i * 5, PolishLife.ORBIT_SQUASH, flock);
                floor(g, 60 + i * 48, 390, 32, 1);
                alive(g, looks[i], 3, TowerPose.idle(), 60 + i * 48, 390, 32, 0, t, 11 + i * 5,
                        PolishLife.ORBIT_SQUASH, flock);
            }
        });
    }

    /** An enemy stand-in for the flame sheet: one of the game's top shapes, filled, at {@code x, y}. */
    private static Shape enemyAt(int which, double x, double y, double size, double facing) {
        Shape s = switch (which % 4) {
            case 0 -> EnemyShapes.beetleShape((float) size);
            case 1 -> EnemyShapes.giantShape((float) size);
            case 2 -> EnemyShapes.lizardShape((float) size);
            default -> EnemyShapes.slabShape((float) size);
        };
        AffineTransform t = AffineTransform.getTranslateInstance(x, y);
        t.rotate(facing);
        return t.createTransformedShape(s);
    }

    private static final Color[] ENEMY_BODY = {new Color(0x8a6a4a), new Color(0x6a7a8a), new Color(0x6a8a5a),
            new Color(0x9a8a7a)};

    /**
     * The flame, now and new: left the game's stream as it is (two cones stacking their alpha and hiding the enemies),
     * right the same two streams merged, each enemy in them a silhouette with a hot rim, wisps and plasma. Under them
     * the heat ladder: the bare flame, Hot Flame, White Flame, every damage node, and the Core.
     */
    private static void flame(File file) throws IOException {
        int scale = 64;
        gif(file, 1200, 60 + scale * 6 + 200 + 330, 3, (g, t) -> {
            text(g, "The flame (tick clock). Left: now. Right: merged cones, enemies as silhouettes with a hot rim,"
                    + " wisps and plasma. Below: heat from the fuel, then the Core", 12, 20, TEXT, TITLE);
            int flicker = (int) Math.floor(t * 20 / 3);
            for (int side = 0; side < 2; side++) {
                double ox = 20 + side * (scale * 8 + 20);
                double oy = 40;
                for (int x = 0; x < 8; x++) {
                    for (int y = 0; y < 6; y++) {
                        boolean path = y == 2 || y == 3;
                        g.setColor(path ? PATH : FLOOR);
                        g.fill(new Rectangle2D.Double(ox + x * scale, oy + y * scale, scale, scale));
                        g.setColor(GRID);
                        g.setStroke(new BasicStroke(1f));
                        g.draw(new Rectangle2D.Double(ox + x * scale, oy + y * scale, scale, scale));
                    }
                }
                double[][] towers = {{1.5, 0.5, 0.80}, {3.5, 5.5, -1.40}};
                double[][] foes = {{3.0 + 0.4 * Math.sin(t), 2.6}, {4.1 + 0.3 * t % 1, 3.1}, {3.6, 2.2},
                        {5.6, 3.3}, {2.2, 3.3}};
                for (int i = 0; i < foes.length; i++) {
                    Shape body = enemyAt(i, ox + foes[i][0] * scale, oy + foes[i][1] * scale, scale * 0.16, 0);
                    g.setColor(ENEMY_BODY[i % 4]);
                    g.fill(body);
                    g.setColor(ENEMY_BODY[i % 4].brighter());
                    g.draw(body);
                }
                List<PolishFlame.Stream> streams = new ArrayList<>();
                for (double[] tw : towers) {
                    double cx = ox + tw[0] * scale;
                    double cy = oy + tw[1] * scale;
                    Built f = PolishLooks.of(Look.CINDER, 2, TowerPose.idle().with(TowerCue.STREAMING));
                    tower(g, f, cx, cy, scale, tw[2]);
                    double mx = cx + Math.cos(tw[2]) * f.muzzle() * scale;
                    double my = cy + (Math.sin(tw[2]) * f.muzzle() - PolishLooks.LIFT * f.muzzleHeight()) * scale;
                    double reach = 3.4 * scale;
                    if (side == 0) {
                        StreamDrawLab.paint(g, mx, my, tw[2], reach, Math.toRadians(30), flicker);
                    } else {
                        streams.add(new PolishFlame.Stream(mx, my, tw[2], reach, Math.toRadians(30), 0.25, 0, false,
                                flicker));
                    }
                }
                if (side == 1) {
                    List<PolishFlame.Caught> caught = new ArrayList<>();
                    for (int i = 0; i < foes.length; i++) {
                        double fx = ox + foes[i][0] * scale;
                        double fy = oy + foes[i][1] * scale;
                        for (PolishFlame.Stream s : streams) {
                            double d = Math.hypot(fx - s.x(), fy - s.y());
                            double off = Math.abs(Math.atan2(Math.sin(Math.atan2(fy - s.y(), fx - s.x()) - s.heading()),
                                    Math.cos(Math.atan2(fy - s.y(), fx - s.x()) - s.heading())));
                            if (d < s.reach() && off < s.half()) {
                                caught.add(new PolishFlame.Caught(enemyAt(i, fx, fy, scale * 0.16, 0), fx, fy,
                                        0.25 + 0.3 * i % 2, s.x(), s.y(), false));
                                break;
                            }
                        }
                    }
                    PolishFlame.paint(g, streams, caught, scale, t);
                }
            }
            double[] heats = {0, 0.25, 0.75, 1.0, 1.0};
            String[] names = {"bare flame", "Hot Flame", "+ White Flame", "+ Hot Flame II", "+ Core"};
            for (int i = 0; i < 5; i++) {
                double ox = 20 + i * 234;
                double oy = 60 + scale * 6 + 30;
                g.setColor(FLOOR);
                g.fill(new Rectangle2D.Double(ox, oy, 210, 150));
                Shape body = enemyAt(i, ox + 140, oy + 75, scale * 0.16, Math.PI);
                g.setColor(ENEMY_BODY[i % 4]);
                g.fill(body);
                PolishFlame.Stream s = new PolishFlame.Stream(ox + 15, oy + 75, 0, 185, Math.toRadians(26), heats[i],
                        i == 4 ? Math.toRadians(9) : 0, false, flicker);
                PolishFlame.paint(g, List.of(s), List.of(new PolishFlame.Caught(body, ox + 140, oy + 75,
                        heats[i] + (i == 4 ? PolishFlame.CORE_HEAT : 0), ox + 15, oy + 75, false)), scale, t);
                text(g, names[i], ox + 4, oy + 166, TEXT, LABEL);
            }
            double big = 200;
            double top = 60 + scale * 6 + 200;
            text(g, "Close-up at about 6x: an enemy standing in the jet (Hot Flame; then every damage node and the Core)",
                    12, top + 14, TEXT, TITLE);
            for (int i = 0; i < 2; i++) {
                double ox = 20 + i * 590;
                double oy = top + 24;
                g.setColor(FLOOR);
                g.fill(new Rectangle2D.Double(ox, oy, 570, 290));
                double fx = ox + 330;
                double fy = oy + 145 + 6 * Math.sin(t * 1.3 + i);
                Shape body = enemyAt(i, fx, fy, big * 0.16, Math.PI);
                g.setColor(ENEMY_BODY[i % 4]);
                g.fill(body);
                double heat = i == 0 ? 0.25 : 1.0;
                Shape clip = g.getClip();
                g.clip(new Rectangle2D.Double(ox, oy, 570, 290));
                PolishFlame.Stream s = new PolishFlame.Stream(ox + 10, oy + 145, 0, 560, Math.toRadians(20), heat,
                        i == 1 ? Math.toRadians(8) : 0, false, flicker);
                PolishFlame.paint(g, List.of(s), List.of(new PolishFlame.Caught(body, fx, fy,
                        heat + (i == 1 ? PolishFlame.CORE_HEAT : 0), ox + 10, oy + 145, false)), big, t);
                g.setClip(clip);
            }
        });
    }

    /** The game's stream as it is today (Java2DFrameRenderer.paintStream with streamTongues), for "now". */
    private static final class StreamDrawLab {
        static void paint(Graphics2D g, double x, double y, double heading, double reach, double half, int flicker) {
            AffineTransform save = g.getTransform();
            g.translate(x, y);
            g.rotate(heading);
            double[][] layers = {{reach, half, 9, 0.55, 0xe2402a}, {reach * 0.78, half * 0.66, 7, 0.75, 0xff8a2a},
                    {reach * 0.42, half * 0.26, 5, 0.9, 0xffe27a}};
            for (int i = 0; i < layers.length; i++) {
                Color c = new Color((int) layers[i][4]);
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (layers[i][3] * 255)));
                g.fill(PolishFlame.tongue(layers[i][0], layers[i][1], (int) layers[i][2], flicker * 7 + i));
            }
            g.setTransform(save);
        }
    }

    /** The Burst's stone as it flies now (size 1) and at its new size, with its shadow, at 3x and 1x. */
    private static void stones(File file) throws IOException {
        BufferedImage image = new BufferedImage(620, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = pen(image);
        g.setColor(BACK);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        text(g, "The Burst's stone in flight: now (size 1) and new (size " + PolishShots.STONE_SIZE + "), 3x and 1x",
                12, 20, TEXT, TITLE);
        float[] sizes = {1f, PolishShots.STONE_SIZE};
        for (int k = 0; k < 2; k++) {
            for (int z = 0; z < 2; z++) {
                double scale = z == 0 ? 96 : 32;
                double cx = 40 + k * 300 + (z == 0 ? 80 : 220);
                floor(g, cx, 200, scale, 1);
                stone(g, cx, 200, scale * 0.5, sizes[k], scale);
                stone(g, cx + scale * 0.3, 200 - scale * 0.9, scale * 0.9, sizes[k], scale);
            }
            text(g, k == 0 ? "now" : "new", 40 + k * 300 + 60, 280, k == 0 ? DIM : TEXT, LABEL);
        }
        g.dispose();
        save(image, file);
    }

    /** As Java2DFrameRenderer.paintShell's plain stone: radius PROJECTILE_SIZE (5 px a 32 px cell) times size. */
    private static void stone(Graphics2D g, double x, double y, double lift, float size, double scale) {
        double radius = 5.0 * size * scale / 32;
        double sx = radius * 1.4;
        g.setColor(new Color(0x6a, 0x70, 0x84, 150));
        g.fill(new Ellipse2D.Double(x - sx, y - sx * 0.55, sx * 2, sx * 1.1));
        double cy = y - lift;
        Tones tones = Tones.of(new Color(139, 90, 43));
        g.setColor(tones.shade());
        g.fill(new Ellipse2D.Double(x - radius, cy - radius, radius * 2, radius * 2));
        double lit = radius * 0.78;
        double shift = radius - lit;
        g.setColor(tones.body());
        g.fill(new Ellipse2D.Double(x - radius, cy - radius, lit * 2 + shift, lit * 2 + shift));
        g.setColor(tones.highlight());
        g.fill(new Ellipse2D.Double(x - radius * 0.62, cy - radius * 0.62, radius * 0.56, radius * 0.56));
    }

    private static void dragonTurn(File file) throws IOException {
        gif(file, 620, 380, 6.4, (g, t) -> {
            text(g, "Dragon's Breath: the head turning through all 64 heading steps", 12, 20, TEXT, TITLE);
            double aim = Math.round(t / 6.4 * 64) * 2 * Math.PI / 64;
            TowerPose pose = TowerPose.idle().with(TowerCue.DRAGONS_BREATH);
            if (t > 3.2 && t < 4.8) {
                pose = pose.with(TowerCue.STREAMING);
            }
            Built f = PolishLooks.of(Look.CINDER, 2, pose);
            floor(g, 190, 230, 160, 0);
            tower(g, f, 190, 230, 160, aim);
            PolishLife.paintLives(g, f, 190, 230, 160, aim, t, 3);
            Built f3 = PolishLooks.of(Look.CINDER, 3, pose);
            floor(g, 430, 150, 64, 0);
            tower(g, f3, 430, 150, 64, aim);
            floor(g, 430, 290, 32, 0);
            tower(g, f, 430, 290, 32, aim);
            floor(g, 520, 290, 32, 0);
            tower(g, f3, 520, 290, 32, aim);
            floor(g, 560, 150, 16, 0);
            tower(g, f, 560, 150, 16, aim);
        });
    }
}
