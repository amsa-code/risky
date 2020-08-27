package au.gov.amsa.geo;

public final class TimedPosition {
    public final double lat;
    public final double lon;
    public final long time;

    public TimedPosition(double lat, double lon, long time) {
        this.lat = lat;
        this.lon = lon;
        this.time = time;
    }
}