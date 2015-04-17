package au.gov.amsa.geo.adhoc;

import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.util.nmea.NmeaMessage;

public class PercentSatelliteReports {

    public static void main(String[] args) {
        String filename = "/media/analysis/nmea/2014/NMEA_ITU_20140815.gz";

        Streams.nmeaFromGzip(filename).map(Streams.LINE_TO_NMEA_MESSAGE)
                .compose(Streams.<NmeaMessage> valueIfPresent()).forEach(m -> {
                    // ha! source not available!
                        if (m.getSource() != null)
                            System.out.println("source=" + m.getSource());
                    });
    }

}
