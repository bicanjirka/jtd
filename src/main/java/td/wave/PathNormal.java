package td.wave;

import java.util.ArrayList;
import java.util.List;

import td.cell.Cell;

/**
 * Trida udavajici cestu mobum
 * @author Juras
 *
 */
public class PathNormal implements Path {
    
	private int[] stepsX = new int[0];
    private int[] stepsY = new int[0];
    private List<Integer> stepXv;
    private List<Integer> stepYv;
    private int scale;
    /**
     * Konstruktor
     * @param scale - rozliseni herniho policka
     */
    public PathNormal(int scale) {
    	this.scale = scale;
    	this.stepXv = new ArrayList<Integer>();
        this.stepYv = new ArrayList<Integer>();
    }
    /**
     * Prida dalsi krok do cesty
     * @param x - souradnice kroku
     * @param y - souradnice kroku
     */
    public void addStep(int x, int y) {
        this.stepXv.add(x);
        this.stepYv.add(y);
    }
    /**
     * Delka cesty
     * @return - delka cesty
     */
    public int length() {
        return this.stepsX.length;
    }
    /**
     * Na zaklade kroku vrati souradnice kroku na ceste<br>
     * V pripade kroku <0 vraci souradnice prvniho<br>
     * V pripade kroku vetsiho nez delka cesty vraci posledni
     * @param step - krok
     * @return - souradnice {x,y}
     */
    public Point getStep(int step) {
        if (this.stepsX.length == 0) {
            return new Point(0, 0);
        }
        if (step < 0) {
            return this.getStep(0);
        }
        if (step >= this.stepsX.length) {
            return this.getStep(this.stepsX.length-1);
        }
        int x = stepsX[step];
        int y = stepsY[step];
        return new Point(x * scale + (scale/2), y * scale + (scale/2));
    }
    /**
     * Ukonci nacitani cesty a prevede nasbirane souradnice
     * z vektoru do poli<br>
     * Nastavi hernim polickum lezicim na ceste,
     * ze se na nich neda stavet
     * @param grid - sit hernich policek
     */
    public void finalise(Cell[][] grid) {
    	this.stepsX = new int[this.stepXv.size()];
    	this.stepsY = new int[this.stepYv.size()];
        for (int i=0; i<this.stepsX.length; i++) {
            this.stepsX[i] = this.stepXv.get(i);
            this.stepsY[i] = this.stepYv.get(i);
			if(this.stepsX[i]>=0 && this.stepsX[i]<grid.length)
				if(this.stepsY[i]>=0 && this.stepsY[i]<grid[0].length)
					grid[this.stepsX[i]][this.stepsY[i]].enable(false);
        }
    }
}
