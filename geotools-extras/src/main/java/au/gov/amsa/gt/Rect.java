package au.gov.amsa.gt;

public class Rect {

    private final double minX;
    private final double minY;
    private final double maxX;
    private final double maxY;

    public Rect(double minX, double minY, double maxX, double maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public double minX() {
        return minX;
    }

    public double minY() {
        return minY;
    }

    public double maxX() {
        return maxX;
    }

    public double maxY() {
        return maxY;
    }

    public Rect add(Rect r) {
        return new Rect(Math.min(minX, r.minX), //
                Math.min(minY, r.minY), //
                Math.max(maxX, r.maxX), //
                Math.max(maxY, r.maxY) //
        );
    }

    @Override
    public String toString() {
        return "Rect [minX=" + minX + ", minY=" + minY + ", maxX=" + maxX + ", maxY=" + maxY + "]";
    }

}
