import td.GameEngine;
import td.cell.CellGrid;
import td.damage.Damage;
import td.effect.DamageSink;
import td.economy.EconomyDelta;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeNode;
import td.ui.BoardRenderer;
import td.ui.Java2DFrameRenderer;
import td.ui.render.RenderFrame;
import td.util.GameHost;
import td.util.GameWorld;
import td.util.TickRate;
import td.wave.Wave;
import td.wave.WaveContent;
import td.wave.WaveScript;
import td.zone.Zone;
import td.zone.ZoneKind;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * In-process, headless REPL for composing and rendering any board scene - a level's cells/path
 * with any mix of placed towers, spawned enemies at any rank, and mid-combat projectiles - to a
 * PNG. No {@code TowerDefense}, no {@code JFrame}, no {@code Robot}, no display required at all:
 * unlike {@code Driver.java}, everything here goes through {@link GameEngine} (itself already
 * headless and display-free by design - see its own doc comment) and the exact same
 * {@link BoardRenderer}/{@link Java2DFrameRenderer} pipeline the real board paints through, so a
 * rendered scene can't drift from the real in-game look. Reads one command per line from stdin;
 * see SKILL.md in this directory for the command list.
 * <p>
 * The balance harnesses already proved this shape (headless {@code GameEngine} + real tower
 * placement) for balance simulation; this reuses the same recipe, adding {@code spawn} for exact
 * enemy ids/ranks and a {@code render} step through the actual painting pipeline instead of a
 * text report.
 */
public class PreviewBoard {

    private static GameEngine engine;
    private static int gameTime = 0;
    private static boolean cellGrid = false;
    private static double zoom = 1;
    private static boolean squint = false;
    private static int[] crop = null;

