package au.gov.amsa.geo.adhoc;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.davidmoten.hilbert.HilbertCurve;
import org.davidmoten.hilbert.Range;
import org.davidmoten.hilbert.SmallHilbertCurve;

import com.github.davidmoten.guavamini.Preconditions;

import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.BinaryFixesFormat;
import au.gov.amsa.risky.format.Fix;
import rx.functions.Action1;

public class AdhocMain2 {

    public static void main(String[] args) throws IOException {

        String folder = "/home/dave/Downloads";
        // String folder = "/media/an/temp";
        File file = new File(folder + "/2017-11-16.fix");

        int choice = 2;
        if (choice == 1) {
            // write the file
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file, true))) {
                AtomicLong count = new AtomicLong();
                Streams.nmeaFromGzip(folder + "/2017-11-16.txt.gz") //
                        .compose(x -> Streams.extractFixes(x)) //
                        .doOnNext(log(count)) //
                        .forEach(f -> BinaryFixes.write(f, os, BinaryFixesFormat.WITH_MMSI)); //
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (choice == 2) {
            // print out histogram (which turns out to be uniform surprisingly)
            // and files
            long minTime = BinaryFixes.from(file, false, BinaryFixesFormat.WITH_MMSI) //
                    .map(x -> x.time()).reduce((x, y) -> Math.min(x, y)).toBlocking().single();

            long maxTime = BinaryFixes.from(file, false, BinaryFixesFormat.WITH_MMSI) //
                    .map(x -> x.time()).reduce((x, y) -> Math.max(x, y)).toBlocking().single();
            System.out.println("start=" + new Date(minTime) + ", finish=" + new Date(maxTime));

            int bits = 20;
            int dimensions = 3;
            SmallHilbertCurve h = HilbertCurve.small().bits(bits).dimensions(dimensions);
            long maxIndexes = 1L << bits * dimensions;
            long maxOrdinates = 1L << bits;

            // get ranges for Sydney query to measure effectiveness
            double lat1 = -33.806477;
            double lon1 = 151.181767;
            long t1 = minTime + (maxTime - minTime) / 2;
            double lat2 = -33.882896;
            double lon2 = 151.281330;
            long t2 = t1 + TimeUnit.HOURS.toMillis(1);
            int splits = 1;
            List<Range> ranges = h.query2(
                    scalePoint(lat1, lon1, t1, minTime, maxTime, maxOrdinates),
                    scalePoint(lat2, lon2, t2, minTime, maxTime, maxOrdinates), splits);
            ranges.forEach(System.out::println);

            int numPartitions = 10;
            int[] counts = new int[numPartitions];
            long step = maxIndexes / numPartitions;
            AtomicLong inBounds = new AtomicLong();
            AtomicLong inRanges = new AtomicLong();
            AtomicLong missed = new AtomicLong();
            AtomicLong count = new AtomicLong();
            BinaryFixes.from(file, false, BinaryFixesFormat.WITH_MMSI) //
                    .doOnNext(log(count)) //
                    .forEach(fix -> {
                        long index = h.index(scalePoint(fix.lat(), fix.lon(), fix.time(), minTime,
                                maxTime, maxOrdinates));
                        int partition = (int) (index / step);
                        counts[partition]++;
                        boolean inRange = false;
                        for (Range r : ranges) {
                            if (index >= r.low() && index <= r.high()) {
                                inRanges.incrementAndGet();
                                inRange = true;
                                break;
                            }
                        }
                        if (fix.lat() >= Math.min(lat1, lat2) && fix.lat() <= Math.max(lat1, lat2)
                                && fix.lon() >= Math.min(lon1, lon2)
                                && fix.lon() <= Math.max(lon1, lon2) && fix.time() >= t1
                                && fix.time() <= t2) {
                            inBounds.incrementAndGet();
                            if (!inRange) {
                                missed.incrementAndGet();
                            }
                        }
                    });

            System.out.println("ranges=" + ranges.size() + ", hit rate="
                    + inBounds.get() / ((double) inRanges.get()) + ", missed=" + missed.get()
                    + " of " + inBounds.get());
            System.out.println();

            System.out.println("===============");
            System.out.println("== HISTOGRAM ==");
            System.out.println("===============");
            long sum = 0;
            for (int i = 0; i < numPartitions; i++) {
                sum += counts[i];
            }
            DecimalFormat df = new DecimalFormat("0.00000");
            for (int i = 0; i < numPartitions; i++) {
                if (counts[i] != 0) {
                    System.out.println(i + " -> " + df.format(counts[i] * 100.0 / sum));
                }
            }
            System.out.println("total=" + sum);
            System.out.println();

            System.exit(0);
            System.out.println("===================");
            System.out.println("== WRITING FILES ==");
            System.out.println("===================");

            OutputStream[] outs = new OutputStream[numPartitions];
            for (int i = 0; i < outs.length; i++) {
                outs[i] = new BufferedOutputStream(
                        new FileOutputStream(new File(folder + "/partition" + i + ".fix")));
            }
            count.set(0);
            ByteBuffer bb = BinaryFixes.createFixByteBuffer(BinaryFixesFormat.WITH_MMSI);
            BinaryFixes.from(file, false, BinaryFixesFormat.WITH_MMSI) //
                    .doOnNext(log(count)) //
                    .doOnNext(fix -> {
                        long x = Math.round(Math.floor((fix.lat() + 90) / 180.0 * maxIndexes));
                        long y = Math.round(Math.floor((fix.lon() + 180) / 360.0 * maxIndexes));
                        long z = Math.round(Math.floor((fix.time() - minTime)
                                / ((double) maxTime - minTime) * maxIndexes));
                        long index = h.index(x, y, z);
                        int partition = (int) (index / step);
                        bb.clear();
                        BinaryFixes.write(fix, bb, BinaryFixesFormat.WITH_MMSI);
                        try {
                            outs[partition].write(bb.array());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).doOnError(t -> t.printStackTrace()) //
                    .subscribe();

            for (int i = 0; i < outs.length; i++) {
                outs[i].close();
            }

        }
    }

    private static Action1<? super Fix> log(AtomicLong count) {
        return f -> {
            long c = count.incrementAndGet();
            if (c % 1000000 == 0) {
                System.out
                        .println("count=" + new DecimalFormat("0.###").format(c / 1000000.0) + "m");
            }
        };
    }

    private static long[] scalePoint(double lat, double lon, long time, long minTime, long maxTime,
            long max) {
        long x = scale((lat + 90.0) / 180, max);
        long y = scale((lon + 180.0) / 360, max);
        long z = scale(((double) time - minTime) / (maxTime - minTime), max);
        return new long[] { x, y, z };
    }

    private static long scale(double d, long max) {
        Preconditions.checkArgument(d >= 0 && d <= 1);
        if (d == 1) {
            return max;
        } else {
            return Math.round(Math.floor(d * (max + 1)));
        }
    }

}
