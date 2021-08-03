package au.gov.amsa.navigation.ais;

import java.util.Optional;

import com.github.davidmoten.guavamini.Preconditions;

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
	private Optional<String> nmea;

	private Vessel(long mmsi, Optional<Integer> imo,
			Optional<Integer> shipType, Optional<Integer> dimensionA,
			Optional<Integer> dimensionB, Optional<Integer> dimensionC,
			Optional<Integer> dimensionD, Optional<Integer> lengthMetres,
			Optional<Integer> widthMetres, Optional<String> nmea) {
		Preconditions.checkNotNull(imo);
		Preconditions.checkNotNull(shipType);
		Preconditions.checkNotNull(dimensionA);
		Preconditions.checkNotNull(dimensionB);
		Preconditions.checkNotNull(dimensionC);
		Preconditions.checkNotNull(dimensionD);
		Preconditions.checkNotNull(lengthMetres);
		Preconditions.checkNotNull(widthMetres);
		Preconditions.checkNotNull(nmea);
		this.mmsi = mmsi;
		this.imo = imo;
		this.shipType = shipType;
		this.dimensionA = dimensionA;
		this.dimensionB = dimensionB;
		this.dimensionC = dimensionC;
		this.dimensionD = dimensionD;
		this.lengthMetres = lengthMetres;
		this.widthMetres = widthMetres;
		this.nmea = nmea;
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

	public Optional<String> getNmea() {
		return nmea;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static Builder from(Vessel v) {
		return builder().mmsi(v.mmsi).imo(v.imo).shipType(v.shipType)
				.dimensionA(v.dimensionA).dimensionB(v.dimensionB)
				.dimensionC(v.dimensionC).dimensionD(v.dimensionD)
				.lengthMetres(v.lengthMetres).widthMetres(v.widthMetres)
				.nmea(v.nmea);
	}

	public static class Builder {

		private long mmsi;
		private Optional<Integer> imo = Optional.empty();
		private Optional<Integer> shipType = Optional.empty();
		private Optional<Integer> dimensionA = Optional.empty();
		private Optional<Integer> dimensionB = Optional.empty();
		private Optional<Integer> dimensionC = Optional.empty();
		private Optional<Integer> dimensionD = Optional.empty();
		private Optional<Integer> lengthMetres = Optional.empty();
		private Optional<Integer> widthMetres = Optional.empty();
		private Optional<String> nmea = Optional.empty();

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

		public Builder lengthMetres(Optional<Integer> lengthMetres) {
			this.lengthMetres = lengthMetres;
			return this;
		}

		public Builder widthMetres(Optional<Integer> widthMetres) {
			this.widthMetres = widthMetres;
			return this;
		}

		public Builder nmea(Optional<String> nmea) {
			this.nmea = nmea;
			return this;
		}

		public Vessel build() {
			return new Vessel(mmsi, imo, shipType, dimensionA, dimensionB,
					dimensionC, dimensionD, lengthMetres, widthMetres, nmea);
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
		b.append(", nmea=");
		b.append(nmea);
		b.append("]");
		return b.toString();
	}

	
}
