package au.gov.amsa.geo.adhoc;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.BinaryFixesFormat;
import rx.Observable;

public class BinaryFixesWithMmsiGzSorterMain {

    private static final Logger log = Logger.getLogger(BinaryFixesWithMmsiGzSorterMain.class);

    public static void main(String[] args) {

        File tracksDir = new File("/home/dxm/AIS/tracks");
        File sortedTracks = new File("/home/dxm/AIS/tracks-sorted");

        BinaryFixes.from(new File(tracksDir, "2019-01-01.track.gz"), true, BinaryFixesFormat.WITH_MMSI) //
                .doOnNext(System.out::println) //
                .subscribe();

        sortedTracks.mkdir();
        Observable<File> files = Observable //
                .from(tracksDir.listFiles()) //
                .filter(x -> x.getName().endsWith(".track.gz"));
        files.flatMap(x -> Observable //
                .just(x) //
                .doOnNext(f -> log.info("sorting " + f)) //
                .doOnNext(f -> sort(f, sortedTracks))) //
                .doOnNext(f -> log.info("sorted " + f)) //
                .toBlocking() //
                .subscribe();
    }

    private static void sort(File file, File outputDir) {
        File sorted = new File(outputDir, file.getName());
        try (OutputStream out = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(sorted)))) {
            BinaryFixes.from(file, true, BinaryFixesFormat.WITH_MMSI) //
                    .doOnNext(System.out::println) //
                    .toSortedList((x, y) -> Long.compare(x.time(), y.time())) //
                    .flatMap(x -> Observable.from(x)) //
                    .doOnNext(fix -> BinaryFixes.write(fix, out, BinaryFixesFormat.WITH_MMSI)) //
                    .toBlocking() //
                    .subscribe();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
