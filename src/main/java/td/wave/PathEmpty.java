package td.wave;

import td.cell.Cell;

public class PathEmpty implements Path {

    double x = 0;
    double y = 0;

    @Override
    public void addStep(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int length() {
        return 1;
    }

    @Override
    public Vec2 getStep(int step) {
        return new Vec2(x, y);
    }

    @Override
    public void finalise(Cell[][] grid) {
    }

}
