package au.gov.amsa.navigation.ais;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;

import java.util.concurrent.ConcurrentHashMap;

import au.gov.amsa.ais.message.AisPositionBExtended;
import au.gov.amsa.ais.message.AisShipStaticA;
import au.gov.amsa.navigation.Identifier;
import au.gov.amsa.navigation.Mmsi;
import au.gov.amsa.navigation.ais.Vessel.Builder;

import com.google.common.base.Optional;

public class VesselData {

	private ConcurrentHashMap<Identifier, Vessel> map = new ConcurrentHashMap<>();

	public Optional<Vessel> get(Identifier id) {
		return Optional.fromNullable(map.get(id));
	}

	public VesselData add(AisShipStaticA m) {
		Identifier id = new Mmsi(m.getMmsi());
		// use atomic compare and set algorithm for non-blocking concurrency
		while (true) {
			Optional<Vessel> value = fromNullable(map.get(id));
			final Optional<Vessel> v;
			if (!value.isPresent()) {
				v = of(Vessel.builder().mmsi(m.getMmsi()).build());
			} else
				v = value;
			Builder builder = Vessel.from(v.get());
			builder = builder.imo(m.getImo());
			builder = builder.dimensionA(m.getDimensionA());
			builder = builder.dimensionA(m.getDimensionB());
			builder = builder.dimensionA(m.getDimensionC());
			builder = builder.dimensionA(m.getDimensionD());
			builder = builder.lengthMetres(m.getLengthMetres());
			builder = builder.shipType(Optional.of(m.getShipType()));
			Vessel vessel = builder.build();
			final boolean inserted;
			if (value.isPresent())
				inserted = map.replace(id, v.get(), vessel);
			else
				inserted = map.putIfAbsent(id, vessel) == null;
			if (inserted) {
				return this;
			}
		}

	}

	public VesselData add(AisPositionBExtended message) {
		// TODO
		return this;
	}
}
