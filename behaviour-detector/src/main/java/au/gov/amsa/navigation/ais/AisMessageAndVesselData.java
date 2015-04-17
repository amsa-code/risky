package au.gov.amsa.navigation.ais;

import rx.functions.Func2;
import au.gov.amsa.ais.AisMessage;
import au.gov.amsa.ais.message.AisPositionBExtended;
import au.gov.amsa.ais.message.AisShipStaticA;
import au.gov.amsa.ais.rx.Streams.TimestampedAndLine;

import com.google.common.base.Optional;

public final class AisMessageAndVesselData {

    private Optional<TimestampedAndLine<AisMessage>> message;
    private VesselData data;

    public AisMessageAndVesselData(Optional<TimestampedAndLine<AisMessage>> message, VesselData data) {
        this.message = message;
        this.data = data;
    }

    public AisMessageAndVesselData() {
        this(Optional.<TimestampedAndLine<AisMessage>> absent(), new VesselData());
    }

    public Optional<TimestampedAndLine<AisMessage>> message() {
        return message;
    }

    public VesselData data() {
        return data;
    }

    public static Func2<AisMessageAndVesselData, TimestampedAndLine<AisMessage>, AisMessageAndVesselData> aggregate = (
            messageAndData, message) -> {
        if (!message.getMessage().isPresent())
            throw new RuntimeException("unexpected");
        Optional<String> line = Optional.of(message.getLine());
        AisMessage m = message.getMessage().get().message();
        if (m instanceof AisShipStaticA)
            return new AisMessageAndVesselData(Optional.of(message), messageAndData.data().add(
                    (AisShipStaticA) m, line));
        else if (m instanceof AisPositionBExtended)
            return new AisMessageAndVesselData(Optional.of(message), messageAndData.data().add(
                    (AisPositionBExtended) m, line));
        else
            return new AisMessageAndVesselData(Optional.of(message), messageAndData.data());
    };

}
