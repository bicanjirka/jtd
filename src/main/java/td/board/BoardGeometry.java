package td.board;

/**
 * A level's pixel scale and cell dimensions, and the cell-to-pixel math. {@link #empty()} is the
 * value before any level loads.
 */
public record BoardGeometry(int scale, int widthCells, int heightCells) {

    private static final BoardGeometry EMPTY = new BoardGeometry(32, 0, 0);

    public static BoardGeometry empty() {
        return EMPTY;
    }

    public static BoardGeometry of(int scale, int widthCells, int heightCells) {
        return new BoardGeometry(scale, widthCells, heightCells);
    }

    public int maxX() {
        return this.pixelWidth() - 1;
    }

    public int maxY() {
        return this.pixelHeight() - 1;
    }

    public int pixelWidth() {
        return this.widthCells * this.scale;
    }

    public int pixelHeight() {
        return this.heightCells * this.scale;
    }

    public int cellX(int pixelX) {
        return pixelX / this.scale;
    }

    public int cellY(int pixelY) {
        return pixelY / this.scale;
    }

    public boolean containsPixel(int pixelX, int pixelY) {
        return pixelX >= 0 && pixelX < this.pixelWidth() && pixelY >= 0 && pixelY < this.pixelHeight();
    }
}
