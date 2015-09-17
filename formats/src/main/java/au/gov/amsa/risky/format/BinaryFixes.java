package au.gov.amsa.risky.format;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.davidmoten.rx.Functions;
import com.github.davidmoten.rx.slf4j.Logging;

import au.gov.amsa.util.Files;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

public final class BinaryFixes {

    private static Logger log = LoggerFactory.getLogger(BinaryFixes.class);

    public static final int BINARY_FIX_BYTES = 31;
    public static final short SOG_ABSENT = 1023;
    public static final short COG_ABSENT = 3600;
    public static final short HEADING_ABSENT = 360;
    public static final byte NAV_STATUS_ABSENT = Byte.MAX_VALUE;
    public static final int LATENCY_ABSENT = -1;
    public static final short SOURCE_ABSENT = 0;
    public static final byte ROT_ABSENT = Byte.MIN_VALUE;
    public static final byte SOURCE_PRESENT_BUT_UNKNOWN = 1;
    protected static final char COMMA = ',';
    protected static final byte RATE_OF_TURN_ABSENT = -128;

    /**
     * Automatically detects gzip based on filename.
     * 
     * @param file
     * @return
     */
    public static Observable<Fix> from(File file) {
        return from(file, false);
    }

    /**
     * Automatically detects gzip based on filename.
     * 
     * @param file
     * @param backpressure
     * @return
     */
    public static Observable<Fix> from(File file, boolean backpressure) {
        if (backpressure)
            return BinaryFixesOnSubscribeWithBackp.from(file, false);
        else
            return BinaryFixesOnSubscribeFastPath.from(file);
    }

    public static Observable<String> csv(Observable<Fix> fixes) {
        return fixes.map(f -> {
            StringBuilder s = new StringBuilder();
            s.append(f.lat());
            s.append(COMMA);
            s.append(f.lon());
            s.append(COMMA);
            s.append(new DateTime(f.time()).toString());
            s.append(COMMA);
            s.append(f.source().or(SOURCE_ABSENT));
            s.append(COMMA);
            s.append(f.latencySeconds().or(LATENCY_ABSENT));
            s.append(COMMA);
            s.append(f.navigationalStatus().or(NavigationalStatus.values()[NAV_STATUS_ABSENT]));
            s.append(COMMA);
            s.append(f.rateOfTurn().or(RATE_OF_TURN_ABSENT));
            s.append(COMMA);
            // TODO add the rest of the fields
            return s.toString();
        });
    }

