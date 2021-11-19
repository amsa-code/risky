package au.gov.amsa.ais.rx;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.davidmoten.rx.Checked;
import com.github.davidmoten.rx.slf4j.Logging;

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
import au.gov.amsa.risky.format.BinaryFixesFormat;
import au.gov.amsa.risky.format.BinaryFixesWriter;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.FixImpl;
import au.gov.amsa.risky.format.NavigationalStatus;
import au.gov.amsa.streams.Strings;
import au.gov.amsa.util.Files;
import au.gov.amsa.util.nmea.NmeaMessage;
import au.gov.amsa.util.nmea.NmeaMessageParseException;
import au.gov.amsa.util.nmea.NmeaUtil;
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

    public static Observable<TimestampedAndLine<AisMessage>> connectAndExtract(String host,
            int port) {
        return extract(connect(host, port));
    }

    public static Observable<TimestampedAndLine<AisMessage>> extract(
            Observable<String> rawAisNmea) {
        return rawAisNmea
                // parse nmea
                .map(Streams.LINE_TO_NMEA_MESSAGE)
                // if error filter out
                .compose(Streams.<NmeaMessage> valueIfPresent())
                // aggregate multi line nmea
                .compose(aggregateMultiLineNmea(BUFFER_SIZE))
                // parse ais message and include line
                .map(TO_AIS_MESSAGE_AND_LINE);
    }

    public static Observable<TimestampedAndLines<AisMessage>> extractWithLines(
            Observable<String> rawAisNmea) {
        return rawAisNmea
                // parse nmea
                .map(LINE_TO_NMEA_MESSAGE)
                // if error filter out
                .compose(Streams.<NmeaMessage> valueIfPresent())
                // aggregate multi line nmea
                .compose(addToBuffer(BUFFER_SIZE))
                // parse ais message and include line
                .map(TO_AIS_MESSAGE_AND_LINES);
    }

    public static Observable<Timestamped<AisMessage>> extractMessages(
            Observable<String> rawAisNmea) {
        return rawAisNmea.map(LINE_TO_NMEA_MESSAGE)
                //
                .compose(Streams.<NmeaMessage> valueIfPresent())
                //
                .compose(aggregateMultiLineNmea(BUFFER_SIZE))
                //
                .map(TO_AIS_MESSAGE)
                //
                .compose(Streams.<Timestamped<AisMessage>> valueIfPresent());
    }

    public static <T> Func1<Optional<T>, Boolean> isPresent() {
        return x -> x.isPresent();
    }

    public static <T> Func1<Optional<T>, T> toValue() {
        return x -> x.get();

    }

    public static <T> Transformer<Optional<T>, T> valueIfPresent() {
        return o -> o.filter(Streams.<T> isPresent()).map(Streams.<T> toValue());
    }

    public static Observable<Fix> extractFixes(Observable<String> rawAisNmea) {
        return extractMessages(rawAisNmea).flatMap(TO_FIX, 1);
    }

    private static final Func1<Timestamped<AisMessage>, Observable<Fix>> TO_FIX = m -> {
        try {
            if (m.message() instanceof AisPosition) {
                AisPosition a = (AisPosition) m.message();
                if (a.getLatitude() == null || a.getLongitude() == null || a.getLatitude() < -90
                        || a.getLatitude() > 90 || a.getLongitude() < -180
                        || a.getLongitude() > 180)
                    return Observable.empty();
                else {
                    Optional<NavigationalStatus> nav;
                    if (a instanceof AisPositionA) {
                        AisPositionA p = (AisPositionA) a;
                        nav = of(NavigationalStatus.values()[p.getNavigationalStatus().ordinal()]);
                    } else
                        nav = empty();

                    Optional<Float> sog;
                    if (a.getSpeedOverGroundKnots() == null)
                        sog = empty();
                    else
                        sog = of((a.getSpeedOverGroundKnots().floatValue()));
                    Optional<Float> cog;
                    if (a.getCourseOverGround() == null || a.getCourseOverGround() >= 360
                            || a.getCourseOverGround() < 0)
                        cog = empty();
                    else
                        cog = of((a.getCourseOverGround().floatValue()));
                    Optional<Float> heading;
                    if (a.getTrueHeading() == null || a.getTrueHeading() >= 360
                            || a.getTrueHeading() < 0)
                        heading = empty();
                    else
                        heading = of((a.getTrueHeading().floatValue()));

                    AisClass aisClass;
                    if (a instanceof AisPositionA)
                        aisClass = AisClass.A;
                    else
                        aisClass = AisClass.B;
                    Optional<Short> src;
                    if (a.getSource() != null) {
                        // TODO decode
                        src = of((short) BinaryFixes.SOURCE_PRESENT_BUT_UNKNOWN);
                    } else
                        src = empty();

                    // TODO latency
                    Optional<Integer> latency = empty();

                    Fix f = new FixImpl(a.getMmsi(), a.getLatitude().floatValue(),
                            a.getLongitude().floatValue(), m.time(), latency, src, nav, sog, cog,
                            heading, aisClass);
                    return Observable.just(f);
                }
            } else
                return Observable.empty();
        } catch (RuntimeException e) {
            log.warn(e.getMessage(), e);
            return Observable.empty();
        }
    };

    public static Observable<String> nmeaFrom(final File file) {
        return Observable.using(
                //
                Checked.f0(() -> new FileInputStream(file)), is -> nmeaFrom(is),
                //
                is -> {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // don't care
                    }
                } , true);
    }

    public static Observable<String> nmeaFrom(InputStream is) {
        return Strings.split(Strings.from(new InputStreamReader(is, UTF8)), "\n");
    }

    public static Observable<String> nmeaFromGzip(String filename) {
        return nmeaFromGzip(new File(filename));
    }

    public static Observable<Observable<String>> nmeasFromGzip(Observable<File> files) {
        return files.map(f -> nmeaFromGzip(f.getPath()));
    }

    public static Observable<String> nmeaFromGzip(final File file) {

        Func0<Reader> resourceFactory = () -> {
            try {
                return new InputStreamReader(new GZIPInputStream(new BufferedInputStream(new FileInputStream(file), 1024 * 1024)), UTF8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        Func1<Reader, Observable<String>> observableFactory = reader -> Strings
                .split(Strings.from(reader), "\n");

        Action1<Reader> disposeAction = reader -> {
            try {
                reader.close();
            } catch (IOException e) {
                // ignore
            }
        };
        return Observable.using(resourceFactory, observableFactory, disposeAction, true);
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

    public static final Func1<String, Optional<NmeaMessage>> LINE_TO_NMEA_MESSAGE = line -> {
        try {
            return Optional.of(NmeaUtil.parseNmea(line, true));
        } catch (RuntimeException e) {
            return Optional.empty();
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

    public static class TimestampedAndLines<T extends AisMessage> {
        private final Optional<Timestamped<T>> message;
        private final List<String> lines;
        private final String error;

        public TimestampedAndLines(Optional<Timestamped<T>> message, List<String> lines,
                String error) {
            this.message = message;
            this.lines = lines;
            this.error = error;
        }

        public Optional<Timestamped<T>> getMessage() {
            return message;
        }

        public List<String> getLines() {
            return lines;
        }

        public String getError() {
            return error;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            if (message.isPresent())
                builder.append("message=" + message);
            else
                builder.append("error=" + error);
            builder.append(", lines=");
            builder.append(lines);
            return builder.toString();
        }

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
            if (message.isPresent())
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
            } catch (RuntimeException e) {
                return Optional.empty();
            }
        }
    };

    public static final Func1<NmeaMessage, TimestampedAndLine<AisMessage>> TO_AIS_MESSAGE_AND_LINE = nmea -> {
        String line = nmea.toLine();
        try {
            AisNmeaMessage n = new AisNmeaMessage(nmea);
            return new TimestampedAndLine<AisMessage>(
                    Optional.of(n.getTimestampedMessage(System.currentTimeMillis())), line, null);
        } catch (AisParseException e) {
            return new TimestampedAndLine<AisMessage>(Optional.<Timestamped<AisMessage>> empty(),
                    line, e.getMessage());
        } catch (RuntimeException e) {
            log.warn(e.getMessage(), e);
            throw e;
        }
    };

    public static final Func1<Optional<List<NmeaMessage>>, TimestampedAndLines<AisMessage>> TO_AIS_MESSAGE_AND_LINES = nmeas -> {
        if (nmeas.isPresent()) {
            List<String> lines = nmeas.get() //
                    .stream() //
                    .map(new Function<NmeaMessage, String>() {
                @Override
                public String apply(NmeaMessage t) {
                    return t.toLine();
                }
            }).collect(Collectors.toList());
            Optional<NmeaMessage> concat = AisNmeaBuffer.concatenateMessages(nmeas.get());
            if (concat.isPresent()) {
                try {
                    AisNmeaMessage n = new AisNmeaMessage(concat.get());
                    return new TimestampedAndLines<AisMessage>(
                            Optional.of(n.getTimestampedMessage(System.currentTimeMillis())), lines,
                            null);
                } catch (AisParseException e) {
                    return new TimestampedAndLines<AisMessage>(
                            Optional.<Timestamped<AisMessage>> empty(), lines, e.getMessage());
                }
            } else {
                return new TimestampedAndLines<AisMessage>(
                        Optional.<Timestamped<AisMessage>> empty(), lines, "could not concat");
            }
        } else {
            return new TimestampedAndLines<AisMessage>(Optional.empty(), Collections.emptyList(),
                    null);
        }
    };

    public static final Transformer<NmeaMessage, Optional<List<NmeaMessage>>> addToBuffer(
            int bufferSize) {
        return new Transformer<NmeaMessage, Optional<List<NmeaMessage>>>() {

            @Override
            public Observable<Optional<List<NmeaMessage>>> call(Observable<NmeaMessage> o) {
                return Observable.defer(() -> {
                    AisNmeaBuffer buffer = new AisNmeaBuffer(bufferSize);
                    // use maxConcurrent so doesn't request unbounded
                    return o.map(nmea -> buffer.add(nmea));
                });
            }

        };
    }

    public static final Transformer<NmeaMessage, NmeaMessage> aggregateMultiLineNmea(
            int bufferSize) {
        return new Transformer<NmeaMessage, NmeaMessage>() {

            @Override
            public Observable<NmeaMessage> call(Observable<NmeaMessage> o) {
                return Observable.defer(() -> {
                    AisNmeaBuffer buffer = new AisNmeaBuffer(bufferSize);
                    // use maxConcurrent so doesn't request unbounded
                    return o.flatMap(nmea -> {
                        return addToBuffer(buffer, nmea);
                    } , 1);
                });
            }

        };
    }

    public static final Transformer<NmeaMessage, NmeaMessage> aggregateMultiLineNmeaWithLines(
            int bufferSize) {
        return new Transformer<NmeaMessage, NmeaMessage>() {

            @Override
            public Observable<NmeaMessage> call(Observable<NmeaMessage> o) {
                return Observable.defer(() -> {
                    AisNmeaBuffer buffer = new AisNmeaBuffer(bufferSize);
                    // use maxConcurrent so doesn't request unbounded
                    return o.flatMap(nmea -> {
                        return addToBuffer(buffer, nmea);
                    } , 1);
                });
            }

        };
    }

    private static Observable<? extends NmeaMessage> addToBuffer(AisNmeaBuffer buffer,
            NmeaMessage nmea) {
        try {
            Optional<List<NmeaMessage>> list = buffer.add(nmea);
            if (!list.isPresent())
                return Observable.empty();
            else {
                Optional<NmeaMessage> concat = AisNmeaBuffer.concatenateMessages(list.get());
                if (concat.isPresent())
                    return Observable.just(concat.get());
                else
                    return Observable.empty();
            }
        } catch (RuntimeException e) {
            log.warn(e.getMessage(), e);
            return Observable.empty();
        }
    }

    // private static Charset US_ASCII = Charset.forName("US-ASCII");

    public static Observable<String> connectOnce(final HostPort hostPort) {

        return Observable.unsafeCreate(new OnSubscribe<String>() {

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

    public static Func1<List<File>, Observable<Integer>> extractFixesFromNmeaGzAndAppendToFile(
            final int linesPerProcessor, final Scheduler scheduler,
            final Func1<Fix, String> fileMapper, final int writeBufferSize,
            final Action1<File> logger) {
        return files -> {
            Observable<Fix> fixes = Streams
                    .extractFixes(
                            Observable.from(files)
                                    // log
                                    .doOnNext(logger)
                                    // one file at a time
                                    .concatMap(file -> Streams.nmeaFromGzip(file.getAbsolutePath()) //
                                            .doOnError(e -> log.warn("problem reading file " + file
                                                    + ": " + e.getMessage())) //
                            .onErrorResumeNext(Observable.empty())));
            return BinaryFixesWriter
                    .writeFixes(fileMapper, fixes, writeBufferSize, false,
                            BinaryFixesFormat.WITHOUT_MMSI)
                    // total counts
                    .reduce(0, countFixes())
                    // do async
                    .subscribeOn(scheduler);
        };
    }

    private static Func2<Integer, List<Fix>, Integer> countFixes() {
        return (count, fixes) -> count + fixes.size();
    }

    public static Observable<Integer> writeFixesFromNmeaGz(File input, Pattern inputPattern,
            File output, int logEvery, int writeBufferSize, Scheduler scheduler,
            int linesPerProcessor, long downSampleIntervalMs, Func1<Fix, String> fileMapper) {

        final List<File> fileList = Files.find(input, inputPattern);
        Observable<File> files = Observable.from(fileList);

        // count files across parallel streams
        // OperatorLogging<File> logger = Logging.<File> logger().showCount()
        // .showRateSinceStart("rateFilesPerSecond").showValue().log();
        Action1<File> logger = new Action1<File>() {
            AtomicInteger count = new AtomicInteger();
            Long start = null;

            @Override
            public void call(File file) {
                if (start == null)
                    start = System.currentTimeMillis();
                int num = count.incrementAndGet();
                double filesPerSecond = (System.currentTimeMillis() - start) / (double) num
                        / 1000.0;
                log.info("file " + num + " of " + fileList.size() + ", " + file.getName()
                        + ", rateFilesPerSecond=" + filesPerSecond);
            }
        };

        deleteDirectory(output);

        return files
                // log the filename
                .buffer(Math.max(fileList.size() / Runtime.getRuntime().availableProcessors(), 1))
                // extract fixes
                .flatMap(extractFixesFromNmeaGzAndAppendToFile(linesPerProcessor, scheduler,
                        fileMapper, writeBufferSize, logger), 1)
                // count number written fixes
                .scan(0, (a, b) -> a + b)
                // log
                .lift(Logging.<Integer> logger().showCount().showMemory()
                        .showRateSince("rate", 5000).every(logEvery).log())
                // get the final count
                .last()
                // on completion of writing fixes, sort the track files and emit
                // the count of files
                .doOnCompleted(
                        () -> log.info("completed converting nmea to binary fixes, starting sort"))
                .concatWith(BinaryFixes.sortBinaryFixFilesByTime(output, downSampleIntervalMs,
                        scheduler));
    }

    private static void deleteDirectory(File output) {
        try {
            FileUtils.deleteDirectory(output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        Streams.nmeaFromGzip(new File("/media/an/nmea/2015/NMEA_ITU_20150521.gz"))
                .compose(o -> Streams.extract(o)).takeLast(10000).forEach(System.out::println);

    }
}
