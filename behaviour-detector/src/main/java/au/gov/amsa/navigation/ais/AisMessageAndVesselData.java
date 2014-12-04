package au.gov.amsa.navigation.ais;

import rx.functions.Func2;
import au.gov.amsa.ais.AisMessage;
import au.gov.amsa.ais.Timestamped;
import au.gov.amsa.ais.message.AisPositionBExtended;
import au.gov.amsa.ais.message.AisShipStaticA;

import com.google.common.base.Optional;

public final class AisMessageAndVesselData {

	private Optional<Timestamped<AisMessage>> message;
	private VesselData data;

	public AisMessageAndVesselData(Optional<Timestamped<AisMessage>> message, VesselData data) {
		this.message = message;
		this.data = data;
	}

	public AisMessageAndVesselData() {
		this(Optional.<Timestamped<AisMessage>> absent(), new VesselData());
	}

	public Optional<Timestamped<AisMessage>> message() {
		return message;
	}

	public VesselData data() {
		return data;
	}

	public static Func2<AisMessageAndVesselData, Timestamped<AisMessage>, AisMessageAndVesselData> aggregate = new Func2<AisMessageAndVesselData, Timestamped<AisMessage>, AisMessageAndVesselData>() {

		@Override
		public AisMessageAndVesselData call(AisMessageAndVesselData messageAndData,
				Timestamped<AisMessage> message) {
			if (message.message() instanceof AisShipStaticA)
				return new AisMessageAndVesselData(Optional.of(message),
						messageAndData.data().add((AisShipStaticA) message.message()));
			else if (message.message() instanceof AisPositionBExtended)
				return new AisMessageAndVesselData(Optional.of(message),
						messageAndData.data().add((AisPositionBExtended) message.message()));
			else
				return new AisMessageAndVesselData(Optional.of(message),
						messageAndData.data());
		}
	};

}
