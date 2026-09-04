package td.wave;

import java.util.Vector;

import td.cell.Cell;

/**
 * Trida udavajici cestu mobum
 * @author Juras
 *
 */
public class PathNormal implements Path {
    
	private int[] stepsX;
    private int[] stepsY;
    private Vector<Integer> stepXv;
    private Vector<Integer> stepYv;
    private int scale;
    /**
     * Konstruktor
     * @param scale - rozliseni herniho policka
     */
    public PathNormal(int scale) {
    	this.scale = scale;
    	this.stepsX = null;
    	this.stepXv = new Vector<Integer>();
        this.stepYv = new Vector<Integer>();
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
    public int[] getStep(int step) {
        if (this.stepsX.equals(null)) {
        	int[] retval = {0,0};
            return retval;
        } else {
            if (step < 0) {
            	int[] retVal = new int[2];
            	retVal = this.getStep(0);
            	return retVal;
            }
            if (step >= this.stepsX.length) {
            	int[] retVal = new int[2];
            	retVal = this.getStep(this.stepsX.length-1);
            	return retVal;
            }
            int[] returnVals = new int[2];
            int x = stepsX[step];
            int y = stepsY[step];
            returnVals[0] = x * scale + (scale/2);
            returnVals[1] = y * scale + (scale/2);
            return returnVals;
        }
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
