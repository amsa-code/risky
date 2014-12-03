package au.gov.amsa.ais;

public class LineAndTime {

	private final String line;
	private final long time;

	public LineAndTime(String line, long time) {
		this.line = line;
		this.time = time;
	}

	public String getLine() {
		return line;
	}

	public long getTime() {
		return time;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LineAndTime [line=");
		builder.append(line);
		builder.append(", time=");
		builder.append(time);
		builder.append("]");
		return builder.toString();
	}
	
}
