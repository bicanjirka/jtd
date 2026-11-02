import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Icon lab: reads pictogram definitions and renders a contact sheet.
 * Line format: key | tone r,g,b | frame r,g,b | title | layer ; layer ; ...
 * Layer: op[@deg] path   op: F fill, E even-odd fill, S stroke 2.25, T stroke 1.5, D dim fill, K knock-out fill, X knock-out stroke 1.6
 */
public class IconLab {

    static final float UNIT = 24f;

    record Layer(char op, float rotation, Shape shape) {}
    record Icon(String key, Color tone, Color frame, String title, List<Layer> layers) {}

    public static void main(String[] args) throws Exception {
        if (args[0].equals("faces")) {
            faces(load(args[1]), args[2]);
            return;
        }
        if (args[0].equals("catalogue")) {
            catalogue(args[1], args[2]);
            return;
        }
        if (args[0].equals("pane")) {
            PaneMock.draw(load(args[1]), args[2]);
            return;
        }
        List<Icon> icons = load(args[0]);
        int cols = Integer.parseInt(args.length > 2 ? args[2] : "6");
        int cellW = 190, cellH = 118;
        int rows = (icons.size() + cols - 1) / cols;
        BufferedImage sheet = new BufferedImage(cols * cellW, rows * cellH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        for (int i = 0; i < icons.size(); i++) {
            Icon icon = icons.get(i);
            int x = (i % cols) * cellW, y = (i / cols) * cellH;
            drawCell(g, icon, x, y);
        }
        g.dispose();
        ImageIO.write(sheet, "png", new File(args[1]));
    }

    /** Sections "Title=file1+file2;Title=file" (files may use "=key" reuse across all files listed in {@code all}). */
    static void catalogue(String spec, String out) throws Exception {
        int cols = 8, cellW = 150, cellH = 50, headerH = 26;
        List<String> titles = new ArrayList<>();
        List<List<Icon>> sections = new ArrayList<>();
        String all = "";
        for (String section : spec.split(";")) {
            String[] kv = section.split("=");
            titles.add(kv[0]);
            String files = kv[1].replace('+', ',');
            all = all.isEmpty() ? files : all + "," + files;
            List<Icon> icons = load(all);
            java.util.Set<String> keys = new java.util.HashSet<>();
            for (String f : files.split(",")) {
                for (String raw : Files.readAllLines(Path.of(f))) {
                    String l = raw.strip();
                    if (!l.isEmpty() && !l.startsWith("#")) keys.add(l.split("\\|")[0].strip());
                }
            }
            sections.add(icons.stream().filter(i -> keys.contains(i.key())).toList());
        }
        int height = 8;
        for (List<Icon> s : sections) height += headerH + ((s.size() + cols - 1) / cols) * cellH + 6;
        BufferedImage sheet = new BufferedImage(cols * cellW + 12, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        hints(g);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        int y = 8;
        for (int si = 0; si < sections.size(); si++) {
            g.setColor(new Color(220, 255, 220));
            g.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
            g.drawString(titles.get(si) + "  (" + sections.get(si).size() + ")", 10, y + 18);
            g.setColor(new Color(78, 104, 78));
            g.drawLine(10, y + 22, sheet.getWidth() - 10, y + 22);
            y += headerH;
            List<Icon> icons = sections.get(si);
            for (int i = 0; i < icons.size(); i++) {
                Icon icon = icons.get(i);
                int x = 10 + (i % cols) * cellW, cy = y + (i / cols) * cellH;
                BufferedImage big = face(icon, 40, 0, "", 0f, -1f);
                BufferedImage small = face(icon, 18, 0, "", 0f, -1f);
                g.drawImage(big, x, cy + 2, null);
                g.drawImage(small, x + 44, cy + 2, null);
                g.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
                g.setColor(ink(icon.tone()));
                String title = icon.title();
                while (g.getFontMetrics().stringWidth(title) > cellW - 48 && title.length() > 3) title = title.substring(0, title.length() - 1);
                g.drawString(title, x + 44, cy + 36);
            }
            y += ((icons.size() + cols - 1) / cols) * cellH + 6;
        }
        g.dispose();
        ImageIO.write(sheet, "png", new File(out));
    }

    /** One row of states per icon: plain, stacks, sweep 30%, sweep 80%, bar, immune, resisted, numeral+progress, faded. */
    static void faces(List<Icon> icons, String out) throws Exception {
        int zoom = 4;
        int cell = 18 * zoom + 10;
        BufferedImage sheet = new BufferedImage(9 * cell + 10, icons.size() * (cell + 14) + 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        for (int r = 0; r < icons.size(); r++) {
            Icon icon = icons.get(r);
            FaceState[] states = {
                    FaceState.plain(), FaceState.plain().stacks("3"), FaceState.plain().sweep(0.3f).time("8.2"),
                    FaceState.plain().sweep(0.8f).time("1.4"), FaceState.plain().bar(0.6f), FaceState.plain().slash(1f),
                    FaceState.plain().slash(0.5f).stacks("50%"), FaceState.plain().numeral("III").stacks("3/4"),
                    FaceState.plain().fade()};
            for (int c = 0; c < states.length; c++) {
                BufferedImage face = TermFace.paint(icon, states[c]);
                g.drawImage(face, 10 + c * cell, 10 + r * (cell + 14), face.getWidth() * zoom, face.getHeight() * zoom, null);
                g.drawImage(face, 10 + c * cell + 18 * zoom - 18, 10 + r * (cell + 14) + face.getHeight() * zoom + 2, null);
            }
        }
        g.dispose();
        ImageIO.write(sheet, "png", new File(out));
    }

    record FaceState(String stacks, float sweep, String time, float bar, float slash, String numeral, boolean faded) {
        static FaceState plain() { return new FaceState("", 0f, "", -1f, 0f, "", false); }
        FaceState stacks(String s) { return new FaceState(s, sweep, time, bar, slash, numeral, faded); }
        FaceState sweep(float s) { return new FaceState(stacks, s, time, bar, slash, numeral, faded); }
        FaceState time(String t) { return new FaceState(stacks, sweep, t, bar, slash, numeral, faded); }
        FaceState bar(float b) { return new FaceState(stacks, sweep, time, b, slash, numeral, faded); }
        FaceState slash(float s) { return new FaceState(stacks, sweep, time, bar, s, numeral, faded); }
        FaceState numeral(String n) { return new FaceState(stacks, sweep, time, bar, slash, n, faded); }
        FaceState fade() { return new FaceState(stacks, sweep, time, bar, slash, numeral, true); }
    }

    /** The 18 px face with its live state, and the time label under it. */
    static final class TermFace {
        static final int SIZE = 20;
        static final int LABEL = 11;
        static final Color IMMUNE = new Color(255, 96, 86);
        static final Font COUNT_FONT = new Font(Font.DIALOG, Font.BOLD, 9);
        static final Font NUMERAL_FONT = new Font(Font.DIALOG, Font.BOLD, 8);
        static final Font TIME_FONT = new Font(Font.DIALOG, Font.PLAIN, 9);

        static BufferedImage paint(Icon icon, FaceState state) {
            BufferedImage img = new BufferedImage(SIZE, SIZE + LABEL, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            hints(g);
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, img.getWidth(), img.getHeight());
            if (state.faded()) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.38f));
            }
            RoundRectangle2D.Float box = new RoundRectangle2D.Float(0.5f, 0.5f, SIZE - 1f, SIZE - 1f, 4f, 4f);
            Color fill = shade(icon.tone());
            g.setColor(fill);
            g.fill(box);
            Graphics2D s = (Graphics2D) g.create();
            s.translate(2.75, 2.75);
            s.scale(14.5f / UNIT, 14.5f / UNIT);
            paintSymbol(s, icon, ink(icon.tone()), fill);
            s.dispose();
            if (state.sweep() > 0f) {
                Graphics2D w = (Graphics2D) g.create();
                w.clip(box);
                w.setColor(new Color(0, 0, 0, 150));
                w.fill(new Arc2D.Float(-SIZE / 2f, -SIZE / 2f, SIZE * 2f, SIZE * 2f, 90f, -360f * state.sweep(), Arc2D.PIE));
                w.dispose();
            }
            if (state.bar() >= 0f) {
                g.setColor(new Color(0, 0, 0, 170));
                g.fill(new Rectangle2DF(2.5f, SIZE - 4.6f, SIZE - 5f, 2.2f).shape());
                g.setColor(ink(icon.tone()));
                g.fill(new Rectangle2DF(2.5f, SIZE - 4.6f, (SIZE - 5f) * state.bar(), 2.2f).shape());
            }
            if (state.slash() > 0f) {
                Line2D.Float line = new Line2D.Float(3.6f, 3.6f, SIZE - 3.6f, SIZE - 3.6f);
                g.setColor(fill);
                g.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(line);
                g.setColor(IMMUNE);
                g.setStroke(state.slash() >= 1f ? new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                        : new BasicStroke(1.7f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{2.2f, 1.6f}, 0f));
                g.draw(line);
            }
            g.setColor(icon.frame());
            g.setStroke(new BasicStroke(1.25f));
            g.draw(box);
            if (!state.numeral().isEmpty()) {
                outlined(g, state.numeral(), NUMERAL_FONT, 2.5f, 8.5f, Color.WHITE);
            }
            if (!state.stacks().isEmpty()) {
                g.setFont(COUNT_FONT);
                float w = g.getFontMetrics().stringWidth(state.stacks());
                outlined(g, state.stacks(), COUNT_FONT, SIZE - 1.5f - w, SIZE - 2f, Color.WHITE);
            }
            if (!state.time().isEmpty()) {
                g.setFont(TIME_FONT);
                float w = g.getFontMetrics().stringWidth(state.time());
                g.setColor(new Color(220, 255, 220));
                g.drawString(state.time(), (SIZE - w) / 2f, SIZE + LABEL - 1f);
            }
            g.dispose();
            return img;
        }

        static void outlined(Graphics2D g, String text, Font font, float x, float y, Color color) {
            g.setFont(font);
            g.setColor(Color.BLACK);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx != 0 || dy != 0) g.drawString(text, x + dx, y + dy);
                }
            }
            g.setColor(color);
            g.drawString(text, x, y);
        }
    }

    /** A mock of the enemy pane, the tower pane and two tooltips, at real size, to check spacing. */
    static final class PaneMock {
        static final Color TEXT = new Color(220, 255, 220);
        static final Color VALUE = new Color(170, 205, 170);
        static final Color PROSE = new Color(150, 170, 150);
        static final Color BETTER = new Color(140, 255, 140);
        static final Color WORSE = new Color(255, 120, 120);
        static final Color BORDER = new Color(78, 104, 78);
        static final Font FONT = new Font(Font.DIALOG, Font.PLAIN, 12);
        static final Font BOLD = new Font(Font.DIALOG, Font.BOLD, 12);
        static final Font SMALL = new Font(Font.DIALOG, Font.PLAIN, 11);
        static java.util.Map<String, Icon> icons;

        static void draw(List<Icon> all, String out) throws Exception {
            icons = new java.util.HashMap<>();
            all.forEach(i -> icons.put(i.key(), i));
            BufferedImage img = new BufferedImage(880, 420, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            hints(g);
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, img.getWidth(), img.getHeight());
            enemyPane(g, 640, 10);
            towerPane(g, 420, 10);
            tooltip(g, 400, 250, "effect.unraveled", "Unravel", new String[]{
                    "Each stack lowers magic resist by 10.",
                    "Up to 5 stacks on one shared clock."}, "3 stacks · 4.2 s left");
            appliesTooltip(g, 140, 10);
            g.dispose();
            ImageIO.write(img, "png", new File(out));
        }

        static void panel(Graphics2D g, int x, int y, int w, int h, String title) {
            g.setColor(BORDER);
            g.drawRect(x, y + 6, w, h);
            g.setFont(SMALL);
            int tw = g.getFontMetrics().stringWidth(title);
            g.setColor(Color.BLACK);
            g.fillRect(x + 6, y, tw + 6, 12);
            g.setColor(TEXT);
            g.drawString(title, x + 9, y + 10);
        }

        static void enemyPane(Graphics2D g, int x0, int y0) {
            panel(g, x0, y0, 200, 230, "Info");
            int x = x0 + 8, w = 184, y = y0 + 26;
            g.setFont(BOLD);
            g.setColor(new Color(170, 170, 255));
            g.fill(new java.awt.geom.Rectangle2D.Float(x + 1, y - 9, 9, 9));
            g.setColor(TEXT);
            g.drawString("Golem", x + 16, y);
            g.setFont(SMALL);
            g.setColor(new Color(255, 215, 80));
            g.drawString("✦ Elite", x + w - 42, y);
            y += 8;
            g.setColor(new Color(12, 22, 12));
            g.fillRect(x, y, w - 36, 12);
            g.setColor(new Color(52, 122, 52));
            g.fillRect(x, y, (w - 36) * 7 / 10, 12);
            g.setColor(BORDER);
            g.drawRect(x, y, w - 36, 12);
            g.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
            g.setColor(TEXT);
            g.drawString("294 / 420", x + 50, y + 10);
            g.setFont(FONT);
            g.setColor(VALUE);
            g.drawString("$12", x + w - 24, y + 11);
            y += 30;
            y = statRow(g, x, y, w, "›", "Speed", "22 px/s", WORSE);
            y = statRow(g, x, y, w, "·", "Armor", "40", VALUE);
            y = statRow(g, x, y, w, "·", "Magic resist", "-30", BETTER);
            y = statRow(g, x, y, w, "·", "Regeneration", "4/s", VALUE);
            y += 4;
            y = iconRow(g, x, y, w, "Traits", new String[]{"trait.regeneration", "ability.brace", "!effect.freeze"},
                    new String[]{"", "", ""}, new String[]{"", "", ""});
            y = iconRow(g, x, y, w, "Buffs", new String[]{"effect.shield"}, new String[]{""}, new String[]{"4"});
            y = iconRow(g, x, y, w, "Debuffs", new String[]{"effect.chill", "effect.unraveled", "effect.burn", "effect.vulnerable",
                    "tower.stoke", "effect.cracked", "effect.toll"}, new String[]{"", "3", "", "2", "2", "", "4"},
                    new String[]{"", "4", "", "3", "", "5", ""});
            g.setFont(SMALL);
            g.setColor(PROSE);
            g.drawString("A slow wall of stone that", x, y + 6);
            g.drawString("shrugs off physical hits.", x, y + 20);
        }

        static void towerPane(Graphics2D g, int x0, int y0) {
            panel(g, x0, y0, 200, 230, "Info");
            int x = x0 + 8, w = 184, y = y0 + 26;
            g.setFont(BOLD);
            g.setColor(Color.GREEN);
            g.fill(new java.awt.geom.Path2D.Float() {{ moveTo(x + 5, y - 10); lineTo(x + 10, y); lineTo(x, y); closePath(); }});
            g.setColor(TEXT);
            g.drawString("Sniper tower", x + 16, y);
            int yy = y + 22;
            yy = statRow(g, x, yy, w, "○", "Range", "4.5", VALUE);
            yy = statRow(g, x, yy, w, "•", "Physical damage", "40 → 56", BETTER);
            yy = statRow(g, x, yy, w, "›", "Fire rate", "0.4/s", VALUE);
            yy = statRow(g, x, yy, w, "·", "Targets", "first", VALUE);
            yy += 4;
            yy = iconRow(g, x, yy, w, "Perks", new String[]{"base.range", "base.attune", "sniper.head.marksmans_eye.1",
                    "sniper.special.fifth_shot", "sniper.extra.tradecraft.1"}, new String[]{"", "", "", "3/4", "2/3"},
                    new String[]{"", "", "", "", ""}, new String[]{"II", "II", "I", "", "III"});
            yy = iconRow(g, x, yy, w, "Buffs", new String[]{"tower.steady_aim", "tower.silver_round", "tower.aura"},
                    new String[]{"3", "", "2"}, new String[]{"2", "", ""});
            yy = iconRow(g, x, yy, w, "Debuffs", new String[]{"tower.jammed"}, new String[]{""}, new String[]{""});
            yy = iconRow(g, x, yy, w, "Applies", new String[]{"effect.vulnerable"}, new String[]{""}, new String[]{""});
            g.setFont(FONT);
            yy = statRow(g, x, yy, w, "◆", "XP", "340 · Hero", VALUE);
            statRow(g, x, yy, w, "☠", "Kills", "41 · 5120 dmg", VALUE);
        }

        static int statRow(Graphics2D g, int x, int y, int w, String glyph, String label, String value, Color valueColor) {
            g.setFont(FONT);
            g.setColor(PROSE);
            g.drawString(glyph, x + 2, y);
            g.setColor(TEXT);
            g.drawString(label, x + 14, y);
            g.setColor(valueColor);
            g.drawString(value, x + w - g.getFontMetrics().stringWidth(value), y);
            return y + 16;
        }

        static int iconRow(Graphics2D g, int x, int y, int w, String label, String[] keys, String[] stacks, String[] times) {
            return iconRow(g, x, y, w, label, keys, stacks, times, new String[keys.length]);
        }

        static int iconRow(Graphics2D g, int x, int y, int w, String label, String[] keys, String[] stacks, String[] times,
                           String[] numerals) {
            int labelWidth = 48, gap = 2, cell = TermFace.SIZE + gap;
            boolean anyTime = java.util.Arrays.stream(times).anyMatch(t -> t != null && !t.isEmpty());
            int lineHeight = TermFace.SIZE + (anyTime ? TermFace.LABEL : 0) + 3;
            g.setFont(FONT);
            g.setColor(PROSE);
            g.drawString(label, x, y + 13);
            int perLine = (w - labelWidth) / cell;
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                boolean immune = key.startsWith("!");
                Icon icon = icons.get(immune ? key.substring(1) : key);
                FaceState state = FaceState.plain().stacks(stacks[i] == null ? "" : stacks[i])
                        .time(times[i] == null ? "" : times[i]).numeral(numerals[i] == null ? "" : numerals[i]);
                if (immune) state = state.slash(1f);
                if (key.equals("effect.burn")) state = state.bar(0.55f);
                if (key.equals("effect.chill")) state = state.bar(0.35f);
                if (key.equals("effect.vulnerable")) state = state.sweep(0.35f);
                if (key.equals("tower.steady_aim")) state = state.sweep(0.6f);
                BufferedImage face = TermFace.paint(icon, state);
                int col = i % perLine, row = i / perLine;
                g.drawImage(face, x + labelWidth + col * cell, y + row * lineHeight, null);
            }
            int lines = (keys.length + perLine - 1) / perLine;
            return y + lines * lineHeight + 3;
        }

        static void tooltip(Graphics2D g, int x, int y, String key, String title, String[] lines, String live) {
            int w = 230, h = 30 + lines.length * 15 + 18;
            g.setColor(new Color(8, 14, 8));
            g.fillRect(x, y, w, h);
            g.setColor(BORDER);
            g.drawRect(x, y, w, h);
            Icon icon = icons.get(key);
            g.drawImage(TermFace.paint(icon, FaceState.plain()), x + 6, y + 6, null);
            g.setFont(BOLD);
            g.setColor(ink(icon.tone()));
            g.drawString(title, x + 30, y + 19);
            g.setFont(FONT);
            g.setColor(TEXT);
            int yy = y + 42;
            for (String line : lines) {
                g.drawString(line, x + 8, yy);
                yy += 15;
            }
            g.setColor(PROSE);
            g.drawString(live, x + 8, yy + 2);
        }

        static void appliesTooltip(Graphics2D g, int x, int y) {
            int w = 250, h = 150;
            g.setColor(new Color(8, 14, 8));
            g.fillRect(x, y, w, h);
            g.setColor(BORDER);
            g.drawRect(x, y, w, h);
            Icon vul = icons.get("effect.vulnerable");
            g.drawImage(TermFace.paint(vul, FaceState.plain()), x + 6, y + 6, null);
            g.setFont(BOLD);
            g.setColor(ink(vul.tone()));
            g.drawString("Vulnerable", x + 30, y + 19);
            g.setFont(FONT);
            g.setColor(TEXT);
            g.drawString("Each stack: +15% damage taken from", x + 8, y + 42);
            g.drawString("every source. Up to 3 stacks.", x + 8, y + 57);
            g.setColor(BORDER);
            g.drawLine(x + 8, y + 66, x + w - 8, y + 66);
            g.setFont(BOLD);
            g.setColor(TEXT);
            g.drawString("Hollow Point:", x + 8, y + 84);
            g.setFont(FONT);
            g.drawString("each shot applies 1", x + 92, y + 84);
            Icon v = icons.get("effect.vulnerable");
            BufferedImage mention = face(v, 13, 0, "", 0f, -1f);
            g.drawImage(mention, x + 8, y + 90, null);
            g.setColor(v.tone());
            g.drawString("Vulnerable", x + 24, y + 101);
            g.setColor(TEXT);
            g.drawString("for 4 s.", x + 88, y + 101);
            g.setColor(PROSE);
            g.drawString("Hover a term to open its own tooltip.", x + 8, y + 132);
        }
    }

    record Rectangle2DF(float x, float y, float w, float h) {
        Shape shape() { return new java.awt.geom.Rectangle2D.Float(x, y, w, h); }
    }

    static List<Icon> load(String files) throws Exception {
        List<Icon> icons = new ArrayList<>();
        java.util.Map<String, List<Layer>> byKey = new java.util.HashMap<>();
        List<String> lines = new ArrayList<>();
        for (String file : files.split(",")) {
            lines.addAll(Files.readAllLines(Path.of(file)));
        }
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("\\|");
            String key = parts[0].strip();
            Color tone = rgb(parts[1]);
            Color frame = rgb(parts[2]);
            String title = parts[3].strip();
            List<Layer> layers = new ArrayList<>();
            if (parts.length > 4 && parts[4].strip().startsWith("=")) {
                layers.addAll(byKey.get(parts[4].strip().substring(1).strip()));
            } else if (parts.length > 4) {
                for (String layerText : parts[4].split(";")) {
                    String t = layerText.strip();
                    if (t.isEmpty()) continue;
                    int space = t.indexOf(' ');
                    String head = t.substring(0, space);
                    char op = head.charAt(0);
                    float rot = head.contains("@") ? Float.parseFloat(head.substring(head.indexOf('@') + 1)) : 0f;
                    Shape s = PictogramPath.parse(t.substring(space + 1));
                    if (rot != 0f) {
                        s = AffineTransform.getRotateInstance(Math.toRadians(rot), 12, 12).createTransformedShape(s);
                    }
                    layers.add(new Layer(op, rot, s));
                }
            }
            byKey.put(key, layers);
            if (!key.startsWith("_")) {
                icons.add(new Icon(key, tone, frame, title, layers));
            }
        }
        return icons;
    }

    static void drawCell(Graphics2D g, Icon icon, int x, int y) {
        // Big vector preview: 80 px on a faint grid.
        Graphics2D big = (Graphics2D) g.create();
        hints(big);
        big.translate(x + 6, y + 6);
        big.setColor(new Color(20, 26, 20));
        big.fillRect(0, 0, 84, 84);
        big.setColor(new Color(40, 52, 40));
        for (int k = 0; k <= 24; k += 4) {
            big.drawLine(Math.round(k * 3.5f), 0, Math.round(k * 3.5f), 84);
            big.drawLine(0, Math.round(k * 3.5f), 84, Math.round(k * 3.5f));
        }
        big.scale(3.5, 3.5);
        paintSymbol(big, icon, ink(icon.tone()), new Color(20, 26, 20));
        big.dispose();
        // Real faces: 18 px row icon and 13 px inline, rendered then zoomed 3x nearest-neighbour.
        BufferedImage face18 = face(icon, 18, 3, "", 0.0f, -1f);
        BufferedImage face13 = face(icon, 13, 0, "", 0f, -1f);
        g.drawImage(face18, x + 96, y + 6, null);
        g.drawImage(face13, x + 120, y + 8, null);
        g.drawImage(face18, x + 96, y + 30, 54, 54, null);
        g.drawImage(face13, x + 154, y + 30, 39, 39, null);
        g.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
        g.setColor(new Color(200, 230, 200));
        g.drawString(icon.key(), x + 6, y + 104);
        g.setColor(ink(icon.tone()));
        g.drawString(icon.title(), x + 96, y + 96);
        // An inline mention as text would show it.
        BufferedImage mention = new BufferedImage(90, 16, BufferedImage.TYPE_INT_RGB);
        Graphics2D m = mention.createGraphics();
        hints(m);
        m.drawImage(face13, 0, 1, null);
        m.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        m.setColor(icon.tone());
        m.drawString(icon.title(), 16, 12);
        m.dispose();
        g.drawImage(mention, x + 138, y + 6, null);
    }

    static BufferedImage face(Icon icon, int size, int stacks, String corner, float sweep, float bar) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        hints(g);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, size, size);
        float arc = size * 4f / 18f;
        RoundRectangle2D.Float box = new RoundRectangle2D.Float(0.5f, 0.5f, size - 1f, size - 1f, arc, arc);
        Color fill = shade(icon.tone());
        g.setColor(fill);
        g.fill(box);
        float pad = size * 2.5f / 18f;
        float symbol = size - 2 * pad;
        Graphics2D s = (Graphics2D) g.create();
        s.translate(pad, pad);
        s.scale(symbol / UNIT, symbol / UNIT);
        paintSymbol(s, icon, ink(icon.tone()), fill);
        s.dispose();
        g.setColor(icon.frame());
        g.setStroke(new BasicStroke(Math.max(1f, size * 1.25f / 18f)));
        g.draw(box);
        if (stacks > 0 && size >= 16) {
            g.setFont(new Font(Font.DIALOG, Font.BOLD, 9));
            FontMetrics fm = g.getFontMetrics();
            String text = Integer.toString(stacks);
            int tx = size - 2 - fm.stringWidth(text);
            int ty = size - 2;
            g.setColor(Color.BLACK);
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) g.drawString(text, tx + dx, ty + dy);
            g.setColor(Color.WHITE);
            g.drawString(text, tx, ty);
        }
        g.dispose();
        return img;
    }

    static void paintSymbol(Graphics2D g, Icon icon, Color ink, Color background) {
        if (icon.layers().isEmpty()) {
            g.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
            FontMetrics fm = g.getFontMetrics();
            String mono = icon.title().length() >= 2 ? icon.title().substring(0, 2) : icon.title();
            g.setColor(ink);
            g.drawString(mono, (UNIT - fm.stringWidth(mono)) / 2f, (UNIT + fm.getAscent() - fm.getDescent()) / 2f);
            return;
        }
        Color dim = new Color(ink.getRed(), ink.getGreen(), ink.getBlue(), 115);
        for (Layer layer : icon.layers()) {
            switch (layer.op()) {
                case 'F' -> { g.setColor(ink); g.fill(nonZero(layer.shape())); }
                case 'E' -> { g.setColor(ink); g.fill(evenOdd(layer.shape())); }
                case 'D' -> { g.setColor(dim); g.fill(nonZero(layer.shape())); }
                case 'K' -> { g.setColor(background); g.fill(nonZero(layer.shape())); }
                case 'S' -> { g.setColor(ink); g.setStroke(new BasicStroke(2.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(layer.shape()); }
                case 'T' -> { g.setColor(ink); g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(layer.shape()); }
                case 'X' -> { g.setColor(background); g.setStroke(new BasicStroke(1.75f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(layer.shape()); }
                default -> throw new IllegalArgumentException("op " + layer.op());
            }
        }
    }

    static Shape nonZero(Shape s) {
        Path2D.Float p = new Path2D.Float(Path2D.WIND_NON_ZERO);
        p.append(s, false);
        return p;
    }

    static Shape evenOdd(Shape s) {
        Path2D.Float p = new Path2D.Float(Path2D.WIND_EVEN_ODD);
        p.append(s, false);
        return p;
    }

    static Color ink(Color tone) {
        float luminance = (0.2126f * tone.getRed() + 0.7152f * tone.getGreen() + 0.0722f * tone.getBlue()) / 255f;
        return mix(tone, Color.WHITE, luminance < 0.3f ? 0.5f : 0.3f);
    }

    static Color shade(Color tone) {
        return mix(Color.BLACK, tone, 0.24f);
    }

    static Color mix(Color a, Color b, float t) {
        return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    static Color rgb(String text) {
        String[] c = text.strip().split(",");
        return new Color(Integer.parseInt(c[0].strip()), Integer.parseInt(c[1].strip()), Integer.parseInt(c[2].strip()));
    }

    /** The SVG path subset pictograms are written in, plus {@code O cx cy r} for a circle. */
    static final class PictogramPath {

        private final String data;
        private int pos;

        private PictogramPath(String data) {
            this.data = data;
        }

        static Path2D.Float parse(String data) {
            return new PictogramPath(data).read();
        }

        private Path2D.Float read() {
            Path2D.Float path = new Path2D.Float();
            float x = 0, y = 0, startX = 0, startY = 0, ctrlX = 0, ctrlY = 0;
            char command = ' ';
            char previous = ' ';
            while (true) {
                skipSeparators();
                if (this.pos >= this.data.length()) break;
                char c = this.data.charAt(this.pos);
                if (Character.isLetter(c)) {
                    command = c;
                    this.pos++;
                } else if (command == 'M') {
                    command = 'L';
                } else if (command == 'm') {
                    command = 'l';
                } else if (command == ' ') {
                    throw new IllegalArgumentException("Path data starts with a number: " + this.data);
                }
                boolean rel = Character.isLowerCase(command);
                float ox = rel ? x : 0, oy = rel ? y : 0;
                switch (Character.toUpperCase(command)) {
                    case 'M' -> { x = ox + num(); y = oy + num(); path.moveTo(x, y); startX = x; startY = y; }
                    case 'L' -> { x = ox + num(); y = oy + num(); path.lineTo(x, y); }
                    case 'H' -> { x = (rel ? x : 0) + num(); path.lineTo(x, y); }
                    case 'V' -> { y = (rel ? y : 0) + num(); path.lineTo(x, y); }
                    case 'C' -> {
                        float x1 = ox + num(), y1 = oy + num(), x2 = ox + num(), y2 = oy + num();
                        x = ox + num(); y = oy + num();
                        path.curveTo(x1, y1, x2, y2, x, y); ctrlX = x2; ctrlY = y2;
                    }
                    case 'S' -> {
                        boolean smooth = "CcSs".indexOf(previous) >= 0;
                        float x1 = smooth ? 2 * x - ctrlX : x, y1 = smooth ? 2 * y - ctrlY : y;
                        float x2 = ox + num(), y2 = oy + num();
                        x = ox + num(); y = oy + num();
                        path.curveTo(x1, y1, x2, y2, x, y); ctrlX = x2; ctrlY = y2;
                    }
                    case 'Q' -> {
                        float x1 = ox + num(), y1 = oy + num();
                        x = ox + num(); y = oy + num();
                        path.quadTo(x1, y1, x, y); ctrlX = x1; ctrlY = y1;
                    }
                    case 'T' -> {
                        boolean smooth = "QqTt".indexOf(previous) >= 0;
                        float x1 = smooth ? 2 * x - ctrlX : x, y1 = smooth ? 2 * y - ctrlY : y;
                        x = ox + num(); y = oy + num();
                        path.quadTo(x1, y1, x, y); ctrlX = x1; ctrlY = y1;
                    }
                    case 'A' -> {
                        float rx = num(), ry = num(), rotation = num();
                        boolean large = flag(), sweep = flag();
                        float x2 = ox + num(), y2 = oy + num();
                        arcTo(path, x, y, rx, ry, rotation, large, sweep, x2, y2);
                        x = x2; y = y2;
                    }
                    case 'Z' -> { path.closePath(); x = startX; y = startY; }
                    case 'O' -> {
                        float cx = num(), cy = num(), r = num();
                        path.append(new Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r), false);
                        x = cx; y = cy; command = ' ';
                    }
                    default -> throw new IllegalArgumentException("Unknown path command " + command + " in " + this.data);
                }
                previous = command;
                if (Character.toUpperCase(command) == 'Z') command = ' ';
            }
            return path;
        }

        /** An SVG elliptical arc from (x1, y1) to (x2, y2), by the endpoint-to-centre conversion. */
        private static void arcTo(Path2D.Float path, float x1, float y1, float rxIn, float ryIn, float degrees,
                                  boolean large, boolean sweep, float x2, float y2) {
            if (x1 == x2 && y1 == y2) return;
            double rx = Math.abs(rxIn), ry = Math.abs(ryIn);
            if (rx == 0 || ry == 0) { path.lineTo(x2, y2); return; }
            double phi = Math.toRadians(degrees);
            double cos = Math.cos(phi), sin = Math.sin(phi);
            double dx = (x1 - x2) / 2.0, dy = (y1 - y2) / 2.0;
            double x1p = cos * dx + sin * dy, y1p = -sin * dx + cos * dy;
            double lambda = (x1p * x1p) / (rx * rx) + (y1p * y1p) / (ry * ry);
            if (lambda > 1) { rx *= Math.sqrt(lambda); ry *= Math.sqrt(lambda); }
            double num = rx * rx * ry * ry - rx * rx * y1p * y1p - ry * ry * x1p * x1p;
            double den = rx * rx * y1p * y1p + ry * ry * x1p * x1p;
            double coef = (large != sweep ? 1 : -1) * Math.sqrt(Math.max(0, num / den));
            double cxp = coef * rx * y1p / ry, cyp = -coef * ry * x1p / rx;
            double cx = cos * cxp - sin * cyp + (x1 + x2) / 2.0, cy = sin * cxp + cos * cyp + (y1 + y2) / 2.0;
            double ux = (x1p - cxp) / rx, uy = (y1p - cyp) / ry, vx = (-x1p - cxp) / rx, vy = (-y1p - cyp) / ry;
            double start = Math.atan2(uy, ux);
            double extent = Math.atan2(ux * vy - uy * vx, ux * vx + uy * vy);
            if (!sweep && extent > 0) extent -= 2 * Math.PI;
            if (sweep && extent < 0) extent += 2 * Math.PI;
            Arc2D.Double arc = new Arc2D.Double(cx - rx, cy - ry, 2 * rx, 2 * ry, -Math.toDegrees(start), -Math.toDegrees(extent), Arc2D.OPEN);
            Shape shape = phi == 0 ? arc : AffineTransform.getRotateInstance(phi, cx, cy).createTransformedShape(arc);
            path.append(shape, true);
        }

        private void skipSeparators() {
            while (this.pos < this.data.length() && (Character.isWhitespace(this.data.charAt(this.pos)) || this.data.charAt(this.pos) == ',')) {
                this.pos++;
            }
        }

        private boolean flag() {
            skipSeparators();
            char c = this.data.charAt(this.pos++);
            if (c != '0' && c != '1') throw new IllegalArgumentException("Arc flag must be 0 or 1 in " + this.data);
            return c == '1';
        }

        private float num() {
            skipSeparators();
            int start = this.pos;
            if (this.pos < this.data.length() && (this.data.charAt(this.pos) == '-' || this.data.charAt(this.pos) == '+')) this.pos++;
            boolean dot = false;
            while (this.pos < this.data.length()) {
                char c = this.data.charAt(this.pos);
                if (Character.isDigit(c)) {
                    this.pos++;
                } else if (c == '.' && !dot) {
                    dot = true;
                    this.pos++;
                } else {
                    break;
                }
            }
            if (start == this.pos) throw new IllegalArgumentException("Expected a number at " + start + " in " + this.data);
            return Float.parseFloat(this.data.substring(start, this.pos));
        }
    }
}
