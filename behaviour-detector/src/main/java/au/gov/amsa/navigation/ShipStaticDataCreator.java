package au.gov.amsa.navigation;

import java.io.File;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeSet;

import com.github.davidmoten.guavamini.Preconditions;
import com.github.davidmoten.rx.Checked;
import com.github.davidmoten.rx.Transformers;
import com.github.davidmoten.rx.slf4j.Logging;
import com.google.common.base.Optional;

import au.gov.amsa.ais.AisMessage;
import au.gov.amsa.ais.Timestamped;
import au.gov.amsa.ais.message.AisShipStatic;
import au.gov.amsa.ais.message.AisShipStaticA;
import au.gov.amsa.ais.message.AisShipStaticUtil;
import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.ais.rx.Streams.TimestampedAndLine;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observables.GroupedObservable;
import rx.schedulers.Schedulers;

public final class ShipStaticDataCreator {

    public static Observable<AisShipStatic> writeStaticDataToFile(List<File> files,
            File outputFile) {

        return writeStaticDataToFile(files, outputFile, Schedulers.computation());
    }

    public static Observable<AisShipStatic> writeStaticDataToFile(List<File> files, File outputFile,
            Scheduler scheduler) {
        Func0<PrintStream> resourceFactory = Checked.f0(() -> new PrintStream(outputFile));
        Func1<PrintStream, Observable<AisShipStatic>> observableFactory = out -> Observable
                .from(files)
                // buffer into chunks for each processor
                .buffer(Math.max(1, files.size() / Runtime.getRuntime().availableProcessors() - 1))
                .flatMap(
                        list -> Observable.from(list) //
                                .lift(Logging.<File> logger().showValue().showMemory().log()) //
                                .concatMap(
                                        file -> Streams.extract(Streams.nmeaFromGzip(file)) //
                                                .flatMap(aisShipStaticOnly) //
                                                .map(m -> m.getMessage().get().message()) //
                                                .distinct(m -> m.getMmsi()) //
                                                .doOnError(e -> System.err.println("could not read "
                                                        + file + ": " + e.getMessage())) //
                                .onErrorResumeNext(Observable.<AisShipStatic> empty())) //
                        .distinct(m -> m.getMmsi()) //
                        .subscribeOn(scheduler)) //
                .distinct(m -> m.getMmsi()) //
                .compose(Transformers.mapWithIndex()) //
                .doOnNext(indexed -> {
                    if (indexed.index() == 0) {
                        out.println(
                                "# MMSI, IMO, AisClass, AisShipType, MaxPresentStaticDraughtMetres, DimAMetres, DimBMetres, DimCMetres, DimDMetres, LengthMetres, WidthMetres, Name");
                        out.println("# columns are tab delimited");
                        out.println("# -1 = not present");
                    }
                })
                //
                .map(indexed -> indexed.value())
                //
                .doOnNext(m -> {
                    out.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n", m.getMmsi(),
                            getImo(m).or(-1), m instanceof AisShipStaticA ? "A" : "B",
                            m.getShipType(), getMaximumPresentStaticDraughtMetres(m).or(-1F),
                            m.getDimensionA().or(-1), m.getDimensionB().or(-1),
                            m.getDimensionC().or(-1), m.getDimensionD().or(-1),
                            AisShipStaticUtil.lengthMetres(m).or(-1),
                            AisShipStaticUtil.widthMetres(m).or(-1), prepareName(m.getName()));
                    out.flush();
                });

