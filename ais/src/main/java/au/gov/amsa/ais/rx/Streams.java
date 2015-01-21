package au.gov.amsa.ais.rx;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import au.gov.amsa.ais.AisMessage;
import au.gov.amsa.ais.AisNmeaBuffer;
import au.gov.amsa.ais.AisNmeaMessage;
import au.gov.amsa.ais.AisParseException;
import au.gov.amsa.ais.LineAndTime;
import au.gov.amsa.ais.Timestamped;
import au.gov.amsa.ais.message.AisPosition;
import au.gov.amsa.ais.message.AisPositionA;
import au.gov.amsa.ais.message.AisShipStaticA;
import au.gov.amsa.risky.format.AisClass;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.NavigationalStatus;
import au.gov.amsa.streams.Strings;
import au.gov.amsa.util.nmea.NmeaMessage;
import au.gov.amsa.util.nmea.NmeaMessageParseException;
import au.gov.amsa.util.nmea.NmeaUtil;

import com.google.common.base.Optional;

public class Streams {

	public static final int BUFFER_SIZE = 100;

	private static Logger log = LoggerFactory.getLogger(Streams.class);

	public static Observable<String> connect(String host, int port) {
		return connect(new HostPort(host, port));
	}

	private static Observable<String> connect(HostPort socket) {
		return connectOnce(socket).timeout(1, TimeUnit.MINUTES).retry();
	}

	public static Observable<TimestampedAndLine<AisMessage>> connectAndExtract(
			String host, int port) {
		return extract(connect(host, port));
	}

	public static Observable<TimestampedAndLine<AisMessage>> extract(
			Observable<String> rawAisNmea) {
		return rawAisNmea.flatMap(toNmeaMessage())
				.flatMap(aggregateMultiLineNmea(BUFFER_SIZE))
				.map(TO_AIS_MESSAGE_AND_LINE);
	}

	public static Observable<Timestamped<AisMessage>> extractMessages(
			Observable<String> rawAisNmea) {
		return rawAisNmea.flatMap(toNmeaMessage())
				.flatMap(aggregateMultiLineNmea(BUFFER_SIZE))
				.flatMap(toAisMessage());
	}

	public static Observable<Fix> extractFixes(Observable<String> rawAisNmea) {
		return extractMessages(rawAisNmea).flatMap(TO_FIX);
	}

	// TODO unit test
	private static final Func1<Timestamped<AisMessage>, Observable<Fix>> TO_FIX = new Func1<Timestamped<AisMessage>, Observable<Fix>>() {

		@Override
		public Observable<Fix> call(Timestamped<AisMessage> m) {
			if (m.message() instanceof AisPosition) {
				AisPosition a = (AisPosition) m.message();
				if (a.getLatitude() == null || a.getLongitude() == null)
					return Observable.empty();
				else {
					final Optional<NavigationalStatus> nav;
					if (a instanceof AisPositionA) {
						AisPositionA p = (AisPositionA) a;
						if (p.getNavigationalStatus() == 15
								|| p.getNavigationalStatus() > NavigationalStatus
										.values().length - 1)
							nav = absent();
						else
							nav = of(NavigationalStatus.values()[p
									.getNavigationalStatus()]);
					} else
						nav = absent();

					final Optional<Float> sog;
					if (a.getSpeedOverGroundKnots() == null)
						sog = absent();
					else
						sog = of((a.getSpeedOverGroundKnots().floatValue()));
					final Optional<Float> cog;
					if (a.getCourseOverGround() == null)
						cog = absent();
					else
						cog = of((a.getCourseOverGround().floatValue()));
					final Optional<Float> heading;
					if (a.getTrueHeading() == null)
						heading = absent();
					else
						heading = of((a.getTrueHeading().floatValue()));

					final AisClass aisClass;
					if (a instanceof AisPositionA)
						aisClass = AisClass.A;
					else
						aisClass = AisClass.B;
					final Optional<Short> src;
					if (a.getSource() != null) {
						src = of((short) 1);
					} else
						src = absent();

					Fix f = new Fix(a.getMmsi(), a.getLatitude().floatValue(),
							a.getLongitude().floatValue(), m.time(),
							Optional.<Integer> absent(), src, nav, sog, cog,
							heading, aisClass);
					return Observable.just(f);
				}
			} else
				return Observable.empty();
		}

	};

