package au.gov.amsa.navigation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.Downsample;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.FixImpl;
import au.gov.amsa.util.Files;
import au.gov.amsa.util.identity.MmsiValidator2;

public class BinaryFixesDriftDetectorMain {

    private static final String DRIFT_CANDIDATES_TXT = "target/drift-candidates.txt";
    private static final Logger log = LoggerFactory.getLogger(BinaryFixesDriftDetectorMain.class);

    public static void main(String[] args) {
        new File(DRIFT_CANDIDATES_TXT).delete();
        VesselPosition.validate = false;
        FixImpl.validate = false;
        List<File> files = Files.find(new File("/media/an/binary-fixes/2012"),
                Pattern.compile(".*\\.track"));
        log.info("files=" + files.size());
        final AtomicLong num = new AtomicLong();
        int count = Observable
        // list files
                .from(files)
                // exclude invalid mmsi
                .filter(onlyValidMmsis())
                // share the load between processors
                .buffer(Math.max(1, files.size() / Runtime.getRuntime().availableProcessors() - 1))
                // search each list of files for drift detections
                .flatMap(detectDrift(num, Schedulers.immediate()))
                // count
                .reduce(0, BinaryFixesDriftDetectorMain.<Integer> add()).toBlocking().single();
        log.info("drift detections = " + count);
    }

    private static Func1<? super File, Boolean> onlyValidMmsis() {
        return new Func1<File, Boolean>() {

            @Override
            public Boolean call(File file) {
                return MmsiValidator2.INSTANCE.isValid(Long.parseLong(file.getName().substring(0,
                        file.getName().indexOf('.'))));
            }
        };
    }

    private static <T extends Number> Func2<T, T, T> add() {
        return new Func2<T, T, T>() {
            @SuppressWarnings("unchecked")
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

    private static Func1<List<File>, Observable<Integer>> detectDrift(final AtomicLong num,
            final Scheduler scheduler) {
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
                                .compose(DriftDetector.detectDrift())
                                // downsample to min 5 minutes between reports
                                // but ensure that start of drift is always
                                // included
                                .compose(
                                        Downsample.<DriftCandidate> minTimeStep(5,
                                                TimeUnit.MINUTES, isStartOfDrift()))
                                // onNext
                                .doOnNext(ON_NEXT)
                                // count
                                .count();
                    }

                })
                // schedule
                        .subscribeOn(scheduler);
            }
        };
    }

    private static Func1<DriftCandidate, Boolean> isStartOfDrift() {
        return new Func1<DriftCandidate, Boolean>() {
            @Override
            public Boolean call(DriftCandidate c) {
                return c.driftingSince() == c.fix().time();
            }
        };
    }

    private static final Action1<DriftCandidate> ON_NEXT = new Action1<DriftCandidate>() {
        final char COMMA = ',';
        final Charset UTF8 = Charset.forName("UTF-8");

        @Override
        public synchronized void call(DriftCandidate c) {
            try {
                FileOutputStream os = new FileOutputStream(DRIFT_CANDIDATES_TXT, true);
                Fix f = c.fix();
                StringBuilder s = new StringBuilder();
                s.append(f.mmsi());
                s.append(COMMA);
                s.append(f.lat());
                s.append(COMMA);
                s.append(f.lon());
                s.append(COMMA);
                s.append(f.time());
                s.append(COMMA);
                s.append(f.aisClass());
                s.append(COMMA);
                s.append(f.courseOverGroundDegrees().get());
                s.append(COMMA);
                s.append(f.headingDegrees().get());
                s.append(COMMA);
                s.append(f.speedOverGroundKnots().get());
                s.append(COMMA);
                s.append(f.navigationalStatus().isPresent() ? f.navigationalStatus().get()
                        .toString() : "");
                s.append(COMMA);
                s.append(c.driftingSince());
                s.append('\n');
                os.write(s.toString().getBytes(UTF8));
                os.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    };

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
