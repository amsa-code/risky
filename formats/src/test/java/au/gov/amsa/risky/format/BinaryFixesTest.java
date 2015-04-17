package au.gov.amsa.risky.format;

import static com.google.common.base.Optional.of;
import static org.junit.Assert.assertEquals;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;

import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import au.gov.amsa.util.Files;

public final class BinaryFixesTest {

    private static final double PRECISION = 0.000001;

    @Test
    public void testWriteAndReadBinaryFixes() throws IOException {
        File trace = new File("target/123456789.track");
        int numFixes = 10000;
        writeTrace(trace, numFixes);

        System.out.println("wrote " + numFixes + " fixes");

        List<Fix> fixes = BinaryFixes.from(trace).toList().toBlocking().single();
        assertEquals(numFixes, fixes.size());
        Fix f = fixes.get(fixes.size() - 1);
        assertEquals(123456789, f.mmsi());
        assertEquals(-10.0, f.lat(), PRECISION);
        assertEquals(135, f.lon(), PRECISION);
        assertEquals(1000, f.time(), PRECISION);
        assertEquals(12, (int) f.latencySeconds().get());
        assertEquals(1, (int) f.source().get());
        assertEquals(NavigationalStatus.ENGAGED_IN_FISHING, f.navigationalStatus().get());
        assertEquals(7.5, f.speedOverGroundKnots().get(), PRECISION);
        assertEquals(45, f.courseOverGroundDegrees().get(), PRECISION);
        assertEquals(46, f.headingDegrees().get(), PRECISION);
        assertEquals(AisClass.B, f.aisClass());
    }

    private void writeTrace(File trace, int repetitions) throws IOException {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(trace));
        Fix fix = new FixImpl(213456789, -10f, 135f, 1000, of(12), of((short) 1),
                of(NavigationalStatus.ENGAGED_IN_FISHING), of(7.5f), of(45f), of(46f), AisClass.B);
        byte[] bytes = new byte[BinaryFixes.BINARY_FIX_BYTES];
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        BinaryFixes.write(fix, bb);
        for (int i = 0; i < repetitions; i++)
            os.write(bytes);
        os.close();
    }

    @Test
    public void testReadPerformance() throws IOException {
        File trace = new File("target/123456788.track");
        int numFixes = 1000000;
        writeTrace(trace, numFixes);
        System.out.println("testing performance reading numFixes=" + numFixes);
        long t = System.currentTimeMillis();
        BinaryFixes.from(trace).subscribe();
        double rate = numFixes * 1000.0 / (System.currentTimeMillis() - t);
        double size = trace.length() / 1000000.0;
        System.out.println("read " + numFixes + ", fileSizeMB=" + size + ", rateMsgPerSecond="
                + rate);
    }

    @Test
    public void testWriteTwoBinaryFixes() throws IOException {
        TestingUtil.writeTwoBinaryFixes("target/123456790.track");
    }

    @Test
    public void testConcurrencyDemo() {
        // using concurrency, count all the fixes across all files in the target
        // directory
        Observable<File> files = Observable.from(Files.find(new File("target"),
                Pattern.compile("\\d+\\.track")));
        int count = files
        // group the files against each processor
                .buffer(Runtime.getRuntime().availableProcessors() - 1)
                // do the work per buffer on a separate scheduler
                .flatMap(list -> {
                    return Observable.from(list)
                    // count the fixes in each file
                            .flatMap(countFixes())
                            // perform concurrently
                            .subscribeOn(Schedulers.computation());
                })
                // total all the counts
                .reduce(0, (a, b) -> a + b)
                // block and get the result
                .toBlocking().single();
        System.out.println("total fixes = " + count);
    }

    private Func1<File, Observable<Integer>> countFixes() {
        return file -> BinaryFixes.from(file).count();
    }
}
