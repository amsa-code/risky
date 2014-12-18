package au.gov.amsa.streams;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import com.github.davidmoten.rx.Functions;
import com.google.common.collect.EvictingQueue;

public final class Logging {

	public static <T> Builder<T> builder() {
		return new Builder<T>();
	}

	public static class Builder<T> {

		private final List<Transition<T>> transitions = new ArrayList<Transition<T>>();

		public Builder<T> count(final String prefix) {
			final AtomicLong count = new AtomicLong();
			Func1<Func1<T, String>, Func1<T, String>> message = new Func1<Func1<T, String>, Func1<T, String>>() {
				@Override
				public Func1<T, String> call(final Func1<T, String> f) {
					return new Func1<T, String>() {

						@Override
						public String call(T t) {
							StringBuilder line = new StringBuilder();
							line.append(prefix);
							line.append(count.get());
							String value = f.call(t);
							if (value.length() > 0) {
								line.append(", ");
								line.append(value);
							}
							return line.toString();
						}
					};
				}
			};
			Func1<Action1<T>, Action1<T>> action = new Func1<Action1<T>, Action1<T>>() {

				@Override
				public Action1<T> call(final Action1<T> action) {
					return new Action1<T>() {

						@Override
						public void call(T t) {
							count.incrementAndGet();
							action.call(t);
						}
					};
				}
			};
			transitions.add(new Transition<T>(action, message));
			return this;
		}

		public Builder<T> every(final long every) {
			final AtomicLong count = new AtomicLong();
			Func1<Func1<T, String>, Func1<T, String>> message = Functions
					.identity();
			Func1<Action1<T>, Action1<T>> action = new Func1<Action1<T>, Action1<T>>() {
				@Override
				public Action1<T> call(final Action1<T> action) {
					return new Action1<T>() {
						@Override
						public void call(T t) {
							if (count.incrementAndGet() % every == 0)
								action.call(t);
						}
					};
				}
			};
			transitions.add(new Transition<T>(action, message));
			return this;
		}

		public Builder<T> value(final String prefix) {
			Func1<Func1<T, String>, Func1<T, String>> message = new Func1<Func1<T, String>, Func1<T, String>>() {
				@Override
				public Func1<T, String> call(final Func1<T, String> f) {
					return new Func1<T, String>() {

						@Override
						public String call(T t) {
							StringBuilder line = new StringBuilder();
							line.append(prefix);
							line.append(String.valueOf(t));
							return appendValue(f, t, line);
						}

					};
				}
			};
			Func1<Action1<T>, Action1<T>> action = Functions.identity();
			transitions.add(new Transition<T>(action, message));
			return this;
		}

		public Builder<T> memory() {
			Func1<Func1<T, String>, Func1<T, String>> message = new Func1<Func1<T, String>, Func1<T, String>>() {
				@Override
				public Func1<T, String> call(final Func1<T, String> f) {
					return new Func1<T, String>() {

						@Override
						public String call(T t) {
							StringBuilder line = new StringBuilder();
							line.append(memoryUsage());
							return appendValue(f, t, line);
						}
					};
				}
			};
			Func1<Action1<T>, Action1<T>> action = Functions.identity();
			transitions.add(new Transition<T>(action, message));
			return this;
		}

		public Builder<T> rate(final String prefix, final int over,
				final TimeUnit per) {
			final DecimalFormat df = new DecimalFormat("#0.000");
			final EvictingQueue<Long> times = EvictingQueue.create(over);
			Func1<Func1<T, String>, Func1<T, String>> message = new Func1<Func1<T, String>, Func1<T, String>>() {
				@Override
				public Func1<T, String> call(final Func1<T, String> f) {
					return new Func1<T, String>() {

						@Override
						public String call(T t) {
							long now = System.currentTimeMillis();
							long firstTime = times.peek();
							StringBuilder line = new StringBuilder();
							if (firstTime < now) {
								double rate = times.size()
										/ (double) (now - firstTime)
										* per.toMillis(1);
								line.append(prefix);
								line.append(df.format(rate));
							}
							return appendValue(f, t, line);
						}
					};
				}
			};
			Func1<Action1<T>, Action1<T>> action = new Func1<Action1<T>, Action1<T>>() {
				@Override
				public Action1<T> call(final Action1<T> action) {
					return new Action1<T>() {
						@Override
						public void call(T t) {
							times.add(System.currentTimeMillis());
							action.call(t);
						}
					};
				}
			};
			transitions.add(new Transition<T>(action, message));
			return this;
		}

		public Action1<T> build() {
			final AtomicReference<Func1<T, String>> messageRef = new AtomicReference<Func1<T, String>>();
			Action1<T> action = new Action1<T>() {
				@Override
				public void call(T t) {
					System.out.println(messageRef.get().call(t));
				}
			};
			Func1<T, String> message = blank();
			for (int i = transitions.size() - 1; i >= 0; i--) {
				Transition<T> transition = transitions.get(i);
				action = transition.action.call(action);
				message = transition.message.call(message);
			}
			messageRef.set(message);
			final Action1<T> start = action;
			return new Action1<T>() {
				@Override
				public void call(T t) {
					start.call(t);
				}
			};
		}
	}

	private static <T> Func1<T, String> blank() {
		return new Func1<T, String>() {
			@Override
			public String call(T t) {
				return "";
			}
		};
	}

	private static class Transition<T> {
		final Func1<Action1<T>, Action1<T>> action;
		final Func1<Func1<T, String>, Func1<T, String>> message;

		Transition(Func1<Action1<T>, Action1<T>> action,
				Func1<Func1<T, String>, Func1<T, String>> messsage) {

			this.action = action;
			this.message = messsage;
		}
	}

	private static String memoryUsage() {
		StringBuilder s = new StringBuilder();
		Runtime r = Runtime.getRuntime();
		long mem = r.totalMemory() - r.freeMemory();
		s.append("usedMem=");
		s.append(new DecimalFormat("0").format(mem / 1000000.0));
		s.append("MB, percentMax=");
		s.append(new DecimalFormat("0.0").format((double) mem / r.maxMemory()
				* 100));
		s.append(", max=");
		s.append(new DecimalFormat("0").format(r.maxMemory() / 1000000.0));
		s.append("MB");
		return s.toString();
	}

	private static <T> String appendValue(final Func1<T, String> f, T t,
			StringBuilder line) {
		String value = f.call(t);
		if (value.length() > 0) {
			line.append(", ");
			line.append(value);
		}
		return line.toString();
	}

	public static void main(String[] args) {
		Observable
				.range(1, 1000)
				.doOnNext(new Action1<Integer>() {

					@Override
					public void call(Integer n) {
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
						}
					}
				})
				.doOnNext(
						Logging.builder()
								.rate("msgsPerSecond=", 100, TimeUnit.SECONDS)
								.count("count=").count("n=").every(100)
								.count("mod100=").value("v=").memory().build())
				.subscribe();
	}
}
