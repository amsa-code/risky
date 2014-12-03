package au.gov.amsa.ais.rx.operators;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import rx.Observable.Operator;
import rx.Subscriber;
import au.gov.amsa.ais.rx.CraftProperties;
import au.gov.amsa.ais.rx.CraftProperty;
import au.gov.amsa.ais.rx.Mmsi;

public class OperatorCraftPropertAggregator implements
		Operator<Map<Mmsi, CraftProperties>, CraftProperty> {

	private final ConcurrentHashMap<Mmsi, CraftProperties> map = new ConcurrentHashMap<>();

	@Override
	public Subscriber<? super CraftProperty> call(
			final Subscriber<? super Map<Mmsi, CraftProperties>> child) {
		// all subscribers insert into same map to enable concurrency

		return new Subscriber<CraftProperty>(child) {

			@Override
			public void onCompleted() {
				if (isUnsubscribed())
					return;
				else
					child.onNext(map);
			}

			@Override
			public void onError(Throwable e) {
				if (!isUnsubscribed())
					child.onError(e);
			}

			@Override
			public void onNext(CraftProperty p) {
				// non-blocking algorithm

				// put if absent
				map.putIfAbsent(p.getMmsi(), new CraftProperties(p.getMmsi()));

				// attempt to update
				while (true) {
					CraftProperties v = map.get(p.getMmsi());
					if (map.replace(p.getMmsi(), v, v.add(p)))
						return;
				}
			}
		};
	}
}
