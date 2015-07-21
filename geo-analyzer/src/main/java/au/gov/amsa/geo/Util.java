package au.gov.amsa.geo;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import au.gov.amsa.geo.model.Fix;
import au.gov.amsa.geo.model.HasPosition;
import au.gov.amsa.util.Files;
import au.gov.amsa.util.navigation.Position;

public class Util {

    private static Logger log = Logger.getLogger(Util.class);

    public static String memoryUsage() {
        Runtime r = Runtime.getRuntime();
        long used = r.totalMemory() - r.freeMemory();
        return "usedHeap=" + (used / 1000000) + "MB, percent=" + (100.0 * used / r.maxMemory());
    }

    public static void logMemoryUsage() {
        log.info(memoryUsage());
    }

    public static Position toPos(HasPosition a) {
        return new Position(a.getPosition().getLat(), a.getPosition().getLon());
    }

    public static Observable<File> getFiles(String directory, final String pattern) {
        return Observable.from(Files.find(new File(directory), Pattern.compile(pattern)));
    }

    public static final Func2<Fix, Fix, Integer> COMPARE_FIXES_BY_POSITION_TIME = new Func2<Fix, Fix, Integer>() {
        @Override
        public Integer call(Fix a, Fix b) {
            return ((Long) a.getTime()).compareTo(b.getTime());
        }
    };

    public static final Func1<List<Fix>, Observable<Fix>> TO_OBSERVABLE = new Func1<List<Fix>, Observable<Fix>>() {
        @Override
        public Observable<Fix> call(List<Fix> list) {
            return Observable.from(list);
        }
    };

}
