package au.gov.amsa.geo.model;

import java.io.Serializable;

import com.google.common.base.Optional;

/**
 * A latitude longitude rectangle bordered by rhumb lines east-west and
 * north-south. The rectangle is equal in degrees in width and height (but in
 * terms of distance this can of course vary depending on the location of the
 * cell on the earth).
 */
public class Cell implements Serializable {

	private static final long serialVersionUID = 5489135874617545438L;
	private static final double radiusEarthKm = 6371.01;
	private static final double KM_PER_NM = 1.852;

	private final long latIndex;
	private final long lonIndex;
	private final int hashCode;

	/**
	 * Constructor.
	 * 
	 * @param latIndex
	 * @param lonIndex
	 */
	Cell(long latIndex, long lonIndex) {
		this.latIndex = latIndex;
		this.lonIndex = lonIndex;
		hashCode = calculateHashCode();
	}

	public static Optional<Cell> cellAt(HasPosition p, Options options) {
		return cellAt(p.getPosition().getLat(), p.getPosition().getLon(),
				options);
	}

	public static Optional<Cell> cellAt(double lat, double lon, Options options) {
		return options.getGrid().cellAt(lat, lon);
	}

	public double leftEdgeLongitude(Options options) {
		return options.getGrid().leftEdgeLongitude(this);
	}

	public double rightEdgeLongitude(Options options) {
		return options.getGrid().rightEdgeLongitude(this);
	}

	public double topEdgeLatitude(Options options) {
		return options.getGrid().topEdgeLatitude(this);
	}

	public double bottomEdgeLatitude(Options options) {
		return options.getGrid().bottomEdgeLatitude(this);
	}

	public long getLatIndex() {
		return latIndex;
	}

	public long getLonIndex() {
		return lonIndex;
	}

	public double getCentreLon(Options options) {
		return options.getGrid().centreLon(lonIndex);
	}

	public double getCentreLat(Options options) {
		return options.getGrid().centreLat(latIndex);
	}

	/**
	 * From http://mathforum.org/library/drmath/view/63767.html
	 * 
	 * @param options
	 * @return
	 */
	public double areaNauticalMiles(Options options) {
		double topLatRads = Math.toRadians(topEdgeLatitude(options));
		double bottomLatRads = Math.toRadians(bottomEdgeLatitude(options));

		return Math.PI / 180 * radiusEarthKm * radiusEarthKm
				* Math.abs(Math.sin(topLatRads) - Math.sin(bottomLatRads))
				* options.getCellSizeDegreesAsDouble()
				/ (KM_PER_NM * KM_PER_NM);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	private int calculateHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (latIndex ^ (latIndex >>> 32));
		result = prime * result + (int) (lonIndex ^ (lonIndex >>> 32));
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
		Cell other = (Cell) obj;
		if (latIndex != other.latIndex)
			return false;
		if (lonIndex != other.lonIndex)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Cell [latIndex=" + latIndex + ", lonIndex=" + lonIndex + "]";
	}

}
