package au.gov.amsa.craft.analyzer.wms;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.gov.amsa.navigation.DriftCandidate;
import au.gov.amsa.navigation.DriftCandidates;
import au.gov.amsa.navigation.DriftDetector;
import au.gov.amsa.navigation.VesselPosition;
import au.gov.amsa.navigation.VesselPositions;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.NavigationalStatus;
import au.gov.amsa.util.Files;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class Sources {
    private static final Logger log = LoggerFactory.getLogger(Sources.class);

    public static Observable<VesselPosition> fixes() {
        List<File> files = Files.find(new File("/home/dave/Downloads/binary-fixes-2014-5-minutes"),
                Pattern.compile(".*\\.track"));
        log.info("files=" + files.size());
        final AtomicLong num = new AtomicLong();
        return Observable
                // list files
                .from(files)
                // share the load between processors
                .buffer(Math.max(1, files.size() / Runtime.getRuntime().availableProcessors() - 1))
                // search each list of files for drift detections
                .flatMap(detectDrift(num, Schedulers.computation()))
                .map(VesselPositions.TO_VESSEL_POSITION);
    }

    public static Observable<VesselPosition> fixes2(File file) {
        return DriftCandidates.fromCsv(file, false).filter(new Func1<DriftCandidate, Boolean>() {

            @Override
            public Boolean call(DriftCandidate c) {
                return !c.fix().navigationalStatus().isPresent()
                        || c.fix().navigationalStatus().get() != NavigationalStatus.ENGAGED_IN_FISHING;
            }
        }).map(VesselPositions.toVesselPosition(new Func1<DriftCandidate, Optional<?>>() {
            @Override
            public Optional<Long> call(DriftCandidate c) {
                return Optional.of(c.driftingSince());
            }
        }));
    }

    private static Func1<List<File>, Observable<Fix>> detectDrift(AtomicLong num,
            final Scheduler scheduler) {
        return new Func1<List<File>, Observable<Fix>>() {

            @Override
            public Observable<Fix> call(List<File> files) {
                return BinaryFixes.from(files).compose(DriftDetector.detectDrift())
                        .map(new Func1<DriftCandidate, Fix>() {

                            @Override
                            public Fix call(DriftCandidate c) {
                                return c.fix();
                            }
                        }).subscribeOn(scheduler);
            }
        };
    }
}
