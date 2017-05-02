package au.gov.amsa.navigation;

import java.io.File;
import java.io.PrintStream;
import java.util.List;

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
                .buffer(Math.max(1,
                        files.size()
                                / Runtime.getRuntime().availableProcessors())
                        - 1)
                .flatMap(
                        list -> Observable.from(list) //
                                .lift(Logging.<File> logger().showValue().showMemory().log()) //
                                .concatMap(
                                        file -> Streams.extract(Streams.nmeaFromGzip(file)) //
                                                .flatMap(aisShipStaticOnly) //
                                                .map(m -> m.getMessage().get().message()) //
                                                .filter(m -> m instanceof AisShipStatic) //
                                                .cast(AisShipStatic.class) //
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
