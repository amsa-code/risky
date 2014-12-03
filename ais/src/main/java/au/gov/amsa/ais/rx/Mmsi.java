package au.gov.amsa.ais.rx;


public class Mmsi {
	private final long mmsi;

	public Mmsi(long mmsi) {
		this.mmsi = mmsi;
	}

	public long getMmsi() {
		return mmsi;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (mmsi ^ (mmsi >>> 32));
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
		Mmsi other = (Mmsi) obj;
		if (mmsi != other.mmsi)
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Mmsi [mmsi=");
		builder.append(mmsi);
		builder.append("]");
		return builder.toString();
	}

}
