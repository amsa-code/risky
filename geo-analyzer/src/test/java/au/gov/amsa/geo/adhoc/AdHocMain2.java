package au.gov.amsa.geo.adhoc;

import java.io.File;

import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.BinaryFixesFormat;

public class AdHocMain2 {
    
    public static void main(String[] args) {
        BinaryFixes.from(new File("/home/dxm/combinedSortedTracks2/2019-01-02.track.gz"), true, BinaryFixesFormat.WITH_MMSI) //
        .filter(fix -> fix.mmsi() == 503432000) //
        .doOnNext(System.out::println)
        .subscribe();
    }

}
