package au.gov.amsa.risky.format;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import au.gov.amsa.util.Files;

import com.github.davidmoten.rx.Functions;
import com.github.davidmoten.rx.slf4j.Logging;

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

    public static Observable<Fix> from(File file) {
        return from(file, false);
    }

    public static Observable<Fix> from(File file, boolean backpressure) {
        if (backpressure)
            return BinaryFixesOnSubscribeWithBackp.from(file);
        else
            return BinaryFixesOnSubscribeFastPath.from(file);
    }

    public static Observable<String> csv(Observable<Fix> fixes) {
        return fixes.map(new Func1<Fix, String>() {

            @Override
            public String call(Fix f) {
                StringBuilder s = new StringBuilder();
                s.append(f.getLat());
                s.append(COMMA);
                s.append(f.getLon());
                s.append(COMMA);
                s.append(new DateTime(f.getTime()).toString());
                s.append(COMMA);
                s.append(f.getSource().or(SOURCE_ABSENT));
                s.append(COMMA);
                s.append(f.getLatencySeconds().or(LATENCY_ABSENT));
                s.append(COMMA);
                s.append(f.getNavigationalStatus().or(
                        NavigationalStatus.values()[NAV_STATUS_ABSENT]));
                s.append(COMMA);
                s.append(f.getRateOfTurn().or(RATE_OF_TURN_ABSENT));
                s.append(COMMA);
                // TODO add the rest of the fields
                return s.toString();
            }
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
        bb.putFloat(fix.getLat());
        bb.putFloat(fix.getLon());
        bb.putLong(fix.getTime());
        if (fix.getLatencySeconds().isPresent())
            bb.putInt(fix.getLatencySeconds().get());
        else
            bb.putInt(LATENCY_ABSENT);
        if (fix.getSource().isPresent())
            bb.putShort(fix.getSource().get());
        else
            bb.putShort(SOURCE_ABSENT);

        if (fix.getNavigationalStatus().isPresent())
            bb.put((byte) fix.getNavigationalStatus().get().ordinal());
        else
            bb.put(NAV_STATUS_ABSENT);

        // rot
        bb.put(ROT_ABSENT);

        if (fix.getSpeedOverGroundKnots().isPresent())
            bb.putShort((short) Math.round(10 * fix.getSpeedOverGroundKnots().get()));
        else
            bb.putShort(SOG_ABSENT);

        if (fix.getCourseOverGroundDegrees().isPresent())
            bb.putShort((short) Math.round(10 * fix.getCourseOverGroundDegrees().get()));
        else
            bb.putShort(COG_ABSENT);

        if (fix.getHeadingDegrees().isPresent())
            bb.putShort((short) Math.round(10 * fix.getHeadingDegrees().get()));
        else
            bb.putShort(HEADING_ABSENT);
        if (fix.getAisClass() == AisClass.A)
            bb.put((byte) 0);
        else
            bb.put((byte) 1);
    }

    // TODO move this stuff to formats/BinaryFixes.java
    public static Observable<Integer> sortBinaryFixFilesByTime(File output,
            final long downSampleIntervalMs, Scheduler scheduler) {
        return Observable.just(output).onBackpressureBuffer()
        // log
                .lift(Logging.<File> logger().prefix("sorting=").log())
                // find the track files
                .concatMap(findTrackFiles())
                // sort the fixes in each file in each list and rewrite files
                .flatMap(sortFileFixes(downSampleIntervalMs, scheduler))
                // return the count
                .count();
    }

    private static Func1<List<File>, Observable<Integer>> sortFileFixes(
            final long downSampleIntervalMs, final Scheduler scheduler) {
        return new Func1<List<File>, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(final List<File> files) {
                return Observable
                // from list of files
                        .from(files)
                        // process one file after another
                        .concatMap(sortFileFixes(downSampleIntervalMs))
                        // async
                        .subscribeOn(scheduler);
            }
        };
    }

    private static Func1<File, Observable<Integer>> sortFileFixes(final long downSampleIntervalMs) {
        return new Func1<File, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(final File file) {
                return BinaryFixes
                        .from(file)
                        // to list
                        .toList()
                        .doOnNext(new Action1<List<Fix>>() {
                            @Override
                            public void call(List<Fix> list) {
                                log.info("sorting " + file);
                            }
                        })
                        // sort each list
                        .map(sortFixes())
                        // flatten
                        .flatMapIterable(Functions.<List<Fix>> identity())
                        // downsample the sorted
                        // fixes
                        .compose(
                                Downsample.minTimeStep(downSampleIntervalMs, TimeUnit.MILLISECONDS))
                        // make into a list
                        // again
                        .toList()
                        // replace the file with
                        // sorted fixes
                        .doOnNext(writeFixes(file))
                        // count the fixes
                        .count();
            }
        };
    }

    private static Func1<File, Observable<List<File>>> findTrackFiles() {
        return new Func1<File, Observable<List<File>>>() {
            @Override
            public Observable<List<File>> call(File output) {
                List<File> files = Files.find(output, Pattern.compile("\\d+\\.track"));
                System.out.println("found files " + files.size());
                return Observable.from(files).buffer(
                        Math.max(1, files.size() / Runtime.getRuntime().availableProcessors() - 1));
            }
        };
    }

    private static Action1<List<Fix>> writeFixes(final File file) {
        return new Action1<List<Fix>>() {
            @Override
            public void call(List<Fix> list) {
                BinaryFixesWriter.writeFixes(list, file, false, false);
            }
        };
    }

    private static Func1<List<Fix>, List<Fix>> sortFixes() {
        return new Func1<List<Fix>, List<Fix>>() {

            @Override
            public List<Fix> call(List<Fix> list) {
                ArrayList<Fix> temp = new ArrayList<Fix>(list);
                Collections.sort(temp, FIX_ORDER_BY_TIME);
                return temp;
            }
        };
    }

    private static final Comparator<Fix> FIX_ORDER_BY_TIME = new Comparator<Fix>() {
        @Override
        public int compare(Fix a, Fix b) {
            return ((Long) a.getTime()).compareTo(b.getTime());
        }
    };

    public static Observable<Fix> from(List<File> files) {
        return Observable.from(files).concatMap(new Func1<File, Observable<Fix>>() {

            @Override
            public Observable<Fix> call(File file) {
                return BinaryFixes.from(file);
            }
        });
    }
}
