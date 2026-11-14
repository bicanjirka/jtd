package td.ui;

import java.util.Optional;
import td.tower.upgrade.UpgradeState;
import td.GameEngine;
import td.economy.EconomyDelta;
import td.economy.StatusTerm;
import td.effect.EffectKind;
import td.enemy.AbilityTerm;
import td.enemy.Affix;
import td.enemy.TraitTerm;
import td.level.BuiltInLevelCatalog;
import td.term.Term;
import td.tower.AbstractTower;
import td.tower.NodeWords;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.TowerTerm;
import td.tower.proc.HitBonus;
import td.tower.proc.Proc;
import td.tower.upgrade.Mechanic;
import td.tower.upgrade.OwnedMechanics;
import td.tower.upgrade.UpgradeBonus;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.ui.render.IconFace;
import td.ui.render.Palette;
import td.util.GameHost;
import td.util.GameWorld;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The upgrade review board, headless. Paints every tower's Upgrades panel and each node's hover sheet as the game
 * does, reads the tier table of each {@code docs/features/FEATURE-next-gen-<type>-tower.md}, and writes
 * {@code index.html} (today's panels with their real hovers beside an interactive mock of each next-gen tree),
 * {@code sheets/<type>.png} and {@code ledger.md} (what retiring the nodes no table carries over leaves unused).
 * <p>
 * In package {@code td.ui} for the panel's package-private pieces. Run from the repository root:
 * {@code java -Djava.awt.headless=true -cp "$CP" td.ui.PreviewUpgrades [outDir]} (default
 * {@code target/upgrade-board}). A table it can't read stops it with the file, row and reason.
 */
public final class PreviewUpgrades {

    private static final int ZOOM = 2;
    private static final int SHEET_COLUMN = 2600;
    private static final int GLYPH = 40;
    private static final Path FEATURES = Path.of("docs", "features");
    private static final Path TEMPLATE = Path.of(".claude", "skills", "run-jtd", "upgrade-board.html");

    private final GameWorld world;
    private final Map<TowerFactory.Type, Tower> towers = new LinkedHashMap<>();
    private final Map<String, Term> termsByTitle = new LinkedHashMap<>();
    private final Map<String, String> glyphs = new LinkedHashMap<>();

    private PreviewUpgrades(GameWorld world) {
        this.world = world;
        List<Term> all = new ArrayList<>();
        all.addAll(Arrays.asList(TowerTerm.values()));
        all.addAll(Arrays.asList(EffectKind.values()));
        all.addAll(Arrays.asList(StatusTerm.values()));
        all.addAll(Arrays.asList(TraitTerm.values()));
        all.addAll(Arrays.asList(AbilityTerm.values()));
        all.addAll(Arrays.asList(Affix.values()));
        all.forEach(term -> this.termsByTitle.putIfAbsent(term.title().toLowerCase(Locale.ROOT), term));
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "target/upgrade-board");
        int[] status = {0};
        SwingUtilities.invokeAndWait(() -> {
            try {
                status[0] = run(out);
            } catch (Exception e) {
                e.printStackTrace();
                status[0] = 1;
            }
        });
        System.exit(status[0]);
    }

    private static int run(File out) throws Exception {
        new File(out, "sheets").mkdirs();
        GameEngine engine = new GameEngine(GameHost.noOp());
        engine.loadLevel(new BuiltInLevelCatalog().levels().get(0));
        GameWorld world = engine.getGameWorld();
        world.economy().apply(EconomyDelta.credits(1_000_000));
        PreviewUpgrades board = new PreviewUpgrades(world);
        board.place(engine);

        List<Object> old = new ArrayList<>();
        for (Tower tower : board.towers.values()) {
            old.add(board.dumpPanel(tower, out));
        }
        Map<String, Object> designs = new LinkedHashMap<>();
        Map<TowerFactory.Type, Spec> specs = new LinkedHashMap<>();
        for (Tower tower : board.towers.values()) {
            Path doc = FEATURES.resolve("FEATURE-next-gen-" + key(tower.getType()) + "-tower.md");
            if (Files.exists(doc)) {
                Spec spec = Spec.read(doc, tower, board);
                specs.put(tower.getType(), spec);
                designs.put(key(tower.getType()), spec.json);
            }
        }
        Map<String, Object> terms = board.termsMentioned(specs.values());
        Ledger ledger = board.ledger(specs);
        specs.forEach((type, spec) -> spec.json.put("dead", ledger.deadOf(type)));

        String html = Files.readString(TEMPLATE, StandardCharsets.UTF_8)
                .replace("/*OLD*/null", Json.write(old))
                .replace("/*DESIGNS*/null", Json.write(designs))
                .replace("/*TERMS*/null", Json.write(terms))
                .replace("/*GLYPHS*/null", Json.write(board.glyphs));
        Files.writeString(out.toPath().resolve("index.html"), html, StandardCharsets.UTF_8);
        Files.writeString(out.toPath().resolve("ledger.md"), ledger.markdown(specs), StandardCharsets.UTF_8);
        System.out.println("OK " + old.size() + " panels, " + designs.size() + " next-gen trees ("
                + String.join(", ", designs.keySet()) + "), " + ledger.retiredCount() + " retired nodes -> "
                + out.getPath());
        return 0;
    }

    static String key(TowerFactory.Type type) {
        return type.name().toLowerCase(Locale.ROOT);
    }

    private void place(GameEngine engine) {
        int scale = this.world.getBoard().scale();
        List<int[]> cells = new ArrayList<>();
        for (int y = 0; y < engine.cells().height(); y++) {
            for (int x = 0; x < engine.cells().width(); x++) {
                if (engine.cells().at(x, y).buildable()) {
                    cells.add(new int[]{x, y});
                }
            }
        }
        int next = 0;
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            int[] cell = cells.get(next);
            next += 5;
            engine.startPlacing(type, 0f);
            engine.mouseClicked(cell[0] * scale + scale / 2, cell[1] * scale + scale / 2);
            this.towers.put(type, engine.cells().at(cell[0], cell[1]).getTower());
        }
    }

    /** The panel and every hover sheet on one image; returns the panel's nodes with where each sits. */
    private Map<String, Object> dumpPanel(Tower tower, File out) throws IOException {
        int w = UpgradeTreeLayout.WIDTH;
        int h = UpgradeTreeLayout.HEIGHT;
        PanelUpgradeTree panel = new PanelUpgradeTree();
        panel.setGameWorld(this.world);
        panel.setSize(w, h);
        panel.setTower(tower);
        panel.doLayout();
        BufferedImage panelImage = image(w * ZOOM, h * ZOOM);
        Graphics2D pg = graphics(panelImage);
        pg.scale(ZOOM, ZOOM);
        panel.paint(pg);
        pg.dispose();

        UpgradeTree tree = tower.upgradeTree();
        UpgradeTreeLayout layout = UpgradeTreeLayout.of(tree, UpgradeState.none(), Optional.empty(), Set.of());
        float s = Math.min(1.15f, Math.min(w / (float) UpgradeTreeLayout.WIDTH, h / (float) UpgradeTreeLayout.HEIGHT));
        float ox = (w - UpgradeTreeLayout.WIDTH * s) / 2f;
        float oy = (h - UpgradeTreeLayout.HEIGHT * s) / 2f;

        List<BufferedImage> tips = new ArrayList<>();
        for (UpgradeNode node : tree.nodes()) {
            TooltipPanel tip = new TooltipPanel();
            tip.show(UpgradeSheetText.hover(UpgradeOffer.of(node, tower, this.world),
                    UpgradeHover.of(node, tower, this.world), tree));
            layOut(tip);
            BufferedImage tipImage = image(tip.getWidth() * ZOOM, tip.getHeight() * ZOOM);
            Graphics2D tg = graphics(tipImage);
            tg.scale(ZOOM, ZOOM);
            tip.paint(tg);
            tg.dispose();
            tips.add(tipImage);
        }
        int x = panelImage.getWidth();
        int y = 0;
        int columnWidth = 0;
        List<int[]> places = new ArrayList<>();
        for (BufferedImage tip : tips) {
            if (y + tip.getHeight() > SHEET_COLUMN) {
                x += columnWidth + 4;
                y = 0;
                columnWidth = 0;
            }
            places.add(new int[]{x, y});
            y += tip.getHeight() + 4;
            columnWidth = Math.max(columnWidth, tip.getWidth());
        }
        BufferedImage sheet = image(x + columnWidth, Math.max(panelImage.getHeight(), SHEET_COLUMN));
        Graphics2D sg = sheet.createGraphics();
        sg.setColor(Hud.BACKGROUND);
        sg.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        sg.drawImage(panelImage, 0, 0, null);
        for (int i = 0; i < tips.size(); i++) {
            sg.drawImage(tips.get(i), places.get(i)[0], places.get(i)[1], null);
        }
        sg.dispose();
        ImageIO.write(sheet, "png", new File(out, "sheets/" + key(tower.getType()) + ".png"));

        List<Object> nodes = new ArrayList<>();
        for (int i = 0; i < tree.nodes().size(); i++) {
            UpgradeNode node = tree.nodes().get(i);
            UpgradeTreeLayout.Spot spot = layout.spots().get(node);
            if (spot == null) {
                continue;
            }
            float size = spot.width() * s;
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", node.id());
            n.put("name", node.displayName());
            n.put("x", round(ox + spot.x() * s - size / 2));
            n.put("y", round(oy + spot.y() * s - size / 2));
            n.put("s", round(size));
            n.put("r", !tower.wordsOf(node).sentences().isEmpty());
            n.put("tx", places.get(i)[0] / ZOOM);
            n.put("ty", places.get(i)[1] / ZOOM);
            n.put("tw", tips.get(i).getWidth() / ZOOM);
            n.put("th", tips.get(i).getHeight() / ZOOM);
            n.put("alt", plainWords(tower.wordsOf(node)));
            nodes.add(n);
        }
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("type", key(tower.getType()));
        t.put("name", tower.getType().displayName());
        t.put("price", tower.getType().price);
        t.put("w", w);
        t.put("h", h);
        t.put("sw", sheet.getWidth() / ZOOM);
        t.put("sh", sheet.getHeight() / ZOOM);
        t.put("nodes", nodes);
        System.out.println("OK " + key(tower.getType()) + " " + tree.nodes().size() + " nodes");
        return t;
    }

    /**
     * The glyph key for {@code ref}, painted once as a white mask: a node id of {@code tower}'s tree or any tree, a
     * term's title, or a node pictogram's key whose tower has left the game. Empty when nothing matches.
     */
    String glyph(String ref, Tower tower) {
        String clean = ref.trim();
        if (clean.isEmpty() || this.glyphs.containsKey(clean)) {
            return clean.isEmpty() ? "" : clean;
        }
        IconFace face = null;
        List<Tower> order = new ArrayList<>();
        if (tower != null) {
            order.add(tower);
        }
        order.addAll(this.towers.values());
        for (Tower candidate : order) {
            UpgradeTree tree = candidate.upgradeTree();
            UpgradeNode node = tree.node(clean).orElse(null);
            if (node != null) {
                face = TermLooks.faceOfLine(candidate.getType(), tree.artNodeOf(node), tree.levelOf(node));
                break;
            }
        }
        if (face == null) {
            Term term = this.termsByTitle.get(clean.toLowerCase(Locale.ROOT));
            face = term != null ? TermLooks.faceOf(term) : keptPictogramFace(clean);
            if (face == null) {
                return "";
            }
        }
        this.glyphs.put(clean, mask(face));
        return clean;
    }

    /** A face for a node pictogram kept after its tower left, painted by its key alone; null when there is none. */
    static IconFace keptPictogramFace(String key) {
        return PictogramCatalogue.of(key).isPresent()
                ? IconFace.of(key, TermLooks.monogram(key), Palette.TERM_BASE_PERK, Palette.TERM_BASE_PERK)
                : null;
    }

    /** Each game term the docs' text names, by title: its glyph and what it does today. */
    private Map<String, Object> termsMentioned(Iterable<Spec> specs) {
        Map<String, Object> out = new TreeMap<>();
        for (Term term : this.termsByTitle.values()) {
            for (Spec spec : specs) {
                if (spec.names(term.title())) {
                    Map<String, Object> t = new LinkedHashMap<>();
                    t.put("glyph", this.glyph(term.title(), null));
                    t.put("text", Spec.inline(term.describe().asSentence().plain()));
                    out.put(term.title(), t);
                    break;
                }
            }
        }
        return out;
    }

    Term termTitled(String title) {
        return this.termsByTitle.get(title.trim().toLowerCase(Locale.ROOT));
    }

    Set<Tower> allTowers() {
        return new LinkedHashSet<>(this.towers.values());
    }

    private static String mask(IconFace face) {
        BufferedImage big = image(64, 64);
        Graphics2D g = big.createGraphics();
        TermIconPainter.paintBareSymbol(g, face, 64, Color.WHITE, Color.BLACK);
        g.dispose();
        for (int y = 0; y < big.getHeight(); y++) {
            for (int x = 0; x < big.getWidth(); x++) {
                int argb = big.getRGB(x, y);
                int alpha = argb >>> 24;
                int red = (argb >> 16) & 0xFF;
                big.setRGB(x, y, ((alpha * red / 255) << 24) | 0xFFFFFF);
            }
        }
        BufferedImage small = image(GLYPH, GLYPH);
        Graphics2D sg = graphics(small);
        sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        sg.drawImage(big, 0, 0, GLYPH, GLYPH, null);
        sg.dispose();
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(small, "png", bytes);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------- the ledger ----------

    /** One thing a node brings that code keeps alive: a mechanic class, a term, a pictogram, a stat... */
    private record Item(String kind, String name) implements Comparable<Item> {

        @Override
        public int compareTo(Item other) {
            int byKind = this.kind.compareTo(other.kind);
            return byKind != 0 ? byKind : this.name.compareTo(other.name);
        }
    }

    private record Use(TowerFactory.Type type, UpgradeNode node, boolean retired) {
    }

    final class Ledger {

        private final Map<Item, List<Use>> users = new TreeMap<>();
        private final Map<TowerFactory.Type, List<UpgradeNode>> retired = new LinkedHashMap<>();
        private final Set<Item> keptBySpec = new LinkedHashSet<>();

        boolean dead(Item item) {
            return !this.keptBySpec.contains(item) && this.users.get(item).stream().allMatch(Use::retired);
        }

        int retiredCount() {
            return this.retired.values().stream().mapToInt(List::size).sum();
        }

        /** What retiring this tower's nodes leaves unused, as "kind: name" lines. */
        List<Object> deadOf(TowerFactory.Type type) {
            List<Object> out = new ArrayList<>();
            this.users.forEach((item, uses) -> {
                if (this.dead(item) && uses.stream().anyMatch(use -> use.type() == type)) {
                    out.add(Map.of("kind", item.kind(), "name", item.name()));
                }
            });
            return out;
        }

        String markdown(Map<TowerFactory.Type, Spec> specs) {
            StringBuilder md = new StringBuilder("# Retirement ledger\n\nGenerated by `PreviewUpgrades` on ")
                    .append(LocalDate.now()).append(" from ")
                    .append(specs.isEmpty() ? "no next-gen feature docs" : specs.values().stream()
                            .map(spec -> "`" + spec.doc.getFileName() + "`").reduce((a, b) -> a + ", " + b).orElse(""))
                    .append(". A node is retired when its tower has a next-gen tree table and no row's Replaces names it. ")
                    .append("Something is unused once nothing but retired nodes brings it; a term the tables' text mentions ")
                    .append("counts as used. Terms, stats and procs can have users outside upgrades (enemies, statuses): ")
                    .append("search before deleting them. Mechanic classes reach play only through upgrades.\n");
            md.append("\n## Unused once retired\n");
            Map<String, List<Item>> byKind = new TreeMap<>();
            this.users.keySet().stream().filter(this::dead).forEach(item ->
                    byKind.computeIfAbsent(item.kind(), k -> new ArrayList<>()).add(item));
            if (byKind.isEmpty()) {
                md.append("\nNothing.\n");
            }
            byKind.forEach((kind, items) -> {
                md.append("\n### ").append(kind).append("\n\n");
                items.forEach(item -> md.append("- `").append(item.name()).append("` - ")
                        .append(this.users.get(item).stream().map(use -> nodeLabel(use.type(), use.node()))
                                .distinct().reduce((a, b) -> a + ", " + b).orElse(""))
                        .append('\n'));
            });
            this.retired.forEach((type, nodes) -> {
                md.append("\n## ").append(type.displayName()).append(": ").append(nodes.size())
                        .append(" retired nodes\n\n| Node | Id | Still used elsewhere |\n|---|---|---|\n");
                for (UpgradeNode node : nodes) {
                    List<String> kept = new ArrayList<>();
                    this.users.forEach((item, uses) -> {
                        if (!this.dead(item) && uses.stream().anyMatch(use -> use.node() == node)) {
                            kept.add(item.kind() + " `" + item.name() + "`");
                        }
                    });
                    md.append("| ").append(node.displayName()).append(" | `").append(node.id()).append("` | ")
                            .append(String.join(", ", kept)).append(" |\n");
                }
            });
            return md.toString();
        }
    }

    private static String nodeLabel(TowerFactory.Type type, UpgradeNode node) {
        return type.displayName() + " " + node.displayName();
    }

    Ledger ledger(Map<TowerFactory.Type, Spec> specs) throws ReflectiveOperationException {
        Ledger ledger = new Ledger();
        Method owned = AbstractTower.class.getDeclaredMethod("ownedMechanics");
        owned.setAccessible(true);
        for (Tower tower : this.towers.values()) {
            Spec spec = specs.get(tower.getType());
            OwnedMechanics<?> mechanics = (OwnedMechanics<?>) owned.invoke(tower);
            UpgradeTree tree = tower.upgradeTree();
            for (UpgradeNode node : tree.nodes()) {
                boolean retired = spec != null && !spec.replaced.contains(node.id());
                if (retired) {
                    ledger.retired.computeIfAbsent(tower.getType(), k -> new ArrayList<>()).add(node);
                }
                Use use = new Use(tower.getType(), node, retired);
                for (Item item : this.itemsOf(tower, tree, node, mechanics)) {
                    ledger.users.computeIfAbsent(item, k -> new ArrayList<>()).add(use);
                }
            }
        }
        for (Spec spec : specs.values()) {
            for (Item item : ledger.users.keySet()) {
                if (item.kind().equals("Terms") && spec.mentions(item.name())) {
                    ledger.keptBySpec.add(item);
                }
                if (item.kind().equals("Pictograms") && spec.pictograms.contains(item.name())) {
                    ledger.keptBySpec.add(item);
                }
            }
        }
        return ledger;
    }

    private List<Item> itemsOf(Tower tower, UpgradeTree tree, UpgradeNode node, OwnedMechanics<?> mechanics) {
        List<Item> items = new ArrayList<>();
        for (Mechanic mechanic : mechanics.catalogue().mechanicsOf(node)) {
            items.add(new Item("Mechanics", mechanic.getClass().getName()));
        }
        for (Proc proc : node.effects().procs()) {
            items.add(new Item("Procs", proc.trigger() + " -> " + proc.payload().getClass().getSimpleName()));
        }
        for (HitBonus bonus : node.effects().hitBonuses()) {
            items.add(new Item("Hit bonuses", bonus.when().getClass().getSimpleName() + " / "
                    + bonus.scaling().getClass().getSimpleName()));
        }
        node.effects().capabilities().forEach(c -> items.add(new Item("Capabilities", c.name())));
        node.effects().targeting().ifPresent(t -> items.add(new Item("Targeting", t.name())));
        NodeWords words = tower.wordsOf(node);
        for (UpgradeBonus bonus : words.stats()) {
            items.add(new Item("Stats", bonus.stat().name()));
        }
        words.sentences().forEach(sentence -> sentence.mentions().stream()
                .filter(term -> this.termsByTitle.containsValue(term))
                .forEach(term -> items.add(new Item("Terms", term.title()))));
        String pictogram = TermLooks.faceOfLine(tower.getType(), tree.artNodeOf(node), tree.levelOf(node)).pictogram();
        if (pictogram != null && !pictogram.isEmpty() && PictogramCatalogue.of(pictogram).isPresent()) {
            boolean termFace = this.termsByTitle.values().stream()
                    .anyMatch(term -> pictogram.equals(TermLooks.faceOf(term).pictogram()));
            if (!termFace) {
                items.add(new Item("Pictograms", pictogram));
            }
        }
        return items.stream().distinct().toList();
    }

    // ---------- the next-gen tree table ----------

    /** One {@code FEATURE-next-gen-<type>-tower.md}, read into what the page draws. */
    static final class Spec {

        final Path doc;
        final Map<String, Object> json = new LinkedHashMap<>();
        final Set<String> replaced = new LinkedHashSet<>();
        final Set<String> pictograms = new LinkedHashSet<>();
        private final StringBuilder text = new StringBuilder();

        private Spec(Path doc) {
            this.doc = doc;
        }

        boolean mentions(String title) {
            return Pattern.compile("\\b" + Pattern.quote(title) + "\\b", Pattern.CASE_INSENSITIVE)
                    .matcher(this.text).find();
        }

        /** Whether the text names {@code title} as a term does: capitalized, maybe plural. */
        boolean names(String title) {
            return Pattern.compile("\\b" + Pattern.quote(title) + "s?\\b").matcher(this.text).find();
        }

        static Spec read(Path doc, Tower tower, PreviewUpgrades board) throws IOException {
            Spec spec = new Spec(doc);
            List<String> lines = Files.readAllLines(doc, StandardCharsets.UTF_8);
            Map<String, List<Map<String, String>>> tables = new LinkedHashMap<>();
            Map<String, List<String>> lists = new LinkedHashMap<>();
            String section = "";
            String status = "";
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).strip();
                if (line.startsWith("**Status:**")) {
                    StringBuilder paragraph = new StringBuilder(line.substring("**Status:**".length()).strip());
                    for (int k = i + 1; k < lines.size() && !lines.get(k).isBlank(); k++) {
                        paragraph.append(' ').append(lines.get(k).strip());
                    }
                    // The tag shows the date and what is written: the first two sentences, without where they came from.
                    String[] sentences = paragraph.toString().split("(?<=\\.)\\s+", 3);
                    status = String.join(" ", Arrays.copyOf(sentences, Math.min(2, sentences.length)))
                            .replaceFirst(",\\s+from\\s.*$", "");
                }
                if (line.matches("#{2,6}\s.*")) {
                    section = line.replaceFirst("#+\s+", "").strip().toLowerCase(Locale.ROOT);
                } else if (line.startsWith("|") && !tables.containsKey(section) && i + 1 < lines.size()
                        && lines.get(i + 1).strip().startsWith("|-")) {
                    List<String> header = cells(line).stream().map(c -> c.toLowerCase(Locale.ROOT)).toList();
                    List<Map<String, String>> rows = new ArrayList<>();
                    int j = i + 2;
                    for (; j < lines.size() && lines.get(j).strip().startsWith("|"); j++) {
                        List<String> cells = cells(lines.get(j).strip());
                        Map<String, String> row = new LinkedHashMap<>();
                        for (int c = 0; c < header.size(); c++) {
                            row.put(header.get(c), c < cells.size() ? cells.get(c) : "");
                        }
                        rows.add(row);
                    }
                    tables.put(section, rows);
                    i = j - 1;
                } else if (line.matches("\\d+\\.\\s.*")) {
                    lists.computeIfAbsent(section, k -> new ArrayList<>()).add(line.replaceFirst("\\d+\\.\\s+", ""));
                } else if (!line.isEmpty() && lines.get(i).startsWith(" ") && lists.containsKey(section)) {
                    List<String> items = lists.get(section);
                    items.set(items.size() - 1, items.getLast() + " " + line);
                }
            }
            spec.build(tower, board, status, tables, lists.getOrDefault("open questions", List.of()));
            return spec;
        }

        private void build(Tower tower, PreviewUpgrades board, String status,
                           Map<String, List<Map<String, String>>> tables, List<String> questions) {
            List<Map<String, String>> bare = this.table(tables, "bare tower");
            List<Map<String, String>> tierRows = this.table(tables, "tiers");
            List<Map<String, String>> nodeRows = this.table(tables, "upgrade tree");
            this.json.put("status", status.isEmpty() ? "Draft" : inline(status));

            List<Object> base = new ArrayList<>();
            Map<String, Object> passive = new LinkedHashMap<>();
            for (Map<String, String> row : bare) {
                String stat = row.getOrDefault("stat", "");
                String value = row.getOrDefault("value", "");
                this.text.append(value).append('\n');
                for (String id : split(row.getOrDefault("replaces", ""))) {
                    this.carry(id, inline(stat.equalsIgnoreCase("passive") ? value.split(":")[0].strip() : stat), tower);
                }
                if (stat.equalsIgnoreCase("passive")) {
                    int colon = value.indexOf(':');
                    String name = colon < 0 ? value : value.substring(0, colon).strip();
                    passive.put("name", inline(name));
                    passive.put("text", inline(colon < 0 ? "" : value.substring(colon + 1).strip()));
                    passive.put("glyph", board.glyph(name, tower));
                } else {
                    base.add(List.of(inline(stat), inline(value), inline(row.getOrDefault("today", ""))));
                }
            }
            this.json.put("base", base);
            this.json.put("passive", passive);

            Map<String, String> refs = new LinkedHashMap<>();
            Map<String, Map<String, Object>> tiers = new LinkedHashMap<>();
            for (Map<String, String> row : tierRows) {
                String label = row.getOrDefault("tier", "").strip();
                Map<String, Object> tier = new LinkedHashMap<>();
                tier.put("id", "t" + tiers.size());
                tier.put("label", label.equals("0") ? "" : label);
                String colour = row.getOrDefault("colour", "").strip();
                String[] hues = colour.split("[\\s,]+");
                tier.put("hue", hues[0].isEmpty() ? "#8a8a8e" : hues[0]);
                if (hues.length > 1) {
                    tier.put("grad", List.of(hues[0], hues[1]));
                }
                String gate = row.getOrDefault("gate", "").strip();
                if (!gate.isEmpty() && !gate.equals("-")) {
                    Matcher xp = Pattern.compile("(?i)XP\\s*(\\d+)").matcher(gate);
                    Map<String, Object> g = new LinkedHashMap<>();
                    g.put("text", inline(gate));
                    if (xp.find()) {
                        g.put("xp", Integer.parseInt(xp.group(1)));
                    }
                    tier.put("gate", g);
                }
                Matcher cost = Pattern.compile("\\$\\s*(\\d+)").matcher(row.getOrDefault("unlock", ""));
                if (cost.find()) {
                    tier.put("cost", Integer.parseInt(cost.group(1)));
                }
                String fork = row.getOrDefault("fork", "").strip();
                if (!fork.isEmpty()) {
                    tier.put("fork", "c_" + slug(fork));
                }
                tier.put("items", new ArrayList<>());
                tiers.put(label, tier);
            }
            for (Map<String, String> row : nodeRows) {
                String name = row.getOrDefault("node", "").strip();
                refs.put(name.toLowerCase(Locale.ROOT), slug(name));
                String cluster = row.getOrDefault("cluster", "").strip();
                if (!cluster.isEmpty()) {
                    refs.put(cluster.toLowerCase(Locale.ROOT), "c_" + slug(cluster));
                }
            }
            Map<String, Map<String, Object>> clusters = new LinkedHashMap<>();
            for (Map<String, String> row : nodeRows) {
                String name = row.getOrDefault("node", "").strip();
                String where = this.doc.getFileName() + ", row " + name + ": ";
                Map<String, Object> tier = tiers.get(row.getOrDefault("tier", "").strip());
                if (tier == null) {
                    throw new IllegalArgumentException(where + "tier '" + row.get("tier") + "' is not in the Tiers table");
                }
                String kind = row.getOrDefault("kind", "stat").strip().toLowerCase(Locale.ROOT);
                if (!kind.equals("stat") && !kind.equals("ability")) {
                    throw new IllegalArgumentException(where + "kind must be stat or ability, not '" + kind + "'");
                }
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("id", slug(name));
                node.put("name", inline(name));
                node.put("kind", kind);
                String glyphRef = row.getOrDefault("glyph", "").strip();
                String glyph = board.glyph(glyphRef, tower);
                if (glyph.isEmpty()) {
                    throw new IllegalArgumentException(where + "glyph '" + glyphRef
                            + "' is neither a node id nor a term title");
                }
                node.put("glyph", glyph);
                this.pictogramOf(glyphRef, tower, board);
                String hue = row.getOrDefault("colour", "").strip();
                if (!hue.isEmpty()) {
                    node.put("hue", hue);
                }
                List<String> after = new ArrayList<>();
                for (String ref : split(row.getOrDefault("after", ""))) {
                    String id = refs.get(ref.toLowerCase(Locale.ROOT));
                    if (id == null) {
                        throw new IllegalArgumentException(where + "After names '" + ref + "', which no row or cluster is");
                    }
                    after.add(id);
                }
                if (!after.isEmpty()) {
                    node.put("requires", after);
                }
                Matcher cost = Pattern.compile("(\\d+)\\s*(\\S.*)?").matcher(row.getOrDefault("cost", "").strip());
                if (cost.matches()) {
                    node.put("cost", Integer.parseInt(cost.group(1)));
                    node.put("currency", cost.group(2) == null ? "charges" : inline(cost.group(2)));
                }
                for (String id : split(row.getOrDefault("replaces", ""))) {
                    this.carry(id, inline(name), tower);
                }
                node.put("lines", lines(row.getOrDefault("does", "")));
                node.put("notes", lines(row.getOrDefault("notes", "")));
                this.text.append(row.getOrDefault("does", "")).append('\n').append(row.getOrDefault("notes", ""));
                @SuppressWarnings("unchecked")
                List<Object> items = (List<Object>) tier.get("items");
                String cluster = row.getOrDefault("cluster", "").strip();
                if (cluster.isEmpty()) {
                    items.add(node);
                } else {
                    Map<String, Object> group = clusters.computeIfAbsent(cluster, k -> {
                        Map<String, Object> made = new LinkedHashMap<>();
                        made.put("cluster", "c_" + slug(k));
                        made.put("name", inline(k));
                        made.put("members", new ArrayList<>());
                        items.add(made);
                        return made;
                    });
                    @SuppressWarnings("unchecked")
                    List<Object> members = (List<Object>) group.get("members");
                    members.add(node);
                }
            }
            for (Map<String, Object> tier : tiers.values()) {
                Object fork = tier.get("fork");
                if (fork != null && ((List<?>) tier.get("items")).stream()
                        .noneMatch(it -> it instanceof Map<?, ?> m && fork.equals(m.get("cluster")))) {
                    throw new IllegalArgumentException(this.doc.getFileName() + ", Tiers: Fork names no cluster of its tier");
                }
                if (((List<?>) tier.get("items")).isEmpty()) {
                    tier.put("placeholder", true);
                }
            }
            this.json.put("tiers", new ArrayList<>(tiers.values()));
            this.json.putIfAbsent("carried", new LinkedHashMap<>());
            List<Object> qs = new ArrayList<>();
            for (String q : questions) {
                Matcher topic = Pattern.compile("\\*\\*(.+?)\\*\\*\\s*(.*)").matcher(q);
                qs.add(topic.matches() ? List.of(inline(topic.group(1).replaceAll("[.:]$", "")), inline(topic.group(2)))
                        : List.of("", inline(q)));
                this.text.append(q).append('\n');
            }
            this.json.put("questions", qs);
            List<Object> terms = new ArrayList<>();
            for (Map<String, String> row : tables.getOrDefault("terms", List.of())) {
                String name = row.getOrDefault("term", "").strip();
                String glyphRef = row.getOrDefault("glyph", "").strip();
                if (glyphRef.isEmpty() && board.termTitled(name) != null) {
                    glyphRef = name;
                }
                String glyph = board.glyph(glyphRef, tower);
                if (!glyphRef.isEmpty() && glyph.isEmpty()) {
                    throw new IllegalArgumentException(this.doc.getFileName() + ", term " + name + ": glyph '"
                            + glyphRef + "' is neither a node id nor a term title");
                }
                Map<String, Object> term = new LinkedHashMap<>();
                term.put("name", inline(name));
                term.put("glyph", glyph);
                term.put("lines", lines(row.getOrDefault("means", "")));
                terms.add(term);
                this.text.append(row.getOrDefault("means", "")).append('\n');
            }
            this.json.put("terms", terms);
        }

        private void carry(String id, String by, Tower tower) {
            if (tower.upgradeTree().node(id).isEmpty()) {
                throw new IllegalArgumentException(this.doc.getFileName() + ", " + by + ": Replaces names '" + id
                        + "', which is not a node of " + tower.getType().displayName());
            }
            this.replaced.add(id);
            @SuppressWarnings("unchecked")
            Map<String, Object> carried = (Map<String, Object>) this.json.computeIfAbsent("carried",
                    k -> new LinkedHashMap<String, Object>());
            carried.merge(id, by, (a, b) -> a + ", " + b);
        }

        private void pictogramOf(String ref, Tower tower, PreviewUpgrades board) {
            Term term = board.termTitled(ref);
            IconFace face = null;
            if (term != null) {
                face = TermLooks.faceOf(term);
            } else {
                for (Tower candidate : board.allTowers()) {
                    UpgradeNode node = candidate.upgradeTree().node(ref).orElse(null);
                    if (node != null) {
                        face = TermLooks.faceOfLine(candidate.getType(), candidate.upgradeTree().artNodeOf(node),
                                candidate.upgradeTree().levelOf(node));
                        break;
                    }
                }
                if (face == null) {
                    face = keptPictogramFace(ref);
                }
            }
            if (face != null && face.pictogram() != null) {
                this.pictograms.add(face.pictogram());
            }
        }

        private List<Map<String, String>> table(Map<String, List<Map<String, String>>> tables, String name) {
            List<Map<String, String>> rows = tables.get(name);
            if (rows == null) {
                throw new IllegalArgumentException(this.doc.getFileName() + ": no table under a '"
                        + name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1) + "' heading");
            }
            return rows;
        }

        private static List<String> cells(String line) {
            String inner = line.substring(1, line.endsWith("|") ? line.length() - 1 : line.length());
            return Arrays.stream(inner.split("\\|", -1)).map(String::strip).toList();
        }

        private static List<String> split(String cell) {
            return Arrays.stream(cell.split(",")).map(String::strip).filter(s -> !s.isEmpty() && !s.equals("-")).toList();
        }

        private static List<Object> lines(String cell) {
            return Arrays.stream(cell.split("(?i)<br\\s*/?>")).map(String::strip).filter(s -> !s.isEmpty())
                    .map(Spec::inline).map(Object.class::cast).toList();
        }

        /** Markdown's bold, italics and code in a cell, as HTML; everything else escaped. */
        static String inline(String text) {
            String html = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            html = html.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
            html = html.replaceAll("(?<![\\w*])\\*(?!\\s)(.+?)(?<!\\s)\\*(?![\\w*])", "<em>$1</em>");
            return html.replaceAll("`(.+?)`", "<code>$1</code>");
        }

        static String slug(String name) {
            return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        }
    }

    // ---------- helpers ----------

    private static String plainWords(NodeWords words) {
        List<String> parts = new ArrayList<>();
        words.stats().forEach(bonus -> parts.add(bonus.stat().name().toLowerCase(Locale.ROOT).replace('_', ' ')));
        words.sentences().forEach(sentence -> parts.add(sentence.plain()));
        return String.join("; ", parts);
    }

    private static void layOut(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container inner) {
                layOut(inner);
            }
        }
    }

    private static BufferedImage image(int w, int h) {
        return new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
    }

    private static Graphics2D graphics(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        return g;
    }

    private static double round(float value) {
        return Math.round(value * 10) / 10.0;
    }

    /** Just enough JSON for maps, lists, strings, numbers and booleans. */
    static final class Json {

        private Json() {
        }

        static String write(Object value) {
            StringBuilder out = new StringBuilder();
            write(value, out);
            return out.toString();
        }

        private static void write(Object value, StringBuilder out) {
            switch (value) {
                case null -> out.append("null");
                case Map<?, ?> map -> {
                    out.append('{');
                    boolean first = true;
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        out.append(first ? "" : ",");
                        string(String.valueOf(entry.getKey()), out);
                        out.append(':');
                        write(entry.getValue(), out);
                        first = false;
                    }
                    out.append('}');
                }
                case List<?> list -> {
                    out.append('[');
                    for (int i = 0; i < list.size(); i++) {
                        out.append(i == 0 ? "" : ",");
                        write(list.get(i), out);
                    }
                    out.append(']');
                }
                case Number number -> out.append(number);
                case Boolean bool -> out.append(bool);
                default -> string(value.toString(), out);
            }
        }

        private static void string(String text, StringBuilder out) {
            out.append('"');
            for (char c : text.toCharArray()) {
                switch (c) {
                    case '"' -> out.append("\\\"");
                    case '\\' -> out.append("\\\\");
                    case '\n' -> out.append("\\n");
                    case '\r' -> out.append("\\r");
                    case '\t' -> out.append("\\t");
                    case '<' -> out.append("\\u003c");
                    default -> out.append(c < 0x20 ? String.format("\\u%04x", (int) c) : String.valueOf(c));
                }
            }
            out.append('"');
        }
    }
}
