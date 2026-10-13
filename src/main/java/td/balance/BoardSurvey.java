package td.balance;

import td.GameEngine;
import td.cell.CellGrid;
import td.level.LevelDefinition;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameHost;
import td.util.PathRuntime;
import td.util.RandomSource;
import td.wave.ArcLengthPath;
import td.wave.Point;
import td.wave.Vec2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A level's board as a player sizes it up before building: the buildable cells, the paths sampled along their
 * length, each tower type's reach, and how much path each cell would put in that reach. Immutable once built,
 * so every play of the level shares one.
 */
public final class BoardSurvey {

    /** Path samples this many pixels apart. */
    private static final double SAMPLE_PIXELS = 4.0;

    private final LevelDefinition level;
    private final int scale;
    private final List<Point> buildable;
    private final List<List<Vec2>> samples;
    private final Map<TowerFactory.Type, Float> reach;
    private final Map<TowerFactory.Type, Map<Point, Double>> coverage;

    private BoardSurvey(LevelDefinition level, int scale, List<Point> buildable, List<List<Vec2>> samples,
                        Map<TowerFactory.Type, Float> reach) {
        this.level = level;
        this.scale = scale;
        this.buildable = List.copyOf(buildable);
        this.samples = samples.stream().map(List::copyOf).toList();
        this.reach = Collections.unmodifiableMap(new EnumMap<>(reach));
        Map<TowerFactory.Type, Map<Point, Double>> byType = new EnumMap<>(TowerFactory.Type.class);
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            Map<Point, Double> cells = new LinkedHashMap<>();
            for (Point cell : this.buildable) {
                cells.put(cell, this.pathWithin(cell, this.reach.get(type)));
            }
            byType.put(type, Collections.unmodifiableMap(cells));
        }
        this.coverage = Collections.unmodifiableMap(byType);
    }

    /** Surveys {@code level} on a throwaway engine. */
    public static BoardSurvey of(LevelDefinition level) {
        GameEngine engine = new GameEngine(GameHost.noOp(), RandomSource.seeded(0));
        engine.loadLevel(level);
        int scale = engine.getGameWorld().getBoard().scale();
        CellGrid grid = engine.cells();
        List<Point> buildable = new ArrayList<>();
        for (int x = 0; x < grid.width(); x++) {
            for (int y = 0; y < grid.height(); y++) {
                if (grid.at(x, y).buildable()) {
                    buildable.add(new Point(x, y));
                }
            }
        }
        List<List<Vec2>> samples = new ArrayList<>();
        for (PathRuntime runtime : engine.getGameWorld().level().paths()) {
            samples.add(sample(runtime, engine.getGameWorld().getBoard().pixelWidth(),
                    engine.getGameWorld().getBoard().pixelHeight()));
        }
        engine.debugGrantCredits(1_000_000);
        Map<TowerFactory.Type, Float> reach = new EnumMap<>(TowerFactory.Type.class);
        int next = 0;
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            Point cell = buildable.get(next++);
            Tower tower = Turn.place(engine, type, cell).orElseThrow();
            reach.put(type, tower.getRangeReal());
        }
        return new BoardSurvey(level, scale, buildable, samples, reach);
    }

    private static List<Vec2> sample(PathRuntime runtime, int width, int height) {
        List<Vec2> points = new ArrayList<>();
        ArcLengthPath.of(runtime.path()).ifPresent(path -> {
            for (double d = 0; d <= path.totalLength(); d += SAMPLE_PIXELS) {
                Vec2 at = path.poseAt(d).position();
                if (at.x() >= 0 && at.y() >= 0 && at.x() < width && at.y() < height) {
                    points.add(at);
                }
            }
        });
        return points;
    }

    public LevelDefinition level() {
        return this.level;
    }

    /** Pixels per cell. */
    public int scale() {
        return this.scale;
    }

    /** Every cell a tower may stand on before anything is built. */
    public List<Point> buildable() {
        return this.buildable;
    }

    /** How many paths the level has. */
    public int pathCount() {
        return this.samples.size();
    }

    /** {@code type}'s reach in pixels, unbuffed. */
    public float reach(TowerFactory.Type type) {
        return this.reach.get(type);
    }

    /** Pixels of path, every path counted, within {@code type}'s reach of {@code cell}. */
    public double coverage(TowerFactory.Type type, Point cell) {
        return this.coverage.get(type).getOrDefault(cell, 0.0);
    }

    /** Pixels of path {@code pathIndex} within {@code radius} pixels of {@code cell}'s centre. */
    public double pathWithin(Point cell, double radius, int pathIndex) {
        Vec2 centre = this.centre(cell);
        double r2 = radius * radius;
        long near = this.samples.get(pathIndex).stream()
                .filter(p -> square(p.x() - centre.x()) + square(p.y() - centre.y()) <= r2).count();
        return near * SAMPLE_PIXELS;
    }

    private double pathWithin(Point cell, double radius) {
        double total = 0;
        for (int i = 0; i < this.samples.size(); i++) {
            total += this.pathWithin(cell, radius, i);
        }
        return total;
    }

    /** A cell's centre in board pixels. */
    public Vec2 centre(Point cell) {
        return new Vec2(cell.x() * this.scale + this.scale / 2.0, cell.y() * this.scale + this.scale / 2.0);
    }

    /** The on-board stretch of path {@code pathIndex}, sampled along its length. */
    public List<Vec2> samples(int pathIndex) {
        return this.samples.get(pathIndex);
    }

    private static double square(double v) {
        return v * v;
    }
}
