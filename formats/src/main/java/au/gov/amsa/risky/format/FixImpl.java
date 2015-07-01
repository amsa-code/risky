package au.gov.amsa.risky.format;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public final class FixImpl implements HasFix, Fix {

    public static boolean validate = true;

    private final long mmsi;
    private final float lat;
    private final float lon;
    private final long time;
    private final Optional<NavigationalStatus> navigationalStatus;
    private final Optional<Float> speedOverGroundKnots;
    private final Optional<Float> courseOverGroundDegrees;
    private final Optional<Float> headingDegrees;
    private final AisClass aisClass;
    private final Optional<Integer> latencySeconds;
    private final Optional<Short> source;

    public FixImpl(long mmsi, float lat, float lon, long time, Optional<Integer> latencySeconds,
            Optional<Short> source, Optional<NavigationalStatus> navigationalStatus,
            Optional<Float> speedOverGroundKnots, Optional<Float> courseOverGroundDegrees,
            Optional<Float> headingDegrees, AisClass aisClass) {

        if (validate) {
            Preconditions.checkNotNull(navigationalStatus);
            Preconditions.checkNotNull(courseOverGroundDegrees);
            Preconditions.checkNotNull(headingDegrees);
            Preconditions.checkNotNull(aisClass);
            Preconditions.checkNotNull(latencySeconds);
            Preconditions.checkNotNull(source);
            if (courseOverGroundDegrees.isPresent()) {
                Preconditions.checkArgument(courseOverGroundDegrees.get() < 360
                        && courseOverGroundDegrees.get() >= 0,
                        "cog=" + courseOverGroundDegrees.get());
            }
            if (headingDegrees.isPresent()) {
                Preconditions
                        .checkArgument(headingDegrees.get() < 360 && headingDegrees.get() >= 0);
            }
            Preconditions.checkArgument(lat >= -90 && lat <= 90);
            Preconditions.checkArgument(lon >= -180 && lon <= 180);
        }
        this.mmsi = mmsi;
        this.lat = lat;
        this.lon = lon;
        this.time = time;
        this.latencySeconds = latencySeconds;
        this.source = source;
        this.navigationalStatus = navigationalStatus;
        this.speedOverGroundKnots = speedOverGroundKnots;
        this.courseOverGroundDegrees = courseOverGroundDegrees;
        this.headingDegrees = headingDegrees;
        this.aisClass = aisClass;
    }

    @Override
    public long mmsi() {
        return mmsi;
    }

    @Override
    public long time() {
        return time;
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
    public Optional<NavigationalStatus> navigationalStatus() {
        return navigationalStatus;
    }

    @Override
    public Optional<Float> speedOverGroundKnots() {
        return speedOverGroundKnots;
    }

    @Override
    public Optional<Float> courseOverGroundDegrees() {
        return courseOverGroundDegrees;
    }

    @Override
    public Optional<Float> headingDegrees() {
        return headingDegrees;
    }

    @Override
    public AisClass aisClass() {
        return aisClass;
    }

    @Override
    public Optional<Integer> latencySeconds() {
        return latencySeconds;
    }

    @Override
    public Optional<Short> source() {
        return source;
    }

    @Override
    public Optional<Byte> rateOfTurn() {
        return Optional.absent();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Fix [mmsi=");
        b.append(mmsi);
        b.append(", lat=");
        b.append(lat);
        b.append(", lon=");
        b.append(lon);
        b.append(", time=");
        b.append(DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(time), ZoneId.of("UTC"))));
        b.append(", navigationalStatus=");
        b.append(navigationalStatus.orNull());
        b.append(", speedOverGroundKnots=");
        b.append(speedOverGroundKnots.orNull());
        b.append(", courseOverGroundDegrees=");
        b.append(courseOverGroundDegrees.orNull());
        b.append(", headingDegrees=");
        b.append(headingDegrees.orNull());
        b.append(", aisClass=");
        b.append(aisClass);
        b.append(", latencySeconds=");
        b.append(latencySeconds.orNull());
        b.append(", source=");
        b.append(source.orNull());
        b.append("]");
        return b.toString();
    }

    @Override
    public Fix fix() {
        return this;
    }

}
