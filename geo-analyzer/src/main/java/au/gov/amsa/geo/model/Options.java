package au.gov.amsa.geo.model;

import static au.gov.amsa.geo.model.Util.formatDate;

import java.math.BigDecimal;
import java.util.Optional;

import org.joda.time.DateTime;

import com.github.davidmoten.guavamini.Preconditions;

public class Options {

	private static final double KM_PER_NM = 1.852;
	private final BigDecimal originLat;
	private final BigDecimal originLon;
	private final BigDecimal cellSizeDegrees;
	private final double cellSizeDegreesDouble;
	private final Bounds bounds;
	private final Grid grid;
	private final Bounds filterBounds;
	private final SegmentOptions segmentOptions;
	private final Optional<Long> startTime;
	private final Optional<Long> finishTime;

	public Options(BigDecimal originLat, BigDecimal originLon,
			BigDecimal cellSizeDegrees, Bounds bounds,
			Optional<Bounds> filterBounds, SegmentOptions segmentOptions,
			Optional<Long> startTime, Optional<Long> finishTime) {
		Preconditions.checkNotNull(originLat);
		Preconditions.checkNotNull(originLon);
		Preconditions.checkNotNull(cellSizeDegrees);
		Preconditions.checkArgument(cellSizeDegrees.doubleValue() > 0);
		Preconditions.checkNotNull(filterBounds);
		Preconditions.checkNotNull(startTime);
		Preconditions.checkNotNull(finishTime);
		this.originLat = originLat;
		this.originLon = originLon;
		this.cellSizeDegrees = cellSizeDegrees;
		this.bounds = bounds;
		if (filterBounds.isPresent())
			this.filterBounds = filterBounds.get();
		else
			this.filterBounds = bounds;
		this.segmentOptions = segmentOptions;
		this.startTime = startTime;
		this.finishTime = finishTime;
		grid = new Grid(this);
		this.cellSizeDegreesDouble = cellSizeDegrees.doubleValue();
	}

	public int maxCells() {
		return (int) Math.round(bounds.getWidthDegrees()
				* bounds.getHeightDegrees() / cellSizeDegreesDouble
				/ cellSizeDegreesDouble);
	}

	public static Bounds createFilterBounds(Bounds bounds,
			SegmentOptions segmentOptions) {
		double maxTimeTravellingHours = 4.0;
		double maxDistanceKm = maxTimeTravellingHours
				* segmentOptions.maxSpeedKnots() * KM_PER_NM;
		au.gov.amsa.util.navigation.Position topLeftBounds = au.gov.amsa.util.navigation.Position
				.create(bounds.getTopLeftLat(), bounds.getTopLeftLon());
		double topLat = topLeftBounds.predict(maxDistanceKm, 0).getLat();
		double leftLon = topLeftBounds.predict(maxDistanceKm, -90).getLon();
		au.gov.amsa.util.navigation.Position bottomRightBounds = au.gov.amsa.util.navigation.Position
				.create(bounds.getBottomRightLat(), bounds.getBottomRightLon());

		double bottomLat = bottomRightBounds.predict(maxDistanceKm, 180)
				.getLat();
		double rightLon = bottomRightBounds.predict(maxDistanceKm, 90).getLon();

		return new Bounds(topLat, leftLon, bottomLat, rightLon);
	}

	public Optional<Long> getStartTime() {
		return startTime;
	}

	public Optional<Long> getFinishTime() {
		return finishTime;
	}

	public BigDecimal getOriginLat() {
		return originLat;
	}

	public BigDecimal getOriginLon() {
		return originLon;
	}

	public BigDecimal getCellSizeDegrees() {
		return cellSizeDegrees;
	}

	public double getCellSizeDegreesAsDouble() {
		return cellSizeDegreesDouble;
	}

	public Bounds getBounds() {
		return bounds;
	}

	public Bounds getFilterBounds() {
		return filterBounds;
	}

	public Grid getGrid() {
		return grid;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Options [originLat=");
		builder.append(originLat);
		builder.append(", originLon=");
		builder.append(originLon);
		builder.append(", cellSizeDegrees=");
		builder.append(cellSizeDegrees);
		builder.append(", bounds=");
		builder.append(bounds);
		builder.append(", filterBounds=");
		builder.append(filterBounds);
		builder.append(", startTime=");
		builder.append(formatDate(startTime));
		builder.append(", finishTime=");
		builder.append(formatDate(finishTime));
		builder.append(", segmentOptions=");
		builder.append(segmentOptions);
		builder.append("]");
		return builder.toString();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static Builder builder(Options o) {
		return builder().bounds(o.getBounds())
				.cellSizeDegrees(o.getCellSizeDegrees())
				.filterBounds(o.getFilterBounds())
				.finishTime(o.getFinishTime()).startTime(o.getStartTime())
				.originLat(o.getOriginLat()).originLon(o.getOriginLon())
				.segmentOptions(o.getSegmentOptions());
	}

	public SegmentOptions getSegmentOptions() {
		return segmentOptions;
	}

	public static class Builder {

		private BigDecimal originLat = BigDecimal.ZERO;
		private BigDecimal originLon = BigDecimal.ZERO;
		private BigDecimal cellSizeDegrees = BigDecimal.ONE;
		private Bounds bounds = new Bounds(15, 67, -60, 179);
		private SegmentOptions segmentOptions = SegmentOptions.builder()
				.build();
		private Optional<Bounds> filterBounds = Optional.empty();
		private Optional<Long> startTime = Optional.empty();
		private Optional<Long> finishTime = Optional.empty();

		private Builder() {
		}

		public Builder originLat(BigDecimal originLat) {
			this.originLat = originLat;
			return this;
		}

		public Builder originLat(double originLat) {
			this.originLat = BigDecimal.valueOf(originLat);
			return this;
		}

		public Builder originLon(BigDecimal originLon) {
			this.originLon = originLon;
			return this;
		}

		public Builder originLon(double originLon) {
			this.originLon = BigDecimal.valueOf(originLon);
			return this;
		}

		public Builder cellSizeDegrees(BigDecimal cellSizeDegrees) {
			this.cellSizeDegrees = cellSizeDegrees;
			return this;
		}

		public Builder cellSizeDegrees(double cellSizeDegrees) {
			this.cellSizeDegrees = BigDecimal.valueOf(cellSizeDegrees)
					.setScale(30);
			return this;
		}

		public Builder bounds(Bounds bounds) {
			this.bounds = bounds;
			return this;
		}

		public Builder filterBounds(Bounds bounds) {
			this.filterBounds = Optional.of(bounds);
			return this;
		}

		public Builder startTime(Optional<Long> startTime) {
			this.startTime = startTime;
			return this;
		}

		public Builder startTime(String isoDateTimeFormat) {
			this.startTime = Optional.of(DateTime.parse(isoDateTimeFormat)
					.getMillis());
			return this;
		}

		public Builder finishTime(Optional<Long> finishTime) {
			this.finishTime = finishTime;
			return this;
		}

		public Builder finishTime(String isoDateTimeFormat) {
			this.finishTime = Optional.of(DateTime.parse(isoDateTimeFormat)
					.getMillis());
			return this;
		}

		public Builder segmentOptions(SegmentOptions o) {
			this.segmentOptions = o;
			return this;
		}

		public Options build() {
			return new Options(originLat, originLon, cellSizeDegrees, bounds,
					filterBounds, segmentOptions, startTime, finishTime);
		}
	}

	public Builder buildFrom() {
		return builder(this);
	}

}