    public static void write(Fix fix, OutputStream os) {
        byte[] bytes = new byte[BINARY_FIX_BYTES];
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        write(fix, bb);
        try {
            os.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ByteBuffer createFixByteBuffer() {
        return ByteBuffer.allocate(BINARY_FIX_BYTES);
    }

    public static void write(Fix fix, ByteBuffer bb) {
        bb.putFloat(fix.lat());
        bb.putFloat(fix.lon());
        bb.putLong(fix.time());
        if (fix.latencySeconds().isPresent())
            bb.putInt(fix.latencySeconds().get());
        else
            bb.putInt(LATENCY_ABSENT);
        if (fix.source().isPresent())
            bb.putShort(fix.source().get());
        else
            bb.putShort(SOURCE_ABSENT);

        if (fix.navigationalStatus().isPresent())
            bb.put((byte) fix.navigationalStatus().get().ordinal());
        else
            bb.put(NAV_STATUS_ABSENT);

        // rot
        bb.put(ROT_ABSENT);

        if (fix.speedOverGroundKnots().isPresent())
            bb.putShort((short) Math.round(10 * fix.speedOverGroundKnots().get()));
        else
            bb.putShort(SOG_ABSENT);

        if (fix.courseOverGroundDegrees().isPresent())
            bb.putShort((short) Math.round(10 * fix.courseOverGroundDegrees().get()));
        else
            bb.putShort(COG_ABSENT);

        if (fix.headingDegrees().isPresent())
            bb.putShort((short) Math.round(10 * fix.headingDegrees().get()));
        else
            bb.putShort(HEADING_ABSENT);
        if (fix.aisClass() == AisClass.A)
            bb.put((byte) 0);
        else
            bb.put((byte) 1);
    }

    public static Observable<Integer> sortBinaryFixFilesByTime(File output,
            final long downSampleIntervalMs, Scheduler scheduler) {
        final AtomicInteger numFiles = new AtomicInteger();
        final AtomicLong totalSizeBytes = new AtomicLong();
        final Action1<File> preSortAction = createLogAction(numFiles, totalSizeBytes);
        return Observable.just(output)
                // just does not support backpressure so add it
                .onBackpressureBuffer()
                // log
                .lift(Logging.<File> logger().prefix("sorting files in folder ").log())
                // find the track files
                .concatMap(findTrackFiles(numFiles, totalSizeBytes))
                // sort the fixes in each file in each list and rewrite files
                .flatMap(sortFileFixes(downSampleIntervalMs, scheduler, preSortAction))
                // return the count
                .count();
    }

    private static Action1<File> createLogAction(final AtomicInteger numFiles,
            final AtomicLong totalSizeBytes) {
        return new Action1<File>() {
            final AtomicInteger count = new AtomicInteger();
            final long startTime = System.currentTimeMillis();
            final AtomicLong size = new AtomicLong();

            @Override
            public void call(File f) {
                long t = System.currentTimeMillis();
                int n = count.incrementAndGet();
                long bytes = size.getAndAdd(f.length());
                double timeToFinishMins;
                if (n > 1) {
                    timeToFinishMins = (t - startTime) / (double) bytes
                            * (totalSizeBytes.get() - bytes) / 1000.0 / 60.0;
                } else
                    timeToFinishMins = -1;
                DecimalFormat df = new DecimalFormat("0.000");
                log.info("sorting " + n + " of " + numFiles.get() + ":" + f + ", sizeMB="
                        + df.format(f.length() / 1000000.0) + ", finish in mins="
                        + df.format(timeToFinishMins));
            }
        };
    }

    private static Func1<List<File>, Observable<Integer>> sortFileFixes(
            final long downSampleIntervalMs, final Scheduler scheduler,
            final Action1<File> preSortAction) {
        return files -> {
            return Observable
                    // from list of files
                    .from(files)
                    // log
                    .doOnNext(preSortAction)
                    // process one file after another
                    .concatMap(sortFileFixes(downSampleIntervalMs))
                    // async
                    .subscribeOn(scheduler);
        };
    }

    private static Func1<File, Observable<Integer>> sortFileFixes(final long downSampleIntervalMs) {
        return file -> {
            return BinaryFixes.from(file)
                    // to list
                    .toList()
                    // sort each list
                    .map(sortFixes())
                    // flatten
                    .flatMapIterable(Functions.<List<Fix>> identity())
                    // downsample the sorted fixes
                    .compose(Downsample.minTimeStep(downSampleIntervalMs, TimeUnit.MILLISECONDS))
                    .cast(HasFix.class)
                    // make into a list again
                    .toList()
                    // replace the file with sorted fixes
                    .doOnNext(writeFixes(file))
                    // count the fixes
                    .count();
        };
    }

    private static Func1<File, Observable<List<File>>> findTrackFiles(final AtomicInteger numFiles,
            final AtomicLong totalSize) {
        return output -> {
            List<File> files = Files.find(output, Pattern.compile("\\d+\\.track"));
            log.info("found files " + files.size());
            log.info("getting total size");
            long size = 0;
            for (File file : files)
                size += file.length();
            log.info("total size=" + size);
            totalSize.set(size);
            numFiles.set(files.size());
            return Observable.from(files).buffer(
                    Math.max(1, files.size() / Runtime.getRuntime().availableProcessors() - 1));
        };
    }

    private static Action1<List<HasFix>> writeFixes(final File file) {
        return list -> BinaryFixesWriter.writeFixes(list, file, false, false, false);
    }

    private static Func1<List<Fix>, List<Fix>> sortFixes() {
        return list -> {
            ArrayList<Fix> temp = new ArrayList<Fix>(list);
            Collections.sort(temp, FIX_ORDER_BY_TIME);
            return temp;
        };
    }

    private static final Comparator<Fix> FIX_ORDER_BY_TIME = (a, b) -> ((Long) a.time())
            .compareTo(b.time());

    public static Observable<Fix> from(List<File> files) {
        return Observable.from(files).concatMap(file -> BinaryFixes.from(file));
    }
}
