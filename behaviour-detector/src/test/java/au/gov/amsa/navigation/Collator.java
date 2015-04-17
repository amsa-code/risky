package au.gov.amsa.navigation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import au.gov.amsa.ais.message.AisShipStaticA;
import au.gov.amsa.ais.rx.Streams;

import com.github.davidmoten.rx.slf4j.Logging;

public class Collator {

    public static void main(String[] args) throws IOException {

        final PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(
                "target/destinations.txt"), Charset.forName("UTF-8")));

        File[] files = new File("/media/analysis/nmea/2013").listFiles(f -> f.getName().endsWith(
                ".gz"));
        Arrays.sort(files, (f1, f2) -> f1.getName().compareTo(f2.getName()));
        Observable<File> fileList = Observable.from(files).lift(
                Logging.<File> logger().showValue().log());
        final AtomicInteger count = new AtomicInteger();
        Observable<Observable<String>> nmeas = Streams.nmeasFromGzip(fileList);
        nmeas.flatMap(nmea -> getDestinations(nmea.doOnNext(line -> {
            int n = count.incrementAndGet();
            if (n % 1000000 == 0)
                System.out.println("lines read=" + (n / 1000000) + "m");
        })))
                // non null
                .filter(line -> {
                    if (line == null)
                        System.out.println("line is null!");
                    return line != null;
                })
                .distinct()
                .doOnNext(destination -> {
                    out.println(destination);
                    out.flush();
                })
                .lift(Logging.<String> logger().showCount().showValue().showMemory().every(100)
                        .log()).count().toBlocking().single();
        out.close();
    }

    private static Observable<String> getDestinations(Observable<String> nmea) {
        return Streams.extractMessages(nmea).flatMap(m -> {
            if (m.message() instanceof AisShipStaticA) {
                AisShipStaticA msg = (AisShipStaticA) m.message();
                if (msg.getDestination() != null && !msg.getDestination().trim().isEmpty()) {
                    return Observable.just(msg.getDestination());
                }
            }
            return Observable.empty();
        }).onBackpressureBuffer();
    }

}
