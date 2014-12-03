package au.gov.amsa.ais.rx;

import java.util.Date;

public class CraftProperty {
	private final Mmsi mmsi;
	private final CraftPropertyName name;
	private final String value;
	private final long time;

	public CraftProperty(Mmsi mmsi, CraftPropertyName name, String value,
			long time) {
		this.mmsi = mmsi;
		this.name = name;
		this.value = value;
		this.time = time;
	}

	public Mmsi getMmsi() {
		return mmsi;
	}

	public CraftPropertyName getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	public long getTime() {
		return time;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CraftProperty [mmsi=");
		builder.append(mmsi);
		builder.append(", name=");
		builder.append(name);
		builder.append(", value=");
		builder.append(value);
		builder.append(", time=");
		builder.append(new Date(time));
		builder.append("]");
		return builder.toString();
	}

}
