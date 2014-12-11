package au.gov.amsa.navigation.ais;

import com.google.common.base.Optional;

public class Vessel {

	private long mmsi;
	private Optional<Integer> imo;
	private Optional<Integer> shipType;
	private Optional<Integer> dimensionA;
	private Optional<Integer> dimensionB;
	private Optional<Integer> dimensionC;
	private Optional<Integer> dimensionD;
	private Optional<Integer> lengthMetres;
	private Optional<Integer> widthMetres;
	

	private Vessel(long mmsi, Optional<Integer> imo,
			Optional<Integer> shipType, Optional<Integer> dimensionA,
			Optional<Integer> dimensionB, Optional<Integer> dimensionC,
			Optional<Integer> dimensionD, Optional<Integer> lengthMetres, Optional<Integer> widthMetres) {
		this.mmsi = mmsi;
		this.imo = imo;
		this.shipType = shipType;
		this.dimensionA = dimensionA;
		this.dimensionB = dimensionB;
		this.dimensionC = dimensionC;
		this.dimensionD = dimensionD;
		this.lengthMetres = lengthMetres;
		this.widthMetres = widthMetres;
	}

	public long getMmsi() {
		return mmsi;
	}

	public Optional<Integer> getImo() {
		return imo;
	}

	public Optional<Integer> getShipType() {
		return shipType;
	}

	public Optional<Integer> getDimensionA() {
		return dimensionA;
	}

	public Optional<Integer> getDimensionB() {
		return dimensionB;
	}

	public Optional<Integer> getDimensionC() {
		return dimensionC;
	}

	public Optional<Integer> getDimensionD() {
		return dimensionD;
	}

	public Optional<Integer> getLengthMetres() {
		return lengthMetres;
	}

	public Optional<Integer> getWidthMetres() {
		return widthMetres;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static Builder from(Vessel v) {
		return builder().mmsi(v.mmsi).imo(v.imo).shipType(v.shipType)
				.dimensionA(v.dimensionA).dimensionB(v.dimensionB)
				.dimensionC(v.dimensionC).dimensionD(v.dimensionD);
	}

	public static class Builder {

		private long mmsi;
		private Optional<Integer> imo = Optional.absent();
		private Optional<Integer> shipType = Optional.absent();
		private Optional<Integer> dimensionA = Optional.absent();
		private Optional<Integer> dimensionB = Optional.absent();
		private Optional<Integer> dimensionC = Optional.absent();
		private Optional<Integer> dimensionD = Optional.absent();
		private Optional<Integer> lengthMetres = Optional.absent();
		private Optional<Integer> widthMetres = Optional.absent();

		private Builder() {
		}

		public Builder mmsi(long mmsi) {
			this.mmsi = mmsi;
			return this;
		}

		public Builder imo(Optional<Integer> imo) {
			this.imo = imo;
			return this;
		}

		public Builder shipType(Optional<Integer> shipType) {
			this.shipType = shipType;
			return this;
		}

		public Builder dimensionA(Optional<Integer> dimensionA) {
			this.dimensionA = dimensionA;
			return this;
		}

		public Builder dimensionB(Optional<Integer> dimensionB) {
			this.dimensionB = dimensionB;
			return this;
		}

		public Builder dimensionC(Optional<Integer> dimensionC) {
			this.dimensionC = dimensionC;
			return this;
		}

		public Builder dimensionD(Optional<Integer> dimensionD) {
			this.dimensionD = dimensionD;
			return this;
		}

		public Vessel build() {
			return new Vessel(mmsi, imo, shipType, dimensionA, dimensionB,
					dimensionC, dimensionD, lengthMetres, widthMetres);
		}

		public Builder lengthMetres(Optional<Integer> lengthMetres) {
			this.lengthMetres = lengthMetres;
			return this;
		}
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("Vessel [mmsi=");
		b.append(mmsi);
		b.append(", imo=");
		b.append(imo);
		b.append(", shipType=");
		b.append(shipType);
		b.append(", dimensionA=");
		b.append(dimensionA);
		b.append(", dimensionB=");
		b.append(dimensionB);
		b.append(", dimensionC=");
		b.append(dimensionC);
		b.append(", dimensionD=");
		b.append(dimensionD);
		b.append(", lengthMetres=");
		b.append(lengthMetres);
		b.append(", widthMetres=");
		b.append(widthMetres);
		b.append("]");
		return b.toString();
	}
	
	

	

}
