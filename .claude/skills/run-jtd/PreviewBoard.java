import td.GameEngine;
import td.cell.CellGrid;
import td.damage.Damage;
import td.effect.Effect;
import td.effect.EffectKind;
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

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

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
 * {@code td.BalanceHarness} already proved this shape (headless {@code GameEngine} + real tower
 * placement) for balance simulation; this reuses the same recipe, adding {@code spawn} for exact
 * enemy ids/ranks and a {@code render} step through the actual painting pipeline instead of a
 * text report.
 */
public class PreviewBoard {

    private static GameEngine engine;
    private static int gameTime = 0;
    private static boolean cellGrid = false;

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
            case "effect" -> applyEffect(rest.trim());
            case "wave" -> startWave();
            case "tick" -> tick(Integer.parseInt(rest.trim()));
            case "kill" -> killEnemies();
            case "render" -> render(rest.trim());
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

    private static void loadLevel(int index) {
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
    // centre), the same path BalanceHarness.placeLoadout and TowerDefense's own mouse listener
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

    /** {@code effect <kind> [ticks]}: puts the effect on every enemy, as a tower would. */
    private static void applyEffect(String args) {
        String[] parts = args.split("\s+");
        EffectKind kind = EffectKind.valueOf(parts[0].toUpperCase());
        int ticks = parts.length > 1 ? Integer.parseInt(parts[1]) : 200;
        for (EnemyMob mob : engine.getGameWorld().enemies().getEnemies()) {
            Effect effect = switch (kind) {
                case CHILL -> Effect.chill(0.5f, ticks, d -> { });
                case BURN -> Effect.burn(Damage.magic(1), ticks, d -> { });
                case POISON -> Effect.poison(Damage.magic(1), ticks, d -> { });
                case FREEZE -> Effect.freeze(ticks, d -> { });
                case SHIELD -> Effect.shield(0.3f, ticks, d -> { });
                case INVISIBLE -> Effect.invisible(ticks, d -> { });
                case HEAL -> Effect.heal(1, ticks, d -> { });
                case VULNERABLE -> Effect.vulnerable(2, ticks, d -> { });
                case REVEALED -> Effect.revealed(ticks, d -> { });
                case SUNDERED -> Effect.sundered(3, ticks, d -> { });
                case EXPOSED -> Effect.exposed(ticks, d -> { });
                case MARKED -> Effect.marked(ticks, d -> { });
                case PRIORITY -> Effect.priority(ticks, d -> { });
                case RESONATING -> Effect.resonating(2, ticks, d -> { });
                case FRACTURED -> Effect.fractured(3, d -> { });
                case DAZED -> Effect.dazed(ticks, d -> { });
                case SATURATED -> Effect.saturated(3, ticks, d -> { });
                case CHARGED -> Effect.charged(ticks, td.damage.AttackOrigin.none(), d -> { });
                case DOOM, BLIGHT, CONTAGION -> Effect.hex(kind, ticks, d -> { });
                case SCORCHED, SICKENED -> throw new IllegalArgumentException(kind + " is earned by burn and poison");
            };
            mob.applyEffect(effect);
        }
        System.out.println("OK effect " + kind);
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
     * Buys the named node for the tower at a cell through the real mechanism, so its gate must be
     * cleared first (tick a fight); a refusal prints why.
     */
    private static void upgradeTower(String args) {
        String[] parts = args.split("\s+", 3);
        Tower tower = engine.cells().at(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])).getTower();
        String name = parts[2];
        UpgradeNode node = tower.upgradeTree().nodes().stream().filter(n -> n.displayName().equals(name))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("no node named " + name));
        boolean offered = tower.offeredUpgrades(engine.getGameWorld()).contains(node);
        if (tower.buyUpgrade(node)) {
            System.out.println("OK upgrade " + name);
        } else {
            System.out.println("FAILED upgrade " + name + ": " + (offered
                    ? node.xpProgress(tower) + ", " + node.gate().progress(tower, engine.getGameWorld())
                            + ", credits " + engine.getGameWorld().economy().getCredits()
                    : "not offered"));
        }
    }

    // Starts placing a tower and points at a cell, so the render shows the placement highlight.
    private static void hoverPlace(String args) {
        String[] parts = args.split("\s+");
        int scale = engine.getGameWorld().getBoard().scale();
        engine.startPlacing(td.tower.TowerFactory.Type.valueOf(parts[0].toUpperCase()), 0f);
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
        GameWorld world = engine.getGameWorld();
        BoardRenderer boardRenderer = new BoardRenderer(world);
        boardRenderer.setCellGridShown(cellGrid);
        double animationSeconds = gameTime / TickRate.TICKS_PER_SECOND;
        // alpha 1.0: this is always a snapshot exactly at gameTime (the last doTick that ran),
        // never mid-interpolation into a tick that hasn't happened.
        RenderFrame frame = boardRenderer.buildFrame(gameTime, 1.0, animationSeconds);

        BufferedImage image = new BufferedImage(frame.maxX(), frame.maxY(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        new Java2DFrameRenderer().paint(g2, frame);
        g2.dispose();

        File outFile = new File(path);
        File parent = outFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        ImageIO.write(image, "png", outFile);
        System.out.println("OK wrote " + path + " (" + frame.maxX() + "x" + frame.maxY() + ")");
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
