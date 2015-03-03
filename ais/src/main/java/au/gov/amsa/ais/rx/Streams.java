package au.gov.amsa.ais.rx;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Transformer;
import rx.Observer;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import au.gov.amsa.ais.AisMessage;
import au.gov.amsa.ais.AisNmeaBuffer;
import au.gov.amsa.ais.AisNmeaMessage;
import au.gov.amsa.ais.AisParseException;
import au.gov.amsa.ais.LineAndTime;
import au.gov.amsa.ais.Timestamped;
import au.gov.amsa.ais.message.AisPosition;
import au.gov.amsa.ais.message.AisPositionA;
import au.gov.amsa.risky.format.AisClass;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.BinaryFixesWriter;
import au.gov.amsa.risky.format.Downsample;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.NavigationalStatus;
import au.gov.amsa.streams.Ob;
import au.gov.amsa.streams.Strings;
import au.gov.amsa.util.Files;
import au.gov.amsa.util.nmea.NmeaMessage;
import au.gov.amsa.util.nmea.NmeaMessageParseException;
import au.gov.amsa.util.nmea.NmeaUtil;

import com.github.davidmoten.rx.Functions;
import com.github.davidmoten.rx.operators.OperatorUnsubscribeEagerly;
import com.github.davidmoten.rx.slf4j.Logging;
import com.google.common.base.Optional;

public class Streams {

	public static final int BUFFER_SIZE = 100;
	private static final Charset UTF8 = Charset.forName("UTF-8");

	private static Logger log = LoggerFactory.getLogger(Streams.class);

	public static Observable<String> connect(String host, int port) {
		return connect(new HostPort(host, port));
	}

	private static Observable<String> connect(HostPort socket) {
		return connectOnce(socket).timeout(1, TimeUnit.MINUTES).retry();
	}

	public static Observable<TimestampedAndLine<AisMessage>> connectAndExtract(String host, int port) {
		return extract(connect(host, port));
	}

	public static Observable<TimestampedAndLine<AisMessage>> extract(Observable<String> rawAisNmea) {
		return rawAisNmea
		// parse nmea
		        .map(LINE_TO_NMEA_MESSAGE)
		        // if error filter out
		        .compose(Streams.<NmeaMessage> valueIfPresent())
		        // aggregate multi line nmea
		        .map(aggregateMultiLineNmea(BUFFER_SIZE))
		        // if error filter out
		        .compose(Streams.<NmeaMessage> valueIfPresent())
		        // parse ais message and include line
		        .map(TO_AIS_MESSAGE_AND_LINE);
	}

	public static Observable<Timestamped<AisMessage>> extractMessages(Observable<String> rawAisNmea) {
		return rawAisNmea.map(LINE_TO_NMEA_MESSAGE).compose(Streams.<NmeaMessage> valueIfPresent())
		        .map(aggregateMultiLineNmea(BUFFER_SIZE))
		        .compose(Streams.<NmeaMessage> valueIfPresent()).map(TO_AIS_MESSAGE)
		        .compose(Streams.<Timestamped<AisMessage>> valueIfPresent());

	}

	public static <T> Func1<Optional<T>, Boolean> isPresent() {
		return new Func1<Optional<T>, Boolean>() {
			@Override
			public Boolean call(Optional<T> value) {
				return value.isPresent();
			}
		};
	}

	public static <T> Func1<Optional<T>, T> toValue() {
		return new Func1<Optional<T>, T>() {
			@Override
			public T call(Optional<T> value) {
				return value.get();
			}
		};
	}

	public static <T> Transformer<Optional<T>, T> valueIfPresent() {
		return new Transformer<Optional<T>, T>() {

			@Override
			public Observable<T> call(Observable<Optional<T>> o) {
				return o.filter(Streams.<T> isPresent()).map(Streams.<T> toValue());
			}
		};
	}

	public static Observable<Fix> extractFixes(Observable<String> rawAisNmea) {
		return extractMessages(rawAisNmea).flatMap(TO_FIX);
	}

