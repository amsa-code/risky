package au.gov.amsa.geo.distance;

import au.gov.amsa.geo.model.Cell;
import au.gov.amsa.geo.model.Options;

public class CellAndDistance {
	private final Cell cell;
	private final double distanceNm;

	public CellAndDistance(Cell cell, double distanceNm) {
		this.cell = cell;
		this.distanceNm = distanceNm;
	}

	public Cell getCell() {
		return cell;
	}

	public double getDistanceNm() {
		return distanceNm;
	}

	/**
	 * Returns traffic density value for the cell in nm per square nm.
	 * 
	 * @param options
	 * @return
	 */
	public double getTrafficDensity(Options options) {
		return distanceNm / cell.areaNauticalMiles(options);
	}

	@Override
	public String toString() {
		return "CellAndDistance [cell=" + cell + ", distanceNm=" + distanceNm
				+ "]";
	}

}
