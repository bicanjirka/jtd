package td.wave;

import td.cell.Cell;

public interface Path {

    void addStep(double x, double y);

    int length();

    Vec2 getStep(int step);

    void finalise(Cell[][] grid);

}
