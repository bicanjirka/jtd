package td.wave;

import td.cell.Cell;

import java.util.ArrayList;
import java.util.List;

public class PathNormal implements Path {

    private int[] stepsX = new int[0];
    private int[] stepsY = new int[0];
    private final List<Integer> stepXv;
    private final List<Integer> stepYv;
    private final int scale;

    public PathNormal(int scale) {
        this.scale = scale;
        this.stepXv = new ArrayList<>();
        this.stepYv = new ArrayList<>();
    }

    public void addStep(int x, int y) {
        this.stepXv.add(x);
        this.stepYv.add(y);
    }

    public int length() {
        return this.stepsX.length;
    }

    public Point getStep(int step) {
        if (this.stepsX.length == 0) {
            return new Point(0, 0);
        }
        if (step < 0) {
            return this.getStep(0);
        }
        if (step >= this.stepsX.length) {
            return this.getStep(this.stepsX.length - 1);
        }
        int x = stepsX[step];
        int y = stepsY[step];
        return new Point(x * scale + (scale / 2), y * scale + (scale / 2));
    }

    public void finalise(Cell[][] grid) {
        this.stepsX = new int[this.stepXv.size()];
        this.stepsY = new int[this.stepYv.size()];
        for (int i = 0; i < this.stepsX.length; i++) {
            this.stepsX[i] = this.stepXv.get(i);
            this.stepsY[i] = this.stepYv.get(i);
            if (this.stepsX[i] >= 0 && this.stepsX[i] < grid.length)
                if (this.stepsY[i] >= 0 && this.stepsY[i] < grid[0].length)
                    grid[this.stepsX[i]][this.stepsY[i]].enable(false);
        }
    }
}
