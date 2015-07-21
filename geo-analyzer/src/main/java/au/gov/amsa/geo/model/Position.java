package au.gov.amsa.geo.model;

import java.io.Serializable;

import au.gov.amsa.risky.format.HasPosition;

public class Position implements HasPosition, Serializable {

    private static final long serialVersionUID = 4470547471465912154L;

    private final float lat, lon;

    private final int hashCode;

    public Position(float lat, float lon) {
        this.lat = lat;
        this.lon = lon;
        this.hashCode = calculateHashCode();
    }

    public Position(double lat, double lon) {
        this((float) lat, (float) lon);
    }

    @Override
    public float lat() {
        return lat;
    }

    @Override
    public float lon() {
        return lon;
    }

    @Override
    public String toString() {
        return "Position [lat=" + lat + ", lon=" + lon + "]";
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private int calculateHashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(lat);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lon);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Position other = (Position) obj;
        if (Double.doubleToLongBits(lat) != Double.doubleToLongBits(other.lat))
            return false;
        if (Double.doubleToLongBits(lon) != Double.doubleToLongBits(other.lon))
            return false;
        return true;
    }

}
