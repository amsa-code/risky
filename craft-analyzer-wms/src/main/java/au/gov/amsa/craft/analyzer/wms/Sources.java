package au.gov.amsa.craft.analyzer.wms;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import au.gov.amsa.navigation.DriftingDetectorFix;
import au.gov.amsa.navigation.VesselPosition;
import au.gov.amsa.navigation.VesselPositions;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.util.Files;

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

    private static Func1<List<File>, Observable<au.gov.amsa.risky.format.Fix>> detectDrift(
            AtomicLong num, final Scheduler scheduler) {
        return new Func1<List<File>, Observable<au.gov.amsa.risky.format.Fix>>() {

            @Override
            public Observable<au.gov.amsa.risky.format.Fix> call(List<File> files) {
                return BinaryFixes.from(files).compose(DriftingDetectorFix.detectDrift())
                        .subscribeOn(scheduler);
            }
        };
    }
}
