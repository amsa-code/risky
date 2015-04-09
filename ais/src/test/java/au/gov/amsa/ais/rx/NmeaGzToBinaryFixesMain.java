package au.gov.amsa.ais.rx;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.functions.Func1;
import rx.schedulers.Schedulers;
import au.gov.amsa.risky.format.BinaryFixesWriter;
import au.gov.amsa.risky.format.Fix;

public class NmeaGzToBinaryFixesMain {

    private static final String BY_MONTH = "month";
    private static final String BY_YEAR = "year";
    private static final Logger log = LoggerFactory.getLogger(NmeaGzToBinaryFixesMain.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        log.info("starting");

        final String inputFilename = prop("input", "/home/dxm/temp");
        final String outputFilename = prop("output", "target/binary");
        final String pattern = prop("pattern", "NMEA_ITU_.*.gz");
        final String by = prop("by", BY_MONTH);

        log.info("Converting NMEA files in " + inputFilename);
        log.info("to BinaryFixes format in " + outputFilename);
        log.info("using pattern='" + pattern + "'");

        File input = new File(inputFilename);
        File output = new File(outputFilename);
        long t = System.currentTimeMillis();

        int logEvery = 100000;

        // append fixes to a file once you have this many for the one mmsi
        int writeBufferSize = 100;

        // Note that the ring buffer size is twice the linesPerProcessor
        final int ringBufferSize = 16 * 8192;
        System.getProperty("rx.ring-buffer.size", ringBufferSize + "");
        int linesPerProcessor = ringBufferSize / 2;
        // don't downsample
        long downSampleIntervalMs = TimeUnit.MINUTES.toMillis(0);
        Pattern inputPattern = Pattern.compile(pattern);

        final Func1<Fix, String> fileMapper;
        if (BY_MONTH.equals(by))
            fileMapper = new BinaryFixesWriter.ByMonth(output);
        else if (BY_YEAR.equals(by))
            fileMapper = new BinaryFixesWriter.ByYear(output);
        else
            throw new RuntimeException("unknown file mapper (by):" + by);

        Streams.writeFixesFromNmeaGz(input, inputPattern, output, logEvery, writeBufferSize,
                Schedulers.computation(), linesPerProcessor, downSampleIntervalMs, fileMapper)
                .count().toBlocking().single();
        Thread.sleep(1000);
        // else {
        // // read 11 million NMEA lines
        // Streams.nmeaFromGzip("/home/dxm/temp/NMEA_ITU_20150101.gz")
        // // Streams.nmeaFromGzip("/home/dxm/temp/temp.txt.gz")
        // // buffer in groups of 20,000 to assign to computation
        // // threads
        // // buffer
        // .buffer(20000)
        // // parse the messages asynchronously using computation
        // // scheduler
        // .flatMap(
        // new Func1<List<String>, Observable<Timestamped<AisMessage>>>() {
        // @Override
        // public Observable<Timestamped<AisMessage>> call(
        // List<String> list) {
        // return Streams
        // // extract the messages from a list
        // .extractMessages(from(list))
        // // do async
        // .subscribeOn(
        // Schedulers.computation());
        // }
        // })
        // // log stuff
        // .lift(Logging.<Timestamped<AisMessage>> logger()
        // .showMemory().showRateSince("rate", 1000)
        // .showCount().every(100000).log())
        // // count emitted
        // .count()
        // // log count
        // .lift(Logging.<Integer> logger().prefix("count=").log())
        // // go
        // .toBlocking().single();
        //
        // Thread.sleep(3000);
        // }

        log.info("finished in " + (System.currentTimeMillis() - t) / 1000.0 + "s");
    }

    private static String prop(String name, String defaultValue) {
        return System.getProperty(name, defaultValue);
    }
}
