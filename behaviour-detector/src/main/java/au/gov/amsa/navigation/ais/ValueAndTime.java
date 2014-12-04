package au.gov.amsa.navigation.ais;

public class ValueAndTime {

	private Object value;
	private long time;

	public ValueAndTime(Object value, long time) {
		this.value = value;
		this.time = time;
	}

	public Object getValue() {
		return value;
	}

	public long getTime() {
		return time;
	}

}
