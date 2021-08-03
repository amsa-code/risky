package au.gov.amsa.navigation.ais;

import static java.util.Optional.ofNullable;

import java.util.Comparator;
import java.util.Optional;

import au.gov.amsa.ais.AisMessage;
import au.gov.amsa.ais.message.AisPosition;
import au.gov.amsa.ais.message.AisPositionA;
import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.ais.rx.Streams.TimestampedAndLine;
import au.gov.amsa.navigation.Mmsi;
import au.gov.amsa.navigation.VesselClass;
import au.gov.amsa.navigation.VesselPosition;
import au.gov.amsa.navigation.VesselPosition.NavigationalStatus;
import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Func1;

public class AisVesselPositions {

    public static Observable<VesselPosition> positions(Observable<String> nmea) {
        return Streams.extract(nmea).filter(isPresent())
        // aggregate ship data with the message
                .scan(new AisMessageAndVesselData(), AisMessageAndVesselData.aggregate)
                // positions only
                .filter(isPosition)
                // convert to vessel positions
                .map(toVesselPosition);
    }

    public static Transformer<String, VesselPosition> positions() {
        return nmea -> positions(nmea);
    }

    private static Func1<TimestampedAndLine<AisMessage>, Boolean> isPresent() {
        return t -> t.getMessage().isPresent();
    }

    public static Observable<TimestampedAndLine<AisMessage>> sortByTime(
            Observable<TimestampedAndLine<AisMessage>> source) {
        Comparator<TimestampedAndLine<AisMessage>> comparator = (t1, t2) -> ((Long) t1.getMessage()
                .get().time()).compareTo(t2.getMessage().get().time());
        return source
        // sort by time
                .lift(new SortOperator<TimestampedAndLine<AisMessage>>(comparator, 20000000));
    }

    public static Observable<VesselPosition> positionsSortedByTime(Observable<String> nmea) {
        return sortByTime(Streams.extract(nmea))
        // aggregate ship data with the message
                .scan(new AisMessageAndVesselData(), AisMessageAndVesselData.aggregate)
                // positions only, with lat long present
                .filter(isPosition)
                // convert to vessel positions
                .map(toVesselPosition);
    }

    private static final Func1<AisMessageAndVesselData, Boolean> isPosition = m -> {
        if (m.message().isPresent()
                && m.message().get().getMessage().get().message() instanceof AisPosition) {
            AisPosition p = (AisPosition) m.message().get().getMessage().get().message();
            return (p.getLatitude() != null && p.getLongitude() != null);
        } else
            return false;
    };

    private static final Func1<AisMessageAndVesselData, VesselPosition> toVesselPosition = messageAndData -> {

        AisPosition p = (AisPosition) messageAndData.message().get().getMessage().get().message();

        VesselClass cls;
        if (p instanceof AisPositionA)
            cls = VesselClass.A;
        else
            cls = VesselClass.B;
        Mmsi id = new Mmsi(p.getMmsi());
        Optional<Vessel> vessel = messageAndData.data().get(id);
        Optional<Integer> lengthMetres = vessel.isPresent() ? vessel.get().getLengthMetres()
                : Optional.<Integer> empty();

        Optional<Integer> widthMetres = vessel.isPresent() ? vessel.get().getWidthMetres()
                : Optional.<Integer> empty();

        Optional<Double> speedMetresPerSecond = p.getSpeedOverGroundKnots() != null ? Optional.of(p
                .getSpeedOverGroundKnots() * 0.5144444444) : Optional.<Double> empty();

        Optional<Integer> shipType = vessel.isPresent() ? vessel.get().getShipType() : Optional
                .<Integer> empty();

        NavigationalStatus navigationalStatus;
        if (p instanceof AisPositionA) {
            AisPositionA a = (AisPositionA) p;
            if (Util.equals(a.getNavigationalStatus(),
                    au.gov.amsa.ais.message.NavigationalStatus.AT_ANCHOR))
                navigationalStatus = NavigationalStatus.AT_ANCHOR;
            else if (Util.equals(a.getNavigationalStatus(),
                    au.gov.amsa.ais.message.NavigationalStatus.MOORED)) {
                navigationalStatus = NavigationalStatus.MOORED;
            } else
                navigationalStatus = NavigationalStatus.NOT_DEFINED;
        } else
            navigationalStatus = NavigationalStatus.NOT_DEFINED;

        Optional<String> positionAisNmea;
        if (p instanceof AisPositionA) {
            positionAisNmea = Optional.of(messageAndData.message().get().getLine());
        } else
            positionAisNmea = Optional.empty();

        Optional<String> shipStaticAisNmea;
        if (vessel.isPresent())
            shipStaticAisNmea = vessel.get().getNmea();
        else
            shipStaticAisNmea = Optional.empty();

        // TODO adjust lat, lon for position of ais set on ship
        // given by A,B,C,D? Or instead store the position offset in
        // metres in VesselPosition (preferred because RateOfTurn
        // (ROT) may enter the picture later).
        return VesselPosition.builder()
        // cog
                .cogDegrees(ofNullable(p.getCourseOverGround()))
                // heading
                .headingDegrees(ofNullable(toDouble(p.getTrueHeading())))
                // speed
                .speedMetresPerSecond(speedMetresPerSecond)
                // lat
                .lat(p.getLatitude())
                // lon
                .lon(p.getLongitude())
                // id
                .id(id)
                // length
                .lengthMetres(lengthMetres)
                // width
                .widthMetres(widthMetres)
                // time
                .time(messageAndData.message().get().getMessage().get().time())
                // ship type
                .shipType(shipType)
                // class
                .cls(cls)
                // at anchor
                .navigationalStatus(navigationalStatus)
                // position nmea
                .positionAisNmea(positionAisNmea)
                // ship static nmea
                .shipStaticAisNmea(shipStaticAisNmea)
                // build it
                .build();
    };

    private static Double toDouble(Number i) {
        if (i == null)
            return null;
        else
            return i.doubleValue();
    }

}
