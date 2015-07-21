package au.gov.amsa.geo.model;

import au.gov.amsa.risky.format.HasFix;
import au.gov.amsa.util.navigation.Position;

/**
 * Geographic bounds using a latitude, longitude rectangle.
 */
public class Bounds {

    private final double topLeftLat, topLeftLon, bottomRightLat, bottomRightLon;

    public Bounds(double topLeftLat, double topLeftLon, double bottomRightLat, double bottomRightLon) {
        this.topLeftLat = topLeftLat;
        this.topLeftLon = Position.to180(topLeftLon);
        this.bottomRightLat = bottomRightLat;
        this.bottomRightLon = Position.to180(bottomRightLon);
    }

    public double getTopLeftLat() {
        return topLeftLat;
    }

    public double getTopLeftLon() {
        return topLeftLon;
    }

    public double getBottomRightLat() {
        return bottomRightLat;
    }

    public double getBottomRightLon() {
        return bottomRightLon;
    }

    public double getWidthDegrees() {
        if (bottomRightLon < topLeftLon)
            return bottomRightLon + 360 - topLeftLon;
        else
            return bottomRightLon - topLeftLon;
    }

    public double getHeightDegrees() {
        return topLeftLat - bottomRightLat;
    }

    public boolean contains(HasFix p) {
        return contains(p.fix().lat(), p.fix().lon());
    }

    public boolean contains(double lat, double lon) {
        return topLeftLat >= lat && bottomRightLat <= lat
                && betweenLongitudes(topLeftLon, bottomRightLon, Position.to180(lon));
    }

    /**
     * Returns true if and only if lon is between the longitudes topLeftLon and
     * bottomRightLon.
     * 
     * @param topLeftLon
     *            must be between -180 and 180
     * @param bottomRightLon
     *            must be between -180 and 180
     * @param lon
     *            must be between -180 and 180
     * @return
     */
    private static boolean betweenLongitudes(double topLeftLon, double bottomRightLon, double lon) {
        if (topLeftLon <= bottomRightLon)
            return lon >= topLeftLon && lon <= bottomRightLon;
        else
            return lon >= topLeftLon || lon <= bottomRightLon;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Bounds [topLeftLat=");
        builder.append(topLeftLat);
        builder.append(", topLeftLon=");
        builder.append(topLeftLon);
        builder.append(", bottomRightLat=");
        builder.append(bottomRightLat);
        builder.append(", bottomRightLon=");
        builder.append(bottomRightLon);
        builder.append("]");
        return builder.toString();
    }

    public Bounds expand(double latExpansionDegrees, double lonExpansionDegrees) {
        return new Bounds(Math.min(90, topLeftLat + latExpansionDegrees), topLeftLon
                - lonExpansionDegrees, Math.max(-90, bottomRightLat - latExpansionDegrees),
                bottomRightLon + lonExpansionDegrees);
    }

}
