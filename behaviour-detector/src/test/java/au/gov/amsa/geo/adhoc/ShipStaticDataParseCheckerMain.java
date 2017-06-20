package au.gov.amsa.geo.adhoc;

import java.io.File;

import com.github.davidmoten.rx.Transformers;

import au.gov.amsa.ais.rx.Streams;
import rx.Observable;

public class ShipStaticDataParseCheckerMain {

    public static void main(String[] args) {
        Observable<String> nmea = Streams.nmeaFrom(new File(
                "/media/an/amsa_26_05_2017_5_IEC/iec/amsa_26_05_2017_5_ITU5_20170526_1.txt"))
                .compose(Transformers.split("\\|"))
                .doOnNext(System.out::println);
        Streams.extractWithLines(nmea) //
                .filter(m -> m.getError() != null) //
                .doOnNext(System.out::println) //
                .count().toBlocking().single();
    }

}
