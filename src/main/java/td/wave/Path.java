package td.wave;

import td.cell.Cell;

public interface Path {

    void addStep(int x, int y);

    int length();

    Point getStep(int step);

    void finalise(Cell[][] grid);

}
