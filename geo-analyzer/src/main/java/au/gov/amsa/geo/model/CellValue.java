package au.gov.amsa.geo.model;

import java.io.Serializable;

public class CellValue implements Serializable {
	private static final long serialVersionUID = -4991689182553669704L;
	private final double centreLat;
	private final double centreLon;
	private final double value;

	public CellValue(double centreLat, double centreLon, double value) {
		this.centreLat = centreLat;
		this.centreLon = centreLon;
		this.value = value;
	}

	public double getCentreLat() {
		return centreLat;
	}

	public double getCentreLon() {
		return centreLon;
	}

	public double getValue() {
		return value;
	}

}
