package au.gov.amsa.util.rx;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func2;
import rx.observers.Subscribers;

public class OperatorMapEntries<A, B, C> implements Operator<C, Map<A, B>> {

	private static Logger log = Logger.getLogger(OperatorMapEntries.class);
	private final Func2<A, B, C> function;

	public OperatorMapEntries(Func2<A, B, C> function) {
		this.function = function;
	}

	@Override
	public Subscriber<? super Map<A, B>> call(final Subscriber<? super C> child) {
		final AtomicBoolean subscribed = new AtomicBoolean(true);
		child.add(new Subscription() {
			@Override
			public void unsubscribe() {
				subscribed.set(false);
			}

			@Override
			public boolean isUnsubscribed() {
				return !subscribed.get();
			}
		});
		Subscriber<Map<A, B>> parent = Subscribers
				.from(new Observer<Map<A, B>>() {

					@Override
					public void onCompleted() {
						child.onCompleted();
					}

					@Override
					public void onError(Throwable e) {
						child.onError(e);
					}

					@Override
					public void onNext(Map<A, B> map) {
						log.info("emitting map with " + map.size() + " entries");
						for (Entry<A, B> entry : map.entrySet()) {
							if (subscribed.get()) {
								child.onNext(function.call(entry.getKey(),
										entry.getValue()));
							} else
								return;
						}
					}
				});
		child.add(parent);
		return parent;
	}
}
