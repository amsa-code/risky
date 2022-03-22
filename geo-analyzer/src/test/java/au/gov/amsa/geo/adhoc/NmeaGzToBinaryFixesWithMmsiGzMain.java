package au.gov.amsa.geo.adhoc;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.BinaryFixesFormat;
import au.gov.amsa.risky.format.Fix;
import rx.Observable;
import rx.schedulers.Schedulers;

public final class NmeaGzToBinaryFixesWithMmsiGzMain {

    private static final Logger log = LoggerFactory.getLogger(NmeaGzToBinaryFixesWithMmsiGzMain.class);

    public static void main(String[] args) throws Exception {
        // input is one year of nmea lines split into daily files
        //
        File inputDir = new File("/home/dxm/AIS");
        File outputDir = new File(inputDir, "tracks");
        outputDir.mkdir();
        Observable<File> inputFiles = Observable.from(inputDir.listFiles()) //
                .filter(f -> f.getName().endsWith(".txt.gz")).toSortedList() //
                .flatMap(o -> Observable.from(o));
        AtomicInteger n = new AtomicInteger();
        inputFiles //
                .flatMap(x -> Observable.just(x) //
                        .doOnNext(f -> convert(f, outputDir, n)) //
                        .subscribeOn(Schedulers.computation()), Runtime.getRuntime().availableProcessors()) //
                .toBlocking().subscribe();
    }
    
    

    private static void convert(File file, File outputDir, AtomicInteger n) {
        try {
            log.info("converting " + file.getName() + ", gzipped file size = " + (double) file.length() / 1024 / 1024
                    + "MB");
            long t = System.currentTimeMillis();
            Observable<String> nmea = Streams.nmeaFromGzip(file).onErrorResumeNext(e -> Observable.empty());
            long count;
            String outFilename = file.getName().substring(0, file.getName().indexOf(".txt.gz")) + ".track.gz";
            File outFile = new File(outputDir, outFilename);
            try (OutputStream out = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)))) {
                count = nmea //
                        .compose(o -> Streams.extractFixes(o)) //
                        .filter(x -> inRegion(x)) //
                        // ensure that time is not ridiculous (1970 < time < 2050)
                        .filter(x -> x.time() > 0 && x.time() < TimeUnit.DAYS.toMillis(80*365)) //
                        .doOnNext(fix -> BinaryFixes.write(fix, out, BinaryFixesFormat.WITH_MMSI)) //
                        .countLong() //
                        .toBlocking() //
                        .first();
            }
            double seconds = (System.currentTimeMillis() - t) / 1000.0;
            n.incrementAndGet();
            log.info(n.get() + ": file=" + file.getName() //
                    + ", count=" + count // "
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
