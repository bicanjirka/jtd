package td.ui;

import td.board.BoardGround;
import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;
import td.ui.render.EnemyLook;
import td.ui.render.Palette;
import td.ui.render.TowerLook;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The checkable part of the board's look (docs/UI.md, "The board's look"), printed as a report rather than kept as a
 * test: what stands must read on every level's floor under that level's light, what persists stays in the saturation
 * band, and sibling identities stay apart. Run with the art sheets ({@code TowerArtSheets docs/art looks}).
 */
public final class LookChecks {

    private static final double READABLE = 3.0;
    private static final double SIBLING_DISTANCE = 10.0;
    /** A lane's moving chevron, painted at its alpha over the floor, reads as a mark on the ground. */
    private static final double LANE_READABLE = 2.5;
    private static final int CHEVRON_ALPHA = 210;
    /** Tower identities are the band's ceiling: nothing that persists is more saturated than the most saturated one. */
    private static final List<TowerLook> IDENTITIES = List.of(TowerLook.SNIPER, TowerLook.SPLASH,
            TowerLook.STORMCALLER, TowerLook.HEXER, TowerLook.MORTAR, TowerLook.CINDER, TowerLook.SEEKER,
            TowerLook.PULSE, TowerLook.AURA);
    private static final List<Palette> FIELDS = List.of(Palette.ZONE_BURNING, Palette.ZONE_TAR, Palette.ZONE_FROST,
            Palette.ZONE_FALLOUT, Palette.ZONE_FLARE, Palette.ZONE_HEALING, Palette.ZONE_HASTE, Palette.ZONE_WARD,
            Palette.ZONE_BLINDING, Palette.ZONE_FIELD, Palette.ZONE_FIELD_BURDEN, Palette.ZONE_FIELD_GRAVITY,
            Palette.ZONE_FIELD_UMBRA);

    private LookChecks() {
    }

    public static void main(String[] args) {
        System.out.println(report());
    }

    /** The whole report; its last line says how many checks failed. */
    public static String report() {
        StringBuilder out = new StringBuilder();
        int failures = 0;
        double band = IDENTITIES.stream().mapToDouble(look -> chroma(TowerLooks.color(look))).max().orElseThrow();
        out.append(String.format(Locale.ROOT, "Saturation band: OKLCH chroma <= %.3f (the most saturated tower)%n", band));
        for (LevelDefinition level : new BuiltInLevelCatalog().levels()) {
            failures += readability(out, level);
        }
        failures += saturation(out, band);
        failures += distances(out);
        out.append(failures == 0 ? "All look checks pass." : failures + " look check(s) fail.");
        return out.toString();
    }

    private static int readability(StringBuilder out, LevelDefinition level) {
        BoardGround ground = level.ground();
        SceneLight light = SceneLight.of(ground.light());
        Color lightest = lighter(new Color(ground.floorRgb()), new Color(ground.farFloorRgb()));
        out.append(String.format(Locale.ROOT, "%n%s: floor #%06X, far #%06X (luminance %.3f)%n", level.name(),
                ground.floorRgb(), ground.farFloorRgb(), luminance(lightest)));
        List<String> failing = new ArrayList<>();
        double weakestTower = Double.MAX_VALUE;
        for (TowerLook look : IDENTITIES) {
            Color body = TowerLooks.color(look);
            double best = Math.max(contrast(body, lightest), contrast(light.on(Tones.of(body)).highlight(), lightest));
            weakestTower = Math.min(weakestTower, best);
            if (best < READABLE) {
                failing.add(String.format(Locale.ROOT, "tower %s %.2f", look, best));
            }
        }
        double weakestEnemy = Double.MAX_VALUE;
        for (EnemyLook look : EnemyLook.values()) {
            Tones tones = light.on(EnemyLooks.of(look).tones());
            double best = Math.max(contrast(tones.body(), lightest), contrast(tones.highlight(), lightest));
            weakestEnemy = Math.min(weakestEnemy, best);
            if (best < READABLE) {
                failing.add(String.format(Locale.ROOT, "enemy %s %.2f", look, best));
            }
        }
        out.append(String.format(Locale.ROOT, "  weakest tower %.2f, weakest enemy %.2f (body or lit rim, need %.1f)%n",
                weakestTower, weakestEnemy, READABLE));
        double weakestLane = Double.MAX_VALUE;
        for (var path : level.paths()) {
            Color lane = new Color(path.color().r(), path.color().g(), path.color().b());
            double c = contrast(over(lane, CHEVRON_ALPHA, lightest), lightest);
            weakestLane = Math.min(weakestLane, c);
            if (c < LANE_READABLE) {
                failing.add(String.format(Locale.ROOT, "lane #%06X %.2f", lane.getRGB() & 0xFFFFFF, c));
            }
        }
        out.append(String.format(Locale.ROOT, "  weakest lane chevron %.2f (need %.1f)%n", weakestLane, LANE_READABLE));
        failing.forEach(line -> out.append("  FAIL ").append(line).append('\n'));
        return failing.size();
    }

    private static int saturation(StringBuilder out, double band) {
        out.append("\nSaturation of what persists:\n");
        int failures = 0;
        for (EnemyLook look : EnemyLook.values()) {
            double c = chroma(EnemyLooks.of(look).tones().body());
            if (c > band + 1e-3) {
                out.append(String.format(Locale.ROOT, "  FAIL enemy %s chroma %.3f%n", look, c));
                failures++;
            }
        }
        for (Palette field : FIELDS) {
            double c = chroma(Java2DFrameRenderer.colorFor(field));
            if (c > band + 1e-3) {
                out.append(String.format(Locale.ROOT, "  FAIL field %s chroma %.3f%n", field, c));
                failures++;
            }
        }
        if (failures == 0) {
            out.append("  every enemy body and field is in the band\n");
        }
        return failures;
    }

