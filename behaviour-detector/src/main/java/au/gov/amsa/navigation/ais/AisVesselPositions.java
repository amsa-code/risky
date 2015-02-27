package au.gov.amsa.navigation.ais;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;
import static rx.Observable.empty;
import static rx.Observable.just;

import java.util.Comparator;

import rx.Observable;
import rx.functions.Func1;
import au.gov.amsa.ais.AisMessage;
import au.gov.amsa.ais.message.AisPosition;
import au.gov.amsa.ais.message.AisPositionA;
import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.ais.rx.Streams.TimestampedAndLine;
import au.gov.amsa.navigation.Mmsi;
import au.gov.amsa.navigation.VesselClass;
import au.gov.amsa.navigation.VesselPosition;
import au.gov.amsa.navigation.VesselPosition.NavigationalStatus;

import com.google.common.base.Optional;
import com.sleepycat.je.rep.elections.Utils;

public class AisVesselPositions {


	public static Observable<VesselPosition> positions(Observable<String> nmea) {
		return Streams.extract(nmea)
				.filter(isPresent())
				// aggregate ship data with the message
				.scan(new AisMessageAndVesselData(),
						AisMessageAndVesselData.aggregate)
				// positions only
				.filter(isPosition)
				// convert to vessel positions
				.flatMap(toVesselPosition);
	}

	private static Func1<TimestampedAndLine<AisMessage>, Boolean> isPresent() {
		return new Func1<TimestampedAndLine<AisMessage>, Boolean>() {

			@Override
			public Boolean call(TimestampedAndLine<AisMessage> t) {
				return t.getMessage().isPresent();
			}
		};
	}

	public static Observable<TimestampedAndLine<AisMessage>> sortByTime(
			Observable<TimestampedAndLine<AisMessage>> source) {
		Comparator<TimestampedAndLine<AisMessage>> comparator = new Comparator<TimestampedAndLine<AisMessage>>() {
			@Override
			public int compare(TimestampedAndLine<AisMessage> t1,
					TimestampedAndLine<AisMessage> t2) {
				return ((Long) t1.getMessage().get().time()).compareTo(t2
						.getMessage().get().time());
			}
		};
		return source
		// sort by time
				.lift(new SortOperator<TimestampedAndLine<AisMessage>>(
						comparator, 20000000));
	}

	public static Observable<VesselPosition> positionsSortedByTime(
			Observable<String> nmea) {
		return sortByTime(Streams.extract(nmea))
		// aggregate ship data with the message
				.scan(new AisMessageAndVesselData(),
						AisMessageAndVesselData.aggregate)
				// positions only
				.filter(isPosition)
				// convert to vessel positions
				.flatMap(toVesselPosition);
	}

	private static final Func1<AisMessageAndVesselData, Boolean> isPosition = new Func1<AisMessageAndVesselData, Boolean>() {
		@Override
		public Boolean call(AisMessageAndVesselData m) {
			return m.message().isPresent()
					&& m.message().get().getMessage().get().message() instanceof AisPosition;
		}
	};

	private static final Func1<AisMessageAndVesselData, Observable<VesselPosition>> toVesselPosition = new Func1<AisMessageAndVesselData, Observable<VesselPosition>>() {
		@Override
		public Observable<VesselPosition> call(
				AisMessageAndVesselData messageAndData) {

			if (messageAndData.message().isPresent()
					&& messageAndData.message().get().getMessage().get()
							.message() instanceof AisPosition) {
				AisPosition p = (AisPosition) messageAndData.message().get()
						.getMessage().get().message();
				if (p.getCourseOverGround() == null
						|| p.getTrueHeading() == null
						|| p.getSpeedOverGroundKnots() == null
						|| p.getLatitude() == null || p.getLongitude() == null)
					return empty();
				else {
					final VesselClass cls;
					if (p instanceof AisPositionA)
						cls = VesselClass.A;
					else
						cls = VesselClass.B;
					Mmsi id = new Mmsi(p.getMmsi());
					Optional<Vessel> vessel = messageAndData.data().get(id);
					Optional<Integer> lengthMetres = vessel.isPresent() ? vessel
							.get().getLengthMetres() : Optional
							.<Integer> absent();

					Optional<Integer> widthMetres = vessel.isPresent() ? vessel
							.get().getWidthMetres() : Optional
							.<Integer> absent();

					Optional<Double> speedMetresPerSecond = p
							.getSpeedOverGroundKnots() != null ? of(p
							.getSpeedOverGroundKnots() * 0.5144444444)
							: Optional.<Double> absent();

					Optional<Integer> shipType = vessel.isPresent() ? vessel
							.get().getShipType() : Optional.<Integer> absent();

					final NavigationalStatus navigationalStatus;
					if (p instanceof AisPositionA) {
						AisPositionA a = (AisPositionA) p;
						if (Util.equals( a.getNavigationalStatus(),au.gov.amsa.ais.message.NavigationalStatus.AT_ANCHOR))
							navigationalStatus = NavigationalStatus.AT_ANCHOR;
						else if (Util.equals(a.getNavigationalStatus(),au.gov.amsa.ais.message.NavigationalStatus.MOORED)) {
							navigationalStatus = NavigationalStatus.MOORED;
						} else
							navigationalStatus = NavigationalStatus.NOT_DEFINED;
					} else
						navigationalStatus = NavigationalStatus.NOT_DEFINED;


					Optional<String> positionAisNmea;
					if (p instanceof AisPositionA) {
						positionAisNmea = Optional.of(messageAndData.message()
								.get().getLine());
					} else
						positionAisNmea = Optional.absent();

					Optional<String> shipStaticAisNmea;
					if (vessel.isPresent())
						shipStaticAisNmea = vessel.get().getNmea();
					else
						shipStaticAisNmea = Optional.absent();

					// TODO adjust lat, lon for position of ais set on ship
					// given by A,B,C,D? Or instead store the position offset in
					// metres in VesselPosition (preferred because RateOfTurn
					// (ROT) may enter the picture later).
					try {
						return just(VesselPosition
								.builder()
								.cogDegrees(
										fromNullable(p.getCourseOverGround()))
								.headingDegrees(
										fromNullable((double) p
												.getTrueHeading()))
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
								.time(messageAndData.message().get()
										.getMessage().get().time())
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
								.build());
					} catch (RuntimeException e) {
						throw e;
					}
				}
			} else
				return empty();
		}
	};

}
