package au.gov.amsa.geo.adhoc;

import java.io.File;
import java.text.ParseException;
import java.util.Arrays;

import au.gov.amsa.geo.distance.OperatorEffectiveSpeedChecker;
import au.gov.amsa.geo.model.SegmentOptions;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.Fix;
import rx.Observable;
import rx.functions.Func2;

public class EffectiveSpeedCheckFailures2Main {

    public static void main(String[] args) throws ParseException {
        SegmentOptions options = SegmentOptions.builder().acceptAnyFixHours(12L).maxSpeedKnots(50)
                .build();
        tasmania().groupBy(fix -> fix.mmsi())
                .flatMap(g -> g.lift(new OperatorEffectiveSpeedChecker(options))
                        .buffer(2,
                                1)
                .filter(list -> list.size() == 2 && list.get(0).isOk() && !list.get(1).isOk())
                .doOnNext(list -> {
                    System.out.println(" ok," + list.get(0));
                    System.out.println("bad," + list.get(1));
                }))
                // count and go
                .count().toBlocking().single();
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
                }).flatMapIterable(x -> x);
    }
}
