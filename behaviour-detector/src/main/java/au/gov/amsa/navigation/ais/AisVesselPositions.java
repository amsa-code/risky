package au.gov.amsa.navigation.ais;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;
import static rx.Observable.empty;
import static rx.Observable.just;

import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.functions.Func1;
import au.gov.amsa.ais.AisMessage;
import au.gov.amsa.ais.Timestamped;
import au.gov.amsa.ais.message.AisPosition;
import au.gov.amsa.ais.message.AisPositionA;
import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.navigation.Mmsi;
import au.gov.amsa.navigation.VesselClass;
import au.gov.amsa.navigation.VesselPosition;

import com.google.common.base.Optional;

public class AisVesselPositions {

	private static final Logger log = LoggerFactory
			.getLogger(AisVesselPositions.class);

	public static Observable<VesselPosition> positions(Observable<String> nmea) {
		return Streams
				.extractMessages(nmea)
				// aggregate ship data with the message
				.scan(new AisMessageAndVesselData(),
						AisMessageAndVesselData.aggregate)
				// positions only
				.filter(isPosition)
				// convert to vessel positions
				.flatMap(toVesselPosition);
	}

	public static Observable<Timestamped<AisMessage>> sortByTime(
			Observable<Timestamped<AisMessage>> source) {
		Comparator<Timestamped<AisMessage>> comparator = new Comparator<Timestamped<AisMessage>>() {
			@Override
			public int compare(Timestamped<AisMessage> t1,
					Timestamped<AisMessage> t2) {
				return ((Long) t1.time()).compareTo(t2.time());
			}
		};
		return source
		// sort by time
				.lift(new SortOperator<Timestamped<AisMessage>>(comparator,
						20000000));
	}

	public static Observable<VesselPosition> positionsSortedByTime(
			Observable<String> nmea) {
		return sortByTime(Streams.extractMessages(nmea))
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
					&& m.message().get().message() instanceof AisPosition;
		}
	};

	private static final Func1<AisMessageAndVesselData, Observable<VesselPosition>> toVesselPosition = new Func1<AisMessageAndVesselData, Observable<VesselPosition>>() {
		@Override
		public Observable<VesselPosition> call(
				AisMessageAndVesselData messageAndData) {

			if (messageAndData.message().isPresent()
					&& messageAndData.message().get().message() instanceof AisPosition) {
				AisPosition p = (AisPosition) messageAndData.message().get()
						.message();
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

					boolean isAtAnchor;
					if (p instanceof AisPositionA)
						isAtAnchor = ((AisPositionA) p).isAtAnchor();
					else
						isAtAnchor = false;
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
								.lat(p.getLatitude()).lon(p.getLongitude())
								.id(id).lengthMetres(lengthMetres)
								.time(messageAndData.message().get().time())
								.widthMetres(widthMetres).cls(cls)
								.atAnchor(isAtAnchor).build());
					} catch (RuntimeException e) {
						throw e;
					}
				}
			} else
				return empty();
		}
	};

}
