package td.cell;

import td.tower.Tower;
import td.util.Context;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Obycejne herni policko
 *
 * @author Juras
 *
 */
public class CellNormal implements Cell {

    private final Context context;
    private highlightType highlight = highlightType.none;
    private float highlightRange = 0;
    private Shape rangeCircle;

    private final int scale;
    private final int x;
    private final int y;

    private boolean buildable = true;
    private Tower tower = null;

    private final Color highlightOK = Color.GRAY;
    private final Color highlightNOK = Color.RED;
    private Color highlightColor;
    private final Color rangeColor = new Color(250, 250, 210, 150);

    /**
     * Vytvori nove herni policko
     *
     * @param x       - souradnice x herniho policka
     * @param y       - souradnice y herniho policka
     * @param context - herni kontext
     */
    public CellNormal(int x, int y, Context context) {
        this.x = x;
        this.y = y;
        this.context = context;
        this.scale = this.context.scale;
        this.highlightColor = this.highlightOK;
    }

    public boolean hasTower() {
        return (this.tower != null);
    }

    public void unSetTower() {
        if (this.tower != null) {
            this.tower = null;
            this.buildable = true;
        }
        this.highlight = highlightType.none;
    }

    public Tower getTower() {
        return this.tower;
    }

    public void setTower(Tower tower) {
        if (this.buildable) {
            this.tower = tower;
            this.buildable = false;
        }
    }

    public boolean buildable() {
        return this.buildable;
    }

    public void setHighlight(highlightType highlight) {
        this.highlight = highlight;
    }

    public void setHighlightRange(float range) {
        this.highlightRange = range;
        float realRange = this.highlightRange * this.context.scale;
        float halfScale = this.context.scale / 2;
        this.rangeCircle = new Ellipse2D.Float(this.x - realRange + halfScale, this.y - realRange + halfScale, realRange * 2, realRange * 2);
    }

    /**
     * Vykresli kruznici udavajici dostel veze
     *
     * @param g2 - grafika
     */
    private void paintRangeCircle(Graphics2D g2) {
        g2.setColor(this.rangeColor);
        g2.draw(this.rangeCircle);
    }

    public void paintEffect(Graphics2D g2) {
        switch (this.highlight) {
            case place -> {
                this.paintRangeCircle(g2);
                Color tempColor;
                tempColor = new Color(this.highlightColor.getRed(), this.highlightColor.getGreen(), this.highlightColor.getBlue(), 80);
                g2.setColor(tempColor);
                //horizontalni a vertikalni linky
                g2.drawLine(this.x, 0, this.x, this.context.maxY);
                g2.drawLine(this.x + this.scale, 0, this.x + this.scale, this.context.maxY);
                g2.drawLine(0, this.y, this.context.maxX, this.y);
                g2.drawLine(0, this.y + this.scale, this.context.maxX, y + this.scale);
                //vyplneni herniho policka
                g2.fillRect(this.x, this.y, this.scale, this.scale);

                tempColor = new Color(this.highlightColor.getRed(), this.highlightColor.getGreen(), this.highlightColor.getBlue(), 140);
                g2.setColor(tempColor);
                //zaobleny ctverec
                g2.drawRoundRect(this.x - this.scale, this.y - this.scale, this.scale * 3, this.scale * 3, 40, 40);

                g2.setColor(this.highlightColor);
                //obtazeni policka
                g2.drawRect(this.x, this.y, this.scale, this.scale);
            }
            case select -> {
                Color tempColor2;
                tempColor2 = new Color(this.highlightOK.getRed(), this.highlightOK.getGreen(), this.highlightOK.getBlue(), 80);
                g2.setColor(tempColor2);
                //vyplneni policka
                g2.fillRect(this.x, this.y, this.scale, this.scale);
                g2.setColor(this.highlightOK);
                //obtazeni policka
                g2.drawRect(this.x - 1, this.y - 1, this.scale + 1, this.scale + 1);
            }
            default -> {
            }
        }
    }

    public void enable(boolean b) {
        this.buildable = b;
        this.highlightColor = b ? this.highlightOK : this.highlightNOK;
    }

}
