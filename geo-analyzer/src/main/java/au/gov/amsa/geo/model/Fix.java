package au.gov.amsa.geo.model;

import java.util.Date;

public class Fix implements HasPosition, Comparable<Fix> {
	private final String craftId;
	private final long time;
	private final Position position;
	private final String source;
	private final int hashCode;

	public Fix(String craftId, double lat, double lon, long time, String source) {
		this.craftId = craftId;
		this.position = new Position(lat, lon);
		this.time = time;
		this.source = source;
		this.hashCode = calculateHashCode();
	}

	public Fix(String craftId, double lat, double lon, long time) {
		this(craftId, lat, lon, time, null);
	}

	public String getCraftId() {
		return craftId;
	}

	@Override
	public Position getPosition() {
		return position;
	}

	public long getTime() {
		return time;
	}

	public String getSource() {
		return source;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Fix [craftId=");
		builder.append(craftId);
		builder.append(", time=");
		builder.append(time);
		builder.append(", time=");
		builder.append(new Date(time));
		builder.append(", position=");
		builder.append(position);
		builder.append("]");
		return builder.toString();
	}

	public Fix time(long t) {
		return new Fix(craftId, position.getLat(), position.getLon(), t);
	}

	public Fix addTime(long delta) {
		return time(time + delta);
	}

	@Override
	public int compareTo(Fix o) {
		return ((Long) time).compareTo(o.time);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	private int calculateHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((craftId == null) ? 0 : craftId.hashCode());
		result = prime * result
				+ ((position == null) ? 0 : position.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + (int) (time ^ (time >>> 32));
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
		Fix other = (Fix) obj;
		if (craftId == null) {
			if (other.craftId != null)
				return false;
		} else if (!craftId.equals(other.craftId))
			return false;
		if (position == null) {
			if (other.position != null)
				return false;
		} else if (!position.equals(other.position))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (time != other.time)
			return false;
		return true;
	}

}
