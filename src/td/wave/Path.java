package td.wave;

import td.cell.Cell;

public interface Path {
	
	public void addStep(int x, int y);
	public int length();
	public int[] getStep(int step);
	public void finalise(Cell[][] grid);

}
