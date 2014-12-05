package au.gov.amsa.geo.consensus;

import au.gov.amsa.geo.model.Fix;

public class ConsensusValue implements Comparable<ConsensusValue> {
	private final Fix fix;
	private final double value;

	public ConsensusValue(Fix fix, double value) {
		this.fix = fix;
		this.value = value;
	}

	public Fix getFix() {
		return fix;
	}

	public double getValue() {
		return value;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Consensus[fix=");
		builder.append(fix);
		builder.append(", value=");
		builder.append(value);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int compareTo(ConsensusValue o) {
		return ((Double) value).compareTo(o.value);
	}

}
