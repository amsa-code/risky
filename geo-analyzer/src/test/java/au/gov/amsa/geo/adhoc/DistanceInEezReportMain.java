package au.gov.amsa.geo.adhoc;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.BinaryFixesFormat;
import au.gov.amsa.risky.format.Fix;
import rx.Observable;
import rx.schedulers.Schedulers;

public final class DistanceInEezReportMain {

    private static final Logger log = Logger.getLogger(DistanceInEezReportMain.class);

    public static void main(String[] args) throws Exception {
        // input is one year of nmea lines split into daily files
        //
        File inputDir = new File("/home/dxm/AIS");
        File outputDir = new File(inputDir, "tracks");
        outputDir.mkdir();
        Observable<File> inputFiles = Observable.from(inputDir.listFiles()) //
                .filter(f -> f.getName().endsWith(".txt.gz")).toSortedList() //
                .flatMap(o -> Observable.from(o));
        inputFiles //
                .flatMap(x -> Observable.just(x) //
                        .doOnNext(System.out::println) //
                        .doOnNext(f -> convert(f, outputDir)) //
                        .subscribeOn(Schedulers.computation())) //
                .toBlocking().subscribe();
    }

    private static void convert(File file, File outputDir) {
        try {
            log.info("converting " + file.getName() + ", gzipped file size = " + (double) file.length() / 1024 / 1024 + "MB");
            long t = System.currentTimeMillis();
            Observable<String> nmea = Streams.nmeaFromGzip(file).onErrorResumeNext(e -> Observable.empty());
            long downsampleMs = TimeUnit.MINUTES.toMillis(15);
            Map<Integer, Long> latestMmsiTime = new HashMap<>();
            long count;

            String outFilename = file.getName().substring(0, file.getName().indexOf(".txt.gz")) + ".track.gz";
            File outFile = new File(file.getParentFile(), outFilename);
            try (OutputStream out = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)))) {
                count = nmea //
                        .compose(o -> Streams.extractFixes(o)) //
                        .filter(x -> inRegion(x)) //
                        .filter(x -> {
                            // crude downsample because nmea not sorted by position time
                            Long time = latestMmsiTime.get(x.mmsi());
                            if (time == null || Math.abs(x.time() - time) >= downsampleMs) {
                                if (time != null && x.time() > time) {
                                    latestMmsiTime.put(x.mmsi(), x.time());
                                }
                                return true;
                            } else {
                                return false;
                            }
                        }).doOnNext(fix -> BinaryFixes.write(fix, out, BinaryFixesFormat.WITH_MMSI)) //
                        .countLong().toBlocking().first();
            }
            log.info(count);
            double seconds = (System.currentTimeMillis() - t) / 1000.0;
            log.info("file=" + file.getName() //
            + ", count=" + count  //"
                    + ", time=" + seconds + "s" //
                    + ", rate = " + (count / seconds) + " lines/s" //
                    + ", outputFileSize = " + outFile.length() / 1024.0 / 1024 + "MB");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean inRegion(Fix f) {
        return f.lat() > -45 && f.lat() < -8 && f.lon() > 108 && f.lon() < 160;
    }
}
