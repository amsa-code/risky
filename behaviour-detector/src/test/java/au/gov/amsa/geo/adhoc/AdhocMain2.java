package au.gov.amsa.geo.adhoc;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.davidmoten.hilbert.HilbertCurve;
import org.davidmoten.hilbert.SmallHilbertCurve;

import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.BinaryFixesFormat;

public class AdhocMain2 {

    public static void main(String[] args) {

        File file = new File("/media/an/temp/out.fix");

        int choice = 2;
        if (choice == 1) {
            // write the file
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file, true))) {
                AtomicLong count = new AtomicLong();
                Streams.nmeaFromGzip("/media/an/temp/2017-11-16.txt.gz") //
                        .compose(x -> Streams.extractFixes(x)).doOnNext(f -> {
                            long c = count.incrementAndGet();
                            if (c % 10000 == 0) {
                                System.out.println("count=" + c);
                            }
                        }) //
                        .forEach(f -> BinaryFixes.write(f, os, BinaryFixesFormat.WITH_MMSI)); //
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {

            long minTime = BinaryFixes.from(file, false, BinaryFixesFormat.WITH_MMSI) //
                    .map(x -> x.time()).reduce((x, y) -> Math.min(x, y)).toBlocking().single();

            long maxTime = BinaryFixes.from(file, false, BinaryFixesFormat.WITH_MMSI) //
                    .map(x -> x.time()).reduce((x, y) -> Math.max(x, y)).toBlocking().single();
            System.out.println("start=" + new Date(minTime) + ", finish=" + new Date(maxTime));

            SmallHilbertCurve h = HilbertCurve.small().bits(16).dimensions(3);
            int numPartitions = 1000;
            int[] counts = new int[numPartitions];
            long step = Long.MAX_VALUE / numPartitions;
            BinaryFixes.from(file, false, BinaryFixesFormat.WITH_MMSI) //
                    .forEach(fix -> {
                        long x = Math.round(Math.floor((fix.lat() + 90) / 180.0 * Long.MAX_VALUE));
                        long y = Math.round(Math.floor((fix.lon() + 180) / 360.0 * Long.MAX_VALUE));
                        long z = Math.round(
                                Math.floor((fix.time() - minTime) / ((double) maxTime - minTime) * Long.MAX_VALUE));
                        long index = h.index(x, y, z);
                        int partition = (int) (index / step);
                        counts[partition]++;
                    });
            for (int i = 0; i < numPartitions; i++) {
                System.out.println(i + " -> " + counts[i]);
            }

        }
    }

}
