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
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.BinaryFixesFormat;
import au.gov.amsa.risky.format.Fix;
import rx.Observable;

public final class BinaryFixesWithMmsiGzCombinedSortMain {
    
    private static final Logger log = Logger.getLogger(BinaryFixesWithMmsiGzCombinedSortMain.class);

    public static void main(String[] args) throws InterruptedException {
        log.info("starting");
        File tracks = new File("/home/dxm/AIS/tracks");
        File combinedSortedTracks = new File("/home/dxm/combinedSortedTracks2");
        combinedSortedTracks.mkdir();

        
        AtomicReference<FileFixes> previous = new AtomicReference<>();
        AtomicInteger n = new AtomicInteger();
        Observable //
                .from(tracks.listFiles()) //
                .filter(x -> x.getName().endsWith(".track.gz") && x.getName().startsWith("2019-01-0")) //
                .sorted((x,y) -> x.getName().compareTo(y.getName())) //
                .map(x -> new FileFixes(x, BinaryFixes.from(x, true, BinaryFixesFormat.WITH_MMSI) //
                        .toList() //
                        .toBlocking() //
                        .first()))
                .doOnNext(ff -> {
                    FileFixes prev = previous.get();
                    if (prev != null) {
                        long removed = 0;
                        long added = 0;
                        List<Fix> removeThese = new ArrayList<>(); 
                        for (Fix fix: ff.fixes) {
                            if (fix.time() < ff.startTime) {
                                removeThese.add(fix);
                                removed++;
                                if (fix.time() >= prev.startTime) {
                                    prev.fixes.add(fix);
                                    added++;
                                }
                            }
                        }
                        prev.fixes.removeAll(removeThese);
                        prev.fixes.sort((x, y) -> Long.compare(x.time(), y.time()));
                        writeFixes(combinedSortedTracks, n, prev, removed, added);
                    }
                    previous.set(ff);
                }).subscribe();
        if (previous.get() != null) {
            writeFixes(combinedSortedTracks, n, previous.get(), 0, 0);
        }
    }

    private static void writeFixes(File combinedSortedTracks, AtomicInteger n, FileFixes prev, long removed,
            long added) {
        File f = new File(combinedSortedTracks, prev.file.getName());
        try (OutputStream out = new GZIPOutputStream(new FileOutputStream(f))) {
            for (Fix fix : prev.fixes) {
                BinaryFixes.write(fix, out, BinaryFixesFormat.WITH_MMSI);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        log.info(n.incrementAndGet() + ": removed from next day=" + removed + ", added to this day=" + added + " to " + f);
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
