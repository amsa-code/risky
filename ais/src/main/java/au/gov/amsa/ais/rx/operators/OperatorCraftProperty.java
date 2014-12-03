package au.gov.amsa.ais.rx.operators;

import rx.Observable.Operator;
import rx.Subscriber;
import au.gov.amsa.ais.AisMessage;
import au.gov.amsa.ais.HasMmsi;
import au.gov.amsa.ais.Timestamped;
import au.gov.amsa.ais.message.AisPositionBExtended;
import au.gov.amsa.ais.message.AisShipStaticA;
import au.gov.amsa.ais.rx.CraftProperty;
import au.gov.amsa.ais.rx.CraftPropertyName;
import au.gov.amsa.ais.rx.Mmsi;

public class OperatorCraftProperty implements
		Operator<CraftProperty, Timestamped<? extends AisMessage>> {

	@Override
	public Subscriber<? super Timestamped<? extends AisMessage>> call(
			final Subscriber<? super CraftProperty> child) {

		return new Subscriber<Timestamped<? extends AisMessage>>(child) {

			@Override
			public void onCompleted() {
				if (!isUnsubscribed())
					child.onCompleted();
			}

			@Override
			public void onError(Throwable e) {
				if (!isUnsubscribed())
					child.onError(e);
			}

			@SuppressWarnings("unchecked")
			@Override
			public void onNext(Timestamped<? extends AisMessage> m) {
				if (m.message() instanceof AisShipStaticA) {
					handleShipStatic((Timestamped<AisShipStaticA>) m, child);
				} else if (m.message() instanceof AisPositionBExtended)
					handleAisPositionBExtended(
							(Timestamped<AisPositionBExtended>) m, child);
			}

			private void handleShipStatic(Timestamped<AisShipStaticA> m,
					Subscriber<? super CraftProperty> child) {
				handleProperty(child, m, CraftPropertyName.CALLSIGN, m
						.message().getCallsign());
				handleProperty(child, m, CraftPropertyName.DESTINATION, m
						.message().getDestination());
				handleProperty(child, m, CraftPropertyName.DIMENSION_A, m
						.message().getDimensionA());
				handleProperty(child, m, CraftPropertyName.DIMENSION_B, m
						.message().getDimensionB());
				handleProperty(child, m, CraftPropertyName.DIMENSION_C, m
						.message().getDimensionC());
				handleProperty(child, m, CraftPropertyName.DIMENSION_D, m
						.message().getDimensionD());
				handleProperty(child, m, CraftPropertyName.IMO_NUMBER, m
						.message().getImo());
				handleProperty(child, m, CraftPropertyName.LENGTH_METRES, m
						.message().getLengthMetres());
				handleProperty(child, m, CraftPropertyName.DRAUGHT_METRES, m
						.message().getMaximumPresentStaticDraughtMetres());
				handleProperty(child, m, CraftPropertyName.NAME, m.message()
						.getName());
				handleProperty(child, m, CraftPropertyName.SHIP_TYPE, m
						.message().getShipType());
				handleProperty(child, m, CraftPropertyName.WIDTH_METRES, m
						.message().getWidthMetres());
			}

			private void handleAisPositionBExtended(
					Timestamped<AisPositionBExtended> m,
					Subscriber<? super CraftProperty> child) {
				handleProperty(child, m, CraftPropertyName.DIMENSION_A, m
						.message().getDimensionA());
				handleProperty(child, m, CraftPropertyName.DIMENSION_B, m
						.message().getDimensionB());
				handleProperty(child, m, CraftPropertyName.DIMENSION_C, m
						.message().getDimensionC());
				handleProperty(child, m, CraftPropertyName.DIMENSION_D, m
						.message().getDimensionD());
				handleProperty(child, m, CraftPropertyName.LENGTH_METRES, m
						.message().getLengthMetres());
				handleProperty(child, m, CraftPropertyName.NAME, m.message()
						.getName());
				handleProperty(child, m, CraftPropertyName.SHIP_TYPE, m
						.message().getShipType());
				handleProperty(child, m, CraftPropertyName.WIDTH_METRES, m
						.message().getWidthMetres());
			}

			private <R extends AisMessage & HasMmsi> void  handleProperty(
					Subscriber<? super CraftProperty> child,
					Timestamped<R > m,
					CraftPropertyName name, Object value) {
				if (!isUnsubscribed() && value != null)
					child.onNext(new CraftProperty(new Mmsi(m.message()
							.getMmsi()), name, value.toString(), m.time()));
			}
		};
	}
}
