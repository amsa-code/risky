package au.gov.amsa.geo.adhoc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;
import java.util.regex.Pattern;

import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import au.gov.amsa.ais.AisMessage;
import au.gov.amsa.ais.Timestamped;
import au.gov.amsa.ais.message.AisShipStatic;
import au.gov.amsa.ais.message.AisShipStaticA;
import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.ais.rx.Streams.TimestampedAndLine;
import au.gov.amsa.util.Files;

import com.github.davidmoten.rx.slf4j.Logging;
import com.google.common.base.Optional;

public class ShipStaticDataMain {

    public static void main(String[] args) throws FileNotFoundException {
        PrintStream out = new PrintStream("target/out.txt");
        // String filename = "/media/an/nmea/2014/NMEA_ITU_20140815.gz";
        List<File> files = Files.find(new File("/media/an/nmea/2014/"),
                Pattern.compile("NMEA_ITU.*.gz"));

        Observable
                .from(files)
                .buffer(Math.max(1, files.size() / Runtime.getRuntime().availableProcessors()) - 1)
                .flatMap(
                        list -> Observable.from(list)
                                .lift(Logging.<File> logger().showValue().showMemory().log())
                                .concatMap(file -> Streams.extract(Streams.nmeaFromGzip(file))
                                //
                                // .lift(Logging
                                // .<TimestampedAndLine<AisMessage>>
                                // logger()
                                // .showCount().every(1000000).log())
                                //
                                        .flatMap(aisShipStaticOnly)
                                        //
                                        .map(m -> m.getMessage().get().message())
                                        //
                                        .filter(m -> m instanceof AisShipStaticA)
                                        //
                                        .cast(AisShipStaticA.class)
                                        //
                                        .distinct(m -> m.getMmsi()))
                                //
                                .distinct(m -> m.getMmsi())
                                //
                                .subscribeOn(Schedulers.computation()))
                //
                .distinct(m -> m.getMmsi())
                //
                .doOnNext(
                        m -> {
                            out.format("%s,%s,%s,%s,%s,%s,%s,%s\n", m.getMmsi(), m.getImo().or(-1),
                                    "A", m.getShipType(), (float) m
                                            .getMaximumPresentStaticDraughtMetres(), m
                                            .getDimensionA().or(-1), m.getDimensionB().or(-1), m
                                            .getDimensionC().or(-1), m.getDimensionD().or(-1));
                            out.flush();
                        })
                // go
                .count().toBlocking().single();
        out.close();
    }

    private static Func1<TimestampedAndLine<AisMessage>, Observable<TimestampedAndLine<AisShipStatic>>> aisShipStaticOnly = m -> {
        Optional<Timestamped<AisMessage>> message = m.getMessage();
        if (message.isPresent() && message.get().message() instanceof AisShipStatic) {
            @SuppressWarnings("unchecked")
            TimestampedAndLine<AisShipStatic> m2 = (TimestampedAndLine<AisShipStatic>) (TimestampedAndLine<?>) m;
            return Observable.just(m2);
        } else
            return Observable.empty();
    };
}