        Action1<PrintStream> disposeAction = out -> out.close();
        return Observable.using(resourceFactory, observableFactory, disposeAction);
    }

    public static Observable<Timestamped<AisShipStatic>> writeStaticDataToFileWithTimestamps(
            List<File> files, File outputFile, Scheduler scheduler) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Func0<PrintStream> resourceFactory = Checked.f0(() -> new PrintStream(outputFile));
        Func1<PrintStream, Observable<Timestamped<AisShipStatic>>> observableFactory = out -> Observable
                .from(files)
                // buffer into chunks for each processor
                .buffer(Math.max(1, files.size() / Runtime.getRuntime().availableProcessors() - 1))
                .flatMap(list -> Observable.from(list) //
                        .lift(Logging.<File> logger().showValue().showMemory().log()) //
                        .concatMap(file -> timestampedShipStatics(file))) //
                .groupBy(m -> m.message().getMmsi()) //
                .flatMap(g -> collect(g).subscribeOn(scheduler)) //
                .compose(Transformers.doOnFirst(x -> {
                    out.println(
                            "# MMSI, Time, IMO, AisClass, AisShipType, MaxPresentStaticDraughtMetres, DimAMetres, DimBMetres, DimCMetres, DimDMetres, LengthMetres, WidthMetres, Name");
                    out.println("# columns are tab delimited");
                    out.println("# -1 = not present");
                })) //
                .filter(set -> set.size() <= 10) //
                .flatMapIterable(set -> set) //
                .doOnNext(k -> {
                    AisShipStatic m = k.message();
                    out.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n", m.getMmsi(),
                            sdf.format(new Date(k.time())), getImo(m).or(-1),
                            m instanceof AisShipStaticA ? "A" : "B", m.getShipType(),
                            getMaximumPresentStaticDraughtMetres(m).or(-1F),
                            m.getDimensionA().or(-1), m.getDimensionB().or(-1),
                            m.getDimensionC().or(-1), m.getDimensionD().or(-1),
                            AisShipStaticUtil.lengthMetres(m).or(-1),
                            AisShipStaticUtil.widthMetres(m).or(-1), prepareName(m.getName()));
                    out.flush();
                });
        Action1<PrintStream> disposeAction = out -> out.close();
        return Observable.using(resourceFactory, observableFactory, disposeAction);
    }

    private static Observable<Timestamped<AisShipStatic>> timestampedShipStatics(File file) {
        return Streams.extract(Streams.nmeaFromGzip(file)) //
                .flatMap(aisShipStaticOnly) //
                .doOnError(
                        e -> System.err.println("could not read " + file + ": " + e.getMessage()))
                .onErrorResumeNext(Observable.<TimestampedAndLine<AisShipStatic>> empty())
                .filter(x -> x.getMessage().isPresent()) //
                .map(x -> x.getMessage().get());
    }

    private static Observable<TreeSet<Timestamped<AisShipStatic>>> collect(
            GroupedObservable<Integer, Timestamped<AisShipStatic>> g) {
        return g.collect(() -> new TreeSet<Timestamped<AisShipStatic>>(
                (a, b) -> Long.compare(a.time(), b.time())), (set, x) -> {
                    Timestamped<AisShipStatic> a = set.floor(x);
                    Timestamped<AisShipStatic> b = set.ceiling(x);
                    if (a != null && a.time() == x.time()) {
                        return;
                    }
                    if (b != null && b.time() == x.time()) {
                        return;
                    }

                    // There is a hole in this code for when a == b and a != x
                    // this seems unlikely and is presumably a correction
                    // of a bad record so happy to miss this case.
                    if (a == null) {
                        // nothing before
                        if (b == null) {
                            // nothing after
                            set.add(x);
                        } else if (isDifferent(b.message(), x.message())) {
                            set.add(x);
                        }
                        // otherwise ignore
                    } else {
                        boolean axDifferent = isDifferent(a.message(), x.message());
                        if (b == null) {
                            // nothing after
                            if (axDifferent) {
                                set.add(x);
                            }
                        } else {
                            boolean bxDifferent = isDifferent(x.message(), b.message());
                            if (axDifferent) {
                                set.add(x);
                                if (!bxDifferent) {
                                    remove(set, b);
                                }
                            }
                        }
                    }
                });
    }

    private static void remove(TreeSet<Timestamped<AisShipStatic>> set,
            Timestamped<AisShipStatic> a) {
        // slow O(n) remove
        Iterator<Timestamped<AisShipStatic>> it = set.iterator();
        while (it.hasNext()) {
            if (it.next() == a) {
                it.remove();
            }
        }
    }

    private static final boolean justImo = true;

    private static boolean isDifferent(AisShipStatic a, AisShipStatic b) {
        Preconditions.checkArgument(a.getMmsi() == b.getMmsi());

        boolean different = !justImo //
                && (!a.getDimensionA().equals(b.getDimensionA())
                        || !a.getDimensionB().equals(b.getDimensionB())
                        || !a.getDimensionC().equals(b.getDimensionC())
                        || !a.getDimensionD().equals(b.getDimensionD())
                        || !a.getLengthMetres().equals(b.getLengthMetres())
                        || !a.getWidthMetres().equals(b.getWidthMetres())
                        || !a.getName().equals(b.getName()) //
                        || a.getShipType() != b.getShipType());
        if (different) {
            return true;
        } else if (a instanceof AisShipStaticA && b instanceof AisShipStaticA) {
            AisShipStaticA a2 = (AisShipStaticA) a;
            AisShipStaticA b2 = (AisShipStaticA) b;
            return !a2.getImo().equals(b2.getImo());
        } else {
            return false;
        }
    }

    private static String prepareName(String name) {
        if (name == null)
            return "";
        else
            return name.replace("\t", " ").trim();
    }

    private static Optional<Integer> getImo(AisShipStatic m) {
        if (m instanceof AisShipStaticA) {
            return ((AisShipStaticA) m).getImo();
        } else
            return Optional.absent();
    }

    private static Optional<Float> getMaximumPresentStaticDraughtMetres(AisShipStatic m) {
        if (m instanceof AisShipStaticA) {
            return Optional.of((float) ((AisShipStaticA) m).getMaximumPresentStaticDraughtMetres());
        } else
            return Optional.absent();
    }

    private static Func1<TimestampedAndLine<AisMessage>, Observable<TimestampedAndLine<AisShipStatic>>> aisShipStaticOnly = m -> {
        Optional<Timestamped<AisMessage>> message = m.getMessage();
        if (message.isPresent() && message.get().message() instanceof AisShipStatic) {
            @SuppressWarnings("unchecked")
            TimestampedAndLine<AisShipStatic> m2 = (TimestampedAndLine<AisShipStatic>) (TimestampedAndLine<?>) m;
            return Observable.just(m2);
        } else
            return Observable.empty();
    };
}
