package au.gov.amsa.ais.router.model;

public final class Rectangle implements Region {
    private final float minLat;
    private final float maxLat;
    private final float minLon;
    private final float maxLon;

    private Rectangle(float minLat, float maxLat, float minLon, float maxLon) {
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
    }

    public float minLat() {
        return minLat;
    }

    public float maxLat() {
        return maxLat;
    }

    public float minLon() {
        return minLon;
    }

    public float maxLon() {
        return maxLon;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private float minLat;
        private float maxLat;
        private float minLon;
        private float maxLon;

        private Builder() {
        }

        public Builder minLat(float minLat) {
            this.minLat = minLat;
            return this;
        }

        public Builder maxLat(float maxLat) {
            this.maxLat = maxLat;
            return this;
        }

        public Builder minLon(float minLon) {
            this.minLon = minLon;
            return this;
        }

        public Builder maxLon(float maxLon) {
            this.maxLon = maxLon;
            return this;
        }

        public Rectangle build() {
            return new Rectangle(minLat, maxLat, minLon, maxLon);
        }
    }

    @Override
    public boolean contains(float lat, float lon) {
        return minLat <= lat && maxLat >= lat && minLon <= lon && maxLon >= lon;
    }

}
