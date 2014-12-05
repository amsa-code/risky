package au.gov.amsa.geo.model;

import static au.gov.amsa.util.navigation.Position.to180;

import java.math.BigDecimal;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

//TODO tested only in the environs of Australian SAR region (60 degrees longitude to just over the 180 boundary to the east)
public class Grid {

	private static Logger log = Logger.getLogger(Grid.class);

	private final TreeSet<Double> lats;

	/**
	 * top lat of cell <--> index of cell
	 */
	private final BiMap<Double, Long> latIndexes;

	private final TreeSet<Double> lons;
	/**
	 * left lon of cell <--> index of cell
	 */
	private final BiMap<Double, Long> lonIndexes;

	private final Options options;

	public Grid(Options options) {
		this.options = options;
		lats = new TreeSet<Double>();
		{
			BigDecimal lat = getStartLat(options);
			while (lat.doubleValue() >= options.getFilterBounds()
					.getBottomRightLat()) {
				lats.add(lat.doubleValue());
				lat = lat.subtract(options.getCellSizeDegrees());
			}
			lat = lat.subtract(options.getCellSizeDegrees());
			lats.add(lat.doubleValue());
		}

		{
			latIndexes = HashBiMap.create();
			long index = 0;
			for (double lat : lats.descendingSet()) {
				latIndexes.put(lat, index);
				index++;
			}
		}

		{
			lons = new TreeSet<Double>();
			BigDecimal lon = getStartLon(options);
			// handle values around the 180 longitude line
			// TODO this will need more handling for arbitrary regions on the
			// earth's surface.
			double maxLon;
			if (options.getFilterBounds().getBottomRightLon() < lon
					.doubleValue())
				maxLon = options.getFilterBounds().getBottomRightLon() + 360;
			else
				maxLon = options.getFilterBounds().getBottomRightLon();
			while (lon.doubleValue() <= maxLon) {
				lons.add(to180(lon.doubleValue()));
				lon = lon.add(options.getCellSizeDegrees());
			}
			lon.add(options.getCellSizeDegrees());
			lons.add(lon.doubleValue());
		}
		{
			lonIndexes = HashBiMap.create();
			long index = 0;
			for (double lon : lons) {
				lonIndexes.put(lon, index);
				index++;
			}
		}

	}

	@VisibleForTesting
	static BigDecimal getStartLat(Options options) {
		final long moveStartLatUpByCells;
		if (options.getFilterBounds().getTopLeftLat() == options.getOriginLat()
				.doubleValue())
			moveStartLatUpByCells = 0;
		else
			moveStartLatUpByCells = Math.max(0, Math.round(Math.floor((options
					.getFilterBounds().getTopLeftLat() - options.getOriginLat()
					.doubleValue())
					/ options.getCellSizeDegrees().doubleValue()) + 1));
		BigDecimal result = options.getOriginLat();
		for (int i = 0; i < moveStartLatUpByCells; i++)
			result = result.add(options.getCellSizeDegrees());
		return result;
	}

	@VisibleForTesting
	static BigDecimal getStartLon(Options options) {
		final long moveStartLonLeftByCells;
		if (options.getFilterBounds().getTopLeftLon() == options.getOriginLon()
				.doubleValue())
			moveStartLonLeftByCells = 0;
		else {
			moveStartLonLeftByCells = Math.max(0, Math.round(Math
					.floor((options.getOriginLon().doubleValue() - options
							.getFilterBounds().getTopLeftLon())
							/ options.getCellSizeDegrees().doubleValue()) + 1));
		}
		BigDecimal result = options.getOriginLon();
		for (int i = 0; i < moveStartLonLeftByCells; i++)
			result = result.subtract(options.getCellSizeDegrees());
		return result;
	}

	public Optional<Cell> cellAt(double lat, double lon) {
		if (!options.getFilterBounds().contains(lat, lon))
			return Optional.absent();
		else {
			Long latIndex = latIndexes.get(lats.ceiling(lat));
			Long lonIndex = lonIndexes.get(lons.floor(lon));
			return Optional.of(new Cell(latIndex, lonIndex));
		}
	}

	public double leftEdgeLongitude(Cell cell) {
		return leftEdgeLongitude(cell.getLonIndex());
	}

	private double leftEdgeLongitude(long lonIndex) {
		return lonIndexes.inverse().get(lonIndex);
	}

	public double rightEdgeLongitude(Cell cell) {
		try {
			return lonIndexes.inverse().get(cell.getLonIndex() + 1);
		} catch (RuntimeException e) {
			log.warn("cell=" + cell + ", options=" + options);
			throw e;
		}
	}

	public double topEdgeLatitude(Cell cell) {
		return topEdgeLatitude(cell.getLatIndex());
	}

	public double topEdgeLatitude(long latIndex) {
		return latIndexes.inverse().get(latIndex);
	}

	public double bottomEdgeLatitude(Cell cell) {
		return latIndexes.inverse().get(cell.getLatIndex() + 1);
	}

	public double centreLat(long latIndex) {
		return topEdgeLatitude(latIndex)
				- options.getCellSizeDegrees().doubleValue() / 2;
	}

	public double centreLon(long lonIndex) {
		return leftEdgeLongitude(lonIndex)
				+ options.getCellSizeDegrees().doubleValue() / 2;
	}

}
