package td.wave;

import td.cell.Cell;

public class PathEmpty implements Path {

    int x = 0;
    int y = 0;

    @Override
    public void addStep(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int length() {
        return 1;
    }

    @Override
    public Point getStep(int step) {
        return new Point(x, y);
    }

    @Override
    public void finalise(Cell[][] grid) {
    }

}