	public static Observable<Observable<String>> nmeasFromGzip(
			Observable<File> files) {
		return files.map(new Func1<File, Observable<String>>() {

			@Override
			public Observable<String> call(File f) {
				return nmeaFromGzip(f.getPath()).subscribeOn(
						Schedulers.computation());
			}
		});
	}

	public static Observable<String> nmeaFromGzip(String filename) {
		InputStreamReader isr;
		try {
			isr = new InputStreamReader(new GZIPInputStream(
					new FileInputStream(filename)));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return Strings.split(Strings.from(isr), "\n");
	}

	public static void print(Observable<?> stream, final PrintStream out) {
		stream.subscribe(new Observer<Object>() {

			@Override
			public void onCompleted() {
			}

			@Override
			public void onError(Throwable e) {
				e.printStackTrace();
			}

			@Override
			public void onNext(Object line) {
				out.println(line);
			}
		});
	}

	public static void print(Observable<?> stream) {
		print(stream, System.out);
	}

	public static final Func1<String, Observable<NmeaMessage>> toNmeaMessage(
			final boolean logWarnings) {
		return new Func1<String, Observable<NmeaMessage>>() {

			@Override
			public Observable<NmeaMessage> call(String line) {
				try {
					return Observable.just(NmeaUtil.parseNmea(line));
				} catch (NmeaMessageParseException e) {
					if (logWarnings) {
						log.warn(e.getMessage());
						log.warn("LINE=" + line);
					}
					return Observable.empty();
				} catch (RuntimeException e) {
					if (logWarnings) {
						log.warn(e.getMessage());
						log.warn("LINE=" + line);
					}
					return Observable.empty();
				}
			}
		};
	}

	public static final Func1<String, Observable<LineAndTime>> toLineAndTime() {
		return new Func1<String, Observable<LineAndTime>>() {

			@Override
			public Observable<LineAndTime> call(String line) {
				try {
					Long t = NmeaUtil.parseNmea(line).getUnixTimeMillis();
					if (t == null)
						return Observable.empty();
					else
						return Observable.just(new LineAndTime(line, t));
				} catch (NmeaMessageParseException e) {
					return Observable.empty();
				} catch (RuntimeException e) {
					return Observable.empty();
				}

			}
		};
	}

	public static final Func1<String, Observable<NmeaMessage>> toNmeaMessage() {
		return toNmeaMessage(false);
	}

	public static class TimestampedAndLine<T extends AisMessage> {
		private final Optional<Timestamped<T>> message;
		private final String line;
		private final String error;

		public TimestampedAndLine(Optional<Timestamped<T>> message,
				String line, String error) {
			this.message = message;
			this.line = line;
			this.error = error;
		}

		public Optional<Timestamped<T>> getMessage() {
			return message;
		}

		public String getLine() {
			return line;
		}

		public String getError() {
			return error;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			if (message != null)
				builder.append("message=" + message);
			else
				builder.append("error=" + error);
			builder.append(", line=");
			builder.append(line);
			return builder.toString();
		}

	}

	public static final Func1<NmeaMessage, Observable<Timestamped<AisMessage>>> toAisMessage(
			final boolean logWarnings) {
		return new Func1<NmeaMessage, Observable<Timestamped<AisMessage>>>() {

			@Override
			public Observable<Timestamped<AisMessage>> call(NmeaMessage nmea) {
				try {
					AisNmeaMessage n = new AisNmeaMessage(nmea);
					Timestamped<AisMessage> m = n.getTimestampedMessage();
					if (m.message() instanceof AisShipStaticA) {
						AisShipStaticA s = (AisShipStaticA) m.message();
						if (logWarnings
								&& containsWeirdCharacters(s.getDestination())) {
							log.warn("weird destination '" + s.getDestination()
									+ "'");
							log.warn("line=" + n.getNmea().toLine());
						}
					}
					return Observable.just(m);
				} catch (AisParseException e) {
					return Observable.empty();
				}
			}

		};
	}

	public static final Func1<NmeaMessage, Observable<Timestamped<AisMessage>>> toAisMessage() {
		return toAisMessage(false);
	}

	private static boolean containsWeirdCharacters(String s) {
		if (s == null)
			return false;
		else {
			for (char ch : s.toCharArray()) {
				if (ch < 32 && ch != 10 && ch != 13) {
					log.warn("ch=" + (int) ch);
					return true;
				}
			}
		}
		return false;
	}

	public static final Func1<NmeaMessage, TimestampedAndLine<AisMessage>> TO_AIS_MESSAGE_AND_LINE = new Func1<NmeaMessage, TimestampedAndLine<AisMessage>>() {

		@Override
		public TimestampedAndLine<AisMessage> call(NmeaMessage nmea) {
			String line = nmea.toLine();
			try {
				AisNmeaMessage n = new AisNmeaMessage(nmea);
				return new TimestampedAndLine<AisMessage>(Optional.of(n
						.getTimestampedMessage(System.currentTimeMillis())),
						line, null);
			} catch (AisParseException e) {
				return new TimestampedAndLine<AisMessage>(
						Optional.<Timestamped<AisMessage>> absent(), line,
						e.getMessage());
			}
		}
	};

	public static final Func1<NmeaMessage, Observable<NmeaMessage>> aggregateMultiLineNmea(
			final int bufferSize) {
		return new Func1<NmeaMessage, Observable<NmeaMessage>>() {
			private final AisNmeaBuffer buffer = new AisNmeaBuffer(bufferSize);

			@Override
			public Observable<NmeaMessage> call(NmeaMessage nmea) {
				Optional<List<NmeaMessage>> list = buffer.add(nmea);
				if (!list.isPresent())
					return Observable.empty();
				else {
					Optional<NmeaMessage> concat = AisNmeaBuffer
							.concatenateMessages(list.get());
					if (concat.isPresent())
						return Observable.just(concat.get());
					else
						return Observable.empty();
				}
			}
		};
	}

	// private static Charset US_ASCII = Charset.forName("US-ASCII");

	public static Observable<String> connectOnce(final HostPort hostPort) {

		return Observable.create(new OnSubscribe<String>() {

			private Socket socket = null;

			private BufferedReader reader = null;

			@Override
			public void call(Subscriber<? super String> subscriber) {
				try {
					synchronized (this) {
						log.info("creating new socket");
						socket = createSocket(hostPort.getHost(),
								hostPort.getPort());
					}
					log.info("waiting one second before attempting connect");
					Thread.sleep(1000);
					InputStream is = socket.getInputStream();
					reader = new BufferedReader(new InputStreamReader(is,
							Charset.forName("UTF-8")));
					subscriber.add(createSubscription());
					while (!subscriber.isUnsubscribed()) {
						String line;
						try {
							line = reader.readLine();
						} catch (IOException e) {
							if (subscriber.isUnsubscribed())
								// most likely socket closed as a result of
								// unsubscribe so don't report as onError
								return;
							else
								throw e;
						}
						if (line != null)
							subscriber.onNext(line);
						else {
							// close stuff eagerly rather than waiting for
							// unsubscribe following onComplete
							cancel();
							subscriber.onCompleted();
						}
					}
				} catch (Exception e) {
					log.warn(e.getMessage(), e);
					cancel();
					subscriber.onError(e);
				}
			}

			private Subscription createSubscription() {
				return new Subscription() {

					private final AtomicBoolean subscribed = new AtomicBoolean(
							true);

					@Override
					public boolean isUnsubscribed() {
						return !subscribed.get();
					}

					@Override
					public void unsubscribe() {
						subscribed.set(false);
						cancel();
					}
				};
			}

			public void cancel() {
				log.info("cancelling socket read");
				// only allow socket to be closed once because a fresh
				// instance of Socket could have been opened to the same
				// host and port and we don't want to mess with it.
				synchronized (this) {
					if (socket != null) {
						if (reader != null)
							try {
								reader.close();
							} catch (IOException e) {
								// ignore
							}
						try {
							socket.close();
							// release memory (not much)
							socket = null;
						} catch (IOException e) {
							// ignore
						}
					}
				}
			}
		});
	}

	public static Observable<CraftProperties> aggregate(
			Observable<CraftProperty> source) {
		// TODO
		return null;
	}

	private static Socket createSocket(final String host, final int port) {
		try {
			return new Socket(host, port);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		print(connectAndExtract("mariweb", 9010).take(10));
	}
}
