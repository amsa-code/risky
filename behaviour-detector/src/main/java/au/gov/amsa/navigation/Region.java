package au.gov.amsa.navigation;

import com.google.common.base.Preconditions;

public class Region {

	private final double topLeftLat, topLeftLon, bottomRightLat, bottomRightLon;

	public Region(double topLeftLat, double topLeftLon, double bottomLeftLat,
			double bottomRightLon) {
		Preconditions.checkArgument(bottomLeftLat <= topLeftLat);
		Preconditions.checkArgument(topLeftLon <= bottomRightLon);
		this.topLeftLat = topLeftLat;
		this.topLeftLon = topLeftLon;
		this.bottomRightLat = bottomLeftLat;
		this.bottomRightLon = bottomRightLon;
	}

	public double topLeftLat() {
		return topLeftLat;
	}

	public double topLeftLon() {
		return topLeftLon;
	}

	public double bottomRightLat() {
		return bottomRightLat;
	}

	public double bottomRightLon() {
		return bottomRightLon;
	}

	public boolean inRegion(double lat, double lon) {
		return lat >= bottomRightLat && lat <= topLeftLat && lon >= topLeftLon
				&& lon <= bottomRightLon;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(bottomRightLat);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(bottomRightLon);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(topLeftLat);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(topLeftLon);
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
		Region other = (Region) obj;
		if (Double.doubleToLongBits(bottomRightLat) != Double
				.doubleToLongBits(other.bottomRightLat))
			return false;
		if (Double.doubleToLongBits(bottomRightLon) != Double
				.doubleToLongBits(other.bottomRightLon))
			return false;
		if (Double.doubleToLongBits(topLeftLat) != Double
				.doubleToLongBits(other.topLeftLat))
			return false;
		if (Double.doubleToLongBits(topLeftLon) != Double
				.doubleToLongBits(other.topLeftLon))
			return false;
		return true;
	}
	
}
