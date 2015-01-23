package au.gov.amsa.risky.format;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Func2;

public class Downsample implements Transformer<Fix, Fix> {

	private long maxTimeBetweenFixesMs;

	public Downsample(long maxTimeBetweenFixesMs) {
		this.maxTimeBetweenFixesMs = maxTimeBetweenFixesMs;
	}

	public static Downsample downsample(long duration, TimeUnit unit) {
		return new Downsample(unit.toMillis(duration));
	}

	@Override
	public Observable<Fix> call(Observable<Fix> fixes) {
		return fixes.scan(new Func2<Fix, Fix, Fix>() {
			@Override
			public Fix call(Fix latest, Fix fix) {
				if (fix.getTime() - latest.getTime() > maxTimeBetweenFixesMs)
					return fix;
				else
					return latest;
			}
		})
		// throw away repeats
				.distinct();
	}
}
