package au.gov.amsa.risky.format;

import com.google.common.base.Optional;

public final class Fix {

	private final long mmsi;
	private final float lat;
	private final float lon;
	private final long time;
	private final Optional<NavigationalStatus> navigationalStatus;
	private final Optional<Float> speedOverGroundKnots;
	private final Optional<Float> courseOverGroundDegrees;
	private final Optional<Float> headingDegrees;
	private final AisClass aisClass;
	
	public Fix(long mmsi, float lat, float lon,long time, 
			Optional<NavigationalStatus> navigationalStatus,
			Optional<Float> speedOverGroundKnots,
			Optional<Float> courseOverGroundDegrees,
			Optional<Float> headingDegrees, AisClass aisClass) {
		this.mmsi = mmsi;
		this.lat = lat;
		this.lon = lon;
		this.time = time;
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
	
}
