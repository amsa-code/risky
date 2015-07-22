package au.gov.amsa.geo.model;

import java.util.concurrent.TimeUnit;

public class SegmentOptions {
    private final Long acceptAnyFixAfterHours;
    private final double speedCheckDistanceThresholdNm;
    private final long speedCheckMinTimeDiffMs;
    private final double maxSpeedKnots;
    private final Double maxDistancePerSegmentNm;

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("SegmentOptions [acceptAnyFixAfterHours=");
        b.append(acceptAnyFixAfterHours);
        b.append(", speedCheckDistanceThresholdNm=");
        b.append(speedCheckDistanceThresholdNm);
        b.append(", speedCheckMinTimeDiffMs=");
        b.append(speedCheckMinTimeDiffMs);
        b.append(", maxSpeedKnots=");
        b.append(maxSpeedKnots);
        b.append(", maxDistancePerSegmentNm=");
        b.append(maxDistancePerSegmentNm);
        b.append(", maxTimePerSegmentHours=");
        b.append((double) maxTimePerSegmentMs / TimeUnit.HOURS.toMillis(1));
        b.append("]");
        return b.toString();
    }

    private final Long maxTimePerSegmentMs;

    private SegmentOptions(Long acceptAnyFixAfterHours, double speedCheckDistanceThresholdNm,
            long speedCheckMinTimeDiffMs, double maxSpeedKnots, Double maxDistancePerSegmentNm,
            Long maxTimePerSegmentMs) {
        this.acceptAnyFixAfterHours = acceptAnyFixAfterHours;
        this.speedCheckDistanceThresholdNm = speedCheckDistanceThresholdNm;
        this.speedCheckMinTimeDiffMs = speedCheckMinTimeDiffMs;
        this.maxSpeedKnots = maxSpeedKnots;
        this.maxDistancePerSegmentNm = maxDistancePerSegmentNm;
        this.maxTimePerSegmentMs = maxTimePerSegmentMs;
    }

    public Long acceptAnyFixAfterHours() {
        return acceptAnyFixAfterHours;
    }

    public double speedCheckDistanceThresholdNm() {
        return speedCheckDistanceThresholdNm;
    }

    public long speedCheckMinTimeDiffMs() {
        return speedCheckMinTimeDiffMs;
    }

    public double maxSpeedKnots() {
        return maxSpeedKnots;
    }

    public Double maxDistancePerSegmentNm() {
        return maxDistancePerSegmentNm;
    }

    public Long maxTimePerSegmentMs() {
        return maxTimePerSegmentMs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static SegmentOptions getDefault() {
        return SegmentOptions.builder().build();
    }

    public static class Builder {

        private Long acceptAnyFixHours = null;// 24
        private double speedCheckDistanceThresholdNm = 30;
        private long speedCheckMinTimeDiffMs = 180000;
        private double maxSpeedKnots = 50;
        private Double maxDistancePerSegmentNm = null;// 1000.0;
        private Long maxTimePerSegmentMs = TimeUnit.DAYS.toMillis(1);

        private Builder() {
        }

        public Builder acceptAnyFixHours(Long acceptAnyFixHours) {
            this.acceptAnyFixHours = acceptAnyFixHours;
            return this;
        }

        public Builder speedCheckDistanceThresholdNm(double speedCheckDistanceThresholdNm) {
            this.speedCheckDistanceThresholdNm = speedCheckDistanceThresholdNm;
            return this;
        }

        public Builder speedCheckMinTimeDiffMs(long speedCheckMinTimeDiffMs) {
            this.speedCheckMinTimeDiffMs = speedCheckMinTimeDiffMs;
            return this;
        }

        public Builder speedCheckMinTimeDiff(long duration, TimeUnit unit) {
            return speedCheckMinTimeDiffMs(unit.toMillis(duration));
        }

        public Builder maxSpeedKnots(double maxSpeedKnots) {
            this.maxSpeedKnots = maxSpeedKnots;
            return this;
        }

        public Builder maxDistancePerSegmentNm(Double maxDistancePerSegmentNm) {
            this.maxDistancePerSegmentNm = maxDistancePerSegmentNm;
            return this;
        }

        public Builder maxTimePerSegmentMs(Long maxTimePerSegmentMs) {
            this.maxTimePerSegmentMs = maxTimePerSegmentMs;
            return this;
        }

        public Builder maxTimePerSegment(long duration, TimeUnit unit) {
            return maxTimePerSegmentMs(unit.toMillis(duration));
        }

        public SegmentOptions build() {
            return new SegmentOptions(acceptAnyFixHours, speedCheckDistanceThresholdNm,
                    speedCheckMinTimeDiffMs, maxSpeedKnots, maxDistancePerSegmentNm,
                    maxTimePerSegmentMs);
        }
    }
}