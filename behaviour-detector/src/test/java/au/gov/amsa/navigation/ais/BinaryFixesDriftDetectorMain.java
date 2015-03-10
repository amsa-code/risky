package au.gov.amsa.navigation.ais;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import au.gov.amsa.navigation.DriftingDetectorFix;
import au.gov.amsa.navigation.VesselPosition;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.NavigationalStatus;
import au.gov.amsa.util.Files;

public class BinaryFixesDriftDetectorMain {

    private static final Logger log = LoggerFactory.getLogger(BinaryFixesDriftDetectorMain.class);

    public static void main(String[] args) {

        VesselPosition.validate = false;
        Fix.validate = false;
        List<File> files = Files.find(new File("/home/dave/Downloads/binary-fixes-2014-5-minutes"),
                Pattern.compile(".*\\.track"));
        log.info("files=" + files.size());
        final AtomicLong num = new AtomicLong();
        int count = Observable
        // list files
                .from(files)
                // share the load between processors
                .buffer(Math.max(1, files.size() / Runtime.getRuntime().availableProcessors() - 1))
                // search each list of files for drift detections
                .flatMap(detectDrift(num))
                // count
                .reduce(0, BinaryFixesDriftDetectorMain.<Integer> add()).toBlocking().single();
        System.out.println("drift detections = " + count);
    }

    private static <T extends Number> Func2<T, T, T> add() {
        return new Func2<T, T, T>() {
            @Override
            public T call(T a, T b) {
                if (a instanceof Integer)
                    return (T) (Number) (a.intValue() + b.intValue());
                else if (a instanceof Long)
                    return (T) (Number) (a.longValue() + b.longValue());
                else if (a instanceof Double)
                    return (T) (Number) (a.doubleValue() + b.doubleValue());
                else if (a instanceof Float)
                    return (T) (Number) (a.floatValue() + b.floatValue());
                else if (a instanceof Byte)
                    return (T) (Number) (a.byteValue() + b.byteValue());
                else if (a instanceof Short)
                    return (T) (Number) (a.shortValue() + b.shortValue());
                else
                    throw new RuntimeException("not implemented");
            }
        };
    }

    private static Func1<List<File>, Observable<Integer>> detectDrift(final AtomicLong num) {
        return new Func1<List<File>, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(List<File> list) {
                return Observable.from(list).concatMap(new Func1<File, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> call(File file) {
                        return BinaryFixes.from(file)
                        // log count
                                .doOnNext(logCount(num))
                                // detect drift
                                .compose(DriftingDetectorFix.detectDrift())
                                // .doOnNext(onEach())
                                .count();
                    }

                })
                // schedule
                        .subscribeOn(Schedulers.computation());
            }
        };
    }

    private static Action1<Fix> onEach() {
        return new Action1<Fix>() {

            @Override
            public void call(Fix fix) {
                if (false && fix.getNavigationalStatus().isPresent()
                        && fix.getNavigationalStatus().get() == NavigationalStatus.AT_ANCHOR
                        && fix.getHeadingDegrees().isPresent()
                        && fix.getSpeedOverGroundKnots().isPresent()
                        && fix.getSpeedOverGroundKnots().get() > 0.5)
                    log.info("mmsi=" + fix.getMmsi() + ", heading=" + fix.getHeadingDegrees().get()
                            + " " + new Date(fix.getTime()));
            }
        };
    }

    private static Action1<Fix> logCount(final AtomicLong num) {
        return new Action1<Fix>() {
            @Override
            public void call(Fix p) {
                long n = num.incrementAndGet();
                if (n % 1000000 == 0)
                    log.info((n / 1000000.0) + "m");
            }
        };
    }

}
