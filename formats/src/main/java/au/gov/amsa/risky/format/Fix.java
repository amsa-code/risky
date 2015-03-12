package au.gov.amsa.risky.format;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public final class Fix implements HasFix {

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

	public Fix(long mmsi, float lat, float lon, long time, Optional<Integer> latencySeconds,
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

	public long getMmsi() {
		return mmsi;
	}

	public long getTime() {
		return time;
	}

	public float getLat() {
		return lat;
	}

	public float getLon() {
		return lon;
	}

	public Optional<NavigationalStatus> getNavigationalStatus() {
		return navigationalStatus;
	}

	public Optional<Float> getSpeedOverGroundKnots() {
		return speedOverGroundKnots;
	}

	public Optional<Float> getCourseOverGroundDegrees() {
		return courseOverGroundDegrees;
	}

	public Optional<Float> getHeadingDegrees() {
		return headingDegrees;
	}

	public AisClass getAisClass() {
		return aisClass;
	}

	public Optional<Integer> getLatencySeconds() {
		return latencySeconds;
	}

	public Optional<Short> getSource() {
		return source;
	}

	public Optional<Byte> getRateOfTurn() {
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
		b.append(time);
		b.append(", navigationalStatus=");
		b.append(navigationalStatus);
		b.append(", speedOverGroundKnots=");
		b.append(speedOverGroundKnots);
		b.append(", courseOverGroundDegrees=");
		b.append(courseOverGroundDegrees);
		b.append(", headingDegrees=");
		b.append(headingDegrees);
		b.append(", aisClass=");
		b.append(aisClass);
		b.append(", latencySeconds=");
		b.append(latencySeconds);
		b.append(", source=");
		b.append(source);
		b.append("]");
		return b.toString();
	}

	@Override
	public Fix fix() {
		return this;
	}

}
