package au.gov.amsa.geo.consensus;

public class Options {

	private final int before;
	private final int after;
	private final double maxSpeedKnots;
	private final long adjustmentLowerLimitMillis;
	private final long adjustmentUpperLimitMillis;

	private Options(int before, int after, double maxSpeedKnots,
			long adjustmentLowerLimitMillis, long adjustmentUpperLimitMillis) {
		this.before = before;
		this.after = after;
		this.maxSpeedKnots = maxSpeedKnots;
		this.adjustmentLowerLimitMillis = adjustmentLowerLimitMillis;
		this.adjustmentUpperLimitMillis = adjustmentUpperLimitMillis;
	}

	public int before() {
		return before;
	}

	public int after() {
		return after;
	}

	public double maxSpeedKnots() {
		return maxSpeedKnots;
	}

	public long adjustmentLowerLimitMillis() {
		return adjustmentLowerLimitMillis;
	}

	public long adjustmentUpperLimitMillis() {
		return adjustmentUpperLimitMillis;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private int before;
		private int after;
		private double maxSpeedKnots;
		private long adjustmentLowerLimitMillis;
		private long adjustmentUpperLimitMillis;

		private Builder() {
		}

		public Builder before(int before) {
			this.before = before;
			return this;
		}

		public Builder after(int after) {
			this.after = after;
			return this;
		}

		public Builder maxSpeedKnots(double maxSpeedKnots) {
			this.maxSpeedKnots = maxSpeedKnots;
			return this;
		}

		public Builder adjustmentLowerLimitMillis(
				long adjustmentLowerLimitMillis) {
			this.adjustmentLowerLimitMillis = adjustmentLowerLimitMillis;
			return this;
		}

		public Builder adjustmentUpperLimitMillis(
				long adjustmentUpperLimitMillis) {
			this.adjustmentUpperLimitMillis = adjustmentUpperLimitMillis;
			return this;
		}

		public Options build() {
			return new Options(before, after, maxSpeedKnots,
					adjustmentLowerLimitMillis, adjustmentUpperLimitMillis);
		}
	}

}