    public static void main(String[] args) throws Exception {
        engine = new GameEngine(GameHost.noOp());

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            try {
                handle(line);
            } catch (Exception e) {
                System.out.println("ERROR: " + e);
            }
            System.out.flush();
        }
    }

    private static void handle(String line) throws Exception {
        String[] parts = line.split("\\s+", 2);
        String cmd = parts[0];
        String rest = parts.length > 1 ? parts[1] : "";
        switch (cmd) {
            case "levels" -> listLevels();
            case "level" -> loadLevel(Integer.parseInt(rest.trim()));
            case "credits" -> setCredits(rest.trim());
            case "lives" -> setLives(rest.trim());
            case "place" -> placeTower(rest.trim());
            case "upgrade" -> upgradeTower(rest.trim());
            case "spawn" -> spawnEnemy(rest.trim());
            case "script" -> spawnScript(rest.trim());
            case "effect" -> applyEffect(rest.trim());
            case "zone" -> addZone(rest.trim());
            case "wave" -> startWave();
            case "tick" -> tick(Integer.parseInt(rest.trim()));
            case "kill" -> killEnemies();
            case "render" -> render(rest.trim());
            case "record" -> record(rest.trim());
            case "zoom" -> {
                zoom = Double.parseDouble(rest.trim());
                System.out.println("OK zoom " + zoom);
            }
            case "squint" -> {
                squint = !rest.trim().equals("off");
                System.out.println("OK squint " + (squint ? "on" : "off"));
            }
            case "crop" -> setCrop(rest.trim());
            case "ground" -> tryGround(rest.trim());
            case "grid" -> {
                cellGrid = !rest.trim().equals("off");
                System.out.println("OK grid " + (cellGrid ? "on" : "off"));
            }
            case "gates" -> {
                boolean waived = rest.trim().equals("off");
                engine.getGameWorld().playtestRules().setUpgradeGatesIgnored(waived);
                System.out.println("OK gates " + (waived ? "off" : "on"));
            }
            case "hoverplace" -> hoverPlace(rest.trim());
            case "state" -> state();
            case "quit" -> {
                return;
            }
            default -> System.out.println("ERROR: unknown command '" + cmd + "'");
        }
    }

    private static void listLevels() {
        List<LevelDefinition> levels = new BuiltInLevelCatalog().levels();
        for (int i = 0; i < levels.size(); i++) {
            System.out.println(i + ": " + levels.get(i).name());
        }
    }

    private static int levelIndex = 0;

    private static void loadLevel(int index) {
        levelIndex = index;
        LevelDefinition level = new BuiltInLevelCatalog().levels().get(index);
        engine.loadLevel(level);
        gameTime = 0;
        GameWorld world = engine.getGameWorld();
        System.out.println("OK level " + index + " (" + level.name() + "), board " + world.getBoard().maxX()
                + "x" + world.getBoard().maxY() + " scale " + world.getBoard().scale());
    }

    // Same absolute-value-via-delta shape as Driver.java's setCredits/setLives.
    private static void setCredits(String arg) {
        GameWorld world = engine.getGameWorld();
        int target = Integer.parseInt(arg.trim());
        world.economy().apply(EconomyDelta.credits(target - world.economy().getCredits()));
        System.out.println("OK credits " + target);
    }

    private static void setLives(String arg) {
        GameWorld world = engine.getGameWorld();
        int target = Integer.parseInt(arg.trim());
        world.economy().apply(EconomyDelta.lives(target - world.economy().getLives()));
        System.out.println("OK lives " + target);
    }

    // Places through the real input surface (startPlacing + mouseClicked at the cell's pixel
    // centre), the same path HarnessBoard.place and TowerDefense's own mouse listener
    // use - not by reaching into engine state directly - so affordability/buildability are the
    // real rules, not reimplemented here. Range 0f: only used for the placement-preview
    // highlight radius, never the built tower's own stats (see TowerPlacement.start).
    private static void placeTower(String args) {
        String[] parts = args.split("\\s+");
        TowerFactory.Type type = TowerFactory.Type.valueOf(parts[0].toUpperCase());
        int cellX = Integer.parseInt(parts[1]);
        int cellY = Integer.parseInt(parts[2]);
        int scale = engine.getGameWorld().getBoard().scale();

        engine.startPlacing(type, 0f);
        engine.mouseClicked(cellX * scale + scale / 2, cellY * scale + scale / 2);

        CellGrid grid = engine.cells();
        boolean placed = grid.at(cellX, cellY).hasTower();
        System.out.println((placed ? "OK place " : "FAILED place (unbuildable or unaffordable) ")
                + type + " at " + cellX + "," + cellY);
    }

    // Resolves the definition at `rank`, not the always-GRUNT catalog.get(id) overload - see the
    // fix applied to Driver.java's own spawnEnemy for why that distinction matters.
    private static void spawnEnemy(String args) {
        String[] parts = args.split("\\s+", 2);
        String id = parts[0];
        Rank rank = parts.length > 1 ? Rank.valueOf(parts[1].trim().toUpperCase()) : Rank.GRUNT;

        GameWorld world = engine.getGameWorld();
        EnemyCatalog catalog = world.getEnemyCatalog();
        EnemyDefinition definition = catalog.get(id, rank);
        EnemyMob mob = catalog.spawn(id, world, 0, definition.baseHealth(), definition.price(), rank);
        world.enemies().add(mob);
        System.out.println("OK spawn " + id + " at rank " + rank);
    }

    /** {@code script <wave script>}: spawns it on the first path as the dev panel's spawn box does, Grunt by default. */
    private static void spawnScript(String script) {
        GameWorld world = engine.getGameWorld();
        WaveContent content = WaveScript.parse(script, Rank.GRUNT, world.getEnemyCatalog());
        for (EnemyMob mob : new Wave(world, content, 1, 0, 1f).spawn()) {
            world.enemies().add(mob);
        }
        System.out.println("OK script " + content.enemyCount() + " enemies");
    }

    /** {@code effect <kind> [ticks]}: puts the effect on every enemy, as a tower would. */
    private static void applyEffect(String args) {
        System.out.println(EffectCommand.apply(args, engine.getGameWorld().enemies().getEnemies()));
    }

    /** {@code zone <kind> <cellX> <cellY> [radiusCells=1] [ticks=200]}: a ground zone no tower owns. */
    private static void addZone(String args) {
        String[] parts = args.split("\s+");
        ZoneKind kind = ZoneKind.valueOf(parts[0].toUpperCase());
        int scale = engine.getGameWorld().getBoard().scale();
        double x = Integer.parseInt(parts[1]) * scale + scale / 2.0;
        double y = Integer.parseInt(parts[2]) * scale + scale / 2.0;
        float radius = (parts.length > 3 ? Float.parseFloat(parts[3]) : 1f) * scale;
        int ticks = parts.length > 4 ? Integer.parseInt(parts[4]) : 200;
        engine.getGameWorld().zones().add(new Zone(kind, x, y, radius, ticks, 100,
                (target, effect) -> target.applyEffect(effect.apply(DamageSink.none()))));
        System.out.println("OK zone " + kind);
    }

    private static void startWave() {
        if (!engine.isWaveReady()) {
            System.out.println("FAILED wave (not ready - already in progress)");
            return;
        }
        if (engine.getCurrentWaveIndex() >= engine.getWaveCount()) {
            System.out.println("FAILED wave (no more waves)");
            return;
        }
        engine.nextWave();
        System.out.println("OK wave " + engine.getCurrentWaveIndex());
    }

    private static void tick(int count) {
        for (int i = 0; i < count; i++) {
            gameTime++;
            engine.doTick(gameTime);
        }
        System.out.println("OK tick (gameTime=" + gameTime + ")");
    }

    /**
     * Buys the named node for the tower at a cell, with every node and tier unlock it needs, through the real
     * mechanism, so its tier's XP must be met first (tick a fight, or {@code gates off}); a refusal prints why.
     */
    private static void upgradeTower(String args) {
        String[] parts = args.split("\s+", 3);
        Tower tower = engine.cells().at(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])).getTower();
        String name = parts[2];
        UpgradeNode node = tower.upgradeTree().nodes().stream().filter(n -> n.displayName().equals(name))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("no node named " + name));
        var path = tower.upgradeTree().planPath(node, tower, engine.getGameWorld());
        if (tower.buyPath(node)) {
            System.out.println("OK upgrade " + name);
        } else {
            System.out.println("FAILED upgrade " + name + ": " + path.block().map(block -> block.at().displayName()
                    + " " + block.reason()).orElse("credits " + engine.getGameWorld().economy().getCredits()));
        }
    }

    // Starts placing a tower and points at a cell, so the render shows the placement highlight.
    private static void hoverPlace(String args) {
        String[] parts = args.split("\s+");
        int scale = engine.getGameWorld().getBoard().scale();
        engine.startPlacing(td.tower.TowerFactory.Type.valueOf(parts[0].toUpperCase()), parts.length > 3 ? Float.parseFloat(parts[3]) : 0f);
        engine.highlightCell(Integer.parseInt(parts[1]) * scale + scale / 2, Integer.parseInt(parts[2]) * scale + scale / 2);
        System.out.println("OK hoverplace " + args);
    }

    private static void killEnemies() {
        for (EnemyMob mob : engine.getGameWorld().enemies().getEnemies()) {
            mob.doDamage(Damage.physical(Integer.MAX_VALUE / 2));
        }
        System.out.println("OK kill");
    }

    private static void render(String path) throws Exception {
        BoardRenderer boardRenderer = renderer();
        // alpha 1.0: this is always a snapshot exactly at gameTime (the last doTick that ran),
        // never mid-interpolation into a tick that hasn't happened.
        BufferedImage image = paint(boardRenderer, gameTime, 1.0);
        Film.png(new File(path), image);
        System.out.println("OK wrote " + path + " (" + image.getWidth() + "x" + image.getHeight() + ")");
    }

    /**
     * {@code record <path.gif> <ticks> [frames per tick]}: plays that many ticks and paints every frame between them,
     * as the live board would at 1x, into a looping GIF and a strip of evenly picked frames beside it.
     */
    private static void record(String args) throws Exception {
        String[] parts = args.split("\\s+");
        String path = parts[0];
        int ticks = Integer.parseInt(parts[1]);
        int perTick = parts.length > 2 ? Integer.parseInt(parts[2]) : 2;
        BoardRenderer boardRenderer = renderer();
        List<BufferedImage> frames = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int t = 0; t < ticks; t++) {
            gameTime++;
            engine.doTick(gameTime);
            for (int f = 1; f <= perTick; f++) {
                double alpha = f / (double) perTick;
                frames.add(paint(boardRenderer, gameTime, alpha));
                labels.add(String.format(Locale.ROOT, "t%d %+.2fs", gameTime, (gameTime - 1 + alpha) / TickRate.TICKS_PER_SECOND));
            }
        }
        int delayMillis = Math.round(1000 / (TickRate.TICKS_PER_SECOND * perTick));
        Film.gif(new File(path), frames, delayMillis);
        String strip = path.replaceFirst("\\.gif$", "") + "-strip.png";
        Film.png(new File(strip), Film.strip(frames, labels, 18));
        System.out.println("OK recorded " + frames.size() + " frames to " + path + " and " + strip
                + " (gameTime=" + gameTime + ")");
    }

    /**
     * {@code ground [floor=RRGGBB] [motif=RRGGBB] [haze=0.4] [air=RRGGBB] [key=RRGGBB] [shadow=RRGGBB]}: reloads the
     * current level with its ground and light changed as given, to try a level's look before writing it down. Clears
     * the board, so give it before placing anything.
     */
    private static void tryGround(String args) {
        LevelDefinition level = new BuiltInLevelCatalog().levels().get(levelIndex);
        td.board.BoardGround ground = level.ground();
        td.board.BoardLight light = ground.light();
        int floor = ground.floorRgb();
        int motif = ground.motifRgb();
        float haze = ground.haze();
        int air = light.hazeRgb();
        int key = light.keyRgb();
        int shadow = light.shadowRgb();
        for (String pair : args.split("\\s+")) {
            String[] kv = pair.split("=");
            switch (kv[0]) {
                case "floor" -> floor = Integer.parseInt(kv[1], 16);
                case "motif" -> motif = Integer.parseInt(kv[1], 16);
                case "haze" -> haze = Float.parseFloat(kv[1]);
                case "air" -> air = Integer.parseInt(kv[1], 16);
                case "key" -> key = Integer.parseInt(kv[1], 16);
                case "shadow" -> shadow = Integer.parseInt(kv[1], 16);
                default -> throw new IllegalArgumentException("unknown ground key " + kv[0]);
            }
        }
        engine.loadLevel(level.withGround(td.board.BoardGround.of(floor, ground.motif(), motif)
                .withLight(td.board.BoardLight.of(key, shadow, air)).withHaze(haze)));
        gameTime = 0;
        System.out.println("OK ground " + args);
    }

    private static void setCrop(String args) {
        if (args.equals("off")) {
            crop = null;
            System.out.println("OK crop off");
            return;
        }
        String[] parts = args.split("\\s+");
        crop = new int[] {Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3])};
        System.out.println("OK crop " + args + " (board pixels)");
    }

    private static BoardRenderer renderer() {
        BoardRenderer boardRenderer = new BoardRenderer(engine.getGameWorld(), engine::cellHighlight);
        boardRenderer.setCellGridShown(cellGrid);
        return boardRenderer;
    }

    /** The frame at {@code tick} and {@code alpha}, at the preview's zoom and crop, squinted if asked. */
    private static BufferedImage paint(BoardRenderer boardRenderer, int tick, double alpha) {
        double animationSeconds = (tick - 1 + alpha) / TickRate.TICKS_PER_SECOND;
        RenderFrame frame = boardRenderer.buildFrame(tick, alpha, animationSeconds);
        int x = crop == null ? 0 : crop[0];
        int y = crop == null ? 0 : crop[1];
        int w = crop == null ? frame.maxX() : crop[2];
        int h = crop == null ? frame.maxY() : crop[3];
        BufferedImage image = new BufferedImage((int) Math.ceil(w * zoom), (int) Math.ceil(h * zoom),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.scale(zoom, zoom);
        g2.translate(-x, -y);
        new Java2DFrameRenderer().paint(g2, frame);
        g2.dispose();
        return squint ? Film.squint(image, zoom) : image;
    }

    private static void state() {
        GameWorld world = engine.getGameWorld();
        System.out.println("gameTime=" + gameTime
                + " wave=" + engine.getCurrentWaveIndex() + "/" + engine.getWaveCount()
                + " waveReady=" + engine.isWaveReady()
                + " credits=" + world.economy().getCredits()
                + " lives=" + world.economy().getLives()
                + " score=" + world.economy().getScore()
                + " enemiesAlive=" + world.enemies().aliveCount()
                + " towers=" + world.towers().all().size()
                + " projectiles=" + world.projectiles().getProjectiles().size());
    }
}
