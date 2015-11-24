package au.gov.amsa.geo.adhoc;

import java.io.File;

import au.gov.amsa.ais.message.AisAidToNavigation;
import au.gov.amsa.ais.rx.Streams;

public class AdHocMain {
    public static void main(String[] args) {
        Streams.extract(Streams.nmeaFrom(new File("/media/an/temp/2015-11-12.txt")))
                //
                .filter(m -> m.getMessage().isPresent())
                //
                .filter(m -> m.getMessage().get().message().getMessageId() == 21)
                //
                // .map(m -> (AisShipStatic) m.getMessage().get().message())

                //
                .map(m -> (AisAidToNavigation) m.getMessage().get().message())
                //
                .filter(m -> String.valueOf(m.getMmsi()).startsWith("999"))
                // .distinct(m -> m.getMmsi())
                // .filter(m -> m.getMmsi() == 553111494)
                //
                .doOnNext(System.out::println).subscribe();
    }

    private static boolean isAton(String mmsi) {
        return mmsi.startsWith("99");
    }
}
