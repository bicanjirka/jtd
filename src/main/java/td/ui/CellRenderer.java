package td.ui;

import td.cell.Cell;
import td.util.Context;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

/**
 * Draws a cell's placement/selection highlight. Only one {@link Cell}
 * implementation exists, so unlike towers/enemies this needs no visitor -
 * just plain-data getters read here.
 */
public final class CellRenderer {

    private static final Color OK = Color.GRAY;
    private static final Color NOK = Color.RED;
    private static final Color RANGE_COLOR = new Color(250, 250, 210, 150);

    private final Graphics2D g2;
    private final Context context;

    public CellRenderer(Graphics2D g2, Context context) {
        this.g2 = g2;
        this.context = context;
    }

    public void paint(Cell cell) {
        switch (cell.getHighlight()) {
            case place -> this.paintPlaceHighlight(cell);
            case select -> this.paintSelectHighlight(cell);
            case none -> {
            }
        }
    }

    private void paintPlaceHighlight(Cell cell) {
        int scale = this.context.scale;
        int x = cell.getX();
        int y = cell.getY();
        Color highlightColor = cell.buildable() ? OK : NOK;

        this.paintRangeCircle(cell);

        Color tempColor = new Color(highlightColor.getRed(), highlightColor.getGreen(), highlightColor.getBlue(), 80);
        this.g2.setColor(tempColor);
        this.g2.drawLine(x, 0, x, this.context.maxY);
        this.g2.drawLine(x + scale, 0, x + scale, this.context.maxY);
        this.g2.drawLine(0, y, this.context.maxX, y);
        this.g2.drawLine(0, y + scale, this.context.maxX, y + scale);
        this.g2.fillRect(x, y, scale, scale);

        tempColor = new Color(highlightColor.getRed(), highlightColor.getGreen(), highlightColor.getBlue(), 140);
        this.g2.setColor(tempColor);
        this.g2.drawRoundRect(x - scale, y - scale, scale * 3, scale * 3, 40, 40);

        this.g2.setColor(highlightColor);
        this.g2.drawRect(x, y, scale, scale);
    }

    private void paintRangeCircle(Cell cell) {
        int scale = this.context.scale;
        float realRange = cell.getHighlightRange() * scale;
        float halfScale = (float) scale / 2;
        int x = cell.getX();
        int y = cell.getY();
        this.g2.setColor(RANGE_COLOR);
        this.g2.draw(new Ellipse2D.Float(x - realRange + halfScale, y - realRange + halfScale, realRange * 2, realRange * 2));
    }

    private void paintSelectHighlight(Cell cell) {
        int scale = this.context.scale;
        int x = cell.getX();
        int y = cell.getY();
        Color tempColor = new Color(OK.getRed(), OK.getGreen(), OK.getBlue(), 80);
        this.g2.setColor(tempColor);
        this.g2.fillRect(x, y, scale, scale);
        this.g2.setColor(OK);
        this.g2.drawRect(x - 1, y - 1, scale + 1, scale + 1);
    }
}