    private static int distances(StringBuilder out) {
        out.append("\nSibling distance (CIEDE2000, need 10):\n");
        int failures = 0;
        for (int i = 0; i < IDENTITIES.size(); i++) {
            for (int j = i + 1; j < IDENTITIES.size(); j++) {
                double d = deltaE(TowerLooks.color(IDENTITIES.get(i)), TowerLooks.color(IDENTITIES.get(j)));
                if (d < SIBLING_DISTANCE) {
                    out.append(String.format(Locale.ROOT, "  FAIL towers %s / %s %.1f%n", IDENTITIES.get(i),
                            IDENTITIES.get(j), d));
                    failures++;
                }
            }
        }
        if (failures == 0) {
            out.append("  every pair of towers is apart\n");
        }
        return failures;
    }

    // ---- colour maths -------------------------------------------------------------------------------------------

    private static Color over(Color top, int alpha, Color under) {
        float a = alpha / 255f;
        return new Color(Math.round(top.getRed() * a + under.getRed() * (1 - a)),
                Math.round(top.getGreen() * a + under.getGreen() * (1 - a)),
                Math.round(top.getBlue() * a + under.getBlue() * (1 - a)));
    }

    private static Color lighter(Color a, Color b) {
        return luminance(a) >= luminance(b) ? a : b;
    }

    static double contrast(Color a, Color b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    static double luminance(Color c) {
        return 0.2126 * linear(c.getRed()) + 0.7152 * linear(c.getGreen()) + 0.0722 * linear(c.getBlue());
    }

    private static double linear(int channel) {
        double v = channel / 255.0;
        return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    static double chroma(Color c) {
        double[] lab = oklab(c);
        return Math.hypot(lab[1], lab[2]);
    }

    private static double[] oklab(Color c) {
        double r = linear(c.getRed());
        double g = linear(c.getGreen());
        double b = linear(c.getBlue());
        double l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
        double m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
        double s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
        return new double[] {0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
                1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
                0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s};
    }

    private static double[] cielab(Color c) {
        double r = linear(c.getRed());
        double g = linear(c.getGreen());
        double b = linear(c.getBlue());
        double x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / 0.95047;
        double y = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        double z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / 1.08883;
        double fx = f(x);
        double fy = f(y);
        double fz = f(z);
        return new double[] {116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz)};
    }

    private static double f(double t) {
        return t > 216.0 / 24389 ? Math.cbrt(t) : (24389.0 / 27 * t + 16) / 116;
    }

    static double deltaE(Color c1, Color c2) {
        double[] a = cielab(c1);
        double[] b = cielab(c2);
        double cBar = (Math.hypot(a[1], a[2]) + Math.hypot(b[1], b[2])) / 2;
        double g = 0.5 * (1 - Math.sqrt(Math.pow(cBar, 7) / (Math.pow(cBar, 7) + Math.pow(25, 7))));
        double a1 = (1 + g) * a[1];
        double a2 = (1 + g) * b[1];
        double c1p = Math.hypot(a1, a[2]);
        double c2p = Math.hypot(a2, b[2]);
        double h1 = Math.toDegrees(Math.atan2(a[2], a1)) % 360;
        double h2 = Math.toDegrees(Math.atan2(b[2], a2)) % 360;
        h1 = h1 < 0 ? h1 + 360 : h1;
        h2 = h2 < 0 ? h2 + 360 : h2;
        double dL = b[0] - a[0];
        double dC = c2p - c1p;
        double dh = c1p * c2p == 0 ? 0 : Math.abs(h2 - h1) <= 180 ? h2 - h1 : h2 - h1 > 180 ? h2 - h1 - 360 : h2 - h1 + 360;
        double dH = 2 * Math.sqrt(c1p * c2p) * Math.sin(Math.toRadians(dh / 2));
        double lBar = (a[0] + b[0]) / 2;
        double cBarP = (c1p + c2p) / 2;
        double hBar = c1p * c2p == 0 ? h1 + h2
                : Math.abs(h1 - h2) <= 180 ? (h1 + h2) / 2 : h1 + h2 < 360 ? (h1 + h2 + 360) / 2 : (h1 + h2 - 360) / 2;
        double t = 1 - 0.17 * Math.cos(Math.toRadians(hBar - 30)) + 0.24 * Math.cos(Math.toRadians(2 * hBar))
                + 0.32 * Math.cos(Math.toRadians(3 * hBar + 6)) - 0.20 * Math.cos(Math.toRadians(4 * hBar - 63));
        double sl = 1 + 0.015 * Math.pow(lBar - 50, 2) / Math.sqrt(20 + Math.pow(lBar - 50, 2));
        double sc = 1 + 0.045 * cBarP;
        double sh = 1 + 0.015 * cBarP * t;
        double rt = -2 * Math.sqrt(Math.pow(cBarP, 7) / (Math.pow(cBarP, 7) + Math.pow(25, 7)))
                * Math.sin(Math.toRadians(60 * Math.exp(-Math.pow((hBar - 275) / 25, 2))));
        return Math.sqrt(Math.pow(dL / sl, 2) + Math.pow(dC / sc, 2) + Math.pow(dH / sh, 2)
                + rt * (dC / sc) * (dH / sh));
    }
}
