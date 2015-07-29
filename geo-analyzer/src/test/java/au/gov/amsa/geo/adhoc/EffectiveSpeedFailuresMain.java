package au.gov.amsa.geo.adhoc;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import rx.Observable;
import au.gov.amsa.geo.distance.OperatorEffectiveSpeedChecker;
import au.gov.amsa.geo.model.SegmentOptions;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.util.Files;

public class EffectiveSpeedFailuresMain {

    public static void main(String[] args) {

        Pattern pattern = Pattern.compile(".*\\.track");
        List<File> files = Files.find(new File("/media/an/binary-fixes-5-minute/2015"), pattern);
        int count = Observable
                .from(files)
                .filter(file -> !file.getName().equals("0.track"))
                // .lift(Logging.<File> logger().showCount().showValue().log())
                // read fixes from file
                .flatMap(
                        file -> BinaryFixes
                                .from(file)
                                .lift(new OperatorEffectiveSpeedChecker(SegmentOptions.builder()
                                        .acceptAnyFixHours(12L).build()))
                                .filter(check -> !check.isOk())
                                .reduce(new MmsiCount(0, 0),
                                        (mc, fix) -> new MmsiCount(fix.fix().mmsi(), mc.count + 1))
                                .filter(mc -> mc.count >= 100))
                .toSortedList((a, b) -> Long.compare(b.count, a.count))
                // flatten
                .flatMapIterable(x -> x)
                // print answer
                .doOnNext(mc -> System.out.println(mc.mmsi + " " + mc.count))
                //
                .count()
                // go
                .toBlocking().single();
        System.out.println(count);
    }

    private static class MmsiCount {
        long mmsi;
        long count;

        MmsiCount(long mmsi, long count) {
            this.mmsi = mmsi;
            this.count = count;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("MmsiCount [mmsi=");
            builder.append(mmsi);
            builder.append(", count=");
            builder.append(count);
            builder.append("]");
            return builder.toString();
        }

    }
}
