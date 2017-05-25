package au.gov.amsa.geo.adhoc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.util.Files;
import rx.Observable;

public class VesselsInGbrMain {

    public static void main(String[] args) {
        Pattern pattern = Pattern.compile(".*\\.track");
        List<File> list = new ArrayList<File>();
        list.addAll(Files.find(new File("/media/an/binary-fixes-5-minute/2014"), pattern));
        list.addAll(Files.find(new File("/media/an/binary-fixes-5-minute/2015"), pattern));
        list.addAll(Files.find(new File("/media/an/binary-fixes-5-minute/2016"), pattern));
        AtomicInteger count = new AtomicInteger();
        Observable.from(list) //
                .groupBy(f -> count.getAndIncrement() % Runtime.getRuntime().availableProcessors()) //
                .flatMap(files -> files.doOnNext(System.out::println) //
                        .flatMap(f -> BinaryFixes.from(f) //
                                .filter(fix -> inGbr(fix)) //
                                .map(fix -> fix.mmsi()) //
                                .distinct()))
                .distinct() //
                .toBlocking() //
                .forEach(System.out::println);
    }

    private static boolean inGbr(Fix fix) {
        return fix.lat() >= -27.8 && fix.lat() <= -8.4 && fix.lon() >= 142 && fix.lon() <= 162;
    }

}
