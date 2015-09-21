package au.gov.amsa.animator;

import java.io.File;
import java.util.Arrays;

import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.BinaryFixesFormat;
import au.gov.amsa.risky.format.Fix;
import rx.Observable;
import rx.functions.Func2;

public class Sources {

    public static Observable<Fix> singleDay() {
        File file = new File("/media/an/daily-fixes/2014/2014-02-01.fix");
        return BinaryFixes.from(file, true, BinaryFixesFormat.WITH_MMSI);
        // File file = new File("/media/an/nmea/2014/NMEA_ITU_20140201.gz");
        // return Streams.extractFixes(Streams.nmeaFromGzip(file))
        // // .filter(fix -> fix.mmsi() == 503433000)
        // .take(10000000);
    }

    public static Observable<Fix> tasmania() {
        Long[] vessels = new Long[] { 503433000L, 503432000L, 503087000L };
        return Observable.from(Arrays.asList(vessels))
                .map(mmsi -> new File("/media/an/binary-fixes-5-minute/2015/" + mmsi + ".track"))
                .flatMap(file -> BinaryFixes.from(file))
                .toSortedList(new Func2<Fix, Fix, Integer>() {

                    @Override
                    public Integer call(Fix a, Fix b) {
                        return Long.compare(a.time(), b.time());
                    }
                }).doOnNext(list -> System.out.println("sorted")).flatMapIterable(x -> x);
    }

}
