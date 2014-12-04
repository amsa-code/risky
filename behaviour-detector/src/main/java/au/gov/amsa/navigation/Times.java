package au.gov.amsa.navigation;

import static com.google.common.base.Optional.of;

import com.google.common.base.Optional;

public class Times {

	private final long time1;
	private final Optional<Long> time2;

	private Times(long time1, Optional<Long> time2) {
		this.time1 = time1;
		this.time2 = time2;
	}

	public Times(long time1) {
		this(time1, Optional.<Long> absent());
	}

	public Times(long time1, long time2) {
		this(time1, Optional.of(time2));
	}

	public long time1() {
		return time1;
	}

	public Optional<Long> time2() {
		return time2;
	}

	public Optional<Long> leastPositive() {
		// TODO unit test
		if (time1 >= 0 && time2.isPresent() && time2.get() >= 0)
			return of(Math.min(time1, time2.get()));
		else if (time1 >= 0)
			return of(time1);
		else if (time2.isPresent() && time2.get() >= 0) {
			return of(time2.get());
		} else
			return Optional.absent();
	}
}
