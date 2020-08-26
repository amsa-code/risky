package au.gov.amsa.geo.adhoc;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.BinaryFixesFormat;

public class BinaryFixesWithMmsiErrorDiagnosisMain {

    public static void main(String[] args) throws FileNotFoundException, IOException {
        File file = new File("target/out.gz");
        try (OutputStream out = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            Streams //
                    .nmeaFromGzip(new File("/home/dxm/AIS/2019-01-01.txt.gz")) //
                    .compose(o -> Streams.extractFixes(o)) //
                    .doOnNext(fix -> {
                        BinaryFixes.write(fix, out, BinaryFixesFormat.WITH_MMSI);
                    }) //
                    .toBlocking().subscribe();
        }
        BinaryFixes.from(file, true, BinaryFixesFormat.WITH_MMSI) //
                .subscribe();
    }

}
