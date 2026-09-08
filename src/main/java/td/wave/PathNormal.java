package td.wave;

import td.cell.Cell;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PathNormal implements Path {

    private double[] xs = new double[0];
    private double[] ys = new double[0];
    private final List<Double> xv;
    private final List<Double> yv;
    private final int scale;

    public PathNormal(int scale) {
        this.scale = scale;
        this.xv = new ArrayList<>();
        this.yv = new ArrayList<>();
    }

    public void addStep(double x, double y) {
        this.xv.add(x);
        this.yv.add(y);
    }

    public int length() {
        return this.xs.length;
    }

    public Vec2 getStep(int step) {
        if (this.xs.length == 0) {
            return new Vec2(0, 0);
        }
        if (step < 0) {
            return this.getStep(0);
        }
        if (step >= this.xs.length) {
            return this.getStep(this.xs.length - 1);
        }
        return new Vec2(this.xs[step], this.ys[step]);
    }

    public void finalise(Cell[][] grid) {
        this.xs = new double[this.xv.size()];
        this.ys = new double[this.yv.size()];
        List<Vec2> polyline = new ArrayList<>(this.xs.length);
        for (int i = 0; i < this.xs.length; i++) {
            this.xs[i] = this.xv.get(i);
            this.ys[i] = this.yv.get(i);
            polyline.add(new Vec2(this.xs[i], this.ys[i]));
        }
        int width = grid.length;
        int height = width == 0 ? 0 : grid[0].length;
        Set<Point> covered = PathCoverage.unbuildableCells(polyline, this.scale, width, height);
        for (Point cell : covered) {
            grid[cell.x()][cell.y()].enable(false);
        }
    }
}
