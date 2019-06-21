package au.gov.amsa.ais.rx;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import au.gov.amsa.ais.AisMessage;
import au.gov.amsa.ais.message.AisPosition;
import au.gov.amsa.ais.message.AisPositionA;
import au.gov.amsa.ais.message.AisPositionBExtended;
import rx.Observable;

public class NmeaGzToCsvMain {

    private static final String POSITION_FORMAT = "%s,%s,%s,%s,%s,%s,%s,%s,%s,\"%s\",%s,%s,%s,%s,%s";
    private static final String ABSENT = "";

    public static void main(String[] args) throws IOException {

        long t = System.currentTimeMillis();
        File input = new File(System.getProperty("user.home") + "/Downloads/2018-11-27.txt.gz");
        long[] count = new long[1];
        Observable<String> nmea = Streams.nmeaFromGzip(input);
        long n = 100000;
        try (PrintStream out = new PrintStream(
                new BufferedOutputStream(new FileOutputStream("target/2018-11-27-positions.csv")))) {
            Streams.extract(nmea) //
                    .doOnNext(x -> {
                        count[0]++;
                        if (count[0] % n == 0) {
                            System.out.println(count[0] / 1000000.0 + "m records read");
                        }
                    }) //
                    .filter(x -> x.getMessage().isPresent()) //
                    .map(x -> csv(x.getMessage().get().message())) //
                    .filter(x -> !x.isEmpty()) //
                    .doOnNext(x -> out.println(x)) //
                    .subscribe();
        }
        System.out.println("finished in "+ (System.currentTimeMillis() - t)/1000.0 + "s");
    }

    private static String csv(AisMessage m) {
        if (m instanceof AisPositionA) {
            return csv((AisPositionA) m);
        } else if (m instanceof AisPositionBExtended) {
            return csv((AisPositionBExtended) m);
        } else if (m instanceof AisPosition) {
            return csv((AisPosition) m);
        } else {
            return "";
        }
    }

    private static String csv(AisPositionA p) {
        return String.format(POSITION_FORMAT, //
                p.getMmsi(), //
                p.getMessageId(), //
                blankIfNull(p.getLatitude()), //
                blankIfNull(p.getLongitude()), //
                blankIfNull(p.getSpeedOverGroundKnots()), //
                blankIfNull(p.getCourseOverGround()), //
                blankIfNull(p.getTrueHeading()), //
                blankIfNull(p.getNavigationalStatus()), //
                blankIfNull(p.getRateOfTurn()), //
                blankIfNull(p.getSource()), //
                // TODO add latencySeconds
                blankIfNull(p.getSpecialManoeuvreIndicator()), //
                blankIfNull(p.getTimeSecondsOnly()), //
                yn(p.isHighAccuracyPosition()), //
                yn(p.isUsingRAIM()), //
                cls(p));
    }

    private static String csv(AisPositionBExtended p) {
        return String.format(POSITION_FORMAT, //
                p.getMmsi(), //
                p.getMessageId(), //
                blankIfNull(p.getLatitude()), //
                blankIfNull(p.getLongitude()), //
                blankIfNull(p.getSpeedOverGroundKnots()), //
                blankIfNull(p.getCourseOverGround()), //
                blankIfNull(p.getTrueHeading()), //
                ABSENT, //
                ABSENT, //
                blankIfNull(p.getSource()), //
                // TODO add latencySeconds
                ABSENT, //
                blankIfNull(p.getTimeSecondsOnly()), //
                yn(p.isHighAccuracyPosition()), //
                yn(p.isUsingRAIM()), //
                cls(p));
    }

    private static String csv(AisPosition p) {
        return String.format(POSITION_FORMAT, //
                p.getMmsi(), //
                p.getMessageId(), //
                blankIfNull(p.getLatitude()), //
                blankIfNull(p.getLongitude()), //
                blankIfNull(p.getSpeedOverGroundKnots()), //
                blankIfNull(p.getCourseOverGround()), //
                blankIfNull(p.getTrueHeading()), //
                ABSENT, //
                ABSENT, //
                blankIfNull(p.getSource()), //
                ABSENT, //
                blankIfNull(p.getTimeSecondsOnly()), //
                yn(p.isHighAccuracyPosition()), //
                yn(p.isUsingRAIM()), //
                cls(p));
    }

    private static String cls(AisPosition p) {
        if (p instanceof AisPositionA) {
            return "A";
        } else if (p instanceof AisPositionBExtended) {
            return "B";
        } else {
            return "";
        }
    }

    private static String yn(boolean b) {
        return b ? "Y" : "N";
    }

    private static String blankIfNull(Object o) {
        if (o == null) {
            return "";
        } else {
            return o.toString();
        }
    }

}