	private static final Func1<Timestamped<AisMessage>, Observable<Fix>> TO_FIX = new Func1<Timestamped<AisMessage>, Observable<Fix>>() {

		@Override
		public Observable<Fix> call(Timestamped<AisMessage> m) {
			try {
				if (m.message() instanceof AisPosition) {
					AisPosition a = (AisPosition) m.message();
					if (a.getLatitude() == null || a.getLongitude() == null
					        || a.getLatitude() < -90 || a.getLatitude() > 90
					        || a.getLongitude() < -180 || a.getLongitude() > 180)
						return Observable.empty();
					else {
						final Optional<NavigationalStatus> nav;
						if (a instanceof AisPositionA) {
							AisPositionA p = (AisPositionA) a;
							nav = of(NavigationalStatus.values()[p.getNavigationalStatus()
							        .ordinal()]);
						} else
							nav = absent();

						final Optional<Float> sog;
						if (a.getSpeedOverGroundKnots() == null)
							sog = absent();
						else
							sog = of((a.getSpeedOverGroundKnots().floatValue()));
						final Optional<Float> cog;
						if (a.getCourseOverGround() == null || a.getCourseOverGround() >= 360
						        || a.getCourseOverGround() < 0)
							cog = absent();
						else
							cog = of((a.getCourseOverGround().floatValue()));
						final Optional<Float> heading;
						if (a.getTrueHeading() == null || a.getTrueHeading() >= 360
						        || a.getTrueHeading() < 0)
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
							// TODO decode
							src = of((short) BinaryFixes.SOURCE_PRESENT_BUT_UNKNOWN);
						} else
							src = absent();

						// TODO latency
						Optional<Integer> latency = absent();

						Fix f = new Fix(a.getMmsi(), a.getLatitude().floatValue(), a.getLongitude()
						        .floatValue(), m.time(), latency, src, nav, sog, cog, heading,
						        aisClass);
						return Observable.just(f);
					}
				} else
					return Observable.empty();
			} catch (RuntimeException e) {
				log.warn(e.getMessage(), e);
				return Observable.empty();
			}
		}

	};

	public static Observable<String> nmeaFrom(final File file) {
		return Ob.using(new Func0<InputStream>() {

			@Override
			public InputStream call() {
				try {
					return new FileInputStream(file);
				} catch (FileNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		}, new Func1<InputStream, Observable<String>>() {

			@Override
			public Observable<String> call(InputStream is) {
				return nmeaFrom(is);
			}
		}, new Action1<InputStream>() {

			@Override
			public void call(InputStream is) {
				try {
					is.close();
				} catch (IOException e) {
					// don't care
				}
			}
		}, true);
	}

	public static Observable<String> nmeaFrom(InputStream is) {
		return Strings.split(Strings.from(new InputStreamReader(is, UTF8)), "\n");
	}

	public static Observable<Observable<String>> nmeasFromGzip(Observable<File> files) {
		return files.map(new Func1<File, Observable<String>>() {

			@Override
			public Observable<String> call(File f) {
				return nmeaFromGzip(f.getPath()).subscribeOn(Schedulers.computation());
			}
		});
	}

	public static Observable<String> nmeaFromGzip(String filename) {
		return nmeaFromGzip(new File(filename));
	}

	public static Observable<String> nmeaFromGzip(final File file) {

		Func0<Reader> resourceFactory = new Func0<Reader>() {

			@Override
			public Reader call() {
				try {
					return new InputStreamReader(new GZIPInputStream(new FileInputStream(file)),
					        UTF8);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		};
		Func1<Reader, Observable<String>> observableFactory = new Func1<Reader, Observable<String>>() {

			@Override
			public Observable<String> call(Reader reader) {
				// return StringObservable.split(Strings.from(reader), "\n");
				return Strings.split(Strings.from(reader), "\n");
			}
		};
		Action1<Reader> disposeAction = new Action1<Reader>() {

			@Override
			public void call(Reader reader) {
				try {
					reader.close();
				} catch (IOException e) {
					// ignore
				}
			}
		};
		return Ob.using(resourceFactory, observableFactory, disposeAction, true);
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

	public static final Func1<String, Optional<NmeaMessage>> LINE_TO_NMEA_MESSAGE = new Func1<String, Optional<NmeaMessage>>() {

		@Override
		public Optional<NmeaMessage> call(String line) {
			try {
				return Optional.of(NmeaUtil.parseNmea(line));
			} catch (RuntimeException e) {
				return Optional.absent();
			}
		}

	};

	// public static final Func1<String, Observable<NmeaMessage>>
	// toNmeaMessage() {
	// return toNmeaMessage(false);
	// }
	//
	// public static final Func1<String, Observable<NmeaMessage>> toNmeaMessage(
	// final boolean logWarnings) {
	// return new Func1<String, Observable<NmeaMessage>>() {
	//
	// @Override
	// public Observable<NmeaMessage> call(String line) {
	// try {
	// return Observable.just(NmeaUtil.parseNmea(line));
	// } catch (NmeaMessageParseException e) {
	// if (logWarnings) {
	// log.warn(e.getMessage());
	// log.warn("LINE=" + line);
	// }
	// return Observable.empty();
	// } catch (RuntimeException e) {
	// if (logWarnings) {
	// log.warn(e.getMessage());
	// log.warn("LINE=" + line);
	// }
	// return Observable.empty();
	// }
	// }
	// };
	// }

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

	public static class TimestampedAndLine<T extends AisMessage> {
		private final Optional<Timestamped<T>> message;
		private final String line;
		private final String error;

		public TimestampedAndLine(Optional<Timestamped<T>> message, String line, String error) {
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

	// public static final Func1<NmeaMessage,
	// Observable<Timestamped<AisMessage>>> toAisMessage(
	// final boolean logWarnings) {
	// return new Func1<NmeaMessage, Observable<Timestamped<AisMessage>>>() {
	//
	// @Override
	// public Observable<Timestamped<AisMessage>> call(NmeaMessage nmea) {
	// try {
	// AisNmeaMessage n = new AisNmeaMessage(nmea);
	// Timestamped<AisMessage> m = n.getTimestampedMessage();
	// if (m.message() instanceof AisShipStaticA) {
	// AisShipStaticA s = (AisShipStaticA) m.message();
	// if (logWarnings
	// && containsWeirdCharacters(s.getDestination())) {
	// log.warn("weird destination '" + s.getDestination()
	// + "'");
	// log.warn("line=" + n.getNmea().toLine());
	// }
	// }
	// return Observable.just(m);
	// } catch (AisParseException e) {
	// return Observable.empty();
	// }
	// }
	//
	// };
	// }

	public static final Func1<NmeaMessage, Optional<Timestamped<AisMessage>>> TO_AIS_MESSAGE = new Func1<NmeaMessage, Optional<Timestamped<AisMessage>>>() {

		@Override
		public Optional<Timestamped<AisMessage>> call(NmeaMessage nmea) {
			try {
				AisNmeaMessage n = new AisNmeaMessage(nmea);
				Timestamped<AisMessage> m = n.getTimestampedMessage();
				// if (m.message() instanceof AisShipStaticA) {
				// AisShipStaticA s = (AisShipStaticA) m.message();
				// if (logWarnings
				// && containsWeirdCharacters(s.getDestination())) {
				// log.warn("weird destination '" + s.getDestination()
				// + "'");
				// log.warn("line=" + n.getNmea().toLine());
				// }
				// }
				return Optional.of(m);
			} catch (AisParseException e) {
				return Optional.absent();
			}
		}
	};

	// public static final Func1<NmeaMessage,
	// Observable<Timestamped<AisMessage>>> toAisMessage() {
	// return toAisMessage(false);
	// }

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
				        .getTimestampedMessage(System.currentTimeMillis())), line, null);
			} catch (AisParseException e) {
				return new TimestampedAndLine<AisMessage>(
				        Optional.<Timestamped<AisMessage>> absent(), line, e.getMessage());
			} catch (RuntimeException e) {
				log.warn(e.getMessage(), e);
				throw e;
			}
		}
	};

	// public static final Func1<NmeaMessage, Observable<NmeaMessage>>
	// aggregateMultiLineNmea(
	// final int bufferSize) {
	// return new Func1<NmeaMessage, Observable<NmeaMessage>>() {
	// private final AisNmeaBuffer buffer = new AisNmeaBuffer(bufferSize);
	//
	// @Override
	// public Observable<NmeaMessage> call(NmeaMessage nmea) {
	// try {
	// Optional<List<NmeaMessage>> list = buffer.add(nmea);
	// if (!list.isPresent())
	// return Observable.empty();
	// else {
	// Optional<NmeaMessage> concat = AisNmeaBuffer
	// .concatenateMessages(list.get());
	// if (concat.isPresent())
	// return Observable.just(concat.get());
	// else
	// return Observable.empty();
	// }
	// } catch (RuntimeException e) {
	// log.warn(e.getMessage(), e);
	// return Observable.empty();
	// }
	// }
	// };
	// }

	public static final Func1<NmeaMessage, Optional<NmeaMessage>> aggregateMultiLineNmea(
	        final int bufferSize) {
		return new Func1<NmeaMessage, Optional<NmeaMessage>>() {

			private final AisNmeaBuffer buffer = new AisNmeaBuffer(bufferSize);

			@Override
			public Optional<NmeaMessage> call(NmeaMessage nmea) {

				try {
					Optional<List<NmeaMessage>> list = buffer.add(nmea);
					if (!list.isPresent())
						return absent();
					else {
						Optional<NmeaMessage> concat = AisNmeaBuffer
						        .concatenateMessages(list.get());
						if (concat.isPresent())
							return of(concat.get());
						else
							return absent();
					}
				} catch (RuntimeException e) {
					log.warn(e.getMessage(), e);
					return absent();
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
						socket = createSocket(hostPort.getHost(), hostPort.getPort());
					}
					log.info("waiting one second before attempting connect");
					Thread.sleep(1000);
					InputStream is = socket.getInputStream();
					reader = new BufferedReader(new InputStreamReader(is, UTF8));
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

					private final AtomicBoolean subscribed = new AtomicBoolean(true);

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

	private static Socket createSocket(final String host, final int port) {
		try {
			return new Socket(host, port);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Func1<File, Observable<Integer>> extractFixesFromNmeaGzAndAppendToFile(
	        final int linesPerProcessor, final Scheduler scheduler,
	        final Func1<Fix, String> fileMapper, final int writeBufferSize) {
		return new Func1<File, Observable<Integer>>() {
			@Override
			public Observable<Integer> call(File file) {
				return Streams.nmeaFromGzip(file.getAbsolutePath())
				//
				        .buffer(linesPerProcessor)
				        // parse the messages asynchronously using computation
				        // scheduler
				        .flatMap(new Func1<List<String>, Observable<Integer>>() {
					        @Override
					        public Observable<Integer> call(List<String> list) {
						        return BinaryFixesWriter
						                .writeFixes(fileMapper,
						                        Streams.extractFixes(Observable.from(list)),
						                        writeBufferSize, false)
						                // total counts
						                .reduce(0, new Func2<Integer, List<Fix>, Integer>() {
							                @Override
							                public Integer call(Integer count, List<Fix> fixes) {
								                return count + fixes.size();
							                }
						                })
						                // do async
						                .subscribeOn(scheduler);
					        }
				        });
			}
		};
	}

	public static Observable<Integer> writeFixesFromNmeaGz(File input, Pattern inputPattern,
	        File output, int logEvery, int writeBufferSize, Scheduler scheduler,
	        int linesPerProcessor, long downSampleIntervalMs, Func1<Fix, String> fileMapper) {

		Observable<File> files = Observable.from(Files.find(input, inputPattern));

		deleteDirectory(output);

		return files
		// log the filename
		        .lift(Logging.<File> logger().showCount().showValue().log())
		        // extract fixes
		        .flatMap(
		                extractFixesFromNmeaGzAndAppendToFile(linesPerProcessor, scheduler, fileMapper,
		                        writeBufferSize))
		        // count number written fixes
		        .scan(0, new Func2<Integer, Integer, Integer>() {
			        @Override
			        public Integer call(Integer a, Integer b) {
				        return a + b;
			        }
		        })
		        // log
		        .lift(Logging.<Integer> logger().showCount().showMemory()
		                .showRateSince("rate", 5000).every(logEvery).log())
		        // get the final count
		        .last()
		        // on completion of writing fixes, sort the track files and emit
		        // the count of files
		        .concatWith(sortOutputFilesByTime(output, downSampleIntervalMs));
	}

	private static void deleteDirectory(File output) {
		try {
			FileUtils.deleteDirectory(output);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static Observable<Integer> sortOutputFilesByTime(File output,
	        final long downSampleIntervalMs) {
		return Observable.just(1).onBackpressureBuffer()
		// use output lazily
		        .map(Functions.constant(output))
		        // log
		        .lift(Logging.<File> logger().prefix("sorting ").log())
		        // find the track files
		        .flatMap(findTrackFiles())
		        // sort the fixes in each one and rewrite
		        .flatMap(sortFileFixes(downSampleIntervalMs))
		        // return the count
		        .count();
	}

	private static Func1<File, Observable<Integer>> sortFileFixes(final long downSampleIntervalMs) {
		return new Func1<File, Observable<Integer>>() {
			@Override
			public Observable<Integer> call(final File file) {
				return BinaryFixes.from(file)
				// ensure file is closed in case we want to rewrite
				// downstream
				        .lift(OperatorUnsubscribeEagerly.<Fix> instance())
				        // to list
				        .toList()
				        // sort each list
				        .map(sortFixes())
				        // flatten
				        .flatMapIterable(Functions.<List<Fix>> identity())
				        // downsample the sorted fixes
				        .compose(
				                Downsample.minTimeStep(downSampleIntervalMs, TimeUnit.MILLISECONDS))
				        // make into a list again
				        .toList()
				        // replace the file with sorted fixes
				        .doOnNext(writeFixes(file))
				        // count the fixes
				        .count();
			}

		};
	}

	private static Func1<File, Observable<File>> findTrackFiles() {
		return new Func1<File, Observable<File>>() {
			@Override
			public Observable<File> call(File output) {
				return Observable.from(Files.find(output, Pattern.compile("\\d+\\.track")));
			}
		};
	}

	private static Action1<List<Fix>> writeFixes(final File file) {
		return new Action1<List<Fix>>() {
			@Override
			public void call(List<Fix> list) {
				BinaryFixesWriter.writeFixes(list, file, false, false);
			}
		};
	}

	private static Func1<List<Fix>, List<Fix>> sortFixes() {
		return new Func1<List<Fix>, List<Fix>>() {

			@Override
			public List<Fix> call(List<Fix> list) {
				ArrayList<Fix> temp = new ArrayList<Fix>(list);
				Collections.sort(temp, FIX_ORDER_BY_TIME);
				return temp;
			}
		};
	}

	private static final Comparator<Fix> FIX_ORDER_BY_TIME = new Comparator<Fix>() {
		@Override
		public int compare(Fix a, Fix b) {
			return ((Long) a.getTime()).compareTo(b.getTime());
		}
	};

	public static void main(String[] args) {
		// print(connectAndExtract("mariweb", 9010).take(10));
		long t = System.currentTimeMillis();
		int count = extractMessages(nmeaFrom(new File("/home/dxm/temp/NMEA_ITU_20150101"))).count()
		        .toBlocking().single();
		double rate = count * 1000.0 / (System.currentTimeMillis() - t);
		System.out.println("read " + count + " records, rateMsgsPerSecond = " + rate);
	}
}
