package au.gov.amsa.geo.adhoc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.BinaryFixesFormat;
import au.gov.amsa.risky.format.Fix;
import rx.Observable;

public final class BinaryFixesWithMmsiGzCombinedSortMain {
    
    private static final Logger log = Logger.getLogger(BinaryFixesWithMmsiGzCombinedSortMain.class);

    public static void main(String[] args) throws InterruptedException {
        File tracks = new File("/home/dxm/AIS/tracks");
        File combinedSortedTracks = new File("/home/dxm/combinedSortedTracks");
        combinedSortedTracks.mkdir();

        AtomicReference<FileFixes> previous = new AtomicReference<>();
        AtomicInteger n = new AtomicInteger();
        Observable //
                .from(tracks.listFiles()) //
                .filter(x -> x.getName().endsWith(".track.gz") && x.getName().compareTo("2019-08-06") >= 0) //
                .map(x -> new FileFixes(x,
                        BinaryFixes.from(x, true, BinaryFixesFormat.WITH_MMSI).toList().toBlocking().first()))
                .doOnNext(ff -> {
                    FileFixes prev = previous.get();
                    if (prev != null) {
                        List<Fix> move = new ArrayList<>();
                        for (Fix fix : ff.fixes) {
                            if (fix.time() < ff.startTime) {
                                move.add(fix);
                            }
                        }
                        ff.fixes.removeAll(move);
                        List<Fix> addThese = move.stream().filter(fix -> fix.time() >= prev.startTime)
                                .collect(Collectors.toList());
                        prev.fixes.addAll(addThese);
                        prev.fixes.sort((x, y) -> Long.compare(x.time(), y.time()));
                        File f = new File(combinedSortedTracks, prev.file.getName());
                        try (OutputStream out = new GZIPOutputStream(new FileOutputStream(f))) {
                            for (Fix fix : prev.fixes) {
                                BinaryFixes.write(fix, out, BinaryFixesFormat.WITH_MMSI);
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                        log.info(n.incrementAndGet() + ": removed " + move.size() + ", added " + addThese.size() + " to " + f);
                    }
                    previous.set(ff);
                }).subscribe();
    }

    private static final class FileFixes {
        final File file;
        final List<Fix> fixes;
        final long startTime;

        FileFixes(File file, List<Fix> fixes) {
            this.file = file;
            this.fixes = fixes;
            String datePart = file.getName().substring(0, 10);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            try {
                startTime = sdf.parse(datePart).getTime();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
