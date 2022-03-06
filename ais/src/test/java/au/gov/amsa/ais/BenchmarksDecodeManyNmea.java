package au.gov.amsa.ais;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.util.nmea.NmeaMessage;
import rx.Observable;

@State(Scope.Benchmark)
public class BenchmarksDecodeManyNmea {

    private static final List<String> nmeaLines = Streams.nmeaFromGzip(new File("src/test/resources/ais.txt.gz"))
            .toList().toBlocking().single();

    private static final List<String> nmeaLinesShorter = nmeaLines.subList(0, 1000);

    @Benchmark
    public void parseManyFixesShorter() throws IOException {
        // process 1K lines
        Streams //
                .extractFixes(Observable.from(nmeaLinesShorter)) //
                .subscribe();
    }

    @Benchmark
    public void parseManyNmeaMessage() throws IOException {
        // process 44K lines
        Observable //
                .from(nmeaLines) //
                .map(Streams.LINE_TO_NMEA_MESSAGE) //
                .compose(Streams.<NmeaMessage>valueIfPresent()) //
                .subscribe();
    }

    public static void main(String[] args) {
        while (true) {
            Streams //
                    .extractFixes(Observable.from(nmeaLines)) //
                    .subscribe();
        }
    }

}
